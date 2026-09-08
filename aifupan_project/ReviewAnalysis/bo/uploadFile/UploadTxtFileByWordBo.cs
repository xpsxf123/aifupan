using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.vo.common;

namespace ReviewAnalysis.bo.uploadFile
{
    public class UploadTxtFileByWordBo : BasicSettingsBaseDto
    {
        /// <summary>
        /// 平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书
        /// </summary>
        public int platformType { get; set; }
        /// <summary>
        /// 文本内容
        /// </summary>
        public string content { get; set; }
    }
}
