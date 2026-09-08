using douyin.Utils;
using Newtonsoft.Json;
using Qiniu.Http;
using Qiniu.Storage;
using Swan.Parsers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Asr
{
    public class QiNiuHttpUtils
    {

        /// <summary>
        /// 上传视频文件到七牛
        /// </summary>
        /// <param name="filePath">文件路径</param>
        /// <param name="fileName">文件名</param>
        public static void UploadToOss(string filePath, string fileName)
        {
            // 获取服务器的临时上传凭证
            string token = GetUploadToken();

            Config config = new Config();
            // 设置上传区域
            config.Zone = Zone.ZONE_CN_South;
            // 设置 http 或者 https 上传
            config.UseHttps = true;
            config.UseCdnDomains = true;
            config.ChunkSize = ChunkUnit.U512K;
            // 表单上传
            FormUploader target = new FormUploader(config);
            HttpResult result = target.UploadFile(filePath, fileName, token, null);
            if(result.Code == 200)
            {
                FileUtils.log("上传成功");
            }
            FileUtils.log("form upload result: " + result.ToString());
        }

        /// <summary>
        /// 获取七牛上传文件的临时凭证
        /// </summary>
        /// <returns></returns>
        public static string GetUploadToken()
        {
            var request = new HttpRequestMessage(HttpMethod.Get, $"{ReplayHttpUtils.BaseUrl}/qiniu/getUploadToken")
            {
                Headers =
                {
                    { "token", ReplayHttpUtils.Token }
                }
            };
            try
            {
                HttpClient Client = new HttpClient();
                Client.Timeout = TimeSpan.FromSeconds(30);
                var response = Client.SendAsync(request).Result;

                if (!response.IsSuccessStatusCode)
                {
                    FileUtils.LogAnalysis($"获取七牛上传文件的临时凭证请求失败： {response}.");
                    return null;
                }

                var responseBody = response.Content.ReadAsStringAsync().Result;
                var jsonObject = JsonConvert.DeserializeObject<dynamic>(responseBody);

                if(jsonObject.data != null)
                {
                    return jsonObject.data.ToString();
                }

                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"获取七牛上传文件的临时凭证请求失败： {ex}.");
                return null;
            }
        }
    }
}
