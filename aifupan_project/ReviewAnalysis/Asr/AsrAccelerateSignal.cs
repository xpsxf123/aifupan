using System.Collections.Concurrent;

namespace ReviewAnalysis.Asr
{
    /// <summary>
    /// 录播 ASR「加速」信号：进程级、按 videoId 键入，表示
    /// 「该视频尚未识别的剩余分片改走腾讯云」。
    ///
    /// 有效期严格锁定在 ASR **识别循环真正在跑** 的窗口内（<see cref="_running"/>），
    /// 而非上层粗粒度的 isAnalysis 标志——后者在 ASR 前置真、ASR 后好几个 await 才置假，
    /// 会导致「ASR 已跑完但 isAnalysis 仍为 true」窗口内登记的僵尸信号残留，污染下次重分析。
    ///
    /// 生命周期：
    ///   <see cref="MarkRunning"/>（RunLocalEngineAsync 进入分片循环前）
    ///     → 前端 /api/anchorvideo/accelerate → <see cref="TryRequest"/> 登记（仅运行中才接受）
    ///     → 分片循环 <see cref="IsRequested"/> 消费
    ///     → 识别结束 finally 先 <see cref="MarkStopped"/> 再 <see cref="Clear"/>。
    ///
    /// 全部用 ConcurrentDictionary，HTTP 线程与分析线程并发访问安全（值无意义，仅用 Key 做集合语义）。
    /// 内存标记：进程重启后丢失，重启后该视频按默认引擎路由识别。
    /// </summary>
    public static class AsrAccelerateSignal
    {
        private static readonly ConcurrentDictionary<string, byte> _signals =
            new ConcurrentDictionary<string, byte>();

        // 当前 ASR 识别循环正在处理的 videoId 集合（录播单槽，通常至多 1 个）。
        private static readonly ConcurrentDictionary<string, byte> _running =
            new ConcurrentDictionary<string, byte>();

        /// <summary>标记该 videoId 的 ASR 识别循环已开始（进入分片循环前调用）。</summary>
        public static void MarkRunning(string videoId)
        {
            if (!string.IsNullOrEmpty(videoId))
                _running[videoId] = 1;
        }

        /// <summary>标记该 videoId 的 ASR 识别循环已结束（finally 中先于 Clear 调用，先拒绝后续请求）。</summary>
        public static void MarkStopped(string videoId)
        {
            if (!string.IsNullOrEmpty(videoId))
                _running.TryRemove(videoId, out _);
        }

        /// <summary>
        /// 请求加速：仅当该 videoId 的 ASR 识别循环正在跑时才登记信号并返回 true；
        /// 否则（空 / 未在识别 / 已结束）返回 false 且不登记，杜绝僵尸信号残留。
        /// </summary>
        public static bool TryRequest(string videoId)
        {
            // 在asr分析前也能点击加速，比如：转MP4，切割音频。。
            // if (string.IsNullOrEmpty(videoId) || !_running.ContainsKey(videoId))
            if (string.IsNullOrEmpty(videoId))
                return false;
            _signals[videoId] = 1;
            return true;
        }

        /// <summary>该 videoId 是否已请求加速。</summary>
        public static bool IsRequested(string videoId)
        {
            return !string.IsNullOrEmpty(videoId) && _signals.ContainsKey(videoId);
        }

        /// <summary>清除该 videoId 的加速信号；识别结束时调用，防止信号残留影响后续重分析。</summary>
        public static void Clear(string videoId)
        {
            if (!string.IsNullOrEmpty(videoId))
                _signals.TryRemove(videoId, out _);
        }
    }
}
