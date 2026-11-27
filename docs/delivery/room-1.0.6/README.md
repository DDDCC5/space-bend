# 1.0.6 稳定空间、启动定位与语音

版本 `1.0.6` / `7`，包名 `com.example.spacebend`，SDK 保持 6.1.9。

扫描阶段以 0.4 秒采样合并共面相连区域，保留凹边界、孔洞与不相连的空隙，不使用凸包扩张。法线夹角容差 1°，双向边界到平面距离容差 1.5 cm；不同已知语义不合并。用户点击“开始绘制”后固定当前几何，取消 SDK 订阅、停止扫描并取消快照轮询。绘制、选物、运行、回主页不会隐式解锁；主动重扫才采集新数据。已有虫洞所在的锚点优先保留坐标基准。

工作面板不再固定在世界坐标。首次有效 HMD 位姿到来时，在其视线前方 1.45 m 放置并朝向用户；在此之前不显示默认位置的面板。恢复前台等待新会话的 HMD 回调后重新定位（不会读取暂停前缓存位姿），正常操作中不跟随头部。射线命中使用面板局部轴，支持转向、俯仰和拖动。

24 条提示全部重制：中文 `zh-CN-XiaoxiaoNeural`，英文 `en-US-AriaNeural`，方向为平静、自然、略带温度的实验引导女声；短句、适度放慢、保留句内停顿，不加金属回声或声码器。48 kHz / 16-bit / 单声道 WAV，响度与峰值统一处理。简繁文字区分，语音共用 `zh-Hans`；运行时无需 TTS 服务或联网。生成脚本为 `tools/assets/redesign_voice.py`，完整来源与声学校验见 `voice-validation.json`。非人工配音；用户已反馈“声音是可以的”，确认中文音色可用。

验证：

- Gradle `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 通过；81 项 JVM 测试，零失败。
- 真机 3 项自动交互测试通过（25.965 秒）：主流程、主动重扫及有效坐标保持、确认后的 SDK 平面连续 8 秒不变。见 `native-final.txt`。
- 本次实际观察 16 个原始区域合并为 15 个有效平面。HMD/面板相对位姿计算确认距离 1.45 m、朝向点积 1。见 `runtime-validation.json`。
- 最终 APK 内 24 条 WAV 与源文件逐字节一致；JTS 第三方许可已随包。图标、按语言区分的应用名和 v2 签名检查通过，见 `apk-identity/` 与 `signature.txt`。
- 已通过 `--replace` 覆盖安装并启动，未卸载应用或清除数据。见 `installed-version.json`。

验证边界：原先尝试的 ActivityScenario `STARTED` 强制状态切换在 PICO Full Space 中超时，失败记录保留在 `native-lifecycle-harness-failure.txt`；最终测试改为观察固定快照，未宣称验证系统后台恢复。真机 FOV 截图只呈现透视画面，没有应用面板，私有保存在 `.scratch`，不公开房间照片，也不以其证明 UI 位置。中文音色已获用户确认；实际佩戴面板位置、英文听感与手柄/手势手感待反馈。本次没有执行 Perfetto 性能基准。

多边形并集使用 JTS Core 1.20.0；项目与 APK 的 `assets/third-party/jts-1.20.0/` 保留上游许可。
