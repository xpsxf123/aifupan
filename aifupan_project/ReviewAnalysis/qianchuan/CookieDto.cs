using CefSharp;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.qianchuan
{
    /// <summary>
    /// Cookie序列化DTO（兼容CefSharp.Cookie）
    /// </summary>
    [Serializable]
    public class CookieDto
    {
        public string Name { get; set; }
        public string Value { get; set; }
        public string Domain { get; set; }
        public string Path { get; set; } = "/";
        public DateTime Expires { get; set; } = DateTime.MinValue;
        public bool Secure { get; set; }
        public bool HttpOnly { get; set; }

        // DTO转CefSharp.Cookie
        public Cookie ToCefCookie()
        {
            return new Cookie
            {
                Name = Name,
                Value = Value,
                Domain = Domain.StartsWith(".") ? Domain : "." + Domain, // Cef要求子域名带.
                Path = Path,
                Expires = Expires,
                Secure = Secure,
                HttpOnly = HttpOnly
            };
        }

        // CefSharp.Cookie转DTO
        public static CookieDto FromCefCookie(Cookie cefCookie)
        {
            return new CookieDto
            {
                Name = cefCookie.Name,
                Value = cefCookie.Value,
                Domain = cefCookie.Domain?.TrimStart('.') ?? string.Empty, // 处理Domain为null的情况
                Path = cefCookie.Path ?? string.Empty, // 处理Path为null的情况
                Expires = cefCookie.Expires ?? DateTime.MinValue,
                Secure = cefCookie.Secure,
                HttpOnly = cefCookie.HttpOnly
            };
        }
    }
}