using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台轮询管理器 - 管理多个主播的轮询
    /// </summary>
    public static class AnchorLiveDataCollectionManager
    {
        private static ConcurrentDictionary<string, AnchorLiveDataPoller> _pollers = new ConcurrentDictionary<string, AnchorLiveDataPoller>();
        private static Timer _detectionTimer;
        private static bool _isInitialized;

        /// <summary>
        /// 初始化
        /// </summary>
        public static void Initialize()
        {
            if (_isInitialized) return;
            _isInitialized = true;

            FileUtils.LogRpa("主播后台数据采集管理器初始化", "主播后台管理");
        }

        /// <summary>
        /// 启动直播状态检测（每分钟检测一次）
        /// </summary>
        public static void StartDetection()
        {
            if (_detectionTimer != null) return;

            _detectionTimer = new Timer(DetectionTimerCallback, null, 30000, 30000);
            FileUtils.LogRpa("主播后台直播状态检测已启动", "主播后台管理");
        }

        /// <summary>
        /// 停止检测
        /// </summary>
        public static void StopDetection()
        {
            if (_detectionTimer != null)
            {
                _detectionTimer.Dispose();
                _detectionTimer = null;
            }

            // 停止所有轮询
            foreach (var poller in _pollers)
            {
                poller.Value.Stop();
            }
            _pollers.Clear();

            FileUtils.LogRpa("主播后台数据采集已停止", "主播后台管理");
        }

        /// <summary>
        /// 定时器回调
        /// </summary>
        private static void DetectionTimerCallback(object state)
        {
            Task.Run(async () =>
            {
                try
                {
                    await CheckAllAnchorsAsync();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"主播后台检测异常：{ex.Message}", "主播后台管理");
                }
            });
        }

        /// <summary>
        /// 检查所有主播状态
        /// </summary>
        private static async Task CheckAllAnchorsAsync()
        {
            await CheckAndUpdateAllCookies();

            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            
            foreach (var anchor in anchors)
            {
                if (anchor == null) continue;

                // 2. 检查直播状态并启停轮询
                await CheckAndStartPollingIfLive(anchor);
            }
        }

        /// <summary>
        /// 检查并更新所有主播的Cookie授权状态
        /// </summary>
        private static async Task CheckAndUpdateAllCookies()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null) continue;

                string cookiePath = AnchorLiveUtils.getCookiePath(anchor.SecUid);
                bool cookieValid = CheckCookieValid(cookiePath);

                if (cookieValid)
                {
                    if (anchor.anchorLiveAuthStatus != (int)AnchorLiveAuthStatusEnum.auth)
                    {
                        anchor.anchorLiveAuthStatus = (int)AnchorLiveAuthStatusEnum.auth;
                        AnchorCacheManager.SetAnchorCache(anchor);
                        SyncAuthStatusToServer(anchor, AnchorLiveAuthStatusEnum.auth);
                        FileUtils.LogRpa($"主播【{anchor.AnchorName}】主播后台 Cookie 有效，状态更新为已授权", "主播后台管理");
                    }
                }
                else
                {
                    if (anchor.anchorLiveAuthStatus == (int)AnchorLiveAuthStatusEnum.auth)
                    {
                        anchor.anchorLiveAuthStatus = (int)AnchorLiveAuthStatusEnum.authExpires;
                        AnchorCacheManager.SetAnchorCache(anchor);
                        SyncAuthStatusToServer(anchor, AnchorLiveAuthStatusEnum.authExpires);
                        FileUtils.LogRpa($"主播【{anchor.AnchorName}】主播后台 Cookie 无效/过期，状态更新为授权过期", "主播后台管理");
                    }
                }
            }
        }

        /// <summary>
        /// 检查主播是否正在直播，如果是则启动轮询
        /// </summary>
        private static async Task CheckAndStartPollingIfLive(AnchorInfo anchor)
        {
            // 检查授权状态
            if (anchor.anchorLiveAuthStatus != (int)AnchorLiveAuthStatusEnum.auth)
            {
                StopPolling(anchor.SecUid);
                return;
            }

            // 正在轮询时，检查Cookie有效性
            bool isPolling = _pollers.ContainsKey(anchor.SecUid);
            if (isPolling)
            {
                var cookies = AnchorLiveDataHandle.GetCookiesFromLocal(anchor.SecUid);
                bool cookieValid = cookies.Count > 0 && cookies.ContainsKey("sessionid");
                if (!cookieValid)
                {
                    StopPolling(anchor.SecUid);
                    FileUtils.LogRpa($"主播【{anchor.AnchorName}】Cookie失效，停止主播后台数据轮询", "主播后台管理");
                }
            }
            else
            {
                string roomId = await DouYinAnchorBll.GetLiveRoomIdIfOnline(anchor);
                if (!string.IsNullOrEmpty(roomId))
                {
                    StartPolling(anchor, roomId, "");
                    FileUtils.LogRpa($"检测到直播，自动启动主播后台数据轮询，主播：{anchor.AnchorName}，roomId={roomId}", "主播后台管理");
                }
            }

        }

        /// <summary>
        /// 启动轮询
        /// </summary>
        public static void StartPolling(AnchorInfo anchorInfo, string roomId, string videoId = "", int intervalSeconds = 30)
        {
            if (string.IsNullOrEmpty(anchorInfo?.SecUid))
            {
                FileUtils.LogRpa($"错误：主播信息无效", "主播后台管理");
                return;
            }

            // 如果已存在，先停止旧的
            StopPolling(anchorInfo.SecUid);

            // 创建新的轮询器
            var poller = new AnchorLiveDataPoller(anchorInfo, intervalSeconds);
            poller.RoomId = roomId;
            poller.VideoId = videoId;

            _pollers[anchorInfo.SecUid] = poller;
            poller.Start();

            FileUtils.LogRpa($"已为【{anchorInfo.AnchorName}】启动主播后台轮询（每{intervalSeconds}秒，videoId={videoId}）", "主播后台管理");
        }

        /// <summary>
        /// 停止轮询
        /// </summary>
        public static void StopPolling(string secUid)
        {
            if (_pollers.TryRemove(secUid, out var poller))
            {
                poller.Stop();
                FileUtils.LogRpa($"[管理器] ⏹️ 已停止【{secUid.Substring(0, Math.Min(20, secUid.Length))}...】的轮询", "主播后台管理");
            }
        }

        /// <summary>
        /// 停止所有轮询
        /// </summary>
        public static void StopAllPolling()
        {
            FileUtils.LogRpa($"[管理器] ⏹️ 正在停止所有轮询...", "主播后台管理");

            foreach (var kvp in _pollers)
            {
                kvp.Value.Stop();
            }

            _pollers.Clear();
            FileUtils.LogRpa($"[管理器] ✅ 所有轮询已停止", "主播后台管理");
        }

        /// <summary>
        /// 检查是否正在轮询
        /// </summary>
        public static bool IsPolling(string secUid)
        {
            return _pollers.ContainsKey(secUid);
        }

        /// <summary>
        /// 获取活跃的轮询器数量
        /// </summary>
        public static int ActivePollersCount => _pollers.Count;

        #region 辅助方法

        /// <summary>
        /// 检查 Cookie 是否有效
        /// </summary>
        private static bool CheckCookieValid(string cookiePath)
        {
            try
            {
                if (!File.Exists(cookiePath))
                    return false;

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = Newtonsoft.Json.JsonConvert.DeserializeObject<List<AnchorLiveCookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                    return false;

                var sessionCookie = cookies.FirstOrDefault(c =>
                    c.Name.Equals("sessionid", StringComparison.OrdinalIgnoreCase));

                if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
                    return false;

                if (sessionCookie.Expires.HasValue && sessionCookie.Expires.Value < DateTime.Now)
                    return false;

                return true;
            }
            catch
            {
                return false;
            }
        }



        // 同步授权状态到服务端
        private static void SyncAuthStatusToServer(AnchorInfo anchorInfo, AnchorLiveAuthStatusEnum status)
        {
            try
            {
                var updateDto = new UpdateAnchorUserDto
                {
                    secUid = anchorInfo.SecUid,
                    authAnchorLiveStatus = (int)status
                };
                AnchorApi.UpdateAnchorUserInfo(updateDto);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"同步主播后台授权状态到服务端异常：{ex.Message}", "主播后台管理");
            }
        }

        #endregion
    }
}
