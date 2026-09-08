using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    public class PagedResult
    {
        /// <summary>
        /// 内容
        /// </summary>
        public List<string> Lines { get; set; } = new List<string>();

        /// <summary>
        /// 是否有上一页
        /// </summary>
        public bool HasPreviousPage { get; set; } = false;

    }
}
