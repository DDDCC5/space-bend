package com.example.spacebend.platform.spatial

import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.EditResult
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import com.pico.spatial.ui.platform.HandController
import kotlin.math.abs

internal data class InputFrame(val id: String,val ray: Ray,val trigger: Boolean,val grip: Boolean=false,val delete: Boolean=false,val stickClick: Boolean=false,val stickX: Float=0f,val hand: HandController=HandController.Unknown,val trackedHand: Boolean=false)
internal interface InputScene {
 fun panelAt(ray: Ray): PanelHit?
 fun movePanel(panel: GamePanel,point: V3)
 fun pulse(hand: HandController,strong: Boolean)
 fun stroke(points: List<V3>)
 fun drawSound(active: Boolean,point: V3?)
}
internal data class PanelHit(val panel: GamePanel,val point: V3,val distance: Double,val header: Boolean,val position: V3)
internal class InputRouter(private val vm: GameViewModel,private val scene: InputScene) {
 private data class Previous(val trigger: Boolean=false,val grip: Boolean=false,val stick: Boolean=false,var axisHeld: Boolean=false)
 private val previous=mutableMapOf<String,Previous>()
 private var owner: String?=null
 private var strokeSurface: String?=null
 private val strokePoints=mutableListOf<V2>()
 private var latched=false
 private var dragPortal: Long?=null
 private var lastDrag: V2?=null
 private var panelDrag: GamePanel?=null
 private var panelDistance=0.0
 private var panelOffset=V3.ZERO
 private var deleteOwner: String?=null
 private var deleting: Long?=null
 private var deleteTime=0.0
 private var vibrationTime=0.0
 fun cancel(){owner=null;strokeSurface=null;strokePoints.clear();dragPortal=null;panelDrag=null;latched=false;deleting=null;deleteOwner=null;deleteTime=0.0;vm.deleteProgress(0f);scene.stroke(emptyList());scene.drawSound(false,null);previous.clear()}
 fun process(frame: InputFrame,dt: Double) {
  val old=previous[frame.id]?:Previous()
  val pressed=frame.trigger&&!old.trigger
  val gripPressed=frame.grip&&!old.grip
  val state=vm.state.value
  if(frame.stickClick&&!old.stick){vm.onEvent(GameEvent.Show(GamePanel.PICKER))}
  var axis=old.axisHeld
  if(abs(frame.stickX)<.3f)axis=false
  if(GamePanel.PICKER in vm.state.value.panels&&abs(frame.stickX)>.65f&&!axis){vm.onEvent(GameEvent.Cycle(if(frame.stickX>0)1 else -1));axis=true}
  val panel=scene.panelAt(frame.ray)
  val hits=vm.engine.surfaces.mapNotNull{it.hit(frame.ray)}.sortedBy{it.distance}
  val hit=hits.firstOrNull()
  val overPanel=panel!=null&&(hit==null||panel.distance<=hit.distance)
  val portal=hit?.let{h->vm.engine.portals.firstOrNull{it.surfaceId==h.surface.id&&Polygon.contains(it.outline,h.surface.project(h.point))}}
  if(owner==null&&panel!=null&&overPanel&&(gripPressed||(frame.trackedHand&&pressed&&panel.header))){
   owner=frame.id;panelDrag=panel.panel;panelDistance=panel.distance;panelOffset=panel.position-panel.point
  }
  if(owner==frame.id&&panelDrag!=null){
   if(frame.grip||(frame.trackedHand&&frame.trigger))scene.movePanel(panelDrag!!,frame.ray.origin+frame.ray.direction*panelDistance+panelOffset)
   else {panelDrag=null;owner=null}
  }else if(owner==null&&pressed&&GamePanel.PICKER in state.panels&&!overPanel){
   vm.onEvent(GameEvent.Launch);latched=true;owner=frame.id
  }else if(state.tracking&&state.phase==ExperimentPhase.PLANNING&&!overPanel){
   if(owner==null&&gripPressed&&portal!=null){owner=frame.id;dragPortal=portal.id;lastDrag=hit!!.surface.project(hit.point)}
   if(owner==frame.id&&dragPortal!=null){
    if(frame.grip){
     val p=vm.engine.portals.firstOrNull{it.id==dragPortal};val same=p?.let{vm.engine.surfaces.firstOrNull{s->s.id==p.surfaceId}}?.hit(frame.ray)
     if(same!=null){val point=same.surface.project(same.point);val previous=lastDrag;if(previous!=null)vm.movePortal(p!!.id,same.surface.id,point-previous);lastDrag=point}
    }else {dragPortal=null;lastDrag=null;owner=null}
   }
   if(owner==null&&pressed&&hit!=null){owner=frame.id;latched=false;strokeSurface=hit.surface.id;strokePoints.clear();strokePoints+=hit.surface.project(hit.point)}
   if(owner==frame.id&&strokeSurface!=null&&!latched&&frame.trigger){
    val surface=vm.engine.surfaces.firstOrNull{it.id==strokeSurface};val same=surface?.hit(frame.ray)
    if(same!=null){
     val p=surface.project(same.point)
     if(strokePoints.isEmpty()||(p-strokePoints.last()).length()>.012){
      if(strokePoints.size<1024)strokePoints+=p
      scene.stroke(strokePoints.map{surface.world(it)+surface.normal*.009});scene.drawSound(true,same.point)
      if(strokePoints.size>=12&&(p-strokePoints.first()).length()<.07&&Polygon.area(strokePoints)>.035){
       strokePoints.removeAt(strokePoints.lastIndex)
       val result=vm.addPortal(surface.id,strokePoints.toList());scene.pulse(frame.hand,result==EditResult.OK)
       latched=true;strokeSurface=null;strokePoints.clear();scene.stroke(emptyList());scene.drawSound(false,null)
      }
     }
     vibrationTime+=dt;if(vibrationTime>=.045){scene.pulse(frame.hand,false);vibrationTime=0.0}
    }
   }
  }
  if(owner==frame.id&&!frame.trigger&&!frame.grip){
   if(strokeSurface!=null&&!latched){vm.notify("invalid_shape");scene.stroke(emptyList());scene.drawSound(false,null)}
   owner=null;strokeSurface=null;strokePoints.clear();latched=false
  }
  if(frame.delete&&portal!=null&&state.phase==ExperimentPhase.PLANNING&&!overPanel&&(deleteOwner==null||deleteOwner==frame.id)){
   if(deleting!=portal.id){deleting=portal.id;deleteOwner=frame.id;deleteTime=0.0};deleteTime+=dt;vm.deleteProgress((deleteTime/2).toFloat().coerceIn(0f,1f))
   if(deleteTime>=2){vm.deletePortal(portal.id);scene.pulse(frame.hand,true);deleting=null;deleteOwner=null;deleteTime=0.0;vm.deleteProgress(0f)}
  }else if(deleting!=null&&deleteOwner==frame.id){deleting=null;deleteOwner=null;deleteTime=0.0;vm.deleteProgress(0f)}
  previous[frame.id]=Previous(frame.trigger,frame.grip,frame.stickClick,axis)
 }
 fun finishFrame(activeIds: Set<String>){
  if(deleteOwner!=null&&deleteOwner !in activeIds){deleting=null;deleteOwner=null;deleteTime=0.0;vm.deleteProgress(0f)}
  if(owner!=null&&owner !in activeIds)cancel()
  previous.keys.filter{it !in activeIds}.forEach{previous.remove(it)}
 }
}
