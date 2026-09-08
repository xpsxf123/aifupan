using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.bo.blessBag
{
    public class BlessBagBo
    {
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是采集的直播的房间Id, room_id
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 福袋信息-json
        /// </summary>
        public string lotteryInfo { get; set; }

        /// <summary>
        /// 参与抽奖的条件-json
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
    }
}
