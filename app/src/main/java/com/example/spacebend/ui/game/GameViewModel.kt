package com.example.spacebend.ui.game

import androidx.lifecycle.ViewModel
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.*
import com.example.spacebend.ui.navigation.GamePanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(private val session: GameSessionUseCase): ViewModel() {
 private val initial=session.settings()
 private val mutable=MutableStateFlow(GameUiState(settings=initial,strings=session.strings(initial.locale),records=session.records()))
 val state=mutable.asStateFlow()
 var engine=PuzzleEngine(GameMode.CHALLENGE,Missions.all.first());private set
 var revision=0L;private set
 var narration: String?="v_welcome";private set
 private var lastPublish=0.0
 private var scanned=false
 private val primaryPanels=setOf(GamePanel.MENU,GamePanel.SCAN,GamePanel.LEVELS,GamePanel.RESULT)
 fun takeNarration(): String?=narration.also{narration=null}
 private fun update(block: (GameUiState)->GameUiState){mutable.value=block(mutable.value)}
 fun sceneReady(){update{it.copy(sceneReady=true)}}
 fun notify(key: String){update{it.copy(statusKey=key)}}
 fun onEvent(event: GameEvent) {
  val s=mutable.value
  when(event){
   is GameEvent.Show -> update { it.copy(panels=(if(event.panel in primaryPanels)it.panels-primaryPanels else it.panels)+event.panel) }
   is GameEvent.Close -> update { it.copy(panels=it.panels-event.panel) }
   is GameEvent.ChooseMission -> if(event.id in 1..12&&Missions.unlocked(event.id,s.records)){
    val surfaces=engine.surfaces;engine=PuzzleEngine(GameMode.CHALLENGE,Missions.all[event.id-1]);engine.updateSurfaces(surfaces);revision++
    update{it.copy(mode=GameMode.CHALLENGE,missionId=event.id,selected=Missions.all[event.id-1].allowed.first(),phase=engine.phase,stars=0,portalCount=0,elapsed=0.0,loops=0,panels=setOf(if(scanned)GamePanel.HUD else GamePanel.SCAN),statusKey="draw_hint")}
    narration=if(scanned)"v_draw" else "v_scan"
   }
   GameEvent.Sandbox -> {
    val surfaces=engine.surfaces;engine=PuzzleEngine(GameMode.SANDBOX,Missions.all.first());engine.updateSurfaces(surfaces);revision++
    update{it.copy(mode=GameMode.SANDBOX,phase=engine.phase,stars=0,portalCount=0,elapsed=0.0,loops=0,panels=setOf(if(scanned)GamePanel.HUD else GamePanel.SCAN),statusKey="sandbox_hint")}
   }
   GameEvent.Launch -> if(scanned&&s.sceneReady&&engine.launch(s.selected)){
    update{it.copy(panels=(it.panels-GamePanel.PICKER)+GamePanel.HUD,statusKey="running")};narration="v_launch"
   }else notify(if(!scanned)"scan_hint" else "launch_needs_fit")
   GameEvent.Reset -> {engine.reset();revision++;update{it.copy(phase=engine.phase,stars=0,portalCount=0,elapsed=0.0,loops=0,panels=setOf(GamePanel.HUD),statusKey="draw_hint")};narration="v_draw"}
   GameEvent.Scan -> {scanned=false;update{it.copy(scanning=true,panels=(it.panels-primaryPanels)+GamePanel.SCAN,statusKey="scan_hint")};narration="v_scan"}
   GameEvent.ContinueScan -> if(s.planeCount>0&&s.scanAvailable){
    if(engine.portals.any{p->engine.surfaces.none{it.id==p.surfaceId}}){engine.reset();revision++}
    scanned=true;update{it.copy(tracking=true)};update{it.copy(scanning=false,panels=if(it.settings.tutorialDone)(it.panels-GamePanel.SCAN)+GamePanel.MENU else setOf(GamePanel.TUTORIAL,GamePanel.HUD),statusKey="draw_hint")};narration="v_ready"
   }
   GameEvent.TutorialNext -> {
    val step=s.tutorialStep+1
    if(step>=5){settings(s.settings.copy(tutorialDone=true));update{it.copy(tutorialStep=0,panels=(it.panels-GamePanel.TUTORIAL)+GamePanel.MENU)}}
    else {update{it.copy(tutorialStep=step)};narration=listOf("v_scan","v_draw","v_select","v_launch","v_loop")[step]}
   }
   is GameEvent.Select -> if(s.mode==GameMode.SANDBOX||event.kind in engine.mission.allowed)update{it.copy(selected=event.kind)}
   is GameEvent.Cycle -> {
    val allowed=PayloadKind.entries.filter{s.mode==GameMode.SANDBOX||it in engine.mission.allowed};val i=allowed.indexOf(s.selected).coerceAtLeast(0)
    update{it.copy(selected=allowed[Math.floorMod(i+event.direction,allowed.size)])}
   }
   is GameEvent.Locale -> {settings(s.settings.copy(locale=event.locale));update{it.copy(strings=session.strings(event.locale))}}
   is GameEvent.Volume -> settings(when(event.channel){"music"->s.settings.copy(music=event.value.coerceIn(0f,1f));"effects"->s.settings.copy(effects=event.value.coerceIn(0f,1f));else->s.settings.copy(voice=event.value.coerceIn(0f,1f))})
   is GameEvent.Haptics -> settings(s.settings.copy(haptics=event.enabled))
  }
  publish()
 }
 private fun settings(value: GameSettings){session.saveSettings(value);update{it.copy(settings=value)}}
 fun updateSurfaces(surfaces: List<Surface>,running: Boolean) {
  engine.updateSurfaces(surfaces)
  val available=running&&surfaces.isNotEmpty()
  val valid=available&&engine.portals.all{p->surfaces.any{it.id==p.surfaceId}}
  val was=mutable.value.tracking
  update{it.copy(planeCount=surfaces.size,tracking=valid,scanAvailable=available)}
  if(was&&!valid){update{it.copy(scanning=true,panels=(it.panels-primaryPanels)+GamePanel.SCAN,statusKey="plane_lost")};narration="v_lost"}
 }
 fun addPortal(surfaceId: String,points: List<V2>): EditResult {
  val result=engine.addPortal(surfaceId,points)
  if(result==EditResult.OK){revision++;notify("portal_ready");narration="v_closed"}else notify(when(result){EditResult.LIMIT->"portal_limit";EditResult.OVERLAP->"portal_overlap";EditResult.OUTSIDE_SURFACE->"outside_surface";else->"invalid_shape"})
  publish();return result
 }
 fun movePortal(id: Long,surface: String,delta: V2): Boolean {val ok=engine.movePortal(id,surface,delta)==EditResult.OK;if(ok)revision++;return ok}
 fun deletePortal(id: Long): Boolean {val ok=engine.deletePortal(id);if(ok){revision++;publish()};return ok}
 fun deleteProgress(value: Float){update{it.copy(deleteProgress=value)}}
 fun advance(dt: Double): List<SimulationEvent> {
  val events=engine.advance(dt,scanned&&mutable.value.tracking)
  if(events.isNotEmpty()){
   for(e in events)when(e){
    SimulationEvent.Stable -> {
     val stars=Scoring.grade(engine.mission,true,engine.portals.size,engine.resultSeconds?:engine.elapsedSeconds,engine.collisions)
     val records=session.record(engine.mission.id,stars,engine.resultSeconds?:engine.elapsedSeconds)
     update{it.copy(stars=stars,records=records,panels=(it.panels-primaryPanels)+GamePanel.RESULT,statusKey="success")};narration="v_loop"
    }
    SimulationEvent.Failed -> {update{it.copy(panels=(it.panels-primaryPanels)+GamePanel.RESULT,statusKey="failure")};narration="v_fail"}
    is SimulationEvent.Collided -> {notify(if(engine.mode==GameMode.SANDBOX)"sandbox_collision" else "failure");revision++}
    is SimulationEvent.Transferred -> {}
   }
   publish()
  }
  lastPublish+=dt;if(lastPublish>.1){lastPublish=0.0;publish()}
  return events
 }
 private fun publish(){update{it.copy(phase=engine.phase,portalCount=engine.portals.size,elapsed=engine.resultSeconds?:engine.elapsedSeconds,loops=engine.payload?.loops?:0)}}
}
