# Space Bend — accepted implementation contract

The full Chinese request in this chat is authoritative. Style A and the assets/style-a review package were approved for implementation on 2026-11-02. Latest correction: zh-Hans and zh-Hant retain separate text but both resolve narration to zh-Hans; English retains its own narration.

Application: 空间折跃 / Space Bend; com.example.spacebend. Kotlin + Compose + Spatial SDK 6.1.9 (official stage template), StageStyle.Mixed. All real-plane content is Stage ECS; independent draggable control panels are AttachmentPanel entities. UI follows accepted custom chamfered engineering design rather than native rounded controls. PicoTheme remains intact; app colors are named tokens.

Architecture: pure Kotlin geometry/physics/level engine; Android persistence and localized asset/audio repositories; public PICO plane/input/audio/haptic adapters; ECS scene controller; state-driven Compose control panels. SDK runtime is confined to platform adapters so geometry rules and challenge progression can be tested on JVM. Gravity is 9.8 m/s². Fixed-step swept collisions prevent tunneling. Arbitrary simple closed portal polygons are anchored to detected planes. Full payload cross-section must fit before transfer. Arrival speed equals terminal suction speed, outgoing direction is the destination surface normal with preserved side semantics. Portals route by creation order in a cycle. Collision triggers falling, all portals collapse, failure result waits for manual reset. Sandbox never emits victory or defeat, but collisions still cause the specified fall/collapse feedback.

Challenge success requires the payload traversing every portal in order and returning to the first; prove repeated return before stable-loop success. Twelve progressively unlocked missions in four tiers, restricted payload/portal budgets and independent two-/three-star time thresholds. Store best time and best stars separately. First-run tutorial covers scan/draw/select/launch/loop.

Input: trigger / tracked pinch draws; grip moves within original plane; hold B/Y two seconds deletes; stick click opens picker, horizontal axis edge cycles, trigger places payload. UI supports ray and hand targeting via SpatialUI. Drawing is invalid off real planes; no fabricated device planes in production. Tracking loss pauses simulation, stops drawing and prompts rescan.

Unspecified choices: portal count must be at least 2 to launch; invalid/self-intersecting/tiny strokes get actionable feedback. Plane geometry and all object bounds are converted to Stage root space. Portal size editing is absent. Simulated planes, if needed for emulator validation, live only in an explicitly separate debug test harness.

Resources: six approved local GLB models; original procedural dynamic portal rim follows user contour (fixed reference GLB does not replace drawn shape); all approved WAV resources; zh-Hant narration aliases zh-Hans without duplicating files. Official Editor failure and procedural asset provenance remain recorded in docs/assets/production-status.md.

Verification: JVM physics/geometry/progression/localization tests, APK build, static style checks, emulator install/launch/log/screenshot evidence. Real plane scan, hand/controller physical input, vibration, full spatial audio and MR comfort need supported hardware; report any unverified capability precisely.
