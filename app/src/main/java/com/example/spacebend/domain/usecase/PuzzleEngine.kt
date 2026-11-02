package com.example.spacebend.domain.usecase

import com.example.spacebend.domain.model.*
import kotlin.math.*

enum class EditResult { OK, NO_SURFACE, INVALID_SHAPE, OUTSIDE_SURFACE, OVERLAP, LIMIT, BUSY }
/** Deterministic 240 Hz simulation. Platform state and SDK objects never enter this class. */
class PuzzleEngine(val mode: GameMode,val mission: Mission) {
    private val surfaceMap=linkedMapOf<String,Surface>()
    val surfaces: List<Surface> get()=surfaceMap.values.toList()
    private val portalList=mutableListOf<Portal>()
    val portals: List<Portal> get()=portalList.toList()
    var payload: Payload?=null;private set
    var phase=ExperimentPhase.PLANNING;private set
    var elapsedSeconds=0.0;private set
    var resultSeconds: Double?=null;private set
    var collisions=0;private set
    var collapseProgress=0.0;private set
    private var nextId=1L
    private var accumulator=0.0
    private var fallingTime=0.0
    private var enteringId: Long?=null
    private var enteringSide=1.0
    private var exitGrace: Long?=null
    private var sequenceValid=true
    fun updateSurfaces(newSurfaces: List<Surface>) {surfaceMap.clear();newSurfaces.forEach{surfaceMap[it.id]=it}}
    fun addPortal(surfaceId: String,outline: List<V2>): EditResult {
        if(phase!=ExperimentPhase.PLANNING)return EditResult.BUSY
        val surface=surfaceMap[surfaceId]?:return EditResult.NO_SURFACE
        if(outline.size<8||!Polygon.isSimple(outline)||Polygon.area(outline)<.035)return EditResult.INVALID_SHAPE
        if(mode==GameMode.CHALLENGE&&portalList.size>=mission.maxPortals)return EditResult.LIMIT
        if(!surface.permits(outline))return EditResult.OUTSIDE_SURFACE
        if(overlaps(surfaceId,outline,null))return EditResult.OVERLAP
        portalList+=Portal(nextId++,surfaceId,outline,portalList.size)
        return EditResult.OK
    }
    private fun overlaps(surface: String,outline: List<V2>,excluding: Long?): Boolean = portalList.any {p->
        p.id!=excluding&&p.surfaceId==surface&&Polygon.overlaps(outline,p.outline)
    }
    fun movePortal(id: Long,targetSurfaceId: String,delta: V2): EditResult {
        if(phase!=ExperimentPhase.PLANNING)return EditResult.BUSY
        val i=portalList.indexOfFirst{it.id==id};if(i<0)return EditResult.NO_SURFACE
        val p=portalList[i];val s=surfaceMap[p.surfaceId]?:return EditResult.NO_SURFACE
        if(targetSurfaceId!=p.surfaceId)return EditResult.NO_SURFACE
        val next=p.outline.map{it+delta}
        if(!s.permits(next))return EditResult.OUTSIDE_SURFACE
        if(overlaps(p.surfaceId,next,id))return EditResult.OVERLAP
        portalList[i]=p.copy(outline=next);return EditResult.OK
    }
    fun deletePortal(id: Long): Boolean {
        if(phase!=ExperimentPhase.PLANNING)return false
        val removed=portalList.removeAll{it.id==id}
        if(removed)portalList.indices.forEach{portalList[it]=portalList[it].copy(order=it)}
        return removed
    }
    fun launch(kind: PayloadKind): Boolean {
        if(phase!=ExperimentPhase.PLANNING||portalList.size<2)return false
        if(mode==GameMode.CHALLENGE&&(kind !in mission.allowed||portalList.size<mission.optimalPortals))return false
        val first=portalList.first();val s=surfaceMap[first.surfaceId]?:return false
        if(!portalList.all{p->Polygon.fitsCircle(p.outline,p.center,kind.radius)})return false
        payload=Payload(kind,s.world(first.center)+s.normal*(kind.radius+.07),-s.normal*kind.launchSpeed)
        phase=ExperimentPhase.RUNNING;enteringId=null;exitGrace=null;sequenceValid=true;return true
    }
    fun reset() {
        portalList.clear();payload=null;phase=ExperimentPhase.PLANNING;elapsedSeconds=0.0;resultSeconds=null
        accumulator=0.0;fallingTime=0.0;collapseProgress=0.0;collisions=0;enteringId=null;exitGrace=null
    }
    fun advance(delta: Double,tracking: Boolean=true): List<SimulationEvent> {
        if(!tracking||delta<=0.0||!delta.isFinite())return emptyList()
        val events=mutableListOf<SimulationEvent>()
        // Keep all time up to a one-second application suspension; UI separately stops on lifecycle pause.
        accumulator+=delta.coerceAtMost(1.0)
        while(accumulator+1e-10>=STEP){step(STEP,events);accumulator-=STEP}
        return events
    }
    private fun step(dt: Double,events: MutableList<SimulationEvent>) {
        if(phase==ExperimentPhase.PLANNING||phase==ExperimentPhase.RUNNING)elapsedSeconds+=dt
        val p=payload?:return
        if(phase==ExperimentPhase.FAILED&&!p.isFalling)return
        if(phase==ExperimentPhase.FALLING||phase==ExperimentPhase.FAILED){
            fallingTime+=dt;collapseProgress=(fallingTime/.65).coerceIn(0.0,1.0)
            if(p.isFalling) {
            val old=p.position;val next=old+p.velocity*dt+V3.GRAVITY*(.5*dt*dt)
            p.velocity+=V3.GRAVITY*dt
            val ground=surfaces.filter{it.normal.y>.5}.mapNotNull {s->
                val a=s.signedDistance(old)-p.kind.radius;val b=s.signedDistance(next)-p.kind.radius
                if(a>=0&&b<=0){val t=a/(a-b);val q=old+(next-old)*t;if(s.contains(s.project(q)))q else null}else null
            }.firstOrNull()
            if(ground!=null){p.position=ground;p.velocity=V3.ZERO;p.isFalling=false}else p.position=next
            }
            if(fallingTime>=.8&&phase==ExperimentPhase.FALLING){portalList.clear();if(mode==GameMode.CHALLENGE){phase=ExperimentPhase.FAILED;events+=SimulationEvent.Failed}else{phase=ExperimentPhase.PLANNING;payload=null}}
            return
        }
        integrate(dt,events,0)
        if(phase==ExperimentPhase.RUNNING&&p.loops>=2&&mode==GameMode.CHALLENGE){phase=ExperimentPhase.STABLE;resultSeconds=elapsedSeconds;events+=SimulationEvent.Stable}
    }
    private fun integrate(dt: Double,events: MutableList<SimulationEvent>,depth: Int) {
        if(phase!=ExperimentPhase.RUNNING&&phase!=ExperimentPhase.STABLE)return
        val p=payload?:return
        val old=p.position;val radius=p.kind.radius
        var acceleration=V3.GRAVITY
        // A conservative near-mouth attraction field accelerates approach and brakes departure.
        // Applying it only on entry would add energy forever and turn a closed loop into a ceiling collision.
        val suction=portalList.firstOrNull {portal->
            val s=surfaceMap[portal.surfaceId]?:return@firstOrNull false
            abs(s.signedDistance(old))<radius+.28&&Polygon.fitsCircle(portal.outline,s.project(old),radius)
        }
        if(suction!=null){
            val s=surfaceMap[suction.surfaceId]!!
            val d=s.signedDistance(old)
            val side=if(abs(d)>1e-8)sign(d) else sign(p.velocity.dot(s.normal)).let{if(it==0.0)1.0 else it}
            if(exitGrace!=suction.id&&p.velocity.dot(s.normal)*side<0&&enteringId!=suction.id){
                enteringId=suction.id;enteringSide=side
            }
            acceleration+=s.normal*(-side*1.5)
        }
        val next=old+p.velocity*dt+acceleration*(.5*dt*dt)
        val velocity=p.velocity+acceleration*dt
        var collided: V3?=null
        var collisionNormal=V3.ZERO
        var transferPortal: Portal?=null
        var transferSide=1.0
        var transferFraction=1.0
        var collisionFraction=Double.POSITIVE_INFINITY
        for(s in surfaceMap.values){
            val d0=s.signedDistance(old);val d1=s.signedDistance(next)
            val contact=SweepCollision.firstContact(s,old,next,radius)?:continue
            val side=if(abs(d0)>1e-8)sign(d0) else -sign(velocity.dot(s.normal)).let{if(it==0.0)1.0 else it}
            val denom=d0-d1
            val centerT=if(abs(denom)>1e-12)(d0/denom).coerceIn(0.0,1.0)else 0.0
            val centerAtPlane=old+(next-old)*centerT
            val aperture=portalList.firstOrNull {it.surfaceId==s.id&&Polygon.fitsCircle(it.outline,s.project(centerAtPlane),radius)&&Polygon.fitsCircle(it.outline,s.project(contact.center),radius)}
            if(aperture==null){
                if(contact.fraction<collisionFraction){collided=contact.center;collisionFraction=contact.fraction;collisionNormal=contact.normal}
                continue
            }
            if(exitGrace==aperture.id)continue
            if(enteringId==null){enteringId=aperture.id;enteringSide=side}
            if(enteringId==aperture.id&&d1*enteringSide<=0.0){
                val t=if(abs(denom)>1e-12)(d0/denom).coerceIn(0.0,1.0)else 1.0
                if(t<=transferFraction){transferFraction=t;transferPortal=aperture;transferSide=enteringSide}
            }
        }
        if(collided!=null&&(transferPortal==null||collisionFraction<=transferFraction)){
            p.position=collided;p.velocity=(velocity-collisionNormal*velocity.dot(collisionNormal))*.18;p.isFalling=true;collisions++
            phase=ExperimentPhase.FALLING;fallingTime=0.0;enteringId=null
            events+=SimulationEvent.Collided(collided);return
        }
        if(transferPortal!=null){
            val source=transferPortal;val sourceIndex=portalList.indexOfFirst{it.id==source.id}
            val destinationIndex=(sourceIndex+1)%portalList.size;val dest=portalList[destinationIndex]
            val ds=surfaceMap[dest.surfaceId]?:return
            // Full projected body must fit the aperture. Transfer occurs at the center-plane crossing;
            // adding body-radius offsets here would inject gravitational potential every cycle.
            val terminal=p.velocity+acceleration*(dt*transferFraction)
            val speed=terminal.length()
            val normal=ds.normal*transferSide
            p.position=ds.world(dest.center)+normal*.002
            p.velocity=normal*speed
            val remaining=dt*(1-transferFraction)
            if(sourceIndex!=p.expectedEntry){sequenceValid=false;p.traversals=0;p.loops=0}
            if(sourceIndex==0){sequenceValid=true}
            p.expectedEntry=destinationIndex;p.traversals++;p.lastExitId=dest.id;exitGrace=dest.id;enteringId=null
            if(destinationIndex==0&&sequenceValid)p.loops++
            events+=SimulationEvent.Transferred(source.id,dest.id,speed)
            if(remaining>1e-9&&depth<8)integrate(remaining,events,depth+1)
        }else{p.position=next;p.velocity=velocity}
        exitGrace?.let{id->val portal=portalList.find{it.id==id};val s=portal?.let{surfaceMap[it.surfaceId]};if(s==null||abs(s.signedDistance(p.position))>radius+.025)exitGrace=null}
        if(p.position.y < -20||p.position.length()>100){
            p.isFalling=true;phase=ExperimentPhase.FALLING;fallingTime=0.0;collisions++;events+=SimulationEvent.Collided(p.position)
        }
    }
    companion object { const val STEP=1.0/240.0 }
}
