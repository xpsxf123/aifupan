using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.Global;

namespace ReviewAnalysis.Utils
{
    public class StringEncryptor
    {
        // 测试环境
        private static string testAppId = "replay_test";
        private static string testAppSecret = "JneqLXbb1PLs1y28zbbabYh4cPjruM5KG2MzBT3lMXZ286w89KNzZwaBGFP3Pst5"; // 加密后的Secret

        // 预发布环境
        private static string yzAppId = "replay_yz";
        private static string yzAppSecret = "jvz3VpuEMJrliR1rJn6KkRwYwwgQU+ek0EyOUXuATcfXg/Cna5Rc2C6R6pzD1EXC";

        // 正式环境
        private static string prodAppId = "replay_prod";
        private static string prodAppSecret = "TyiJ2I09dB/lhyjPBe9yeq7TO0afcZ4vj6sRx4lWVlCqRL5MIkxem187niqCqGcW";

        // 用于缓存AppId和AppSecret，防止递归调用导致栈溢出
        private static string _currentAppId = null;
        private static string _currentAppSecret = null;

        public static string currentAppSecret
        {
            get
            {
                if (string.IsNullOrEmpty(_currentAppSecret))
                {
                    if (Constant.env == "prod")
                    {
                        _currentAppSecret = Decrypt(prodAppSecret);
                    } 
                    if (Constant.env == "yz")
                    {
                        _currentAppSecret = Decrypt(yzAppSecret);
                    }
                    else
                    {
                        _currentAppSecret = Decrypt(testAppSecret);
                    }
                }
                return _currentAppSecret;
            }
            set
            {
                _currentAppSecret = value;
            }
        }

        public static string currentAppId
        {
            get
            {
                if (string.IsNullOrEmpty(_currentAppId))
                {
                    if (Constant.env == "prod")
                    {
                        _currentAppId = prodAppId;
                    }
                    if (Constant.env == "yz")
                    {
                        _currentAppSecret = yzAppId;
                    }
                    else
                    {
                        _currentAppId = testAppId;
                    }
                }
                return _currentAppId;
            }
            set
            {
                _currentAppId = value;
            }
        }

        private const string Key = "NDU2Mzk0NzkzNDI4NjU5Mg=="; // base64, 原值从base64反转
        private const string Iv = "MTY0MjcxNDIxMzU1MzI1MQ==";

        public static string Encrypt(string plainText)
        {
            using (Aes aesAlg = Aes.Create())
            {
                aesAlg.Key = Encoding.UTF8.GetBytes(Key);
                aesAlg.IV = Encoding.UTF8.GetBytes(Iv);

                ICryptoTransform encryptor = aesAlg.CreateEncryptor(aesAlg.Key, aesAlg.IV);

                byte[] encryptedBytes = encryptor.TransformFinalBlock(
                    Encoding.UTF8.GetBytes(plainText), 0, plainText.Length);

                return Convert.ToBase64String(encryptedBytes);
            }
        }

        public static string base64ToString(string base64String)
        {
            string str = "";
            try
            {
                // 1. 将 Base64 字符串转换为字节数组
                byte[] dataBytes = Convert.FromBase64String(base64String);

                // 2. 将字节数组转换为字符串（使用 UTF-8 编码）
                str = Encoding.UTF8.GetString(dataBytes);
            }
            catch (FormatException ex)
            {
                FileUtils.log("Invalid Base64 string: " + ex.Message);
                FileUtils.LogError(ex.Message, "将 Base64 字符串转换字符串报错");
            }
            return str;
        }

        /// <summary>
        /// 解密
        /// </summary>
        /// <param name="cipherText"></param>
        /// <returns></returns>
        public static string Decrypt(string cipherText)
        {
            using (Aes aesAlg = Aes.Create())
            {
                aesAlg.Key = Encoding.UTF8.GetBytes(base64ToString(Key));
                aesAlg.IV = Encoding.UTF8.GetBytes(base64ToString(Iv));

                ICryptoTransform decryptor = aesAlg.CreateDecryptor(aesAlg.Key, aesAlg.IV);

                byte[] cipherBytes = Convert.FromBase64String(cipherText);
                byte[] plainBytes = decryptor.TransformFinalBlock(cipherBytes, 0, cipherBytes.Length);

                return Encoding.UTF8.GetString(plainBytes);
            }
        }

    }
}
