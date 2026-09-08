using COSXML;
using COSXML.Auth;
using COSXML.Model.Object;
using COSXML.Model.Bucket;
using COSXML.CosException;
using ReviewAnalysis.Model;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using COSXML.Common;
using Swan.Parsers;
using System.Net.Http;
using Newtonsoft.Json;
using douyin.Utils;
using System.IO;
using System.IO.Compression;
using ReviewAnalysis.Utils;
using System.Drawing;
using Swan;

namespace ReviewAnalysis.Asr
{

    public class TencentCosToken
    {

        /// <summary>
        /// 临时token
        /// </summary>
        public string token;
        /// <summary>
        /// 临时SecretId
        /// </summary>
        public string tempSecretId;
        /// <summary>
        /// 临时SecretKey
        /// </summary>
        public string tempSecretKey;
        /// <summary>
        /// 存储区域
        /// </summary>
        public string region;
        /// <summary>
        /// 存储桶名称
        /// </summary>
        public string bucketName;
    }
    public class TencentCosUtils
    {
        /// <summary>
        /// 去服务器获取cos的临时token
        /// </summary>
        /// <returns></returns>
        public static TencentCosToken HttpGetCosToken()
        {
            try
            {
                var request = new HttpRequestMessage(HttpMethod.Get, ReplayHttpUtils.BaseUrl + "/openapi/v1930/cosUploadTempToken")
                {
                    Headers =
                    {
                        { "token", ReplayHttpUtils.Token }
                    }
                };

                using(HttpClient Client = new HttpClient())
                {
                    var response = Client.SendAsync(request).Result;

                    if (!response.IsSuccessStatusCode)
                    {
                        FileUtils.log($"获取服务器时间失败： {response}.");
                        throw new Exception("网络不佳，请稍后重试");
                    }

                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    if (!string.IsNullOrEmpty(responseBody))
                    {
                        var jo = JsonConvert.DeserializeObject<dynamic>(responseBody);

                        if (jo.code == 0)
                        {
                            TencentCosToken jsonObject = JsonConvert.DeserializeObject<TencentCosToken>(jo.data.ToString());
                            return jsonObject;
                        }
                    }
                }
                
                
            }
            catch (Exception ex)
            {
                FileUtils.log(ex.Message);
                FileUtils.log($"{ex.Message}", "获取cos临时token");
            }
            return null;
        }

        /// <summary>
        /// websocket采集文件上传cos
        /// </summary>
        /// <param name="filePath"></param>
        /// <returns></returns>
        public static string uploadFile(string filePath, string prefix = "socketMessageFile")
        {
            if (!string.IsNullOrEmpty(filePath) && File.Exists(filePath))
            {
                try
                {
                    // 获取文件的父路径
                    DirectoryInfo parentDir = new FileInfo(filePath).Directory;
                    string zipName = $"{Guid.NewGuid().ToString()}.zip";
                    string zipNamePath = $"{parentDir.FullName}/{zipName}";
                    FileUtils.ZipFileToOneFile(filePath, zipNamePath);
                    string time = ServerTimeUtils.getCurrentTimeStr();
                    DateTime dateTime = DateTime.ParseExact(time, "yyyy-MM-dd HH:mm:ss", null);
                    string path = $"{prefix}/{dateTime.ToString("yyyy/MM/dd/") + zipName}";
                    bool isUpload= upload(zipNamePath, path);
                    
                    File.Delete(zipNamePath);
                    if (isUpload)
                    {
                        return path;
                    }
                }
                catch (Exception e)
                {
                    FileUtils.log($"创建websocket-json的zip包出错={e.Message}");
                }
            }
            return null;
        }

        /// <summary>
        /// 下载websocket采集的json文件
        /// </summary>
        /// <param name="cosKey"></param>
        /// <param name="localDir"></param>
        /// <param name="localFileName"></param>
        /// <returns></returns>
        public static bool downloadWebcsocketJsonFile(string cosKey, string localDir, string localFileName)
        {
            try
            {
                TencentCosToken tempToken = HttpGetCosToken();
                if (tempToken == null) return false;
                if (!string.IsNullOrEmpty(localDir) && !string.IsNullOrEmpty(localFileName))
                {
                    string zipName = $"{Guid.NewGuid()}.zip";
                    string zipPath = downloadCosFile(cosKey, localDir, localFileName);
                    if (!string.IsNullOrEmpty(zipPath) && File.Exists(zipPath))
                    {
                        try
                        {
                            ZipFile.ExtractToDirectory(zipPath, localDir);
                        }
                        catch (Exception e)
                        {
                            FileUtils.log($"zip路径= {zipPath}，错误={e.Message}", "websocket采集文件解压错误");
                            return false;
                        }
                        string filePath = localDir + "\\" + localFileName;
                        if (File.Exists(filePath))
                        {
                            File.Delete(zipPath);
                            return true;
                        }
                        else
                        {
                            FileUtils.log($"zip解压后没有{filePath}文件，zip路径 = {zipPath}", "websocket采集文件格式错误");
                            return false;
                        }
                    }
                    else
                    {
                        FileUtils.log($"cosKey={cosKey}, localDir={localDir}, localFileName = {localFileName}, zipPath = {zipPath}", "下载cos的websocket采集文件失败");
                        return false;
                    }
                }
            }
            catch (Exception e)
            {
                FileUtils.log($"{e.Message}", "下载cos的websocket采集json文件报错");
            }
            return false;
        }

        /// <summary>
        /// COS文件下载
        /// </summary>
        /// <param name="cosKey"></param>
        /// <param name="localDir"></param>
        /// <param name="localFileName"></param>
        /// <returns></returns>
        public static string downloadCosFile(string cosKey, string localDir, string localFileName)
        {
            TencentCosToken tempToken = HttpGetCosToken();
            if (tempToken == null) return null;
            try
            {
                string region = tempToken.region; //设置一个默认的存储桶地域
                CosXmlConfig config = new CosXmlConfig.Builder()
                  .IsHttps(true)  //设置默认 HTTPS 请求
                  .SetRegion(region)  //设置一个默认的存储桶地域
                  .SetDebugLog(false)  //显示日志
                  .Build();  //创建 CosXmlConfig 对象
                QCloudCredentialProvider cosCredentialProvider = new DefaultSessionQCloudCredentialProvider(tempToken.tempSecretId, tempToken.tempSecretKey, ServerTimeUtils.getServerCurrentTime() / 1000 + 60, tempToken.token);
                var request = new GetObjectRequest(tempToken.bucketName, cosKey, localDir, localFileName);
                try
                {
                    CosXml cosXml = new CosXmlServer(config, cosCredentialProvider);
                    // 执行下载操作
                    GetObjectResult result = cosXml.GetObject(request);
                    // 拼接下载后的文件路径
                    string localFilePath = Path.Combine(localDir, localFileName);
                    if (result.IsSuccessful())
                    {
                        return localFilePath;
                    }
                    else
                    {
                        FileUtils.LogError(JsonConvert.SerializeObject(result), "cos文件下载失败");
                    }
                }
                catch (CosClientException clientEx)
                {
                    FileUtils.log("客户端错误: " + clientEx.Message);
                }
                catch (CosServerException serverEx)
                {
                    FileUtils.log("服务器错误: " + serverEx.Message);
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"cosKey={cosKey}, localDir={localDir}, localFileName={localFileName}");
                FileUtils.log("构建cos下载对象失败: " + ex.Message);
            }
            return null;
        }

        /// <summary>
        /// cos的文件上传
        /// </summary>
        /// <param name="filePath"></param>
        /// <param name="fileName"></param>
        /// <returns></returns>
        public static bool upload(string filePath, string fileName)
        {
            TencentCosToken tempToken = HttpGetCosToken();
            if (tempToken == null) return false;
            string region = tempToken.region; //设置一个默认的存储桶地域
            CosXmlConfig config = new CosXmlConfig.Builder()
              .IsHttps(true)  //设置默认 HTTPS 请求
              .SetRegion(region)  //设置一个默认的存储桶地域
              .SetDebugLog(false)  //显示日志
              .Build();  //创建 CosXmlConfig 对象
            long startTime = ServerTimeUtils.getServerCurrentTime() / 1000;
            long endTime = startTime + 60;
            QCloudCredentialProvider cosCredentialProvider = new DefaultSessionQCloudCredentialProvider(tempToken.tempSecretId, tempToken.tempSecretKey, startTime, endTime, tempToken.token);
            PutObjectResult result;
            try
            {
                // 存储桶名称，此处填入格式必须为 bucketname-APPID
                string bucket = tempToken.bucketName;
                string key = fileName; //对象键
                string srcPath = filePath;//本地文件绝对路径

                PutObjectRequest request = new PutObjectRequest(bucket, key, srcPath);
                //设置进度回调
                request.SetCosProgressCallback(delegate (long completed, long total) {
                    FileUtils.log(String.Format("progress = {0:##.##}%", completed * 100.0 / total));
                });

                CosXml cosXml = new CosXmlServer(config, cosCredentialProvider);

                result = cosXml.PutObject(request);
                if(result.httpCode == 200 && result.httpMessage == "OK")
                {
                    return true;
                }
                else
                {
                    FileUtils.LogError(JsonConvert.SerializeObject(result), "cos文件上传失败");
                }
            }
            catch (COSXML.CosException.CosClientException clientEx)
            {
                //string requestId = result.responseHeaders.GetValueOrDefault("x-cos-request-id")[0];
                //FileUtils.log("cos文件上传的requestId: " + requestId);
                FileUtils.log("CosClientException: " + clientEx);
            }
            catch (COSXML.CosException.CosServerException serverEx)
            {
                FileUtils.log("CosServerException: " + serverEx.GetInfo());
            }

            return false;
        }
    }
}
