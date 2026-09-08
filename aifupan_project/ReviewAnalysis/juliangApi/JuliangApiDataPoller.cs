using douyin.Utils;
using ReviewAnalysis.Model;
using System;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliangApi
{
    /// <summary>
    /// 巨量百应无窗体模块 - 轮询器（30s间隔）
    /// </summary>
    public class JuliangApiDataPoller
    {
        public AnchorInfo AnchorInfo { get; private set; }
        public string RoomId { get; set; }
        public string VideoId { get; set; }
        private System.Timers.Timer _pollingTimer;
        private bool _isRunning;
        private bool _isTaskRunning;
        private readonly object _lockObj = new object();

        public JuliangApiDataPoller(AnchorInfo anchorInfo)
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

            FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量API数据轮询已启动", "巨量API");

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

            FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量API数据轮询已停止", "巨量API");
        }

        private async Task PollDataAsync()
        {
            lock (_lockObj)
            {
                if (_isTaskRunning)
                {
                    FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量API上次轮询任务仍在执行，跳过本次轮询", "巨量API");
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
                    FileUtils.LogRpa("巨量API轮询参数不完整，跳过本次轮询", "巨量API");
                    return;
                }

                // 检查Cookie是否有效
                string luopanDt = JuliangApiDataHandle.GetLuopanDt(AnchorInfo.SecUid);
                if (string.IsNullOrEmpty(luopanDt))
                {
                    FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量Cookie无效，停止轮询", "巨量API");
                    Stop();
                    return;
                }

                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量API轮询开始执行", "巨量API");
                await JuliangApiDataHandle.PullJuliangData(AnchorInfo, RoomId, VideoId);
                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量API轮询执行完成", "巨量API");

                // 拉取千川数据
                await JuliangApiDataHandle.PullQianchuanData(AnchorInfo, RoomId, VideoId);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"主播{AnchorInfo?.AnchorName}巨量API数据轮询异常: {ex.Message}\n{ex.StackTrace}", "巨量API");
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
