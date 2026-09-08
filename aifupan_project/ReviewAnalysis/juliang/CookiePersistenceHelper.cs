using CefSharp;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.juliang
{
    /// <summary>
    /// Cookie本地读写工具（支持初始化传入自定义路径）
    /// </summary>
    public class CookiePersistenceHelper : IDisposable
    {
        // 线程锁（保证文件操作线程安全）
        private readonly object _fileLock = new object();
        // 标记是否已释放
        private bool _disposed = false;

        // ========== 可配置参数 ==========
        /// <summary>当前Cookie存储路径（外部可读取）</summary>
        public string CookieSavePath { get; private set; }
        /// <summary>AES加密密钥（自定义，16/24/32位）</summary>
        public string AesKey { get; private set; }

        #region 构造函数（支持传入自定义路径）
        /// <summary>默认构造函数（使用原默认路径和密钥）</summary>
        public CookiePersistenceHelper()
        {
            // 默认路径：AppData下的BuyinSpider目录
            CookieSavePath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "BuyinSpider",
                "login_cookies.json"
            );
            // 默认密钥（建议生产环境从配置文件读取）
            AesKey = "your_16_bit_key_here";
            // 确保目录存在
            EnsureDirectoryExists();
        }

        /// <summary>自定义构造函数（传入存储路径，使用默认密钥）</summary>
        /// <param name="cookieSavePath">Cookie存储文件的完整路径（如 D:\cookies\account1.json）</param>
        public CookiePersistenceHelper(string cookieSavePath)
        {
            if (string.IsNullOrWhiteSpace(cookieSavePath))
                throw new ArgumentNullException(nameof(cookieSavePath), "Cookie存储路径不能为空");

            CookieSavePath = cookieSavePath;
            AesKey = "your_16_bit_key_here";
            EnsureDirectoryExists();
        }

        /// <summary>自定义构造函数（传入存储路径+加密密钥）</summary>
        /// <param name="cookieSavePath">Cookie存储文件的完整路径</param>
        /// <param name="aesKey">AES加密密钥（16/24/32位）</param>
        public CookiePersistenceHelper(string cookieSavePath, string aesKey)
        {
            if (string.IsNullOrWhiteSpace(cookieSavePath))
                throw new ArgumentNullException(nameof(cookieSavePath), "Cookie存储路径不能为空");
            if (string.IsNullOrWhiteSpace(aesKey))
                throw new ArgumentNullException(nameof(aesKey), "AES密钥不能为空");
            if (aesKey.Length != 16 && aesKey.Length != 24 && aesKey.Length != 32)
                throw new ArgumentException("AES密钥必须是16/24/32位", nameof(aesKey));

            CookieSavePath = cookieSavePath;
            AesKey = aesKey;
            EnsureDirectoryExists();
        }
        #endregion

        #region 核心方法：从本地读取Cookie
        public List<Cookie> LoadCefCookiesFromLocal(bool decrypt = true)
        {
            CheckDisposed();

            try
            {
                // 核心：加锁+重试读取（解决文件占用问题）
                const int maxRetry = 3; // 最多重试3次
                int retryCount = 0;
                while (retryCount < maxRetry)
                {
                    try
                    {
                        // 1. 检查原路径文件是否存在
                        if (!File.Exists(CookieSavePath))
                        {
                            FileUtils.log($"原Cookie文件不存在：{CookieSavePath}，尝试读取兜底路径");
                            // 尝试读取兜底路径
                            return LoadFromFallbackPath(decrypt);
                        }

                        // 2. 加锁读取原路径文件
                        string content;
                        lock (_fileLock)
                        {
                            content = File.ReadAllText(CookieSavePath, Encoding.UTF8);
                        }

                        // 3. 解密+反序列化
                        var jsonStr = decrypt ? AesDecrypt(content, AesKey) : content;
                        var dtos = JsonConvert.DeserializeObject<List<CookieDto>>(jsonStr) ?? new List<CookieDto>();
                        var cefCookies = dtos.ConvertAll(dto => dto.ToCefCookie());

                        FileUtils.log($"成功从 {CookieSavePath} 读取 {cefCookies.Count} 个Cookie");
                        return cefCookies;
                    }
                    catch (IOException ex) when (ex.Message.Contains("被另一进程使用"))
                    {
                        // 文件被占用：等待100ms后重试
                        retryCount++;
                        FileUtils.log($"Cookie文件被占用，重试第 {retryCount} 次：{ex.Message}");
                        Thread.Sleep(100);
                    }
                }

                // 重试失败：尝试读取兜底路径
                FileUtils.log($"⚠️ 原路径读取重试{maxRetry}次失败，尝试读取兜底路径");
                return LoadFromFallbackPath(decrypt);
            }
            catch (UnauthorizedAccessException ex)
            {
                // 权限不足：直接读取兜底路径
                FileUtils.log($"❌ 原路径权限不足：{ex.Message}，尝试读取兜底路径");
                return LoadFromFallbackPath(decrypt);
            }
            catch (Exception ex)
            {
                FileUtils.log($"❌ 读取本地Cookie失败：{ex.Message}");
                // 兜底返回空列表，避免程序崩溃
                return new List<Cookie>();
            }
        }

        /// <summary>从AppData兜底路径读取Cookie（与Save的兜底路径一致）</summary>
        /// <param name="decrypt">是否解密</param>
        private List<Cookie> LoadFromFallbackPath(bool decrypt = true)
        {
            try
            {
                // 生成与Save一致的兜底路径
                var fallbackPath = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    "ReviewAnalysis",
                    "Cookies",
                    Path.GetFileName(CookieSavePath)
                );

                // 检查兜底路径文件是否存在
                if (!File.Exists(fallbackPath))
                {
                    FileUtils.log($"❌ 兜底路径Cookie文件也不存在：{fallbackPath}");
                    return new List<Cookie>();
                }

                // 读取兜底路径文件
                string content;
                lock (_fileLock)
                {
                    content = File.ReadAllText(fallbackPath, Encoding.UTF8);
                }

                // 解密+反序列化
                var jsonStr = decrypt ? AesDecrypt(content, AesKey) : content;
                var dtos = JsonConvert.DeserializeObject<List<CookieDto>>(jsonStr) ?? new List<CookieDto>();
                var cefCookies = dtos.ConvertAll(dto => dto.ToCefCookie());

                // 更新当前实例的CookieSavePath为兜底路径（后续读写统一用兜底路径）
                CookieSavePath = fallbackPath;
                FileUtils.log($"✅ 从兜底路径 {fallbackPath} 读取 {cefCookies.Count} 个Cookie");
                return cefCookies;
            }
            catch (Exception ex)
            {
                FileUtils.log($"❌ 读取兜底路径Cookie失败：{ex.Message}");
                return new List<Cookie>();
            }
        }
        #endregion

        #region 核心方法：保存Cookie到本地
        /// <summary>保存CefSharp.Cookie列表到本地（增加容错逻辑）</summary>
        public void SaveCefCookiesToLocal(List<Cookie> cefCookies, bool encrypt = true)
        {
            CheckDisposed();

            if (cefCookies == null || cefCookies.Count == 0)
            {
                FileUtils.log("无Cookie可保存");
                return;
            }

            try
            {
                // 读取现有Cookie
                List<CookieDto> existingDtos = new List<CookieDto>();
                if (File.Exists(CookieSavePath))
                {
                    string existingContent = File.ReadAllText(CookieSavePath, Encoding.UTF8);
                    if (!string.IsNullOrEmpty(existingContent))
                    {
                        try
                        {
                            var decryptedContent = encrypt ? existingContent : AesDecrypt(existingContent, AesKey);
                            existingDtos = JsonConvert.DeserializeObject<List<CookieDto>>(decryptedContent) ?? new List<CookieDto>();
                        }
                        catch
                        {
                            // 如果解密或反序列化失败，使用空列表
                            existingDtos = new List<CookieDto>();
                        }
                    }
                }
                
                // 转换新Cookie为DTO
                var newDtos = cefCookies.ConvertAll(CookieDto.FromCefCookie);
                
                // 合并Cookie（更新现有，添加新的）
                var mergedDtos = MergeCookies(existingDtos, newDtos);
                
                // 序列化合并后的Cookie
                var jsonStr = JsonConvert.SerializeObject(mergedDtos, Formatting.Indented);
                var content = encrypt ? AesEncrypt(jsonStr, AesKey) : jsonStr;

                // 加锁+重试写入（解决文件占用问题）
                const int maxRetry = 3; // 最多重试3次
                int retryCount = 0;
                while (retryCount < maxRetry)
                {
                    try
                    {
                        lock (_fileLock)
                        {
                            File.WriteAllText(CookieSavePath, content, Encoding.UTF8);
                        }
                        FileUtils.log($"成功保存 {mergedDtos.Count} 个Cookie到 {CookieSavePath}");
                        return;
                    }
                    catch (IOException ex) when (ex.Message.Contains("被另一进程使用"))
                    {
                        // 文件被占用：等待100ms后重试
                        retryCount++;
                        FileUtils.log($"⚠️ Cookie文件被占用，重试第 {retryCount} 次：{ex.Message}");
                        Thread.Sleep(100);
                    }
                }

                // 重试失败：切换到AppData兜底路径
                FallbackToAppDataSave(content);
            }
            catch (UnauthorizedAccessException ex)
            {
                // 权限不足：切换到AppData兜底路径
                FileUtils.log($"❌ 原路径权限不足：{ex.Message}，切换到AppData兜底路径");
                
                // 读取现有Cookie
                List<CookieDto> existingDtos = new List<CookieDto>();
                if (File.Exists(CookieSavePath))
                {
                    string existingContent = File.ReadAllText(CookieSavePath, Encoding.UTF8);
                    if (!string.IsNullOrEmpty(existingContent))
                    {
                        try
                        {
                            var decryptedContent = AesDecrypt(existingContent, AesKey);
                            existingDtos = JsonConvert.DeserializeObject<List<CookieDto>>(decryptedContent) ?? new List<CookieDto>();
                        }
                        catch
                        {
                            // 如果解密或反序列化失败，使用空列表
                            existingDtos = new List<CookieDto>();
                        }
                    }
                }
                
                // 转换新Cookie为DTO
                var newDtos = cefCookies.ConvertAll(CookieDto.FromCefCookie);
                
                // 合并Cookie（更新现有，添加新的）
                var mergedDtos = MergeCookies(existingDtos, newDtos);
                
                // 序列化合并后的Cookie
                var jsonStr = JsonConvert.SerializeObject(mergedDtos, Formatting.Indented);
                var content = encrypt ? AesEncrypt(jsonStr, AesKey) : jsonStr;
                FallbackToAppDataSave(content);
            }
            catch (Exception ex)
            {
                FileUtils.log($"保存本地Cookie失败：{ex.Message}");
            }
        }
        
        /// <summary>
        /// 合并Cookie列表（更新现有，添加新的）
        /// </summary>
        /// <param name="existingDtos">现有Cookie</param>
        /// <param name="newDtos">新Cookie</param>
        /// <returns>合并后的Cookie列表</returns>
        private List<CookieDto> MergeCookies(List<CookieDto> existingDtos, List<CookieDto> newDtos)
        {
            // 使用字典来存储Cookie，键为Name+Domain，确保唯一性
            var cookieDict = new Dictionary<string, CookieDto>();
            
            // 添加现有Cookie
            foreach (var existingDto in existingDtos)
            {
                var key = existingDto.Name + existingDto.Domain;
                cookieDict[key] = existingDto;
            }
            
            // 添加或更新新Cookie
            foreach (var newDto in newDtos)
            {
                var key = newDto.Name + newDto.Domain;
                cookieDict[key] = newDto; // 覆盖现有Cookie
            }
            
            return cookieDict.Values.ToList();
        }

        /// <summary>兜底：写入AppData目录</summary>
        private void FallbackToAppDataSave(string content)
        {
            // 生成AppData兜底路径
            var fallbackPath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "ReviewAnalysis",
                "Cookies",
                Path.GetFileName(CookieSavePath)
            );
            // 更新当前实例的CookieSavePath为兜底路径
            CookieSavePath = fallbackPath;
            EnsureDirectoryExists();

            // 写入兜底路径
            lock (_fileLock)
            {
                File.WriteAllText(fallbackPath, content, Encoding.UTF8);
            }
            FileUtils.log($"✅ 已保存Cookie到兜底路径：{fallbackPath}");
        }
        #endregion

        #region 辅助方法
        /// <summary>确保存储目录存在（不存在则创建）</summary>
        private void EnsureDirectoryExists()
        {
            var dir = Path.GetDirectoryName(CookieSavePath);
            if (!Directory.Exists(dir))
            {
                Directory.CreateDirectory(dir);
                FileUtils.log($"创建Cookie存储目录：{dir}");
            }
        }

        /// <summary>AES加密（ECB模式，UTF8编码）</summary>
        private string AesEncrypt(string plainText, string key)
        {
            var keyBytes = Encoding.UTF8.GetBytes(key);
            var aes = Aes.Create();
            aes.Key = keyBytes;
            aes.Mode = CipherMode.ECB;
            aes.Padding = PaddingMode.PKCS7;

            var encryptor = aes.CreateEncryptor();
            var plainBytes = Encoding.UTF8.GetBytes(plainText);
            var encryptedBytes = encryptor.TransformFinalBlock(plainBytes, 0, plainBytes.Length);
            return Convert.ToBase64String(encryptedBytes);
        }

        /// <summary>AES解密（ECB模式，UTF8编码）</summary>
        private string AesDecrypt(string cipherText, string key)
        {
            var keyBytes = Encoding.UTF8.GetBytes(key);
            var aes = Aes.Create();
            aes.Key = keyBytes;
            aes.Mode = CipherMode.ECB;
            aes.Padding = PaddingMode.PKCS7;

            var decryptor = aes.CreateDecryptor();
            var cipherBytes = Convert.FromBase64String(cipherText);
            var decryptedBytes = decryptor.TransformFinalBlock(cipherBytes, 0, cipherBytes.Length);
            return Encoding.UTF8.GetString(decryptedBytes);
        }

        /// <summary>
        /// 修改 CEF 内存中指定 Cookie 的过期时间
        /// </summary>
        /// <param name="originalCookie">原 Cookie（需修改的目标 Cookie）</param>
        /// <param name="newExpiryTime">新的过期时间（UTC 时间，CEF 要求）</param>
        /// <param name="cefManager">CEF 上下文管理器</param>
        /// <returns>是否修改成功</returns>
        public bool ModifyCefCookieExpiry(Cookie originalCookie, DateTime newExpiryTime, CefRequestContextManager cefManager)
        {
            if (originalCookie == null) throw new ArgumentNullException(nameof(originalCookie));
            if (cefManager == null) throw new ArgumentNullException(nameof(cefManager));
            // CEF 的 Cookie 过期时间必须是 UTC 时间，否则会异常
            var newExpiryUtc = newExpiryTime.ToUniversalTime();

            try
            {
                var cookieManager = cefManager.GetRequestContext().GetCookieManager(null);
                if (cookieManager == null)
                {
                    FileUtils.log("❌ 获取 CookieManager 失败，无法修改过期时间");
                    return false;
                }

                // 步骤1：删除原 Cookie（CEF Cookie 不可变，需先删后加）
                var deleteTask = cookieManager.DeleteCookiesAsync(originalCookie.Name, originalCookie.Domain);
                deleteTask.Wait(); // 等待删除完成

                // 步骤2：创建新 Cookie（复制原属性，仅修改过期时间）
                var newCookie = new Cookie
                {
                    Name = originalCookie.Name,
                    Value = originalCookie.Value,
                    Domain = originalCookie.Domain,
                    Path = originalCookie.Path,
                    Expires = newExpiryUtc, // 核心：修改过期时间（UTC）
                    Secure = originalCookie.Secure,
                    HttpOnly = originalCookie.HttpOnly,
                    SameSite = originalCookie.SameSite,
                    //IsSession = newExpiryUtc == DateTime.MinValue.ToUniversalTime() // 会话 Cookie（无过期时间）
                };

                // 步骤3：添加新 Cookie 到 CEF 内存
                var addTask = cookieManager.SetCookieAsync(originalCookie.Domain, newCookie);
                addTask.Wait();

                FileUtils.log($"✅ 成功修改 CEF 中 Cookie [{originalCookie.Name}] 的过期时间为：{newExpiryTime}");
                return true;
            }
            catch (Exception ex)
            {
                FileUtils.log($"❌ 修改 CEF Cookie 过期时间失败：{ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// 修改本地 Cookie 文件中指定 Cookie 的过期时间
        /// </summary>
        /// <param name="cookieName">Cookie 名称</param>
        /// <param name="domain">Cookie 域名</param>
        /// <param name="newExpiryTime">新的过期时间</param>
        /// <returns>是否修改成功</returns>
        public bool ModifyLocalCookieExpiry(string cookieName, string domain, DateTime newExpiryTime)
        {
            lock (_fileLock)
            {
                try
                {
                    if (!File.Exists(CookieSavePath))
                    {
                        FileUtils.log($"⚠️ 本地 Cookie 文件不存在：{CookieSavePath}");
                        return false;
                    }

                    // 步骤1：读取本地 Cookie 文件（假设你用 JSON 序列化，适配你的存储格式）
                    var jsonContent = File.ReadAllText(CookieSavePath, Encoding.UTF8);
                    if (string.IsNullOrEmpty(jsonContent))
                    {
                        FileUtils.log("⚠️ 本地 Cookie 文件为空，无需修改");
                        return false;
                    }

                    // 步骤2：反序列化为 Cookie 列表（需和你 Save 方法的序列化格式一致）
                    var localCookies = JsonConvert.DeserializeObject<List<CookieDto>>(jsonContent) ?? new List<CookieDto>();
                    var targetCookie = localCookies.FirstOrDefault(c =>
                        c.Name.Equals(cookieName, StringComparison.OrdinalIgnoreCase) &&
                        c.Domain.Equals(domain, StringComparison.OrdinalIgnoreCase));

                    if (targetCookie == null)
                    {
                        FileUtils.log($"⚠️ 本地文件中未找到 Cookie [{cookieName}]（域名：{domain}）");
                        return false;
                    }

                    // 步骤3：修改过期时间（同步 UTC 格式）
                    targetCookie.Expires = newExpiryTime.ToUniversalTime();
                    //targetCookie.IsSession = newExpiryTime == DateTime.MinValue; // 会话 Cookie 标记

                    // 步骤4：重新序列化并写入文件
                    var newJsonContent = JsonConvert.SerializeObject(localCookies, Formatting.Indented);
                    File.WriteAllText(CookieSavePath, newJsonContent, Encoding.UTF8);

                    FileUtils.log($"✅ 成功修改本地文件中 Cookie [{cookieName}] 的过期时间为：{newExpiryTime}");
                    return true;
                }
                catch (Exception ex)
                {
                    FileUtils.log($"❌ 修改本地 Cookie 过期时间失败：{ex.Message}");
                    return false;
                }
            }
        }

        /// <summary>
        /// 一键修改：同步修改 CEF 内存 + 本地文件的 Cookie 过期时间
        /// </summary>
        /// <param name="originalCookie">原 Cookie</param>
        /// <param name="newExpiryTime">新过期时间（本地时间即可，内部转 UTC）</param>
        /// <param name="cefManager">CEF 上下文管理器</param>
        /// <returns>是否全部修改成功</returns>
        public bool ModifyCookieExpiry(Cookie originalCookie, DateTime newExpiryTime, CefRequestContextManager cefManager)
        {
            // 先修改 CEF 内存
            var cefModified = ModifyCefCookieExpiry(originalCookie, newExpiryTime, cefManager);
            // 再修改本地文件（同步）
            var localModified = ModifyLocalCookieExpiry(originalCookie.Name, originalCookie.Domain, newExpiryTime);
            // 只有两者都成功，才返回 true
            var success = cefModified && localModified;
            FileUtils.log(success
                ? $"✅ Cookie [{originalCookie.Name}] 过期时间修改完成（CEF+本地）"
                : $"❌ Cookie [{originalCookie.Name}] 过期时间修改失败（CEF：{cefModified}，本地：{localModified}）");
            return success;
        }
        /// <summary>
        /// 删除 CEF 内存中指定名称+域名的 Cookie
        /// </summary>
        /// <param name="cookieName">Cookie 名称（如 "LUOPAN_DT"）</param>
        /// <param name="domain">Cookie 域名（如 ".buyin.jinritemai.com"，精准匹配）</param>
        /// <param name="cefManager">CEF 上下文管理器</param>
        /// <returns>是否删除成功</returns>
        public bool DeleteCefCookie(string cookieName, string domain, CefRequestContextManager cefManager)
        {
            if (string.IsNullOrEmpty(cookieName)) throw new ArgumentNullException(nameof(cookieName));
            if (string.IsNullOrEmpty(domain)) throw new ArgumentNullException(nameof(domain));
            if (cefManager == null) throw new ArgumentNullException(nameof(cefManager));

            try
            {
                var cookieManager = cefManager.GetRequestContext().GetCookieManager(null);
                if (cookieManager == null)
                {
                    FileUtils.log("❌ 获取 CookieManager 失败，无法删除 CEF Cookie");
                    return false;
                }

                // 核心：精准删除指定名称+域名的 Cookie（CEF 异步方法，同步等待完成）
                var deleteTask = cookieManager.DeleteCookiesAsync(cookieName, domain);
                deleteTask.Wait(1000); // 超时1秒，避免卡死

                if (deleteTask.IsCompleted)
                {
                    FileUtils.log($"✅ CEF 中已删除 Cookie [{cookieName}]（域名：{domain}）");
                    return true;
                }
                else
                {
                    FileUtils.log($"⚠️ CEF 删除 Cookie [{cookieName}] 超时/失败");
                    return false;
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"❌ 删除 CEF Cookie 异常：{ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// 删除本地 Cookie 文件中指定名称+域名的 Cookie
        /// </summary>
        /// <param name="cookieName">Cookie 名称</param>
        /// <param name="domain">Cookie 域名</param>
        /// <returns>是否删除成功</returns>
        public bool DeleteLocalCookie(string cookieName, string domain)
        {
            lock (_fileLock) // 线程安全锁，避免并发文件操作
            {
                try
                {
                    if (!File.Exists(CookieSavePath))
                    {
                        FileUtils.log($"⚠️ 本地 Cookie 文件不存在：{CookieSavePath}");
                        return false;
                    }

                    // 步骤1：读取并反序列化本地 Cookie 文件
                    var jsonContent = File.ReadAllText(CookieSavePath, Encoding.UTF8);
                    if (string.IsNullOrEmpty(jsonContent))
                    {
                        FileUtils.log("⚠️ 本地 Cookie 文件为空，无需删除");
                        return false;
                    }
                    var localCookies = JsonConvert.DeserializeObject<List<CookieDto>>(jsonContent) ?? new List<CookieDto>();

                    // 步骤2：过滤掉目标 Cookie（忽略大小写，适配不同存储格式）
                    var originalCount = localCookies.Count;
                    localCookies = localCookies.Where(c =>
                        !c.Name.Equals(cookieName, StringComparison.OrdinalIgnoreCase) ||
                        !c.Domain.Equals(domain, StringComparison.OrdinalIgnoreCase)).ToList();

                    // 若没有匹配的 Cookie，直接返回
                    if (localCookies.Count == originalCount)
                    {
                        FileUtils.log($"⚠️ 本地文件中未找到 Cookie [{cookieName}]（域名：{domain}）");
                        return false;
                    }

                    // 步骤3：处理文件被占用的情况（释放 CEF 子进程）
                    try
                    {
                        // 重新序列化并写入文件
                        var newJsonContent = JsonConvert.SerializeObject(localCookies, Formatting.Indented);
                        File.WriteAllText(CookieSavePath, newJsonContent, Encoding.UTF8);
                    }
                    catch (IOException ex) when (ex.Message.Contains("正由另一进程使用"))
                    {
                        FileUtils.log($"⚠️ Cookie 文件被占用，释放 CEF 子进程后重试：{ex.Message}");
                        System.Threading.Thread.Sleep(300);
                        // 再次尝试写入
                        var newJsonContent = JsonConvert.SerializeObject(localCookies, Formatting.Indented);
                        File.WriteAllText(CookieSavePath, newJsonContent, Encoding.UTF8);
                    }

                    FileUtils.log($"✅ 本地文件中已删除 Cookie [{cookieName}]（域名：{domain}）");
                    return true;
                }
                catch (Exception ex)
                {
                    FileUtils.log($"❌ 删除本地 Cookie 异常：{ex.Message}");
                    return false;
                }
            }
        }

        /// <summary>
        /// 一键删除：同步删除 CEF 内存 + 本地文件中的指定 Cookie（推荐）
        /// </summary>
        /// <param name="cookieName">Cookie 名称</param>
        /// <param name="domain">Cookie 域名</param>
        /// <param name="cefManager">CEF 上下文管理器</param>
        /// <returns>是否全部删除成功</returns>
        public bool DeleteCookie(string cookieName, string domain, CefRequestContextManager cefManager)
        {
            // 先删 CEF 内存，再删本地文件
            var cefDeleted = DeleteCefCookie(cookieName, domain, cefManager);
            var localDeleted = DeleteLocalCookie(cookieName, domain);

            var success = cefDeleted && localDeleted;
            FileUtils.log(success
                ? $"✅ Cookie [{cookieName}]（域名：{domain}）删除完成（CEF+本地）"
                : $"❌ Cookie [{cookieName}] 删除失败（CEF：{cefDeleted}，本地：{localDeleted}）");
            return success;
        }

        /// <summary>检查是否已释放</summary>
        private void CheckDisposed()
        {
            if (_disposed)
                throw new ObjectDisposedException(nameof(CookiePersistenceHelper), "Cookie读写工具已释放");
        }
        #endregion

        #region 释放资源
        public void Dispose()
        {
            Dispose(true);
            GC.SuppressFinalize(this);
        }

        protected virtual void Dispose(bool disposing)
        {
            if (_disposed) return;

            if (disposing)
            {
                // 释放托管资源（暂无）
            }

            _disposed = true;
        }

        ~CookiePersistenceHelper()
        {
            Dispose(false);
        }
        #endregion
    }
}
