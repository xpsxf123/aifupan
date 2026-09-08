using douyin.Utils;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.qianchuan
{
    public class QianchuanDataPoller
    {
        private Timer _timer;
        private readonly AnchorInfo _anchorInfo;
        private readonly string _roomId;
        private readonly string _aavid;
        private readonly string _anchorId;
        private bool _isRunning;

        public AnchorInfo AnchorInfo => _anchorInfo;
        public string RoomId => _roomId;
        public string Aavid => _aavid;
        public string AnchorId => _anchorId;

        public QianchuanDataPoller(AnchorInfo anchorInfo, string roomId, string aavid, string anchorId)
        {
            _anchorInfo = anchorInfo;
            _roomId = roomId;
            _aavid = aavid;
            _anchorId = anchorId;
        }

        public void Start(int intervalSeconds = 30)
        {
            if (_isRunning) return;
            _isRunning = true;

            _timer = new Timer(PollingElapsed, null, 0, intervalSeconds * 1000);
            FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}千川数据轮询已启动，间隔{intervalSeconds}秒，aavid={_aavid}", "千川数据采集");
        }

        public void Stop()
        {
            _isRunning = false;
            if (_timer != null)
            {
                _timer.Dispose();
                _timer = null;
            }
            FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}千川数据轮询已停止", "千川数据采集");
        }

        private void PollingElapsed(object state)
        {
            if (!_isRunning) return;

            Task.Run(async () =>
            {
                try
                {
                    await PullQianchuanData();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}千川数据轮询异常: {ex.Message}", "千川数据采集");
                }
            });
        }

        private async Task PullQianchuanData()
        {
            try
            {
                if (string.IsNullOrEmpty(_aavid))
                {
                    FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}aavid为空，无法拉取数据", "千川数据采集");
                    return;
                }

                var cookies = QianchuanDataHandle.GetCookiesFromLocal(_anchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}本地无千川Cookie，无法拉取数据", "千川数据采集");
                    return;
                }

                var overviewData = await QianchuanDataApi.GetOverviewBoardData(_roomId, _aavid, _anchorId, cookies, _anchorInfo.SecUid);
                if (overviewData != null && overviewData.Count > 0)
                {
                    FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}获取千川概览数据成功", "千川数据采集");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"主播{_anchorInfo?.AnchorName}拉取千川数据异常: {ex.Message}", "千川数据采集");
            }
        }
    }
}
