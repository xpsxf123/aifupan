using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.vo.barrage;

namespace ReviewAnalysis.Dto
{
    public class OnlineChartDto
    {
        /// <summary>
        /// 累计观看人数
        /// </summary>
        public int? totalViewersNum { get; set; }

        /// <summary>
        /// 最大在线人数
        /// </summary>
        public int? maxOnlineNum { get; set; }

        /// <summary>
        /// 弹幕总数
        /// </summary>
        public int? totalBarrageNum {  get; set; }

        /// <summary>
        /// 进场人数折线数据
        /// </summary>
        public List<LineChartData> approachDataList { get; set; }

        /// <summary>
        /// 在线人数折线数据
        /// </summary>
        public List<LineChartData> onlineDataList { get; set; }

        /// <summary>
        /// 弹幕曲线
        /// </summary>
        public List<LineChartData> barrageDataList { get; set; }
    }

    public class LineChartData
    {
        /// <summary>
        /// 时间
        /// </summary>
        public long dateTime;

        /// <summary>
        /// 时间-不是自然时间
        /// </summary>
        public long dateTimeNew;

        /// <summary>
        /// 时间对应的值
        /// </summary>
        public int valueNum;
    }
}
