using douyin.Utils;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号数据轮询器
    /// </summary>
    public class EnterpriseDataPoller
    {
        public AnchorInfo AnchorInfo { get; private set; }
        public string RoomId { get; set; }
        public string VideoId { get; set; }
        private System.Timers.Timer _pollingTimer;
        private bool _isRunning;
        private bool _isTaskRunning; // 任务执行标志（避免并发冲突）
        private readonly object _lockObj = new object(); // 锁对象（保证线程安全）

        public EnterpriseDataPoller(AnchorInfo anchorInfo)
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

            FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号数据轮询已启动", "企业号数据采集");

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

            FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号数据轮询已停止", "企业号数据采集");
        }

        private async Task PollDataAsync()
        {
            // 防止重入：如果上一次任务还未完成，跳过本次执行
            lock (_lockObj)
            {
                if (_isTaskRunning)
                {
                    FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号上次轮询任务仍在执行，跳过本次轮询", "企业号数据采集");
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
                    FileUtils.LogRpa("企业号轮询参数不完整，跳过本次轮询", "企业号数据采集");
                    return;
                }

                var cookies = EnterpriseDataHandle.GetCookiesFromLocal(AnchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号Cookie为空，无法获取数据", "企业号数据采集");
                    Stop();
                    return;
                }

                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号轮询开始执行", "企业号数据采集");
                await EnterpriseDataHandle.PullEnterpriseData(AnchorInfo, RoomId, VideoId);
                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号轮询执行完成", "企业号数据采集");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}企业号数据轮询异常: {ex.Message}\n{ex.StackTrace}", "企业号数据采集");
            }
            finally
            {
                // 无论成功失败，都重置执行标志
                lock (_lockObj)
                {
                    _isTaskRunning = false;
                }
            }
        }
    }
}
