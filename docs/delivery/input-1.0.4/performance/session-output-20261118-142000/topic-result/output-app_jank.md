---
issue_type_key: app_main_thread_jank
protocol: app_jank
activated_subprotocols: []
topic_state:
  activation: activated
  execution: completed
  closure_level: mechanism
  causal_role: primary_cause
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
- id: app_jank:e1
  type: perfetto_sql
  location: ../performance-data/sections.json
  one_line_finding: 75次PlaneGeometry平均271.935ms，累计20.395s，占GameFrameSystem约94%。
- id: app_jank:e2
  type: perfetto_sql
  location: ../performance-data/anchor-states.json
  one_line_finding: 280.307ms异常区间内Sleeping204.725ms，Running71.700ms。
- id: app_jank:e3
  type: logcat
  location: ../performance-data/runtime-after-cache.txt
  one_line_finding: 缓存修复后一个10秒窗口约901回调，均值约1.1ms、最大约4.8ms，零轮廓重建。
conclusion_level: confirmed
root_cause: 平面姿态细化触发整批轮廓网格同步销毁与重建，阻塞同一帧线程上的射线更新。
next_actions:
- 应用锚点局部网格缓存，保留官方数据作为碰撞/绘制依据。
limitations:
- SDK内部具体等待原语无调用栈采样，未将等待归因于Runtime锁或服务争用。
- 修复后已有连续10秒窗口的回调统计；第三次相同操作Trace对照待用户确认命令，尚未执行。
- 回调耗时不等于完整端到端输入延迟或显示帧耗时；不声称所有场景恒定90FPS。
- 未测量跨房间长期跟踪、全部12关或极大量虫洞场景。
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

# App Jank
## 1. 问题与窗口
用户确认选择正常后射线仍明显卡顿。由probe解析帧线程utid15，以PlaneGeometry最慢切片1907932锚定窗口，未硬编码线程名。
## 2. 当前证据
30秒内913次GameFrameSystem均值23.753ms，最长283.337ms。75次整批平面轮廓重建平均271.935ms、最长280.307ms，累计20.395秒。
## 3. 因果链
追踪姿态细化 → changed → 原rebuildPlaneVisuals销毁全部Entity/mesh → 逐个同步创建全部轮廓 → GameFrameSystem阻塞 → 射线姿态不能及时更新。原生等待原语未从栈证据解析，不将其称为Runtime内部缺陷。
## 4. 修复与控制边界
锚点局部网格按ID缓存；姿态只更新Transform；真正变形的轮廓至多每帧构建一个。1cm容差仅用于预览轮廓；几何、绘制、碰撞仍用原始平面数据。应用拥有缓存与生命周期，不能控制Runtime内部同步机制。
## 5. 验证
45项JVM、4项真机回归通过，包括100次姿态更新保持同一有效原生mesh及最终释放。运行日志中连续10秒窗口约900–901次更新，均值约1.1ms、最大约4.8ms，零整批重建。窗口不是相同手柄动作的Trace对照，不将其换算为端到端输入延迟。
## 6. 限制与交接
Binder、调度和Spatial Engine扩展已执行；Memory与Spatial Audio在该精确窗口不通过激活门。最后相同动作对照仍待采集授权。
