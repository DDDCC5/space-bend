package com.example.spacebend.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Approved style A brand tokens; native PicoTheme ColorScheme is intentionally unchanged. */
object LabColors {
 val Graphite=Color(0xFF101820) // design-style: fixed-figma-color approved assets/style-a/ui/tokens.json
 val Panel=Color(0xFF18242E) // design-style: fixed-figma-color approved assets/style-a/ui/tokens.json
 val Ice=Color(0xFF9CE5F4) // design-style: fixed-figma-color approved assets/style-a/ui/tokens.json
 val Text=Color(0xFFEAF2F7) // design-style: fixed-figma-color approved assets/style-a/ui/tokens.json
 val Muted=Color(0xFF93A9B8) // design-style: fixed-figma-color approved assets/style-a/ui/tokens.json
 val Amber=Color(0xFFE9B66E) // design-style: fixed-figma-color approved assets/style-a/ui/tokens.json
 val Danger=Color(0xFFF19A91) // design-style: fixed-figma-color approved style A failure state
 val Control=Color(0xFF263E4A) // design-style: fixed-figma-color approved control-library.svg
}
class ChamferShape(private val cutDp: Float=12f): Shape {
 override fun createOutline(size: Size,layoutDirection: LayoutDirection,density: Density): Outline {
  val c=with(density){cutDp.dp.toPx()}.coerceAtMost(minOf(size.width,size.height)*.4f)
  return Outline.Generic(Path().apply{moveTo(c,0f);lineTo(size.width,0f);lineTo(size.width,size.height-c);lineTo(size.width-c,size.height);lineTo(0f,size.height);lineTo(0f,c);close()})
 }
}
