# 空间折跃 / Space Bend · 风格 A 素材审阅包

阶段：用户已选定风格 A，并授权直接开发。原生 Spatial 项目与运行证据见项目根 README.md。此目录保留素材源文件与预览。

先打开 preview.html，可切换三语 UI、查看虫洞动态并试听全部音频。
打开 models-preview.html，可拖拽旋转六种真实网格并分别下载 GLB。

## 内容

- models/：六个 Tripo H3.1 高细节 PBR 模型，统一米制、Y 向上，并将有方向的模型对齐游戏 −Z 前方。每件约 1.2 万三角面，保留颜色、金属粗糙度与法线贴图，贴图最长边 1024；补齐 MikkTSpace 切线。保守碰撞半径、物体质量和发射速度沿用原游戏。完整任务与参数记录见根目录 docs/assets/tripo-20261127/。
- vfx/wormhole-reference.glb：包含 Portal_Close（0.5 秒）、Portal_Suction（1.6 秒）、Portal_Collapse（0.8 秒）真实 glTF 动画。固定不规则参考轮廓，不能代替运行时用户绘制轮廓。运行时使用真实平面锚定、动态边缘光与吸入粒子；未实现背景引力透镜折射。
- vfx/wormhole-cycle.svg：8 秒矢量视觉方向预览，含闭合、吸入、收缩。它不是运行截图或 GLB 动画播放器。
- ui/：8 种面板稿 × 3 语种，共 24 个 SVG；包括主菜单、关卡、物体、成功/失败结算、设置、扫描、教程。另含按钮五态、进度五态、10 个图标、2 个星级、控件总览。全为自定义矢量轮廓。
- locales/：每语种 74 个同名键，包括 UI 文案与 12 条语音脚本。
- audio/：48 kHz、16-bit PCM WAV。32 秒立体声环境音乐、8 种单声道音效、中文与英文各 12 条播报，共 33 个 WAV；繁体中文复用简体中文语音，繁体文案单独保留。音效为程序合成；1.0.6 起播报为离线神经语音，中文 Xiaoxiao、英文 Aria，中文音色已获用户认可。
- reference/：imagegen 生成的风格参考图。仅用于视觉方向，不是实际模型渲染或设备运行截图。
- previews/：本地浏览器渲染的素材截图，不是 PICO 设备截图。

## 参数与待确认项

物体质量、初始速度、界面里的星级、记录和关卡解锁数据均为审阅示例。object-visual-catalog.json 中参数尚未获用户批准。

第三步已经验证六个 GLB 在 PICO SDK 中加载及空间面板、虫洞运行。真实头显上的声音方位、材质观感、扫描精度和视觉舒适度仍待实测；素材预览不能替代设备运行证据。

当前六件物体使用官方 Tripo CLI 0.11.4 的海外站账号生成，实际合计 360 积分。认证由 CLI 用户级配置管理，没有把 Key 放进项目。旧版程序化造型仅为历史版本，tools/assets/build_models.py 不用于重建当前 Tripo 物件。虫洞参考 GLB 仍由 build_portal.py 制作。

## 验证

六个新物件 GLB 均通过 Khronos glTF Validator，0 errors / 0 warnings。旧虫洞参考模型保持原版本。
全部 SVG XML 可解析；三语键集合相同；48 kHz WAV 非空且无满幅削波；24 个面板 SVG 的文本未越出画布。详见项目 docs/assets 下验证 JSON。

审阅重点：模型造型与写实细节、UI 布局与文字、虫洞光色与节奏、音乐/音效、三语发音和播报音色。
用户后续已明确授权直接进入第三步；当前设备验证结果以 docs/delivery/verification.md 为准。
