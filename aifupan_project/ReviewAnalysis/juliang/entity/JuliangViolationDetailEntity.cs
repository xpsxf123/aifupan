using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang.entity
{
    /// <summary>
    /// 违规详情实体（巨量百应API）
    /// </summary>
    public class JuliangViolationDetailEntity
    {
        /// <summary>
        /// 违规单号
        /// </summary>
        public string taskId { get; set; }

        /// <summary>
        /// 处罚对象类型（4达人/5短视频/6直播间）
        /// </summary>
        public int? objectType { get; set; }

        /// <summary>
        /// 处罚对象id
        /// </summary>
        public string objectId { get; set; }

        /// <summary>
        /// 违规状态（1已违规/2已撤销/3已预警）
        /// </summary>
        public int? violationStatus { get; set; }

        /// <summary>
        /// 违规原因
        /// </summary>
        public string violationReason { get; set; }

        /// <summary>
        /// 处罚结果
        /// </summary>
        public string penalizeResult { get; set; }

        /// <summary>
        /// 申诉状态
        /// </summary>
        public int? appealStatus { get; set; }

        /// <summary>
        /// 判罚时间(时间戳)
        /// </summary>
        public long? penalizeTime { get; set; }

        /// <summary>
        /// 详细违规点
        /// </summary>
        public string violationDesc { get; set; }

        /// <summary>
        /// 违规位置
        /// </summary>
        public string proofPoint { get; set; }

        /// <summary>
        /// 违规句
        /// </summary>
        public string proofSentence { get; set; }

        /// <summary>
        /// 违规视频链接
        /// </summary>
        public string videoUrl { get; set; }

        /// <summary>
        /// 违规视频封面
        /// </summary>
        public string coverImg { get; set; }

        /// <summary>
        /// 是否是直播片段
        /// </summary>
        public bool? isLivingCut { get; set; }

        /// <summary>
        /// 违规视频开始时间
        /// </summary>
        public long? liveVideoStartTime { get; set; }

        /// <summary>
        /// 违规视频结束时间
        /// </summary>
        public long? liveVideoEndTime { get; set; }

        /// <summary>
        /// 整改建议
        /// </summary>
        public string suggestion { get; set; }
    }
}
