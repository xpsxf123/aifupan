using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class SceneSliceStatusVo
    {
        /// <summary>
        /// 状态: 0-需要切片 2-已完成
        /// </summary>
        public int status { get; set; }

        /// <summary>
        /// OSS预签名上传URL（status=0时返回）
        /// </summary>
        public string signedUrl { get; set; }

        /// <summary>
        /// OSS文件key（status=0时返回）
        /// </summary>
        public string ossKey { get; set; }

        /// <summary>
        /// 截取秒数（距视频结束的秒数）
        /// </summary>
        public int? sliceSeconds { get; set; }
    }
}
