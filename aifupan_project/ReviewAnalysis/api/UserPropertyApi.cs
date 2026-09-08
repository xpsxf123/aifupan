using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.bo.shortVideo;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.api
{
    public class UserPropertyApi
    {
        /// <summary>
        /// 使用用户资产-有redisId，多退少补
        /// </summary>
        /// <param name="code"></param>
        /// <param name="num"></param>
        /// <param name="redisId"></param>
        /// <param name="aiTokenIds"></param>
        public static void usePropertyReal(string code, int num, string redisId, List<string> aiTokenIds, string remarks, string modelCode, int cachedTokens = 0)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("code", code);
            param.Add("num", num);
            param.Add("redisId", redisId);
            param.Add("aiTokenIds", aiTokenIds);
            param.Add("remarks", remarks);
            param.Add("modelCode", modelCode);
            param.Add("cachedTokens", cachedTokens);
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/openapi/userproperty/minusAssetsRealAiToken", param);

        }

        /// <summary>
        /// 使用用户资产
        /// </summary>
        /// <param name="code"></param>
        /// <param name="num"></param>
        public static void useProperty(string code, int num)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("code", code);
            param.Add("num", num);
            //param.Add("redisId", redisId);
            //param.Add("aiTokenIds", aiTokenIds);
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/openapi/userproperty/useProperty", param);

        }

        /// <summary>
        /// 更新巨量监控位的使用量
        /// </summary>
        /// <param name="num"></param>
        public static void updateRpaPropertyUseNum(int num)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("num", num);
            HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/order/userProperty/updateRpaAmountNum", param);

        }

        /// <summary>
        /// 获取用户资产信息
        /// </summary>
        public static async Task<UserPropertyEntity> GetPropertyInfo()
        {

            try
            {
                string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/openapi/userproperty/getUserProperty", null);
                if (!string.IsNullOrEmpty(dataStr))
                {
                    UserPropertyEntity userPropertyEntity = JsonConvert.DeserializeObject<UserPropertyEntity>(dataStr);
                    return userPropertyEntity;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"获取用户资产信息请求失败");
            }

            return null;

        }
        /// <summary>
        /// 获取用户资产信息-同步
        /// </summary>
        public static UserPropertyEntity GetPropertyInfoSync()
        {

            try
            {
                string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/openapi/userproperty/getUserProperty", null);
                if (!string.IsNullOrEmpty(dataStr))
                {
                    UserPropertyEntity userPropertyEntity = JsonConvert.DeserializeObject<UserPropertyEntity>(dataStr);
                    return userPropertyEntity;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"获取用户资产信息请求失败");
            }

            return null;

        }

        /// <summary>
        /// 客户端-同步爆款订阅数据
        /// </summary>
        /// <param name="bo"></param>
        /// <returns></returns>
        public static bool checkUseProperty(string code, int quantity)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("code", code);
            param.Add("quantity", quantity);
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/order/userProperty/checkUseProperty", param);

            bool result = false;
            if (!string.IsNullOrEmpty(dataStr))
            {
                bool.TryParse(dataStr, out result);
            }

            return result;
        }


    }
}
