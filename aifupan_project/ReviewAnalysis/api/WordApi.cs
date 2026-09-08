using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.analysis;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.socketCollectMessage;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class WordApi
    {

        /// <summary>
        /// 关键词/敏感词识别
        /// </summary>
        /// <param name="token">向服务器请求token</param>
        /// <param name="platform">平台类型 0：全平台 1：抖音 2：快手 3：视频号 4：小红书</param>
        /// <param name="videoId">视频id</param>
        /// <param name="isLastParagraph">是否是最后一段 0：否 1：是</param>
        /// <param name="asrResultEntity">腾讯识别返回的数据对象</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static async Task<AnalysisResultVo> WordsMark(int platform, string videoId, List<ASRResultEntity> asrResultList, string tradeId, int type)
        {
            List<JObject> bodyJsonList = new List<JObject>();
            foreach (var asrResultEntity in asrResultList)
            {
                // 构造请求体参数
                JObject bodyJsonObject = new JObject();
                bodyJsonObject["platformType"] = platform;

                bodyJsonObject["videoId"] = videoId;
                bodyJsonObject["tradeId"] = tradeId;
                if (string.IsNullOrEmpty(asrResultEntity.Result))
                {
                    asrResultEntity.Result = "-";
                }
                bodyJsonObject["content"] = asrResultEntity.Result;
                bodyJsonObject["currentSort"] = asrResultEntity.Paragraph;
                bodyJsonObject["isLast"] = asrResultEntity.Paragraph == asrResultList.Count ? 1 : 0;
                bodyJsonObject["type"] = type;
                JArray wordListArray = new JArray();
                if (asrResultEntity.WordList != null && asrResultEntity.WordList.Count > 0)
                {
                    foreach (var item in asrResultEntity.WordList)
                    {
                        wordListArray.Add(JToken.FromObject(item));
                    }
                }
                else
                {
                    ASRWordEntity wordEntity = new ASRWordEntity();
                    wordEntity.Word = "-";
                    wordEntity.StartTime = 0;
                    wordEntity.EndTime = 20;
                    wordListArray.Add(JToken.FromObject(wordEntity));
                    bodyJsonObject["content"] = asrResultEntity.Result;
                }

                bodyJsonObject["items"] = wordListArray;

                bodyJsonList.Add(bodyJsonObject);
            }

            string dataStr = await HttpAsyncUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/openapi/v2000/wordsMark", bodyJsonList, true, cancellationToken: CancellationToken.None);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                try
                {
                    return JsonConvert.DeserializeObject<AnalysisResultVo>(dataStr, settings);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"关键词/敏感词识别结果转换发生异常");
                }
            }
            return null;
        }

        /// <summary>
        /// 视频切片关键词/敏感词识别
        /// </summary>
        /// <param name="wordsMarkParagraphBos">段落内容列表</param>
        /// <returns></returns>
        public static async Task<AnalysisResultVo> SliceWordsMark(List<WordsMarkParagraphBo> wordsMarkParagraphBos)
        {
            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/openapi/v2000/wordsMark", wordsMarkParagraphBos);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                try
                {
                    return JsonConvert.DeserializeObject<AnalysisResultVo>(dataStr, settings);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"关键词/敏感词识别结果转换发生异常");
                }
            }
            return null;
        }

        /// <summary>
        /// 文本内容关键词/敏感词识别
        /// </summary>
        /// <param name="token">向服务器请求token</param>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">文件id</param>
        /// <param name="content">文本内容</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static async Task<AnalysisResultVo> WordsMarkByText(string token, int platform, string fileId, string content, string tradeId, int type)
        {
            // 构造请求体参数
            JObject bodyJsonObject = new JObject();
            bodyJsonObject["platformType"] = platform;
            bodyJsonObject["videoId"] = fileId;
            bodyJsonObject["tradeId"] = tradeId;
            bodyJsonObject["content"] = content;
            bodyJsonObject["currentSort"] = 1;
            bodyJsonObject["isLast"] = 1;
            bodyJsonObject["type"] = type;

            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/openapi/v2000/wordsMarkByText", bodyJsonObject);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                try
                {
                    return JsonConvert.DeserializeObject<AnalysisResultVo>(dataStr, settings);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"重新选行业分析结果转换发生异常");
                }
            }

            return null;

        }

        /// <summary>
        /// 重新选行业分析-同步
        /// </summary>
        /// <param name="platform">平台标识</param>
        /// <param name="videoId">视频id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static AnalysisResultVo WordsMarkSync(string platform, string videoId, string tradeId, int type)
        {
            // 构造请求体参数
            JObject bodyJson = new JObject();
            bodyJson["videoId"] = videoId;
            bodyJson["tradeId"] = tradeId;
            bodyJson["type"] = type;
            if (platform.Contains("DouYin"))
            {
                // 抖音
                bodyJson["platformType"] = 1;
            }
            else if (platform.Contains("Kuai"))
            {
                // 快手
                bodyJson["platformType"] = 2;
            }
            else if (platform.Contains("ShiP"))
            {
                // 视频号
                bodyJson["platformType"] = 3;
            }
            else
            {
                // 全平台
                bodyJson["platformType"] = platform;
            }


            string dataStr = HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/openapi/v2000/wordsMarkReAnalysis", bodyJson);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                try
                {
                    return JsonConvert.DeserializeObject<AnalysisResultVo>(dataStr, settings);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"重新选行业分析结果转换发生异常");
                }
            }

            return null;

        }

        /// <summary>
        /// 重新选行业分析-异步
        /// </summary>
        /// <param name="platform">平台类型</param>
        /// <param name="videoId">视频id</param>
        /// <param name="tradeId">行业id</param>
        /// <param name="type">类型 0：视频 1：文件</param>
        /// <returns></returns>
        public static async Task<AnalysisResultVo> WordsMarkAsync(string platform, string videoId, string tradeId, int type)
        {
            // 构造请求体参数
            JObject bodyJson = new JObject();
            bodyJson["videoId"] = videoId;
            bodyJson["tradeId"] = tradeId;
            bodyJson["type"] = type;
            if (platform.Contains("DouYin"))
            {
                bodyJson["platformType"] = 1;
            }
            else if (platform.Contains("Kuai"))
            {
                bodyJson["platformType"] = 2;
            }
            else if (platform.Contains("ShiP"))
            {
                bodyJson["platformType"] = 3;
            }
            else
            {
                bodyJson["platformType"] = platform;
            }

            string dataStr = await HttpUtils.SendServerPostAsync(ReplayHttpUtils.BaseUrl + "/openapi/v2000/wordsMarkReAnalysis", bodyJson);

            if (!string.IsNullOrEmpty(dataStr))
            {
                var settings = new JsonSerializerSettings
                {
                    ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                };
                try
                {
                    return JsonConvert.DeserializeObject<AnalysisResultVo>(dataStr, settings);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"{ex}", $"重新选行业分析结果转换发生异常");
                }
            }

            return null;
        }
        /// <summary>
        /// 获取socketMessage的数据
        /// </summary>
        /// <param name="userId"></param>
        /// <param name="videoId"></param>
        /// <param name="batchNumber"></param>
        /// <returns></returns>
        public static SocketCollectMessageVo socketMessageInfoNotJson(long? userId, string videoId, string batchNumber)
        {
            Dictionary<string, Object> param = new Dictionary<string, Object>();
            if(userId != null) param["userId"] = userId;
            param["videoId"] = videoId;
            if (batchNumber != null) param["batchNumber"] = batchNumber;
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v1930/socketMessageInfoNotJson", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SocketCollectMessageVo>(dataStr);
            }

            return null;
        }
    }
}
