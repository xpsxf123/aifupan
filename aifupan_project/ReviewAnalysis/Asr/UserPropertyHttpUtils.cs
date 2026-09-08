using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using Swan.Parsers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class UserPropertyHttpUtils
    {

        

        /// <summary>
        /// 检查用户是否能分析
        /// </summary>
        /// <param name="analysisMinute"></param>
        /// <returns>0：网络异常 1：时长足够 2：时长不足</returns>
        public static int CheckAnalysisMinute(int analysisMinute)
        {

            UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
            if (userPropertyEntity == null)
            {
                return 0;
            }
            if (userPropertyEntity.AiAnalysisTime == -1)
            {
                return 1;
            }
            else if (userPropertyEntity.AiAnalysisTime >= analysisMinute)
            {
                return 1;
            }

            return 2;
        }

        /// <summary>
        /// 检查用户文案提取时长是否足够
        /// </summary>
        /// <param name="videoMinute"></param>
        /// <returns>0：网络异常 1：时长足够 2：时长不足</returns>
        public static async Task<int> CheckTextExtraction(int videoMinute)
        {

            UserPropertyEntity userPropertyEntity = await UserPropertyApi.GetPropertyInfo();
            if (userPropertyEntity == null)
            {
                return 0;
            }
            if (userPropertyEntity.TextExtractionNum == -1)
            {
                return 1;
            }
            else if (userPropertyEntity.TextExtractionNum >= videoMinute)
            {
                return 1;
            }

            return 2;
        }

        /// <summary>
        /// 检查用户是否能添加主播
        /// </summary>
        /// <param name="anchorNum">数量</param>
        /// <param name="platform">平台类型 0：抖音 1：快手 2：视频号</param>
        /// <returns></returns>
        public static bool CheckAddAnchor(int anchorNum, int platform = 0)
        {
            UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
            if (userPropertyEntity == null)
            {
                return false;
            }
            if (userPropertyEntity.AnchorNum == -1)
            {
                return true;
            }

            if (platform == 0)
            {
                // 抖音
                if (userPropertyEntity.AnchorNum >= anchorNum)
                {
                    return true;
                }
            }
            else if(platform == 1)
            {
                // 快手
                if (userPropertyEntity.kuaishouMonitorNum >= anchorNum)
                {
                    return true;
                }
            }
            else if (platform == 2)
            {
                // 微信视频号
                if (userPropertyEntity.WeChatChannelMonitorNum >= anchorNum)
                {
                    return true;
                }
            }
            
            return false;
        }

        /// <summary>
        /// 检查用户是否能录制
        /// </summary>
        /// <param name="recordNum"></param>
        /// <returns></returns>
        public static bool CheckRecord(int recordNum)
        {

            //UserPropertyEntity userPropertyEntity = GetUserProperty(ReplayHttpUtils.Token).Result;
            //if(userPropertyEntity == null)
            //{
            //    return false; 
            //}
            //if (userPropertyEntity.MonitorNum == -1)
            //{
            //    return true;
            //}
            //else if (userPropertyEntity.MonitorNum >= recordNum)
            //{
            //    return true;
            //}

            //return false;

            return true;
        }

        /// <summary>
        /// 获取监控位数量
        /// </summary>
        /// <returns></returns>
        public static int GetMonitorNum()
        {
            int monitorNum = 0;
            UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
            if(userPropertyEntity != null)
            {
                return userPropertyEntity.MonitorNum;
            }
            return monitorNum;
        }

        /// <summary>
        /// 获取文本标注字数数量
        /// </summary>
        /// <returns></returns>
        public static long GetTextWotdNum()
        {
            long textWordNum = 0;
            UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
            if (userPropertyEntity != null)
            {
                return userPropertyEntity.TextTaggingWordCount;
            }
            return textWordNum;
        }

        /// <summary>
        /// 获取用户资产信息
        /// </summary>
        /// <param name="token">认证令牌</param>
        /// <returns></returns>
        public static async Task<UserPropertyEntity> GetUserProperty(string token)
        {
            var request = new HttpRequestMessage(HttpMethod.Get, $"{ReplayHttpUtils.BaseUrl}/openapi/userproperty/getUserProperty")
            {
                Headers =
                {
                    { "token", token }
                }
            };
            try
            {
                using(HttpClient Client = new HttpClient())
                {
                    Client.Timeout = TimeSpan.FromSeconds(30);
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.LogAnalysis($"获取用户资产信息请求失败： {response}.");
                        return null;
                    }

                    var responseBody = await response.Content.ReadAsStringAsync();
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
                        NullValueHandling = NullValueHandling.Ignore // 新增：忽略null值
                    };

                    UserPropertyEntity userPropertyEntity = JsonConvert.DeserializeObject<UserPropertyEntity>(jsonObject.data.ToString(), settings);

                    return userPropertyEntity;
                }
                
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"获取用户资产信息请求失败： {ex}.");
                return null;
            }

            //UserPropertyEntity userPropertyEntity = new UserPropertyEntity();
            //userPropertyEntity.TotalMonitorNum = 100;
            //userPropertyEntity.MonitorNum = 100;
            //userPropertyEntity.AnchorNum = 100;
            //userPropertyEntity.TotalAnchorNum = 100;
            //userPropertyEntity.AiAnalysisTime = 10000;
            //userPropertyEntity.TotalAiAnalysisTime = 10000;

        }

        /// <summary>
        /// 使用用户资产
        /// </summary>
        /// <param name="type">资源类型 1：（监控位数量）monitor_num 2：（语音分析时长）ai_analysis_time  3：（添加主播数量）anchor_num</param>
        /// <param name="num">使用数量</param>
        /// <returns></returns>
        //public static void UpdateUserProperty(int type, int num)
        //{
        //    // 构造请求体参数
        //    JObject bodyJsonObject = new JObject();
        //    bodyJsonObject["type"] = type;
        //    bodyJsonObject["num"] = num;
        //    string body = bodyJsonObject.ToString();
        //    var request = new HttpRequestMessage();
        //    request.Method = HttpMethod.Post;
        //    request.Headers.Add("token", ReplayHttpUtils.Token);
        //    request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/userproperty/saveUserPropertyDetails");
        //    request.Content = new StringContent(body, Encoding.UTF8, "application/json");

        //    using(HttpClient Client = new HttpClient())
        //    var response = Client.SendAsync(request).Result;
        //    if (!response.IsSuccessStatusCode)
        //    {
        //        FileUtils.LogAnalysis($"使用用户资产请求失败： {response}.");
        //    }
        //}

        /// <summary>
        /// 使用用户资产
        /// </summary>
        /// <param name="code">资源类型 aiAnalysisTime :ai语音分析时长;  videoTaggingTime :视频标注时长; textTaggingWordCount :文本标注字数
        /// monitorNum :监控位;  anchorNum :可添加主播数;  storageNum :空间容量
        /// subAccountCount :拥有子账号数量;  child_monitorNum :子账号监控位;  child_anchorNum :子账号可添加主播数</param>
        /// <param name="num">使用数量</param>
        /// <returns></returns>
        public static void UpdateUserProperty(string code, int num)
        {
            UserPropertyApi.useProperty(code, num);

            // 构造请求体参数
            //JObject bodyJsonObject = new JObject();
            //bodyJsonObject["code"] = code;
            //bodyJsonObject["num"] = num;
            //bodyJsonObject["redisId"] = redisId;
            //string body = bodyJsonObject.ToString();
            //var request = new HttpRequestMessage();
            //request.Method = HttpMethod.Post;
            //request.Headers.Add("token", ReplayHttpUtils.Token);
            //request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/openapi/userproperty/useProperty");
            //request.Content = new StringContent(body, Encoding.UTF8, "application/json");

            //using(HttpClient Client = new HttpClient())
            //{
            //    var response = Client.SendAsync(request).Result;
            //    if (!response.IsSuccessStatusCode)
            //    {
            //        FileUtils.LogAnalysis($"使用用户资产请求失败： {response}.");
            //    }
            //}
            
        }

        /// <summary>
        /// 加回用户的资产
        /// </summary>
        /// <param name="type">资源类型 1：（监控位数量）monitor_num 2：（语音分析时长）ai_analysis_time  3：（添加主播数量）anchor_num</param>
        /// <param name="num">数量</param>
        /// <returns></returns>
        //public static async Task AddUserProperty(int type, int num)
        //{
        //    // 构造请求体参数
        //    JObject bodyJsonObject = new JObject();
        //    bodyJsonObject["type"] = type;
        //    bodyJsonObject["num"] = num;
        //    string body = bodyJsonObject.ToString();
        //    var request = new HttpRequestMessage();
        //    request.Method = HttpMethod.Post;
        //    request.Headers.Add("token", ReplayHttpUtils.Token);
        //    request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/userproperty/updateUserPropertyDetails");
        //    request.Content = new StringContent(body, Encoding.UTF8, "application/json");

        //    using(HttpClient Client = new HttpClient())
        //    var response = Client.SendAsync(request).Result;
        //    if (!response.IsSuccessStatusCode)
        //    {
        //        FileUtils.LogAnalysis($"加回用户的资产请求失败： {response}.");
        //    }
        //}

        /// <summary>
        /// 加回用户的资产
        /// </summary>
        /// <param name="code">资源类型 aiAnalysisTime :ai语音分析时长;  videoTaggingTime :视频标注时长; textTaggingWordCount :文本标注字数
        /// monitorNum :监控位;  anchorNum :可添加主播数;  storageNum :空间容量
        /// subAccountCount :拥有子账号数量;  child_monitorNum :子账号监控位;  child_anchorNum :子账号可添加主播数</param>
        /// <param name="num">数量</param>
        /// <returns></returns>
        public static void AddUserProperty(string code, int num)
        {
            UserPropertyApi.useProperty(code, num);

            // 构造请求体参数
            //JObject bodyJsonObject = new JObject();
            //bodyJsonObject["code"] = code;
            //bodyJsonObject["num"] = num;
            //string body = bodyJsonObject.ToString();
            //var request = new HttpRequestMessage();
            //request.Method = HttpMethod.Post;
            //request.Headers.Add("token", ReplayHttpUtils.Token);
            //request.RequestUri = new Uri(ReplayHttpUtils.BaseUrl + "/openapi/userproperty/addProperty");
            //request.Content = new StringContent(body, Encoding.UTF8, "application/json");

            //using(HttpClient Client = new HttpClient())
            //{
            //    var response = Client.SendAsync(request).Result;
            //    if (!response.IsSuccessStatusCode)
            //    {
            //        FileUtils.LogAnalysis($"加回用户的资产请求失败： {response}.");
            //    }
            //}
            
        }

        /// <summary>
        /// 判断用户是否可用添加当前主播
        /// </summary>
        /// <param name="urls"></param>
        /// <returns></returns>
        /// <exception cref="NotImplementedException"></exception>
        public static bool AllowAddAnchor(string secUid)
        {

            var urlBuilder = new UriBuilder(ReplayHttpUtils.BaseUrl + "/anchorurl/seletBySerId")
            {
                Query = $"secUidS={secUid}"
            };

            var request = new HttpRequestMessage(HttpMethod.Get, urlBuilder.Uri)
            {
                Headers =
                {
                    { "token", ReplayHttpUtils.Token }
                }
            };
            try
            {
                using(HttpClient Client = new HttpClient())
                {
                    Client.Timeout = TimeSpan.FromSeconds(15);
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.LogAnalysis($"判断用户是否可用添加当前主播请求失败： {response}.");
                        return false;
                    }

                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                    Boolean allowAdd = jsonObject.data;

                    return allowAdd;
                }
                
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"判断用户是否可用添加当前主播请求失败： {ex}.");
                return false;
            }
        }
    }
}
