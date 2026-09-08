using System;
using System.Security.Cryptography;
using System.Text;

namespace ReviewAnalysis.Utils
{


    public class AESHelper
    {
        private static string KEY = "1a42bc8d0536e97f";
        private static string IV = "ac3489ef1265bd70";

        /// <summary>
        /// 解密
        /// </summary>
        /// <param name="base64Cipher"></param>
        /// <returns></returns>
        public static string Decrypt(string base64Cipher)
        {
            if (string.IsNullOrEmpty(base64Cipher))
            {
                return base64Cipher;
            }
            byte[] cipherBytes = Convert.FromBase64String(base64Cipher);

            using (Aes aes = Aes.Create())
            {
                aes.Key = Encoding.UTF8.GetBytes(KEY);
                aes.IV = Encoding.UTF8.GetBytes(IV);
                aes.Mode = CipherMode.CBC;
                aes.Padding = PaddingMode.PKCS7;

                using (var decryptor = aes.CreateDecryptor())
                {
                    byte[] result = decryptor.TransformFinalBlock(cipherBytes, 0, cipherBytes.Length);
                    return Encoding.UTF8.GetString(result);
                }
            }
        }
    }

}
