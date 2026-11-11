package com.example.spacebend.platform.spatial

import android.util.Log
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.SurfaceAssembler
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
 private var stopSubscription: (() -> Unit)?=null
 private var snapshotJob: Job?=null
 private var anchorLogs=0
 var error: String?=null;private set
 val running: Boolean get()=PlaneTrackingManager.state==TrackingState.RUNNING
 fun start() {
  if(stopSubscription!=null)return
  try {
   val subscription=PlaneTrackingManager.subscribeAnchorUpdate {event->
    if(event.event==AnchorUpdate.Event.REMOVED)pending.add(Change(event.anchor.anchorUUID.toString(),true,emptyList(),emptyList(),""))
    else enqueue(event.anchor)
   }
   stopSubscription={subscription.cancel()};PlaneTrackingManager.start()
   Log.i("SpaceBend","Plane tracking start: state=${PlaneTrackingManager.state}")
   snapshotJob=scope.launch{repeat(10){
    try{val anchors=PlaneTrackingManager.loadAllAnchors();Log.i("SpaceBend","Plane snapshot: state=${PlaneTrackingManager.state} count=${anchors.size}");anchors.forEach{enqueue(it)}}catch(e: kotlinx.coroutines.CancellationException){throw e}catch(e: Exception){Log.w("SpaceBend","Plane load failed",e);error=e.message}
    delay(2000)
   }}
  } catch(e: Exception){error=e.message;Log.e("SpaceBend","Plane tracking could not start",e)}
 }
 private fun enqueue(anchor: PlaneAnchor){
  if(anchorLogs++<5)Log.i("SpaceBend","Plane received: vertices=${anchor.vertices.size} indices=${anchor.indices.size} semantic=${anchor.semantics}")
  val transform=anchor.transform;val rotation=transform.rotation.toQuat();val origin=transform.position.game()
  pending.add(Change(anchor.anchorUUID.toString(),false,anchor.vertices.map{it.game()},anchor.indices.toList(),anchor.semantics.toString(),origin,rotation))
 }
 fun drain(): Boolean {
  var changed=false
  while(true){val c=pending.poll()?:break
   if(c.removed){if(planes.remove(c.id)!=null)changed=true;assembler.remove(c.id);continue}
   val origin=root.convertPositionFrom(c.position.sdk(),null).game()
   val rotation=root.convertRotationFrom(c.rotation,null)
   assembler.update(c.id,c.localVertices,c.indices,origin,Orientation(rotation.x.toDouble(),rotation.y.toDouble(),rotation.z.toDouble(),rotation.w.toDouble()),c.semantic)?.let{if(planes[c.id]!=it){planes[c.id]=it;changed=true}}
  };return changed
 }
 fun surfaces()=planes.values.toList()
 fun stop(){snapshotJob?.cancel();snapshotJob=null;stopSubscription?.invoke();stopSubscription=null;try{PlaneTrackingManager.stop()}catch(e: Exception){Log.w("SpaceBend","Plane stop",e)}}
}
