package com.example.spacebend.platform.spatial

import com.example.spacebend.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class InputFocusTest {
 private fun input(id: String,trigger: Boolean=false,grip: Boolean=false)=InputFrame(id,Ray(V3.ZERO,V3(0.0,0.0,-1.0)),trigger,grip)
 @Test fun leftInteractionTakesFocusAndIdleRightDoesNotStealItBack() {
  val focus=InputFocus()
  assertEquals("right",focus.choose(listOf(input("right"),input("left")))?.id)
  assertEquals("left",focus.choose(listOf(input("right"),input("left",trigger=true)))?.id)
  assertEquals("left",focus.choose(listOf(input("right"),input("left")))?.id)
 }
 @Test fun capturedDrawingKeepsRayUntilReleasedThenOtherHandCanTakeOver() {
  val focus=InputFocus()
  assertEquals("left",focus.choose(listOf(input("right",grip=true),input("left",trigger=true)),"left")?.id)
  focus.choose(listOf(input("right"),input("left")))
  assertEquals("right",focus.choose(listOf(input("right",trigger=true),input("left")))?.id)
 }
 @Test fun disappearingInputFallsBackAndLatestPoseIsReturnedWithoutSmoothingLag() {
  val focus=InputFocus();focus.choose(listOf(input("left",trigger=true)))
  val latest=input("right").copy(ray=Ray(V3(1.0,2.0,3.0),V3(0.0,1.0,0.0)))
  assertSame(latest,focus.choose(listOf(latest)))
  assertNull(focus.choose(emptyList()))
 }
 @Test fun triggerOnAlreadyTouchedLeftControllerOverridesTouchedRightController() {
  val focus=InputFocus()
  val right=input("right").copy(aimActive=true)
  val left=input("left").copy(aimActive=true)
  focus.choose(listOf(right,input("left")))
  focus.choose(listOf(right,left))
  assertEquals("right",focus.choose(listOf(right.copy(trigger=true),left))?.id)
  focus.choose(listOf(right,left))
  assertEquals("left",focus.choose(listOf(right,left.copy(trigger=true)))?.id)
 }
 @Test fun passiveTouchCannotStealRayFromHeldAttachmentPress() {
  val focus=InputFocus()
  val left=input("left",trigger=true)
  focus.choose(listOf(input("right"),left))
  assertEquals("left",focus.choose(listOf(input("right").copy(aimActive=true),left))?.id)
 }
}
