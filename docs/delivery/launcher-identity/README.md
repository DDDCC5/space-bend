# Launcher identity verification - 1.0.1

Verified 2026-11-12. The old APK still used the template launcher images; the submission SVG had not been integrated. This update exports that approved vector into Android compatibility/adaptive assets plus PICO background/foreground layers and matching SDFs, preserving the existing manifest and two-layer array wiring. Version is now 1.0.1 (versionCode 2).

The PICO background contains the circular laboratory/portal mark; the foreground contains the payload and amber trajectory. Both are 1024x1024, below 4MB each. The background is opaque inside its inscribed circle; the foreground uses binary alpha for clear contours. SDFs were produced offline with the installed App Identity Configurator 6.1.1 generator, using its RGBA, 32px spread, Distance fill and 200 threshold settings. No plugin library or private SDK dependency was added to the app.

`assembleDebug testDebugUnitTest` succeeded; 34 existing JVM checks have zero failures/errors. APK v2 signature verification passed. `verify_launcher_apk.py` resolved the compiled manifest's icon/roundIcon to the new PNG and validated both PICO metadata arrays. All six packaged PNG resources were decoded and pixel-compared with the intended source. The extracted icon was visually inspected (`icon-extracted-from-apk.png`). `apk-icon-verification.json` contains checks and the APK SHA256.

This is APK packaging verification, not a fresh headset-launcher visual/performance test. No device installation, data clearing or uninstall was performed during this icon-only update. The physical-headset scanning issue remains unresolved; this update makes no scanning repair claim.

Sources: project BOM 6.1.9 and matching PICO knowledge line 6.1; official bundled docs `spatial-design_foundation_icon_app-icon-and-layered-design.md` and `spatial-toolkit_pico-spatial-plugin_configure-spatial-layered-icons-for-spatial-apps-using-app-identity-configurator.md`. SDF tool signatures and call settings were checked against the installed configurator authoring utility, not inferred for app runtime APIs.
