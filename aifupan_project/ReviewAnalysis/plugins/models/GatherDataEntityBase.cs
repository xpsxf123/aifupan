using System;
using System.Collections.Generic;

namespace ReviewAnalysis.plugins.models
{
    /// <summary>
    /// 汇总数据基类
    /// </summary>
    public abstract class GatherDataEntityBase
    {
        /// <summary>
        /// 主播ID
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 直播间ID
        /// </summary>
        public string RoomId { get; set; }

        /// <summary>
        /// 视频ID
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 批次号
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        /// 采集开始时间
        /// </summary>
        public DateTime StartTime { get; set; }

        /// <summary>
        /// 采集结束时间
        /// </summary>
        public DateTime EndTime { get; set; }

        /// <summary>
        /// OSS路径
        /// </summary>
        public string OssPath { get; set; }

        /// <summary>
        /// 模块ID
        /// </summary>
        public string ModuleId { get; set; }

        /// <summary>
        /// 数据版本
        /// </summary>
        public string Version { get; set; } = "1.0";
    }
}
