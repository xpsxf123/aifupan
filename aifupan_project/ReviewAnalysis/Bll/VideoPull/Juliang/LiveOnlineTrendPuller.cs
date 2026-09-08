using douyin.Utils;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Utils;
using ReviewAnalysis.juliangApi;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll.VideoPull.Juliang
{
    /// <summary>
    /// 巨量罗盘 blend_trend_v2 API 返回的单条趋势数据点。
    /// 代表某个时间点的全部指标快照，各字段与 <see cref="ReviewAnalysis.juliang.entity.JuliangRealTimeDataEntity"/> 对应。
    /// </summary>
    public class CompassTrendPoint
    {
        /// <summary>数据点时间，格式 yyyy-MM-dd HH:mm:ss（东八区）</summary>
        public string Time { get; set; }

        /// <summary>在线人数（online_user_cnt）</summary>
        public double Value { get; set; }

        /// <summary>进入人数（watch_ucnt），用于累计观看人数</summary>
        public double WatchUcnt { get; set; }

        /// <summary>成交订单数（pay_cnt）</summary>
        public double PayCnt { get; set; }

        /// <summary>成交金额（pay_amt）</summary>
        public double PayAmt { get; set; }

        /// <summary>新加团人数（fans_club_ucnt）</summary>
        public double FansClubUcnt { get; set; }

        /// <summary>新增粉丝数（incr_fans_cnt）</summary>
        public double IncrFansCnt { get; set; }

        /// <summary>千川消耗（stat_cost）</summary>
        public double StatCost { get; set; }
    }

    /// <summary>
    /// 巨量罗盘 - 在线人数曲线拉取器。
    /// 调用 blend_trend_v2 API 获取直播间趋势曲线数据（在线人数、成交、粉丝、消耗等），
    /// 复用 <see cref="JuliangVideoDownloader"/> 的 Cookie 和 Headers 构建逻辑。
    /// </summary>
    /// <remarks>
    /// blend_trend_v2 全部可用 index_selected 指标（共 20 个）：
    ///   流量指标: viewing_rate=曝光-观看率, online_user_cnt=在线人数, watch_ucnt=进入人数,
    ///             leave_ucnt=离开人数, stat_cost=千川消耗
    ///   互动指标: per_capita_viewing_time=人均观看时长, interaction_rate=互动率,
    ///             attention_rate=关注率, negative_feedback_rate=负反馈率,
    ///             negative_feedback_cnt=负反馈次数, comment_cnt=新增评论数,
    ///             incr_fans_cnt=新增粉丝数, fans_club_ucnt=新加团人数
    ///   交易指标: gpm=千次观看成交金额, viewing_click_rate=商品点击率,
    ///             click_transaction_rate=商品点击-成交率, uv_value=UV价值,
    ///             pay_ucnt=成交人数, pay_cnt=成交订单数, pay_amt=成交金额
    /// </remarks>
    public class LiveOnlineTrendPuller
    {
        /// <summary>blend_trend_v2 API 地址，query 参数: room_id, index_selected=online_user_cnt, date_type=100</summary>
        private const string BlendTrendApiUrl =
            "https://compass.jinritemai.com/compass_api/author/live/live_screen/blend_trend_v2";

        /// <summary>
        /// 拉取直播间全部实时指标趋势数据。
        /// 一次请求拉取 7 个关键指标（online_user_cnt / watch_ucnt / pay_cnt / pay_amt / fans_club_ucnt / incr_fans_cnt / stat_cost），
        /// 按 horizontal 时间戳对齐合并为 <see cref="CompassTrendPoint"/> 列表，按时间升序排列。
        /// </summary>
        /// <param name="roomId">直播间 room_id（对应 batchNumber）</param>
        /// <param name="secUid">主播 sec_uid，用于从本地 Cookie 文件加载认证凭据</param>
        /// <returns>趋势数据点列表；Cookie 缺失 / API 异常 / st ≠ 0 时返回 null</returns>
        public static async Task<List<CompassTrendPoint>> FetchAsync(string roomId, string secUid)
        {
            try
            {
                string cookieStr = JuliangVideoDownloader.BuildCookieString(secUid);
                if (string.IsNullOrEmpty(cookieStr))
                    return null;

                var headers = JuliangVideoDownloader.BuildCompassHeaders(roomId);
                string indexSelected = "online_user_cnt,watch_ucnt,pay_cnt,pay_amt,fans_club_ucnt,incr_fans_cnt,stat_cost";

                var fp = JuliangApiDataApi.GetFp();
                var msToken = JuliangApiDataApi.GetMsToken();
                var parameters = new Dictionary<string, string>
                {
                    { "room_id", roomId },
                    { "index_selected", indexSelected },
                    { "date_type", "100" },
                    { "vertical_order", "warn,record,violation,bless,product" },
                    { "_lid", JuliangApiDataApi.GetLid() },
                    { "verifyFp", fp },
                    { "fp", fp },
                    { "msToken", msToken }
                };

                var aBogus = JuliangApiDataApi.GetAB(JuliangVideoDownloader.UA, parameters);
                parameters.Add("a_bogus", aBogus);

                var queryString = string.Join("&",
                    parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
                string url = $"{BlendTrendApiUrl}?{queryString}";

                var request = new HttpRequestMessage(HttpMethod.Get, url);
                foreach (var h in headers)
                    request.Headers.TryAddWithoutValidation(h.Key, h.Value);
                request.Headers.TryAddWithoutValidation("Cookie", cookieStr);

                var response = await DownloadFileUtil.SharedClient.SendAsync(request);
                var content = await response.Content.ReadAsStringAsync();

                if (!response.IsSuccessStatusCode)
                    return null;

                JObject json = JObject.Parse(content);
                int st = json.Value<int?>("st") ?? -1;
                if (st != 0)
                    return null;

                var trends = json["data"]?["trends"] as JArray;
                if (trends == null)
                    return null;

                // 按时间点合并所有指标
                var map = new Dictionary<string, CompassTrendPoint>();
                foreach (var trend in trends)
                {
                    string timeStr = FormatTrendTime(trend["horizontal"]?.ToString());
                    string pointName = trend["point_name"]?.ToString();
                    double value = trend["vertical"]?.ToObject<double>() ?? 0;

                    if (!map.TryGetValue(timeStr, out var point))
                    {
                        point = new CompassTrendPoint { Time = timeStr };
                        map[timeStr] = point;
                    }

                    AssignPointField(point, pointName, value);
                }

                return map.Values.OrderBy(p => p.Time).ToList();
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"拉取在线人数曲线异常: {ex.Message}", "LiveOnlineTrendPuller");
                return null;
            }
        }

        /// <summary>
        /// 按 point_name 将趋势值分配到 <see cref="CompassTrendPoint"/> 对应字段。
        /// </summary>
        /// <param name="point">目标数据点</param>
        /// <param name="pointName">API 返回的 point_name</param>
        /// <param name="value">API 返回的 vertical 值</param>
        private static void AssignPointField(CompassTrendPoint point, string pointName, double value)
        {
            switch (pointName)
            {
                case "online_user_cnt": point.Value = value; break;
                case "watch_ucnt": point.WatchUcnt = value; break;
                case "pay_cnt": point.PayCnt = value; break;
                case "pay_amt": point.PayAmt = value; break;
                case "fans_club_ucnt": point.FansClubUcnt = value; break;
                case "incr_fans_cnt": point.IncrFansCnt = value; break;
                case "stat_cost": point.StatCost = value; break;
            }
        }

        /// <summary>
        /// 将 API 返回的原始时间字符串转换为标准格式。
        /// 优先按 Unix 秒时间戳解析，失败则按 DateTime 字符串解析，
        /// 均失败时原样返回。时区固定东八区 (UTC+8)。
        /// </summary>
        /// <param name="raw">API 返回的 horizontal 字段原始值</param>
        /// <returns>yyyy-MM-dd HH:mm:ss 格式的时间字符串</returns>
        private static string FormatTrendTime(string raw)
        {
            if (string.IsNullOrEmpty(raw))
                return "";

            if (long.TryParse(raw, out long ts))
                return DateTimeOffset.FromUnixTimeSeconds(ts).ToOffset(TimeSpan.FromHours(8)).ToString("yyyy-MM-dd HH:mm:ss");

            if (DateTime.TryParse(raw, out DateTime dt))
                return dt.ToString("yyyy-MM-dd HH:mm:ss");

            return raw;
        }
    }
}
