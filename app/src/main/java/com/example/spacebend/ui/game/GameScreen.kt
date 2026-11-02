package com.example.spacebend.ui.game

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spacebend.platform.spatial.*
import com.example.spacebend.ui.game.components.GamePanelContent
import com.example.spacebend.ui.navigation.GamePanel
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.dsl.registerSystem
import com.pico.spatial.ui.foundation.dsl.unregisterSystem
import com.pico.spatial.ui.platform.LocalHandControllerHaptic
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.LengthUnit
import androidx.compose.ui.unit.dp
import com.example.spacebend.domain.model.V2

@Composable
fun GameScreen() {
 val context=LocalContext.current
 val factory=remember(context){GameViewModelFactory(context.applicationContext)}
 val vm: GameViewModel=viewModel(factory=factory)
 val state by vm.state.collectAsStateWithLifecycle()
 val scope=rememberCoroutineScope()
 val haptic=LocalHandControllerHaptic.current
 val converter=LocalPhysicalLengthConverter.current
 val panelMetrics=GamePanel.entries.associateWith{id->
  V2(converter.dpToLength((if(id==GamePanel.HUD)540 else 960).dp,LengthUnit.Meters).toDouble(),converter.dpToLength((if(id==GamePanel.HUD)440 else 640).dp,LengthUnit.Meters).toDouble())
 }
 var runtime by remember{mutableStateOf<SceneRuntime?>(null)}
 val lifecycle=LocalLifecycleOwner.current.lifecycle
 SideEffect{runtime?.updatePanelMetrics(panelMetrics)}
 DisposableEffect(lifecycle){
  registerSystem<GameFrameSystem>()
  val observer=LifecycleEventObserver{_,event->when(event){Lifecycle.Event.ON_PAUSE->runtime?.pause();Lifecycle.Event.ON_RESUME->runtime?.resume();else->{}}}
  lifecycle.addObserver(observer)
  onDispose{lifecycle.removeObserver(observer);runtime?.close();runtime=null;unregisterSystem<GameFrameSystem>()}
 }
 SpatialView(
  modifier=Modifier.fillMaxSize(),
  attachments={GamePanel.entries.forEach{id->AttachmentPanel(id){GamePanelContent(id,state,vm::onEvent)}}},
  initial={content,attachments->
   val root=Entity();content.addEntity(root)
   val scene=SceneRuntime(context.applicationContext,vm,root,scope,haptic,panelMetrics);runtime=scene
   GamePanel.entries.forEach{id->attachments.entity(id)?.let{scene.attachPanel(id,it)}}
   try{scene.load();if(!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))scene.pause()}
   catch(e: Exception){Log.e("SpaceBend","Scene initialization failed",e);vm.notify("asset_error")}
  },
 )
}
