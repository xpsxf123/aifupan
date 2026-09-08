using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台授权状态枚举
    /// </summary>
    public enum AnchorLiveAuthStatusEnum
    {
        /// <summary>
        /// 未授权
        /// </summary>
        unAuth = 0,
        
        /// <summary>
        /// 已授权
        /// </summary>
        auth = 1,
        
        /// <summary>
        /// 授权过期
        /// </summary>
        authExpires = 2,
        
        /// <summary>
        /// 需要刷新
        /// </summary>
        needRefresh = 3
    }
}
