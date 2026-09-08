namespace ReviewAnalysis.vo.config
{
    public class ClientVersionConfigVo
    {
        /// <summary>
        /// 客户端版本
        /// </summary>
        public string clientVersion { get; set; }
        
        /// <summary>
        /// 是否选择版本 0-不选版本，1-选择版本
        /// </summary>
        public int? isVersionSelect { get; set; }
        
        /// <summary>
        /// 账号类型
        /// </summary>
        public int? userType { get; set; }
        
        /// <summary>
        /// 页面类型 0-设置页面，1-登录后页面
        /// </summary>
        public int? pageType { get; set; }
    }
}