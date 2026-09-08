using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.ShortVideo.Enums
{
    /// <summary>
    /// 平台类型枚举
    /// </summary>
    public enum PlatformType
    {
        /// <summary>
        /// 抖音
        /// </summary>
        Douyin = 1,

        /// <summary>
        /// 快手
        /// </summary>
        Kuaishou = 2,

        /// <summary>
        /// 视频号
        /// </summary>
        WechatVideo = 3,

        /// <summary>
        /// 本地上传
        /// </summary>
        LocalUpload = 4
    }

    /// <summary>
    /// 视频来源类型枚举
    /// </summary>
    public enum VideoSourceType
    {
        /// <summary>
        /// 短视频URL
        /// </summary>
        ShortVideoUrl = 1,

        /// <summary>
        /// 本地上传
        /// </summary>
        LocalUpload = 2,

        /// <summary>
        /// 搜达人
        /// </summary>
        SearchCreator = 3,

        /// <summary>
        /// 搜爆款
        /// </summary>
        SearchHot = 4
    }

    /// <summary>
    /// 文案提取状态枚举
    /// </summary>
    public enum ExtractStatus
    {
        /// <summary>
        /// 未提取
        /// </summary>
        NotExtracted = 0,

        /// <summary>
        /// 待处理
        /// </summary>
        Pending = 1,

        /// <summary>
        /// 处理中
        /// </summary>
        Processing = 2,

        /// <summary>
        /// 已完成
        /// </summary>
        Completed = 3,

        /// <summary>
        /// 失败
        /// </summary>
        Failed = 4
    }

    /// <summary>
    /// AI分析状态枚举
    /// </summary>
    public enum AnalysisStatus
    {
        /// <summary>
        /// 未分析
        /// </summary>
        NotAnalyzed = 0,

        /// <summary>
        /// 待处理
        /// </summary>
        Pending = 1,

        /// <summary>
        /// 处理中
        /// </summary>
        Processing = 2,

        /// <summary>
        /// 已完成
        /// </summary>
        Completed = 3,

        /// <summary>
        /// 失败
        /// </summary>
        Failed = 4
    }
}
