# Localized installed app name - 1.0.3

2026-11-13. The previous manifest already referenced `@string/app_name`, but that string contained a literal bilingual name. Added Android locale resources rather than changing the manifest to a hard-coded name:

- Default and English: Space Bend.
- Chinese (`values-zh`): 空间折跃.
- Traditional Chinese (`values-b+zh+Hant`): 空間折躍.

Version is 1.0.3, versionCode 4. No application ID, signing identity, saved-data schema or in-app language preference was changed. The wormhole launcher assets and the 1.0.2 shared-resource crash fix remain in this APK.

Verification: `assembleDebug testDebugUnitTest` succeeded (34 existing JVM checks, zero failures/errors). The compiled APK manifest's application label resolves to the localized `string/app_name` resource. `aapt2 dump badging` reported default/en `Space Bend`, zh/zh-CN `空间折跃`, and zh-HK/zh-Hant/zh-TW `空間折躍`; no compiled application label contains the old slash-separated bilingual name. The resource dump independently contains the expected default, zh, b+zh+Hant and en values. APK v2 signature verification passed. See `apk-name-verification.json` and `apk-localized-labels.txt`.

This is verification of the compiled APK label and locale-resource resolution. No system language was changed on the user's headset and no fresh launcher-cache visual check was performed for this update. Existing simulator screenshots remain historical captures, not evidence of current launcher labels.
