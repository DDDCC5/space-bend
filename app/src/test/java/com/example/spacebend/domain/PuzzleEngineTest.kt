package com.example.spacebend.domain
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PuzzleEngineTest {
 private val boundary=listOf(V2(-10.0,-10.0),V2(10.0,-10.0),V2(10.0,10.0),V2(-10.0,10.0))
 private val floor=Surface("floor",V3.ZERO,V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),boundary)
 private fun aperture(x: Double)=(0 until 24).map { val a=it*PI/12;V2(x+.65*cos(a),.65*sin(a)) }
 private fun game(mode: GameMode=GameMode.CHALLENGE): PuzzleEngine = PuzzleEngine(mode,Missions.all.first()).apply {updateSurfaces(listOf(floor));addPortal("floor",aperture(-1.0));addPortal("floor",aperture(1.0))}
 @Test fun cannotDrawInAirOrMoveAcrossPlanes() {
  val e=game();assertEquals(EditResult.NO_SURFACE,e.addPortal("unknown",aperture(3.0)))
  val id=e.portals.first().id;assertEquals(EditResult.NO_SURFACE,e.movePortal(id,"wall",V2(1.0,0.0)))
  assertEquals(EditResult.OK,e.movePortal(id,"floor",V2(.1,0.0)));assertEquals(-.9,e.portals.first().center.x,1e-8)
 }
 @Test fun invalidSelfIntersectionAndOverlapAreRejected() {
  val e=game();assertEquals(EditResult.OVERLAP,e.addPortal("floor",aperture(-1.0)))
  assertEquals(EditResult.INVALID_SHAPE,e.addPortal("floor",listOf(V2(-1.0,-1.0),V2(1.0,1.0),V2(-1.0,1.0),V2(1.0,-1.0))))
 }
 @Test fun centerMustCrossAndWholeProjectedBodyMustFitBeforeTransfer() {
  val e=game();assertTrue(e.launch(PayloadKind.CORE));val p=e.payload!!
  p.position=V3(-1.0,.05,0.0);p.velocity=V3(0.0,-1.0,0.0)
  e.advance(.01);assertEquals(0,p.traversals)
  var transfer: SimulationEvent.Transferred?=null
  repeat(200){e.advance(1.0/240).filterIsInstance<SimulationEvent.Transferred>().firstOrNull()?.let{transfer=it}}
  assertNotNull(transfer);assertTrue(p.traversals>=1);assertTrue(transfer!!.speed>1)
 }
 @Test fun reverseSideEntryExitsDestinationReverseNormal() {
  val e=game();e.launch(PayloadKind.CORE);val p=e.payload!!;p.position=V3(-1.0,-.4,0.0);p.velocity=V3(0.0,3.0,0.0)
  var transfer=false
  repeat(200){if(!transfer)transfer=e.advance(1.0/240).any{it is SimulationEvent.Transferred}}
  assertTrue(transfer);assertTrue(p.velocity.y<0);assertTrue(p.position.y<0)
 }
 @Test fun highSpeedWallCollisionCannotTunnelAndCollapsesEveryPortal() {
  val e=game();e.launch(PayloadKind.CORE);e.payload!!.apply {position=V3(4.0,2.0,0.0);velocity=V3(0.0,-250.0,0.0)}
  val events=e.advance(.05);assertTrue(events.any{it is SimulationEvent.Collided});assertEquals(ExperimentPhase.FALLING,e.phase)
  e.advance(1.0);assertEquals(ExperimentPhase.FAILED,e.phase);assertTrue(e.portals.isEmpty())
 }
 @Test fun closedTwoPortalOrbitProducesStableOnlyAfterTwoCompleteCircuits() {
  val e=game();e.launch(PayloadKind.CORE)
  repeat(20000){if(e.phase!=ExperimentPhase.STABLE)e.advance(1.0/240)}
  assertEquals(ExperimentPhase.STABLE,e.phase);assertTrue(e.payload!!.loops>=2)
 }
 @Test fun stableOrbitDoesNotGainEnergyEveryPassAndHitRoomCeiling() {
  val e=game();val ceiling=Surface("ceiling",V3(0.0,2.5,0.0),V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),boundary)
  e.updateSurfaces(listOf(floor,ceiling));e.launch(PayloadKind.CORE)
  var highest=0.0
  repeat(240*30){e.advance(1.0/240);highest=max(highest,e.payload!!.position.y)}
  assertEquals(ExperimentPhase.STABLE,e.phase);assertTrue(e.payload!!.loops>10)
  assertTrue("Portal attraction must not inject energy on each cycle: apex=$highest",highest<1.5)
 }
 @Test fun sandboxNeverProducesVictoryOrFailureResults() {
  val e=game(GameMode.SANDBOX);e.launch(PayloadKind.CORE)
  val ev=mutableListOf<SimulationEvent>();repeat(3000){ev+=e.advance(1.0/240)}
  assertFalse(ev.any{it is SimulationEvent.Stable||it is SimulationEvent.Failed})
 }
 @Test fun trackingLossPausesTimeAndMotion() {
  val e=game();e.launch(PayloadKind.CORE);val pos=e.payload!!.position;val t=e.elapsedSeconds
  e.advance(.1,tracking=false);assertEquals(pos,e.payload!!.position);assertEquals(t,e.elapsedSeconds,0.0)
 }
 @Test fun framePartitionDoesNotChangeBallistics() {
  val a=game();val b=game();a.launch(PayloadKind.CORE);b.launch(PayloadKind.CORE)
  repeat(20){a.advance(.01)};repeat(10){b.advance(.02)}
  assertTrue(a.payload!!.position.distance(b.payload!!.position)<1e-6)
 }
 @Test fun fallingContinuesAfterFailurePanelUntilReachingFloor() {
  val e=game();val wall=Surface("wall",V3(0.0,4.0,-2.0),V3(1.0,0.0,0.0),V3(0.0,1.0,0.0),boundary)
  e.updateSurfaces(listOf(floor,wall));e.launch(PayloadKind.CORE)
  e.payload!!.apply{position=V3(4.0,10.0,0.0);velocity=V3(0.0,0.0,-100.0)}
  e.advance(.1);e.advance(.9);assertEquals(ExperimentPhase.FAILED,e.phase)
  val y=e.payload!!.position.y;e.advance(.1);assertTrue(e.payload!!.position.y<y)
 }
 @Test fun teleportRemainderSweepsDestinationObstacles() {
  val e=game();val ceiling=Surface("obstacle",V3(0.0,.5,0.0),V3(1.0,0.0,0.0),V3(0.0,0.0,-1.0),boundary)
  e.updateSurfaces(listOf(floor,ceiling));e.launch(PayloadKind.CORE)
  e.payload!!.apply{position=V3(-1.0,.26,0.0);velocity=V3(0.0,-300.0,0.0)}
  val ev=e.advance(1.0/240);assertTrue(ev.any{it is SimulationEvent.Collided});assertEquals(ExperimentPhase.FALLING,e.phase)
 }
}
