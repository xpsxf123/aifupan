using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.vo
{
    public class OnlineNumVo
    {
        /// <summary>
        /// 记录的时间
        /// </summary>
        [JsonProperty("recordDate")]
        public string RecordDate { get; set; }

        /// <summary>
        /// 在线人数
        /// </summary>
        [JsonProperty("peopleNum")]
        public string PeopleNum { get; set; }
    }
}
