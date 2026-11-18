package com.example.spacebend.platform.spatial

import com.example.spacebend.data.repository.GameRepository
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.GameSessionUseCase
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import com.pico.spatial.ui.platform.HandController
import org.junit.Assert.*
import org.junit.Test

private class InputRepository: GameRepository {
 override fun settings()=GameSettings()
 override fun saveSettings(settings: GameSettings){}
 override fun records()=emptyMap<Int,Record>()
 override fun saveRecord(id: Int,record: Record){}
 override fun strings(locale: GameLocale)=emptyMap<String,String>()
}
private class InputSceneStub: InputScene {
 var panel: PanelHit?=null
 var strokes=0
 override fun panelAt(ray: Ray)=panel
 override fun movePanel(panel: GamePanel,point: V3){}
 override fun pulse(hand: HandController,strong: Boolean){}
 override fun stroke(points: List<V3>){strokes++}
 override fun drawSound(active: Boolean,point: V3?){}
}
class InputRouterTest {
 private val ray=Ray(V3.ZERO,V3(0.0,0.0,-1.0))
 @Test fun visiblePickerOwnsTriggerEvenWhenDetectedWallIsCloser() {
  val vm=GameViewModel(GameSessionUseCase(InputRepository()))
  val scene=InputSceneStub()
  vm.onEvent(GameEvent.Sandbox)
  vm.onEvent(GameEvent.Show(GamePanel.PICKER))
  vm.updateSurfaces(listOf(Surface("wall",V3(0.0,0.0,-.5),V3(1.0,0.0,0.0),V3(0.0,1.0,0.0),listOf(V2(-2.0,-2.0),V2(2.0,-2.0),V2(2.0,2.0),V2(-2.0,2.0)))),true)
  scene.panel=PanelHit(GamePanel.PICKER,V3(0.0,0.0,-1.0),1.0,false,V3(0.0,0.0,-1.0))
  InputRouter(vm,scene).process(InputFrame("left",ray,true),.016)
  assertEquals("A UI trigger must not attempt Launch behind a detected plane","sandbox_hint",vm.state.value.statusKey)
  assertEquals(0,scene.strokes)
 }
 @Test fun stickCyclesOncePerDeflectionAndRearmsAfterCentering() {
  val vm=GameViewModel(GameSessionUseCase(InputRepository()));vm.onEvent(GameEvent.Sandbox)
  val router=InputRouter(vm,InputSceneStub())
  router.process(InputFrame("left",ray,false,stickClick=true),.016)
  assertTrue(GamePanel.PICKER in vm.state.value.panels)
  val initial=vm.state.value.selected
  repeat(20){router.process(InputFrame("left",ray,false,stickX=1f),.016)}
  assertEquals(PayloadKind.entries[(initial.ordinal+1)%6],vm.state.value.selected)
  router.process(InputFrame("left",ray,false),.016)
  router.process(InputFrame("left",ray,false,stickX=1f),.016)
  assertEquals(PayloadKind.entries[(initial.ordinal+2)%6],vm.state.value.selected)
 }
}
