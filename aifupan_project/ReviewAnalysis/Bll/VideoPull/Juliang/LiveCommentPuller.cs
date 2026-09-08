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
    /// 巨量罗盘 review_comment API 返回的单条评论/弹幕。
    /// 包含评论内容、用户昵称和评论事件时间。
    /// </summary>
    public class CompassComment
    {
        /// <summary>评论 / 弹幕文本内容</summary>
        public string Content { get; set; }

        /// <summary>评论用户昵称</summary>
        public string NickName { get; set; }

        /// <summary>评论事件时间，格式 yyyy-MM-dd HH:mm:ss（东八区，由 event_ts 转换）</summary>
        public string EventTime { get; set; }
    }

    /// <summary>
    /// 巨量罗盘 - 直播回放评论/弹幕拉取器。
    /// 调用 review_comment API 按时间范围拉取评论数据。
    /// 复用 <see cref="JuliangVideoDownloader"/> 的 Cookie 和 Headers 构建逻辑。
    /// </summary>
    public class LiveCommentPuller
    {
        /// <summary>review_comment API 地址，query 参数: room_id, page_no, page_size, comment_start_ts, comment_end_ts, is_important</summary>
        private const string ReviewCommentApiUrl =
            "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/review_comment";

        /// <summary>
        /// 拉取直播回放弹幕/评论数据。
        /// 为防止单次请求数据量过大触发 API 限流，将时间范围按 5 分钟（300 秒）切分为多个时间段，
        /// 每个时间段内逐页翻页（page_size=500），直到达到 maxCount 上限或所有时间段拉取完毕。
        /// </summary>
        /// <param name="roomId">直播间 room_id（对应 batchNumber）</param>
        /// <param name="secUid">主播 sec_uid，用于从本地 Cookie 文件加载认证凭据</param>
        /// <param name="startTs">评论开始时间的 Unix 秒时间戳</param>
        /// <param name="endTs">评论结束时间的 Unix 秒时间戳</param>
        /// <param name="maxCount">最多获取评论条数上限；0 表示不限制，拉取全部</param>
        /// <returns>按时间顺序排列的评论列表；Cookie 缺失 / API 异常时返回 null</returns>
        public static async Task<List<CompassComment>> FetchAsync(string roomId, string secUid, long startTs, long endTs, int maxCount = 0)
        {
            try
            {
                string cookieStr = JuliangVideoDownloader.BuildCookieString(secUid);
                if (string.IsNullOrEmpty(cookieStr))
                    return null;

                var headers = JuliangVideoDownloader.BuildCompassHeaders(roomId);
                var allComments = new List<CompassComment>();
                const int segmentSeconds = 300;

                long segStart = startTs;
                while (segStart < endTs)
                {
                    long segEnd = Math.Min(segStart + segmentSeconds, endTs);

                    int pageNo = 1;
                    while (true)
                    {
                        var (comments, total) = await FetchPageAsync(roomId, cookieStr, headers, segStart, segEnd, pageNo);

                        if (comments != null && comments.Count > 0)
                        {
                            allComments.AddRange(comments);
                            if (maxCount > 0 && allComments.Count >= maxCount)
                                return allComments;
                        }

                        if (comments == null || pageNo * 500 >= total)
                            break;

                        pageNo++;
                    }

                    segStart = segEnd;
                }

                return allComments;
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"拉取弹幕数据异常: {ex.Message}", "LiveCommentPuller");
                return null;
            }
        }

        /// <summary>
        /// 拉取单页评论数据。
        /// 请求 review_comment API，解析 data.comments 数组并将 event_ts（Unix 秒）转为东八区时间字符串。
        /// </summary>
        /// <param name="roomId">直播间 room_id</param>
        /// <param name="cookieStr">已构建的 Cookie 字符串</param>
        /// <param name="headers">已构建的 HTTP 请求头</param>
        /// <param name="startTs">本时间段的开始 Unix 秒时间戳</param>
        /// <param name="endTs">本时间段的结束 Unix 秒时间戳</param>
        /// <param name="pageNo">当前页码，从 1 开始</param>
        /// <returns>元组: (本页评论列表, 总评论数 total)；请求失败时列表为 null</returns>
        private static async Task<(List<CompassComment> comments, int total)> FetchPageAsync(
            string roomId, string cookieStr, Dictionary<string, string> headers,
            long startTs, long endTs, int pageNo)
        {
            var fp = JuliangApiDataApi.GetFp();
            var msToken = JuliangApiDataApi.GetMsToken();
            var parameters = new Dictionary<string, string>
            {
                { "room_id", roomId },
                { "page_no", pageNo.ToString() },
                { "page_size", "500" },
                { "comment_start_ts", startTs.ToString() },
                { "comment_end_ts", endTs.ToString() },
                { "is_important", "false" },
                { "_lid", JuliangApiDataApi.GetLid() },
                { "verifyFp", fp },
                { "fp", fp },
                { "msToken", msToken }
            };

            var aBogus = JuliangApiDataApi.GetAB(JuliangVideoDownloader.UA, parameters);
            parameters.Add("a_bogus", aBogus);

            var queryString = string.Join("&",
                parameters.Select(kv => $"{WebUtility.UrlEncode(kv.Key)}={WebUtility.UrlEncode(kv.Value)}"));
            string url = $"{ReviewCommentApiUrl}?{queryString}";

            var request = new HttpRequestMessage(HttpMethod.Get, url);
            foreach (var h in headers)
                request.Headers.TryAddWithoutValidation(h.Key, h.Value);
            request.Headers.TryAddWithoutValidation("Cookie", cookieStr);

            var response = await DownloadFileUtil.SharedClient.SendAsync(request);
            var content = await response.Content.ReadAsStringAsync();

            if (!response.IsSuccessStatusCode)
                return (null, 0);

            JObject json = JObject.Parse(content);
            int st = json.Value<int?>("st") ?? -1;
            if (st != 0)
                return (null, 0);

            int total = json["data"]?["page_result"]?.Value<int?>("total") ?? 0;
            var commentsArray = json["data"]?["comments"] as JArray;
            if (commentsArray == null)
                return (null, total);

            var comments = new List<CompassComment>();
            var dfNickName = "匿名用户";
            foreach (var c in commentsArray)
            {
                long eventTs = c["event_ts"]?.ToObject<long>() ?? 0;
                string eventTime = eventTs > 0
                    ? DateTimeOffset.FromUnixTimeSeconds(eventTs).ToOffset(TimeSpan.FromHours(8)).ToString("yyyy-MM-dd HH:mm:ss")
                    : "";
                string nickName = c["nick_name"]?.ToString() ?? dfNickName;
                comments.Add(new CompassComment
                {
                    Content = c["content"]?.ToString() ?? "",
                    NickName = "用户".Equals(nickName) ? dfNickName : nickName,
                    EventTime = eventTime
                });
            }

            return (comments, total);
        }
    }
}
