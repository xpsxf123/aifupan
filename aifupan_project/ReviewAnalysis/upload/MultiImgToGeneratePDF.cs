using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Net;
using System.Text;
using PdfSharp.Pdf;
using PdfSharp.Drawing;
using douyin.Utils;
using System.Text.RegularExpressions;
using Newtonsoft.Json;
using ReviewAnalysis.BeanCache;

namespace ReviewAnalysis.upload
{
    public class MultiImgToGeneratePDF
    {
        public void multiImgToGeneratePDF(HttpListenerRequest request, HttpListenerResponse response)
        {
            try
            {
                if (!request.HasEntityBody)
                {
                    response.StatusCode = 400;
                    WriteResponse(response, "请求体为空", 3002);
                    return;
                }

                if (!request.ContentType.StartsWith("multipart/form-data", StringComparison.OrdinalIgnoreCase))
                {
                    response.StatusCode = 415;
                    WriteResponse(response, "Content-Type必须为multipart/form-data", 3002);
                    return;
                }

                string boundary = GetBoundary(request.ContentType);
                if (string.IsNullOrEmpty(boundary))
                {
                    response.StatusCode = 400;
                    WriteResponse(response, "未找到boundary", 3002);
                    return;
                }

                // 解析multipart/form-data请求
                FormDataResult formData = ParseMultipartFormData(request, boundary);

                // 获取所有图片
                List<FileData> images = formData.Files;
                if (images.Count == 0)
                {
                    response.StatusCode = 400;
                    WriteResponse(response, "未上传图片", 3002);
                    return;
                }

                // 对images排序，文件名称升序排序
                images.Sort((a, b)=>
                {
                    int t1 = 0;
                    int t2 = 0;
                    string fileName1 = Path.GetFileNameWithoutExtension(a.FileName);
                    string fileName2 = Path.GetFileNameWithoutExtension(b.FileName);
                    int.TryParse(fileName1, out t1);
                    int.TryParse(fileName2, out t2);
                    return t1.CompareTo(t2);
                });

                // 获取fileName字段
                string fileName = formData.Parameters.ContainsKey("fileName") ? formData.Parameters["fileName"] : "导出PDF文件.pdf";
                if (string.IsNullOrEmpty(fileName))
                {
                    response.StatusCode = 400;
                    WriteResponse(response, "未传fileName字段", 3002);
                    return;
                }

                // 判断对应的文件夹存在
                string fileNameWithoutExtension = Path.GetFileNameWithoutExtension(fileName);
                fileName = UploadUtils.getCurrentFileName(fileNameWithoutExtension, ".pdf");

                // 生成保存路径
                string savePath = Path.Combine(UploadUtils.uploadsFilePath, fileName);
                Directory.CreateDirectory(UploadUtils.uploadsFilePath);

                // 合成PDF并保存
                using (PdfDocument pdf = new PdfDocument())
                {
                    foreach (var item in images)
                    {
                        var img = item.img;
                        PdfPage page = pdf.AddPage();
                        page.Width = img.Width;
                        page.Height = img.Height;
                        using (XGraphics gfx = XGraphics.FromPdfPage(page))
                        {
                            using (MemoryStream ms = new MemoryStream())
                            {
                                img.Save(ms, img.RawFormat);
                                ms.Position = 0;
                                using (XImage xImg = XImage.FromStream(ms))
                                {
                                    gfx.DrawImage(xImg, 0, 0, img.Width, img.Height);
                                }
                            }
                        }
                    }
                    pdf.Save(savePath);
                }

                // 生成文件的后置处理
                UploadUtils.uploadsPostProcessing(savePath, fileName, formData.Parameters);

                // 返回成功信息
                response.StatusCode = 200;
                WriteResponse(response, "PDF生成成功", 0);
            }
            catch (Exception ex)
            {
                response.StatusCode = 200;
                FileUtils.LogError($"message = {ex.Message}， StackTrace = {ex.StackTrace}", "多张图片合成pdf报错");
                WriteResponse(response, "生成PDF失败: " + ex.Message, 30002);
            }
            finally
            {
                response.OutputStream.Close();
            }
        }

        private void WriteResponse(HttpListenerResponse response, string msg, int code)
        {
            response.ContentType = "application/json";
            string jsonResponse = JsonConvert.SerializeObject(new HttpReponse(code, msg, ""));
            byte[] bytes = Encoding.UTF8.GetBytes(jsonResponse);
            response.OutputStream.Write(bytes, 0, bytes.Length);
        }

        private string GetBoundary(string contentType)
        {
            string[] elements = contentType.Split(';');
            foreach (string element in elements)
            {
                string trimmed = element.Trim();
                if (trimmed.StartsWith("boundary="))
                {
                    return trimmed.Substring("boundary=".Length).Trim('"');
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
                    FileData file = new FileData 
                    {
                        FieldName = fieldName,
                        FileName = fileName,
                        ContentType = contentType
                    };

                    try
                    {
                        using (MemoryStream ms = new MemoryStream(fileContent))
                        {
                            file.img = Image.FromStream(ms);
                        }
                    }
                    catch
                    {
                        /* 非图片文件自动跳过 */
                        continue;
                    }
                    result.Files.Add(file);
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
            public Image img { get; set; }
        }
    }
}
