using douyin.Utils;
using Microsoft.ML.OnnxRuntime;
using System;
using System.Runtime.ExceptionServices;
using System.Security;
using System.Threading;

namespace ReviewAnalysis.Asr.Local
{
    /// <summary>
    /// 进程级 GPU 可用性闸门。SVS 在 GPU 上推理失败一次后，本次进程剩余时间内所有新建 GpuProbe
    /// 都直接返回 CPU，避免每次音频都重复"GPU 失败 → 降级"的体验损失。应用重启自动恢复（不持久化）。
    /// </summary>
    public static class GpuRuntimeState
    {
        private static int _disabled; // 0=允许 GPU; 1=本次进程禁用

        public static bool GpuDisabled => Volatile.Read(ref _disabled) != 0;

        /// <summary>幂等：首次设置时打 log，后续重复调用静默。</summary>
        public static void DisableGpuForProcess(string reason)
        {
            if (Interlocked.Exchange(ref _disabled, 1) == 0)
                FileUtils.LogAnalysis($"[GpuProbe] GPU 已禁用 (本次进程内): {reason}");
        }
    }

    public class GpuProbe
    {
        public enum Provider { Cpu, Cuda, DirectML }

        public Provider BestProvider { get; private set; } = Provider.Cpu;
        // DirectML 时为独显的 DXGI 枚举索引（不一定是 0）；CPU/CUDA 时为 0。
        public int DeviceId { get; private set; } = 0;

        public GpuProbe()
        {
            // sticky: 本次进程内已被禁用 → 直接 CPU，不再做 GPU 探测
            if (GpuRuntimeState.GpuDisabled)
            {
                BestProvider = Provider.Cpu;
                return;
            }

            // Priority: CUDA > DirectML > CPU
            if (TryProvider("CUDA", 0, (opts, id) => opts.AppendExecutionProvider_CUDA(id)))
            {
                BestProvider = Provider.Cuda;
                return;
            }
            // 「仅独显」策略：解析出独显的 DXGI 索引（专用显存最大且 ≥ 阈值的非软件适配器）。
            // 核显（Intel UHD/HD、AMD APU）专用显存通常 < 阈值 → 返回 -1 → 直接 CPU，绝不在核显上跑 DML。
            // 注意：device_id 用独显的 DXGI 索引，而非固定 0——双卡机上 0 号常是核显，固定 0 会误用核显。
            int discreteIndex = ResolveDiscreteAdapterIndex();
            if (discreteIndex >= 0
                && TryProvider("DirectML", discreteIndex, (opts, id) => opts.AppendExecutionProvider_DML(id)))
            {
                DeviceId = discreteIndex;
                BestProvider = Provider.DirectML;
                return;
            }
            BestProvider = Provider.Cpu;
            FileUtils.LogAnalysis("[GpuProbe] 所有 GPU EP 探测失败，降级 CPU（详情见上方 [GpuProbe] 日志）");
        }

        public void ApplyTo(SessionOptions options)
        {
            switch (BestProvider)
            {
                case Provider.Cuda:
                    options.AppendExecutionProvider_CUDA(DeviceId);
                    break;
                case Provider.DirectML:
                    // ORT 硬约束：使用 DirectML EP 时必须禁用 MemoryPattern，否则 InferenceSession 构造会抛异常
                    options.EnableMemoryPattern = false;
                    // DML 1.12 (随 ORT 1.16) 对部分 fused 算子（如 SenseVoice 带 mask 的 self-attn Softmax）覆盖不全，
                    // 把图优化从 ENABLE_ALL 降到 ENABLE_EXTENDED 规避不兼容的最终阶段融合
                    options.GraphOptimizationLevel = GraphOptimizationLevel.ORT_ENABLE_EXTENDED;
                    options.AppendExecutionProvider_DML(DeviceId);
                    // CPU 兜底 EP：DML 仍跑不动的节点（如某些 Softmax 变体）自动落到 CPU 而非抛 RuntimeException
                    options.AppendExecutionProvider_CPU();
                    break;
                default:
                    // 线程数由 OfflineModel.CreateBaseOptions 统一设定，这里不再覆盖（避免两处打架）。
                    options.AppendExecutionProvider_CPU();
                    break;
            }
        }

        /// <summary>
        /// 解析「独显」适配器的 DXGI 索引（= DirectML device_id），用于把 DML 精确绑定到独显、禁用核显。
        /// 判定：专用显存最大的非软件适配器，且其显存 ≥ 阈值（默认 2048MB，可经 svs_model_config.json 覆盖）才算独显。
        /// 返回 -1 表示「无独显」，调用方据此强制 CPU：
        ///   - 显存 &lt; 阈值 → 判定核显/低端 GPU，禁用核显，走 CPU；
        ///   - 显存或适配器索引探测失败 → 无法确认为独显，按「仅独显」严格策略走 CPU（不退而求其次用核显）。
        /// </summary>
        private static int ResolveDiscreteAdapterIndex()
        {
            long vramMB = HardwareProbe.GetMaxDedicatedVideoMemoryMB();
            int index = HardwareProbe.GetMaxVramAdapterIndex();
            int minMB = HardwareProbe.GetDirectMlMinVramMB();
            if (vramMB < 0 || index < 0)
            {
                FileUtils.LogAnalysis($"[GpuProbe] 未探测到独显（显存/适配器索引未知），按\"仅独显\"策略强制 CPU (阈值={minMB}MB)");
                return -1;
            }
            if (vramMB < minMB)
            {
                FileUtils.LogAnalysis($"[GpuProbe] 最大专用显存 {vramMB}MB < 阈值 {minMB}MB → 判定核显/低端 GPU，禁用核显，强制 CPU");
                return -1;
            }
            FileUtils.LogAnalysis($"[GpuProbe] 检测到独显（适配器#{index}, 专用显存 {vramMB}MB ≥ 阈值 {minMB}MB）→ DirectML 绑定该独显");
            return index;
        }

        // [HandleProcessCorruptedStateExceptions]+[SecurityCritical]：让本方法的 catch 能接住
        // AccessViolationException 等 Corrupted State Exception。native 层 onnxruntime 的
        // AppendExecutionProvider_DML/_CUDA 在驱动/运行库不兼容时可能抛 AVE（默认逃逸出普通 catch，
        // 导致整个 GpuProbe → SVS → 工厂初始化失败）。这里捕获后 sticky 禁用 GPU 并降级 CPU。
        [HandleProcessCorruptedStateExceptions]
        [SecurityCritical]
        private static bool TryProvider(string providerName, int deviceId, Action<SessionOptions, int> append)
        {
            try
            {
                using (var opts = new SessionOptions())
                {
                    append(opts, deviceId);
                }
                FileUtils.LogAnalysis($"[GpuProbe] {providerName} 探测成功 (device={deviceId})");
                return true;
            }
            catch (AccessViolationException ex)
            {
                // native 内存访问异常（provider 与驱动/运行库不兼容）：本次进程粘性禁用 GPU，
                // 避免后续每个音频重复触发崩溃式探测，本进程剩余时间全部走 CPU 推理。
                FileUtils.LogAnalysis($"[GpuProbe] {providerName} 触发内存访问异常(AVE)，本进程禁用 GPU 并降级 CPU: {ex.Message}");
                GpuRuntimeState.DisableGpuForProcess($"{providerName} AccessViolationException");
                return false;
            }
            catch (Exception ex)
            {
                // 暴露真实失败原因：DllNotFoundException 通常是 NuGet 没把 DirectML.dll/CUDA dll 拷到 bin\x64\Debug\；
                // OnnxRuntimeException 通常是驱动/系统版本不满足或硬件不支持。
                // EntryPointNotFoundException（如缺 CUDA EP 入口点）属正常"无此 provider"，继续探测下一个。
                FileUtils.LogAnalysis($"[GpuProbe] {providerName} 不可用: {ex.GetType().Name} - {ex.Message}");
                return false;
            }
        }
    }
}
