using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.diagnosis
{
    public class SaveDiagnosisBo
    {

        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId {  get; set; }

        /// <summary>
        /// 提示词ids
        /// </summary>
        public long cueWordsId { get; set; }

    }
}
