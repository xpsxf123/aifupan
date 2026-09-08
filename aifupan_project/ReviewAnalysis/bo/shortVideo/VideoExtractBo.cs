using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.shortVideo
{
    /// <summary>
    /// 视频提取业务对象
    /// </summary>
    public class VideoExtractBo
    {

        /// <summary>
        /// 主键id
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 视频来源：1-短视频URL 2-本地上传 
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 短视频名称（最大500字符）
        /// </summary>
        public string videoTitle { get; set; }

        /// <summary>
        /// 视频文件hash值（最大50字符）
        /// </summary>
        public string videoHash { get; set; }

        /// <summary>
        /// 视频URL地址
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 文案提取状态：0-未提取 1-待处理 2-处理中 3-已完成 4-失败 
        /// </summary>
        public byte extractStatus { get; set; }

        /// <summary>
        /// AI优化文本
        /// </summary>
        public string extractContent { get; set; }

        /// <summary>
        /// 原文文案内容
        /// </summary>
        public string originalExtractContent { get; set; }

        /// <summary>
        /// 视频时长（秒）
        /// </summary>
        public long duration { get; set; }

        /// <summary>
        /// 封面图片URL
        /// </summary>
        public string coverUrl { get; set; }

        /// <summary>
        /// 提取失败原因
        /// </summary>
        public string extractErrorReason { get; set; }

    }
}
