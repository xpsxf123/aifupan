using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    public class OssUtils
    {

        /// <summary>
        /// 从预签名URL下载文件到本地
        /// </summary>
        /// <param name="presignedUrl">预签名的下载链接</param>
        /// <param name="savePath">保存路径</param>
        /// <returns>是否下载成功</returns>
        public static bool DownloadFileAsync(string presignedUrl, string savePath)
        {
            try
            {
                FileUtils.log($"开始下载文件: {presignedUrl} 到 {savePath}", "诊断报告下载");

                using (HttpClient client = new HttpClient())
                {
                    // 设置超时时间
                    client.Timeout = TimeSpan.FromMinutes(5);

                    // 使用同步方式下载文件并保存
                    using (Stream stream = client.GetStreamAsync(presignedUrl).Result)  // 使用 .Result 阻塞获取下载流
                    using (FileStream fs = new FileStream(savePath, FileMode.Create, FileAccess.Write))
                    {
                        stream.CopyTo(fs);
                    }
                }

                // 检查文件是否下载成功
                if (File.Exists(savePath) && new FileInfo(savePath).Length > 0)
                {
                    FileUtils.log($"文件下载成功: {savePath}", "诊断报告下载");
                    return true;
                }
                else
                {
                    FileUtils.LogError($"文件下载失败或文件大小为0: {savePath}", "诊断报告下载");
                    return false;
                }
            }
            catch (Exception ex)
            {
                string errorMessage = $"下载文件时发生异常: {ex.Message}";
                if (ex.InnerException != null)
                {
                    errorMessage += $", 内部异常: {ex.InnerException.Message}";
                }
                FileUtils.LogError(errorMessage, "诊断报告下载异常");
                return false;
            }
        }

        /// <summary>
        /// 从完整URL下载文件到本地
        /// </summary>
        /// <param name="fullUrl">完整的下载链接</param>
        /// <param name="savePath">保存路径</param>
        /// <returns>是否下载成功</returns>
        public static bool DownloadFileFromFullUrl(string fullUrl, string savePath)
        {
            try
            {
                FileUtils.log($"开始下载文件: {fullUrl} 到 {savePath}", "HTML文件下载");

                using (HttpClient client = new HttpClient())
                {
                    // 设置超时时间
                    client.Timeout = TimeSpan.FromMinutes(5);

                    // 使用同步方式下载文件并保存
                    using (Stream stream = client.GetStreamAsync(fullUrl).Result)  // 使用 .Result 阻塞获取下载流
                    using (FileStream fs = new FileStream(savePath, FileMode.Create, FileAccess.Write))
                    {
                        stream.CopyTo(fs);
                    }
                }

                // 检查文件是否下载成功
                if (File.Exists(savePath) && new FileInfo(savePath).Length > 0)
                {
                    FileUtils.log($"文件下载成功: {savePath}", "HTML文件下载");
                    return true;
                }
                else
                {
                    FileUtils.LogError($"文件下载失败或文件大小为0: {savePath}", "HTML文件下载");
                    return false;
                }
            }
            catch (Exception ex)
            {
                string errorMessage = $"下载文件时发生异常: {ex.Message}";
                if (ex.InnerException != null)
                {
                    errorMessage += $", 内部异常: {ex.InnerException.Message}";
                }
                FileUtils.LogError(errorMessage, "HTML文件下载异常");
                return false;
            }
        }
    }
}
