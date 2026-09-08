using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    /// <summary>
    /// 巨量用户画像明细项
    /// </summary>
    public class JuliangUserPortraitItemEntity
    {
        /// <summary>
        /// 文本
        /// </summary>
        public string label {  get; set; }
        /// <summary>
        /// 值，1表示100%
        /// </summary>
        public double value { get; set; }
    }
}
