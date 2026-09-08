using COSXML.Network;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.contrast;
using Swan.Parsers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.api
{
    public class ConstrastApi
    {

        /// <summary>
        /// 根据对比id从服务器获取对比信息
        /// </summary>
        /// <param name="contrastId">对比唯一标识id</param>
        /// <returns></returns>
        public static async Task<ContrastVo> GetByContrastIdAsync(string contrastId)
        {
            try
            {
                Dictionary<string, object> param = new Dictionary<string, object>();
                param.Add("contrastId", contrastId);

                // 使用异步HTTP请求
                string responseBody = await HttpUtils.SendServerGetAsync(ReplayHttpUtils.BaseUrl + "/synccontrast/clientGetContrast", param).ConfigureAwait(false);
                if (!string.IsNullOrEmpty(responseBody))
                {
                    ContrastVo contrast = JsonConvert.DeserializeObject<ContrastVo>(responseBody);

                    return contrast;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"从服务器获取对比信息发生异常");
            }

            return null;
        }

        /// <summary>
        /// 根据对比id从服务器获取对比信息（同步版本，保留用于向后兼容）
        /// </summary>
        /// <param name="contrastId">对比唯一标识id</param>
        /// <returns></returns>
        public static ContrastVo GetByContrastId(string contrastId)
        {
            try
            {
                Dictionary<string, object> param = new Dictionary<string, object>();
                param.Add("contrastId", contrastId);

                string responseBody = HttpUtils.SendServerGet(ReplayHttpUtils.BaseUrl + "/synccontrast/clientGetContrast", param);
                if (!string.IsNullOrEmpty(responseBody))
                {
                    ContrastVo contrast = JsonConvert.DeserializeObject<ContrastVo>(responseBody);

                    return contrast;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"从服务器获取对比信息发生异常");
            }

            return null;
        }
    }
}
