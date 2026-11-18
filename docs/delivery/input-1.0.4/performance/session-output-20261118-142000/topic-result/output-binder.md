---
issue_type_key: binder_latency
protocol: binder
activated_subprotocols: []
topic_state:
  activation: activated
  execution: completed
  closure_level: caller_and_callee
  causal_role: not_primary
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
- id: binder:e1
  type: perfetto_sql
  location: ../performance-data/anchor-binder.json
  one_line_finding: 流关联到com.pico.spatial.runtime具体Binder线程。
- id: binder:e2
  type: perfetto_sql
  location: ../performance-data/binder-summary.json
  one_line_finding: 24次，累计2.954ms，最大0.242ms。
- id: binder:e3
  type: perfetto_sql
  location: ../performance-data/binder-server-summary.json
  one_line_finding: 服务端最长0.077709ms，累计1.105677ms。
- id: binder:e4
  type: perfetto_sql
  location: ../performance-data/binder-server-states.json
  one_line_finding: 已解析的服务端回复切片全部为Running。
conclusion_level: confirmed
root_cause: 本窗口24次同步Binder调用累计2.954ms，服务端回复累计1.106ms且均为Running，不能解释204.725ms的主要等待。
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

# Binder
## 1. 窗口
同一PlaneGeometry异常窗口，应用utid15。
## 2. Caller
24次同步调用累计2.954ms、最大0.242083ms，时间占比约1.05%。
## 3. Callee
通过flow解析至com.pico.spatial.runtime的Binder线程。24个服务端回复累计1.105677ms，最长0.077709ms。
## 4. 服务端状态
精确裁剪至每个callee切片后，回复区间全部为Running；未见回复内Runnable或阻塞占用。
## 5. 判断
这些调用在关键路径上，但不是本窗口约205ms等待的主导原因；不推断服务锁争用、线程池耗尽或系统服务饥饿。
## 6. 限制
Unknown_Transaction_Code:1没有业务方法名。缺少原生调用栈，不把所有剩余等待自动归于Binder。
