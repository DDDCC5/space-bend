# Space Bend 验证记录

交付类型：原生 PICO Spatial debug 构建，`com.example.spacebend`，版本 1.0。当前为可供设备验收的开发版本，不能视为真机性能或交互验收通过。

## 已通过的验证

| 项目 | 实际证据 |
|---|---|
| 官方环境 | Node.js 22、npm、git；PICO CLI 0.6.1 setup / doctor / project context doctor；原始 JSON 位于 docs/environment |
| 构建 | 官方 Stage 模板；Kotlin / Compose / Spatial SDK BOM 6.1.9；assembleDebug、assembleDebugAndroidTest 成功 |
| JVM 回归 | 34 tests、0 failures、0 errors；覆盖几何、碰撞、传送、闭环、30 秒稳定运行、评分、解锁、语言映射、保存、扫描恢复与导航 |
| 自定义界面 | SpatialUI style verifier：0 errors、0 warnings；架构检查：0 warnings |
| 三维素材 | 7 个 GLB：Khronos Validator 0 errors、0 warnings；运行用六个模型共 13,524 个三角形，零反向面 |
| 语言包 | 三语各 105 个运行文案键；简繁中文有 98 条文案差异；24 个语音文件，仅 en / zh-Hans 两个目录 |
| 实际 SDK 运行观察 | 在独立 PICO 6.1 模拟器中识别 57 个平面，六种模型成功加载；两个虫洞完成两圈并出现三星结算；截图由官方 CLI 捕获 |

## 设备自动测试结果

最终 `SpatialOrbitSmokeTest`：**OK (1 test)**，109.361 秒（包括加载与截图等待）。放宽等待窗口后通过；这项通过只代表检测数据、帧系统、游戏状态、评分保存和语言映射的功能链路，不是流畅度验收。原始输出为 `instrumentation.txt`。此前短等待窗口超时的记录保留于项目 .scratch，不覆盖为通过。

## 运行证据的含义

`SpatialOrbitSmokeTest` 从真实 PlaneTrackingManager 数据选取可放置且上方避开家具的地面位置，通过游戏命令创建两个虫洞、发射能量核心，观察帧系统驱动的状态与记录。它没有注入虚构平面，也没有直接推进引擎时间。它不验证物理手柄按键、手指捏合或震动。

`output/screenshots/space-bend-stable-loop.png` 是实际 XR 合成画面，含真实 SDK 检测轮廓、虫洞、HUD 与三星结算。概念参考图、SVG 素材板与模型浏览器预览均不算设备运行截图。Android UiAutomation 的二维截图无法获得这些 XR 图层，已改用官方 CLI compositor capture。

## 尚未完成的验收

- 模拟器重复启动存在加载变慢、画面更新滞后和自动测试等待超时的观察；原因未确认。保留测试日志，未宣称实时性能通过。详见 performance 目录中的证据缺口记录。
- 手柄及手势端到端输入、连续轻震/闭合强震、真实扫描精度、遮挡和视觉舒适度需要 PICO 真机验证。
- 官方空间音频 API 与距离衰减已经接入；本次没有完成头显上的声源方向和响度听审。
- 12 关的规则与评分有回归覆盖；尚未逐关在多种真实房间内进行人工可解性及难度测试。
- 物体碰撞使用根据实际模型外包围盒计算的包围球；不是逐三角形刚体碰撞。虫洞效果包含边缘光、吸入粒子与收缩；未实现背景引力透镜折射。

源码、build 输出、检查结果和独立评审修复记录都随项目保留。debug APK 使用本地 debug 签名，不包含发布签名。
