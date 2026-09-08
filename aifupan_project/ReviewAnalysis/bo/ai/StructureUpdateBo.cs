using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo
{
    public class StructureUpdateBo
    {
        /// <summary>
        /// 助手类型 0运营助手 1违规助手
        /// </summary>
        public int type { get; set; }

        /// <summary>
        /// 来源的id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 数据类型 0视频，1文件，2对比分析
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// code
        /// </summary>
        public string code;

        /// <summary>
        /// 点赞状态 0点赞，1踩
        /// </summary>
        public int giveStatuc;
    }
}
