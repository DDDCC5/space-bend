package com.example.spacebend.ui.game
import com.example.spacebend.domain.model.*
import com.example.spacebend.ui.navigation.GamePanel

data class GameUiState(
 val settings: GameSettings=GameSettings(),val strings: Map<String,String> = emptyMap(),val records: Map<Int,Record> = emptyMap(),
 val panels: Set<GamePanel> = setOf(GamePanel.SCAN),val mode: GameMode=GameMode.CHALLENGE,val missionId: Int=1,
 val selected: PayloadKind=PayloadKind.CORE,val planeCount: Int=0,val scanning: Boolean=true,val tracking: Boolean=false,val scanAvailable: Boolean=false,
 val phase: ExperimentPhase=ExperimentPhase.PLANNING,val portalCount: Int=0,val elapsed: Double=0.0,val loops: Int=0,
 val statusKey: String="scan_hint",val stars: Int=0,val tutorialStep: Int=0,val deleteProgress: Float=0f,val sceneReady: Boolean=false,
) { fun text(key: String)=strings[key]?:key }
sealed interface GameEvent {
 data class Show(val panel: GamePanel): GameEvent
 data class Close(val panel: GamePanel): GameEvent
 data class ChooseMission(val id: Int): GameEvent
 data object Sandbox: GameEvent
 data object Launch: GameEvent
 data object Reset: GameEvent
 data object Scan: GameEvent
 data object ContinueScan: GameEvent
 data object TutorialNext: GameEvent
 data class Select(val kind: PayloadKind): GameEvent
 data class Cycle(val direction: Int): GameEvent
 data class Locale(val locale: GameLocale): GameEvent
 data class Volume(val channel: String,val value: Float): GameEvent
 data class Haptics(val enabled: Boolean): GameEvent
}
