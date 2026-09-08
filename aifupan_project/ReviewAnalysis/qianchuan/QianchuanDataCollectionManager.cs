using douyin.Utils;
using ReviewAnalysis.Model;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Concurrent;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.qianchuan
{
    public class QianchuanDataCollectionManager
    {
        private static ConcurrentDictionary<string, QianchuanDataPoller> _pollers = new ConcurrentDictionary<string, QianchuanDataPoller>();
        private static Timer _detectionTimer;
        private static bool _isInitialized;

        public static void Initialize()
        {
            if (_isInitialized) return;
            _isInitialized = true;

            FileUtils.LogRpa("千川数据采集管理器初始化", "千川数据采集");
        }

        public static void StartDetection()
        {
            FileUtils.LogRpa("千川StartDetection被调用", "千川数据采集");
            if (_detectionTimer != null)
            {
                FileUtils.LogRpa("千川检测定时器已存在，跳过", "千川数据采集");
                return;
            }

            FileUtils.LogRpa("千川数据采集管理器启动检测", "千川数据采集");
            _detectionTimer = new Timer(DetectionTimerCallback, null, 60000, 60000);
        }

        public static void StopDetection()
        {
            if (_detectionTimer != null)
            {
                _detectionTimer.Dispose();
                _detectionTimer = null;
            }

            foreach (var poller in _pollers.Values)
            {
                poller.Stop();
            }
            _pollers.Clear();

            FileUtils.LogRpa("千川数据采集已停止", "千川数据采集");
        }

        private static void DetectionTimerCallback(object state)
        {
            FileUtils.LogRpa("千川定时器触发了", "千川数据采集");
            Task.Run(() =>
            {
                try
                {
                    FileUtils.LogRpa("千川开始执行CheckAllAnchorsAsync", "千川数据采集");
                    CheckAllAnchorsAsync().Wait();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"千川主播检测异常: {ex.Message}", "千川数据采集");
                }
            });
        }

        private static async Task CheckAllAnchorsAsync()
        {
            FileUtils.LogRpa("千川CheckAllAnchorsAsync被调用", "千川数据采集");
            if (!AnchorBll.IsDetection)
            {
                FileUtils.LogRpa("千川检测被跳过，因为AnchorBll.IsDetection=false", "千川数据采集");
                return;
            }

            FileUtils.LogRpa("开始检查所有主播的千川Cookie和直播状态", "千川数据采集");

            await CheckAndUpdateAllCookies();

            await CheckAndStartPollingIfLive();
        }

        private static async Task CheckAndUpdateAllCookies()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            FileUtils.LogRpa($"千川检查Cookie，共{anchors.Count}个主播", "千川数据采集");

            foreach (var anchor in anchors)
            {
                if (anchor == null) continue;

                FileUtils.LogRpa($"千川检查主播{anchor.AnchorName}，platform={anchor.platform}，qianchuanAuthStatus={anchor.qianchuanAuthStatus}", "千川数据采集");

                if (anchor.platform != 0)
                {
                    FileUtils.LogRpa($"主播{anchor.AnchorName}被跳过（platform={anchor.platform}）", "千川数据采集");
                    continue;
                }

                bool cookieValid = QianchuanUtils.CheckCookieValid(anchor.SecUid);
                FileUtils.LogRpa($"主播{anchor.AnchorName}千川Cookie有效性: {cookieValid}", "千川数据采集");

                if (cookieValid)
                {
                    if (anchor.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.authExpires ||
                        anchor.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.unAuth)
                    {
                        AnchorBll.UpdateQianchuanAuthStatus(anchor, (int)QianchuanAuthStatusEnum.auth);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}千川Cookie有效，状态更新为已授权", "千川数据采集");
                    }
                    else
                    {
                        FileUtils.LogRpa($"主播{anchor.AnchorName}千川Cookie有效，状态已是auth，无需更新", "千川数据采集");
                    }
                }
                else
                {
                    if (anchor.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.auth)
                    {
                        AnchorBll.UpdateQianchuanAuthStatus(anchor, (int)QianchuanAuthStatusEnum.authExpires);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}千川Cookie无效/过期，状态更新为授权过期", "千川数据采集");
                    }
                    else
                    {
                        FileUtils.LogRpa($"主播{anchor.AnchorName}千川Cookie无效，状态已是authExpires/unAuth，无需更新", "千川数据采集");
                    }
                }
            }
        }

        private static async Task CheckAndStartPollingIfLive()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null) continue;

                if (anchor.platform != 0) continue;

                if (anchor.qianchuanAuthStatus != (int)QianchuanAuthStatusEnum.auth)
                {
                    StopPolling(anchor.SecUid);
                    continue;
                }

                bool isPolling = _pollers.ContainsKey(anchor.SecUid);
                bool isLive = await CheckAnchorIsLive(anchor);

                if (isLive && !isPolling)
                {
                    string roomId = await GetLiveRoomId(anchor);
                    string aavid = GetAavidFromLocal(anchor.SecUid);
                    if (!string.IsNullOrEmpty(roomId) && !string.IsNullOrEmpty(aavid))
                    {
                        StartPolling(anchor, roomId, aavid);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}正在直播，开启千川数据轮询", "千川数据采集");
                    }
                    else
                    {
                        if (string.IsNullOrEmpty(roomId))
                            FileUtils.LogRpa($"主播{anchor.AnchorName}无法开启轮询：roomId为空", "千川数据采集");
                        if (string.IsNullOrEmpty(aavid))
                            FileUtils.LogRpa($"主播{anchor.AnchorName}无法开启轮询：aavid为空", "千川数据采集");
                    }
                }
                else if (!isLive && isPolling)
                {
                    StopPolling(anchor.SecUid);
                    FileUtils.LogRpa($"主播{anchor.AnchorName}直播结束，停止千川数据轮询", "千川数据采集");
                }
            }
        }

        public static void StartPolling(AnchorInfo anchorInfo, string roomId, string aavid)
        {
            if (_pollers.ContainsKey(anchorInfo.SecUid)) return;

            // 从 anchorInfo 获取 anchorId（抖音用户ID）
            string anchorId = anchorInfo.SecUid;
            
            var poller = new QianchuanDataPoller(anchorInfo, roomId, aavid, anchorId);
            if (_pollers.TryAdd(anchorInfo.SecUid, poller))
            {
                poller.Start();
            }
        }

        public static void StopPolling(string secUid)
        {
            if (_pollers.TryRemove(secUid, out var poller))
            {
                poller.Stop();
            }
        }

        private static async Task<string> GetLiveRoomId(AnchorInfo anchorInfo)
        {
            try
            {
                FileUtils.LogRpa($"主播{anchorInfo.AnchorName}开始获取千川roomId", "千川数据采集");

                // 方案1：优先使用抖音主播后台 API 获取 current_live_room_id
                var douyinCookies = DouyinUtils.GetCookiesFromLocal(anchorInfo.SecUid);
                if (douyinCookies.Count > 0)
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}尝试从抖音主播后台API获取roomId", "千川数据采集");
                    var accountDetail = await QianchuanDataApi.GetDouyinAccountDetail(douyinCookies);
                    if (accountDetail != null && accountDetail.ContainsKey("current_live_room_id"))
                    {
                        var roomId = accountDetail["current_live_room_id"];
                        if (!string.IsNullOrEmpty(roomId) && roomId != "0")
                        {
                            FileUtils.LogRpa($"主播{anchorInfo.AnchorName}从抖音主播后台获取到roomId: {roomId}", "千川数据采集");
                            return roomId;
                        }
                    }
                }

                // 方案2：备用，使用 BatchNumber（直播批次编号）作为 roomId
                if (!string.IsNullOrEmpty(anchorInfo.BatchNumber))
                {
                    FileUtils.LogRpa($"主播{anchorInfo.AnchorName}使用BatchNumber作为roomId: {anchorInfo.BatchNumber}", "千川数据采集");
                    return anchorInfo.BatchNumber;
                }

                FileUtils.LogRpa($"主播{anchorInfo.AnchorName}无法获取roomId（抖音API无数据且BatchNumber为空）", "千川数据采集");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"主播{anchorInfo.AnchorName}获取roomId异常: {ex.Message}", "千川数据采集");
                return null;
            }
        }

        private static string GetAavidFromLocal(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = QianchuanUtils.getCachePathMd5(secUid);
                string aavidPath = Path.Combine(cachePath, $"qcaavid-{md5Str}");

                if (File.Exists(aavidPath))
                {
                    string aavid = File.ReadAllText(aavidPath).Trim();
                    FileUtils.LogRpa($"主播{secUid}从本地读取aavid: {aavid}", "千川数据采集");
                    return aavid;
                }
                FileUtils.LogRpa($"主播{secUid}本地无aavid文件: {aavidPath}", "千川数据采集");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"读取aavid异常: {ex.Message}", "千川数据采集");
                return null;
            }
        }

        private static async Task<bool> CheckAnchorIsLive(AnchorInfo anchorInfo)
        {
            try
            {
                if (anchorInfo.qianchuanAuthStatus != (int)QianchuanAuthStatusEnum.auth)
                    return false;

                var roomId = await GetLiveRoomId(anchorInfo);
                return !string.IsNullOrEmpty(roomId);
            }
            catch
            {
                return false;
            }
        }
    }
}
