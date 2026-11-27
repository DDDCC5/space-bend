package com.example.spacebend.domain.usecase

import com.example.spacebend.domain.model.*
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon as JtsPolygon
import kotlin.math.abs
import kotlin.math.cos

/** Exact area union, never a convex hull: gaps, concavities and holes stay non-interactive. */
class SurfaceConsolidator {
 private val factory=GeometryFactory()
 fun merge(input: List<Surface>,preferredIds: Set<String> = emptySet()): List<Surface> {
  val result=mutableListOf<Surface>()
  for(surface in input.sortedWith(compareBy<Surface>{if(it.id in preferredIds)0 else 1}.thenBy{it.id})){
   result+=surface
   // Revisit earlier pieces when a new piece connects two previously disjoint regions.
   var joined=true
   while(joined){
    joined=false
    outer@for(i in result.indices)for(j in i+1 until result.size){
     val a=result[i];val b=result[j]
     // Two portal-bearing anchors must retain independent identities on an explicit rescan.
     if(a.id in preferredIds&&b.id in preferredIds)continue
     val union=union(a,b)?:continue
     result[i]=union;result.removeAt(j);joined=true;break@outer
    }
   }
  }
  return result
 }
 private fun union(a: Surface,b: Surface): Surface? {
  if(abs(a.normal.dot(b.normal))<cos(Math.toRadians(1.0)))return null
  if(a.semantic!=b.semantic&&a.semantic!="UNKNOWN"&&b.semantic!="UNKNOWN")return null
  if(b.boundary.any{abs(a.signedDistance(b.world(it)))>.015}||a.boundary.any{abs(b.signedDistance(a.world(it)))>.015})return null
  return try {
   val pa=polygon(a,a);val pb=polygon(b,a)
   if(!pa.isValid||!pb.isValid||!pa.envelopeInternal.intersects(pb.envelopeInternal))return null
   val united=pa.union(pb) as? JtsPolygon?:return null
   if(united.isEmpty)return null
   fun points(coordinates: Array<Coordinate>)=coordinates.dropLast(1).map{V2(it.x,it.y)}
   val boundary=points(united.exteriorRing.coordinates)
   val holes=(0 until united.numInteriorRing).map{points(united.getInteriorRingN(it).coordinates)}
   if(!Polygon.isSimple(boundary)||holes.any{!Polygon.isSimple(it)})null else a.copy(boundary=boundary,holes=holes)
  }catch(_: IllegalArgumentException){null}catch(_: org.locationtech.jts.geom.TopologyException){null}
 }
 private fun polygon(surface: Surface,basis: Surface): JtsPolygon {
  fun ring(points: List<V2>)=factory.createLinearRing((points+points.first()).map{val p=basis.project(surface.world(it));Coordinate(p.x,p.y)}.toTypedArray())
  return factory.createPolygon(ring(surface.boundary),surface.holes.map(::ring).toTypedArray())
 }
}
