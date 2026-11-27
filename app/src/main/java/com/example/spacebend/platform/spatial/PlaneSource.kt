package com.example.spacebend.platform.spatial

import android.util.Log
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.SurfaceAssembler
import com.example.spacebend.domain.usecase.SurfaceConsolidator
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.sense.base.AnchorUpdate
import com.pico.spatial.sense.base.TrackingState
import com.pico.spatial.sense.plane.PlaneAnchor
import com.pico.spatial.sense.plane.PlaneTrackingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue

internal class PlaneSource(private val root: Entity,private val scope: CoroutineScope) {
 private data class Change(val id: String,val removed: Boolean,val localVertices: List<V3>,val indices: List<Int>,val semantic: String,val position: V3=V3.ZERO,val rotation: Quat=Quat())
 private val pending=ConcurrentLinkedQueue<Change>()
 private val planes=linkedMapOf<String,Surface>()
 private val assembler=SurfaceAssembler()
 private val consolidator=SurfaceConsolidator()
 private var snapshot: List<Surface> = emptyList()
 var frozen=false;private set
 @Volatile private var generation=0L
 private var stopSubscription: (() -> Unit)?=null
 private var snapshotJob: Job?=null
 private var anchorLogs=0
 var error: String?=null;private set
 val running: Boolean get()=(frozen&&snapshot.isNotEmpty())||PlaneTrackingManager.state==TrackingState.RUNNING
 fun start() {
  if(stopSubscription!=null||frozen)return
  val token=++generation
  try {
   val subscription=PlaneTrackingManager.subscribeAnchorUpdate {event->
    if(generation!=token)return@subscribeAnchorUpdate
    if(event.event==AnchorUpdate.Event.REMOVED)pending.add(Change(event.anchor.anchorUUID.toString(),true,emptyList(),emptyList(),""))
    else enqueue(event.anchor)
   }
   stopSubscription={subscription.cancel()};PlaneTrackingManager.start()
   Log.i("SpaceBend","Plane tracking start: state=${PlaneTrackingManager.state}")
   snapshotJob=scope.launch{repeat(10){
    try{val anchors=PlaneTrackingManager.loadAllAnchors();Log.i("SpaceBend","Plane snapshot: state=${PlaneTrackingManager.state} count=${anchors.size}");if(generation==token)anchors.forEach{enqueue(it)}}catch(e: kotlinx.coroutines.CancellationException){throw e}catch(e: Exception){Log.w("SpaceBend","Plane load failed",e);error=e.message}
    delay(2000)
   }}
  } catch(e: Exception){error=e.message;Log.e("SpaceBend","Plane tracking could not start",e)}
 }
 private fun enqueue(anchor: PlaneAnchor){
  if(anchorLogs++<5)Log.i("SpaceBend","Plane received: vertices=${anchor.vertices.size} indices=${anchor.indices.size} semantic=${anchor.semantics}")
  val transform=anchor.transform;val rotation=transform.rotation.toQuat();val origin=transform.position.game()
  pending.add(Change(anchor.anchorUUID.toString(),false,anchor.vertices.map{it.game()},anchor.indices.toList(),anchor.semantics.toString(),origin,rotation))
 }
 fun drain(preferredIds: Set<String> = emptySet()): Boolean {
  if(frozen){pending.clear();return false}
  var changed=false
  val batch=linkedMapOf<String,Change>()
  while(true){val c=pending.poll()?:break;batch[c.id]=c}
  for(c in batch.values){
   if(c.removed){if(planes.remove(c.id)!=null)changed=true;assembler.remove(c.id);continue}
   val origin=root.convertPositionFrom(c.position.sdk(),null).game()
   val rotation=root.convertRotationFrom(c.rotation,null)
   assembler.update(c.id,c.localVertices,c.indices,origin,Orientation(rotation.x.toDouble(),rotation.y.toDouble(),rotation.z.toDouble(),rotation.w.toDouble()),c.semantic)?.let{if(planes[c.id]!=it){planes[c.id]=it;changed=true}}
  }
  if(changed){
   snapshot=consolidator.merge(planes.values.toList(),preferredIds)
   Log.i("SpaceBend","Scan geometry: raw=${planes.size} consolidated=${snapshot.size} area=${snapshot.sumOf{Polygon.area(it.boundary)-it.holes.sumOf(Polygon::area)}}")
  }
  return changed
 }
 fun restart(){
  // Keep the per-UUID basis: restarting tracking must not rotate stored portal coordinates.
  // Only a genuine REMOVED anchor event invalidates its basis.
  stop();frozen=false;pending.clear();planes.clear();snapshot=emptyList();error=null;anchorLogs=0;start()
 }
 fun surfaces()=snapshot
 fun freeze(){if(frozen||snapshot.isEmpty())return;frozen=true;stop();pending.clear();Log.i("SpaceBend","Plane snapshot locked: count=${snapshot.size}; subscriptions and polling stopped")}
 fun stop(){generation++;snapshotJob?.cancel();snapshotJob=null;stopSubscription?.invoke();stopSubscription=null;try{PlaneTrackingManager.stop()}catch(e: Exception){Log.w("SpaceBend","Plane stop",e)}}
}
