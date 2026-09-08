using COSXML.Network;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.user;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class UserApi
    {

        /// <summary>
        /// 获取当前登录的用户信息-同步
        /// </summary>
        /// <returns></returns>
        public static Asr.UserVo GetUserLoginInfo()
        {
            try
            {
                string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/user/getUserByToken", null);

                if (!string.IsNullOrEmpty(dataStr))
                {
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                    };
                    Asr.UserVo userVo = JsonConvert.DeserializeObject<Asr.UserVo>(dataStr, settings);

                    return userVo;
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"获取当前登录的用户信息发生异常");
            }
            

            return null;
        }


        /// <summary>
        /// 获取当前用户详细信息
        /// </summary>
        /// <returns></returns>
        public static async Task<UserInfoVo> GetUserInfoVo()
        {
            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/user/infoByClient", null);
            if (!string.IsNullOrEmpty(dataStr))
            {
                UserInfoVo userInfoVo = JsonConvert.DeserializeObject<UserInfoVo>(dataStr);

                return userInfoVo;
            }

            return null;
        }
        /// <summary>
        /// 获取当前用户详细信息-同步
        /// </summary>
        /// <returns></returns>
        public static UserInfoVo GetUserInfoVoSync()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/user/infoByClient", null);
            if (!string.IsNullOrEmpty(dataStr))
            {
                UserInfoVo userInfoVo = JsonConvert.DeserializeObject<UserInfoVo>(dataStr);

                return userInfoVo;
            }

            return null;
        }

        /// <summary>
        /// 退出登录
        /// </summary>
        public static void logout()
        {
            HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/user/logout", null);
        }

        /// <summary>
        /// 退出登录-超时时间为1.5秒
        /// </summary>
        public static void logoutTimeout()
        {
            if (string.IsNullOrEmpty(ReplayHttpUtils.Token))
            {
                return;
            }
            using (var client = new System.Net.Http.HttpClient())
            {
                // 设置超时时间为 1.5 秒
                client.Timeout = TimeSpan.FromMilliseconds(1500); // 1500ms = 1.5s

                try
                {
                    // 构建请求头
                    var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, ReplayHttpUtils.BaseUrl + "/user/logout")
                    {
                        Headers =
                        {
                            { "token", ReplayHttpUtils.Token },
                        }
                    };
                    SignatureHeaders.AddSignature(httpRequestMessage);
                    using (HttpResponseMessage response = client.SendAsync(httpRequestMessage).Result)
                    {
                        if (response.IsSuccessStatusCode)
                        {
                            // 通讯成功 
                            string str = response.Content.ReadAsStringAsync().Result;
                        }
                        else
                        {
                            // 记录非成功状态码的日志 
                            FileUtils.LogError($"请求失败，状态码: {response.StatusCode}", $"异步发送GET请求发生异常");
                        }
                    }
                }
                catch (TaskCanceledException ex) when (ex.InnerException is TimeoutException)
                {
                    FileUtils.log("请求超时：超过 1.5 秒未收到响应。");
                }
                catch (HttpRequestException ex)
                {
                    FileUtils.log($"请求失败: {ex.Message}");
                }
            }
        }
        
        /// <summary>
        /// 客户端登录后的接口
        /// </summary>
        /// <returns></returns>
        public static bool clientLogoPost(Dictionary<string, object> param)
        {
            HttpUtils.R res = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/openapi/user/clientLogoPost", param);
            bool result = false;
            if (res.success() && !string.IsNullOrEmpty(res.data))
            {
                bool.TryParse(res.data, out result);
            }

            return result;
        }

        /// <summary>
        /// 上报电脑硬件配置到服务端（fire-and-forget，异常只记日志）。
        /// </summary>
        public static void reportComputerConfig(Dictionary<string, object> param)
        {
            try
            {
                HttpUtils.SendServerPost(
                    ReplayHttpUtils.BaseUrl + "/openapi/user/saveComputerConfig",
                    param);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "reportComputerConfig failed");
            }
        }


    }
}
