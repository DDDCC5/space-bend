package com.example.spacebend.domain.usecase

import com.example.spacebend.domain.model.*
import kotlin.math.abs

/** Panel pose in its scene owner's coordinates, captured once from a valid head pose. */
data class WorkspacePose(val position: V3,val right: V3,val normal: V3,val up: V3=V3(0.0,1.0,0.0)) {
 data class Hit(val point: V3,val distance: Double,val header: Boolean)
 fun hit(ray: Ray,half: V2): Hit? {
  val denominator=ray.direction.dot(normal)
  if(abs(denominator)<1e-8)return null
  val distance=(position-ray.origin).dot(normal)/denominator
  if(distance<0)return null
  val point=ray.origin+ray.direction*distance;val delta=point-position
  if(abs(delta.dot(right))>half.x||abs(delta.dot(up))>half.y)return null
  return Hit(point,distance,delta.dot(up)>half.y*.65)
 }
 companion object {
  fun inFront(head: V3,forward: V3,headUp: V3=V3(0.0,1.0,0.0)): WorkspacePose? {
   if(!listOf(head.x,head.y,head.z,forward.x,forward.y,forward.z,headUp.x,headUp.y,headUp.z).all{it.isFinite()})return null
   val look=forward.normalized();val normal=-look;val right=headUp.cross(normal).normalized()
   if(right.length()<.1)return null
   return WorkspacePose(head+look*1.45,right,normal,normal.cross(right).normalized())
  }
 }
}
