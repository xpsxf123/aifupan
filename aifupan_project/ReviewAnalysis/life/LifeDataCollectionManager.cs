using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.life
{
    public class LifeDataCollectionManager
    {
        private static ConcurrentDictionary<string, LifeDataPoller> _pollers = new ConcurrentDictionary<string, LifeDataPoller>();
        private static ConcurrentDictionary<string, string> _roomIdCache = new ConcurrentDictionary<string, string>();
        private static Timer _detectionTimer;
        private static bool _isInitialized;

        public static void Initialize()
        {
            if (_isInitialized) return;
            _isInitialized = true;

            FileUtils.LogRpa("来客数据采集管理器初始化", "来客数据采集");
        }

        public static void StartDetection()
        {
            if (_detectionTimer != null) return;

            _detectionTimer = new Timer(DetectionTimerCallback, null, 30000, 30000);
            FileUtils.LogRpa("来客主播直播状态检测已启动", "来客数据采集");
        }

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

            FileUtils.LogRpa("来客数据采集已停止", "来客数据采集");
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
                    FileUtils.LogRpa($"来客主播检测异常: {ex.Message}", "来客数据采集");
                }
            });
        }

        private static async Task CheckAllAnchorsAsync()
        {
            await CheckAndUpdateAllCookies();

            await CheckAndStartPollingIfLive();
        }

        private static async Task CheckAndUpdateAllCookies()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null || anchor.platform != 0) continue;

                string cookiePath = LifeUtils.getCookiePath(anchor.SecUid);
                bool cookieValid = CheckCookieValid(cookiePath);

                if (cookieValid)
                {
                    if (anchor.lifeAuthStatus != (int)LifeAuthStatusEnum.auth)
                    {
                        AnchorBll.UpdateLifeAuthStatus(anchor, (int)LifeAuthStatusEnum.auth);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}来客Cookie有效，状态更新为已授权", "来客数据采集");
                    }
                }
                else
                {
                    if (anchor.lifeAuthStatus == (int)LifeAuthStatusEnum.auth)
                    {
                        AnchorBll.UpdateLifeAuthStatus(anchor, (int)LifeAuthStatusEnum.authExpires);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}来客Cookie无效/过期，状态更新为授权过期", "来客数据采集");
                    }
                }
            }
        }

        private static async Task CheckAndStartPollingIfLive()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null || anchor.platform != 0) continue;

                if (anchor.lifeAuthStatus != (int)LifeAuthStatusEnum.auth)
                {
                    StopPolling(anchor.SecUid);
                    continue;
                }

                bool isPolling = _pollers.ContainsKey(anchor.SecUid);
                if (isPolling)
                {
                    // 已在轮询，检测直播是否结束
                    bool isLive = await CheckAnchorIsLive(anchor);
                    if (!isLive)
                    {
                        StopPolling(anchor.SecUid);
                        FileUtils.LogRpa($"主播{anchor.AnchorName}直播结束，停止来客数据轮询", "来客数据采集");
                    }
                }
                else
                {
                    // 未轮询，检测是否正在直播
                    string roomId = await GetLiveRoomId(anchor);
                    if (!string.IsNullOrEmpty(roomId))
                    {
                        StartPolling(anchor, roomId, "");
                        FileUtils.LogRpa($"检测到直播，自动启动来客数据轮询，主播：{anchor.AnchorName}，roomId={roomId}", "来客数据采集");
                    }
                }
            }
        }

        public static void StartPolling(AnchorInfo anchorInfo, string roomId, string videoId = "")
        {
            if (_pollers.ContainsKey(anchorInfo.SecUid)) return;

            _roomIdCache[anchorInfo.SecUid] = roomId;
            var poller = new LifeDataPoller(anchorInfo) { RoomId = roomId, VideoId = videoId };
            _pollers[anchorInfo.SecUid] = poller;
            poller.Start();
        }

        public static void StopPolling(string secUid)
        {
            if (_pollers.TryRemove(secUid, out var poller))
            {
                poller.Stop();
            }
            _roomIdCache.TryRemove(secUid, out _);
        }

        private static bool CheckCookieValid(string cookiePath)
        {
            try
            {
                if (!File.Exists(cookiePath))
                    return false;

                string cookieContent = File.ReadAllText(cookiePath);
                var (fileDto, _) = LifeCookieFileDto.Parse(cookieContent);
                var cookies = fileDto.Cookies;
                if (cookies == null || cookies.Count == 0)
                    cookies = JsonConvert.DeserializeObject<List<LifeCookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                    return false;

                var sessionCookie = cookies.FirstOrDefault(c =>
                    c.Name == "sessionid_ls" || c.Name == "sessionid_ss_ls");

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

        private static async Task<bool> CheckAnchorIsLive(AnchorInfo anchorInfo)
        {
            try
            {
                var cookies = LifeDataHandle.GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0) return false;

                string roomId = await GetLiveRoomId(anchorInfo);
                //return !string.IsNullOrEmpty(roomId);
                return true;
            }
            catch
            {
                return false;
            }
        }

        private static async Task<string> GetLiveRoomId(AnchorInfo anchorInfo)
        {
            try
            {
                var cookies = LifeDataHandle.GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0) return null;

                var accountDetail = await LifeDataApi.GetAccountDetail(cookies);
                if (accountDetail == null) return null;

                if (accountDetail.ContainsKey("current_live_room_id"))
                {
                    var roomId = accountDetail["current_live_room_id"];
                    if (!string.IsNullOrEmpty(roomId) && roomId != "0")
                        return roomId;
                }
                return null;
            }
            catch
            {
                return null;
            }
        }
    }
}