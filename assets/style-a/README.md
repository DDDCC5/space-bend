# 空间折跃 / Space Bend · 风格 A 素材审阅包

阶段：用户已选定风格 A，并授权直接开发。原生 Spatial 项目与运行证据见项目根 README.md。此目录保留素材源文件与预览。

先打开 preview.html，可切换三语 UI、查看虫洞动态并试听全部音频。
打开 models-preview.html，可拖拽旋转六种真实网格并分别下载 GLB。

## 内容

- models/：6 个独立 GLB，原创程序化硬表面网格，米制、Y 轴向上、−Z 为语义前方。共用钛金属、石墨、陶瓷、冰蓝发光、琥珀标记五类 PBR 材质。未制作写实磨损贴图，当前造型与写实概念图有细节差距，须审阅。
- vfx/wormhole-reference.glb：包含 Portal_Close（0.5 秒）、Portal_Suction（1.6 秒）、Portal_Collapse（0.8 秒）真实 glTF 动画。固定不规则参考轮廓，不能代替运行时用户绘制轮廓。运行时使用真实平面锚定、动态边缘光与吸入粒子；未实现背景引力透镜折射。
- vfx/wormhole-cycle.svg：8 秒矢量视觉方向预览，含闭合、吸入、收缩。它不是运行截图或 GLB 动画播放器。
- ui/：8 种面板稿 × 3 语种，共 24 个 SVG；包括主菜单、关卡、物体、成功/失败结算、设置、扫描、教程。另含按钮五态、进度五态、10 个图标、2 个星级、控件总览。全为自定义矢量轮廓。
- locales/：每语种 74 个同名键，包括 UI 文案与 12 条语音脚本。
- audio/：48 kHz、16-bit PCM WAV。32 秒立体声环境音乐、8 种单声道音效、中文与英文各 12 条播报，共 33 个 WAV；繁体中文复用简体中文语音，繁体文案单独保留。音频均本地合成；中文使用系统已安装 Binbin，英文 Samantha。当前为系统合成音色，需听审确认；不是专业真人录音。
- reference/：imagegen 生成的风格参考图。仅用于视觉方向，不是实际模型渲染或设备运行截图。
- previews/：本地浏览器渲染的素材截图，不是 PICO 设备截图。

## 参数与待确认项

物体质量、初始速度、界面里的星级、记录和关卡解锁数据均为审阅示例。object-visual-catalog.json 中参数尚未获用户批准。

第三步已经验证六个 GLB 在 PICO SDK 中加载及空间面板、虫洞运行。真实头显上的声音方位、材质观感、扫描精度和视觉舒适度仍待实测；素材预览不能替代设备运行证据。

官方 Spatial Editor 工作流在制作前因内置 Python TLS 证书链校验失败而终止。系统证书库在单独子进程测试有效，但后台工作进程没有继承；随后会话清理/取消。未伪造 Editor bundle、provenance 或设备证据。六个 GLB 由 tools/assets/build_models.py 原创生成，虫洞 GLB 由 build_portal.py 生成。

## 验证

7 个 GLB 已通过 Khronos glTF Validator：0 errors / 0 warnings。
全部 SVG XML 可解析；三语键集合相同；48 kHz WAV 非空且无满幅削波；24 个面板 SVG 的文本未越出画布。详见项目 docs/assets 下验证 JSON。

审阅重点：模型造型与写实细节、UI 布局与文字、虫洞光色与节奏、音乐/音效、三语发音和播报音色。
用户后续已明确授权直接进入第三步；当前设备验证结果以 docs/delivery/verification.md 为准。
