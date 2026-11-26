package com.example.spacebend.platform.spatial

import android.content.Context
import android.util.Log
import com.example.spacebend.domain.model.*
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.controller.ControllerActionData
import com.pico.spatial.tracking.controller.ControllerTrackingProvider
import com.pico.spatial.tracking.hand.HandTrackingProvider
import com.pico.spatial.ui.platform.HandController
import com.pico.spatial.ui.platform.HandControllerHapticType
import com.pico.spatial.ui.platform.SpatialHandControllerHaptic
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import kotlin.math.*

internal class SceneRuntime(
 private val context: Context,private val vm: GameViewModel,val root: Entity,
 private val scope: CoroutineScope,private val haptic: SpatialHandControllerHaptic,
 private var panelBaseSizes: Map<GamePanel,V2>,
): InputScene {
 private val geometry=SceneGeometry()
 private val sound=SpatialSound(root)
 private val planes=PlaneSource(root,scope)
 private val controllers=ControllerTrackingProvider()
 private val hands=HandTrackingProvider()
 @Volatile private var actions: ControllerActionData?=null
 private val listener=ControllerTrackingProvider.ControllerActionListener {actions=it}
 private val router=InputRouter(vm,this)
 private val inputFocus=InputFocus()
 private val models=mutableMapOf<PayloadKind,Entity>()
 private val portals=mutableMapOf<Long,PortalVisual>()
 private val panelEntities=mutableMapOf<GamePanel,Entity>()
 private val panelHalfSizes=mutableMapOf<GamePanel,V2>()
 private val fittedPanels=mutableSetOf<GamePanel>()
 private val planeVisuals=mutableMapOf<String,PlaneOutlineVisual>()
 private val planeVisualPlan=PlaneVisualPlan()
 private var strokeVisual: Pair<Entity,MeshResource>?=null
 private var strokeKey: List<V3> = emptyList()
 private var portalRevision=-1L
 private var engineIdentity: Any?=null
 private var loaded=false
 private var started=false
 private var closed=false
 var active=true;private set
 private var lastTracking=false
 private var lastPanel=GamePanel.MENU
 private var workspacePosition=V3(0.0,1.35,-1.45)
 private var time=0.0
 private var scanTimer=0.0
 private var lastScanRevision=0L
 private val cursor=ModelEntity(geometry.dotMesh,geometry.amber)
 private val beam=ModelEntity(geometry.beamMesh,geometry.planeMaterial)
 init {
  root.setName("SpaceBendStageRoot");root.addChild(cursor);root.addChild(beam);cursor.enabled=false;beam.enabled=false
  for((direction,intensity) in listOf(V3(.4,-1.0,-.7) to 1500f,V3(-.5,-.3,.7) to 800f)){
   val light=Entity();light.components.set(DirectionalLightComponent(com.pico.spatial.core.math.Color4(1f,.96f,.93f,1f),intensity));light.components[TransformComponent::class.java]?.setQuaternion(facing(direction));root.addChild(light)
  }
 }
 fun attachPanel(id: GamePanel,entity: Entity) {
  panelEntities[id]=entity;root.addChild(entity);entity.setName("Panel_${id.name}");entity.enabled=id==vm.state.value.currentPanel
  entity.components[TransformComponent::class.java]?.setPosition(workspacePosition.sdk())
 }
 suspend fun load() {
  // Retain each loaded hierarchy; center its actual visual bounds under an identity motion owner.
  val catalog=JSONObject(context.assets.open("model-bounds.json").bufferedReader().use{it.readText()})
  for(kind in PayloadKind.entries){
   val owner=Entity();owner.setName("Payload_${kind.asset}");root.addChild(owner)
   try {
    val model=Entity.loadSuspend("asset://models/${kind.asset}.glb");owner.addChild(model)
    val source=catalog.getJSONObject(kind.asset);val min=source.getJSONArray("min");val max=source.getJSONArray("max")
    val center=V3((min.getDouble(0)+max.getDouble(0))*.5,(min.getDouble(1)+max.getDouble(1))*.5,(min.getDouble(2)+max.getDouble(2))*.5)
    model.components[TransformComponent::class.java]?.setPosition((-center).sdk())
    owner.enabled=false;models[kind]=owner
    Log.i("SpaceBend","Loaded ${kind.asset}; source center=$center, radius=${kind.radius}")
   }catch(e: Exception){owner.destroy();Log.e("SpaceBend","Payload loading failed: ${kind.asset}",e);vm.notify("asset_error");throw e}
  }
  loaded=true;root.components.set(GameRuntimeComponent(this));start();vm.sceneReady()
 }
 private fun start() {
  if(started||closed)return
  started=true;planes.start();controllers.addControllerActionListener(listener)
  controllers.start();hands.start()
  sound.play("audio/ambient-lab-loop.wav",null,vm.state.value.settings,loop=true,key="music",channel="music")
 }
 fun pause(){Log.i("SpaceBend","Scene paused");active=false;router.cancel();inputFocus.reset();actions=null;planes.stop();controllers.removeControllerActionListener(listener);controllers.stop();hands.stop();started=false;sound.pause()}
 fun resume(){Log.i("SpaceBend","Scene resume requested; loaded=$loaded closed=$closed");if(closed||!loaded)return;active=true;start();sound.resume()}
 private var diagnosticFrames=0L
 private var diagnosticAt=0L
 private var diagnosticWindowFrames=0L
 private var diagnosticTotalNanos=0L
 private var diagnosticMaxNanos=0L
 private var diagnosticSlowFrames=0L
 private var diagnosticPlaneBuilds=0L
 private inline fun <T> section(name: String,block: ()->T): T {
  android.os.Trace.beginSection("SpaceBend/$name")
  try{return block()}finally{android.os.Trace.endSection()}
 }
 fun frame(rawDelta: Double) {
  if(closed||!active||!started)return
  val frameStart=java.lang.System.nanoTime()
  diagnosticFrames++
  val now=android.os.SystemClock.uptimeMillis()
  if((context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0&&now-diagnosticAt>10000){
   diagnosticAt=now
   Log.i("SpaceBend","Frame=$diagnosticFrames delta=$rawDelta phase=${vm.engine.phase} tracking=${vm.state.value.tracking} callbackFrames=$diagnosticWindowFrames callbackAvgMs=${diagnosticTotalNanos/1e6/diagnosticWindowFrames.coerceAtLeast(1)} callbackMaxMs=${diagnosticMaxNanos/1e6} callbacksOver11ms=$diagnosticSlowFrames planeBuilds=$diagnosticPlaneBuilds")
   diagnosticWindowFrames=0;diagnosticTotalNanos=0;diagnosticMaxNanos=0;diagnosticSlowFrames=0;diagnosticPlaneBuilds=0
  }
  val dt=rawDelta.coerceIn(0.0,.1);time+=dt;scanTimer+=dt
  val scanRevision=vm.state.value.scanRevision
  val restarted=scanRevision!=lastScanRevision
  if(restarted){section("ScanRestart"){planes.restart()};lastScanRevision=scanRevision;router.cancel(clearButtons=false)}
  val changed=section("PlaneDrain"){planes.drain()}
  if(changed||restarted||scanTimer>.4){
   section("Surfaces"){vm.updateSurfaces(planes.surfaces(),planes.running)};scanTimer=0.0
   if(changed||restarted)section("PlaneVisualDiff"){updatePlaneVisuals()}
   if(planes.error!=null)vm.notify("scan_unavailable")
  }
  // One changed/new outline per frame. Pose updates never enter the native mesh lifecycle.
  section("PlaneGeometry"){buildNextPlaneVisual()}
  val state=vm.state.value
  if(lastTracking&&!state.tracking)router.cancel(clearButtons=false);lastTracking=state.tracking
  if(lastPanel!=state.currentPanel){router.cancel(clearButtons=false);lastPanel=state.currentPanel}
  if(engineIdentity!==vm.engine){router.cancel(clearButtons=false);engineIdentity=vm.engine;portalRevision=-1}
  section("Panels"){fitPanels();panelEntities.forEach{(id,e)->e.enabled=id==state.currentPanel}}
  val inputs=section("InputSamples"){inputFrames()}
  section("InputRoute"){inputs.forEach{router.process(it,dt)};router.finishFrame(inputs.map{it.id}.toSet())}
  section("RayVisual"){
  val ray=inputFocus.choose(inputs,router.capturedInputId)?.ray
  val panelHit=ray?.let(::panelAt)
  val hit=if(panelHit!=null)null else ray?.let{r->vm.engine.surfaces.asSequence().mapNotNull{it.hit(r)}.minByOrNull{it.distance}}
  cursor.enabled=hit!=null&&state.tracking
  if(hit!=null)cursor.components[TransformComponent::class.java]?.setPosition((hit.point+hit.surface.normal*.01).sdk())
  beam.enabled=ray!=null
  if(ray!=null){val dist=panelHit?.distance?:hit?.distance?:2.0;beam.components[TransformComponent::class.java]?.apply{setPosition((ray.origin+ray.direction*(dist*.5)).sdk());setQuaternion(facing(ray.direction));setScaleVector(Vector3(1f,1f,dist.toFloat()))}}
  }
  val events=section("Physics"){vm.advance(dt)}
  for(event in events)when(event){
   is SimulationEvent.Transferred->{val p=vm.engine.payload?.position;sound.play("audio/sfx/portal-close.wav",p,state.settings);sound.play("audio/sfx/object-flight.wav",p,state.settings)}
   is SimulationEvent.Collided->{sound.play("audio/sfx/plane-impact.wav",event.point,state.settings);sound.play("audio/sfx/portal-collapse.wav",event.point,state.settings);portals.keys.forEach{sound.stop("portal_$it")}}
   SimulationEvent.Stable->sound.play("audio/sfx/success.wav",vm.engine.payload?.position,state.settings)
   SimulationEvent.Failed->sound.play("audio/sfx/failure.wav",vm.engine.payload?.position,state.settings)
  }
  section("PortalSync"){if(portalRevision!=vm.revision||portals.keys!=vm.engine.portals.map{it.id}.toSet())syncPortals()}
  section("PortalAnimation"){
  portals.values.forEach{visual->
   val surface=vm.engine.surfaces.firstOrNull{it.id==visual.portal.surfaceId}
   visual.root.enabled=surface!=null
   if(surface!=null){visual.updateSurface(surface);visual.update(dt,vm.engine.collapseProgress);sound.move("portal_${visual.portal.id}",surface.world(visual.portal.center))}
  }
  }
  section("PayloadVisual"){
  models.forEach{(kind,e)->e.enabled=vm.engine.payload?.kind==kind}
  vm.engine.payload?.let{p->models[p.kind]?.components?.get(TransformComponent::class.java)?.apply{setPosition(p.position.sdk());if(p.velocity.length()>.05)setQuaternion(facing(p.velocity))}}
  }
  section("Narration"){
  vm.takeNarration()?.let{key->
   sound.stop("narration");sound.play("audio/voice/${state.settings.locale.voiceFolder}/$key.wav",null,state.settings,key="narration",channel="voice")
  }
  }
  section("AudioTick"){sound.tick(state.settings)}
  val callbackNanos=java.lang.System.nanoTime()-frameStart
  diagnosticWindowFrames++;diagnosticTotalNanos+=callbackNanos;diagnosticMaxNanos=maxOf(diagnosticMaxNanos,callbackNanos)
  if(callbackNanos>11_111_111)diagnosticSlowFrames++
 }
 private fun syncPortals() {
  val current=vm.engine.portals.associateBy{it.id}
  portals.keys.toList().forEach{id->
   if(current[id]!=portals[id]?.portal){portals.remove(id)?.close();sound.stop("portal_$id")}
  }
  for(p in current.values){
   if(p.id in portals)continue
   val surface=vm.engine.surfaces.firstOrNull{it.id==p.surfaceId}?:continue
   val visual=PortalVisual(p,surface,geometry);portals[p.id]=visual;root.addChild(visual.root)
   sound.play("audio/sfx/suction-loop.wav",surface.world(p.center),vm.state.value.settings,loop=true,key="portal_${p.id}")
   sound.play("audio/sfx/portal-close.wav",surface.world(p.center),vm.state.value.settings)
  };portalRevision=vm.revision
 }
 private fun updatePlaneVisuals(){
  planeVisualPlan.update(planes.surfaces()).forEach{id->planeVisuals.remove(id)?.close()}
  planeVisuals.forEach{(id,visual)->planeVisualPlan.surface(id)?.let(visual::updatePose)}
 }
 private fun buildNextPlaneVisual(){
  val surface=planeVisualPlan.nextBuild()?:return
  val visual=PlaneOutlineVisual(surface,geometry)
  val entity=visual.entity?:return
  root.addChild(entity);planeVisuals.put(surface.id,visual)?.close();planeVisualPlan.markBuilt(surface);diagnosticPlaneBuilds++
 }
 fun updatePanelMetrics(metrics: Map<GamePanel,V2>){
  if(metrics!=panelBaseSizes){panelBaseSizes=metrics;fittedPanels.clear()}
 }
 private fun fitPanels(){
  for((id,e) in panelEntities){
   if(id in fittedPanels)continue
   // AttachmentPanel has no mesh bounds. Use the public runtime dp-to-meter converter,
   // including its current worldScale; never infer a fixed px/meter ratio.
   val base=panelBaseSizes[id]?:continue
   if(base.x<=0.0||base.y<=0.0)continue
   val desired=.86
   val scale=desired/base.x
   e.components[TransformComponent::class.java]?.setScaleVector(Vector3(scale.toFloat()))
   panelHalfSizes[id]=V2(desired*.5,base.y*scale*.5);fittedPanels+=id
   Log.i("SpaceBend","Panel $id: runtime source ${base.x} x ${base.y} m, fitted width $desired m")
  }
 }
 override fun panelAt(ray: Ray): PanelHit? = panelEntities.mapNotNull{(id,e)->
  if(id!=vm.state.value.currentPanel)return@mapNotNull null
  val position=e.components[TransformComponent::class.java]?.position?.game()?:return@mapNotNull null
  if(abs(ray.direction.z)<1e-6)return@mapNotNull null
  val t=(position.z-ray.origin.z)/ray.direction.z;if(t<0)return@mapNotNull null
  val q=ray.origin+ray.direction*t
  val half=panelHalfSizes[id]?:return@mapNotNull null
  if(abs(q.x-position.x)>half.x||abs(q.y-position.y)>half.y)return@mapNotNull null
  PanelHit(id,q,t,q.y>position.y+half.y*.65,position)
 }.minByOrNull{it.distance}
 override fun movePanel(panel: GamePanel,point: V3){
  if(panel!=vm.state.value.currentPanel)return
  workspacePosition=point
  panelEntities.values.forEach{it.components[TransformComponent::class.java]?.setPosition(point.sdk())}
 }
 override fun pulse(hand: HandController,strong: Boolean){
  if(!vm.state.value.settings.haptics||hand==HandController.Unknown)return
  haptic.feedback(if(strong)HandControllerHapticType(210,130,70)else HandControllerHapticType(22,150,55),hand)
 }
 override fun stroke(points: List<V3>){
  if(points==strokeKey)return;strokeKey=points
  section("StrokeGeometry"){
  strokeVisual?.let{it.first.destroy();it.second.close()};strokeVisual=null
  geometry.trace(points,.003,false,geometry.ice)?.let{root.addChild(it.first);strokeVisual=it}
  }
 }
 override fun drawSound(active: Boolean,point: V3?){
  if(active&&point!=null){sound.play("audio/sfx/draw-loop.wav",point,vm.state.value.settings,loop=true,key="draw");sound.move("draw",point)}else sound.stop("draw")
 }
 private fun inputFrames(): List<InputFrame> {
  val result=mutableListOf<InputFrame>();val data=controllers.latestData;val action=actions
   for(right in listOf(true,false)){
    val pose=if(right)data.right else data.left;val a=if(right)action?.right else action?.left
    // Pose tracking must not wait for an action callback (idle controllers still move).
    if(pose!=null&&pose.position!=Vector3.ZERO){
     val pos=root.convertPositionFrom(pose.position,null).game();val rotation=root.convertRotationFrom(pose.rotation,null)
     result+=InputFrame(if(right)"right_controller" else "left_controller",Ray(pos,rotate(rotation,V3(0.0,0.0,-1.0)).normalized()),a?.let{it.triggerPressed||it.triggerValue>.65f}?:false,a?.let{it.gripPressed||it.gripValue>.65f}?:false,a?.let{it.bButtonPressed||it.yButtonPressed}?:false,a?.thumbstickPressed?:false,a?.thumbstickValue?.x?:0f,if(right)HandController.Right else HandController.Left,aimActive=a?.let{it.triggerTouched||it.thumbstickTouched||it.aButtonTouched||it.bButtonTouched||it.xButtonTouched||it.yButtonTouched}?:false)
    }
   }
  val handData=hands.latestData
  for(right in listOf(true,false)){
   val pinch=if(right)handData.rightPinch else handData.leftPinch;val aim=if(right)handData.rightAim else handData.leftAim
   if(pinch?.isReady==true&&aim?.isReady==true){
    val side=if(right)HandController.Right else HandController.Left
    // Prefer a currently pinching hand over an idle retained controller pose for that side.
    if(pinch.pinchValue>.15f||result.none{it.hand==side}){
     result.removeAll{it.hand==side&&!it.trigger&&!it.grip}
     val pos=root.convertPositionFrom(aim.aimPose.position,null).game();val rot=root.convertRotationFrom(aim.aimPose.rotation,null)
     result+=InputFrame(if(right)"right_hand" else "left_hand",Ray(pos,rotate(rot,V3(0.0,0.0,-1.0)).normalized()),pinch.pinchValue>.8f,hand=HandController.Unknown,trackedHand=true)
    }
   }
  };return result
 }
 fun close(){
  if(closed)return;pause();closed=true;router.cancel();portals.values.forEach{it.close()};portals.clear();sound.close()
  planeVisuals.values.forEach{it.close()};planeVisuals.clear();planeVisualPlan.clear();strokeVisual?.let{it.first.destroy();it.second.close()};root.destroy();geometry.close()
 }
}
