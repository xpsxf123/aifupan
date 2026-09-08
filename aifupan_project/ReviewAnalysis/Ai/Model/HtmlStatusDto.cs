using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Ai.Model
{
    public class HtmlStatusDto
    {

        /// <summary>
        /// id
        /// </summary>
        public string id;

        /// <summary>
        /// 问ai的提示词
        /// </summary>
        public string content;

        /// <summary>
        /// 生成状态 0待生成，1生成中，2生成成功，3生成失败
        /// </summary>
        public int status { get; set; } = 0;




        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型 0视频，1文件，2对比分析
        /// </summary>
        public int? sourceType { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

        /// <summary>
        /// 助手类型
        /// </summary>
        public int? askType { get; set; }
    }
}
