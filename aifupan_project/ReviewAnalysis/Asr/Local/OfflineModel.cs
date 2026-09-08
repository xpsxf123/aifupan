using douyin.Utils;
using Microsoft.ML.OnnxRuntime;
using System;

namespace ReviewAnalysis.Asr.Local
{
    public class OfflineModel : IDisposable
    {
        private InferenceSession _modelSession;
        private bool _disposed;

        public GpuProbe.Provider ActiveProvider { get; private set; }

        public OfflineModel(string modelFilePath, GpuProbe probe)
        {
            var options = CreateBaseOptions();
            try
            {
                probe.ApplyTo(options);
                _modelSession = new InferenceSession(modelFilePath, options);
                ActiveProvider = probe.BestProvider;
                FileUtils.LogAnalysis($"[Model] ONNX EP={ActiveProvider}, DeviceId={probe.DeviceId}");
            }
            catch (Exception ex) when (probe.BestProvider != GpuProbe.Provider.Cpu)
            {
                // GPU EP reported available but session creation failed — fall back to CPU
                FileUtils.LogAnalysis($"[Model] GPU({probe.BestProvider})初始化失败: {ex.Message}, 降级到CPU");
                options.Dispose();
                var cpuOptions = CreateBaseOptions();
                cpuOptions.AppendExecutionProvider_CPU();
                _modelSession = new InferenceSession(modelFilePath, cpuOptions);
                ActiveProvider = GpuProbe.Provider.Cpu;
                FileUtils.LogAnalysis($"[Model] ONNX EP={ActiveProvider} (fallback)");
            }
        }

        /// <summary>
        /// 创建通用的 SessionOptions（线程配置的**唯一来源**——GpuProbe.ApplyTo 不再覆盖）：
        ///   - GraphOptimizationLevel.ORT_ENABLE_ALL：开启全部图优化（融合 Conv+BN、常量折叠等）
        ///   - ExecutionMode.ORT_SEQUENTIAL：顺序执行（图级并行通常无收益且耗内存）
        ///   - IntraOpNumThreads：算子内并行线程数，取 HardwareProbe 推荐值（按核数分档：≤4核取半核、
        ///     ≥5核只留2核给录制/其他软件，保证 CPU 不被吃满卡死）。此前 OfflineModel 设 cores/2、
        ///     GpuProbe.ApplyTo 又改写成 2，两处打架导致 OfflineModel 的设置被静默覆盖——现统一收口到这里。
        ///   - InterOpNumThreads = 1：禁用图节点并行，与 ORT_SEQUENTIAL 配合（设 >1 在 SEQUENTIAL 下无效）
        /// 兼容性：所有选项均为 ONNX Runtime 标准 API，CPU/CUDA/DirectML 皆适用。
        /// </summary>
        private static SessionOptions CreateBaseOptions()
        {
            var options = new SessionOptions();
            options.GraphOptimizationLevel = GraphOptimizationLevel.ORT_ENABLE_ALL;
            options.ExecutionMode = ExecutionMode.ORT_SEQUENTIAL;
            options.IntraOpNumThreads = HardwareProbe.GetRecommendedIntraOpThreads();
            options.InterOpNumThreads = 1;
            return options;
        }

        public InferenceSession ModelSession { get { return _modelSession; } }

        public void Dispose()
        {
            if (!_disposed)
            {
                _modelSession?.Dispose();
                _disposed = true;
            }
            GC.SuppressFinalize(this);
        }
    }
}
