# Emulator sensing verification - 2026-11-05

Result: PASS. Dedicated SpaceBend_Test_6_1 AVD, PICO emulator 6.1.0 / Android 16 API 36, target emulator-5554. Environment doctor SUCCESS; CLI/plugin 0.6.1, app BOM 6.1.9, matching knowledge line 6.1.

Installed the same diagnostic main APK used during physical-device diagnosis plus the existing opt-in sensing probe. Started instrumentation without -w through pico-cli; captured device TestRunner completion instead of relying on the CLI 30-second synchronous shell limit. No Gradle connected-device task, uninstallation, or data clearing was used.

Probe: first sample zero during initial loading, subsequent seven samples 57 planes and 48 mesh anchors, both trackers RUNNING, mesh update callbacks up to 55. TestRunner: 1 tests, 0 failed, 0 ignored. Direct app launch also received 57 planes, updated UI tracking=true, rendered detected plane outlines and the main menu. Crash buffer empty at observation. This verifies simulated room-geometry consumption; it does not prove real-device optical scanning or identify why the earlier OS 6.2.0 physical headset returned zero geometry.

Screenshot: output/screenshots/space-bend-emulator-scan-ready-20261105.png (2160x2158). The initial screenshot showed zero while data was loading and is excluded from delivery evidence. Emulator screencap rejects --spatial, so the successful screenshot used the default compositor capture, then was visually inspected. The dedicated emulator is left running for inspection.

Main APK SHA256: 01c535b8362f1b028e9cb70d330385b1ffad759a74859c6a0b12d359f264f03b
