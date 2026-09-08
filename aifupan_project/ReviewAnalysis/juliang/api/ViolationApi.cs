using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using RestSharp;
using System;
using System.Collections.Generic;
using System.Web;

namespace ReviewAnalysis.juliang.api
{
    /// <summary>
    /// 违规数据API（罗盘+巨量百应）
    /// </summary>
    public class ViolationApi
    {
        private static readonly Random _random = new Random();

        /// <summary>
        /// 生成_lid参数
        /// </summary>
        private static string GenerateLid()
        {
            var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var timeStr = timestamp.ToString().Substring(0, 5);
            var randomStr = _random.NextDouble().ToString("F6").Substring(2, 4);
            return timeStr + randomStr;
        }

        /// <summary>
        /// 获取违规警告列表（罗盘API）
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="luopanDt">罗盘Cookie</param>
        /// <returns>JSON字符串</returns>
        public static string GetViolationWarnings(string roomId, string luopanDt)
        {
            try
            {
                var options = new RestClientOptions("https://compass.jinritemai.com")
                {
                    MaxTimeout = -1,
                };
                using (RestClient client = new RestClient(options))
                {
                    var request = new RestRequest("/compass_api/author/live/live_screen/violation_warning_v2", Method.Get)
                        .AddParameter("room_id", roomId)
                        .AddParameter("warn_scene", "2")
                        .AddParameter("_lid", GenerateLid());

                    request.AddHeader("Cookie", $"LUOPAN_DT={luopanDt}");
                    request.AddHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                    request.AddHeader("Accept", "application/json, text/plain, */*");
                    request.AddHeader("Accept-Language", "zh-CN,zh;q=0.9");
                    request.AddHeader("Cache-Control", "no-cache");
                    request.AddHeader("Pragma", "no-cache");
                    request.AddHeader("Referer", $"https://compass.jinritemai.com/talent/live-statement?live_room_id={roomId}&tab=coreTag");
                    request.AddHeader("Sec-Fetch-Site", "same-origin");
                    request.AddHeader("Sec-Fetch-Mode", "cors");
                    request.AddHeader("Sec-Fetch-Dest", "empty");
                    request.AddHeader("Sec-Ch-Ua", "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"");
                    request.AddHeader("Sec-Ch-Ua-Mobile", "?0");
                    request.AddHeader("Sec-Ch-Ua-Platform", "\"Windows\"");

                    RestResponse response = client.ExecuteAsync(request).Result;
                    return response.Content;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"获取违规警告异常==={roomId}");
            }
            return "";
        }

        /// <summary>
        /// 获取违规视频链接（罗盘API）
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="penalizeId">违规单号</param>
        /// <param name="luopanDt">罗盘Cookie</param>
        /// <returns>视频链接</returns>
        public static string GetViolationVideoLink(string roomId, string penalizeId, string luopanDt)
        {
            try
            {
                var options = new RestClientOptions("https://compass.jinritemai.com")
                {
                    MaxTimeout = -1,
                };
                using (RestClient client = new RestClient(options))
                {
                    var request = new RestRequest("/compass_api/author/live/live_screen/violation_video_link", Method.Get)
                        .AddParameter("room_id", roomId)
                        .AddParameter("penalize_id", penalizeId)
                        .AddParameter("_lid", GenerateLid());

                    request.AddHeader("Cookie", $"LUOPAN_DT={luopanDt}");
                    request.AddHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                    request.AddHeader("Accept", "application/json, text/plain, */*");
                    request.AddHeader("Accept-Language", "zh-CN,zh;q=0.9");
                    request.AddHeader("Cache-Control", "no-cache");
                    request.AddHeader("Pragma", "no-cache");
                    request.AddHeader("Referer", $"https://compass.jinritemai.com/talent/live-statement?live_room_id={roomId}&tab=coreTag");
                    request.AddHeader("Sec-Fetch-Site", "same-origin");
                    request.AddHeader("Sec-Fetch-Mode", "cors");
                    request.AddHeader("Sec-Fetch-Dest", "empty");

                    RestResponse response = client.ExecuteAsync(request).Result;
                    var content = response.Content;

                    if (!string.IsNullOrEmpty(content))
                    {
                        var jsonObj = JObject.Parse(content);
                        var link = jsonObj["data"]?["link"]?.ToString();
                        if (!string.IsNullOrEmpty(link))
                        {
                            return link.Replace("\\u0026", "&");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"获取违规视频链接异常==={roomId}==={penalizeId}");
            }
            return "";
        }

        /// <summary>
        /// 获取违规详情（巨量百应API）
        /// </summary>
        /// <param name="penalizeId">违规单号</param>
        /// <param name="sasid">巨量百应Cookie</param>
        /// <returns>JSON字符串</returns>
        public static string GetViolationDetails(string penalizeId, string sasid)
        {
            try
            {
                var options = new RestClientOptions("https://buyin.jinritemai.com")
                {
                    MaxTimeout = -1,
                };
                using (RestClient client = new RestClient(options))
                {
                    var request = new RestRequest("/aweme/v2/governance/margin_control/creator/violations", Method.Get)
                        .AddParameter("offset", "0")
                        .AddParameter("limit", "10")
                        .AddParameter("penalize_id", penalizeId);

                    request.AddHeader("Cookie", $"SASID={sasid}; BUYIN_SASID={sasid}");
                    request.AddHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                    request.AddHeader("Accept", "application/json, text/plain, */*");
                    request.AddHeader("Accept-Language", "zh-CN,zh;q=0.9");
                    request.AddHeader("Cache-Control", "no-cache");
                    request.AddHeader("Pragma", "no-cache");
                    request.AddHeader("Referer", $"https://buyin.jinritemai.com/dashboard/content/author-violation?penalize_id={penalizeId}&open_violation_detail=1");
                    request.AddHeader("Sec-Fetch-Site", "same-origin");
                    request.AddHeader("Sec-Fetch-Mode", "cors");
                    request.AddHeader("Sec-Fetch-Dest", "empty");

                    RestResponse response = client.ExecuteAsync(request).Result;
                    return response.Content;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"获取违规详情异常==={penalizeId}");
            }
            return "";
        }
    }
}
