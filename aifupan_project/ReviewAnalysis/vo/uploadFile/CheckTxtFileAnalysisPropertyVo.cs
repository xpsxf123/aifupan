using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.uploadFile
{
    public class CheckTxtFileAnalysisPropertyVo
    {
        /// <summary>
        /// 文本字数
        /// </summary>
        public int fileWordNum {  get; set; }
        /// <summary>
        /// 资产剩余的字数
        /// </summary>
        public long propertyWordNum { get; set; }
        /// <summary>
        /// 资源是否足够 0：不足 1：足够
        /// </summary>
        public int isSufficient { get; set; }
    }
}
