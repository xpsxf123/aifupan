using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class ASRResultErrorEntity
    {
        /// <summary>
        /// 一句话识别错误码
        /// </summary>
        public string Code { get; set; }
        /// <summary>
        /// 一句话识别错误内容
        /// </summary>
        public string Message { get; set; }
    }
}
