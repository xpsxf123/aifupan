

using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;

public class Spider
{
    private readonly string _account = "dengdingnan@jiuyujiaoyu.com";
    private readonly string _password = "Aa12345678";
    private readonly HttpClient _httpClient;
    private readonly HttpClientHandler _handler;

    public Spider()
    {
        // 初始化HttpClientHandler并禁用自动重定向
        _handler = new HttpClientHandler
        {
            AllowAutoRedirect = false,  // 对应Python的allow_redirects=False
            UseCookies = true,
            CookieContainer = new CookieContainer()
        };
        
        _httpClient = new HttpClient(_handler);
        _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
    }

    // 加密方法保持不变
    public string Encrypt(string e)
    {
        if (string.IsNullOrEmpty(e))
            return "";

        var t = new List<byte>();

        foreach (char c in e)
        {
            int code = c;

            if (code >= 0 && code <= 127)
            {
                t.Add((byte)code);
            }
            else if (code >= 128 && code <= 2047)
            {
                t.Add((byte)(192 | ((code >> 6) & 31)));
                t.Add((byte)(128 | (code & 63)));
            }
            else if ((code >= 2048 && code <= 55295) || (code >= 57344 && code <= 65535))
            {
                t.Add((byte)(224 | ((code >> 12) & 15)));
                t.Add((byte)(128 | ((code >> 6) & 63)));
                t.Add((byte)(128 | (code & 63)));
            }
        }

        var n = new List<string>();
        foreach (byte b in t)
        {
            byte xorResult = (byte)(5 ^ b);
            n.Add(xorResult.ToString("x2"));
        }

        return string.Join("", n);
    }

    // GetFp方法保持不变
    public string GetFp()
    {
        const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        var random = new Random();
        long timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;
        
        // 生成base36编码的时间戳
        string n = Base36Encode(timestamp).ToLower();

        var r = new char[36];
        r[8] = '_';
        r[13] = '_';
        r[18] = '_';
        r[23] = '_';
        r[14] = '4';

        for (int o = 0; o < 36; o++)
        {
            if (r[o] == '\0')
            {
                int i = random.Next(chars.Length);
                if (o == 19)
                {
                    r[o] = chars[(3 & i) | 8];
                }
                else
                {
                    r[o] = chars[i];
                }
            }
        }

        return $"verify_{n}_{new string(r)}";
    }

    // Base36Encode方法保持不变
    private string Base36Encode(long number)
    {
        const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (number == 0) return "0";
        
        var result = new StringBuilder();
        bool isNegative = number < 0;
        
        if (isNegative)
            number = -number;

        while (number > 0)
        {
            result.Append(chars[(int)(number % 36)]);
            number /= 36;
        }

        if (isNegative)
            result.Append('-');

        // 反转结果以获得正确的顺序
        char[] arr = result.ToString().ToCharArray();
        Array.Reverse(arr);
        return new string(arr);
    }

    public async Task AccountLogin()
    {
        // 清除默认请求头，避免冲突
        _httpClient.DefaultRequestHeaders.Clear();
        
        // 设置基本请求头（不包含content-type）
        var headers = new Dictionary<string, string>
        {
            {"accept", "application/json, text/plain, */*"},
            {"accept-language", "zh-CN,zh;q=0.9"},
            {"cache-control", "no-cache"},
            {"origin", "https://www.lifexue.com"},
            {"pragma", "no-cache"},
            {"priority", "u=1, i"},
            {"referer", "https://www.lifexue.com/"},
            {"sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"sec-fetch-dest", "empty"},
            {"sec-fetch-mode", "cors"},
            {"sec-fetch-site", "cross-site"},
            {"sec-fetch-storage-access", "none"},
            {"x-requested-with", "XMLHttpRequest"},
            {"x-tt-passport-csrf-token", ""},
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"}
        };

        foreach (var header in headers)
        {
            _httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
        }

        var formData = new FormUrlEncodedContent(new[]
        {
            new KeyValuePair<string, string>("fp", GetFp()),
            new KeyValuePair<string, string>("aid", "405096"),
            new KeyValuePair<string, string>("language", "zh"),
            new KeyValuePair<string, string>("account_sdk_source", "web"),
            new KeyValuePair<string, string>("mix_mode", "1"),
            new KeyValuePair<string, string>("service", "https://www.lifexue.com/"),
            new KeyValuePair<string, string>("account", Encrypt(_account)),
            new KeyValuePair<string, string>("password", Encrypt(_password)),
            new KeyValuePair<string, string>("captcha_key", "")
        });

        // 重要：content-type通过FormUrlEncodedContent自动设置为application/x-www-form-urlencoded
        // 不需要手动添加

        try
        {
            var response = await _httpClient.PostAsync("https://sso.oceanengine.com/account_login/v2/", formData);
            
            // 检查是否有重定向（3xx状态码）
            if (response.StatusCode >= HttpStatusCode.MultipleChoices && 
                response.StatusCode <= HttpStatusCode.TemporaryRedirect)
            {
                Console.WriteLine($"收到重定向响应: {response.StatusCode}");
                if (response.Headers.TryGetValues("Location", out var locations))
                {
                    foreach (var location in locations)
                    {
                        Console.WriteLine($"重定向地址: {location}");
                    }
                }
            }
            
            string responseBody = await response.Content.ReadAsStringAsync();
            Console.WriteLine($"登录响应: {responseBody}");
            
            var jsonData = System.Text.Json.JsonSerializer.Deserialize<System.Text.Json.Nodes.JsonObject>(responseBody);

            if (jsonData != null && jsonData.TryGetPropertyValue("error_code", out var errorCode) && 
                errorCode.GetValue<int>() == 0)
            {
                var userId = jsonData.TryGetPropertyValue("user_id", out var userIdNode) ? userIdNode.ToString() : "未知";
                Console.WriteLine($"学号user_id: {userId}");

                if (jsonData.TryGetPropertyValue("redirect_url", out var redirectUrlNode))
                {
                    string redirectUrl = redirectUrlNode.ToString();
                    Console.WriteLine($"回调接口：{redirectUrl}");
                    await LoginCallback(redirectUrl);
                }
            }
            else
            {
                Console.WriteLine("登录失败: " + responseBody);
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"登录请求异常: {ex.Message}");
            Console.WriteLine($"异常详情: {ex.ToString()}");
        }
    }

    public async Task LoginCallback(string redirectUrl)
    {
        // 清除之前的请求头
        _httpClient.DefaultRequestHeaders.Clear();
        
        // 设置回调请求头
        var headers = new Dictionary<string, string>
        {
            {"accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"},
            {"accept-language", "zh-CN,zh;q=0.9"},
            {"cache-control", "no-cache"},
            {"pragma", "no-cache"},
            {"priority", "u=0, i"},
            {"referer", "https://www.lifexue.com/"},
            {"sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"sec-fetch-dest", "document"},
            {"sec-fetch-mode", "navigate"},
            {"sec-fetch-site", "same-origin"},
            {"sec-fetch-user", "?1"},
            {"upgrade-insecure-requests", "1"},
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"}
        };

        foreach (var header in headers)
        {
            _httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
        }

        try
        {
            var response = await _httpClient.GetAsync(redirectUrl);
            Console.WriteLine($"回调响应状态码: {response.StatusCode}");

            // 检查是否有重定向
            if (response.StatusCode >= HttpStatusCode.MultipleChoices && 
                response.StatusCode <= HttpStatusCode.TemporaryRedirect)
            {
                Console.WriteLine($"回调请求收到重定向: {response.StatusCode}");
            }

            // 获取响应中的Cookie
            var cookies = _handler.CookieContainer.GetCookies(new Uri(redirectUrl));
            foreach (Cookie cookie in cookies)
            {
                if (cookie.Name == "sessionid")
                {
                    Console.WriteLine($"sessionid: {cookie.Value}");
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"回调请求异常: {ex.Message}");
            Console.WriteLine($"异常详情: {ex.ToString()}");
        }
    }

    public static async Task Main(string[] args)
    {
        var spider = new Spider();
        await spider.AccountLogin();
    }
}
