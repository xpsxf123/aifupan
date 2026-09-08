using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;
using System.Web;

using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;



public class XGorgon
{
    private readonly int length = 20;
    private readonly byte[] hexStr = { 30, 64, 224, 217, 147, 69, 0, 180 };

    private byte[] Encryption()
    {
        byte[] hexZu = Enumerable.Range(0, 256).Select(i => (byte)i).ToArray();
        object tmp = null; // 用 object 模拟 Python 的 '' 或 None

        for (int i = 0; i < 256; i++)
        {
            int A;
            if (i == 0)
            {
                A = 0;
            }
            else if (tmp != null)
            {
                A = (int)tmp;
            }
            else
            {
                A = hexZu[i - 1];
            }

            byte B = hexStr[i % 8];

            // 特殊处理 A == 85
            if (A == 85 && i != 1 && (tmp == null || (int)tmp != 85))
            {
                A = 0;
            }
            int C = A + i + B;
            while (C >= 256) C -= 256;

            // 更新 tmp
            tmp = C < i ? (object)C : null;

            hexZu[i] = hexZu[C];
        }

        return hexZu;
    }

    private byte[] Initialize(byte[] inputData, byte[] hexZu)
    {
        var tmpAdd = new List<int>();
        byte[] tmpHex = (byte[])hexZu.Clone(); // 深拷贝

        for (int i = 0; i < length; i++)
        {
            byte A = inputData[i];
            int B = tmpAdd.Count > 0 ? tmpAdd[tmpAdd.Count - 1] : 0;

            int C = hexZu[i + 1] + B; // 注意：这里用的是原始 hexZu[i+1]，不是 tmpHex
            while (C >= 256) C -= 256;

            tmpAdd.Add(C);
            byte D = tmpHex[C];
            tmpHex[i + 1] = D; // 修改 tmpHex

            int E = D + D;
            while (E >= 256) E -= 256;

            byte F = tmpHex[E]; // 使用修改后的 tmpHex
            inputData[i] = (byte)(A ^ F);
        }

        return inputData;
    }


    private byte Reverse(byte num)
    {
        string hex = num.ToString("x2");
        // 交换两个字符：ab -> ba，将 char 转为 string 后拼接
        string reversed = hex[1].ToString() + hex[0].ToString(); 
        return Convert.ToByte(reversed, 16);
    }

    private byte RBIT(byte num)
    {
        string bin = Convert.ToString(num, 2).PadLeft(8, '0');
        char[] chars = bin.ToCharArray();
        Array.Reverse(chars);
        string reversed = new string(chars);
        return Convert.ToByte(reversed, 2);
    }

    private byte[] Handle(byte[] inputData)
    {
        byte[] data = (byte[])inputData.Clone();

        for (int i = 0; i < length; i++)
        {
            byte A = data[i];
            byte B = Reverse(A);
            byte C = data[(i + 1) % length];
            byte D = (byte)(B ^ C);
            byte E = RBIT(D);
            byte F = (byte)(E ^ length);
            uint G = (uint)(~F); // ✅ 用 uint 自动处理补码

            byte H = (byte)(G & 0xFF);
            data[i] = H;
        }

        return data;
    }

    private string Hex2String(byte num)
    {
        return num.ToString("x2");
    }

    private string Main(byte[] gorgon)
    {
        byte[] processed = Handle(Initialize(gorgon, Encryption()));
        string result = string.Concat(processed.Select(Hex2String));

        string prefix = $"0401{Hex2String(hexStr[7])}{Hex2String(hexStr[3])}{Hex2String(hexStr[1])}{Hex2String(hexStr[6])}";
        return prefix + result;
    }

    public Dictionary<string, string> Calculate(string paramsStr, Dictionary<string, string> headers = null)
    {
        headers ??= new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        byte[] gorgon = new byte[20];

        long timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;
        string khronosHex = timestamp.ToString("x");

        // 补齐为偶数长度
        if (khronosHex.Length % 2 != 0)
            khronosHex = "0" + khronosHex;

        // 1. URL MD5 前4字节
        byte[] urlMd5 = MD5.HashData(Encoding.UTF8.GetBytes(paramsStr));
        Array.Copy(urlMd5, 0, gorgon, 0, 4);

        // 2. x-ss-stub 前4字节
        if (headers.TryGetValue("x-ss-stub", out string dataMd5) && dataMd5.Length >= 8)
        {
            byte[] dataBytes = StringToByteArray(dataMd5.Substring(0, 8));
            Array.Copy(dataBytes, 0, gorgon, 4, 4);
        }
        else
        {
            Array.Clear(gorgon, 4, 4);
        }

        // 3. cookie MD5 前4字节
        if (headers.TryGetValue("cookie", out string cookie))
        {
            byte[] cookieMd5 = MD5.HashData(Encoding.UTF8.GetBytes(cookie));
            Array.Copy(cookieMd5, 0, gorgon, 8, 4);
        }
        else
        {
            Array.Clear(gorgon, 8, 4);
        }

        // 4. 中间4字节填0
        Array.Clear(gorgon, 12, 4);

        // 5. 时间戳：从左到右填充4字节（注意顺序）
        byte[] khronosBytes = StringToByteArray(khronosHex.PadLeft(8, '0')); // 补到8位hex（4字节）
        if (khronosBytes.Length >= 4)
        {
            Array.Copy(khronosBytes, khronosBytes.Length - 4, gorgon, 16, 4); // 低4字节放最后
        }
        else
        {
            Array.Copy(khronosBytes, 0, gorgon, 20 - khronosBytes.Length, khronosBytes.Length);
        }

        string xGorgon = Main(gorgon);
        string xKhronos = timestamp.ToString();

        return new Dictionary<string, string>
        {
            { "X-Gorgon", xGorgon },
            { "X-Khronos", xKhronos }
        };
    }

    private static byte[] StringToByteArray(string hex)
    {
        return Enumerable.Range(0, hex.Length / 2)
            .Select(i => Convert.ToByte(hex.Substring(i * 2, 2), 16))
            .ToArray();
    }

    // public static void Main(string[] args)
    // {
    //     string paramsStr = "manifest_version_code=100001&_rticket=1754537817617&app_type=normal&iid=174403850931673&channel=huawei&device_type=22127RK46C&language=zh&resolution=900*1600&openudid=d9d028ddfdd75301&update_version_code=10009900&cdid=85f877fb-5056-4cc1-a6f7-51671e84bde7&os_api=28&dpi=240&ac=wifi&device_id=120557179413229&mcc_mnc=46000&os_version=9&version_code=100000&app_name=douyin_lite&version_name=10.0.0&device_brand=Redmi&ssmix=a&device_platform=android&aid=2329&ts=1754537817";

    //     var xgorgon = new XGorgon();
    //     var result = xgorgon.Calculate(paramsStr);

    //     Console.WriteLine("生成的签名信息:");
    //     Console.WriteLine($"X-Gorgon: {result["X-Gorgon"]}");
    //     Console.WriteLine($"X-Khronos: {result["X-Khronos"]}");
    // }
}



public class DouyinAppSpider
{
    private readonly string _keyword;
    private readonly HttpClient _httpClient;

    public DouyinAppSpider(string keyword)
    {
        _keyword = keyword;
        var handler = new HttpClientHandler
        {
            AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate,
            UseCookies = true,
            AllowAutoRedirect = true
        };
        _httpClient = new HttpClient(handler);
        _httpClient.Timeout = TimeSpan.FromSeconds(30);
    }

    public static class JsonSettings
    {
        public static readonly JsonSerializerOptions Default = new JsonSerializerOptions
        {
            Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping,
            WriteIndented = true,
            AllowTrailingCommas = true,
            ReadCommentHandling = JsonCommentHandling.Skip
        };
    }

    private string ConvertTimestamp(long? timestamp)
    {
        if (timestamp.HasValue)
        {
            return DateTimeOffset.FromUnixTimeSeconds(timestamp.Value)
                .LocalDateTime.ToString("yyyy-MM-dd HH:mm:ss");
        }
        return null;
    }

    private string GenerateId()
    {
        var random = new Random();
        byte[] buffer = new byte[8]; // long 占 8 字节
        random.NextBytes(buffer);
        long num = BitConverter.ToInt64(buffer, 0);
        // 调整到你要的大致范围（若需精准 100000000000000 - 999999999999999，可额外处理）
        num = Math.Abs(num) % 900000000000000 + 100000000000000;
        return num.ToString();
    }

    private string GenerateCdid()
    {
        return Guid.NewGuid().ToString();
    }

    private string GenerateOpenUdid()
    {
        var random = new Random();
        var bytes = new byte[8];
        random.NextBytes(bytes);
        return string.Join("", bytes.Select(b => b.ToString("x2")));
    }

    private Dictionary<string, object> ProcessAwemeItem(JsonElement awemeInfo)
    {
        var result = new Dictionary<string, object>();

        result["aweme_id"] = GetJsonString(awemeInfo, "aweme_id");
        result["desc"] = GetJsonString(awemeInfo, "desc");

        if (awemeInfo.TryGetProperty("create_time", out var createTime))
        {
            result["create_time"] = ConvertTimestamp(createTime.GetInt64());
        }

        result["author"] = ProcessAuthorInfo(awemeInfo);
        result["video"] = ProcessVideoInfo(awemeInfo);
        result["music"] = ProcessMusicInfo(awemeInfo);
        result["statistics"] = ProcessStatistics(awemeInfo);

        // 先判断是否存在images字段
        if (awemeInfo.TryGetProperty("images", out var images))
        {
            // 再检查字段值是否为数组（有效图片数据）
            if (images.ValueKind == JsonValueKind.Array)
            {
                result["aweme_type"] = "note";
                result["aweme_url"] = $"https://www.douyin.com/note/{result["aweme_id"]}";
                result["images"] = ProcessImageList(images);
            }
            // 处理值为null的情况（视为无图片，按视频处理）
            else if (images.ValueKind == JsonValueKind.Null)
            {
                result["aweme_type"] = "video";
                result["aweme_url"] = $"https://www.douyin.com/video/{result["aweme_id"]}";
                result["images"] = null;
            }
            // 处理其他异常类型（如字符串、数字等非数组/非null类型）
            else
            {
                // 可根据业务需求选择抛异常或按默认类型处理
                Console.WriteLine($"images字段类型异常，实际类型：{images.ValueKind}，aweme_id：{result["aweme_id"]}");
                result["aweme_type"] = "video"; // 默认按视频处理
                result["aweme_url"] = $"https://www.douyin.com/video/{result["aweme_id"]}";
                result["images"] = null;
            }
        }
        // 字段不存在的情况（直接按视频处理）
        else
        {
            result["aweme_type"] = "video";
            result["aweme_url"] = $"https://www.douyin.com/video/{result["aweme_id"]}";
            result["images"] = null;
        }

        return result;
    }

    private Dictionary<string, object> ProcessAuthorInfo(JsonElement awemeInfo)
    {
        var author = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("author", out var authorProp))
            return author;

        author["uid"] = GetJsonString(authorProp, "uid");
        author["nickname"] = GetJsonString(authorProp, "nickname");
        author["short_id"] = GetJsonString(authorProp, "short_id");
        author["unique_id"] = GetJsonString(authorProp, "unique_id");
        author["signature"] = GetJsonString(authorProp, "signature");
        author["sec_uid"] = GetJsonString(authorProp, "sec_uid");

        if (authorProp.TryGetProperty("avatar_thumb", out var avatar) &&
            avatar.TryGetProperty("url_list", out var urlList) &&
            urlList.GetArrayLength() > 0)
        {
            author["avatar_thumb"] = urlList[0].GetString();
        }

        author["aweme_count"] = GetJsonInt(authorProp, "aweme_count");
        author["following_count"] = GetJsonInt(authorProp, "following_count");
        author["follower_count"] = GetJsonInt(authorProp, "follower_count");
        author["favoriting_count"] = GetJsonInt(authorProp, "favoriting_count");
        author["total_favorited"] = GetJsonString(authorProp, "total_favorited");

        return author;
    }

    private Dictionary<string, object> ProcessVideoInfo(JsonElement awemeInfo)
    {
        var video = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("video", out var videoProp))
            return video;

        if (videoProp.TryGetProperty("duration", out var duration))
        {
            var dur = duration.GetInt64();
            video["duration"] = dur > 1000 ? dur / 1000 : dur;
        }

        if (videoProp.TryGetProperty("play_addr", out var playAddr) &&
            playAddr.TryGetProperty("url_list", out var urlList) &&
            urlList.GetArrayLength() > 0)
        {
            video["play_url"] = urlList[0].GetString();
        }

        if (videoProp.TryGetProperty("dynamic_cover", out var cover) &&
            cover.TryGetProperty("url_list", out var coverUrls) &&
            coverUrls.GetArrayLength() > 0)
        {
            video["dynamic_cover"] = coverUrls[0].GetString();
        }

        return video;
    }

    private Dictionary<string, object> ProcessMusicInfo(JsonElement awemeInfo)
    {
        var music = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("music", out var musicProp))
            return music;

        music["title"] = GetJsonString(musicProp, "title");
        music["duration"] = GetJsonInt(musicProp, "duration");

        if (musicProp.TryGetProperty("play_url", out var playUrl) &&
            playUrl.TryGetProperty("uri", out var uri))
        {
            music["play_url"] = uri.GetString();
        }

        return music;
    }

    private Dictionary<string, object> ProcessStatistics(JsonElement awemeInfo)
    {
        var stats = new Dictionary<string, object>();
        if (!awemeInfo.TryGetProperty("statistics", out var statsProp))
            return stats;

        stats["digg_count"] = GetJsonInt(statsProp, "digg_count");
        stats["comment_count"] = GetJsonInt(statsProp, "comment_count");
        stats["share_count"] = GetJsonInt(statsProp, "share_count");
        stats["collect_count"] = GetJsonInt(statsProp, "collect_count");

        return stats;
    }

    private List<string> ProcessImageList(JsonElement images)
    {
        var imageList = new List<string>();
        foreach (var img in images.EnumerateArray())
        {
            if (img.TryGetProperty("url_list", out var urlList) && urlList.GetArrayLength() > 0)
            {
                imageList.Add(urlList[0].GetString());
            }
        }
        return imageList;
    }

    private string GetJsonString(JsonElement element, string propertyName)
    {
        if (element.TryGetProperty(propertyName, out var prop) && prop.ValueKind == JsonValueKind.String)
        {
            return prop.GetString();
        }
        return null;
    }

    private int GetJsonInt(JsonElement element, string propertyName, int defaultValue = 0)
    {
        if (element.TryGetProperty(propertyName, out var prop) && prop.ValueKind == JsonValueKind.Number)
        {
            return prop.GetInt32();
        }
        return defaultValue;
    }

    public async Task AppSearchAsync(int maxCount = 100)
    {
        var baseUrl = "https://aweme.snssdk.com/aweme/v1/search/item/";
        var cookies = new Dictionary<string, string> { { "sessionid", "ee362944ff59baa7064cd8c1552c33c5" } };

        var cdid = GenerateCdid();
        var openudid = GenerateOpenUdid();
        var deviceId = GenerateId();
        var iid = GenerateId();

        var headers = new Dictionary<string, string>
        {
            { "User-Agent", "ttnet okhttp/3.10.0.2" },
            { "Connection", "Keep-Alive" },
            { "Accept-Encoding", "gzip" },
            { "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8" },
            // { "X-SS-STUB", "174915DC1CBF1B43F67CEB2698F3761A" },
            { "sdk-version", "1" }
        };

        var currentPage = 1;
        var searchId = "";
        var items = new List<Dictionary<string, object>>();
        var awemeIdList = new List<string>();
        var shouldExit = false;

        while (!shouldExit)
        {
            Console.WriteLine($"正在爬取第{currentPage}页......");

            var rticket = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalMilliseconds;
            var ts = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;

            var parameters = new Dictionary<string, string>
            {
                { "manifest_version_code", "100001" },
                { "_rticket", rticket.ToString() },
                { "app_type", "normal" },
                { "iid", iid },
                { "channel", "huawei" },
                { "device_type", "22127RK46C" },
                { "language", "zh" },
                { "resolution", "900*1600" },
                { "openudid", openudid },
                { "update_version_code", "10009900" },
                { "cdid", cdid },
                { "os_api", "28" },
                { "dpi", "240" },
                { "ac", "wifi" },
                { "device_id", deviceId },
                { "mcc_mnc", "46000" },
                { "os_version", "9" },
                { "version_code", "100000" },
                { "app_name", "douyin_lite" },
                { "version_name", "10.0.0" },
                { "device_brand", "Redmi" },
                { "ssmix", "a" },
                { "device_platform", "android" },
                { "aid", "2329" },
                { "ts", ts.ToString() }
            };

            var paramsStr = string.Join("&", parameters.Select(kv => $"{kv.Key}={HttpUtility.UrlEncode(kv.Value)}"));
            var url = $"{baseUrl}?{paramsStr}";

            var xgorgon = new XGorgon();
            var signature = xgorgon.Calculate(paramsStr, headers);
            Console.WriteLine($"生成的签名信息:");
            Console.WriteLine($"X-Gorgon: {signature["X-Gorgon"]}");
            Console.WriteLine($"X-Khronos: {signature["X-Khronos"]}");

            var requestHeaders = new Dictionary<string, string>(headers);
            requestHeaders["X-Khronos"] = signature["X-Khronos"];
            requestHeaders["X-Gorgon"] = signature["X-Gorgon"];
            requestHeaders["X-SS-REQ-TICKET"] = rticket.ToString();

            var data = new Dictionary<string, string>
            {
                { "keyword", _keyword },
                { "offset", ((currentPage - 1) * 20).ToString() },
                { "count", "20" },
                { "source", "video_search" },
                { "is_pull_refresh", "1" },
                { "hot_search", "0" },
                { "search_id", searchId },
                { "query_correct_type", "1" },
                { "is_filter_search", "1" },
                { "sort_type", "1" },
                { "publish_time", "0" }
            };

            try
            {
                var content = new FormUrlEncodedContent(data);
                var request = new HttpRequestMessage(HttpMethod.Post, url) { Content = content };

                foreach (var header in requestHeaders)
                {
                    request.Headers.TryAddWithoutValidation(header.Key, header.Value);
                }

                foreach (var cookie in cookies)
                {
                    request.Headers.Add("Cookie", $"{cookie.Key}={cookie.Value}");
                }

                var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();

                var responseContent = await response.Content.ReadAsStringAsync();
                // Console.WriteLine($"responseContent: {responseContent}");

                using var jsonDoc = JsonDocument.Parse(responseContent);
                var root = jsonDoc.RootElement;

                // 处理search_id
                if (currentPage == 1 && root.TryGetProperty("extra", out var extra) &&
                    extra.TryGetProperty("logid", out var logid))
                {
                    searchId = logid.ValueKind switch
                    {
                        JsonValueKind.String => logid.GetString(),
                        JsonValueKind.Number => logid.GetInt32().ToString(),
                        _ => null
                    };
                    Console.WriteLine($"search_id: {searchId}");
                }

                // 处理has_more
                bool hasMore = false;
                if (root.TryGetProperty("has_more", out var hasMoreProp))
                {
                    hasMore = hasMoreProp.ValueKind switch
                    {
                        JsonValueKind.True => true,
                        JsonValueKind.False => false,
                        JsonValueKind.Number => hasMoreProp.GetInt32() == 1,
                        _ => false
                    };
                }
                Console.WriteLine(hasMore ? "还有下一页" : "没有下一页");

                // 处理数据
                if (root.TryGetProperty("aweme_list", out var awemeList) && awemeList.ValueKind == JsonValueKind.Array)
                {
                    foreach (var item in awemeList.EnumerateArray())
                    {
                    
                        var resultItem = ProcessAwemeItem(item);
                        if (resultItem != null && resultItem.TryGetValue("aweme_id", out var awemeId))
                        {
                            awemeIdList.Add(awemeId.ToString());
                            items.Add(resultItem);

                            Console.WriteLine(JsonSerializer.Serialize(resultItem, JsonSettings.Default));

                            if (items.Count >= maxCount)
                            {
                                shouldExit = true;
                                break;
                            }
                        }

                    }

                    if (!hasMore)
                    {
                        shouldExit = true;
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"请求错误: {ex.Message}");
                shouldExit = true;
            }

            currentPage++;
            await Task.Delay(1000);
        }

        Console.WriteLine("\n======== 爬取完成 ==========");
        Console.WriteLine($"共{currentPage - 1}页，一共{items.Count}个作品");
        Console.WriteLine($"去重前: {awemeIdList.Count}, 去重后: {awemeIdList.Distinct().Count()}");
    }
}

class Program
{
    static async Task Main(string[] args)
    {
        Console.WriteLine("抖音APP搜索爬虫启动...");
        var spider = new DouyinAppSpider("旺仔小乔");
        await spider.AppSearchAsync(100);
        
        Console.WriteLine("\n程序执行完毕，按任意键退出...");
        Console.ReadKey();
    }
}