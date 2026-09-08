using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台 Cookie DTO
    /// </summary>
    [Serializable]
    public class AnchorLiveCookieDto
    {
        /// <summary>
        /// Cookie 名称
        /// </summary>
        public string Name { get; set; }
        
        /// <summary>
        /// Cookie 值
        /// </summary>
        public string Value { get; set; }
        
        /// <summary>
        /// Cookie 域名
        /// </summary>
        public string Domain { get; set; }
        
        /// <summary>
        /// Cookie 路径
        /// </summary>
        public string Path { get; set; }
        
        /// <summary>
        /// 过期时间
        /// </summary>
        public DateTime? Expires { get; set; }
        
        /// <summary>
        /// 是否仅 HTTPS
        /// </summary>
        public bool Secure { get; set; }
        
        /// <summary>
        /// 是否仅 HTTP
        /// </summary>
        public bool HttpOnly { get; set; }
    }
}
