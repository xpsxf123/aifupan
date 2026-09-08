using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.sliceVideo
{
    public class SaveSliceCorrelationDataBo
    {
        /// <summary>
        /// 切片视频所属原视频id
        /// </summary>
        public string sourceVideoId {  get; set; }
        /// <summary>
        /// 切片视频id
        /// </summary>
        public string sliceVideoId { get; set; }
        /// <summary>
        /// 切片视频自然时间戳-开始
        /// </summary>
        public long? sliceStartNaturalTime { get; set; }
        /// <summary>
        /// 切片视频自然时间戳-结束
        /// </summary>
        public long? sliceEndNaturalTime { get; set; }
    }
}
