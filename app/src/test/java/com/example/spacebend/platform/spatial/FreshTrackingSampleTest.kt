package com.example.spacebend.platform.spatial

import org.junit.Assert.*
import org.junit.Test

class FreshTrackingSampleTest {
 @Test fun restartWaitsForNewSessionSampleRatherThanCachedPose(){
  val gate=FreshTrackingSample<String>();val old=gate.begin();gate.offer(old,"old pose")
  assertEquals("old pose",gate.latest())
  val current=gate.begin();assertNull(gate.latest())
  gate.offer(old,"late old callback");assertNull(gate.latest())
  gate.offer(current,"current pose");assertEquals("current pose",gate.latest())
 }
 @Test fun lateOldCallbackCannotOverwriteFreshSample(){
  val gate=FreshTrackingSample<String>();val old=gate.begin();val current=gate.begin()
  gate.offer(current,"fresh");gate.offer(old,"stale");assertEquals("fresh",gate.latest())
 }
}
