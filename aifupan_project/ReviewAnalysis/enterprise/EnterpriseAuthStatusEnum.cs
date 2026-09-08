namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号授权状态枚举
    /// </summary>
    public enum EnterpriseAuthStatusEnum
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
        /// 授权已过期
        /// </summary>
        authExpires = 2,
        
        /// <summary>
        /// 授权失败
        /// </summary>
        authFailed = 3,
        
        /// <summary>
        /// 授权中
        /// </summary>
        authing = 4,
        
        /// <summary>
        /// 账号不匹配
        /// </summary>
        accountMismatched = 5,
        
        /// <summary>
        /// 子账号无权限
        /// </summary>
        subNoPermission = 6
    }
}
