using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto

{
    public class UploadFileDto
    {
        /// <summary>
        /// 文件显示url
        /// </summary>
        public string ShowUrl {  get; set; }
        /// <summary>
        /// 主键
        /// </summary>
        public int? Id { get; set; }
        /// <summary>
        /// 文件唯一标识id
        /// </summary>
        public string FileId { get; set; }
        /// <summary>
        /// 文件名称
        /// </summary>
        public string FileName { get; set; }
        /// <summary>
        /// 文件类型 0：视频 1：音频 2：文本
        /// </summary>
        public int FileType { get; set; }
        /// <summary>
        /// 文件原路径
        /// </summary>
        public string OriginalPath { get; set; }
        /// <summary>
        /// 文件新路径
        /// </summary>
        public string NowPath { get; set; }
        /// <summary>
        /// 分析状态 analysis_status 0：未分析 1：分析中 2：分析完成 3：分析失败
        /// </summary>
        public int? AnalysisStatus { get; set; }
        /// <summary>
        /// 分析完成时间 analysis_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string AnalysisTime { get; set; }
        /// <summary>
        /// 上传时间 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string UploadTime { get; set; }
        /// <summary>
        /// 文件大小，单位b
        /// </summary>
        public string FileSize { get; set; }
        /// <summary>
        /// 文件时长，单位：秒
        /// </summary>
        public int? FileDuration { get; set; }
        /// <summary>
        /// 分析失败原因
        /// </summary>
        public string ErrorReason { get; set; }

    }
}
