package com.example.spacebend.domain
import com.example.spacebend.data.repository.GameRepository
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.GameSessionUseCase
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import org.junit.Assert.*
import org.junit.Test

private class MemoryGameRepository: GameRepository {
 var current=GameSettings();val scores=mutableMapOf<Int,Record>()
 override fun settings()=current
 override fun saveSettings(settings: GameSettings){current=settings}
 override fun records()=scores.toMap()
 override fun saveRecord(id: Int,record: Record){scores[id]=record}
 override fun strings(locale: GameLocale)=mapOf("app" to locale.textFolder)
}
class GameSessionUseCaseTest {
 @Test fun savingNewBestTimeNeverLosesHigherStars(){val r=MemoryGameRepository();val u=GameSessionUseCase(r);u.record(1,3,60.0);u.record(1,1,40.0);assertEquals(Record(3,40.0),u.records()[1])}
 @Test fun storingLocaleKeepsTraditionalTextWithChineseNarrationAlias(){val u=GameSessionUseCase(MemoryGameRepository());u.saveSettings(GameSettings(locale=GameLocale.TRADITIONAL));assertEquals("zh-Hant",u.strings(u.settings().locale)["app"]);assertEquals("zh-Hans",u.settings().locale.voiceFolder)}
}
class GameViewModelTest {
 private fun vm()=GameViewModel(GameSessionUseCase(MemoryGameRepository()))
 @Test fun initialStateRequiresRealScan(){val v=vm();assertEquals(setOf(GamePanel.SCAN),v.state.value.panels);assertFalse(v.state.value.tracking)}
 @Test fun choosingLockedMissionDoesNotChangeCurrentExperiment(){val v=vm();v.onEvent(GameEvent.ChooseMission(12));assertEquals(1,v.state.value.missionId)}
 @Test fun noDetectedPlanesCannotAdvanceScan(){val v=vm();v.onEvent(GameEvent.ContinueScan);assertTrue(GamePanel.SCAN in v.state.value.panels)}
 @Test fun traditionalLocaleChangesTextOnlyAndPersistsSettings(){val v=vm();v.onEvent(GameEvent.Locale(GameLocale.TRADITIONAL));assertEquals("zh-Hant",v.state.value.strings["app"]);assertEquals("zh-Hans",v.state.value.settings.locale.voiceFolder)}
 @Test fun sandboxAllowsEveryPayloadWhileFirstMissionRestrictsSelection(){val v=vm();v.onEvent(GameEvent.Select(PayloadKind.CAPSULE));assertEquals(PayloadKind.CORE,v.state.value.selected);v.onEvent(GameEvent.Sandbox);v.onEvent(GameEvent.Select(PayloadKind.CAPSULE));assertEquals(PayloadKind.CAPSULE,v.state.value.selected)}
 @Test fun primaryNavigationReplacesCoplanarPanel() {
  val v=vm();v.onEvent(GameEvent.Show(GamePanel.MENU));v.onEvent(GameEvent.Show(GamePanel.LEVELS))
  assertTrue(GamePanel.LEVELS in v.state.value.panels);assertFalse(GamePanel.MENU in v.state.value.panels)
 }
 @Test fun rescanRecoversOrphanedAnchorsByClearingOldExperiment() {
  val v=vm();val b=listOf(V2(-3.0,-3.0),V2(3.0,-3.0),V2(3.0,3.0),V2(-3.0,3.0));val a=Surface("A",V3.ZERO,V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),b)
  v.sceneReady();v.updateSurfaces(listOf(a),true);v.onEvent(GameEvent.ContinueScan)
  v.addPortal("A",(0 until 24).map{val r=it*Math.PI/12;V2(kotlin.math.cos(r),kotlin.math.sin(r))})
  v.updateSurfaces(emptyList(),true);v.updateSurfaces(listOf(a.copy(id="B")),true)
  v.onEvent(GameEvent.ContinueScan);assertTrue(v.state.value.tracking);assertTrue(v.engine.portals.isEmpty())
 }
}
