---
issue_type_key: cpu_sched_latency
protocol: cpu_sched
activated_subprotocols: []
topic_state:
  activation: activated
  execution: completed
  closure_level: bounded_thread_states
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
- id: cpu_sched:e1
  type: perfetto_sql
  location: ../performance-data/anchor-states.json
  one_line_finding: S204.725ms，Running71.700ms，R3.784ms、R+0.098ms。
- id: cpu_sched:e2
  type: perfetto_sql
  location: ../performance-data/anchor-sched.json
  one_line_finding: 应用线程在CPU0–5上有Running区间。
conclusion_level: confirmed
root_cause: 本窗口Runnable总计约3.883ms，占280.307ms约1.4%，调度延迟不是主要停顿。
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

# CPU Scheduling
## 1. 窗口
同一280.307ms PlaneGeometry切片。
## 2. 线程状态
Sleeping204.725ms，Running71.700ms，Runnable(R与R+)约3.883ms。
## 3. CPU归属
sched记录在CPU0–5上运行；数据见anchor-sched.json。
## 4. 判断
调度延迟只占约1.4%，不足以解释主要停顿。无需把大量Sleeping改称CPU争抢。
## 5. 优化边界
本次修复消除应用重复的同步网格生命周期操作；未修改亲和性、优先级或系统调度策略。
## 6. 限制
没有建立温控、频率与竞争线程的独立根因链；不作全设备CPU功耗结论。
