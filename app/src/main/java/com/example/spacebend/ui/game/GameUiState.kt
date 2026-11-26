package com.example.spacebend.ui.game
import com.example.spacebend.domain.model.*
import com.example.spacebend.ui.navigation.GamePanel

data class NewExperiment(val mode: GameMode,val missionId: Int=1,val tutorial: Boolean=false)

data class GameUiState(
 val settings: GameSettings=GameSettings(),val strings: Map<String,String> = emptyMap(),val records: Map<Int,Record> = emptyMap(),
 val currentPanel: GamePanel=GamePanel.MENU,val mode: GameMode=GameMode.CHALLENGE,val missionId: Int=1,
 val selected: PayloadKind=PayloadKind.CORE,val planeCount: Int=0,val scanning: Boolean=false,val tracking: Boolean=false,val scanAvailable: Boolean=false,
 val phase: ExperimentPhase=ExperimentPhase.PLANNING,val portalCount: Int=0,val elapsed: Double=0.0,val loops: Int=0,
 val statusKey: String="flow_home_hint",val stars: Int=0,val tutorialStep: Int=0,val deleteProgress: Float=0f,val sceneReady: Boolean=false,
 val experimentActive: Boolean=false,val tutorialActive: Boolean=false,val requiredPortals: Int=2,
 val scanRevision: Long=0,val launchIssue: LaunchIssue?=null,val invalidAnchorCount: Int=0,val pendingExperiment: NewExperiment?=null,
) {
 val panels: Set<GamePanel> get()=setOf(currentPanel)
 val canEdit: Boolean get()=experimentActive&&currentPanel==GamePanel.HUD&&tracking&&!scanning&&sceneReady&&phase==ExperimentPhase.PLANNING
 val canChoose: Boolean get()=canEdit&&portalCount>=requiredPortals
 val canLaunch: Boolean get()=experimentActive&&currentPanel==GamePanel.PICKER&&tracking&&!scanning&&sceneReady&&phase==ExperimentPhase.PLANNING&&launchIssue==null
 val canContinueScan: Boolean get()=scanAvailable&&planeCount>0&&sceneReady
 fun text(key: String)=strings[key]?:key
}
sealed interface GameEvent {
 data class Show(val panel: GamePanel): GameEvent
 data class Close(val panel: GamePanel): GameEvent
 data class ChooseMission(val id: Int): GameEvent
 data object Sandbox: GameEvent
 data object Launch: GameEvent
 data object Reset: GameEvent
 data object StopAndEdit: GameEvent
 data object Resume: GameEvent
 data object ConfirmNewExperiment: GameEvent
 data object CancelNewExperiment: GameEvent
 data object Scan: GameEvent
 data object ContinueScan: GameEvent
 data object TutorialNext: GameEvent
 data class Select(val kind: PayloadKind): GameEvent
 data class Cycle(val direction: Int): GameEvent
 data class Locale(val locale: GameLocale): GameEvent
 data class Volume(val channel: String,val value: Float): GameEvent
 data class Haptics(val enabled: Boolean): GameEvent
}
