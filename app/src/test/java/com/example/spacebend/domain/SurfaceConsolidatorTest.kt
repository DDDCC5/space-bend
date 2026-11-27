package com.example.spacebend.domain

import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.SurfaceConsolidator
import org.junit.Assert.*
import org.junit.Test

class SurfaceConsolidatorTest {
 private fun rect(id: String,x: Double,y: Double,w: Double,h: Double,height: Double=0.0)=Surface(id,V3(0.0,height,0.0),V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),listOf(V2(x,y),V2(x+w,y),V2(x+w,y+h),V2(x,y+h)),"FLOOR")
 @Test fun adjacentPiecesBecomeOneLargeSurface(){
  val result=SurfaceConsolidator().merge(listOf(rect("a",0.0,0.0,2.0,2.0),rect("b",2.0,0.0,2.0,2.0)))
  assertEquals(1,result.size);assertEquals(8.0,Polygon.area(result.single().boundary),1e-8)
  assertTrue(result.single().permits(listOf(V2(1.5,.5),V2(2.5,.5),V2(2.5,1.5),V2(1.5,1.5))))
 }
 @Test fun unionDoesNotFillAnLShapedConcavity(){
  val result=SurfaceConsolidator().merge(listOf(rect("a",0.0,0.0,3.0,1.0),rect("b",0.0,1.0,1.0,2.0))).single()
  assertEquals(5.0,Polygon.area(result.boundary),1e-8);assertFalse(result.contains(V2(2.0,2.0)))
 }
 @Test fun gapsAndDifferentHeightsAreKeptSeparate(){
  assertEquals(2,SurfaceConsolidator().merge(listOf(rect("a",0.0,0.0,1.0,1.0),rect("b",1.03,0.0,1.0,1.0))).size)
  assertEquals(2,SurfaceConsolidator().merge(listOf(rect("a",0.0,0.0,2.0,2.0),rect("b",0.0,0.0,2.0,2.0,.8))).size)
 }
 @Test fun holeIsPreservedButARealCoveringPieceCanFillIt(){
  val hole=listOf(V2(.5,.5),V2(1.5,.5),V2(1.5,1.5),V2(.5,1.5))
  val a=rect("a",0.0,0.0,2.0,2.0).copy(holes=listOf(hole))
  val m=SurfaceConsolidator()
  assertFalse(m.merge(listOf(a,rect("b",2.0,0.0,1.0,2.0))).single().contains(V2(1.0,1.0)))
  assertTrue(m.merge(listOf(a,rect("b",.5,.5,1.0,1.0))).single().contains(V2(1.0,1.0)))
 }
 @Test fun preferredAnchorRetainsItsOriginAndAxes(){
  val b=rect("b",2.0,0.0,2.0,2.0).let{it.copy(origin=V3(2.0,0.0,0.0),boundary=it.boundary.map{p->p-V2(2.0,0.0)})}
  val result=SurfaceConsolidator().merge(listOf(rect("a",0.0,0.0,2.0,2.0),b),setOf("b")).single()
  assertEquals("b",result.id);assertEquals(b.origin,result.origin);assertEquals(b.u,result.u);assertTrue(result.contains(V2(-1.0,1.0)))
 }
 @Test fun oppositeNormalsAndRotatedAxesStillMerge(){
  val b=rect("b",2.0,0.0,2.0,2.0).copy(v=V3(0.0,0.0,1.0),boundary=listOf(V2(2.0,0.0),V2(4.0,0.0),V2(4.0,-2.0),V2(2.0,-2.0)))
  assertEquals(1,SurfaceConsolidator().merge(listOf(rect("a",0.0,0.0,2.0,2.0),b)).size)
 }
}
