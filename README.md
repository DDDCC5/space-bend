# 空间折跃 / Space Bend

PICO Spatial MR 空间解谜游戏，包名 `com.example.spacebend`。风格 A「引力实验室」：石墨黑工程面板、冰蓝虫洞边缘、低饱和金属物体与琥珀色状态提示。Kotlin + Compose + PICO Spatial SDK 6.1.9，官方 Stage 模板，Mixed 透视模式。

## 测试与迭代

- 当前测试 APK：[GitHub 测试发布](https://github.com/DDDCC5/space-bend/releases/tag/v1.0.2)。仓库已公开，可直接访问源码与发布附件。1.0.2（versionCode 3）修复绘制笔迹、删除并重建虫洞时共享材质与网格被提前释放的问题；保留1.0.1引入的虫洞安装图标。
- 测试反馈：[新建真机测试反馈](https://github.com/DDDCC5/space-bend/issues/new?template=device-test.md)。请附头显 / OS 版本、APK 版本、复现步骤和截图。
- 本地 SDK、登录配置、缓存和签名密钥不进入仓库；APK 作为GitHub 测试发布附件保存。
- [Vibe Lab 投稿字段填写稿](docs/submission/00-填写指南.md)：沿用 Orbit Mission 表单字段，包含可复制简介、版本说明及当前验证边界。

## 运行与构建

交付 APK：`output/SpaceBend-debug.apk`；可移植源码包：`output/SpaceBend-source.zip`。需要兼容 Spatial SDK 6.1 的 PICO OS 环境；普通 Android 手机无法提供空间运行时。

```sh
./gradlew testDebugUnitTest assembleDebug
pico-cli app install output/SpaceBend-debug.apk --replace
pico-cli app launch com.example.spacebend
```

构建环境：Node.js 22、PICO CLI 0.6.1、JDK 21、Android SDK 35。项目根目录保留官方 setup 写入的插件说明与技能。开发机存在旧 CLI 的 PATH 优先级问题，验证使用新版 CLI 的绝对路径；其他机器应先运行 `pico-cli --version`。

## 操作

1. 启动后环顾房间，等待真实平面识别；扫描面板显示识别数量，点击「继续」。首次进入提供五步教程。
2. 在关卡挑战中选择已解锁关卡，或进入自由创作。
3. 对准真实平面，按住扳机或手指捏合绘制闭合轮廓；闭合后生成虫洞。所有虫洞必须完全位于当前检测平面内，不能重叠。
4. 对准虫洞按住握持键，在当前平面内平移。对准虫洞长按 B / Y 两秒删除。删除进度显示在实验 HUD。
5. 按摇杆键打开物体栏，左右拨杆循环选择，按扳机确认放置；手势用户可直接点击 HUD 的「选择物体」与面板内的放置按钮。
6. 对准窗口按握持键拖动；手势用户捏合窗口标题栏拖动。窗口以米为单位定位和缩放。
7. 物体必须完整投影落入虫洞范围才能传送，按绘制顺序连接。连续完成两圈后记录闭环成功。碰撞真实平面会触发落地和全部虫洞收缩；挑战失败后手动点击重置。

## 已实现内容

- 官方 PlaneTrackingManager 提供平面边界、孔洞、语义与锚点姿态；以稳定锚点局部坐标保存虫洞。跟踪丢失暂停模拟并引导重新扫描。
- 六种原始 GLB 科幻物体，独立质量、弹射速度和根据模型边界计算的保守碰撞半径。
- 任意简单闭合轮廓、引力边缘光、向内运动粒子、生成与收缩动画；手柄射线和手势输入适配，绘制与闭合震动。
- 固定 240 Hz 物理步长、9.8 m/s² 重力、近洞口引力、双侧进入/法线方向吐出、入口末速度保留、连续球体与有限平面面/边/顶点碰撞检测。
- 12 关、四个难度梯队、逐关解锁、三星规则、最快用时与星级分别保存；自由创作无胜负结算。
- 八个可独立摆放的自定义切角空间面板，无原生圆角矩形按钮。菜单、扫描、教程、关卡、物体栏、HUD、结算、设置均可从应用流程到达。
- 简体中文 / 繁体中文 / English 文案各自独立。**简体和繁体中文共用 `audio/voice/zh-Hans`；English 使用 `audio/voice/en`。** APK 不打包重复的繁体语音。
- 环境音乐、绘制/闭合/吸入/飞行/碰撞/收缩/成功/失败音效；虫洞和移动物体使用官方 ObjectAudioComponent 距离衰减，音乐及播报使用环境音频。
- SharedPreferences 保存语言、音量、震动设置、教程状态、关卡星级和最快用时。

## 源码与证据

- `app/src/main/java/com/example/spacebend/domain`：独立于平台的几何、物理、关卡与评分。
- `app/src/main/java/com/example/spacebend/platform`：PICO 空间、输入、音频和本地存储适配。
- `app/src/main/java/com/example/spacebend/ui`：ViewModel、Compose 空间面板与自定义控件。
- `app/src/test`：几何、物理、持久化与导航回归；`app/src/androidTest`：真实 SDK 平面驱动的闭环冒烟测试。
- `assets/style-a`：素材源文件与预览；`app/src/main/assets`：实际运行资源。
- `docs/delivery/verification.md`：实际验证结果与边界。`output/screenshots`：官方 CLI 捕获的运行画面。
- `docs/delivery/launcher-identity/`：1.0.1编译后图标引用、分层/SDF资源检查及直接从APK提取的图标。`tools/assets/generate_launcher_identity.py`和`GenerateIconSdf.java`为离线资源导出工具；Android Studio插件仅参与资源制作，不作为APK运行依赖。

## 验证状态

34 项 JVM 回归通过，真实 SDK 平面驱动的 Android 闭环测试通过（1 项，包含加载与截图等待约 109 秒）。已观察到 57 个平面与三星结算。模拟器仍存在加载变慢和画面滞后，根因未确认；功能通过不代表实时性能通过。详细原始结果见 `docs/delivery/verification.md`。

2026-11-05 使用同一诊断 APK 复测 PICO 6.1 模拟器：官方平面接口返回 57 个平面，网格接口返回 48 个网格，专用感知检测 1 项通过。2026-11-11 已覆盖安装并启动至 PICO B3110 真机，但此前该头显与系统自带扫描均未获得有效平面，正常重启后应用仍返回 0；扫描故障尚未修复。详见 `docs/delivery/device-scan/`。投稿时应披露该体验前提，不能把模拟器数据表述为真机扫描成功。

2026-11-13换用另一台B3110后已取得20个真实平面；用户实测扫描可用，但1.0.1在绘制时因共享材质提前释放而崩溃。1.0.2已修复资源所有权并覆盖安装到新设备；两项新增真机资源生命周期回归由旧版失败变为通过，用户随后确认“绘制正常，不再闪退”。详见 `docs/delivery/drawing-crash/`。此确认限于本次绘制问题，完整玩法与性能仍需验收；旧设备的扫描状态不代表新设备。

`SpatialSensingProbeTest` 是显式启用的设备诊断，使用 `-e sensingProbe true`；默认跳过。请通过明确设备目标的 CLI 安装与运行，避免在用户头显上使用会清理/卸载被测应用的 Gradle connected-test 任务。

## 使用边界

碰撞以包围球保守估算物体边界，不是逐三角形刚体模拟。生成式概念图是美术方向参考，实际运行效果以 APK 截图为准。音效为原创程序合成，播报来自本机 TTS。模拟器验证不能替代真实头显上的扫描精度、手势识别、手柄输入/震动、双耳空间音频听感及性能测试；这些硬件体验未在本次开发机上实测。
