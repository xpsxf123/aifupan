using System;

namespace ReviewAnalysis.vo.shortVideo
{
    /// <summary>
    /// 本地视频文件信息VO
    /// </summary>
    public class LocalVideoInfoVo
    {
        /// <summary>
        /// 文件名（包含扩展名）
        /// </summary>
        public string fileName { get; set; }

        /// <summary>
        /// 文件完整路径
        /// </summary>
        public string filePath { get; set; }

        /// <summary>
        /// 文件大小（字节）
        /// </summary>
        public long fileSize { get; set; }

        /// <summary>
        /// 文件大小（格式化显示，如：1.2 MB）
        /// </summary>
        public string fileSizeFormatted { get; set; }

        /// <summary>
        /// 文件扩展名
        /// </summary>
        public string fileExtension { get; set; }

        /// <summary>
        /// 文件创建时间
        /// </summary>
        public DateTime creationTime { get; set; }

        /// <summary>
        /// 文件最后修改时间
        /// </summary>
        public DateTime lastWriteTime { get; set; }

        /// <summary>
        /// 视频时长（秒）
        /// </summary>
        public double? duration { get; set; }

        /// <summary>
        /// 视频时长（格式化显示，如：00:05:30）
        /// </summary>
        public string durationFormatted { get; set; }

        /// <summary>
        /// 视频宽度（像素）
        /// </summary>
        public int? width { get; set; }

        /// <summary>
        /// 视频高度（像素）
        /// </summary>
        public int? height { get; set; }

        /// <summary>
        /// 视频分辨率（格式化显示，如：1920x1080）
        /// </summary>
        public string resolution { get; set; }

        /// <summary>
        /// 视频帧率
        /// </summary>
        public double? frameRate { get; set; }

        /// <summary>
        /// 视频比特率
        /// </summary>
        public long? bitRate { get; set; }

        /// <summary>
        /// 视频编码格式
        /// </summary>
        public string videoCodec { get; set; }

        /// <summary>
        /// 音频编码格式
        /// </summary>
        public string audioCodec { get; set; }

        /// <summary>
        /// 是否选择成功
        /// </summary>
        public bool success { get; set; }

        /// <summary>
        /// 错误信息（如果选择失败）
        /// </summary>
        public string errorMessage { get; set; }
    }
}
