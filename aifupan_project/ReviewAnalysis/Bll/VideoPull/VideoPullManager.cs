using ReviewAnalysis.Bll.VideoPull.Models;
using ReviewAnalysis.Bll.VideoPull.Juliang;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using ReviewAnalysis.api;
using douyin.Utils;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Threading.Tasks;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Bll.VideoPull
{
    public class VideoPullManager
    {
        /// <summary>
        /// 按平台拉取直播记录 → 映射 → 保存到服务器
        /// </summary>
        /// <param name="secUid">主播 sec_uid</param>
        /// <param name="platform">平台标识: juliang / laike</param>
        public async Task PullAndSave(string secUid, string platform = "juliang")
        {
            IVideoPuller puller = ResolvePuller(platform);

            if (!puller.IsAuthorized(secUid))
            {
                FileUtils.LogRpa($"{platform}==secUid = {secUid}== 未授权，无法拉取直播记录", "VideoPull");
                throw new CustomException("未授权，无法拉取直播记录");
            }

            int pullCount = KvHelper.GetIntKvByKey("video_pull_count", 7);
            int pullDay = KvHelper.GetIntKvByKey("video_pull_day", 3);

            // 查询当前在播直播间，多拉一条补偿，后续过滤掉
            string currentLiveId = await puller.GetCurrentLiveRoomAsync(secUid);
            if (!string.IsNullOrEmpty(currentLiveId))
            {
                pullCount++;
                FileUtils.LogRpa($"当前在播: live_id={currentLiveId}, pullCount调整为{pullCount}", "VideoPull");
            }

            var result = await puller.GetRecentLiveSessionsAsync(secUid, pullCount, pullDay);

            if (!result.Success)
            {
                FileUtils.LogRpa($"{platform} ==secUid = {secUid}== 拉取失败: {result.ErrorCode} - {result.Error}", "VideoPull");
                throw new CustomException($"拉取失败: {result.ErrorCode} - {result.Error}");
            }

            if (result.Data == null || result.Data.Count == 0)
            {
                FileUtils.LogRpa($"{platform} ==secUid = {secUid}== 拉取结果为空，无直播记录", "VideoPull");
                throw new CustomException("拉取结果为空，无直播记录");
            }

            // 去掉当前在播的直播间（正在直播的不应纳入录制计划）
            if (!string.IsNullOrEmpty(currentLiveId))
                result.Data.RemoveAll(s => s.LiveId == currentLiveId);

            // 预取主播信息（同主播所有场次共用）
            var anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);

            foreach (var session in result.Data)
                ComputeSegments(session, anchorInfo);

            var videoList = new List<VideoEntity>();
            foreach (var s in result.Data)
            {
                if (s.Segments != null && s.Segments.Count > 0)
                {
                    foreach (var seg in s.Segments)
                    {
                        videoList.Add(new VideoEntity
                        {
                            videoId = Guid.NewGuid().ToString(),
                            batchNumber = s.LiveId,
                            liveTitle = s.LiveRoom,
                            startTime = seg.StartTime,
                            endTime = seg.EndTime,
                            duration = seg.DurationSec.ToString(),
                            secUid = secUid,
                            platformType = "1",
                            paragraph = seg.SegmentIndex,
                            subsectionType = 1,
                        });
                    }
                }
                else
                {
                    videoList.Add(new VideoEntity
                    {
                        videoId = Guid.NewGuid().ToString(),
                        batchNumber = s.LiveId,
                        liveTitle = s.LiveRoom,
                        startTime = s.StartTime,
                        endTime = s.EndTime,
                        duration = s.DurationSec.ToString(),
                        secUid = secUid,
                        platformType = "1",
                        paragraph = 0,
                    });
                }
            }

            await VideoApi.SavePulledVideos(videoList);
            FileUtils.LogRpa($"拉取保存完成: {videoList.Count} 条记录", "VideoPull");
        }

        /// <summary>
        /// 根据平台标识创建对应的 Puller
        /// </summary>
        private static IVideoPuller ResolvePuller(string platform)
        {
            switch (platform)
            {
                case "laike":
                    // return new LaikeVideoPuller();
                    throw new NotImplementedException("来客平台暂未实现");
                default:
                    return new JuliangVideoPuller();
            }
        }

        // ==== 分段计算 ====

        /// <summary>
        /// 根据主播/系统录制设置，将超长直播场次拆分为多个时间段
        /// 优先级：主播按时间点分段 → 主播按时长分段 → 系统按时长分段
        /// </summary>
        /// <param name="session">直播场次</param>
        /// <param name="anchorInfo">主播信息（已预取，避免循环查询）</param>
        /// <param name="limitType">分段类型（已预计算，避免循环查询）</param>
        /// <param name="limitValue">分段阈值分钟数（已预计算，避免循环查询）</param>
        private static void ComputeSegments(LiveSessionInfo session, AnchorInfo anchorInfo)
        {
            var baseStart = DateTime.ParseExact(session.StartTime, "yyyy-MM-dd HH:mm:ss",
                CultureInfo.InvariantCulture);
            var baseEnd = baseStart.AddSeconds(session.DurationSec);

            // 按时间点分段（仅主播级，recordTimeMode=2 且 segmentTimePoints 非空）
            if (anchorInfo?.recordTimeMode == 2
                && !string.IsNullOrEmpty(anchorInfo.segmentTimePoints))
            {
                session.Segments = BuildTimePointSegments(baseStart, baseEnd,
                    anchorInfo.segmentTimePoints);
                return;
            }

            // 按时长分段：主播级优先，否则回退系统级
            int limitType = -1, limitValue = 0;
            if (anchorInfo?.recordLimitType != null && anchorInfo.recordLimitType != -1)
            {
                limitType = anchorInfo.recordLimitType.Value;
                limitValue = anchorInfo.recordLimitValue ?? -1;
                // 与系统配置保持一致：限制时长 → 时长分段
                if (limitType == 1 || (limitType == 0 && limitValue > 0))
                    limitType = 2;
            }
            // 主播 recordLimitType 为 -1（跟随系统），但 recordLimitValue 有值时按分段处理
            if (limitType == -1 && anchorInfo?.recordLimitValue != null && anchorInfo.recordLimitValue > 0)
            {
                limitType = 2;
                limitValue = anchorInfo.recordLimitValue.Value;
            }

            if (limitType == -1)
            {
                var config = new ConfigBll().GetModel();
                limitType = config.LimitType;
                limitValue = config.LimitValue;
            }

            // recordLimitValue = -1 表示跟随系统，回退到系统配置的时长
            if (limitValue <= 0)
            {
                var config = new ConfigBll().GetModel();
                limitValue = config.LimitValue;
            }

            if (limitType != 2 || limitValue <= 0) return;
            long thresholdSec = limitValue * 60L;
            if (session.DurationSec <= thresholdSec) return;

            session.Segments = BuildDurationSegments(baseStart, session.DurationSec, thresholdSec);
        }

        /// <summary>
        /// 按时长分段：将总时长为 totalDurationSec 的视频按每段 thresholdSec 秒均匀切分，
        /// 最后一段可能不足 thresholdSec
        /// </summary>
        private static List<VideoSegmentInfo> BuildDurationSegments(
            DateTime baseStart, long totalDurationSec, long thresholdSec)
        {
            var list = new List<VideoSegmentInfo>();
            long remaining = totalDurationSec;
            int idx = 0;
            long offset = 0;
            while (remaining > 0)
            {
                long dur = Math.Min(thresholdSec, remaining);
                list.Add(new VideoSegmentInfo
                {
                    SegmentIndex = idx,
                    StartTime = baseStart.AddSeconds(offset).ToString("yyyy-MM-dd HH:mm:ss"),
                    EndTime = baseStart.AddSeconds(offset + dur).ToString("yyyy-MM-dd HH:mm:ss"),
                    DurationSec = dur,
                    StartOffsetSec = offset
                });
                offset += dur; remaining -= dur; idx++;
            }
            return list;
        }

        /// <summary>
        /// 按时间点分段：在 baseStart~baseEnd 范围内按钟面时间点切段
        /// 如 session 09:05~12:30，时间点 "09:30,10:40" → 09:05-09:30, 09:30-10:40, 10:40-12:30
        /// </summary>
        /// <param name="baseStart">场次开始时间</param>
        /// <param name="baseEnd">场次结束时间</param>
        /// <param name="segmentTimePoints">逗号分隔的钟面时间，如 "09:30,10:40"</param>
        /// <returns>分段列表，时间点全部落在范围外时返回 null</returns>
        private static List<VideoSegmentInfo> BuildTimePointSegments(
            DateTime baseStart, DateTime baseEnd, string segmentTimePoints)
        {
            var points = segmentTimePoints
                .Split(new[] { ',' }, StringSplitOptions.RemoveEmptyEntries)
                .Select(s => s.Trim())
                .Where(s => TimeSpan.TryParse(s, out _))
                .Select(s => TimeSpan.Parse(s))
                .OrderBy(t => t)
                .ToList();

            if (points.Count == 0) return null;

            // 在 baseStart~baseEnd 范围内生成所有切点（支持跨天，最长 7 天内必有切点）
            var cutPoints = new List<DateTime>();
            var currentDate = baseStart.Date;
            var maxDate = baseStart.Date.AddDays(7);
            while (cutPoints.Count == 0 || cutPoints.Last() < baseEnd)
            {
                if (currentDate > maxDate) break;
                foreach (var pt in points)
                {
                    var cut = currentDate + pt;
                    if (cut > baseStart && cut < baseEnd)
                        cutPoints.Add(cut);
                }
                currentDate = currentDate.AddDays(1);
            }

            if (cutPoints.Count == 0) return null;

            var list = new List<VideoSegmentInfo>();
            var segStart = baseStart;
            int idx = 0;
            long offset = 0;

            foreach (var cut in cutPoints)
            {
                long dur = (long)(cut - segStart).TotalSeconds;
                if (dur <= 0) continue;
                list.Add(new VideoSegmentInfo
                {
                    SegmentIndex = idx,
                    StartTime = segStart.ToString("yyyy-MM-dd HH:mm:ss"),
                    EndTime = cut.ToString("yyyy-MM-dd HH:mm:ss"),
                    DurationSec = dur,
                    StartOffsetSec = offset
                });
                offset += dur; segStart = cut; idx++;
            }

            // 最后一段
            long lastDur = (long)(baseEnd - segStart).TotalSeconds;
            if (lastDur > 0)
            {
                list.Add(new VideoSegmentInfo
                {
                    SegmentIndex = idx,
                    StartTime = segStart.ToString("yyyy-MM-dd HH:mm:ss"),
                    EndTime = baseEnd.ToString("yyyy-MM-dd HH:mm:ss"),
                    DurationSec = lastDur,
                    StartOffsetSec = offset
                });
            }

            return list;
        }
    }
}
