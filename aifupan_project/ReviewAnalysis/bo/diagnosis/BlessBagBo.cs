using System;
using Newtonsoft.Json;

namespace ReviewAnalysis.bo.diagnosis
{
    public class BlessBagBo
    {
        /// <summary>
        /// 福袋主键
        /// </summary>
        public long id { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public long userId { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long tenantId { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 福袋信息
        /// </summary>
        [JsonProperty(PropertyName = "lotteryInfo")]
        public string lotteryInfo { get; set; }

        /// <summary>
        /// 参与抽奖的条件
        /// </summary>
        public string conditions { get; set; }

        /// <summary>
        /// 总奖品数量
        /// </summary>
        public int prizeCount { get; set; }

        /// <summary>
        /// 福袋数量
        /// </summary>
        public int luckyCount { get; set; }

        /// <summary>
        /// 倒计时（单位：秒）
        /// </summary>
        public int countDown { get; set; }

        /// <summary>
        /// 抽奖开始时间
        /// </summary>
        public long startTime { get; set; }

        /// <summary>
        /// 抽奖结束时间
        /// </summary>
        public long drawTime { get; set; }

        /// <summary>
        /// 当前时间
        /// </summary>
        public long currentTime { get; set; }

        /// <summary>
        /// 参与人数
        /// </summary>
        public int candidateNum { get; set; }

        /// <summary>
        /// 当前时间
        /// </summary>
        public long now { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public DateTime createDate { get; set; }

        /// <summary>
        /// 修改时间
        /// </summary>
        public DateTime updateDate { get; set; }

        /// <summary>
        /// 是否已删除
        /// </summary>
        public int isDeleted { get; set; }
    }
} 