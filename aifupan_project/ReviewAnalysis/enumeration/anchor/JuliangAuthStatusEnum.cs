using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.enumeration.anchor
{
    /// <summary>
    /// 巨量授权状态
    /// </summary>
    public class JuliangAuthStatusEnum
    {
        /// <summary>
        /// 未授权
        /// </summary>
        public static int unAuth = 0;
        /// <summary>
        /// 已授权
        /// </summary>
        public static int auth = 1;
        /// <summary>
        /// 授权过期
        /// </summary>
        public static int authExpires = 2;
        /// <summary>
        /// 授权失败
        /// </summary>
        public static int authError = 3;
        /// <summary>
        /// 授权中
        /// </summary>
        public static int authing = 4;
        /// <summary>
        /// 授权抖音号不匹配
        /// </summary>
        public static int accountMismatched = 5;
        /// <summary>
        /// 子账号无权限
        /// </summary>
        public static int subNoPermission = 6;
    }
}
