using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json.Serialization;
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using douyin.Utils;
using ReviewAnalysis.Model;
using System.Threading;
using ReviewAnalysis.Global;
using System.IO;
using System.Security.Cryptography;
using System.Diagnostics;
using ReviewAnalysis.vo;
using ReviewAnalysis.Bll;
using System.Net.Http.Headers;
using ReviewAnalysis.vo.trade;
using System.Runtime.Remoting.Contexts;
using ReviewAnalysis.Utils;
using Swan.Parsers;
using CefSharp.DevTools.IO;
using ReviewAnalysis.Bll.Anchor;


namespace ReviewAnalysis.Asr
{
    public class ReplayHttpUtils
    {
        public static string BaseUrl = Constant.GetApiBaseUrl();
        public static string GovernanceBaseUrl = Constant.GetGovernanceApiBaseUrl();
        //public static string BaseUrl = Constant.BaseUrl;
        //public static string BaseUrl = Constant.BaseTestUrl;
        public static string Token = "";

        /// <summary>
        /// 客户端版本
        /// 纯录制版：record
        /// 复盘版：replay
        /// </summary>
        private static string _userClientVersion;
        public static string UserClientVersion
        {
            get
            {
                if (!string.IsNullOrEmpty(Token) && !string.IsNullOrEmpty(_userClientVersion))
                {
                    return _userClientVersion;
                }
                return Constant.CLIENT_VERSION;
            }
            set => _userClientVersion = value;
        }

        /// <summary>
        /// 当前登录的用户id
        /// </summary>
        public static string UserId = "";
        /// <summary>
        /// 当前登录用户的租户id
        /// </summary>
        public static long ActiveTenantId = 0;
        public static UserVo UserInfo = new UserVo();
        public static VersionUpdateVo VersionVo;
        public static VersionUpdateVo VersionUpdateVo;
        public static VersionUpdateVo VersionPatchVo;
        public static bool GetUserFlag = true;
        public static bool IsUpdateThread = false;
        public static string uuid;
        public static string cpuid;
        public static int developmentMode = 1; // 开发模式 0开发，1发布
        public static bool mainUpdateDownload = false; // 当前是否在下载中
        public static bool patchUpdateDownload = false; // 当前是否在下载补丁包
        public static bool isOpenUpdate = false; // 是否有打开过更新弹窗，每次打开软件都会打开一次

        /// <summary>
        /// 将在线的主播信息同步到服务器
        /// </summary>
        /// <param name="anchor"></param>
        public static void SendAnchorToServer(AnchorInfo anchor)
        {

            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/anchorurl/save");
            Dictionary<string, string> postContent = new Dictionary<string, string>();
            postContent.Add("secUid", anchor.SecUid);
            postContent.Add("homeUrl", anchor.HomeUrl);
            postContent.Add("liveUrl", anchor.LiveUrl);
            postContent.Add("anchorName", anchor.AnchorName);
            postContent.Add("anchorAvatar", anchor.AnchorAvatar);
            string body = JsonConvert.SerializeObject(postContent);
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");
            using (HttpClient client = new HttpClient())
            {
                HttpResponseMessage result = client.SendAsync(request).Result;
            }
            
        }

        /// 添加或修改主播信息到服务器
        /// </summary>
        /// <param name="audioaAlysisList"></param>
        public static void SaveOrUpdateAnchor(AnchorInfo anchorInfo)
        {

            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", anchorInfo.SecUid);
            dictionary.Add("homeUrl", anchorInfo.HomeUrl);
            dictionary.Add("liveUrl", anchorInfo.LiveUrl);
            dictionary.Add("anchorName", anchorInfo.AnchorName);
            dictionary.Add("anchorAvatar", anchorInfo.AnchorAvatar);
            dictionary.Add("platform", anchorInfo.platform);
            dictionary.Add("platformResource", anchorInfo.AnchorPlatform);
            dictionary.Add("anchorUserId", anchorInfo.AnchorUserId);
            dictionary.Add("webSocketId", anchorInfo.WebSocketId);

            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/anchorurl/saveOrUpdateAnchor");
            string body = JsonConvert.SerializeObject(dictionary);
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            

        }

        /// <summary>        /// 同步视频分析数据到服务器
        /// </summary>
        /// <param name="audioaAlyses">分析数据</param>
        /// <param name="videoId">视频id</param>
        /// <param name="tradeId">行业id</param>
        public static void SyncVideoAnalysisToServer(List<AudioaAlysis> audioaAlyses, string videoId, string tradeId)
        {
            if (audioaAlyses != null && audioaAlyses.Count > 0)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/AnchorVideo/syncVideoAnalysisToServer");

                List<SentenceMarkVo> sentenceMarkVos = new List<SentenceMarkVo>();
                foreach (var item in audioaAlyses)
                {
                    sentenceMarkVos.Add(JsonConvert.DeserializeObject<SentenceMarkVo>(item.DataJson));
                }

                SyncVideoAnalysisBo syncVideoAnalysisBo = new SyncVideoAnalysisBo();
                syncVideoAnalysisBo.VideoId = videoId;
                syncVideoAnalysisBo.TradeId = tradeId;
                syncVideoAnalysisBo.SentenceMarkVoList = sentenceMarkVos;

                string body = JsonConvert.SerializeObject(syncVideoAnalysisBo);
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");
                using(HttpClient Client = new HttpClient())
                {
                    HttpResponseMessage result = Client.SendAsync(request).Result;
                }
            }
        }

        /// <summary>
        /// 判断服务器是否有视频的分析数据
        /// </summary>
        /// <returns></returns>
        public static bool CheckVideoAnalysisExist(AnchorVideo anchorVideo)
        {
            try
            {
                var urlBuilder = new UriBuilder(BaseUrl + "/AnchorVideo/checkVideoAnalysisExist")
                {
                    Query = $"videoId={anchorVideo.VideoId}&tradeId={anchorVideo.TradeId}"
                };

                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                {
                    Headers =
                    {
                        { "token", Token }, { "webVersion", Constant.VERSION }
                    }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.log($"获取服务器时间失败： {response}.");
                        throw new Exception("网络不佳，请稍后重试");
                    }

                    var responseBody = response.Content.ReadAsStringAsync().Result;

                    if (!string.IsNullOrEmpty(responseBody))
                    {
                        JObject jsonObject = JObject.Parse(responseBody);

                        return (bool)jsonObject["data"];
                    }
                }

            }
            catch (Exception ex)
            {
                FileUtils.log(ex.ToString());
            }


            throw new Exception("网络不佳，请稍后重试");
        }

        /// <summary>
        /// 获取服务器时间
        /// </summary>
        /// <returns></returns>
        public static long GetReplayServerTime()
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/openapi/v1930/currentTime")
                {
                    Headers =
                    {
                        { "token", Token }, { "webVersion", Constant.VERSION }
                    }
                };

                HttpClient Client = new HttpClient();
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.LogError($"获取服务器时间失败： {response}.");
                    return 0;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;

                if (!string.IsNullOrEmpty(responseBody))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(responseBody.ToString());
                    if(res.code == 0)
                    {
                        return long.TryParse(res.data.ToString(), out long val) ? val : 0;
                    }
                    else
                    {
                        FileUtils.LogError($"获取服务器时间返回错误： {responseBody.ToString()}.");
                        return 0;
                    }
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"获取服务器时间报错： {ex.ToString()}.");
            }


            return 0;
        }


        /// <summary>
        /// 清空服务器文件的分析数据
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns></returns>
        public static void ClearFileAnalysis(string fileId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/UploadFile/clearAnalysis")
            {
                Query = $"fileId={fileId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 清空服务器视频的分析数据
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns></returns>
        public static void ClearVideoAnalysis(string videoId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/AnchorVideo/clearAnalysis")
            {
                Query = $"videoId={videoId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }


        /// <summary>
        /// 修改上传的文件到服务器
        /// </summary>
        /// <param name="uploadFile"></param>
        public static void UpdateFileToServer(UploadFile uploadFile)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/UploadFile/updateUploadFile");
            request.Content = new StringContent(JsonConvert.SerializeObject(uploadFile), Encoding.UTF8, "application/json");
            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }

        /// <summary>
        /// 保存上传的文件到服务器
        /// </summary>
        /// <param name="uploadFile"></param>
        public static void SaveFileToServer(UploadFile uploadFile)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/UploadFile/saveUploadFile");
            request.Content = new StringContent(JsonConvert.SerializeObject(uploadFile), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }

        /// <summary>
        /// 同步文件分析记录到服务器
        /// </summary>
        /// <param name="uploadFileAlyses"></param>
        public static void SaveFileAnalysisToServer(List<UploadFileAlysis> uploadFileAlyses)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/UploadFile/saveFileAnalysis");
            string body = JsonConvert.SerializeObject(uploadFileAlyses);
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 同步删除服务器的文件
        /// </summary>
        /// <param name="fileIds"></param>
        public static void DelServerFile(List<string> fileIds)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/UploadFile/removeByFileId");
            string body = JsonConvert.SerializeObject(fileIds);
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }

        //同步场观数据
        public static void GetChangGuan()
        {
            try
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Get;
                request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(Constant.GetApiBaseUrl() + $"/openapi/v1930/synchronizeTwoDayVideo");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;
                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        if (result != null)
                        {
                            var res = JsonConvert.DeserializeObject<dynamic>(result);

                            // 视频维度
                            if (res.code == 0 && res.data != null && res?.data?.socketCollectMessageList != null)
                            {
                                foreach (var item in res.data.socketCollectMessageList)
                                {
                                    //保存场观
                                    VideoViewershipNumBll videoViewershipNumBll = new VideoViewershipNumBll();
                                    string secUid = item.secUid?.ToString() ?? string.Empty;
                                    string batchNumber = item.batchNumber?.ToString() ?? string.Empty;
                                    string observationNum = item.observationNum?.ToString() ?? string.Empty;
                                    string totalBarrageNum = item.totalBarrageNum?.ToString() ?? string.Empty;
                                    string videoId = item.videoId?.ToString() ?? string.Empty;
                                    string startDate = item.startDate?.ToString() ?? string.Empty;
                                    string endDate = item.endDate?.ToString() ?? string.Empty;
                                    videoViewershipNumBll.SaveOrUpdate(secUid, batchNumber, observationNum, totalBarrageNum, videoId, startDate, endDate);
                                }
                            }

                            // 场次维度
                            if (res.code == 0 && res.data != null && res?.data?.totalSocketMessageList != null)
                            {
                                foreach (var item in res.data.totalSocketMessageList)
                                {
                                    // 保存累计观看人数
                                    TotalOnlineNumBll totalOnlineNumBll = new TotalOnlineNumBll();
                                    string secUid = item.secUid?.ToString() ?? string.Empty;
                                    string batchNumber = item.batchNumber?.ToString() ?? string.Empty;
                                    string totalOnlineNum = item.totalOnlineNum?.ToString() ?? string.Empty;
                                    string videoId = item.videoId?.ToString() ?? string.Empty;
                                    string startDate = item.startDate?.ToString() ?? string.Empty;
                                    string endDate = item.endDate?.ToString() ?? string.Empty;
                                    totalOnlineNumBll.SaveOrUpdate(secUid, batchNumber, totalOnlineNum, videoId, startDate, endDate);
                                }
                            }


                        }
                    }
                }
                
            }
            catch (Exception ex)
            {

            }
        }
        /// <summary>
        /// 获取用户在服务器上的文件分析列表
        /// </summary>
        /// <param name="fileId">文件唯一标识id</param>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static List<UploadFileAlysis> GetFileAnalysisList(string fileId, string tradeId)
        {
            // 构造请求体参数
            JObject bodyJsonObject = new JObject();
            bodyJsonObject["fileId"] = fileId;
            bodyJsonObject["tradeId"] = tradeId;

            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/UploadFile/seletContent");
            request.Content = new StringContent(bodyJsonObject.ToString(), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取用户在服务器上的文件分析列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                if (!string.IsNullOrEmpty(responseBody))
                {
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    // 配置序列化忽略大小写
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                    };
                    if (jsonObject.data != null)
                    {
                        List<UploadFileAlysis> uploadFileAlyses = JsonConvert.DeserializeObject<List<UploadFileAlysis>>(jsonObject.data.ToString(), settings);

                        return uploadFileAlyses;
                    }
                }
            }
            

            return null;
        }

        /// <summary>
        /// 获取用户在服务器上的文件列表
        /// </summary>
        /// <returns></returns>
        public static List<UploadFile> GetFileList()
        {
            var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/UploadFile/seletList")
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取用户在服务器上的文件列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                // 配置序列化忽略大小写
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                if (jsonObject.data != null)
                {
                    List<UploadFile> uploadFiles = JsonConvert.DeserializeObject<List<UploadFile>>(jsonObject.data.ToString(), settings);

                    return uploadFiles;
                }
            }
            

            return null;
        }

        /// <summary>
        /// 同步删除服务器的视频
        /// </summary>
        /// <param name="videoIds"></param>
        public static void DelServerVideo(List<string> videoIds)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/AnchorVideo/removeByVoidId");
            string body = JsonConvert.SerializeObject(videoIds);
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 获取用户在服务器上的视频分析列表
        /// </summary>
        /// <param name="videoId">视频唯一标识id</param>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static List<AudioaAlysis> GetVideoAnalysisList(string videoId, string tradeId)
        {
            // 构造请求体参数
            JObject bodyJsonObject = new JObject();
            bodyJsonObject["videoId"] = videoId;
            bodyJsonObject["tradeId"] = tradeId;

            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/AnchorVideo/selectByVideoIdOrTradeId");
            request.Content = new StringContent(bodyJsonObject.ToString(), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取用户在服务器上的文件分析列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                if (!string.IsNullOrEmpty(responseBody))
                {
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    // 配置序列化忽略大小写
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                    };
                    if (jsonObject.data != null)
                    {
                        List<AudioaAlysis> audioaAlyses = JsonConvert.DeserializeObject<List<AudioaAlysis>>(jsonObject.data.ToString(), settings);

                        return audioaAlyses;
                    }
                }
            }
           

            return null;
        }

        /// <summary>
        /// 同步视频分析记录到服务器
        /// </summary>
        /// <param name="audioaAlysisList"></param>
        public static void SaveVideoAnalysisToServer(List<AudioaAlysis> audioaAlysisList)
        {
            if (audioaAlysisList != null && audioaAlysisList.Count > 0)
            {
                List<Dictionary<string, object>> list = new List<Dictionary<string, object>>();
                foreach (var item in audioaAlysisList)
                {
                    Dictionary<string, object> dictionary = new Dictionary<string, object>();
                    dictionary.Add("id", item.Id);
                    dictionary.Add("videoId", item.VideoId);
                    dictionary.Add("paragraph", item.Paragraph);
                    dictionary.Add("status", item.Status);
                    dictionary.Add("dataJson", item.DataJson);
                    dictionary.Add("tradeId", item.TradeId);
                    list.Add(dictionary);
                }

                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/AnchorVideo/saveAnchorVideoRecod");
                string body = JsonConvert.SerializeObject(list);
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    HttpResponseMessage result = Client.SendAsync(request).Result;
                }
               
            }

        }

        /// <summary>
        /// 修改视频到服务器
        /// </summary>
        /// <param name="anchorVideo"></param>
        public static void UpdateVideoToServer(AnchorVideo anchorVideo)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/AnchorVideo/UpdateVideo");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("id", anchorVideo.Id);
            dictionary.Add("videoId", anchorVideo.VideoId);
            dictionary.Add("videoName", anchorVideo.VideoName);
            dictionary.Add("startTime", anchorVideo.StartTime);
            dictionary.Add("paragraph", anchorVideo.Paragraph);
            dictionary.Add("subsectionType", anchorVideo.SubsectionType);
            dictionary.Add("duration", anchorVideo.Duration);
            dictionary.Add("vedioSizie", anchorVideo.VedioSizie);
            dictionary.Add("shareUrl", anchorVideo.ShareUrl);
            dictionary.Add("playUrl", anchorVideo.PlayUrl);
            dictionary.Add("endTime", anchorVideo.EndTime);
            dictionary.Add("batchNumber", anchorVideo.BatchNumber);
            dictionary.Add("videoType", anchorVideo.VideoType);
            dictionary.Add("definition", anchorVideo.Definition);
            dictionary.Add("storagePath", anchorVideo.StoragePath);
            dictionary.Add("sourceType", anchorVideo.SourceType);
            dictionary.Add("sourceUrl", anchorVideo.SourceUrl);
            dictionary.Add("anchorId", anchorVideo.AnchorId);
            dictionary.Add("liveTitle", anchorVideo.LiveTitle);
            dictionary.Add("isRecording", anchorVideo.IsRecording);
            dictionary.Add("analysisTime", anchorVideo.AnalysisTime);
            dictionary.Add("analysisStatus", anchorVideo.AnalysisStatus);
            dictionary.Add("errorReason", anchorVideo.ErrorReason);
            dictionary.Add("secUid", anchorVideo.SecUid);
            dictionary.Add("tradeId", anchorVideo.TradeId);
            dictionary.Add("platformType", anchorVideo.PlatformType);
            dictionary.Add("uploadStatus", anchorVideo.UploadStatus);
            dictionary.Add("isMark", anchorVideo.IsMark);
            dictionary.Add("cloudStore", anchorVideo.CloudStore);
            //dictionary.Add("deleteStatus", anchorVideo.DeleteStatus);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           

        }

        /// <summary>
        /// 保存视频到服务器
        /// </summary>
        /// <param name="anchorVideo"></param>
        public static void SaveVideoToServer(AnchorVideo anchorVideo)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/AnchorVideo/saveVideoinfo");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("id", anchorVideo.Id);
            dictionary.Add("videoId", anchorVideo.VideoId);
            dictionary.Add("videoName", anchorVideo.VideoName);
            dictionary.Add("startTime", anchorVideo.StartTime);
            dictionary.Add("paragraph", anchorVideo.Paragraph);
            dictionary.Add("subsectionType", anchorVideo.SubsectionType);
            dictionary.Add("duration", anchorVideo.Duration);
            dictionary.Add("vedioSizie", anchorVideo.VedioSizie);
            dictionary.Add("endTime", anchorVideo.EndTime);
            dictionary.Add("batchNumber", anchorVideo.BatchNumber);
            dictionary.Add("videoType", anchorVideo.VideoType);
            dictionary.Add("definition", anchorVideo.Definition);
            dictionary.Add("storagePath", anchorVideo.StoragePath);
            dictionary.Add("sourceType", anchorVideo.SourceType);
            dictionary.Add("sourceUrl", anchorVideo.SourceUrl);
            dictionary.Add("anchorId", anchorVideo.AnchorId);
            dictionary.Add("liveTitle", anchorVideo.LiveTitle);
            dictionary.Add("isRecording", anchorVideo.IsRecording);
            dictionary.Add("analysisTime", anchorVideo.AnalysisTime);
            dictionary.Add("analysisStatus", anchorVideo.AnalysisStatus);
            dictionary.Add("errorReason", anchorVideo.ErrorReason);
            dictionary.Add("secUid", anchorVideo.SecUid);
            dictionary.Add("tradeId", anchorVideo.TradeId);
            dictionary.Add("platformType", anchorVideo.PlatformType);
            dictionary.Add("uploadStatus", anchorVideo.UploadStatus);
            dictionary.Add("isMark", anchorVideo.IsMark);
            dictionary.Add("cloudStore", anchorVideo.CloudStore);
            //dictionary.Add("deleteStatus", anchorVideo.DeleteStatus);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 获取用户在服务器上的视频列表
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <returns></returns>
        public static List<AnchorVideo> GetVideoList()
        {
            var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/AnchorVideo/selectByuserId")
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取用户在服务器上的视频列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                // 配置序列化忽略大小写
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                if (jsonObject.data != null)
                {
                    List<AnchorVideo> anchorVideos = JsonConvert.DeserializeObject<List<AnchorVideo>>(jsonObject.data.ToString(), settings);

                    return anchorVideos;
                }
            }
            

            return null;
        }

        /// <summary>
        /// 根据租户id获取用户在服务器上的视频列表
        /// </summary>
        /// <returns></returns>
        public static List<AnchorVideoInfoVo> GetVideoListByTenantId()
        {
            var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/AnchorVideo/getVideoListByTenantId")
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"根据租户id获取用户在服务器上的视频列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                // 配置序列化忽略大小写
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                if (jsonObject.data != null)
                {
                    List<AnchorVideoInfoVo> anchorVideos = JsonConvert.DeserializeObject<List<AnchorVideoInfoVo>>(jsonObject.data.ToString(), settings);

                    return anchorVideos;
                }
            }
            

            return null;
        }

        /// <summary>
        /// 删除用户和主播的关联
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public static void DelUserAnchor(string token, string secUid)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/anchorurl/deletBysecuid")
            {
                Query = $"secUid={secUid}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }

        /// <summary>
        /// 修改用户跟主播的关联
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <param name="tradeId">主播行业id</param>
        /// <param name="isAutoRecord">在线时,是否自动录制分析</param>
        /// <param name="isRemoveRecord">是否从录制列表移除了 0：否 1：是</param>
        /// <returns></returns>
        public static void UpdateUserAnchor(string secUid, string tradeId, int isAutoRecord, int isRemoveRecord)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/anchorurl/updateUserAnchor");

            Dictionary<string, string> dictionary = new Dictionary<string, string>();
            dictionary.Add("anchorUrlSecUid", secUid);
            if (!string.IsNullOrEmpty(tradeId))
            {
                dictionary.Add("tradeId", tradeId);
            }
            if (isAutoRecord != -1)
            {
                dictionary.Add("isAutoRecord", isAutoRecord + "");
            }
            if (isRemoveRecord != -1)
            {
                dictionary.Add("isRemoveRecord", isRemoveRecord + "");
            }


            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 添加用户跟主播的关联
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <param name="secUidList">主播secUid列表</param>
        /// <returns></returns>
        public static void SaveUserAnchor(string token, List<string> secUidList, string tradeId)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/anchorurl/saveBatchs");

            List<Dictionary<string, string>> requestList = new List<Dictionary<string, string>>();
            foreach (var item in secUidList)
            {
                Dictionary<string, string> dictionary = new Dictionary<string, string>();
                dictionary.Add("anchorUrlSecUid", item);
                dictionary.Add("tradeId", tradeId);
                requestList.Add(dictionary);
            }

            request.Content = new StringContent(JsonConvert.SerializeObject(requestList), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 绑定用户跟主播的关联
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <param name="tradeId">行业id</param>
        /// <returns></returns>
        public static void BindUserAnchor(string secUid, string tradeId)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/anchorurl/bindUserAnchor");

            Dictionary<string, string> requestData = new Dictionary<string, string>();
            requestData.Add("secUid", secUid);
            requestData.Add("tradeId", tradeId);

            request.Content = new StringContent(JsonConvert.SerializeObject(requestData), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 获取用户在服务器上的主播列表
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <returns></returns>
        public static List<AnchorUrlEntity> GetAnchorList(string token)
        {

            string responseBody = HttpUtils.SendGet(ReplayHttpUtils.BaseUrl + "/anchorurl/listByUserToken", null);

            var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

            // 配置序列化忽略大小写
            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
            };
            if (jsonObject?.data != null)
            {
                List<AnchorUrlEntity> anchorUrlEntities = JsonConvert.DeserializeObject<List<AnchorUrlEntity>>(jsonObject.data.ToString(), settings);

                return anchorUrlEntities;
            }

            return null;
        }



        /// <summary>
        /// 询问是否还有QPS余量
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <returns></returns>
        public static CheckSurplusEntity CheckSurplus(string token)
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/audio/checkSurplus")
                {
                    Headers =
                {
                    { "token", token }, { "webVersion", Constant.VERSION }
                }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.log($"询问是否还有QPS余量请求失败： {response}.");
                        return null;
                    }

                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    if (jsonObject.data != null)
                    {
                        CheckSurplusEntity checkSurplusEntity = JsonConvert.DeserializeObject<CheckSurplusEntity>(jsonObject.data.ToString());
                        if (checkSurplusEntity != null)
                        {
                            checkSurplusEntity.Code = jsonObject.code;
                            checkSurplusEntity.Msg = jsonObject.msg;

                            return checkSurplusEntity;
                        }
                    }
                }
                

                return null;
            }
            catch (Exception ex) {

                FileUtils.log($"询问是否还有QPS余量请求报错： {ex.Message}.");
                return null;
            }

        }

        /// <summary>
        /// 记录QPS访问
        /// </summary>
        /// <returns></returns>
        public static void RecordQPS()
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/audio/record")
                {
                    Headers =
                    {
                        { "token", Token }, { "webVersion", Constant.VERSION }
                    }
                };

                using(HttpClient Client = new HttpClient())
                {
                    HttpResponseMessage result = Client.SendAsync(request).Result;
                }
                

            }
            catch (Exception ex)
            {

                FileUtils.log($"记录QPS访问报错： {ex.Message}.");
            }

        }

        /// <summary>
        /// 通知增加QPS余量
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <param name="qpsId">QPS ID</param>
        /// <param name="secretId">密钥ID</param>
        public static void AddSurplus(string token, long qpsId, string secretId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/audio/addSurplus")
            {
                Query = $"id={qpsId}&secretId={secretId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"通知增加QPS余量请求失败： {response}.");
                }
            }
            
        }

        /// <summary>
        /// 获取语音识别接口临时调用凭证
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <param name="secretId">密钥ID</param>
        /// <returns></returns>
        public static AudioTempTokenEntity GetTempToken(string token, string secretId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/audio/getTempToken")
            {
                Query = $"secretId={secretId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"询问是否还有QPS余量请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                return JsonConvert.DeserializeObject<AudioTempTokenEntity>(jsonObject.data.ToString());
            }
            
        }

        /// <summary>
        /// 获取腾讯云点播临时调用凭证
        /// </summary>
        /// <returns></returns>
        public static AudioTempTokenEntity GetVodTempToken()
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/vod/getVodTempToken");

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取腾讯云点播临时调用凭证请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                return JsonConvert.DeserializeObject<AudioTempTokenEntity>(jsonObject.data.ToString());
            }
            
        }

        /// <summary>
        /// 关键词/敏感词识别
        /// </summary>
        /// <param name="token">向服务器请求token</param>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">视频id</param>
        /// <param name="isLastParagraph">是否是最后一段 0：否 1：是</param>
        /// <param name="asrResultEntity">腾讯识别返回的数据对象</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static async Task<AnalysisResultVo> WordsMark(string platform, string videoId, List<ASRResultEntity> asrResultList, string tradeId, int type)
        {
            List<JObject> bodyJsonList = new List<JObject>();
            foreach (var asrResultEntity in asrResultList)
            {
                // 构造请求体参数
                JObject bodyJsonObject = new JObject();
                bodyJsonObject["platformType"] = 1;

                bodyJsonObject["videoId"] = videoId;
                bodyJsonObject["tradeId"] = tradeId;
                if (string.IsNullOrEmpty(asrResultEntity.Result))
                {
                    asrResultEntity.Result = "-";
                }
                bodyJsonObject["content"] = asrResultEntity.Result;
                bodyJsonObject["currentSort"] = asrResultEntity.Paragraph;
                bodyJsonObject["isLast"] = asrResultEntity.Paragraph == asrResultList.Count ? 1 : 0;
                bodyJsonObject["type"] = type;
                JArray wordListArray = new JArray();
                if (asrResultEntity.WordList != null && asrResultEntity.WordList.Count > 0)
                {
                    foreach (var item in asrResultEntity.WordList)
                    {
                        wordListArray.Add(JToken.FromObject(item));
                    }
                } else
                {
                    ASRWordEntity wordEntity = new ASRWordEntity();
                    wordEntity.Word = "-";
                    wordEntity.StartTime = 0;
                    wordEntity.EndTime = 20;
                    wordListArray.Add(JToken.FromObject(wordEntity));
                    bodyJsonObject["content"] = asrResultEntity.Result;
                }

                bodyJsonObject["items"] = wordListArray;

                bodyJsonList.Add(bodyJsonObject);
            }

            string body = JsonConvert.SerializeObject(bodyJsonList);

            // 重试3次机会
            for (int i = 0; i < 3; i++)
            {
                try
                {
                    var request = new HttpRequestMessage();
                    request.Method = HttpMethod.Post;
                    request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                    request.RequestUri = new Uri(BaseUrl + "/openapi/v2000/wordsMark");
                    request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                    using (HttpClient Client = new HttpClient())
                    {
                        Client.Timeout = TimeSpan.FromSeconds(120);
                        var response = await Client.SendAsync(request);
                        FileUtils.LogAnalysis($"服务器关键词/敏感词识别网络状态：{response}");
                        if (response.IsSuccessStatusCode)
                        {
                            string result = await response.Content.ReadAsStringAsync();
                            FileUtils.log($"服务器关键词/敏感词识别结果：{result}");
                            var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                            if (jsonObject.code == 0)
                            {
                                string str = jsonObject.data.ToString();
                                var settings = new JsonSerializerSettings
                                {
                                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                                };
                                try
                                {
                                    return JsonConvert.DeserializeObject<AnalysisResultVo>(str, settings);
                                }
                                catch (Exception ex)
                                {
                                    FileUtils.log(ex.Message);
                                }

                            }
                        }
                        else
                        {
                            FileUtils.LogAnalysis($"第{i}次", $"服务器关键词/敏感词识别接口请求失败，将尝试重新发送");
                            Thread.Sleep(30000);
                        }
                    }
                }
                catch(Exception e)
                {
                    FileUtils.LogAnalysis($"{e}", $"服务器关键词/敏感词识别接口异常");
                }
                
            }

            return null;

        }

        /// <summary>
        /// 重新选行业分析
        /// </summary>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">视频id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static AnalysisResultVo WordsMarkSync(string platform, string videoId, string tradeId, int type)
        {
            // 构造请求体参数
            JObject bodyJson = new JObject();
            bodyJson["videoId"] = videoId;
            bodyJson["tradeId"] = tradeId;
            bodyJson["type"] = type;
            if (platform.Contains("DouYin"))
            {
                // 抖音
                bodyJson["platformType"] = 1;
            }
            else if (platform.Contains("Kuai"))
            {
                // 快手
                bodyJson["platformType"] = 2;
            }
            else if (platform.Contains("ShiP"))
            {
                // 视频号
                bodyJson["platformType"] = 3;
            }
            else
            {
                // 全平台
                bodyJson["platformType"] = platform;
            }

            string body = JsonConvert.SerializeObject(bodyJson);


            // 重试3次机会
            for (int i = 0; i < 3; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/openapi/v2000/wordsMarkReAnalysis");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;
                    FileUtils.LogAnalysis($"服务器关键词/敏感词识别网络状态：{response}");
                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        FileUtils.log($"服务器关键词/敏感词识别结果：{result}");
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            string str = jsonObject.data.ToString();
                            var settings = new JsonSerializerSettings
                            {
                                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                            };
                            try
                            {
                                return JsonConvert.DeserializeObject<AnalysisResultVo>(str, settings);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.log(ex.Message);
                            }
                        }
                    }
                    else
                    {
                        Thread.Sleep(30000);
                    }
                    FileUtils.LogAnalysis($"服务器关键词/敏感词识别WordsMark接口请求失败：request：{request},response：{response}");
                }
                
            }

            return null;

        }


        /// <summary>
        /// 关键词/敏感词识别-同步接口
        /// </summary>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">视频id</param>
        /// <param name="asrResultList">腾讯识别返回的数据列表</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="isReAnalysis">是否是二次重新分析 0：否 1：是</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static List<SentenceMarkVo> WordsMarkSync(string platform, string videoId, List<ASRResultEntity> asrResultList, string tradeId, int isReAnalysis, int type)
        {
            // 构造请求体参数
            List<JObject> bodyJsonList = new List<JObject>();
            foreach (var asrResultEntity in asrResultList)
            {
                // 构造请求体参数
                JObject bodyJsonObject = new JObject();
                if (platform.Contains("DouYin"))
                {
                    // 抖音
                    bodyJsonObject["platformType"] = 1;
                }
                else if (platform.Contains("Kuai"))
                {
                    // 快手
                    bodyJsonObject["platformType"] = 2;
                }
                else if (platform.Contains("ShiP"))
                {
                    // 视频号
                    bodyJsonObject["platformType"] = 3;
                }
                else
                {
                    // 全平台
                    bodyJsonObject["platformType"] = platform;
                }

                bodyJsonObject["videoId"] = videoId;
                bodyJsonObject["tradeId"] = tradeId;
                bodyJsonObject["isReAnalysis"] = isReAnalysis;
                bodyJsonObject["content"] = asrResultEntity.Result;
                bodyJsonObject["currentSort"] = asrResultEntity.Paragraph;
                bodyJsonObject["isLast"] = asrResultEntity.Paragraph == asrResultList.Count ? 1 : 0;
                bodyJsonObject["type"] = type;
                JArray wordListArray = new JArray();
                if (asrResultEntity.WordList != null && asrResultEntity.WordList.Count > 0)
                {
                    foreach (var item in asrResultEntity.WordList)
                    {
                        wordListArray.Add(JToken.FromObject(item));
                    }
                }
                else
                {
                    ASRWordEntity wordEntity = new ASRWordEntity();
                    wordEntity.Word = "-";
                    wordEntity.StartTime = 0;
                    wordEntity.EndTime = 20;
                    wordListArray.Add(JToken.FromObject(wordEntity));
                }

                bodyJsonObject["items"] = wordListArray;

                bodyJsonList.Add(bodyJsonObject);
            }

            string body = JsonConvert.SerializeObject(bodyJsonList);


            // 重试3次机会
            for (int i = 0; i < 3; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/sensitivewords/wordsMark");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;
                    FileUtils.LogAnalysis($"服务器关键词/敏感词识别网络状态：{response}");
                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        FileUtils.log($"服务器关键词/敏感词识别结果：{result}");
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            string str = jsonObject.data.ToString();
                            return JsonConvert.DeserializeObject<List<SentenceMarkVo>>(str);
                        }
                    }
                    else
                    {
                        Thread.Sleep(3000);
                        FileUtils.LogAnalysis($"服务器关键词/敏感词识别WordsMark接口请求失败：request：{request},response：{response}");
                    }
                }
                
            }

            return null;

        }

        /// <summary>
        /// 文本内容关键词/敏感词识别
        /// </summary>
        /// <param name="token">向服务器请求token</param>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">文件id</param>
        /// <param name="content">文本内容</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static async Task<AnalysisResultVo> WordsMarkByText(string token, string platform, string fileId, string content, string tradeId, int type)
        {
            // 构造请求体参数
            JObject bodyJsonObject = new JObject();
            if (platform.Contains("DouYin"))
            {
                // 抖音
                bodyJsonObject["platformType"] = 1;
            }
            else if (platform.Contains("Kuai"))
            {
                // 快手
                bodyJsonObject["platformType"] = 2;
            }
            else if (platform.Contains("ShiP"))
            {
                // 视频号
                bodyJsonObject["platformType"] = 3;
            }
            else
            {
                // 全平台
                bodyJsonObject["platformType"] = platform;
            }

            bodyJsonObject["videoId"] = fileId;
            bodyJsonObject["tradeId"] = tradeId;
            bodyJsonObject["content"] = content;
            bodyJsonObject["currentSort"] = 1;
            bodyJsonObject["isLast"] = 1;
            bodyJsonObject["type"] = type;
            string body = bodyJsonObject.ToString();

            // 重试3次机会
            for (int i = 0; i < 3; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/openapi/v2000/wordsMarkByText");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = await Client.SendAsync(request);
                    if (response.IsSuccessStatusCode)
                    {
                        string result = await response.Content.ReadAsStringAsync();
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            string str = jsonObject.data.ToString();
                            var settings = new JsonSerializerSettings
                            {
                                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                            };
                            try
                            {
                                return JsonConvert.DeserializeObject<AnalysisResultVo>(str, settings);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.log(ex.Message);
                            }
                        }
                    }
                    else
                    {
                        Thread.Sleep(30000);
                    }
                }
                

            }

            return null;

        }

        /// <summary>
        /// 文本内容关键词/敏感词识别-同步接口
        /// </summary>
        /// <param name="token">向服务器请求token</param>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">文件id</param>
        /// <param name="content">文本内容</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static SentenceMarkVo WordsMarkByTextSync(string token, string platform, string fileId, string content, string tradeId, int type)
        {
            // 构造请求体参数
            JObject bodyJsonObject = new JObject();
            if (platform.Contains("DouYin"))
            {
                // 抖音
                bodyJsonObject["platformType"] = 1;
            }
            else if (platform.Contains("Kuai"))
            {
                // 快手
                bodyJsonObject["platformType"] = 2;
            }
            else if (platform.Contains("ShiP"))
            {
                // 视频号
                bodyJsonObject["platformType"] = 3;
            }
            else
            {
                // 全平台
                bodyJsonObject["platformType"] = platform;
            }

            bodyJsonObject["videoId"] = fileId;
            bodyJsonObject["tradeId"] = tradeId;
            bodyJsonObject["content"] = content;
            bodyJsonObject["currentSort"] = 1;
            bodyJsonObject["isLast"] = 1;
            bodyJsonObject["type"] = type;
            string body = bodyJsonObject.ToString();

            // 重试3次机会
            for (int i = 0; i < 3; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/sensitivewords/wordsMarkByText");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;
                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            string str = jsonObject.data.ToString();
                            return JsonConvert.DeserializeObject<SentenceMarkVo>(str);
                        }
                    }
                    else
                    {
                        Thread.Sleep(3000);
                        FileUtils.log("WordsMarkByText接口请求失败，" + response + request);
                    }
                }
                
            }

            return null;

        }

        /// <summary>
        /// 从服务器获取对比数据列表
        /// </summary>
        /// <returns></returns>
        public static List<VideoContrast> GetContrastList()
        {
            var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/synccontrast/listByToken")
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"从服务器获取对比数据列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                // 配置序列化忽略大小写
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                if (jsonObject.data != null)
                {
                    List<VideoContrast> videoContrastsList = JsonConvert.DeserializeObject<List<VideoContrast>>(jsonObject.data.ToString(), settings);

                    return videoContrastsList;
                }

            }

            return null;
        }

        /// <summary>
        /// 保存对比数据到服务器
        /// </summary>
        /// <param name="videoContrast"></param>
        public static void SaveContrastToServer(VideoContrast videoContrast)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/synccontrast/save");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("videoOneId", videoContrast.VideoOneId);
            dictionary.Add("videoTwoId", videoContrast.VideoTwoId);
            dictionary.Add("contrastTime", videoContrast.ContrastTime);
            dictionary.Add("anchorOneId", videoContrast.AnchorOneId);
            dictionary.Add("anchorTwoId", videoContrast.AnchorTwoId);
            dictionary.Add("fileOneId", videoContrast.FileOneId);
            dictionary.Add("fileTwoId", videoContrast.FileTwoId);
            dictionary.Add("contrastId", videoContrast.ContrastId);
            dictionary.Add("isShard", videoContrast.IsShard);
            dictionary.Add("deleteStatus", videoContrast.DeleteStatus);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 修改对比数据到服务器
        /// </summary>
        /// <param name="videoContrast"></param>
        public static void UpdateContrastToServer(VideoContrast videoContrast)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/synccontrast/update");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("videoOneId", videoContrast.VideoOneId);
            dictionary.Add("videoTwoId", videoContrast.VideoTwoId);
            dictionary.Add("contrastTime", videoContrast.ContrastTime);
            dictionary.Add("anchorOneId", videoContrast.AnchorOneId);
            dictionary.Add("anchorTwoId", videoContrast.AnchorTwoId);
            dictionary.Add("fileOneId", videoContrast.FileOneId);
            dictionary.Add("fileTwoId", videoContrast.FileTwoId);
            dictionary.Add("contrastId", videoContrast.ContrastId);
            dictionary.Add("isShard", videoContrast.IsShard);
            dictionary.Add("shareUrl", videoContrast.ShareUrl);
            if(videoContrast.DeleteStatus != -1)
            {
                dictionary.Add("deleteStatus", videoContrast.DeleteStatus);
            }
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }

        /// <summary>
        /// 定时获取当前登录的用户信息
        /// </summary>
        public static void GetUserInfo()
        {
            while (GetUserFlag)
            {
                try
                {



                    var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/user/getUserByToken")
                    {
                        Headers =
                        {
                            { "token", Token }, { "webVersion", Constant.VERSION }
                        }
                    };

                    using(HttpClient Client = new HttpClient())
                    {
                        var response = Client.SendAsync(request).Result;

                        var responseBody = response.Content.ReadAsStringAsync().Result;

                        if (!string.IsNullOrEmpty(responseBody))
                        {

                            var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);
                            // 配置序列化忽略大小写
                            var settings = new JsonSerializerSettings
                            {
                                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                            };
                            if (jsonObject.data != null)
                            {
                                UserVo userVo = JsonConvert.DeserializeObject<UserVo>(jsonObject.data.ToString(), settings);

                                UserInfo = userVo;
                            }
                            else
                            {
                                UserInfo = null;
                            }

                            //FileUtils.log("User信息：" + JsonConvert.SerializeObject(UserInfo));
                        }
                    }
                    

                }
                catch (Exception ex)
                {
                    FileUtils.log($"定时获取当前登录的用户信息请求失败： {ex}.");
                }
                Thread.Sleep(8000);
            }
        }

        /// <summary>
        /// 从云点播删掉视频文件， 加回云空间容量
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        public static void DeleteOnlineVideo(string videoId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/AnchorVideo/deleteOnlineVideo")
            {
                Query = $"videoId={videoId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"从云点播删掉视频文件请求失败： {response}.");
                    throw new Exception("删除失败，请检查网络后重试或联系管理员");
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (jsonObject.code != 0)
                {
                    throw new Exception("删除失败，请检查网络后重试或联系管理员");
                }
            }
            
        }

        /// <summary>
        /// 定时检测是否有新版本
        /// </summary>
        public static async void CheckVersionUpdate()
        {
            OperationAnchorBll operationAnchor = new OperationAnchorBll();
            int i = 0;
            while (true)
            {
                try
                {
                    await VersionUpdate(i);
                }
                catch (Exception ex)
                {
                    FileUtils.log($"定时检测是否有新版本请求失败： {ex}.");
                }
                Thread.Sleep(1000 * 60 * 5);
                // Thread.Sleep(1000 * 60);
            }
        }

        public static async Task VersionUpdate(int i)
        {
            // 检查爱复盘软件是否有更新
            CheckAifupanVersionUpdate(null);
            // 检查更新软件是否有更新
            CheckAifupanVersionUpdate(null, 1);
            // 检查是否有补丁包更新
            //CheckPackageUpdate(null);
            // 定时同步主播列表
            if (i % 3 == 0)
            {
                OperationAnchorBll operationAnchor = new OperationAnchorBll();
                await operationAnchor.updateAnchor();
            }
            // 打开更新弹窗
            openUpdateVersion();
        }

        /// <summary>
        /// 调用前端显示更新弹窗
        /// </summary>
        /// <param name="isOne">是否是自动推送的升级</param>
        public static void openUpdateVersion(bool isOne = true)
        {
            if (!isOne || !isOpenUpdate)
            {
                if (ReplayHttpUtils.VersionVo != null && !Constant.VERSION.Equals(ReplayHttpUtils.VersionVo.VersionNum))
                {
                    // 通知前端弹窗
                    try
                    {
                        bool flag = false;
                        ConfigBll configBll = new ConfigBll();
                        Config config = configBll.GetModel();
                        if (config.IsRocord == 0)
                        {
                            flag = true;
                        }
                        else
                        {
                            if (AnchorBll.recordingList == null || AnchorBll.recordingList.IsEmpty)
                            {
                                // 抖音已经发起警告，停止录制
                                //AnchorBll.StopDecectorAll();
                                flag = true;
                            }
                        }
                        if (flag)
                        {
                            updateVersion();
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogError($"{e}", $"通知前端打开更新弹窗发生异常");
                    }
                }
            }
            
        }

        public static void updateVersion(int UpdateType = -1)
        {
            try
            {
                // 通知前端上传
                FrontNotice frontNotice = new FrontNotice();
                Dictionary<string, object> result = new Dictionary<string, object>();
                result.Add("code", 0);
                result.Add("status", 200);
                result.Add("action", "openUpdate");
                Dictionary<string, object> data = new Dictionary<string, object>();
                data.Add("updateType", UpdateType == -1 ? ReplayHttpUtils.VersionVo.UpdateType : UpdateType);
                data.Add("currentVersion", Constant.VERSION);
                data.Add("updateVersion", ReplayHttpUtils.VersionVo.VersionNum);
                data.Add("updateRemarks", ReplayHttpUtils.VersionVo.UpdateInfo);
                result.Add("data", data);
                frontNotice.NoticeJs(JsonConvert.SerializeObject(result));

                // 第一次会打开更新弹窗
                isOpenUpdate = true;
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "打开更新弹窗报错");
            }
            
        }

        public static void allAreOpenUpdateVersion()
        {
            if (ReplayHttpUtils.VersionVo != null && !Constant.VERSION.Equals(ReplayHttpUtils.VersionVo.VersionNum))
            {
                updateVersion(0);
            }
        }

        public static void CheckPackageUpdate(string version)
        {
            try
            {

                // 查看单前是否有强制更新的标识，就不走了
                if (File.Exists(Path.GetFullPath("updateTemp\\updateType.txt")))
                {
                    return;
                }
                if (patchUpdateDownload)
                {
                    Debug.WriteLine("已经在下载中，不能再次下载");
                    return;
                }

                patchUpdateDownload = true;
                string updateTempPath = Path.GetFullPath("updateTemp");
                if (!Directory.Exists(updateTempPath))
                {
                    Directory.CreateDirectory(updateTempPath);
                    FileUtils.log("成功", "创建临时文件");
                }

                // 获取initPackageConfig.json
                string fileName = "initPackageConfig.json";
                string packageVersion = null;
                if (File.Exists(fileName))
                {
                    string json = File.ReadAllText(fileName);
                    Dictionary<string, string> dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(json);
                    dictionary.TryGetValue("version", out packageVersion);
                }



                string query = "version=" + (string.IsNullOrEmpty(version) ? Constant.VERSION : version);
                query += "&packageVersion=" + packageVersion;

                var urlBuilder = new UriBuilder(BaseUrl + "/openapi/clientupdate/getPackageVersion")
                {
                    Query = query
                };


                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                {
                    Headers =
                    {
                        { "token", Token }, { "webVersion", Constant.VERSION }
                    }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    var responseBody = response.Content.ReadAsStringAsync().Result;

                    if (!string.IsNullOrEmpty(responseBody))
                    {


                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                        if (jsonObject != null && jsonObject.code == 0)
                        {
                            // 配置序列化忽略大小写
                            var settings = new JsonSerializerSettings
                            {
                                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                            };
                            VersionUpdateVo versionUpdateVo = JsonConvert.DeserializeObject<VersionUpdateVo>(jsonObject.data.ToString(), settings);

                            string filePath = Path.GetFullPath("updateTemp\\servicePatchPack.zip");

                            if (versionUpdateVo == null || versionUpdateVo.FileDownLoadUrls == null)
                            {
                                return;
                            }

                            bool isDownload = true;
                            // 判断文件是否存在，如果存在，检验md5是否一致
                            if (File.Exists(filePath))
                            {
                                using (var md5 = MD5.Create())
                                {
                                    using (var stream = File.OpenRead(filePath))
                                    {
                                        byte[] hash = md5.ComputeHash(stream);
                                        string md5Str = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                                        if (md5Str.Equals(versionUpdateVo.FileMd5))
                                        {
                                            isDownload = false;
                                        }
                                    }
                                }
                            }
                            // 下载补丁包文件
                            if (isDownload)
                            {
                                FileUtils.DownloadFileAsync(versionUpdateVo.FileDownLoadUrls[0], filePath, "").GetAwaiter();
                                filePath = Path.GetFullPath("updateTemp\\updatePatch.txt");
                                File.WriteAllText(filePath, responseBody);
                            }
                            VersionPatchVo = versionUpdateVo;
                        }
                        else
                        {
                            VersionPatchVo = null;
                        }
                    }
                }
                
                patchUpdateDownload = false;
            }
            catch (Exception ex)
            {
                FileUtils.log($"定时检测是否有新版本请求失败： {ex.Message}.", "错误");
                patchUpdateDownload = false;
            }
        }

        /// <summary>
        /// 检查新版本
        /// </summary>
        /// <param name="type">检查下载类型 0：爱复盘软件 1：更新软件</param>
        public static void CheckAifupanVersionUpdate(string version, int type = 0)
        {
            try
            {
                if (type == 0)
                {
                    if (mainUpdateDownload)
                    {
                        FileUtils.log("已经在下载中，不能再次下载");
                        return;
                    }

                    mainUpdateDownload = true;
                    VersionVo = null;
                }
                string updateTempPath = Path.GetFullPath("updateTemp");
                if (!Directory.Exists(updateTempPath))
                {
                    Directory.CreateDirectory(updateTempPath);
                    FileUtils.log("成功", "创建临时文件");
                }

                string query = "version=" + (string.IsNullOrEmpty(version) ? Constant.VERSION : version);
                query += $"&isFront={type}&status={developmentMode}";

                var urlBuilder = new UriBuilder(BaseUrl + "/openapi/clientupdate/getVersionByVersion")
                {
                    Query = query
                };
                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                {
                    Headers =
                        {
                            { "token", ReplayHttpUtils.Token }, { "webVersion", Constant.VERSION }
                        }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    var responseBody = response.Content.ReadAsStringAsync().Result;

                    if (!string.IsNullOrEmpty(responseBody))
                    {


                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                        if (jsonObject != null && jsonObject.code == 0)
                        {
                            // 配置序列化忽略大小写
                            var settings = new JsonSerializerSettings
                            {
                                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                            };
                            VersionUpdateVo versionUpdateVo = JsonConvert.DeserializeObject<VersionUpdateVo>(jsonObject.data.ToString(), settings);

                            string filePath = "";
                            if (type == 0)
                            {
                                filePath = Path.GetFullPath("updateTemp\\servicePack.zip");
                            }
                            else
                            {
                                filePath = Path.GetFullPath("updateTemp\\updateServicePack.zip");
                            }

                            if (versionUpdateVo == null || versionUpdateVo.FileDownLoadUrls == null)
                            {
                                mainUpdateDownload = false;
                                return;
                            }

                            bool isDownload = true;
                            // 判断文件是否存在，如果存在，检验md5是否一致
                            if (File.Exists(filePath))
                            {
                                using (var md5 = MD5.Create())
                                {
                                    using (var stream = File.OpenRead(filePath))
                                    {
                                        byte[] hash = md5.ComputeHash(stream);
                                        string md5Str = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                                        if (md5Str.Equals(versionUpdateVo.FileMd5))
                                        {
                                            isDownload = false;
                                        }
                                    }
                                }
                            }
                            // 下载文件
                            if (isDownload)
                            {
                                using (HttpClient client = new HttpClient())
                                {
                                    // 使用同步方式下载文件并保存
                                    using (Stream stream = client.GetStreamAsync(versionUpdateVo.FileDownLoadUrls[0]).Result)  // 使用 .Result 阻塞获取下载流
                                    using (FileStream fs = new FileStream(filePath, FileMode.Create, FileAccess.Write))
                                    {
                                        stream.CopyTo(fs);
                                    }
                                }

                                //if (versionUpdateVo.UpdateType == 1)
                                //{
                                //    filePath = Path.GetFullPath("updateTemp\\updateType.txt");
                                //    File.WriteAllText(filePath, responseBody);
                                //}
                            }

                            if (type == 0)
                            {
                                VersionVo = null;
                                // 判断文件是否存在，如果存在，检验md5是否一致
                                if (File.Exists(filePath))
                                {
                                    using (var md5 = MD5.Create())
                                    {
                                        using (var stream = File.OpenRead(filePath))
                                        {
                                            byte[] hash = md5.ComputeHash(stream);
                                            string md5Str = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                                            if (md5Str.Equals(versionUpdateVo.FileMd5))
                                            {
                                                VersionVo = versionUpdateVo;
                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                VersionUpdateVo = versionUpdateVo;
                            }
                        }
                        else
                        {
                            if (type == 0)
                            {
                                VersionVo = null;
                            }
                            else
                            {
                                VersionUpdateVo = null;
                            }
                        }
                    }
                }
                
                if (type == 0)
                {
                    mainUpdateDownload = false;
                }
            }
            catch (Exception ex) {
                FileUtils.log($"定时检测是否有新版本请求失败： {ex}.");
                if (type == 0)
                {
                    mainUpdateDownload = false;
                }
            }
        }


        public static void CheckAifupanVersionUpdate2(string version, int type = 0)
        {
            try
            {
                if (type == 0)
                {
                    if (mainUpdateDownload)
                    {
                        Debug.WriteLine("已经在下载中，不能再次下载");
                        return;
                    }

                    mainUpdateDownload = true;
                }

                string updateTempPath = Path.GetFullPath("updateTemp");
                if (!Directory.Exists(updateTempPath))
                {
                    Directory.CreateDirectory(updateTempPath);
                    FileUtils.log("成功", "创建临时文件");
                }

                string query = "version=" + (string.IsNullOrEmpty(version) ? Constant.VERSION : version);
                query += $"&isFront={type}&status={developmentMode}";

                var urlBuilder = new UriBuilder(BaseUrl + "/openapi/clientupdate/getVersionByVersion")
                {
                    Query = query
                };

                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                {
                    Headers =
                        {
                            { "token", ReplayHttpUtils.Token }, { "webVersion", Constant.VERSION }
                        }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    var responseBody = response.Content.ReadAsStringAsync().Result;

                    if (!string.IsNullOrEmpty(responseBody))
                    {


                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                        if (jsonObject != null && jsonObject.code == 0)
                        {
                            // 配置序列化忽略大小写
                            var settings = new JsonSerializerSettings
                            {
                                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                            };
                            VersionUpdateVo versionUpdateVo = JsonConvert.DeserializeObject<VersionUpdateVo>(jsonObject.data.ToString(), settings);

                            string filePath = "";
                            if (type == 0)
                            {
                                filePath = Path.GetFullPath("updateTemp\\servicePack.zip");
                            }
                            else
                            {
                                filePath = Path.GetFullPath("updateTemp\\updateServicePack.zip");
                            }

                            bool isDownload = true;
                            // 判断文件是否存在，如果存在，检验md5是否一致
                            if (File.Exists(filePath))
                            {
                                using (var md5 = MD5.Create())
                                {
                                    using (var stream = File.OpenRead(filePath))
                                    {
                                        byte[] hash = md5.ComputeHash(stream);
                                        string md5Str = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                                        if (md5Str.Equals(versionUpdateVo.FileMd5))
                                        {
                                            isDownload = false;
                                        }
                                    }
                                }
                            }
                            // 下载文件
                            if (isDownload)
                            {
                                using (HttpClient client = new HttpClient())
                                {
                                    // 使用同步方式下载文件并保存
                                    using (Stream stream = client.GetStreamAsync(versionUpdateVo.FileDownLoadUrls[0]).Result)  // 使用 .Result 阻塞获取下载流
                                    using (FileStream fs = new FileStream(filePath, FileMode.Create, FileAccess.Write))
                                    {
                                        stream.CopyTo(fs);
                                    }
                                }

                            }
                            filePath = Path.GetFullPath("updateTemp\\updateType.txt");
                            File.WriteAllText(filePath, responseBody);
                            FileUtils.log("启动更新程序", "启动更新程序");
                            FileUtils.StartUpdate();
                            Thread.Sleep(10000);
                            if (type == 0)
                            {
                                VersionVo = null;
                                // 判断文件是否存在，如果存在，检验md5是否一致
                                if (File.Exists(filePath))
                                {
                                    using (var md5 = MD5.Create())
                                    {
                                        using (var stream = File.OpenRead(filePath))
                                        {
                                            byte[] hash = md5.ComputeHash(stream);
                                            string md5Str = BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
                                            if (md5Str.Equals(versionUpdateVo.FileMd5))
                                            {
                                                VersionVo = versionUpdateVo;
                                            }
                                        }
                                    }
                                }
                            }
                            else
                            {
                                VersionUpdateVo = versionUpdateVo;
                            }
                        }
                        else
                        {
                            if (type == 0)
                            {
                                VersionVo = null;
                            }
                            else
                            {
                                VersionUpdateVo = null;
                            }
                        }
                    }
                }
                
                if (type == 0)
                {
                    mainUpdateDownload = false;
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"定时检测是否有新版本请求失败： {ex}.");
                if (type == 0)
                {
                    mainUpdateDownload = false;
                }
            }

        }


        /// <summary>
        /// 保存在线人数到服务器
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void SaveOnlineNum(OnlineNum onlineNum)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/onlinenum/save");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", onlineNum.SecUid);
            dictionary.Add("batchNumber", onlineNum.BatchNumber);
            dictionary.Add("recordDate", onlineNum.RecordDate);
            dictionary.Add("peopleNum", onlineNum.PeopleNum);
            dictionary.Add("videoId", onlineNum.VideoId);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 保存或修改在线人数到服务器
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void SaveOrUpdateOnlineNum(OnlineNumNew onlineNum)
        {

            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", onlineNum.SecUid);
            dictionary.Add("batchNumber", onlineNum.BatchNumber);
            dictionary.Add("videoId", onlineNum.VideoId);
            dictionary.Add("peopleNumData", onlineNum.PeopleNumData);

            for (int i = 0; i < 10; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/onlinenum/saveOrUpdate");
                request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            break;
                        }
                    }
                    else
                    {
                        Thread.Sleep(3000);
                    }
                }
               
            }
        }

        /// <summary>
        /// 根据视频id和批次号获取服务器的在线人数数据
        /// </summary>
        /// <param name="videoId">视频唯一标识id</param>
        /// <param name="batchNumber">场次号</param>
        /// <returns></returns>
        public static OnlineNumNew GetServerOnlineNum(string videoId, string batchNumber)
        {

            var urlBuilder = new UriBuilder(BaseUrl + "/openapi/v1930/getOnlineNumList")
            {
                Query = $"videoId={videoId}&batchNumber={batchNumber}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"获取用户在服务器上的视频列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                // 配置序列化忽略大小写
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                if (jsonObject.data != null)
                {
                    List<OnlineNumServerVo> onlineNumServerVos = JsonConvert.DeserializeObject<List<OnlineNumServerVo>>(jsonObject.data.ToString(), settings);

                    if (onlineNumServerVos != null && onlineNumServerVos.Count > 0)
                    {
                        OnlineNumNew onlineNumNew = new OnlineNumNew();
                        onlineNumNew.UserId = UserId;
                        onlineNumNew.VideoId = videoId;
                        onlineNumNew.BatchNumber = batchNumber;
                        onlineNumNew.PeopleNumData = "";

                        foreach (var item in onlineNumServerVos)
                        {
                            if (!string.IsNullOrEmpty(item.RecordDate) && !string.IsNullOrEmpty(item.PeopleNum))
                            {
                                if (string.IsNullOrEmpty(onlineNumNew.PeopleNumData))
                                {
                                    onlineNumNew.PeopleNumData = item.RecordDate + "@" + item.PeopleNum;
                                }
                                else
                                {
                                    onlineNumNew.PeopleNumData = onlineNumNew.PeopleNumData + "_" + item.RecordDate + "@" + item.PeopleNum;
                                }
                            }


                        }

                        return onlineNumNew;
                    }
                }
            }
            

            return null;
        }

        /// <summary>
        /// 保存或修改总观看人数到服务器
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void SaveOrUpdateTotalOnlineNum(TotalOnlineNum onlineNum)
        {

            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", onlineNum.SecUid);
            dictionary.Add("batchNumber", onlineNum.BatchNumber);
            dictionary.Add("recordDate", onlineNum.RecordDate);
            dictionary.Add("peopleNum", onlineNum.PeopleNum);
            dictionary.Add("videoId", onlineNum.VideoId);
            dictionary.Add("startRecordDate", onlineNum.StartRecordDate);

            for (int i = 0; i < 10; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/totalonlinenum/saveOrUpdate");
                request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            break;
                        }
                    }
                    else
                    {
                        Thread.Sleep(3000);
                    }
                }
                
            }
        }

        /// <summary>
        /// 保存总观看人数到服务器
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void SaveTotalOnlineNum(TotalOnlineNum onlineNum)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/totalonlinenum/save");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", onlineNum.SecUid);
            dictionary.Add("batchNumber", onlineNum.BatchNumber);
            dictionary.Add("recordDate", onlineNum.RecordDate);
            dictionary.Add("peopleNum", onlineNum.PeopleNum);
            dictionary.Add("videoId", onlineNum.VideoId);
            dictionary.Add("startRecordDate", onlineNum.StartRecordDate);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
           
        }

        /// <summary>
        /// 修改总观看人数到服务器
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void UpdateTotalOnlineNum(TotalOnlineNum onlineNum)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/totalonlinenum/update");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", onlineNum.SecUid);
            dictionary.Add("batchNumber", onlineNum.BatchNumber);
            dictionary.Add("recordDate", onlineNum.RecordDate);
            dictionary.Add("peopleNum", onlineNum.PeopleNum);
            dictionary.Add("videoId", onlineNum.VideoId);
            dictionary.Add("startRecordDate", onlineNum.StartRecordDate);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage result = Client.SendAsync(request).Result;
            }
            
        }

        /// <summary>
        /// 查询服务器是否存在websocket采集的数据
        /// </summary>
        /// <param name="batchNumber">场次id</param>
        /// <param name="userId">用户id</param>
        /// <param name="videoId">视屏id</param>
        /// <returns></returns>
        public static bool SocketDataExist(string batchNumber, long userId, string videoId)
        {
            try
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Get;
                request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + $"/openapi/v1930/socketDataExist?batchNumber={batchNumber}&userId={userId}&videoId={videoId}");
                //request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            return jsonObject.data;
                        }
                    }
                }
                
            }
            catch (Exception e)
            {
                FileUtils.log($"v1930/socketDataExist接口调用报错，Message = {e.Message}");
            }
            return false;
        }

        /// <summary>
        /// 把websocket采集数据推送到服务器
        /// </summary>
        /// <param name="content"></param>
        /// <returns></returns>
        public static bool UploadSocketData(string content)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/openapi/v1930/uploadSocketData");
            request.Content = new StringContent(content, Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;
                if (response.IsSuccessStatusCode)
                {
                    string result = response.Content.ReadAsStringAsync().Result;
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                    if (jsonObject.code == 0)
                    {
                        return true;
                    }
                    else
                    {
                        FileUtils.LogError(result, "把websocket采集数据推送到服务器报错");
                    }
                }
            }
            
            return false;
        }

        /// 下载分析文件
        /// </summary>
        /// <param name="videoId">视频唯一标识 uuid，传其中一个</param>
        /// <param name="fileId">文件唯一标识 uuid，传其中一个</param>
        /// <returns></returns>
        public static string DownloadAnalysisFile(string videoId, string fileId, string filePath)
        {

            string url = $"{BaseUrl}/openapi/v2000/downloadAnalysisFile";

            string uuid = "";
            if (!string.IsNullOrEmpty(videoId))
            {
                url += $"/0/{videoId}";
                uuid = videoId;
            }
            else
            {
                url += $"/1/{fileId}";
                uuid = fileId;
            }

            string folderPath = Path.GetDirectoryName(filePath);

            // 确保文件夹存在
            if (!Directory.Exists(folderPath))
            {
                Directory.CreateDirectory(folderPath);
            }

            using (HttpClient client = new HttpClient())
            {

                client.DefaultRequestHeaders.Add("token", Token);
            client.DefaultRequestHeaders.Add("webVersion", Constant.VERSION);
                // 使用同步方式下载文件并保存
                long fileLength = 0;
                using (Stream stream = client.GetStreamAsync(url).Result)  // 使用 .Result 阻塞获取下载流
                using (FileStream fs = new FileStream(filePath, FileMode.Create, FileAccess.Write))
                {
                    stream.CopyTo(fs);
                    fileLength = fs.Length;
                }

                if (fileLength == 0)
                {
                    // 删除文件
                    File.Delete(filePath);
                } else
                {
                    return filePath;
                }

            }

            return null;
        }
        
        /// <summary>
        /// 异步下载分析文件（真正的异步实现）
        /// </summary>
        /// <param name="videoId">视频ID（与fileId二选一）</param>
        /// <param name="fileId">文件ID（与videoId二选一）</param>
        /// <param name="filePath">文件保存路径</param>
        /// <returns>成功返回文件路径，失败返回null</returns>
        public static async Task<string> DownloadAnalysisFileAsync(string videoId, string fileId, string filePath)
        {
            // 拼接请求URL
            string url = $"{BaseUrl}/openapi/v2000/downloadAnalysisFile";
            string uuid = string.IsNullOrEmpty(videoId) ? fileId : videoId;
            url += string.IsNullOrEmpty(videoId) ? $"/1/{fileId}" : $"/0/{videoId}";

            // 确保文件夹存在（创建文件夹是快速操作，无需异步）
            string folderPath = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(folderPath))
            {
                Directory.CreateDirectory(folderPath);
            }

            // 推荐：复用HttpClient单例（避免频繁创建导致端口耗尽）
            // 可将HttpClient定义为类的静态字段，而非方法内每次创建
            using (HttpClient client = new HttpClient())
            {
                // 关键：设置超时时间（2分钟，根据业务调整）
                client.Timeout = TimeSpan.FromMinutes(2);
                client.DefaultRequestHeaders.Add("token", Token);
            client.DefaultRequestHeaders.Add("webVersion", Constant.VERSION);

                try
                {
                    // 异步获取下载流（替换.Result为await，避免阻塞）
                    using (Stream stream = await client.GetStreamAsync(url))
                    using (FileStream fs = new FileStream(filePath, FileMode.Create, FileAccess.Write, FileShare.None, 4096, useAsync: true))
                    {
                        // 异步复制流（IO密集型操作异步化，释放线程）
                        await stream.CopyToAsync(fs);
                        // 刷新并确保数据写入磁盘
                        await fs.FlushAsync();

                        // 获取文件长度（异步写入后长度已正确）
                        long fileLength = fs.Length;

                        // 文件为空则删除，否则返回路径
                        if (fileLength == 0)
                        {
                            File.Delete(filePath);
                            return null;
                        }
                        return filePath;
                    }
                }
                catch (Exception ex)
                {
                    // 异常处理：删除未完成的文件，避免残留空文件
                    if (File.Exists(filePath))
                    {
                        File.Delete(filePath);
                    }
                    // 可根据业务需求记录日志、抛出自定义异常等
                    FileUtils.LogError($"下载文件失败（UUID：{uuid}）：{ex.Message}");
                    return null;
                }
            }
        }

        /// <summary>
        /// 下载云空间分析文件
        /// </summary>
        /// <param name="url">请求地址</param>
        /// <param name="filePath">保存地址</param>
        /// <returns></returns>
        public static string DownloadCloudAnalysisFile(string url, string filePath)
        {

            string folderPath = Path.GetDirectoryName(filePath);

            // 确保文件夹存在
            if (!Directory.Exists(folderPath))
            {
                Directory.CreateDirectory(folderPath);
            }


            using (HttpClient client = new HttpClient())
            {
                client.DefaultRequestHeaders.Add("token", Token);
            client.DefaultRequestHeaders.Add("webVersion", Constant.VERSION);
                // 使用同步方式下载文件并保存
                long fileLength = 0;
                using (Stream stream = client.GetStreamAsync(url).Result)  // 使用 .Result 阻塞获取下载流
                using (FileStream fs = new FileStream(filePath, FileMode.Create, FileAccess.Write))
                {
                    stream.CopyTo(fs);
                    fileLength = fs.Length;
                }

                if (fileLength == 0)
                {
                    // 删除文件
                    File.Delete(filePath);
                }
                else
                {
                    return filePath;
                }

            }

            return null;
        }

        /// <summary>
        /// 根据视频id从服务器获取视频
        /// </summary>
        /// <param name="videoId">视频唯一标识id</param>
        /// <returns></returns>
        public static AnchorVideo GetServerVideoByVideoId(string videoId)
        {

            var urlBuilder = new UriBuilder(BaseUrl + "/AnchorVideo/infoByVideoId")
            {
                Query = $"videoId={videoId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"根据视频id从服务器获取视频请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (jsonObject.code == 0 && jsonObject.data != null)
                {
                    AnchorVideo anchorVideo = JsonConvert.DeserializeObject<AnchorVideo>(jsonObject.data.ToString());

                    return anchorVideo;
                }
            }
            

            return null;
        }

        /// <summary>
        /// 根据对比id从服务器获取对比信息
        /// </summary>
        /// <param name="contrastId">对比唯一标识id</param>
        /// <returns></returns>
        public static CloudContrastInfoVo GetServerContrastByContrastId(string contrastId)
        {

            var urlBuilder = new UriBuilder(BaseUrl + "/synccontrast/infoByContrastId")
            {
                Query = $"contrastId={contrastId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"根据对比id从服务器获取视频请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (jsonObject.code == 0 && jsonObject.data != null)
                {
                    CloudContrastInfoVo videoContrast = JsonConvert.DeserializeObject<CloudContrastInfoVo>(jsonObject.data.ToString());

                    return videoContrast;
                }
            }
            

            return null;
        }

        /// <summary>
        /// 根据行业id获取行业信息
        /// </summary>
        /// <param name="tradeId"></param>
        /// <returns></returns>
        public static TradeVo GetTrade(string tradeId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/trade/info")
            {
                Query = $"id={tradeId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"行业信息请求失败： {response}.");
                    FileUtils.log($"行业信息请求失败, tradeId={tradeId}");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (jsonObject.code == 0 && jsonObject.data != null)
                {
                    TradeVo videoContrast = JsonConvert.DeserializeObject<TradeVo>(jsonObject.data.ToString());

                    return videoContrast;
                }
            }
            

            return null;
        }

        /// <summary>
        /// 保存ai会话文件
        /// </summary>
        /// <param name="type"></param>
        /// <param name="userId"></param>
        /// <param name="sourceId"></param>
        /// <param name="sourceType"></param>
        /// <param name="contextId"></param>
        /// <param name="coskey"></param>
        /// <param name="resourceType"></param>
        /// <param name="platformType"></param>
        /// <exception cref="CustomException"></exception>
	    public static void updateCosThumbsFile(int type, string userId, string sourceId, int sourceType, string contextId, string coskey, int? thumbState)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/openapi/v2200/updateCosThumbsFile");
            var temp = new
            {
                userId,
                sourceId,
                sourceType,
                coskey,
                contextId,
                resourceType = 0,
                platformType = 1,
                cosType = type,
                thumbState
            };
            request.Content = new StringContent(JsonConvert.SerializeObject(temp), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"保存ai会话文件请求失败, response={response}");
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (jsonObject.code != 0)
                {
                    throw new CustomException("保存上下问缓存id失败", 7003);
                }
            }

            
        }

        /// <summary>
        /// 预扣
        /// </summary>
        /// <param name="thisUseNum"></param>
        /// <returns></returns>
        /// <exception cref="CustomException"></exception>
        public static string isPropertyHaveAiToken(long thisUseNum)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", ReplayHttpUtils.Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/openapi/userproperty/isPropertyHaveAiToken");
            var temp = new
            {
                thisUseNum
            };
            request.Content = new StringContent(JsonConvert.SerializeObject(temp), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"调预扣失败, thisUseNum={thisUseNum}");
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var res = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (res.code == 0)
                {
                    if (res.data != null && (bool)res.data.isHave)
                    {
                        return res.data.redisId;
                    }
                    else
                    {
                        throw new CustomException("AI分析文本数量不足", 7001);
                    }
                }
                else
                {
                    throw new CustomException("预扣失败", 7005);
                }
            }

            
        }

        /// <summary>
        /// 删除预扣
        /// </summary>
        /// <param name="redisId"></param>
        public static void removeTempUserProperty(string redisId)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/openapi/userproperty/removeTempUserProperty")
            {
                Query = $"redisId={redisId}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.LogError($"调用删除预扣资产接口失败, tradeId={redisId}");
                    return;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var res = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (res.code != 0)
                {
                    FileUtils.LogError($"删除预扣资产返回错误, msg={res.msg}, data={res.data}, code={res.code}");
                }
            }
            
        }

        /// <summary>
		
		/// <summary>
        /// 根据主播url集合或secuid集合获取主播信息列表
        /// </summary>
        /// <param name="urls">主播url集合或secuid集合</param>
        /// <returns></returns>
        public static List<AnchorInfo> GetAnchorListBySecUidsOrUrls(List<string> urls)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/anchorurl/listBySecUids");
            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            request.Content = new StringContent(JsonConvert.SerializeObject(urls), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                HttpResponseMessage response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"根据主播url集合或secuid集合获取主播信息列表请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if (jsonObject.code == 0 && jsonObject.data != null)
                {
                    // 配置序列化忽略大小写
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                    };
                    List<AnchorInfo> anchorInfos = JsonConvert.DeserializeObject<List<AnchorInfo>>(jsonObject.data.ToString(), settings);

                    return anchorInfos;
                }
            }
            

            return null;
        }

		/// <summary>
        /// 更新主播弹幕监控状态
        /// </summary>
        /// <param name="secUid"></param>
        /// <param name="isBarrageMonitoring"></param>
        /// <returns></returns>
        public static dynamic UpdateBarrageMonitoring(string secUid, int isBarrageMonitoring)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/openapi/v2100/updateBarrageMonitoring")
            {
                Query = $"secUid={secUid}&isBarrageMonitoring={isBarrageMonitoring}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"更新主播弹幕监控状态接口请求失败： {response}.");
                    return new { code = 500, msg = "网络错误，请重试" };
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                dynamic jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                return jsonObject;
            }
            
        }

        /// <summary>
        /// 查询弹幕数据是否存在
        /// </summary>
        /// <param name="secUid"></param>
        /// <param name="batchNumber"></param>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static bool existsBarrage(string secUid, string batchNumber, string videoId)
        {
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
            request.RequestUri = new Uri(BaseUrl + "/openapi/v2100/existsBarrage");
            request.Content = new StringContent(JsonConvert.SerializeObject(new { secUid, batchNumber, videoId }), Encoding.UTF8, "application/json");

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;
                if (response.IsSuccessStatusCode)
                {
                    string result = response.Content.ReadAsStringAsync().Result;
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                    if (jsonObject.code == 0)
                    {
                        return jsonObject.data;
                    }
                }
            }
            
            return false;
        }

        /// 更新主播自动上传云空间状态
        /// </summary>
        /// <param name="secUid"></param>
        /// <param name="isAutoUploadCloud"></param>
        /// <returns></returns>
        public static void UpdateAutoUploadCloud(string secUid, int isAutoUploadCloud)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/openapi/v2100/updateAutoUploadCloud")
            {
                Query = $"secUid={secUid}&isAutoUploadCloud={isAutoUploadCloud}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using(HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"更新主播自动上传云空间状态接口请求失败： {response}.");
                    throw new Exception("网络异常，请稍后重试");
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                dynamic dynamic = JsonConvert.DeserializeObject<dynamic>(responseBody);
                if (dynamic.code != 0)
                {
                    throw new Exception(dynamic.msg);
                }
            }
            
        }

        /// <summary>
        /// 上传弹幕文件
        /// </summary>
        /// <param name="filePath"></param>
        public static void uploadDanMuData(string filePath, string secUid, string batchNumber, string videoId)
        {
            byte[] fileBytes = File.ReadAllBytes(filePath);
            ByteArrayContent fileContent = new ByteArrayContent(fileBytes);
            fileContent.Headers.ContentType = MediaTypeHeaderValue.Parse("multipart/form-data");

            var tempObj = new { secUid, batchNumber, videoId };
            // 将 danMuData 序列化为 JSON
            string danMuDataJson = JsonConvert.SerializeObject(tempObj);
            StringContent danMuDataContent = new StringContent(danMuDataJson, Encoding.UTF8, "application/json");
            try
            {
                Action<MultipartFormDataContent> fillForm = (formData) =>
                {
                    // 对应 @RequestPart("danMuData")
                    formData.Add(danMuDataContent, "danMuData");
                    // 对应 @RequestPart("file")
                    formData.Add(fileContent, "file", Path.GetFileName(filePath));
                };

                string respnseBody = HttpUtils.SendPost($"{BaseUrl}/openapi/v2100/uploadDanMuData", fillForm, 5, 10*60);

                if (!string.IsNullOrEmpty(respnseBody))
                {
                    var resultObj = JsonConvert.DeserializeObject<dynamic>(respnseBody);
                    if (resultObj != null && resultObj.code == 0)
                    {
                        return;
                    }
                }
                FileUtils.LogError($"danMuDataJson={danMuDataJson}，filePath={filePath},错误信息：{respnseBody}", "上传弹幕出错");
                throw new CustomException("上传弹幕出错");
            }
            catch (Exception ex)
            {
                throw ex;
            }
        }

        /// <summary>
        /// 判断是否要上传会话cos文件
        /// </summary>
        /// <param name="contextId"></param>
        /// <returns></returns>
        public static bool HasUploadCosThumbsFile(string contextId)
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/openapi/v2200/getCosThumbsFileByContextId?contextId={contextId}")
                {
                    Headers =
                    {
                        { "token", Token }, { "webVersion", Constant.VERSION }
                    }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.LogError($"根据contextId获取上下文的记录请求失败： {response}.");
                        return false;
                    }

                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    var res = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    if (res.data != null)
                    {
                        if (res.data.coskay == null || string.IsNullOrEmpty(res.data.coskey))
                        {
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }
                }
                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"根据contextId获取上下文的记录请求报错： {ex.Message}.");
                return false;
            }

        }

        /// <summary>
        /// 根据字数获取要切换模型
        /// </summary>
        /// <param name="currentNum"></param>
        /// <returns></returns>
        public static int GetAiModel(int currentNum)
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/openapi/v2200/getAiModel?currentNum={currentNum}")
                {
                    Headers =
                    {
                        { "token", Token }, { "webVersion", Constant.VERSION }
                    }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.LogError($"根据字数获取要切换模型请求失败： {response}.");
                        return 0;
                    }

                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    var res = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    if (res.code == 0)
                    {
                        return res.data ?? 0;
                    }
                }
                

                return 0;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"根据字数获取要切换模型请求报错： {ex.Message}.");
                return 0;
            }
        }

        /// <summary>
        /// 更新主播置顶信息
        /// </summary>
        /// <param name="secUid">主播secuid</param>
        /// <param name="action">动作 0：取消置顶 1：置顶</param>
        /// <param name="addTopTime">添加置顶的时间</param>
        public static void UpdateAnchorTop(string secUid, int action, string addTopTime)
        {
            var urlBuilder = new UriBuilder(BaseUrl + "/openapi/v2300/updateAnchorTop")
            {
                Query = $"secUid={secUid}&action={action}&addTopTime={addTopTime}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
            };

            using (HttpClient Client = new HttpClient())
            {
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"更新主播置顶信息接口请求失败： {response}.");
                    throw new Exception("网络异常，请稍后重试");
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                dynamic dynamic = JsonConvert.DeserializeObject<dynamic>(responseBody);
                if (dynamic.code != 0)
                {
                    throw new Exception(dynamic.msg);
                }
            }
        }

        /// <summary>
        /// 更新主播最后开始录制时间
        /// </summary>
        /// <param name="secUid">主播secuid</param>
        /// <param name="lastRecordTime">最后开始录制时间</param>
        public static void UpdateAnchorLastRecordTime(string secUid, string lastRecordTime)
        {
            try
            {
                var urlBuilder = new UriBuilder(BaseUrl + "/openapi/v2300/updateAnchorLastRecordTime")
                {
                    Query = $"secUid={secUid}&lastRecordTime={lastRecordTime}"
                };

                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
                {
                    Headers =
                {
                    { "token", Token }, { "webVersion", Constant.VERSION }
                }
                };

                using (HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.LogRecrd($"{response}", $"更新主播最后开始录制时间接口请求失败");
                        FileUtils.log($"更新主播最后开始录制时间接口请求失败： {response}.");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"更新主播最后开始录制时间异常");
            }
            
        }

        /// <summary>
        /// 批量保存商品详情到服务器
        /// </summary>
        /// <param name="productDetailsList">商品详情列表</param>
        /// <returns>是否成功</returns>
        public static bool SaveProductDetailsBatch(List<bo.ProductDetailsBo> productDetailsList)
        {
            try
            {
                if (productDetailsList == null || productDetailsList.Count == 0)
                {
                    FileUtils.LogRecrd("商品详情列表为空", "批量保存商品详情");
                    return false;
                }

                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", Token);
                request.RequestUri = new Uri(BaseUrl + "/words/productDetails/saveBatch");
                // 序列化时排除 id 字段
                string body = JsonConvert.SerializeObject(productDetailsList, new JsonSerializerSettings
                {
                    NullValueHandling = NullValueHandling.Ignore,
                    ContractResolver = new IgnoreIdContractResolver()
                });
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                using (HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (response.IsSuccessStatusCode)
                    {
                        string result = response.Content.ReadAsStringAsync().Result;
                        var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                        if (jsonObject.code == 0)
                        {
                            FileUtils.LogRecrd($"商品详情批量保存成功，共 {productDetailsList.Count} 条", "批量保存商品详情");
                            return true;
                        }
                        else
                        {
                            FileUtils.LogRecrd($"商品详情保存失败: {jsonObject.msg}", "批量保存商品详情");
                        }
                    }
                    else
                    {
                        FileUtils.LogRecrd($"商品详情保存请求失败: {response.StatusCode}", "批量保存商品详情");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"批量保存商品详情异常: {ex.Message}", "批量保存商品详情");
            }
            return false;
        }
    }

    /// <summary>
    /// 序列化时排除 id 字段
    /// </summary>
    public class IgnoreIdContractResolver : DefaultContractResolver
    {
        protected override IList<JsonProperty> CreateProperties(Type type, MemberSerialization memberSerialization)
        {
            var properties = base.CreateProperties(type, memberSerialization);
            properties = properties.Where(p => p.PropertyName != "id").ToList();
            return properties;
        }
    }
}
