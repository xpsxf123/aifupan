using System;

namespace ReviewAnalysis.vo.anchor
{
    /// <summary>
    /// 授权用量信息 VO，对应后端 fupan-server AuthUsageVo
    /// </summary>
    /// <remarks>
    /// 用于 getAuthUsage 接口返回值，包含指定授权类型的授权总量、已用量及剩余量，
    /// 供客户端在发起授权前动态校验是否有可用授权位。
    /// </remarks>
    public class AuthUsageVo
    {
        /// <summary>
        /// 授权总量
        /// </summary>
        public long totalCount { get; set; }

        /// <summary>
        /// 已使用数量
        /// </summary>
        public long usedCount { get; set; }

        /// <summary>
        /// 剩余数量
        /// </summary>
        public long remainingCount { get; set; }
    }
}
