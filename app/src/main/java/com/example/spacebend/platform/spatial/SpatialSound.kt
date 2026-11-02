package com.example.spacebend.platform.spatial

import android.util.Log
import com.example.spacebend.domain.model.*
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.audio.AudioPlayerController
import com.pico.spatial.core.ecs.audio.Directivity
import com.pico.spatial.core.ecs.audio.DistanceAttenuationMode
import com.pico.spatial.core.ecs.resource.AudioResource

/** AudioResource is cached once; controllers and emitter Entities are scene-owned. */
internal class SpatialSound(private val root: Entity) {
 private data class Voice(val entity: Entity,val player: AudioPlayerController,val channel: String,val loop: Boolean)
 private val resources=mutableMapOf<String,AudioResource>()
 private val voices=mutableMapOf<String,Voice>()
 private var serial=0L
 private fun volume(channel: String,s: GameSettings)=when(channel){"music"->s.music;"voice"->s.voice;else->s.effects}
 fun play(path: String,position: V3?,settings: GameSettings,loop: Boolean=false,key: String?=null,channel: String="effects") {
  val id=key?:"one_${serial++}"
  if(voices.containsKey(id))return
  try{
   // Native emitter disposal can invalidate a shared SDK handle; never reuse a stale handle.
   val resource=resources[path]?.takeIf{it.valid}?:AudioResource.load("SpaceBendAudio_${serial++}","asset://$path").also{resources[path]=it}
   if(!resource.valid){Log.e("SpaceBend","Invalid audio: $path");return}
   val emitter=Entity();emitter.setName("Audio_$id")
   if(position==null)emitter.components.set(AmbientAudioComponent(1f))
   else {emitter.components.set(ObjectAudioComponent(1f,Directivity(0f,1f),DistanceAttenuationMode.INVERSE_SQUARED));emitter.components[TransformComponent::class.java]?.setPosition(position.sdk())}
   root.addChild(emitter)
   val player=emitter.prepareAudio(resource);player.setLoop(loop);player.setVolume(volume(channel,settings));player.play()
   voices[id]=Voice(emitter,player,channel,loop)
  }catch(e: Exception){Log.e("SpaceBend","Spatial audio failed: $path",e)}
 }
 fun stop(key: String){voices.remove(key)?.let{it.player.stop();it.player.close();it.entity.destroy()}}
 fun move(key: String,point: V3){voices[key]?.entity?.components?.get(TransformComponent::class.java)?.setPosition(point.sdk())}
 fun tick(settings: GameSettings){
  voices.values.forEach{it.player.setVolume(volume(it.channel,settings))}
  voices.filterValues{!it.loop&&it.player.isCompleted()}.keys.toList().forEach{stop(it)}
 }
 fun pause(){voices.values.forEach{it.player.pause()}}
 fun resume(){voices.values.forEach{it.player.resume()}}
 fun close(){voices.keys.toList().forEach{stop(it)};resources.values.forEach{it.close()};resources.clear()}
}
