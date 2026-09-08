using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.barrage
{
    public class BarrageDataVo
    {

        public List<BarrageData> barrageDataList {  get; set; }

        public int? totalBarrageNum { get; set; }

    }

    public class BarrageData
    {
        public string dateTime { get; set; }

        public string date {  get; set; }

        public int barrageNum { get; set; }
    }

    public class BarrageDoubleData
    {
        public string dateTime { get; set; }

        public string date { get; set; }

        public double value { get; set; }
    }
}
