using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.analysis
{
    public class WordListItemBo
    {

        /// <summary>
        /// 词语
        /// </summary>
        public string Word { get; set; }

        /// <summary>
        /// 开始时间，毫秒
        /// </summary>
        public long StartTime { get; set; }

        /// <summary>
        /// 结束时间，毫秒
        /// </summary>
        public long EndTime { get; set; }

    }
}
