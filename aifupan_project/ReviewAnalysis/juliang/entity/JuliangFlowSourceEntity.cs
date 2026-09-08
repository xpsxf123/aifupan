using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    public class JuliangFlowSourceEntity
    {
        /// <summary>
        /// 流量来源
        /// </summary>
        public string channelName {  get; set; }
        /// <summary>
        /// 流量占比，1表示100%
        /// </summary>
        public double ratio { get; set; }
        /// <summary>
        /// 子流量结构信息
        /// </summary>
        public List<JuliangFlowSourceEntity> subFlow {  get; set; }
    }
}
