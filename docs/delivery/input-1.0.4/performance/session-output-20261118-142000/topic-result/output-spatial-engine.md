---
issue_type_key: spatial_frame_abnormal
protocol: spatial-engine
activated_subprotocols: []
topic_state:
  activation: activated
  execution: completed
  closure_level: producer_budget_owner
  causal_role: downstream
  controller_handoff: []
time_window: 'trace2 slice1907932: [17478477207394,17478757514321) ns; frame-driving
  utid15 from probe'
symptoms: 物体选择曾无响应；修复选择后，真实手柄射线仍明显卡顿。
target:
  package: com.example.spacebend
  process: com.example.spacebend
  threads:
  - ample.spacebend (utid15, tid1098)
evidence:
- id: spatial-engine:e1
  type: perfetto_sql
  location: ../performance-data/spatial-same-window.json
  one_line_finding: 窗口内26个GPUTime均值9.956ms、最大11.337ms；CPUTime均值6.248ms、最大8.289ms。
- id: spatial-engine:e2
  type: perfetto_sql
  location: ../performance-data/sections.json
  one_line_finding: 应用GameFrameSystem最长283.337ms，PlaneGeometry最长280.307ms。
conclusion_level: likely
root_cause: 主要预算损失发生在App Produce；同窗口Runtime/GPU每帧耗时远短于应用280ms阻塞，不能据此声明完整显示管线无抖动。
next_actions: []
limitations:
- 结论仅针对本次选定异常窗口。
extensions:
  knowledge_base:
    backend: pico-dev-knowledge
    status: available
    gate:
      reference_loaded: true
      probe: completed
      global_triage: completed
      category_specific: completed
      window_anchored: completed
      pre_conclusion_cross_check: completed
      complete: true
    matched_entries:
    - exp-6.1-checklists-triage-global-bottlenecks-before-category-diagnosis
    - exp-6.1-pitfalls-do-not-overload-frame-driving-update-callbacks
    - exp-6.1-pitfalls-avoid-main-thread-binder-ipc
    - exp-6.1-pitfalls-avoid-per-frame-managed-allocation
    selected_observations:
    - name: bounded app frame and mesh lifecycle attribution
      state: queried
      reason: PlaneGeometry sections, thread_state, Binder flows and native mesh identity
        test inspected
    limitation: Knowledge guidance is assistive; current trace and native resource
      tests supply evidence.
---

# Spatial Engine
## 1. 身份与窗口
probe同时解析目标应用与Spatial Runtime，使用与App Jank相同的异常锚点。
## 2. 帧管线
OpenXRClientSpatialFrames在该窗口内可观察到26组CPUTime/GPUTime。
## 3. 预算归属
CPUTime均值6.248ms、最大8.289ms；GPUTime均值9.956ms、最大11.337ms。与应用280ms轮廓操作相比，主要时间首先丢在App Produce。
## 4. 因果方向
应用未更新射线时，Runtime仍可重用已有场景进行显示；Runtime帧数不能替代应用交互更新率。
## 5. 判断
不把全局几何计数或GPU接近预算直接称为GPU根因。本次修复减少应用轮廓生命周期工作，不改变模型、灯光或统一渲染设置。
## 6. 限制
这些是跟踪表的CPUTime/GPUTime，未建立GPU完整硬件栈或每个源帧与显示帧的一一映射，因此不承诺全管线稳定90FPS。
