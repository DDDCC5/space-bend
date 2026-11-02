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
import java.util.concurrent.ConcurrentLinkedQueue

internal class PlaneSource(private val root: Entity,private val scope: CoroutineScope) {
 private data class Change(val id: String,val removed: Boolean,val localVertices: List<V3>,val indices: List<Int>,val semantic: String,val position: V3=V3.ZERO,val rotation: Quat=Quat())
 private val pending=ConcurrentLinkedQueue<Change>()
 private val planes=linkedMapOf<String,Surface>()
 private val assembler=SurfaceAssembler()
 private var stopSubscription: (() -> Unit)?=null
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
   scope.launch{try{PlaneTrackingManager.loadAllAnchors().forEach{enqueue(it)}}catch(e: Exception){Log.w("SpaceBend","Plane load failed",e);error=e.message}}
  } catch(e: Exception){error=e.message;Log.e("SpaceBend","Plane tracking could not start",e)}
 }
 private fun enqueue(anchor: PlaneAnchor){
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
 fun stop(){stopSubscription?.invoke();stopSubscription=null;try{PlaneTrackingManager.stop()}catch(e: Exception){Log.w("SpaceBend","Plane stop",e)}}
}
