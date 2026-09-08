using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Dto
{
    public class AnchorPageDto<T>  where T:class
    {

        /// <summary>
        /// 查询的总数
        /// </summary>
        public int Total { get; set; }

        /// <summary>
        /// 总页数
        /// </summary>
        public int PageTotal { get; set; }


        /// <summary>
        /// 数据列表
        /// </summary>
        public List<T> DataList { get; set; }

        /// <summary>
        /// 直播中的主播数量
        /// </summary>
        public int CurrentLiveNum { get; set; }

        /// <summary>
        /// 录制中的主播数量
        /// </summary>
        public int CurrentRecordNum { get; set; }

    }
}
