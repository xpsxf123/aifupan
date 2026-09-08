using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class WindowsUtils
    {

        /// <summary>
        /// 将文件名称的违规符号去掉
        /// </summary>
        /// <param name="folderName">文件名称</param>
        /// <returns></returns>
        public static string SanitizeForFolderName(string folderName)
        {

            if (string.IsNullOrEmpty(folderName))
            {
                // 名称为空，改成随机uuid字符串
                return Guid.NewGuid().ToString().Replace("-", "");
            }

            // 替换非法字符
            string invalidChars = Regex.Escape(new string(Path.GetInvalidFileNameChars()));
            Regex illegalCharsRegex = new Regex($"[{invalidChars}]", RegexOptions.Compiled);
            string sanitized = illegalCharsRegex.Replace(folderName, "");

            // 移除颜文字/emoji（代理对字符，UTF-16中U+10000以上的字符通过代理对表示）
            sanitized = Regex.Replace(sanitized, @"\p{Cs}", "");

            // 处理保留名称（不区分大小写）
            Regex reservedNamesRegex = new Regex(
                @"^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\..*)?$",
                RegexOptions.IgnoreCase | RegexOptions.Compiled
            );
            if (reservedNamesRegex.IsMatch(sanitized))
            {
                sanitized += "_";
            }
                

            // 去除首尾空格和点，并处理连续点
            sanitized = sanitized.TrimStart(' ', '.');
            sanitized = sanitized.TrimEnd(' ', '.');
            sanitized = Regex.Replace(sanitized, @"\.{2,}", "."); // 处理多个连续点

            if(string.IsNullOrEmpty(sanitized) || sanitized.Length > 255)
            {
                // 去掉特殊符号后名称为空，改成随机uuid字符串
                sanitized = Guid.NewGuid().ToString().Replace("-", "");
            }

            return sanitized;
        }
    }
}
