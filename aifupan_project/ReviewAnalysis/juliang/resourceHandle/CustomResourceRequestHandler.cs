using CefSharp;
using CefSharp.DevTools.IndexedDB;
using douyin.Utils;
using juliang;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.enumeration.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Model;
using ReviewAnalysis.socketAddress.browser;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using Swan;
using Swan.Formatters;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Windows;
using System.Windows.Forms;

namespace ReviewAnalysis.juliang
{
    public class CustomResourceRequestHandler : IResourceRequestHandler
    {

        private JuliangForm juliangForm;

        public CustomResourceRequestHandler(JuliangForm juliangForm)
        {
            this.juliangForm = juliangForm;
        }

        /// <summary>
        /// 当浏览器接收到资源的 HTTP 响应头后触发的过滤器
        /// </summary>
        public IResponseFilter GetResourceResponseFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            CustomResponseFilter customResponseFilter = new CustomResponseFilter();
            CustomResponseFilterManager.AddFilter(request.Identifier.ToString(), customResponseFilter);
            return customResponseFilter;

        }


        /// <summary>
        /// 资源加载完成时触发
        /// </summary>
        public void OnResourceLoadComplete(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, UrlRequestStatus status, long receivedContentLength)
        {

            string key = "";
            string url_str = "";

            try
            {
                url_str = request.Url.ToLower();

                string videoId = juliangForm.videoId;
                string roomId = juliangForm.roomId;
                AnchorInfo anchorInfo = juliangForm.anchorInfo;

                //FileUtils.log($"url======={url_str}");

                if (url_str.Contains("/account/login?code="))
                {
                    // 扫码授权成功,隐藏登录授权窗口
                    FileUtils.LogRpa($"{anchorInfo?.AnchorName}", $"扫码授权成功,隐藏登录授权窗口");
                    juliangForm.hideForm();
                    juliangForm.loginSuccess = true;
                }
                if (url_str.Contains("/workbench/info?"))
                {
                    // 登录成功
                    if (juliangForm.authType == 1)
                    {
                        juliangForm.hideForm();
                    }
                    juliangForm.loginSuccess = true;
                    //juliangForm.bloadHomeFinishHandle = true;
                    //juliangForm.loadHomeFinishHandle(anchorInfo);
                    //loginSuccessHandle(anchorInfo);
                }
                //else if (url_str.Contains("buyin.jinritemai.com/dashboard"))
                //{
                //    juliangForm.hideForm();
                //    juliangForm.loginSuccess = true;
                //}
                else if (url_str.Contains("web/common?mstoken=") || url_str.Contains("api/v1/config"))
                {
                    juliangForm.hideForm();
                    juliangForm.loginSuccess=true;
                    // 加载首页完成
                    if (juliangForm.loginSuccess)
                    {
                        //juliangForm.bloadHomeFinishHandle = true;
                        //juliangForm.loadHomeFinishHandle(anchorInfo);
                    }
                }
                else if (url_str.Contains("buyin.jinritemai.com/dashboard"))
                {
                    juliangForm.hideForm();
                    juliangForm.loginSuccess = true;
                    // 加载首页完成
                    //juliangForm.bloadHomeFinishHandle = true;
                    //juliangForm.loadHomeFinishHandle(anchorInfo);
                }

                //临时放开
                //else if (url_str.Contains("mcs.zijieapi.com/v1/list") || url_str.Contains("mcs.zijieapi.com/list"))
                //{
                //    // 加载直播大屏
                //    if (juliangForm.allowLoadDataScreen && juliangForm.CheckAuthStatusByCookie() == JuliangAuthStatusEnum.auth && !string.IsNullOrEmpty(juliangForm.roomId))
                //    {
                //        juliangForm.allowLoadDataScreen = false;
                //        juliangForm.firstLoadMajorData = true;
                //        juliangForm.openDataScreen(2);
                //    }
                //}
                //else if (url_str.Contains("/basic_live_screen/base_info?"))
                //{
                //    // 大屏数据-基础版
                //    key = JuliangDataKeyTypeEnum.bigScreenDataBase;

                //    if (juliangForm.isTemp)
                //    {
                //        juliangForm.tempLoadDataFlagSet.Add(JuliangDataKeyTypeEnum.bigScreenDataBase);
                //        juliangForm.openDataScreen(1);
                //    }
                //}
                //else if (url_str.Contains("/live_screen/core_data?"))
                //{
                //    // 专业版大屏数据
                //    if (juliangForm.firstLoadMajorData)
                //    {
                //        // 第一次加载专业版大屏数据，加载基础版大屏
                //        juliangForm.firstLoadMajorData = false;
                //        juliangForm.openDataScreen(0);
                //    }
                //    else
                //    {
                //        key = JuliangDataKeyTypeEnum.bigScreenDataPro;
                //        if (juliangForm.isTemp)
                //        {
                //            juliangForm.tempLoadDataFlagSet.Add(JuliangDataKeyTypeEnum.bigScreenDataPro);
                //        }
                //    }
                //}
                //else if (url_str.Contains("/basic_live_screen/flow_order_source?"))
                //{
                //    // 流量结构
                //    key = JuliangDataKeyTypeEnum.flowOrderSource;
                //    if (juliangForm.isTemp)
                //    {
                //        juliangForm.tempLoadDataFlagSet.Add(JuliangDataKeyTypeEnum.flowOrderSource);
                //    }
                //}
                //else if (url_str.Contains("/basic_live_screen/user_portrait?"))
                //{
                //    // 用户画像
                //    key = userPortraitHandle(url_str, request.Identifier.ToString());
                //}

                //-------------------------------------------------

                var data = getData(request.Identifier.ToString());
                if (string.IsNullOrEmpty(data))
                {
                    return;
                }
                //FileUtils.LogRpaAsync($"{data}", $"巨量百应所有Log==={anchorInfo?.AnchorName}");
                //if ((data.Contains("\"msg\":\"系统繁忙，请稍后重试\"") && data.Contains("\"code\":5001")) && juliangForm.anchorInfo.juliangAuthStatus == 1 && !string.IsNullOrEmpty(juliangForm.roomId) && juliangForm.checkAuthFileExist())
                //{
                //    FileUtils.LogRpa($"{data}", $"授权登录已过期==={anchorInfo?.AnchorName}");
                //    loginExpire(anchorInfo, JuliangAuthStatusEnum.authExpires);
                //    return;
                //}
                if (
                        anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth &&
                        (
                            (data.Contains("\"msg\":\"达人没有直播间权限\"") && data.Contains("\"st\":10014")) ||
                            (data.Contains("\"msg\":\"达人没有直播间权限\"") && data.Contains("\"st\":625"))
                        )
                    )
                {
                    FileUtils.LogRpa($"{data}", $"授权的抖音号不匹配==={anchorInfo?.AnchorName}");
                    accountMismatched(anchorInfo);
                    return;
                }

                //if (
                //        anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth &&
                //        (
                //            (data.Contains("\"msg\":\"未登录\"") && data.Contains("\"st\":10005"))
                //        )
                //    )
                //{
                //    FileUtils.LogRpa($"{data}", $"授权登录已过期==={anchorInfo?.AnchorName}");
                //    loginExpire(anchorInfo, JuliangAuthStatusEnum.authExpires);
                //    return;
                //}

                //if (data.Contains("您的登录状态已过期") || data.Contains("当前账号类型不支持访问"))
                //{
                //    FileUtils.LogRpa($"{data}", $"授权登录已过期==={anchorInfo?.AnchorName}");
                //    loginExpire(anchorInfo, JuliangAuthStatusEnum.authExpires);
                //    return;
                //}

                //if(anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth && data.Contains("\"msg\":\"子账号无权限\"") && data.Contains("\"st\":10008"))
                //{
                //    FileUtils.LogRpa($"{data}", $"子账号无权限==={anchorInfo?.AnchorName}");
                //    loginExpire(anchorInfo, JuliangAuthStatusEnum.subNoPermission);
                //    return;
                //}


                if (data.Contains(anchorInfo.SecUid) || data.Contains(anchorInfo.AnchorUserId))
                {
                    // 当前扫码的抖音号跟主播一致
                    juliangForm.scanObjectAuthSuccess = true;
                }

                if(url_str.Contains("buyin.jinritemai.com/passport/account/info/v2/?fp"))
                {
                    // 有登录标识，表示当前本地没有缓存
                    juliangForm.loginSuccessFlag = true;
                }

                if(data.Contains("用户未登录") 
                    && !url_str.Contains("monitor_web/settings/browser-settings") 
                    && anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.authError
                    && anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                {
                    // 当前扫码的抖音号没有巨量百应的权限
                    scanFailHandle(anchorInfo);
                }


                // 拿到响应数据
                //if (key.Length > 1)
                //{
                //    DataCollectEventArgs args = new DataCollectEventArgs()
                //    {
                //        anchorName = anchorInfo.AnchorName,
                //        secUid = anchorInfo.SecUid,
                //        batchNumber = roomId,
                //        videoId = videoId,
                //        key = key,
                //        data = data,
                //        juliangForm = juliangForm
                //    };
                //    juliangForm.OnDataCollectReached(this, args);

                //}

            } 
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"巨量资源加载完成时的回调方法发生异常==={url_str}");

                // 当前授权成功，但是发生异常，修改授权状态
                if (url_str.Contains("web/common?mstoken=") || url_str.Contains("api/v1/config"))
                {
                    JuliangUtils.authing = false;
                }
            }
        }

        /// <summary>
        /// 授权失效的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="juliangStatus">授权状态</param>
        private void loginExpire(AnchorInfo anchorInfo, int juliangStatus)
        {
            if(anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {
                return;
            }
            // 修改主播的授权状态
            AnchorBll.UpdateJuliangAuthStatus(anchorInfo, juliangStatus);
            // 删除本地缓存授权版本号
            JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);
            // 重新设置资产
            JuliangUtils.recountJuliangProperty();

            // 通知前端
            FrontNotice frontNotice = new FrontNotice();
            var requestDataObj = new Dictionary<string, object>();
            requestDataObj["code"] = 0;
            requestDataObj["status"] = 200;
            requestDataObj["action"] = "juliangAuthExpires";
            requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            // 关闭窗体
            juliangForm.closeForm();
        }

        /// <summary>
        /// 授权账号不匹配的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        private void accountMismatched(AnchorInfo anchorInfo)
        {
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {
                return;
            }
            // 修改主播的授权状态
            AnchorBll.UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.accountMismatched);
            // 删除本地缓存授权版本号
            JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);
            // 重新设置资产
            JuliangUtils.recountJuliangProperty();

            // 关闭窗体
            juliangForm.closeForm();
        }

        /// <summary>
        /// 用户画像的处理
        /// </summary>
        /// <param name="url_str">url</param>
        /// <param name="requestIdentifierStr">请求信息</param>
        /// <returns></returns>
        private string userPortraitHandle(string url_str, string requestIdentifierStr)
        {
            string key = "";
            if (url_str.Contains("source=pay_user"))
            {
                // 下单的用户画像数据
                key = JuliangDataKeyTypeEnum.payUserPortrait;

                // 判断有没有数据
                var portraitData = getData(requestIdentifierStr);
                if (!string.IsNullOrEmpty(portraitData))
                {
                    try
                    {
                        JObject jObj = JObject.Parse(portraitData);
                        if (jObj.ContainsKey("data"))
                        {
                            var dataJObj = jObj["data"];
                            JArray provinceDistribution = (JArray)dataJObj["province_distribution"];
                            if (provinceDistribution != null && provinceDistribution.Count > 0)
                            {

                                string js_str = @"
                                            const div = Array.from(document.querySelectorAll('span')).find(el => el.textContent.includes('\u770b\u64ad\u7528\u6237\u753b\u50cf'));
                                            if(div){
                                                div.click();
                                            }
                                            ";

                                juliangForm.runScript(js_str);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"{ex}", $"巨量判断有没有用户画像数据，发生异常");
                    }
                }

                if (juliangForm.isTemp)
                {
                    juliangForm.tempLoadDataFlagSet.Add(JuliangDataKeyTypeEnum.payUserPortrait);
                }
            }
            else
            {
                // 观看的用户画像数据
                key = JuliangDataKeyTypeEnum.watchUserPortrait;
                if (juliangForm.isTemp)
                {
                    juliangForm.tempLoadDataFlagSet.Add(JuliangDataKeyTypeEnum.watchUserPortrait);
                }
            }

            return key;
        }

        /// <summary>
        /// 当前扫码的抖音号没有巨量百应的权限
        /// </summary>
        /// <param name="anchorInfo"></param>
        private void scanFailHandle(AnchorInfo anchorInfo)
        {
            AnchorBll.UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.authError);
            FileUtils.LogRpa($"{JsonConvert.SerializeObject(anchorInfo)}", $"扫码的抖音号未开通巨量百应权限，授权失败");

            // 删除授权缓存版本号
            JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);

            // 通知前端
            FrontNotice frontNotice = new FrontNotice();
            var requestDataObj = new Dictionary<string, object>();
            requestDataObj["code"] = 0;
            requestDataObj["status"] = 200;
            requestDataObj["action"] = "juliangAuthError";

            Dictionary<string, object> dataDictionary = new Dictionary<string, object>();
            dataDictionary.Add("secUid", anchorInfo.SecUid);
            dataDictionary.Add("msg", "扫码的抖音号未开通巨量百应权限，授权失败");

            requestDataObj.Add("data", dataDictionary);
            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));

            JuliangUtils.authing = false;

            juliangForm.closeForm();
        }

        /// <summary>
        /// 加载完首页后的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        private void loadHomeFinishHandle(AnchorInfo anchorInfo)
        {
            // 设置主播的授权状态
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {

                FileUtils.LogRpa($"{anchorInfo?.AnchorName}", $"加载完首页成功，将授权标识写进文件");
                // 将授权标识写进文件
                string cachePath = juliangForm.getCachePath();
                //JuliangDataHandle.writeAuthFlag(videoId, cachePath);

                // 扣减资产
                //UserPropertyApi.useProperty("rpaAmountNum", 1);

                AnchorBll.UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.auth);
                anchorInfo.IsDataViewing = 0;

                // 重新设置资产
                JuliangUtils.recountJuliangProperty();

                // 通知前端
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "juliangAuthSuccess";
                Dictionary<string, object> dataDictionary = new Dictionary<string, object>();
                dataDictionary.Add("secUid", anchorInfo.SecUid);
                requestDataObj.Add("data", dataDictionary);
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));

                JuliangUtils.authing = false;

                // 同步修改数据看板状态到服务器
                try
                {
                    AnchorApi.AddOrUpdateAnchorSync(anchorInfo);
                }
                catch (Exception e)
                {
                    FileUtils.LogRpa($"{e}", $"授权成功修改主播数据看板状态到服务器发生异常");
                }
                //this.closeForm();
                juliangForm.SaveCurrentCookiesToLocal();

                juliangForm.closeForm();

                //// 如果主播在录制中以及没有巨量窗体，开启巨量窗体
                //try
                //{
                //    bool isRecord = AnchorCacheManager.GetAnchorByIdFromCache(anchorInfo.SecUid).RecordStatus == 1;
                //    if(isRecord)
                //    {
                //        JuliangUtils.juliangFormList.TryGetValue(anchorInfo.SecUid, out JuliangForm jf);

                //        // 获取最后一条在录制的视频数据，
                //        if (jf == null)
                //        {
                //            // 打开新的巨量窗体

                //        }
                //        else
                //        {
                //            // 直接接在大屏页
                //        }
                //    }
                //}
                //catch (Exception ex)
                //{
                //    FileUtils.LogRpa($"{ex}", $"授权成功后判断是否需要开启巨量窗体发生异常=={anchorInfo?.AnchorName}");
                //}

            }
        }

        /// <summary>
        /// 登录成功后的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        private void loginSuccessHandle(AnchorInfo anchorInfo)
        {
            if (juliangForm.allowLoadDataScreen)
            {
                //juliangForm.showBigWebForm();

                // 加载首页
                FileUtils.LogRpa($"{anchorInfo?.AnchorName}", $"授权登录成功，将加载首页");
                juliangForm.openHome();

                // 临时改的授权方法
                loadHomeFinishHandle(anchorInfo);

                //if (juliangForm.scanObjectAuthSuccess || !juliangForm.loginSuccessFlag)
                //{
                //    // 加载首页
                //    juliangForm.openHome();
                //}
                //else
                //{
                //    // 当前扫码的抖音跟主播不对应，设置主播的授权状态，授权失败
                //    if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.authError && anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                //    {
                //        anchorInfo.juliangAuthStatus = JuliangAuthStatusEnum.authError;
                //        AnchorCacheManager.SetAnchorCache(anchorInfo);
                //        FileUtils.LogRpa($"{JsonConvert.SerializeObject(anchorInfo)}", $"当前扫码的抖音跟主播不对应，授权失败");

                //        // 删除授权缓存版本号
                //        JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);

                //        // 通知前端
                //        FrontNotice frontNotice = new FrontNotice();
                //        var requestDataObj = new Dictionary<string, object>();
                //        requestDataObj["code"] = 0;
                //        requestDataObj["status"] = 200;
                //        requestDataObj["action"] = "juliangAuthError";

                //        Dictionary<string, object> dataDictionary = new Dictionary<string, object>();
                //        dataDictionary.Add("secUid", anchorInfo.SecUid);
                //        dataDictionary.Add("msg", "授权认证失败，请使用主播对应的抖音号扫码授权");

                //        requestDataObj.Add("data", dataDictionary);
                //        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));

                //        JuliangUtils.authing = false;
                //        juliangForm.closeForm();

                //    }
                //}

            }
        }

        /// <summary>
        /// 获取监听的网络资源数据
        /// </summary>
        /// <param name="requestIdentifier"></param>
        /// <returns></returns>
        private string getData(string requestIdentifier)
        {
            IResponseFilter myFilter = CustomResponseFilterManager.GetFilter(requestIdentifier);
            if (myFilter == null)
            {
                return null;
            }
            var filter = myFilter as CustomResponseFilter;
            UTF8Encoding encoding = new UTF8Encoding();

            var data = encoding.GetString(filter.dataAll.ToArray());
            return data;
        }


        public void Dispose()
        {

        }

        public ICookieAccessFilter GetCookieAccessFilter(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request)
        {
            return null;
        }

        public IResourceHandler GetResourceHandler(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request)
        {
            return null;
        }

        

        public CefReturnValue OnBeforeResourceLoad(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IRequestCallback callback)
        {
            try
            {
                var url = request.Url.ToLower();

                // 本地 HTTP 服务的资源（复盘视频播放）不拦截
                if (url.StartsWith("http://localhost") || url.StartsWith("https://localhost"))
                    return CefReturnValue.Continue;

                // 常见的视频后缀和流媒体请求
                string[] videoExtensions = { ".mp4", ".flv", ".m3u8", ".ts", ".mov", ".avi", ".wmv", ".mkv" };

                // 简单判断：后缀 或 URL 中包含流媒体关键字
                if (videoExtensions.Any(ext => url.Contains(ext))
                || url.Contains("videoplayback")
                || url.Contains("bytecdn")   // 抖音/巨量的视频 CDN
                || url.Contains("playwm"))   // 抖音无水印视频
                {
                    FileUtils.log($"[CefSharp] Blocked video resource: {url}");
                    return CefReturnValue.Cancel;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", "OnBeforeResourceLoad 视频拦截时异常");
            }

            return CefReturnValue.Continue;
        }

        public bool OnProtocolExecution(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request)
        {
            return false;
        }

        

        public void OnResourceRedirect(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response, ref string newUrl)
        {
            
        }

        public bool OnResourceResponse(IWebBrowser chromiumWebBrowser, IBrowser browser, IFrame frame, IRequest request, IResponse response)
        {
            return false;
        }
    }
}
