package com.example.spacebend.domain

import com.example.spacebend.data.repository.GameRepository
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.GameSessionUseCase
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

private class FlowRepository: GameRepository {
 var value=GameSettings(tutorialDone=true)
 override fun settings()=value
 override fun saveSettings(settings: GameSettings){value=settings}
 override fun records()=emptyMap<Int,Record>()
 override fun saveRecord(id: Int,record: Record){}
 override fun strings(locale: GameLocale)=emptyMap<String,String>()
}
class ExperienceFlowTest {
 private val floor=Surface("floor",V3.ZERO,V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),listOf(V2(-8.0,-8.0),V2(8.0,-8.0),V2(8.0,8.0),V2(-8.0,8.0)))
 private fun vm()=GameViewModel(GameSessionUseCase(FlowRepository()))
 private fun ready(v: GameViewModel){v.sceneReady();v.updateSurfaces(listOf(floor),true);v.onEvent(GameEvent.Sandbox);v.onEvent(GameEvent.ContinueScan)}
 private fun portal(v: GameViewModel,x: Double){v.addPortal(floor.id,(0 until 32).map{val a=it*PI/16;V2(x+.7*cos(a),.7*sin(a))})}
 @Test fun homeComesBeforeModeSpecificScan() {assertEquals(setOf(GamePanel.MENU),vm().state.value.panels)}
 @Test fun scanContinuesDirectlyIntoChosenSandboxWithoutAnotherMenu() {val v=vm();ready(v);assertEquals(GameMode.SANDBOX,v.state.value.mode);assertEquals(setOf(GamePanel.HUD),v.state.value.panels)}
 @Test fun selectingBeforeTwoPortalsIsGuardedAndDoesNotOpenPicker() {val v=vm();ready(v);portal(v,-1.0);v.onEvent(GameEvent.Show(GamePanel.PICKER));assertEquals(setOf(GamePanel.HUD),v.state.value.panels)}
 @Test fun pickerReplacesDrawingPanelAndBackPreservesLayout() {
  val v=vm();ready(v);portal(v,-1.0);portal(v,1.0);val layout=v.engine.portals
  v.onEvent(GameEvent.Show(GamePanel.PICKER));assertEquals(setOf(GamePanel.PICKER),v.state.value.panels)
  v.onEvent(GameEvent.Close(GamePanel.PICKER));assertEquals(setOf(GamePanel.HUD),v.state.value.panels);assertEquals(layout,v.engine.portals)
 }
 @Test fun settingsIsExclusiveAndReturnsToExactPriorStep() {
  val v=vm();ready(v);portal(v,-1.0);portal(v,1.0);v.onEvent(GameEvent.Show(GamePanel.PICKER))
  v.onEvent(GameEvent.Show(GamePanel.SETTINGS));assertEquals(setOf(GamePanel.SETTINGS),v.state.value.panels)
  v.onEvent(GameEvent.Close(GamePanel.SETTINGS));assertEquals(setOf(GamePanel.PICKER),v.state.value.panels)
 }
 @Test fun tutorialCannotAdvanceByClickingContinueBeforeActualActions() {
  val r=FlowRepository().apply{value=GameSettings(tutorialDone=false)};val v=GameViewModel(GameSessionUseCase(r))
  repeat(6){v.onEvent(GameEvent.TutorialNext)}
  assertFalse(v.state.value.settings.tutorialDone);assertEquals(0,v.state.value.tutorialStep)
 }

 private fun prepareLayout(v: GameViewModel){ready(v);portal(v,-1.0);portal(v,1.0)}
 private fun launch(v: GameViewModel){prepareLayout(v);v.onEvent(GameEvent.Show(GamePanel.PICKER));assertTrue(v.state.value.canLaunch);v.onEvent(GameEvent.Launch);assertEquals(ExperimentPhase.RUNNING,v.state.value.phase)}
 @Test fun stopReturnsToDrawingWithLayoutAndElapsedPreserved() {
  val v=vm();launch(v);v.advance(.02);val layout=v.engine.portals;val elapsed=v.engine.elapsedSeconds
  v.onEvent(GameEvent.StopAndEdit);assertEquals(setOf(GamePanel.HUD),v.state.value.panels);assertEquals(ExperimentPhase.PLANNING,v.state.value.phase);assertEquals(layout,v.engine.portals);assertEquals(elapsed,v.engine.elapsedSeconds,1e-9)
 }
 @Test fun settingsPausesRunningExperimentAndResumesExactState() {
  val v=vm();launch(v);val p=v.engine.payload!!.position;val time=v.engine.elapsedSeconds
  v.onEvent(GameEvent.Show(GamePanel.SETTINGS));repeat(20){v.advance(.1)};assertEquals(p,v.engine.payload!!.position);assertEquals(time,v.engine.elapsedSeconds,0.0)
  v.onEvent(GameEvent.StopAndEdit);assertEquals(ExperimentPhase.RUNNING,v.state.value.phase)
  v.onEvent(GameEvent.Close(GamePanel.SETTINGS));assertEquals(setOf(GamePanel.HUD),v.state.value.panels);v.advance(.02);assertTrue(v.engine.elapsedSeconds>time)
 }
 @Test fun homeAndResumeDoNotClearCurrentExperiment() {
  val v=vm();launch(v);val layout=v.engine.portals;val p=v.engine.payload
  v.onEvent(GameEvent.Show(GamePanel.MENU));repeat(10){v.advance(.1)};v.onEvent(GameEvent.Resume)
  assertEquals(setOf(GamePanel.HUD),v.state.value.panels);assertEquals(layout,v.engine.portals);assertSame(p,v.engine.payload)
 }
 @Test fun startingAnotherModeAsksBeforeClearingAndCancelPreservesLayout() {
  val v=vm();prepareLayout(v);val layout=v.engine.portals
  v.onEvent(GameEvent.Show(GamePanel.MENU));v.onEvent(GameEvent.ChooseMission(1))
  assertNotNull(v.state.value.pendingExperiment);assertEquals(layout,v.engine.portals)
  v.onEvent(GameEvent.CancelNewExperiment);assertEquals(setOf(GamePanel.HUD),v.state.value.panels);assertEquals(layout,v.engine.portals)
  v.onEvent(GameEvent.ChooseMission(1));v.onEvent(GameEvent.ConfirmNewExperiment)
  assertEquals(GameMode.CHALLENGE,v.state.value.mode);assertTrue(v.engine.portals.isEmpty());assertNull(v.state.value.pendingExperiment)
 }
 @Test fun confirmedScanIgnoresLaterPlaneUpdatesAndServiceStop() {
  val v=vm();prepareLayout(v);v.onEvent(GameEvent.Show(GamePanel.PICKER));val layout=v.engine.portals
  v.updateSurfaces(emptyList(),false)
  assertEquals(setOf(GamePanel.PICKER),v.state.value.panels);assertTrue(v.state.value.canLaunch)
  v.updateSurfaces(listOf(floor.copy(id="new-room")),true)
  assertEquals(listOf(floor),v.engine.surfaces);assertEquals(layout,v.engine.portals)
 }
 @Test fun invalidAnchorsRequireExplicitRedrawAndDoNotSilentlyEraseLayout() {
  val v=vm();prepareLayout(v);val layout=v.engine.portals
  v.onEvent(GameEvent.Scan)
  v.updateSurfaces(listOf(floor.copy(id="new-room")),true)
  assertEquals(setOf(GamePanel.SCAN),v.state.value.panels);assertEquals(2,v.state.value.invalidAnchorCount);assertEquals(layout,v.engine.portals)
  v.onEvent(GameEvent.ContinueScan);assertEquals(setOf(GamePanel.HUD),v.state.value.panels);assertTrue(v.engine.portals.isEmpty());assertEquals("flow_layout_redraw",v.state.value.statusKey)
 }
 @Test fun manualScanNeedsFreshPlaneAvailabilityBeforeContinue() {
  val v=vm();prepareLayout(v);v.onEvent(GameEvent.Show(GamePanel.PICKER));v.onEvent(GameEvent.Scan)
  assertFalse(v.state.value.canContinueScan);v.onEvent(GameEvent.ContinueScan);assertEquals(setOf(GamePanel.SCAN),v.state.value.panels)
  v.updateSurfaces(listOf(floor),true);v.onEvent(GameEvent.ContinueScan);assertEquals(setOf(GamePanel.PICKER),v.state.value.panels)
 }
 @Test fun retryingManualScanRetainsItsPickerReturnDestination() {
  val v=vm();prepareLayout(v);v.onEvent(GameEvent.Show(GamePanel.PICKER));v.onEvent(GameEvent.Scan);v.onEvent(GameEvent.Scan);v.updateSurfaces(listOf(floor),true);v.onEvent(GameEvent.ContinueScan)
  assertEquals(setOf(GamePanel.PICKER),v.state.value.panels)
 }
 @Test fun sandboxLoopNeverOpensResultPanelAndCollisionReturnsToDrawing() {
  val v=vm();launch(v);repeat(4000){v.advance(1.0/240)}
  assertTrue(v.state.value.loops>0);assertEquals(setOf(GamePanel.HUD),v.state.value.panels)
  v.engine.payload!!.apply{position=V3(4.0,2.0,0.0);velocity=V3(0.0,-250.0,0.0)}
  v.advance(.05);assertEquals(ExperimentPhase.FALLING,v.state.value.phase);v.onEvent(GameEvent.StopAndEdit);assertEquals(ExperimentPhase.FALLING,v.state.value.phase)
  repeat(12){v.advance(.1)};assertEquals(ExperimentPhase.PLANNING,v.state.value.phase);assertEquals(setOf(GamePanel.HUD),v.state.value.panels);assertTrue(v.engine.portals.isEmpty());assertEquals("flow_collision_redraw",v.state.value.statusKey)
 }
 @Test fun completedRealPracticePersistsTutorialDoneWithoutExtraPanel() {
  val r=FlowRepository().apply{value=GameSettings(tutorialDone=false)};val v=GameViewModel(GameSessionUseCase(r));launch(v)
  assertTrue(v.state.value.tutorialActive);repeat(4000){v.advance(1.0/240)}
  assertTrue(r.value.tutorialDone);assertFalse(v.state.value.tutorialActive);assertEquals(setOf(GamePanel.HUD),v.state.value.panels)
 }
 @Test fun sceneNotReadyCannotBypassScanning() {
  val v=vm();v.onEvent(GameEvent.Sandbox);v.updateSurfaces(listOf(floor),true);v.onEvent(GameEvent.ContinueScan)
  assertEquals(setOf(GamePanel.SCAN),v.state.value.panels);assertFalse(v.state.value.canEdit)
 }

 @Test fun challengeResultAllowsTheAlreadyFallingObjectToLand() {
  val v=vm();ready(v);v.onEvent(GameEvent.ChooseMission(1));portal(v,-1.0);portal(v,1.0)
  v.onEvent(GameEvent.Show(GamePanel.PICKER));v.onEvent(GameEvent.Launch)
  val wall=Surface("high-wall",V3(0.0,5.0,2.0),V3(0.0,1.0,0.0),V3(0.0,0.0,1.0),listOf(V2(-10.0,-10.0),V2(10.0,-10.0),V2(10.0,10.0),V2(-10.0,10.0)))
  v.onEvent(GameEvent.Scan);v.updateSurfaces(listOf(floor,wall),true);v.onEvent(GameEvent.ContinueScan);v.engine.payload!!.apply{position=V3(1.0,8.0,2.0);velocity=V3(-10.0,0.0,0.0)}
  repeat(10){v.advance(.1)};assertEquals(setOf(GamePanel.RESULT),v.state.value.panels);assertEquals(ExperimentPhase.FAILED,v.state.value.phase)
  val height=v.engine.payload!!.position.y;assertTrue(height>1.0)
  v.advance(.1);assertTrue("Falling must continue behind the result panel",v.engine.payload!!.position.y<height)
 }
}
