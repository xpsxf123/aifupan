using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using System.Collections.Generic;
using douyin.Utils;
using ReviewAnalysis.Model;
using System.Collections;
using ReviewAnalysis.DataCache;
using Swan.Parsers;
using static System.Windows.Forms.VisualStyles.VisualStyleElement.Tab;
using System.Threading;
using ReviewAnalysis.Global;
using Qiniu.Util;
using System.IO;
using System.Security.Cryptography;
using System.Data.Entity.Infrastructure;
using EmbedIO.Sessions;
using System.Diagnostics;
using System.Security.Policy;
using ReviewAnalysis.Utils;


namespace ReviewAnalysis.Asr
{
    public class ReplayHttpUtils1
    {
        public static string BaseUrl = Constant.GetApiBaseUrl();
        //public static string BaseUrl = Constant.BaseUrl;
        //public static string BaseUrl = Constant.BaseTestUrl;
        public static string Token = "";
        public static string UserId = "";
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

        /// <summary>
        /// 清空服务器文件的分析数据
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns></returns>
        public static void ClearFileAnalysis(string fileId)
        {

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("fileId", fileId);
            HttpUtils.SendGet(ReplayHttpUtils.BaseUrl + "/UploadFile/clearAnalysis", param);

        }

        /// <summary>
        /// 清空服务器视频的分析数据
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns></returns>
        public static void ClearVideoAnalysis(string videoId)
        {

            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("videoId", videoId);
            HttpUtils.SendGet(ReplayHttpUtils.BaseUrl + "/AnchorVideo/clearAnalysis", param);

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
            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            var response = Client.SendAsync(request).Result;

            if (!response.IsSuccessStatusCode)
            {
                FileUtils.log($"获取用户在服务器上的文件分析列表请求失败： {response}.");
                return null;
            }

            var responseBody = response.Content.ReadAsStringAsync().Result;
            if(!string.IsNullOrEmpty(responseBody))
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

            HttpClient Client = new HttpClient();
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            var response = Client.SendAsync(request).Result;

            if (!response.IsSuccessStatusCode)
            {
                FileUtils.log($"获取用户在服务器上的文件分析列表请求失败： {response}.");
                return null;
            }

            var responseBody = response.Content.ReadAsStringAsync().Result;
            if(!string.IsNullOrEmpty(responseBody))
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
                List < Dictionary<string, object> >  list = new List<Dictionary<string, object>>();
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

                HttpClient Client = new HttpClient();
                Client.SendAsync(request);
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
            dictionary.Add("deleteStatus", anchorVideo.DeleteStatus);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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
            dictionary.Add("deleteStatus", anchorVideo.DeleteStatus);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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
            if(!string.IsNullOrEmpty(tradeId))
            {
                dictionary.Add("tradeId", tradeId);
            }
            if(isAutoRecord != -1)
            {
                dictionary.Add("isAutoRecord", isAutoRecord + "");
            }
            if (isRemoveRecord != -1)
            {
                dictionary.Add("isRemoveRecord", isRemoveRecord + "");
            }


            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
        }

        /// <summary>
        /// 获取用户在服务器上的主播列表
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <returns></returns>
        public static List<AnchorUrlEntity> GetAnchorList(string token)
        {
            //var request = new HttpRequestMessage(HttpMethod.Get, $"{BaseUrl}/anchorurl/listByUserToken")
            //{
            //    Headers =
            //    {
            //        { "token", token }, { "webVersion", Constant.VERSION }
            //    }
            //};

            //HttpClient Client = new HttpClient();
            //var response = Client.SendAsync(request).Result;

            //if (!response.IsSuccessStatusCode)
            //{
            //    FileUtils.log($"获取用户在服务器上的主播列表请求失败： {response}.");
            //    return null;
            //}

            //var responseBody = response.Content.ReadAsStringAsync().Result;

            string responseBody = HttpUtils.SendGet(ReplayHttpUtils.BaseUrl + "/anchorurl/listByUserToken", null);

            var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

            // 配置序列化忽略大小写
            var settings = new JsonSerializerSettings
            {
                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
            };
            if(jsonObject.data != null)
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

                HttpClient Client = new HttpClient();
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.log($"询问是否还有QPS余量请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if(jsonObject.data != null)
                {
                    CheckSurplusEntity checkSurplusEntity = JsonConvert.DeserializeObject<CheckSurplusEntity>(jsonObject.data.ToString());
                    if (checkSurplusEntity != null)
                    {
                        checkSurplusEntity.Code = jsonObject.code;
                        checkSurplusEntity.Msg = jsonObject.msg;

                        return checkSurplusEntity;
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

                HttpClient Client = new HttpClient();
                Client.SendAsync(request);

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

            HttpClient Client = new HttpClient();
            var response = Client.SendAsync(request).Result;

            if (!response.IsSuccessStatusCode)
            {
                FileUtils.log($"通知增加QPS余量请求失败： {response}.");
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

            HttpClient Client = new HttpClient();
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

            HttpClient Client = new HttpClient();
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
        public static async Task<List<SentenceMarkVo>> WordsMark(string platform, string videoId, List<ASRResultEntity> asrResultList, string tradeId, int type)
        {
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
                if(string.IsNullOrEmpty(asrResultEntity.Result))
                {
                    asrResultEntity.Result = "-";
                }
                bodyJsonObject["content"] = asrResultEntity.Result;
                bodyJsonObject["currentSort"] = asrResultEntity.Paragraph;
                bodyJsonObject["isLast"] = asrResultEntity.Paragraph == asrResultList.Count ? 1 : 0;
                bodyJsonObject["type"] = type;
                JArray wordListArray = new JArray();
                if(asrResultEntity.WordList != null && asrResultEntity.WordList.Count > 0)
                {
                    foreach (var item in asrResultEntity.WordList)
                    {
                        wordListArray.Add(JToken.FromObject(item));
                    }
                }else
                {
                    ASRWordEntity wordEntity = new ASRWordEntity();
                    wordEntity.Word = "-";
                    wordEntity.StartTime = 0;
                    wordEntity.EndTime = 20;
                    wordListArray.Add (JToken.FromObject(wordEntity));
                    bodyJsonObject["content"] = asrResultEntity.Result;
                }
                
                bodyJsonObject["items"] = wordListArray;

                bodyJsonList.Add(bodyJsonObject);
            }

            string body = JsonConvert.SerializeObject(bodyJsonList);

            // 重试3次机会
            for(int i = 0; i < 3; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/sensitivewords/wordsMark");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                HttpClient Client = new HttpClient();
                var response = await Client.SendAsync(request);
                FileUtils.LogAnalysis($"服务器关键词/敏感词识别网络状态：{response}");
                if (response.IsSuccessStatusCode)
                {
                    string result = await response.Content.ReadAsStringAsync();
                    FileUtils.log($"服务器关键词/敏感词识别结果：{result}");
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                    if(jsonObject.code == 0)
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
        public static List<SentenceMarkVo> WordsMarkSync(string platform, string videoId, string tradeId, int type)
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
                request.RequestUri = new Uri(BaseUrl + "/sensitivewords/wordsMarkReAnalysis");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                HttpClient Client = new HttpClient();
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
            for(int i = 0; i < 3; i++)
            {
                var request = new HttpRequestMessage();
                request.Method = HttpMethod.Post;
                request.Headers.Add("token", Token);
            request.Headers.Add("webVersion", Constant.VERSION);
                request.RequestUri = new Uri(BaseUrl + "/sensitivewords/wordsMark");
                request.Content = new StringContent(body, Encoding.UTF8, "application/json");

                HttpClient Client = new HttpClient();
                var response = Client.SendAsync(request).Result;
                FileUtils.LogAnalysis($"服务器关键词/敏感词识别网络状态：{response}");
                if (response.IsSuccessStatusCode)
                {
                    string result = response.Content.ReadAsStringAsync().Result;
                    FileUtils.log($"服务器关键词/敏感词识别结果：{result}");
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(result);
                    if(jsonObject.code == 0)
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
        public static async Task<SentenceMarkVo> WordsMarkByText(string token, string platform, string fileId, string content, string tradeId, int type)
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

                HttpClient Client = new HttpClient();
                var response = await Client.SendAsync(request);
                if (response.IsSuccessStatusCode)
                {
                    string result = await response.Content.ReadAsStringAsync();
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

                HttpClient Client = new HttpClient();
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

            HttpClient Client = new HttpClient();
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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
            dictionary.Add("deleteStatus", videoContrast.DeleteStatus);
            request.Content = new StringContent(JsonConvert.SerializeObject(dictionary), Encoding.UTF8, "application/json");

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

                    HttpClient Client = new HttpClient();
                    var response = Client.SendAsync(request).Result;

                    var responseBody = response.Content.ReadAsStringAsync().Result;

                    if (!string.IsNullOrEmpty(responseBody)) {

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
                        }else
                        {
                            UserInfo = null;
                        }

                        //FileUtils.log("User信息：" + JsonConvert.SerializeObject(UserInfo));
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

            HttpClient Client = new HttpClient();
            var response = Client.SendAsync(request).Result;

            if (!response.IsSuccessStatusCode)
            {
                FileUtils.log($"从云点播删掉视频文件请求失败： {response}.");
                throw new Exception("删除失败，请检查网络后重试或联系管理员");
            }

            var responseBody = response.Content.ReadAsStringAsync().Result;
            var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

            if(jsonObject.code != 0)
            {
                throw new Exception("删除失败，请检查网络后重试或联系管理员");
            }
        }

        /// <summary>
        /// 定时检测是否有新版本
        /// </summary>
        public static async void CheckVersionUpdate()
        {
            while (true)
            {
                try
                {
                    // 检查爱复盘软件是否有更新
                    CheckAifupanVersionUpdate(null);
                    // 检查更新软件是否有更新
                    CheckAifupanVersionUpdate(null,1);
                    // 检查是否有补丁包更新
                    CheckPackageUpdate(null);
                }
                catch (Exception ex)
                {
                    FileUtils.log($"定时检测是否有新版本请求失败： {ex}.");
                }
                Thread.Sleep(1000 * 60 * 5);
                // Thread.Sleep(1000 * 60);
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
                query += "&packageVersion="+ packageVersion;

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

                HttpClient Client = new HttpClient();
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
        public static void CheckAifupanVersionUpdate(string version,int type=0)
        {
            try
            {
                if(type == 0)
                {
                    if (mainUpdateDownload)
                    {
                        Debug.WriteLine("已经在下载中，不能再次下载");
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
                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri);

                HttpClient Client = new HttpClient();
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

                            //Thread downloadTask = FileUtils.DownloadFile(versionUpdateVo.FileDownLoadUrls[0], filePath, "");
                            // 启动下载任务
                            //Task downloadTask = FileUtils.DownloadFileAsync2(versionUpdateVo.FileDownLoadUrls[0], filePath, "");

                            // 手动等待任务完成
                            //downloadTask.Wait();

                            if (versionUpdateVo.UpdateType == 1)
                            {
                                filePath = Path.GetFullPath("updateTemp\\updateType.txt");
                                File.WriteAllText(filePath, responseBody);
                            }
                        }

                        if (type == 0)
                        {
                            VersionVo = versionUpdateVo;
                        }
                        else
                        {
                            VersionUpdateVo = versionUpdateVo;
                        }
                    }else
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

                var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri);

                HttpClient Client = new HttpClient();
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
                            VersionVo = versionUpdateVo;
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
        }

        /// <summary>
        /// 保存或修改在线人数到服务器
        /// </summary>
        /// <param name="onlineNum"></param>
        public static void SaveOrUpdateOnlineNum(OnlineNum onlineNum)
        {

            Dictionary<string, object> dictionary = new Dictionary<string, object>();
            dictionary.Add("secUid", onlineNum.SecUid);
            dictionary.Add("batchNumber", onlineNum.BatchNumber);
            dictionary.Add("recordDate", onlineNum.RecordDate);
            dictionary.Add("peopleNum", onlineNum.PeopleNum);
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

                HttpClient Client = new HttpClient();
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

                HttpClient Client = new HttpClient();
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
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

            HttpClient Client = new HttpClient();
            Client.SendAsync(request);
        }
    }
}
