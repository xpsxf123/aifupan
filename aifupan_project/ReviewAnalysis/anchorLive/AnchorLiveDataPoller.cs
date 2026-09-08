using douyin.Utils;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台数据轮询器 - 定时自动采集直播数据
    /// </summary>
    public class AnchorLiveDataPoller
    {
        private AnchorInfo _anchorInfo;
        private string _roomId;
        private string _videoId;
        private System.Timers.Timer _pollingTimer;
        private bool _isRunning;
        private int _intervalSeconds;
        private bool _isTaskRunning;
        private readonly object _lockObj = new object();
        
        public AnchorLiveDataPoller(AnchorInfo anchorInfo, int intervalSeconds = 30)
        {
            _anchorInfo = anchorInfo;
            _intervalSeconds = intervalSeconds;
            _isRunning = false;
        }
        
        public string RoomId
        {
            get => _roomId;
            set => _roomId = value;
        }

        public string VideoId
        {
            get => _videoId;
            set => _videoId = value;
        }
        
        /// <summary>
        /// 启动轮询
        /// </summary>
        public void Start()
        {
            if (_isRunning)
            {
                FileUtils.LogRpa($"[轮询器] 轮询已在运行中", "主播后台轮询");
                return;
            }
            
            if (string.IsNullOrEmpty(_roomId))
            {
                FileUtils.LogRpa($"[轮询器] 错误：房间号为空，无法启动轮询", "主播后台轮询");
                return;
            }
            
            _isRunning = true;
            _pollingTimer = new System.Timers.Timer(_intervalSeconds * 1000);
            _pollingTimer.Elapsed += (s, e) =>
            {
                _ = Task.Run(async () => await PollDataAsync());
            };
            _pollingTimer.AutoReset = true;
            _pollingTimer.Start();
            
            FileUtils.LogRpa($"[轮询器] ✅ 已启动轮询（每{_intervalSeconds}秒采集一次）", "主播后台轮询");
            FileUtils.LogRpa($"[轮询器] 📊 主播：{_anchorInfo.AnchorName}", "主播后台轮询");
            FileUtils.LogRpa($"[轮询器] 🏠 房间号：{_roomId}", "主播后台轮询");

            // 启动后立即执行一次
            _ = Task.Run(async () => await PollDataAsync());
        }
        
        /// <summary>
        /// 停止轮询
        /// </summary>
        public void Stop()
        {
            if (!_isRunning)
            {
                return;
            }
            
            _isRunning = false;
            
            if (_pollingTimer != null)
            {
                _pollingTimer.Stop();
                _pollingTimer.Dispose();
                _pollingTimer = null;
            }
            
            FileUtils.LogRpa($"[轮询器] ⏹️ 已停止轮询", "主播后台轮询");
        }
        
        /// <summary>
        /// 执行数据采集
        /// </summary>
        private async Task PollDataAsync()
        {
            lock (_lockObj)
            {
                if (_isTaskRunning)
                {
                    FileUtils.LogRpa($"[轮询器] 上次轮询任务仍在执行，跳过本次", "主播后台轮询");
                    return;
                }
                _isTaskRunning = true;
            }

            try
            {
                if (!_isRunning) return;

                FileUtils.LogRpa($"", "主播后台轮询");
                FileUtils.LogRpa($"[{DateTime.Now:HH:mm:ss}] 🔄 开始第{GetTickCount()}次轮询...", "主播后台轮询");
                
                // 检查 Cookie
                var cookies = AnchorLiveDataHandle.GetCookiesFromLocal(_anchorInfo.SecUid);
                
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa($"[{DateTime.Now:HH:mm:ss}] ⚠️ Cookie 为空，跳过本次轮询", "主播后台轮询");
                    return;
                }
                
                // 拉取数据
                bool success = await AnchorLiveDataHandle.PullAnchorData(_anchorInfo, _roomId, _videoId);
                
                if (success)
                {
                    FileUtils.LogRpa($"[{DateTime.Now:HH:mm:ss}] ✅ 轮询完成，下次采集将在 {_intervalSeconds} 秒后", "主播后台轮询");
                }
                else
                {
                    FileUtils.LogRpa($"[{DateTime.Now:HH:mm:ss}] ⚠️ 轮询失败，请检查日志", "主播后台轮询");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[{DateTime.Now:HH:mm:ss}] ❌ 轮询异常：{ex.Message}", "主播后台轮询");
                FileUtils.LogRpa($"[{DateTime.Now:HH:mm:ss}] 📝 堆栈：{ex.StackTrace}", "主播后台轮询");
            }
            finally
            {
                lock (_lockObj)
                {
                    _isTaskRunning = false;
                }
            }
        }
        
        /// <summary>
        /// 获取运行时长（用于日志）
        /// </summary>
        private int GetTickCount()
        {
            return Environment.TickCount % 10000;
        }
    }
}
