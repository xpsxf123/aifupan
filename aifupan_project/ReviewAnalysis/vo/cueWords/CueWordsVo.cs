using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.cueWords
{
    /// <summary>
    /// 提示词信息
    /// </summary>
    public class CueWordsVo
    {
        /// <summary>
        /// ID
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 行业id，0表示全行业
        /// </summary>
        public long? tradeId { get; set; }

        /// <summary>
        /// 来源类型 0：系统
        /// </summary>
        public int? resourceType { get; set; }

        /// <summary>
        /// 提示词
        /// </summary>
        public string cueWord { get; set; }

        /// <summary>
        /// 实际提示词：具体问题
        /// </summary>
        public string problem { get; set; }

        /// <summary>
        /// 提示词用于：0：单个分析，1：对比分析
        /// </summary>
        public int? applyTo { get; set; }

        /// <summary>
        /// 提示词类型0: 运营提示词，1：违规提示词 2：对比复盘提示词
        /// </summary>
        public int? cueType { get; set; }

        /// <summary>
        /// 范围 0:全文，1:段落
        /// </summary>
        public int? scope { get; set; }

        /// <summary>
        /// 场景 0:直接提示，1:弹框操作
        /// </summary>
        public int? scene { get; set; }

        /// <summary>
        /// 提示词在当前行业排序
        /// </summary>
        public int? sort { get; set; }

        /// <summary>
        /// 描述
        /// </summary>
        public string remarks { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public DateTime createDate { get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        public DateTime updateDate { get; set; }
    }
}
