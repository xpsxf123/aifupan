using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.system
{
    public class DictDataListVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 字典类型id
        /// </summary>
        public long? typeId { get; set; }

        /// <summary>
        /// 字典label
        /// </summary>
        public string label { get; set; }

        /// <summary>
        /// 字典value
        /// </summary>
        public string value { get; set; }

        /// <summary>
        /// 状态 0：启用 1：禁用
        /// </summary>
        public int? status { get; set; }

        /// <summary>
        /// 排序
        /// </summary>
        public int? sort { get; set; }

        /// <summary>
        /// 字典类型
        /// </summary>
        public string typeName { get; set; }
    }
}
