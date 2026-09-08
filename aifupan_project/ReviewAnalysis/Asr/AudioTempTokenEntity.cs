using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class AudioTempTokenEntity
    {
        /// <summary>
        /// 临时token
        /// </summary>
        public string Token { get; set; }
        /// <summary>
        /// 临时SecretId
        /// </summary>
        public string TempSecretId { get; set; }
        /// <summary>
        /// 临时SecretKey
        /// </summary>
        public string TempSecretKey { get; set; }
    }
}
