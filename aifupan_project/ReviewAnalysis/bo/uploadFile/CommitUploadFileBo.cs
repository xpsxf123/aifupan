using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Dto;
using ReviewAnalysis.vo.common;

namespace ReviewAnalysis.bo.uploadFile
{
    public class CommitUploadFileBo : BasicSettingsBaseDto
    {
        /// <summary>
        /// 平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
        /// </summary>
        public int platformType { get; set; }
        /// <summary>
        /// 文件类型 0：音视频 1：文本文件
        /// </summary>
        public int fileType { get; set; }
        /// <summary>
        /// 文件完整路径
        /// </summary>
        public string filePath { get; set; }
        /// <summary>
        /// 一句话识别引擎模型，如：16k_zh
        /// </summary>
        public string engSerViceType { get; set; }
    }
}
