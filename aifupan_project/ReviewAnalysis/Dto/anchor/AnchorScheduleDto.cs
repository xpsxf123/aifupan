using ReviewAnalysis.Model;

namespace ReviewAnalysis.Dto.anchor
{
    /// <summary>
    /// 排班数据传输对象
    /// </summary>
    public class AnchorScheduleDto
    {
        /// <summary>
        /// 平台类型 DOU_YIN, KUAISHOU, WEIXIN
        /// </summary>
        public string platformType { get; set; }

        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 带货间ID
        /// </summary>
        public long? governanceRoomId { get; set; }

        /// <summary>
        /// 工作日期 格式：yyyy-MM-dd
        /// </summary>
        public string workDay { get; set; }

        /// <summary>
        /// 开始工作时间 格式：HH:mm:ss
        /// </summary>
        public string startWork { get; set; }

        /// <summary>
        /// 结束工作时间 格式：HH:mm:ss
        /// </summary>
        public string endWork { get; set; }

        /// <summary>
        /// 排班时长（分钟）
        /// </summary>
        public int? scheduleDuration { get; set; }

        /// <summary>
        /// 转换为排班实体
        /// </summary>
        /// <returns>排班实体</returns>
        public AnchorSchedule ToAnchorSchedule()
        {
            return new AnchorSchedule
            {
                ScheduleId = $"{secUid}_{workDay}_{startWork}",
                SecUid = secUid,
                ScheduleDate = workDay,
                StartTime = startWork,
                EndTime = endWork,
                PlatformType = platformType,
                GovernanceRoomId = governanceRoomId,
                ScheduleDuration = scheduleDuration
            };
        }
    }

    /// <summary>
    /// API响应基类
    /// </summary>
    /// <typeparam name="T">数据类型</typeparam>
    public class ApiResponse<T>
    {
        /// <summary>
        /// 响应码 0=成功
        /// </summary>
        public int code { get; set; }

        /// <summary>
        /// 响应消息
        /// </summary>
        public string msg { get; set; }

        /// <summary>
        /// 响应数据
        /// </summary>
        public T data { get; set; }

        /// <summary>
        /// 异常信息
        /// </summary>
        public string exception { get; set; }

        /// <summary>
        /// 时间戳
        /// </summary>
        public long timestamp { get; set; }
    }
}