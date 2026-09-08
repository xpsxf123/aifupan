using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.juliang;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliangApi
{
    /// <summary>
    /// 巨量百应无窗体模块 - 采集管理器
    /// </summary>
    public class JuliangApiDataCollectionManager
    {
        private static ConcurrentDictionary<string, JuliangApiDataPoller> _pollers = new ConcurrentDictionary<string, JuliangApiDataPoller>();
        private static ConcurrentDictionary<string, string> _roomIdCache = new ConcurrentDictionary<string, string>();
        private static Timer _detectionTimer;
        private static bool _isInitialized;

        public static void StartDetection()
        {
            if (_detectionTimer != null) return;

            _detectionTimer = new Timer(DetectionTimerCallback, null, 30000, 30000);
            FileUtils.LogRpa("巨量API主播直播状态检测已启动", "巨量API");
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

            FileUtils.LogRpa("巨量API数据采集已停止", "巨量API");
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
                    FileUtils.LogRpa($"巨量API主播检测异常: {ex.Message}", "巨量API");
                }
            });
        }

        private static async Task CheckAllAnchorsAsync()
        {
            await CheckAndUpdateAllCookies();
            await CheckAndStartPollingIfLive();
        }

        /// <summary>
        /// 检查所有主播的巨量Cookie有效性，同步授权状态
        /// </summary>
        private static async Task CheckAndUpdateAllCookies()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null || anchor.platform != 0) continue;

                try
                {
                    int cookieStatus = JuliangUtils.CheckAuthStatusByCookie(anchor.SecUid);

                    if (cookieStatus == JuliangAuthStatusEnum.auth)
                    {
                        if (anchor.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                        {
                            AnchorBll.UpdateJuliangAuthStatus(anchor, JuliangAuthStatusEnum.auth);
                            FileUtils.LogRpa($"主播{anchor.AnchorName}巨量Cookie有效，状态更新为已授权", "巨量API");
                        }
                    }
                    else if (cookieStatus == JuliangAuthStatusEnum.authExpires)
                    {
                        if (anchor.juliangAuthStatus == JuliangAuthStatusEnum.auth)
                        {
                            AnchorBll.UpdateJuliangAuthStatus(anchor, JuliangAuthStatusEnum.authExpires);
                            FileUtils.LogRpa($"主播{anchor.AnchorName}巨量Cookie过期，状态更新为授权过期", "巨量API");
                        }

                        // Cookie过期时停止轮询
                        StopPolling(anchor.SecUid);
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"检查主播{anchor.AnchorName}巨量Cookie异常: {ex.Message}", "巨量API");
                }
            }
        }

        /// <summary>
        /// 检查正在轮询的主播，直播结束时停止轮询
        /// </summary>
        private static async Task CheckAndStartPollingIfLive()
        {
            var anchors = AnchorCacheManager.GetAllNotRemoveAnchors();
            foreach (var anchor in anchors)
            {
                if (anchor == null || anchor.platform != 0) continue;

                if (anchor.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                {
                    StopPolling(anchor.SecUid);
                    continue;
                }

                // 检测轮询状态
                bool isPolling = _pollers.ContainsKey(anchor.SecUid);
                if (isPolling)
                {
                    // 已在轮询，检查Cookie有效性（Cookie过期已在CheckAndUpdateAllCookies中处理停止）
                }
                else
                {
                    // 未轮询，检测抖音直播状态获取roomId
                    string roomId = await DouYinAnchorBll.GetLiveRoomIdIfOnline(anchor);
                    if (!string.IsNullOrEmpty(roomId))
                    {
                        StartPolling(anchor, roomId, "");
                        FileUtils.LogRpa($"检测到直播，自动启动巨量API数据轮询，主播：{anchor.AnchorName}，roomId={roomId}", "巨量API");
                    }
                }
            }
        }

        /// <summary>
        /// 启动单个主播的巨量数据轮询
        /// </summary>
        public static void StartPolling(AnchorInfo anchorInfo, string roomId, string videoId = "")
        {
            if (_pollers.ContainsKey(anchorInfo.SecUid)) return;

            _roomIdCache[anchorInfo.SecUid] = roomId;
            var poller = new JuliangApiDataPoller(anchorInfo) { RoomId = roomId, VideoId = videoId };
            _pollers[anchorInfo.SecUid] = poller;
            poller.Start();
        }

        /// <summary>
        /// 停止单个主播的巨量数据轮询
        /// </summary>
        public static void StopPolling(string secUid)
        {
            if (_pollers.TryRemove(secUid, out var poller))
            {
                poller.Stop();
            }
            _roomIdCache.TryRemove(secUid, out _);
        }

    }
}
