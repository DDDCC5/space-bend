package com.example.spacebend.platform.spatial
import com.pico.spatial.core.ecs.Component
import com.pico.spatial.core.ecs.EntityQueryCondition
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
internal class GameRuntimeComponent(val runtime: SceneRuntime): Component() {
 override fun clone(): Component=GameRuntimeComponent(runtime)
}
class GameFrameSystem: System() {
 private var diagnosticAt=0L
 private val query=EntityQueryCondition.hasComponent(GameRuntimeComponent::class.java)
 override fun update(context: SceneUpdateContext) {
  val entities=context.scene.queryEntity(query)
  val now=android.os.SystemClock.uptimeMillis()
  if(now-diagnosticAt>10000){diagnosticAt=now;android.util.Log.i("SpaceBend","ECS update: runtimes=${entities.size} delta=${context.deltaTime}")}
  entities.forEach{it.components[GameRuntimeComponent::class.java]?.runtime?.frame(context.deltaTime.toDouble())}
 }
}
