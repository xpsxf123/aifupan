using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Security.Cryptography;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr.Local
{
    public class ModelManager
    {
        // File name → download URL mapping
        private static readonly Dictionary<string, string> DefaultFileUrls = new Dictionary<string, string>
        {
            { "model_quant.onnx", "https://yuanlian-video-1330324554.cos.ap-shanghai.myqcloud.com/ai-models/SenseVoice-Small-onnx/model_quant.onnx" },
            { "tokens.json", "https://download1.ifupan.com/sensevoice_tokens.txt" }
        };

        private readonly string _modelDir;
        private readonly string _configDir;
        private readonly string _configPath;
        private readonly string _manifestPath;
        private static readonly HttpClient _httpClient = new HttpClient { Timeout = TimeSpan.FromMinutes(30) };
        private Dictionary<string, string> _fileUrls;
        private static readonly object _downloadLock = new object();
        private static Task _downloadTask;
        private static volatile bool _modelVerified;

        private class ManifestEntry
        {
            public string file;
            public string sha256;
        }

        private class ManifestDoc
        {
            public string version;
            public List<ManifestEntry> files;
        }

        public ModelManager()
        {
            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            _modelDir = Path.Combine(localAppData, "ReviewAnalysis", "models", "sensevoice-small");
            _configDir = Path.Combine(localAppData, "ReviewAnalysis", "Config");
            _configPath = Path.Combine(_configDir, "svs_model_config.json");
            _manifestPath = Path.Combine(_modelDir, "manifest.json");
            LoadDownloadUrl();
        }

        /// <summary>
        /// Fire-and-forget background warmup. Triggers model download if not already cached.
        /// Safe to call at app startup — does not block.
        /// </summary>
        public static void WarmupAsync()
        {
            Task.Run(async () =>
            {
                try
                {
                    FileUtils.LogAnalysis("[Model] 后台预热开始，检查模型文件...");
                    var mgr = new ModelManager();
                    if (!mgr.IsModelReady())
                    {
                        FileUtils.LogAnalysis("[Model] 模型文件缺失，开始后台下载...");
                        await mgr.EnsureDownloadStartedAsync();
                    }
                    FileUtils.LogAnalysis("[Model] 后台预热完成");
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[Model] 后台预热失败: {ex.Message}");
                }
            });
        }

        public string GetModelPath()
        {
            return Path.Combine(_modelDir, "model_quant.onnx");
        }

        public string GetTokensPath()
        {
            return Path.Combine(_modelDir, "tokens.json");
        }

        /// <summary>
        /// Check if model files exist AND pass SHA256 verification against local manifest.
        /// After first successful verification, subsequent calls use a fast path
        /// (file existence only, skipping expensive SHA256).
        /// </summary>
        public bool IsModelReady()
        {
            // Fast path: already verified, just check files still exist
            if (_modelVerified)
            {
                foreach (string file in _fileUrls.Keys)
                {
                    if (!File.Exists(Path.Combine(_modelDir, file)))
                    {
                        _modelVerified = false;
                        return false;
                    }
                }
                return true;
            }

            // Slow path: full SHA256 verification on first call or after invalidation
            foreach (string file in _fileUrls.Keys)
            {
                if (!File.Exists(Path.Combine(_modelDir, file)))
                    return false;
            }

            if (!File.Exists(_manifestPath))
                return false;

            try
            {
                var manifest = ReadManifest();
                if (manifest == null || manifest.files == null)
                    return false;

                foreach (var entry in manifest.files)
                {
                    string filePath = Path.Combine(_modelDir, entry.file);
                    if (!File.Exists(filePath))
                        return false;

                    string actualHash = ComputeSha256(filePath);
                    if (!string.Equals(actualHash, entry.sha256, StringComparison.OrdinalIgnoreCase))
                    {
                        File.Delete(filePath);
                        _modelVerified = false;
                        return false;
                    }
                }

                _modelVerified = true;
                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[Model] SHA256校验异常: {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// Trigger download and return the Task. First call starts download,
        /// subsequent calls return the same Task. Safe to call multiple times.
        /// </summary>
        public Task EnsureDownloadStartedAsync()
        {
            if (_downloadTask != null)
                return _downloadTask;

            lock (_downloadLock)
            {
                if (_downloadTask != null)
                    return _downloadTask;
                _downloadTask = Task.Run(() => DownloadAsync());
                return _downloadTask;
            }
        }

        #region Download

        private async Task DownloadAsync()
        {
            var swTotal = Stopwatch.StartNew();
            try
            {
                FileUtils.LogAnalysis($"[Model] 开始下载模型文件到 {_modelDir}");
                Directory.CreateDirectory(_modelDir);

                var entries = new List<ManifestEntry>();

                foreach (var kv in _fileUrls)
                {
                    string filePath = await DownloadFileWithRetry(kv.Key, kv.Value, maxRetries: 3);
                    if (filePath == null)
                    {
                        FileUtils.LogAnalysis($"[Model] 下载失败: {kv.Key}, 已重试3次");
                        return;
                    }

                    // Compute SHA256 locally and add to manifest
                    var swHash = Stopwatch.StartNew();
                    string sha256 = ComputeSha256(filePath);
                    FileUtils.LogAnalysis($"[Model] SHA256 校验完成: {kv.Key}, hash={sha256}, 耗时={swHash.ElapsedMilliseconds}ms");

                    entries.Add(new ManifestEntry
                    {
                        file = kv.Key,
                        sha256 = sha256
                    });
                }

                // Write local manifest with locally-computed hashes
                var manifest = new ManifestDoc { version = "1.0", files = entries };
                File.WriteAllText(_manifestPath, JsonConvert.SerializeObject(manifest, Formatting.Indented));
                _modelVerified = true;
                FileUtils.LogAnalysis($"[Model] 所有模型文件下载完成, 总耗时={swTotal.ElapsedMilliseconds}ms");
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[Model] 下载过程异常: {ex.Message}");
            }
        }

        private async Task<string> DownloadFileWithRetry(string fileName, string url, int maxRetries)
        {
            string finalPath = Path.Combine(_modelDir, fileName);
            string partPath = finalPath + ".part";

            FileUtils.LogAnalysis($"[Model] 下载文件: {fileName}, url={url}");

            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    string fileUrl = url;
                    long existingBytes = File.Exists(partPath) ? new FileInfo(partPath).Length : 0;
                    if (existingBytes > 0)
                        FileUtils.LogAnalysis($"[Model] 续传: {fileName}, 已有={existingBytes / 1024}KB");

                    var swDownload = Stopwatch.StartNew();
                    using (var request = new HttpRequestMessage(HttpMethod.Get, fileUrl))
                    {
                        if (existingBytes > 0)
                            request.Headers.Range = new System.Net.Http.Headers.RangeHeaderValue(existingBytes, null);

                        using (var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead))
                        {
                            if (response.StatusCode == System.Net.HttpStatusCode.RequestedRangeNotSatisfiable)
                            {
                                File.Delete(partPath);
                                continue;
                            }

                            response.EnsureSuccessStatusCode();

                            using (var stream = await response.Content.ReadAsStreamAsync())
                            using (var fileStream = new FileStream(partPath,
                                existingBytes > 0 ? FileMode.Append : FileMode.Create,
                                FileAccess.Write, FileShare.None, 8192,
                                FileOptions.Asynchronous))
                            {
                                await stream.CopyToAsync(fileStream);
                            }
                        }
                    }

                    // Atomic replace: rename .part → final
                    if (File.Exists(finalPath))
                        File.Delete(finalPath);
                    File.Move(partPath, finalPath);

                    long fileSize = new FileInfo(finalPath).Length;
                    FileUtils.LogAnalysis($"[Model] 下载完成: {fileName}, 大小={fileSize / 1024}KB, 耗时={swDownload.ElapsedMilliseconds}ms");

                    return finalPath;
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[Model] 下载重试 {attempt + 1}/{maxRetries}: {fileName}, 错误={ex.Message}");
                    if (attempt < maxRetries - 1)
                        await Task.Delay(1000 * (attempt + 1));
                }
            }

            // All retries exhausted
            FileUtils.LogAnalysis($"[Model] 下载最终失败: {fileName}, 已重试{maxRetries}次");
            if (File.Exists(partPath))
                File.Delete(partPath);
            return null;
        }

        private ManifestDoc ReadManifest()
        {
            string json = File.ReadAllText(_manifestPath);
            return JsonConvert.DeserializeObject<ManifestDoc>(json);
        }

        private static string ComputeSha256(string filePath)
        {
            using (var sha = SHA256.Create())
            using (var stream = File.OpenRead(filePath))
            {
                byte[] hash = sha.ComputeHash(stream);
                return BitConverter.ToString(hash).Replace("-", "").ToLowerInvariant();
            }
        }

        #endregion

        #region Config

        private void LoadDownloadUrl()
        {
            _fileUrls = new Dictionary<string, string>(DefaultFileUrls);

            try
            {
                if (File.Exists(_configPath))
                {
                    string json = File.ReadAllText(_configPath);
                    var config = JsonConvert.DeserializeObject<Dictionary<string, object>>(json);

                    if (config != null && config.TryGetValue("files", out object filesObj) &&
                        filesObj is JObject files)
                    {
                        foreach (var prop in files.Properties())
                        {
                            if (_fileUrls.ContainsKey(prop.Name))
                                _fileUrls[prop.Name] = prop.Value.ToString();
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"[Model] 配置文件解析失败, 使用默认URL: {ex.Message}");
            }

            // Write default config on first run only
            if (!File.Exists(_configPath))
            {
                try
                {
                    Directory.CreateDirectory(_configDir);
                    var filesConfig = new Dictionary<string, string>();
                    foreach (var kv in DefaultFileUrls)
                        filesConfig[kv.Key] = kv.Value;

                    var defaultConfig = new Dictionary<string, object>
                    {
                        { "version", "1.0" },
                        { "files", filesConfig }
                    };
                    File.WriteAllText(_configPath, JsonConvert.SerializeObject(defaultConfig, Formatting.Indented));
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[Model] 写入默认配置失败(非致命): {ex.Message}");
                }
            }
        }

        #endregion
    }
}
