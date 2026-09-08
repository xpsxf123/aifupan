using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class ServerTimeUtils
    {
        /// <summary>
        /// 服务器时间与本地时间的偏移量（毫秒）：服务器时间 - 本地时间
        /// </summary>
        private static long _serverTimeOffset = 0;
        private static bool _offsetInitialized = false;
        private static readonly object _offsetLock = new object();

        public static bool timeAccurate = true;
        public static string recordServerTime = "";
        public static long recordServerMilliseconds = 0;

        /// <summary>
        /// 获取当前时间字符串 默认yyyy-MM-dd HH:mm:ss格式
        /// </summary>
        /// <returns></returns>
        public static string getCurrentTimeStr(string format = "yyyy-MM-dd HH:mm:ss")
        {
            long milliseconds = getCurrentTime();
            var epoch = new DateTimeOffset(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
            var dateTime = epoch.AddMilliseconds(milliseconds).ToOffset(TimeSpan.FromHours(8));
            return dateTime.ToString(format);
        }

        /// <summary>
        /// 获取当前服务器时间的 DateTime 对象（东八区）
        /// </summary>
        /// <returns></returns>
        public static DateTime getCurrentDateTime()
        {
            long milliseconds = getCurrentTime();
            var epoch = new DateTimeOffset(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
            var dateTime = epoch.AddMilliseconds(milliseconds).ToOffset(TimeSpan.FromHours(8));
            return dateTime.DateTime;
        }

        public static long GetCurrentUnixTimeMilliseconds()
        {
            DateTimeOffset now = DateTimeOffset.UtcNow;
            DateTimeOffset unixEpoch = new DateTimeOffset(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
            return now.ToUnixTimeMilliseconds();

        }

        /// <summary>
        /// 将时间戳转成日期时间戳字符串
        /// </summary>
        /// <param name="milliseconds">时间戳（毫秒）</param>
        /// <param name="format">日期格式</param>
        /// <returns></returns>
        public static string getTimeStrByTime(long milliseconds, string format = "yyyy-MM-dd HH:mm:ss")
        {
            var epoch = new DateTimeOffset(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
            var dateTime = epoch.AddMilliseconds(milliseconds).ToOffset(TimeSpan.FromHours(8));
            return dateTime.ToString(format);
        }

        /// <summary>
        /// 获取服务时间戳
        /// </summary>
        /// <returns></returns>
        public static long getReplayServerTime()
        {
            long temp = ReplayHttpUtils.GetReplayServerTime();
            if (temp == 0)
            {
                temp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            }
            return temp;
        }

        /// <summary>
        /// 获取当前时间戳
        /// </summary>
        /// <returns></returns>
        public static long getCurrentTime()
        {
            long localTime = GetCurrentUnixTimeMilliseconds();
            if (_offsetInitialized)
            {
                return localTime + _serverTimeOffset;
            }
            return localTime;
        }

        /// <summary>
        /// 刷新服务器时间偏移（启动时调用一次，之后定时调用）
        /// 只请求一次服务器，后续用 本地时间 + 偏移 计算
        /// </summary>
        public static void RefreshServerTimeOffset()
        {
            try
            {
                long serverTime = getServerCurrentTime();
                if (serverTime != 0)
                {
                    long localTime = GetCurrentUnixTimeMilliseconds();
                    long offset = serverTime - localTime;
                    lock (_offsetLock)
                    {
                        _serverTimeOffset = offset;
                        _offsetInitialized = true;
                    }
                    // timeAccurate = Math.Abs(offset) <= 5000;
                }
            }
            catch (Exception e)
            {
                FileUtils.log($"刷新服务器时间偏移失败: {e.Message}");
            }
        }

        /// <summary>
        /// 获取服务器时间
        /// </summary>
        /// <returns></returns>
        public static long getServerCurrentTime()
        {
            long serverTime = TimeApi.GetServerTime();
            if (serverTime == 0)
            {
                serverTime = getCurrentTime();
                FileUtils.log($"获取服务器时间返回0，已降级使用本地时间: {serverTime}", "服务器时间降级");
            }
            return serverTime;
        }

        /// <summary>
        /// 获取服务器时间字符串 默认yyyy-MM-dd HH:mm:ss格式
        /// </summary>
        /// <param name="format">日期格式</param>
        /// <returns></returns>
        public static string getServerCurrentTimeStr(string format = "yyyy-MM-dd HH:mm:ss")
        {
            long milliseconds = getServerCurrentTime();
            var epoch = new DateTimeOffset(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
            var dateTime = epoch.AddMilliseconds(milliseconds).ToOffset(TimeSpan.FromHours(8));
            return dateTime.ToString(format);
        }

        public static void checkTimeAccurate()
        {
            timeAccurate = true;
            RefreshServerTimeOffset();
        }

        /// <summary>
        /// 定时更新时间是否准确状态
        /// </summary>
        public static void UpdateTimeAccurate()
        {

            while (true)
            {
                try
                {
                    checkTimeAccurate();
                    
                    Thread.Sleep(1 * 60 * 1000); // 定时30分钟
                } catch(Exception e)
                {
                    FileUtils.log(e.ToString());
                }
                
            }
        }

        /// <summary>
        /// 将时分秒字符串转成秒
        /// </summary>
        /// <param name="time">时分秒 HH:mm:ss</param>
        /// <returns></returns>
        public static int TimeToSecond(string time)
        {
            int second = 0;
            try
            {
                string[] arr = time.Split(':');
                second += int.Parse(arr[0]) * 3600;
                second += int.Parse(arr[1]) * 60;
                second += 0;    // 当前没有秒的数值
            }
            catch(Exception e)
            {
                FileUtils.LogError($"{e}", $"将时分秒字符串转成秒发送错误");
            }

            return second;
        }
    }
}
