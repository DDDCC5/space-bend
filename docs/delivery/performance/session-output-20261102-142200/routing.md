# Simulator performance evidence gap

1. Primary Protocol: App Jank / Spatial Engine routing; no trace-based analysis has been executed.
2. Activated Sub-protocols: none. No Perfetto trace is available to pass a temporal Activation Gate.
3. Directions to be Extended: establish frame-driving thread and runtime identities, capture the same slow-load/run window, then route App Jank before any Binder/CPU/Memory causal extension.
4. Judgment Basis: on the dedicated PICO 6.1 emulator, some launches loaded all six models in a few seconds and produced a three-star closed loop; other launches took tens of seconds and the real-time instrumentation wait expired. Compositor screenshots sometimes lagged behind the observed ViewModel state. App-scoped logs include a controller-provider start failure and an SDK/runtime ControllerActionResult size diagnostic.
5. Missing Evidence: a synchronized Perfetto trace, actual frame timings in the abnormal window, runtime compatibility confirmation and reproduction on a physical headset. The code/build/unit-test results do not establish a performance root cause.
6. Next Actions: retain logs and exact test results; do not label the slowdown an emulator bug, a lifecycle bug, GPU pressure or SDK incompatibility without trace evidence. Do not claim a frame-rate or performance acceptance pass.

No system settings, unrelated emulator or user application were changed for this investigation. The original emulator-5554 was left running; only the dedicated SpaceBend_Test_6_1 / emulator-5556 instance was restarted.
