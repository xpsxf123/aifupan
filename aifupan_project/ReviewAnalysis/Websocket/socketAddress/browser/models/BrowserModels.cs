using CefSharp.WinForms;
using System;
using System.Timers;

namespace ReviewAnalysis
{
    public class BrowserModels
    {
        /// <summary>
        /// web加加载的url
        /// </summary>
        public string url { get; set; }

        /// <summary>
        /// url类型，DouYinLive抖音直播，KuaiShouLive快手直播，RedeLightLive小红书直播
        /// </summary>
        public string type { get; set; }

        /// <summary>
        /// 谷歌控件
        /// </summary>
        public ChromiumWebBrowser chromiumWeb {  get; set; }

        /// <summary>
        /// 超时处理-定时任务
        /// </summary>
        public Timer timeoutTimer { get; set; }

        /// <summary>
        /// 状态 0待开始获取websocket地址，1获取websocket地址中，2完成
        /// </summary>
        public int status { get; set; }

        public string ttwid {  get; set; }

        public string wssUrl { get; set; }

        public string cachePath { get; set; }
    }
}
