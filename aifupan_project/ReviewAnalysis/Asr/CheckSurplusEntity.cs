using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class CheckSurplusEntity
    {
        /// <summary>
        /// id，用于加回余量
        /// </summary>
        public long Id { get; set; }
        /// <summary>
        /// secretId，用于加回余量
        /// </summary>
        public string SecretId { get; set; }
        /// <summary>
        /// 询问是否还有QPS服务器返回的code
        /// </summary>
        public int Code { get; set; }
        /// <summary>
        /// 询问是否还有QPS服务器返回的msg
        /// </summary>
        public string Msg { get; set; }
    }
}
