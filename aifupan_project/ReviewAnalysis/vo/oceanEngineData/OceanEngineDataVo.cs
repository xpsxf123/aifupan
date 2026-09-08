using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.oceanEngineData
{
    public class OceanEngineDataVo
    {
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId {  get; set; }

        /// <summary>
        /// 主播id
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 下载地址
        /// </summary>
        public string ossPath { get; set; }

        /// <summary>
        /// 汇总的数据
        /// </summary>
        public string dataJson { get; set; }

        /// <summary>
        /// 批次号
        /// </summary>
        public string batchNumber { get; set; }

    }

    public class OceanEngineDataDto
    {
        public string dateTime { get; set; }

        public string date { get; set; }

        public int payComboCnt { get; set; }

        public double salesCount { get; set; }

        public double uv { get; set; }

        /// <summary>
        /// 观看人次（本段）
        /// </summary>
        public int watchNum { get; set; }
    }
}
