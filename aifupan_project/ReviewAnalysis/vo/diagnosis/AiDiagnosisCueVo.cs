using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.diagnosis
{
    public class AiDiagnosisCueVo
    {

        /// <summary>
        /// 模型id
        /// </summary>
        public long? modelId { get; set; }

        /// <summary>
        /// 提示词集合
        /// </summary>
        public List<ListDiagnosisCueVo> list { get; set; }
    }

    public class ListDiagnosisCueVo
    {
        /// <summary>
        /// 提示词类型 
        /// </summary>
        public int cueType { get; set; }

        /// <summary>
        /// 标签名称 
        /// </summary>
        public string tagName { get; set; }

        /// <summary>
        /// id 
        /// </summary>
        public List<CueWords> cueWordsList { get; set; }
    }

    /// <summary>
    /// ai诊断提示词配置信息项-提示词 
    /// </summary>
    public class CueWords
    {
        /// <summary>
        /// 提示词id 
        /// </summary>
        public long cueWordsId { get; set; }

        /// <summary>
        /// 行业id，0表示全行业 
        /// </summary>
        public long tradeId { get; set; }

        /// <summary>
        /// 提示词 
        /// </summary>
        public string cueWord { get; set; }

        /// <summary>
        /// 提示词在当前行业排序 
        /// </summary>
        public int sort { get; set; }

        /// <summary>
        /// 主播的是否选中 
        /// </summary>
        public int anchorSelect { get; set; }

        /// <summary>
        /// 视频的是否选中 
        /// </summary>
        public int videoSelect { get; set; }

        /// <summary>
        /// 状态：0待分析，1分析中，2分析完成
        /// </summary>
        public int status { get; set; } = 0;
    }
}
