using juliang;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class JuliangReachDataConfig
    {
        /// <summary>
        /// 直播间 ID（必填）
        /// </summary>
        public string RoomId { get; set; } = ""; // 默认值，外部可覆盖
        /// <summary>
        /// 视频Id
        /// </summary>
        public string VideoId { get; set; } = "";
        /// <summary>
        /// 主播secUid
        /// </summary>
        public AnchorInfo AnchorInfo { get; set; }

        /// <summary>
        /// 轮询间隔（秒），默认 60 秒，最小 5 秒
        /// </summary>
        public int PollingIntervalSeconds { get; set; } = 60;

        /// <summary>
        /// 1.js 文件路径（默认项目运行目录下的 1.js）
        /// </summary>
        public string JsFilePath { get; set; } = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "script\\juliang.js");

        /// <summary>
        /// HttpClient 超时时间（秒），默认 10 秒
        /// </summary>
        public int HttpTimeoutSeconds { get; set; } = 10;

        /// <summary>
        /// 初始 Cookie（LUOPAN_DT），默认值，外部可更新
        /// </summary>
        public string LuopanDtCookie { get; set; } = "";

        /// <summary>
        /// 初始 Cookie（sessionid），默认值，外部可更新
        /// </summary>
        public string SessionIdCookie { get; set; } = "";

        public JuliangForm juliangForm { get; set; }

        /// <summary>
        /// 验证配置有效性
        /// </summary>
        public void Validate()
        {
            if (string.IsNullOrWhiteSpace(RoomId))
                throw new ArgumentException("直播间 ID（RoomId）不能为空");
            if (string.IsNullOrWhiteSpace(VideoId))
                throw new ArgumentException("视频 ID（VideoId）不能为空");
            if (PollingIntervalSeconds < 5)
                throw new ArgumentOutOfRangeException(nameof(PollingIntervalSeconds), "轮询间隔不能小于 5 秒");
            if (HttpTimeoutSeconds < 1)
                throw new ArgumentOutOfRangeException(nameof(HttpTimeoutSeconds), "超时时间不能小于 1 秒");
            if (string.IsNullOrWhiteSpace(JsFilePath))
                throw new ArgumentException("JS 文件路径（JsFilePath）不能为空");
        }
    }
    /// <summary>
    /// 轮询结果事件参数（外部订阅时获取数据）
    /// </summary>
    public class PollingResultEventArgs : EventArgs
    {
        /// <summary>
        /// 执行时间
        /// </summary>
        public DateTime ExecuteTime { get; set; } = DateTime.Now;

        /// <summary>
        /// 是否成功
        /// </summary>
        public bool IsSuccess { get; set; }

        /// <summary>
        /// 成功时的返回数据（序列化后的 JSON）
        /// </summary>
        public string Data { get; set; }

        /// <summary>
        /// 失败时的异常信息
        /// </summary>
        public string ErrorMessage { get; set; }

        /// <summary>
        /// 异常详情（可选）
        /// </summary>
        public Exception Exception { get; set; }
    }

    /// <summary>
    /// 状态变更事件参数
    /// </summary>
    public class StatusChangedEventArgs : EventArgs
    {
        public string Status { get; set; }
        public DateTime ChangeTime { get; set; } = DateTime.Now;
    }
}
