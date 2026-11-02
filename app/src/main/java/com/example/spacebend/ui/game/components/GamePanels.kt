package com.example.spacebend.ui.game.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebend.domain.model.*
import com.example.spacebend.domain.usecase.Missions
import com.example.spacebend.ui.game.*
import com.example.spacebend.ui.navigation.GamePanel
import com.example.spacebend.ui.theme.*
import com.pico.spatial.ui.design.Text
import java.util.Locale

@Composable
fun GamePanelContent(panel: GamePanel,state: GameUiState,onEvent: (GameEvent)->Unit) {
 CompositionLocalProvider(LocalLabHaptics provides state.settings.haptics){
  val titleKey=when(panel){GamePanel.MENU->"app";GamePanel.SCAN->"scan";GamePanel.LEVELS->"missions";GamePanel.PICKER->"select";GamePanel.RESULT->if(state.phase==ExperimentPhase.STABLE)"success" else "failure";GamePanel.SETTINGS->"settings";GamePanel.TUTORIAL->"tutorial";GamePanel.HUD->"lab"}
  val compact=panel==GamePanel.HUD
  // design-style: opaque-root user explicitly approved the custom chamfered engineering panel, not native glass.
  Column(Modifier.width(if(compact)540.dp else 960.dp).height(if(compact)440.dp else 640.dp)
   .clip(ChamferShape(24f)).background(LabColors.Graphite).padding(32.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     Text("SB / GRAVITY LABORATORY",color=LabColors.Ice,fontSize=12.sp)
     Spacer(Modifier.height(12.dp));Text(state.text(titleKey),color=if(panel==GamePanel.RESULT&&state.phase!=ExperimentPhase.STABLE)LabColors.Danger else LabColors.Text,fontSize=if(compact)25.sp else 30.sp,maxLines=2)
    }
    if(panel!=GamePanel.SCAN&&panel!=GamePanel.HUD)LabButton(state.text("close"),{onEvent(GameEvent.Close(panel))},Modifier.width(105.dp))
   }
   Spacer(Modifier.height(12.dp))
   Text(state.text("panel_drag"),color=LabColors.Muted,fontSize=12.sp)
   Spacer(Modifier.height(18.dp))
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
    when(panel){
     GamePanel.MENU->MainMenu(state,onEvent)
     GamePanel.SCAN->ScanPanel(state,onEvent)
     GamePanel.LEVELS->LevelPanel(state,onEvent)
     GamePanel.PICKER->ObjectPanel(state,onEvent)
     GamePanel.RESULT->ResultPanel(state,onEvent)
     GamePanel.SETTINGS->SettingsPanel(state,onEvent)
     GamePanel.TUTORIAL->TutorialPanel(state,onEvent)
     GamePanel.HUD->HudPanel(state,onEvent)
    }
   }
  }
 }
}
@Composable
private fun MainMenu(s: GameUiState,event: (GameEvent)->Unit) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){
   Text(s.text("lab"),color=LabColors.Text,fontSize=40.sp)
   Spacer(Modifier.height(14.dp));Text("FIELD / 9.8 m·s⁻²",color=LabColors.Muted,fontSize=16.sp)
   Spacer(Modifier.height(28.dp))
   LabButton(s.text("challenge"),{event(GameEvent.Show(GamePanel.LEVELS))},Modifier.fillMaxWidth(),selected=true)
   Spacer(Modifier.height(14.dp));LabButton(s.text("sandbox"),{event(GameEvent.Sandbox)},Modifier.fillMaxWidth())
   Spacer(Modifier.height(14.dp));LabButton(s.text("tutorial"),{event(GameEvent.Show(GamePanel.TUTORIAL))},Modifier.fillMaxWidth())
   Spacer(Modifier.height(22.dp));LabButton(s.text("settings"),{event(GameEvent.Show(GamePanel.SETTINGS))},Modifier.width(180.dp))
  }
  Spacer(Modifier.width(50.dp));PortalDiagram(Modifier.width(330.dp).height(360.dp))
 }
}
@Composable
private fun ScanPanel(s: GameUiState,event: (GameEvent)->Unit) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){
   Text(if(s.tracking)s.text("scan_ready")else s.text("scanning"),color=LabColors.Ice,fontSize=28.sp)
   Spacer(Modifier.height(18.dp));Text(s.text("v_scan"),color=LabColors.Text,fontSize=21.sp)
   Spacer(Modifier.height(20.dp));Text(s.text("scan_real_only"),color=LabColors.Muted,fontSize=16.sp)
   Spacer(Modifier.height(18.dp));Text(s.text(s.statusKey),color=LabColors.Amber,fontSize=16.sp)
   Spacer(Modifier.height(26.dp));Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
    LabButton(s.text("scan_retry"),{event(GameEvent.Scan)})
    LabButton(s.text("continue"),{event(GameEvent.ContinueScan)},enabled=s.scanAvailable&&s.planeCount>0&&s.sceneReady,selected=true)
   }
  }
  Column(Modifier.width(220.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text("${s.planeCount}",color=LabColors.Text,fontSize=100.sp)
   Text(s.text("detected_surfaces"),color=LabColors.Muted,fontSize=16.sp)
  }
 }
}
@Composable
private fun LevelPanel(s: GameUiState,event: (GameEvent)->Unit) {
 for(tier in 1..4){
  Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
   Column(Modifier.width(158.dp)){Text("0$tier",color=LabColors.Ice,fontSize=22.sp);Text(s.text("tier$tier"),color=LabColors.Text,fontSize=17.sp)}
   Missions.all.filter{it.tier==tier}.forEach{m->
    val unlocked=Missions.unlocked(m.id,s.records);val record=s.records[m.id]
    Column(Modifier.weight(1f)){
     LabButton(String.format(Locale.US,"%02d / %s",m.id,s.text(m.titleKey)),{event(GameEvent.ChooseMission(m.id))},Modifier.fillMaxWidth(),enabled=unlocked,selected=m.id==s.missionId)
     Spacer(Modifier.height(5.dp));LabStars(record?.stars?:0,Modifier.height(22.dp))
     Text(if(unlocked) record?.let{formatTime(it.bestSeconds)}?:s.text("no_record") else s.text("locked"),color=LabColors.Muted,fontSize=12.sp)
    }
   }
  }
 }
}
@Composable
private fun ObjectPanel(s: GameUiState,event: (GameEvent)->Unit) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  PayloadKind.entries.forEachIndexed{i,k->
   val allowed=s.mode==GameMode.SANDBOX||k in Missions.all[s.missionId-1].allowed
   Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
    PayloadGlyph(i,Modifier.size(90.dp))
    LabButton(s.text(k.textKey),{event(GameEvent.Select(k))},Modifier.fillMaxWidth(),enabled=allowed,selected=s.selected==k)
   }
  }
 }
 Spacer(Modifier.height(24.dp));Text(s.text(s.selected.textKey),color=LabColors.Text,fontSize=27.sp)
 Spacer(Modifier.height(12.dp));Text("${s.text("mass")}  ${s.selected.massKg} kg   /   ${s.text("velocity")}  ${s.selected.launchSpeed} m/s",color=LabColors.Muted,fontSize=20.sp)
 Spacer(Modifier.height(10.dp));Text(s.text("fit_hint"),color=LabColors.Muted,fontSize=16.sp)
 Spacer(Modifier.height(20.dp));Text(s.text("object_hint"),color=LabColors.Muted,fontSize=15.sp)
 Spacer(Modifier.height(16.dp));LabButton(s.text("place"),{event(GameEvent.Launch)},Modifier.width(240.dp),enabled=s.tracking&&s.portalCount>=2&&s.phase==ExperimentPhase.PLANNING,selected=true)
}
@Composable
private fun ResultPanel(s: GameUiState,event: (GameEvent)->Unit) {
 val success=s.phase==ExperimentPhase.STABLE
 if(success)LabStars(s.stars,Modifier.alignForResult())
 Spacer(Modifier.height(28.dp))
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
  Metric(s.text("time"),formatTime(s.elapsed));Metric(s.text("portal_count"),"${s.portalCount}");Metric(s.text("stars"),"${s.stars} / 3")
 }
 Spacer(Modifier.height(30.dp));Text(s.text(if(success)"three_star" else "reset_hint"),color=LabColors.Muted,fontSize=19.sp)
 Spacer(Modifier.height(32.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){
  LabButton(s.text("reset"),{event(GameEvent.Reset)},selected=!success)
  if(success&&s.missionId<12)LabButton(s.text("next"),{event(GameEvent.ChooseMission(s.missionId+1))},selected=true)
  LabButton(s.text("missions"),{event(GameEvent.Show(GamePanel.LEVELS))})
 }
}
private fun Modifier.alignForResult()=this.width(200.dp).height(60.dp)
@Composable
private fun Metric(label: String,value: String){Column{Text(label,color=LabColors.Muted,fontSize=16.sp);Spacer(Modifier.height(10.dp));Text(value,color=LabColors.Text,fontSize=32.sp)}}
@Composable
private fun SettingsPanel(s: GameUiState,event: (GameEvent)->Unit) {
 Text(s.text("language"),color=LabColors.Text,fontSize=18.sp)
 Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
  GameLocale.entries.forEach{locale->LabButton(locale.label,{event(GameEvent.Locale(locale))},Modifier.weight(1f),selected=s.settings.locale==locale)}
 }
 Spacer(Modifier.height(24.dp))
 LabMeter(s.text("music"),s.settings.music,{event(GameEvent.Volume("music",it))})
 LabMeter(s.text("sfx"),s.settings.effects,{event(GameEvent.Volume("effects",it))})
 LabMeter(s.text("voice"),s.settings.voice,{event(GameEvent.Volume("voice",it))})
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
  Text(s.text("haptics"),color=LabColors.Text,fontSize=18.sp)
  LabButton(s.text(if(s.settings.haptics)"on" else "off"),{event(GameEvent.Haptics(!s.settings.haptics))},Modifier.width(150.dp),selected=s.settings.haptics)
 }
}
@Composable
private fun TutorialPanel(s: GameUiState,event: (GameEvent)->Unit) {
 val labels=listOf("scan","draw","select","place","loop")
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  labels.forEachIndexed{i,k->LabButton("0${i+1} / ${s.text(k)}",{},Modifier.weight(1f),enabled=false,selected=i==s.tutorialStep)}
 }
 Spacer(Modifier.height(30.dp));Text(s.text(labels[s.tutorialStep]),color=LabColors.Text,fontSize=32.sp)
 Spacer(Modifier.height(20.dp));Text(s.text(listOf("v_scan","draw_hint","object_hint","v_launch","v_loop")[s.tutorialStep]),color=LabColors.Muted,fontSize=23.sp)
 Spacer(Modifier.height(24.dp));Text(s.text("tutorial_practice"),color=LabColors.Ice,fontSize=16.sp)
 Spacer(Modifier.height(25.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){
  LabButton(s.text("continue"),{event(GameEvent.TutorialNext)},selected=true)
  LabButton(s.text("select"),{event(GameEvent.Show(GamePanel.PICKER))})
 }
}
@Composable
private fun HudPanel(s: GameUiState,event: (GameEvent)->Unit) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric(s.text("portal_count"),"${s.portalCount}");Metric(s.text("time"),formatTime(s.elapsed))}
 Spacer(Modifier.height(18.dp));Text(s.text(s.statusKey),color=if(s.tracking)LabColors.Ice else LabColors.Amber,fontSize=17.sp)
 if(s.mode==GameMode.CHALLENGE){val m=Missions.all[s.missionId-1];Spacer(Modifier.height(10.dp));Text("${s.text("mission_target")} ${m.optimalPortals}–${m.maxPortals}  /  ${s.text("optimal_time")} ${m.optimalSeconds.toInt()}s",color=LabColors.Muted,fontSize=14.sp)}
 if(s.deleteProgress>0f){Spacer(Modifier.height(10.dp));Text("${s.text("delete_hint")} ${(s.deleteProgress*100).toInt()}%",color=LabColors.Amber,fontSize=14.sp)}
 Spacer(Modifier.height(16.dp));Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
  LabButton(s.text("select"),{event(GameEvent.Show(GamePanel.PICKER))},Modifier.weight(1f),enabled=s.phase==ExperimentPhase.PLANNING)
  LabButton(s.text("reset"),{event(GameEvent.Reset)},Modifier.weight(1f))
 }
 Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
  LabButton(s.text("app"),{event(GameEvent.Show(GamePanel.MENU))},Modifier.weight(1f))
  LabButton(s.text("scan"),{event(GameEvent.Scan)},Modifier.weight(1f))
 }
}
internal fun formatTime(seconds: Double): String=String.format(Locale.US,"%02d:%05.2f",seconds.toInt()/60,seconds%60)
