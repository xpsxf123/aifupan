using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class TimeUtils
    {

        /// <summary>
        /// 毫秒格式化
        /// </summary>
        /// <param name="milliseconds">毫秒</param>
        /// <param name="format">格式化</param>
        /// <returns></returns>
        public static string millisecondsFormat(long milliseconds, string format = @"hh\:mm\:ss\.fff")
        {
            TimeSpan time = TimeSpan.FromMilliseconds(milliseconds);
            return time.ToString(format);
        }

        /// <summary>
        /// 将毫秒时间戳转成日期时间戳字符串
        /// </summary>
        /// <param name="milliseconds">时间戳（毫秒）</param>
        /// <param name="format">日期格式</param>
        /// <returns></returns>
        public static string getDateTimeStrByMillisecond(long milliseconds, string format = "yyyy-MM-dd HH:mm:ss")
        {
            var epoch = new DateTimeOffset(1970, 1, 1, 0, 0, 0, TimeSpan.Zero);
            var dateTime = epoch.AddMilliseconds(milliseconds).ToOffset(TimeSpan.FromHours(8));
            return dateTime.ToString(format);
        }

        /// <summary>
        /// 将日期时间转成时间戳（毫秒）
        /// </summary>
        /// <param name="dateTime">日期时间 yyyy-MM-dd HH:mm:ss</param>
        /// <returns></returns>
        public static long getMillisecondByDateTimeStr(string dateTime)
        {
            string format = "yyyy-MM-dd HH:mm:ss";
            return DateTimeOffset.ParseExact(dateTime, format, CultureInfo.InvariantCulture).ToUnixTimeMilliseconds();
        }
    }
}
