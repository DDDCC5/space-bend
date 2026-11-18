---
issue_type_key:
- app_main_thread_jank
- binder_latency
- cpu_sched_latency
- spatial_frame_abnormal
activated_subprotocols:
- app_jank
- binder
- cpu_sched
- spatial-engine
conclusion_level: likely
root_cause: 主要射线停顿来自应用在平面细化时整批同步重建轮廓，94%的帧更新累计时间落在该操作。
next_actions:
- action: 锚点局部网格缓存已实现并通过原生复用/释放回归；继续取得相同手柄动作的修复后对照。
  priority: P0
  sources:
  - ./topic-result/output-app_jank.md
limitations:
- SDK内部具体等待原语无调用栈采样，未将等待归因于Runtime锁或服务争用。
- 修复后已有连续10秒窗口的回调统计；第三次相同操作Trace对照待用户确认命令，尚未执行。
- 回调耗时不等于完整端到端输入延迟或显示帧耗时；不声称所有场景恒定90FPS。
- 未测量跨房间长期跟踪、全部12关或极大量虫洞场景。
topic_outputs:
- topic: app_jank
  file: ./topic-result/output-app_jank.md
  conclusion_level: confirmed
- topic: binder
  file: ./topic-result/output-binder.md
  conclusion_level: confirmed
- topic: cpu_sched
  file: ./topic-result/output-cpu_sched.md
  conclusion_level: confirmed
- topic: spatial-engine
  file: ./topic-result/output-spatial-engine.md
  conclusion_level: likely
routing_registry:
- topic: app_jank
  status: executed
  evidence_refs:
  - ./topic-result/output-app_jank.md
- topic: binder
  status: executed
  evidence_refs:
  - ./topic-result/output-binder.md
- topic: cpu_sched
  status: executed
  evidence_refs:
  - ./topic-result/output-cpu_sched.md
- topic: spatial-engine
  status: executed
  evidence_refs:
  - ./topic-result/output-spatial-engine.md
- topic: memory
  status: not_activated
  reason: 精确窗口anchor-gc.json无GC/STW重叠证据。
  evidence_refs:
  - ./performance-data/anchor-gc.json
- topic: spatial_audio
  status: not_activated
  reason: 精确PlaneGeometry窗口AudioTick重叠查询为空；未见该窗口音频等待链。AudioTick其他窗口的24ms离群值保留为限制，未宣称所有离群帧均消失。
  evidence_refs:
  - ./performance-data/anchor-audio-overlap.json
  - ./performance-data/sections.json
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

# 空间折跃 · 射线卡顿诊断
## A. Topic Output Index
| Topic | Output | conclusion_level | Finding |
|---|---|---|---|
| app_jank | [详情](topic-result/output-app_jank.md) | confirmed | 平面轮廓整批同步重建为主要预算所有者 |
| binder | [详情](topic-result/output-binder.md) | confirmed | 已解析Caller和Callee，调用累计占比约1% |
| cpu_sched | [详情](topic-result/output-cpu_sched.md) | confirmed | Runnable约1.4%，非主要等待 |
| spatial-engine | [详情](topic-result/output-spatial-engine.md) | likely | 损失首先发生在App Produce |

## B. Cross-Topic Evidence-Chain Analysis
用户确认物体选择已正常而射线仍卡顿后，先执行App Jank。分段Trace把主要时间归到PlaneGeometry：75次重建累计20.395秒，占GameFrameSystem累计21.687秒约94%。同一280.307ms窗口，线程Sleeping204.725ms；Binder扩展已找到24个Runtime服务端回复，但累计仅1.106ms，且全部Running。调度扩展的Runnable约3.883ms不足以解释主要停顿。渲染跟踪表显示Runtime继续产生帧，不能因此把App交互更新率当作显示帧率。

修复仅改变应用持有的平面预览网格缓存和调度：姿态细化保留网格、形状改变按ID增量重建，每帧最多一个。100次原生姿态更新保持同一mesh；删除时资源释放。修复后运行窗口约901次回调/10秒、平均约1.1ms、最大约4.8ms、零重建。这验证了持续识别时的主导阻塞下降，尚不能替代相同手柄动作的最终Trace对照。
## C. Directions to Extend
相同手柄动作修复后30秒对照待命令确认；原始Trace无CPU采样栈，SDK内部具体等待原语不可见。其他窗口音频轮询有少量24ms离群切片，不把本次修复写成消灭所有掉帧。精确选定窗口没有GC/STW或音频调用重叠证据，完整registry见YAML。
## D. Final Action Plan
应用已覆盖安装1.0.4，完成45项JVM与4项真机回归；保持用户数据。取得最后对照后按相同统计更新报告。原始系统Trace只留本机，公开库提供聚合证据及此报告。
