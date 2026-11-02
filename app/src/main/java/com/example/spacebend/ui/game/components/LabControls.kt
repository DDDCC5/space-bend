package com.example.spacebend.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebend.ui.theme.*
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.haptic.controllerHapticFeedback
import com.pico.spatial.ui.foundation.hover.spatialHoverEffect
import com.pico.spatial.ui.platform.HandControllerHapticType
import kotlin.math.*

val LocalLabHaptics=staticCompositionLocalOf {true}

// User explicitly requires custom chamfered controls; rounded native buttons do not fit the accepted design.
@Composable
fun LabButton(label: String,onClick: ()->Unit,modifier: Modifier=Modifier,enabled: Boolean=true,selected: Boolean=false) {
 val interactions=remember{MutableInteractionSource()};val shape=remember{ChamferShape()}
 Box(modifier.defaultMinSize(minWidth=110.dp,minHeight=48.dp).clip(shape)
  .background(if(selected)LabColors.Control else LabColors.Panel)
  .spatialHoverEffect(enabled=enabled)
  .controllerHapticFeedback(type=if(LocalLabHaptics.current)HandControllerHapticType.Press else HandControllerHapticType.None,interactionSource=interactions)
  .clickable(interactionSource=interactions,indication=LocalIndication.current,enabled=enabled,role=Role.Button,onClick=onClick)
  .semantics{contentDescription=label}.padding(horizontal=16.dp,vertical=12.dp),contentAlignment=Alignment.CenterStart) {
  Text(label,color=if(enabled)LabColors.Text else LabColors.Muted.copy(alpha=.45f),fontSize=17.sp,maxLines=2)
 }
}
@Composable
fun LabStars(earned: Int,modifier: Modifier=Modifier) {
 Canvas(modifier.size(160.dp,48.dp).semantics{contentDescription="$earned / 3"}){
  val r=size.height*.42f
  for(j in 0..2){val cx=size.width*(j+.5f)/3;val cy=size.height*.5f
   val path=Path();for(i in 0..9){val a=-PI/2+i*PI/5;val rr=if(i%2==0)r else r*.44f;val x=cx+cos(a).toFloat()*rr;val y=cy+sin(a).toFloat()*rr;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)};path.close();drawPath(path,if(j<earned)LabColors.Amber else LabColors.Muted.copy(alpha=.25f))
  }
 }
}
@Composable
fun LabMeter(label: String,value: Float,onChange: (Float)->Unit,modifier: Modifier=Modifier) {
 val callback by rememberUpdatedState(onChange)
 Column(modifier.semantics{contentDescription=label;progressBarRangeInfo=ProgressBarRangeInfo(value,0f..1f);setProgress{callback(it.coerceIn(0f,1f));true}}){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=LabColors.Text,fontSize=18.sp);Text("${(value*100).roundToInt()}%",color=LabColors.Ice,fontSize=16.sp)}
  Canvas(Modifier.fillMaxWidth().height(46.dp).pointerInput(label){
   awaitEachGesture{
    val down=awaitFirstDown();callback((down.position.x/size.width).coerceIn(0f,1f));down.consume()
    do {val e=awaitPointerEvent();val c=e.changes.firstOrNull{it.id==down.id}?:break;callback((c.position.x/size.width).coerceIn(0f,1f));c.consume()}while(c.pressed)
   }
  }){
   val y=size.height/2;drawLine(LabColors.Muted.copy(alpha=.3f),Offset(0f,y),Offset(size.width,y),5.dp.toPx())
   drawLine(LabColors.Ice,Offset(0f,y),Offset(size.width*value,y),5.dp.toPx())
   drawRect(LabColors.Text,Offset((size.width*value-4.dp.toPx()).coerceIn(0f,size.width-8.dp.toPx()),y-10.dp.toPx()),androidx.compose.ui.geometry.Size(8.dp.toPx(),20.dp.toPx()))
  }
 }
}
@Composable
fun PortalDiagram(modifier: Modifier=Modifier) {
 Canvas(modifier){
  val cx=size.width*.5f;val cy=size.height*.5f;val rx=size.width*.36f;val ry=size.height*.40f
  drawOval(LabColors.Ice.copy(alpha=.10f),Offset(cx-rx,cy-ry),androidx.compose.ui.geometry.Size(rx*2,ry*2),style=Stroke(20.dp.toPx()))
  drawOval(LabColors.Ice,Offset(cx-rx,cy-ry),androidx.compose.ui.geometry.Size(rx*2,ry*2),style=Stroke(2.dp.toPx()))
  for(i in 0..15){val a=i*PI/8;val x=cx+cos(a).toFloat()*rx*1.12f;val y=cy+sin(a).toFloat()*ry*1.12f;drawLine(LabColors.Amber,Offset(x,y),Offset(cx+(x-cx)*1.04f,cy+(y-cy)*1.04f),1.dp.toPx())}
 }
}
@Composable
fun PayloadGlyph(index: Int,modifier: Modifier=Modifier) {
 Canvas(modifier){
  val cx=size.width/2;val cy=size.height/2;val r=minOf(size.width,size.height)*.34f;val stroke=Stroke(2.dp.toPx())
  when(index){
   0->{drawRect(LabColors.Muted,Offset(cx-r,cy-r*.3f),androidx.compose.ui.geometry.Size(r*2,r*.8f),style=stroke);drawLine(LabColors.Ice,Offset(cx,cy-r*.3f),Offset(cx,cy-r),3.dp.toPx());for(j in 0..4)drawCircle(LabColors.Muted,r*.13f,Offset(cx-r*.8f+j*r*.4f,cy+r*.7f),style=stroke)}
   1->{drawCircle(LabColors.Ice,r*.25f,Offset(cx,cy));drawCircle(LabColors.Muted,r,Offset(cx,cy),style=stroke);drawOval(LabColors.Muted,Offset(cx-r,cy-r*.35f),androidx.compose.ui.geometry.Size(r*2,r*.7f),style=stroke)}
   2->{val p=Path().apply{moveTo(cx,cy-r);lineTo(cx+r*.35f,cy);lineTo(cx+r,cy+r*.7f);lineTo(cx,cy+r*.4f);lineTo(cx-r,cy+r*.7f);lineTo(cx-r*.35f,cy);close()};drawPath(p,LabColors.Muted,style=stroke)}
   3->{drawRect(LabColors.Muted,Offset(cx-r*.2f,cy-r*.6f),androidx.compose.ui.geometry.Size(r*.4f,r*1.2f),style=stroke);for(s in listOf(-1,1))for(t in listOf(-1,1))drawLine(LabColors.Ice,Offset(cx,cy),Offset(cx+s*r,cy+t*r*.65f),2.dp.toPx())}
   4->{drawRect(LabColors.Muted,Offset(cx-r,cy-r*.8f),androidx.compose.ui.geometry.Size(r*2,r*1.6f),style=stroke);for(s in listOf(-1,1))drawLine(LabColors.Muted,Offset(cx+s*r*.6f,cy-r),Offset(cx+s*r*.6f,cy+r),3.dp.toPx());drawLine(LabColors.Amber,Offset(cx-r*.3f,cy),Offset(cx+r*.3f,cy),4.dp.toPx())}
   else->{drawLine(LabColors.Muted,Offset(cx,cy+r),Offset(cx,cy-r*.8f),4.dp.toPx());drawArc(LabColors.Ice,205f,130f,false,Offset(cx-r,cy-r),androidx.compose.ui.geometry.Size(r*2,r),style=stroke);drawLine(LabColors.Muted,Offset(cx-r*.8f,cy+r),Offset(cx+r*.8f,cy+r),4.dp.toPx())}
  }
 }
}
