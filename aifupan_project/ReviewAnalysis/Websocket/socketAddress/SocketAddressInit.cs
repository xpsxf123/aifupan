using douyin.Utils;
using ReviewAnalysis.socketAddress.browser;
using ReviewAnalysis.Websocket.Entity;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using static ReviewAnalysis.Websocket.socketAddress.SocketAddressInit;

namespace ReviewAnalysis.Websocket.socketAddress
{
    public static class SocketAddressInit
    {
        /// <summary>
        /// 成功获取websocket地址后的回调事件
        /// </summary>
        /// <param name="url"></param>
        /// <param name="ttwid"></param>
        /// <param name="wssUrl"></param>
        public delegate void GetWebsocketAddress(string url, string ttwid, string wssUrl);
        /// <summary>
        /// 获取websocket地址超时的回调事件
        /// </summary>
        /// <param name="url"></param>
        public delegate void GetWebsocketTimeout(string url);

        // 定义事件
        public static event GetWebsocketAddress getWebsocketAddress;
        public static event GetWebsocketTimeout getWebsocketTimeout;

        public static WebsocketPage websocketPage = null;
        public static bool isProxy = false;

        public static void init()
        {
            // 使用异步方式显示窗体，避免阻塞调用线程
            // 特别适用于从后台线程调用的情况
            try
            {
                if (System.Windows.Forms.Application.OpenForms.Count > 0)
                {
                    System.Windows.Forms.Application.OpenForms[0]?.BeginInvoke(new Action(() =>
                    {
                        try
                        {
                            if (websocketPage == null || websocketPage.IsDisposed)
                            {
                                websocketPage = new WebsocketPage();
                            }
                            if (!websocketPage.Visible)
                            {
                                websocketPage.Show();
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"初始化WebsocketPage失败：{ex.Message}", "SocketAddressInit");
                        }
                    }));
                }
                else
                {
                    // 如果没有主窗体，直接创建
                    if (websocketPage == null || websocketPage.IsDisposed)
                    {
                        websocketPage = new WebsocketPage();
                    }
                    if (!websocketPage.Visible)
                    {
                        websocketPage.Show();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"init异常：{ex.Message}", "SocketAddressInit");
            }
        }


        // 触发成功获取 WebSocket 地址的事件
        public static void TriggerGetWebsocketAddress(string url, string ttwid, string wssUrl)
        {
            getWebsocketAddress?.Invoke(url, ttwid, wssUrl);  // 使用 ?.Invoke 安全触发事件
        }

        // 触发获取 WebSocket 地址超时的事件
        public static void TriggerGetWebsocketTimeout(string url)
        {
            getWebsocketTimeout?.Invoke(url);  // 使用 ?.Invoke 安全触发事件
        }

        public static void setWebsocketAddress(WebsocketEntity websocket)
        {
            if (websocket.socketAddressType == 2)
            {
                if(websocketPage == null)
                {
                    FileUtils.log("采集websocket地址的页面没有初始化");
                    throw new Exception("页面没有初始化");
                }
                long time = websocket.websocketLinkErroeNum >= 1 || isProxy ? 90 * 1000 : 60 * 1000;
                bool proxy = websocket.websocketLinkErroeNum >= 1;
                websocketPage.addUrl(websocket.url, "DouYinLive", time, proxy);
            }
            else if (websocket.socketAddressType == 0)
            {
                // TODO
            }
            else if(websocket.socketAddressType == 1)
            {
                // TODO
            }
        }
    }
}
