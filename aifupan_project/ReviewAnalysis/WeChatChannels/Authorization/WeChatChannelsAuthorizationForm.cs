using CefSharp.WinForms;
using douyin.Utils;
using Microsoft.AspNetCore.WebUtilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.WeChatChannels.Common;
using ReviewAnalysis.WeChatChannels.Models;
using ReviewAnalysis.WeChatChannels.Services;
using ReviewAnalysis.WeChatChannels.Utils;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Threading.Tasks;
using System.Web;
using System.Windows.Forms;

namespace ReviewAnalysis.WeChatChannels.Authorization
{
    public partial class WeChatChannelsAuthorizationForm : Form
    {
        private bool _isAuthenticated = false;
        private string redirectUrl;
        private ChromiumWebBrowser _webBrowser;

        public WeChatChannelsAuthorizationForm()
        {
            InitializeComponent();
            InitializeForm();
        }

        /// <summary>
        /// 初始化窗口
        /// </summary>
        private void InitializeForm()
        {
            Owner = FrontNotice.formMain;   // 用于当前窗口打开时至于软件最上层，目的是调用 Show 方法是等用于 ShowDialog，因为调用 Show 方法不会阻塞当前线程。
            MinimizeBox = false;
            MaximizeBox = false;
            Icon = new Icon(FormMain.icoPath);
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(800, 700);
            Text = "微信视频号授权";

            // CEF 控件不能在界面上拖放过去，需要通过代码添加，界面上拖放会造成整个软件闪退。
            _webBrowser = new ChromiumWebBrowser();
            _webBrowser.Dock = DockStyle.Fill;
            _webBrowser.LoadingStateChanged += WebBrowser_LoadingStateChanged;

            Controls.Add(_webBrowser);

            FormClosed += WeChatChannelsAuthorizationForm_FormClosed;
        }

        private void WeChatChannelsAuthorizationForm_FormClosed(object sender, FormClosedEventArgs e)
        {
            HideAuthorization();
            Hide();
        }

        private void WebBrowser_LoadingStateChanged(object sender, CefSharp.LoadingStateChangedEventArgs e)
        {
            var currentWebBrowser = sender as ChromiumWebBrowser;
            FileUtils.LogRecrd($"{currentWebBrowser?.Address}", $"授权视频号成功后的跳转url");
            if (currentWebBrowser.Address != null &&
                currentWebBrowser.Address.StartsWith(redirectUrl))
            {
                var returnData = currentWebBrowser.Address.Substring(currentWebBrowser.Address.IndexOf("?") + 1);
                var authorizationResult = ParseReturnData(returnData);
                WeChatChannelsSerivce.SetLatelyAuthorizationResult(authorizationResult);
                _isAuthenticated = true;
                CallFrontNotice(authorizationResult);
                HideAuthorization();

                Invoke(new Action(() => { Hide(); }));  // 需要调用 Hide 方法。不能调用 Close 方法，Close 方法会造成整个软件闪退。
            }
        }

        private void CallFrontNotice(AuthorizationResultModel authorizationResult)
        {
            FrontNotice frontNotice = new FrontNotice();
            var requestDataObj = new Dictionary<string, object>();

            // 通知前端
            requestDataObj["code"] = 0;
            requestDataObj["status"] = 200;
            requestDataObj["action"] = "weChatChannelsAuthorizationResult";
            requestDataObj["data"] = authorizationResult;
            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            return;

        }

        /// <summary>
        /// 关闭授权窗口时的处理事项
        /// </summary>
        private void HideAuthorization()
        {
            WeChatChannelsUtils.IsOpenAuthorizationForm = false;
            if (!_isAuthenticated)
            {
                WeChatChannelsSerivce.ClearLatelyAuthorizationResult();
            }
        }

        /// <summary>
        /// 显示授权界面
        /// </summary>
        /// <param name="redirectUrl">授权成功的返回地址</param>
        public async void ShowAuthorization(string redirectUrl)
        {
            Show();

            this.redirectUrl = redirectUrl;
            await OpenAuthorizationUrl();
        }

        /// <summary>
        /// 分析视频号授权返回的授权数据
        /// </summary>
        private AuthorizationResultModel ParseReturnData(string returnData)
        {
            var accountData = QueryHelpers.ParseQuery(returnData);
            var authorizationResult = new AuthorizationResultModel();

            if (accountData.Count == 3 ||
                accountData.Count == 4)
            {
                authorizationResult.UserChannelId = int.Parse(accountData["userChannelId"]);

                authorizationResult.AuthorizerInfoId = int.Parse(accountData["authorizerInfoId"]);

                var accountBodyJson = HttpUtility.UrlDecode(accountData["accountBody"]);
                authorizationResult.AccountBody = JsonConvert.DeserializeObject<WeChatChannelsAccount>(accountBodyJson);

                return authorizationResult;
            }

            throw new Exception("视频号授权后返回的数据并非有效数据。");
        }

        /// <summary>
        /// 打开授权地址
        /// </summary>
        private async Task OpenAuthorizationUrl()
        {
            var authorizationUrl = await WeChatChannelsSerivce.BuildOpenAuthUrl(redirectUrl);

            await _webBrowser.LoadUrlAsync(authorizationUrl);
        }
    }
}
