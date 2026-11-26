# 空间折跃 / Space Bend

PICO Spatial MR 空间解谜游戏，包名 `com.example.spacebend`。风格 A「引力实验室」：石墨黑工程面板、冰蓝虫洞边缘、低饱和金属物体与琥珀色状态提示。Kotlin + Compose + PICO Spatial SDK 6.1.9，官方 Stage 模板，Mixed 透视模式。

## 测试与迭代

- 当前测试 APK：[GitHub 测试发布](https://github.com/DDDCC5/space-bend/releases/tag/v1.0.5)。1.0.5（versionCode 6）将体验调整为“选模式→扫描→绘制→选物发射→观察”，工作面板按阶段互斥显示。停止、普通返回与清空分开，教程融入实际操作。保留原有图标、分语言应用名、网格缓存与绘制闪退修复。详见 [1.0.5 流程验证](docs/delivery/flow-1.0.5/README.md)。
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

构建环境：Node.js 22、PICO CLI 0.7.0、JDK 21、Android SDK 35，项目运行时 BOM 为6.1.9。其他机器请先按官方流程配置 PICO CLI，并运行 `pico-cli --version` 与 `pico-cli doctor`。

## 操作

1. 在主页选择自由体验或关卡挑战；挑战先选关。本次空间尚未确认时进入扫描，识别到真实平面且场景就绪后点击“开始绘制”，直接继续所选模式。
2. 对准真实平面按住扳机或抓捏绘制闭合虫洞。自由体验至少两个，挑战按界面显示的本关要求。准备好后主动点击“选择物体”，也可以继续画更多虫洞。
3. 在选物页点击图标/名称，或左右拨杆切换。检查尺寸提示，再点击明确的“发射＋物体名”按钮。面板外扳机不发射。“返回绘制”保留布局；洞口过小需换较小物体，或删除并重画对应洞口。
4. 运行时观察传送与循环。“停止并调整”保留虫洞；“清空重画”才清空。碰撞仍使全部虫洞收缩消失；自由体验随后直接重画，挑战则显示结算。
5. 绘制时对准虫洞按握持键，在同一平面内移动；长按B/Y两秒删除。对准工作面板按握持键拖动，手势可捏合标题栏。下一步骤沿用相同工作位置。
6. 主页和设置临时暂停实验，返回后继续。另开非空实验会先确认清空原布局。短暂跟踪丢失时恢复扫描；原锚点失效会明确提示重画。
7. 新手引导嵌入以上步骤，依据实际绘制、传送与循环推进。中文两种文字各自独立，共用简体中文播报。

## 已实现内容

- 官方 PlaneTrackingManager 提供平面边界、孔洞、语义与锚点姿态；以稳定锚点局部坐标保存虫洞。跟踪丢失暂停模拟并引导重新扫描。
- 六种原始 GLB 科幻物体，独立质量、弹射速度和根据模型边界计算的保守碰撞半径。
- 任意简单闭合轮廓、引力边缘光、向内运动粒子、生成与收缩动画；手柄射线和手势输入适配，绘制与闭合震动。
- 固定 240 Hz 物理步长、9.8 m/s² 重力、近洞口引力、双侧进入/法线方向吐出、入口末速度保留、连续球体与有限平面面/边/顶点碰撞检测。
- 12 关、四个难度梯队、逐关解锁、三星规则、最快用时与星级分别保存；自由创作无胜负结算。
- 独立可拖拽的自定义切角空间面板，无原生圆角矩形按钮。当前任务互斥显示，教程嵌入工作步骤；普通返回保留布局。
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
