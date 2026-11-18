package com.example.spacebend.platform.spatial

import com.example.spacebend.domain.model.*

/** Geometry is anchor-local: pose refinement must not invalidate a mesh. */
internal class PlaneVisualPlan {
 private var desired=emptyMap<String,Surface>()
 private val built=mutableMapOf<String,List<V2>>()
 private val pending=linkedMapOf<String,Surface>()
 fun update(surfaces: List<Surface>): Set<String> {
  desired=surfaces.associateBy{it.id}
  val removed=built.keys-desired.keys
  removed.forEach{built.remove(it)}
  pending.keys.retainAll(desired.keys)
  for(s in surfaces){
   if(sameOutline(built[s.id],s.boundary))pending.remove(s.id)else pending[s.id]=s
  }
  return removed
 }
 fun surface(id: String)=desired[id]
 fun nextBuild(): Surface? = pending.entries.firstOrNull()?.let{(id,s)->pending.remove(id);s}
 fun markBuilt(surface: Surface){built[surface.id]=surface.boundary.toList()}
 fun clear(){desired=emptyMap();built.clear();pending.clear()}
 private fun sameOutline(a: List<V2>?,b: List<V2>): Boolean {
  if(a==null||a.size!=b.size)return false
  if(a==b)return true
  // Millimetre-scale anchor tessellation jitter is not a new preview outline.
  // Authoritative collision/drawing surfaces always use the unmodified SDK data.
  val tolerance=.01
  val start=b.indices.minByOrNull{(b[it]-a[0]).length()}?:return true
  return a.indices.all{(a[it]-b[(start+it)%b.size]).length()<=tolerance}||
   a.indices.all{(a[it]-b[Math.floorMod(start-it,b.size)]).length()<=tolerance}
 }
}
