spec_mode: SLIM

# Change Summary
- What changed: `VideoUtils.ConvertToMP4` 对非 ts/flv 输入优先用 `-c copy`（仅 remux 容器），失败兜底原 libx264 重编码。
- Why: 非 ts/flv 视频（mov/mp4 变种/mkv 等）多数 H.264/AAC 已 mp4 友好，重编码纯属浪费 CPU（~30s → ~3s）。

# Scope of Change
- `Utils/VideoUtils.cs`
  - `ConvertToMP4`: 参数选择从单一字符串改为 primary+fallback；提取 Process 执行到新 helper
  - 新增 `TryRunFfmpegConvert(string args, string outputPath) → bool` 私有静态方法
- 不改 ConvertToMP4Async（异步版本未在本次范围；后续若复用 fallback 逻辑可在独立 spec 推进）

# Risk & Rollback
- Why LOW: ts/flv 路径行为字节级等价；非 ts/flv 成功路径更快（与改前结果相同）；失败路径 fallback 到改前的 libx264 命令；返回值契约不变（始终返回 outputDirectoryPath）；17 处调用方无需变更
- Rollback: 单文件 git checkout `Utils/VideoUtils.cs`

# Verification & Evidence
- Mac 不可编译；csharp-code-review 自检 PASS（方法长度 / 反模式 / 资源管理）
- Windows 端 A/B 关注：
  - **常见格式（mov/avi）** ：转换时间应从 ~30s 缩到 ~3s；输出 mp4 大小可能差异（remux 不重编码，原始码率保留）
  - **罕见编码组合** ：观察日志是否出现 `-c copy 失败，降级 libx264 重编码`；fallback 成功后下游识别行为应与改前一致
  - **17 处调用方** ：无需主动测试；契约不变，输出路径相同；任何回归都是合法 bug，应回退
- 失败回退条件：
  - 任意调用方出现"找不到 mp4 文件"类错误 → 回退
  - 任意素材出现明显画质问题且影响业务 → 回退（注意：本项目所有 mp4 用途都是抽音频 + 重切片，画质应无影响）
