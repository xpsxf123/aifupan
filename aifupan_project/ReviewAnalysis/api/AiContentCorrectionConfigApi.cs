using System.Collections.Generic;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using static ReviewAnalysis.Utils.HttpUtils;

namespace ReviewAnalysis.api
{
    public class AiContentCorrectionConfigApi
    {
        /// <summary>
        /// 获取AI内容校正配置
        /// </summary>
        /// <param name="sceneType">场景类型</param>
        /// <returns>modelCode和contentPrompt，未找到时返回(null, null)</returns>
        public static (string modelCode, string contentPrompt) GetContentCorrectionConfig(int sceneType)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sceneType", sceneType);
            R result = HttpUtils.SendServerGetR(ReplayHttpUtils.BaseUrl + "/ai/getContentCorrectionConfig", param);
            if (result?.code == 0 && !string.IsNullOrEmpty(result.data))
            {
                var config = JsonConvert.DeserializeObject<dynamic>(result.data);
                string modelCode = config?.modelCode?.ToString() ?? "";
                string contentPrompt = config?.contentPrompt?.ToString() ?? "";
                return (modelCode, contentPrompt);
            }
            return (null, null);
        }
    }
}
