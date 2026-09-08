using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.ai
{
    public class ConversationPageBo
    {

        /// <summary>
        /// 类型
        /// </summary>
        public int type {  get; set; }

        /// <summary>
        /// 资源id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 资源类型
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 单前页
        /// </summary>
        public int? page {  get; set; }

        /// <summary>
        /// 总条数
        /// </summary>
        public int? limit { get; set; }

    }
}
