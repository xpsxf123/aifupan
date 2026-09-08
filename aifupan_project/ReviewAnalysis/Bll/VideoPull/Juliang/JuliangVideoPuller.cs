using douyin.Utils;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Bll.VideoPull.Models;
using ReviewAnalysis.Utils;
using ReviewAnalysis.juliangApi;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using ReviewAnalysis.Bll.VideoPull;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll.VideoPull.Juliang
{
    /// <summary>
    /// 巨量百应 - 历史直播列表拉取
    ///
    /// API: GET https://buyin.jinritemai.com/compass_api/content_live/author/live_detail/history_live
    ///
    /// 认证: Cookie-based, 从本地 dataCollect\config\jlby-{MD5} 读取
    /// 翻页: 接口每页最多 10 条, 超过自动翻页
    /// 条数: 由 KvHelper.GetIntKvByKey("video_pull_count", 7) 控制
    ///
    /// 单条记录 key 速查（完整中文说明见 IndexSelectedMap）:
    ///   基础字段:
    ///     live_room           - 直播标题
    ///     cover_img_uri       - 封面图URL
    ///     operation.live_id   - 直播ID（19位数字字符串）
    ///     start_time.start_time   - 开播时间 "2026/07/08 16:33"
    ///     start_time.live_duration - 直播时长 "1小时21分5秒"
    ///   常用指标:
    ///     watch_ucnt          - 直播间观看人数
    ///     pcu                 - 最高在线人数
    ///     acu                 - 平均在线人数
    ///     avg_watch_duration  - 人均观看时长
    ///     comment_cnt         - 评论次数
    ///     incr_fans_cnt       - 新增粉丝数
    ///     pay_gmv             - 成交金额（格式 "¥49.9"）
    ///     pay_order_cnt       - 成交订单数
    ///     pay_ucnt            - 成交人数
    ///     fans_rate           - 看播粉丝占比（格式 "100.00%"）
    ///     ...（完整列表见 IndexSelectedMap）
    /// </summary>
    public class JuliangVideoPuller : IVideoPuller
    {
        /// <summary>
        /// 检查巨量授权状态：Cookie 文件存在且非空即为已授权
        /// </summary>
        public bool IsAuthorized(string secUid)
        {
            var cookieStr = BuildCookieString(secUid);
            return cookieStr != "NOT_FOUND" && cookieStr != "EMPTY";
        }

        /// <summary>接口基础地址（域名 buyin, 非 compass）</summary>
        private const string BaseUrl = "https://buyin.jinritemai.com/compass_api/content_live/author/live_detail/history_live";

        /// <summary>接口固定每页条数上限</summary>
        private const int ApiPageSize = 10;

        /// <summary>请求 User-Agent，用于 a_bogus 签名与请求头保持一致</summary>
        private const string UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36";

        /// <summary>
        /// 指标字典（key = 接口返回字段名, value = 中文说明）
        /// 按巨量 data_head 返回顺序排列, 使用时直接用 key 取值
        /// </summary>
        private static readonly Dictionary<string, string> IndexSelectedMap = new Dictionary<string, string>
        {
            // ==== 直播指标 ====
            ["watch_ucnt"]                  = "直播间观看人数",
            ["avg_hour_watch_ucnt"]         = "单小时观看人数",
            ["watch_cnt"]                   = "直播间观看人次",
            ["pcu"]                         = "最高在线人数",
            ["acu"]                         = "平均在线人数",
            ["avg_watch_duration"]          = "人均观看时长",
            ["comment_cnt"]                 = "评论次数",
            ["fans_club_ucnt"]              = "新加直播团人数",
            ["incr_fans_cnt"]               = "新增粉丝数",
            ["decr_fans_cnt"]               = "取关粉丝数",
            ["fans_rate"]                   = "看播粉丝占比",

            // ==== 电商指标 ====
            ["pay_order_fans_rate"]         = "成交粉丝占比",
            ["popular_product_cnt"]         = "带货商品数",
            ["product_show_ucnt"]           = "直播间商品曝光人数",
            ["product_click_ucnt"]          = "直播间商品点击人数",
            ["product_click_rate"]          = "商品点击率(人数)",
            ["product_conversation_rate"]   = "点击成交转化率(人数)",
            ["pay_order_cnt"]               = "直播间成交订单数",
            ["pay_gmv"]                     = "直播间成交金额",
            ["avg_hour_pay_amt"]            = "单小时GMV",
            ["pay_product_cnt"]             = "直播间成交件数",
            ["pay_ucnt"]                    = "直播间成交人数",
            ["refund_gmv"]                  = "直播间退款金额",
            ["refund_cnt"]                  = "直播间退款订单数",
            ["refund_ucnt"]                 = "直播间订单退款人数",
            ["predict_commission"]          = "预估佣金收入",
            ["product_show_cnt"]            = "直播间商品曝光次数",
            ["product_click_cnt"]           = "直播间商品点击次数",
            ["prod_click_cnt_rate"]         = "商品点击率(次数)",
            ["prod_click_to_pay_cnt_rate"]  = "点击成交转化率(次数)",
            ["price_per_pay_combo_in_live"] = "成交件单价",
            ["watch_cnt_to_pay_rate_in_live"]  = "看播成交转化率(次数)",
            ["watch_ucnt_to_pay_rate_in_live"] = "看播成交转化率(人数)",
            ["pre_sell_order_cnt"]          = "预售订单数",
            ["presale_depay_deamt"]         = "预售定金金额",
            ["pre_sell_order_amt"]          = "预售全款金额",
            ["ecom_live_ecf_joinclub_ucnt_td"] = "新加购物团人数",
            ["achv_ship_ord_amt"]           = "发货金额",
            ["achv_ship_prod_cnt"]          = "发货件数",
            ["t7payin8d_predstl_ord_amt"]   = "结算有效成交金额",
            ["t7payin8d_predstl_ord_cnt"]   = "结算有效成交订单量"
        };

        /// <summary>请求参数 index_selected 的值, 由 IndexSelectedMap.Keys 拼接</summary>
        private static readonly string IndexSelected = string.Join(",", IndexSelectedMap.Keys);

        /// <summary>
        /// 拉取主播最近 N 场直播记录
        ///
        /// 返回值判断:
        ///   result.Success == true  → 成功, result.Data 是 JArray（可能为空）
        ///   result.Success == false → 失败, 看 result.ErrorCode 定位:
        ///     COOKIE_NOT_FOUND  主播未授权巨量
        ///     COOKIE_EMPTY      授权文件损坏
        ///     HTTP_ERROR        网络问题
        ///     HTTP_EMPTY        服务端无响应
        ///     API_ERROR         接口报错（Cookie 过期/权限不足等）, 日志中有 st + msg
        ///     PARSE_ERROR       响应格式异常
        /// </summary>
        /// <param name="secUid">主播 sec_uid</param>
        /// <param name="pullCount">拉取条数上限</param>
        /// <param name="pullDay">只保留最近 N 天内的直播记录（后置过滤，不改变 API 请求参数）</param>
        public async Task<VideoPullResult> GetRecentLiveSessionsAsync(string secUid, int pullCount, int pullDay)
        {
            // 1. 读 Cookie 文件
            var cookieStr = BuildCookieString(secUid);
            if (cookieStr == "NOT_FOUND")
                return Fail("COOKIE_NOT_FOUND", "Cookie文件不存在，主播未授权巨量");
            if (cookieStr == "EMPTY")
                return Fail("COOKIE_EMPTY", "Cookie文件存在但内容为空");

            // 2. 时间范围（固定 7 天，确保能拉到足够的历史数据）
            var dateRange = GetHistoryLiveDateRange();

            // 3. 翻页拉取（pullDay + pullCount 均为循环内终止条件）
            var allData = new List<LiveSessionInfo>();
            var cutoffDate = DateTime.Today.AddDays(-pullDay);
            int total = 0;
            int pageNo = 1;
            bool dateExceeded = false;

            while (allData.Count < pullCount && !dateExceeded)
            {
                int pageSize = ApiPageSize;

                var url = BuildRequestUrl(dateRange, pageNo, pageSize);

                string responseText;
                try
                {
                    responseText = await SendGetRequest(url, cookieStr, BuildHeaders());
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"巨量直播列表 HTTP 异常(page={pageNo}): {ex.Message}", "JuliangVideoPull");
                    return Fail("HTTP_ERROR", $"HTTP请求异常: {ex.Message}");
                }

                if (string.IsNullOrEmpty(responseText))
                    return Fail("HTTP_EMPTY", "HTTP响应为空");

                var pageResult = ParsePage(responseText);
                if (pageResult == null)
                    return Fail("API_ERROR", "接口返回错误，查看日志");

                total = pageResult.Total;

                if (pageResult.Data == null || pageResult.Data.Count == 0)
                    break;

                foreach (var session in pageResult.Data)
                {
                    if (IsBeforeCutoff(session, cutoffDate))
                    {
                        dateExceeded = true;
                        break;
                    }
                    allData.Add(session);
                }

                pageNo++;

                if (pageResult.Data.Count < pageSize)
                    break;
            }

            // 固定 pageSize 翻页可能多拉，截断到目标条数
            if (allData.Count > pullCount)
                allData = allData.Take(pullCount).ToList();

            // 按 LiveId 去重（防御 API 跨页重叠等异常）
            allData = allData.GroupBy(s => s.LiveId).Select(g => g.First()).ToList();

            FileUtils.LogRpa($"巨量直播列表完成: total={total}, fetched={allData.Count}", "JuliangVideoPull");
            return new VideoPullResult { Success = true, Data = allData, Total = total };
        }

        /// <summary>
        /// 查询当前在播的直播ID
        /// API: GET https://buyin.jinritemai.com/compass_api/author/live/live_detail/today_live_room
        /// 返回第一个 live_status=true 的 live_id，无在播则返回 null
        /// </summary>
        public async Task<string> GetCurrentLiveRoomAsync(string secUid)
        {
            // 1. 读 Cookie 文件
            var cookieStr = BuildCookieString(secUid);
            if (cookieStr == "NOT_FOUND")
                return null;
            if (cookieStr == "EMPTY")
                return null;

            // 2. 请求今日直播列表（拼装签名参数 + a_bogus）
            var fp = JuliangApiDataApi.GetFp();
            var msToken = JuliangApiDataApi.GetMsToken();
            var parameters = new Dictionary<string, string>
            {
                { "_lid", JuliangApiDataApi.GetLid() },
                { "verifyFp", fp },
                { "fp", fp },
                { "msToken", msToken }
            };
            var aBogus = JuliangApiDataApi.GetAB(UA, parameters);
            parameters.Add("a_bogus", aBogus);
            var queryString = string.Join("&",
                parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            var url = $"https://buyin.jinritemai.com/compass_api/author/live/live_detail/today_live_room?{queryString}";

            string responseText;
            try
            {
                responseText = await SendGetRequest(url, cookieStr, BuildHeaders());
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"巨量今日直播 API 异常: {ex.Message}", "JuliangVideoPull");
                return null;
            }

            if (string.IsNullOrEmpty(responseText))
                return null;

            // 3. 解析响应，查找 live_status=true 的场次
            try
            {
                var json = JObject.Parse(responseText);
                var st = json.Value<int?>("st");
                if (st != 0)
                {
                    FileUtils.LogRpa($"巨量今日直播 API st={st}, msg={json.Value<string>("msg")}", "JuliangVideoPull");
                    return null;
                }

                var cardList = json["data"]?["card_list"] as JArray;
                if (cardList == null || cardList.Count == 0)
                    return null;

                foreach (var card in cardList)
                {
                    var jCard = (JObject)card;
                    var liveStatus = jCard.Value<bool?>("live_status");
                    if (liveStatus == true)
                    {
                        var liveId = jCard.Value<string>("live_id");
                        if (!string.IsNullOrEmpty(liveId))
                        {
                            FileUtils.LogRpa($"巨量今日直播中: live_id={liveId}", "JuliangVideoPull");
                            return liveId;
                        }
                    }
                }

                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"巨量今日直播 JSON 解析异常: {ex.Message}", "JuliangVideoPull");
                return null;
            }
        }

        /// <summary>
        /// 读本地 Cookie 文件, 返回 "key1=value1; key2=value2; ..." 格式
        /// 返回 "NOT_FOUND" / "EMPTY" 表示异常, 调用方据此设置 ErrorCode
        /// </summary>
        private string BuildCookieString(string secUid)
        {
            var cookiePath = juliang.JuliangUtils.getCookiePath(secUid);
            if (!File.Exists(cookiePath))
            {
                FileUtils.LogRpa($"巨量Cookie文件不存在: {cookiePath}", "JuliangVideoPull");
                return "NOT_FOUND";
            }

            var helper = new juliang.CookiePersistenceHelper(cookiePath);
            var cookies = helper.LoadCefCookiesFromLocal(false);
            if (cookies == null || cookies.Count == 0)
            {
                FileUtils.LogRpa($"巨量Cookie文件为空: {cookiePath}", "JuliangVideoPull");
                return "EMPTY";
            }

            return string.Join("; ", cookies.Select(c => $"{c.Name}={c.Value}"));
        }

        /// <summary>
        /// 构造请求 URL（含 query string）
        /// </summary>
        private string BuildRequestUrl(HistoryLiveDateRange dateRange, int pageNo, int pageSize)
        {
            var fp = JuliangApiDataApi.GetFp();
            var msToken = JuliangApiDataApi.GetMsToken();
            var parameters = new Dictionary<string, string>
            {
                { "_lid",               JuliangApiDataApi.GetLid() },
                { "is_asc",             "false" },
                { "page_no",            pageNo.ToString() },
                { "page_size",          pageSize.ToString() },
                { "date_type",          dateRange.DateType },
                { "begin_date",         dateRange.BeginDateTimestamp },
                { "begin_date_format",  dateRange.BeginDateIsoFormat },
                { "index_selected",     "" },
                { "sort_field",         "" },
                { "verifyFp",           fp },
                { "fp",                 fp },
                { "msToken",            msToken }
            };

            var aBogus = JuliangApiDataApi.GetAB(UA, parameters);
            parameters.Add("a_bogus", aBogus);

            var queryString = string.Join("&",
                parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            return $"{BaseUrl}?{queryString}";
        }

        /// <summary>
        /// 计算时间范围, 复用 Python buyin_tool.get_last_7_days_range(7) 逻辑
        ///
        /// Python 逻辑:
        ///   today = date.today()
        ///   end_date_obj = today - 1天        → 昨天 00:00:00
        ///   begin_date_obj = end - 6天        → 7天前 00:00:00
        ///   history_live_begin = begin + 1天  → 6天前 00:00:00 (历史直播查询用)
        ///   date_type = "21" (近7天)
        /// </summary>
        private HistoryLiveDateRange GetHistoryLiveDateRange()
        {
            var today = DateTime.Today;                       // 今天 00:00:00
            var endDate = today.AddDays(-1);                  // 昨天
            var beginDate = endDate.AddDays(-89);              // 7天前
            var historyLiveBeginDate = beginDate.AddDays(1);  // 6天前（巨量历史查询偏移）

            // 转 Unix 时间戳（秒）
            var beginDt = new DateTime(historyLiveBeginDate.Year, historyLiveBeginDate.Month,
                historyLiveBeginDate.Day, 0, 0, 0, DateTimeKind.Local);
            var unixTimestamp = (long)(beginDt.ToUniversalTime() -
                new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;

            return new HistoryLiveDateRange
            {
                BeginDateTimestamp = unixTimestamp.ToString(),
                BeginDateIsoFormat = $"{historyLiveBeginDate:yyyy-MM-dd}T00:00:00+08:00",
                DateType = "24"
            };
        }

        /// <summary>
        /// 浏览器模拟 headers, 与 Python 直播明细.py 保持一致
        /// Chrome 148, Windows, zh-CN
        /// </summary>
        private static Dictionary<string, string> BuildHeaders()
        {
            return new Dictionary<string, string>
            {
                ["accept"]          = "application/json, text/plain, */*",
                ["accept-language"] = "zh-CN,zh;q=0.9,en;q=0.8",
                ["cache-control"]   = "no-cache",
                ["pragma"]          = "no-cache",
                ["priority"]        = "u=1, i",
                ["referer"]         = $"https://buyin.jinritemai.com/dashboard/compass-home/live-list?universal_page_params_id={Guid.NewGuid()}",
                ["sec-ch-ua"]       = "\"Chromium\";v=\"148\", \"Google Chrome\";v=\"148\", \"Not/A)Brand\";v=\"99\"",
                ["sec-ch-ua-mobile"] = "?0",
                ["sec-ch-ua-platform"] = "\"Windows\"",
                ["sec-fetch-dest"]   = "empty",
                ["sec-fetch-mode"]   = "cors",
                ["sec-fetch-site"]   = "same-origin",
                ["user-agent"]       = UA
            };
        }

        /// <summary>生成 _lid 参数: 时间戳前5位 + 随机4位数字</summary>
        private static string GetLid()
        {
            var timestamp = (long)(DateTime.UtcNow -
                new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;
            var randomPart = new Random().Next(1000, 9999);
            return $"{timestamp.ToString().Substring(0, 5)}{randomPart}";
        }

        /// <summary>HTTP GET, 手动注入 Cookie header（UseCookies=false）</summary>
        private async Task<string> SendGetRequest(string url, string cookieStr, Dictionary<string, string> headers)
        {
            using (var handler = new HttpClientHandler
            {
                UseCookies = false,                                              // 手动管 Cookie
                AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
                AllowAutoRedirect = true
            })
            using (var client = new HttpClient(handler))
            {
                var request = new HttpRequestMessage(HttpMethod.Get, url);

                foreach (var header in headers)
                    request.Headers.TryAddWithoutValidation(header.Key, header.Value);

                if (!string.IsNullOrEmpty(cookieStr))
                    request.Headers.TryAddWithoutValidation("Cookie", cookieStr);

                var response = await client.SendAsync(request);
                var content = await response.Content.ReadAsStringAsync();

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.LogRpa($"巨量直播列表 HTTP {(int)response.StatusCode}", "JuliangVideoPull");
                    return null;
                }

                return content;
            }
        }

        /// <summary>
        /// 解析单页响应, 返回 PageResult（JToken → LiveSessionInfo 映射）
        /// 以下情况返回 null:
        ///   - st != 0: 接口业务错误（Cookie 过期/权限不足）
        ///   - JSON 解析异常: 响应格式异常
        /// </summary>
        private PageResult ParsePage(string responseText)
        {
            try
            {
                var json = JObject.Parse(responseText);
                var st = json.Value<int?>("st");
                if (st != 0)
                {
                    FileUtils.LogRpa($"巨量直播列表 API st={st}, msg={json.Value<string>("msg")}", "JuliangVideoPull");
                    return null;
                }

                var page = json["data"]?["page_result"];
                var total = page?.Value<int?>("total") ?? 0;

                var dataResult = json["data"]?["data_result"] as JArray;
                if (dataResult == null || dataResult.Count == 0)
                    return new PageResult { Data = new List<LiveSessionInfo>(), Total = total };

                var list = new List<LiveSessionInfo>();
                foreach (var item in dataResult)
                {
                    var jItem = (JObject)item;
                    var rawStartTime = jItem["start_time"]?["start_time"]?.Value<string>();
                    var rawDuration = jItem["start_time"]?["live_duration"]?.Value<string>();

                    var startTime = FormatStartTime(rawStartTime);
                    var durationSec = ParseLiveDuration(rawDuration);
                    var endTime = ComputeEndTime(startTime, durationSec);

                    var session = new LiveSessionInfo
                    {
                        LiveRoom    = jItem.Value<string>("live_room"),
                        CoverImgUri = jItem.Value<string>("cover_img_uri"),
                        LiveId      = jItem["operation"]?["live_id"]?.Value<string>(),
                        StartTime   = startTime,
                        EndTime     = endTime,
                        DurationSec = durationSec,
                    };

                    list.Add(session);
                }

                return new PageResult { Data = list, Total = total };
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"巨量直播列表 JSON 解析异常: {ex.Message}", "JuliangVideoPull");
                return null;
            }
        }

        /// <summary>格式化开播时间 "2026/07/08 16:33" → "2026-07-08 16:33:00"</summary>
        private static string FormatStartTime(string rawTime)
        {
            if (string.IsNullOrEmpty(rawTime)) return "";
            if (DateTime.TryParseExact(rawTime, "yyyy/MM/dd HH:mm",
                System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out var dt))
                return dt.ToString("yyyy-MM-dd HH:mm:ss");
            return rawTime;
        }

        /// <summary>解析 "1小时21分5秒" → 秒数</summary>
        private static long ParseLiveDuration(string liveDuration)
        {
            if (string.IsNullOrEmpty(liveDuration)) return 0;
            long totalSec = 0;
            var hourMatch = System.Text.RegularExpressions.Regex.Match(liveDuration, @"(\d+)小时");
            if (hourMatch.Success) totalSec += long.Parse(hourMatch.Groups[1].Value) * 3600;
            var minMatch = System.Text.RegularExpressions.Regex.Match(liveDuration, @"(\d+)分");
            if (minMatch.Success) totalSec += long.Parse(minMatch.Groups[1].Value) * 60;
            var secMatch = System.Text.RegularExpressions.Regex.Match(liveDuration, @"(\d+)秒");
            if (secMatch.Success) totalSec += long.Parse(secMatch.Groups[1].Value);
            return totalSec;
        }

        /// <summary>根据开播时间和时长计算结束时间</summary>
        private static string ComputeEndTime(string startTime, long durationSec)
        {
            if (string.IsNullOrEmpty(startTime)) return "";
            if (DateTime.TryParseExact(startTime, "yyyy-MM-dd HH:mm:ss",
                System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out var dt))
                return dt.AddSeconds(durationSec).ToString("yyyy-MM-dd HH:mm:ss");
            return "";
        }

        /// <summary>
        /// 判断直播场次的开始时间是否早于截止日期
        /// 数据按时间倒序排列，一旦命中即可终止翻页
        /// </summary>
        /// <param name="session">直播场次信息</param>
        /// <param name="cutoffDate">截止日期（不含时间部分）</param>
        /// <returns>true=早于截止日期，应跳过并终止翻页</returns>
        private static bool IsBeforeCutoff(LiveSessionInfo session, DateTime cutoffDate)
        {
            if (string.IsNullOrEmpty(session?.StartTime))
                return false;

            if (DateTime.TryParseExact(session.StartTime, "yyyy-MM-dd HH:mm:ss",
                System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out var dt))
                return dt < cutoffDate;

            return false;
        }

        /// <summary>快捷创建失败结果</summary>
        private static VideoPullResult Fail(string code, string msg)
        {
            return new VideoPullResult { Success = false, ErrorCode = code, Error = msg };
        }

        // ==== 内部类型 ====

        /// <summary>单页解析结果</summary>
        private class PageResult
        {
            public List<LiveSessionInfo> Data { get; set; }
            public int Total { get; set; }
        }

        /// <summary>历史直播查询时间范围</summary>
        private class HistoryLiveDateRange
        {
            public string BeginDateTimestamp { get; set; }
            public string BeginDateIsoFormat { get; set; }
            public string DateType { get; set; }
        }
    }
}
