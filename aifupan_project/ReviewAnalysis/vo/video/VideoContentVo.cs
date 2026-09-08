using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.video
{
    public class VideoContentVo
    {

        /// <summary>
        /// 主键
        /// </summary>
        public string id { get; set; }

        /// <summary>
        /// 来源id
        /// </summary>
        public string sourceId { get; set; }

        /// <summary>
        /// 来源类型0视频、1文件、2对比分析
        /// </summary>
        public int sourceType { get; set; }

        /// <summary>
        /// 内容
        /// </summary>
        public string content { get; set; }

        /// <summary>
        /// 提示词
        /// </summary>
        public string cueWord { get; set; }

        /// <summary>
        /// 生成状态 0未生成，1已生成
        /// </summary>
        public int generateStatus { get; set; }

        /// <summary>
        /// 内容List
        /// </summary>
        public List<string> contentList { get; set; }

        /// <summary>
        /// 段落 从0开始
        /// </summary>
        public int paragraph { get; set; }

        /// <summary>
        /// 内容类型 1自然原文，2优化原文
        /// </summary>
        public int type { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public long? tradeId { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public long? userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId { get; set; }

    }
}
