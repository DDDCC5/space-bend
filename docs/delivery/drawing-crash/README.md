# Drawing crash repair - 1.0.2 (2026-11-13)

Device: new B3110 headset, serial ending 0063B. Unlike the previous headset, this device supplies room planes; the repaired app log observed 20 planes with tracking=true.

## Root cause

Two app crashes at 14:04 and 14:05 reported `IllegalArgumentException: You can't construct ModelComponent with a closed UnlitMaterial`. The first app frame was `SceneGeometry.trace`, called by `SceneRuntime.stroke` through `InputRouter.process`. Replacing a drawing stroke destroys the old ModelEntity. SDK reference-counted ownership then releases the shared material when its last model owner disappears; caching the Kotlin handle does not retain the native resource. Reusing that handle for the next segment crashes. The same lifecycle error affects shared portal materials and particle meshes after portal deletion.

## Fix

All four cached SceneGeometry materials and both shared primitive meshes are explicitly retained with the public SDK `toGlobal()` API. Transient trace meshes remain entity-owned; existing cleanup destroys scene entities before SceneGeometry.close() explicitly releases its six retained resources. No exception is swallowed and no draw operation is disabled. Version is 1.0.2 (versionCode 3), retaining the integrated launcher icon.

## Verification

- On 1.0.1, both new opt-in native regressions failed on the actual headset: first stroke replacement released its material; first portal deletion released its edge material. See native-tests-red.txt.
- On 1.0.2, both native regressions passed: 160 trace replacements across all four materials, 40 shared-mesh owner destructions, and 30 portal create/update/collapse/destroy cycles. They also verify retained resources become invalid after explicit scene cleanup. See native-tests-green.txt and resource-cycles.txt.
- `assembleDebug testDebugUnitTest assembleDebugAndroidTest` succeeded; 34 host-side JVM checks have zero failures/errors.
- Compiled APK identity is 1.0.2 (3); existing icon resource verification passed. The repair was installed with `pico-cli app install --replace`, without uninstalling or clearing data. No Gradle connected-device task was used.
- The opt-in native tests use synthetic geometry to isolate resource ownership; they are not a physical controller/hand drawing or room-scanning acceptance test. Separately, after reinstall and launch, the user confirmed on 2026-11-13 that drawing works normally and no longer crashes. The input modality was not separately identified; this confirmation is limited to the reported drawing failure, not all gameplay or performance.

Reference: public resource lifecycle guidance for SDK BOM 6.1.9 / knowledge line 6.1 in `spatial-sdk_resource-management_overview.md`, retrieved through the matching PICO development knowledge. Shared resources must be persisted before transient owners are destroyed, and explicitly closed at their owning scene's end.

Run the native regressions only by explicitly targeting a device and opting in with `-e geometryLifecycle true -e class com.example.spacebend.platform.spatial.SceneGeometryLifecycleTest`. Run `am instrument` without `-w` and observe TestRunner logs to avoid the CLI's bounded synchronous shell timeout. This preserves the installed app and user data.
