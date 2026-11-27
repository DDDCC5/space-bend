# 1.0.7 · 六种 Tripo 物体

包名 `com.example.spacebend`，版本 `1.0.7` / `8`，SDK 仍为 6.1.9。

用户要求用 Tripo 重做物件，并说明已在 Shatter 配置。查证后复用官方 Tripo CLI 0.11.4 的用户级海外站配置，未读取或拷贝密钥。编辑器缺少的是独立的中国区配置，本次无需修改它。通过官方 `doctor` 和六项 `make --dry-run` 后，提交六个 H3.1 `v3.1-20260211` 任务，每件一个候选，实际每件 60、合计 360 积分；没有重新生成或收费后处理。任务 ID、提示词与回执摘要见 [生成计划](../../assets/tripo-20261127/generation-plan.json) 和 [交付清单](../../assets/tripo-20261127/delivery-manifest.json)。

六种模型为探测机器人、能量核心、小型航天器、微型探测器、重型舱体和信标装置。全部带颜色、金属粗糙度及法线贴图；最长边整理到 1024，离线生成 MikkTSpace 切线，对少量零面积 UV 角点补充正交单位切线。标准 glTF 2.0，所有资源内嵌，不需要压缩或材质扩展。六个文件经 Khronos glTF Validator 检查，零错误、零警告。

实际 GLB 已在 WebGL 中逐件审阅；有方向的物件根据实际正反面证据从原始 +X 对齐到游戏 −Z。均匀缩放至现有碰撞半径的 98%，质量、发射速度及碰撞半径不变。由于 SDK 原生导入结果与离线测量有微小中心差异，加载后用 `getVisualBounds(owner)` 在运动父节点坐标中再次居中；修复前能量核心的约 5 mm 偏移证据保留在 `native-centering-before.json`。修复后真机六项中心近零、包围球都小于碰撞球，见 `native-bounds.json`。

验证结果：

- Gradle `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 通过；81 项 JVM 测试，零失败。
- 最终真机自动交互 3 项通过（23.157 秒），覆盖现有主流程、主动重扫和固定平面快照；启动时六个 PBR GLB 均已原生加载。
- 最终 APK 的六个 GLB 和 `model-bounds.json` 与审阅版本逐字节一致；原有分语言应用名、图标及 v2 签名检查通过。
- 已通过 `--replace` 覆盖安装至当前真机，保留应用数据。安装信息见 `installed-version.json`。
- 独立代码复核无待修问题；没有发现密钥或带签名下载链接进入待提交文件。

预览：[六件总览](../../../assets/tripo/review/contact-sheet.png)，[可旋转的实际 GLB 预览](../../../assets/tripo/review/index.html)。总览使用 Tripo 输出的模型渲染图，不是头显截图；实际头显材质观感和手柄手感仍待用户体验。没有把浏览器预览或流程测试当作性能基准。

本地模型包 `output/SpaceBend-Tripo-models.zip` 含六个最终 GLB、总览与来源清单。原始下载、任务原始回执、编辑器状态和 CLI 认证缓存均被 Git 忽略。离线工具依赖以 `tools/assets/tripo-runtime/package-lock.json` 固定，执行 `npm ci --prefix tools/assets/tripo-runtime` 可安装；生成脚本不会重复提交已有任务。

官方工具参考：[Tripo CLI](https://developers.tripo3d.ai/zh/docs/cli)、[glTF Transform 切线处理](https://gltf-transform.dev/modules/functions/functions/tangents)。运行 APK 不依赖这些生成工具或网络服务。
