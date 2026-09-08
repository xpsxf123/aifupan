using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    public class CruxTypeInfoVo : CruxTypeVo
    {
        /// <summary>
        /// 父级id数组
        /// </summary>
        public List<string> parentIdArr { get; set; }

        /// <summary>
        /// 层级id数组
        /// </summary>
        public List<string> idArr { get; set; }

        /// <summary>
        /// 父级名称数组
        /// </summary>
        public List<string> parentNameArr { get; set; }

        /// <summary>
        /// 层级名称数组
        /// </summary>
        public List<string> nameArr { get; set; }
    }
}
