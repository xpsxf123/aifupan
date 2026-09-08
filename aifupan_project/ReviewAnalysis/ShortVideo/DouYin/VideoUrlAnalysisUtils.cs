using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using CefSharp.DevTools.Debugger;
using douyin.Utils;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.ShortVideo.DouYin
{
    public class VideoUrlAnalysisUtils
    {

        public static string VideoIdByUrl(string url)
        {
            string vidoId = "";
            // 判断是否是一个正常的url
            if (ValidateVideoUrl(url))
            {
                // 是正常的url
                // 获取url中的modal_id
                vidoId = ShortVideoUtils.getModalId(url);
            }
            else
            {
                // 不是正常的url，是分享的链接
                var urlPatterns = new[]
                {
                    @"https://v\.douyin\.com/[A-Za-z0-9_-]+/?",
                    @"v\.douyin\.com/[A-Za-z0-9_-]+/?",
                    @"https?://v\.douyin\.com/\S+?(?=\s|$|\))"
                };

                string httpUrl = Extract(urlPatterns, url);
                if (string.IsNullOrEmpty(httpUrl))
                {
                    throw new CustomException("没有找到完整的URL链接，请检查视频链接");
                }

                vidoId = videoIdByShareUrl(httpUrl.Trim());
                
            }
            if (string.IsNullOrEmpty(vidoId))
            {
                throw new CustomException("获取视频失败，请检查抖音视频是否存在或视频链接是否正确");
            }
            return $"https://www.douyin.com/video/{vidoId}";
        }



        public static string videoIdByShareUrl(string url)
        {
            // 创建随机数实例（推荐将 Random 定义为类成员，避免短时间多次调用导致重复）
            Random random = new Random();
            int version = random.Next(130, 140);

            string location = null;

            // 设置不自动重定向
            using (var handler = new HttpClientHandler { AllowAutoRedirect = false })
            using (var httpClient = new HttpClient(handler))
            {
                // 设置请求头
                httpClient.DefaultRequestHeaders.Add("user-agent", $"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/{version}.0.0.0 Safari/537.36");
                httpClient.DefaultRequestHeaders.Add("accept", "*/*");
                httpClient.DefaultRequestHeaders.Add("accept-language", "zh-CN,zh;q=0.9");
                httpClient.DefaultRequestHeaders.Add("sec-ch-ua", $"\"Chromium\";v=\"{version}\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"");
                httpClient.DefaultRequestHeaders.Add("sec-ch-ua-mobile", "?0");
                httpClient.DefaultRequestHeaders.Add("sec-ch-ua-platform", "\"Windows\"");

                try
                {
                    // 发送GET请求
                    using (HttpResponseMessage response = httpClient.GetAsync(url).Result)
                    {
                        // 获取响应头信息
                        foreach (var header in response.Headers)
                        {
                            if ("LOCATION".Equals(header.Key?.ToString().ToUpper() ?? "0") && header.Value != null && header.Value.Count() > 0)
                            {
                                location = header.Value?.First() ?? "";
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log($"请求失败: {ex.Message}");
                }
            }
            if (string.IsNullOrEmpty(location))
            {
                return null;
            }

            // 主要正则模式
            var patterns = new[]
            {
                @"(?:https?://)?(?:www\.)?iesdouyin\.com/share/video/(\d+)(?:/|\?|&|$)",
                @"/share/video/(\d+)",
                @"/video/(\d+)",
                @"video/(\d+)"
            };

            string urlTemp = Extract(patterns, location);

            if (string.IsNullOrEmpty(urlTemp))
            {
                return null;
            }

            if (urlTemp.EndsWith("/"))
            {
                urlTemp = urlTemp.Substring(0, urlTemp.Length - 1);
            }

            string[] split = urlTemp.Split('/');

            return split[split.Length - 1];

        }

        /// <summary>
        /// 验证视频URL是否正常
        /// </summary>
        /// <param name="videoUrl">视频URL</param>
        private static bool ValidateVideoUrl(string videoUrl)
        {
            try
            {
                // 验证URL格式
                if (!Uri.TryCreate(videoUrl, UriKind.Absolute, out Uri uri))
                {
                    return false;
                }

                // 验证协议
                if (uri.Scheme != "http" && uri.Scheme != "https")
                {
                    return false;
                }

                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"URL验证失败: {videoUrl}, 错误: {ex.Message}", "ValidateVideoUrl");
                throw new CustomException($"URL验证失败: {ex.Message}");
            }
            return false;
        }


        /// <summary>
        /// 正则表达式匹配
        /// </summary>
        public static string Extract(string[] patterns, string url)
        {
            if (string.IsNullOrWhiteSpace(url))
                return null;

            foreach (string pattern in patterns)
            {
                var match = Regex.Match(url, pattern, RegexOptions.IgnoreCase);
                if (match.Success)
                {
                    return match.Groups[0].Value;
                }
            }

            return null;
        }



    }
}
