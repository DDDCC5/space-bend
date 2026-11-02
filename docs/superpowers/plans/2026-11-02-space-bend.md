# Space Bend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement task by task. User authorized direct development; no further approval gate.

**Goal:** Build the complete Mixed Reality Space Bend puzzle game and runnable debug APK.
**Architecture:** Pure Kotlin simulation + geometry and challenge rules, public SDK platform adapters, SpatialView ECS scene, custom Compose panels and persistent state.
**Tech Stack:** Official Spatial SDK 6.1.9 stage template, Kotlin 2.1.20, Compose, Android API 35.
**Spec:** docs/superpowers/specs/2026-11-02-space-bend-design.md

## Global Constraints
- Preserve com.example.spacebend, accepted style A, custom non-rounded controls, PicoTheme, no Material.
- Stage Mixed for detected real planes, all metrics in meters; no fabricated production plane data.
- zh-Hant text stays separate; narration aliases zh-Hans, English separate.
- Only documented public APIs supported by installed SDK, prove compile and disclose device limitations.

## Task 1: Official scaffold and narration alias
- [x] Create official Stage project, preserve assets/guidance, build scaffold.
- [x] Write failing narration-resolution test, then implement shared zh-Hans voice resolution.
- [x] Import approved models/audio/localizations and document asset provenance.
Expected: official scaffold build and locale tests pass; one physical Chinese voice directory.

## Task 2: Deterministic puzzle engine
- [x] Write failing tests for plane projection, arbitrary polygon fitting, self-intersection, sweep crossing, normal exit, suction speed, portal order, plane-locked edit, failure collapse, loop success, scoring and progressive unlock.
- [x] Implement domain/ geometry, portal drawing, physics integration, challenge catalog and grading.
- [x] Run domain tests; retain fixed-step physics with swept collision checks and full payload-fit rule.
Expected: tested engine handles grazing, oversize objects, both entry sides, collision outside aperture, reordered frame deltas, progression and sandbox rules.

## Task 3: PICO adapters and interactive scene
- [x] Query version-matched knowledge; read public plane, tracking, controller, hand, spatial audio, vibration and ECS documentation.
- [x] Implement lifecycle-safe tracking/input/audio adapters, dynamic contour meshes, model pool, surface feedback, per-frame simulation and reset.
- [x] Follow managed scene codegen root handoff for Kotlin Entity changes and validated build.
Expected: real APIs compile; detected planes drive game, input actions call tested engine; no synthetic production scan.

## Task 4: Custom spatial panels and data
- [x] Implement scan/tutorial/menu/levels/picker/results/settings as draggable ECS-attached panels with custom chamfered UI.
- [x] Implement trilingual text switching, shared Chinese voice mapping, volumes/haptics settings, local stars/best times.
- [x] Exercise transitions and persistence tests; run style verification with explicit custom-design exception from user.
Expected: full task flow reachable from UI with accessible labels and ray/hand interaction.

## Task 5: Build, run and review
- [x] Run complete JVM suite and assembleDebug; inspect errors rather than assuming runtime validity.
- [x] Install/launch in PICO emulator, inspect logs, capture screenshot evidence with official CLI.
- [x] Independent final review; fix material findings; document artifact, run steps and hardware-only verification gaps.
Expected: deliver actual APK and honest runtime evidence; do not claim unobserved hardware behaviors.

## Delivery evidence

See `docs/delivery/verification.md`. JVM, asset/style validation and managed scene build have passed. Simulator observations are distinguished from physical controller/hand/haptic/audio validation.
