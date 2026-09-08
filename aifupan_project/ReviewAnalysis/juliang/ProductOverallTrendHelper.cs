using douyin.Utils;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class ProductOverallTrendHelper
    {
        private readonly string _roomId;
        private readonly string _cookie;

        public ProductOverallTrendHelper(string roomId, string cookie)
        {
            _roomId = roomId;
            _cookie = cookie;
        }

        public async Task<ProductOverallTrendResult> GetProductOverallTrendAsync(string productId)
        {
            var result = new ProductOverallTrendResult();

            try
            {
                string lid = GenLid();
                string url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product_overall_trend";

                var fp = SignatureHelper.GetFp();
                var queryParams = new Dictionary<string, string>
                {
                    ["room_id"] = _roomId,
                    ["product_id"] = productId,
                    ["_lid"] = lid,
                    ["verifyFp"] = fp,
                    ["fp"] = fp,
                    ["msToken"] = SignatureHelper.GetMsToken(176)
                };

                var paramsStr = BuildQueryString(queryParams);
                var aBogus = SignatureHelper.GetABogus(GetUserAgent(), paramsStr);
                if (!string.IsNullOrEmpty(aBogus))
                {
                    queryParams["a_bogus"] = aBogus;
                }

                var queryString = BuildQueryString(queryParams);
                var fullUrl = $"{url}?{queryString}";

                var handler = new HttpClientHandler { UseProxy = false, UseCookies = false };

                using (var httpClient = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(15) })
                {
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept", "application/json, text/plain, */*");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("cache-control", "no-cache");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("pragma", "no-cache");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", GetUserAgent());
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}&live_app_id=1128");
                    httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", _cookie);

                    FileUtils.LogRpa($"请求商品趋势URL: {fullUrl}", "商品趋势数据");

                    var response = await httpClient.GetAsync(fullUrl);
                    var responseText = await response.Content.ReadAsStringAsync();

                    if (string.IsNullOrEmpty(responseText))
                    {
                        FileUtils.LogRpa("获取商品趋势数据响应为空", "商品趋势数据");
                        return result;
                    }

                    var jsonData = JObject.Parse(responseText);

                    if (jsonData["data"] == null)
                    {
                        FileUtils.LogRpa($"获取商品趋势数据失败: {responseText}", "商品趋势数据");
                        return result;
                    }

                    var data = jsonData["data"];

                    if (data["explain_list"] != null)
                    {
                        foreach (var explain in data["explain_list"])
                        {
                            var explainItem = new ExplainItem
                            {
                                ExplainStartTs = ConvertTimestamp(explain["explain_start_ts"]),
                                ExplainEndTs = ConvertTimestamp(explain["explain_end_ts"]),
                                ExplainStartTime = explain["explain_start_time"]?.ToString(),
                                ExplainEndTime = explain["explain_end_time"]?.ToString(),
                                ProductId = explain["product_id"]?.ToString()
                            };
                            result.ExplainList.Add(explainItem);
                        }
                    }

                    if (data["left_trend"]?["trends"] != null)
                    {
                        foreach (var trend in data["left_trend"]["trends"])
                        {
                            result.LeftTrends.Add(ParseTrendPoint(trend));
                        }
                    }

                    if (data["right_trend"]?["trends"] != null)
                    {
                        foreach (var trend in data["right_trend"]["trends"])
                        {
                            result.RightTrends.Add(ParseTrendPoint(trend));
                        }
                    }

                    FileUtils.LogRpa($"获取商品趋势数据成功: {result.ExplainList.Count}个讲解段, {result.LeftTrends.Count + result.RightTrends.Count}个趋势点", "商品趋势数据");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品趋势数据异常: {ex.Message}", "商品趋势数据");
            }

            return result;
        }

        private TrendPoint ParseTrendPoint(JToken trend)
        {
            return new TrendPoint
            {
                PointName = trend["point_name"]?.ToString(),
                DisplayName = trend["display_name"]?.ToString(),
                XValue = trend["horizontal"]?.ToString(),
                YValue = trend["vertical"]?.ToObject<double>() ?? 0,
                Time = trend["time"]?.ToString()
            };
        }

        private string ConvertTimestamp(object timestamp)
        {
            if (timestamp == null) return null;

            try
            {
                long ts;
                string tsStr = timestamp.ToString();

                if (tsStr.Length == 13)
                {
                    ts = long.Parse(tsStr);
                }
                else
                {
                    ts = long.Parse(tsStr) * 1000;
                }

                DateTimeOffset dateTimeOffset = DateTimeOffset.FromUnixTimeMilliseconds(ts);
                return dateTimeOffset.ToString("yyyy-MM-dd HH:mm:ss");
            }
            catch
            {
                return null;
            }
        }

        private string GenLid()
        {
            var timestamp = DateTimeOffset.Now.ToUnixTimeMilliseconds().ToString();
            var random = new Random();
            var randomPart = random.Next(10000).ToString("D4");
            return timestamp.Substring(0, 5) + randomPart;
        }

        private string BuildQueryString(Dictionary<string, string> parameters)
        {
            var sb = new StringBuilder();
            foreach (var param in parameters)
            {
                if (sb.Length > 0) sb.Append('&');
                sb.Append(Uri.EscapeDataString(param.Key));
                sb.Append('=');
                sb.Append(Uri.EscapeDataString(param.Value));
            }
            return sb.ToString();
        }

        private string GetUserAgent()
        {
            return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";
        }
    }
    /// <summary>
    /// 趋势
    /// </summary>
    public class ProductOverallTrendResult
    {
        /// <summary>
        /// 讲解时间
        /// </summary>
        public List<ExplainItem> ExplainList { get; set; } = new List<ExplainItem>();
        /// <summary>
        /// 商品用户支付金额趋势
        /// </summary>
        public List<TrendPoint> LeftTrends { get; set; } = new List<TrendPoint>();
        /// <summary>
        /// 右边趋势包括：  直播间进入次数，商品点击次数，在线人数
        /// </summary>
        public List<TrendPoint> RightTrends { get; set; } = new List<TrendPoint>();
    }

    public class ExplainItem
    {
        public string ExplainStartTs { get; set; }
        public string ExplainEndTs { get; set; }
        public string ExplainStartTime { get; set; }
        public string ExplainEndTime { get; set; }
        public string ProductId { get; set; }
    }

    public class TrendPoint
    {
        public string PointName { get; set; }
        public string DisplayName { get; set; }
        public string XValue { get; set; }
        public double YValue { get; set; }
        public string Time { get; set; }
    }
}
