using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.video
{
    public class RenameVideoBo
    {
        /// <summary>
        /// 视频Id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 新视频名称（不含扩展名）
        /// </summary>
        public string newVideoName { get; set; }
    }
}
