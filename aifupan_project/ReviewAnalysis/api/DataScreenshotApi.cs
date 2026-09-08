using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.contrast;
using ReviewAnalysis.vo.dataScreenshot;

namespace ReviewAnalysis.api
{
    public class DataScreenshotApi
    {

        /// <summary>
        /// 查询视频中有效的数据截图
        /// </summary>
        /// <param name="sourceType"></param>
        /// <param name="sourceId"></param>
        /// <returns></returns>
        public static List<DataScreenshotVo> getExistDataScreenshotList(int sourceType, string sourceId) 
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("sourceType", sourceType);
            param.Add("sourceId", sourceId);

            string responseBody = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/v2300/getExistDataScreenshotList", param);

            if (responseBody != null)
            {
                return JsonConvert.DeserializeObject<List<DataScreenshotVo>>(responseBody);
            }

            return new List<DataScreenshotVo> { };
        }

    }
}
