using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.vo;

namespace ReviewAnalysis.upload
{
    public class UploadUtils
    {

        /// <summary>
        /// 导出、下载文件位置
        /// </summary>
        public static string uploadsFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "uploads");

        /// <summary>
        /// 文件生成完后的后置处理
        /// </summary>
        /// <param name="savePath">本地文件保存地址</param>
        /// <param name="finalFileName">文件名称</param>
        /// <param name="Parameters"></param>
        public static void uploadsPostProcessing(string savePath, string finalFileName, Dictionary<string, string> Parameters)
        {
            // 在这里打印出前端传入的参数
            int uploadType = 0;
            string otherObjStr = "";
            bool notFolder = true; // 打开文件夹
            string videoId = "";

            // 处理otherObj参数
            if (Parameters.ContainsKey("otherObj"))
            {
                otherObjStr = Parameters["otherObj"];

                try
                {
                    // 解析JSON字符串
                    dynamic otherObj = JsonConvert.DeserializeObject(otherObjStr);

                    // 提取videoId
                    if (otherObj.videoId != null)
                    {
                        videoId = otherObj.videoId.ToString();
                    }

                    // 提取notFolder
                    if (otherObj.notFolder != null)
                    {
                        int notFolderValue = Convert.ToInt32(otherObj.notFolder);
                        notFolder = notFolderValue != 1;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError(ex.Message, "解析otherObj失败");
                }
            }

            // 处理uploadType参数
            if (Parameters.ContainsKey("uploadType"))
            {
                // 只有是诊断报告才要上传oss
                int.TryParse(Parameters["uploadType"], out uploadType);
            }
            // 处理
            uploadsPostProcessing(savePath, finalFileName, uploadType, videoId, notFolder);
        }
        
        /// <summary>
        /// 文件生成完后的后置处理
        /// </summary>
        /// <param name="savePath">本地保存地址</param>
        /// <param name="finalFileName">文件名称</param>
        /// <param name="uploadType">上传类型 0：普通类型，1：内容诊断，2：数据诊断</param>
        /// <param name="videoId">视频id</param>
        /// <param name="notFolder">是否打开文件夹</param>
        public static void uploadsPostProcessing(string savePath, string finalFileName, int? uploadType, string videoId, bool notFolder = true)
        {
            // 在这里打印出前端传入的参数
            uploadType = uploadType ?? 0;
            // 处理uploadType参数
            // 只有是诊断报告才要上传oss
            if ((uploadType == 1 || uploadType == 2) && !string.IsNullOrEmpty(videoId))
            {
                // 把文件上传到oss上
                diagnosisPut(videoId, savePath, finalFileName, uploadType);
            }

            // 根据notFolder决定是否打开文件夹
            if (notFolder)
            {
                FileUtils.openFile(savePath);
            }
        }

        /// <summary>
        /// 获取文件名称的全路径
        /// </summary>
        /// <param name="fileNameWithoutExtension">文件名称-不带后缀</param>
        /// <param name="suffixName">后缀</param>
        /// <returns></returns>
        public static string getCurrentFileName(string fileNameWithoutExtension, string suffixName)
        {
            // Ensure directory exists
            Directory.CreateDirectory(uploadsFilePath);

            String finalFileName = $"{fileNameWithoutExtension}{suffixName}";

            // 检查文件是否存在，如果存在则添加编号
            string savePath = Path.Combine(uploadsFilePath, finalFileName);
            int counter = 0;

            while (File.Exists(savePath))
            {
                if (counter == 0)
                {
                    finalFileName = $"{fileNameWithoutExtension}{suffixName}";
                }
                else
                {
                    finalFileName = $"{fileNameWithoutExtension}({counter}){suffixName}";
                }
                savePath = Path.Combine(uploadsFilePath, finalFileName);
                counter++;
            }
            return finalFileName;
        }


        /// <summary>
        /// 把文件上到oss上
        /// </summary>
        /// <param name="videoId"></param>
        /// <exception cref="NotImplementedException"></exception>
        private static void diagnosisPut(string videoId, string filePath, string finalFileName, int? uploadType)
        {
            try
            {
                // 获取预签名URL
                SignUploadUrlVo vo = DiagnosisApi.getDiagnosisSignUploadUrl(videoId, 0, uploadType);

                if (vo != null && !string.IsNullOrEmpty(vo.signedUrl))
                {
                    // 打印URL信息用于调试
                    FileUtils.log($"预签名URL: {vo.signedUrl}", "诊断报告上传");
                    
                    if (UploadFileAsync(vo.signedUrl, filePath))
                    {
                        if (uploadType == 1)
                        {
                            DiagnosisApi.updateHasDiagnosisReport(videoId, 1, finalFileName);
                        }
                        else if (uploadType == 2)
                        {
                            DiagnosisApi.updateHasDataDiagnosisReport(videoId, 1, finalFileName);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                string errorMessage = $"上传文件到OSS时发生异常: {ex.Message}";
                FileUtils.log(errorMessage);
                FileUtils.LogError(errorMessage, "诊断报告上传异常");
            }
        }

        /// <summary>
        /// 使用预签名上传链接上传文件到OSS
        /// </summary> 
        /// <param name="presignedUrl">预签名的上传链接</param>
        /// <param name="filePath">本地文件路径</param> 
        /// <returns>是否上传成功</returns> 
        public static bool UploadFileAsync(string presignedUrl, string filePath)
        {
            return UploadFileAsync(presignedUrl, filePath, null);
        }

        /// <summary>
        /// 使用预签名上传链接上传文件到OSS
        /// </summary> 
        /// <param name="presignedUrl">预签名的上传链接</param>
        /// <param name="filePath">本地文件路径</param>
        /// <param name="contentType">类型</param>
        /// <returns>是否上传成功</returns>
        public static bool UploadFileAsync(string presignedUrl, string filePath, string contentType)
        {
            try
            {
                // 检查文件是否存在
                if (!File.Exists(filePath))
                {
                    FileUtils.LogError($"文件不存在: {filePath}", "OSS上传失败");
                    return false;
                }

                // 获取文件内容
                byte[] fileBytes = File.ReadAllBytes(filePath);

                // 创建HttpClient实例
                using (var fileStream = new FileStream(filePath, FileMode.Open, FileAccess.Read))
                using (HttpClient client = new HttpClient())
                {
                    // 设置超时时间
                    client.Timeout = TimeSpan.FromMinutes(10);

                    // 创建HttpRequestMessage
                    using (HttpRequestMessage request = new HttpRequestMessage(HttpMethod.Put, presignedUrl))
                    {
                        // 设置内容
                        request.Content = new StreamContent(fileStream);

                        // 根据文件扩展名设置Content-Type
                        //string extension = Path.GetExtension(filePath).ToLower();
                        //string contentType = GetContentType(extension);
                        if (!string.IsNullOrEmpty(contentType))
                        {
                            request.Content.Headers.ContentType = new MediaTypeHeaderValue(contentType);
                            if ("text/html".Equals(contentType))
                            {
                                string fileName = Path.GetFileName(filePath);
                                request.Content.Headers.TryAddWithoutValidation("Content-Disposition", $"inline");
                            }
                        }

                        // 记录上传开始
                        FileUtils.log($"开始上传文件: {filePath}, 大小: {fileBytes.Length} 字节", "OSS上传");

                        // 发送请求
                        HttpResponseMessage response = client.SendAsync(request).Result;

                        // 检查响应状态
                        if (response.IsSuccessStatusCode)
                        {
                            FileUtils.log($"文件上传成功: {filePath}", "OSS上传");
                            return true;
                        }
                        else
                        {
                            string errorMessage = $"上传失败，状态码: {response.StatusCode}, 原因: {response.ReasonPhrase}";
                            FileUtils.LogError(errorMessage, "OSS上传失败");
                            return false;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                string errorMessage = $"上传文件时发生异常: {ex.Message}";
                if (ex.InnerException != null)
                {
                    errorMessage += $", 内部异常: {ex.InnerException.Message}";
                }
                FileUtils.LogError(errorMessage, "OSS上传异常");
                return false;
            }
        }

        /// <summary>
        /// 根据文件扩展名获取MIME类型
        /// </summary>
        /// <param name="extension">文件扩展名</param>
        /// <returns>MIME类型</returns>
        private static string GetContentType(string extension)
        {
            Dictionary<string, string> mimeTypes = new Dictionary<string, string>
            {
                { ".jpg", "image/jpeg" },
                { ".jpeg", "image/jpeg" },
                { ".png", "image/png" },
                { ".gif", "image/gif" },
                { ".pdf", "application/pdf" },
                { ".doc", "application/msword" },
                { ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
                { ".xls", "application/vnd.ms-excel" },
                { ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
                { ".ppt", "application/vnd.ms-powerpoint" },
                { ".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation" },
                { ".txt", "text/plain" },
                { ".zip", "application/zip" },
                { ".rar", "application/x-rar-compressed" },
                { ".7z", "application/x-7z-compressed" },
                { ".mp4", "video/mp4" },
                { ".mp3", "audio/mpeg" },
                { ".html", "text/html" }

            };

            if (mimeTypes.ContainsKey(extension))
            {
                return mimeTypes[extension];
            }

            // 默认返回二进制流
            return "application/octet-stream";
        }
    }
}
