using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web;

public class DouyinScraper
{
    private readonly HttpClient _httpClient;
    
    public DouyinScraper()
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
    

    // 创建 JsonSerializerOptions 配置
    public static class JsonSettings
    {
        public static readonly JsonSerializerOptions Default = new JsonSerializerOptions
        {
            Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping, // 禁用 Unicode 转义
            WriteIndented = true,
            AllowTrailingCommas = true,
            ReadCommentHandling = JsonCommentHandling.Skip
        };
    }


    /// <summary>
    /// 安全地从JsonElement获取长整数值
    /// </summary>
    private long? GetLongValue(JsonElement element)
    {
        if (element.ValueKind == JsonValueKind.Number)
        {
            return element.TryGetInt64(out long value) ? value : null;
        }
        return null;
    }


    /// <summary>
    /// 获取视频详细信息
    /// </summary>
    /// <param name="modalId">视频ID</param>
    /// <returns>视频详细信息</returns>
    public async Task<Dictionary<string, object>> GetVideoDetailAsync(string modalId)
    {
        // 创建新的HttpRequestMessage
        var request = new HttpRequestMessage(HttpMethod.Get, $"https://www.douyin.com/jingxuan?modal_id={modalId}");

        // 配置请求头
        request.Headers.Add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        request.Headers.Add("sec-ch-ua", "\"Microsoft Edge\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"");
        request.Headers.Add("sec-ch-ua-mobile", "?0");
        request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
        request.Headers.Add("upgrade-insecure-requests", "1");
        request.Headers.Add("sec-fetch-site", "none");
        request.Headers.Add("sec-fetch-mode", "navigate");
        request.Headers.Add("sec-fetch-user", "?1");
        request.Headers.Add("sec-fetch-dest", "document");
        request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");
        request.Headers.Add("priority", "u=0, i");

        // 发送请求
        var response = await _httpClient.SendAsync(request);
        var responseText = await response.Content.ReadAsStringAsync();

        // 处理响应文本
        responseText = HttpUtility.UrlDecode(responseText.Replace("\\u0026", "&"));

        // 使用正则表达式提取视频详情信息
        var videoDetailMatch = Regex.Match(responseText, "\"videoDetail\":(.*?),\"lazyLoadConfig\"");
        if (!videoDetailMatch.Success) return null;

        // 解析JSON数据为JsonDocument而非Dictionary，以便更好地处理类型
        using var doc = JsonDocument.Parse(videoDetailMatch.Groups[1].Value);
        var videoDetail = doc.RootElement;
    
        // 构建返回结果
        var result = new Dictionary<string, object>
        {
            ["authorInfo"] = new Dictionary<string, object>
            {
                ["uid"] = videoDetail.TryGetProperty("authorInfo", out var authorInfo) ? 
                    authorInfo.TryGetProperty("uid", out var uid) ? uid.ToString() : null : null,
                ["secUid"] = videoDetail.TryGetProperty("authorInfo", out authorInfo) ? 
                    authorInfo.TryGetProperty("secUid", out var secUid) ? secUid.ToString() : null : null,
                ["nickname"] = videoDetail.TryGetProperty("authorInfo", out authorInfo) ? 
                    authorInfo.TryGetProperty("nickname", out var nickname) ? nickname.ToString() : null : null,
                ["avatarUri"] = videoDetail.TryGetProperty("authorInfo", out authorInfo) ? 
                    authorInfo.TryGetProperty("avatarUri", out var avatarUri) ? avatarUri.ToString() : null : null,
                ["followerCount"] = videoDetail.TryGetProperty("authorInfo", out authorInfo) ? 
                    GetLongValue(authorInfo.GetProperty("followerCount")) : null,
                ["totalFavorited"] = videoDetail.TryGetProperty("authorInfo", out authorInfo) ? 
                    GetLongValue(authorInfo.GetProperty("totalFavorited")) : null,
            },
            ["awemeId"] = videoDetail.TryGetProperty("awemeId", out var awemeId) ? awemeId.ToString() : null,
            ["awemeType"] = videoDetail.TryGetProperty("awemeType", out var awemeType) ? GetLongValue(awemeType) : null,
            ["desc"] = videoDetail.TryGetProperty("desc", out var desc) ? desc.ToString() : null,
            
            ["createTime"] = videoDetail.TryGetProperty("createTime", out var createTime) ? 
                ConvertTimestamp(GetLongValue(createTime)) : null,
            
            ["video"] = new Dictionary<string, object>
            {
                ["dynamicCover"] = videoDetail.TryGetProperty("video", out var video) ? 
                    video.TryGetProperty("dynamicCover", out var dynamicCover) ? dynamicCover.ToString() : null : null,
                ["duration"] = videoDetail.TryGetProperty("video", out video) ? 
                    video.TryGetProperty("duration", out var duration) ? 
                    (GetLongValue(duration) ?? 0) / 1000 : 0 : 0,
                ["dataSize"] = GetDataSizeString(videoDetail.TryGetProperty("video", out video) ? 
                    video.TryGetProperty("dataSize", out var dataSize) ? GetLongValue(dataSize) : null : null),
                ["video_play_addr"] = GetVideoPlayAddr(videoDetail.TryGetProperty("video", out video) ? video : default)
            },
            ["stats"] = new Dictionary<string, object>
            {
                ["commentCount"] = videoDetail.TryGetProperty("stats", out var stats) ? 
                    GetLongValue(stats.GetProperty("commentCount")) : null,
                ["diggCount"] = videoDetail.TryGetProperty("stats", out stats) ? 
                    GetLongValue(stats.GetProperty("diggCount")) : null,
                ["shareCount"] = videoDetail.TryGetProperty("stats", out stats) ? 
                    GetLongValue(stats.GetProperty("shareCount")) : null,
                ["collectCount"] = videoDetail.TryGetProperty("stats", out stats) ? 
                    GetLongValue(stats.GetProperty("collectCount")) : null,
                ["recommendCount"] = videoDetail.TryGetProperty("stats", out stats) ? 
                    GetLongValue(stats.GetProperty("recommendCount")) : null,
            }
        };

        Console.WriteLine(JsonSerializer.Serialize(result, JsonSettings.Default));
        return result;
    }

    /// <summary>
    /// 获取视频播放地址
    /// </summary>
    private string GetVideoPlayAddr(JsonElement video)
    {
        if (video.ValueKind == JsonValueKind.Undefined)
            return null;
            
        if (video.TryGetProperty("playAddr", out var playAddr) && playAddr.ValueKind == JsonValueKind.Array)
        {
            foreach (var addr in playAddr.EnumerateArray())
            {
                if (addr.TryGetProperty("src", out var src) && src.ToString().Contains("v3-web.douyinvod.com"))
                {
                    return src.ToString();
                }
            }
        }
        return null;
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

    public static async Task Main(string[] args)
    {
        var scraper = new DouyinScraper();
        string modal_id = "7531331104026201384"; // 视频id
        await scraper.GetVideoDetailAsync(modal_id);
    }
}
