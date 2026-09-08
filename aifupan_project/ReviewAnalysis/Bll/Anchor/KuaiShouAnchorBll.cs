using AngleSharp.Dom;
using AngleSharp;
using Qiniu.Storage;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Bll.Anchor.Entity;
using Newtonsoft.Json;
using CefSharp;
using ReviewAnalysis.Utils.kuaishou;

namespace ReviewAnalysis.Bll.Anchor
{
    public class KuaiShouAnchorBll
    {

        private static IConfiguration config = Configuration.Default.WithDefaultLoader();

        /// <summary>
        /// 获取快手主播直播信息，包括直播流等信息
        /// </summary>
        /// <param name="webIdOrCustomId">快手网页id或自定义id</param>
        /// <returns></returns>
        public static async Task<DouYinAnchorInfoEntity> GetLiveAnchorInfo(string webIdOrCustomId)
        {
            try
            {
                KuaiShouGetStreamClient instance = KuaiShouGetStreamClient.Instance;
                JObject jobject = await instance.getAnchorLiveInfo(webIdOrCustomId);

                //JObject jobject = await getKuaishouHtmlJObject(webIdOrCustomId);

                if (jobject == null)
                {
                    FileUtils.LogRecrd($"{webIdOrCustomId}", $"获取快手主播直播信息内容异常：内容为空");
                    return null;
                }

                JArray playList = (JArray)jobject?["playList"];
                if (playList == null || playList.Count < 1)
                {
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(jobject)}", $"获取快手主播直播信息内容异常：playList为空==={webIdOrCustomId}");
                    return null;
                }

                JToken playInfo = playList[0];
                // 主播信息
                JToken author = playInfo["author"];
                if (author == null)
                {
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(jobject)}", $"获取快手主播直播信息内容异常：author为空==={webIdOrCustomId}");
                    return null;
                }

                // 直播流信息列表
                JArray representation = (JArray)playInfo?["liveStream"]?["playUrls"]?["h264"]?["adaptationSet"]?["representation"];
                if(representation == null || representation.Count < 1)
                {
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(jobject)}", $"获取快手主播直播信息内容异常：直播流信息列表为空==={webIdOrCustomId}");
                    return null;
                }

                
                try
                {
                    DouYinAnchorInfoEntity anchorInfoEntity = new DouYinAnchorInfoEntity();
                    // 封装主播信息
                    anchorInfoEntity.AnchorId = author["originUserId"].Value<string>();
                    anchorInfoEntity.SecUid = author["originUserId"].Value<string>();
                    anchorInfoEntity.AnchorName = author["name"].Value<string>();
                    anchorInfoEntity.AnchorThump = author["avatar"].Value<string>();
                    anchorInfoEntity.RoomId = playInfo["liveStream"]["id"].Value<string>();
                    anchorInfoEntity.RoomTitle = author["description"].Value<string>();

                    // 封装直播流信息
                    List< DouYinAnchorInfoEntity.StreamInfo > streamInfos = new List<DouYinAnchorInfoEntity.StreamInfo> ();
                    foreach (JToken item in representation)
                    {
                        DouYinAnchorInfoEntity.StreamInfo streamInfo = new DouYinAnchorInfoEntity.StreamInfo();
                        streamInfo.StreaUrl = item["url"].Value<string>();
                        streamInfo.LiveSource = 1;
                        if ("STANDARD".Equals(item["qualityType"].Value<string>()))
                        {
                            // 标清(快手展示为高清)
                            streamInfo.Quality = 0;
                        }
                        else if ("HIGH".Equals(item["qualityType"].Value<string>()))
                        {
                            // 高清(快手展示为超清)
                            streamInfo.Quality = 1;
                        }
                        else if ("SUPER".Equals(item["qualityType"].Value<string>()))
                        {
                            // 超清(快手展示为蓝光)
                            streamInfo.Quality = 2;
                        }
                        else if ("BLUE_RAY".Equals(item["qualityType"].Value<string>()))
                        {
                            // 蓝光(快手展示为蓝光-至臻)
                            streamInfo.Quality = 3;
                        }
                        streamInfos.Add(streamInfo);
                    }
                    anchorInfoEntity.StreamInfos = streamInfos;


                    return anchorInfoEntity;
                }
                catch (Exception e)
                {
                    FileUtils.LogRecrd($"{jobject}", $"采集快手主播信息内容异常：AnchorInfo赋值失败==={webIdOrCustomId}");
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"获取快手主播是否在线发生异常==={webIdOrCustomId}");
            }

            return null;
        }

        /// <summary>
        /// 获取快手主播是否在线-异步
        /// </summary>
        /// <param name="webId">快手webId</param>
        /// <returns> 0：检测失败 1：在线 2：离线</returns>
        public static async Task<int> GetIsOnline(string webId)
        {
            KuaiShouRequestClient instance = KuaiShouRequestClient.Instance;
            return await instance.getOnlineStatusByWebIdOrCustomId(webId);
        }


        /// <summary>
        /// 添加快手主播-采集信息
        /// </summary>
        /// <param name="kuaishouId"></param>
        /// <returns></returns>
        public static async Task<AnchorInfo> getAddKuaiShouAnchorInfo(string kuaishouId)
        {
            try
            {
                KuaiShouRequestClient instance = KuaiShouRequestClient.Instance;
                string secUid = ""; // 快手原始id
                string anchorUserId = ""; // 快手网页id
                string anchorNumber = ""; // 快手自定义id

                bool isInteger = long.TryParse(kuaishouId, out long _);
                if (isInteger)
                {
                    // 纯数字，表示输入的是快手原始用户id
                    secUid = kuaishouId;
                    // 获取快手网页id
                    anchorUserId = await instance.getWebIdByOriginUserId(kuaishouId);
                }
                else
                {
                    // 非纯数字，用户输入的是快手网页id或用户自定义id
                    // 获取快手原始id
                    secUid = await instance.getOriginUserIdByWebIdOrCustomId(kuaishouId);
                    if(secUid == null)
                    {
                        return null;
                    }
                    // 判断当前是快手网页id还是用户自定义id
                    if(kuaishouId.StartsWith("3x"))
                    {
                        // 快手网页id
                        anchorUserId = kuaishouId;
                    }else
                    {
                        // 用户自定义id
                        anchorNumber = kuaishouId;
                        // 获取快手网页id
                        anchorUserId = await instance.getWebIdByOriginUserId(secUid);
                    }

                    //int status = await instance.judgeIsWebOrCustom(kuaishouId);
                    //if (status == 0)
                    //{
                    //    // 快手网页id
                    //    anchorUserId = kuaishouId;
                    //}
                    //else if (status == 1)
                    //{
                    //    // 用户自定义id
                    //    anchorNumber = kuaishouId;
                    //    // 获取快手网页id
                    //    anchorUserId = await instance.getWebIdByOriginUserId(secUid);
                    //}
                }

                FileUtils.LogRecrd($"原始id：{secUid}，网页id：{anchorUserId}，自定义id：{anchorNumber}", $"添加快手主播-采集到的快手号信息=={kuaishouId}");

                if (string.IsNullOrEmpty(secUid) || string.IsNullOrEmpty(anchorUserId))
                {
                    return null;
                }

                // 获取主播信息
                AnchorInfo anchorInfo = await getKuaiShouAnchorInfo(anchorUserId);
                if(anchorInfo != null)
                {
                    anchorInfo.SecUid = secUid;
                    anchorInfo.AnchorUserId = anchorUserId;
                    anchorInfo.anchorNumber = anchorNumber;

                    return anchorInfo;
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"添加快手主播-采集信息异常=={kuaishouId}");
            }

            return null;
        }

        /// <summary>
        /// 获取快手主播信息
        /// </summary>
        /// <param name="webIdOrCustomId">快手网页id或自定义id</param>
        /// <returns></returns>
        public static async Task<AnchorInfo> getKuaiShouAnchorInfo(string webIdOrCustomId)
        {
            try
            {
                KuaiShouGetStreamClient instance = KuaiShouGetStreamClient.Instance;
                JObject jobject = await instance.getAnchorLiveInfo(webIdOrCustomId);

                //JObject jobject = await getKuaishouHtmlJObject(webIdOrCustomId);
                if (jobject == null)
                {
                    FileUtils.LogRecrd($"{webIdOrCustomId}", $"采集快手主播信息内容异常：内容为空");
                    return null;
                }

                JArray playList = (JArray)jobject?["playList"];
                if(playList == null || playList.Count < 1)
                {
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(jobject)}", $"采集快手主播信息内容异常：playList为空==={webIdOrCustomId}");
                    return null;
                }

                JToken playInfo = playList[0];

                JToken author = playInfo["author"];
                if(author == null)
                {
                    FileUtils.LogRecrd($"{JsonConvert.SerializeObject(jobject)}", $"采集快手主播信息内容异常：author为空==={webIdOrCustomId}");
                    return null;
                }

                try
                {
                    AnchorInfo anchorInfo = new AnchorInfo();
                    anchorInfo.AnchorPlatform = "KuaiShouLive";
                    anchorInfo.AnchorName = author["name"].Value<string>();
                    anchorInfo.AnchorAvatar = author["avatar"].Value<string>();
                    anchorInfo.LiveUrl = "https://live.kuaishou.com/u/" + webIdOrCustomId;
                    anchorInfo.HomeUrl = "https://live.kuaishou.com/profile/" + webIdOrCustomId;
                    anchorInfo.platform = 1;

                    return anchorInfo;
                }
                catch(Exception e)
                {
                    FileUtils.LogRecrd($"{jobject}", $"采集快手主播信息内容异常：AnchorInfo赋值失败==={webIdOrCustomId}");
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"采集快手主播信息发生异常==={webIdOrCustomId}");
            }

            return null;
        }

        /// <summary>
        /// 采集快手主播和直播间信息
        /// </summary>
        /// <param name="anchorNumber">快手主播快手号</param>
        /// <returns></returns>
        private static async Task<JObject> getKuaishouHtmlJObject(string anchorNumber)
        {
            try
            {
                IBrowsingContext context = BrowsingContext.New(config);

                IDocument document = await context.OpenAsync($"https://live.kuaishou.com/u/{anchorNumber}");

                IHtmlCollection<IElement> collection = document.QuerySelectorAll("script");

                foreach (IElement element in collection)
                {
                    string js_str = element.InnerHtml;
                    js_str = js_str.Replace("undefined", "null");
                    js_str = js_str.Replace("\\u002F", "/");

                    if (js_str.Contains("window.__INITIAL_STATE__"))
                    {
                        try
                        {
                            int start_index = js_str.IndexOf("{");
                            int end_index = js_str.IndexOf(";");
                            js_str = js_str.Substring(start_index, end_index - start_index);

                            return JObject.Parse(js_str);
                        }
                        catch (Exception jsonEx)
                        {
                            // 记录JSON解析异常
                            FileUtils.LogRecrd($"{js_str}", $"采集快手主播信息JSON解析失败==={anchorNumber}");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"采集快手主播信息发生异常==={anchorNumber}");
            }

            return null;
        }

        /// <summary>
        /// 采集快手主播和直播间信息-同步
        /// </summary>
        /// <param name="anchorNumber">快手主播快手号</param>
        /// <returns></returns>
        private static JObject getKuaishouHtmlJObjectSync(string anchorNumber)
        {
            try
            {
                IBrowsingContext context = BrowsingContext.New(config);

                IDocument document = context.OpenAsync($"https://live.kuaishou.com/u/{anchorNumber}").Result;

                IHtmlCollection<IElement> collection = document.QuerySelectorAll("script");

                foreach (IElement element in collection)
                {
                    string js_str = element.InnerHtml;
                    js_str = js_str.Replace("undefined", "null");
                    js_str = js_str.Replace("\\u002F", "/");

                    if (js_str.Contains("window.__INITIAL_STATE__"))
                    {
                        try
                        {
                            int start_index = js_str.IndexOf("{");
                            int end_index = js_str.IndexOf(";");
                            js_str = js_str.Substring(start_index, end_index - start_index);

                            return JObject.Parse(js_str);
                        }
                        catch (Exception jsonEx)
                        {
                            // 记录JSON解析异常
                            FileUtils.LogRecrd($"{js_str}", $"采集快手主播信息JSON解析失败==={anchorNumber}");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"采集快手主播信息发生异常==={anchorNumber}");
            }

            return null;
        }
    }
}
