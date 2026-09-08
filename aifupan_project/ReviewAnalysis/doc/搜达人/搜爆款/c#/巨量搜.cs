using System;
using System.Net.Http;
using System.Text;
using System.Text.Json;
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
            WriteIndented = true
        };
    }


    /// <summary>
    /// 搜索视频
    /// </summary>
    /// <param name="keyword">搜索关键词</param>
    /// <returns>搜索结果</returns>
    public async Task SearchVideoAsync(string keyword)
    {
        // 创建新的HttpRequestMessage而不是直接修改HttpClient的默认头
        var request = new HttpRequestMessage(HttpMethod.Post, "https://trendinsight.oceanengine.com/api/v2/index/itemQuery");

        // 配置请求头
        request.Headers.Add("accept", "application/json, text/plain, */*");
        request.Headers.Add("accept-language", "zh-CN,zh;q=0.9");
        request.Headers.Add("appsource", "PC");
        request.Headers.Add("cache-control", "no-cache");
        request.Headers.Add("pragma", "no-cache");
        request.Headers.Add("priority", "u=1, i");
        request.Headers.Add("referer", $"https://trendinsight.oceanengine.com/arithmetic-index/videosearch?query={HttpUtility.UrlEncode(keyword)}");
        request.Headers.Add("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Microsoft Edge\";v=\"138\"");
        request.Headers.Add("sec-ch-ua-mobile", "?0");
        request.Headers.Add("sec-ch-ua-platform", "\"Windows\"");
        request.Headers.Add("sec-fetch-dest", "empty");
        request.Headers.Add("sec-fetch-mode", "cors");
        request.Headers.Add("sec-fetch-site", "same-origin");
        request.Headers.Add("x-secsdk-csrf-token", "DOWNGRADE");

        // 构建请求数据
        var requestData = new
        {
            query = keyword,
            authorIds = new string[0],
            categoryId = "0",
            dateType = 0,
            labelType = 0,
            durationType = 0,
            total = "150"
        };

        // 设置请求内容
        var jsonContent = JsonSerializer.Serialize(requestData);
        request.Content = new StringContent(jsonContent, Encoding.UTF8, "application/json");

        // 发送请求
        var response = await _httpClient.SendAsync(request);
        var responseText = await response.Content.ReadAsStringAsync();

        if (responseText.Length > 1000)
        {
            var jsonData = JsonSerializer.Deserialize<Dictionary<string, object>>(responseText);
            if (jsonData.ContainsKey("status") && jsonData["status"].ToString() == "0")
            {
                var data = JsonSerializer.Deserialize<Dictionary<string, object>>(jsonData["data"].ToString());
                var items = JsonSerializer.Deserialize<List<Dictionary<string, object>>>(data["data"].ToString());

                foreach (var item in items.GetRange(0, Math.Min(150, items.Count)))
                {
                    Console.WriteLine(new string('/', 100));
                    if (item.ContainsKey("itemId"))
                    {
                        // 输出结果
                        Console.WriteLine(JsonSerializer.Serialize(item, JsonSettings.Default));

                 
                    }
                }
            }
        }
        else
        {
            Console.WriteLine(responseText);
        }
    }

    public static async Task Main(string[] args)
    {
        var scraper = new DouyinScraper();
        string keyword = "旺仔小乔";
        await scraper.SearchVideoAsync(keyword);
    }
}
