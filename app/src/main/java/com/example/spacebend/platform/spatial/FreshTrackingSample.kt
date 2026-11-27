package com.example.spacebend.platform.spatial

/** Late callbacks from stopped providers must never place a newly resumed workspace. */
internal class FreshTrackingSample<T> {
 private var generation=0L
 @Volatile private var sample: T?=null
 @Synchronized fun begin(): Long {generation++;sample=null;return generation}
 @Synchronized fun offer(token: Long,value: T){if(token==generation)sample=value}
 fun latest(): T?=sample
}
