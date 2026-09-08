using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using System.Web;
using ReviewAnalysis.Utils;
using ReviewAnalysis.ShortVideo.DouYin.dto;
using douyin.Utils;
using System.Net;
using COSXML.Network;
using HttpClient = System.Net.Http.HttpClient;
using Newtonsoft.Json.Linq;

namespace ReviewAnalysis.ShortVideo.DouYin
{

    public class DouyinSpider
    {
        #region public方法
        /// <summary>
        /// 巨量算数搜索达人
        /// </summary>
        /// <param name="keyword"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public List<JuLiangUserInfoDto> JuLiangSearchDaren(string keyword, int total = 30)
        {
            string url = "https://trendinsight.oceanengine.com/api/v2/daren/get_sug_great_user_list";

            var data = new
            {
                total = $"{total}",
                keyword = keyword
            };
            // 创建新的 HttpClient 实例以避免历史 Cookie（或清空当前实例的默认 Cookie）
            var newHandler = new HttpClientHandler
            {
                UseCookies = false // 关闭自动 Cookie 管理
            };
            using (var client = new HttpClient(newHandler))
            {
                // 设置请求头
                client.DefaultRequestHeaders.Add("accept", "application/json, text/plain, */*");
                client.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
                client.DefaultRequestHeaders.Add("appsource", "PC");
                client.DefaultRequestHeaders.Add("cache-control", "no-cache");
                client.DefaultRequestHeaders.Add("origin", "https://trendinsight.oceanengine.com");
                client.DefaultRequestHeaders.Add("pragma", "no-cache");
                client.DefaultRequestHeaders.Add("priority", "u=1, i");
                client.DefaultRequestHeaders.Add("referer", $"https://trendinsight.oceanengine.com/arithmetic-index/daren/search?keyword={HttpUtility.UrlEncode(keyword)}");
                client.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
                client.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                client.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                client.DefaultRequestHeaders.Add("sec-fetch-dest", "empty");
                client.DefaultRequestHeaders.Add("sec-fetch-mode", "cors");
                client.DefaultRequestHeaders.Add("sec-fetch-site", "same-origin");
                client.DefaultRequestHeaders.Add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
                client.DefaultRequestHeaders.Add("x-secsdk-csrf-token", "DOWNGRADE");

                // 设置Cookie
                string sessionId = DouYinSession.GetValidSessionId();
                if (string.IsNullOrEmpty(sessionId))
                {
                    throw new CustomException("获取sessionId失败，请联系管理员");
                }
                client.DefaultRequestHeaders.Add("Cookie", $"sessionid_count={sessionId}");

                var jsonData = JsonConvert.SerializeObject(data);
                var content = new StringContent(jsonData, System.Text.Encoding.UTF8, "application/json");

                try
                {
                    // 构建请求体和请求头
                    var httpRequestMessage = new HttpRequestMessage(HttpMethod.Post, url)
                    {
                        Content = content
                    };
                    httpRequestMessage.Headers.Add("Cookie", $"sessionid_count={sessionId}");

                    //var response = client.PostAsync(url, content).Result;
                    var response = client.SendAsync(httpRequestMessage).Result;


                    var responseString = response.Content.ReadAsStringAsync().Result;
                    var jsonResponse = JsonConvert.DeserializeObject<dynamic>(responseString);

                    if (jsonResponse.status == 0)
                    {
                        var userList = jsonResponse.data.userlist;
                        if (userList != null && userList.Count > 0)
                        {
                            return userList.ToObject<List<JuLiangUserInfoDto>>();
                        }
                        else
                        {
                            FileUtils.log("没有结果");
                            return new List<JuLiangUserInfoDto>() ;
                        }
                    }
                    else
                    {
                        FileUtils.log(jsonResponse.ToString());
                        return null;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log($"请求异常: {ex.Message}");
                    return null;
                }
            }
        }

        /// <summary>
        /// 巨量算数搜索达人详情
        /// </summary>
        /// <param name="keyword"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public JuLiangUserInfo JuLiangAuthorInfo(string userId)
        {
            string url = "https://trendinsight.oceanengine.com/api/v2/daren/get_author_info";

            var data = new
            {
                user_id = userId
            };
            // 创建新的 HttpClient 实例以避免历史 Cookie（或清空当前实例的默认 Cookie）
            var newHandler = new HttpClientHandler
            {
                UseCookies = false // 关闭自动 Cookie 管理
            };
            using (var client = new HttpClient(newHandler))
            {
                // 设置请求头
                client.DefaultRequestHeaders.Add("accept", "application/json, text/plain, */*");
                client.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
                client.DefaultRequestHeaders.Add("appsource", "PC");
                client.DefaultRequestHeaders.Add("cache-control", "no-cache");
                client.DefaultRequestHeaders.Add("origin", "https://trendinsight.oceanengine.com");
                client.DefaultRequestHeaders.Add("pragma", "no-cache");
                client.DefaultRequestHeaders.Add("priority", "u=1, i");
                client.DefaultRequestHeaders.Add("referer", $"https://trendinsight.oceanengine.com/arithmetic-index/daren/detail?uid={userId}");
                client.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
                client.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                client.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
                client.DefaultRequestHeaders.Add("sec-fetch-dest", "empty");
                client.DefaultRequestHeaders.Add("sec-fetch-mode", "cors");
                client.DefaultRequestHeaders.Add("sec-fetch-site", "same-origin");
                client.DefaultRequestHeaders.Add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
                client.DefaultRequestHeaders.Add("x-secsdk-csrf-token", "DOWNGRADE");

                // 设置Cookie
                string sessionId = DouYinSession.GetValidSessionId();
                if (string.IsNullOrEmpty(sessionId))
                {
                    throw new CustomException("获取sessionId失败，请联系管理员");
                }
                client.DefaultRequestHeaders.Add("Cookie", $"sessionid_count={sessionId}");

                var jsonData = JsonConvert.SerializeObject(data);
                var content = new StringContent(jsonData, System.Text.Encoding.UTF8, "application/json");

                try
                {
                    // 构建请求体和请求头
                    var httpRequestMessage = new HttpRequestMessage(HttpMethod.Post, url)
                    {
                        Content = content
                    };
                    httpRequestMessage.Headers.Add("Cookie", $"sessionid_count={sessionId}");

                    //var response = client.PostAsync(url, content).Result;
                    var response = client.SendAsync(httpRequestMessage).Result;


                    var responseString = response.Content.ReadAsStringAsync().Result;
                    var jsonResponse = JsonConvert.DeserializeObject<dynamic>(responseString);

                    if (jsonResponse.status == 0)
                    {
                        var dataObj = jsonResponse.data;
                        if (dataObj != null)
                        {
                            return dataObj.ToObject<JuLiangUserInfo>();
                        }
                        else
                        {
                            return null;
                        }
                    }
                    else
                    {
                        FileUtils.LogError($"请求参数异常: {jsonResponse.ToString()}", "巨量算数搜索达人请求参数异常");
                        return null;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"请求异常: {ex.Message}", "巨量算数搜索达人请求异常");
                    return null;
                }
            }
        }

        /// <summary>
        /// 用户信息接口调用
        /// </summary>
        /// <param name="secUserId"></param>
        /// <returns></returns>
        public DouyinUser UserProfile(string secUserId)
        {
            if (string.IsNullOrEmpty(secUserId))
            {
                FileUtils.log("sec_user_id不能为空");
                return null;
            }

            string macAddress = GenerateMac();
            string cdid = GenerateCdid();
            string uuid = GenerateImei();
            string openudid = GenerateOpenUdid();

            // 毫秒级时间戳（rticket）
            long rticket = (long)DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            // 秒级时间戳（ts）
            long ts = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            string deviceId = "4484249592557674";
            string iid = "810786544184746";

            using (HttpClient client = new HttpClient())
            {
                // 设置请求头
                client.DefaultRequestHeaders.UserAgent.ParseAdd("okhttp/3.10.0.1");
                // client.DefaultRequestHeaders.AcceptEncoding.Add(new StringWithQualityHeaderValue("gzip"));
                client.DefaultRequestHeaders.Add("x-ss-req-ticket", rticket.ToString());
                client.DefaultRequestHeaders.Add("sdk-version", "1");
                client.DefaultRequestHeaders.Add("x-khronos", ts.ToString());

                // 构建查询参数
                Dictionary<string, string> parameters = new Dictionary<string, string>
                {
                    { "sec_user_id", secUserId },
                    { "address_book_access", "2" },
                    { "from", "0" },
                    { "publish_video_strategy_type", "2" },
                    { "manifest_version_code", "110501" },
                    { "_rticket", rticket.ToString() },
                    { "app_type", "normal" },
                    { "iid", iid },
                    { "channel", "gdt_growth14_big_yybwz" },
                    { "device_type", "V2307A" },
                    { "language", "zh" },
                    { "cpu_support64", "true" },
                    { "host_abi", "armeabi-v7a" },
                    { "uuid", uuid },
                    { "resolution", "900*1600" },
                    { "openudid", openudid },
                    { "update_version_code", "11509900" },
                    { "cdid", cdid },
                    { "os_api", "28" },
                    { "mac_address", macAddress },
                    { "dpi", "240" },
                    { "ac", "wifi" },
                    { "device_id", deviceId },
                    { "mcc_mnc", "46000" },
                    { "os_version", "9" },
                    { "version_code", "110500" },
                    { "app_name", "aweme" },
                    { "version_name", "11.5.0" },
                    { "device_brand", "vivo" },
                    { "ssmix", "a" },
                    { "device_platform", "android" },
                    { "aid", "1128" },
                    { "ts", ts.ToString() }
                };

                // 构建请求URL
                string queryString = string.Join("&", parameters.Select(p => $"{Uri.EscapeDataString(p.Key)}={Uri.EscapeDataString(p.Value)}"));
                string url = $"https://aweme.snssdk.com/aweme/v1/user/profile/other/?{queryString}";

                try
                {
                    HttpResponseMessage response = client.GetAsync(url).Result;

                    if (response.IsSuccessStatusCode)
                    {

                        string responseBody = response.Content.ReadAsStringAsync().Result;

                        return ParseResponseJson(responseBody);
                    }
                    else
                    {
                        // 记录非成功状态码的日志 
                        FileUtils.LogError($"请求错误: GetHashCode =  {response.GetHashCode()}, Content = {response.Content}", "请求抖音用户信息接口请求错误");
                    }
                }
                catch (HttpRequestException ex)
                {
                    FileUtils.LogError($"请求错误: {ex.Message}", "请求抖音用户信息接口请求错误");
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"发生错误: {ex.Message}", "请求抖音用户信息接口发生错误");
                }
                return null;
            }
        }

        /// <summary>
        /// 获取抖音主页的视频列表
        /// </summary>
        /// <param name="secUid"></param>
        /// <param name="maxCount"></param>
        /// <param name="maxInterval"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public List<DouYinVideoInfo> GetUserAwemeVideoList(string secUid, int? maxCount, long? maxInterval)
        {
            DateTime time = DateTime.Now;
            // 获取当前时间戳
            long now = ServerTimeUtils.getServerCurrentTime();
            List<DouYinVideoInfo> result = new List<DouYinVideoInfo>();
            int currentPage = 1;
            long maxCursor = 0;
            string browserName = "edge"; // 使用哪个浏览器环境
            bool whileExit = true;
            FileUtils.log($"secUid = {secUid}，currentPage = {currentPage}， maxInterval = {maxInterval}", "同步达人视频-接口调用开始");
            while (whileExit)
            {
                if (currentPage >= 200)
                {
                    break;
                }
                var obj = GetAwemeListAsync(secUid, currentPage, maxCursor, browserName);
                int statusCode = obj?.status_code ?? -1;
                FileUtils.log($"调用抖音接口时间： {(DateTime.Now - time).TotalMilliseconds} ms");

                if (statusCode == 0)
                {
                    var data = obj?.aweme_list ?? null;
                    if (data != null && data.Type == JTokenType.Array)
                    {
                        foreach (var item in data)
                        {
                            var temp = item;
                            if (temp != null)
                            {
                                if (!string.IsNullOrEmpty(temp?.video?.play_addr?.file_hash?.ToString() ?? null))
                                {
                                    DouYinVideoInfo tempObj = temp.ToObject<DouYinVideoInfo>();
                                    tempObj.desc = ShortVideoUtils.formatDesc(tempObj.desc);
                                    if ((tempObj?.video?.duration ?? 0) > 0)
                                    {
                                        tempObj.video.duration = ShortVideoUtils.formatDuration(tempObj?.video?.duration ?? 0);
                                    }
                                    result.Add(tempObj);
                                }
                            }

                            // 判断是否已经够
                            long currentTime = temp?.create_time ?? 0;
                            long tempa = ((now / 1000) - currentTime);
                            // 置顶的不过滤
                            string isTop = temp?.is_top?.ToString() ?? "0";
                            if (isTop != "1" && maxInterval != null && tempa > maxInterval)
                            {
                                whileExit = false;
                                break;
                            }

                            if (maxCount != null && result.Count >= maxCount)
                            {
                                whileExit = false;
                                break;
                            }
                        }
                    }
                    maxCursor = long.Parse(obj?.max_cursor?.ToString() ?? "0");

                    bool hasMore = obj?.has_more?.ToString() == "1";
                    if (!hasMore)
                    {
                        FileUtils.log($"secUid  = {secUid},没有下一页,全部作品也没有{maxCount}条");
                        whileExit = false;
                        break;
                    }
                    currentPage++;
                }
                else
                {
                    FileUtils.LogError($"text = {obj.ToString()}", "同步达人视频-采集主页视频失败");
                    throw new CustomException("采集主页视频失败");
                }

                FileUtils.log($"处理抖音主页的视频逻辑时间： {(DateTime.Now - time).TotalMilliseconds} ms \n");
            }
            FileUtils.log($"同步达人视频搜索完成 secUid = {secUid}，currentPage = {currentPage}，条数：{result?.Count ?? 0}", "同步达人视频-搜索完成");
            return result;
        }

        #endregion

        #region 私有方法
        /// <summary>
        /// 随机MAC地址生成
        /// </summary>
        /// <returns></returns>
        private string GenerateMac()
        {
            Random random = new Random();
            byte[] macBytes = new byte[6];
            random.NextBytes(macBytes);
            return string.Join(":", macBytes.Select(b => b.ToString("X2")));
        }

        /// <summary>
        /// 生成CDID (UUID)
        /// </summary>
        /// <returns></returns>
        private string GenerateCdid()
        {
            return Guid.NewGuid().ToString();
        }

        /// <summary>
        /// Luhn算法计算校验位
        /// </summary>
        /// <param name="number"></param>
        /// <returns></returns>
        /// <exception cref="ArgumentException"></exception>
        private int LuhnCheckDigit(string number)
        {
            if (string.IsNullOrEmpty(number))
                throw new ArgumentException("输入的数字不能为空", nameof(number));

            List<int> digits = number.Select(c => int.Parse(c.ToString())).ToList();
            List<int> oddDigits = new List<int>();
            List<int> evenDigits = new List<int>();

            // 分离奇位和偶位数字
            for (int i = digits.Count - 1; i >= 0; i--)
            {
                if ((digits.Count - i) % 2 == 1)
                    oddDigits.Add(digits[i]);
                else
                    evenDigits.Add(digits[i]);
            }

            int checksum = oddDigits.Sum();

            foreach (int d in evenDigits)
            {
                checksum += (d * 2).ToString().Select(c => int.Parse(c.ToString())).Sum();
            }

            return (10 - (checksum % 10)) % 10;
        }

        /// <summary>
        /// 生成随机IMEI
        /// </summary>
        /// <returns></returns>
        private string GenerateImei()
        {
            Random random = new Random();
            // 生成14位随机数字
            long first14Digits = (long)(random.NextDouble() * 9e13 + 1e13);
            string first14Str = first14Digits.ToString("D14");
            // 计算校验位
            int checkDigit = LuhnCheckDigit(first14Str);
            return first14Str + checkDigit;
        }

        /// <summary>
        /// 生成OpenUDID
        /// </summary>
        /// <returns></returns>
        private string GenerateOpenUdid()
        {
            Random random = new Random();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++)
            {
                sb.Append(random.Next(1, 256).ToString("x2"));
            }
            return sb.ToString();
        }

        /// <summary>
        /// 解析响应JSON
        /// </summary>
        /// <param name="json">响应的JSON字符串</param>
        private DouyinUser ParseResponseJson(string json)
        {
            if (string.IsNullOrEmpty(json))
            {
                return null;
            }

            try
            {
                dynamic jsonResponse = JsonConvert.DeserializeObject<dynamic>(json);

                if (jsonResponse?.status_code == 0)
                {
                    if (jsonResponse?.user != null)
                    {
                        DouyinUser result = jsonResponse.user.ToObject<DouyinUser>();
                        result.verificationStatus = 0;

                        // 处理认证的信息
                        string account_cert_info = jsonResponse?.user?.account_cert_info ?? "";
                        try
                        {
                            dynamic temp = JsonConvert.DeserializeObject(account_cert_info);
                            result.verificationInfo = temp.label_text;
                            if (temp.label_style == 3)
                            {
                                result.verificationStatus = 1;
                            }
                            else if (temp.label_style == 5)
                            {
                                result.verificationStatus = 2;
                            }

                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"json = {account_cert_info}, ex = {ex.Message}", "解析json失败");
                        }

                        return result;
                    }
                }
                else
                {
                    FileUtils.LogError($"获取数据失败: {json}", "获取数据失败");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"解析JSON错误: {ex.Message}", "解析JSON错误");
            }
            return null;
        }

        bool bEdge = false;
        /// <summary>
        /// 抖音主页获取作品
        /// </summary>
        /// <param name="secUid"></param>
        /// <param name="currentPage"></param>
        /// <param name="maxCursor"></param>
        /// <param name="browserName"></param>
        /// <param name="count"></param>
        /// <returns></returns>
        /// <exception cref="ArgumentException"></exception>
        private dynamic GetAwemeListAsync(string secUid, int currentPage, long maxCursor, string browserName, int count = 18, int forNum= 0)
        {
            if (forNum > 3)
            {
                throw new CustomException("3次采集失败，主页视频失败");
            }
            string url = currentPage == 1
                ? "https://www.douyin.com/aweme/v1/web/aweme/post/"
                : "https://www-hj.douyin.com/aweme/v1/web/aweme/post/";

            if (!BrowserEnvironments.getBrowserEnvironments().TryGetValue(browserName, out var browserEnv))
            {
                throw new ArgumentException($"Unsupported browser environment: {browserName}");
            }

            // 克隆headers和params以避免修改原始数据
            var headers = new Dictionary<string, string>(browserEnv.Headers);
            var _params = new Dictionary<string, string>(browserEnv.Params);

            // 更新referer
            headers["referer"] = $"https://www.douyin.com/user/{secUid}";

            // 合并基础参数
            var paramsDict = new Dictionary<string, string>
            {
                ["device_platform"] = "webapp",
                ["aid"] = "6383",
                ["channel"] = "channel_pc_web",
                ["sec_user_id"] = secUid,
                ["max_cursor"] = maxCursor.ToString(),
                ["locate_query"] = "false",
                ["show_live_replay_strategy"] = "1",
                ["need_time_list"] = currentPage == 1 ? "1" : "0",
                ["time_list_query"] = "0",
                ["whale_cut_token"] = "",
                ["cut_version"] = "1",
                ["count"] = $"{count}",  // 默认18,可调到41
                ["publish_video_strategy_type"] = "2",
                ["from_user_page"] = "1",
                ["version_code"] = "290100",
                ["version_name"] = "29.1.0",
            };

            // 合并浏览器特定参数
            foreach (var param in _params)
            {
                paramsDict[param.Key] = param.Value;
            }

            // 使用HttpClientHandler处理Cookie
            // var handler = new HttpClientHandler
            // {
            //     UseCookies = true,
            //     CookieContainer = new CookieContainer()
            // };


            // 使用HttpClientHandler处理Cookie和代理
            var handler = new HttpClientHandler
            {
                UseCookies = true,
                CookieContainer = new CookieContainer(),
                // 添加代理设置
                // Proxy = new WebProxy("http://127.0.0.1:7897"), // 替换为实际代理地址
                // UseProxy = true
            };
            // 如果代理需要认证，添加以下代码
            // var proxyCredentials = new NetworkCredential("用户名", "密码");
            // handler.Proxy.Credentials = proxyCredentials;
            // handler.Credentials = proxyCredentials;

            // 添加抖音所需的Cookie
            // 设置Cookie
            string sessionId = DouYinSession.GetValidSessionId();
            handler.CookieContainer.Add(new Uri("https://www.douyin.com"), new Cookie("sessionid", sessionId));
            handler.CookieContainer.Add(new Uri("https://www-hj.douyin.com"), new Cookie("sessionid", sessionId));


            using (var client = new HttpClient(handler))
            {
                // 设置请求头
                foreach (var header in headers)
                {
                    if (!client.DefaultRequestHeaders.Contains(header.Key))
                    {
                        client.DefaultRequestHeaders.Add(header.Key, header.Value);
                    }
                }

                // 添加抖音API特定的请求头
                // client.DefaultRequestHeaders.Add("accept-encoding", "gzip, deflate, br");
                // client.DefaultRequestHeaders.Add("connection", "keep-alive");

                // 构建查询字符串
                var queryString = string.Join("&", paramsDict.Select(kvp =>
                    $"{kvp.Key}={WebUtility.UrlEncode(kvp.Value)}"));
                var requestUri = $"{url}?{queryString}";

                try
                {
                    var response = client.GetAsync(requestUri).Result;
                    var responseText = response.Content.ReadAsStringAsync().Result;

                    if (responseText.Length < 10)
                    {
                        int tempNum = forNum % 3;
                        List<string> tempList = new List<string>{"edge", "chrome", "firefox"};
                        FileUtils.LogError($"当前={browserName}， 跟换为={tempList[tempNum]}", "获取达人失败，跟换浏览环境");
                        return GetAwemeListAsync(secUid, currentPage, maxCursor, tempList[tempNum], count, forNum +1);
                    }

                    return JsonConvert.DeserializeObject<dynamic>(responseText);
                }
                catch (HttpRequestException e)
                {
                    FileUtils.LogError($"Request error: {e.Message}, Stack Trace: {e.StackTrace}", "抖音主页获取作品错误");
                    FileUtils.log($"Request error: {e.Message}");
                    FileUtils.log($"Stack Trace: {e.StackTrace}");
                    return null;
                }
            }
        }

        #endregion
    }
}
