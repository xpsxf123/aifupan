using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    /// <summary>
    /// 违规警告信息实体（罗盘API）
    /// </summary>
    public class JuliangViolationEntity
    {
        /// <summary>
        /// 违规单号
        /// </summary>
        public string penalizeId { get; set; }

        /// <summary>
        /// 处罚等级
        /// </summary>
        public string punishLevel { get; set; }

        /// <summary>
        /// 处罚原因
        /// </summary>
        public string punishReason { get; set; }

        /// <summary>
        /// 处罚结果
        /// </summary>
        public string punishResult { get; set; }

        /// <summary>
        /// 整改建议
        /// </summary>
        public string punishSuggestion { get; set; }

        /// <summary>
        /// 处罚时间(时间戳)
        /// </summary>
        public long? punishTime { get; set; }

        /// <summary>
        /// 违规视频封面
        /// </summary>
        public string punishVideoCover { get; set; }

        /// <summary>
        /// 违规直播间ID
        /// </summary>
        public string roomId { get; set; }

        /// <summary>
        /// 违规视频m3u8播放链接
        /// </summary>
        public string videoLink { get; set; }
    }
}
