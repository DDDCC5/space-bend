package com.example.spacebend.domain
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
class SurfaceAssemblerTest {
 private val p=listOf(V3(-1.0,0.0,-1.0),V3(1.0,0.0,-1.0),V3(1.0,0.0,1.0),V3(-1.0,0.0,1.0))
 @Test fun planeBasisDoesNotJumpWhenTriangleOrderChanges(){
  val a=SurfaceAssembler();val first=a.update("A",p,listOf(0,1,2,0,2,3),V3.ZERO,Orientation())!!
  val second=a.update("A",p,listOf(2,3,0,2,0,1),V3(0.0,1.0,0.0),Orientation())!!
  assertEquals(first.u,second.u);assertEquals(first.v,second.v);assertEquals(1.0,second.world(V2(.2,.3)).y,1e-6)
 }
 @Test fun triangleSoupWithDuplicatedVerticesStillProducesBoundary(){
  val soup=listOf(p[0],p[1],p[2],p[0],p[2],p[3]);val a=SurfaceAssembler()
  val s=a.update("A",soup,listOf(0,1,2,3,4,5),V3.ZERO,Orientation())
  assertNotNull(s);assertEquals(4,s!!.boundary.size);assertEquals(4.0,Polygon.area(s.boundary),1e-6)
 }
}
