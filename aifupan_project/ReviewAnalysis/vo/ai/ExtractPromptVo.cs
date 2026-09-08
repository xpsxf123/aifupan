using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class ExtractPromptVo
    {

        /// <summary>
        /// 提示词
        /// </summary>
        public string cueWord { get; set; }

        /// <summary>
        /// ai的身份
        /// </summary>
        public string aiIdentity { get; set; }

        /// <summary>
        /// 使用的ai模型
        /// </summary>
        public int? aiModel { get; set; }

    }
}
