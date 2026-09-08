using CefSharp.Web;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor.Entity;
using ReviewAnalysis.Bll.VedioModels;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.user;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Security.Policy;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Bll.Anchor
{
    public class DouYinAnchorBll
    {

        

        /// <summary>
        /// 获取抖音主播在线状态
        /// </summary>
        /// <param name="anchorUserId">抖音主播用户id</param>
        /// <returns> 0：检测失败 1：在线 2：离线</returns>
        public static int GetOnlineStatus(string anchorUserId)
        {

            AnchorOnlineWebClient anchorOnlineWebClient = AnchorOnlineWebClient.Instance;
            return anchorOnlineWebClient.getAnchorOnlineStatus(anchorUserId);
        }

        /// <summary>
        /// 检测抖音主播是否在直播，如果在直播返回RoomId，否则返回null
        /// </summary>
        public static async Task<string> GetLiveRoomIdIfOnline(AnchorInfo anchorInfo)
        {
            if (string.IsNullOrEmpty(anchorInfo?.AnchorUserId)) return null;
            int status = GetOnlineStatus(anchorInfo.AnchorUserId);
            if (status != 1) return null;
            var entity = await AnchorInfoWebClient.Instance
                .GetLiveInfoByAnchorNumber(anchorInfo.anchorNumber);
            if (entity == null)
                entity = await GetDouYinAnchorInfo(anchorInfo.LiveUrl);
            return entity?.RoomId;
        }

        /// <summary>
        /// 从抖音获取主播信息-同步
        /// </summary>
        /// <param name="liveRoomUrl">主播直播间地址</param>
        /// <returns></returns>
        public static DouYinAnchorInfoEntity GetDouYinAnchorInfoSync(string liveRoomUrl)
        {
            string htmlStr = "";
            DouyinHttpClient webClient = DouyinHttpClient.Instance;
            try
            {

                DouYinAnchorInfoEntity douYinAnchorInfoEntity = new DouYinAnchorInfoEntity();

                // 获取直播间网页内容
                htmlStr = webClient.GetHtmlText(liveRoomUrl);
                if (string.IsNullOrEmpty(htmlStr) || !htmlStr.Contains("state"))
                {
                    // 没取成功，再取一次
                    htmlStr = webClient.GetHtmlText(liveRoomUrl);
                }

                // 获取cookies
                //var cookies2 = webClient.CookieContainer.GetCookies(new Uri(liveRoomUrl));
                //douYinAnchorInfoEntity.Ttwid = cookies2["ttwid"]?.Value;
                //douYinAnchorInfoEntity.AcNonce = cookies2["__ac_nonce"]?.Value;

                // 用正则取出json内容
                var matchJsonStr = Regex.Match(htmlStr, @"(\{\\""state\\\"":.*?)]\\n\""\]\)");
                if (matchJsonStr == null)
                {
                    //采集出错 url非法
                    FileUtils.LogRecrd($"{liveRoomUrl}", $"采集出错 url非法");
                    return null;
                }

                // 如果取出不成功，换一种正则
                if (!matchJsonStr.Success)
                {
                    matchJsonStr = Regex.Match(htmlStr, @"(\{\\""common\\\"":.*?)]\\n""]\)</script><div hidden");
                }

                // 取出json第一个捕获组中的内容
                var jsonStr = matchJsonStr.Groups[1].Value;
                //var cleanedString = jsonStr.Replace("\\", "").Replace(@"u0026", @"&");
                var cleanedString = jsonStr.Replace("\\\\", "\\").Replace("\\\"", "\"").Replace(@"\u0026", @"&");
                try
                {
                    string errorPromStr = JObject.Parse(cleanedString)["state"]["detailExtra"]["errorPrompts"].ToString();
                    if (!string.IsNullOrEmpty(errorPromStr))
                    {
                        if (errorPromStr.Equals("直播已结束"))
                        {
                            return null;
                        }
                    }
                }
                catch
                {
                    FileUtils.log("直播未结束===继续");
                }

                var roomStoreMatch = Regex.Match(cleanedString, @"""roomStore"":(.*?),""linkmicStore""", RegexOptions.Singleline);
                var roomStore = roomStoreMatch.Groups[1].Value;
                var noLive = roomStoreMatch.Groups[1].Value;
                try
                {
                    var roomInfo = JObject.Parse(noLive)["roomInfo"];
                    if (roomInfo["roomId"] != null && roomInfo["roomId"].ToString() == "")
                    {
                        var anchor = JObject.Parse(noLive)["roomInfo"]["anchor"];
                        douYinAnchorInfoEntity.SecUid = anchor["sec_uid"].ToString();
                        douYinAnchorInfoEntity.AnchorId = anchor["id_str"].ToString();
                        douYinAnchorInfoEntity.AnchorName = anchor["nickname"].ToString();
                        var avatarThumb = anchor["avatar_thumb"];
                        JArray thumb2 = (JArray)avatarThumb["url_list"];
                        douYinAnchorInfoEntity.AnchorThump = thumb2[0].ToString();
                        return douYinAnchorInfoEntity;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log($"获取roomInfo序列化失败：{noLive}");
                    //FileUtils.log($"错误信息:{ex.Message.ToString()}", "获取roomInfo序列化失败");
                }
                roomStore = roomStore.Split(new string[] { @",""has_commerce_goods""" }, StringSplitOptions.None)[0] + "}}}";
                try
                {
                    //roomStore 是否能序列化
                    JObject.Parse(roomStore);
                }
                catch (Exception e)
                {
                    FileUtils.log($"错误信息:{e.Message.ToString()}", "采集过程中，发生了json解析失败，需要重新去掉stream_data字符串");
                    int streamDataStartIndex = roomStore.IndexOf("\"stream_data\":") - 1; // "+12"是为了跳过"\"stream_data\":"
                    int endIndex = roomStore.IndexOf("\"extra\"") - 3;
                    string streamData = roomStore.Substring(streamDataStartIndex, endIndex - streamDataStartIndex);
                    roomStore = roomStore.Replace(streamData, "");
                }
                var jsonData = JObject.Parse(roomStore)["roomInfo"]["room"];
                //直播间的信息
                //直播状态  2正在直播，4 是未开播
                douYinAnchorInfoEntity.LiveStatus = Convert.ToInt32(jsonData["status"]);
                if (douYinAnchorInfoEntity.LiveStatus != 2)
                {
                    try
                    {
                        var anchor = JObject.Parse(noLive)["roomInfo"]["anchor"];
                        douYinAnchorInfoEntity.SecUid = anchor["sec_uid"].ToString();
                        douYinAnchorInfoEntity.AnchorId = anchor["id_str"].ToString();
                        douYinAnchorInfoEntity.AnchorName = anchor["nickname"].ToString();
                        var avatarThumb = anchor["avatar_thumb"];
                        JArray thumb2 = (JArray)avatarThumb["url_list"];
                        douYinAnchorInfoEntity.AnchorThump = thumb2[0].ToString();
                        return douYinAnchorInfoEntity;
                    }
                    catch (JsonReaderException ex)
                    {
                        FileUtils.log($"换一种截取的解法，截取掉relevantRooms---danmakuQueue的字符串，错误信息:{ex.Message.ToString()}", "直播地址采集报错");
                        int subIndexStart = cleanedString.IndexOf("relevantRooms");
                        string subSart = cleanedString.Substring(0, subIndexStart);
                        int subIndexEnd = cleanedString.IndexOf("danmakuQueue");
                        string subEnd = cleanedString.Substring(subIndexEnd, cleanedString.Length - subIndexEnd);
                        cleanedString = subSart + subEnd;
                        var anchor = JObject.Parse(cleanedString)["state"]["roomStore"]["roomInfo"]["anchor"];
                        douYinAnchorInfoEntity.SecUid = anchor["sec_uid"].ToString();
                        douYinAnchorInfoEntity.AnchorId = anchor["id_str"].ToString();
                        douYinAnchorInfoEntity.AnchorName = anchor["nickname"].ToString();
                        var avatarThumb = anchor["avatar_thumb"];
                        JArray thumb2 = (JArray)avatarThumb["url_list"];
                        douYinAnchorInfoEntity.AnchorThump = thumb2[0].ToString();
                        return douYinAnchorInfoEntity;
                    }
                }

                var owner = jsonData["owner"];
                //主播的信息
                douYinAnchorInfoEntity.AnchorName = owner["nickname"].ToString();
                douYinAnchorInfoEntity.SecUid = owner["sec_uid"].ToString();
                douYinAnchorInfoEntity.AnchorId = owner["id_str"].ToString();
                var avatar_thumb = owner["avatar_thumb"];
                JArray thumb = (JArray)avatar_thumb["url_list"];
                douYinAnchorInfoEntity.AnchorThump = thumb[0].ToString();

                //直播间Id
                douYinAnchorInfoEntity.RoomId = jsonData["id_str"].ToString();

                //直播标题
                douYinAnchorInfoEntity.RoomTitle = jsonData["title"].ToString();
                //访问人数 简写：如5000+
                douYinAnchorInfoEntity.BriefNumber = jsonData["user_count_str"].ToString();

                //获取直播封面图
                var cover = jsonData["cover"];
                JArray url_list = (JArray)cover["url_list"];
                douYinAnchorInfoEntity.RoomConverUrl = url_list[0].ToString();


                // 直播推流地址
                JToken stream_url = jsonData["stream_url"];
                // 封装直播推流地址
                List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = packageStreamUrl(stream_url);
                douYinAnchorInfoEntity.StreamInfos = streamInfos;

                //访问量记录
                var stats = jsonData["stats"];
                //进入直播间的总人数 例如：5W+
                douYinAnchorInfoEntity.TotalVisits = stats["total_user_str"].ToString();
                //采集的当前时间的直播间人数 准确的例如:5036
                douYinAnchorInfoEntity.CurrentNumber = stats["user_count_str"].ToString();
                return douYinAnchorInfoEntity;
            }
            catch (JsonReaderException ex)
            {
                FileUtils.log($"{htmlStr},错误信息：{ex.Message.ToString()}", "json解析抛异常====最开始采集的页面信息");
                FileUtils.LogRecrd($"{htmlStr}", $"==={liveRoomUrl}采集数据json解析异常===");
                throw; // 重新抛出异常以便调用方处理
            }
            catch (Exception ex)
            {
                //if (ex.Message.ToString().Contains("Too Many Requests") || htmlStr.Contains("验证码中间页"))
                //{
                //    // 使用代理
                //    webClient.UseProxyIP();
                //}

                // 使用代理
                webClient.UseProxyIP();

                FileUtils.log($"{htmlStr}", $"抛异常====最开始采集的页面信息,异常信息:{ex.ToString()}");
                FileUtils.LogRecrd($"{ex.ToString()}", $"==={liveRoomUrl}采集数据异常===");


                throw; // 重新抛出异常以便调用方处理
            }
        }

        /// <summary>
        /// 从抖音获取主播信息-异步
        /// </summary>
        /// <param name="liveRoomUrl">主播直播间地址</param>
        /// <returns></returns>
        public static async Task<DouYinAnchorInfoEntity> GetDouYinAnchorInfo(string liveRoomUrl)
        {
            string htmlStr = "";
            DouyinHttpClient webClient = DouyinHttpClient.Instance;
            try
            {
                DouYinAnchorInfoEntity douYinAnchorInfoEntity = new DouYinAnchorInfoEntity();

                // 获取直播间网页内容
                htmlStr = await webClient.GetHtmlTextAsync(liveRoomUrl);
                if (string.IsNullOrEmpty(htmlStr) || !htmlStr.Contains("state"))
                {
                    // 没取成功，再取一次
                    htmlStr = await webClient.GetHtmlTextAsync(liveRoomUrl);
                }

                // 获取cookies
                //var cookies2 = webClient.CookieContainer.GetCookies(new Uri(liveRoomUrl));
                //douYinAnchorInfoEntity.Ttwid = cookies2["ttwid"]?.Value;
                //douYinAnchorInfoEntity.AcNonce = cookies2["__ac_nonce"]?.Value;

                // 用正则取出json内容
                var matchJsonStr = Regex.Match(htmlStr, @"(\{\\""state\\\"":.*?)]\\n\""\]\)");
                if (matchJsonStr == null)
                {
                    //采集出错 url非法
                    FileUtils.LogRecrd($"{liveRoomUrl}", $"采集出错 url非法");
                    return null;
                }

                // 如果取出不成功，换一种正则
                if (!matchJsonStr.Success)
                {
                    matchJsonStr = Regex.Match(htmlStr, @"(\{\\""common\\\"":.*?)]\\n""]\)</script><div hidden");
                }

                // 取出json第一个捕获组中的内容
                var jsonStr = matchJsonStr.Groups[1].Value;
                //var cleanedString = jsonStr.Replace("\\", "").Replace(@"u0026", @"&");
                var cleanedString = jsonStr.Replace("\\\\", "\\").Replace("\\\"", "\"").Replace(@"\u0026", @"&");
                try
                {
                    string errorPromStr = JObject.Parse(cleanedString)["state"]["detailExtra"]["errorPrompts"].ToString();
                    if (!string.IsNullOrEmpty(errorPromStr))
                    {
                        if (errorPromStr.Equals("直播已结束"))
                        {
                            return null;
                        }
                    }
                }
                catch
                {
                    FileUtils.log("直播未结束===继续");
                }

                var roomStoreMatch = Regex.Match(cleanedString, @"""roomStore"":(.*?),""linkmicStore""", RegexOptions.Singleline);
                var roomStore = roomStoreMatch.Groups[1].Value;
                var noLive = roomStoreMatch.Groups[1].Value;
                try
                {
                    var roomInfo = JObject.Parse(noLive)["roomInfo"];
                    if (roomInfo["roomId"] != null && roomInfo["roomId"].ToString() == "")
                    {
                        var anchor = JObject.Parse(noLive)["roomInfo"]["anchor"];
                        douYinAnchorInfoEntity.SecUid = anchor["sec_uid"].ToString();
                        douYinAnchorInfoEntity.AnchorId = anchor["id_str"].ToString();
                        douYinAnchorInfoEntity.AnchorName = anchor["nickname"].ToString();
                        var avatarThumb = anchor["avatar_thumb"];
                        JArray thumb2 = (JArray)avatarThumb["url_list"];
                        douYinAnchorInfoEntity.AnchorThump = thumb2[0].ToString();
                        return douYinAnchorInfoEntity;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log($"获取roomInfo序列化失败：{noLive}");
                    //FileUtils.log($"错误信息:{ex.Message.ToString()}", "获取roomInfo序列化失败");
                }
                roomStore = roomStore.Split(new string[] { @",""has_commerce_goods""" }, StringSplitOptions.None)[0] + "}}}";
                try
                {
                    //roomStore 是否能序列化
                    JObject.Parse(roomStore);
                }
                catch (Exception e)
                {
                    FileUtils.log($"错误信息:{e.Message.ToString()}", "采集过程中，发生了json解析失败，需要重新去掉stream_data字符串");
                    int streamDataStartIndex = roomStore.IndexOf("\"stream_data\":") - 1; // "+12"是为了跳过"\"stream_data\":"
                    int endIndex = roomStore.IndexOf("\"extra\"") - 3;
                    string streamData = roomStore.Substring(streamDataStartIndex, endIndex - streamDataStartIndex);
                    roomStore = roomStore.Replace(streamData, "");
                }
                var jsonData = JObject.FromObject(JObject.Parse(roomStore)["roomInfo"]["room"]);
                //直播间的信息
                //直播状态  2正在直播，4 是未开播
                douYinAnchorInfoEntity.LiveStatus = Convert.ToInt32(jsonData["status"]);
                if (douYinAnchorInfoEntity.LiveStatus != 2)
                {
                    try
                    {
                        var anchor = JObject.Parse(noLive)["roomInfo"]["anchor"];
                        douYinAnchorInfoEntity.SecUid = anchor["sec_uid"].ToString();
                        douYinAnchorInfoEntity.AnchorId = anchor["id_str"].ToString();
                        douYinAnchorInfoEntity.AnchorName = anchor["nickname"].ToString();
                        var avatarThumb = anchor["avatar_thumb"];
                        JArray thumb2 = (JArray)avatarThumb["url_list"];
                        douYinAnchorInfoEntity.AnchorThump = thumb2[0].ToString();
                        //return douYinAnchorInfoEntity;
                    }
                    catch (JsonReaderException ex)
                    {
                        FileUtils.log($"换一种截取的解法，截取掉relevantRooms---danmakuQueue的字符串，错误信息:{ex.Message.ToString()}", "直播地址采集报错");
                        int subIndexStart = cleanedString.IndexOf("relevantRooms");
                        string subSart = cleanedString.Substring(0, subIndexStart);
                        int subIndexEnd = cleanedString.IndexOf("danmakuQueue");
                        string subEnd = cleanedString.Substring(subIndexEnd, cleanedString.Length - subIndexEnd);
                        cleanedString = subSart + subEnd;
                        var anchor = JObject.Parse(cleanedString)["state"]["roomStore"]["roomInfo"]["anchor"];
                        douYinAnchorInfoEntity.SecUid = anchor["sec_uid"].ToString();
                        douYinAnchorInfoEntity.AnchorId = anchor["id_str"].ToString();
                        douYinAnchorInfoEntity.AnchorName = anchor["nickname"].ToString();
                        var avatarThumb = anchor["avatar_thumb"];
                        JArray thumb2 = (JArray)avatarThumb["url_list"];
                        douYinAnchorInfoEntity.AnchorThump = thumb2[0].ToString();
                        //return douYinAnchorInfoEntity;
                    }
                }

                if (douYinAnchorInfoEntity.LiveStatus == 4)
                {
                    //FileUtils.LogRecrd($"{htmlStr}", $"主播在线==但采集不到直播流");
                }

                if (jsonData.ContainsKey("owner"))
                {
                    var owner = jsonData["owner"];
                    //主播的信息
                    douYinAnchorInfoEntity.AnchorName = owner["nickname"].ToString();
                    douYinAnchorInfoEntity.SecUid = owner["sec_uid"].ToString();
                    douYinAnchorInfoEntity.AnchorId = owner["id_str"].ToString();
                    var avatar_thumb = owner["avatar_thumb"];
                    JArray thumb = (JArray)avatar_thumb["url_list"];
                    douYinAnchorInfoEntity.AnchorThump = thumb[0].ToString();
                }


                //直播间Id
                if (jsonData.ContainsKey("id_str"))
                {
                    douYinAnchorInfoEntity.RoomId = jsonData["id_str"].ToString();
                }


                //直播标题
                if (jsonData.ContainsKey("title"))
                {
                    douYinAnchorInfoEntity.RoomTitle = jsonData["title"].ToString();
                }

                //访问人数 简写：如5000+
                if (jsonData.ContainsKey("user_count_str"))
                {
                    douYinAnchorInfoEntity.BriefNumber = jsonData["user_count_str"].ToString();
                }


                //获取直播封面图
                if (jsonData.ContainsKey("cover"))
                {
                    var cover = jsonData["cover"];
                    JArray url_list = (JArray)cover["url_list"];
                    douYinAnchorInfoEntity.RoomConverUrl = url_list[0].ToString();
                }



                // 直播推流地址
                if (jsonData.ContainsKey("stream_url"))
                {
                    JToken stream_url = jsonData["stream_url"];
                    // 封装直播推流地址
                    List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = packageStreamUrl(stream_url);
                    douYinAnchorInfoEntity.StreamInfos = streamInfos;
                }


                //访问量记录
                if (jsonData.ContainsKey("stats"))
                {
                    var stats = jsonData["stats"];
                    //进入直播间的总人数 例如：5W+
                    douYinAnchorInfoEntity.TotalVisits = stats["total_user_str"].ToString();
                    //采集的当前时间的直播间人数 准确的例如:5036
                    douYinAnchorInfoEntity.CurrentNumber = stats["user_count_str"].ToString();
                }

                return douYinAnchorInfoEntity;
            }
            catch (JsonReaderException ex)
            {
                FileUtils.log($"{htmlStr},错误信息：{ex.Message.ToString()}", "json解析抛异常====最开始采集的页面信息");
                FileUtils.LogRecrd($"{htmlStr}", $"==={liveRoomUrl}采集数据json解析异常===");
                throw; // 重新抛出异常以便调用方处理
            }
            catch (Exception ex)
            {
                //if (ex.Message.ToString().Contains("Too Many Requests") || htmlStr.Contains("验证码中间页"))
                //{
                //    // 使用代理
                //    webClient.UseProxyIP();
                //}
                // 使用代理
                webClient.UseProxyIP();

                FileUtils.log($"{htmlStr}", $"抛异常====最开始采集的页面信息,异常信息:{ex.ToString()}");
                FileUtils.LogRecrd($"{ex.ToString()}", $"==={liveRoomUrl}采集数据异常===");

                throw; // 重新抛出异常以便调用方处理
            }
        }


        /// <summary>
        /// 封装直播推流地址
        /// </summary>
        /// <param name="stream_url">直播推流地址</param>
        /// <returns></returns>
        private static List<DouYinAnchorInfoEntity.StreamInfo> packageStreamUrl(JToken stream_url)
        {
            List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = new List<DouYinAnchorInfoEntity.StreamInfo>();

            // 默认的清晰度规格
            var default_name = stream_url["live_core_sdk_data"]["pull_data"]["options"]["default_quality"]["name"].ToString();

            //flv源地址  SD1:标清 SD2:高清 HD:超清 FULL_HD:当前直播最高画质,最高蓝光
            JToken flvUrls = stream_url["flv_pull_url"];
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
            JToken hlsUrls = stream_url["hls_pull_url_map"];
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

        private static string getVideoUrls(JToken hlsUrls, string key)
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
        /// 获取直播视频流地址
        /// </summary>
        /// <param name="douYinAnchorInfoEntity">采集到的抖音主播信息</param>
        /// <param name="liveSource">直播源  0 m3u8  1 flv</param>
        /// <param name="recordDefinition">视频清晰度 0 标清 1高清 2超清 3 蓝光</param>
        /// <returns></returns>
        public static string GetStreamUrl(DouYinAnchorInfoEntity douYinAnchorInfoEntity, int liveSource, int recordDefinition)
        {

            string streaUrl = "";

            List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = douYinAnchorInfoEntity.StreamInfos;
            if (streamInfos == null || streamInfos.Count == 0)
            {
                return streaUrl;
            }


            List<DouYinAnchorInfoEntity.StreamInfo> list = new List<DouYinAnchorInfoEntity.StreamInfo>();

            // 将符合的直播源添加进集合
            foreach (DouYinAnchorInfoEntity.StreamInfo streamInfo in streamInfos)
            {
                if (streamInfo.LiveSource == liveSource)
                {
                    list.Add(streamInfo);
                }
            }
            // 没有符合的，全部添加
            if (list.Count < 1)
            {
                foreach (DouYinAnchorInfoEntity.StreamInfo streamInfo in streamInfos)
                {
                    list.Add(streamInfo);
                }
            }

            List<DouYinAnchorInfoEntity.StreamInfo> listSort = list.OrderBy(dto => dto.Quality).ToList();

            if (listSort.Count < recordDefinition + 1)
            {
                DouYinAnchorInfoEntity.StreamInfo streamInfo = listSort[listSort.Count - 1];
                streaUrl = streamInfo.StreaUrl;
            }
            else
            {
                DouYinAnchorInfoEntity.StreamInfo streamInfo = listSort[recordDefinition];
                streaUrl = streamInfo.StreaUrl;
            }
            return streaUrl;
        }

        /// <summary>
        /// 从抖音获取主播信息，并转成AnchorInfo对象
        /// </summary>
        /// <param name="url">直播url</param>
        /// <param name="urlType">主播url类型 0：抖音号链接 1：直播间链接</param>
        /// <returns></returns>
        public static async Task<AnchorInfo> GetAnchorInfo(string url, int urlType)
        {
            AnchorInfoWebClient anchorWebClient = AnchorInfoWebClient.Instance;
            for (int i = 0; i < 3; i++)
            {
                try
                {
                    if(urlType == 0)
                    {
                        // 先尝试方案一
                        string anchorNumber = url.Substring(url.LastIndexOf('/') + 1);
                        string secUid = await anchorWebClient.GetSecUidByAnchorNumber(anchorNumber);
                        if(!string.IsNullOrEmpty(secUid))
                        {
                            AnchorBaseInfoDto getAnchorBySecUidDto = await anchorWebClient.GetAnchorBySecUid(secUid);
                            if(getAnchorBySecUidDto != null)
                            {
                                AnchorInfo anchorInfo = new AnchorInfo();
                                anchorInfo.SecUid = secUid;
                                anchorInfo.AnchorPlatform = "DouYinLive";
                                anchorInfo.AnchorName = getAnchorBySecUidDto.anchorName;
                                anchorInfo.AnchorAvatar = getAnchorBySecUidDto.anchorAvatar;
                                anchorInfo.LiveUrl = url;
                                anchorInfo.HomeUrl = "https://www.douyin.com/user/" + secUid;
                                anchorInfo.AnchorUserId = getAnchorBySecUidDto.anchorUserId;
                                anchorInfo.platform = 0;
                                return anchorInfo;
                            }
                            else
                            {
                                FileUtils.LogRecrd($"{url}", $"第{i + 1}添加抖音主播失败，原因：根据SecUid获取主播信息为空");
                            }
                        }
                        else
                        {
                            FileUtils.LogRecrd($"{url}", $"第{i + 1}添加抖音主播失败，原因：SecUid未获取到");
                        }
                    }


                    // 尝试方案二
                    DouYinAnchorInfoEntity douYinAnchorInfoEntity = await GetDouYinAnchorInfo(url);
                    if (douYinAnchorInfoEntity == null)
                    {
                        FileUtils.LogError($"{url}", $"第{i + 1}添加抖音主播失败，原因：从抖音号直播间页面获取的主播信息为空");
                    }
                    else
                    {
                        AnchorInfo anchorInfo = new AnchorInfo();
                        anchorInfo.SecUid = douYinAnchorInfoEntity.SecUid;
                        anchorInfo.AnchorPlatform = "DouYinLive";
                        anchorInfo.AnchorName = douYinAnchorInfoEntity.AnchorName;
                        anchorInfo.AnchorAvatar = douYinAnchorInfoEntity.AnchorThump;
                        anchorInfo.LiveUrl = url;
                        anchorInfo.HomeUrl = "https://www.douyin.com/user/" + douYinAnchorInfoEntity.SecUid;
                        anchorInfo.AnchorUserId = douYinAnchorInfoEntity.AnchorId;
                        anchorInfo.platform = 0;

                        if (urlType == 1)
                        {
                            // 采集主页，获取抖音号
                            string anchorNum = await DouyinLiveParser.getAnchorNum(anchorInfo.HomeUrl);
                            if (string.IsNullOrEmpty(anchorNum))
                            {
                                // 使用代理采集
                                anchorNum = await DouyinLiveParser.getAnchorNum(anchorInfo.HomeUrl, true);
                            }

                            if (!string.IsNullOrEmpty(anchorNum))
                            {
                                anchorInfo.LiveUrl = "https://live.douyin.com/" + anchorNum;
                            }
                            else
                            {
                                FileUtils.LogError($"{url}", $"第{i + 1}添加抖音主播失败，原因：从直播间页面获取的抖音号为空");
                                return null;
                            }
                        }

                        return anchorInfo;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"第{i + 1}添加抖音主播失败发生异常");
                }
                Thread.Sleep(3000);
            }
            return null;

        }
    }
}
