package com.example.spacebend
import com.example.spacebend.ui.game.GameScreen
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultStage
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
fun mainApp(scope: SpatialAppScope)=with(scope){DefaultStage{PicoTheme{GameScreen()}}}
