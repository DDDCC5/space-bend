package com.example.spacebend.domain.usecase
import com.example.spacebend.domain.model.*
import kotlin.math.*

data class SweepContact(val fraction: Double,val center: V3,val normal: V3)
/** Continuous sphere vs finite planar polygon, including its edge capsules and corner spheres. */
object SweepCollision {
 fun firstContact(surface: Surface,from: V3,to: V3,radius: Double): SweepContact? {
  val travel=to-from;var best=Double.POSITIVE_INFINITY;var normal=surface.normal
  fun accept(t: Double,n: V3){if(t>=-1e-9&&t<=1+1e-9&&t<best){best=t.coerceIn(0.0,1.0);normal=n.normalized()}}
  val d0=surface.signedDistance(from);val d1=surface.signedDistance(to)
  if(min(d0,d1)>radius||max(d0,d1)<-radius)return null
  if(abs(d0)<=radius&&surface.contains(surface.project(from)))accept(0.0,surface.normal*if(d0>=0)1.0 else -1.0)
  if(abs(d1-d0)>1e-12)for(side in listOf(-1.0,1.0)){
   val t=(side*radius-d0)/(d1-d0)
   if(t in 0.0..1.0){val p=from+travel*t;if(surface.contains(surface.project(p)))accept(t,surface.normal*side)}
  }
  val polygons=listOf(surface.boundary)+surface.holes
  for(poly in polygons)for(i in poly.indices){
   val a=surface.world(poly[i]);val b=surface.world(poly[(i+1)%poly.size]);val edge=b-a;val e2=edge.dot(edge)
   if(e2<1e-15)continue
   val offset=from-a;val projection=(offset.dot(edge)/e2).coerceIn(0.0,1.0)
   val closest=a+edge*projection
   if(from.distance(closest)<=radius)accept(0.0,(from-closest).let{if(it.length()<1e-8)surface.normal else it})
   val edgeTravel=edge.dot(travel);val edgeOffset=edge.dot(offset)
   val aa=e2*travel.dot(travel)-edgeTravel*edgeTravel
   val bb=e2*offset.dot(travel)-edgeOffset*edgeTravel
   val cc=e2*(offset.dot(offset)-radius*radius)-edgeOffset*edgeOffset
   val disc=bb*bb-aa*cc
   if(aa>1e-15&&disc>=0){val t=(-bb-sqrt(disc))/aa;val y=edgeOffset+t*edgeTravel
    if(t in 0.0..1.0&&y in 0.0..e2){val center=from+travel*t;accept(t,center-(a+edge*(y/e2)))}
   }
   val va=travel.dot(travel);val vb=offset.dot(travel);val vc=offset.dot(offset)-radius*radius;val vd=vb*vb-va*vc
   if(va>1e-15&&vd>=0){val t=(-vb-sqrt(vd))/va;if(t in 0.0..1.0)accept(t,from+travel*t-a)}
  }
  return if(best.isFinite())SweepContact(best,from+travel*best,normal) else null
 }
}
