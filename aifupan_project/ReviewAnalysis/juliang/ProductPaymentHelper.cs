using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using douyin.Utils;
using System.Linq;
using ReviewAnalysis.vo;

namespace ReviewAnalysis.juliang
{
    /// <summary>
    /// 商品支付金额获取帮助类
    /// </summary>
    public class ProductPaymentHelper
    {
        private readonly HttpClient _httpClient;
        private readonly string _roomId;
        private readonly string _cookie;

        /// <summary>
        /// 商品指标名称映射（英文key -> 中文名称）
        /// </summary>
        private static readonly Dictionary<string, string> ProductIndexMap = new Dictionary<string, string>
        {
            { "product_id", "商品id" },
            { "title", "商品标题" },
            { "image_uri", "商品图片" },
            { "product_bind_time", "直播间上架时间" },
            { "market_price", "到手价" },
            { "explain_cnt", "讲解次数" },
            { "product_show_ucnt", "商品曝光人数" },
            { "product_click_ucnt", "商品点击人数" },
            { "product_show_click_ucnt_ratio", "曝光-点击转化率" },
            { "product_show_pay_ucnt_ratio", "曝光-成交转化率" },
            { "product_click_pay_ucnt_ratio", "点击-成交转化率" },
            { "gpm", "商品千次曝光成交" },
            { "pay_amt", "累计成交金额" },
            { "avg_max_pay_amt_min", "分钟最高成交金额" },
            { "pay_combo_cnt", "累计成交件数" },
            { "pay_cnt", "累计成交订单数" },
            { "create_cnt", "创建订单数" },
            { "create_pay_ucnt_ratio", "订单支付率" },
            { "pay_deposit_pre_order_cnt", "预售订单数" },
            { "presale_depay_deamt", "预售定金金额" },
            { "pay_deposit_pre_order_amt", "预售全款金额" },
            { "refund_cnt", "退款订单数" },
            { "real_refund_amt", "退款金额" },
            { "refund_rate", "退款率" }
        };

        public ProductPaymentHelper(HttpClient httpClient, string roomId, string cookie)
        {
            _httpClient = httpClient;
            _roomId = roomId;
            _cookie = cookie;
        }

        /// <summary>
        /// 获取直播间商品支付金额列表
        /// </summary>
        public async Task<List<ProductInfoVo>> GetProductPaymentAmountAsync()
        {
            var productList = new List<ProductInfoVo>();
            try
            {
                FileUtils.LogRpa($"开始获取商品支付金额数据，房间号: {_roomId}", "商品支付金额");

                var headers = new Dictionary<string, string>
                {
                    ["accept"] = "application/json, text/plain, */*",
                    ["accept-language"] = "zh-CN,zh;q=0.9,en;q=0.8",
                    ["cache-control"] = "no-cache",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["sec-ch-ua"] = "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36",
                    ["referer"] = $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}&live_app_id=1128"
                };

                // 设置请求头
                _httpClient.DefaultRequestHeaders.Clear();
                foreach (var header in headers)
                {
                    if (!_httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value))
                    {
                        FileUtils.LogRpa($"无法添加头信息: {header.Key}", "商品支付金额");
                    }
                }

                // 添加Cookie
                if (!string.IsNullOrEmpty(_cookie))
                {
                    _httpClient.DefaultRequestHeaders.Remove("Cookie");
                    _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", _cookie);
                }

                var lid = GenLid();
                var queryParams = new Dictionary<string, string>
                {
                    ["category_id"] = "0",
                    ["product_filter_type"] = "0",
                    ["explained_filter_type"] = "0",
                    ["index_selected"] = "",
                    ["room_id"] = _roomId,
                    ["page_no"] = "1",
                    ["page_size"] = "50",
                    ["_lid"] = lid
                };

                var url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list";
                var queryString = string.Join("&", queryParams.Select(p => $"{p.Key}={Uri.EscapeDataString(p.Value)}"));
                var fullUrl = $"{url}?{queryString}";

                var response = await _httpClient.GetAsync(fullUrl);
                var responseText = await response.Content.ReadAsStringAsync();

                if (string.IsNullOrEmpty(responseText))
                {
                    FileUtils.LogRpa("获取商品列表响应为空", "商品支付金额");
                    return productList;
                }

                var jsonData = JObject.Parse(responseText);
                if (jsonData["st"]?.Value<int>() == 0 &&
                    jsonData["msg"]?.Value<string>() == "" &&
                    jsonData["data"] != null)
                {
                    var dataResults = jsonData["data"]?["data_result"] as JArray;
                    if (dataResults != null && dataResults.Count > 0)
                    {
                        FileUtils.LogRpa($"获取到 {dataResults.Count} 个商品", "商品支付金额");
                        
                        foreach (var item in dataResults)
                        {
                            var productInfo = ParseProductInfo(item);
                            if (productInfo != null)
                            {
                                productList.Add(productInfo);
                                FileUtils.LogRpa($"商品: {productInfo.Title}, 商品ID: {productInfo.ProductId}, 支付金额: {productInfo.PayAmt}, 库存: {productInfo.StockCnt}, 近5分钟点击: {productInfo.ProductClickCnt5Min}", "商品支付金额");
                                
                                // 如需获取某个商品的趋势数据，传入商品ID调用以下方法
                                // await GetProductOverallTrendAsync(productInfo.ProductId);
                            }
                        }
                    }
                    else
                    {
                        FileUtils.LogRpa("商品列表为空", "商品支付金额");
                    }
                }
                else
                {
                    FileUtils.LogRpa($"获取商品列表失败: {jsonData["msg"]}", "商品支付金额");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品支付金额异常: {ex.Message}", "商品支付金额");
            }
            return productList;
        }

        /// <summary>
        /// 解析商品信息
        /// 数据格式: {"field_name": {"unit": "...", "value": xxx}} 或 {"field_name": xxx}
        /// </summary>
        private ProductInfoVo ParseProductInfo(JToken item)
        {
            try
            {
                var productInfo = new ProductInfoVo
                {
                    FetchTime = DateTime.Now
                };

                // 基础字段（直接值）
                productInfo.ProductId = item["product_id"]?.ToString();
                productInfo.Title = item["title"]?.ToString();
                productInfo.ImageUri = item["image_uri"]?.ToString();
                productInfo.PriceText = item["price_text"]?.ToString();
                productInfo.PromotionId = item["promotion_id"]?.ToString();
                productInfo.ShopId = item["shop_id"]?.Value<long>() ?? 0;
                productInfo.RoomCartNum = item["room_cart_num"]?.Value<int>() ?? 0;
                productInfo.Explaining = item["explaining"]?.Value<bool>() ?? false;

                // 数值字段（对象格式: {"unit": "...", "value": xxx}）
                productInfo.StockCnt = GetIntValue(item, "stock_cnt");
                productInfo.CampaignStockCnt = GetIntValue(item, "campaign_stock_cnt");
                productInfo.PayAmt = GetDecimalValue(item, "pay_amt");
                productInfo.AvgMaxPayAmtMin = GetDecimalValue(item, "avg_max_pay_amt_min");
                productInfo.ProductClickCnt5Min = GetIntValue(item, "product_click_cnt_5min");
                productInfo.ProductShowPayUcntRatio = GetDecimalValue(item, "product_show_pay_ucnt_ratio");
                productInfo.UnpayCnt = GetIntValue(item, "unpay_cnt");
                productInfo.ExplainCnt = GetIntValue(item, "explain_cnt");
                productInfo.MarketPrice = GetDecimalValue(item, "market_price");
                productInfo.ProductBindTime = GetLongValue(item, "product_bind_time");

                return productInfo;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"解析商品信息异常: {ex.Message}", "商品支付金额");
                return null;
            }
        }

        /// <summary>
        /// 获取整数值（支持对象格式和直接值）
        /// </summary>
        private int GetIntValue(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return 0;

            // 对象格式: {"unit": "...", "value": 123}
            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"].Value<int>();
            }
            // 直接值格式: 123
            return field.Value<int>();
        }

        /// <summary>
        /// 获取长整数值（支持对象格式和直接值）
        /// </summary>
        private long GetLongValue(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return 0;

            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"].Value<long>();
            }
            return field.Value<long>();
        }

        /// <summary>
        /// 获取小数值（支持对象格式和直接值）
        /// </summary>
        private decimal GetDecimalValue(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return 0;

            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"].Value<decimal>();
            }
            return field.Value<decimal>();
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

        /// <summary>
        /// 构建查询字符串
        /// </summary>
        private string BuildQueryString(Dictionary<string, string> parameters)
        {
            var sb = new System.Text.StringBuilder();
            foreach (var param in parameters)
            {
                if (sb.Length > 0) sb.Append('&');
                sb.Append(Uri.EscapeDataString(param.Key));
                sb.Append('=');
                sb.Append(Uri.EscapeDataString(param.Value));
            }
            return sb.ToString();
        }

        /// <summary>
        /// 获取商品整体趋势数据（含讲解时间段）
        /// </summary>
        public async Task GetProductOverallTrendAsync(string productId)
        {
            try
            {
                FileUtils.LogRpa($"开始获取商品趋势数据，商品ID: {productId}", "商品趋势");

                var lid = GenLid();
                var fp = SignatureHelper.GetFp();
                var msToken = SignatureHelper.GetMsToken(172);
                var ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";

                var queryParams = new Dictionary<string, string>
                {
                    ["room_id"] = _roomId,
                    ["product_id"] = productId,
                    ["_lid"] = lid,
                    ["verifyFp"] = fp,
                    ["fp"] = fp,
                    ["msToken"] = msToken
                };

                var paramsStr = BuildQueryString(queryParams);
                var aBogus = SignatureHelper.GetABogus(ua, paramsStr);
                if (!string.IsNullOrEmpty(aBogus))
                {
                    queryParams["a_bogus"] = aBogus;
                }

                // 设置请求头
                _httpClient.DefaultRequestHeaders.Clear();
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept", "application/json, text/plain, */*");
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("cache-control", "no-cache");
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("pragma", "no-cache");
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("user-agent", ua);
                _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("referer", $"https://compass.jinritemai.com/screen/live/talent?live_room_id={_roomId}&live_app_id=1128");
                if (!string.IsNullOrEmpty(_cookie))
                {
                    _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", _cookie);
                }

                var url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product_overall_trend";
                var queryString = BuildQueryString(queryParams);
                var fullUrl = $"{url}?{queryString}";

                var response = await _httpClient.GetAsync(fullUrl);
                var responseText = await response.Content.ReadAsStringAsync();

                if (string.IsNullOrEmpty(responseText))
                {
                    FileUtils.LogRpa("获取商品趋势响应为空", "商品趋势");
                    return;
                }

                var jsonData = Newtonsoft.Json.Linq.JObject.Parse(responseText);

                // 解析讲解时间段
                var explainList = jsonData["data"]?["explain_list"] as Newtonsoft.Json.Linq.JArray;
                if (explainList != null && explainList.Count > 0)
                {
                    foreach (var explain in explainList)
                    {
                        var startTime = explain["explain_start_time"]?.ToString();
                        var endTime = explain["explain_end_time"]?.ToString();
                        FileUtils.LogRpa($"商品讲解期间: {startTime} - {endTime}", "商品趋势");
                    }
                }

                // 解析趋势数据
                var trends = jsonData["data"]?["left_trend"]?["trends"] as Newtonsoft.Json.Linq.JArray;
                if (trends != null && trends.Count > 0)
                {
                    FileUtils.LogRpa($"趋势数据共 {trends.Count} 条", "商品趋势");
                    foreach (var trend in trends)
                    {
                        FileUtils.LogRpa(trend.ToString(), "商品趋势");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取商品趋势异常: {ex.Message}", "商品趋势");
            }
        }

        /// <summary>
        /// 获取直播大屏商品列表数据（直播后数据，包含更完整指标）
        /// </summary>
        /// <returns>商品数据列表</returns>
        public async Task<List<Dictionary<string, object>>> GetLiveScreenProductListAfterLiveAsync()
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

                string url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list_after_live";

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

                    // 使用简单 HttpClient (CompassTest 验证过的快速方案)
                    using (var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(15) })
                    {
                        httpClient.DefaultRequestHeaders.Add("Cookie", _cookie);
                        httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                        // 记录详细耗时
                        var totalSw = System.Diagnostics.Stopwatch.StartNew();
                        FileUtils.LogRpa($"开始请求第 {pageNo}/{totalPages} 页商品数据", "直播后商品列表");

                        // 1. 发送请求并获取响应
                        var requestSw = System.Diagnostics.Stopwatch.StartNew();
                        var response = await httpClient.GetAsync(fullUrl);
                        requestSw.Stop();
                        
                        // 2. 读取响应内容
                        var readSw = System.Diagnostics.Stopwatch.StartNew();
                        var responseText = await response.Content.ReadAsStringAsync();
                        readSw.Stop();
                        
                        totalSw.Stop();
                        
                        FileUtils.LogRpa($"第 {pageNo} 页请求完成，总耗时: {totalSw.ElapsedMilliseconds}ms, " +
                            $"发送请求: {requestSw.ElapsedMilliseconds}ms, " +
                            $"读取响应: {readSw.ElapsedMilliseconds}ms, " +
                            $"状态码: {response.StatusCode}", "直播后商品列表");

                        if (string.IsNullOrEmpty(responseText))
                        {
                            FileUtils.LogRpa("获取直播后商品列表响应为空", "直播后商品列表");
                            break;
                        }

                        var jsonData = JObject.Parse(responseText);

                        if (jsonData["data"] != null && jsonData["data"]["data_result"] != null)
                        {
                            JArray dataResult = jsonData["data"]["data_result"] as JArray;

                            // 获取分页信息
                            if (pageNo == 1 && jsonData["data"]["page_result"] != null)
                            {
                                int total = jsonData["data"]["page_result"]["total"]?.Value<int>() ?? 0;
                                totalPages = (int)Math.Ceiling((double)total / pageSize);
                                FileUtils.LogRpa($"直播后商品数据：共 {total} 条，{totalPages} 页", "直播后商品列表");
                            }

                            // 处理数据
                            foreach (JObject item in dataResult)
                            {
                                var productData = new Dictionary<string, object>();

                                foreach (var property in item.Properties())
                                {
                                    string key = property.Name;
                                    JToken value = property.Value;

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

                                result.Add(productData);
                            }
                        }
                        else
                        {
                            FileUtils.LogRpa($"获取直播后商品数据失败: {responseText}", "直播后商品列表");
                            break;
                        }

                        pageNo++;

                    } // using 结束

                } while (pageNo <= totalPages);

                FileUtils.LogRpa($"成功获取 {result.Count} 条直播后商品数据", "直播后商品列表");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取直播后商品数据异常: {ex.Message}", "直播后商品列表");
            }

            return result;
        }

        /// <summary>
        /// 将商品数据转换为中文键名格式
        /// </summary>
        /// <param name="productData">原始商品数据</param>
        /// <returns>中文键名的商品数据</returns>
        public static Dictionary<string, object> ConvertToChineseKeys(Dictionary<string, object> productData)
        {
            var result = new Dictionary<string, object>();

            foreach (var item in productData)
            {
                if (ProductIndexMap.ContainsKey(item.Key))
                {
                    result[ProductIndexMap[item.Key]] = item.Value;
                }
                else
                {
                    result[item.Key] = item.Value;
                }
            }

            return result;
        }
    }
}