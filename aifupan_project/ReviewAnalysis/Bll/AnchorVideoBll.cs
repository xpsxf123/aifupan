using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.IO;
using System.Threading.Tasks;
using System.Diagnostics;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.HttpServer;
using douyin.Utils;
using System.Threading;
using ReviewAnalysis.Global;
using System.Text;
using System.Net.Http;
using System.Net;
using System.Security.Policy;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.Tab;
using Swan.Parsers;
using System.Windows.Forms;
using ReviewAnalysis.Websocket;
using Newtonsoft.Json.Linq;
using COSXML.Network;
using static System.Net.Mime.MediaTypeNames;
using System.Drawing.Drawing2D;
using ReviewAnalysis.vo;
using System.Security.Cryptography;
using static System.Windows.Forms.VisualStyles.VisualStyleElement;
using static ReviewAnalysis.Websocket.Entity.WebsocketUtilsEntity;
using ReviewAnalysis.Websocket.utils;
using System.Runtime.InteropServices;
using System.Linq;
using MediaInfo;
using ReviewAnalysis.vo.contrast;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.video;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.entity.anchor;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.vo.barrage;
using System.Web.UI.WebControls;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.Ai;
using CefSharp.DevTools.IO;
using Swan.Formatters;
using ReviewAnalysis.upload;
using CefSharp.DevTools.Network;
using System.Web;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.vo.oceanEngineData;
using Microsoft.Playwright;
using System.Windows;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Bll.VideoPull;
using ReviewAnalysis.Bll.VideoPull.Juliang;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.bo.analysis;
using System.Web.Util;
using ReviewAnalysis.bo.sliceVideo;
using ReviewAnalysis.WeChatChannels.Services;
using OpenCvSharp;
using System.Text.RegularExpressions;
using ReviewAnalysis.enums;
using System.Globalization;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.plugins.core;
using ReviewAnalysis.plugins.dataPullers;
using ReviewAnalysis.Websocket.Entity;

namespace ReviewAnalysis.Bll
{
    public class AnchorVideoBll
    {
        /// <summary>
        /// 是否正在分析
        /// </summary>
        public static volatile bool isAnalysis = false;
        public static string currentAnalysisId = "";
        /// <summary>
        /// 是否开启了自动分析
        /// </summary>
        public static bool autoAnalysis = false;
        /// <summary>
        /// 是否正在切割短视频
        /// </summary>
        public static volatile bool sliceVideoing = false;
        public static volatile object _lockObject = new object();

        /// <summary>
        /// 待「手动重新分析」强制走 Tencent 的视频集合（值无意义，仅用 Key 做集合语义）。
        /// 由 ReAnalysis 入队前打标，AutoAnalysis 捡起时一次性 TryRemove 消费并透传来源。
        /// 内存标记：进程重启后丢失，重启后该视频按默认引擎路由分析。
        /// </summary>
        private static readonly ConcurrentDictionary<string, byte> _retryTencentVideoIds =
            new ConcurrentDictionary<string, byte>();

        /// <summary>
        /// 标记某视频的下一次分析为「手动重新分析」，强制走 Tencent 引擎。
        /// </summary>
        /// <param name="videoId">视频 ID</param>
        public static void MarkRetryTencent(string videoId)
        {
            if (!string.IsNullOrEmpty(videoId))
            {
                _retryTencentVideoIds[videoId] = 0;
            }
        }

        /// <summary>
        /// MP4转换等待超时时间（秒）
        /// </summary>
        private const int MP4_CONVERT_WAIT_TIMEOUT_SECONDS = 5 * 60;

        /// <summary>
        /// 等待MP4转换完成
        /// 如果当前视频ID与队列当前的任务视频ID一致，则自旋等待（持续5分钟）
        /// 直到视频ID不一致了或者超时，才继续往下执行
        /// </summary>
        /// <param name="videoId">当前要分析的视频ID</param>
        /// <returns></returns>
        private async Task WaitForMp4ConvertComplete(string videoId)
        {
            if (string.IsNullOrEmpty(videoId))
            {
                return;
            }

            string currentConvertingVideoId = Mp4ConvertQueueManager.GetCurrentConvertingVideoId();
            if (string.IsNullOrEmpty(currentConvertingVideoId) || !currentConvertingVideoId.Equals(videoId))
            {
                // 当前没有正在转换的任务，或者转换的不是当前视频，无需等待
                return;
            }

            FileUtils.LogAnalysis($"{videoId}", $"检测到当前视频正在队列中转换MP4，开始自旋等待");

            DateTime startTime = DateTime.Now;
            int waitIntervalMs = 1000; // 每次等待间隔（1秒）

            while (true)
            {
                // 检查是否超时
                TimeSpan elapsed = DateTime.Now - startTime;
                if (elapsed.TotalSeconds >= MP4_CONVERT_WAIT_TIMEOUT_SECONDS)
                {
                    FileUtils.LogAnalysis($"{videoId}", $"MP4转换等待超时（{MP4_CONVERT_WAIT_TIMEOUT_SECONDS}秒），继续执行分析");
                    break;
                }

                // 检查当前转换任务视频ID是否与当前视频ID不一致
                currentConvertingVideoId = Mp4ConvertQueueManager.GetCurrentConvertingVideoId();
                if (string.IsNullOrEmpty(currentConvertingVideoId) || !currentConvertingVideoId.Equals(videoId))
                {
                    FileUtils.LogAnalysis($"{videoId}", $"MP4转换已完成或任务已变更，继续执行分析，等待时间：{elapsed.TotalSeconds:F1}秒");
                    break;
                }

                // 等待一段时间后继续检查
                await Task.Delay(waitIntervalMs);
            }
        }

        /// <summary>
        /// 自动分析
        /// </summary>
        /// <param name="token">token</param>
        public async Task AutoAnalysis()
        {
            while (autoAnalysis)
            {
                try
                {
                    // 获取当前用户是否还有分析时长余额

                    // 获取所有未分析视频
                    List<VideoEntity> videoList = await VideoApi.ListByNotAnalysis();
                    var autoAnalysisTasks = new List<Task>();
                    if (videoList != null && videoList.Count > 0)
                    {
                        foreach (var item in videoList)
                        {
                            try
                            {
                                // 重新获取一下视频信息
                                VideoEntity video = await VideoApi.GetVideoByVideoId(item.videoId);
                                if(video == null)
                                {
                                    FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(video)}", $"视频分析--获取视频信息失败");
                                    await VideoApi.UpdateVideoAnalysisStatus(item.videoId, 3, "网络不佳");
                                    continue;
                                }
                                if(video.analysisStatus != 0)
                                {
                                    continue;
                                }

                                if (autoAnalysis)
                                {

                                    // 巨量拉取未下载视频：先下载再分析
                                    if (video.dataSource == 1 && video.isDownloaded == 0)
                                    {
                                        if (!await TryDownloadJuliangVideo(video))
                                            continue;
                                    }

                                    // 判断本地视频文件是否存在
                                    if (!File.Exists(video.storagePath))
                                    {
                                        FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(video)}", $"视频文件不存在，跳过分析");
                                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "视频文件不存在");
                                        AnchorVideoBll.isAnalysis = false;
                                        continue;
                                    }

                                    // 判断磁盘空间是否充足
                                    DriveInfo drive = new DriveInfo(video.storagePath.Substring(0, 1));
                                    if (!drive.IsReady)
                                    {
                                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "磁盘不存在");
                                        continue;
                                    }
                                    long availableFreeSpace = drive.AvailableFreeSpace;
                                    if(availableFreeSpace - long.Parse(video.vedioSizie) < (1024.0 * 1024.0 * 1024.0))
                                    {
                                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "磁盘空间不足");
                                        continue;
                                    }

                                    DriveInfo installDrive = new DriveInfo(Path.GetFullPath("/").Substring(0, 1));
                                    if (!installDrive.IsReady)
                                    {
                                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "安装的磁盘不存在");
                                        continue;
                                    }
                                    if (installDrive.AvailableFreeSpace < (1024.0 * 1024.0 * 100))
                                    {
                                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "安装的磁盘空间不足");
                                        continue;
                                    }


                                    // 开始分析
                                    isAnalysis = true;
                                    AnchorVideoBll.currentAnalysisId = video.videoId;
                                    // 一次性消费重试标记：命中则本次强制走 Tencent（手动重新分析） || 如果是巨量拉取的视频也直接走 tencent
                                    AnalysisSource source = AnalysisSource.Auto;
                                    if (_retryTencentVideoIds.ContainsKey(video.videoId) || (video.dataSource ?? 0) == 1)
                                    {
                                        source = AnalysisSource.ManualRetry;
                                        _retryTencentVideoIds.TryRemove(video.videoId, out _);
                                    }

                                    if (Constant.env == "test" ||  Constant.env == "dev")
                                    {
                                        source =  AnalysisSource.Auto;
                                    }

                                    await Analysis(video, source); //改回排队分析
                                    //autoAnalysisTasks.Add(Analysis(video));
                                    isAnalysis = false;

                                }
                                await Task.Delay(1000);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogAnalysis($"{ex}", $"自动分析发生异常==={JsonConvert.SerializeObject(item)}");
                            }
                        }
                    }
                    //await Task.WhenAll(autoAnalysisTasks);
                    //isAnalysis = false;
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"自动分析发生异常：{ex}");
                }
                await Task.Delay(60000);
            }                                
            await Task.Delay(60000);
        }


        /// <summary>
        /// 巨量拉取视频下载：从罗盘 API 获取 m3u8 回放地址 → ffmpeg 下载 → 更新 storagePath
        /// </summary>
        private async Task<bool> TryDownloadJuliangVideo(VideoEntity video)
        {
            await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 1, "");

            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(video.secUid);
            if (anchorInfo == null)
            {
                await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "未找到主播信息");
                return false;
            }

            Config config = new ConfigBll().GetModel();
            string dateFolder = ServerTimeUtils.getCurrentTimeStr("yyyyMMdd");
            if (!string.IsNullOrEmpty(video.startTime)
                && DateTime.TryParseExact(video.startTime, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime dt))
            {
                dateFolder = dt.ToString("yyyyMMdd");
            }
            string saveDirectory = Path.Combine(config.SavePath, anchorInfo.FolderName, dateFolder);
            string videoName = Path.GetFileNameWithoutExtension(video.videoName);

            // 计算分段偏移（仅 paragraph > 0 时需要查服务端获取场次原始开始时间）
            long startOffsetSec = 0;
            long segmentDurationSec = 0;
            if (!string.IsNullOrEmpty(video.duration) && long.TryParse(video.duration, out var durSec))
                segmentDurationSec = durSec;

            VideoEntity session = null;
            if (video.paragraph != null && video.paragraph > 0 && segmentDurationSec > 0)
            {
                session = await VideoApi.GetLiveSessionByBatchNumber(video.batchNumber);
                if (session != null && !string.IsNullOrEmpty(session.startTime)
                    && !string.IsNullOrEmpty(video.startTime))
                {
                    if (DateTime.TryParseExact(session.startTime, "yyyy-MM-dd HH:mm:ss",
                        CultureInfo.InvariantCulture, DateTimeStyles.None, out var sessionStart)
                        && DateTime.TryParseExact(video.startTime, "yyyy-MM-dd HH:mm:ss",
                        CultureInfo.InvariantCulture, DateTimeStyles.None, out var segmentStart))
                    {
                        startOffsetSec = (long)(segmentStart - sessionStart).TotalSeconds;
                        if (startOffsetSec < 0) startOffsetSec = 0;
                    }
                }
            }

            // 检查视频是否超过最大可下载天数
            int maxDownloadDays = KvHelper.GetIntKvByKey("JuliangVideoMaxDownloadDays", 14);
            string sessionStartTime = session?.startTime ?? video.startTime;
            if (!string.IsNullOrEmpty(sessionStartTime)
                && DateTime.TryParseExact(sessionStartTime, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime startDt))
            {
                double daysSinceStart = (DateTime.Now - startDt).TotalDays;
                if (daysSinceStart > maxDownloadDays)
                {
                    string errMsg = $"开播已超过{maxDownloadDays}天，无法下载";
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, errMsg);
                    return false;
                }
            }
            
            // 并行启动数据拉取，不关心结果
            var pullDataTask = Task.Run(async () =>
            {
                await TryDownloadDataVideo(video);
            });

            var downloader = new JuliangVideoDownloader();
            var (success, localPath, error) = await downloader.DownloadAsync(
                video.batchNumber, video.secUid, saveDirectory, videoName,
                startOffsetSec, segmentDurationSec);

            // 等数据拉取完成再返回（不关心成败，只确保不遗留后台任务）
            try { await pullDataTask; } catch { }

            if (!success)
            {
                await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, error);
                return false;
            }

            video.storagePath = localPath;
            video.isDownloaded = 1;
            video.videoType = Path.GetExtension(localPath).ToLower() == ".mp4" ? 2 : 0;
            video.vedioSizie = new FileInfo(localPath).Length.ToString();
            video.videoName = $"{videoName}.ts";
            await VideoApi.SaveOrUpdateVideoAsync(new VideoEntity()
            {
                id = video.id,
                videoId = video.videoId,
                videoName = video.videoName,
                storagePath = video.storagePath,
                isDownloaded = video.isDownloaded,
                videoType = video.videoType,
                vedioSizie = video.vedioSizie,
            });

            // 巨量视频下载完成并通知服务器后，再通知前端进度：downloadDone
            FrontNotice.NoticeAnalysisProgress(video.videoId, "downloadDone");

            return true;
        }

        /// <summary>
        /// 拉取视频的完整数据包：在线人数曲线 + 弹幕 + 巨量平台数据。
        /// 三个子任务顺序执行，各自独立 try-catch，单步骤失败不阻断后续步骤。
        /// 完成后调用 SaveListToFile 将 dataQueue 中所有实体刷入文件。
        /// </summary>
        /// <param name="video">目标视频实体（需含 batchNumber、secUid、videoId、startTime、endTime）</param>
        /// <param name="maxCommentCount">弹幕拉取上限，0 表示不限制</param>
        internal async Task TryDownloadDataVideo(VideoEntity video, int maxCommentCount = 0)
        {
            try
            {
                // 1. 拉取在线人数曲线
                await TryPullOnlineTrend(video);

                // 2. 拉取弹幕数据
                await TryPullComments(video, maxCommentCount);

                // 3+4 合并：统一采集上传（全平台数据采集，同时上报 governance 和 fupan-server）
                var anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(video.secUid);
                if (anchorInfo != null)
                    await AnchorInstance.CollectAndUploadAsync(anchorInfo, video.batchNumber, video.videoId);

                WebsocketDataHandle.SaveListToFile();
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"失败，{e}", "手动拉取视频数据");
                FileUtils.LogError($"失败，{e}", "手动拉取视频数据");
            }
        }

        /// <summary>
        /// 拉取在线人数曲线数据。
        /// 调用 <see cref="LiveOnlineTrendPuller.FetchAsync"/> 获取趋势数据点，
        /// 每个数据点构造一个 RenShu 实体并通过 WebsocketDataEntity 入队到 dataQueue，
        /// 由 <see cref="WebsocketDataHandle.SaveListToFile"/> 批量写入 renshu.txt。
        /// 拉取前会删除已有的 renshu.txt 文件以避免旧数据叠加。
        /// </summary>
        /// <param name="video">目标视频实体</param>
        private async Task TryPullOnlineTrend(VideoEntity video)
        {
            try
            {
                var trends = await LiveOnlineTrendPuller.FetchAsync(video.batchNumber, video.secUid);
                if (trends == null || trends.Count == 0)
                    return;

                string filePath = Path.Combine(WebsocketDataHandle.savePath,
                    $"{video.batchNumber}-{ReplayHttpUtils.UserId}", "renshu.txt");

                try { if (File.Exists(filePath)) File.Delete(filePath); } catch { }

                foreach (var point in trends)
                {
                    var renShu = new RenShu { content = point.Value.ToString() };
                    var entity = new WebsocketDataEntity
                    {
                        messageType = "renshu",
                        data = renShu,
                        fileSavePath = filePath,
                        batchNumber = video.batchNumber,
                        videoId = video.videoId,
                        secUid = video.secUid,
                        pushDate = point.Time
                    };
                    WebsocketDataHandle.dataQueue.Enqueue(entity);
                }

                // 写入累计观看人数文件（watch_ucnt 进入人数，累加）
                string leijiFilePath = Path.Combine(WebsocketDataHandle.savePath,
                    $"{video.batchNumber}-{ReplayHttpUtils.UserId}", "leijiguankanrenshu.txt");
                try { if (File.Exists(leijiFilePath)) File.Delete(leijiFilePath); } catch { }

                double cumulativeWatchUcnt = 0;
                foreach (var point in trends)
                {
                    cumulativeWatchUcnt += point.WatchUcnt;
                    var renShu = new RenShu { content = cumulativeWatchUcnt.ToString() };
                    var entity = new WebsocketDataEntity
                    {
                        messageType = "leijiguankanrenshu",
                        data = renShu,
                        fileSavePath = leijiFilePath,
                        batchNumber = video.batchNumber,
                        videoId = video.videoId,
                        secUid = video.secUid,
                        pushDate = point.Time
                    };
                    WebsocketDataHandle.dataQueue.Enqueue(entity);
                }

                // 写入巨量实时数据文件（累加值）
                WriteRealtimeDataFromTrends(video, trends);
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"拉取在线人数失败: {ex.Message}", "TryDownloadDataVideo");
            }
        }

        /// <summary>
        /// 将趋势数据点写入巨量实时数据文件（dataCollect\juliang\realTime\{videoId}.txt）。
        /// blend_trend_v2 API 返回的是每个时间段内的增量值，实时文件存储累加值，
        /// 因此遍历时对每个指标做累计求和后再写入。
        /// </summary>
        /// <param name="video">目标视频实体</param>
        /// <param name="trends">全部指标的趋势数据点列表（按时间升序）</param>
        private static void WriteRealtimeDataFromTrends(VideoEntity video, List<CompassTrendPoint> trends)
        {
            try
            {
                string filePath = JuliangDataHandle.getDataFilePath(video.videoId, 0);
                string dir = Path.GetDirectoryName(filePath);
                if (!Directory.Exists(dir))
                    Directory.CreateDirectory(dir);

                using (var sw = File.AppendText(filePath))
                {
                    double accWatchNum = 0;
                    double accPayCnt = 0;
                    double accPayAmt = 0;
                    double accFansClub = 0;
                    double accIncrFans = 0;
                    double accStatCost = 0;
                    Random random = new Random();
                    foreach (var point in trends)
                    {
                        accWatchNum += point.WatchUcnt;
                        accPayCnt += point.PayCnt;
                        accPayAmt += point.PayAmt;
                        accFansClub += point.FansClubUcnt;
                        accIncrFans += point.IncrFansCnt;
                        accStatCost += point.StatCost;

                        decimal totalRoi = 0;
                        if (accStatCost > 0)
                            totalRoi = (decimal)(accPayAmt / accStatCost);

                        var entity = new JuliangRealTimeDataEntity
                        {
                            watchNum = (int)accWatchNum,
                            payComboCnt = (int)accPayCnt,
                            payAmt = (int)accPayAmt,
                            fansClubJoinUcnt = (int)accFansClub,
                            followAnchorUcnt = (int)accIncrFans,
                            qianchuanCost = (decimal)accStatCost,
                            totalRoi = totalRoi,
                            refundAmt = 0,
                            gatherDateTime = point.Time,
                            gatherTimeStamp = ToUnixMilliseconds(point.Time)
                        };
                        sw.Write(JsonConvert.SerializeObject(entity));
                        sw.WriteLine();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"写入巨量实时数据文件失败: {ex.Message}", "TryDownloadDataVideo");
            }
        }

        /// <summary>
        /// 将 "yyyy-MM-dd HH:mm:ss" 格式的东八区时间字符串转换为 Unix 毫秒时间戳。
        /// </summary>
        /// <param name="dateTimeStr">格式为 yyyy-MM-dd HH:mm:ss 的时间字符串</param>
        /// <returns>Unix 毫秒时间戳；解析失败返回 0</returns>
        private static long ToUnixMilliseconds(string dateTimeStr)
        {
            if (string.IsNullOrEmpty(dateTimeStr))
                return 0;
            if (DateTime.TryParseExact(dateTimeStr, "yyyy-MM-dd HH:mm:ss",
                System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out DateTime dt))
            {
                return new DateTimeOffset(dt, TimeSpan.FromHours(8)).ToUnixTimeMilliseconds();
            }
            return 0;
        }

        /// <summary>
        /// 拉取直播回放弹幕/评论数据。
        /// 将 video.startTime / video.endTime 从 "yyyy-MM-dd HH:mm:ss" 转换为 Unix 秒时间戳后
        /// 调用 <see cref="LiveCommentPuller.FetchAsync"/>，每条评论构造一个 DanMu 实体
        /// （含雪花 ID、默认等级 0）通过 WebsocketDataEntity 入队到 dataQueue。
        /// 拉取前会删除已有的 danmu-{videoId}.txt 文件以避免旧数据叠加。
        /// </summary>
        /// <param name="video">目标视频实体</param>
        /// <param name="maxCount">弹幕拉取上限，0 表示不限制</param>
        private async Task TryPullComments(VideoEntity video, int maxCount)
        {
            try
            {
                long startTs = ToUnixSeconds(video.startTime);
                long endTs = ToUnixSeconds(video.endTime);

                var comments = await LiveCommentPuller.FetchAsync(video.batchNumber, video.secUid, startTs, endTs, maxCount);
                if (comments == null || comments.Count == 0)
                    return;

                string filePath = Path.Combine(WebsocketDataHandle.savePath,
                    $"{video.batchNumber}-{ReplayHttpUtils.UserId}", $"danmu-{video.videoId}.txt");

                try { if (File.Exists(filePath)) File.Delete(filePath); } catch { }

                foreach (var c in comments)
                {
                    var danMu = new DanMu
                    {
                        content = c.Content ?? "",
                        nickName = string.IsNullOrEmpty(c.NickName) ? "用户" : c.NickName,
                        level = -1,
                        fansLevel = -1,
                        msgId = NextSnowflakeId()
                    };
                    var entity = new WebsocketDataEntity
                    {
                        messageType = "danmu",
                        data = danMu,
                        fileSavePath = filePath,
                        batchNumber = video.batchNumber,
                        videoId = video.videoId,
                        secUid = video.secUid,
                        pushDate = c.EventTime
                    };
                    WebsocketDataHandle.dataQueue.Enqueue(entity);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"拉取弹幕失败: {ex.Message}", "TryDownloadDataVideo");
            }
        }

        /// <summary>
        /// <summary>雪花 ID 序列号计数器，自增后对 1000000 取模</summary>
        private static long _snowflakeSequence;

        /// <summary>雪花 ID 序列号自增的线程安全锁</summary>
        private static readonly object _snowflakeLock = new object();

        /// <summary>
        /// 生成 19 位雪花 ID（毫秒级时间戳 × 1,000,000 + 自增序列号）。
        /// 使用 UTC 毫秒时间戳保证跨时区唯一性，序列号线程安全自增并对 1,000,000 取模。
        /// 适用于弹幕 msgId 等需要全局唯一标识的场景。
        /// </summary>
        /// <returns>19 位数字字符串，如 "7660442509915886627"</returns>
        private static string NextSnowflakeId()
        {
            long ts = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            long seq;
            lock (_snowflakeLock) { seq = _snowflakeSequence++ % 1000000; }
            return (ts * 1000000 + seq).ToString();
        }

        /// <summary>
        /// 将 "yyyy-MM-dd HH:mm:ss" 格式的时间字符串转换为 Unix 秒时间戳。
        /// 输入时间视为东八区 (UTC+8) 时间，例如 "2025-06-15 20:30:00" → 对应 UTC+8 的 Unix 秒数。
        /// </summary>
        /// <param name="dateTimeStr">格式为 yyyy-MM-dd HH:mm:ss 的时间字符串</param>
        /// <returns>Unix 秒时间戳；输入为空时返回 0</returns>
        private static long ToUnixSeconds(string dateTimeStr)
        {
            if (string.IsNullOrEmpty(dateTimeStr))
                return 0;
            DateTime dt = DateTime.ParseExact(dateTimeStr, "yyyy-MM-dd HH:mm:ss",
                CultureInfo.InvariantCulture, DateTimeStyles.None);
            return new DateTimeOffset(dt, TimeSpan.FromHours(8)).ToUnixTimeSeconds();
        }


        /// <summary>
        /// 视频分析
        /// </summary>
        /// <param name="video">视频信息</param>
        /// <param name="source">分析来源；ManualRetry 时本次 ASR 强制走 Tencent。</param>
        public async Task Analysis(VideoEntity video, AnalysisSource source = AnalysisSource.Auto)
        {
            FileUtils.LogAnalysis($"获取视频分析-视频信息：{JsonConvert.SerializeObject(video)}");

            if (video.platformType != null && video.platformType.Equals("1"))
            {
                try
                {
                    // 上传抖音的websocket采集的数据
                    WebsocketDataHandle.UploadSocketData(video.videoId, video.batchNumber, video.startTime, video.endTime, video.secUid, long.Parse(video.userId), true);
                    if (!ServerTimeUtils.timeAccurate)
                    {
                        ServerTimeUtils.checkTimeAccurate();
                    }
                }
                catch (Exception ex)
                {
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络异常，sk数据错误");
                    FileUtils.LogAnalysis($"{ex}", $"上传websocket采集的抖音数据发生异常");
                    AnchorVideoBll.isAnalysis = false;
                    return;
                }
            }

            bool isUploadDashboardData = false;
            if (video.platformType != null && video.platformType.Equals("1"))
            {
                // 上传抖音的巨量的数据
                isUploadDashboardData = JuliangDataHandle.dataUpload(video.batchNumber, video.videoId, video.secUid);
                if (!isUploadDashboardData)
                {
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络异常，巨量百应数据错误");
                    AnchorVideoBll.isAnalysis = false;
                    return;
                }
                
                // 上传来客数据（抖音平台的一部分）
                FileUtils.LogAnalysis($"{video.videoName}", $"上传来客直播大屏数据");
                bool isUploadLifeData = ReviewAnalysis.life.LifeDataHandle.dataUpload(video.batchNumber, video.videoId, video.secUid);
                if (!isUploadLifeData)
                {
                    FileUtils.LogAnalysis($"{video.videoName}", $"来客数据上传失败，继续后续流程");
                    // 来客数据上传失败不阻断流程，仅记录日志
                }

                // 上传企业号数据（抖音平台的一部分）
                FileUtils.LogAnalysis($"{video.videoName}", $"上传企业号直播大屏数据");
                bool isUploadEnterpriseData = ReviewAnalysis.enterprise.EnterpriseDataHandle.dataUpload(video.batchNumber, video.videoId, video.secUid);
                if (!isUploadEnterpriseData)
                {
                    FileUtils.LogAnalysis($"{video.videoName}", $"企业号数据上传失败，继续后续流程");
                    // 企业号数据上传失败不阻断流程，仅记录日志
                }
            }
            else if (video.platformType != null && video.platformType.Equals("3"))
            {
                try
                {
                    FileUtils.LogAnalysis($"{video.videoName}", $"微信视频号直播大屏数据===延迟1分钟");
                    Task.Delay(60 * 1000);
                }catch (Exception e)
                {
                    FileUtils.LogAnalysis($"{e}", $"微信视频号直播大屏数据===延迟1分钟发生异常");
                    return;
                }
                
                FileUtils.LogAnalysis($"{video.videoName}", $"上传微信视频号直播大屏数据");
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(video.secUid);
                if(ReplayHttpUtils.UserClientVersion.Equals(ClientVersion.Replay) || (anchorInfo.pureRecordOnlineNum != null && anchorInfo.pureRecordOnlineNum == 1))
                {
                    // 上传微信视频号直播大屏数据
                    await WeChatChannelsDashboardService.SaveDashboardData(video);
                    isUploadDashboardData = WeChatChannelsDashboardService.UploadDashboardData(video.videoId);
                    if (!isUploadDashboardData)
                    {
                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络异常，视频号数据错误");
                        AnchorVideoBll.isAnalysis = false;
                        return;
                    }
                }
                
            }

            try
            {

                // 判断用户是否有足够的分析时长余额
                int minute = (int)(Convert.ToDouble(video.duration) / 60) < 1 ? 1 : (int)(Convert.ToDouble(video.duration) / 60);
                int analysisStatus = UserPropertyHttpUtils.CheckAnalysisMinute(minute);
                if (analysisStatus == 2)
                {
                    // 将视频分析使用资源设置为文案提取
                    video.useAnalysisPropertyType = 1;
                    await VideoApi.SaveOrUpdateVideoAsync(video);

                    // 分析时长不足，判断文案提取时长是否足够
                    int textExtractionStatus = await UserPropertyHttpUtils.CheckTextExtraction(minute);
                    if(textExtractionStatus == 2)
                    {
                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "提取时长不足");
                        AnchorVideoBll.isAnalysis = false;
                        return;
                    }
                    else if (textExtractionStatus == 0)
                    {
                        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络不佳");
                        AnchorVideoBll.isAnalysis = false;
                        return;
                    }
                    
                }
                else if (analysisStatus == 0)
                {
                    FileUtils.LogAnalysis($"视频时长：{minute}", $"获取时长余额失败");
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络不佳");
                    AnchorVideoBll.isAnalysis = false;
                    return;
                }
                else if(analysisStatus == 1 && video.useAnalysisPropertyType == 1)
                {
                    // 之前是使用提取时长，现在智能分析时长充足，将视频分析使用资源设置为智能分析
                    video.useAnalysisPropertyType = 0;
                    await VideoApi.SaveOrUpdateVideoAsync(video);
                }

                // 修改视频分析状态
                await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 1, "");

                // 分析视频
                await AnalysisVideo(video, source);
                AnchorVideoBll.isAnalysis = false;
                return;
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"视频分析失败========={JsonConvert.SerializeObject(video)}");
            }

            await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络异常");
            AnchorVideoBll.isAnalysis = false;

        }
        //public async Task Analysis(VideoEntity video)
        //{
        //    try
        //    {
        //        FileUtils.LogAnalysis($"获取视频分析-视频信息：{JsonConvert.SerializeObject(video)}");

        //        int num = 3;
        //        while (num > 0)
        //        {
        //            try
        //            {
        //                // 上传websocket采集的数据
        //                WebsocketDataHandle.UploadSocketData(video.videoId, video.batchNumber, video.startTime, video.endTime, video.secUid, long.Parse(video.userId), true);
        //                if (!ServerTimeUtils.timeAccurate)
        //                {
        //                    ServerTimeUtils.checkTimeAccurate();
        //                }

        //                // 判断用户是否有足够的分析时长余额
        //                int minute = minute = (int)(Convert.ToDouble(video.duration) / 60) < 1 ? 1 : (int)(Convert.ToDouble(video.duration) / 60);
        //                int analysisStatus = UserPropertyHttpUtils.CheckAnalysisMinute(minute);
        //                if (analysisStatus == 2)
        //                {
        //                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "分析余额不足");
        //                    AnchorVideoBll.isAnalysis = false;
        //                    return;
        //                }else if (analysisStatus == 0)
        //                {
        //                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络不佳");
        //                    AnchorVideoBll.isAnalysis = false;
        //                    return;
        //                }

        //                // 修改视频分析状态
        //                await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 1, "");

        //                // 分析视频
        //                await AnalysisVideo(video);
        //                AnchorVideoBll.isAnalysis = false;
        //                return;
        //            }
        //            catch (Exception e)
        //            {
        //                // 分析失败，清理数据
        //                DelVideoLocalData(video);

        //                // 剩余尝试次数减1
        //                num--;
        //                FileUtils.LogAnalysis($"{e}", $"视频分析失败，将重新进行分析===={JsonConvert.SerializeObject(video)}");
        //            }
        //        }

        //        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "分析失败");
        //        FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(video)}", $"视频3次都分析失败");
        //    }
        //    catch (Exception ex)
        //    {
        //        await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "分析失败");
        //        FileUtils.LogAnalysis($"{ex}", $"视频分析失败，状态已改成分析失败===={JsonConvert.SerializeObject(video)}");
        //    }

        //    AnchorVideoBll.isAnalysis = false;

        //}

        /// <summary>
        /// 删除视频本地数据
        /// </summary>
        /// <param name="videoEntity">视频信息</param>
        public void DelVideoLocalData(VideoEntity videoEntity)
        {
            if (videoEntity != null && !string.IsNullOrEmpty(videoEntity.storagePath))
            {
                // 删除mp4文件
                string videoPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
                videoPath += "\\mp4\\";
                videoPath += videoEntity.videoName + ".mp4";
                if (File.Exists(videoPath))
                {
                    File.Delete(videoPath);
                }

                // 删除音频文件
                string audioDirectoryPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
                audioDirectoryPath += "\\audio\\";
                audioDirectoryPath += videoEntity.videoName.Substring(0, videoEntity.videoName.LastIndexOf("."));
                if (Directory.Exists(audioDirectoryPath))
                {
                    Directory.Delete(audioDirectoryPath, true);
                }

                // 删除关联的分析数据
                string filePath = Path.GetFullPath($"analysisData\\video\\{videoEntity.startTime.Substring(0, 10)}\\{videoEntity.videoId}.txt");
                if(File.Exists(filePath))
                {
                    File.Delete(filePath);
                }
            }
        }

        /// <summary>
        /// 文件分析失败，清理数据
        /// </summary>
        /// <param name="uploadFile">文件信息</param>
        public void ClearFileData(AnchorVideo anchorVideo)
        {

            // 清除服务器上的分析数据
            //ReplayHttpUtils.ClearVideoAnalysis(anchorVideo.VideoId);

            // 删除关联的音频和识别数据
            Audio audio = new Audio();
            audio.DeleteModelByVideoId(anchorVideo.VideoId);
            //AudioaAlysis audioaAlysis = new AudioaAlysis();
            //audioaAlysis.DeleteModelByVideoId(anchorVideo.VideoId);

            // 删除关联的对比信息
            //VideoContrast videoContrast = new VideoContrast();
            //videoContrast.DeleteModelByVideoId( anchorVideo.VideoId);


            // 删除mp4文件
            string videoPath = anchorVideo.StoragePath.Substring(0, anchorVideo.StoragePath.LastIndexOf("\\"));
            videoPath += "\\mp4\\";
            videoPath += anchorVideo.VideoName + ".mp4";
            if (File.Exists(videoPath))
            {
                File.Delete(videoPath);
            }

            // 删除音频文件
            string audioDirectoryPath = anchorVideo.StoragePath.Substring(0, anchorVideo.StoragePath.LastIndexOf("\\"));
            audioDirectoryPath += "\\audio\\";
            audioDirectoryPath += anchorVideo.VideoName.Substring(0, anchorVideo.VideoName.LastIndexOf("."));
            if (Directory.Exists(audioDirectoryPath))
            {
                Directory.Delete(audioDirectoryPath, true);
            }

        }

        /// <summary>
        /// 进行分析视频
        /// </summary>
        /// <param name="anchorVideo">视频信息</param>
        /// <param name="token">向服务器发请求的凭证</param>
        private async Task AnalysisVideo(VideoEntity video, AnalysisSource source = AnalysisSource.Auto)
        {
            string audioDirectoryPath = "";
            try
            {
                // 将ts/flv视频转成mp4
                string mp4Path = "";
                if (!video.storagePath.EndsWith(".mp4"))
                {
                    // 检查当前视频ID是否与队列当前的任务视频ID一致，如果一致则自旋等待
                    await WaitForMp4ConvertComplete(video.videoId);

                    mp4Path = VideoUtils.ConvertToMP4(video.storagePath, video.videoName, int.Parse(video.platformType));
                    FileUtils.LogAnalysis($"{video.videoId}", $"视频分析-开始检查ts和mp4时长一致性");

                    // 检查ts文件和mp4文件时长是否一致（仅用于检测录制网络异常，不再用于决定是否更新时长）
                    int checkDuration = VideoUtils.CkeckTsAndMp4Consistent(video.storagePath, mp4Path);
                    FileUtils.LogAnalysis($"{video.videoId}, checkDuration={checkDuration}", $"视频分析-检查时长一致性完成");
                    if (checkDuration != -1 && checkDuration >= 60)
                    {
                        // 标记录制网络异常状态
                        video.recordErrorStatus = 7;
                    }
                }
                else
                {
                    // 视频已经是MP4格式，直接使用原路径
                    mp4Path = video.storagePath;
                }

                // 始终使用MP4文件的实际时长纠正录制时长，避免CppRecordUtils.getTaskTime()返回累计任务时长导致时长偏差
                try
                {
                    int actualDuration = VideoUtils.getVideoDuration(mp4Path);
                    if (actualDuration > 0)
                    {
                        int recordedDuration = 0;
                        int.TryParse(video.duration, out recordedDuration);

                        // 差值大于5秒才更新，避免频繁无意义的服务器请求
                        if (Math.Abs(actualDuration - recordedDuration) > 5)
                        {
                            FileUtils.LogAnalysis($"{video.videoId}, 原时长={recordedDuration}s, 实际时长={actualDuration}s", $"视频分析-纠正视频时长");

                            video.duration = actualDuration.ToString();

                            UpdateVideoSizeDurationBo updateVideoSizeDurationBo = new UpdateVideoSizeDurationBo();
                            updateVideoSizeDurationBo.videoId = video.videoId;
                            updateVideoSizeDurationBo.duration = actualDuration;

                            DateTime originalDateTime = DateTime.ParseExact(video.startTime, "yyyy-MM-dd HH:mm:ss", null);
                            DateTime newDateTime = originalDateTime.AddSeconds(actualDuration);
                            video.endTime = newDateTime.ToString("yyyy-MM-dd HH:mm:ss");

                            updateVideoSizeDurationBo.endTime = video.endTime;

                            await VideoApi.UpdateVideoSizeDuration(updateVideoSizeDurationBo);
                        }
                    }

                    // 如果检测到录制网络异常，同步 recordErrorStatus 到服务器
                    if (video.recordErrorStatus == 7)
                    {
                        await VideoApi.SaveOrUpdateVideoAsync(video);
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"视频分析-纠正视频时长发生异常，继续后续流程");
                }

                // 1.切割视频成音频
                audioDirectoryPath = video.storagePath.Substring(0, video.storagePath.LastIndexOf("\\"));
                audioDirectoryPath += "\\audio\\";
                audioDirectoryPath += video.videoName.Substring(0, video.videoName.LastIndexOf("."));
                FileUtils.LogAnalysis($"{video.videoId}", $"视频分析-开始切割视频成音频");
                AudioUtils.SlicingAudio(mp4Path, audioDirectoryPath);
                FileUtils.LogAnalysis($"{video.videoId}", $"视频分析-切割音频完成");
            }
            catch (Exception ex)
            {
                string errorMsg = $"音频切割失败：{ex.Message}";
                FileUtils.LogAnalysis($"{ex}", $"切割视频成音频发生异常");
                await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, errorMsg);
                return;
            }

            try
            {
                AnchorEntity anchorInfo = await AnchorApi.GetCurrUserAnchorBySecUid(video.secUid);

                // 3.将音频文件夹传入文字识别接口（手动重新分析强制走 Tencent；videoId 用于「加速」在飞切腾讯）
                Dictionary<string, object> result = await AsrUtils.AsrByDirectoryPath(audioDirectoryPath, ReplayHttpUtils.Token, anchorInfo.engSerViceType, AsrConsumerType.AnchorReplay, source == AnalysisSource.ManualRetry, video.videoId);
                string asrError = await AnalysisUtils.checkAsrError(result);
                if(!string.IsNullOrEmpty(asrError))
                {
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, asrError);
                    return;
                }

                // 取出分析结果
                List<ASRResultEntity> asrResultEntities = (List<ASRResultEntity>)result["data"];
                if(asrResultEntities == null || asrResultEntities.Count < 1)
                {
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "音频识别异常");
                    return;
                }

                // 将分析结果传到服务器，进行关键词/敏感词识别
                AnalysisResultVo analysisResultVo = await WordApi.WordsMark(1, video.videoId, asrResultEntities, video.tradeId, 0);
                if (analysisResultVo == null)
                {
                    await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络不佳");
                }

                // 将分析结果存到本地文件
                AnalysisUtils.saveVideoLocalAnalysisData(analysisResultVo, video.tradeId, video);
                FileUtils.LogAnalysis($"{video?.videoName}", $"整理分析数据成功");

                // 扣减分析资源
                int duration = (int)(Convert.ToDouble(video.duration) / 60) < 1 ? 1 : (int)(Convert.ToDouble(video.duration) / 60);
                if (video.useAnalysisPropertyType == 0)
                {
                    // 扣除智能分析时长
                    UserPropertyHttpUtils.UpdateUserProperty("aiAnalysisTime", duration);
                }
                else
                {
                    // 扣除文案提取时长
                    UserPropertyHttpUtils.UpdateUserProperty("textExtractionNum", duration);
                }

                // 将视频分析状态改成分析完成
                FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(video)}", $"视频分析成功");
                await VideoApi.UpdateVideoAnalysisStatusSuccess(video.videoId, 2, 1);

                // 检测是否需要自动生成诊断报告/生成自然/优化原文
                if (video.useAnalysisPropertyType == 0 && video.videoSliceType == 0)
                {
                    AnalysisUtils.checkAutoGenerate(video);
                }

                // 判断是否需要自动上传云空间
                if (ReplayHttpUtils.UserClientVersion.Equals(ClientVersion.Replay))
                {
                    checkAutoUploadCloud(anchorInfo, video);
                }
                
                // 上传场景切片
                SceneSliceBll.Execute(video.videoId);

            }
            catch(Exception e)
            {
                await VideoApi.UpdateVideoAnalysisStatus(video.videoId, 3, "网络不佳");
                FileUtils.LogAnalysis($"{e}", $"分析视频发生异常");
            }
            
        }

        /// <summary>
        /// 检查是否需要自动上传云空间
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="video">视频信息</param>
        /// <returns></returns>
        public async Task checkAutoUploadCloud(AnchorEntity anchorInfo, VideoEntity video)
        {
            try
            {
                FileUtils.LogAnalysis($"主播状态：{anchorInfo.isAutoUploadCloud},视频信息：{JsonConvert.SerializeObject(video)} ", $"检测自动上传");
                if (anchorInfo.isAutoUploadCloud == 1 && video.uploadStatus == 0)
                {
                    // 判断云空间资源是否足够
                    string videoPath = video.storagePath.Substring(0, video.storagePath.LastIndexOf("\\"));
                    videoPath += "\\mp4\\";
                    videoPath += video.videoName + ".mp4";

                    if (File.Exists(videoPath))
                    {
                        // 文件存在
                        FileInfo fileInfo = new FileInfo(videoPath);
                        long fileSize = fileInfo.Length / 1024; // 文件大小，KB

                        UserPropertyEntity userProperty = await UserPropertyApi.GetPropertyInfo();
                        FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(userProperty)}", $"检测自动上传-资产");
                        if (userProperty != null && userProperty.StorageNum >= fileSize)
                        {
                            // 自动上传
                            new Thread(() => AutoUploadCloud(video)).Start();
                        }
                        else
                        {
                            // 资源不足
                            new Thread(() => NoticeCloudPropertyInsufficient()).Start();
                        }
                    }

                }
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"自动上传云空间错误==={video.videoId}");
            }
        }

        /// <summary>
        /// 自动上传到云空间
        /// </summary>
        /// <param name="anchorVideo">视频信息</param>
        /// <returns></returns>
        public async Task AutoUploadCloud(VideoEntity video)
        {
            try
            {
                // 修改视频的上传状态为上传中
                await VideoApi.UpdateVideoUploadStatus(video.videoId, 2);

                // 通知前端上传
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "autoUploadCloud";
                var dataObj = Compress(video.videoId);
                string filePath = (string)dataObj["filePath"];
                dataObj["filePath"] = filePath.Replace("\\", "/");
                dataObj["videId"] = video.videoId;
                requestDataObj["data"] = dataObj;
                FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(requestDataObj)}", $"开始自动上传到云空间");

                string response = await frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj), true);

                JObject jObject = JObject.Parse(response);
                int code = (int)jObject["code"];
                string data = (string)jObject["data"];
                if (code == 0 && !string.IsNullOrEmpty(data))
                {
                    FileUtils.LogAnalysis($"{response}", $"自动上传到云空间成功");
                    int fileSize = (int)dataObj["fileSize"]; // 文件大小，MB
                    UserPropertyHttpUtils.UpdateUserProperty("storageNum", fileSize * 1024); // 消耗云空间资源
                    // 上传成功
                    ShareAnalysis(video.videoId, data);
                }
                else
                {
                    // 上传失败
                    FileUtils.LogAnalysis($"{response}", $"自动上传到云空间失败");

                    await VideoApi.UpdateVideoUploadStatus(video.videoId, 0);
                }
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"自动上传到云空间发生异常");
                await VideoApi.UpdateVideoUploadStatus(video.videoId, 0);
            }
            
        }

        /// <summary>
        /// 通知前端云空间资源不足
        /// </summary>
        /// <returns></returns>
        public async Task NoticeCloudPropertyInsufficient()
        {
            try
            {
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "cloudPropertyInsufficient";

                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));

            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"通知前端云空间资源不足发生异常");
            }

        }


        /// <summary>
        /// 查看视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        public SentenceMarkDto LockAnalysis(string videoId)
        {
            SentenceMarkDto sentenceMarkDto = new SentenceMarkDto();

            // 查询视频信息
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if (videoEntity != null)
            {

                // 配置序列化忽略大小写，忽略null值
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                    NullValueHandling = NullValueHandling.Ignore
                };
                videoEntity.id = 0;
                AnchorVideo anchorVideo = JsonConvert.DeserializeObject<AnchorVideo>(JsonConvert.SerializeObject(videoEntity), settings);

                if (File.Exists(anchorVideo.StoragePath))
                {
                    VideoUtils.ConvertToMP4(anchorVideo.StoragePath, anchorVideo.VideoName);
                }

                sentenceMarkDto.videoInfo = anchorVideo;

                // 获取主播信息
                AnchorEntity anchorEntity = null;
                if (!string.IsNullOrEmpty(videoEntity.userId) && videoEntity.userId.Equals(ReplayHttpUtils.UserId))
                {
                    anchorEntity = AnchorApi.GetCurrUserAnchorBySecUidSync(anchorVideo.SecUid);
                }
                else
                {
                    anchorEntity = AnchorApi.GetAnchorBySecUidSync(anchorVideo.SecUid);
                }
                AnchorInfo anchorInfo = null;
                if (anchorEntity != null)
                {
                    anchorEntity.id = 0;
                    anchorEntity.anchorInfo.id = 0;
                    anchorInfo = JsonConvert.DeserializeObject<AnchorInfo>(JsonConvert.SerializeObject(anchorEntity.anchorInfo), settings);
                    anchorInfo.FolderName = anchorEntity.folderName;
                    anchorInfo.AccountType = anchorEntity.accountType ?? 0;
                    anchorInfo.IsRemoveRecord = anchorEntity.isRemoveRecord;
                    anchorInfo.pureRecordOnlineNum = anchorEntity.pureRecordOnlineNum;
                    // 查询是否巨量授权
                    if (ReplayHttpUtils.UserId == videoEntity.userId)
                    {
                        AnchorInfo temp = AnchorCacheManager.GetAnchorByIdFromCache(anchorInfo.SecUid);
                        anchorInfo.AccountType = temp?.AccountType ?? 1;
                        anchorInfo.juliangAuthStatus = temp?.juliangAuthStatus ?? 0;
                    }

                    sentenceMarkDto.anchorInfo = anchorInfo;
                }

                // 获取视频播放地址
                try
                {
                    // 获取mp4地址
                    string tsVideoPath = anchorVideo.StoragePath;
                    string mp4VideoPath = tsVideoPath.Substring(0, tsVideoPath.LastIndexOf("\\"));
                    mp4VideoPath += $"\\mp4\\{anchorVideo.VideoName}.mp4";
                    if (File.Exists(mp4VideoPath) && anchorInfo != null)
                    {
                        try
                        {
                            // 截取主播名称
                            MatchCollection matches = Regex.Matches(tsVideoPath, @"\\");
                            int startPosition = matches[matches.Count - 3].Index + 1;
                            int endPosition = matches[matches.Count - 2].Index;
                            string anchorName = tsVideoPath.Substring(startPosition, endPosition - startPosition);
                            // 获取mp4播放地址
                            string serverPath = tsVideoPath.Substring(0, tsVideoPath.IndexOf(anchorName));
                            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                            string port = httpResourceFileServer.GetPort(serverPath);

                            string videoPath = "http://localhost:" + port + "/" + mp4VideoPath.Substring(mp4VideoPath.IndexOf(anchorName));
                            sentenceMarkDto.playUrl = videoPath;
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"{ex}", "使用存储路径获取播放路径失败，换方式");

                            string serverPath = tsVideoPath.Substring(0, tsVideoPath.IndexOf(!string.IsNullOrEmpty(anchorInfo.FolderName) ? anchorInfo.FolderName : anchorInfo.AnchorName));
                            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                            string port = httpResourceFileServer.GetPort(serverPath);

                            string videoPath = "http://localhost:" + port + "/" + mp4VideoPath.Substring(mp4VideoPath.IndexOf(!string.IsNullOrEmpty(anchorInfo.FolderName) ? anchorInfo.FolderName : anchorInfo.AnchorName));
                            sentenceMarkDto.playUrl = videoPath;
                        }

                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"获取视频播放地址发生异常====={JsonConvert.SerializeObject(anchorVideo)}================={JsonConvert.SerializeObject(anchorInfo)}");
                }

                // 判断本地有没有文件
                AnalysisResultTxtVo analysisResultTxtVo = AnalysisUtils.getVideoLocalAnalysisData(videoEntity);
                if (analysisResultTxtVo != null)
                {
                    sentenceMarkDto.audioaAlyses = analysisResultTxtVo.audioaAlyses;
                    sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                    sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                    sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                }
                else
                {
                    // 本地没有分析记录，从服务器同步
                    string zipPath = ReplayHttpUtils.DownloadAnalysisFile(videoId, null, Path.GetFullPath($"analysisTemp\\{videoId}.zip"));
                    if (!string.IsNullOrEmpty(zipPath))
                    {
                        // 将压缩包里面的内容转成字符串
                        string jsonCentent = ZipUtils.ReadFileFromZip(zipPath);
                        if (!string.IsNullOrEmpty(jsonCentent))
                        {

                            AnalysisResultVo analysisResultVo = JsonConvert.DeserializeObject<AnalysisResultVo>(jsonCentent, settings);
                            //将分析数据存到本地文件
                            analysisResultTxtVo = AnalysisUtils.saveVideoLocalAnalysisData(analysisResultVo, videoEntity.tradeId, videoEntity);

                            sentenceMarkDto.audioaAlyses = analysisResultTxtVo.audioaAlyses;
                            sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                            sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                            sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                        }
                        if (File.Exists(zipPath))
                        {
                            File.Delete(zipPath);
                        }
                    }
                }

                List<OnlineNum> onlineNumList = new List<OnlineNum>();
                // 查询在线人数
                DataWebSocket webSocketEntity = WebsocketDataHandle.GetWebsocketData(videoEntity);
                int observationNum = -1;
                if (webSocketEntity != null)
                {
                    foreach (var item in webSocketEntity.datas)
                    {
                        if (!string.IsNullOrEmpty(item.renshu))
                        {
                            OnlineNum itemOnlineNum = new OnlineNum();
                            itemOnlineNum.RecordDate = item.time;
                            itemOnlineNum.PeopleNum = item.renshu;
                            onlineNumList.Add(itemOnlineNum);
                        }
                    }
                    // 获取场观
                    int.TryParse(webSocketEntity.observationNum, out observationNum);
                }
                if (onlineNumList.Count == 0)
                {
                    onlineNumList.Add(new OnlineNum()
                    {
                        RecordDate = sentenceMarkDto?.videoInfo?.StartTime,
                        PeopleNum = "0"
                    });
                }
                sentenceMarkDto.onlineNumList = onlineNumList;

                // 获取巨量的数据
                VideoDataViewingConfuseVo dataViewingConfuseVo = OceanEngineDataApi.videodataviewingInfoByVideoId(videoId);
                if (dataViewingConfuseVo != null)
                {
                    // 互动率
                    sentenceMarkDto.interactionPercent = dataViewingConfuseVo.interactionPercent;

                    // 成交量
                    sentenceMarkDto.purchaseCountStart = dataViewingConfuseVo.purchaseCountStart;
                    sentenceMarkDto.purchaseCountEnd = dataViewingConfuseVo.purchaseCountEnd;
                    sentenceMarkDto.purchaseCount = dataViewingConfuseVo.purchaseCountStart;


                    // 总观看人次
                    sentenceMarkDto.totalWatchNum = dataViewingConfuseVo.totalWatchNum;

                    // 数据来源类型
                    sentenceMarkDto.dataSourceType = dataViewingConfuseVo.dataSourceType;

                    // uv
                    sentenceMarkDto.uvValueEnd = dataViewingConfuseVo.uvValueEnd;
                    sentenceMarkDto.uvValueStart = dataViewingConfuseVo.uvValueStart;

                    // 销售额
                    sentenceMarkDto.volumeStart = dataViewingConfuseVo.volumeStart;
                    sentenceMarkDto.volumeEnd = dataViewingConfuseVo.volumeEnd;

                    // 投放消耗（元）
                    sentenceMarkDto.totalQianchuanCost = dataViewingConfuseVo.launchRoiAmount;
                    // 净成交ROI
                    sentenceMarkDto.totalNetTransactionRoi = dataViewingConfuseVo.netTransactionRoi;
                }

                // 对应分钟段落的成交数量列表
                sentenceMarkDto.juLiangDataList = JuliangDataHandle.getParagraphJuLiang(videoId, sentenceMarkDto.audioaAlyses, sentenceMarkDto?.videoInfo?.StartTime ?? "", videoEntity.platformType);
                var oceanEngineCurves = JuliangDataHandle.getOceanEngineCurves(videoId, sentenceMarkDto.audioaAlyses, sentenceMarkDto?.videoInfo?.StartTime ?? "", videoEntity.platformType);
                sentenceMarkDto.qianchuanCostDataList = oceanEngineCurves.qianchuanCostList;
                sentenceMarkDto.netTransactionRoiDataList = oceanEngineCurves.netTransactionRoiList;

                // 弹幕标注
                BarrageDataVo barrageDataVo = BarrageApi.videoBarrageData(videoId);
                sentenceMarkDto.barrageDataList = barrageDataVo.barrageDataList;
                sentenceMarkDto.totalBarrageNum = barrageDataVo.totalBarrageNum;
                if (sentenceMarkDto.interactionPercent == null && observationNum > 0 && sentenceMarkDto.totalBarrageNum != null && sentenceMarkDto.totalBarrageNum > 0)
                {
                    sentenceMarkDto.interactionPercent = Math.Round((double)sentenceMarkDto.totalBarrageNum / (double)observationNum * 100, 4);
                }

                if (videoEntity.useAnalysisPropertyType == 1)
                {
                    // 当前是文案提取，去掉数据
                    // sentenceMarkDto.onlineNumList = null;
                    sentenceMarkDto.barrageDataList = null;
                    sentenceMarkDto.totalBarrageNum = 0;
                    sentenceMarkDto.cruxTypeList = null;
                    sentenceMarkDto.wordsTabList = null;
                    sentenceMarkDto.wordsCollect = null;
                }

                // 获取是否已推荐行业
                AnchorVideoDetailVo anchorVideoDetail = VideoApi.infoDetailByVideoId(videoId);
                sentenceMarkDto.suggestTrade = anchorVideoDetail?.suggestTrade ?? 0;

                // 获取视频的行业信息
                TradeVo tradeVo = TradeApi.GetTradeById(videoEntity.tradeId);
                sentenceMarkDto.tradeInfo = tradeVo;

                return sentenceMarkDto;
            }
            return null;

        }


        /// <summary>
        /// 异步查看视频分析,LockAnalysis方法改写，添加Async后缀（规范）
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>

        public async Task<SentenceMarkDto> LockVideoSisAnalyAsync(string videoId)
        {
            SentenceMarkDto sentenceMarkDto = new SentenceMarkDto();

            try
            {
                // 1. 异步查询视频信息
                VideoEntity videoEntity = await VideoApi.GetVideoByVideoIdAsync(videoId);

                if (videoEntity != null)
                {
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                        NullValueHandling = NullValueHandling.Ignore
                    };
                    videoEntity.id = 0;
                    AnchorVideo anchorVideo = JsonConvert.DeserializeObject<AnchorVideo>(JsonConvert.SerializeObject(videoEntity), settings);

                    // 2. 异步转码视频
                    if (File.Exists(anchorVideo.StoragePath))
                    {
                        await VideoUtils.ConvertToMP4Async(anchorVideo.StoragePath, anchorVideo.VideoName);
                    }

                    sentenceMarkDto.videoInfo = anchorVideo;

                    // 3. 异步获取主播信息
                    AnchorEntity anchorEntity = null;
                    if (!string.IsNullOrEmpty(videoEntity.userId) && videoEntity.userId.Equals(ReplayHttpUtils.UserId))
                    {
                        anchorEntity = await AnchorApi.GetCurrUserAnchorBySecUidAsync(anchorVideo.SecUid);
                    }
                    else
                    {
                        anchorEntity = await AnchorApi.GetAnchorBySecUidAsync(anchorVideo.SecUid);
                    }

                    AnchorInfo anchorInfo = null;
                    if (anchorEntity != null)
                    {
                        anchorEntity.id = 0;
                        anchorEntity.anchorInfo.id = 0;
                        anchorInfo = JsonConvert.DeserializeObject<AnchorInfo>(JsonConvert.SerializeObject(anchorEntity.anchorInfo), settings);
                        anchorInfo.FolderName = anchorEntity.folderName;
                        anchorInfo.AccountType = anchorEntity.accountType ?? 0;
                        anchorInfo.IsRemoveRecord = anchorEntity.isRemoveRecord;

                        if (ReplayHttpUtils.UserId == videoEntity.userId)
                        {
                            AnchorInfo temp = await AnchorCacheManager.GetAnchorByIdFromCacheAsync(anchorInfo.SecUid);
                            anchorInfo.AccountType = temp?.AccountType ?? 1;
                            anchorInfo.juliangAuthStatus = temp?.juliangAuthStatus ?? 0;
                        }

                        sentenceMarkDto.anchorInfo = anchorInfo;
                    }

                    // 4. 异步获取视频播放地址
                    try
                    {
                        string mp4VideoPath = anchorVideo.StoragePath.Substring(0, anchorVideo.StoragePath.LastIndexOf("\\"));
                        mp4VideoPath += $"\\mp4\\{anchorVideo.VideoName}.mp4";
                        if (File.Exists(mp4VideoPath) && anchorInfo != null)
                        {
                            // 逐级向上获取目录  D:\workSpace\visualStudio\ReviewAnalysis\bin\Debug\download\崔师傅面点技术\20251128\a.ts
                            // D:\workSpace\visualStudio\ReviewAnalysis\bin\Debug\download
                            string serverPath = Path.GetDirectoryName(anchorVideo.StoragePath);
                            serverPath = Path.GetDirectoryName(serverPath);
                            serverPath = Path.GetDirectoryName(serverPath);

                            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                            string port = await httpResourceFileServer.GetPortAsync(serverPath + "\\");

                            string videoPath = "http://localhost:" + port + mp4VideoPath.Substring(serverPath.Length);
                            sentenceMarkDto.playUrl = videoPath;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", $"获取视频播放地址发生异常====={JsonConvert.SerializeObject(anchorVideo)}================={JsonConvert.SerializeObject(anchorInfo)}");
                    }

                    // 5. 异步读取本地分析文件（使用AnalysisUtils工具类）
                    AnalysisResultTxtVo analysisResultTxtVo = AnalysisUtils.getVideoLocalAnalysisData(videoEntity);
                    if (analysisResultTxtVo != null)
                    {
                        sentenceMarkDto.audioaAlyses = analysisResultTxtVo.audioaAlyses;
                        sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                        sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                        sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                    }
                    else
                    {
                        // 6. 异步下载服务器分析文件
                        string zipPath = Path.GetFullPath($"analysisTemp\\{videoId}.zip");
                        string downloadedZipPath = await ReplayHttpUtils.DownloadAnalysisFileAsync(videoId, null, zipPath);

                        if (!string.IsNullOrEmpty(downloadedZipPath))
                        {
                            string jsonContent = await ZipUtils.ReadFileFromZipAsync(downloadedZipPath);
                            if (!string.IsNullOrEmpty(jsonContent))
                            {
                                AnalysisResultVo analysisResultVo = JsonConvert.DeserializeObject<AnalysisResultVo>(jsonContent, settings);

                                // 使用AnalysisUtils保存本地文件
                                analysisResultTxtVo = AnalysisUtils.saveVideoLocalAnalysisData(analysisResultVo, videoEntity.tradeId, videoEntity);

                                sentenceMarkDto.audioaAlyses = analysisResultTxtVo.audioaAlyses;
                                sentenceMarkDto.cruxTypeList = analysisResultTxtVo.cruxTypeList;
                                sentenceMarkDto.wordsCollect = analysisResultTxtVo.wordsCollect;
                                sentenceMarkDto.wordsTabList = analysisResultTxtVo.wordsTabList;
                            }

                            if (File.Exists(downloadedZipPath))
                            {
                                File.Delete(downloadedZipPath);
                            }
                        }
                    }

                    // 7. 异步获取在线人数（使用原有方法，不需要异步）
                    List<OnlineNum> onlineNumList = new List<OnlineNum>();
                    DataWebSocket webSocketEntity = WebsocketDataHandle.GetWebsocketData(videoEntity);
                    int observationNum = -1;
                    if (webSocketEntity != null)
                    {
                        foreach (var item in webSocketEntity.datas)
                        {
                            if (!string.IsNullOrEmpty(item.renshu))
                            {
                                onlineNumList.Add(new OnlineNum
                                {
                                    RecordDate = item.time,
                                    PeopleNum = item.renshu
                                });
                            }
                        }
                        int.TryParse(webSocketEntity.observationNum, out observationNum);
                    }

                    if (onlineNumList.Count == 0)
                    {
                        onlineNumList.Add(new OnlineNum
                        {
                            RecordDate = sentenceMarkDto?.videoInfo?.StartTime,
                            PeopleNum = "0"
                        });
                    }
                    sentenceMarkDto.onlineNumList = onlineNumList;

                    // 8. 异步获取巨量数据
                    VideoDataViewingConfuseVo dataViewingConfuseVo = await OceanEngineDataApi.videodataviewingInfoByVideoIdAsync(videoId);
                    if (dataViewingConfuseVo != null)
                    {
                        // 互动率
                        sentenceMarkDto.interactionPercent = dataViewingConfuseVo.interactionPercent;

                        // 成交量
                        sentenceMarkDto.purchaseCountStart = dataViewingConfuseVo.purchaseCountStart;
                        sentenceMarkDto.purchaseCountEnd = dataViewingConfuseVo.purchaseCountEnd;
                        sentenceMarkDto.purchaseCount = dataViewingConfuseVo.purchaseCountStart;

                        // 总观看人次
                        sentenceMarkDto.totalWatchNum = dataViewingConfuseVo.totalWatchNum;

                        // 数据来源类型
                        sentenceMarkDto.dataSourceType = dataViewingConfuseVo.dataSourceType;

                        // uv
                        sentenceMarkDto.uvValueEnd = dataViewingConfuseVo.uvValueEnd;
                        sentenceMarkDto.uvValueStart = dataViewingConfuseVo.uvValueStart;

                        // 销售额
                        sentenceMarkDto.volumeStart = dataViewingConfuseVo.volumeStart;
                        sentenceMarkDto.volumeEnd = dataViewingConfuseVo.volumeEnd;

                        // 投放消耗（元）
                        sentenceMarkDto.totalQianchuanCost = dataViewingConfuseVo.launchRoiAmount;
                        // 净成交ROI
                        sentenceMarkDto.totalNetTransactionRoi = dataViewingConfuseVo.netTransactionRoi;
                    }

                    // 9. 巨量段落数据
                    sentenceMarkDto.juLiangDataList = JuliangDataHandle.getParagraphJuLiang(videoId, sentenceMarkDto.audioaAlyses, sentenceMarkDto?.videoInfo?.StartTime ?? "", videoEntity.platformType);
                    var oceanEngineCurves = JuliangDataHandle.getOceanEngineCurves(videoId, sentenceMarkDto.audioaAlyses, sentenceMarkDto?.videoInfo?.StartTime ?? "", videoEntity.platformType);
                    sentenceMarkDto.qianchuanCostDataList = oceanEngineCurves.qianchuanCostList;
                    sentenceMarkDto.netTransactionRoiDataList = oceanEngineCurves.netTransactionRoiList;

                    // 10. 异步获取弹幕数据
                    BarrageDataVo barrageDataVo = await BarrageApi.videoBarrageDataAsync(videoId);
                    sentenceMarkDto.barrageDataList = barrageDataVo.barrageDataList;
                    sentenceMarkDto.totalBarrageNum = barrageDataVo.totalBarrageNum;
                    if (sentenceMarkDto.interactionPercent == null && observationNum > 0 && sentenceMarkDto.totalBarrageNum != null && sentenceMarkDto.totalBarrageNum > 0)
                    {
                        sentenceMarkDto.interactionPercent = Math.Round((double)sentenceMarkDto.totalBarrageNum / (double)observationNum * 100, 4);
                    }

                    // 11. 过滤文案提取场景的数据
                    if (videoEntity.useAnalysisPropertyType == 1)
                    {
                        // sentenceMarkDto.onlineNumList = null;
                        sentenceMarkDto.barrageDataList = null;
                        sentenceMarkDto.totalBarrageNum = 0;
                        sentenceMarkDto.cruxTypeList = null;
                        sentenceMarkDto.wordsTabList = null;
                        sentenceMarkDto.wordsCollect = null;
                    }

                    // 12. 异步获取推荐行业信息
                    AnchorVideoDetailVo anchorVideoDetail = await VideoApi.infoDetailByVideoIdAsync(videoId);
                    sentenceMarkDto.suggestTrade = anchorVideoDetail?.suggestTrade ?? 0;

                    // 13. 获取视频的行业信息
                    TradeVo tradeVo = await TradeApi.GetTradeByIdAsync(videoEntity.tradeId);
                    sentenceMarkDto.tradeInfo = tradeVo;

                    return sentenceMarkDto;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"视频分析主流程异常（videoId：{videoId}）：{ex}", ex.StackTrace);
            }
            return sentenceMarkDto;
        }


        /// <summary>
        /// 查看视频分析对比
        /// </summary>
        /// <param name="videoId">对比id</param>
        public SentenceMarkContrastDto LockAnalysisContrast(string contrastId)
        {
            // 获取对比信息
            ContrastVo contrast = ConstrastApi.GetByContrastId(contrastId);
            if(contrast == null)
            {
                throw new Exception("对比信息不存在");
            }

            SentenceMarkContrastDto sentenceMarkContrastDto = new SentenceMarkContrastDto();

            if(!string.IsNullOrEmpty(contrast.videoOneId))
            {
                // 对比的是录制的视频
                sentenceMarkContrastDto.SentenceMark1 = LockAnalysis(contrast.videoOneId);
                sentenceMarkContrastDto.SentenceMark2 = LockAnalysis(contrast.videoTwoId);
            }
            else
            {
                // 对比的是上传的文件
                UploadFileBll uploadFileBll = new UploadFileBll();
                sentenceMarkContrastDto.SentenceMark1 = uploadFileBll.LockAnalysis(contrast.fileOneId);
                sentenceMarkContrastDto.SentenceMark2 = uploadFileBll.LockAnalysis(contrast.fileTwoId);
            }

            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
            };
            sentenceMarkContrastDto.VideoContrast = JsonConvert.DeserializeObject<VideoContrast>(JsonConvert.SerializeObject(contrast), settings);

            return sentenceMarkContrastDto;
        }

        /// <summary>
        /// 查看视频分析对比（异步版本）
        /// </summary>
        /// <param name="contrastId">对比id</param>
        public async Task<SentenceMarkContrastDto> LockAnalysisContrastAsync(string contrastId)
        {
            // 获取对比信息（使用异步API）
            ContrastVo contrast = await ConstrastApi.GetByContrastIdAsync(contrastId).ConfigureAwait(false);
            if(contrast == null)
            {
                throw new Exception("对比信息不存在");
            }

            SentenceMarkContrastDto sentenceMarkContrastDto = new SentenceMarkContrastDto();

            if(!string.IsNullOrEmpty(contrast.videoOneId))
            {
                // 对比的是录制的视频
                sentenceMarkContrastDto.SentenceMark1 = LockAnalysis(contrast.videoOneId);
                sentenceMarkContrastDto.SentenceMark2 = LockAnalysis(contrast.videoTwoId);
            }
            else
            {
                // 对比的是上传的文件
                UploadFileBll uploadFileBll = new UploadFileBll();
                sentenceMarkContrastDto.SentenceMark1 = uploadFileBll.LockAnalysis(contrast.fileOneId);
                sentenceMarkContrastDto.SentenceMark2 = uploadFileBll.LockAnalysis(contrast.fileTwoId);
            }

            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
            };
            sentenceMarkContrastDto.VideoContrast = JsonConvert.DeserializeObject<VideoContrast>(JsonConvert.SerializeObject(contrast), settings);

            return sentenceMarkContrastDto;
        }

        /// <summary>
        /// 预览视频
        /// </summary>
        /// <param name="videoId">视频id</param>
        public string preview(string videoId)
        {
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);

            if (!File.Exists(videoEntity.storagePath))
            {
                // 文件不存在
                throw new Exception("本地不存在视频文件");
            }

            VideoUtils.ConvertToMP4(videoEntity.storagePath, videoEntity.videoName);


            // 获取主播信息
            AnchorEntity anchorEntity = null;
            if (!string.IsNullOrEmpty(videoEntity.userId) && videoEntity.userId.Equals(ReplayHttpUtils.UserId))
            {
                anchorEntity = AnchorApi.GetCurrUserAnchorBySecUidSync(videoEntity.secUid);
            }
            else
            {
                anchorEntity = AnchorApi.GetAnchorBySecUidSync(videoEntity.secUid);
            }

            string serverPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.IndexOf(anchorEntity.folderName));

            HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
            string port = httpResourceFileServer.GetPort(serverPath);

            string videoPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
            videoPath += "\\mp4\\";
            videoPath += videoEntity.videoName + ".mp4";
            videoPath = "http://localhost:"+port+"/" + videoPath.Substring(videoPath.IndexOf(anchorEntity.folderName));

            return videoPath;
        }

        /// <summary>
        /// 打开视频所在的目录
        /// </summary>
        /// <param name="videoId">视频Id</param>
        /// <returns></returns>
        public bool OpenFolder(string videoId)
        {
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if(videoEntity == null)
            {
                throw new Exception("视频信息不存在");
            }

            if (0 == (videoEntity.isDownloaded??1))
            {
                int status = videoEntity.analysisStatus ?? 0;
                if (status != 1)
                {
                    throw new Exception("视频还未下载，下载完成后才有视频文件夹");
                }
                else
                {
                    throw new Exception("视频正在下载中，下载完成后才有视频文件夹");
                }
            }

            string videoPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
            if(!Directory.Exists(videoPath))
            {
                throw new Exception("文件夹不存在");
            }

            //explorer.exe是Windows操作系统中的文件资源管理器程序，用于浏览文件系统、管理文件和文件夹
            Process.Start("explorer.exe", videoPath);
            return true;
        }

        /// <summary>
        /// 重新选择行业分析
        /// </summary>
        /// <param name="videoId">视频Id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="platformType">平台类型 0：全平台 1：...</param>
        /// <param name="duration">消耗时长，单位：分钟</param>
        public SentenceMarkDto ReAnalysisByTrade(string videoId, string tradeId, string platformType)
        {

            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);

            if(videoEntity == null)
            {
                throw new Exception("视频信息不存在，操作失败");
            }

            // 修改视频的行业
            videoEntity.tradeId = tradeId;
            VideoApi.SaveOrUpdateVideo(videoEntity);

            // 配置序列化忽略大小写，忽略null值
            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                NullValueHandling = NullValueHandling.Ignore
            };
            videoEntity.id = 0;
            AnchorVideo anchorVideo = JsonConvert.DeserializeObject<AnchorVideo>(JsonConvert.SerializeObject(videoEntity), settings);

            anchorVideo.TradeId = tradeId;
            anchorVideo.PlatformType = platformType;
            anchorVideo.AnalysisTime = ServerTimeUtils.getCurrentTimeStr();

            // 重新分析
            AnalysisResultVo analysisResultVo = WordApi.WordsMarkSync(platformType, anchorVideo.VideoId, tradeId, 0);
            if (analysisResultVo != null)
            {
                // 将分析数据存到本地文件
                AnalysisUtils.saveVideoLocalAnalysisData(analysisResultVo, tradeId, videoEntity);

            }
            else
            {
                // 将文件分析状态改成失败，原因：关键词/敏感词识别失败
                throw new Exception("分析失败，请检查网络后再重试");
            }

            // 获取新的分析数据
            return LockAnalysis(videoId);


        }

        /// <summary>
        /// 重新选行业分析-异步
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="platformType">平台类型</param>
        /// <returns></returns>
        public async Task<SentenceMarkDto> ReAnalysisByTradeAsync(string videoId, string tradeId, string platformType)
        {
            // 异步查询视频信息
            VideoEntity videoEntity = await VideoApi.GetVideoByVideoIdAsync(videoId);

            if(videoEntity == null)
            {
                throw new Exception("视频信息不存在，操作失败");
            }

            // 修改视频的行业
            videoEntity.tradeId = tradeId;
            VideoApi.SaveOrUpdateVideo(videoEntity);

            // 配置序列化忽略大小写，忽略null值
            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                NullValueHandling = NullValueHandling.Ignore
            };
            videoEntity.id = 0;
            AnchorVideo anchorVideo = JsonConvert.DeserializeObject<AnchorVideo>(JsonConvert.SerializeObject(videoEntity), settings);

            anchorVideo.TradeId = tradeId;
            anchorVideo.PlatformType = platformType;
            anchorVideo.AnalysisTime = ServerTimeUtils.getCurrentTimeStr();

            // 异步重新分析
            AnalysisResultVo analysisResultVo = await WordApi.WordsMarkAsync(platformType, anchorVideo.VideoId, tradeId, 0);
            if (analysisResultVo != null)
            {
                // 将分析数据存到本地文件
                AnalysisUtils.saveVideoLocalAnalysisData(analysisResultVo, tradeId, videoEntity);
            }
            else
            {
                // 将文件分析状态改成失败，原因：关键词/敏感词识别失败
                throw new Exception("分析失败，请检查网络后再重试");
            }

            // 异步获取新的分析数据
            return await LockVideoSisAnalyAsync(videoId);
        }

        /// <summary>
        /// 根据视频id集合删除视频
        /// <param name="ids">视频id集合</param>
        /// </summary>
        /// <param name="ids">根据视频id集合</param>
        public void Delete(List<string> ids)
        {
            if(ids != null || ids.Count > 0)
            {

                // 批量获取信息
                List<VideoEntity> videoEntities = VideoApi.ListVideoByVideoIdsSync(ids);

                if (videoEntities != null && videoEntities.Count > 0)
                {

                    foreach (var video in videoEntities)
                    {
                        if (File.Exists(video.storagePath))
                        {
                            try
                            {
                                using (var fs = File.Open(video.storagePath, FileMode.Open, FileAccess.ReadWrite, FileShare.None))
                                {
                                    // 尝试是否能打开
                                }
                            }
                            catch (IOException ex)
                            {
                                throw new Exception($"{video.videoName}文件正在被占用，删除失败，请明天再试");
                            }
                        }
                    }

                    // 批量删除服务器视频
                    VideoApi.DelVideoByIdsSync(ids);

                    foreach (var videoEntity in videoEntities)
                    {
                        try
                        {
                            // 删除视频相关信息
                            DelVideoLocalData(videoEntity);
                            // 删除ts视频
                            if (File.Exists(videoEntity.storagePath))
                            {
                                File.Delete(videoEntity.storagePath);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"{ex}", $"删除本地视频发生异常");
                        }
                    }

                }

            }

        }

        /// 根据视频id集合删除视频-异步
        /// <param name="ids">视频id集合</param>
        /// </summary>
        /// <param name="ids">根据视频id集合</param>
        public async Task DeleteAsync(List<string> ids)
        {
            if(ids != null || ids.Count > 0)
            {

                // 批量获取信息
                List<VideoEntity> videoEntities = await VideoApi.ListVideoByVideoIdsAsync(ids);

                if (videoEntities != null && videoEntities.Count > 0)
                {

                    foreach (var video in videoEntities)
                    {
                        if (File.Exists(video.storagePath))
                        {
                            try
                            {
                                using (var fs = File.Open(video.storagePath, FileMode.Open, FileAccess.ReadWrite, FileShare.None))
                                {
                                    // 尝试是否能打开
                                }
                            }
                            catch (IOException ex)
                            {
                                throw new Exception($"{video.videoName}文件正在被占用，删除失败，请明天再试");
                            }
                        }
                    }

                    // 批量删除服务器视频
                    await VideoApi.DelVideoByIds(ids);

                    foreach (var videoEntity in videoEntities)
                    {
                        try
                        {
                            // 删除视频相关信息
                            DelVideoLocalData(videoEntity);
                            // 删除ts视频
                            if (File.Exists(videoEntity.storagePath))
                            {
                                File.Delete(videoEntity.storagePath);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"{ex}", $"删除本地视频发生异常");
                        }
                    }

                }

            }

        }

        /// <summary>
        /// 同步服务器的视频列表到本地
        /// </summary>
        public void syncServerVideo()
        {
            // 获取服务器上的视频
            //List<AnchorVideoInfoVo> serverVideoList = ReplayHttpUtils.GetVideoListByTenantId();
            //if (serverVideoList != null && serverVideoList.Count > 0)
            //{
            //    // 获取本地的视频
            //    List<AnchorVideo> anchorVideos = AnchorVideoCacheManager.GetAnchorVideosByUserId(ReplayHttpUtils.UserId);
            //    if (anchorVideos != null && anchorVideos.Count > 0)
            //    {
            //        // 本地有视频，只同步本地没有的
            //        foreach (var serverVideo in serverVideoList)
            //        {
            //            bool exist = false;
            //            foreach (var item in anchorVideos)
            //            {
            //                if (item.VideoId == serverVideo.VideoId)
            //                {
            //                    exist = true;
            //                    break;
            //                }
            //            }
            //            if (!exist)
            //            {
            //                serverVideo.UserId = ReplayHttpUtils.UserId;
            //                serverVideo.UploadStatus = 1;
            //                if (string.IsNullOrEmpty(serverVideo.PlatformType))
            //                {
            //                    serverVideo.PlatformType = "1";
            //                }
            //                AnchorVideoCacheManager.SetAnchorVideoCache(serverVideo);
            //            }
            //        }

            //    } else
            //    {
            //        // 本地没有视频，同步所有
            //        foreach (var item in serverVideoList)
            //        {
            //            item.UserId = ReplayHttpUtils.UserId;
            //            item.UploadStatus = 1;
            //            if (string.IsNullOrEmpty(item.PlatformType))
            //            {
            //                item.PlatformType = "1";
            //            }
            //            AnchorVideoCacheManager.SetAnchorVideoCache(item);
            //        }
            //    }
            //}

            //// 删除本地视频
            //List<AnchorVideo> anchorVideos = AnchorVideoCacheManager.GetAllList();
            //if(anchorVideos != null && anchorVideos.Count > 0)
            //{
            //    foreach (var item in anchorVideos)
            //    {
            //        AnchorVideoCacheManager.DelAnchorVideoCacheByVideoId(item.VideoId);
            //    }
            //}
            //// 删除本地视频分析记录
            //AudioaAlysis audioaAlysis = new AudioaAlysis();
            //audioaAlysis.DeleteAll();
            //// 删除本地视频音频记录
            //Audio audio = new Audio();
            //audio.DeleteAll();

                //// 从服务器同步视频列表到本地
                //List<AnchorVideo> videoList = ReplayHttpUtils.GetVideoList();
                //if(videoList != null && videoList.Count > 0)
                //{
                //    foreach (var item in videoList)
                //    {
                //        if (string.IsNullOrEmpty(item.PlatformType))
                //        {
                //            item.PlatformType = "1";
                //        }
                //        AnchorVideoCacheManager.SetAnchorVideoCache(item);
                //    }
                //}
        }

        /// <summary>
        /// 同步服务器的对比数据到本地
        /// </summary>
        public void syncServerContrast()
        {
            // 获取服务器上的对比数据
            List<VideoContrast> contrastList = ReplayHttpUtils.GetContrastList();
            if (contrastList != null && contrastList.Count > 0)
            {
                // 获取本地所有对比数据
                VideoContrast videoContrast = new VideoContrast();
                videoContrast.UserId = ReplayHttpUtils.UserId;
                List<VideoContrast> videoContrasts = videoContrast.GetList();

                if (videoContrasts != null && videoContrasts.Count > 0)
                {
                    // 本地有文件，只同步不存在的
                    foreach (var serverContrast in contrastList)
                    {
                        bool exist = false;
                        foreach (var item in videoContrasts)
                        {
                            if (item.ContrastId == serverContrast.ContrastId)
                            {
                                exist = true;
                                break;
                            }
                        }
                        if (!exist)
                        {
                            serverContrast.UserId = ReplayHttpUtils.UserId;
                            serverContrast.IsShard = 1;
                            serverContrast.Save();
                        }
                    }

                }
                else
                {
                    // 本地没有对比数据，同步所有
                    foreach (var item in contrastList)
                    {
                        item.UserId = ReplayHttpUtils.UserId;
                        item.IsShard = 1;
                        item.Save();
                    }
                }
            }

        }


        /// <summary>
        /// 分享文件复盘
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <param name="onlineFileUrl">在线播放地址</param>
        public string ShareAnalysis(string videoId, string onlineFileUrl)
        {

            // 修改视频的分享状态
            VideoApi.shareVideoToCloud(videoId, onlineFileUrl);

            string shareUrl = Constant.GetOnlineUrl() + "onlineAnalysis/0/" + videoId;
            //VideoApi.UpdateVideoUploadStatusSync(videoId, 1, shareUrl, onlineFileUrl);
            return shareUrl;


        }


        /// <summary>
        /// 压缩视频
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        public Dictionary<string, object> Compress(string videoId)
        {
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);

            string videoPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
            videoPath += "\\mp4\\";
            videoPath += videoEntity.videoName + ".mp4";

            if (!File.Exists(videoPath))
            {
                throw new Exception("文件不存在");
            }
            // 压缩视频文件
            Dictionary<string, object> dictionary = VideoUtils.Compress(videoPath);

            object fileSize = 0;
            dictionary.TryGetValue("fileSize", out fileSize);
            videoEntity.cloudStore = (int)fileSize;

            VideoApi.SaveOrUpdateVideo(videoEntity);

            return dictionary;

        }


        /// <summary>
        /// 查看云空间视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        public CloudAnalysisVo LockCloudAnalysis(string videoId)
        {
            // 从服务器获取视频信息
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if (videoEntity != null)
            {

                // 配置序列化忽略大小写，忽略null值
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                    NullValueHandling = NullValueHandling.Ignore
                };
                videoEntity.id = 0;
                AnchorVideo anchorVideo = JsonConvert.DeserializeObject<AnchorVideo>(JsonConvert.SerializeObject(videoEntity), settings);

                anchorVideo.UpdateDate = anchorVideo.UpdateDate.Replace(":", "").Replace("-", "").Replace(" ", "");

                // 查本地有没有zip
                string filePath = Path.GetFullPath($"analysisCloudData20\\video\\{videoId}\\{videoId}-{anchorVideo.UpdateDate}.zip");

                if (!File.Exists(filePath))
                {
                    // 创建文件夹
                    string folderPath = Path.GetDirectoryName(filePath);
                    if (!Directory.Exists(folderPath))
                    {
                        Directory.CreateDirectory(folderPath);
                    }

                    // 从服务器拉取zip
                    string url = $"{ReplayHttpUtils.BaseUrl}/openapi/v2000/getOnlineAnalysisZip/0/{videoId}";

                    filePath = ReplayHttpUtils.DownloadCloudAnalysisFile(url, filePath);

                }

                if (!string.IsNullOrEmpty(filePath) && File.Exists(filePath))
                {
                    // 将zip解压
                    string jsonCentent = ZipUtils.ReadFileFromZip(filePath);
                    if (!string.IsNullOrEmpty(jsonCentent))
                    {
                        CloudAnalysisVo cloudAnalysisVo = JsonConvert.DeserializeObject<CloudAnalysisVo>(jsonCentent);

                        // 弹幕标注
                        BarrageDataVo barrageDataVo = BarrageApi.videoBarrageData(videoId);
                        cloudAnalysisVo.barrageDataList = barrageDataVo.barrageDataList;
                        cloudAnalysisVo.totalBarrageNum = barrageDataVo.totalBarrageNum;

                        return cloudAnalysisVo;
                    }
                }

            }

            throw new Exception("数据不存在，查看失败，可尝试重新分析");

        }


        /// <summary>
        /// 将未完成录制的视频的录制状态改为录制完成
        /// </summary>
        public void UpdateVideoRecordStatus()
        {
            try
            {
                // 获取全部未完成录制的视频
                List<VideoEntity> videoList = VideoApi.ListByRecordStatus(1).Result;

                if (videoList != null && videoList.Count > 0)
                {

                    List<string> delVideoIds = new List<string>();
                    List<UpdateVideoSizeDurationBo> updateVideoSizeDurationBoList = new List<UpdateVideoSizeDurationBo>();

                    foreach (VideoEntity videoEntity in videoList)
                    {
                        try
                        {
                            if (videoEntity.isRecording == 1 && File.Exists(videoEntity.storagePath))
                            {
                                // 合并视频
                                //VideoUtils.MegerVideo(videoEntity.storagePath, videoEntity.videoId);

                                // 获取视频时长
                                int duration = VideoUtils.getVideoDuration(videoEntity.storagePath);

                                if (duration > 0)
                                {
                                    int sourceDuration = int.Parse(videoEntity.duration);
                                    if (duration - sourceDuration > 180)
                                    {
                                        // 如果读取时长减原记录时长大于180秒，说明读取出异常了，转成MP4后读取MP4时长
                                        string mp4Path = VideoUtils.ConvertToMP4(videoEntity.storagePath, videoEntity.videoName);
                                        duration = VideoUtils.getVideoDuration(mp4Path);
                                    }

                                    if (duration - sourceDuration > 180)
                                    {
                                        // 依旧大于180秒，用原记录时长
                                        duration = sourceDuration;
                                    }

                                    if (duration < 60)
                                    {
                                        // 删除视频信息
                                        delVideoIds.Add(videoEntity.videoId);
                                        File.Delete(videoEntity.storagePath);
                                        continue;
                                    }

                                    UpdateVideoSizeDurationBo updateVideoSizeDurationBo = new UpdateVideoSizeDurationBo();
                                    updateVideoSizeDurationBo.videoId = videoEntity.videoId;
                                    updateVideoSizeDurationBo.duration = duration;

                                    // 获取视频大小
                                    FileInfo fileInfo = new FileInfo(videoEntity.storagePath);
                                    updateVideoSizeDurationBo.fileSize = fileInfo.Length;

                                    // 计算录制结束时间
                                    DateTime originalDateTime = DateTime.ParseExact(videoEntity.startTime, "yyyy-MM-dd HH:mm:ss", null);
                                    DateTime newDateTime = originalDateTime.AddSeconds(duration);
                                    updateVideoSizeDurationBo.endTime = newDateTime.ToString("yyyy-MM-dd HH:mm:ss");

                                    updateVideoSizeDurationBo.isRecording = 0;

                                    updateVideoSizeDurationBoList.Add(updateVideoSizeDurationBo);
                                }
                                else
                                {
                                    FileUtils.LogError($"{videoEntity.storagePath}", $"无法解析视频时长");
                                }

                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"{JsonConvert.SerializeObject(videoEntity)}===={ex}", $"将视频的录制状态改为录制完成发生异常");
                        }
                    }

                    if (delVideoIds.Count > 0)
                    {
                        VideoApi.DelVideoByIds(delVideoIds);
                    }
                    if (updateVideoSizeDurationBoList.Count > 0)
                    {
                        VideoApi.UpdateVideoSizeDurationList(updateVideoSizeDurationBoList);
                    }
                }

            }
            catch (Exception e)
            {

                FileUtils.LogError($"{e}", $"将视频的录制状态改为录制完成发生异常");
            }
        }


        /// <summary>
        /// 文案提取改智能分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public void IntelligentAnalysis(string videoId)
        {
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if (videoEntity == null)
            {
                throw new Exception("视频不存在");
            }

            UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
            if (userPropertyEntity == null)
            {
                throw new Exception("智能分析时长余额不足");
            }

            int minute = (int)(Convert.ToDouble(videoEntity.duration) / 60) < 1 ? 1 : (int)(Convert.ToDouble(videoEntity.duration) / 60);
            if (userPropertyEntity.AiAnalysisTime >= minute)
            {
                // 清除本地旧数据
                this.DelVideoLocalData(videoEntity);

                // 设置视频的状态，使其重新加入到自动分析队列中
                videoEntity.useAnalysisPropertyType = 0;
                videoEntity.analysisStatus = 0;
                videoEntity.errorReason = "";
                VideoApi.SaveOrUpdateVideo(videoEntity);

            }
            else
            {
                throw new Exception("智能分析时长余额不足");
            }
        }

        /// <summary>
        /// 导出视频话术
        /// </summary>
        /// <param name="vo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public void exportVideoContent(ExportVideoContentVo vo)
        {

            System.Net.Http.HttpClient client = HttpUtils.getClient();

            string url = ReplayHttpUtils.BaseUrl + "/words/videoContent/exportVideoContent";

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", vo.sourceId);
            param.Add("sourceType", vo.sourceType);
            param.Add("type", vo.type);

            // 拼接请求参数
            string paramStr = "";
            if (param != null && param.Count > 0)
            {
                paramStr = string.Join("&", param.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value?.ToString())}"));
            }
            var urlBuilder = new UriBuilder(url)
            {
                Query = paramStr
            };

            // 构建请求头
            var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                    {
                        { "token", ReplayHttpUtils.Token }
                    }
            };

            using (HttpResponseMessage res = client.SendAsync(httpRequestMessage).Result)
            {
                if (res.IsSuccessStatusCode)
                {
                    // 获取 Content-Type 头信息 
                    string contentType = res.Content.Headers.ContentType?.MediaType;

                    if (contentType != "application/octet-stream")
                    {
                        string respnseBody = res.Content.ReadAsStringAsync().Result;
                        dynamic resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                        string msg = resultObj.msg == null ? "导出失败" : resultObj.msg.ToString();
                        throw new CustomException(msg, 3001);
                    }
                    else
                    {
                        string path = UploadUtils.uploadsFilePath;
                        // 确保目录存在
                        Directory.CreateDirectory(path);
                        string fileName = $"{vo.sourceId}.txt";

                        // 获取文件名称
                        IEnumerable<string> contentDispositionValues;
                        if (res.Content.Headers.TryGetValues("Content-Disposition", out contentDispositionValues))
                        {
                            foreach (string value in contentDispositionValues)
                            {
                                if (value.StartsWith("attachment"))
                                {
                                    int index = value.IndexOf("filename=", StringComparison.OrdinalIgnoreCase);
                                    if (index > -1)
                                    {
                                        fileName = value.Substring(index + 9).Trim('"');
                                        break;
                                    }
                                }
                            }
                        }
                        fileName = HttpUtility.UrlDecode(fileName);
                        path += $"\\{fileName}";
                        using (Stream stream = res.Content.ReadAsStreamAsync().Result)
                        using (FileStream fs = new FileStream(path, FileMode.Create, FileAccess.Write))
                        {
                            stream.CopyTo(fs);
                        }

                        FileUtils.openFile(path);
                    }
                }
                else
                {
                    // 记录非成功状态码的日志 
                    FileUtils.LogError($"请求失败，状态码: {res.StatusCode}", $"异步发送GET请求发生异常");
                }
            }

        }

        /// <summary>
        /// 异步导出视频话术
        /// </summary>
        /// <param name="vo">导出视频话术参数</param>
        /// <returns></returns>
        public async Task exportVideoContentAsync(ExportVideoContentVo vo)
        {
            System.Net.Http.HttpClient client = HttpUtils.getClient();

            string url = ReplayHttpUtils.BaseUrl + "/words/videoContent/exportVideoContent";

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceId", vo.sourceId);
            param.Add("sourceType", vo.sourceType);
            param.Add("type", vo.type);

            // 拼接请求参数
            string paramStr = "";
            if (param != null && param.Count > 0)
            {
                paramStr = string.Join("&", param.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value?.ToString())}"));
            }
            var urlBuilder = new UriBuilder(url)
            {
                Query = paramStr
            };

            // 构建请求头
            var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                    {
                        { "token", ReplayHttpUtils.Token }
                    }
            };

            using (HttpResponseMessage res = await client.SendAsync(httpRequestMessage).ConfigureAwait(false))
            {
                if (res.IsSuccessStatusCode)
                {
                    // 获取 Content-Type 头信息 
                    string contentType = res.Content.Headers.ContentType?.MediaType;

                    if (contentType != "application/octet-stream")
                    {
                        string respnseBody = await res.Content.ReadAsStringAsync().ConfigureAwait(false);
                        dynamic resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                        string msg = resultObj.msg == null ? "导出失败" : resultObj.msg.ToString();
                        throw new CustomException(msg, 3001);
                    }
                    else
                    {
                        string path = UploadUtils.uploadsFilePath;
                        // 确保目录存在
                        Directory.CreateDirectory(path);
                        string fileName = $"{vo.sourceId}.txt";

                        // 获取文件名称
                        IEnumerable<string> contentDispositionValues;
                        if (res.Content.Headers.TryGetValues("Content-Disposition", out contentDispositionValues))
                        {
                            foreach (string value in contentDispositionValues)
                            {
                                if (value.StartsWith("attachment"))
                                {
                                    int index = value.IndexOf("filename=", StringComparison.OrdinalIgnoreCase);
                                    if (index > -1)
                                    {
                                        fileName = value.Substring(index + 9).Trim('"');
                                        break;
                                    }
                                }
                            }
                        }
                        fileName = HttpUtility.UrlDecode(fileName);
                        path += $"\\{fileName}";
                        using (Stream stream = await res.Content.ReadAsStreamAsync().ConfigureAwait(false))
                        using (FileStream fs = new FileStream(path, FileMode.Create, FileAccess.Write))
                        {
                            await stream.CopyToAsync(fs).ConfigureAwait(false);
                        }

                        FileUtils.openFile(path);
                    }
                }
                else
                {
                    // 记录非成功状态码的日志 
                    FileUtils.LogError($"请求失败，状态码: {res.StatusCode}", $"异步发送GET请求发生异常");
                }
            }
        }

        /// <summary>
        /// 取消视频分析
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public void cancelVideoAnalysis(string videoId)
        {
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if(videoEntity == null)
            {
                throw new Exception("视频不存在");
            }

            if(videoEntity.analysisStatus == 1)
            {
                throw new Exception("当前视频已经在分析中，无法停止！");
            }
            else if (videoEntity.analysisStatus == 2 || videoEntity.analysisStatus == 3)
            {
                throw new Exception("当前视频已经分析完成，无法停止！");
            }

            VideoApi.UpdateVideoAnalysisStatusSync(videoEntity.videoId, 3, "手动停止");

        }

        /// <summary>
        /// 「加速」：录播识别进行中时，前端请求把该视频尚未识别的剩余分片切换到腾讯云。
        /// 有效期严格锁定在该 videoId 的 ASR 识别循环真正在跑的窗口内（AsrAccelerateSignal.TryRequest），
        /// 而非粗粒度的 isAnalysis 标志——避免「ASR 已跑完但 isAnalysis 仍为 true」窗口内登记僵尸信号。
        /// TryRequest 内部用 ConcurrentDictionary 判定，无跨线程可见性隐患。
        /// </summary>
        /// <param name="videoId">视频 id</param>
        /// <returns>true=该视频 ASR 正在识别、已登记加速；false=空 videoId / 该视频 ASR 未在识别中</returns>
        public bool RequestAccelerate(string videoId)
        {
            bool accepted = AsrAccelerateSignal.TryRequest(videoId);
            FileUtils.LogAnalysis(accepted
                ? $"[ASR] 加速请求已登记: videoId={videoId}"
                : $"[ASR] 加速请求被忽略（该视频 ASR 未在识别中）: videoId={videoId}");
            return accepted;
        }

        /// <summary>
        /// 根据视频id集合删除视频-仅删除视频
        /// <param name="ids">视频id集合</param>
        /// </summary>
        public void DeleteLocalVideoByIds(List<string> ids)
        {

            List< VideoEntity > videoEntities = new List< VideoEntity >(); 

            foreach (var videoId in ids)
            {
                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
                videoEntities.Add(videoEntity);
                if (File.Exists(videoEntity.storagePath))
                {
                    try
                    {
                        using (var fs = File.Open(videoEntity.storagePath, FileMode.Open, FileAccess.ReadWrite, FileShare.None))
                        {
                            // 尝试是否能打开
                        }
                    }
                    catch (IOException ex)
                    {
                        throw new Exception($"{videoEntity.videoName}文件正在被占用，删除失败，请明天再试");
                    }
                }
            }

            // 删除本地ts视频
            try
            {

                // 修改服务器的视频标识
                VideoApi.DeleteLocalVideoByIds(ids);

                foreach (var videoEntity in videoEntities)
                {
                    if(videoEntity == null)
                    {
                        continue;
                    }

                    // 删除本地ts文件
                    if(File.Exists(videoEntity.storagePath))
                    {
                        File.Delete(videoEntity.storagePath);
                    }

                    // 删除mp4文件
                    string videoPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
                    videoPath += "\\mp4\\";
                    videoPath += videoEntity.videoName + ".mp4";
                    if (File.Exists(videoPath))
                    {
                        File.Delete(videoPath);
                    }

                    // 删除音频文件
                    string audioDirectoryPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\"));
                    audioDirectoryPath += "\\audio\\";
                    audioDirectoryPath += videoEntity.videoName.Substring(0, videoEntity.videoName.LastIndexOf("."));
                    if (Directory.Exists(audioDirectoryPath))
                    {
                        Directory.Delete(audioDirectoryPath, true);
                    }
                }

            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"根据视频id集合删除视频-仅删除视频发生异常");
            }
        }

        /// <summary>
        /// 每天定时删除本地视频（服务器表驱动）。
        /// 外层每 24 小时一轮；内层按页拉取「本地未删、分析完成、源视频」列表逐条处理，
        /// 页间空 5 秒降低服务器压力。
        /// </summary>
        /// <summary>
        /// 删除循环防重入标记：0=未启动，1=已启动。整个进程生命周期只允许一个 24 小时删除循环，
        /// 避免 SetToken 多次登录/切换租户时累积多个 while(true) 并发循环。
        /// </summary>
        private static int _deleteAgoLocalVideoRunning = 0;

        /// <summary>
        /// 每天定时删除本地视频（服务器表驱动）。
        /// 外层每 24 小时一轮，每轮调用 <see cref="DeleteAgoLocalVideoOnce"/> 执行一次完整删除。
        /// </summary>
        public static async Task DeleteAgoLocalVideo()
        {
            // 防重入：仅第一个调用者进入循环，后续调用直接返回
            if (Interlocked.CompareExchange(ref _deleteAgoLocalVideoRunning, 1, 0) != 0)
            {
                return;
            }

            int delayTime = 24 * 60 * 60 * 1000;

            while (true)
            {
                await Task.Delay(delayTime);
                await DeleteAgoLocalVideoOnce();
            }
        }

        /// <summary>
        /// 执行一轮删除本地视频（不进入 24 小时循环）。
        /// 按页拉取「本地未删、分析完成、源视频」列表逐条处理，页间空 5 秒降低服务器压力。
        /// 供 <see cref="DeleteAgoLocalVideo"/> 每轮调用，也供测试接口手动触发一次。
        /// </summary>
        public static async Task DeleteAgoLocalVideoOnce()
        {
            try
            {
                FileUtils.log("开始删除本地视频", "自动删除本地视频");
                Config config = new ConfigBll().GetModel();

                int page = 1;
                int pageSize = 200;      // 每页条数，与分页结束判断保持一致
                int pageDelayMs = 5000;  // 每页完成后空 5 秒，避免服务器压力大（可调）
                int processedCount = 0;  // 累计已处理条数，用于按总条数判断分页结束
                while (true)
                {
                    // 拉取一页"本地未删源视频"
                    PageResult<VideoEntity> pageResult = await VideoApi.ListLocalSourceVideo(page, pageSize);
                    if (pageResult == null || pageResult.list == null || pageResult.list.Count == 0) break;

                    // 逐条删除本地文件。不回写服务器 local_video_status：
                    // 一旦回写，服务器 listLocalSourceVideo 不再返回该记录，mp4 成品会失去唯一清理入口而永久残留。
                    // 代价是源/mp4 均已删尽的记录每轮仍会被返回、重复检查（空转），可接受。
                    foreach (VideoEntity v in pageResult.list)
                    {
                        deletedLocalRecordVideo(v, config);
                    }

                    // 按总条数判断是否已取完全部页：totalCount 缺失(=0)时退回靠「下一页为空」终止，避免提前停或漏页
                    processedCount += pageResult.list.Count;
                    if (pageResult.totalCount > 0 && processedCount >= pageResult.totalCount) break;
                    page++;
                    await Task.Delay(pageDelayMs);  // 页间空一段时间，降低服务器压力
                }
                FileUtils.log("本轮删除本地视频结束", "自动删除本地视频");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"每天定时删除本地视频发生异常");
            }
        }

        /// <summary>
        /// 删除单条源视频记录对应的本地文件。
        /// 删除内容（ts 源视频 / mp4 成品 / all 都删）由主播级或全局 deleteContent 决定；
        /// 删除时机由主播级或全局 autoDeleteTime 决定（-1/空 跟随系统 / 0 不删 / N 分钟后删；系统未设置则不删）。
        /// 不回写服务器 local_video_status：回写会让该记录从 listLocalSourceVideo 消失，
        /// mp4 成品失去唯一清理入口而永久残留；源/mp4 均删尽后记录每轮空转，可接受。
        /// </summary>
        /// <param name="videoEntity">服务器返回的源视频记录</param>
        /// <param name="config">全局配置</param>
        private static void deletedLocalRecordVideo(VideoEntity videoEntity, Config config)
        {
            try
            {
                if (videoEntity == null || string.IsNullOrEmpty(videoEntity.storagePath)) return;

                // 1. 源视频 / mp4 成品存在性（独立判断：源视频可能已删，mp4 成品仍需独立清理）
                FileInfo sourceFile = new FileInfo(videoEntity.storagePath);
                bool sourceExists = sourceFile.Exists;

                // mp4 路径解析：storagePath 无 "\" 时 getVideoMp4Path 会抛异常，此时无法定位 mp4，按「无 mp4」处理（仅不删 mp4，不阻塞源视频删除）
                string mp4Path = null;
                bool mp4Exists = false;
                try
                {
                    mp4Path = AnalysisUtils.getVideoMp4Path(videoEntity.storagePath, videoEntity.videoName);
                    mp4Exists = mp4Path != null && File.Exists(mp4Path);
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"解析mp4路径发生异常=={videoEntity?.storagePath}");
                }

                // 源视频与 mp4 成品都不存在：被动不存在（换电脑/手动删），直接跳过（下轮仍会被返回，空转）
                if (!sourceExists && !mp4Exists) return;

                // 2. 查主播缓存，拿主播级配置（主播已被删除时忽略其设置，改用全局配置）
                AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(videoEntity.secUid);
                bool useAnchorConfig = anchor != null && anchor.IsRemoveRecord == 0;

                // 3. 解析有效配置（主播有效 → 全局 → 兜底）
                string deleteContent = useAnchorConfig && !string.IsNullOrEmpty(anchor.deleteContent)
                    ? anchor.deleteContent
                    : (!string.IsNullOrEmpty(config.deleteContent) ? config.deleteContent : "");
                // 解析自动删除时间（新枚举：-1/空 跟随系统，0 不删，N 分钟后删；系统未设置则不删）
                int? deleteMinutes = ResolveDeleteMinutes(
                    useAnchorConfig ? anchor.autoDeleteTime : null,
                    config.autoDeleteTime);

                // 大小写不敏感 + 去首尾空格：防止 "TS"/"All"/" ts " 等变体被漏匹配，导致既不删源也不删成品
                string deleteContentLower = deleteContent.Trim().ToLowerInvariant();
                bool deleteSource = deleteContentLower.Contains("ts") || "all".Equals(deleteContentLower);
                bool deleteMp4 = deleteContentLower.Contains("mp4") || "all".Equals(deleteContentLower);

                // 4. 删源视频：用源视频自身最后修改时间判断到期，与 mp4 独立（mp4 不存在也不阻塞删源视频）
                if (deleteSource && sourceExists && videoEntity.analysisStatus == 2
                    && ShouldDeleteByMinutes(deleteMinutes, sourceFile.LastWriteTime))
                {
                    ClearReadOnlyAttribute(videoEntity.storagePath);  // 清只读，避免删除失败后永久空转
                    try
                    {
                        File.Delete(videoEntity.storagePath);
                        FileUtils.log($"删除源视频成功=={videoEntity.storagePath}", "自动删除本地视频");
                    }
                    catch (Exception ex)
                    {
                        // 源视频删除失败（如被播放器占用）不影响 mp4 成品删除，下轮重试
                        FileUtils.LogError($"{ex}", $"删除源视频失败=={videoEntity?.storagePath}");
                    }
                }

                // 5. 删 mp4 成品：用 mp4 自身最后修改时间判断到期，与源视频独立（源视频已删也不影响清理 mp4）
                if (deleteMp4 && mp4Exists
                    && ShouldDeleteByMinutes(deleteMinutes, File.GetLastWriteTime(mp4Path)))
                {
                    ClearReadOnlyAttribute(mp4Path);  // 清只读，避免删除失败后永久空转
                    try
                    {
                        File.Delete(mp4Path);
                        FileUtils.log($"删除mp4成功=={mp4Path}", "自动删除本地视频");
                    }
                    catch (Exception ex)
                    {
                        // mp4 删除失败（如被播放器占用）不影响源视频删除，下轮重试
                        FileUtils.LogError($"{ex}", $"删除mp4失败=={mp4Path}");
                    }
                }

            }
            catch (Exception ex)
            {
                if (!ex.ToString().Contains("当前网络情况不佳"))
                    FileUtils.LogError($"{ex}", $"无法删除文件=={videoEntity?.storagePath}");
            }
        }

        /// <summary>
        /// 解析自动删除时间，返回「多少分钟后删除」；返回 null 表示不删除。
        /// 新枚举语义：-1 或空 = 跟随系统；0 = 不删除；正整数 N = N 分钟后删除。
        /// 主播级为 -1 / 空 / 非法时回落到系统级；系统级为 -1 / 空 / 0 / 非法时最终不删除。
        /// </summary>
        /// <param name="anchorAutoDeleteTime">主播级自动删除时间配置（主播不可用时传 null）</param>
        /// <param name="globalAutoDeleteTime">系统级自动删除时间配置</param>
        /// <returns>正整数表示 N 分钟后删除；null 表示不删除</returns>
        private static int? ResolveDeleteMinutes(string anchorAutoDeleteTime, string globalAutoDeleteTime)
        {
            // 主播级：0 显式不删，N&gt;0 显式删除；-1 / 空 / 非法 → 跟随系统
            int? anchorMinutes = ParseAutoDeleteTime(anchorAutoDeleteTime);
            if (anchorMinutes.HasValue)
            {
                return anchorMinutes.Value > 0 ? anchorMinutes.Value : (int?)null; // N 分钟后删 / 0 不删
            }

            // 系统级：0 不删，N&gt;0 删除；-1 / 空 / 非法 → 不删除（系统未设置）
            int? globalMinutes = ParseAutoDeleteTime(globalAutoDeleteTime);
            if (globalMinutes.HasValue && globalMinutes.Value > 0)
            {
                return globalMinutes.Value;
            }
            return null;
        }

        /// <summary>
        /// 解析单个 autoDeleteTime 字符串为「有效分钟数」：返回 0 或正整数；-1 / 空 / 非法返回 null（未设置）。
        /// </summary>
        /// <param name="autoDeleteTime">自动删除时间配置字符串，单位分钟</param>
        /// <returns>0 或正整数表示有效值；null 表示未设置（-1 / 空 / 非法）</returns>
        private static int? ParseAutoDeleteTime(string autoDeleteTime)
        {
            if (string.IsNullOrEmpty(autoDeleteTime)) return null;      // 空 = 未设置
            int minutes;
            if (!int.TryParse(autoDeleteTime.Trim(), out minutes)) return null; // 非法 = 未设置
            if (minutes < 0) return null;                               // 负数（-1）= 未设置
            return minutes;                                             // 0 或正整数
        }

        /// <summary>
        /// 根据「删除分钟数」与文件最后修改时间判断是否应删除（纯函数，便于单测）。
        /// </summary>
        /// <param name="deleteMinutes">多少分钟后删除；null 表示不删除</param>
        /// <param name="lastWriteTime">文件最后修改时间</param>
        /// <returns>true 表示应删除，false 表示不删除</returns>
        private static bool ShouldDeleteByMinutes(int? deleteMinutes, DateTime lastWriteTime)
        {
            if (!deleteMinutes.HasValue || deleteMinutes.Value <= 0) return false; // null / 0 / 负数不删
            return DateTime.Now - lastWriteTime > TimeSpan.FromMinutes(deleteMinutes.Value);
        }

        /// <summary>
        /// 清除文件只读属性（只清 ReadOnly 位，保留隐藏/系统等其他属性），
        /// 避免 File.Delete 对只读文件抛 UnauthorizedAccessException 导致删除失败后永久空转。
        /// </summary>
        /// <param name="path">文件路径</param>
        private static void ClearReadOnlyAttribute(string path)
        {
            if (string.IsNullOrEmpty(path)) return;
            try
            {
                FileAttributes attr = File.GetAttributes(path);
                if ((attr & FileAttributes.ReadOnly) == FileAttributes.ReadOnly)
                {
                    File.SetAttributes(path, attr & ~FileAttributes.ReadOnly);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"清除只读属性失败=={path}");
            }
        }

        /// <summary>
        /// 新增视频切片
        /// </summary>
        /// <param name="addVideoSliceBo">新增视频切片数据</param>
        /// <returns></returns>
        public void addVideoSlice(AddVideoSliceBo addVideoSliceBo)
        {
            lock (_lockObject)
            {
                if (sliceVideoing)
                {
                    throw new Exception("请等待上一个视频切片完成再继续");
                }
                sliceVideoing = true;
            }

            try
            {

                if(string.IsNullOrEmpty(addVideoSliceBo.videoName))
                {
                    throw new Exception("切片视频名称不能为空");
                }
                addVideoSliceBo.videoName = WindowsUtils.SanitizeForFolderName(addVideoSliceBo.videoName);

                if (addVideoSliceBo.endTimeMs - addVideoSliceBo.startTimeMs < 1000)
                {
                    throw new Exception("切片视频时长不能低于1秒");
                }
                // 获取原视频信息
                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(addVideoSliceBo.videoId);
                if (videoEntity == null)
                {
                    throw new Exception("视频信息不存在");
                }
                if (!File.Exists(videoEntity.storagePath))
                {
                    throw new Exception("视频文件不存在");
                }

                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(videoEntity.secUid);
                if (anchorInfo == null)
                {
                    throw new Exception("主播信息不存在");
                }

                // 设置切片保存地址
                if (addVideoSliceBo.savePathType == 0)
                {
                    addVideoSliceBo.savePath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.LastIndexOf("\\")) + "\\";
                }
                else
                {
                    addVideoSliceBo.savePath = addVideoSliceBo.savePath + "\\" + anchorInfo.FolderName + "\\";
                }

                addVideoSliceBo.videoName = addVideoSliceBo.videoName + ".mp4";
                string sliceVideoPath = addVideoSliceBo.savePath + addVideoSliceBo.videoName;
                if (File.Exists(sliceVideoPath))
                {
                    throw new Exception("切片视频已存在，请检查路径和视频名称");
                }

                // 取出本地分析段落文字
                AnalysisResultTxtVo analysisResultTxtVo = AnalysisUtils.getVideoLocalAnalysisData(videoEntity);
                if (analysisResultTxtVo == null || analysisResultTxtVo.audioaAlyses == null || analysisResultTxtVo.audioaAlyses.Count < 1)
                {
                    throw new Exception("当前视频分析数据丢失，请尝试重新分析");
                }

                // 异步做剩下任务
                Task.Run(async () =>
                {
                    VideoEntity sliceVideo = null;
                    try
                    {
                        // 将原视频转成MP4
                        VideoUtils.ConvertToMP4(videoEntity.storagePath, videoEntity.videoName, int.Parse(videoEntity.platformType));
                        string mp4Path = AnalysisUtils.getVideoMp4Path(videoEntity.storagePath, videoEntity.videoName);

                        // 保存切片视频到服务器
                        sliceVideo = await saveSliceVideoToServer(videoEntity, addVideoSliceBo, sliceVideoPath);

                        // 切割视频
                        long durationMs = addVideoSliceBo.endTimeMs - addVideoSliceBo.startTimeMs;
                        VideoUtils.SliceVideo(mp4Path, sliceVideoPath, addVideoSliceBo.startTimeMs, durationMs);

                        // 设置视频大小到服务器
                        FileInfo fileInfo = new FileInfo(sliceVideo.storagePath);
                        sliceVideo.vedioSizie = fileInfo.Length.ToString();
                        await VideoApi.SaveOrUpdateVideoAsync(sliceVideo);

                        // 封装段落词语
                        List<WordsMarkParagraphBo> wordsMarkParagraphBos = packageParagraphWord(addVideoSliceBo, analysisResultTxtVo, sliceVideo);

                        // 敏感词/关键词识别
                        AnalysisResultVo analysisResultVo = await WordApi.SliceWordsMark(wordsMarkParagraphBos);

                        // 将分析结果存到本地文件
                        AnalysisUtils.saveVideoLocalAnalysisData(analysisResultVo, sliceVideo.tradeId, sliceVideo);
                        FileUtils.LogAnalysis($"{sliceVideo?.videoName}", $"整理切片视频分析数据成功");

                        // 将视频分析状态改成分析完成
                        FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(sliceVideo)}", $"切片视频分析成功");
                        await VideoApi.UpdateVideoAnalysisStatusSuccess(sliceVideo.videoId, 2, 1);

                        // 从服务器拷贝一份websocket、数据看板等关联数据
                        SaveSliceCorrelationDataBo saveSliceCorrelationDataBo = new SaveSliceCorrelationDataBo()
                        {
                            sourceVideoId = videoEntity.videoId,
                            sliceVideoId = sliceVideo.videoId,
                            sliceStartNaturalTime = TimeUtils.getMillisecondByDateTimeStr(sliceVideo.startTime),
                            sliceEndNaturalTime = TimeUtils.getMillisecondByDateTimeStr(sliceVideo.endTime),
                        };
                        await VideoSliceApi.SaveSliceCorrelationData(saveSliceCorrelationDataBo);

                        // 判断是否需要上传云空间
                        if (addVideoSliceBo.isAutoUploadCloud == 1)
                        {
                            autoUploadSliceVideoToCloud(sliceVideo);
                        }

                        // 转成MP4
                        VideoUtils.ConvertToMP4(sliceVideo.storagePath, sliceVideo.videoName, int.Parse(sliceVideo.platformType));

                        // 通知前端
                        try
                        {
                            FrontNotice frontNotice = new FrontNotice();
                            var requestDataObj = new Dictionary<string, object>();
                            requestDataObj["code"] = 0;
                            requestDataObj["status"] = 200;
                            requestDataObj["action"] = "sliceSuccess";
                            Dictionary<string, object> data = new Dictionary<string, object>();
                            data.Add("sliceName", sliceVideo.videoName);
                            data.Add("videoId", sliceVideo.videoId);
                            data.Add("sliceType", addVideoSliceBo.sliceType);
                            requestDataObj["data"] = data;
                            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        }
                        catch (Exception e)
                        {
                            FileUtils.LogError($"{e}", $"通知前端短视频切割完成发生异常");
                        }
                        sliceVideoing = false;
                    }
                    catch (Exception ex)
                    {
                        sliceVideoing = false;
                        FileUtils.LogAnalysis($"{ex}", $"切片视频任务发生异常==={JsonConvert.SerializeObject(addVideoSliceBo)}");
                        if (sliceVideo != null)
                        {
                            // 从服务器删除视频
                            await VideoApi.DelVideoById(sliceVideo.videoId);
                            DelVideoLocalData(sliceVideo);
                        }
                        if (File.Exists(sliceVideoPath))
                        {
                            File.Delete(sliceVideoPath);
                        }
                    }
                });
            }
            catch (Exception e)
            {
                sliceVideoing = false;
                FileUtils.LogAnalysis($"{e}", $"切片视频任务异常==={JsonConvert.SerializeObject(addVideoSliceBo)}");
                throw new Exception(e.Message);
            }
            

        }

        /// <summary>
        /// 切片视频自动上传云空间
        /// </summary>
        /// <param name="video">切片视频信息</param>
        private async void autoUploadSliceVideoToCloud(VideoEntity video)
        {
            try
            {
                VideoUtils.ConvertToMP4(video.storagePath, video.videoName, int.Parse(video.platformType));

                // 判断云空间资源是否足够
                string videoPath = video.storagePath.Substring(0, video.storagePath.LastIndexOf("\\"));
                videoPath += "\\mp4\\";
                videoPath += video.videoName + ".mp4";

                if (File.Exists(videoPath))
                {
                    // 文件存在
                    FileInfo fileInfo = new FileInfo(videoPath);
                    long fileSize = fileInfo.Length / 1024; // 文件大小，KB

                    UserPropertyEntity userProperty = await UserPropertyApi.GetPropertyInfo();
                    FileUtils.LogAnalysis($"{JsonConvert.SerializeObject(userProperty)}", $"检测自动上传-资产");
                    if (userProperty != null && userProperty.StorageNum >= fileSize)
                    {
                        // 自动上传
                        new Thread(() => AutoUploadCloud(video)).Start();
                    }
                    else
                    {
                        // 资源不足
                        new Thread(() => NoticeCloudPropertyInsufficient()).Start();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"{ex}", $"切片视频自动上传云空间发生异常==={video.videoName}");
            }
        }

        /// <summary>
        /// 封装视频切片的段落词语
        /// </summary>
        /// <param name="addVideoSliceBo">切片信息</param>
        /// <param name="analysisResultTxtVo">原视频段落词语信息</param>
        /// <param name="videoEntity">切片视频信息</param>
        /// <returns></returns>
        private List<WordsMarkParagraphBo> packageParagraphWord(AddVideoSliceBo addVideoSliceBo, AnalysisResultTxtVo analysisResultTxtVo, VideoEntity videoEntity)
        {
            try
            {
                // 取出符合的词语
                List<WordListItemVo> allWordList = new List<WordListItemVo>();
                foreach (var audioaAlysis in analysisResultTxtVo.audioaAlyses)
                {
                    SentenceMarkVo sentenceMarkVo = JsonConvert.DeserializeObject<SentenceMarkVo>(audioaAlysis.DataJson);
                    List<WordListItemVo> wordItems = sentenceMarkVo.Items;
                    if (wordItems != null && wordItems.Count > 0)
                    {
                        // 不在切片时间范围内的段落直接跳过
                        if (wordItems[0].StartTime > addVideoSliceBo.endTimeMs || wordItems[wordItems.Count - 1].EndTime < addVideoSliceBo.startTimeMs)
                        {
                            continue;
                        }

                        foreach (var wordItem in wordItems)
                        {
                            // 获取在切片时间范围内的词语
                            if (wordItem.StartTime >= addVideoSliceBo.startTimeMs && wordItem.EndTime <= addVideoSliceBo.endTimeMs)
                            {
                                allWordList.Add(wordItem);
                            }
                        }
                    }
                }

                // 计算一共有多少段
                long sliceVideoDuration = addVideoSliceBo.endTimeMs - addVideoSliceBo.startTimeMs;
                int paragraphCount = (int)Math.Ceiling(sliceVideoDuration / 59000.0);
                // 计算切片起始时间戳在所属段落的开始时间戳
                long paragraphStartTimeRemainderMs = addVideoSliceBo.startTimeMs % 59000;

                List<WordsMarkParagraphBo> wordsMarkParagraphBos = new List<WordsMarkParagraphBo>();
                for (int i = 0; i < paragraphCount; i++)
                {
                    // 整理段落数据
                    WordsMarkParagraphBo wordsMarkParagraphBo = new WordsMarkParagraphBo();
                    wordsMarkParagraphBo.type = 0;
                    wordsMarkParagraphBo.videoId = videoEntity.videoId;
                    wordsMarkParagraphBo.tradeId = videoEntity.tradeId;
                    wordsMarkParagraphBo.platformType = int.Parse(videoEntity.platformType);
                    wordsMarkParagraphBo.currentSort = i + 1;
                    wordsMarkParagraphBo.isLast = paragraphCount == (i + 1) ? 1 : 0;

                    string content = "";
                    List<WordListItemBo> paragraphWordList = new List<WordListItemBo>();
                    long paragraphStartTimeMs = i * 59000;
                    long paragraphEndTimeMs = paragraphStartTimeMs + 59000;
                    foreach (var wordItem in allWordList)
                    {
                        // 将符合段落的词语添加进列表
                        if (wordItem.StartTime - addVideoSliceBo.startTimeMs >= paragraphStartTimeMs && wordItem.EndTime - addVideoSliceBo.startTimeMs <= paragraphEndTimeMs)
                        {
                            WordListItemBo wordListItemBo = new WordListItemBo();
                            // 段落词语时间 = 原词语时间 - 切片开始时间 - 段落起始时间
                            wordListItemBo.StartTime = wordItem.StartTime - addVideoSliceBo.startTimeMs - paragraphStartTimeMs;
                            wordListItemBo.EndTime = wordItem.EndTime - addVideoSliceBo.startTimeMs - paragraphStartTimeMs;
                            wordListItemBo.Word = wordItem.Word;
                            paragraphWordList.Add(wordListItemBo);
                            content += wordListItemBo.Word;
                        }
                    }

                    if (paragraphWordList.Count < 1)
                    {
                        WordListItemBo wordListItemBo = new WordListItemBo();
                        wordListItemBo.StartTime = 0;
                        wordListItemBo.EndTime = 20;
                        wordListItemBo.Word = "-";
                        paragraphWordList.Add(wordListItemBo);
                        content += "-";
                    }

                    wordsMarkParagraphBo.content = content;
                    wordsMarkParagraphBo.items = paragraphWordList;
                    wordsMarkParagraphBos.Add(wordsMarkParagraphBo);
                    
                }
                return wordsMarkParagraphBos;
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"封装视频切片的段落词语发生异常==={JsonConvert.SerializeObject(addVideoSliceBo)}");
            }
            return null;
            
        }

        /// <summary>
        /// 保存切片视频到服务器
        /// </summary>
        /// <param name="sourceVideo">原视频信息</param>
        /// <param name="addVideoSliceBo">切片视频信息</param>
        /// <param name="savePath">切片视频保存路径</param>
        private async Task<VideoEntity> saveSliceVideoToServer(VideoEntity sourceVideo, AddVideoSliceBo addVideoSliceBo, string savePath)
        {
            // 视频信息
            VideoEntity videoEntity = new VideoEntity();

            long durationMs = addVideoSliceBo.endTimeMs - addVideoSliceBo.startTimeMs;
            videoEntity.duration = (durationMs / 1000) + "";
            long sliceStartTimeMs = TimeUtils.getMillisecondByDateTimeStr(sourceVideo.startTime) + addVideoSliceBo.startTimeMs;
            videoEntity.startTime = TimeUtils.getDateTimeStrByMillisecond(sliceStartTimeMs);
            long sliceEndTimeMs = sliceStartTimeMs + durationMs;
            videoEntity.endTime = TimeUtils.getDateTimeStrByMillisecond(sliceEndTimeMs);

            string sliceVideoFullPath = savePath;
            //FileInfo fileInfo = new FileInfo(sliceVideoFullPath);
            //long fileSize = fileInfo.Length;
            //videoEntity.vedioSizie = fileSize.ToString();
            videoEntity.vedioSizie = "0";
            videoEntity.videoName = addVideoSliceBo.videoName;
            videoEntity.videoRename = addVideoSliceBo.videoName;
            videoEntity.anchorId = sourceVideo.anchorId;
            videoEntity.batchNumber = sourceVideo.batchNumber;
            videoEntity.storagePath = sliceVideoFullPath;
            videoEntity.paragraph = sourceVideo.paragraph;
            videoEntity.sourceUrl = sourceVideo.sourceUrl;
            videoEntity.definition = sourceVideo.definition;
            videoEntity.sourceType = sourceVideo.sourceType;
            videoEntity.subsectionType = sourceVideo.subsectionType;
            videoEntity.videoType = sourceVideo.videoType;
            videoEntity.liveTitle = sourceVideo.liveTitle;
            videoEntity.isRecording = sourceVideo.isRecording;
            videoEntity.secUid = sourceVideo.secUid;
            videoEntity.tradeId = sourceVideo.tradeId;
            videoEntity.analysisStatus = 5;
            videoEntity.platformType = sourceVideo.platformType;
            videoEntity.videoId = Guid.NewGuid().ToString();
            videoEntity.userId = sourceVideo.userId;
            videoEntity.tenantId = sourceVideo.tenantId;
            videoEntity.videoSliceType = addVideoSliceBo.sliceType + 1;

            // 视频切片信息
            VideoSliceEntity videoSliceEntity = new VideoSliceEntity();
            videoSliceEntity.userId = videoEntity.userId;
            videoSliceEntity.tenantId = videoEntity.tenantId;
            videoSliceEntity.sourceId = videoEntity.videoId;
            videoSliceEntity.sourceType = 0;
            videoSliceEntity.sourceParentId = sourceVideo.videoId;
            videoSliceEntity.sliceType = addVideoSliceBo.sliceType;
            videoSliceEntity.sliceClass = addVideoSliceBo.sliceClass;
            videoSliceEntity.startMillisecond = addVideoSliceBo.startTimeMs;
            videoSliceEntity.startTime = TimeUtils.millisecondsFormat(addVideoSliceBo.startTimeMs);
            videoSliceEntity.endMillisecond = addVideoSliceBo.endTimeMs;
            videoSliceEntity.endTime = TimeUtils.millisecondsFormat(addVideoSliceBo.endTimeMs);
            videoSliceEntity.remarks = addVideoSliceBo.remarks;
            videoSliceEntity.isAutoUploadCloud = addVideoSliceBo.isAutoUploadCloud;
            videoSliceEntity.savePathType = addVideoSliceBo.savePathType;
            videoSliceEntity.savePath = videoEntity.storagePath;
            videoSliceEntity.sliceVideoName = addVideoSliceBo.videoName;
            videoSliceEntity.sliceTimeType = addVideoSliceBo.sliceTimeType;

            // 保存到服务器
            await VideoApi.SaveOrUpdateVideoAsync(videoEntity);
            await VideoSliceApi.SaveVideoSliceAsync(videoSliceEntity);

            return videoEntity;
        }

        /// <summary>
        /// 弹幕数据导出
        /// </summary>
        /// <param name="bo"></param>
        /// <exception cref="NotImplementedException"></exception>
        public void queryDanMuExport(dynamic bo)
        {
            if (bo is string)
            {
                bo = JsonConvert.DeserializeObject<dynamic>(bo);
            }

            string fileName = bo?.fileName ?? null;
            if (string.IsNullOrEmpty(fileName))
            {
                throw new CustomException("文件名称不能为空");
            }

            // 调用接口 replay/openapi/v2100/queryDanMuExport, post， 参数为：bo，下载弹幕数据的导出文件到savnPath处
            string url = ReplayHttpUtils.BaseUrl + "/openapi/v2100/queryDanMuExport";

            System.Net.Http.HttpClient client = HttpUtils.getClient();
            // 构建请求头

            var httpRequestMessage = new HttpRequestMessage(HttpMethod.Post, url)
            {
                Headers = { { "token", ReplayHttpUtils.Token } },
                Content = new StringContent(JsonConvert.SerializeObject(bo), Encoding.UTF8, "application/json")
            };
            SignatureHeaders.AddSignature(httpRequestMessage);

            using (HttpResponseMessage res = client.SendAsync(httpRequestMessage).Result)
            {

                if (res.IsSuccessStatusCode)
                {
                    // 获取 Content-Type 头信息
                    string contentType = res.Content.Headers.ContentType?.MediaType;

                    if (contentType != "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    {
                        string respnseBody = res.Content.ReadAsStringAsync().Result;
                        dynamic resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                        string msg = resultObj.msg == null ? "导出失败" : resultObj.msg.ToString();
                        throw new CustomException(msg, 3001);
                    }
                    else
                    {
                        string path = UploadUtils.uploadsFilePath;
                        // 确保目录存在
                        Directory.CreateDirectory(path);

                        // 获取文件名称
                        IEnumerable<string> contentDispositionValues;
                        if (res.Content.Headers.TryGetValues("Content-Disposition", out contentDispositionValues))
                        {
                            foreach (string value in contentDispositionValues)
                            {
                                if (value.StartsWith("attachment"))
                                {
                                    int index = value.IndexOf("filename=", StringComparison.OrdinalIgnoreCase);
                                    if (index > -1)
                                    {
                                        fileName = value.Substring(index + 9).Trim('"');
                                        break;
                                    }
                                }
                            }
                        }
                        fileName = HttpUtility.UrlDecode(fileName);

                        // 在设置文件名前调用 getCurrentFileName 方法，避免文件覆盖
                        string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(fileName);
                        string suffixName = Path.GetExtension(fileName);
                        fileName = UploadUtils.getCurrentFileName(fileNameWithoutExtension, suffixName);

                        path += $"\\{fileName}";
                        using (Stream stream = res.Content.ReadAsStreamAsync().Result)
                        using (FileStream fs = new FileStream(path, FileMode.Create, FileAccess.Write))
                        {
                            stream.CopyTo(fs);
                        }

                        // 下载完成后
                        FileUtils.openFile(path);
                    }
                }
                else
                {
                    // 记录非成功状态码的日志
                    FileUtils.LogError($"请求失败，状态码: {res.StatusCode}", $"弹幕数据导出请求发生异常");
                    throw new CustomException($"导出失败，状态码: {res.StatusCode}", 3001);
                }
            }
        }

        /// <summary>
        /// 修改视频名称
        /// </summary>
        /// <param name="videoId">视频Id</param>
        /// <param name="newVideoName">新视频名称（不含扩展名）</param>
        public async Task RenameVideoAsync(string videoId, string newVideoName)
        {
            if (string.IsNullOrEmpty(videoId))
            {
                throw new Exception("视频Id不能为空");
            }

            if (string.IsNullOrEmpty(newVideoName))
            {
                throw new Exception("新视频名称不能为空");
            }

            // 1. 通过videoId获取视频信息
            VideoEntity video = VideoApi.GetVideoByVideoIdSync(videoId);
            if (video == null)
            {
                throw new Exception("视频信息不存在");
            }

            if (File.Exists(video.storagePath))
            {
                try
                {
                    using (var fs = File.Open(video.storagePath, FileMode.Open, FileAccess.ReadWrite, FileShare.None))
                    {
                        // 尝试是否能打开
                    }
                }
                catch (IOException ex)
                {
                    throw new Exception($"{video.videoName}文件正在被占用，删除失败，请明天再试");
                }
            }

            string oldStoragePath = video.storagePath;
            string oldVideoName = video.videoName;

            // 记录需要回滚的文件路径映射 (新路径 -> 旧路径)
            Dictionary<string, string> renamedFiles = new Dictionary<string, string>();

            try
            {
                // 2. 获取文件目录和扩展名
                string directory = Path.GetDirectoryName(oldStoragePath);
                string oldExtension = Path.GetExtension(oldStoragePath);

                // 检查并重命名视频文件
                string newVideoPath = Path.Combine(directory, newVideoName + oldExtension);
                if (File.Exists(oldStoragePath))
                {
                    File.Move(oldStoragePath, newVideoPath);
                    renamedFiles.Add(newVideoPath, oldStoragePath);
                }

                // 检查并重命名mp4文件
                string oldMp4Path = Path.Combine(directory, "mp4", oldVideoName + ".mp4");
                string newMp4Path = Path.Combine(directory, "mp4", newVideoName + oldExtension + ".mp4");
                if (File.Exists(oldMp4Path))
                {
                    File.Move(oldMp4Path, newMp4Path);
                    renamedFiles.Add(newMp4Path, oldMp4Path);
                }

                // 更新视频实体信息
                video.storagePath = newVideoPath;
                video.videoName = newVideoName + oldExtension;
                video.videoRename = newVideoName + oldExtension;

                // 3. 异步同步到服务器
                await VideoApi.SaveOrUpdateVideoAsync(video).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                // 4. 同步服务器失败，回滚本地文件名
                foreach (var kvp in renamedFiles)
                {
                    try
                    {
                        if (File.Exists(kvp.Key))
                        {
                            File.Move(kvp.Key, kvp.Value);
                        }
                    }
                    catch
                    {
                        // 回滚失败时忽略，避免影响异常抛出
                        FileUtils.LogError($"{ex}", $"同步服务器失败，回滚本地视频文件名发生异常==={kvp.Key}==={kvp.Value}");
                    }
                }

                throw new Exception("修改视频名称失败：" + ex.Message);
            }
        }

        public DataDiagnosisConfigVo updateDataDiagnosisConfig(DataDiagnosisConfigVo bo)
        {
            if (bo == null || bo.basicSettingsVo == null || bo.sourceId == null || bo.sourceType == null)
            {
                throw new CustomException("数据格式不正确");
            }
            if ((bo?.sourceType ?? 0) == 1)
            {
                return null;
            }
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(bo.sourceId);
            if (videoEntity == null)
            {
                throw new CustomException("视频不存在或已被删除");
            }
            if (bo.tradeId != null && !bo.tradeId.Equals(videoEntity.tradeId))
            {
                // 更换换行
                ReAnalysisByTrade(bo.sourceId, bo.tradeId, videoEntity.platformType);
            }

            // 调用接口
            DataDiagnosisConfigVo res = VideoApi.updateDataDiagnosisConfig(bo);
            if (res?.basicSettingsVo == null)
            {
                throw new CustomException("配置更新失败，请稍后重试");
            }

            // 修改客户端的缓存
            AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(videoEntity.secUid);
            if (anchor != null && ReplayHttpUtils.UserId.Equals(videoEntity.userId) && ReplayHttpUtils.ActiveTenantId.Equals(videoEntity.tenantId))
            {
                anchor.SecUid = videoEntity.secUid;
                anchor.AccountType = res.basicSettingsVo.accountType ?? anchor.AccountType;
                anchor.premiereDate = res.basicSettingsVo.premiereDate ?? anchor.premiereDate;
                anchor.accountStage = res.basicSettingsVo.accountStage ?? anchor.accountStage;
                anchor.accountWaterLevel = res.basicSettingsVo.accountWaterLevel ?? anchor.accountWaterLevel;
                anchor.accountFlow = res.basicSettingsVo.accountFlow ?? anchor.accountFlow;
                anchor.livingTarget = res.basicSettingsVo.livingTarget ?? anchor.livingTarget;
                anchor.livingModality = res.basicSettingsVo.livingModality ?? anchor.livingModality;
                anchor.marketing = res.basicSettingsVo.marketing ?? anchor.marketing;
                anchor.optimizeDirection = res.basicSettingsVo.optimizeDirection ?? anchor.optimizeDirection;
                anchor.learning = res.basicSettingsVo.learning ?? anchor.learning;
                anchor.livingMode = res.basicSettingsVo.livingMode ?? anchor.livingMode;
                anchor.AnchorSituation = res.basicSettingsVo.anchorSituation ?? anchor.AnchorSituation;

                // 添加到缓存
                AnchorCacheManager.SetAnchorCache(anchor);
            }
            
            return res;
        }

        /// <summary>
        /// 获取视频的播放地址
        /// </summary>
        /// <param name="videoId">视频id</param>
        public string getVideoPayUrl(string videoId)
        {
            // 查询视频信息
            VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
            if(videoEntity == null)
            {
                throw new Exception("视频信息不存在");
            }
            // 获取主播信息
            AnchorEntity anchorEntity = null;
            if (!string.IsNullOrEmpty(videoEntity.userId) && videoEntity.userId.Equals(ReplayHttpUtils.UserId))
            {
                anchorEntity = AnchorApi.GetCurrUserAnchorBySecUidSync(videoEntity.secUid);
            }
            else
            {
                anchorEntity = AnchorApi.GetAnchorBySecUidSync(videoEntity.secUid);
            }
            if (anchorEntity == null)
            {
                throw new Exception("主播信息不存在");
            }

            // 转成MP4
            if (File.Exists(videoEntity.storagePath))
            {
                VideoUtils.ConvertToMP4(videoEntity.storagePath, videoEntity.videoName);
            }

            // 获取视频播放地址
            try
            {
                // 获取mp4地址
                string tsVideoPath = videoEntity.storagePath;
                string mp4VideoPath = tsVideoPath.Substring(0, tsVideoPath.LastIndexOf("\\"));
                mp4VideoPath += $"\\mp4\\{videoEntity.videoName}.mp4";
                if (File.Exists(mp4VideoPath) && anchorEntity != null)
                {
                    try
                    {
                        // 截取主播名称
                        MatchCollection matches = Regex.Matches(tsVideoPath, @"\\");
                        int startPosition = matches[matches.Count - 3].Index + 1;
                        int endPosition = matches[matches.Count - 2].Index;
                        string anchorName = tsVideoPath.Substring(startPosition, endPosition - startPosition);
                        // 获取mp4播放地址
                        string serverPath = tsVideoPath.Substring(0, tsVideoPath.IndexOf(anchorName));
                        HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                        string port = httpResourceFileServer.GetPort(serverPath);

                        string videoPath = "http://localhost:" + port + "/" + mp4VideoPath.Substring(mp4VideoPath.IndexOf(anchorName));
                        return videoPath;
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", "使用存储路径获取播放路径失败，换方式");

                        string serverPath = videoEntity.storagePath.Substring(0, videoEntity.storagePath.IndexOf(!string.IsNullOrEmpty(anchorEntity.folderName) ? anchorEntity.folderName : anchorEntity.anchorInfo.anchorName));
                        HttpResourceFileServer httpResourceFileServer = new HttpResourceFileServer();
                        string port = httpResourceFileServer.GetPort(serverPath);

                        string videoPath = "http://localhost:" + port + "/" + mp4VideoPath.Substring(mp4VideoPath.IndexOf(!string.IsNullOrEmpty(anchorEntity.folderName) ? anchorEntity.folderName : anchorEntity.anchorInfo.anchorName));
                        return videoPath;
                    }

                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"获取视频播放地址发生异常====={JsonConvert.SerializeObject(videoEntity)}================={JsonConvert.SerializeObject(anchorEntity)}");
            }

            throw new Exception("视频不存在");
        }
    }

}
