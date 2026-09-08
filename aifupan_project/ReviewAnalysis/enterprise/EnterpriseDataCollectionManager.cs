using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
using ReviewAnalysis.DataCache;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号数据采集管理器
    /// </summary>
    public class EnterpriseDataCollectionManager
    {
        private static ConcurrentDictionary<string, EnterpriseDataPoller> _pollers = new ConcurrentDictionary<string, EnterpriseDataPoller>();
        private static ConcurrentDictionary<string, string> _roomIdCache = new ConcurrentDictionary<string, string>();
        private static Timer _detectionTimer;
        private static bool _isInitialized;

        /// <summary>
        /// 初始化
        /// </summary>
        public static void Initialize()
        {
            if (_isInitialized) return;
            _isInitialized = true;

            FileUtils.LogRpa("企业号数据采集管理器初始化", "企业号数据采集");
        }

        /// <summary>
        /// 启动检测
        /// </summary>
        public static void StartDetection()
        {
            if (_detectionTimer != null) return;

            _detectionTimer = new Timer(DetectionTimerCallback, null, 30000, 30000);
            FileUtils.LogRpa("企业号主播直播状态检测已启动", "企业号数据采集");
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

            foreach (var poller in _pollers)
            {
                poller.Value.Stop();
            }
            _pollers.Clear();
            _roomIdCache.Clear();

            FileUtils.LogRpa("企业号数据采集已停止", "企业号数据采集");
        }

        private static void DetectionTimerCallback(object state)
        {
            Task.Run(() =>
            {
                try
                {
                    CheckAllAnchorsAsync().Wait();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"企业号主播检测异常: {ex.Message}", "企业号数据采集");
                }
            });
        }

        private static async Task CheckAllAnchorsAsync()
        {
            await CheckAndUpdateAllCookies();

            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null) continue;

                // 企业号属于抖音平台，检查平台类型
                // platform == 0 表示抖音

                if (anchor.enterpriseAuthStatus != (int)EnterpriseAuthStatusEnum.auth)
                {
                    StopPolling(anchor.SecUid);
                    continue;
                }

                // videoId 由检测定时器自动启动，传空值
                bool isPolling = _pollers.ContainsKey(anchor.SecUid);
                if (isPolling)
                {
                    // 已在轮询，检测直播是否结束
                    bool isLive = await CheckAnchorIsLive(anchor);
                    if (!isLive)
                    {
                        StopPolling(anchor.SecUid);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}直播结束，停止企业号数据轮询", "企业号数据采集");
                    }
                }
                else
                {
                    // 未轮询，检测抖音直播状态获取roomId
                    string roomId = await DouYinAnchorBll.GetLiveRoomIdIfOnline(anchor);
                    if (!string.IsNullOrEmpty(roomId))
                    {
                        StartPolling(anchor, roomId, "");
                        FileUtils.LogRpa($"检测到直播，自动启动企业号数据轮询，主播：{anchor.AnchorName}，roomId={roomId}", "企业号数据采集");
                    }
                }
            }
        }

        /// <summary>
        /// 启动轮询
        /// </summary>
        public static void StartPolling(AnchorInfo anchorInfo, string roomId, string videoId = "")
        {
            if (_pollers.ContainsKey(anchorInfo.SecUid)) return;

            _roomIdCache[anchorInfo.SecUid] = roomId;
            var poller = new EnterpriseDataPoller(anchorInfo) { RoomId = roomId, VideoId = videoId };
            _pollers[anchorInfo.SecUid] = poller;
            poller.Start();
        }

        /// <summary>
        /// 停止轮询
        /// </summary>
        public static void StopPolling(string secUid)
        {
            if (_pollers.TryRemove(secUid, out var poller))
            {
                poller.Stop();
            }
            _roomIdCache.TryRemove(secUid, out _);
        }

        private static async Task<bool> CheckAnchorIsLive(AnchorInfo anchor)
        {
            // 企业号API需要手动传入roomId，无法自动检测直播状态
            // 这里简化处理，检查Cookie是否有效
            try
            {
                var cookies = EnterpriseDataHandle.GetCookiesFromLocal(anchor.SecUid);
                return cookies.Count > 0 && cookies.ContainsKey("sessionid");
            }
            catch
            {
                return false;
            }
        }

        private static async Task<string> GetLiveRoomId(AnchorInfo anchor)
        {
            // 企业号API需要手动传入roomId
            // 这里返回anchor的BatchNumber作为roomId
            return anchor?.BatchNumber;
        }

        private static async Task CheckAndUpdateAllCookies()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null) continue;

                int cookieStatus = EnterpriseUtils.CheckAuthStatusByCookie(anchor.SecUid);
                bool cookieValid = (cookieStatus == (int)EnterpriseAuthStatusEnum.auth);

                if (cookieValid)
                {
                    if (anchor.enterpriseAuthStatus == (int)EnterpriseAuthStatusEnum.authExpires ||
                        anchor.enterpriseAuthStatus == (int)EnterpriseAuthStatusEnum.unAuth)
                    {
                        anchor.enterpriseAuthStatus = (int)EnterpriseAuthStatusEnum.auth;
                        AnchorCacheManager.SetAnchorCache(anchor);
                        SyncAuthStatusToServer(anchor, EnterpriseAuthStatusEnum.auth);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}企业号Cookie有效，状态更新为已授权", "企业号数据采集");
                    }
                }
                else
                {
                    if (anchor.enterpriseAuthStatus == (int)EnterpriseAuthStatusEnum.auth)
                    {
                        int newStatus = (cookieStatus == (int)EnterpriseAuthStatusEnum.authExpires)
                            ? (int)EnterpriseAuthStatusEnum.authExpires
                            : (int)EnterpriseAuthStatusEnum.authExpires;
                        anchor.enterpriseAuthStatus = newStatus;
                        AnchorCacheManager.SetAnchorCache(anchor);
                        SyncAuthStatusToServer(anchor, (EnterpriseAuthStatusEnum)newStatus);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}企业号Cookie无效/过期，状态更新为授权过期", "企业号数据采集");
                    }
                }
            }
        }

        private static void SyncAuthStatusToServer(AnchorInfo anchorInfo, EnterpriseAuthStatusEnum status)
        {
            try
            {
                var updateDto = new UpdateAnchorUserDto
                {
                    secUid = anchorInfo.SecUid,
                    authEnterpriseStatus = (int)status
                };
                AnchorApi.UpdateAnchorUserInfo(updateDto);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"同步企业号授权状态到服务端异常: {ex.Message}", "企业号数据采集");
            }
        }

        private static bool CheckCookieValid(string cookiePath)
        {
            try
            {
                if (!File.Exists(cookiePath))
                    return false;

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<EnterpriseCookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                    return false;

                var sessionCookie = cookies.FirstOrDefault(c => c.name == "sessionid");
                return sessionCookie != null && !string.IsNullOrEmpty(sessionCookie.value);
            }
            catch
            {
                return false;
            }
        }
    }
}
