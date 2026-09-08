using System.Collections.Generic;
using System.Linq;

namespace ReviewAnalysis.Asr.Local.Vad
{
    /// <summary>
    /// 基于 VAD 语音段输出，规划音频智能切分点。无状态算法，纯函数。
    ///
    /// 设计目标（per spec 20260526_fsmn_vad_smart_slicing.md §5.2）：
    ///   ① 切点优先落在相邻语音段之间的 silence gap 中点
    ///   ② 每段长度 ≤ MaxChunkMs（默认 58s，给 SVS 60s 硬限留余量）
    ///   ③ 每段长度 ≥ MinChunkMs（默认 45s，避免摊不开 SVS 加载开销）
    ///   ④ **必须**输出覆盖整段音频（不丢任何区间，含纯 BGM 段）—— BGM 保留约束
    /// </summary>
    public static class SmartSlicer
    {
        public const int DefaultMaxChunkMs = 58000;
        public const int DefaultMinChunkMs = 45000;

        /// <summary>切分计划：一段从 StartMs 到 EndMs（左闭右开）。</summary>
        public struct CutPlan
        {
            public long StartMs;
            public long EndMs;
            public CutPlan(long s, long e) { StartMs = s; EndMs = e; }
            public long DurationMs => EndMs - StartMs;
        }

        /// <summary>
        /// 给定 VAD 语音段和总时长，规划切分点。
        /// </summary>
        /// <param name="speechSegments">VAD 输出的语音段（按起始时间排序）。可空。</param>
        /// <param name="totalDurationMs">音频总时长（ms）</param>
        /// <param name="maxChunkMs">单段最大时长。超过则强制硬切。</param>
        /// <param name="minChunkMs">单段最小切点偏移（避免切得过早）。</param>
        /// <returns>切分计划列表。覆盖 [0, totalDurationMs)。</returns>
        public static List<CutPlan> PlanCuts(
            IList<VadSegment> speechSegments,
            long totalDurationMs,
            int maxChunkMs = DefaultMaxChunkMs,
            int minChunkMs = DefaultMinChunkMs)
        {
            var plans = new List<CutPlan>();
            if (totalDurationMs <= 0) return plans;

            // 计算 silence gaps（相邻语音段之间的非语音区间）
            // gaps[i] = (gapStartMs, gapEndMs)，按起始时间排序
            var gaps = new List<(long start, long end)>();
            if (speechSegments != null && speechSegments.Count > 0)
            {
                var sorted = speechSegments.OrderBy(s => s.StartMs).ToList();
                long prevEnd = 0;
                foreach (var seg in sorted)
                {
                    if (seg.StartMs > prevEnd)
                        gaps.Add((prevEnd, seg.StartMs));
                    if (seg.EndMs > prevEnd) prevEnd = seg.EndMs;
                }
                if (prevEnd < totalDurationMs)
                    gaps.Add((prevEnd, totalDurationMs));
            }

            long chunkStart = 0;
            while (chunkStart < totalDurationMs)
            {
                long remaining = totalDurationMs - chunkStart;
                if (remaining <= maxChunkMs)
                {
                    // 剩余在一段内，直接收尾
                    plans.Add(new CutPlan(chunkStart, totalDurationMs));
                    break;
                }

                // 在 [chunkStart + minChunkMs, chunkStart + maxChunkMs] 窗口内找最居中的 gap
                long windowLo = chunkStart + minChunkMs;
                long windowHi = chunkStart + maxChunkMs;
                long target = chunkStart + maxChunkMs; // 目标切点尽量靠后但不超 max

                long? bestCut = null;
                long bestDist = long.MaxValue;
                foreach (var g in gaps)
                {
                    long mid = (g.start + g.end) / 2;
                    if (mid < windowLo || mid > windowHi) continue;
                    long dist = mid > target ? mid - target : target - mid;
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        bestCut = mid;
                    }
                }

                long cutAt = bestCut ?? windowHi; // 无 gap 候选 → 硬切到 maxChunkMs
                plans.Add(new CutPlan(chunkStart, cutAt));
                chunkStart = cutAt;
            }

            return plans;
        }
    }
}
