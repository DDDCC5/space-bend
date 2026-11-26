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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
  val titleKey=when(panel){GamePanel.MENU->"app";GamePanel.SCAN->"scan";GamePanel.LEVELS->"missions";GamePanel.PICKER->"select";GamePanel.RESULT->if(state.phase==ExperimentPhase.STABLE)"success" else "failure";GamePanel.SETTINGS->"settings";GamePanel.TUTORIAL->"tutorial";GamePanel.HUD->if(state.phase==ExperimentPhase.RUNNING)"flow_stage_run" else if(state.phase==ExperimentPhase.FALLING)"flow_collapsing" else "flow_stage_draw"}
  // design-style: opaque-root user explicitly approved the custom chamfered engineering panel, not native glass.
  Column(Modifier.testTag("spacebend-panel-${panel.name}").width(960.dp).height(640.dp)
   .clip(ChamferShape(24f)).background(LabColors.Graphite).padding(32.dp)) {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){
     Text("SB / GRAVITY LABORATORY",color=LabColors.Ice,fontSize=12.sp)
     Spacer(Modifier.height(12.dp));Text(state.text(titleKey),color=if(panel==GamePanel.RESULT&&state.phase!=ExperimentPhase.STABLE)LabColors.Danger else LabColors.Text,fontSize=30.sp,maxLines=2)
    }
    if(panel!=GamePanel.MENU&&panel!=GamePanel.HUD)LabButton(state.text(if(panel==GamePanel.PICKER)"flow_back_draw" else "back"),{onEvent(GameEvent.Close(panel))},Modifier.width(170.dp))
   }
   Spacer(Modifier.height(12.dp))
   Text(state.text("panel_drag"),color=LabColors.Muted,fontSize=12.sp)
   Spacer(Modifier.height(18.dp))
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState())){
    when(panel){
     GamePanel.MENU->if(state.pendingExperiment!=null)NewExperimentConfirmation(state,onEvent)else MainMenu(state,onEvent)
     GamePanel.SCAN->ScanPanel(state,onEvent)
     GamePanel.LEVELS->LevelPanel(state,onEvent)
     GamePanel.PICKER->ObjectPanel(state,onEvent)
     GamePanel.RESULT->ResultPanel(state,onEvent)
     GamePanel.SETTINGS->SettingsPanel(state,onEvent)
     GamePanel.TUTORIAL->HudPanel(state,onEvent)
     GamePanel.HUD->HudPanel(state,onEvent)
    }
   }
   if(panel==GamePanel.PICKER)PickerActions(state,onEvent)
   if(panel==GamePanel.HUD)HudActions(state,onEvent)
  }
 }
}
@Composable
private fun MainMenu(s: GameUiState,event: (GameEvent)->Unit) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){
   Text(s.text("flow_home_hint"),color=LabColors.Text,fontSize=25.sp)
   Spacer(Modifier.height(14.dp));Text(s.text("flow_mode_steps"),color=LabColors.Muted,fontSize=17.sp)
   Spacer(Modifier.height(22.dp))
   if(s.experimentActive){LabButton(s.text("flow_resume"),{event(GameEvent.Resume)},Modifier.fillMaxWidth(),selected=true);Spacer(Modifier.height(12.dp))}
   LabButton(s.text("sandbox"),{event(GameEvent.Sandbox)},Modifier.fillMaxWidth(),selected=!s.experimentActive)
   Spacer(Modifier.height(12.dp));LabButton(s.text("challenge"),{event(GameEvent.Show(GamePanel.LEVELS))},Modifier.fillMaxWidth())
   Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
    LabButton(s.text("tutorial"),{event(GameEvent.Show(GamePanel.TUTORIAL))},Modifier.weight(1f))
    LabButton(s.text("settings"),{event(GameEvent.Show(GamePanel.SETTINGS))},Modifier.weight(1f))
   }
  }
  Spacer(Modifier.width(35.dp));PortalDiagram(Modifier.width(300.dp).height(330.dp))
 }
}
@Composable
private fun NewExperimentConfirmation(s: GameUiState,event: (GameEvent)->Unit) {
 Text(s.text("flow_new_experiment_title"),color=LabColors.Text,fontSize=30.sp)
 Spacer(Modifier.height(24.dp));Text(s.text("flow_new_experiment_hint"),color=LabColors.Amber,fontSize=22.sp)
 Spacer(Modifier.height(32.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){
  LabButton(s.text("flow_keep_experiment"),{event(GameEvent.CancelNewExperiment)},selected=true)
  LabButton(s.text("flow_replace_experiment"),{event(GameEvent.ConfirmNewExperiment)})
 }
}
@Composable
private fun ScanPanel(s: GameUiState,event: (GameEvent)->Unit) {
 InlineTutorial(s)
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f)){
   Text(s.text(if(s.canContinueScan)"scan_ready"else "scanning"),color=LabColors.Ice,fontSize=28.sp)
   Spacer(Modifier.height(18.dp));Text(s.text("v_scan"),color=LabColors.Text,fontSize=21.sp)
   Spacer(Modifier.height(16.dp));Text(s.text(if(s.invalidAnchorCount>0)"flow_anchor_lost"else "scan_real_only"),color=if(s.invalidAnchorCount>0)LabColors.Amber else LabColors.Muted,fontSize=17.sp)
   Spacer(Modifier.height(18.dp));Text(s.text(s.statusKey),color=LabColors.Muted,fontSize=16.sp)
   Spacer(Modifier.height(24.dp));Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
    LabButton(s.text("scan_retry"),{event(GameEvent.Scan)})
    LabButton(s.text(if(s.invalidAnchorCount>0)"flow_redraw_now"else if(s.phase==ExperimentPhase.PLANNING)"flow_start_drawing"else "flow_scan_resume"),{event(GameEvent.ContinueScan)},enabled=s.canContinueScan,selected=true)
   }
  }
  Column(Modifier.width(180.dp),horizontalAlignment=Alignment.CenterHorizontally){
   Text("${s.planeCount}",color=LabColors.Text,fontSize=90.sp)
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
 InlineTutorial(s)
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
  PayloadKind.entries.forEachIndexed{i,k->
   val allowed=s.mode==GameMode.SANDBOX||k in Missions.all[s.missionId-1].allowed
   Column(Modifier.weight(1f).labAction(s.text(k.textKey),allowed,s.selected==k){event(GameEvent.Select(k))},horizontalAlignment=Alignment.CenterHorizontally){
    PayloadGlyph(i,Modifier.size(90.dp).alpha(if(allowed)1f else .3f))
    LabButtonFace(s.text(k.textKey),Modifier.fillMaxWidth(),enabled=allowed,selected=s.selected==k)
   }
  }
 }
 Spacer(Modifier.height(18.dp));Text(s.text(s.selected.textKey),color=LabColors.Text,fontSize=27.sp)
 Spacer(Modifier.height(10.dp));Text("${s.text("mass")}  ${s.selected.massKg} kg   /   ${s.text("velocity")}  ${s.selected.launchSpeed} m/s",color=LabColors.Muted,fontSize=20.sp)
 Spacer(Modifier.height(12.dp));Text(s.text("object_hint")+if(s.mode==GameMode.CHALLENGE)"\n"+String.format(s.text("object_allowed"),Missions.all[s.missionId-1].allowed.joinToString(" / "){s.text(it.textKey)})else "",color=LabColors.Muted,fontSize=15.sp)
}
@Composable
private fun PickerActions(s: GameUiState,event: (GameEvent)->Unit) {
 Spacer(Modifier.height(10.dp));Text(launchExplanation(s),color=if(s.canLaunch)LabColors.Ice else LabColors.Amber,fontSize=16.sp,maxLines=3)
 Spacer(Modifier.height(10.dp));Row(horizontalArrangement=Arrangement.spacedBy(18.dp)){
  LabButton(String.format(s.text("flow_launch_object"),s.text(s.selected.textKey)),{event(GameEvent.Launch)},enabled=s.canLaunch,selected=true)
  LabButton(s.text("flow_back_draw"),{event(GameEvent.Close(GamePanel.PICKER))})
 }
}
private fun launchExplanation(s: GameUiState): String = when {
 !s.tracking||s.scanning->s.text("flow_launch_tracking")
 !s.sceneReady->s.text("flow_launch_loading")
 s.launchIssue==null->s.text("flow_launch_ready")
 s.launchIssue.reason==LaunchBlocker.APERTURE_TOO_SMALL->String.format(s.text("flow_launch_too_small"),s.launchIssue.portalOrder)
 s.launchIssue.reason==LaunchBlocker.MORE_PORTALS->String.format(s.text("flow_need_count"),(s.requiredPortals-s.portalCount).coerceAtLeast(0))
 s.launchIssue.reason==LaunchBlocker.KIND_RESTRICTED->s.text("flow_launch_restricted")
 s.launchIssue.reason==LaunchBlocker.ANCHOR_MISSING->s.text("flow_anchor_lost")
 else->s.text("flow_launch_not_editing")
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
  LabButton(s.text("flow_retry"),{event(GameEvent.Reset)},selected=!success)
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
private fun InlineTutorial(s: GameUiState) {
 if(!s.tutorialActive)return
 Text(String.format(s.text("flow_tutorial_step"),s.tutorialStep+1),color=LabColors.Amber,fontSize=15.sp)
 Spacer(Modifier.height(6.dp));Text(s.text(listOf("v_scan","flow_tutorial_draw","flow_tutorial_select","flow_tutorial_launch","flow_tutorial_loop")[s.tutorialStep.coerceIn(0,4)]),color=LabColors.Muted,fontSize=17.sp)
 Spacer(Modifier.height(16.dp))
}
@Composable
private fun HudPanel(s: GameUiState,event: (GameEvent)->Unit) {
 InlineTutorial(s)
 val planning=s.phase==ExperimentPhase.PLANNING
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
  Metric(s.text("portal_count"),"${s.portalCount}");Metric(s.text(if(planning)"flow_required"else "flow_loops"),if(planning)"${s.requiredPortals}"else "${s.loops}");Metric(s.text("time"),formatTime(s.elapsed))
 }
 Spacer(Modifier.height(20.dp))
 val guidance=when {
  s.phase==ExperimentPhase.FALLING->s.text("flow_collapsing")
  !planning->s.text("flow_running_hint")
  s.portalCount==0->s.text("flow_draw_first")
  !s.canChoose->String.format(s.text("flow_need_count"),(s.requiredPortals-s.portalCount).coerceAtLeast(0))
  else->s.text("flow_draw_ready")
 }
 Text(guidance,color=LabColors.Text,fontSize=24.sp)
 Spacer(Modifier.height(12.dp));Text(s.text(s.statusKey),color=if(s.tracking)LabColors.Ice else LabColors.Amber,fontSize=17.sp)
 if(planning){Spacer(Modifier.height(12.dp));Text(s.text("draw_hint")+"  /  "+s.text("move_hint")+"  /  "+s.text("delete_hint"),color=LabColors.Muted,fontSize=15.sp)}
 if(s.mode==GameMode.CHALLENGE){val m=Missions.all[s.missionId-1];Spacer(Modifier.height(10.dp));Text("${s.text("mission_target")} ${m.optimalPortals}–${m.maxPortals}  /  ${s.text("optimal_time")} ${m.optimalSeconds.toInt()}s",color=LabColors.Muted,fontSize=15.sp)}
 if(s.deleteProgress>0f){Spacer(Modifier.height(10.dp));Text("${s.text("delete_hint")} ${(s.deleteProgress*100).toInt()}%",color=LabColors.Amber,fontSize=14.sp)}
}
@Composable
private fun HudActions(s: GameUiState,event: (GameEvent)->Unit) {
 val planning=s.phase==ExperimentPhase.PLANNING
 Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
  if(planning){
   LabButton(s.text("select"),{event(GameEvent.Show(GamePanel.PICKER))},enabled=s.canChoose,selected=true)
   LabButton(s.text("flow_clear"),{event(GameEvent.Reset)},enabled=s.portalCount>0)
  }else LabButton(s.text("flow_stop_edit"),{event(GameEvent.StopAndEdit)},enabled=s.phase==ExperimentPhase.RUNNING,selected=true)
 }
 Spacer(Modifier.height(14.dp));Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){
  LabButton(s.text("flow_home"),{event(GameEvent.Show(GamePanel.MENU))})
  LabButton(s.text("settings"),{event(GameEvent.Show(GamePanel.SETTINGS))})
  if(planning)LabButton(s.text("scan"),{event(GameEvent.Scan)})
 }
}
internal fun formatTime(seconds: Double): String=String.format(Locale.US,"%02d:%05.2f",seconds.toInt()/60,seconds%60)
