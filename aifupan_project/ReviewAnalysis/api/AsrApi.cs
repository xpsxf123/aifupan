using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class AsrApi
    {

        /// <summary>
        /// 查询当前用户可用的 ASR 引擎列表。
        /// 后端根据用户权限 + 业务方返回有序引擎标识（"sense-voice" / "tencent"），顺序即优先级。
        /// 任何失败返回 null，由调用方兜底默认引擎。
        ///
        /// 请求体：`{ "language": "16k_zh", "business": "anchor_replay" }`
        ///
        /// 响应格式约定：
        ///   后端真实返回 `{"code":0, "data":["sense-voice","tencent"], "msg":...}`
        ///   `HttpUtils.SendServerPostAsync` 已拆掉 envelope 层，返回的 `resp` 字符串
        ///   就是 `data` 字段本身（JSON 数组）。所以这里直接 `JArray.Parse(resp)`，
        ///   不要再次解 `{code, data, msg}` 包装（旧实现误以为是 JObject，永远 catch → null）。
        /// </summary>
        /// <param name="language">归一化后的腾讯语言码（如 16k_zh）。</param>
        /// <param name="consumer">业务消费方枚举，序列化为后端 business 字段字符串码。</param>
        public static async Task<List<string>> GetAvailableEnginesAsync(string language, AsrConsumerType consumer)
        {
            try
            {
                var param = new { language, business = consumer.ToWireCode() };
                string resp = await HttpUtils.SendServerPostAsync(
                    ReplayHttpUtils.BaseUrl + "/audio/asr-engine", param);

                if (string.IsNullOrEmpty(resp))
                    return null;

                JArray arr;
                try
                {
                    arr = JArray.Parse(resp);
                }
                catch (Exception ex)
                {
                    FileUtils.LogAnalysis($"[ASR] 引擎列表响应解析失败: {ex.Message}, resp={resp}");
                    return null;
                }

                if (arr.Count == 0)
                    return null;

                return arr.Select(t => t.Value<string>() ?? "")
                          .Where(s => s.Length > 0)
                          .ToList();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "GetAvailableEnginesAsync 发生异常");
                return null;
            }
        }

        /// <summary>获取语音识别接口临时调用凭证。</summary>
        public static AudioTempTokenEntity GetTempToken(string secretId)
        {
            try
            {
                var param = new Dictionary<string, object> { ["secretId"] = secretId };
                string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/audio/getTempToken", param);

                if (!string.IsNullOrEmpty(dataStr))
                {
                    var settings = new JsonSerializerSettings
                    {
                        ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver()
                    };
                    AudioTempTokenEntity audioTempTokenEntity = JsonConvert.DeserializeObject<AudioTempTokenEntity>(dataStr, settings);

                    return audioTempTokenEntity;
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"获取语音识别接口临时调用凭证发生异常");
            }


            return null;
        }
    }
}
