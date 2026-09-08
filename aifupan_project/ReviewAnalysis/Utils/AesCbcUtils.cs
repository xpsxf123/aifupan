using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    public class AesCbcUtils
    {
        /// <summary>
        /// AES-CBC加密（输出Base64编码的密文，包含IV）
        /// </summary>
        /// <param name="plainText">明文</param>
        /// <param name="key">密钥（16/24/32字节，对应128/192/256位）</param>
        /// <returns>Base64编码的密文（格式：IV+密文）</returns>
        public static string Encrypt(string plainText, byte[] key)
        {
            if (string.IsNullOrEmpty(plainText))
                throw new ArgumentNullException(nameof(plainText));
            if (key == null || (key.Length != 16 && key.Length != 24 && key.Length != 32))
                throw new ArgumentException("密钥长度必须为16/24/32字节（128/192/256位）", nameof(key));

            using (var aes = Aes.Create())
            {
                aes.Mode = CipherMode.CBC;       // 设置CBC模式
                aes.Padding = PaddingMode.PKCS7; // 设置填充模式
                aes.Key = key;
                aes.GenerateIV(); // 生成随机IV（16字节）

                // 创建加密器
                using (var encryptor = aes.CreateEncryptor(aes.Key, aes.IV))
                using (var ms = new MemoryStream())
                {
                    // 先写入IV（解密时需提取）
                    ms.Write(aes.IV, 0, aes.IV.Length);

                    using (var cs = new CryptoStream(ms, encryptor, CryptoStreamMode.Write))
                    using (var sw = new StreamWriter(cs, Encoding.UTF8))
                    {
                        sw.Write(plainText); // 写入明文并加密
                    }

                    // 将IV+密文转为Base64
                    return Convert.ToBase64String(ms.ToArray());
                }
            }
        }

        /// <summary>
        /// AES-CBC解密（从Base64密文中提取IV并解密）
        /// </summary>
        /// <param name="cipherTextBase64">Base64编码的密文（格式：IV+密文）</param>
        /// <param name="key">密钥（16/24/32字节）</param>
        /// <returns>明文</returns>
        public static string Decrypt(string cipherTextBase64, byte[] key)
        {
            if (string.IsNullOrEmpty(cipherTextBase64))
                throw new ArgumentNullException(nameof(cipherTextBase64));
            if (key == null || (key.Length != 16 && key.Length != 24 && key.Length != 32))
                throw new ArgumentException("密钥长度必须为16/24/32字节", nameof(key));

            byte[] cipherBytes = Convert.FromBase64String(cipherTextBase64);
            using (var aes = Aes.Create())
            {
                aes.Mode = CipherMode.CBC;
                aes.Padding = PaddingMode.PKCS7;
                aes.Key = key;

                // 提取IV（前16字节）
                byte[] iv = new byte[16];
                Array.Copy(cipherBytes, 0, iv, 0, iv.Length);
                aes.IV = iv;

                // 创建解密器
                using (var decryptor = aes.CreateDecryptor(aes.Key, aes.IV))
                using (var ms = new MemoryStream())
                {
                    // 写入密文（跳过IV部分）
                    using (var cs = new CryptoStream(ms, decryptor, CryptoStreamMode.Write))
                    {
                        cs.Write(cipherBytes, iv.Length, cipherBytes.Length - iv.Length);
                    }

                    // 将解密后的字节转为字符串
                    return Encoding.UTF8.GetString(ms.ToArray());
                }
            }
        }

        public static string DecryptAes(string base64Cipher, byte[] key, byte[] iv)
        {
            try
            {
                using (var aes = Aes.Create())
                {
                    aes.Mode = CipherMode.CBC; // 假设是 CBC 模式（需与加密一致）
                    aes.Padding = PaddingMode.PKCS7; // 假设是 PKCS7 填充
                    aes.Key = key;
                    aes.IV = iv;

                    var decryptor = aes.CreateDecryptor(aes.Key, aes.IV);
                    byte[] cipherBytes = Convert.FromBase64String(base64Cipher);

                    using (var ms = new MemoryStream(cipherBytes))
                    using (var cs = new CryptoStream(ms, decryptor, CryptoStreamMode.Read))
                    using (var sr = new StreamReader(cs, Encoding.UTF8))
                    {
                        return sr.ReadToEnd(); // 解密成功则返回明文
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"解密失败（参数可能不匹配）：{ex.Message}");
                return null;
            }
        }

        /// <summary>
        /// 从密码生成密钥（基于RFC2898，加盐迭代）
        /// </summary>
        /// <param name="password">密码</param>
        /// <param name="salt">盐（8字节以上）</param>
        /// <param name="keySize">密钥长度（128/192/256）</param>
        /// <returns>生成的密钥</returns>
        public static byte[] GenerateKeyFromPassword(string password, byte[] salt, int keySize = 256)
        {
            if (string.IsNullOrEmpty(password))
                throw new ArgumentNullException(nameof(password));
            if (salt == null || salt.Length < 8)
                throw new ArgumentException("盐长度需≥8字节", nameof(salt));

            using (var deriveBytes = new Rfc2898DeriveBytes(password, salt, 100000, HashAlgorithmName.SHA256))
            {
                return deriveBytes.GetBytes(keySize / 8); // 转换为字节数（128/8=16）
            }
        }


        /// <summary>
        /// 处理Email生成AES的Key/IV（UTF-8，16字节截断/补0）
        /// </summary>
        /// <param name="email">邮箱地址</param>
        /// <returns>16字节的Key/IV数组</returns>
        private static byte[] ProcessEmailToKeyIv(string email)
        {
            if (string.IsNullOrEmpty(email))
                throw new ArgumentNullException(nameof(email), "Email不能为空");

            // 1. Email转UTF-8字节数组
            byte[] emailBytes = Encoding.UTF8.GetBytes(email);
            // 2. 初始化16字节数组（默认全0）
            byte[] keyIv = new byte[16];

            // 3. 复制email字节：超过16取前16，不足补0
            int copyLength = Math.Min(emailBytes.Length, 16);
            Array.Copy(emailBytes, 0, keyIv, 0, copyLength);

            return keyIv;
        }

        /// <summary>
        /// 解密邮箱密码（AES-CBC/PKCS5Padding，Base64密文）
        /// </summary>
        /// <param name="cipherTextBase64">Base64编码的密文</param>
        /// <param name="email">用于生成Key/IV的邮箱地址</param>
        /// <returns>解密后的明文密码</returns>
        public static string DecryptEmailPassword(string cipherTextBase64, string email)
        {
            // 验证参数
            if (string.IsNullOrEmpty(cipherTextBase64))
                throw new ArgumentNullException(nameof(cipherTextBase64), "密文不能为空");
            try
            {
            // 生成Key和IV（Key=IV=处理后的email字节）
            byte[] key = ProcessEmailToKeyIv(email);
            byte[] iv = ProcessEmailToKeyIv(email); // Key和IV相同（按需求）

            // Base64解码密文
            byte[] cipherBytes = Convert.FromBase64String(cipherTextBase64);

            using (Aes aes = Aes.Create())
            {
                aes.Mode = CipherMode.CBC;              // CBC模式
                aes.Padding = PaddingMode.PKCS7;        // PKCS7兼容PKCS5Padding
                aes.Key = key;                          // Email生成的Key
                aes.IV = iv;                            // Email生成的IV

                // 创建解密器
                using (ICryptoTransform decryptor = aes.CreateDecryptor(aes.Key, aes.IV))
                using (MemoryStream ms = new MemoryStream())
                using (CryptoStream cs = new CryptoStream(ms, decryptor, CryptoStreamMode.Write))
                {
                    // 写入密文并解密
                    cs.Write(cipherBytes, 0, cipherBytes.Length);
                    cs.FlushFinalBlock(); // 处理填充字节

                    // 解密结果转UTF-8明文
                    return Encoding.UTF8.GetString(ms.ToArray());
                }
                }
            }
            catch(Exception ex)
            {
                FileUtils.log($"解密失败（参数可能不匹配）：{ex.Message}");
                return string.Empty;
            }
            
        }

        // ========== 辅助：生成测试加密密文（用于验证解密逻辑） ==========
        //public static string EncryptForTest(string plainPassword, string email)
        //{
        //    byte[] key = ProcessEmailToKeyIv(email);
        //    byte[] iv = ProcessEmailToKeyIv(email);

        //    using (Aes aes = Aes.Create())
        //    {
        //        aes.Mode = CipherMode.CBC;
        //        aes.Padding = PaddingMode.PKCS7;
        //        aes.Key = key;
        //        aes.IV = iv;

        //        using (ICryptoTransform encryptor = aes.CreateEncryptor())
        //        using (MemoryStream ms = new MemoryStream())
        //        using (CryptoStream cs = new CryptoStream(ms, encryptor, CryptoStreamMode.Write))
        //        using (StreamWriter sw = new StreamWriter(cs, Encoding.UTF8))
        //        {
        //            sw.Write(plainPassword);
        //        }

        //        return Convert.ToBase64String(ms.ToArray());
        //    }
        //}
    }
}
