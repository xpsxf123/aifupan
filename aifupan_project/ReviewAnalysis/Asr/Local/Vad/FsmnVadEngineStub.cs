using douyin.Utils;

namespace ReviewAnalysis.Asr.Local.Vad
{
    /// <summary>
    /// VAD 引擎桩实现：始终返回 IsReady=false。
    ///
    /// 用于 Phase 1 架构落地阶段——让 AudioUtils 的智能切片代码路径完整就位，但默认走降级到
    /// 旧硬切，保证零行为变化。Phase 2 会以真正的 FsmnVadEngine（基于 ModelScope
    /// iic/speech_fsmn_vad_zh-cn-16k-common-onnx 模型）替换此桩。
    ///
    /// 替换方式：在 Program.cs 启动逻辑里 `AudioUtils.VadEngine = new FsmnVadEngine(...);`
    /// </summary>
    public class FsmnVadEngineStub : IVadEngine
    {
        public bool IsReady => false;

        public VadSegment[] GetSegments(float[] pcm16kMono)
        {
            FileUtils.LogAnalysis("[VAD] Stub 引擎被调用 (IsReady=false)，调用方应走降级路径");
            return new VadSegment[0];
        }
    }
}
