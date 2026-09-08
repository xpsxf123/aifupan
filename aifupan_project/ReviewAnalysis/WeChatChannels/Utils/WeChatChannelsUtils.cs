using ReviewAnalysis.WeChatChannels.Authorization;

namespace ReviewAnalysis.WeChatChannels.Utils
{
    /// <summary>
    /// 微信视频号工具类
    /// </summary>
    public static class WeChatChannelsUtils
    {
        private static readonly object _lock = new object();
        private static WeChatChannelsAuthorizationForm _authorizationForm;  // 窗口必须设置为静态对象，防止被释放。

        /// <summary>
        /// 是否已打开授权窗口，仅能打开1个窗口。
        /// </summary>
        public static volatile bool IsOpenAuthorizationForm = false;

        /// <summary>
        /// 打开微信直播号授权地址
        /// </summary>
        /// <param name="redirectUrl">授权成功的返回地址</param>
        public static void OpenAuthorizationUrl(string redirectUrl)
        {
            lock (_lock)
            {

                if (!IsOpenAuthorizationForm)
                {
                    IsOpenAuthorizationForm = true;
                    _authorizationForm = new WeChatChannelsAuthorizationForm();
                    _authorizationForm.ShowAuthorization(redirectUrl);

                    // 每次打开视频号窗口后必须 new WeChatChannelsAuthorizationForm，否者下次打开窗口会闪退。
                    //_authorizationForm = new WeChatChannelsAuthorizationForm();
                }

            }
        }

    }
}
