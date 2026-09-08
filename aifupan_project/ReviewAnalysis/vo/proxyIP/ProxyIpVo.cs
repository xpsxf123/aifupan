using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.proxyIP
{
    public class ProxyIpVo
    {
        /// <summary>
        /// 代理ip
        /// </summary>
        public string ip { get; set; }
        /// <summary>
        /// 代理端口
        /// </summary>
        public string port { get; set; }
        /// <summary>
        /// 代理IP账号
        /// </summary>
        public string proxyUsername { get; set; }
        /// <summary>
        /// 代理IP密码
        /// </summary>
        public string proxyPassword { get; set; }
        /// <summary>
        /// 过期时间戳
        /// </summary>
        public long expireTime { get; set; }
    }
}
