package com.example.spacebend

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.spacebend.domain.model.ExperimentPhase
import com.example.spacebend.platform.LaunchActivity
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class ExperienceFlowInteractionTest {
 @Test fun homeScanDrawSelectStopSettingsAndReturnUseOneTaskPanel() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("experienceFlow")=="true")
  ActivityScenario.launch(LaunchActivity::class.java).use{scenario->
   val ref=AtomicReference<GameViewModel>();scenario.onActivity{ref.set(ViewModelProvider(it,GameViewModelFactory(it))[GameViewModel::class.java])};val vm=ref.get()
   NativeFlowFixture.waitUntil("Scene and official plane scan must be ready"){vm.state.value.sceneReady&&vm.state.value.scanAvailable}
   fun panel(expected: GamePanel){NativeFlowFixture.waitUntil("Expected single $expected panel"){vm.state.value.panels==setOf(expected)&&NativeFlowFixture.root(expected)!=null};Log.i("SpaceBendFlowTest","single panel: $expected")}
   panel(GamePanel.MENU)
   NativeFlowFixture.tap(vm,GamePanel.MENU,"sandbox");panel(GamePanel.SCAN)
   NativeFlowFixture.tap(vm,GamePanel.SCAN,"flow_start_drawing");panel(GamePanel.HUD)
   NativeFlowFixture.main{assertFalse(vm.state.value.canChoose);NativeFlowFixture.makeTwoPortals(vm)}
   NativeFlowFixture.waitUntil{vm.state.value.canChoose}
   val layout=NativeFlowFixture.main{vm.engine.portals}
   NativeFlowFixture.tap(vm,GamePanel.HUD,"select");panel(GamePanel.PICKER)
   NativeFlowFixture.tap(vm,GamePanel.PICKER,"flow_back_draw");panel(GamePanel.HUD)
   assertEquals(layout,NativeFlowFixture.main{vm.engine.portals})
   NativeFlowFixture.tap(vm,GamePanel.HUD,"select");panel(GamePanel.PICKER)
   val launchLabel=NativeFlowFixture.main{assertTrue(vm.state.value.canLaunch);String.format(vm.state.value.text("flow_launch_object"),vm.state.value.text(vm.state.value.selected.textKey))}
   NativeFlowFixture.tap(vm,GamePanel.PICKER,"flow_launch_object",launchLabel)
   panel(GamePanel.HUD)
   NativeFlowFixture.waitUntil{vm.state.value.phase==ExperimentPhase.RUNNING}
   NativeFlowFixture.tap(vm,GamePanel.HUD,"flow_stop_edit");panel(GamePanel.HUD)
   NativeFlowFixture.waitUntil{vm.state.value.phase==ExperimentPhase.PLANNING}
   assertEquals(layout,NativeFlowFixture.main{vm.engine.portals})
   NativeFlowFixture.tap(vm,GamePanel.HUD,"settings");panel(GamePanel.SETTINGS)
   NativeFlowFixture.tap(vm,GamePanel.SETTINGS,"back");panel(GamePanel.HUD)
   NativeFlowFixture.tap(vm,GamePanel.HUD,"flow_home");panel(GamePanel.MENU)
   NativeFlowFixture.tap(vm,GamePanel.MENU,"sandbox");NativeFlowFixture.waitUntil{vm.state.value.pendingExperiment!=null}
   NativeFlowFixture.tap(vm,GamePanel.MENU,"flow_keep_experiment");panel(GamePanel.HUD)
   assertEquals(layout,NativeFlowFixture.main{vm.engine.portals})
   NativeFlowFixture.tap(vm,GamePanel.HUD,"flow_clear");NativeFlowFixture.waitUntil{vm.state.value.portalCount==0};panel(GamePanel.HUD)
   NativeFlowFixture.tap(vm,GamePanel.HUD,"flow_home");panel(GamePanel.MENU)
   Log.i("SpaceBendFlowTest","flow pointer actions passed; back/stop/settings preserve layout, clear removes it")
  }
 }

 @Test fun manualRescanResumesDrawingAndPreservesValidAnchorCoordinates() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("experienceFlow")=="true")
  ActivityScenario.launch(LaunchActivity::class.java).use{scenario->
   val ref=AtomicReference<GameViewModel>();scenario.onActivity{ref.set(ViewModelProvider(it,GameViewModelFactory(it))[GameViewModel::class.java])};val vm=ref.get()
   NativeFlowFixture.waitUntil{vm.state.value.sceneReady&&vm.state.value.scanAvailable}
   NativeFlowFixture.tap(vm,GamePanel.MENU,"sandbox");NativeFlowFixture.tap(vm,GamePanel.SCAN,"flow_start_drawing")
   val before=NativeFlowFixture.main{NativeFlowFixture.makeTwoPortals(vm);vm.engine.portals.associate{p->p.id to vm.engine.surfaces.first{it.id==p.surfaceId}.world(p.center)}}
   NativeFlowFixture.tap(vm,GamePanel.HUD,"scan")
   NativeFlowFixture.waitUntil{vm.state.value.currentPanel==GamePanel.SCAN&&vm.state.value.canContinueScan}
   val key=NativeFlowFixture.main{if(vm.state.value.invalidAnchorCount>0)"flow_redraw_now"else "flow_start_drawing"}
   NativeFlowFixture.tap(vm,GamePanel.SCAN,key)
   NativeFlowFixture.waitUntil{vm.state.value.panels==setOf(GamePanel.HUD)}
   NativeFlowFixture.main{
    if(vm.engine.portals.isEmpty())assertEquals("flow_layout_redraw",vm.state.value.statusKey)
    else {
     assertEquals(2,vm.engine.portals.size)
     for(p in vm.engine.portals){val point=vm.engine.surfaces.first{it.id==p.surfaceId}.world(p.center);assertTrue("Valid anchor coordinates rotated during scan restart",point.distance(before.getValue(p.id))<.05)}
    }
    vm.onEvent(GameEvent.Show(GamePanel.MENU))
   }
   Log.i("SpaceBendFlowTest","manual rescan used fresh SDK planes, resumed drawing and retained coordinates for valid anchors")
  }
 }
}
