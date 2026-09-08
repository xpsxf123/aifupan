using douyin.Utils;
using Microsoft.AspNetCore.WebUtilities;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Global;
using ReviewAnalysis.Model;
using ReviewAnalysis.WeChatChannels.Common;
using ReviewAnalysis.WeChatChannels.Models;
using ReviewAnalysis.WeChatChannels.Utils;
using System;
using System.Collections.Generic;
using System.ComponentModel.Composition.Primitives;
using System.IO;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.WeChatChannels.Services
{
    /// <summary>
    /// 微信视频号服务类
    /// </summary>
    public static class WeChatChannelsSerivce
    {
        
        private static AuthorizationResultModel _latelyAuthorizationResult;
        private static DateTime _latelyAuthorizationDateTime;
        /// <summary>
        /// 锁对象
        /// </summary>
        private static readonly SemaphoreSlim _semaphore = new SemaphoreSlim(1, 1);

        /// <summary>
        /// 获得直播信息数据
        /// </summary>
        /// <param name="authorizerInfoId">微信直播号后端服务的授权 Id</param>
        public static async Task<LiveStatusModel> QueryLiveInfo(int authorizerInfoId)
        {
            var url = "/api/user-channel/live-info";
            var requestParams = new Dictionary<string, string>()
            {
                { "authorizerInfoId", authorizerInfoId.ToString() }
            };

            var result = await SendAsync(url, HttpMethod.Get, requestParams);
            FileUtils.LogRecrd($"{result.Data}", $"通过secUid查询视频号直播信息");
            if (result.Code != 0)
            {
                // 记录异常，返回默认值。
                FileUtils.LogRecrd($"Code:{result.Code}, Message:{result.Message}", $"获取视频号直播信息数据异常，视频号授权 Id：{authorizerInfoId}");
                
                return new LiveStatusModel
                {
                    AuthorizerInfoId = authorizerInfoId,
                    LiveStatus = 0,
                    OpenLive = false,
                    Live = new WeChatChannelsLiveStream()
                };
            }

            var model = JsonConvert.DeserializeObject<LiveStatusModel>(result.Data);
            if (model.Live == null)
            {
                model.Live = new WeChatChannelsLiveStream();
            }

            if (!string.IsNullOrWhiteSpace(model.Live.PullStream))
            {
                model.Live.PullStream = ConvertToM3u8Format(model.Live.PullStream);
            }

            return model;
        }

        /// <summary>
        /// 将直播流的地址转换成 m3u8 格式
        /// </summary>
        /// <param name="url">直播流地址</param>
        private static string ConvertToM3u8Format(string url)
        {
            if (string.IsNullOrEmpty(url))
            {
                return url;
            }

            var currentUri = new Uri(url);
            var streamExtension = Path.GetExtension(currentUri.LocalPath);
            if (streamExtension.Equals(".m3u8", StringComparison.OrdinalIgnoreCase))
            {
                return url;
            }

            var flvExtensionLocalPath = Path.ChangeExtension(currentUri.LocalPath, "m3u8");
            Uri flvStreamUri = new Uri($"{currentUri.Scheme}://{currentUri.Host}{flvExtensionLocalPath}{currentUri.Query}");

            return flvStreamUri.ToString();
        }

        /// <summary>
        /// 查询直播大屏数据
        /// </summary>
        /// <param name="video">视频信息</param>
        public static async Task<WeChatChannelsDashboardModel> QueryDashboard(VideoEntity video)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(video.secUid);
            ResponseResult result = null;
            try
            {
                
                var url = "/open/channel/dashboard";
                var requestParams = new Dictionary<string, string>()
                {
                    { "exportId", video.batchNumber }
                };
                result = await SendAsync(url, HttpMethod.Post, requestParams);
                if (result.Code != 0 || result.Data == null)
                {
                    
                    FileUtils.LogRecrd(JsonConvert.SerializeObject(result), $"获取视频号直播大屏数据失败==={anchorInfo.AnchorName}");

                    return null;
                }

                return JsonConvert.DeserializeObject<WeChatChannelsDashboardModel>(result.Data);
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"获取视频号直播大屏数据发生异常==={anchorInfo.AnchorName}");
            }
            
            return null;
            
        }

        /// <summary>
        /// 微信视频号解绑
        /// </summary>
        /// <param name="authorizerInfoId">微信视频号授权 Id</param>
        public static async Task UnbindWeChatChannels(int authorizerInfoId)
        {
            var url = "/api/user-channel/unbind";
            var requestParams = new Dictionary<string, string>()
            {
                { "authorizerInfoId", authorizerInfoId.ToString() }
            };

            var result = await SendAsync(url, HttpMethod.Post, requestParams);
            if (result.Code != 0)
            {
                // 解绑失败不需要提示，也不需要异常
                //throw new Exception($"解绑 {authorizerInfoId} 失败：{result.Message}");
            }
        }

        /// <summary>
        /// 创建微信视频号授权页面 URl
        /// </summary>
        /// <param name="redirectUrl">授权成功的返回地址</param>
        public static async Task<string> BuildOpenAuthUrl(string redirectUrl)
        {
            await CheckOAuthToken();

            if (string.IsNullOrEmpty(redirectUrl))
            {
                redirectUrl = string.Empty;
            }

            var url = "/wechat/open-auth";
            var requestUrl = $"{Constant.GetWeChatChannelsSerivceAddress()}{url}";
            var requestParams = new Dictionary<string, string>()
            {
                { "userId", ReplayHttpUtils.UserId },
                { "forceRefresh", string.Empty },
                { "redirect", redirectUrl },
                { "jy-application", WeChatChannelsServiceToken.Token }
            };

            return QueryHelpers.AddQueryString(requestUrl, requestParams);
        }

        /// <summary>
        /// 查询直播心跳
        /// </summary>
        /// <param name="exportId">直播 Id</param>
        public static async Task<LiveHeartbeatModel> QueryLiveHeartbeat(string exportId)
        {
            var url = "/api/user-channel/heartbeat";
            var requestParams = new Dictionary<string, string>()
            {
                { "exportId", exportId }
            };

            var result = await SendAsync(url, HttpMethod.Post, requestParams);
            FileUtils.LogRecrd($"{result.Data}", $"通过直播场次id查询视频号直播信息==={exportId}");
            if (result.Code != 0)
            {
                FileUtils.LogRecrd($"查询微信视频号直播心跳异常，直播Id:{exportId}，Code:{result.Code}, Message:{result.Message}");
            }

            return JsonConvert.DeserializeObject<LiveHeartbeatModel>(result.Data);
        }

        /// <summary>
        /// 已发送请求
        /// </summary>
        /// <param name="url">地址</param>
        /// <param name="method">Http 方式</param>
        /// <param name="requestParams">请求数据字典集合</param>
        public static async Task<ResponseResult> SendAsync(string url, HttpMethod method, Dictionary<string, string> requestParams)
        {
            await CheckOAuthToken();

            var requestUrl = $"{Constant.GetWeChatChannelsSerivceAddress()}{url}";
            var requestMessage = WeChatChannelsHttpUtils.BuildHttpRequestMessageWithSign(requestUrl, method, requestParams);

            var returnData = await WeChatChannelsHttpUtils.SendAsync(requestMessage);
            if (!string.IsNullOrEmpty(returnData))
            {
                var result = ConvertToResponseResult(returnData);
                if (result.Code == 401)
                {
                    // 401 表示 Token 失效。再次刷新 Token，然后再次发送请求。
                    await RefreshOAuthToken();
                    // 重新构建requestMessage
                    requestMessage = WeChatChannelsHttpUtils.BuildHttpRequestMessageWithSign(requestUrl, method, requestParams);
                    returnData = await WeChatChannelsHttpUtils.SendAsync(requestMessage);
                    result = ConvertToResponseResult(returnData);
                }

                return result;
            }

            throw new Exception($"微信视频号后端服务异常：{requestUrl}");
        }

        /// <summary>
        /// 将接口返回的数据转换成 ResponseResult
        /// </summary>
        private static ResponseResult ConvertToResponseResult(string returnData)
        {
            var returnObject = JObject.Parse(returnData);
            var result = new ResponseResult
            {
                Code = int.Parse(returnObject["code"].ToString()),
                Message = returnObject["msg"].ToString()
            };

            if (returnObject["data"] != null)
            {
                result.Data = returnObject["data"].ToString();
            }

            return result;
        }

        /// <summary>
        /// 异步发送请求
        /// </summary>
        /// <param name="requestMessage">请求消息</param>
        private static async Task<ResponseResult> SendAsync(HttpRequestMessage requestMessage)
        {
            var returnData = await WeChatChannelsHttpUtils.SendAsync(requestMessage);
            if (!string.IsNullOrEmpty(returnData))
            {
                var returnObject = JObject.Parse(returnData);
                var result = new ResponseResult
                {
                    Code = int.Parse(returnObject["code"].ToString()),
                    Message = returnObject["msg"].ToString()
                };

                // 401 表示 Token 失效，再次刷新。为避免死循环请求不再次发送。
                if (result.Code == 401)
                {
                    await RefreshOAuthToken();
                }

                if (returnObject["data"] != null)
                {
                    result.Data = returnObject["data"].ToString();
                }

                return result;
            }

            throw new Exception($"微信视频号后端服务异常：{requestMessage.RequestUri}");
        }

        /// <summary>
        /// 检查 Token，失效了就重新获取
        /// </summary>
        private static async Task CheckOAuthToken()
        {
            await _semaphore.WaitAsync();

            try
            {
                if (IsExpiredToken())
                {
                    await RefreshOAuthToken();
                }
            }
            finally
            {
                _semaphore.Release();
            }
        }

        /// <summary>
        /// 获得微信视频号后端服务 Token
        /// </summary>
        private static async Task RefreshOAuthToken()
        {
            var requestUrl = $"{Constant.GetWeChatChannelsSerivceAddress()}/open/oauth";
            var requestMessage = WeChatChannelsHttpUtils.BuildHttpRequestMessageWithGenerateOAuthToken(requestUrl);
            var result = await SendAsync(requestMessage);

            if (result.Code != 0)
            {
                throw new Exception($"获得微信视频号后端服务 Token 失败：{result.Message}");
            }

            var serviceToken = JsonConvert.DeserializeObject<WeChatChannelsServiceTokenModel>(result.Data);
            WeChatChannelsServiceToken.Token = serviceToken.AccessToken;
            WeChatChannelsServiceToken.Secret = serviceToken.Secret;
            WeChatChannelsServiceToken.SecretExpiryTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds() + serviceToken.ExpiresIn - 60;
        }

        /// <summary>
        /// 是否 Token 已过期
        /// </summary>
        public static bool IsExpiredToken()
        {
            return WeChatChannelsServiceToken.SecretExpiryTime < DateTimeOffset.UtcNow.ToUnixTimeSeconds();
        }


        /// <summary>
        /// 获得最近一次的微信直播授权数据
        /// </summary>
        public static AuthorizationResultModel GetLatelyAuthorizationResult()
        {
            return _latelyAuthorizationResult;
        }

        /// <summary>
        /// 设置最近一次的微信直播授权数据
        /// </summary>
        public static void SetLatelyAuthorizationResult(AuthorizationResultModel model)
        {
            _latelyAuthorizationResult = model;
            _latelyAuthorizationDateTime = DateTime.Now;
        }

        /// <summary>
        /// 清除最近一次的微信直播授权数据
        /// </summary>
        public static void ClearLatelyAuthorizationResult()
        {
            _latelyAuthorizationResult = null;
            _latelyAuthorizationDateTime = DateTime.MinValue;
        }
    }
}
