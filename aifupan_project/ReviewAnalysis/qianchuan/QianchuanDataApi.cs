using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Utils;
using douyin.Utils;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.bo.anchor;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.qianchuan
{
    /// <summary>
    /// 千川数据API工具类
    /// </summary>
    public class QianchuanDataApi
    {
        /// <summary>
        /// 指标名称映射
        /// </summary>
        private static readonly Dictionary<string, string> IndexNameMap = new Dictionary<string, string>
        {
            { "board_roi2_overview_conf_next", "全域大屏-核心概览看板" },
            { "total_order_settle_count_realtime_for_roi2_1h", "净成交订单数" },
            { "total_cost_per_pay_order_settle_realtime_for_roi2_1h", "净成交订单成本" },
            { "total_order_real_settle_amount_realtime_for_roi2_1h", "用户实际支付净成交金额" },
            { "total_order_settle_amount_rate_realtime_for_roi2_1h", "净成交金额结算率" },
            { "total_order_settle_count_rate_realtime_for_roi2_1h", "净成交订单结算率" },
            { "total_refund_order_count_for_roi2_1h", "1小时内退款订单数" },
            { "total_refund_order_gmv_for_roi2_1h_all", "1小时内退款金额" },
            { "total_refund_order_gmv_for_roi2_1h_rate", "1小时内退款率" },
            { "total_pay_order_count_realtime_for_roi2", "整体成交订单数" },
            { "total_live_pay_order_gpm_realtime_for_roi2", "GPM" },
            { "live_watch_to_pay_rate_for_roi2", "观看成交转化率" },
            { "total_cost_per_pay_order_realtime_for_roi2", "整体成交订单成本" },
            { "total_pay_order_gmv_realtime_for_roi2", "用户实际支付金额" },
            { "total_pay_order_coupon_amount_realtime_for_roi2", "整体成交智能优惠券金额" },
            { "total_ecom_platform_subsidy_amount_realtime_for_roi2", "电商平台补贴金额" },
            { "total_unfinished_estimate_order_gmv_realtime_for_roi2", "整体未完结预售订单预估金额" },
            { "total_refund_order_amount_for_roi2_90d_all", "直播间退款金额" },
            { "total_refund_order_amount_rate_for_roi2_90d_all", "直播间退款率" },
            { "live_online_user_count", "实时在线人数" },
            { "live_show_count_for_roi2", "直播间整体曝光次数" },
            { "total_show_to_watch_rate_for_roi2", "曝光观看率(次数)" },
            { "live_watch_ucount_for_roi2", "直播间整体观看人数" },
            { "live_duration_avg_ecom_for_roi2", "直播间平均停留时长(整场)" },
            { "live_follow_count_for_roi2", "直播间整体新增粉丝数" },
            { "live_comment_count_for_roi2", "直播间评论次数" },
            { "live_product_show_count_for_roi2", "直播间商品曝光次数" },
            { "live_product_click_count_for_roi2", "直播间商品点击次数" },
            { "live_share_count_for_roi2", "分享次数" },
            { "total_live_share_rate_for_roi2", "分享率" },
            { "live_gift_count_for_roi2", "打赏次数" },
            { "total_live_like_rate_for_roi2", "点赞率" },
            { "stat_cost_for_roi2", "整体消耗" },
            { "total_prepay_and_pay_order_realtime_roi2", "整体支付ROI" },
            { "total_pay_order_gmv_include_coupon_realtime_for_roi2", "整体成交金额" },
            { "total_prepay_and_pay_settle_realtime_roi2_1h", "净成交ROI" },
            { "total_order_settle_amount_realtime_for_roi2_1h", "净成交金额" }
        };

        /// <summary>
        /// 获取千川账户列表
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>账户列表</returns>
        public static async Task<List<Dictionary<string, string>>> GetAccountUserList(string roomId, Dictionary<string, string> cookies, string secUid = null)
        {
            try
            {
                string url = "https://compass.jinritemai.com/ad/api/data/compass/get-account-list";
                string paramsStr = $"roomId={roomId}&gfversion=1.0.1.7967";
                string fullUrl = $"{url}?{paramsStr}";

                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(fullUrl);
                request.Method = "GET";
                request.UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";
                request.Accept = "application/json, text/plain, */*";
                request.Referer = $"https://compass.jinritemai.com/qianchuan/check-login?roomId={roomId}&source=luopan";
                request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
                request.Headers.Add("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"");
                request.Headers.Add("sec-ch-ua-mobile", "?0");
                request.Headers.Add("sec-fetch-site", "same-origin");
                request.Headers.Add("sec-fetch-mode", "cors");
                request.Headers.Add("sec-fetch-dest", "empty");
                request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");
                request.Headers.Add("priority", "u=1, i");
                // 关键 Header：CSRF Token
                request.Headers.Add("x-secsdk-csrf-token", "DOWNGRADE");
                request.Headers.Add("Origin", "https://compass.jinritemai.com");

                // 添加Cookie
                StringBuilder cookieString = new StringBuilder();
                foreach (var cookie in cookies)
                {
                    cookieString.Append($"{cookie.Key}={cookie.Value}; ");
                }
                request.Headers.Add("Cookie", cookieString.ToString().TrimEnd(';', ' '));

                FileUtils.LogRpa($"请求千川账户列表，Cookie数量: {cookies.Count}", "千川数据API");

                using (HttpWebResponse response = (HttpWebResponse)await request.GetResponseAsync())
                using (Stream stream = response.GetResponseStream())
                using (StreamReader reader = new StreamReader(stream))
                {
                    string responseText = await reader.ReadToEndAsync();
                    JObject jsonData = JObject.Parse(responseText);

                    if (jsonData["status_code"].ToString() == "0" && jsonData["message"].ToString() == "success" && jsonData["data"] != null)
                    {
                        JArray userAccountInfos = jsonData["data"]["ecpAdvs"] as JArray;
                        if (userAccountInfos != null)
                        {
                            List<Dictionary<string, string>> accountList = new List<Dictionary<string, string>>();
                            foreach (JObject item in userAccountInfos)
                            {
                                Dictionary<string, string> account = new Dictionary<string, string>
                                {
                                    { "id", item["id"].ToString() },
                                    { "name", item["name"].ToString() }
                                };
                                accountList.Add(account);
                            }
                            return accountList;
                        }
                    }
                    else
                    {
                        FileUtils.LogRpa($"获取千川账户列表失败: {responseText}", "千川数据API");
                    }
                }
            }
            catch (WebException wex) when (wex.Response is HttpWebResponse httpResponse && httpResponse.StatusCode == HttpStatusCode.Forbidden)
            {
                FileUtils.LogRpa($"获取千川账户列表返回403 Forbidden，授权已过期", "千川数据API");
                QianchuanUtils.HandleAuthExpired(secUid);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川账户列表异常: {ex.Message}", "千川数据API");
            }
            return new List<Dictionary<string, string>>();
        }

        /// <summary>
        /// 从抖音主播后台获取当前直播间 ID
        /// </summary>
        /// <param name="cookies">抖音 Cookie 字典</param>
        /// <returns>包含 current_live_room_id 的字典</returns>
        public static async Task<Dictionary<string, string>> GetDouyinAccountDetail(Dictionary<string, string> cookies)
        {
            try
            {
                string url = "https://anchor.douyin.com/anchor_pc_tinker_proxy/lego/native/webcast_api/room/replay/overview_v3?roomID=0";

                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(url);
                request.Method = "GET";
                request.UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36";
                request.Accept = "application/json, text/plain, */*";
                request.Referer = "https://anchor.douyin.com/";
                request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
                request.Headers.Add("sec-ch-ua", "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"");
                request.Headers.Add("sec-ch-ua-mobile", "?0");
                request.Headers.Add("x-appid", "477650");
                request.Headers.Add("x-requested-with", "XMLHttpRequest");
                request.Headers.Add("x-sub-web-id", "1116");
                request.Headers.Add("x-use-bpsc", "1");
                request.Headers.Add("sec-fetch-site", "same-origin");
                request.Headers.Add("sec-fetch-mode", "cors");
                request.Headers.Add("sec-fetch-dest", "empty");
                request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");

                // 添加 Cookie
                StringBuilder cookieString = new StringBuilder();
                foreach (var cookie in cookies)
                {
                    cookieString.Append($"{cookie.Key}={cookie.Value}; ");
                }
                request.Headers.Add("Cookie", cookieString.ToString().TrimEnd(';', ' '));

                using (HttpWebResponse response = (HttpWebResponse)await request.GetResponseAsync())
                using (Stream stream = response.GetResponseStream())
                using (StreamReader reader = new StreamReader(stream))
                {
                    string responseText = await reader.ReadToEndAsync();
                    JObject jsonData = JObject.Parse(responseText);

                    if (jsonData["data"] != null)
                    {
                        var result = new Dictionary<string, string>
                        {
                            ["current_live_room_id"] = jsonData["data"]?["current_live_room_id"]?.ToString() ?? "",
                            ["room_id"] = jsonData["data"]?["room_id"]?.ToString() ?? ""
                        };
                        return result;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取抖音主播账户详情异常: {ex.Message}", "千川数据API");
            }
            return null;
        }

        /// <summary>
        /// 获取全域大屏-核心概览看板数据
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="aavid">千川账户ID</param>
        /// <param name="anchorId">主播ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>看板数据</returns>
        public static async Task<Dictionary<string, string>> GetOverviewBoardData(string roomId, string aavid, string anchorId, Dictionary<string, string> cookies, string secUid = null)
        {
            try
            {
                // 参数校验：aavid/anchorId/roomId 任一为空则不发起请求
                if (string.IsNullOrEmpty(aavid))
                {
                    FileUtils.LogRpa("获取千川大屏数据失败：aavid为空", "千川数据API");
                    return new Dictionary<string, string>();
                }
                if (string.IsNullOrEmpty(anchorId))
                {
                    FileUtils.LogRpa("获取千川大屏数据失败：anchorId为空", "千川数据API");
                    return new Dictionary<string, string>();
                }
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("获取千川大屏数据失败：roomId为空", "千川数据API");
                    return new Dictionary<string, string>();
                }

                string url = "https://qianchuan.jinritemai.com/ad/api/data/v1/common/statQuery";
                string paramsStr = $"reqFrom=commonMetricCard&aavid={aavid}&gfversion=1.0.0.294";
                string fullUrl = $"{url}?{paramsStr}";

                // 构建请求数据
                var requestData = new
                {
                    DataSetKey = "board_roi2_overview_conf_next",
                    Metrics = new List<string>
                    {
                        // 原有10个指标
                        "total_pay_order_count_realtime_for_roi2",                          // 整体成交订单数
                        "total_live_pay_order_gpm_realtime_for_roi2",                       // GPM
                        "live_watch_to_pay_rate_for_roi2",                                  // 观看成交转化率
                        "total_cost_per_pay_order_realtime_for_roi2",                       // 整体成交订单成本
                        "live_online_user_count",                                           // 实时在线人数
                        "total_show_to_watch_rate_for_roi2",                                // 曝光观看率(次数)
                        "live_watch_ucount_for_roi2",                                       // 直播间整体观看人数
                        "stat_cost_for_roi2",                                               // 整体消耗
                        "total_prepay_and_pay_order_realtime_roi2",                         // 整体支付ROI
                        "total_pay_order_gmv_include_coupon_realtime_for_roi2",             // 整体成交金额

                        // 新增 - 净成交相关
                        "total_order_settle_count_realtime_for_roi2_1h",                    // 净成交订单数
                        "total_cost_per_pay_order_settle_realtime_for_roi2_1h",             // 净成交订单成本
                        "total_order_real_settle_amount_realtime_for_roi2_1h",              // 用户实际支付净成交金额
                        "total_order_settle_amount_rate_realtime_for_roi2_1h",              // 净成交金额结算率
                        "total_order_settle_count_rate_realtime_for_roi2_1h",               // 净成交订单结算率
                        "total_prepay_and_pay_settle_realtime_roi2_1h",                     // 净成交ROI
                        "total_order_settle_amount_realtime_for_roi2_1h",                   // 净成交金额

                        // 新增 - 退款相关
                        "total_refund_order_count_for_roi2_1h",                             // 1小时内退款订单数
                        "total_refund_order_gmv_for_roi2_1h_all",                           // 1小时内退款金额
                        "total_refund_order_gmv_for_roi2_1h_rate",                          // 1小时内退款率
                        "total_refund_order_amount_for_roi2_90d_all",                       // 直播间退款金额
                        "total_refund_order_amount_rate_for_roi2_90d_all",                  // 直播间退款率

                        // 新增 - 成交金额相关
                        "total_pay_order_gmv_realtime_for_roi2",                            // 用户实际支付金额
                        "total_pay_order_coupon_amount_realtime_for_roi2",                  // 整体成交智能优惠券金额
                        "total_ecom_platform_subsidy_amount_realtime_for_roi2",             // 电商平台补贴金额
                        "total_unfinished_estimate_order_gmv_realtime_for_roi2",            // 整体未完结预售订单预估金额

                        // 新增 - 直播间互动相关
                        "live_show_count_for_roi2",                                         // 直播间整体曝光次数
                        "live_duration_avg_ecom_for_roi2",                                  // 直播间平均停留时长(整场)
                        "live_follow_count_for_roi2",                                       // 直播间整体新增粉丝数
                        "live_comment_count_for_roi2",                                      // 直播间评论次数
                        "live_product_show_count_for_roi2",                                 // 直播间商品曝光次数
                        "live_product_click_count_for_roi2",                                // 直播间商品点击次数
                        "live_share_count_for_roi2",                                        // 分享次数
                        "total_live_share_rate_for_roi2",                                   // 分享率
                        "live_gift_count_for_roi2",                                         // 打赏次数
                        "total_live_like_rate_for_roi2"                                     // 点赞率
                    },
                    Dimensions = new List<string>(),
                    PageParams = new
                    {
                        Offset = 0,
                        Limit = -1
                    },
                    Filters = new
                    {
                        ConditionRelationshipType = 1,
                        Conditions = new List<object>
                        {
                            new { Field = "advertiser_id", Operator = 7, Values = new List<string> { aavid } },
                            new { Field = "room_id", Operator = 7, Values = new List<string> { roomId } },
                            new { Field = "anchor_id", Operator = 7, Values = new List<string> { anchorId } }
                        }
                    },
                    refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next",
                    Base = new
                    {
                        Extra = new
                        {
                            refer = "ecp,7532804468681916467,7542810029855047726,board_roi2_overview_conf_next"
                        }
                    }
                };

                string dataJson = JsonConvert.SerializeObject(requestData, new JsonSerializerSettings { Formatting = Formatting.None });

                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(fullUrl);
                request.Method = "POST";
                request.UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";
                request.Accept = "application/json, text/plain, */*";
                request.ContentType = "application/json";
                request.Referer = $"https://qianchuan.jinritemai.com/board-next?live_room_id={roomId}&aavid={aavid}&ad_origin=1&fromScene=luopan";
                request.Headers.Add("sec-ch-ua-platform", "Windows");
                request.Headers.Add("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"");
                request.Headers.Add("sec-ch-ua-mobile", "?0");
                request.Headers.Add("x-secsdk-csrf-token", "DOWNGRADE");
                request.Headers.Add("origin", "https://qianchuan.jinritemai.com");
                request.Headers.Add("sec-fetch-site", "same-origin");
                request.Headers.Add("sec-fetch-mode", "cors");
                request.Headers.Add("sec-fetch-dest", "empty");
                request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");
                request.Headers.Add("priority", "u=1, i");

                // 添加Cookie
                StringBuilder cookieString = new StringBuilder();
                foreach (var cookie in cookies)
                {
                    cookieString.Append($"{cookie.Key}={cookie.Value}; ");
                }
                request.Headers.Add("Cookie", cookieString.ToString().TrimEnd(';', ' '));

                // 写入请求数据
                byte[] dataBytes = Encoding.UTF8.GetBytes(dataJson);
                request.ContentLength = dataBytes.Length;
                using (Stream requestStream = await request.GetRequestStreamAsync())
                {
                    await requestStream.WriteAsync(dataBytes, 0, dataBytes.Length);
                }

                using (HttpWebResponse response = (HttpWebResponse)await request.GetResponseAsync())
                using (Stream stream = response.GetResponseStream())
                using (StreamReader reader = new StreamReader(stream))
                {
                    string responseText = await reader.ReadToEndAsync();
                    FileUtils.LogRpa(responseText, "https://qianchuan.jinritemai.com/ad/api/data/v1/common/statQuery");
                    JObject jsonData = JObject.Parse(responseText);

                    if (jsonData["status_code"].ToString() == "0" && jsonData["message"].ToString() == "success" && jsonData["data"] != null)
                    {
                        JObject totals = jsonData["data"]["StatsData"]["Totals"] as JObject;
                        if (totals != null)
                        {
                            Dictionary<string, string> result = new Dictionary<string, string>();
                            foreach (var item in totals)
                            {
                                string key = item.Key;
                                string value = item.Value["ValueStr"].ToString();

                                // 跳过无效数据（"-"表示无数据）
                                if (value == "-")
                                {
                                    continue;
                                }

                                if (IndexNameMap.ContainsKey(key))
                                {
                                    result[IndexNameMap[key]] = value;
                                }
                                else
                                {
                                    result[key] = value;
                                }
                            }
                            return result;
                        }
                    }
                    else
                    {
                        FileUtils.LogRpa($"获取千川大屏数据失败: {responseText}", "千川数据API");
                    }
                }
            }
            catch (WebException wex) when (wex.Response is HttpWebResponse httpResponse && httpResponse.StatusCode == HttpStatusCode.Forbidden)
            {
                FileUtils.LogRpa($"获取千川大屏数据返回403 Forbidden，授权已过期", "千川数据API");
                QianchuanUtils.HandleAuthExpired(secUid);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川大屏数据异常: {ex.Message}", "千川数据API");
            }
            return new Dictionary<string, string>();
        }

        /// <summary>
        /// 获取直播间详情数据（包括开始时间、结束时间、主播信息等）
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="aavid">千川账户ID</param>
        /// <param name="anchorId">主播ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>直播间详情数据</returns>
        public static async Task<Dictionary<string, string>> GetRoomDetailData(string roomId, string aavid, string anchorId, Dictionary<string, string> cookies, string secUid = null)
        {
            try
            {
                // 参数校验
                if (string.IsNullOrEmpty(aavid) || string.IsNullOrEmpty(anchorId) || string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("获取千川直播间详情失败：参数为空", "千川数据API");
                    return new Dictionary<string, string>();
                }

                string url = "https://qianchuan.jinritemai.com/ad/api/data/v1/common/statQuery";
                string paramsStr = $"reqFrom=init&aavid={aavid}&gfversion=1.0.0.323";
                string fullUrl = $"{url}?{paramsStr}";

                // 获取过去7天的时间范围（支持跨天直播查询）
                var timeRange = GetRecentDaysTimeRange(7);

                // 调试日志：输出请求参数
                FileUtils.LogRpa($"获取千川直播间详情 - 参数: roomId={roomId}, aavid={aavid}, anchorId={anchorId}", "千川数据API");
                FileUtils.LogRpa($"获取千川直播间详情 - 时间范围: StartTime={timeRange.StartTime}, EndTime={timeRange.EndTime}", "千川数据API");

                // 构建请求数据
                var requestData = new
                {
                    DataSetKey = "board_room_detail",
                    Metrics = new List<string> { "stat_cost" },
                    Dimensions = new List<string>
                    {
                        "room_icon",
                        "room_name",
                        "room_status",
                        "room_with_anchor_id",
                        "room_with_anchor_name",
                        "room_start_time",
                        "room_end_time",
                        "optimize_goal",
                        "room_with_anchor_show_id",
                        "room_anchor_avatar"
                    },
                    StartTime = timeRange.StartTime,
                    EndTime = timeRange.EndTime,
                    // 动态构建 refer 字段，使用传入的 aavid 和 anchorId
                    refer = $"ecp,{aavid},{anchorId},board_room_detail",
                    Filters = new
                    {
                        ConditionRelationshipType = 1,
                        Conditions = new List<object>
                        {
                            new { Field = "time_filter", Operator = 7, Values = new List<string> { "2" } },
                            new { Field = "room_id", Operator = 7, Values = new List<string> { roomId } },
                            new { Field = "advertiser_id", Operator = 7, Values = new List<string> { aavid } }
                        }
                    },
                    PageParams = new
                    {
                        Offset = 0,
                        Limit = 1
                    },
                    Base = new
                    {
                        Extra = new
                        {
                            // 动态构建 refer 字段，使用传入的 aavid 和 anchorId
                            refer = $"ecp,{aavid},{anchorId},board_room_detail"
                        }
                    }
                };

                string dataJson = JsonConvert.SerializeObject(requestData, new JsonSerializerSettings { Formatting = Formatting.None });

                // 生成 UUID 用于 Referer（与 Python 代码保持一致）
                string cascadeId = Guid.NewGuid().ToString();
                string globalId = Guid.NewGuid().ToString();

                HttpWebRequest request = (HttpWebRequest)WebRequest.Create(fullUrl);
                request.Method = "POST";
                request.UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";
                request.Accept = "application/json, text/plain, */*";
                request.ContentType = "application/json";
                request.Referer = $"https://qianchuan.jinritemai.com/board-next?live_room_id={roomId}&anchorId={anchorId}&aavid={aavid}&fromModule=roi2_live_analysis_card_column&cascade_id={cascadeId}&global_id={globalId}";
                request.Headers.Add("sec-ch-ua-platform", "Windows");
                request.Headers.Add("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\"");
                request.Headers.Add("sec-ch-ua-mobile", "?0");
                // 移除 x-secsdk-csrf-token（Python 代码已注释掉此字段）
                request.Headers.Add("origin", "https://qianchuan.jinritemai.com");
                request.Headers.Add("sec-fetch-site", "same-origin");
                request.Headers.Add("sec-fetch-mode", "cors");
                request.Headers.Add("sec-fetch-dest", "empty");
                request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");
                request.Headers.Add("priority", "u=1, i");

                // 添加Cookie
                StringBuilder cookieString = new StringBuilder();
                foreach (var cookie in cookies)
                {
                    cookieString.Append($"{cookie.Key}={cookie.Value}; ");
                }
                request.Headers.Add("Cookie", cookieString.ToString().TrimEnd(';', ' '));

                // 写入请求数据
                byte[] dataBytes = Encoding.UTF8.GetBytes(dataJson);
                request.ContentLength = dataBytes.Length;
                using (Stream requestStream = await request.GetRequestStreamAsync())
                {
                    await requestStream.WriteAsync(dataBytes, 0, dataBytes.Length);
                }

                using (HttpWebResponse response = (HttpWebResponse)await request.GetResponseAsync())
                using (Stream stream = response.GetResponseStream())
                using (StreamReader reader = new StreamReader(stream))
                {
                    string responseText = await reader.ReadToEndAsync();

                    // 调试日志：输出API响应（前500字符）
                    FileUtils.LogRpa($"获取千川直播间详情 - API响应: {responseText}", url);

                    JObject jsonData = JObject.Parse(responseText);

                    if (jsonData["status_code"].ToString() == "0" && jsonData["message"].ToString() == "success" && jsonData["data"] != null)
                    {
                        JArray rows = jsonData["data"]["StatsData"]["Rows"] as JArray;
                        if (rows != null && rows.Count > 0)
                        {
                            // 调试日志：输出Rows数量
                            FileUtils.LogRpa($"获取千川直播间详情 - Rows数量: {rows.Count}", "千川数据API");

                            JObject dimensions = rows[0]["Dimensions"] as JObject;
                            if (dimensions != null)
                            {
                                // 调试日志：输出Dimensions字段
                                FileUtils.LogRpa($"获取千川直播间详情 - Dimensions字段: {dimensions.ToString()}", "千川数据API");

                                Dictionary<string, string> result = new Dictionary<string, string>();

                                // 提取所需字段
                                string[] fields = { "room_start_time", "room_end_time", "room_status", "room_with_anchor_name", "room_with_anchor_id" };
                                foreach (var field in fields)
                                {
                                    var fieldObj = dimensions[field];
                                    if (fieldObj != null)
                                    {
                                        string value = fieldObj["Value"]?.ToString();
                                        if (!string.IsNullOrEmpty(value) && value != "-")
                                        {
                                            result[field] = value;
                                        }
                                    }
                                }

                                // 调试日志：输出成功信息
                                FileUtils.LogRpa($"获取千川直播间详情 - 成功，提取字段数: {result.Count}", "千川数据API");
                                return result;
                            }
                            else
                            {
                                // 调试日志：Dimensions为null
                                FileUtils.LogRpa($"获取千川直播间详情 - Dimensions为null", "千川数据API");
                            }
                        }
                        else
                        {
                            // 调试日志：Rows为空
                            FileUtils.LogRpa($"获取千川直播间详情 - Rows为空或数量为0", "千川数据API");
                        }
                    }
                    else
                    {
                        // 调试日志：API返回错误
                        FileUtils.LogRpa($"获取千川直播间详情 - API返回错误: status_code={jsonData["status_code"]}, message={jsonData["message"]}", "千川数据API");
                    }
                }
            }
            catch (WebException wex) when (wex.Response is HttpWebResponse httpResponse && httpResponse.StatusCode == HttpStatusCode.Forbidden)
            {
                FileUtils.LogRpa($"获取千川直播间详情返回403 Forbidden，授权已过期", "千川数据API");
                QianchuanUtils.HandleAuthExpired(secUid);
            }
            catch (Exception ex)
            {
                // 调试日志：异常信息
                FileUtils.LogRpa($"获取千川直播间详情异常: {ex.Message}, 堆栈: {ex.StackTrace}", "千川数据API");
            }
            return new Dictionary<string, string>();
        }

        /// <summary>
        /// 获取过去7天的时间范围（字符串格式）
        /// 用于查询跨天直播的直播间详情
        /// </summary>
        private static (string StartTime, string EndTime) GetRecentDaysTimeRange(int days = 7)
        {
            var endTime = DateTime.Today.AddDays(1).AddSeconds(-1);  // 今天 23:59:59
            var startTime = DateTime.Today.AddDays(-days);            // 7天前 00:00:00
            
            string startStr = startTime.ToString("yyyy-MM-dd HH:mm:ss");
            string endStr = endTime.ToString("yyyy-MM-dd HH:mm:ss");
            
            return (startStr, endStr);
        }

        /// <summary>
        /// 从Cookie字符串解析Cookie字典
        /// </summary>
        /// <param name="cookieString">Cookie字符串</param>
        /// <returns>Cookie字典</returns>
        public static Dictionary<string, string> ParseCookieString(string cookieString)
        {
            Dictionary<string, string> cookies = new Dictionary<string, string>();
            if (!string.IsNullOrEmpty(cookieString))
            {
                string[] cookiePairs = cookieString.Split(';');
                foreach (string pair in cookiePairs)
                {
                    string[] parts = pair.Trim().Split('=');
                    if (parts.Length >= 2)
                    {
                        string key = parts[0].Trim();
                        string value = string.Join("=", parts, 1, parts.Length - 1).Trim();
                        cookies[key] = value;
                    }
                }
            }
            return cookies;
        }
    }
}