using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.hotSearch
{
    /// <summary>
    /// 热搜邮箱账号返回VO
    /// </summary>
    public class HotSearchEmailAccountVo
    {
        /// <summary>
        /// 账号ID
        /// </summary>
        public string accountId { get; set; }
        /// <summary>
        /// 账号ID
        /// </summary>
        public string email { get; set; }
        /// <summary>
        /// 邮箱密码
        /// </summary>
        public string emailPassword { get; set; }
        /// <summary>
        /// 账号类型
        /// </summary>
        public int accountType { get; set; }
        /// <summary>
        /// 账号类型名称
        /// </summary>
        public string accountTypeName { get; set; }
        /// <summary>
        /// 账号归属城市
        /// </summary>
        public string accountCity { get; set; }
        /// <summary>
        /// 客户端城市
        /// </summary>
        public string clientCity { get; set; }
        /// <summary>
        /// 是否同城
        /// </summary>
        public bool isSameCity { get; set; }
        /// <summary>
        /// 使用超时时间（分钟），超过此时间账号将自动释放
        /// </summary>
        public int useTimeoutMinutes { get; set; }
    }
}
