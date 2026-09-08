using System;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号Cookie DTO
    /// </summary>
    public class EnterpriseCookieDto
    {
        /// <summary>
        /// Cookie名称
        /// </summary>
        public string name { get; set; }
        
        /// <summary>
        /// Cookie值
        /// </summary>
        public string value { get; set; }
        
        /// <summary>
        /// 域名
        /// </summary>
        public string domain { get; set; }
        
        /// <summary>
        /// 路径
        /// </summary>
        public string path { get; set; }
        
        /// <summary>
        /// 过期时间
        /// </summary>
        public DateTime? expires { get; set; }
        
        /// <summary>
        /// 是否仅HTTP
        /// </summary>
        public bool httpOnly { get; set; }
        
        /// <summary>
        /// 是否安全
        /// </summary>
        public bool secure { get; set; }
    }
}
