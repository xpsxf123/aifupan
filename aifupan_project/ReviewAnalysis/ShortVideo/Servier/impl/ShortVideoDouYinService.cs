using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Newtonsoft.Json;
using System.Web;
using ReviewAnalysis.ShortVideo.Model;
using CefSharp.DevTools.IO;
using ReviewAnalysis.Utils;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.ShortVideo.DouYin.dto;

namespace ReviewAnalysis.ShortVideo.Servier
{
    public class ShortVideoDouYinService : ShortVideoService
    {

        private readonly HttpClient _httpClient;

        public ShortVideoDouYinService()
        {
            // 初始化 HttpClient 并配置默认请求头
            _httpClient = new HttpClient();
            _httpClient.DefaultRequestHeaders.Add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36 Edg/137.0.0.0");
        }

        /// <summary>
        /// 将 Unix 时间戳转换为日期字符串
        /// </summary>
        /// <param name="timestamp">Unix 时间戳</param>
        /// <returns>格式化后的日期字符串</returns>
        private string ConvertTimestamp(long? timestamp)
        {
            if (timestamp == null) return null;

            DateTimeOffset dateTimeOffset = DateTimeOffset.FromUnixTimeSeconds(timestamp.Value);
            return dateTimeOffset.ToString("yyyy-MM-dd HH:mm:ss");
        }

        /// <summary>
        /// 安全地从JsonElement获取长整数值
        /// </summary>
        //private long? GetLongValue(JsonElement element)
        //{
        //    if (element.ValueKind == JsonValueKind.Number)
        //    {
        //        return element.TryGetInt64(out long value) ? value : null;
        //    }
        //    return null;
        //}


        /// <summary>
        /// 获取视频详细信息
        /// </summary>
        /// <param name="modalId">视频ID</param>
        /// <returns>视频详细信息</returns>
        public DouYinVideoInfo GetVideoDetailAsync(string url)
        {
            if (string.IsNullOrEmpty(url))
            {
                throw new CustomException("视频的URL为空");
            }

            // 获取url中的modal_id
            string modalId = ShortVideoUtils.getModalId(url);

            if (string.IsNullOrEmpty(modalId))
            {
                throw new CustomException("抖音短视频url不正确");
            }

            // 创建新的HttpRequestMessage
            var request = new HttpRequestMessage(HttpMethod.Get, $"https://www.douyin.com/aweme/v1/web/aweme/detail/?aid=6383&msToken=&aweme_id={modalId}");

            // 配置请求头
            request.Headers.Add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            request.Headers.Add("Accept", "application/json, text/plain, */*");
            request.Headers.Add("sec-ch-ua", "\"Google Chrome\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"");
            request.Headers.Add("sec-ch-ua-mobile", "?0");
            request.Headers.Add("sec-ch-ua-platform", "\"Linux\"");
            request.Headers.Add("origin", "https://open.douyin.com");
            request.Headers.Add("sec-fetch-site", "same-site");
            request.Headers.Add("sec-fetch-mode", "cors");
            request.Headers.Add("sec-fetch-dest", "empty");
            request.Headers.Add("referer", "https://open.douyin.com/");
            request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");

            // 发送请求

            var response = _httpClient.SendAsync(request).Result;

            var responseText = response.Content.ReadAsStringAsync().Result;

            if (responseText == null || string.IsNullOrEmpty(responseText))
            {
                return null;
            }

            // 解析JSON数据为JsonDocument而非Dictionary，以便更好地处理类型
            dynamic videoDetail = JsonConvert.DeserializeObject<dynamic>(responseText);

            if (videoDetail == null || videoDetail.aweme_detail == null)
            {
                throw new CustomException("视频找不到了！");
            }
            
            // 构建返回结果
            DouYinVideoInfo result = JsonConvert.DeserializeObject<DouYinVideoInfo>(JsonConvert.SerializeObject(videoDetail.aweme_detail));
            
            // 处理视频播放地址
            GetVideoPlayAddr(result);
            if ((result?.video?.duration ?? 0) > 0)
            {
                result.video.duration = (int)ShortVideoUtils.formatDuration(result?.video?.duration ?? 0);
            }

            return result;
        }

        /// <summary>
        /// 获取视频播放地址
        /// </summary>
        private void GetVideoPlayAddr(DouYinVideoInfo result)
        {
            if (result?.video?.play_addr?.url_list == null || result.video.play_addr.url_list.Count == 0)
            {
                return;
            }

            List<string> urlList = result.video.play_addr.url_list;
            List<string> temp = new List<string>();
            for (int i = 0; i < urlList.Count; i++)
            {
                string url = urlList[i];
                if (!string.IsNullOrEmpty(url) && (url.Contains("www.douyin.com")))
                {
                    temp.Add(url);
                }
            }
            result.video.play_addr.url_list = temp;
        }

        /// <summary>
        /// 格式化数据大小显示
        /// </summary>
        private string GetDataSizeString(long? dataSize)
        {
            if (!dataSize.HasValue)
                return "0MB";

            return dataSize > 1024 * 1024 ?
                $"{(dataSize / 1024.0 / 1024.0):F2}MB" :
                $"{dataSize / 1024.0:F2}KB";
        }

    }
}
