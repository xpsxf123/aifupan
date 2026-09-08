using douyin.Utils;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    /// <summary>
    /// 直播大屏商品数据获取帮助类（完全独立，避免与其他方法耦合）
    /// </summary>
    public class LiveScreenProductHelper
    {
        private readonly string _roomId;
        private readonly string _cookie;

        public LiveScreenProductHelper(string roomId, string cookie)
        {
            _roomId = "7626184166157454114";// roomId;
            _cookie = cookie;
        }

        /// <summary>
        /// 获取直播后商品列表，优先使用 product_list_after_live，无数据时降级使用 product_list
        /// </summary>
        public async Task<List<Dictionary<string, object>>> GetLiveScreenProductListAfterLiveAsync()
        {
            // 优先使用直播后接口
            string afterLiveUrl = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list_after_live";
            var result = await FetchProductListAsync(afterLiveUrl);

            // 无数据时降级使用直播中接口
            if (result == null || result.Count == 0)
            {
                FileUtils.LogRpa("product_list_after_live 未获取到数据，尝试 product_list", "直播后商品列表");
                string liveUrl = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list";
                result = await FetchProductListAsync(liveUrl);
            }

            return result;
        }

        /// <summary>
        /// 通用商品列表请求方法，支持不同 API URL
        /// </summary>
        private async Task<List<Dictionary<string, object>>> FetchProductListAsync(string url)
        {
            var result = new List<Dictionary<string, object>>();

            try
            {
                // 生成_lid参数
                string lid = GenLid();

                // 构建指标选择参数
                string indexSelected = string.Join(",", new List<string>
                {
                    "product_bind_time", "market_price", "explain_cnt", "product_show_ucnt",
                    "product_click_ucnt", "product_show_click_ucnt_ratio", "product_show_pay_ucnt_ratio",
                    "product_click_pay_ucnt_ratio", "gpm", "pay_amt", "avg_max_pay_amt_min",
                    "pay_combo_cnt", "pay_cnt", "create_cnt", "create_pay_ucnt_ratio",
                    "pay_deposit_pre_order_cnt", "presale_depay_deamt", "pay_deposit_pre_order_amt",
                    "refund_cnt", "real_refund_amt", "refund_rate"
                });

                // 先请求第一页获取总页数
                int pageNo = 1;
                int pageSize = 20;
                int totalPages = 1;

                do
                {
                    var queryParams = new Dictionary<string, string>
                    {
                        ["index_selected"] = indexSelected,
                        ["data_range"] = "0",
                        ["room_id"] = _roomId,
                        ["page_no"] = pageNo.ToString(),
                        ["page_size"] = pageSize.ToString(),
                        ["_lid"] = lid
                    };

                    var queryString = string.Join("&", queryParams.Select(p => $"{p.Key}={Uri.EscapeDataString(p.Value)}"));
                    var fullUrl = $"{url}?{queryString}";

                    // 使用完全独立的 HttpClient（CompassTest 验证过的完整配置）
                    var handler = new HttpClientHandler
                    {
                        UseProxy = false,
                        UseCookies = false
                    };
                    
                    using (var httpClient = new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(15) })
                    {
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept", "application/json, text/plain, */*");
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("cache-control", "no-cache");
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("pragma", "no-cache");
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}&live_app_id=1128");
                        httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", _cookie);
                        
                        // 记录详细耗时
                        var totalSw = Stopwatch.StartNew();
                        FileUtils.LogRpa($"[{url.Substring(url.LastIndexOf('/') + 1)}] 开始请求第 {pageNo}/{totalPages} 页商品数据", "直播后商品列表");

                        // 1. 发送请求并获取响应
                        var requestSw = Stopwatch.StartNew();
                        var response = await httpClient.GetAsync(fullUrl);
                        requestSw.Stop();

                        // 2. 读取响应内容
                        var readSw = Stopwatch.StartNew();
                        var responseText = await response.Content.ReadAsStringAsync();
                        readSw.Stop();

                        totalSw.Stop();

                        FileUtils.LogRpa($"第 {pageNo} 页请求完成，总耗时: {totalSw.ElapsedMilliseconds}ms, " +
                            $"发送请求: {requestSw.ElapsedMilliseconds}ms, " +
                            $"读取响应: {readSw.ElapsedMilliseconds}ms, " +
                            $"状态码: {response.StatusCode}", "直播后商品列表");

                        if (string.IsNullOrEmpty(responseText))
                        {
                            FileUtils.LogRpa("获取商品列表响应为空", "直播后商品列表");
                            break;
                        }

                        var jsonData = JObject.Parse(responseText);

                        if (jsonData["data"] != null && jsonData["data"]["data_result"] != null)
                        {
                            JArray dataResult = jsonData["data"]["data_result"] as JArray;

                            // 获取分页信息
                            if (pageNo == 1 && jsonData["data"]["page_result"] != null)
                            {
                                var pageResult = jsonData["data"]["page_result"];
                                int total = 0;
                                if (pageResult["total"] != null)
                                {
                                    total = pageResult["total"].Type == JTokenType.Integer 
                                        ? pageResult["total"].Value<int>() 
                                        : 0;
                                }
                                totalPages = (int)Math.Ceiling((double)total / pageSize);
                                FileUtils.LogRpa($"商品数据：共 {total} 条，{totalPages} 页", "直播后商品列表");
                            }

                            // 处理数据
                            foreach (JObject item in dataResult)
                            {
                                var productData = new Dictionary<string, object>();

                                foreach (var property in item.Properties())
                                {
                                    string key = property.Name;
                                    JToken value = property.Value;

                                    try
                                    {
                                        // 处理带有unit和value的对象格式
                                        if (value is JObject obj && obj["value"] != null)
                                        {
                                            productData[key] = obj["value"].ToObject<object>();
                                        }
                                        else
                                        {
                                            productData[key] = value.ToObject<object>();
                                        }
                                    }
                                    catch (Exception ex)
                                    {
                                        FileUtils.LogRpa($"处理字段 {key} 时出错: {ex.Message}, 值类型: {value?.GetType()?.Name}", "直播后商品列表");
                                        productData[key] = value?.ToString();
                                    }
                                }

                                result.Add(productData);
                            }
                        }
                        else
                        {
                            FileUtils.LogRpa($"获取商品数据失败: {responseText}", "直播后商品列表");
                            break;
                        }

                        pageNo++;

                    } // using 结束

                } while (pageNo <= totalPages);

                FileUtils.LogRpa($"成功获取 {result.Count} 条商品数据", "直播后商品列表");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品数据异常: {ex.Message}", "直播后商品列表");
            }

            return result;
        }

        /// <summary>
        /// 生成lid参数
        /// </summary>
        private string GenLid()
        {
            var timestamp = DateTimeOffset.Now.ToUnixTimeMilliseconds().ToString();
            var random = new Random();
            var randomPart = random.Next(10000).ToString("D4");
            return timestamp.Substring(0, 5) + randomPart;
        }
    }
}
