# PICO room sensing returns no geometry

Device: B3110 / swan, arm64-v8a. OS display ID 6.2.0, Android 16 / API 36. App com.example.spacebend 1.0 debug; Spatial SDK BOM 6.1.9.

User reports zero planes, including failure of the built-in headset room scanning before reboot. Plane observer permission/init/start logs report error_code=0. Before reboot, eight uninterrupted samples from public PlaneTrackingManager and MeshTrackingManager reported RUNNING with planes=0, meshes=0, meshUpdates=0. A normal headset reboot completed (sys.boot_completed=1). A later direct app observation exceeded 40 seconds, with live ECS frames and ten RUNNING plane snapshots all returning count=0; crash buffer was empty. Native Photoid logs contain GetOnlineMeshFloorHeight Failed, but this is not a proven root cause.

Please investigate online plane/mesh generation in this firmware and compatibility with SDK 6.1.9. No factory reset, system-room-data clearing, firmware/SDK upgrade, or system-property modification was performed. Post-reboot built-in room scanning comparison remains pending.

Test limitation: initial post-reboot automated observation was interrupted by manual app closure. Gradle connected-test cleanup also removed the target installation; it has been reinstalled, but earlier app-local settings may be lost. No system room data was cleared. The final direct observation ran uninterrupted.
