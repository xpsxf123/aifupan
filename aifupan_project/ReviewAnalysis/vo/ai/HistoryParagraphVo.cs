using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class HistoryParagraphVo
    {

        /// <summary>
        /// 助手类型 0运营助手 1违规助手
        /// </summary>
        public int type { get; set; }

        /// <summary>
        /// 历史段落的code 
        /// </summary>
        public string code { get; set; }

        /// <summary>
        /// 别名 
        /// </summary>
        public string alias { get; set; }

        /// <summary>
        /// 段落内容
        /// </summary>
        public string content { get; set; }

        /// <summary>
        /// 来源的id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 数据类型 0视频，1文件，2对比分析
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate { get; set; }

    }
}
