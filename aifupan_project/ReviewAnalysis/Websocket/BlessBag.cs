using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using System.Web;
using douyin.Utils;
using EmbedIO.Sessions;
using Jint;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.blessBag;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enums;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.Websocket
{
    /// <summary>
    /// 获取福袋
    /// </summary>
    public class BlessBag
    {

        public volatile static string taskId = Guid.NewGuid().ToString();
        /// <summary>
        /// 福袋接口
        /// </summary>
        public static string blessBagUrl = "https://live.douyin.com/webcast/lottery/melon/lottery_info/";

        /// <summary>
        /// UA
        /// </summary>
        private static string ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0";

        /// <summary>
        /// 定时抓取福袋的信息
        /// </summary>
        public static void init()
        {

            Task.Run(async () =>
            {
                taskId = Guid.NewGuid().ToString();
                string tempTaskId = taskId;
                while (true)
                {
                    if (tempTaskId == taskId)
                    {

                        if (ReplayHttpUtils.UserClientVersion == ClientVersion.Replay)
                        {
                            await grabBlessBag();
                        }
                    }
                    else
                    {
                        break;
                    }

                    Thread.Sleep(30000);
                }
            });
        }

        /// <summary>
        /// 查询福袋
        /// </summary>
        /// <returns></returns>
        public static async Task grabBlessBag()
        {
            ConcurrentDictionary<string, AnchorRecordBll> recordingList = AnchorBll.recordingList;
            if (recordingList != null && recordingList.Count > 0)
            {
                // 获取sessionId
                string sessionId = await getSessionId();
                if (string.IsNullOrEmpty(sessionId))
                {
                    FileUtils.LogError("sessionId为空", "福袋获取sessionId错误");
                    return;
                }

                // 创建新的 HttpClient 实例以避免历史 Cookie（或清空当前实例的默认 Cookie）
                var newHandler = new HttpClientHandler
                {
                    UseCookies = false // 关闭自动 Cookie 管理
                };
                using (var client = new HttpClient(newHandler))
                {
                    // 添加请求头
                    client.DefaultRequestHeaders.Add("Cookie", "sessionid=" + sessionId);
                    client.DefaultRequestHeaders.Add("User-Agent", ua);
                    client.DefaultRequestHeaders.Add("Accept", "application/json, text/plain, */*");
                    client.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                    client.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Microsoft Edge\";v=\"145\", \"Chromium\";v=\"145\"");
                    client.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                    client.DefaultRequestHeaders.Add("sec-fetch-site", "same-origin");
                    client.DefaultRequestHeaders.Add("sec-fetch-mode", "cors");
                    client.DefaultRequestHeaders.Add("sec-fetch-dest", "empty");
                    client.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
                    client.DefaultRequestHeaders.Add("priority", "u=1, i");

                    foreach (string key in AnchorBll.recordingList.Keys)
                    {
                        if (AnchorBll.recordingList.TryGetValue(key, out AnchorRecordBll item))
                        {
                            AnchorInfo anchor = item.GetAnchorInfo();
                            VideoEntity video = item.GetVideo();

                            // 构建请求参数（使用 List 保证顺序与 Python 一致）
                            string msToken = getMsToken();
                            var parameters = new List<KeyValuePair<string, string>>()
                            {
                                new KeyValuePair<string, string>("aid", "6383"),
                                new KeyValuePair<string, string>("app_name", "douyin_web"),
                                new KeyValuePair<string, string>("live_id", "1"),
                                new KeyValuePair<string, string>("device_platform", "web"),
                                new KeyValuePair<string, string>("language", "zh-CN"),
                                new KeyValuePair<string, string>("enter_from", "link_share"),
                                new KeyValuePair<string, string>("cookie_enabled", "true"),
                                new KeyValuePair<string, string>("screen_width", "1920"),
                                new KeyValuePair<string, string>("screen_height", "1080"),
                                new KeyValuePair<string, string>("browser_language", "zh-CN"),
                                new KeyValuePair<string, string>("browser_platform", "Win32"),
                                new KeyValuePair<string, string>("browser_name", "Edge"),
                                new KeyValuePair<string, string>("browser_version", "145.0.0.0"),
                                new KeyValuePair<string, string>("room_id", anchor.BatchNumber),
                                new KeyValuePair<string, string>("query_from", "1"),
                                new KeyValuePair<string, string>("msToken", msToken)
                            };

                            // 获取 a_bogus
                            string a_bogus = getAbogus(ua, parameters);
                            if (!string.IsNullOrEmpty(a_bogus))
                            {
                                parameters.Add(new KeyValuePair<string, string>("a_bogus", a_bogus));
                            }

                            // 构建请求URL（使用 Uri.EscapeDataString 与 Python urlencode 保持一致）
                            var queryString = string.Join("&", parameters.Select(kv => $"{kv.Key}={Uri.EscapeDataString(kv.Value)}"));
                            string url = $"{blessBagUrl}?{queryString}";

                            // 添加 referer
                            client.DefaultRequestHeaders.Remove("referer");
                            client.DefaultRequestHeaders.Add("referer", $"https://live.douyin.com/{anchor.BatchNumber}");

                            try
                            {
                                // 构建请求头
                                var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, url);

                                using (HttpResponseMessage response = await client.SendAsync(httpRequestMessage))
                                {
                                    if (response.IsSuccessStatusCode)
                                    {
                                        // 通讯成功 
                                        string dataStr = await response.Content.ReadAsStringAsync();

                                        if (!string.IsNullOrEmpty(dataStr))
                                        {
                                            try
                                            {
                                                dynamic responseData = JsonConvert.DeserializeObject<dynamic>(dataStr);

                                                // 检查是否有数据
                                                if (responseData?.data?.lottery_info != null && responseData.status_code == 0)
                                                {
                                                    dynamic lotteryInfo = responseData.data.lottery_info;

                                                    // 完善赋值
                                                    BlessBagBo bo = new BlessBagBo();
                                                    bo.videoId = video.videoId;
                                                    bo.batchNumber = anchor.BatchNumber;

                                                    // 福袋信息的核心数据
                                                    bo.lotteryInfo = JsonConvert.SerializeObject(lotteryInfo);

                                                    // 参与条件
                                                    if (lotteryInfo.conditions != null)
                                                    {
                                                        bo.conditions = JsonConvert.SerializeObject(lotteryInfo.conditions);
                                                    }

                                                    // 提取其他关键字段
                                                    if (lotteryInfo.prize_count != null)
                                                    {
                                                        bo.prizeCount = (int)lotteryInfo.prize_count;
                                                    }

                                                    if (lotteryInfo.lucky_count != null)
                                                    {
                                                        bo.luckyCount = (int)lotteryInfo.lucky_count;
                                                    }

                                                    if (lotteryInfo.count_down != null)
                                                    {
                                                        bo.countDown = (int)lotteryInfo.count_down;
                                                    }

                                                    if (lotteryInfo.start_time != null)
                                                    {
                                                        bo.startTime = (long)lotteryInfo.start_time;
                                                    }

                                                    if (lotteryInfo.draw_time != null)
                                                    {
                                                        bo.drawTime = (long)lotteryInfo.draw_time;
                                                    }

                                                    if (lotteryInfo.current_time != null)
                                                    {
                                                        bo.currentTime = (long)lotteryInfo.current_time;
                                                    }

                                                    if (lotteryInfo.candidate_num != null)
                                                    {
                                                        bo.candidateNum = (int)lotteryInfo.candidate_num;
                                                    }

                                                    // 当前时间戳
                                                    if (responseData.extra?.now != null)
                                                    {
                                                        bo.now = (long)responseData.extra.now;
                                                    }

                                                    // 保存福袋数据
                                                    BlessBagApi.save(bo);

                                                    FileUtils.log($"已保存福袋数据，videoId: {video.videoId}，批次号: {anchor.BatchNumber}", "福袋数据");
                                                }
                                            }
                                            catch (Exception ex)
                                            {
                                                FileUtils.LogError($"解析福袋数据出错: {ex.Message}", "福袋数据解析异常");
                                            }
                                        }
                                    }
                                    else
                                    {
                                        // 记录非成功状态码的日志 
                                        FileUtils.LogError($"BatchNumber = {anchor.BatchNumber}，状态码: {response.StatusCode}", $"获取福袋请求失败");
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                FileUtils.LogError($"{e}", $"获取福袋请求发生异常");
                            }
                        }

                        Thread.Sleep(3000);
                    }

                }



                
            }
        }

        /// <summary>
        /// 获取福袋的sessionId
        /// </summary>
        /// <returns></returns>
        public static async Task<string> getSessionId()
        {
            BlessBagSessionId session = new BlessBagSessionId();
            return await session.getBlessBagSessionId();
        }

        /// <summary>
        /// 生成msToken（与 Python 逻辑一致，长度为 baseStr.Length - 1 = 62）
        /// </summary>
        /// <returns></returns>
        private static string getMsToken()
        {
            string baseStr = "ABCDEFGHIGKLMNOPQRSTUVwXYZabcdefghigklmnopqrstuVwxyz0123456789=";
            int length = baseStr.Length - 1; // Python: length = len(base_str) - 1
            Random random = new Random();
            char[] msToken = new char[length];
            for (int i = 0; i < length; i++)
            {
                msToken[i] = baseStr[random.Next(0, length + 1)]; // Python: random.randint(0, length)
            }
            return new string(msToken);
        }

        /// <summary>
        /// 获取a_bogus签名
        /// </summary>
        /// <param name="ua">User-Agent</param>
        /// <param name="parameters">请求参数</param>
        /// <returns></returns>
        private static string getAbogus(string ua, List<KeyValuePair<string, string>> parameters)
        {
            try
            {
                var engine = new Engine();
                // 设置 global 对象（JS 代码中使用了 Node.js 的 global）
                engine.SetValue("global", engine.Global);
                // 模拟 performance 对象
                engine.Execute("var performance = { now: function() { return Date.now(); } };");
                engine.Execute(DouYinUtils.getFuDaiABJS());

                if (parameters == null || parameters.Count == 0)
                    return "";

                // 使用 Uri.EscapeDataString 编码（与 Python urlencode 保持一致）
                var encodedParts = parameters.Select(pair =>
                    $"{pair.Key}={Uri.EscapeDataString(pair.Value)}");
                string paramsStr = string.Join("&", encodedParts);


                // 调用函数并传递参数（ua, paramsStr, dataStr）
                var result = engine.Invoke("main", ua, paramsStr, "");

                return result?.ToString() ?? string.Empty;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"msg = {ex.Message}, stack = {ex.StackTrace}", "getAbogus错误");
                return "";
            }
        }

    }
}
