package com.example.spacebend

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.platform.app.InstrumentationRegistry
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.EditResult
import com.example.spacebend.domain.usecase.SweepCollision
import com.example.spacebend.ui.game.GameViewModel
import com.example.spacebend.ui.navigation.GamePanel
import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

/** Uses real SDK surfaces and real attachment pointer dispatch; never physical-controller evidence. */
internal object NativeFlowFixture {
 fun <T> main(block: ()->T): T {val r=AtomicReference<T>();InstrumentationRegistry.getInstrumentation().runOnMainSync{r.set(block())};return r.get()}
 fun waitUntil(message: String="Spatial flow did not settle",seconds: Int=50,block: ()->Boolean) {
  val end=SystemClock.uptimeMillis()+seconds*1000
  while(SystemClock.uptimeMillis()<end){if(main(block))return;Thread.sleep(100)}
  assertTrue(message,main(block))
 }
 private fun views(v: View): List<View> = listOf(v)+(if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList())
 fun nodes(n: SemanticsNode): List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
 fun root(panel: GamePanel): ViewRootForTest? = WindowInspector.getGlobalWindowViews().flatMap(::views).filterIsInstance<ViewRootForTest>().firstOrNull{r->nodes(r.semanticsOwner.rootSemanticsNode).any{it.config.getOrNull(SemanticsProperties.TestTag)=="spacebend-panel-${panel.name}"}}
 fun tap(vm: GameViewModel,panel: GamePanel,labelKey: String,labelText: String?=null) {
  var target: Pair<ViewRootForTest,SemanticsNode>?=null
  waitUntil("No enabled $panel control $labelKey"){
   if(vm.state.value.currentPanel!=panel)return@waitUntil false
   val r=root(panel)?:return@waitUntil false;val label=labelText?:vm.state.value.text(labelKey)
   val n=nodes(r.semanticsOwner.rootSemanticsNode).firstOrNull{label in (it.config.getOrNull(SemanticsProperties.ContentDescription)?:emptyList())&&it.config.contains(SemanticsActions.OnClick)&&!it.config.contains(SemanticsProperties.Disabled)}
   target=n?.let{r to it};target!=null
  }
  main{
   val (r,n)=target!!;val b=n.boundsInRoot
   assertTrue("Primary action $labelKey is clipped outside the viewport",b.width>0&&b.height>0&&b.center.y in 0f..r.view.height.toFloat())
   val now=SystemClock.uptimeMillis()
   for(a in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
    val e=MotionEvent.obtain(now,now+if(a==MotionEvent.ACTION_UP)40 else 0,a,b.center.x,b.center.y,0)
    try{r.view.dispatchTouchEvent(e)}finally{e.recycle()}
   }
  }
 }
 fun makeTwoPortals(vm: GameViewModel) {
  val choice=vm.engine.surfaces.sortedByDescending{Polygon.area(it.boundary)}.firstNotNullOfOrNull{s->
   if(s.normal.y<.9)return@firstNotNullOfOrNull null
   val minX=s.boundary.minOf{it.x};val maxX=s.boundary.maxOf{it.x};val minY=s.boundary.minOf{it.y};val maxY=s.boundary.maxOf{it.y}
   val centers=mutableListOf<V2>();var x=minX+.5
   while(x<=maxX-.5){var y=minY+.5;while(y<=maxY-.5){val q=V2(x,y);if(s.permits(outline(q))){val base=s.world(q);if(vm.engine.surfaces.filter{it.id!=s.id}.all{other->SweepCollision.firstContact(other,base+s.normal*.25,base+s.normal*.85,PayloadKind.CORE.radius)==null})centers+=q};y+=.3};x+=.3}
   centers.firstNotNullOfOrNull{a->centers.firstOrNull{b->(a-b).length()>1.0}?.let{b->Triple(s,a,b)}}
  }
  assertNotNull("Real SDK floor must fit two test portals",choice)
  val (s,a,b)=choice!!
  assertEquals(EditResult.OK,vm.addPortal(s.id,outline(a)));assertEquals(EditResult.OK,vm.addPortal(s.id,outline(b)))
 }
 private fun outline(q: V2)=(0 until 32).map{i->val a=i*PI/16;V2(q.x+.46*cos(a),q.y+.46*sin(a))}
}
