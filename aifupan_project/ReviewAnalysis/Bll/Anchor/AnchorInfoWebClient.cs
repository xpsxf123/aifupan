using douyin.Utils;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Bll.Anchor.Entity;
using CefSharp.DevTools.IO;
using ReviewAnalysis.Asr;
using System.Security.Policy;
using System.Threading;
using ReviewAnalysis.api;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.vo.proxyIP;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Dto;
using System.Collections;
using AngleSharp.Dom;
using Microsoft.Playwright.Core;
using System.Text.RegularExpressions;
using ReviewAnalysis.ShortVideo.DouYin.dto;

namespace ReviewAnalysis.Bll.Anchor
{
    public class AnchorInfoWebClient
    {
        private static readonly Lazy<AnchorInfoWebClient> _instance = new Lazy<AnchorInfoWebClient>(() => new AnchorInfoWebClient());

        /// <summary>
        /// 请求实例
        /// </summary>
        public static AnchorInfoWebClient Instance => _instance.Value;

        private HttpClient _httpClient;
        private HttpClientHandler _httpClientHandler;
        private readonly object _httpClientLock = new object();
        private readonly object _proxyIpLock = new object();

        // 代理相关属性
        /// <summary>
        /// 当前代理IP信息
        /// </summary>
        private volatile ProxyIpVo _currentProxyIp;

        /// <summary>
        /// 是否正在使用代理
        /// </summary>
        private volatile bool _isUseProxy;

        /// <summary>
        /// 使用代理的过期时间，超过这个时间则切回正常IP
        /// </summary>
        private long _proxyExpireTime = 0;



        /// <summary>
        /// 手机UA列表
        /// </summary>
        private static List<string> mobileUaList = new List<string>
        {
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.4936.1160 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.5813.1294 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.3 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.5431.1706 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.4 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/27.0 Chrome/125.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/135.0.7049.83 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) GSA/363.0.743255906 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.1470.1951 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.4102.1129 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/135.0.7049.53 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_3_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2893.1844 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/135.0.7049.83 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.7100.1187 Mobile Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        };


        /// <summary>
        /// 私有化构造器
        /// </summary>
        private AnchorInfoWebClient()
        {
            InitializeClient(useProxy: false);
        }

        /// <summary>
        /// 根据主播抖音号获取直播信息
        /// </summary>
        /// <param name="anchorNumber"></param>
        /// <returns></returns>
        public async Task<DouYinAnchorInfoEntity> GetLiveInfoByAnchorNumber(string anchorNumber)
        {
            try
            {
                // 获取重定向的直播地址
                string redirectUrl = await GetH5LiveUrlByAnchorNumber(anchorNumber);

                if(string.IsNullOrEmpty(redirectUrl))
                {
                    FileUtils.LogRecrd($"{anchorNumber}", $"根据主播抖音号获取直播信息===获取重定向的直播地址为空");
                    return null;
                }

                // 发送GET请求
                using (HttpResponseMessage response = await _httpClient.GetAsync(redirectUrl))
                {
                    response.EnsureSuccessStatusCode();
                    byte[] bytes = response.Content.ReadAsByteArrayAsync().Result;
                    string resultStr = Encoding.UTF8.GetString(bytes);
                    // 解析成对象
                    DouYinAnchorInfoEntity douYinAnchorInfoEntity = ParseHtml(resultStr);
                    return douYinAnchorInfoEntity;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据主播抖音号获取直播信息发生异常==={anchorNumber}");
            }

            return null;
        }

        /// <summary>
        /// 根据主播抖音号采集主播secUid
        /// </summary>
        /// <param name="anchorNumber">主播抖音号</param>
        /// <returns></returns>
        public async Task<string> GetSecUidByAnchorNumber(string anchorNumber)
        {
            try
            {
                // 获取重定向的地址
                string redirectUrl = await GetH5LiveUrlByAnchorNumber(anchorNumber);
                if (!string.IsNullOrEmpty(redirectUrl))
                {
                    // 解析secUid
                    string[] parts = redirectUrl.Split(new[] { "sec_user_id=" }, StringSplitOptions.None);
                    if (parts.Length > 1)
                    {
                        return parts[1];
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据主播抖音号采集主播secUid发生异常===={anchorNumber}");
            }

            return null;
        }

        /// <summary>
        /// 根据主播抖音号获取H5的直播地址
        /// </summary>
        /// <param name="anchorNumber">主播抖音号</param>
        /// <returns></returns>
        public async Task<string> GetH5LiveUrlByAnchorNumber(string anchorNumber)
        {
            try
            {

                // 构建完整URL
                string fullUrl = "";
                if(new Random().Next(2) == 0)
                {
                    fullUrl = $"https://live.douyin.com/{anchorNumber}";
                }else
                {
                    fullUrl = $"https://www.iesdouyin.com/live/{anchorNumber}";
                }
                

                // 配置请求头
                ConfigureGetSecUidByAnchorNumberHeaders();

                // 发送GET请求
                using (HttpResponseMessage response = await _httpClient.GetAsync(fullUrl))
                {
                    // 获取重定向的地址
                    if(response.StatusCode == HttpStatusCode.Redirect)
                    {
                        string redirectUrl = response.Headers?.Location?.ToString();
                        if (!string.IsNullOrEmpty(redirectUrl))
                        {
                            return redirectUrl;
                        }
                    }
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据主播抖音号获取H5的直播地址发生异常===={anchorNumber}");
            }

            return null;
        }

        /// <summary>
        /// 根据secUid采集主播信息
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public async Task<AnchorBaseInfoDto> GetAnchorBySecUid(string secUid)
        {
            try
            {
                // 生成10位随机数字字符串
                string randomStr = GenerateRandom10DigitString();

                // 构建查询参数
                var queryParams = new Dictionary<string, string>
                {
                    { "aid", "339757" },
                    { "version_name", "1.1.29" },
                    { "version_code", "1.1.29" },
                    { "device_platform", "win32" },
                    { "sec_user_id", secUid },
                    { "source", "together" },
                    { "os_version", "10.0.26100" },
                    { "screen_width", "1920" },
                    { "screen_height", "1080" },
                    { "browser_language", "zh-CN" },
                    { "browser_platform", "Win32" },
                    { "browser_name", "Mozilla" },
                    { "browser_version", "5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) douyinim/1.1.29 Chrome/130.0.6723.58 Electron/33.2.0-rs.13.release.main.0 TTElectron/33.2.0-rs.13.release.main.0 Safari/537.36" },
                    { "browser_online", "true" },
                    { "cookie_enabled", "true" },
                    { "device_id", randomStr },
                    { "did", randomStr },
                    { "iid", "0" },
                    { "channel", "0" }
                };

                // 构建完整URL
                string queryString = string.Join("&",
                    queryParams.Select(kvp => $"{WebUtility.UrlEncode(kvp.Key)}={WebUtility.UrlEncode(kvp.Value)}"));
                string fullUrl = $"https://imdesktop.douyin.com/aweme/v1/web/user/profile/other/?{queryString}";

                // 配置请求头
                ConfigureGetAnchorBySecUidHeaders();

                // 发送GET请求
                using (HttpResponseMessage response = await _httpClient.GetAsync(fullUrl))
                {
                    // 获取响应内容
                    string responseContent = await response.Content.ReadAsStringAsync();

                    AnchorBaseInfoDto getAnchorBySecUidDto = new AnchorBaseInfoDto();
                    getAnchorBySecUidDto.secUid = secUid;

                    // 解析数据
                    JObject jObj = JObject.Parse(responseContent);
                    getAnchorBySecUidDto.anchorName = jObj?["user"]?["nickname"]?.Value<string>();
                    // 主播抖音用户id
                    getAnchorBySecUidDto.anchorUserId = jObj?["user"]?["uid"]?.Value<string>();
                    // 主播抖音号
                    if (!string.IsNullOrEmpty(jObj?["user"]?["unique_id"]?.Value<string>()))
                    {
                        getAnchorBySecUidDto.anchorNumber = jObj?["user"]?["unique_id"]?.Value<string>();
                    }
                    else
                    {
                        getAnchorBySecUidDto.anchorNumber = jObj?["user"]?["short_id"]?.Value<string>();
                    }
                    // 主播头像
                    JArray avatarList = (JArray)(jObj?["user"]?["avatar_thumb"]?["url_list"]);
                    if (avatarList != null && avatarList.Count > 0)
                    {
                        getAnchorBySecUidDto.anchorAvatar = avatarList[0]?.Value<string>();
                    }

                    return getAnchorBySecUidDto;
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"根据secUid采集主播信息发生异常===={secUid}");
            }

            return null;
        }

        /// <summary>
        /// 生成10位随机数字字符串
        /// </summary>
        /// <returns></returns>
        private string GenerateRandom10DigitString()
        {
            Random random = new Random();
            // 第一位：1-9
            string firstDigit = random.Next(1, 10).ToString();
            // 后9位：0-9
            string otherDigits = string.Join("", Enumerable.Range(0, 9).Select(_ => random.Next(0, 10).ToString()));
            return firstDigit + otherDigits;
        }

        /// <summary>
        /// 解析html
        /// </summary>
        /// <param name="htmlText"></param>
        /// <returns></returns>
        private DouYinAnchorInfoEntity ParseHtml(string htmlText)
        {
            JObject jsonData = ExtractAndParseJson(htmlText);
            if(jsonData == null)
            {
                return null;
            }

            JToken dataJObj = jsonData["data"];
            if(dataJObj == null)
            {
                return null; 
            }

            DouYinAnchorInfoEntity douYinAnchorInfoEntity = new DouYinAnchorInfoEntity();
            douYinAnchorInfoEntity.AnchorId = dataJObj["room"]?["ownerUserId"]?.Value<string>();
            douYinAnchorInfoEntity.AnchorName = dataJObj["room"]?["owner"]?["nickname"]?.Value<string>();
            JArray avatarList = (JArray)(dataJObj["room"]?["owner"]?["avatarThumb"]?["urlList"]);
            if(avatarList != null && avatarList.Count > 0)
            {
                douYinAnchorInfoEntity.AnchorThump = avatarList[0].Value<string>();
            }
            douYinAnchorInfoEntity.RoomId = dataJObj["room"]?["idStr"]?.Value<string>();
            douYinAnchorInfoEntity.LiveStatus = dataJObj["room"]?["status"]?.Value<int>() ?? 4;
            douYinAnchorInfoEntity.RoomTitle = dataJObj["room"]?["title"]?.Value<string>();

            JToken streamUrlJToken = dataJObj["room"]?["streamUrl"];
            if(streamUrlJToken != null)
            {
                List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = packageStreamUrl(streamUrlJToken);
                douYinAnchorInfoEntity.StreamInfos = streamInfos;
            }

            return douYinAnchorInfoEntity;
        }

        /// <summary>
        /// 封装直播推流地址
        /// </summary>
        /// <param name="streamUrl">直播推流地址</param>
        /// <returns></returns>
        private List<DouYinAnchorInfoEntity.StreamInfo> packageStreamUrl(JToken streamUrl)
        {
            List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = new List<DouYinAnchorInfoEntity.StreamInfo>();

            //flv源地址  SD1:标清 SD2:高清 HD:超清 FULL_HD:当前直播最高画质,最高蓝光
            JToken flvUrls = streamUrl["flvPullUrl"];
            if (flvUrls != null)
            {

                string ldStr = getVideoUrls(flvUrls, "SD1").ToString(); // 标清
                string sdStr = getVideoUrls(flvUrls, "SD2").ToString(); // 高清
                string hdStr = getVideoUrls(flvUrls, "HD1").ToString(); // 超清
                string fullHdStr = getVideoUrls(flvUrls, "FULL_HD1").ToString(); // 蓝光

                streamInfos.Add(createStreamInfo(1, 0, ldStr));
                streamInfos.Add(createStreamInfo(1, 1, sdStr));
                streamInfos.Add(createStreamInfo(1, 2, hdStr));
                streamInfos.Add(createStreamInfo(1, 3, fullHdStr));

            }

            //m3u8地址
            JToken hlsUrls = streamUrl["hlsPullUrlMap"];
            if (hlsUrls != null)
            {

                string ldStr = getVideoUrls(hlsUrls, "SD1").ToString(); // 标清
                string sdStr = getVideoUrls(hlsUrls, "SD2").ToString(); // 高清
                string hdStr = getVideoUrls(hlsUrls, "HD1").ToString(); // 超清
                string fullHdStr = getVideoUrls(hlsUrls, "FULL_HD1").ToString(); // 蓝光

                streamInfos.Add(createStreamInfo(0, 0, ldStr));
                streamInfos.Add(createStreamInfo(0, 1, sdStr));
                streamInfos.Add(createStreamInfo(0, 2, hdStr));
                streamInfos.Add(createStreamInfo(0, 3, fullHdStr));

            }

            return streamInfos;
        }

        /// <summary>
        /// 获取某个清晰度的直播地址
        /// </summary>
        /// <param name="hlsUrls"></param>
        /// <param name="key"></param>
        /// <returns></returns>
        private string getVideoUrls(JToken hlsUrls, string key)
        {

            // 获取当前清晰度的流地址
            string resultUrl = hlsUrls[key] == null ? "" : hlsUrls[key].ToString();

            if (string.IsNullOrEmpty(resultUrl))
            {
                List<String> list = new List<string>();
                list.Add("FULL_HD1");
                list.Add("HD1");
                list.Add("SD2");
                list.Add("SD1");

                foreach (var item in list)
                {
                    resultUrl = hlsUrls[item] == null ? "" : hlsUrls[item].ToString();
                    if (!string.IsNullOrEmpty(resultUrl))
                    {
                        return resultUrl;
                    }
                }

            }

            return resultUrl;
        }

        /// <summary>
        /// 创建直播流地址对象
        /// </summary>
        /// <param name="LiveSource">拉流直播源类型 0 m3u8 1 flv</param>
        /// <param name="Quality">清晰度  0 标清  1高清 2超清 3蓝光</param>
        /// <param name="StreaUrl">拉流地址</param>
        /// <param name="IsDefault"></param>
        /// <returns></returns>
        private static DouYinAnchorInfoEntity.StreamInfo createStreamInfo(int LiveSource, int Quality, string StreaUrl, int IsDefault = 0)
        {
            DouYinAnchorInfoEntity.StreamInfo result = new DouYinAnchorInfoEntity.StreamInfo();
            result.LiveSource = LiveSource;
            result.Quality = Quality;
            result.StreaUrl = StreaUrl;
            return result;
        }

        /// <summary>
        /// 从html文本提取JSON
        /// </summary>
        /// <param name="text">html文本</param>
        /// <returns></returns>
        private JObject ExtractAndParseJson(string text)
        {
            // 去掉转义
            text = Regex.Unescape(text);

            int startIndex = text.IndexOf("{\"data\":{\"room\":{");

            if (startIndex == -1)
            {
                FileUtils.LogRecrd($"{text}", $"根据主播抖音号获取直播信息失败===没有json数据");
                return null;
            }

            // 从开始位置向后查找完整的 JSON 对象字符串
            int braceCount = 0;
            for (int i = startIndex; i < text.Length; i++)
            {
                char currentChar = text[i];
                // 统计大括号（不在字符串内时）
                if (currentChar == '{')
                {
                    braceCount++;
                }
                else if (currentChar == '}')
                {
                    braceCount--;
                    // 当大括号计数归零时，表示找到了完整的 JSON 对象
                    if (braceCount == 0)
                    {
                        string jsonString = text.Substring(startIndex, i - startIndex + 1);
                        return JObject.Parse(jsonString);
                    }
                }
            }

            FileUtils.LogRecrd($"{text}", $"根据主播抖音号获取直播信息失败===json数据解析错误");
            return null;
        }




        /// <summary>
        /// 初始化/重置 HttpClient
        /// </summary>
        /// <param name="useProxy">是否使用代理</param>
        /// <param name="proxyIp">代理的IP</param>
        /// <param name="proxyPort">代理的端口</param>
        /// <param name="username">代理验证账号</param>
        /// <param name="password">代理验证密码</param>
        private void InitializeClient(bool useProxy, string proxyIp = "", string proxyPort = "", string username = "", string password = "")
        {
            lock (_httpClientLock)
            {
                // 清理旧实例
                _httpClient?.Dispose();
                _httpClientHandler?.Dispose();

                // 创建新的 Handler
                _httpClientHandler = new HttpClientHandler();
                // 禁止重定向
                _httpClientHandler.AllowAutoRedirect = false;

                // 设置代理
                if (useProxy && !string.IsNullOrEmpty(proxyIp) && !string.IsNullOrEmpty(proxyPort))
                {
                    // 设置代理
                    WebProxy webProxy = new WebProxy($"{proxyIp}:{proxyPort}");
                    if (!string.IsNullOrEmpty(username) && !string.IsNullOrEmpty(password))
                    {
                        webProxy.Credentials = new NetworkCredential(username, password);
                    }

                    _httpClientHandler.Proxy = webProxy;
                    _httpClientHandler.UseProxy = true;
                }
                else
                {
                    // 去掉代理
                    _httpClientHandler.Proxy = null;
                    _httpClientHandler.UseProxy = false;
                }

                // 创建新的 HttpClient
                _httpClient = new HttpClient(_httpClientHandler);
            }
        }

        /// <summary>
        /// 配置通过secuid获取主播信息接口的请求头
        /// </summary>
        private void ConfigureGetAnchorBySecUidHeaders()
        {
            _httpClient.DefaultRequestHeaders.Clear();
            _httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) douyinim/1.1.29 Chrome/130.0.6723.58 Electron/33.2.0-rs.13.release.main.0 TTElectron/33.2.0-rs.13.release.main.0 Safari/537.36");
            _httpClient.DefaultRequestHeaders.Add("Accept", "application/json, text/plain, */*");
            _httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-dest", "empty");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-mode", "cors");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-site", "cross-site");
            _httpClient.DefaultRequestHeaders.Add("referer", "https://imdesktop.douyin.com");
            _httpClient.DefaultRequestHeaders.Add("sdk-version", "2");
            _httpClient.DefaultRequestHeaders.Add("sec-ch-ua", "\"Not?A_Brand\";v=\"99\", \"Chromium\";v=\"130\"");
            _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
            _httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");
            _httpClient.DefaultRequestHeaders.Add("priority", "u=1, i");

        }

        /// <summary>
        /// 配置通过抖音号获取主播secUid接口的请求头
        /// </summary>
        private void ConfigureGetSecUidByAnchorNumberHeaders()
        {
            _httpClient.DefaultRequestHeaders.Clear();
            _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(mobileUaList[new Random().Next(mobileUaList.Count)]);
            _httpClient.DefaultRequestHeaders.Accept.ParseAdd("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            _httpClient.DefaultRequestHeaders.Add("pragma", "no-cache");
            _httpClient.DefaultRequestHeaders.Add("cache-control", "no-cache");
            _httpClient.DefaultRequestHeaders.Add("upgrade-insecure-requests", "1");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-site", "none");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-mode", "navigate");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-user", "?1");
            _httpClient.DefaultRequestHeaders.Add("sec-fetch-dest", "document");
            _httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
            _httpClient.DefaultRequestHeaders.Add("priority", "u=0, i");

        }








        /// <summary>
        /// 设置/更新代理
        /// </summary>
        /// <param name="proxyIp">代理的IP</param>
        /// <param name="proxyPort">代理的端口</param>
        /// <param name="username">代理验证账号</param>
        /// <param name="password">代理验证密码</param>
        public void SetProxy(string proxyIp = "", string proxyPort = "", string username = "", string password = "")
        {
            bool useProxy = !string.IsNullOrEmpty(proxyIp) && !string.IsNullOrEmpty(proxyPort);
            InitializeClient(useProxy, proxyIp, proxyPort, username, password);
        }
        /// <summary>
        /// 检查并代理是否已失效，失效了设置为直连
        /// </summary>
        private void CheckProxyExpire()
        {
            lock (_proxyIpLock)
            {

                if (!_isUseProxy)
                {
                    // 未启用代理IP，return
                    return;
                }

                long currentTime = ServerTimeUtils.getCurrentTime();

                // 检查代理周期是否过期
                if (currentTime > _proxyExpireTime)
                {
                    FileUtils.LogRecrd($"当前时间：{currentTime}，过期时间：{_proxyExpireTime}", $"抖音采集流地址代理IP已过期");
                    SetProxy();
                    _isUseProxy = false;
                }
            }
        }
        /// <summary>
        /// 使用代理IP
        /// </summary>
        public void UseProxyIP()
        {
            lock (_proxyIpLock)
            {
                if (_isUseProxy)
                {
                    // 已经在使用代理IP，return
                    return;
                }

                ProxyIpVo proxyIpVo = ProxyApi.GetProxyIpSync(true, 0);
                if (proxyIpVo != null)
                {
                    _isUseProxy = true;
                    _currentProxyIp = proxyIpVo;
                    _proxyExpireTime = proxyIpVo.expireTime;

                    // 设置代理
                    SetProxy(proxyIpVo.ip, proxyIpVo.port, proxyIpVo.proxyUsername, proxyIpVo.proxyPassword);

                    FileUtils.LogRecrd($"IP: {proxyIpVo.ip}:{proxyIpVo.port}，过期时间：{proxyIpVo.expireTime}", $"抖音采集流地址代理IP设置成功");
                }
                else
                {
                    FileUtils.LogRecrd($"没有可用IP，已取消设置代理", $"获取抖音采集流地址代理IP失败");
                    SetProxy();
                    _isUseProxy = false;
                }
            }

        }


    }
}
