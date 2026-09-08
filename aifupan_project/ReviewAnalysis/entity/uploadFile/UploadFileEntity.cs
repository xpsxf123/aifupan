using ReviewAnalysis.entity.video;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.vo.common;

namespace ReviewAnalysis.entity.uploadFile
{
    public class UploadFileEntity : BasicSettingsBaseDto
    {
        /// <summary>
        /// ID
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// 文件名称
        /// </summary>
        public string fileName { get; set; }

        /// <summary>
        /// 文件类型 0：视频 1：音频 2：文本
        /// </summary>
        public int fileType { get; set; }

        /// <summary>
        /// 文件源路径
        /// </summary>
        public string originalPath { get; set; }

        /// <summary>
        /// 文件新路径
        /// </summary>
        public string nowPath { get; set; }

        /// <summary>
        /// 文件大小，单位b
        /// </summary>
        public long? fileSize { get; set; }

        /// <summary>
        /// 时长，单位：秒
        /// </summary>
        public long? fileDuration { get; set; }

        /// <summary>
        /// 错误原因
        /// </summary>
        public string errorReason { get; set; }

        /// <summary>
        /// 行业Id
        /// </summary>
        public long? tradeId { get; set; }

        /// <summary>
        /// 平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
        /// </summary>
        public int platformType { get; set; }

        /// <summary>
        /// 分析状态  0：未分析 1：分析中 2：分析完成 3分析错误 4：未开启自动分析 4：切片中
        /// </summary>
        public int? analysisStatus { get; set; }

        /// <summary>
        /// 分析时间
        /// </summary>
        public string analysisTime { get; set; }

        /// <summary>
        /// 更新时间
        /// </summary>
        public string uploadTime { get; set; }

        /// <summary>
        /// 上传用户
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 文件Id
        /// </summary>
        public string fileId { get; set; }

        /// <summary>
        /// 文件字数，只有文本文件有
        /// </summary>
        public int? fileWordNum { get; set; }

        /// <summary>
        /// 文件上传状态 0：未上传 1：已上传
        /// </summary>
        public int? uploadStatus { get; set; }

        /// <summary>
        /// 是否已标注敏感词 0：未标注 1：已标注
        /// </summary>
        public int? isMark { get; set; }

        /// <summary>
        /// 占用云空间的大小，单位：M
        /// </summary>
        public int? cloudStore { get; set; }

        /// <summary>
        /// 在线播放地址，只有视频和音频有
        /// </summary>
        public string playUrl { get; set; }

        /// <summary>
        /// 在线复盘的url
        /// </summary>
        public string shareUrl { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }
        /// <summary>
        /// 文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频
        /// </summary>
        public int? fileSliceType { get; set; }
        /// <summary>
        /// 文件切片信息
        /// </summary>
        public VideoSliceEntity videoSliceInfo { get; set; }
        /// <summary>
        /// 切片视频所属原文件信息
        /// </summary>
        public UploadFileEntity parentFileInfo { get; set; }
        /// <summary>
        /// 原文件下的所有切片信息
        /// </summary>
        public List<VideoSliceEntity> sliceList { get; set; }
        /// <summary>
        /// 重命名
        /// </summary>
        public string videoRename { get; set; }
        /// <summary>
        /// 一句话识别引擎模型，如：16k_zh
        /// </summary>
        public string engSerViceType { get; set; }
    }
}
