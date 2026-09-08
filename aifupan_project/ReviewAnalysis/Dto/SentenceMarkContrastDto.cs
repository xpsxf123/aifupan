using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class SentenceMarkContrastDto
    {
        /// <summary>
        /// 视频1/文件1分析信息
        /// </summary>
        public SentenceMarkDto SentenceMark1 { get; set; }
        /// <summary>
        /// 视频2/文件2分析信息
        /// </summary>
        public SentenceMarkDto SentenceMark2 { get; set; }
        /// <summary>
        /// 对比信息
        /// </summary>
        public VideoContrast VideoContrast { get; set; }
    }
}
