package com.example.spacebend.domain

import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test

class GeometryTest {
    private val square = listOf(V2(-1.0,-1.0), V2(1.0,-1.0), V2(1.0,1.0), V2(-1.0,1.0))
    @Test fun wholeObjectFitsOnlyWithEdgeClearance() {
        assertTrue(Polygon.fitsCircle(square, V2(0.0,0.0), .9))
        assertFalse(Polygon.fitsCircle(square, V2(.9,0.0), .2))
        assertFalse(Polygon.fitsCircle(square, V2(0.0,0.0), 1.1))
    }
    @Test fun concaveApertureRejectsCircleOverNotch() {
        val poly=listOf(V2(-1.0,-1.0),V2(1.0,-1.0),V2(1.0,1.0),V2(.1,1.0),V2(.1,0.0),V2(-.1,0.0),V2(-.1,1.0),V2(-1.0,1.0))
        assertFalse(Polygon.fitsCircle(poly,V2(0.0,.5),.2))
        assertTrue(Polygon.fitsCircle(poly,V2(-.6,.0),.2))
    }
    @Test fun selfCrossedOutlineIsInvalid() {
        assertFalse(Polygon.isSimple(listOf(V2(-1.0,-1.0),V2(1.0,1.0),V2(-1.0,1.0),V2(1.0,-1.0))))
        assertTrue(Polygon.isSimple(square))
    }
    @Test fun tiltedPlaneRoundTripsAndRayIntersects() {
        val plane=Surface("wall", V3(0.0,1.0,-2.0),V3(1.0,0.0,0.0),V3(0.0,1.0,0.0),square)
        val q=V2(.3,-.2);assertEquals(q.x,plane.project(plane.world(q)).x,1e-9);assertEquals(q.y,plane.project(plane.world(q)).y,1e-9)
        assertEquals(V3(0.0,1.0,-2.0),plane.hit(Ray(V3(0.0,1.0,0.0),V3(0.0,0.0,-1.0)))!!.point)
        assertNull(plane.hit(Ray(V3(3.0,1.0,0.0),V3(0.0,0.0,-1.0))))
    }
    @Test fun wholeOutlineMustStayInsideConcaveSurface() {
        val u=listOf(V2(-2.0,-2.0),V2(2.0,-2.0),V2(2.0,2.0),V2(.2,2.0),V2(.2,0.0),V2(-.2,0.0),V2(-.2,2.0),V2(-2.0,2.0))
        val bridge=listOf(V2(-1.0,.5),V2(-.6,.5),V2(.6,.5),V2(1.0,.5),V2(1.0,1.0),V2(.6,1.0),V2(-.6,1.0),V2(-1.0,1.0))
        assertFalse(Polygon.encloses(u,bridge))
    }
    @Test fun finitePlaneCollisionIncludesSphereOverhangingAnEdge() {
        val s=Surface("table",V3.ZERO,V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),square)
        val hit=SweepCollision.firstContact(s,V3(1.1,1.0,0.0),V3(1.1,-1.0,0.0),.2)
        assertNotNull(hit);assertTrue(hit!!.fraction in .35..0.5)
        assertNull(SweepCollision.firstContact(s,V3(1.3,1.0,0.0),V3(1.3,-1.0,0.0),.2))
    }
}
