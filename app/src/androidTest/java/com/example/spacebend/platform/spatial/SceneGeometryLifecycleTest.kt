package com.example.spacebend.platform.spatial

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import com.example.spacebend.domain.model.Portal
import com.example.spacebend.domain.model.Surface
import com.example.spacebend.domain.model.V2
import com.example.spacebend.domain.model.V3
import com.example.spacebend.platform.LaunchActivity
import com.pico.spatial.core.ecs.ModelEntity
import com.example.spacebend.ui.game.GameViewModel
import com.example.spacebend.ui.game.GameViewModelFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Native resource regression; synthetic geometry is not evidence of room scanning or physical input. */
class SceneGeometryLifecycleTest {
 private fun enabled()=assumeTrue("Opt in with -e geometryLifecycle true",
  InstrumentationRegistry.getArguments().getString("geometryLifecycle")=="true")
 private fun awaitScene(scenario: ActivityScenario<LaunchActivity>) {
  val deadline=android.os.SystemClock.uptimeMillis()+60000
  while(android.os.SystemClock.uptimeMillis()<deadline){
   var ready=false
   scenario.onActivity {activity->ready=ViewModelProvider(activity,GameViewModelFactory(activity))[GameViewModel::class.java].state.value.sceneReady}
   if(ready)return
   Thread.sleep(100)
  }
  throw AssertionError("Native scene did not initialize")
 }

 @Test fun repeatedStrokeReplacementKeepsSceneResourcesAlive() {
  enabled()
  ActivityScenario.launch(LaunchActivity::class.java).use { scenario ->
   awaitScene(scenario)
   scenario.onActivity {
    val geometry=SceneGeometry()
    try {
     for(material in listOf(geometry.ice,geometry.halo,geometry.planeMaterial,geometry.amber)) {
      repeat(40){step ->
       val points=(0..(step%12+1)).map{i->V3(i*.02,1.0,-1.0+step*.001)}
       val trace=geometry.trace(points,.003,false,material)!!
       trace.first.destroy();trace.second.close()
       assertTrue("Shared stroke material was released on replacement $step",material.valid)
      }
     }
     repeat(20){
      val dot=ModelEntity(geometry.dotMesh,geometry.ice);dot.destroy()
      val beam=ModelEntity(geometry.beamMesh,geometry.planeMaterial);beam.destroy()
      assertTrue("Particle mesh was released by a transient owner",geometry.dotMesh.valid)
      assertTrue("Beam mesh was released by a transient owner",geometry.beamMesh.valid)
     }
     Log.i("SpaceBendResourceTest","160 trace replacements and 40 shared-mesh owner destructions passed")
    } finally { geometry.close() }
    assertFalse("Scene material must release on scene close",geometry.ice.valid)
    assertFalse("Scene mesh must release on scene close",geometry.dotMesh.valid)
   }
  }
 }

 @Test fun repeatedPortalRemovalAndRecreationKeepsSharedResourcesAlive() {
  enabled()
  ActivityScenario.launch(LaunchActivity::class.java).use { scenario ->
   awaitScene(scenario)
   scenario.onActivity {
    val geometry=SceneGeometry()
    val boundary=listOf(V2(-1.0,-1.0),V2(1.0,-1.0),V2(1.0,1.0),V2(-1.0,1.0))
    val surface=Surface("resource-test",V3(0.0,1.0,-1.0),V3(1.0,0.0,0.0),V3(0.0,1.0,0.0),boundary)
    val outline=listOf(V2(-.3,-.3),V2(.3,-.3),V2(.3,.3),V2(-.3,.3))
    try {
     repeat(30){cycle ->
      val portal=Portal(cycle.toLong(),surface.id,outline,0)
      val visual=PortalVisual(portal,surface,geometry)
      visual.update(.016,0.0);visual.update(.016,.8);visual.close()
      assertTrue("Portal edge material released after deletion $cycle",geometry.ice.valid)
      assertTrue("Portal halo material released after deletion $cycle",geometry.halo.valid)
      assertTrue("Portal particle mesh released after deletion $cycle",geometry.dotMesh.valid)
     }
     Log.i("SpaceBendResourceTest","30 portal create/update/collapse/destroy cycles passed")
    } finally { geometry.close() }
    assertFalse(geometry.halo.valid)
    assertFalse(geometry.beamMesh.valid)
   }
  }
 }
}
