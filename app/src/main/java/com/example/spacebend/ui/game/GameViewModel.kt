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
 private var recovering=false
 private var scanReturn=GamePanel.HUD
 private var settingsReturn=GamePanel.MENU
 private var resumePanel=GamePanel.HUD
 fun takeNarration(): String?=narration.also{narration=null}
 private fun update(block: (GameUiState)->GameUiState){mutable.value=block(mutable.value)}
 fun sceneReady(){update{it.copy(sceneReady=true)};publish()}
 fun notify(key: String){update{it.copy(statusKey=key)}}
 private fun navigate(panel: GamePanel){update{it.copy(currentPanel=panel,deleteProgress=0f)}}
 private fun validWorkPanel(panel: GamePanel): GamePanel = when {
  engine.mode==GameMode.CHALLENGE&&engine.phase in setOf(ExperimentPhase.STABLE,ExperimentPhase.FAILED)->GamePanel.RESULT
  panel==GamePanel.PICKER&&engine.phase==ExperimentPhase.PLANNING&&engine.portals.size>=engine.requiredPortals->GamePanel.PICKER
  panel==GamePanel.SETTINGS->GamePanel.SETTINGS
  else->GamePanel.HUD
 }
 private fun resume() {
  if(!mutable.value.experimentActive){navigate(GamePanel.MENU);return}
  if(scanned&&mutable.value.tracking){navigate(validWorkPanel(resumePanel))}
  else {scanReturn=validWorkPanel(resumePanel);update{it.copy(currentPanel=GamePanel.SCAN,scanning=true,statusKey="scan_hint")}}
 }
 private fun begin(request: NewExperiment) {
  val surfaces=engine.surfaces;engine=PuzzleEngine(request.mode,Missions.all[request.missionId-1]);engine.updateSurfaces(surfaces);revision++
  recovering=false;scanReturn=GamePanel.HUD;resumePanel=GamePanel.HUD
  val ready=scanned&&mutable.value.tracking&&mutable.value.sceneReady
  update{it.copy(mode=request.mode,missionId=request.missionId,selected=engine.mission.allowed.first(),phase=engine.phase,stars=0,portalCount=0,elapsed=0.0,loops=0,experimentActive=true,tutorialActive=request.tutorial||!it.settings.tutorialDone,tutorialStep=if(ready)1 else 0,pendingExperiment=null,currentPanel=if(ready)GamePanel.HUD else GamePanel.SCAN,scanning=!ready,statusKey=if(ready)"flow_draw_first" else "scan_hint")}
  narration=if(ready)"v_draw" else "v_scan"
 }
 private fun requestExperiment(request: NewExperiment) {
  val s=mutable.value
  val unfinished=s.experimentActive&&engine.portals.isNotEmpty()&&engine.phase !in setOf(ExperimentPhase.STABLE,ExperimentPhase.FAILED)
  if(unfinished){
   if(s.currentPanel !in setOf(GamePanel.MENU,GamePanel.LEVELS))resumePanel=if(s.currentPanel==GamePanel.SCAN)scanReturn else s.currentPanel
   update{it.copy(currentPanel=GamePanel.MENU,pendingExperiment=request)}
  }else begin(request)
 }
 private fun show(panel: GamePanel) {
  val s=mutable.value
  when(panel){
   GamePanel.PICKER->if(s.canChoose){navigate(panel);narration="v_select"}else if(s.currentPanel==GamePanel.HUD)notify("flow_need_more_portals")
   GamePanel.SETTINGS->{if(s.currentPanel!=panel){settingsReturn=s.currentPanel;navigate(panel)}}
   GamePanel.TUTORIAL->requestExperiment(NewExperiment(GameMode.SANDBOX,tutorial=true))
   GamePanel.MENU->{if(s.currentPanel !in setOf(GamePanel.MENU,GamePanel.LEVELS,GamePanel.SETTINGS))resumePanel=if(s.currentPanel==GamePanel.SCAN)scanReturn else s.currentPanel;navigate(panel)}
   GamePanel.SCAN->scan()
   GamePanel.HUD->resume()
   GamePanel.RESULT->if(s.mode==GameMode.CHALLENGE&&s.phase in setOf(ExperimentPhase.STABLE,ExperimentPhase.FAILED))navigate(panel)
   GamePanel.LEVELS->navigate(panel)
  }
 }
 private fun scan() {
  val s=mutable.value
  if(s.currentPanel!=GamePanel.SCAN)scanReturn=if(s.experimentActive)validWorkPanel(s.currentPanel)else GamePanel.MENU
  scanned=false;recovering=false
  update{it.copy(scanning=true,currentPanel=GamePanel.SCAN,statusKey="scan_hint",scanRevision=it.scanRevision+1,planeCount=0,scanAvailable=false,tracking=false)};narration="v_scan"
 }
 private fun continueScan() {
  if(mutable.value.currentPanel!=GamePanel.SCAN||!mutable.value.canContinueScan)return
  val lost=engine.portals.any{p->engine.surfaces.none{it.id==p.surfaceId}}
  if(lost){engine.reset();revision++;scanReturn=GamePanel.HUD}
  scanned=true;recovering=false
  update{it.copy(tracking=true,scanning=false,invalidAnchorCount=0,currentPanel=if(it.experimentActive)validWorkPanel(scanReturn)else GamePanel.MENU,statusKey=if(lost)"flow_layout_redraw" else if(engine.phase!=ExperimentPhase.PLANNING||scanReturn==GamePanel.PICKER||scanReturn==GamePanel.SETTINGS)"flow_tracking_restored" else if(engine.portals.isNotEmpty())"flow_draw_ready" else "flow_draw_first",tutorialStep=if(it.tutorialActive)1 else it.tutorialStep)}
  narration="v_ready"
 }
 fun onEvent(event: GameEvent) {
  val s=mutable.value
  when(event){
   is GameEvent.Show->show(event.panel)
   is GameEvent.Close->if(s.currentPanel==event.panel)when(event.panel){
    GamePanel.SETTINGS->{if(settingsReturn==GamePanel.MENU||settingsReturn==GamePanel.LEVELS)navigate(settingsReturn)else{resumePanel=settingsReturn;resume()}}
    GamePanel.PICKER->navigate(GamePanel.HUD)
    GamePanel.SCAN->{resumePanel=scanReturn;navigate(GamePanel.MENU)}
    GamePanel.LEVELS,GamePanel.RESULT->navigate(GamePanel.MENU)
    GamePanel.MENU->resume()
    else->show(GamePanel.MENU)
   }
   is GameEvent.ChooseMission->if(event.id in 1..12&&Missions.unlocked(event.id,s.records))requestExperiment(NewExperiment(GameMode.CHALLENGE,event.id))
   GameEvent.Sandbox->requestExperiment(NewExperiment(GameMode.SANDBOX))
   GameEvent.Resume->{update{it.copy(pendingExperiment=null)};resume()}
   GameEvent.ConfirmNewExperiment->s.pendingExperiment?.let(::begin)
   GameEvent.CancelNewExperiment->{update{it.copy(pendingExperiment=null)};resume()}
   GameEvent.Launch->if(s.canLaunch&&scanned&&engine.launch(s.selected)){
    update{it.copy(currentPanel=GamePanel.HUD,statusKey="running",tutorialStep=if(it.tutorialActive)3 else it.tutorialStep)};narration="v_launch"
   }else notify(if(!scanned||!s.tracking)"scan_hint" else "launch_needs_fit")
   GameEvent.Reset->if(s.experimentActive){engine.reset();revision++;resumePanel=GamePanel.HUD;update{it.copy(currentPanel=if(scanned&&it.tracking)GamePanel.HUD else GamePanel.SCAN,statusKey="flow_draw_first",stars=0,tutorialStep=if(it.tutorialActive)1 else it.tutorialStep)}}
   GameEvent.StopAndEdit->if(s.currentPanel==GamePanel.HUD&&engine.phase==ExperimentPhase.RUNNING&&engine.stopForEditing()){revision++;update{it.copy(currentPanel=GamePanel.HUD,statusKey="flow_stopped")};narration="v_draw"}
   GameEvent.Scan->scan()
   GameEvent.ContinueScan->continueScan()
   GameEvent.TutorialNext->when(s.currentPanel){GamePanel.SCAN->continueScan();GamePanel.HUD->if(s.canChoose)show(GamePanel.PICKER);GamePanel.PICKER->if(s.canLaunch)onEvent(GameEvent.Launch);else->{}}
   is GameEvent.Select->if(s.currentPanel==GamePanel.PICKER&&s.phase==ExperimentPhase.PLANNING&&(s.mode==GameMode.SANDBOX||event.kind in engine.mission.allowed))update{it.copy(selected=event.kind)}
   is GameEvent.Cycle->if(s.currentPanel==GamePanel.PICKER){
    val allowed=PayloadKind.entries.filter{s.mode==GameMode.SANDBOX||it in engine.mission.allowed};val i=allowed.indexOf(s.selected).coerceAtLeast(0)
    update{it.copy(selected=allowed[Math.floorMod(i+event.direction,allowed.size)])}
   }
   is GameEvent.Locale->{settings(s.settings.copy(locale=event.locale));update{it.copy(strings=session.strings(event.locale))}}
   is GameEvent.Volume->settings(when(event.channel){"music"->s.settings.copy(music=event.value.coerceIn(0f,1f));"effects"->s.settings.copy(effects=event.value.coerceIn(0f,1f));else->s.settings.copy(voice=event.value.coerceIn(0f,1f))})
   is GameEvent.Haptics->settings(s.settings.copy(haptics=event.enabled))
  }
  publish()
 }
 private fun settings(value: GameSettings){session.saveSettings(value);update{it.copy(settings=value)}}
 fun updateSurfaces(surfaces: List<Surface>,running: Boolean) {
  engine.updateSurfaces(surfaces)
  val s=mutable.value;val available=running&&surfaces.isNotEmpty()
  val lost=engine.portals.count{p->surfaces.none{it.id==p.surfaceId}}
  val valid=available&&lost==0
  update{it.copy(planeCount=surfaces.size,tracking=valid,scanAvailable=available,invalidAnchorCount=lost)}
  if(scanned&&!valid&&s.experimentActive){
   scanned=false;recovering=true
   scanReturn=if(s.currentPanel==GamePanel.SCAN)scanReturn else s.currentPanel
   if(s.currentPanel !in setOf(GamePanel.MENU,GamePanel.LEVELS))update{it.copy(scanning=true,currentPanel=GamePanel.SCAN,statusKey="plane_lost")}
   narration="v_lost"
  }else if(recovering&&valid&&mutable.value.sceneReady){
   scanned=true;recovering=false
   update{it.copy(scanning=false,currentPanel=if(it.currentPanel==GamePanel.SCAN)validWorkPanel(scanReturn)else it.currentPanel,statusKey="flow_tracking_restored")}
  }
  publish()
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
  val s=mutable.value
  val events=engine.advance(dt,scanned&&s.tracking&&s.experimentActive&&(s.currentPanel==GamePanel.HUD||s.currentPanel==GamePanel.PICKER||s.currentPanel==GamePanel.RESULT))
  if(s.phase==ExperimentPhase.FALLING&&engine.phase==ExperimentPhase.PLANNING)update{it.copy(currentPanel=GamePanel.HUD,statusKey="flow_collision_redraw")}
  for(e in events)when(e){
   SimulationEvent.Stable->{
    val stars=Scoring.grade(engine.mission,true,engine.portals.size,engine.resultSeconds?:engine.elapsedSeconds,engine.collisions)
    val records=session.record(engine.mission.id,stars,engine.resultSeconds?:engine.elapsedSeconds)
    update{it.copy(stars=stars,records=records,currentPanel=GamePanel.RESULT,statusKey="success")};narration="v_loop"
   }
   SimulationEvent.Failed->{update{it.copy(currentPanel=GamePanel.RESULT,statusKey="failure")};narration="v_fail"}
   is SimulationEvent.Collided->{notify(if(engine.mode==GameMode.SANDBOX)"flow_collapsing" else "failure");revision++}
   is SimulationEvent.Transferred->{}
  }
  if(s.tutorialActive){
   val p=engine.payload
   if((p?.loops?:0)>=1){settings(mutable.value.settings.copy(tutorialDone=true));update{it.copy(tutorialActive=false,statusKey="flow_tutorial_complete")}}
   else if((p?.traversals?:0)>0)update{it.copy(tutorialStep=4)}
  }
  if(events.isNotEmpty())publish()
  lastPublish+=dt;if(lastPublish>.1){lastPublish=0.0;publish()}
  return events
 }
 private fun publish(){update{it.copy(phase=engine.phase,portalCount=engine.portals.size,elapsed=engine.resultSeconds?:engine.elapsedSeconds,loops=engine.payload?.loops?:0,requiredPortals=engine.requiredPortals,launchIssue=engine.launchIssue(it.selected),tutorialStep=if(it.tutorialActive&&engine.phase==ExperimentPhase.PLANNING){if(it.scanning)0 else if(it.currentPanel==GamePanel.PICKER)2 else 1}else it.tutorialStep)}}
}
