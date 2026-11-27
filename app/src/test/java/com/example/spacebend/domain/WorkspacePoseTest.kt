package com.example.spacebend.domain

import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.WorkspacePose
import org.junit.Assert.*
import org.junit.Test

class WorkspacePoseTest {
 @Test fun startsInFrontOfHeadAtAnyYawAndPosition(){
  val head=V3(3.0,1.7,-2.0)
  for(forward in listOf(V3(0.0,0.0,-1.0),V3(1.0,0.0,0.0),V3(0.0,0.0,1.0),V3(-1.0,0.0,0.0))){
   val pose=WorkspacePose.inFront(head,forward)!!
   assertEquals(1.45,pose.position.distance(head),1e-8)
   assertEquals(1.0,(pose.position-head).normalized().dot(forward),1e-8)
   val hit=pose.hit(Ray(head,forward),V2(.43,.287))!!
   assertEquals(1.45,hit.distance,1e-8);assertFalse(hit.header)
   assertNull(pose.hit(Ray(head,-forward),V2(.43,.287)))
  }
 }
 @Test fun hitTestUsesRotatedLocalPanelAxes(){
  val p=WorkspacePose.inFront(V3(2.0,1.7,3.0),V3(1.0,0.0,0.0))!!
  val eye=p.position-p.normal*1.0
  val target=p.position+p.right*.2+V3(0.0,.24,0.0)
  assertTrue(p.hit(Ray(eye,(target-eye).normalized()),V2(.43,.287))!!.header)
  assertNull(p.hit(Ray(eye,(target+p.right*.6-eye).normalized()),V2(.43,.287)))
 }
 @Test fun invalidTrackingCannotPlacePanel(){assertNull(WorkspacePose.inFront(V3.ZERO,V3.ZERO));assertNull(WorkspacePose.inFront(V3(Double.NaN,0.0,0.0),V3(0.0,0.0,-1.0)))}
 @Test fun lookingStraightDownStillPlacesPanelAtGazeCenter(){
  val head=V3(0.0,1.7,0.0);val gaze=V3(0.0,-1.0,0.0)
  val pose=WorkspacePose.inFront(head,gaze,V3(0.0,0.0,-1.0))!!
  assertEquals(1.45,pose.hit(Ray(head,gaze),V2(.43,.287))!!.distance,1e-8)
  assertEquals(0.0,pose.normal.dot(pose.right),1e-8);assertEquals(0.0,pose.normal.dot(pose.up),1e-8)
 }
}
