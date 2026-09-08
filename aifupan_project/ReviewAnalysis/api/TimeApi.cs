using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;

namespace ReviewAnalysis.api
{
    /// <summary>
    /// 服务端时间 API
    /// </summary>
    public class TimeApi
    {
        /// <summary>
        /// 获取服务端当前时间戳（毫秒），带重试机制。
        /// 失败时返回 0，调用方负责 fallback。
        /// </summary>
        /// <returns>服务端毫秒时间戳，失败返回 0</returns>
        public static long GetServerTime()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/time/currentTime", null, retry: true);

            if (!string.IsNullOrEmpty(dataStr) && long.TryParse(dataStr, out long timestamp))
            {
                return timestamp;
            }

            return 0;
        }
    }
}
