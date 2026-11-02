package com.example.spacebend.domain.model

enum class GameLocale(val textFolder: String,val voiceFolder: String,val label: String) {
    SIMPLIFIED("zh-Hans","zh-Hans","简体中文"), TRADITIONAL("zh-Hant","zh-Hans","繁體中文"), ENGLISH("en","en","English")
}
enum class PayloadKind(val asset: String,val textKey: String,val massKg: Double,val launchSpeed: Double,val radius: Double) {
    ROBOT("exploration_robot","robot",2.4,2.8,0.390967), CORE("energy_core","core",1.2,3.6,0.228435),
    CRAFT("spacecraft","craft",1.8,4.0,0.391893), PROBE("micro_probe","probe",.3,4.8,0.264023),
    CAPSULE("heavy_capsule","capsule",8.0,1.8,0.386920), BEACON("beacon","beacon",.8,3.2,0.282809)
}
data class Record(val stars: Int,val bestSeconds: Double) {
    fun merge(newStars: Int,newSeconds: Double)=Record(maxOf(stars,newStars),minOf(bestSeconds,newSeconds))
}
data class Mission(val id: Int,val tier: Int,val maxPortals: Int,val recommendedPortals: Int,val optimalPortals: Int,val limitSeconds: Double,val optimalSeconds: Double,val allowed: Set<PayloadKind>,val titleKey: String)
data class Portal(val id: Long,val surfaceId: String,val outline: List<V2>,val order: Int) {
    val center: V2 get()=Polygon.centroid(outline)
}
data class Payload(val kind: PayloadKind,var position: V3,var velocity: V3,var expectedEntry: Int=0,var traversals: Int=0,var loops: Int=0,var lastExitId: Long?=null,var isFalling: Boolean=false)
enum class ExperimentPhase { PLANNING, RUNNING, STABLE, FALLING, FAILED }
enum class GameMode { CHALLENGE, SANDBOX }
data class GameSettings(val locale: GameLocale=GameLocale.SIMPLIFIED,val music: Float=.45f,val effects: Float=.7f,val voice: Float=.8f,val haptics: Boolean=true,val tutorialDone: Boolean=false)
sealed interface SimulationEvent {
    data class Transferred(val source: Long,val destination: Long,val speed: Double): SimulationEvent
    data class Collided(val point: V3): SimulationEvent
    data object Stable: SimulationEvent
    data object Failed: SimulationEvent
}
