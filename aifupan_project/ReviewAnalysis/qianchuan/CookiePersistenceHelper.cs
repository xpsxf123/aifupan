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

namespace ReviewAnalysis.qianchuan
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