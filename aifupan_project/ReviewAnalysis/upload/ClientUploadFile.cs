using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.BeanCache;
using ReviewAnalysis.vo;
using System.Net.Http;
using System.Diagnostics;
using System.Net.Http.Headers;
using System.Threading;
using ReviewAnalysis.Utils;
using System.Reflection;

namespace ReviewAnalysis.upload
{
    public class ClientUploadFile
    {

        /// <summary>
        /// wkhtmltopdf.exe的文件位置
        /// </summary>
        public static string wkhtmltopdfPath = Path.GetFullPath("tools\\wkhtmltopdf.exe");

        /// <summary>
        /// 文件上传
        /// </summary>
        /// <param name="request"></param>
        /// <param name="response"></param>
        public void frontUpload(HttpListenerRequest request, HttpListenerResponse response)
        {
            try
            {
                if (!request.HasEntityBody)
                {
                    response.StatusCode = 400;
                    return;
                }

                // Check if content type is multipart/form-data
                if (!request.ContentType.StartsWith("multipart/form-data", StringComparison.OrdinalIgnoreCase))
                {
                    response.StatusCode = 415; // Unsupported Media Type
                    return;
                }

                string boundary = GetBoundary(request.ContentType);
                if (string.IsNullOrEmpty(boundary))
                {
                    response.StatusCode = 400;
                    return;
                }

                // 解析multipart/form-data请求
                FormDataResult formData = ParseMultipartFormData(request, boundary);

                // 获取文件信息
                if (!formData.Files.Any())
                {
                    response.StatusCode = 400;
                    return;
                }

                FileData fileData = formData.Files.First();
                string originalFileName = fileData.FileName;
                string fileExtension = Path.GetExtension(originalFileName);
                string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(originalFileName);

                // Ensure directory exists
                Directory.CreateDirectory(UploadUtils.uploadsFilePath);

                string savePath = "";
                string finalFileName = "";
                // 保存文件
                if (formData.Parameters.ContainsKey("generationType"))
                {
                    string generationType = formData.Parameters["generationType"];
                    if ("0".Equals(generationType))
                    {
                        if (!File.Exists(wkhtmltopdfPath))
                        {
                            CustomException.create("wkhtmltopdf.exe文件丢失，请联系统管理员");
                        }

                        if (fileExtension.ToLower().EndsWith("html"))
                        {
                            Directory.CreateDirectory(Path.GetFullPath("temp"));
                            string htmlOut = Path.GetFullPath($"temp\\{Guid.NewGuid().ToString("N")}.html");
                            try
                            {
                                // 获取文件名称
                                finalFileName = UploadUtils.getCurrentFileName(fileNameWithoutExtension, ".pdf");
                                savePath = Path.Combine(UploadUtils.uploadsFilePath, finalFileName);
                                File.WriteAllBytes(htmlOut, fileData.FileContent);

                                string Arguments = $"" +
                                    //$"--header-html {Path.GetFullPath("tools\\pdf-watermark.html")} " +
                                    $"--margin-top 20mm " +
                                    $" {htmlOut} {savePath}";
                                // 创建一个新的ProcessStartInfo对象
                                ProcessStartInfo processStartInfo = new ProcessStartInfo
                                {
                                    FileName = wkhtmltopdfPath,
                                    Arguments = Arguments,
                                    RedirectStandardOutput = true,
                                    CreateNoWindow = true,
                                    UseShellExecute = false
                                };
                                processStartInfo.Verb = "runas"; // 指定以管理员权限运行
                                Process process = new Process();
                                process.StartInfo = processStartInfo;
                                process.Start();

                                process.WaitForExit();

                                // 可以获取进程的退出代码 
                                int exitCode = process.ExitCode;
                                // 关闭进程资源 
                                process.Close();
                            }
                            catch(Exception ex)
                            {
                                FileUtils.LogError(ex.Message, "wkhtmltopdfz转出pdf报错");
                                CustomException.create("生成失败，请重新生成");
                            }
                            finally
                            {
                                //File.Delete(htmlOut);
                            }
                        }
                        else
                        {
                            CustomException.create("流文件名必须为html类型");
                        }
                    }
                    else if ("1".Equals(generationType))
                    {
                        // 获取文件名称
                        finalFileName = UploadUtils.getCurrentFileName(fileNameWithoutExtension, fileExtension);
                        savePath = Path.Combine(UploadUtils.uploadsFilePath, finalFileName); 
                        File.WriteAllBytes(savePath, fileData.FileContent);
                    }
                    else
                    {
                        CustomException.create("generationType类型未知");
                    }
                }
                else
                {
                    FileUtils.LogError(JsonConvert.SerializeObject(formData.Parameters), "generationType参数为空");
                    CustomException.create("generationType参数为空");
                }

                // 生成文件的后置处理
                UploadUtils.uploadsPostProcessing(savePath, finalFileName, formData.Parameters);

                string jsonResponse = JsonConvert.SerializeObject(new HttpReponse(0, "成功", ""));
                
                response.StatusCode = 200;
                response.ContentType = "application/json";
                byte[] responseBytes = Encoding.UTF8.GetBytes(jsonResponse);
                response.OutputStream.Write(responseBytes, 0, responseBytes.Length);
            }
            catch (Exception ex)
            {
                response.StatusCode = 200;
                response.ContentType = "application/json";
                byte[] errorBytes = Encoding.UTF8.GetBytes(JsonConvert.SerializeObject(new HttpReponse(7005, ex.Message, null)));
                response.OutputStream.Write(errorBytes, 0, errorBytes.Length);
            }
            finally
            {
                response.OutputStream.Close();
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
                        string extension = Path.GetExtension(filePath).ToLower();
                        string contentType = GetContentType(extension);
                        //request.Content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream");

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
                { ".mp3", "audio/mpeg" }
            };
            
            if (mimeTypes.ContainsKey(extension))
            {
                return mimeTypes[extension];
            }
            
            // 默认返回二进制流
            return "application/octet-stream";
        }

        /// <summary>
        /// 从Content-Type获取boundary
        /// </summary>
        private string GetBoundary(string contentType)
        {
            if (string.IsNullOrEmpty(contentType))
                return null;

            string[] elements = contentType.Split(';');
            foreach (string element in elements)
            {
                string trimmedElement = element.Trim();
                if (trimmedElement.StartsWith("boundary="))
                {
                    return trimmedElement.Substring("boundary=".Length).Trim('"');
                }
            }
            return null;
        }

        /// <summary>
        /// 解析multipart/form-data请求
        /// </summary>
        private FormDataResult ParseMultipartFormData(HttpListenerRequest request, string boundary)
        {
            FormDataResult result = new FormDataResult();
            
            // 读取整个请求内容
            byte[] requestData;
            using (MemoryStream memoryStream = new MemoryStream())
            {
                request.InputStream.CopyTo(memoryStream);
                requestData = memoryStream.ToArray();
            }
            
            // 构建分隔符字节数组
            byte[] boundaryBytes = Encoding.ASCII.GetBytes("--" + boundary + "\r\n");
            byte[] finalBoundaryBytes = Encoding.ASCII.GetBytes("--" + boundary + "--");
            
            // 查找所有分隔符的位置
            List<int> boundaryPositions = new List<int>();
            for (int i = 0; i < requestData.Length - boundaryBytes.Length; i++)
            {
                bool isMatch = true;
                for (int j = 0; j < boundaryBytes.Length; j++)
                {
                    if (requestData[i + j] != boundaryBytes[j])
                    {
                        isMatch = false;
                        break;
                    }
                }
                
                if (isMatch)
                {
                    boundaryPositions.Add(i);
                    i += boundaryBytes.Length - 1;
                }
            }
            
            // 查找结束分隔符位置
            int finalBoundaryPosition = -1;
            for (int i = 0; i < requestData.Length - finalBoundaryBytes.Length; i++)
            {
                bool isMatch = true;
                for (int j = 0; j < finalBoundaryBytes.Length; j++)
                {
                    if (requestData[i + j] != finalBoundaryBytes[j])
                    {
                        isMatch = false;
                        break;
                    }
                }
                
                if (isMatch)
                {
                    finalBoundaryPosition = i;
                    break;
                }
            }
            
            // 处理每个部分
            for (int i = 0; i < boundaryPositions.Count; i++)
            {
                int startPos = boundaryPositions[i] + boundaryBytes.Length;
                int endPos = (i < boundaryPositions.Count - 1) ? boundaryPositions[i + 1] : finalBoundaryPosition;
                
                if (endPos <= startPos) continue;
                
                // 查找头部和内容的分隔位置
                int headerEndPos = -1;
                for (int j = startPos; j < endPos - 3; j++)
                {
                    if (requestData[j] == '\r' && requestData[j + 1] == '\n' && 
                        requestData[j + 2] == '\r' && requestData[j + 3] == '\n')
                    {
                        headerEndPos = j;
                        break;
                    }
                }
                
                if (headerEndPos == -1) continue;
                
                // 提取头部信息
                string headers = Encoding.UTF8.GetString(requestData, startPos, headerEndPos - startPos);
                
                // 提取内容（二进制数据）
                int contentStartPos = headerEndPos + 4; // 跳过\r\n\r\n
                int contentLength = endPos - contentStartPos - 2; // 减去结尾的\r\n
                
                // 检查是否为文件
                if (headers.Contains("filename="))
                {
                    // 提取文件名
                    Match fileNameMatch = Regex.Match(headers, @"filename=""?([^""\r\n]+)""?");
                    string fileName = fileNameMatch.Success ? fileNameMatch.Groups[1].Value : "unknown";
                    
                    // 提取Content-Type
                    Match contentTypeMatch = Regex.Match(headers, @"Content-Type:\s*([^\r\n]+)");
                    string contentType = contentTypeMatch.Success ? contentTypeMatch.Groups[1].Value : "application/octet-stream";
                    
                    // 提取表单字段名
                    Match nameMatch = Regex.Match(headers, @"name=""?([^""\r\n]+)""?");
                    string fieldName = nameMatch.Success ? nameMatch.Groups[1].Value : "file";
                    
                    // 直接复制二进制内容
                    byte[] fileContent = new byte[contentLength];
                    Array.Copy(requestData, contentStartPos, fileContent, 0, contentLength);
                    
                    result.Files.Add(new FileData
                    {
                        FieldName = fieldName,
                        FileName = fileName,
                        ContentType = contentType,
                        FileContent = fileContent
                    });
                }
                else // 普通表单参数
                {
                    // 提取表单字段名
                    Match nameMatch = Regex.Match(headers, @"name=""?([^""\r\n]+)""?");
                    if (nameMatch.Success)
                    {
                        string fieldName = nameMatch.Groups[1].Value;
                        string value = Encoding.UTF8.GetString(requestData, contentStartPos, contentLength);
                        
                        result.Parameters[fieldName] = value;
                    }
                }
            }
            
            return result;
        }

        /// <summary>
        /// 表单数据结果类
        /// </summary>
        private class FormDataResult
        {
            public Dictionary<string, string> Parameters { get; } = new Dictionary<string, string>();
            public List<FileData> Files { get; } = new List<FileData>();
        }

        /// <summary>
        /// 文件数据类
        /// </summary>
        private class FileData
        {
            public string FieldName { get; set; }
            public string FileName { get; set; }
            public string ContentType { get; set; }
            public byte[] FileContent { get; set; }
        }
    }
}
