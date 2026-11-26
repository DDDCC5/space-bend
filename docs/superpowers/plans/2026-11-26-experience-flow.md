# Guided Experience Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Deliver the approved, single-task-panel draw/select/launch/observe flow in Space Bend.
**Architecture:** One typed currentPanel in GameUiState, derived compatibility panels set, ViewModel-owned navigation and scan return destination. Physics retains authoritative portal rules and gains stop-with-layout preservation; attachment Entities remain independently draggable but share the user's workspace placement. Tutorial progress is derived from real actions.
**Tech Stack:** Kotlin, SpatialUI Compose, Spatial SDK BOM6.1.9, existing Gradle/JDK21.
**Spec:** docs/design/2026-11-26-experience-flow.md (approved by user: “那你继续开发”).

## Global Constraints
- Package com.example.spacebend; preserve style A, custom chamfered UI, current assets, localized installed names and existing storage/signature.
- 简体中文/繁體中文/English; both Chinese locales use zh-Hans narration.
- Keep real-plane-only drawing, portal drawing order, gravity and collision collapse rules, 12 missions and scoring.
- Existing Mixed Stage + 8 attachment Entities; one enabled task panel at a time; no new native window model or 3D asset generation.
- Release1.0.5/versionCode6, public GitHub and replace-only install when target available. Never connectedAndroidTest/uninstall/clear data.

## Review Focus
- Scan unavailable/lost mid-selection: no bypass; preserve valid anchors and correct return step; explicitly inform if old anchors must be redrawn.
- Picker/menu/settings must not leak trigger gestures into world drawing or implicit launch.
- Stop preserves layout/time/collisions; genuine collision cannot be cancelled to rescue collapsing portals.
- Changing mode with a nonempty layout requires explicit destructive confirmation, ordinary back/resume does not clear it.
- Tutorial Next cannot bypass actual drawing/selection/transfer; legacy tutorialDone and record data remain compatible.

### Task1: Single panel navigation and scan continuation
- [x] Write/observe failing JVM tests for home, scan->selected mode, picker gating, exclusive panels and settings return.
- [x] Implement typed currentPanel, start/resume/temporary navigation, real-progress tutorial and tracking recovery in GameViewModel/GameUiState.
- [x] Add negative tests for unavailable sensing, anchors lost, stale return destinations and new experiment confirmation.

### Task2: Preserve layout on stop and explain launch readiness
- [x] Add engine regression for stop preserves portals/time/collisions, blocks stop during real collapse; run red then green.
- [x] Add typed launch validation with portal order and required count; keep selection possible when object is too big.
- [x] Prove launch/physics/closed-loop regressions still pass.

### Task3: Stage UI, input routing and placement
- [x] Remove implicit outside-panel launch and gate world editing to drawing stage; prove negative routes in JVM tests.
- [x] Update primary stage controls, inline tutorial, picker back/explicit launch/fit reason, mode-aware result/reset text and trilingual strings.
- [x] Preserve shared workspace position across task transitions and collect state in attachment compositions.
- [x] Run SpatialUI design-style verification and Gradle build/unit tests.

### Task4: Runtime acceptance and delivery
- [x] Update opt-in native interaction tests for complete flow and one visible panel; run on available explicit target without uninstalling app.
- [x] Fresh-context read-only branch review; resolve actionable issues, repeat affected checks.
- [x] Verify APK identity/signature/icons/locales and version1.0.5; install --replace when target available.
- [ ] Update release notes/VibeLab/source archive, push existing public repo and publish test APK; report limitations honestly.

## Ledger
- 2026-11-26: User approved flow A. Implement inline; review once at end, no repeated design approvals.
- Environment doctor: CLI0.7.0, SDK6.1.9/knowledge6.1 aligned. Editor orphan process error is outside this Kotlin/UI/device path; do not terminate another session's Editor. Required runtime/build checks have no error.

- Final review: fixed result-panel terminal-fall animation; added red/green regression. Scan retry preserves per-UUID coordinate bases. Voice prompts updated to match visible actions.
