using douyin.Utils;
using RestSharp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Web;
using System.Windows.Forms.VisualStyles;

namespace ReviewAnalysis.juliang.api
{
    public class FlowOrderSourceApi
    {

        /// <summary>
        /// 获取巨量的流量结构数据
        /// </summary>
        /// <param name="roomId">直播间场次id</param>
        /// <param name="luopanDt">session</param>
        /// <returns></returns>
        public static string getFlowOrderSource(string roomId, string luopanDt)
        {
            try
            {
                var options = new RestClientOptions("https://compass.jinritemai.com")
                {
                    MaxTimeout = -1,
                };
                using (RestClient client = new RestClient(options))
                {
                    // 生成_lid参数
                    var random = new Random();
                    var timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                    var timeStr = timestamp.ToString().Substring(0, 5);
                    var randomStr = random.NextDouble().ToString("F6").Substring(2, 4);
                    var lid = timeStr + randomStr;

                    var request = new RestRequest("/compass_api/content_live/author/basic_live_screen/flow_order_source", Method.Get)
                        .AddParameter("room_id", roomId)
                        .AddParameter("data_range", "1")
                        .AddParameter("_lid", lid);

                    request.AddHeader("Cookie", $"LUOPAN_DT={luopanDt}");
                    request.AddHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0");
                    request.AddHeader("Sec-Fetch-Site", "same-origin");
                    request.AddHeader("Sec-Fetch-Mode", "cors");
                    request.AddHeader("Sec-Fetch-Dest", "empty");
                    request.AddHeader("Sec-Ch-Ua-Platform", "\"Windows\"");
                    request.AddHeader("Sec-Ch-Ua-Mobile", "?0");
                    request.AddHeader("Sec-Ch-Ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
                    request.AddHeader("Referer", $"https://compass.jinritemai.com/screen/talent/main?live_room_id={roomId}");
                    request.AddHeader("Priority", "u=1, i");
                    request.AddHeader("Pragma", "no-cache");
                    request.AddHeader("Cache-Control", "no-cache,no-cache");
                    request.AddHeader("Accept-Language", "zh-CN,zh;q=0.9");
                    request.AddHeader("Accept", "application/json, text/plain, */*");

                    RestResponse response = client.ExecuteAsync(request).Result;
                    string content = response.Content;

                    return content;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"巨量获取流量结构接口发生异常==={roomId}==={luopanDt}");
            }

            return "";
        }
    }
}
