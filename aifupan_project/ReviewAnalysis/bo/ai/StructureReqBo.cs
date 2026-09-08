using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class StructureReqBo
    {

        /// <summary>
        /// 保存到文件的内容
        /// </summary>
        public List<ContentObj> contentList { get; set; }

        /// <summary>
        /// 来源ID
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 数据类型 0视频，1文件，2对比分析
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 助手类型 0运营助手 1违规助手
        /// </summary>
        public int type { get; set; }


    }

    public class ContentObj
    {
        
        public string code { get; set; }

        public string content { get; set; }
    }
}
