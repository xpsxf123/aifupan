# Rules WAL: 本地 ASR 引擎 — 经验教训与约束

**Date**: 2026-05-14
**Source**: Phase 1-3 implementation
**Type**: rules

## New Constraints

### [ONNX Runtime 版本锁定]
- **Rule**: 本项目使用 ONNX Runtime 1.16.3，不可升级到 1.17+
- **Why**: 1.17+ 仅支持 .NET 6+，本项目为 .NET Framework 4.7.2
- **How to apply**: NuGet 版本号固定为 1.16.3，PR 中不可修改此依赖

### [ONNX 推理串行化]
- **Rule**: ONNX InferenceSession.Run() 必须通过 `static SemaphoreSlim(1)` 串行调用
- **Why**: ONNX Runtime 1.16.3 的 InferenceSession 不是线程安全的，多线程并发推理会导致崩溃或错误结果
- **How to apply**: 所有调用 `_offlineModel.ModelSession.Run()` 的地方必须包裹 `await _inferenceLock.WaitAsync()` / `finally { _inferenceLock.Release() }`

### [模型文件无远端 manifest]
- **Rule**: 模型下载不依赖远端 manifest.json，文件列表硬编码，SHA256 本地计算
- **Why**: ModelScope (`iic/SenseVoiceSmall-onnx`) 不提供 manifest.json 文件，只能逐个下载后计算校验和
- **How to apply**: `ModelFiles` 数组硬编码文件名列表，`DownloadAsync()` 下载完成后调用 `ComputeSha256()` 生成本地 manifest.json

### [下载 URL 可配置不可硬编码]
- **Rule**: 模型下载 URL 从 `svs_model_config.json` 读取，不存在时写入默认值
- **Why**: 未来可能切换 CDN 或镜像源，硬编码会导致无法更新
- **How to apply**: `ModelManager.LoadDownloadUrl()` 读取配置文件，默认值为 ModelScope API URL

### [.part 文件原子替换]
- **Rule**: 下载使用 .part 临时文件，完成校验后 rename 为正式文件
- **Why**: 防止下载中断导致使用损坏的模型文件，rename 在同一文件系统内是原子操作
- **How to apply**: `DownloadFileWithRetry()` 先写 .part → SHA256 校验 → File.Delete(finalPath) → File.Move(partPath, finalPath)

## Anti-Patterns Learned

- **禁止跳过 SHA256 校验直接使用模型文件**: 文件损坏会导致 ONNX 推理崩溃，难以排查
- **禁止在 ONNX 推理期间触发模型下载**: 推理和下载使用不同的锁，但下载会修改模型文件，推理中下载会导致文件被替换
- **禁止使用 `AppendExecutionProvider_CPU(0)`**: deviceId=0 会禁用 arena allocator，应使用无参数版本 `AppendExecutionProvider_CPU()`
