using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.enums;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.system;
using ReviewAnalysis.Websocket.Entity;
using ReviewAnalysis.Websocket.socketAddress;
using Swan.Formatters;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Web;

namespace ReviewAnalysis.Websocket
{

    public class SendErrorData
    {
        /// <summary>
        /// 错误类型  jsSocketAddress:js计算的websocket地址不能使用
        /// </summary>
        public string type { get; set; }

        /// <summary>
        /// 错误
        /// </summary>
        public WebsocketEntity data { get; set; }

    }

    public class WebsocketConnection
    {
        public static volatile ConcurrentDictionary<string, WebsocketEntity> websocketList = new ConcurrentDictionary<string, WebsocketEntity>();

        public static string BarrageGrabPath = Path.GetFullPath("BarrageGrab.exe");

        public static bool isStart = false;

        public static int websocketWay = 0;

        // 地址错误后重试几次
        public static int maxAddressErrorNum = 2;
        // 几次没有数据后切换代理
        public static int maxNoDataNum = 5;
        // 禁用代理的时间
        public static DateTime disableProxyTime = DateTime.Now;
        // 打印日志的时间，这个时间和禁用代理的时间不一致就打印日志
        public static DateTime printLogTime = DateTime.Now;

        public static int danMuMaxNum = 5000;

        public static int[] WebsocketWayDates = { 1, 3 }; // WebsocketWayDates[0] <= n < WebsocketWayDates[1]

        public static Timer sendError = null; // 错误信息多久发一次服务器
        public static Timer getWebsocketWayTimer = null; // 多久获取一次服务的websocket地址的方式
        public static volatile ConcurrentBag<SendErrorData> errorList = new ConcurrentBag<SendErrorData>(); // 错误的缓存

        public static void init()
        {
            if (isStart)
            {
                return;
            }
            SocketAddressInit.init();
            SocketAddressInit.getWebsocketAddress += getWebsocketAddress;
            SocketAddressInit.getWebsocketTimeout += failWebsocketAddress;
            FileUtils.log("获取websocket的事件绑定成功");

            new Thread(() =>
            {
                // 设置定时器在 20-30 分钟之间执行一次回调
                Random rand = new Random();
                int initialDelay = rand.Next(WebsocketWayDates[0], WebsocketWayDates[1]); // 初始延迟 20 到 30 分钟之间
                getWebsocketWayTimer = new Timer(getWebsocketWay, null, TimeSpan.FromMinutes(initialDelay), Timeout.InfiniteTimeSpan);

                // 获取websocket地址的方式
                getWebsocketWay(null);


                // 启动websocket获取
                startWebsocketCheck();
            }).Start();

            // 开启websocket数据写入
            WebsocketDataHandle.Init();

            isStart = true;
        }


        /// <summary>
        /// 启动websocket进程
        /// </summary>
        public static void startWebsocketCheck()
        {
            while (true)
            {
                try
                {
                    if(!AnchorBll.firstRoundDone)
                    {
                        Thread.Sleep(1000);
                        continue;
                    }

                    Thread.Sleep(5000);
                    if (websocketList.Count > 0)
                    {
                        foreach (string key in websocketList.Keys)
                        {
                            if (websocketList.TryGetValue(key, out WebsocketEntity item))
                            {
                                item.status = 0;
                                websocketList.AddOrUpdate(key, item, (a, b) => item);
                            }
                        }
                    }

                    if (AnchorBll.recordingList.Count > 0)
                    {
                        foreach (string key in AnchorBll.recordingList.Keys)
                        {
                            if (AnchorBll.recordingList.TryGetValue(key, out AnchorRecordBll item))
                            {
                                AnchorInfo anchor = item.GetAnchorInfo();
                                if(anchor.platform != 0)
                                {
                                    continue;
                                }
                                if(ReplayHttpUtils.UserClientVersion.Equals(ClientVersion.Record) && (anchor.pureRecordOnlineNum == null || anchor.pureRecordOnlineNum == 0))
                                {
                                    continue;
                                }

                                string url = anchor.LiveUrl;
                                if (websocketList.TryGetValue(url, out WebsocketEntity websocket))
                                {
                                    websocket.status = 1;
                                    if (websocket.videoId != item.GetVideo().videoId)
                                    {
                                        websocket.videoId = item.GetVideo().videoId;
                                        websocket.danMuNum = -1;
                                    }
                                }
                                else
                                {
                                    WebsocketEntity websocket1 = new WebsocketEntity
                                    {
                                        url = url,
                                        status = 1,
                                        batchNumber = anchor.BatchNumber,
                                        secUid = anchor.SecUid,
                                        videoId = item.GetVideo().videoId,
                                        type = anchor.AnchorPlatform,
                                        gatherWay = websocketWay,
                                        //gatherWay = 1,
                                        hasDanMu = anchor.IsBarrageMonitoring,
                                        danMuMaxNum = danMuMaxNum
                                    };
                                    websocketList.AddOrUpdate(url, websocket1, (a, b) => websocket1);
                                }

                                if (websocketList.TryGetValue(url, out WebsocketEntity websocket2))
                                {
                                    // 判断是否历史的数据场次中和当前的场次号是否一样
                                    if (websocket2.batchNumber != anchor.BatchNumber)
                                    {
                                        // 场次号不一致，说明是新一场直播，删除旧条目并重建
                                        FileUtils.log($"场次号不一致，删除旧条目并重建，url={url}, 旧batchNumber={websocket2.batchNumber}, 新batchNumber={anchor.BatchNumber}", "websocket场次切换");
                                        closeWebsocketLink(url, websocket2);

                                        // 创建全新条目
                                        WebsocketEntity newWebsocket = new WebsocketEntity
                                        {
                                            url = url,
                                            status = 1,
                                            batchNumber = anchor.BatchNumber,
                                            secUid = anchor.SecUid,
                                            videoId = item.GetVideo().videoId,
                                            type = anchor.AnchorPlatform,
                                            gatherWay = websocketWay,
                                            hasDanMu = anchor.IsBarrageMonitoring,
                                            danMuMaxNum = danMuMaxNum
                                        };
                                        websocketList.AddOrUpdate(url, newWebsocket, (a, b) => newWebsocket);
                                        websocket2 = newWebsocket;
                                    }
                                    if (websocket2.gatherWay == 1)
                                    {
                                        if (websocket2.webAddressStatus == 0)
                                        {
                                            FileUtils.log($"浏览器待获取抖音的websocket地址，url={url},batchNumber={websocket2.batchNumber}");
                                            SocketAddressInit.setWebsocketAddress(websocket2);
                                            websocket2.webAddressStatus = 0;
                                        }
                                        else
                                        {
                                            if (websocket2.websocketLinkStatus == 0 || websocket2.websocketLinkStatus == 3)
                                            {
                                                SatatBarrageGrabExe(url, websocket2);
                                            }
                                        }
                                    }
                                    else if (websocket2.gatherWay == 0)
                                    {
                                        websocket2.webAddressStatus = 2;
                                        if (websocket2.websocketLinkStatus == 0 || websocket2.websocketLinkStatus == 3)
                                        {
                                            if (websocket2.process == null || websocket2.process.HasExited)
                                            {
                                                SatatBarrageGrabExe(url, websocket2);
                                            }
                                        }
                                    }
                                }
                            }

                            Thread.Sleep(4000);
                        }
                    }

                    // 检查关闭的websocket地址，从而关闭websocket链接
                    if (websocketList.Count > 0)
                    {
                        foreach (string key in websocketList.Keys)
                        {
                            if (websocketList.TryGetValue(key, out WebsocketEntity data))
                            {
                                if (data.status == 0)
                                {
                                    closeWebsocketLink(key, data);
                                }
                                else
                                {
                                    //if(data.gatherWay == 0)
                                    //{
                                    //    // 链接websocket
                                    //    if (data.webAddressStatus == 1 && data.websocketLinkStatus == 0)
                                    //    {
                                    //        addWebsocketLink(data);
                                    //    }
                                    //}else if (data.gatherWay == 1)
                                    //{
                                    //    // TODO 待实现
                                    //}
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    FileUtils.LogError($"管理websocket发生异常: {e.Message}");
                    FileUtils.LogError($"管理websocket发生异常,堆栈跟踪: {e.StackTrace}");
                }
                Thread.Sleep(1000);
            }
        }

        /// <summary>
        /// 启动websocket的进程（使用CreateProcess API避免继承父进程的其他句柄）
        /// </summary>
        /// <param name="url"></param>
        /// <param name="data"></param>
        public static void SatatBarrageGrabExe(string url, WebsocketEntity data)
        {
            try
            {
                if (!File.Exists(BarrageGrabPath))
                {
                    FileUtils.LogError("BarrageGrab.exe文件不存在");
                    return;
                }

                if (data.process != null)
                {
                    // 停止进程（如果正在运行）
                    if (!data.process.HasExited)
                    {
                        try
                        {
                            data.process?.Kill();
                            data.process?.WaitForExit();
                        }
                        catch (Exception ex)
                        {
                            FileUtils.log($"启动websocket的进程-关闭进程报错: {ex.Message}");
                        }
                    }

                    // 释放资源
                    data.process.Dispose();
                    data.process = null;
                }

                string liveId = "";
                string batchNumber = data.batchNumber;
                if (data.gatherWay == 1)
                {
                    liveId = $"{data.wssUrl}~{data.ttwid}";
                }
                else if (data.gatherWay == 0)
                {
                    liveId = data.url.Replace("https://live.douyin.com/", "");
                }
                else
                {
                    FileUtils.LogError($"获取websocket地址方式未知={data.gatherWay}", "获取websocket地址方式出问题");
                }
                // 代理
                string proxyHttpUrl = "";
                if (DateTime.Now > disableProxyTime)
                {
                    proxyHttpUrl = WebsocketProxyCacheManager.getChangeProxyHttpUrl(data.proxyHttpUrl, data.websocketNoDataNum != 0);
                }
                if(DateTime.Now < disableProxyTime && printLogTime != disableProxyTime)
                {
                    FileUtils.log("已经停止在线人数采集", "Websocket的长效代理使用次数超出限制");
                    printLogTime = disableProxyTime;
                }
                printLogTime = DateTime.Now;
                string arguments = $"{liveId} {batchNumber} {proxyHttpUrl}";
                FileUtils.log($"url={data.url}，Arguments={arguments}", "启动websocket的进程");
                data.proxyHttpUrl = proxyHttpUrl;
                if (!string.IsNullOrEmpty(proxyHttpUrl))
                {
                    data.isProxy = true;
                    FileUtils.log($"proxyHttpUrl={proxyHttpUrl}", "启动websocket的进程-使用代理", true);
                }
                else
                {
                    data.isProxy = false;
                    FileUtils.log($"", "启动websocket的进程-不代理", true);
                }

                // 使用CreateProcess API启动子进程，避免继承父进程的其他句柄（如视频文件句柄）1.20
                string currentUrl = data.url; // 捕获当前url用于回调
                data.process = WebSocketProcessUtils.StartProcessWithoutInheritHandles(
                    BarrageGrabPath,
                    arguments,
                    (outputLine) => SendWebsocketDataFromLine(outputLine, currentUrl),
                    (errorLine) => FileUtils.LogError($"BarrageGrab错误输出: {errorLine}")
                );

                data.websocketLinkStatus = 2;
            }
            catch (Exception e)
            {
                data.websocketLinkStatus = 0;
                FileUtils.LogError($"启动websocket进程-发生异常: {e.Message}");
                FileUtils.LogError($"启动websocket进程-堆栈跟踪: {e.StackTrace}");
            }
        }

        /// <summary>
        /// 处理子进程输出的一行数据（从管道读取）
        /// </summary>
        /// <param name="line"></param>
        /// <param name="url"></param>
        private static void SendWebsocketDataFromLine(string line, string url)
        {
            if (string.IsNullOrEmpty(line)) return;
            line = HttpUtility.UrlDecode(line);
            // 调用原有的处理逻辑，构造DataReceivedEventArgs兼容的参数
            var args = CreateDataReceivedEventArgs(line);
            SendWebsocketData(null, args, url);
        }

        /// <summary>
        /// 创建DataReceivedEventArgs对象（用于兼容原有逻辑）
        /// </summary>
        private static DataReceivedEventArgs CreateDataReceivedEventArgs(string data)
        {
            // DataReceivedEventArgs没有公开构造函数，使用反射创建
            var constructor = typeof(DataReceivedEventArgs).GetConstructor(
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance,
                null, new Type[] { typeof(string) }, null);
            return (DataReceivedEventArgs)constructor.Invoke(new object[] { data });
        }

        /// <summary>
        /// 处理websocket解析后的数据
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        /// <param name="url"></param>
        /// <exception cref="NotImplementedException"></exception>
        private static void SendWebsocketData(object sender, DataReceivedEventArgs e, string url)
        {
            try
            {
                if (e?.Data != null)
                {
                    if (e.Data.Contains("[Info]")) return;

                    if (!websocketList.TryGetValue(url, out WebsocketEntity data))
                    {
                        FileUtils.LogError($"处理websocket解析后的数据-获取WebsocketEntity失败");
                    }

                    if(data == null)
                    {
                        return;
                    }

                    bool flag = true;
                    bool noData = true;
                    //FileUtils.log($"接收websocket数据：{e.Data}");
                    if (e.Data.Contains("fensituan"))
                    {
                        FensTuan fensTuan = new FensTuan()
                        {
                            type = "fensituan",
                            nickName = e.Data.Replace("fensituan", ""),
                        };
                        WebsocketDataHandle.Push("fensituan", data, fensTuan);
                    }
                    else if (e.Data.Contains("danmu"))
                    {
                        if (data.hasDanMu == 1 && (data.danMuMaxNum == -1 || data.danMuNum < data.danMuMaxNum))
                        {
                            string content = e.Data.Replace("danmu", "");
                            if (!string.IsNullOrEmpty(content))
                            {
                                DanMu dianzan = JsonConvert.DeserializeObject<DanMu>(content);
                                dianzan.nickName = HttpUtility.UrlDecode(dianzan.nickName);
                                dianzan.content = HttpUtility.UrlDecode(dianzan.content);
                                dianzan.type = "danmu";
                                dianzan.dataMaxNum = data.danMuMaxNum;
                                WebsocketDataHandle.Push("danmu", data, dianzan);
                            }
                        }
                    }
                    else if (e.Data.Contains("guanzhu"))
                    {
                        string name = e.Data.Replace("guanzhu", "");
                        GuanZhu guanzhu = new GuanZhu()
                        {
                            type = "jinru",
                            content = $"{name} 关注了主播",
                            nickName = name,
                        };
                        WebsocketDataHandle.Push("guanzhu", data, guanzhu);
                    }
                    else if (e.Data.Contains("renshu"))
                    {
                        string renshuData = e.Data.Replace("renshu", "");
                        if (!string.IsNullOrEmpty(renshuData))
                        {
                            var temp = renshuData.Split(new char[] { '-' });
                            if (temp.Length > 0)
                            {
                                RenShu renshu = new RenShu()
                                {
                                    type = "renshu",
                                    content = $"{temp[0]}",
                                };
                                WebsocketDataHandle.Push("renshu", data, renshu);

                                if (AnchorBll.recordingList.TryGetValue(data.secUid, out AnchorRecordBll recordBll))
                                {
                                    AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(data.secUid);
                                    if (anchor != null)
                                    {
                                        anchor.OnlineNumber = temp[0];
                                        AnchorCacheManager.SetAnchorCache(anchor);
                                    }
                                    recordBll.GetAnchorInfo().OnlineNumber = temp[0];
                                }

                                if (temp.Length > 1)
                                {
                                    ChangGuan changGuan = new ChangGuan()
                                    {
                                        type = "leijiguankanrenshu",
                                        content = $"{temp[1]}",
                                    };
                                    WebsocketDataHandle.Push("leijiguankanrenshu", data, changGuan);
                                }
                            }
                        }
                    }
                    else if (e.Data.Contains("网络断掉"))
                    {
                        CuoWu cuowu = new CuoWu()
                        {
                            type = "error",
                            content = "网络断掉"
                        };
                        FileUtils.log($"{Json.Serialize(data)}", "websocket-网络断掉");
                        WebsocketDataHandle.PushError("error", data, cuowu);
                        data.websocketLinkStatus = 3;
                    }
                    else if (e.Data.Contains("地址出错"))
                    {
                        CuoWu cuowu = new CuoWu()
                        {
                            type = "error",
                            content = "地址出错"
                        };
                        WebsocketDataHandle.PushError("error", data, cuowu);
                        FileUtils.log($"{Json.Serialize(data)}", "websocket链接地址出错");
                        data.websocketLinkErroeNum = data.websocketLinkErroeNum + 1;
                        flag = false;
                        if (data.websocketLinkErroeNum >= maxAddressErrorNum)
                        {
                            // 通知服务器，
                            if (sendError == null)
                            {
                                sendError = new Timer(sendErroeData, null, TimeSpan.FromSeconds(60 * 5), Timeout.InfiniteTimeSpan);
                            }
                            errorList.Add(new SendErrorData()
                            {
                                type = "jsSocketAddress",
                                data = data,
                            });
                            data.gatherWay = data.gatherWay == 0 ? 1 : 0;
                            if (data.gatherWay == 1)
                            {
                                data.webAddressStatus = 0;
                            }
                            data.websocketLinkErroeNum = 0;
                            data.websocketLinkStatus = 4;
                            FileUtils.log($"Websocket获取地址更换方式：{data.gatherWay}");
                        }
                        else
                        {
                            data.websocketLinkStatus = 3;
                        }
                    }
                    else if (e.Data.Contains("30s内无数据"))
                    {
                        FileUtils.log($"30s内无数据，准备重新链接：{data.url}");
                        CuoWu cuowu = new CuoWu()
                        {
                            type = "error",
                            content = "30s内无数据"
                        };
                        WebsocketDataHandle.PushError("error", data, cuowu);
                        data.websocketLinkStatus = 3;
                        data.websocketNoDataNum++;
                        noData = false;
                        if (data.websocketNoDataNum >= maxNoDataNum)
                        {
                            data.websocketNoDataNum = 0;
                            WebsocketProxyCacheManager.addProxy();
                            SystemStatusTimer.openNetworkFatiguePage();
                        }
                    }
                    else if (e.Data.Contains("主播下播"))
                    {
                        //CuoWu cuowu = new CuoWu()
                        //{
                        //    type = "error",
                        //    content = "主播下播"
                        //};
                        //WebsocketDataHandle.PushError("error", data, cuowu);
                    }
                    else if (e.Data.Contains("暂停直播"))
                    {
                        //CuoWu cuowu = new CuoWu()
                        //{
                        //    type = "error",
                        //    content = "暂停直播"
                        //};
                        //WebsocketDataHandle.PushError("error", data, cuowu);
                    }
                    else if (e.Data.Contains("websocket代理ip过期"))
                    {
                        FileUtils.log(data.url, "websocket代理ip过期");
                        data.websocketLinkStatus = 3;
                    }
                    else if (e.Data.Contains("websocket的代理错误"))
                    {
                        FileUtils.log(data.url, "websocket代理ip过期");
                        data.websocketLinkStatus = 3;
                    }
                    if (flag)
                    {
                        data.websocketLinkErroeNum = 0;
                    }
                    if (noData)
                    {
                        data.websocketNoDataNum = 0;
                    }
                    websocketList.AddOrUpdate(data.url, data, (a, b) => data);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}，data={e?.Data}", "接收websocket解析的数据报错");
                FileUtils.LogError($"接收websocket解析的数据报错-堆栈跟踪: {ex.StackTrace}");
            }
        }

        public static void addWebsocketLink(WebsocketEntity data)
        {
            //DouyinConnection.addWebsocketLink(data);
        }

        public static void getWebsocketAddress(string url, string ttwid, string wssUrl)
        {
            try
            {
                FileUtils.log($"websocket地址获取成功，url={url}, ttwid={ttwid},wssUrl={wssUrl}");
                if (websocketList.TryGetValue(url, out WebsocketEntity data))
                {
                    data.webAddressStatus = 1;
                    data.ttwid = ttwid;
                    data.wssUrl = wssUrl;

                    if (data.websocketLinkStatus == 0 || data.websocketLinkStatus == 4)
                    {
                        //addWebsocketLink(data);
                        SatatBarrageGrabExe(data.url, data);
                    }

                    websocketList.AddOrUpdate(url, data, (a, b) => data);
                    if (websocketList.TryGetValue(url, out WebsocketEntity temp))
                    {
                        FileUtils.log($"websocket地址获取成功，WebsocketEntity={temp}");
                    }
                }
            }
            catch(Exception e)
            {
                FileUtils.LogError($"获取websocket地址成功后-发生异常: {e.Message}");
                FileUtils.LogError($"获取websocket地址成功后-堆栈跟踪: {e.StackTrace}");
            }
        }

        public static void failWebsocketAddress(string url)
        {
            FileUtils.LogError($"{url}", "获取websocket地址失败");
            if (!websocketList.TryGetValue(url, out WebsocketEntity data))
            {
                FileUtils.LogError($"处理websocket解析后的数据-获取WebsocketEntity失败");
            }

            if (data == null)
            {
                return;
            }

            data.websocketLinkErroeNum = data.websocketLinkErroeNum + 1;
            if (data.websocketLinkErroeNum >= maxAddressErrorNum)
            {
                data.gatherWay = data.gatherWay == 0 ? 1 : 0;
                if (data.gatherWay == 1)
                {
                    data.webAddressStatus = 0;
                    data.websocketLinkStatus = 4;
                }
                else
                {
                    data.websocketLinkStatus = 3;
                }
                data.websocketLinkErroeNum = 0;
                FileUtils.log($"Websocket获取地址更换方式：{data.gatherWay}");
            }
            else
            {
                data.webAddressStatus = 0;
                data.websocketLinkStatus = 3;
            }

            websocketList.AddOrUpdate(data.url, data, (a, b) => data);
        }

        public static void closeWebsocketLink(string url, WebsocketEntity data)
        {
            try
            {
                // 删除
                if (websocketList.TryRemove(url, out WebsocketEntity websocket))
                {
                    websocket.websocketTaskId = null;
                    websocket.status = 2;
                    Debug.WriteLine($"直播关闭了，url={url}");

                    try
                    {
                        websocket?.process?.Kill();
                        websocket?.process?.WaitForExit();
                    }
                    catch (Exception e)
                    {

                    }


                    FileUtils.log($"{url}", "websocket链接关闭");
                }
            }
            catch(Exception e)
            {
                FileUtils.LogError($"关闭Websocket进程-发生异常: {e.Message}");
                FileUtils.LogError($"关闭Websocket进程-堆栈跟踪: {e.StackTrace}");
            }
        }

        /// <summary>
        /// websocket采集错误上报服务器
        /// </summary>
        /// <param name="state"></param>
        public static void sendErroeData(object state)
        {
            try
            {
                if (errorList != null && errorList.Count > 0)
                {
                    var request = new HttpRequestMessage();
                    request.Method = HttpMethod.Post;
                    request.Headers.Add("token", ReplayHttpUtils.Token);
                    request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/openapi/v1930/sendErrorMessage");

                    Dictionary<string, string> dictionary = new Dictionary<string, string>();
                    dictionary.Add("cpuId", ReplayHttpUtils.cpuid);
                    dictionary.Add("userId", ReplayHttpUtils.UserId);
                    dictionary.Add("anchorErrorNum", errorList.Count.ToString());
                    dictionary.Add("type", "jsSocketAddress");
                    dictionary.Add("currentDate", ServerTimeUtils.getCurrentTimeStr());

                    request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

                    using (HttpClient Client = new HttpClient())
                    {
                        HttpResponseMessage result = Client.SendAsync(request).Result;
                        errorList = new ConcurrentBag<SendErrorData>();
                    }
                }
            }
            catch(Exception e )
            {
                FileUtils.LogError($"websocket采集错误上报服务器-发生异常: {e.Message}");
                FileUtils.LogError($"websocket采集错误上报服务器-堆栈跟踪: {e.StackTrace}");
            }
        }

        public static void getWebsocketWay(object state)
        {
            try
            {
                List<DictDataListVo> websocketAddressTypeList = SystemApi.dictDataListByCode("websocket_address_type");
                List<DictDataListVo> maxBarrageNumList = SystemApi.dictDataListByCode("max_barrage_num");

                if (websocketAddressTypeList != null && websocketAddressTypeList.Count > 0)
                {
                    int.TryParse(websocketAddressTypeList[0].value, out websocketWay);
                }

                if (maxBarrageNumList != null && maxBarrageNumList.Count > 0)
                {
                    int.TryParse(maxBarrageNumList[0].value, out danMuMaxNum);
                }

                if (state != null)
                {
                    // 设置一个随机时间间隔：20 到 30 分钟
                    Random rand = new Random();
                    int minutes = rand.Next(WebsocketWayDates[0], WebsocketWayDates[1]); // 生成 20 到 30 之间的随机数
                    TimeSpan nextInterval = TimeSpan.FromMinutes(minutes);
                    getWebsocketWayTimer.Change(nextInterval, Timeout.InfiniteTimeSpan);
                }
            }
            catch(Exception e)
            {
                FileUtils.LogError($"获取websocket地址的方式-发生异常: {e.Message}");
                FileUtils.LogError($"获取websocket地址的方式-堆栈跟踪: {e.StackTrace}");
            }
        }
    }
}
