using System;
using System.IO;
using System.Text;
using douyin.Utils;

namespace ReviewAnalysis.juliang
{
    /// <summary>
    /// 签名帮助类，用于生成 fp、msToken、a_bogus 等签名参数
    /// </summary>
    public class SignatureHelper
    {
        private static readonly string CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        
        // 缓存 JS 代码，避免重复读取文件（引擎不可缓存，bdms.js 内部状态会在调用后改变）
        private static string _jsCode;
        private static readonly object _jsLock = new object();

        /// <summary>
        /// 生成 verifyFp 参数
        /// </summary>
        public static string GetFp()
        {
            var timeStamp = DateTimeOffset.Now.ToUnixTimeSeconds().ToString("x");
            var random = new Random();
            var verifyIdParts = new char[36];
            verifyIdParts[8] = verifyIdParts[13] = verifyIdParts[18] = verifyIdParts[23] = '_';
            verifyIdParts[14] = '4';

            for (int i = 0; i < 36; i++)
            {
                if (verifyIdParts[i] == '\0')
                {
                    int randIndex = random.Next(CHARS.Length);
                    if (i == 19)
                    {
                        verifyIdParts[i] = CHARS[(3 & randIndex) | 8];
                    }
                    else
                    {
                        verifyIdParts[i] = CHARS[randIndex];
                    }
                }
            }

            string verifyId = new string(verifyIdParts);
            return $"verify_{timeStamp}_{verifyId}";
        }

        /// <summary>
        /// 生成 msToken 参数
        /// </summary>
        public static string GetMsToken(int length = 172)
        {
            var random = new Random();
            var sb = new StringBuilder();
            for (int i = 0; i < length; i++)
            {
                sb.Append(CHARS[random.Next(CHARS.Length)]);
            }
            return sb.ToString();
        }

        /// <summary>
        /// 通过 bdms.js 生成 a_bogus 签名
        /// </summary>
        public static string GetABogus(string ua, string paramsStr, string dataStr = "")
        {
            try
            {
                // 懒加载 JS 代码（只读一次文件）
                if (_jsCode == null)
                {
                    lock (_jsLock)
                    {
                        if (_jsCode == null)
                        {
                            string jsPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "script", "bdms.js");
                            if (!File.Exists(jsPath))
                            {
                                FileUtils.LogRpa($"bdms.js not found at: {jsPath}", "签名");
                                return string.Empty;
                            }

                            _jsCode = File.ReadAllText(jsPath);
                            FileUtils.LogRpa($"bdms.js loaded, length={_jsCode.Length}", "签名");
                        }
                    }
                }

                // 每次创建新引擎（bdms.js 内部状态会在 main() 调用后改变，不可复用）
                var engine = new Jint.Engine();
                engine.Execute(_jsCode);

                var result = engine.Invoke("main", ua, paramsStr, dataStr);
                string aBogus = result?.ToString();
                if (string.IsNullOrEmpty(aBogus) || aBogus == "undefined" || aBogus == "null")
                {
                    FileUtils.LogRpa($"GetABogus 返回无效值: '{aBogus}'", "签名");
                    return string.Empty;
                }
                return aBogus;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetABogus error: {ex.Message}", "签名");
                return string.Empty;
            }
        }
    }
}
