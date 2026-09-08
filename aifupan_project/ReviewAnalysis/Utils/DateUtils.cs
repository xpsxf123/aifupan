using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 关于时间的utils
    /// </summary>
    public class DateUtils
    {

        /// <summary>
        /// 把字符串的时间转成DateTime
        /// </summary>
        /// <param name="dateTimeString"></param>
        /// <param name="format"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static DateTime ConvertFromDateTime(string dateTimeString, string format = "yyyy-MM-dd HH:mm:ss")
        {
            // 解析字符串为DateTime对象 
            DateTime parsedDateTime;
            if (DateTime.TryParseExact(dateTimeString, format, CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out parsedDateTime))
            {
                // 将DateTime转换为UTC时间 
                DateTime utcDateTime = DateTime.SpecifyKind(parsedDateTime, DateTimeKind.Utc);

                return utcDateTime;
            }
            else
            {
                throw new CustomException("无效的日期时间格式");
            }
        }



        /// <summary>
        /// 将日期时间字符串转换为时间戳（秒级）
        /// </summary>
        /// <param name="dateString">日期时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"</param>
        /// <returns>秒级时间戳</returns>
        public static long StringToTimestamp(string dateString, string DateFormat = "yyyy-MM-dd HH:mm:ss")
        {
            // 解析日期时间字符串为DateTime对象
            DateTime dateTime = DateTime.ParseExact(dateString, DateFormat, CultureInfo.InvariantCulture);

            // 转换为UTC时间
            DateTime utcDateTime = dateTime.ToUniversalTime();

            // 计算从Unix纪元到该时间的毫秒数
            long timestampInSeconds = (long)(utcDateTime - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;

            return timestampInSeconds;
        }

        /// <summary>
        /// 将日期时间字符串转换为时间戳（秒级）
        /// </summary>
        /// <param name="dateString">日期时间字符串，格式为 "yyyy-MM-dd HH:mm:ss"</param>
        /// <returns>秒级时间戳</returns>
        public static long StringToTimestampNoUTC(string dateString, string DateFormat = "yyyy-MM-dd HH:mm:ss")
        {
            // 解析日期时间字符串为DateTime对象
            DateTime dateTime = DateTime.ParseExact(dateString, DateFormat, CultureInfo.InvariantCulture);

            // 计算从Unix纪元到该时间的毫秒数
            long timestampInSeconds = (long)(dateTime - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Local)).TotalMilliseconds;

            return timestampInSeconds;
        }

        public static DateTime StringToDateTime(string dateString, string DateFormat = "yyyy-MM-dd HH:mm:ss")
        {
            // 解析日期时间字符串为DateTime对象
            return DateTime.ParseExact(dateString, DateFormat, CultureInfo.InvariantCulture);
        }

        /// <summary>
        /// 将时间戳转换为DateTime对象
        /// </summary>
        /// <param name="timestampInSeconds">秒级时间戳</param>
        /// <returns>对应的本地时间的DateTime对象</returns>
        public static DateTime TimestampToDateTime(long timestampInSeconds, bool isLocal = true)
        {
            // 从Unix纪元开始计算时间戳对应的时间
            DateTime utcDateTime = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc).AddMilliseconds(timestampInSeconds);

            // 转换为本地时间
            DateTime localDateTime = utcDateTime.ToLocalTime();

            return isLocal ? localDateTime : utcDateTime;
        }

        /// <summary>
        /// 将DateTime对象转换为日期时间字符串
        /// </summary>
        /// <param name="dateTime">DateTime对象</param>
        /// <returns>格式化后的日期时间字符串</returns>
        public static string DateTimeToString(DateTime dateTime, string DateFormat = "yyyy-MM-dd HH:mm:ss")
        {
            // 格式化DateTime对象为指定格式的字符串
            string formattedDateString = dateTime.ToString(DateFormat, CultureInfo.InvariantCulture);

            return formattedDateString;
        }

        /// <summary>
        /// 将DateTime对象转换为日期时间字符串
        /// </summary>
        /// <param name="dateTime">DateTime对象</param>
        /// <returns>格式化后的日期时间字符串</returns>
        public static string DateTimeToString(long timestamp, string DateFormat = "yyyy-MM-dd HH:mm:ss")
        {
            // 格式化DateTime对象为指定格式的字符串
            string formattedDateString = TimestampToDateTime(timestamp).ToString(DateFormat, CultureInfo.InvariantCulture);

            return formattedDateString;
        }

        /// <summary>
        /// 将DateTime对象转换为日期时间字符串
        /// </summary>
        /// <param name="dateTime">DateTime对象</param>
        /// <returns>格式化后的日期时间字符串</returns>
        public static string DateTimeUTCToString(long timestamp, string DateFormat = "yyyy-MM-dd HH:mm:ss")
        {
            // 格式化DateTime对象为指定格式的字符串
            string formattedDateString = TimestampToDateTime(timestamp, false).ToString(DateFormat, CultureInfo.InvariantCulture);

            return formattedDateString;
        }

    }
}
