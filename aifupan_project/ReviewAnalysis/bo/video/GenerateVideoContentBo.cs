using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.video
{
    public class GenerateVideoContentBo
    {

        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 类型类型
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 原文类型 0分钟段落 1自然原文 2优化原文
        /// </summary>
        public int type { get; set; }

    }
}
