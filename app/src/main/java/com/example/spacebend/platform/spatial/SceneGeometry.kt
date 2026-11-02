package com.example.spacebend.platform.spatial

import com.example.spacebend.domain.model.*
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import kotlin.math.*

/** Scene-lifetime registry. Shared resources are closed only after all owning entities are destroyed. */
internal class SceneGeometry {
 val ice=UnlitMaterial.create().apply{setBaseColor(Color4(.61f,.90f,.96f,1f))}
 val halo=UnlitMaterial.create().apply{setBaseColor(Color4(.20f,.48f,.60f,.22f));setBlendingMode(BlendingMode.TRANSPARENT);setDepthWrite(false)}
 val amber=UnlitMaterial.create().apply{setBaseColor(Color4(.91f,.71f,.43f,1f))}
 val planeMaterial=UnlitMaterial.create().apply{setBaseColor(Color4(.3f,.55f,.64f,.35f));setBlendingMode(BlendingMode.TRANSPARENT);setDepthWrite(false)}
 val dotMesh=MeshResource.createSphere(.006f)
 val beamMesh=MeshResource.createBox(Vector3(.0015f,.0015f,1f))
 fun trace(points: List<V3>,width: Double,closed: Boolean,material: UnlitMaterial): Pair<Entity,MeshResource>? {
  if(points.size<2)return null
  val vertices=mutableListOf<Vector3>();val indices=mutableListOf<Int>()
  val segments=if(closed)points.size else points.size-1
  for(i in 0 until segments){
   val a=points[i];val b=points[(i+1)%points.size];val along=(b-a).normalized()
   if((b-a).length()<1e-6)continue
   val ref=if(abs(along.y)<.9)V3(0.0,1.0,0.0) else V3(1.0,0.0,0.0)
   val u=along.cross(ref).normalized()*width;val v=along.cross(u).normalized()*width
   val start=vertices.size
   for(p in listOf(a,b))for(k in 0..3){val angle=k*PI/2;vertices+=(p+u*cos(angle)+v*sin(angle)).sdk()}
   for(k in 0..3){val next=(k+1)%4;indices+=listOf(start+k,start+next,start+4+k,start+next,start+4+next,start+4+k)}
  }
  if(vertices.isEmpty())return null
  val min=Vector3(vertices.minOf{it.x},vertices.minOf{it.y},vertices.minOf{it.z});val max=Vector3(vertices.maxOf{it.x},vertices.maxOf{it.y},vertices.maxOf{it.z})
  val bounds=BoundingBox((min+max)*.5f,(max-min)*.5f)
  val mesh=MeshResource.createWithMeshModel(MeshModel(positions=vertices,triangleIndices=indices),bounds,"SpaceBendTrace")
  return ModelEntity(mesh,material) to mesh
 }
 fun close(){dotMesh.close();beamMesh.close();ice.close();halo.close();amber.close();planeMaterial.close()}
}
internal class PortalVisual(val portal: Portal,var surface: Surface,private val geometry: SceneGeometry) {
 val root=Entity()
 private val meshes=mutableListOf<MeshResource>()
 private val particles=mutableListOf<Entity>()
 private var elapsed=0.0
 private var center=surface.world(portal.center)
 init {
  root.setName("Portal_${portal.order}_${portal.id}")
  updateSurface(surface)
  val points=portal.outline.map{V3(it.x-portal.center.x,it.y-portal.center.y,.008)}
  geometry.trace(points,.004,true,geometry.ice)?.let{root.addChild(it.first);meshes+=it.second}
  geometry.trace(points,.012,true,geometry.halo)?.let{root.addChild(it.first);meshes+=it.second}
  repeat(12){i->val e=ModelEntity(geometry.dotMesh,geometry.ice);e.setName("InwardParticle_$i");root.addChild(e);particles+=e}
 }
 fun updateSurface(updated: Surface){
  surface=updated;center=updated.world(portal.center)
  root.components[TransformComponent::class.java]?.apply{setPosition(center.sdk());setQuaternion(basisRotation(updated.u,updated.v,updated.normal))}
 }
 fun update(dt: Double,collapse: Double) {
  elapsed+=dt
  val appear=(elapsed/.35).coerceIn(.001,1.0);val size=(appear*(1-collapse)).coerceAtLeast(.001).toFloat()
  root.components[TransformComponent::class.java]?.setScaleVector(Vector3(size))
  for(i in particles.indices){
   val phase=(elapsed*.5+i/particles.size.toDouble())%1.0
   val point=portal.outline[(i*portal.outline.size/particles.size).coerceAtMost(portal.outline.lastIndex)]
   val edge=V3(point.x-portal.center.x,point.y-portal.center.y,0.0)
   val local=edge*(1-phase)+V3(0.0,0.0,.13*(1-phase))
   particles[i].components[TransformComponent::class.java]?.apply{setPosition(local.sdk());setScaleVector(Vector3((sin(phase*PI)*.7).toFloat().coerceAtLeast(.01f)))}
  }
 }
 fun close(){root.destroy();meshes.forEach{it.close()}}
}
