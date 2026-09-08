using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using ReviewAnalysis.Model;
using douyin.Utils;

namespace ReviewAnalysis.plugins.dataPullers
{
    /// <summary>
    /// 数据拉取器基类
    /// 职责：封装通用逻辑（Cookie获取、API调用、重试机制）
    /// </summary>
    public abstract class BaseDataPuller
    {
        protected readonly string _platformId;
        protected readonly string _platformName;

        public BaseDataPuller(string platformId, string platformName)
        {
            _platformId = platformId;
            _platformName = platformName;
        }

        /// <summary>
        /// 拉取数据（子类实现具体逻辑）
        /// </summary>
        public abstract Task<string> PullDataAsync(AnchorInfo anchorInfo, string roomId, string videoId);

        /// <summary>
        /// 获取Cookie（子类实现）
        /// </summary>
        protected abstract Dictionary<string, string> GetCookies(string secUid);

        /// <summary>
        /// 构建Cookie字符串
        /// </summary>
        protected string BuildCookieString(Dictionary<string, string> cookies)
        {
            if (cookies == null || cookies.Count == 0) return string.Empty;
            return string.Join("; ", cookies.Select(kv => $"{kv.Key}={kv.Value}"));
        }

        /// <summary>
        /// 带重试的API调用
        /// </summary>
        protected async Task<string> CallApiWithRetry(Func<Task<string>> apiCall, int maxRetry = 3)
        {
            for (int i = 0; i < maxRetry; i++)
            {
                try
                {
                    var result = await apiCall();
                    if (!string.IsNullOrEmpty(result)) return result;
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"API调用失败（第{i + 1}次）: {ex.Message}", _platformName);
                    if (i < maxRetry - 1) await Task.Delay(1000 * (i + 1));
                }
            }
            return null;
        }

        /// <summary>
        /// 带重试的API调用（泛型版本）
        /// </summary>
        protected async Task<T> CallApiWithRetry<T>(Func<Task<T>> apiCall, int maxRetry = 3) where T : class
        {
            for (int i = 0; i < maxRetry; i++)
            {
                try
                {
                    var result = await apiCall();
                    if (result != null) return result;
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"API调用失败（第{i + 1}次）: {ex.Message}", _platformName);
                    if (i < maxRetry - 1) await Task.Delay(1000 * (i + 1));
                }
            }
            return null;
        }
    }
}
