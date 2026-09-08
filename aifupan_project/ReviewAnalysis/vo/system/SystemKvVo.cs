using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.system
{
    public class SystemKvVo
    {

        /// <summary>
        /// id
        /// </summary>
        public long? id { get; set; }

        /// <summary>
        /// code
        /// </summary>
        public string kvKey { get; set; }

        /// <summary>
        /// value 
        /// </summary>
        public string kvValue { get; set; }

        /// <summary>
        /// 备注 
        /// </summary>
        public string remarks { get; set; }

        /// <summary>
        /// 创建时间 
        /// </summary>
        public string createDate { get; set; }

        /// <summary>
        /// 最后修改时间 
        /// </summary>
        public string updateDate { get; set; }


    }
}
