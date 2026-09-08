using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.Global;

namespace ReviewAnalysis.Utils
{
    public class SignatureHeaders
    {
        public static void AddSignature(HttpRequestMessage httpRequest)
        {
            string body = "";

            if (HttpMethod.Get.Equals(httpRequest.Method) || HttpMethod.Delete.Equals(httpRequest.Method))
            {
                // GET/DELETE请求，body为query string
                var uri = httpRequest.RequestUri;
                body = uri != null ? uri.Query.TrimStart('?') : "";
            }
            else if (HttpMethod.Post.Equals(httpRequest.Method) || HttpMethod.Put.Equals(httpRequest.Method))
            {
                // POST/PUT请求，body为请求体内容
                if (httpRequest.Content != null)
                {
                    // 这里假设Content是StringContent或FormUrlEncodedContent等可同步读取
                    body = httpRequest.Content.ReadAsStringAsync().GetAwaiter().GetResult();
                }
            }
            else
            {
                // 其他方法不处理
                return;
            }

            AddSignatureHeaders(httpRequest, body);
        }

        public static void AddSignatureHeaders(HttpRequestMessage request, string body)
        {
            // 生成请求参数
            string timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds().ToString();
            string nonce = Guid.NewGuid().ToString("N");
            string requestId = Guid.NewGuid().ToString("N");
            string fingerprint = "desktop_" + SystemUtils.GenerateMachineCode();

            // 构建签名字符串
            var signParams = new SortedDictionary<string, string>(StringComparer.Ordinal)
            {
                { "X-App-Id", StringEncryptor.currentAppId },
                { "X-Timestamp", timestamp },
                { "X-Nonce", nonce },
                { "X-RequestId", requestId },
                { "X-Fingerprint", fingerprint }
            };

            // 如果有请求体，添加到签名参数中
            if (!string.IsNullOrEmpty(body))
            {
                signParams.Add("body", body);
            }

            // 构建待签名字符串
            string stringToSign = string.Join("&", signParams.Select(p => $"{p.Key}={p.Value}"));

            // 计算签名
            string signature = GenerateHmacSha256Signature(stringToSign, StringEncryptor.currentAppSecret);

            // 添加请求头
            request.Headers.Add("X-App-Id", StringEncryptor.currentAppId);
            request.Headers.Add("X-Timestamp", timestamp);
            request.Headers.Add("X-Nonce", nonce);
            request.Headers.Add("X-RequestId", requestId);
            request.Headers.Add("X-Fingerprint", fingerprint);
            request.Headers.Add("X-Signature", signature);
            request.Headers.Add("X-Sign-Type", "HMAC-SHA256");
        }

        // 生成HMAC-SHA256签名
        private static string GenerateHmacSha256Signature(string data, string secret)
        {
            using (var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret)))
            {
                byte[] hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(data));
                return Convert.ToBase64String(hash);
            }
        }

        // 生成MD5签名
        private string GenerateMd5Signature(string data, string secret)
        {
            string dataWithSecret = secret + data + secret;
            using (var md5 = MD5.Create())
            {
                byte[] hash = md5.ComputeHash(Encoding.UTF8.GetBytes(dataWithSecret));
                return BitConverter.ToString(hash).Replace("-", "").ToLower();
            }
        }

    }
}
