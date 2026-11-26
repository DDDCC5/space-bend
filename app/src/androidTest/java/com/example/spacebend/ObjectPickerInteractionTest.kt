package com.example.spacebend

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.spacebend.domain.model.PayloadKind
import com.example.spacebend.platform.LaunchActivity
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Real attachment composition and pointer dispatch; does not simulate controller tracking. */
@RunWith(AndroidJUnit4::class)
class ObjectPickerInteractionTest {
 private val instrumentation=InstrumentationRegistry.getInstrumentation()
 private fun <T> main(block: ()->T): T {val r=AtomicReference<T>();instrumentation.runOnMainSync{r.set(block())};return r.get()}
 private fun views(v: View): List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList())
 private fun nodes(n: SemanticsNode): List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
 private fun waitUntil(block: ()->Boolean) {
  val end=SystemClock.uptimeMillis()+45000
  while(SystemClock.uptimeMillis()<end){if(main(block))return;Thread.sleep(100)}
  assertTrue("Timed out waiting for Spatial attachment UI",main(block))
 }
 @Test fun everyPayloadGraphicAndLabelSelectsAndUpdatesLiveAttachment() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("pickerInteraction")=="true")
  ActivityScenario.launch(LaunchActivity::class.java).use{scenario->
   val ref=AtomicReference<GameViewModel>()
   scenario.onActivity{ref.set(ViewModelProvider(it,GameViewModelFactory(it))[GameViewModel::class.java])}
   val vm=ref.get();waitUntil{vm.state.value.sceneReady&&vm.state.value.scanAvailable}
   main{vm.onEvent(GameEvent.Sandbox);vm.onEvent(GameEvent.ContinueScan);NativeFlowFixture.makeTwoPortals(vm);vm.onEvent(GameEvent.Show(GamePanel.PICKER))}
   for(kind in PayloadKind.entries){
    val label=main{vm.state.value.text(kind.textKey)}
    var target: Pair<ViewRootForTest,SemanticsNode>?=null
    waitUntil{
     target=WindowInspector.getGlobalWindowViews().flatMap(::views).filterIsInstance<ViewRootForTest>().firstNotNullOfOrNull{root->
      nodes(root.semanticsOwner.rootSemanticsNode).firstOrNull{label in (it.config.getOrNull(SemanticsProperties.ContentDescription)?:emptyList())}?.let{root to it}
     };target!=null
    }
    main{
     val (root,node)=target!!;val bounds=node.boundsInRoot;val density=root.density.density
     assertTrue("The $kind graphic must be inside the selectable target",bounds.height>=130*density)
     val x=bounds.center.x;val y=bounds.top+45*density;val now=SystemClock.uptimeMillis()
     for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
      val e=MotionEvent.obtain(now,now+if(action==MotionEvent.ACTION_UP)40 else 0,action,x,y,0)
      try{root.view.dispatchTouchEvent(e)}finally{e.recycle()}
     }
    }
    waitUntil{vm.state.value.selected==kind}
    waitUntil{
     val selected=target!!.first.semanticsOwner.rootSemanticsNode
     nodes(selected).any{label in (it.config.getOrNull(SemanticsProperties.ContentDescription)?:emptyList())&&it.config.getOrNull(SemanticsProperties.Selected)==true}
    }
    main{vm.onEvent(GameEvent.Select(if(kind==PayloadKind.CORE)PayloadKind.ROBOT else PayloadKind.CORE))}
    waitUntil{
     nodes(target!!.first.semanticsOwner.rootSemanticsNode).any{label in (it.config.getOrNull(SemanticsProperties.ContentDescription)?:emptyList())&&it.config.getOrNull(SemanticsProperties.Selected)==false}
    }
    main{
     val (root,node)=target!!;val b=node.boundsInRoot;val now=SystemClock.uptimeMillis()
     for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
      val e=MotionEvent.obtain(now,now+if(action==MotionEvent.ACTION_UP)40 else 0,action,b.center.x,b.bottom-20*root.density.density,0)
      try{root.view.dispatchTouchEvent(e)}finally{e.recycle()}
     }
    }
    waitUntil{vm.state.value.selected==kind}
   }
   main{vm.onEvent(GameEvent.ChooseMission(1));vm.onEvent(GameEvent.ConfirmNewExperiment);NativeFlowFixture.makeTwoPortals(vm);vm.onEvent(GameEvent.Show(GamePanel.PICKER))}
   waitUntil{
    val label=vm.state.value.text(PayloadKind.ROBOT.textKey)
    WindowInspector.getGlobalWindowViews().flatMap(::views).filterIsInstance<ViewRootForTest>().any{root->
     nodes(root.semanticsOwner.rootSemanticsNode).any{label in (it.config.getOrNull(SemanticsProperties.ContentDescription)?:emptyList())&&it.config.contains(SemanticsProperties.Disabled)}
    }
   }
   assertEquals(PayloadKind.CORE,main{vm.state.value.selected})
   main{vm.onEvent(GameEvent.Show(GamePanel.MENU))}
  }
 }
}
