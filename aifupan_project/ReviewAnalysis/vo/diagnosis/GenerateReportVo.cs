using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.diagnosis
{
    public class GenerateReportVo
    {

        public string videoId { get; set; }

        public int? sourceType { get; set; } = 0;

        public string fileName { get; set; }

        /// <summary>
        /// 类型 1-内容 2-数据
        /// </summary>
        public int? uploadType { get; set; } = 1;
    }
}
