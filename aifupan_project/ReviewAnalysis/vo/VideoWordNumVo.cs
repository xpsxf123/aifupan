using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo
{
    public class VideoWordNumVo
    {
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId {  get; set; }
        /// <summary>
        /// 敏感词数量
        /// </summary>
        public int sensitiveNum { get; set; }
        /// <summary>
        /// 关键词数量
        /// </summary>
        public int cruxNum { get; set; }
    }
}
