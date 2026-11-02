package com.example.spacebend.platform.spatial
import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
internal class GameRuntimeComponent(val runtime: SceneRuntime): Component() {
 override fun clone(): Component=GameRuntimeComponent(runtime)
}
class GameFrameSystem: System() {
 private val query=EntityQueryCondition.hasComponent(GameRuntimeComponent::class.java)
 override fun update(context: SceneUpdateContext) {
  context.scene.queryEntity(query).forEach{it.components[GameRuntimeComponent::class.java]?.runtime?.frame(context.deltaTime.toDouble())}
 }
}
