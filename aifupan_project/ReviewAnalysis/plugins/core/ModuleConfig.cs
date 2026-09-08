namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 模块配置
    /// </summary>
    public class ModuleConfig
    {
        /// <summary>
        /// 采集间隔（毫秒），默认0表示从服务器获取（platform_collector_time）
        /// </summary>
        public int IntervalMs { get; set; } = 0;

        /// <summary>
        /// 错峰启动偏移（毫秒），默认0
        /// </summary>
        public int OffsetMs { get; set; } = 0;

        /// <summary>
        /// 是否启用，默认true
        /// </summary>
        public bool Enabled { get; set; } = true;

        /// <summary>
        /// 采集超时（毫秒），默认60秒
        /// </summary>
        public int TimeoutMs { get; set; } = 60000;

        /// <summary>
        /// 最大重试次数，默认3次
        /// </summary>
        public int MaxRetryCount { get; set; } = 3;

        /// <summary>
        /// 模块优先级（数字越小优先级越高）
        /// </summary>
        public int Priority { get; set; } = 0;

        /// <summary>
        /// 创建默认配置
        /// </summary>
        public static ModuleConfig Default => new ModuleConfig();

        /// <summary>
        /// 创建自定义间隔配置
        /// </summary>
        public static ModuleConfig WithInterval(int intervalMs)
        {
            return new ModuleConfig { IntervalMs = intervalMs };
        }

        /// <summary>
        /// 复制配置
        /// </summary>
        public ModuleConfig Clone()
        {
            return new ModuleConfig
            {
                IntervalMs = this.IntervalMs,
                OffsetMs = this.OffsetMs,
                Enabled = this.Enabled,
                TimeoutMs = this.TimeoutMs,
                MaxRetryCount = this.MaxRetryCount,
                Priority = this.Priority
            };
        }
    }
}
