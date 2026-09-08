using douyin.Utils;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.life
{
    public class LifeDataPoller
    {
        public AnchorInfo AnchorInfo { get; private set; }
        public string RoomId { get; set; }
        public string VideoId { get; set; }
        private System.Timers.Timer _pollingTimer;
        private bool _isRunning;
        private bool _isTaskRunning;
        private readonly object _lockObj = new object();

        public LifeDataPoller(AnchorInfo anchorInfo)
        {
            AnchorInfo = anchorInfo;
            _isRunning = false;
        }

        public void Start()
        {
            if (_isRunning) return;

            _isRunning = true;
            _pollingTimer = new System.Timers.Timer(30000);
            _pollingTimer.Elapsed += (s, e) =>
            {
                _ = Task.Run(async () => await PollDataAsync());
            };
            _pollingTimer.AutoReset = true;
            _pollingTimer.Start();

            FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客数据轮询已启动", "来客数据采集");

            // 启动后立即执行一次
            _ = Task.Run(async () => await PollDataAsync());
        }

        public void Stop()
        {
            if (!_isRunning) return;

            _isRunning = false;
            if (_pollingTimer != null)
            {
                _pollingTimer.Stop();
                _pollingTimer.Dispose();
                _pollingTimer = null;
            }

            FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客数据轮询已停止", "来客数据采集");
        }

        private async Task PollDataAsync()
        {
            lock (_lockObj)
            {
                if (_isTaskRunning)
                {
                    FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客上次轮询任务仍在执行，跳过本次轮询", "来客数据采集");
                    return;
                }
                _isTaskRunning = true;
            }

            try
            {
                if (!_isRunning)
                    return;

                if (AnchorInfo == null || string.IsNullOrEmpty(RoomId))
                {
                    FileUtils.LogRpa("来客轮询参数不完整，跳过本次轮询", "来客数据采集");
                    return;
                }

                var cookies = LifeDataHandle.GetCookiesFromLocal(AnchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客Cookie为空，无法获取数据", "来客数据采集");
                    Stop();
                    return;
                }

                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客轮询开始执行", "来客数据采集");
                await LifeDataHandle.PullLifeData(AnchorInfo, RoomId, VideoId, string.Empty);
                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客轮询执行完成", "来客数据采集");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}来客数据轮询异常: {ex.Message}\n{ex.StackTrace}", "来客数据采集");
            }
            finally
            {
                lock (_lockObj)
                {
                    _isTaskRunning = false;
                }
            }
        }
    }
}