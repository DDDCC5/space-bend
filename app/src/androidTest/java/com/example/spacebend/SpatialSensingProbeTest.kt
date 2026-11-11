package com.example.spacebend

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.spacebend.platform.LaunchActivity
import com.pico.spatial.sense.mesh.MeshTrackingManager
import com.pico.spatial.sense.plane.PlaneTrackingManager
import com.pico.spatial.sense.base.TrackingState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue

/** Device diagnostic only: reports raw SDK output without creating room geometry or changing game progress. */
class SpatialSensingProbeTest {
 @Test fun reportRawPlaneAndMeshOutput() {
  assumeTrue("Manual device diagnostic; run with -e sensingProbe true",InstrumentationRegistry.getArguments().getString("sensingProbe")=="true")
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val updates=AtomicInteger()
  var activeSamples=0
  var maxPlanes=0
  var maxMeshes=0
  ActivityScenario.launch(LaunchActivity::class.java).use {
   Thread.sleep(5000)
   val subscription=MeshTrackingManager.subscribeAnchorUpdate{updates.incrementAndGet()}
   try {
    instrumentation.runOnMainSync{MeshTrackingManager.start()}
    repeat(8){
     runBlocking {
      val planes=withTimeoutOrNull(2000){PlaneTrackingManager.loadAllAnchors()}
      val meshes=withTimeoutOrNull(2000){MeshTrackingManager.loadAllAnchors()}
      if(PlaneTrackingManager.state==TrackingState.RUNNING&&MeshTrackingManager.state==TrackingState.RUNNING)activeSamples++
      maxPlanes=maxOf(maxPlanes,planes?.size?:0);maxMeshes=maxOf(maxMeshes,meshes?.size?:0)
      Log.i("SpaceBendProbe","planeState=${PlaneTrackingManager.state} planes=${planes?.size} meshState=${MeshTrackingManager.state} meshes=${meshes?.size} meshUpdates=${updates.get()}")
     }
     Thread.sleep(2000)
    }
   } finally {subscription.cancel();instrumentation.runOnMainSync{MeshTrackingManager.stop()}}
  }
  assertTrue("Probe interrupted: only $activeSamples/8 samples had both trackers RUNNING",activeSamples>=6)
  assertTrue("No real planes returned; mesh count=$maxMeshes mesh updates=${updates.get()}",maxPlanes>0)
 }
}
