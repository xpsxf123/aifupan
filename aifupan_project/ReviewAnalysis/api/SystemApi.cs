using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CefSharp.DevTools.CSS;
using CefSharp.DevTools.DOM;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo.proxyIP;
using ReviewAnalysis.vo.system;

namespace ReviewAnalysis.api
{
    public class SystemApi
    {

        /// <summary>
        /// 根据字典类型标识和字典值查询字典信息
        /// </summary>
        /// <param name="code"></param>
        /// <param name="value"></param>
        /// <returns></returns>
        public static DictDataListVo dictDataByValue(string code, string value)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("code", code);
            param.Add("value", value);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/system/dictData/dictDataByValue", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<DictDataListVo>(dataStr);
            }
            
            return null;
        }
        
        /// <summary>
        /// 根据字典类型标识和字典标签查询字典信息
        /// </summary>
        /// <param name="code"></param>
        /// <param name="label"></param>
        /// <returns></returns>
        public static DictDataListVo dictDataByLabel(string code, string label)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("code", code);
            param.Add("label", label);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/system/dictData/dictDataByLabel", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<DictDataListVo>(dataStr);
            }
            
            return null;
        }

        /// <summary>
        /// 根据字典类型标识查询字典信息
        /// </summary>
        /// <param name="code"></param>
        /// <returns></returns>
        public static List<DictDataListVo> dictDataListByCode(string code)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("code", code);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/system/dictData/dictDataListByCode", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<DictDataListVo>>(dataStr);
            }

            return null;
        }
        
        /// <summary>
        /// 获取网络检测地址列表
        /// </summary>
        /// <returns></returns>
        public static List<DictDataListVo> listNetworkCheckUrls()
        {
            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/system/dictData/listNetworkCheckUrls", null);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<List<DictDataListVo>>(dataStr);
            }

            return null;
        }
        

        /// <summary>
        /// 根据key获取系统配置的键值对信息
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public static async Task<SystemKvVo> getSystenKvByKeyAsync(string key)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("key", key);

            string dataStr = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/systemkv/getByKey", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SystemKvVo>(dataStr);
            }

            return null;
        }

        /// <summary>
        /// 根据key获取系统配置的键值对信息
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public static SystemKvVo getSystenKvByKey(string key)
        {
            Dictionary<string, object> param = new Dictionary<string, object>();
            param.Add("key", key);

            string dataStr = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/systemkv/getByKey", param);

            if (!string.IsNullOrEmpty(dataStr))
            {
                return JsonConvert.DeserializeObject<SystemKvVo>(dataStr);
            }

            return null;
        }
    }
}
