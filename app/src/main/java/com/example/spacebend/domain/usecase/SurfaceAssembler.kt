package com.example.spacebend.domain.usecase

import com.example.spacebend.domain.model.*
import kotlin.math.roundToLong

/** Keeps anchor-local axes stable even when tessellation and triangle ordering change. */
class SurfaceAssembler {
 private val bases=mutableMapOf<String,Pair<V3,V3>>()
 fun remove(id: String){bases.remove(id)}
 fun update(id: String,vertices: List<V3>,indices: List<Int>,origin: V3,orientation: Orientation,semantic: String="UNKNOWN"): Surface? {
  if(vertices.size<3)return null
  val unique=mutableListOf<V3>();val lookup=mutableMapOf<Triple<Long,Long,Long>,Int>()
  val remap=vertices.map{v->lookup.getOrPut(Triple((v.x*100000).roundToLong(),(v.y*100000).roundToLong(),(v.z*100000).roundToLong())){unique+=v;unique.lastIndex}}
  val triangles=indices.mapNotNull{remap.getOrNull(it)}
  if(triangles.size!=indices.size)return null
  val basis=bases.getOrPut(id){
   val k=(triangles.indices step 3).firstOrNull{i->i+2<triangles.size&&(unique[triangles[i+1]]-unique[triangles[i]]).cross(unique[triangles[i+2]]-unique[triangles[i]]).length()>1e-8}?:return null
   val u=(unique[triangles[k+1]]-unique[triangles[k]]).normalized();var n=u.cross(unique[triangles[k+2]]-unique[triangles[k]]).normalized()
   if(orientation.rotate(n).y<-.5)n=-n
   u to n.cross(u).normalized()
  }
  val u=orientation.rotate(basis.first).normalized();val v=orientation.rotate(basis.second).normalized()
  val loops=loops(unique.size,triangles).map{loop->loop.map{idx->val p=unique[idx];V2(p.dot(basis.first),p.dot(basis.second))}}.filter{Polygon.isSimple(it)}
  val outer=loops.maxByOrNull{Polygon.area(it)}?:return null
  val holes=loops.filter{it!==outer&&Polygon.contains(outer,Polygon.centroid(it))}
  return Surface(id,origin,u,v,outer,semantic,holes)
 }
 private fun loops(size: Int,indices: List<Int>): List<List<Int>> {
  val counts=mutableMapOf<Pair<Int,Int>,Int>()
  for(i in indices.indices step 3){if(i+2>=indices.size)break
   for(j in 0..2){val a=indices[i+j];val b=indices[i+(j+1)%3];if(a !in 0 until size||b !in 0 until size)return emptyList();val key=minOf(a,b) to maxOf(a,b);counts[key]=(counts[key]?:0)+1}
  }
  val unused=counts.filterValues{it==1}.keys.toMutableSet();val links=mutableMapOf<Int,MutableList<Int>>()
  unused.forEach{(a,b)->links.getOrPut(a){mutableListOf()}.add(b);links.getOrPut(b){mutableListOf()}.add(a)}
  val result=mutableListOf<List<Int>>()
  while(unused.isNotEmpty()){
   val start=unused.first().first;var current=start;var prev=-1;val loop=mutableListOf(start);var closed=false
   val limit=unused.size+1
   for(step in 0..limit){
    val next=links[current]?.firstOrNull{it!=prev&&(minOf(it,current) to maxOf(it,current)) in unused}?:break
    unused.remove(minOf(current,next) to maxOf(current,next));if(next==start){closed=true;break}
    loop+=next;prev=current;current=next
   }
   if(closed&&loop.size>=3)result+=loop
   if(!closed&&unused.isNotEmpty())unused.remove(unused.first())
  };return result
 }
}
