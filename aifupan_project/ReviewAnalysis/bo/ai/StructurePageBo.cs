using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.ai
{
    public class StructurePageBo
    {
        /// <summary>
        /// 类型
        /// </summary>
        public int type;

        /// <summary>
        /// 资源id
        /// </summary>
        public string sourceId;

        /// <summary>
        /// 资源类型
        /// </summary>
        public int sourceType;

        /// <summary>
        /// 单前页
        /// </summary>
        public int? pageIndex;

        /// <summary>
        /// 总条数
        /// </summary>
        public int? pageSize;

    }
}
