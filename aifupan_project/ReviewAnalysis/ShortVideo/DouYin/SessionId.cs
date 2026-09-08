

using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.system;

/// <summary>
/// 网络爬虫类，用于处理账户登录和会话管理
/// 主要功能包括账户登录、回调处理和SessionId获取
/// </summary>
public class DouYinSession
{
    #region 私有字段
    /// <summary>
    /// 登录账户邮箱
    /// </summary>
    private static string _account = null;

    /// <summary>
    /// 登录密码
    /// </summary>
    private static string _password = null;

    /// <summary>
    /// HTTP客户端，用于发送网络请求
    /// </summary>
    private readonly HttpClient _httpClient;

    /// <summary>
    /// HTTP客户端处理器，用于配置请求行为
    /// </summary>
    private readonly HttpClientHandler _handler;

    /// <summary>
    /// 保存sessionId的文件
    /// </summary>
    public static string sessionPath = Path.GetFullPath("dataCollect\\session\\config.json");

    /// <summary>
    /// SessionId过期时间（天数）
    /// </summary>
    private const int SESSION_EXPIRE_DAYS = 10;

    /// <summary>
    /// SessionId数据模型
    /// </summary>
    public class SessionData
    {
        /// <summary>
        /// SessionId值
        /// </summary>
        public string SessionId { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public DateTime CreateTime { get; set; }

        /// <summary>
        /// 过期时间
        /// </summary>
        public DateTime ExpireTime { get; set; }
    }
    #endregion

    #region 构造函数
    /// <summary>
    /// 初始化Spider实例
    /// 配置HttpClient和HttpClientHandler的基本设置
    /// </summary>
    public DouYinSession()
    {
        // 初始化HttpClientHandler并禁用自动重定向
        _handler = new HttpClientHandler
        {
            AllowAutoRedirect = false,  // 对应Python的allow_redirects=False，手动处理重定向
            UseCookies = true,          // 启用Cookie支持
            CookieContainer = new CookieContainer()  // Cookie容器
        };

        // 初始化HttpClient并设置默认User-Agent
        _httpClient = new HttpClient(_handler);
        _httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
    }
    #endregion

    #region 静态方法
    /// <summary>
    /// 获取有效的SessionId
    /// 先从文件读取，如果不存在或已过期则重新获取
    /// </summary>
    /// <returns>有效的SessionId，获取失败返回null</returns>
    public static string GetValidSessionId()
    {
        try
        {
            // 尝试从文件读取SessionId
            var sessionData = ReadSessionFromFile();

            // 检查SessionId是否有效且未过期
            if (sessionData != null && !string.IsNullOrEmpty(sessionData.SessionId) &&
                DateTime.Now < sessionData.ExpireTime)
            {
                //FileUtils.LogAnalysis($"使用缓存的SessionId: {sessionData.SessionId}", "GetValidSessionId");
                return sessionData.SessionId;
            }

            // SessionId不存在或已过期，重新获取
            FileUtils.LogAnalysis("SessionId不存在或已过期，开始重新获取", "GetValidSessionId");

            string newSessionId = FetchNewSessionId();

            if (!string.IsNullOrEmpty(newSessionId))
            {
                // 保存新的SessionId到文件
                SaveSessionToFile(newSessionId);
                FileUtils.LogAnalysis($"成功获取并保存新的SessionId: {newSessionId}", "GetValidSessionId");
                return newSessionId;
            }
            else
            {
                FileUtils.LogError("获取SessionId失败", "GetValidSessionId");
                return null;
            }
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"获取SessionId异常: {ex.Message}\n异常详情: {ex.ToString()}", "GetValidSessionId");
            return null;
        }
    }

    /// <summary>
    /// 从文件读取SessionId数据
    /// </summary>
    /// <returns>SessionId数据，读取失败返回null</returns>
    private static SessionData ReadSessionFromFile()
    {
        try
        {
            if (string.IsNullOrEmpty(_account) || string.IsNullOrEmpty(_password))
            {
                List<DictDataListVo> dictDataListVos = SystemApi.dictDataListByCode("dou_yin_learn_account_password");
                if (dictDataListVos == null && dictDataListVos.Count == 0)
                {
                    throw new CustomException("获取抖音生活服务学习中心账号密码失败");
                }
                DictDataListVo randomItem = dictDataListVos[new Random().Next(dictDataListVos.Count)];
                LearnAccountPassword learnAccountPassword = JsonConvert.DeserializeObject<LearnAccountPassword>(randomItem.value);
                _account = learnAccountPassword.account;
                _password = learnAccountPassword.password;
            }
            if (!File.Exists(sessionPath))
            {
                return null;
            }

            string jsonContent = File.ReadAllText(sessionPath);
            if (string.IsNullOrEmpty(jsonContent))
            {
                return null;
            }

            var sessionData = JsonConvert.DeserializeObject<SessionData>(jsonContent);
            return sessionData;
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"读取SessionId文件异常: {ex.Message}\n异常详情: {ex.ToString()}", "ReadSessionFromFile");
            return null;
        }
    }

    /// <summary>
    /// 保存SessionId数据到文件
    /// </summary>
    /// <param name="sessionId">要保存的SessionId</param>
    private static void SaveSessionToFile(string sessionId)
    {
        try
        {
            // 确保目录存在
            string directory = Path.GetDirectoryName(sessionPath);
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }

            // 创建SessionId数据
            var sessionData = new SessionData
            {
                SessionId = sessionId,
                CreateTime = DateTime.Now,
                ExpireTime = DateTime.Now.AddDays(SESSION_EXPIRE_DAYS)
            };

            // 序列化并保存到文件
            string jsonContent = JsonConvert.SerializeObject(sessionData, Formatting.Indented);
            File.WriteAllText(sessionPath, jsonContent);

            FileUtils.LogAnalysis($"SessionId已保存到文件: {sessionPath}", "SaveSessionToFile");
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"保存SessionId文件异常: {ex.Message}\n异常详情: {ex.ToString()}", "SaveSessionToFile");
        }
    }

    /// <summary>
    /// 获取新的SessionId
    /// 通过执行完整的登录流程获取SessionId
    /// </summary>
    /// <returns>新的SessionId，获取失败返回null</returns>
    private static string FetchNewSessionId()
    {
        try
        {
            var spider = new DouYinSession();
            return spider.ExecuteLoginAndGetSessionId();
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"执行登录获取SessionId异常: {ex.Message}\n异常详情: {ex.ToString()}", "FetchNewSessionId");
            return null;
        }
    }
    #endregion

    #region 加密和工具方法
    /// <summary>
    /// 字符串加密方法
    /// 将输入字符串转换为UTF-8字节序列，然后进行XOR加密并转换为十六进制字符串
    /// </summary>
    /// <param name="e">需要加密的字符串</param>
    /// <returns>加密后的十六进制字符串</returns>
    public string Encrypt(string e)
    {
        if (string.IsNullOrEmpty(e))
            return "";

        var t = new List<byte>();

        // 将字符串转换为UTF-8字节序列
        foreach (char c in e)
        {
            int code = c;

            // ASCII字符 (0-127)
            if (code >= 0 && code <= 127)
            {
                t.Add((byte)code);
            }
            // 双字节UTF-8字符 (128-2047)
            else if (code >= 128 && code <= 2047)
            {
                t.Add((byte)(192 | ((code >> 6) & 31)));
                t.Add((byte)(128 | (code & 63)));
            }
            // 三字节UTF-8字符 (2048-65535，排除代理对范围)
            else if ((code >= 2048 && code <= 55295) || (code >= 57344 && code <= 65535))
            {
                t.Add((byte)(224 | ((code >> 12) & 15)));
                t.Add((byte)(128 | ((code >> 6) & 63)));
                t.Add((byte)(128 | (code & 63)));
            }
        }

        // 对每个字节进行XOR加密并转换为十六进制
        var n = new List<string>();
        foreach (byte b in t)
        {
            byte xorResult = (byte)(5 ^ b);  // 使用5作为XOR密钥
            n.Add(xorResult.ToString("x2")); // 转换为两位十六进制字符串
        }

        return string.Join("", n);
    }

    /// <summary>
    /// 生成指纹(Fingerprint)字符串
    /// 用于请求验证，格式为: verify_{timestamp}_{uuid}
    /// </summary>
    /// <returns>生成的指纹字符串</returns>
    public string GetFp()
    {
        const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        var random = new Random();

        // 获取当前时间戳（Unix时间戳）
        long timestamp = (long)(DateTime.UtcNow - new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc)).TotalSeconds;

        // 生成base36编码的时间戳
        string n = Base36Encode(timestamp).ToLower();

        // 创建类似UUID格式的字符数组 (xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx)
        var r = new char[36];
        r[8] = '_';   // UUID中的第一个连字符位置
        r[13] = '_';  // UUID中的第二个连字符位置
        r[18] = '_';  // UUID中的第三个连字符位置
        r[23] = '_';  // UUID中的第四个连字符位置
        r[14] = '4';  // UUID版本号固定为4

        // 填充随机字符
        for (int o = 0; o < 36; o++)
        {
            if (r[o] == '\0')
            {
                int i = random.Next(chars.Length);
                if (o == 19)
                {
                    // UUID的变体位，确保符合RFC 4122规范
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

    /// <summary>
    /// 将长整型数字编码为Base36字符串
    /// Base36使用0-9和A-Z共36个字符进行编码
    /// </summary>
    /// <param name="number">要编码的数字</param>
    /// <returns>Base36编码后的字符串</returns>
    private string Base36Encode(long number)
    {
        const string chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (number == 0) return "0";

        var result = new StringBuilder();
        bool isNegative = number < 0;

        // 处理负数
        if (isNegative)
            number = -number;

        // 进行Base36转换
        while (number > 0)
        {
            result.Append(chars[(int)(number % 36)]);
            number /= 36;
        }

        // 添加负号（如果需要）
        if (isNegative)
            result.Append('-');

        // 反转结果以获得正确的顺序
        char[] arr = result.ToString().ToCharArray();
        Array.Reverse(arr);
        return new string(arr);
    }
    #endregion

    #region 登录相关方法
    /// <summary>
    /// 执行完整的登录流程并返回SessionId
    /// </summary>
    /// <returns>获取到的SessionId，失败返回null</returns>
    public string ExecuteLoginAndGetSessionId()
    {
        try
        {
            return AccountLoginAndGetSessionId();
        }
        catch (Exception ex)
        {
            FileUtils.LogError($"执行登录流程异常: {ex.Message}\n异常详情: {ex.ToString()}", "ExecuteLoginAndGetSessionId");
            return null;
        }
    }

    /// <summary>
    /// 执行账户登录操作并返回SessionId
    /// 向SSO服务器发送登录请求，处理响应和重定向，返回SessionId
    /// </summary>
    /// <returns>获取到的SessionId，失败返回null</returns>
    public string AccountLoginAndGetSessionId()
    {
        // 清除默认请求头，避免冲突
        _httpClient.DefaultRequestHeaders.Clear();

        // 设置登录请求所需的HTTP头部信息
        var headers = new Dictionary<string, string>
        {
            {"accept", "application/json, text/plain, */*"},                    // 接受的响应类型
            {"accept-language", "zh-CN,zh;q=0.9"},                            // 语言偏好
            {"cache-control", "no-cache"},                                     // 缓存控制
            {"origin", "https://www.lifexue.com"},                            // 请求来源
            {"pragma", "no-cache"},                                           // HTTP/1.0缓存控制
            {"priority", "u=1, i"},                                           // 请求优先级
            {"referer", "https://www.lifexue.com/"},                          // 引用页面
            {"sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\""}, // 客户端提示
            {"sec-ch-ua-mobile", "?0"},                                       // 移动设备标识
            {"sec-ch-ua-platform", "\"Windows\""},                           // 平台信息
            {"sec-fetch-dest", "empty"},                                      // 请求目标
            {"sec-fetch-mode", "cors"},                                       // 请求模式
            {"sec-fetch-site", "cross-site"},                                 // 请求站点
            {"sec-fetch-storage-access", "none"},                             // 存储访问
            {"x-requested-with", "XMLHttpRequest"},                           // AJAX请求标识
            {"x-tt-passport-csrf-token", ""},                                 // CSRF令牌
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"}
        };

        // 添加所有请求头
        foreach (var header in headers)
        {
            _httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
        }

        // 构建登录表单数据
        var formData = new FormUrlEncodedContent(new[]
        {
            new KeyValuePair<string, string>("fp", GetFp()),                    // 指纹信息
            new KeyValuePair<string, string>("aid", "405096"),                  // 应用ID
            new KeyValuePair<string, string>("language", "zh"),                 // 语言设置
            new KeyValuePair<string, string>("account_sdk_source", "web"),      // SDK来源
            new KeyValuePair<string, string>("mix_mode", "1"),                  // 混合模式
            new KeyValuePair<string, string>("service", "https://www.lifexue.com/"), // 服务地址
            new KeyValuePair<string, string>("account", Encrypt(_account)),     // 加密后的账户
            new KeyValuePair<string, string>("password", Encrypt(_password)),   // 加密后的密码
            new KeyValuePair<string, string>("captcha_key", "")                 // 验证码密钥（空）
        });

        try
        {
            // 发送登录POST请求
            var response = _httpClient.PostAsync("https://sso.oceanengine.com/account_login/v2/", formData).Result;

            // 检查是否有重定向（3xx状态码）
            if (response.StatusCode >= HttpStatusCode.MultipleChoices &&
                response.StatusCode <= HttpStatusCode.TemporaryRedirect)
            {
                // 记录重定向信息到日志
                if (response.Headers.TryGetValues("Location", out var locations))
                {
                    foreach (var location in locations)
                    {
                        FileUtils.LogAnalysis($"收到重定向响应: {response.StatusCode}, 重定向地址: {location}", "AccountLoginAndGetSessionId");
                    }
                }
            }

            // 读取响应内容
            string responseBody = response.Content.ReadAsStringAsync().Result;

            // 解析JSON响应
            var jsonData = JsonConvert.DeserializeObject<dynamic>(responseBody);

            // 检查登录是否成功
            if (jsonData != null && jsonData.error_code != null &&
                (int)jsonData.error_code == 0)
            {
                // 获取用户ID
                var userId = jsonData.user_id != null ? jsonData.user_id.ToString() : "未知";
                FileUtils.LogAnalysis($"登录成功，学号user_id: {userId}", "AccountLoginAndGetSessionId");

                // 处理重定向URL
                if (jsonData.redirect_url != null)
                {
                    string redirectUrl = jsonData.redirect_url.ToString();
                    FileUtils.LogAnalysis($"回调接口：{redirectUrl}", "AccountLoginAndGetSessionId");
                    string sessionId = LoginCallbackAndGetSessionId(redirectUrl);  // 调用回调处理方法并获取SessionId
                    return sessionId;
                }
            }
            else
            {
                FileUtils.LogError("登录失败: " + responseBody, "AccountLoginAndGetSessionId");
                return null;
            }
        }
        catch (Exception ex)
        {
            // 记录错误日志到文件
            FileUtils.LogError($"登录请求异常: {ex.Message}\n异常详情: {ex.ToString()}", "AccountLoginAndGetSessionId");
            return null;
        }
        return null;
    }

    /// <summary>
    /// 执行账户登录操作（原方法保持兼容性）
    /// 向SSO服务器发送登录请求，处理响应和重定向
    /// </summary>
    public void AccountLogin()
    {
        AccountLoginAndGetSessionId();
    }

    /// <summary>
    /// 处理登录回调请求并返回SessionId
    /// 访问重定向URL以完成登录流程并获取SessionId
    /// </summary>
    /// <param name="redirectUrl">登录成功后的重定向URL</param>
    /// <returns>获取到的SessionId，失败返回null</returns>
    public string LoginCallbackAndGetSessionId(string redirectUrl)
    {
        // 清除之前的请求头，准备设置回调请求头
        _httpClient.DefaultRequestHeaders.Clear();

        // 设置回调请求所需的HTTP头部信息
        var headers = new Dictionary<string, string>
        {
            {"accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"}, // 接受的内容类型
            {"accept-language", "zh-CN,zh;q=0.9"},                            // 语言偏好
            {"cache-control", "no-cache"},                                     // 缓存控制
            {"pragma", "no-cache"},                                           // HTTP/1.0缓存控制
            {"priority", "u=0, i"},                                           // 请求优先级（最高）
            {"referer", "https://www.lifexue.com/"},                          // 引用页面
            {"sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\""}, // 客户端提示
            {"sec-ch-ua-mobile", "?0"},                                       // 移动设备标识
            {"sec-ch-ua-platform", "\"Windows\""},                           // 平台信息
            {"sec-fetch-dest", "document"},                                   // 请求目标（文档）
            {"sec-fetch-mode", "navigate"},                                   // 请求模式（导航）
            {"sec-fetch-site", "same-origin"},                                // 请求站点（同源）
            {"sec-fetch-user", "?1"},                                         // 用户激活
            {"upgrade-insecure-requests", "1"},                               // 升级不安全请求
            {"user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"}
        };

        // 添加所有回调请求头
        foreach (var header in headers)
        {
            _httpClient.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
        }

        try
        {
            // 发送GET请求到重定向URL
            var response = _httpClient.GetAsync(redirectUrl).Result;
            FileUtils.LogAnalysis($"回调响应状态码: {response.StatusCode}", "LoginCallbackAndGetSessionId");

            // 检查是否有重定向响应
            if (response.StatusCode >= HttpStatusCode.MultipleChoices &&
                response.StatusCode <= HttpStatusCode.TemporaryRedirect)
            {
                FileUtils.LogAnalysis($"回调请求收到重定向: {response.StatusCode}", "LoginCallbackAndGetSessionId");
            }

            // 从Cookie容器中获取SessionId
            var cookies = _handler.CookieContainer.GetCookies(new Uri(redirectUrl));
            foreach (Cookie cookie in cookies)
            {
                if (cookie.Name == "sessionid")
                {
                    FileUtils.LogAnalysis($"成功获取sessionid: {cookie.Value}", "LoginCallbackAndGetSessionId");
                    return cookie.Value;  // 返回找到的SessionId
                }
            }

            // 如果没有找到SessionId
            FileUtils.LogError("未找到SessionId", "LoginCallbackAndGetSessionId");
            return null;
        }
        catch (Exception ex)
        {
            // 记录错误日志到文件
            FileUtils.LogError($"回调请求异常: {ex.Message}\n异常详情: {ex.ToString()}", "LoginCallbackAndGetSessionId");
            return null;
        }
    }

    /// <summary>
    /// 处理登录回调请求（原方法保持兼容性）
    /// 访问重定向URL以完成登录流程并获取SessionId
    /// </summary>
    /// <param name="redirectUrl">登录成功后的重定向URL</param>
    public void LoginCallback(string redirectUrl)
    {
        LoginCallbackAndGetSessionId(redirectUrl);
    }
    #endregion

    public class LearnAccountPassword
    {
        public string account { get; set; }

        public string password { get; set; }
    }
}
