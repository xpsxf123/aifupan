using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;

namespace ReviewAnalysis.WeChatChannels.Utils
{
    /// <summary>
    /// 微信视频号后端服务器签名工具类
    /// </summary>
    public static class WeChatChannelsServiceSignatureUtil
    {
        /// <summary>
        /// 生成签名
        /// </summary>
        /// <param name="params">签名的参数集合</param>
        /// <param name="secret">密钥</param>
        /// <param name="timestamp">时间戳</param>
        /// <param name="signModel">签名使用的算法类型</param>
        public static string GenerateSignature(Dictionary<string, string> @params, HttpMethod method, long timestamp, string signModel = "MD5", int saltPosition = 0)
        {
            var secret = WeChatChannelsServiceToken.Secret;
            // 参数排序
            var sortedKeys = @params.Keys.Where(k => k != "jy-signature").OrderBy(k => k).ToList();

            var data = string.Empty;
            if (method == HttpMethod.Get)
            {
                // GET 方式的签名字符串
                var sb = new StringBuilder();
                foreach (var key in sortedKeys)
                {
                    sb.Append($"{key}={@params[key]}&");
                }

                sb.Append(timestamp);
                data = sb.ToString();
            }
            else
            {
                // POST 方式的签名字符串
                data = $"{JsonConvert.SerializeObject(@params)}&{timestamp}";
            }

            // 生成签名
            switch (signModel.ToUpper())
            {
                case "HMAC_SHA256":
                    using (var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret)))
                    {
                        var hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(data));
                        return BitConverter.ToString(hash).Replace("-", "").ToUpper();
                    }

                case "SHA256":
                    // 根据实际实现，盐值位置处理逻辑如下：
                    // 当 saltPosition <= 0 时，盐值放在数据开头
                    // 当 saltPosition >= 数据长度时，盐值放在数据末尾
                    // 当 0 < saltPosition < 数据长度时，盐值放在数据中间指定位置
                    using (var sha256 = SHA256.Create())
                    {
                        byte[] dataBytes = Encoding.UTF8.GetBytes(GetSaltedData(data, secret, saltPosition));
                        byte[] hashBytes = sha256.ComputeHash(dataBytes);
                        return BitConverter.ToString(hashBytes).Replace("-", "").ToUpper();
                    }

                case "MD5":
                default:
                    // 根据实际实现，盐值位置处理逻辑如下：
                    // 当 saltPosition <= 0 时，盐值放在数据开头
                    // 当 saltPosition >= 数据长度时，盐值放在数据末尾
                    // 当 0 < saltPosition < 数据长度时，盐值放在数据中间指定位置
                    using (var md5 = MD5.Create())
                    {
                        byte[] dataBytes = Encoding.UTF8.GetBytes(GetSaltedData(data, secret, saltPosition));
                        byte[] hashBytes = md5.ComputeHash(dataBytes);
                        return BitConverter.ToString(hashBytes).Replace("-", "").ToUpper();
                    }
            }
        }

        private static string GetSaltedData(string data, string salt, int saltPosition)
        {
            // 根据实际实现，盐值处理逻辑
            if (saltPosition <= 0)
            {
                // 盐值放在数据开头
                return salt + data;
            }
            else if (saltPosition >= data.Length)
            {
                // 盐值放在数据末尾
                return data + salt;
            }
            else
            {
                // 盐值放在数据中间指定位置
                return data.Substring(0, saltPosition) + salt + data.Substring(saltPosition);
            }
        }
    }
}
