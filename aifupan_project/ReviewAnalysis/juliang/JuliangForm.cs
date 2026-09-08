using CefSharp;
using CefSharp.DevTools.Network;
using CefSharp.Handler;
using CefSharp.WinForms;
using douyin.Utils;
using Newtonsoft.Json;
using Qiniu.Storage;
using ReviewAnalysis;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Bll.VideoPull;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.enumeration.juliang;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Model;
using ReviewAnalysis.socketAddress.browser;
using ReviewAnalysis.Utils;
using Swan;
using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using System.Web.UI;
using System.Windows.Forms;
using Timer = System.Windows.Forms.Timer;

namespace juliang
{
    public partial class JuliangForm : Form
    {
        private delegate void ShowForm();
        private delegate void ShowBigForm();
        private delegate void HideForm();
        private delegate void CloseForm();
        private delegate void ExecScript(string js);

        /// <summary>
        /// 主播信息
        /// </summary>
        public AnchorInfo anchorInfo { get; set; }
        /// <summary>
        /// 直播间id
        /// </summary>
        public string roomId { get; set; }
        /// <summary>
        /// 视频id
        /// </summary>
        public string videoId { get; set; }
        /// <summary>
        /// 是否临时加载，如果是临时的，同时需要加载专业版，并且加载完后需要关闭
        /// </summary>
        public bool isTemp {  get; set; }
        /// <summary>
        /// 添加临时窗体的时间戳，用于超时关闭临时窗体
        /// </summary>
        public long addTempTime { get; set; }
        /// <summary>
        /// 临时窗体的uuid
        /// </summary>
        public string tempUuid { get; set; }
        /// <summary>
        /// 是否是前端发起的拉取数据，如果是，在获取完数据窗体关闭前回调给前端
        /// </summary>
        public bool isFrontPull { get; set; }
        /// <summary>
        /// 登录类型 0：登录主账号 1：登录子账号
        /// </summary>
        public int authType { get; set; }
        /// <summary>
        /// 临时加载时的数据加载结果标识集合，如果集合有对应标识说明已经加载过该数据
        /// </summary>
        public HashSet<string> tempLoadDataFlagSet {  get; set; }
        /// <summary>
        /// 是否是第一次加载下单的用户画像数据
        /// </summary>
        public volatile bool fristLoadPayUserPortrait = true;

        /// <summary>
        /// 显示窗体
        /// </summary>
        private ShowForm showWebForm;
        /// <summary>
        /// 最大化窗体
        /// </summary>
        private ShowBigForm showBigForm;
        /// <summary>
        /// 隐藏窗体
        /// </summary>
        private HideForm hideWebForm;
        /// <summary>
        /// 关闭窗体
        /// </summary>
        private CloseForm closeWebForm;
        /// <summary>
        /// 执行js脚本
        /// </summary>
        private ExecScript execJavaScript;

        /// <summary>
        /// 当前浏览器对象
        /// </summary>
        ChromiumWebBrowser browser;
        /// <summary>
        /// 加载授权二维码页面的标识 0：初始 1：允许加载授权页面 2：加载授权页面完成
        /// </summary>
        private int qrcodeFlag = 0;
        /// <summary>
        /// 是否第一次激活窗体
        /// </summary>
        private bool firstActivateForm = true;
        /// <summary>
        /// 是否已加载过窗体
        /// </summary>
        private bool loadedForm = false;
        /// <summary>
        /// 是否已加载过浏览器
        /// </summary>
        private bool loadedBrowser = false;
        /// <summary>
        /// 是否第一次加载页面
        /// </summary>
        private bool firstLoadPage = true;
        /// <summary>
        /// 是否允许加载数据大盘地址
        /// </summary>
        public volatile bool allowLoadDataScreen = true;
        /// <summary>
        /// 是否登录成功
        /// </summary>
        public volatile bool loginSuccess = false;
        /// <summary>
        /// 是否是第一次加载专业大屏
        /// </summary>
        public volatile bool firstLoadMajorData = true;
        /// <summary>
        /// 扫码的抖音号跟主播是否一致
        /// </summary>
        public volatile bool scanObjectAuthSuccess = false;
        /// <summary>
        /// 是否有登录成功标识，如果没有，表示本地有缓存，无需判断抖音号跟主播是否一致
        /// </summary>
        public volatile bool loginSuccessFlag = false;

        public event EventHandler<DataCollectEventArgs> DataCollectReached;

        // 目标登录域名（需与登录页面域名一致，不含协议）
        private const string TargetDomain = "jinritemai.com";
        // 目标 URL（带协议，用于 Cookie 过滤和加载登录页）
        private const string TargetBaseUrl = "https://compass.jinritemai.com";//https://buyin.jinritemai.com
        // 登录页面 URL（聚量百应登录页，示例）
        private const string LoginUrl = "https://buyin.jinritemai.com/mpa/account/login";
        // 登录成功后的跳转页面,直播大屏（用于判断登录完成）
        private const string LoginSuccessUrl = "https://buyin.jinritemai.com/dashboard";
        private Timer _cookieCheckTimer;
        private JuliangReachData _juliangReachData;
        private bool bHideBideWebForm = true;
        private bool bModifyLuopanCookieExpiry = false;
        private int ijuliangReachintervalTime = 20;
        public bool bloadHomeFinishHandle = false;
        /// <summary>
        /// 初始化
        /// </summary>
        public void init(bool bhide= true)
        {
            firstLoadPage = true;
            bHideBideWebForm = bhide;
            if (!loadedForm)
            {
                loadedForm = true;
                // 初始化窗体
                InitializeComponent();
                this.Location = new Point(0, 0);
                this.Text = $"巨量百应授权-{anchorInfo.AnchorName}";
                this.DataCollectReached += JuliangUtils.OnDataCollectReached;
                this.FormBorderStyle = FormBorderStyle.FixedDialog;
                this.TopMost = true;
                this.MinimizeBox = false;
                this.MaximizeBox = false;
                this.Icon = new Icon(FormMain.icoPath);
                this.Show();
                if (bHideBideWebForm)
                    BeginInvoke(hideWebForm);
            }
            FileUtils.LogRpa($"isTemp：{isTemp}，allowLoadDataScreen：{allowLoadDataScreen}，firstLoadMajorData：{firstLoadMajorData}", $"初始化窗体");
            if (isTemp)
            {
                // 异步初始化浏览器和加载页面，避免阻塞UI
                _ = Task.Run(() =>
                {
                    try
                    {
                        // 初始化浏览器
                        if (!loadedBrowser)
                        {
                            loadedBrowser = true;
                            this.Invoke(new Action(() => { InitBrowser(); }));
                        }
                        else
                        {
                            int iAuth = CheckAuthStatusByCookie();
                            if (iAuth == JuliangAuthStatusEnum.auth)
                            {
                                // 加载首页
                                this.Invoke(new Action(() => { openHome(); }));
                            }
                            else
                            {
                                // 加载登录页
                                this.Invoke(new Action(() => { openLogin(); }));
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"异步初始化浏览器异常：{ex}", "init方法");
                    }
                }).ConfigureAwait(false);
            }
            else
            {
                try
                {
                    // 初始化浏览器
                    if (!loadedBrowser)
                    {
                        loadedBrowser = true;
                        InitBrowser();
                    }
                    else
                    {
                        // 不检查本地Cookie，始终加载登录页，强制用户重新授权
                        openLogin();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"异步初始化浏览器异常：{ex}", "init方法");
                }
            }

        }
        bool bOpenLuopanDataScreen = false;
        CefSharp.Cookie compassluopanDtCookie = null;
        int iTickCount =0;
        /// <summary>
        /// 定时器触发：检查 LUOPAN_DT Cookie 有效性
        /// </summary>
        private async void CookieCheckTimer_Tick(object sender, EventArgs e)
        {
            try
            {
                if (browser == null)//&& !isTemp
                {
                    return;
                }

                iTickCount++;
                      //从本地读取cookie,本地没过期
                      var localCookies = new CookiePersistenceHelper(getCookiePath()).LoadCefCookiesFromLocal(false);
                var localLuopanDtCookie = localCookies?.FirstOrDefault(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
                var localSessionIdCookie = localCookies?.FirstOrDefault(cookie => cookie.Name.Equals("sessionid", StringComparison.Ordinal));
                //登录态
                var cefCookies = await GetBuyinCookiesAsync();
                    var buyinCookie = cefCookies?.FirstOrDefault(cookie => cookie.Name.Equals("BUYIN_SASID", StringComparison.Ordinal));
                    var luopanDtCookie = cefCookies?.FirstOrDefault(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
                    var sessionIdCookie = cefCookies?.FirstOrDefault(cookie => cookie.Name.Equals("sessionid", StringComparison.Ordinal));

                    if (compassluopanDtCookie != null && luopanDtCookie == null)
                    {
                        _juliangReachData?.Dispose();
                        _juliangReachData = null;
                        ModifyLuopanCookieExpiry();
                        loginExpire(anchorInfo, JuliangAuthStatusEnum.unAuth);
                        FileUtils.LogRpa($"实时登录态检测，检测到用户退出", anchorInfo?.AnchorName);
                    }
                    if (compassluopanDtCookie != null && luopanDtCookie != null && luopanDtCookie.Value != compassluopanDtCookie.Value)
                    {
                        _juliangReachData?.Dispose();
                        _juliangReachData = null;
                        SaveCurrentCookiesToLocal();
                        FileUtils.LogRpa($"登录态检测到用户Cookie变更", anchorInfo?.AnchorName);
                    }
                    if (luopanDtCookie != null && !string.IsNullOrWhiteSpace(luopanDtCookie.Value) && luopanDtCookie.Expires < DateTime.Now.AddHours(-1))
                    {
                        _juliangReachData?.Dispose();
                        _juliangReachData = null;
                        ModifyLuopanCookieExpiry();
                        loginExpire(anchorInfo, JuliangAuthStatusEnum.authExpires);
                        FileUtils.LogRpa($"登录态检测到用户Cookie过期", anchorInfo?.AnchorName);
                    }
                    compassluopanDtCookie = luopanDtCookie;
                if(!isTemp)
                {
                if (luopanDtCookie == null || string.IsNullOrWhiteSpace(luopanDtCookie.Value))
                    {
                        if (cefCookies != null)
                        {
                            if (buyinCookie != null) // && !bOpenLuopanDataScreen
                            {
                                var cookieManager = browser.RequestContext.GetCookieManager(null);
                            if (cookieManager != null)
                            {
                                // 重置过期时间为远期，标记为未过期
                                buyinCookie.Expires = DateTime.Now.AddMonths(2);
                                cookieManager.SetCookieAsync("https://compass.jinritemai.com", buyinCookie);
                            }
                                bOpenLuopanDataScreen = true;
                                openDataScreen(2);
                            this.hideForm();
                            FileUtils.LogRpa($"登录态检测,尝试获取luopanDtCookie", anchorInfo?.AnchorName);
                            }
                            if (buyinCookie == null && anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth)
                            {
                            if (localLuopanDtCookie != null && !string.IsNullOrWhiteSpace(localLuopanDtCookie.Value) && localLuopanDtCookie.Expires > DateTime.Now)
                            {

                            }
                            else
                            {
                                bModifyLuopanCookieExpiry = true;
                                _juliangReachData?.Dispose();
                                _juliangReachData = null;
                                ModifyLuopanCookieExpiry();
                                loginExpire(anchorInfo, JuliangAuthStatusEnum.unAuth);
                                FileUtils.LogRpa($"登录态检测,登录态退出", anchorInfo?.AnchorName);
                            }
                 
                            }
                        if (buyinCookie == null && anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.authing)
                        {
                                if(iTickCount>100)
                                    closeForm(); //超时关闭授权
                        }

                    }
                    }
                    else
                    {
                    if (!bloadHomeFinishHandle)
                    {
                        bloadHomeFinishHandle = true;
                        // 登录成功
                        try
                        {
                            this.hideForm();
                            
                            // 保存最新Cookie到本地（更新过期时间）- 使用BeginInvoke避免阻塞
                            if (this.IsHandleCreated)
                            {
                                this.BeginInvoke((MethodInvoker)(() =>
                                {
                                    loadHomeFinishHandle(anchorInfo);
                                    SaveCurrentCookiesToLocal();
                                }));
                            }
                            else
                            {
                                loadHomeFinishHandle(anchorInfo);
                                SaveCurrentCookiesToLocal();
                            }
                            FileUtils.LogRpa($"登录态检测，首次获取到luopanDtCookie，修改用户登录状态,写入本地", anchorInfo?.AnchorName);
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"登录态检测，首次获取到luopanDtCookie，写入本地异常: {ex.Message}", anchorInfo?.AnchorName);
                        }
                        finally 
                        {
                                if(loginSuccess)
                            closeForm();
                        }

                        

                    }
                        FileUtils.LogRpa($"登录态检测，获取到luopanDtCookie{luopanDtCookie?.Value}", anchorInfo?.AnchorName);
                    }
                }
                

                if (!string.IsNullOrEmpty(roomId) && !string.IsNullOrEmpty(videoId))
                {
                  
                    if (localLuopanDtCookie == null || string.IsNullOrWhiteSpace(localLuopanDtCookie.Value) || localLuopanDtCookie.Expires < DateTime.Now.AddHours(-1))
                    {

                    }
                    else
                    {
                        if (_juliangReachData == null)
                        {
                            _juliangReachData = JuliangReachDataManager.GetInstance(new JuliangReachDataConfig { RoomId = roomId, VideoId = videoId, AnchorInfo = anchorInfo, juliangForm = this, LuopanDtCookie = localLuopanDtCookie.Value, SessionIdCookie = localSessionIdCookie?.Value });
                            _juliangReachData.PollingElapsedHandler();
                            _juliangReachData.StartPolling(ijuliangReachintervalTime);

                            FileUtils.LogRpa($"开始启动巨量数据定时获取程序", anchorInfo?.AnchorName);
                        }
                        else 
                        {
                            //var juliangReachDataCookies = _juliangReachData.CookieContainer.GetCookies(new Uri("https://compass.jinritemai.com"));
                            //var juliangReachDataLuopanDtCookie = GetCookieByNameAndDomain(juliangReachDataCookies, "COMPASS_LUOPAN_DT", "jinritemai.com");
                            //var juliangReachDataSessionIdCookie = GetCookieByNameAndDomain(juliangReachDataCookies, "sessionid", "jinritemai.com");
                            if (_juliangReachData.LuopanDtCookie != localLuopanDtCookie.Value || _juliangReachData.SessionIdCookie != localSessionIdCookie?.Value)
                            {
                                _juliangReachData?.Dispose();
                                _juliangReachData = JuliangReachDataManager.GetInstance(new JuliangReachDataConfig { RoomId = roomId, VideoId = videoId, AnchorInfo = anchorInfo, juliangForm = this, LuopanDtCookie = localLuopanDtCookie.Value, SessionIdCookie = localSessionIdCookie?.Value });
                                _juliangReachData.PollingElapsedHandler();
                                _juliangReachData.StartPolling(ijuliangReachintervalTime);
                                FileUtils.LogRpa($"开始更新Cookie启动巨量数据定时获取程序", anchorInfo?.AnchorName);
                            }
                        }
                        

                    }
                }
                else
                {
                    // 关闭窗体
                    //this.closeForm();
                }

                if (this != null && this.isTemp)
                {
                    HashSet<string> set = this.tempLoadDataFlagSet;
                    if (set.Contains(JuliangDataKeyTypeEnum.bigScreenDataBase) && set.Contains(JuliangDataKeyTypeEnum.bigScreenDataPro) && set.Contains(JuliangDataKeyTypeEnum.flowOrderSource)
                        && set.Contains(JuliangDataKeyTypeEnum.payUserPortrait) && set.Contains(JuliangDataKeyTypeEnum.watchUserPortrait))
                    {
                        _juliangReachData?.StopPolling();
                        this.closeForm();

                    }
                }
            }
            catch (Exception ex)
            {
                // 必须捕获异常，否则定时器会静默停止
                FileUtils.LogRpa($"Cookie 检查失败：{ex.Message}", "定时器触发");
            }
        }// 扩展：获取指定名称+指定域名的Cookie
        public static System.Net.Cookie GetCookieByNameAndDomain(CookieCollection cookieCollection, string cookieName, string domain, bool ignoreCase = true)
        {
            if (cookieCollection == null || cookieCollection.Count == 0 || string.IsNullOrWhiteSpace(cookieName))
            {
                return null;
            }

            foreach (System.Net.Cookie cookie in cookieCollection)
            {
                bool nameMatch = ignoreCase
                    ? string.Equals(cookie.Name, cookieName, StringComparison.OrdinalIgnoreCase)
                    : cookie.Name == cookieName;

                bool domainMatch = ignoreCase
                    ? string.Equals(cookie.Domain, domain, StringComparison.OrdinalIgnoreCase)
                    : cookie.Domain == domain;

                if (nameMatch && domainMatch)
                {
                    return cookie;
                }
            }

            return null;
        }

        /// <summary>
        /// 授权失效的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="juliangStatus">授权状态</param>
        public void loginExpire(AnchorInfo anchorInfo, int juliangStatus)
        {
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {
                return;
            }
            // 修改主播的授权状态
            AnchorBll.UpdateJuliangAuthStatus(anchorInfo, juliangStatus);
            // 删除本地缓存授权版本号
            //JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);
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
            this.closeForm();
        }
        public void loginExpire(int juliangStatus)
        {
            loginExpire(this.anchorInfo, juliangStatus);
        }

        /// <summary>
        /// 登录成功后的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        private void loginSuccessHandle(AnchorInfo anchorInfo)
        {
            if (allowLoadDataScreen)
            {
                //juliangForm.showBigWebForm();

                // 加载首页
                FileUtils.LogRpa($"{anchorInfo?.AnchorName}", $"授权登录成功，将加载首页");
                //openHome();

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
        /// 加载完首页后的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        public void loadHomeFinishHandle(AnchorInfo anchorInfo)
        {
            // 设置主播的授权状态
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {

                FileUtils.LogRpa($"{anchorInfo?.AnchorName}", $"加载完首页成功，将授权标识写进文件");
                // 将授权标识写进文件
                string cachePath = getCachePath();
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
                
                // 判断是否要自动拉取巨量的视频（异步，不阻塞授权完成流程）
                var secUid = anchorInfo.SecUid;
                var anchorName = anchorInfo.AnchorName;
                Task.Run(async () =>
                {
                    try
                    {
                        await Task.Delay(10000); // 延迟10秒
                        
                        int videoCount = VideoApi.CountBySecUid(secUid);
                        if (videoCount == 0)
                        {
                            FileUtils.LogRpa($"{anchorName}", $"授权完成，无视频记录，自动拉取巨量数据");
                            var pullManager = new VideoPullManager();
                            await pullManager.PullAndSave(secUid);
                        }
                        else if (videoCount > 0)
                        {
                            FileUtils.LogRpa($"{anchorName}", $"授权完成，已有 {videoCount} 条视频记录，跳过拉取");
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"{ex}", $"授权完成后自动拉取巨量数据发生异常=={anchorName}");
                    }
                });


                this.closeForm();
                //closeForm();

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
        /// 检测本地是否有授权标识记录
        /// </summary>
        /// <returns></returns>
        public bool checkAuthFileExist()
        {
            string cachePath = getCachePath();
            bool exist = File.Exists($"{cachePath}\\flag.txt");
            FileUtils.LogRpa($"{exist}==={cachePath}\\flag.txt", $"检测本地是否有授权标识记录=={anchorInfo?.AnchorName}");
            return exist;
        }
        /// <summary>
        /// 通过Cookie检测本地是否有授权标识记录
        /// </summary>
        /// <returns></returns>
        public int CheckAuthStatusByCookie()
        {
            var localCookies = new CookiePersistenceHelper(getCookiePath()).LoadCefCookiesFromLocal(false);
            if (localCookies == null)
            {
                return -1;
            }
            var luopanDtCookie = localCookies?.FirstOrDefault(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
            if (luopanDtCookie == null || string.IsNullOrWhiteSpace(luopanDtCookie.Value))
            {
                return JuliangAuthStatusEnum.unAuth;
            }
            if (luopanDtCookie.Expires < DateTime.Now.AddHours(-1))
            {
                return JuliangAuthStatusEnum.authExpires;
            }
            else
                return JuliangAuthStatusEnum.auth;

        }

        /// <summary>
        /// 获取缓存的路径地址
        /// </summary>
        /// <returns></returns>
        public string getCachePath()
        {
            return JuliangUtils.getCachePath(anchorInfo.SecUid);
        }
        /// <summary>
        /// 获取Cookie的路径地址
        /// </summary>
        /// <returns></returns>
        public string getCookiePath()
        {
            return JuliangUtils.getCookiePath(anchorInfo.SecUid);
        }

        /// <summary>
        /// 初始化浏览器
        /// </summary>
        public void InitBrowser()
        {
            string url = "https://buyin.jinritemai.com/mpa/account/login";

            BrowserSettings browserSettings = new BrowserSettings();

            RequestContextSettings requestContextSettings = new RequestContextSettings();
            // 设置不持久化会话 cookie，即关闭浏览器后，会话 cookie 不会被保存。
            requestContextSettings.PersistSessionCookies = false;

            // 不加载之前的浏览器缓存，每次授权都强制重新登录
            // requestContextSettings.CachePath = getCachePath();

            //if (!isTemp)
            //{
                browser = new ChromiumWebBrowser(url)
                {
                    // 配置资源自定义处理
                    RequestHandler = new CustomResourceHandler(this),
                    BrowserSettings = browserSettings,
                    RequestContext = new RequestContext(requestContextSettings)
                };
                // 添加控件到窗体
                Controls.Add(browser);
                Controls.SetChildIndex(browser, 0);
                browser.FrameLoadEnd += browser_FrameLoadEnd;
            //}




            // 初始化定时器：检查Cookie
            _cookieCheckTimer = new Timer
                {
                    Interval = 1 * 3 * 1000,
                    Enabled = true
                };
                // 绑定 Tick 事件（定时执行的逻辑）
                _cookieCheckTimer.Tick += CookieCheckTimer_Tick;
                CookieCheckTimer_Tick(null, null);



        }
        /// <summary>
        /// 保存当前Cef中的Cookie到本地（更新登录态，使用 BeginInvoke + TaskCompletionSource 避免阻塞）
        /// 同时获取 buyin 和 compass 两个域名的Cookie，确保完整性
        /// </summary>
        public async void SaveCurrentCookiesToLocal()
        {
            if (browser == null)
                return;
            
            var cookieManager = browser.RequestContext.GetCookieManager(null);
            
            // 1. 获取 buyin 域名的Cookie
            var buyinCookies = await cookieManager?.VisitUrlCookiesAsync(
                url: "https://buyin.jinritemai.com",
                includeHttpOnly: true
            );
            
            // 2. 获取 compass 域名的Cookie（COMPASS_LUOPAN_DT 在此域名下）
            var compassCookies = await cookieManager?.VisitUrlCookiesAsync(
                url: "https://compass.jinritemai.com",
                includeHttpOnly: true
            );

            // 3. 获取 qianchuan 域名的Cookie（sessionid 可能在此域名下）
            var qianchuanCookies = await cookieManager?.VisitUrlCookiesAsync(
                url: "https://qianchuan.jinritemai.com",
                includeHttpOnly: true
            );
            
            // 4. 合并三个域名的Cookie
            var allCookies = new List<CefSharp.Cookie>();
            if (buyinCookies != null)
                allCookies.AddRange(buyinCookies);
            if (compassCookies != null)
                allCookies.AddRange(compassCookies);
            if (qianchuanCookies != null)
                allCookies.AddRange(qianchuanCookies);
            
            // 5. 去重（根据Name+Domain）
            var uniqueCookies = allCookies
                .GroupBy(c => $"{c.Name}:{c.Domain}")
                .Select(g => g.First())
                .ToList();
            
            // 6. 保存所有Cookie（解决st:10012错误，API需要完整Cookie信息）
            var luopanCookie = uniqueCookies.ToList();
            
            FileUtils.log($"获取到 buyin:{buyinCookies?.Count ?? 0}, compass:{compassCookies?.Count ?? 0}, qianchuan:{qianchuanCookies?.Count ?? 0}, 合并去重后:{luopanCookie.Count} 个Cookie");
            
            // 保存Cookie时使用合并逻辑
            string cookiePath = getCookiePath();
            try
            {
                // 读取现有Cookie
                List<ReviewAnalysis.juliang.CookieDto> existingDtos = new List<ReviewAnalysis.juliang.CookieDto>();
                if (System.IO.File.Exists(cookiePath))
                {
                    string existingContent = System.IO.File.ReadAllText(cookiePath);
                    if (!string.IsNullOrEmpty(existingContent))
                    {
                        existingDtos = Newtonsoft.Json.JsonConvert.DeserializeObject<List<ReviewAnalysis.juliang.CookieDto>>(existingContent) ?? new List<ReviewAnalysis.juliang.CookieDto>();
                    }
                }
                
                // 转换新Cookie为DTO
                var newDtos = luopanCookie.ConvertAll(ReviewAnalysis.juliang.CookieDto.FromCefCookie);
                
                // 合并Cookie（更新现有，添加新的）
                var mergedDtos = MergeCookies(existingDtos, newDtos);
                
                // 保存合并后的Cookie
                var jsonStr = Newtonsoft.Json.JsonConvert.SerializeObject(mergedDtos, Newtonsoft.Json.Formatting.Indented);
                System.IO.File.WriteAllText(cookiePath, jsonStr, System.Text.Encoding.UTF8);
                FileUtils.log("当前Cookie已保存到本地");
                FileUtils.log($"已保存 {luopanCookie.Count} 个Cookie: {string.Join(", ", luopanCookie.Select(c => c.Name))}");
            }
            catch (Exception ex)
            {
                FileUtils.log($"直接保存Cookie失败：{ex.Message}，使用CookiePersistenceHelper保存");
                // 如果直接保存失败，使用CookiePersistenceHelper保存
                new CookiePersistenceHelper(cookiePath).SaveCefCookiesToLocal(luopanCookie, false);
            }
        }
        
        /// <summary>
        /// 合并Cookie列表（更新现有，添加新的）
        /// </summary>
        /// <param name="existingDtos">现有Cookie</param>
        /// <param name="newDtos">新Cookie</param>
        /// <returns>合并后的Cookie列表</returns>
        private List<ReviewAnalysis.juliang.CookieDto> MergeCookies(List<ReviewAnalysis.juliang.CookieDto> existingDtos, List<ReviewAnalysis.juliang.CookieDto> newDtos)
        {
            // 使用字典来存储Cookie，键为Name+Domain，确保唯一性
            var cookieDict = new Dictionary<string, ReviewAnalysis.juliang.CookieDto>();
            
            // 添加现有Cookie
            foreach (var existingDto in existingDtos)
            {
                var key = existingDto.Name + existingDto.Domain;
                cookieDict[key] = existingDto;
            }
            
            // 添加或更新新Cookie
            foreach (var newDto in newDtos)
            {
                var key = newDto.Name + newDto.Domain;
                cookieDict[key] = newDto; // 覆盖现有Cookie
            }
            
            return cookieDict.Values.ToList();
        }

        /// <summary>
        /// 修改罗盘 Cookie 的过期时间
        /// </summary>
        public void ModifyLuopanCookieExpiry()
        {
            try
            {
                var localCookies = new CookiePersistenceHelper(getCookiePath()).LoadCefCookiesFromLocal(false);
                var localLuopanDtCookie = localCookies?.Where(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal))?.ToList();
                var localSessionIdCookie = localCookies?.Where(cookie => cookie.Name.Equals("sessionid", StringComparison.Ordinal))?.ToList();
                if (localLuopanDtCookie == null && localSessionIdCookie == null)
                {
                    FileUtils.log("未找到罗盘 Cookie（LUOPAN_DT 或 sessionid），无法修改过期时间");
                    return;
                }

                var newExpiryTime = DateTime.Now.AddDays(-62);
                bool isSuccess1 = new CookiePersistenceHelper(getCookiePath())
                    .ModifyLocalCookieExpiry("COMPASS_LUOPAN_DT", "jinritemai.com", newExpiryTime);
                bool isSuccess2 = new CookiePersistenceHelper(getCookiePath())
                    .ModifyLocalCookieExpiry("sessionid", "jinritemai.com", newExpiryTime);

                if (isSuccess1 || isSuccess2)
                {
                    FileUtils.log($"罗盘 Cookie 过期时间已修改为：{newExpiryTime}");
                }
                else
                {
                    FileUtils.log("罗盘 Cookie 过期时间修改失败");
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"修改 Cookie 过期时间异常：{ex.Message}");
            }
        }
        
        /// <summary>
        /// 页面加载失败事件（排查网络/证书问题）
        /// </summary>
        private void ChromeBrowser_LoadError(object sender, LoadErrorEventArgs e)
        {
            Invoke(new Action(() =>
            {
                
            }));
        }

        /// <summary>
        /// 打开登录页
        /// </summary>
        public void openLogin()
        {
            openURL("https://buyin.jinritemai.com/mpa/account/login");
            if(this.authType == 1)
            {
                this.showBigWebForm();
            }
        }

        /// <summary>
        /// 打开大屏页
        /// </summary>
        /// <param name="screenType">大屏的类型 0：基础版 1：专业版 2：登录数据大屏</param>
        public void openDataScreen(int screenType)
        {
            
            if(screenType == 0)
            {
                // 打开基础版
                // https://compass.jinritemai.com/login/transfer?login_source=compass&mode=1&from_source=buyin&redirect_url=/screen/live/talent?live_room_id=7577777016504404788
                //string ur = $"https://compass.jinritemai.com/login/transfer?login_source=compass&mode=1&from_source=buyin&redirect_url=/screen/live/talent?source=baiying_home&live_room_id={roomId}";
                openURL($"https://compass.jinritemai.com/screen/talent/main?live_room_id={roomId}");
                FileUtils.LogRpa($"基础版", $"打开大屏页");
            }
            else if(screenType == 1) 
            {
                // 打开专业版
                openURL($"https://compass.jinritemai.com/screen/live/talent?live_room_id={roomId}");
                FileUtils.LogRpa($"专业版", $"打开大屏页");
            }
            else if (screenType == 2)
            {
                // 登录数据大屏
                openURL($"https://compass.jinritemai.com/login/transfer?login_source=compass&mode=1&from_source=buyin&redirect_url=/screen/live/talent?live_room_id={roomId}");
                FileUtils.LogRpa($"登录数据大屏", $"打开大屏页");
            }
            FileUtils.LogRpa($"allowLoadDataScreen：{allowLoadDataScreen}，firstLoadMajorData：{firstLoadMajorData}", $"打开大屏页==数据");

        }

        /// <summary>
        /// 打开首页
        /// </summary>
        public void openHome()
        {
            //Task.Run(() => openURL("https://compass.jinritemai.com/talent"));
            openURL("https://compass.jinritemai.com/talent");
        }

        /// <summary>
        /// 根据url加载页面
        /// </summary>
        /// <param name="url_str"></param>
        public void openURL(string url_str)
        {
            //Task.Run(()=>browser?.Load(url_str));
            browser?.Load(url_str);
        }

        /// <summary>
        /// 显示窗体
        /// </summary>
        void delegatShowForm()
        {
            //设置窗体大小
            float dpiRatio = ScreenUtils.GetDpiRatio();
            this.Size = new Size((int)(250 * dpiRatio), (int)(280 * dpiRatio));
            //设置窗体位置
            this.Location = new Point(Screen.PrimaryScreen.Bounds.Width / 2 - this.Width / 2,
                      Screen.PrimaryScreen.Bounds.Height / 2 - this.Height / 2);
            this.Show();
        }

        /// <summary>
        /// 显示大窗体
        /// </summary>
        void delegatShowBigForm()
        {
            this.Location = new Point(0, 0);
            this.Size = new Size(1366, 768);
            this.Show();
        }

        /// <summary>
        /// 隐藏窗体
        /// </summary>
        void delegatHideForm()
        {
            //this.Location = new Point(0, 0);
            //this.Size = new Size(1, 1);

            this.Hide();
        }

        /// <summary>
        /// 关闭窗体
        /// </summary>
        void delegatCloseForm()
        {
            StopCookieCheckTimer();
            _juliangReachData?.StopPolling();
            if (this.IsDisposed)
            {
                JuliangUtils.tempJuliangFormList.TryRemove(tempUuid, out JuliangForm removeItem);
                return;
            }

            BeginInvoke(hideWebForm);
            // 释放浏览器资源
            try
            {
                if (browser != null)
                {
                   browser.Dispose();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"释放巨量窗体浏览器资源失败");
            }

            // 1. 临时窗体：从临时列表移除
            if (isTemp && tempUuid != null)
            {
                JuliangUtils.tempJuliangFormList.TryRemove(tempUuid, out JuliangForm removeItem);
            }

            // 2. 所有窗体：采集违规数据（统一处理，确保数据完整性）
            // 使用 Task.Run 避免在 UI 线程死锁
            try
            {
                if (_juliangReachData != null)
                {
                    Task.Run(async () => await _juliangReachData.CollectViolationData()).Wait();
                }
                else
                {
                    FileUtils.LogRpa("_juliangReachData 为 null，跳过违规数据采集", "直播停止处理");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"采集违规数据发生异常");
            }

            // 3. 临时窗体：上传数据（正常窗体在自动分析时上传）
            if (isTemp)
            {
                try
                {
                    if (!string.IsNullOrEmpty(roomId) && !string.IsNullOrEmpty(videoId))
                    {
                        JuliangDataHandle.dataUpload(roomId, videoId, anchorInfo?.SecUid);
                    }
                }
                catch (Exception e)
                {
                    FileUtils.LogRpa($"{e}", $"关闭临时窗体前上传巨量数据发生异常");
                }

                if (isFrontPull)
                {
                    // 前端手动拉取的数据，回调给前端
                    FrontNotice frontNotice = new FrontNotice();
                    var requestDataObj = new Dictionary<string, object>();
                    requestDataObj["code"] = 0;
                    requestDataObj["status"] = 200;
                    requestDataObj["action"] = "juliangPullDataSuccess";
                    requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
                    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                }
            }
            _juliangReachData?.Dispose();
            _juliangReachData = null;
            this.Close();
        }

        /// <summary>
        /// 显示窗体
        /// </summary>
        public void showForm()
        {
            BeginInvoke(showWebForm);
        }

        /// <summary>
        /// 最大化窗体
        /// </summary>
        public void showBigWebForm()
        {
            BeginInvoke(showBigForm);
        }

        /// <summary>
        /// 关闭窗体
        /// </summary>
        public void closeForm()
        {
            BeginInvoke(closeWebForm);
            //if (InvokeRequired)
            //{
            //    // 用BeginInvoke+忽略异常（避免窗体关闭时Invoke挂起）
            //    try
            //    {
            //        BeginInvoke(closeWebForm);
            //    }
            //    catch (InvalidOperationException)
            //    {
            //        // 句柄已释放，直接忽略
            //    }
            //}

        }

        /// <summary>
        /// 隐藏窗体
        /// </summary>
        public void hideForm()
        {
            BeginInvoke(hideWebForm);
        }


        public void OnDataCollectReached(object sender, DataCollectEventArgs e)
        {
            DataCollectReached?.Invoke(this, e);
        }

        /// <summary>
        /// 窗体加载完执行
        /// </summary>
        private void frmWeb_Load(object sender, EventArgs e)
        {
            this.Size = new Size(1, 1);
            
            // 赋值事件委托
            showWebForm = delegatShowForm;
            hideWebForm = delegatHideForm;
            showBigForm = delegatShowBigForm;
            closeWebForm = delegatCloseForm;
            execJavaScript = delegatExecScript;
        }
        public async void GetCookie()
        {
            try
            {
                // 精准获取目标域名的 Cookie（135+ 优化版，速度快）
                var cefCookies = await GetBuyinCookiesAsync();
                if (cefCookies == null || cefCookies.Count == 0)
                {
                    FileUtils.LogRpa("未获取到 Cookie，请确认已登录", "巨量百应提示");
                    return;
                }
                var httpCookies = ConvertToHttpCookies(cefCookies);
                var cookieString = FormatToCookieHeader(httpCookies);
                UpdateSpiderCookie(httpCookies);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取 Cookie 失败：{ex.Message}\n详情：{ex.StackTrace}", "巨量百应错误");
            }
            finally
            {

            }

        }
        /// <summary>
        /// 核心：精准获取 buyin.jinritemai.com 的 Cookie（135+ 版本）
        /// </summary>
        private async Task<List<CefSharp.Cookie>> GetBuyinCookiesAsync()
        {
            var cookieManager = browser.RequestContext.GetCookieManager(null);
            if (cookieManager == null)
                throw new InvalidOperationException("CefSharp Cookie 管理器未初始化");

            // 关键：用 VisitUrlCookiesAsync 按 URL 过滤，仅读取目标域名 Cookie
            var allCookies = await cookieManager.VisitUrlCookiesAsync(
                url: "https://compass.jinritemai.com",       // 目标 URL（带协议，精准定位域名）https://buyin.jinritemai.com  
                includeHttpOnly: true    // 必须设为 true！登录态通常在 HttpOnly Cookie 中
                                         //matchDomain: false        // 模糊匹配（兼容子域名，如 .buyin.jinritemai.com）
            );

            // 二次过滤：确保仅保留 buyin.jinritemai.com 相关 Cookie（避免冗余）
            var targetCookies = allCookies.Where(cookie =>
                cookie.Domain.Equals(TargetDomain, StringComparison.OrdinalIgnoreCase) ||
                cookie.Domain.Equals($".{TargetDomain}", StringComparison.OrdinalIgnoreCase)
            ).ToList();

            return targetCookies;
        }
        /// <summary>
        /// 转换 CefSharp.Cookie 为 .NET HttpCookie（适配爬虫的 CookieContainer）
        /// </summary>
        private List<System.Net.Cookie> ConvertToHttpCookies(List<CefSharp.Cookie> cefCookies)
        {
            var httpCookies = new List<System.Net.Cookie>();
            foreach (var cefCookie in cefCookies)
            {
                var httpCookie = new System.Net.Cookie(cefCookie.Name, cefCookie.Value)
                {
                    // 移除域名前缀 "."（如 .buyin.jinritemai.com → buyin.jinritemai.com）
                    Domain = cefCookie.Domain.StartsWith(".") ? cefCookie.Domain.Substring(1) : cefCookie.Domain,
                    Path = cefCookie.Path,
                    Secure = cefCookie.Secure,          // HTTPS 专用 Cookie（buyin 是 HTTPS 站点）
                    HttpOnly = cefCookie.HttpOnly,      // 保护 Cookie 不被 JS 篡改
                    //Expires = cefCookie.Expires         // Cookie 过期时间
                };
                httpCookies.Add(httpCookie);
            }
            return httpCookies;
        }

        /// <summary>
        /// 格式化为 HTTP 请求头的 Cookie 字符串（格式：key1=value1; key2=value2;）
        /// </summary>
        private string FormatToCookieHeader(List<System.Net.Cookie> httpCookies)
        {
            var sb = new System.Text.StringBuilder();
            foreach (var cookie in httpCookies)
            {
                sb.Append($"{cookie.Name}={cookie.Value}; ");
            }
            return sb.ToString().TrimEnd(';', ' ');
        }
        /// <summary>
        /// 更新Cef巨量百应Cookie
        /// </summary>
        private void UpdateSpiderCookie(List<System.Net.Cookie> httpCookies)
        {
            foreach (var cookie in httpCookies)
            {
                _juliangReachData?.CookieContainer.Add(new Uri(TargetBaseUrl), cookie);
                FileUtils.log($"已添加 Cookie：{cookie.Name}={cookie.Value}（域名：{cookie.Domain}）");
            }

        }
        bool firstLoadPageUrl = true;
        /// <summary>
        /// 浏览器frame加载完的事件
        /// </summary>
        public async void browser_FrameLoadEnd(object sender, CefSharp.FrameLoadEndEventArgs e)
        {
            if (firstLoadPage) //&& e.Url.Equals("about:blank")
            {
                firstLoadPage = false;
                // 加入到窗体集合
                if (!this.isTemp)
                {
                    JuliangUtils.juliangFormList.AddOrUpdate(anchorInfo.SecUid, this, (oldKey, oldValue) => this);
                }
                else
                {
                    tempUuid = Guid.NewGuid().ToString();
                    addTempTime = ServerTimeUtils.getCurrentTime();
                    JuliangUtils.tempJuliangFormList.AddOrUpdate(tempUuid, this, (oldKey, oldValue) => this);
                }
            }
            if (e!=null && !string.IsNullOrEmpty(e.Url))
            {
                int iAuth = CheckAuthStatusByCookie();
                if (firstLoadPageUrl) //&& e.Url.Equals("about:blank")
                {
                    firstLoadPageUrl = false;
                    // 加入到窗体集合
                    //if(!this.isTemp)
                    //{
                    //    JuliangUtils.juliangFormList.AddOrUpdate(anchorInfo.SecUid, this, (oldKey, oldValue) => this);
                    //}
                    //else
                    //{
                    //    tempUuid = Guid.NewGuid().ToString();
                    //    addTempTime = ServerTimeUtils.getCurrentTime();
                    //    JuliangUtils.tempJuliangFormList.AddOrUpdate(tempUuid, this, (oldKey, oldValue) => this);
                    //}


                    // 不检查本地Cookie，始终加载登录页，强制用户重新授权
                    openLogin();
                }
                else if (iAuth != JuliangAuthStatusEnum.auth && this.authType == 0 && e.Url.IndexOf("open.douyin.com/qrconnect?container") > 0 && qrcodeFlag == 0)
                {
                    // 当前是授权二维码页面请求，加载授权二维码页面
                    browser.Load(e.Url);
                    qrcodeFlag = 1;
                }
                else if (iAuth != JuliangAuthStatusEnum.auth && this.authType == 0 && e.Url.IndexOf("open.douyin.com/qrconnect?container") > 0 && qrcodeFlag == 1)
                {
                    // 当前已加载完授权二维码页面，显示窗体
                    qrcodeFlag = 2;
                    BeginInvoke(showWebForm);
                }
                else if (this.authType == 1 && e.Url.IndexOf("open.douyin.com/qrconnect?container") > 0)
                {
                    // 当前是子账号登录，当前已加载完授权二维码页面，切成成手机登录
                    string js_str = @"
                            const div = document.querySelector('div.login-switcher--qrPhone');
                            if(div){
                                div.click();
                            }
                     ";

                    runScript(js_str);

                    js_str = @"
                        setTimeout(() => {
                            const div2 = document.querySelector('div.login-switcher--cell');
                            if(div2){
                                div2.style.display = 'none';
                            }
                        }, 100);
                     ";

                    runScript(js_str);

                    js_str = @"
                        setTimeout(() => {
                            const div2 = document.querySelector('div.login-switcher--cell');
                            if(div2){
                                div2.style.display = 'none';
                            }
                        }, 800);
                    ";

                    runScript(js_str);
                }
            }   
        }


        /// <summary>
        /// 窗体激活时触发
        /// </summary>
        private void frmWeb_Activated(object sender, EventArgs e)
        {
            if (firstActivateForm)
            {
                // 当前是第一次激活窗体，隐藏窗体
                firstActivateForm = false;
                BeginInvoke(hideWebForm);
            }
        }

        /// <summary>
        /// 窗体关闭前触发
        /// </summary>
        private void frmWeb_FormClosing(object sender, FormClosingEventArgs e)
        {
            BeginInvoke(hideWebForm);
            // 释放浏览器资源
            try
            {
                if (e.CloseReason == CloseReason.UserClosing)
                {
                    // 窗体是通过点击关闭按钮关闭的
                    JuliangUtils.authing = false;

                    if(!isTemp && anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.authing)
                    {
                        AnchorBll.UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.unAuth);
                    }
                }

                if(!this.isTemp)
                {
                    JuliangUtils.juliangFormList.TryRemove(anchorInfo.SecUid, out JuliangForm juliangForm);
                }
                
                if (browser != null)
                {
                    browser.GetBrowserHost()?.CloseBrowser(true);

                    var rc = browser.GetBrowserHost()?.RequestContext;

                    this.Controls.Remove(browser);
                    browser.Dispose();
                    browser = null;
                    //StopCookieCheckTimer();
                    
                    if (rc != null)
                    {
                        // 断开网络连接，释放缓存句柄
                        rc.ClearHttpAuthCredentials();
                        rc.ClearCertificateExceptions(null);
                        rc.ClearSchemeHandlerFactories();
                        rc.Dispose();
                    }
                }
                
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"释放巨量窗体浏览器资源失败");
            }
            
        }
        // 停止多线程计时器
        private void StopCookieCheckTimer()
        {
            if (_cookieCheckTimer != null)
            {
                _cookieCheckTimer.Stop(); // 停止计时
                _cookieCheckTimer.Enabled = false; // 关闭自动重置
                _cookieCheckTimer.Tick -= CookieCheckTimer_Tick; // 解绑事件
                _cookieCheckTimer.Dispose(); // 释放资源
                _cookieCheckTimer = null;
            }
        }

        /// <summary>
        /// 执行js脚本
        /// </summary>
        /// <param name="script">js脚本</param>
        public void delegatExecScript(string script)
        {
            System.Threading.Tasks.Task.Run(() =>
            {
                
                try
                {
                    //Thread.Sleep(5000);

                    if (browser != null)
                    {
                        browser.ExecuteScriptAsync(script);
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"{ex}", $"执行巨量js脚本出现异常");
                }
                
                FileUtils.log("执行成功");
            });
            
        }
        /// <summary>
        /// 执行js脚本
        /// </summary>
        /// <param name="script">js脚本</param>
        public void runScript(string script)
        {
            BeginInvoke(execJavaScript, script);
        }

    }
}
