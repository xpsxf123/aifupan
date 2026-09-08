using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.LogConsole
{
    // 日志级别枚举（与 FileUtils 日志分级对应）
    public enum LogLevel
    {
        Info,    // 普通日志
        Warning, // 警告日志
        Recrd,   // 录制日志
        Analysis,// 分析日志
        Error    // 错误日志
    }
    // 日志实体
    public class LogModel
    {
        public DateTime Time { get; set; } // 时间戳
        public LogLevel Level { get; set; } // 日志级别
        public string Message { get; set; } // 日志内容
        public string Source { get; set; } // 日志来源（如“抖音搜索接口”）
    }
}
