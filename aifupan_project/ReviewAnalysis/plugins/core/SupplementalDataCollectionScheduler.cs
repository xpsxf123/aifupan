using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using ReviewAnalysis.api;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using douyin.Utils;

namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 主播下播后平台数据补采集调度器。
    /// 登录成功后由 Form1 调用 Start() 启动；登出/切号时 Dispose() 关闭。
    /// 执行模型：Timer 单次触发（dueTime=间隔, period=Infinite），回调串行跑完一轮后再排下一轮 ——
    /// 天然串行、无重入，符合「登录后等一个间隔才跑首轮，一轮全部完成后才等下一个间隔」的语义。
    /// </summary>
    public class SupplementalDataCollectionScheduler : IDisposable
    {
        // KV 配置项 key
        private const string KV_SWITCH = "supplement_collect_switch";
        private const string KV_INTERVAL_HOURS = "supplement_collect_interval_hours";
        private const string KV_WINDOW_DAYS = "supplement_collect_window_days";

        // 默认值（KV 缺失或非法时兜底）
        private const int DEFAULT_INTERVAL_HOURS = 4;
        private const int DEFAULT_WINDOW_DAYS = 7;

        private Timer _timer;
        private readonly object _lock = new object();
        private readonly CollectionScheduler _collectionScheduler;
        private bool _disposed;
        private bool _running;

        /// <summary>
        /// 构造补采集调度器。
        /// </summary>
        /// <param name="collectionScheduler">直播中采集调度器，用于判断主播是否仍在直播（可空，为空时跳过该过滤）</param>
        public SupplementalDataCollectionScheduler(CollectionScheduler collectionScheduler)
        {
            _collectionScheduler = collectionScheduler;
        }

        /// <summary>
        /// 启动调度器（登录成功后调用）。
        /// 读 KV 总开关，关闭则直接返回；开启则在一个间隔（默认 4 小时）之后执行首轮。
        /// </summary>
        public void Start()
        {
            lock (_lock)
            {
                if (_disposed || _timer != null)
                {
                    return;
                }

                // 总开关关闭则不启动
                if (KvHelper.GetIntKvByKey(KV_SWITCH, 1) == 0)
                {
                    FileUtils.LogRpa("补采集总开关已关闭，调度器不启动", "SupplementalDataCollectionScheduler");
                    return;
                }

                int intervalHours = KvHelper.GetIntKvByKey(KV_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS);
                if (intervalHours <= 0)
                {
                    intervalHours = DEFAULT_INTERVAL_HOURS;
                }

                _timer = new Timer(OnTick, null, TimeSpan.FromHours(intervalHours), Timeout.InfiniteTimeSpan);
                FileUtils.LogRpa($"补采集调度器已启动，首次执行在 {intervalHours} 小时后", "SupplementalDataCollectionScheduler");
            }
        }

        /// <summary>
        /// 定时器回调：触发一轮补采集。
        /// 用 Task.Run 跑异步轮次，轮次结束（finally）后再排下一轮（串行 + 完成后延迟）。
        /// </summary>
        private void OnTick(object state)
        {
            lock (_lock)
            {
                if (_disposed || _running)
                {
                    return;
                }
                _running = true;
            }

            _ = Task.Run(async () =>
            {
                try
                {
                    await RunRoundAsync();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"补采集轮次异常: {ex.Message}", "SupplementalDataCollectionScheduler");
                }
                finally
                {
                    lock (_lock)
                    {
                        _running = false;
                        if (!_disposed && _timer != null)
                        {
                            int intervalHours = KvHelper.GetIntKvByKey(KV_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS);
                            if (intervalHours <= 0)
                            {
                                intervalHours = DEFAULT_INTERVAL_HOURS;
                            }
                            _timer.Change(TimeSpan.FromHours(intervalHours), Timeout.InfiniteTimeSpan);
                        }
                    }
                }
            });
        }

        /// <summary>
        /// 执行一轮补采集：读 KV → 拉取窗口内场次 → 逐场过滤 → 逐场补采 → 记录最近尝试时间。
        /// </summary>
        private async Task RunRoundAsync()
        {
            // 1. 总开关关闭则本轮跳过
            if (KvHelper.GetIntKvByKey(KV_SWITCH, 1) == 0)
            {
                FileUtils.LogRpa("补采集总开关已关闭，本轮跳过", "SupplementalDataCollectionScheduler");
                return;
            }

            // 2. 读间隔与窗口（KV 缺失或非法时兜底默认值）
            int intervalHours = KvHelper.GetIntKvByKey(KV_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS);
            int windowDays = KvHelper.GetIntKvByKey(KV_WINDOW_DAYS, DEFAULT_WINDOW_DAYS);
            if (intervalHours <= 0)
            {
                intervalHours = DEFAULT_INTERVAL_HOURS;
            }
            if (windowDays <= 0)
            {
                windowDays = DEFAULT_WINDOW_DAYS;
            }

            // 3. 拉取最近 windowDays 天内开播的场次（后端已按 batchNumber 分组去重）
            List<VideoEntity> sessions = await VideoApi.ListRecentLiveSessions(windowDays);
            if (sessions == null || sessions.Count == 0)
            {
                FileUtils.LogRpa($"本轮无需要补采集的场次，窗口={windowDays}天", "SupplementalDataCollectionScheduler");
                return;
            }

            DateTime now = DateTime.Now;
            int processed = 0;
            int skipped = 0;

            foreach (VideoEntity session in sessions)
            {
                if (session == null)
                {
                    continue;
                }
                string batchNumber = session.batchNumber;
                string secUid = session.secUid;

                try
                {
                    // 过滤 0：场次号或主播标识缺失
                    if (string.IsNullOrEmpty(batchNumber) || string.IsNullOrEmpty(secUid))
                    {
                        skipped++;
                        continue;
                    }
                    // 过滤 1：仅抖音平台（platformType == "1"）
                    if (session.platformType != "1")
                    {
                        skipped++;
                        continue;
                    }
                    // 过滤 2：正在直播
                    if (session.isRecording == 1)
                    {
                        skipped++;
                        continue;
                    }
                    // 过滤 3：本地采集调度器中仍在采集
                    IEnumerable<string> activeSecUids = _collectionScheduler?.GetActiveAnchorSecUids();
                    if (activeSecUids != null && activeSecUids.Contains(secUid))
                    {
                        skipped++;
                        continue;
                    }
                    // 过滤 4：主播无授权（巨量/千川/来客任一授权即可补采）
                    AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                    if (anchorInfo == null)
                    {
                        skipped++;
                        continue;
                    }
                    if (anchorInfo.juliangAuthStatus != 1 && anchorInfo.qianchuanAuthStatus != 1 && anchorInfo.lifeAuthStatus != 1)
                    {
                        skipped++;
                        continue;
                    }
                    // 过滤 5：去重（本间隔内已尝试过则跳过）
                    DateTime? lastAttempt = SupplementCollectRecordStore.GetLastAttemptTime(batchNumber);
                    if (lastAttempt.HasValue && (now - lastAttempt.Value).TotalHours < intervalHours)
                    {
                        skipped++;
                        continue;
                    }

                    // 执行补采集（复用已落地的双端上传 CollectAndUploadAsync）
                    try
                    {
                        await AnchorInstance.CollectAndUploadAsync(anchorInfo, batchNumber, session.videoId);
                    }
                    finally
                    {
                        // 尝试后即记录最近尝试时间（无论成败，失败靠下一轮重试）
                        SupplementCollectRecordStore.UpdateAttemptTime(batchNumber, now, windowDays);
                    }
                    processed++;
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"补采场次异常: {ex.Message}", "SupplementalDataCollectionScheduler");
                }
            }

            FileUtils.LogRpa($"补采集一轮完成，处理={processed}，跳过={skipped}", "SupplementalDataCollectionScheduler");
        }

        /// <summary>
        /// 手动触发一轮补采集（供测试接口调用，无需等待定时器首轮或间隔）。
        /// 与定时器共用 _running 串行锁：已有轮次执行中则直接跳过，保证不重入。
        /// </summary>
        public async Task TriggerRoundAsync()
        {
            lock (_lock)
            {
                if (_disposed)
                {
                    return;
                }
                if (_running)
                {
                    FileUtils.LogRpa("补采集已有一轮正在执行，手动触发被跳过", "SupplementalDataCollectionScheduler");
                    return;
                }
                _running = true;
            }

            try
            {
                await RunRoundAsync();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"手动补采集轮次异常: {ex.Message}", "SupplementalDataCollectionScheduler");
            }
            finally
            {
                lock (_lock)
                {
                    _running = false;
                }
            }
        }

        /// <summary>
        /// 释放调度器：取消定时器。登出/切号时调用，释放后旧定时器不再触发。
        /// </summary>
        public void Dispose()
        {
            lock (_lock)
            {
                if (_disposed)
                {
                    return;
                }
                _disposed = true;
                if (_timer != null)
                {
                    _timer.Dispose();
                    _timer = null;
                }
            }
            FileUtils.LogRpa("补采集调度器已释放", "SupplementalDataCollectionScheduler");
        }
    }
}
