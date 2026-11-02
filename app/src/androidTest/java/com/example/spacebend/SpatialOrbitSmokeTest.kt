package com.example.spacebend

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.SweepCollision
import com.example.spacebend.platform.LaunchActivity
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real SDK planes/rendering with deterministic commands; it does not claim physical input validation. */
@RunWith(AndroidJUnit4::class)
class SpatialOrbitSmokeTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 private fun <T> main(block: ()->T): T {val result=AtomicReference<T>();instrumentation.runOnMainSync{result.set(block())};return result.get()}
 private fun waitFor(seconds: Int,condition: ()->Boolean) {
  val end=System.nanoTime()+seconds*1_000_000_000L
  while(System.nanoTime()<end){if(main(condition))return;Thread.sleep(100)}
  assertTrue("Timed out waiting for real Spatial scene state",main(condition))
 }
 private fun capture(name: String) {
  // Android's 2D automation screenshot is black for XR layers. The host captures the official compositor.
  File(instrumentation.targetContext.getExternalFilesDir(null),"xr-capture-marker.txt").writeText(name)
 }
 @Test fun realPlanesDriveTwoPortalOrbitAndPersistSuccess() {
  val ref=AtomicReference<GameViewModel>()
  ActivityScenario.launch(LaunchActivity::class.java).use{scenario->
   scenario.onActivity{activity->ref.set(ViewModelProvider(activity,GameViewModelFactory(activity))[GameViewModel::class.java])}
   val vm=ref.get();waitFor(60){vm.state.value.sceneReady&&vm.state.value.scanAvailable}
   capture("01-room-scan");Thread.sleep(2200)
   main{vm.onEvent(GameEvent.ContinueScan);vm.onEvent(GameEvent.ChooseMission(1))}
   val chosen=main{
    vm.engine.surfaces.sortedByDescending{Polygon.area(it.boundary)}.firstNotNullOfOrNull{s->
     if(s.normal.y<.9)return@firstNotNullOfOrNull null
     val minX=s.boundary.minOf{it.x};val maxX=s.boundary.maxOf{it.x};val minY=s.boundary.minOf{it.y};val maxY=s.boundary.maxOf{it.y}
     val points=mutableListOf<V2>();var x=minX+.65
     while(x<maxX-.65){var y=minY+.65;while(y<maxY-.65){val q=V2(x,y);val outline=(0 until 40).map{val a=it*PI/20;V2(q.x+.58*cos(a),q.y+.58*sin(a))};if(s.permits(outline)){
       val base=s.world(q)
       val clear=vm.engine.surfaces.filter{it.id!=s.id}.all{other->SweepCollision.firstContact(other,base+s.normal*.25,base+s.normal*1.65,PayloadKind.CORE.radius)==null}
       if(clear)points+=q
      };y+=.4};x+=.4}
     val pair=points.firstNotNullOfOrNull{a->points.firstOrNull{b->(b-a).length()>1.35}?.let{b->a to b}}
     pair?.let{Triple(s,it.first,it.second)}
    }
   }
   File(instrumentation.targetContext.getExternalFilesDir(null),"plane-diagnostic.txt").writeText(main { "chosen=$chosen\n"+vm.engine.surfaces.joinToString("\n") })
   assertNotNull("No real detected plane can fit two safe payload apertures",chosen)
   main{
    val (s,a,b)=chosen!!
    for(q in listOf(a,b)){val outline=(0 until 40).map{val angle=it*PI/20;V2(q.x+.58*cos(angle),q.y+.58*sin(angle))};assertEquals(com.example.spacebend.domain.usecase.EditResult.OK,vm.addPortal(s.id,outline))}
    vm.onEvent(GameEvent.Launch)
   }
   waitFor(5){vm.state.value.phase==ExperimentPhase.RUNNING||vm.state.value.phase==ExperimentPhase.STABLE}
   capture("02-two-portals-running")
   try {waitFor(90){vm.state.value.phase==ExperimentPhase.STABLE||vm.state.value.phase==ExperimentPhase.FAILED}} finally {
    File(instrumentation.targetContext.getExternalFilesDir(null),"wait-diagnostic.txt").writeText(main {"ui=${vm.state.value}, enginePhase=${vm.engine.phase}, elapsed=${vm.engine.elapsedSeconds}, payload=${vm.engine.payload}"})
   }
   capture("03-orbit-terminal")
   File(instrumentation.targetContext.getExternalFilesDir(null),"orbit-diagnostic.txt").writeText(main{"phase=${vm.engine.phase}, payload=${vm.engine.payload}, planes=${vm.engine.surfaces.size}"})
   assertEquals("Real-plane orbit must reach stable loop",ExperimentPhase.STABLE,main{vm.state.value.phase})
   assertTrue(main{(vm.state.value.records[1]?.stars?:0)>0})
   Thread.sleep(6000) // The host captures the official XR compositor; physical UI remains interactive.
   capture("03-stable-loop-result")
   main{vm.onEvent(GameEvent.Show(GamePanel.SETTINGS));vm.onEvent(GameEvent.Locale(GameLocale.TRADITIONAL))}
   assertEquals("zh-Hans",main{vm.state.value.settings.locale.voiceFolder});capture("04-traditional-settings");Thread.sleep(8000)
   main{vm.onEvent(GameEvent.Locale(GameLocale.SIMPLIFIED));vm.onEvent(GameEvent.Close(GamePanel.SETTINGS))}
  }
 }
}
