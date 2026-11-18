package com.example.spacebend.platform.spatial

import com.example.spacebend.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class PlaneVisualPlanTest {
 private fun plane(id: String="a")=Surface(id,V3.ZERO,V3(1.0,0.0,0.0),V3(0.0,1.0,0.0),listOf(V2(-1.0,-1.0),V2(1.0,-1.0),V2(1.0,1.0),V2(-1.0,1.0)))
 @Test fun poseOnlyUpdatesReuseMeshAndRetainLatestPose() {
  val p=PlaneVisualPlan();val s=plane();p.update(listOf(s));p.markBuilt(p.nextBuild()!!)
  repeat(75){val moved=s.copy(origin=V3(it*.001,.3,-2.0));p.update(listOf(moved));assertNull(p.nextBuild());assertEquals(moved,p.surface(s.id))}
 }
 @Test fun shapeChangesAreIncrementalCoalescedAndUseLatestBoundary() {
  val p=PlaneVisualPlan();val a=plane();val b=plane("b");p.update(listOf(a,b));p.markBuilt(p.nextBuild()!!);p.markBuilt(p.nextBuild()!!)
  val resized=a.copy(boundary=a.boundary.map{it*2.0});p.update(listOf(resized,b))
  val latest=resized.copy(boundary=a.boundary.map{it*3.0});p.update(listOf(latest,b))
  assertEquals(latest,p.nextBuild());assertNull(p.nextBuild())
 }
 @Test fun jitterAndEquivalentCyclicOrReversedOutlinesDoNotRebuild() {
  val p=PlaneVisualPlan();val s=plane();p.update(listOf(s));p.markBuilt(p.nextBuild()!!)
  p.update(listOf(s.copy(boundary=(s.boundary.drop(1)+s.boundary.take(1)).reversed().map{it+V2(.002,-.003)})))
  assertNull(p.nextBuild())
 }
 @Test fun removedAnchorsAndPendingBuildsCannotReappear() {
  val p=PlaneVisualPlan();p.update(listOf(plane(),plane("pending")));p.markBuilt(p.nextBuild()!!)
  assertEquals(setOf("a"),p.update(emptyList()));assertNull(p.nextBuild());assertNull(p.surface("a"))
  p.update(listOf(plane()));assertNotNull(p.nextBuild());p.clear();assertNull(p.nextBuild())
 }
}
