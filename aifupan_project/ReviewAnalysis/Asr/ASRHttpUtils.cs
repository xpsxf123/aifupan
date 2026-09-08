using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Security.Cryptography;
using System.Security.Policy;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using TencentCloud.Asr.V20190614.Models;
using TencentCloud.Asr.V20190614;
using TencentCloud.Common.Profile;
using TencentCloud.Common;
using CefSharp.Web;
using static System.Net.WebRequestMethods;
using Swan.Parsers;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Model;
using Qiniu.Util;

namespace ReviewAnalysis.Asr
{
    public class ASRHttpUtils
    {

        private static readonly HttpClientHandler clientHandler = new HttpClientHandler();
        private static readonly HttpClient Client = new HttpClient(clientHandler, disposeHandler: false);

        /// <summary>
        /// 录音文件识别
        /// </summary>
        /// <returns></returns>
        public static string RecTaskRequest(int i)
        {

            string service = "asr";
            string version = "2019-06-14";
            string action = "CreateRecTask";
            string region = "";
            JObject bodyJsonObject = new JObject();
            bodyJsonObject["EngineModelType"] = "16k_zh";
            bodyJsonObject["ChannelNum"] = 1;
            bodyJsonObject["ResTextFormat"] = 2;
            bodyJsonObject["SourceType"] = 0;
            bodyJsonObject["Url"] = $"https://aifupan-test-audio-1330324554.cos.ap-shanghai.myqcloud.com/4hour.mp3";
            bodyJsonObject["SpeakerDiarization"] = 0;

            int retryCount = 100; // 因超出QPS限制导致的异常的重试次数
            int errorCount = 3; // 因其他原因导致的异常的重试次数
            while (retryCount > 0)
            {

                HttpResponseMessage response = null;

                try
                {

                    var request = BuildRequest("AKIDNH0c0whhg-bt_ioexIJeohgOkqCMxDmSgGxHX5sJQlh9yyxwXuJGYfK7mThV0hAX",
                        "daPuTV95nWLCBwK6GwdMN7IRep/5RQ/6eAvrzdBzsDQ=", service, version, action, bodyJsonObject.ToString(), region,
                        "e7h9jjuoA8pZRGdqL1e7O9UNukmtO63aba52a66b2a5a78b5f6dc2ed66f24bbf9oUOSdD5FKNpUx7cQ-unGeyUERsGiqCLZwgbbMbpbTi2wUjvWvUaOTs-n3vCn0NiQv3sLVVSSB1i-zDNCO6P9EINLOsDZGjB50hsMK1ed7XpquoeaK6DigUxlnmR4SNKY7dPua93luLQrg7JJlxs0H4CeL4CX3V5qZ-pdQkl0IzFBt7Kh9CeiU0wX-37T-EXEu7DhzVUQarXNjoED7esN8KbIWcj9zVLTwW4qJLCwgJpbMhjrQND-Kyo52RgluInSkrI3tj-lwKBr1Z8zA0Mfa8cKaKMOc2Vss765_7go-fX1L0AAaSWw09o2IljZ0rpM");

                    response = Client.SendAsync(request).Result;
                    if (response.IsSuccessStatusCode)
                    {
                        var responseBody = response.Content.ReadAsStringAsync().Result;
                        FileUtils.log($"创建第{i}个长音频识别的返回值：{responseBody}");

                        JObject jsonObject = JObject.Parse(responseBody);
                        JObject dataJsonObject = (JObject)jsonObject["Response"]["Data"];
                        if (dataJsonObject != null)
                        {
                            // 获取 TaskId 的值
                            ulong taskId = (ulong)dataJsonObject["TaskId"];
                            if (taskId != null && taskId > 0)
                            {
                                long startTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                                // 轮询查结果
                                while (true)
                                {
                                    Thread.Sleep(3000);

                                    string result = QueryRecTaskResult(taskId);
                                    FileUtils.log($"轮询第{i}个结果：{result}");
                                    if (!"failed".Equals(result) && !"waiting".Equals(result) && !"doing".Equals(result))
                                    {
                                        long endTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                                        FileUtils.log($"第{i}个识别时间：{(endTime - startTime) / 1000}秒");
                                        return result;
                                    }
                                }

                            }

                        }
                        else
                        {
                            // 创建识别任务发生错误
                            FileUtils.LogAnalysis($"{responseBody}", "创建录音文件识别任务腾讯返回异常");

                            JObject errorJsonObject = (JObject)jsonObject["Response"]["Error"];
                            string errorCode = (string)errorJsonObject["Code"];
                            if (!"RequestLimitExceeded".Equals(errorCode))
                            {
                                // 非因为超出qps限制导致的错误， 超过次数直接退出
                                if (errorCount <= 0)
                                {
                                    return "";
                                }
                                errorCount--;
                            }
                            else
                            {
                                FileUtils.log($"正在重试第{i}个长音频识别");
                            }
                        }
                    }
                    else
                    {
                        FileUtils.LogAnalysis($"参数--{bodyJsonObject.ToString()}---错误信息--{response.StatusCode}", "创建录音文件识别任务请求错误");
                    }


                }
                catch (Exception e)
                {
                    FileUtils.LogAnalysis($"{e.ToString()}", "创建录音文件识别任务发生错误");
                }
                finally
                {
                    response?.Dispose();
                }

                retryCount--;
                Thread.Sleep(10000);
            }

            return "";
        }

        /// <summary>
        /// 查询录音文件识别结果 返回 failed 表示失败，返回 waiting 表示等待， doing 表示进行中，否则返回结果
        /// </summary>
        /// <param name="taskId">任务id</param>
        /// <returns></returns>
        public static string QueryRecTaskResult(ulong taskId)
        {

            HttpResponseMessage response = null;
            try
            {

                string service = "asr";
                string version = "2019-06-14";
                string action = "DescribeTaskStatus";
                string region = "";
                JObject bodyJsonObject = new JObject();
                bodyJsonObject["TaskId"] = taskId;

                var request = BuildRequest("AKIDO81CEFtY2DjLT94AAyDalFjXtV2UO-0XHBptwF_sD2TPRPm7-nHcDkpduWhgNsCS",
                    "ydaBe/HdDSIQMOnmdXNxVlqgR4aF90ATosMbIPoTPAo=", service, version, action, bodyJsonObject.ToString(), region,
                    "yAvVFSqxrAxDSlB5QtsZHJOGCOKjMcAa59b6662c1cc0037a2edaa55182ceb9c8WUwAfrX8YCJDbHkEVRZjr3xyfCZICmukAZ8gekriF0e9pE53VeQOAmK_nlKJ5gmYaoiAw-q9bnPHwxxz6hd40pYiPjKFwXXbFLW0eKWXy6xLcrCLuKPFohyOcvxLE7IXYnJK-gW9-CCkXNMdO6JWWC8qZkYIejM6s8ogbuvz6DN7svXmmxUG390h9EoXetSOc_BMHOURCfmCSweKY3tdFqdLr3XTtQlmvi7neg6u6WpI8mVsy3R4o_6aUoexmvuAQmWIGiSqdnRyt7KgN44Ht4h8pytj-nnxeLvSjL-YSbUIJ7G0f1trxmt9m96l7LO5x9iDT0AmTnIWbpBgrUvhOw");

                response = Client.SendAsync(request).Result;
                if (response.IsSuccessStatusCode)
                {
                    var responseBody = response.Content.ReadAsStringAsync().Result;
                    JObject jsonObject = JObject.Parse(responseBody);
                    JObject dataJsonObject = (JObject)jsonObject["Response"]["Data"];
                    if (dataJsonObject != null)
                    {
                        string statusStr = (string)dataJsonObject["StatusStr"];
                        if ("success".Equals(statusStr))
                        {
                            // 返回识别结果json字符串
                            //return JsonConvert.SerializeObject(dataJsonObject["ResultDetail"], Formatting.Indented);
                            return "识别完成";
                        }
                        else
                        {
                            return statusStr;
                        }
                    }
                    else
                    {
                        return "failed";
                    }
                }

            }
            catch (Exception e)
            {
                FileUtils.log(e.ToString());
            }
            finally
            {
                response?.Dispose();
            }

            return "";
        }


        /// <summary>
        /// 一句话识别
        /// </summary>
        /// <param name="secretId"></param>
        /// <param name="secretKey"></param>
        /// <param name="token"></param>
        /// <param name="bodyJsonObject"></param>
        /// <param name="engSerViceType">识别引擎模型类型，默认：16k_zh</param>
        /// <returns></returns>
        public static ASRResultEntity DoRequest(
            string secretId, string secretKey,
            string token, JObject bodyJsonObject, string engSerViceType = "16k_zh"
        )
        {

            string service = "asr";
            string version = "2019-06-14";
            string action = "SentenceRecognition";
            string region = "";

            bodyJsonObject["EngSerViceType"] = engSerViceType;
            bodyJsonObject["SourceType"] = 1;
            bodyJsonObject["VoiceFormat"] = "wav";
            bodyJsonObject["WordInfo"] = 2;

            HttpResponseMessage response = null;
            string responseBody = "";
            try
            {
                var request = BuildRequest(secretId, secretKey, service, version, action, bodyJsonObject.ToString(), region, token);

                response = Client.SendAsync(request).Result;
                if (response.IsSuccessStatusCode)
                {
                    responseBody = response.Content.ReadAsStringAsync().Result;
                    FileUtils.log($"腾讯语音识别接口结果：{responseBody}");

                    if (!string.IsNullOrEmpty(responseBody))
                    {
                        dynamic resultJsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);
                        ASRResultEntity asrResultEntity = JsonConvert.DeserializeObject<ASRResultEntity>(resultJsonObject.Response.ToString());
                        if(asrResultEntity != null && asrResultEntity.Error != null)
                        {
                            FileUtils.LogAnalysis($"{responseBody}", "一句话识别报错==识别结果");
                        }

                        return asrResultEntity;
                    }
                }

            }
            catch (Exception ex) {
                FileUtils.LogAnalysis($"{ex}", "一句话识别报错");
                FileUtils.LogAnalysis($"{responseBody}", "一句话识别报错==识别结果");
            }
            finally
            {
                response?.Dispose();
            }

            return null;
        }

        private static HttpRequestMessage BuildRequest(
            string secretId, string secretKey,
            string service, string version, string action,
            string body, string region, string token
        )
        {
            var host = "asr.tencentcloudapi.com";
            var url = "https://" + host;
            var contentType = "application/json; charset=utf-8";
            if(!ServerTimeUtils.timeAccurate)
            {
                ServerTimeUtils.checkTimeAccurate();
            }
            var timestamp = (ServerTimeUtils.getCurrentTime() / 1000);
            var auth = GetAuth(secretId, secretKey, host, contentType, timestamp.ToString(), body);
            var request = new HttpRequestMessage();
            request.Method = HttpMethod.Post;
            request.Headers.Add("Host", host);
            request.Headers.Add("X-TC-Timestamp", timestamp.ToString());
            request.Headers.Add("X-TC-Version", version);
            request.Headers.Add("X-TC-Action", action);
            request.Headers.Add("X-TC-Region", region);
            request.Headers.Add("X-TC-Token", token);
            request.Headers.Add("X-TC-RequestClient", "SDK_NET_BAREBONE");
            request.Headers.TryAddWithoutValidation("Authorization", auth);
            request.RequestUri = new Uri(url);
            request.Content = new StringContent(body, Encoding.UTF8, "application/json");
            
            return request;
        }

        private static string GetAuth(
            string secretId, string secretKey, string host, string contentType,
            string timestamp, string body
        )
        {
            var canonicalURI = "/";
            var canonicalHeaders = "content-type:" + contentType + "\nhost:" + host + "\n";
            var signedHeaders = "content-type;host";
            var hashedRequestPayload = Sha256Hex(body);
            var canonicalRequest = "POST" + "\n"
                                          + canonicalURI + "\n"
                                          + "\n"
                                          + canonicalHeaders + "\n"
                                          + signedHeaders + "\n"
                                          + hashedRequestPayload;

            var algorithm = "TC3-HMAC-SHA256";
            var date = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc).AddSeconds(int.Parse(timestamp))
                .ToString("yyyy-MM-dd");
            var service = host.Split('.')[0];
            var credentialScope = date + "/" + service + "/" + "tc3_request";
            var hashedCanonicalRequest = Sha256Hex(canonicalRequest);
            var stringToSign = algorithm + "\n"
                                         + timestamp + "\n"
                                         + credentialScope + "\n"
                                         + hashedCanonicalRequest;

            var tc3SecretKey = Encoding.UTF8.GetBytes("TC3" + secretKey);
            var secretDate = HmacSha256(tc3SecretKey, Encoding.UTF8.GetBytes(date));
            var secretService = HmacSha256(secretDate, Encoding.UTF8.GetBytes(service));
            var secretSigning = HmacSha256(secretService, Encoding.UTF8.GetBytes("tc3_request"));
            var signatureBytes = HmacSha256(secretSigning, Encoding.UTF8.GetBytes(stringToSign));
            var signature = BitConverter.ToString(signatureBytes).Replace("-", "").ToLower();

            return algorithm + " "
                             + "Credential=" + secretId + "/" + credentialScope + ", "
                             + "SignedHeaders=" + signedHeaders + ", "
                             + "Signature=" + signature;
        }

        private static string Sha256Hex(string s)
        {
            using (SHA256 algo = SHA256.Create())
            {
                byte[] hashbytes = algo.ComputeHash(Encoding.UTF8.GetBytes(s));
                StringBuilder builder = new StringBuilder();
                foreach (byte b in hashbytes)
                {
                    builder.Append(b.ToString("x2"));
                }

                return builder.ToString();
            }
        }

        private static byte[] HmacSha256(byte[] key, byte[] msg)
        {
            using (HMACSHA256 mac = new HMACSHA256(key))
            {
                return mac.ComputeHash(msg);
            }
        }

        private static string PrintHexBinary(byte[] data)
        {
            StringBuilder r = new StringBuilder(data.Length * 2);
            foreach (byte b in data)
            {
                r.Append(Convert.ToString(b, 16).PadLeft(2, '0'));
            }
            return r.ToString();
        }
    }
}