using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.video
{
    public class UpdateVideoSizeDurationBo
    {
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 时长，秒
        /// </summary>
        public long? duration { get; set; }
        /// <summary>
        /// 文件大小，B
        /// </summary>
        public long? fileSize { get; set; }
        /// <summary>
        /// 录制结束时间
        /// </summary>
        public string endTime { get; set; }
        /// <summary>
        /// 是否在录制 0：否 1：是
        /// </summary>
        public int? isRecording { get; set; }
    }
}
