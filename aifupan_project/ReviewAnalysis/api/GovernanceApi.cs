using System;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using douyin.Utils;

namespace ReviewAnalysis.api
{
    /// <summary>
    /// 通用API响应
    /// </summary>
    public class GovernanceApiResponse<T>
    {
        public int code { get; set; }
        public string msg { get; set; }
        public T data { get; set; }
        public string exception { get; set; }
        public long timestamp { get; set; }
    }

    /// <summary>
    /// 企业后台相关API
    /// </summary>
    public class GovernanceApi
    {
        /// <summary>
        /// 检查当前登录账号是否有企业后台授权
        /// GET /api/governance/check/tenant
        /// </summary>
        /// <returns>true=已授权，false=未授权</returns>
        public static async Task<bool> CheckTenantAuthAsync()
        {
            try
            {
                string dataStr = await HttpUtils.SendServerGetAsync(
                    ReplayHttpUtils.BaseUrl + "/api/governance/check/tenant", null);

                if (!string.IsNullOrEmpty(dataStr))
                {
                    var response = JsonConvert.DeserializeObject<GovernanceApiResponse<bool>>(dataStr);
                    return response?.data ?? false;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"检查企业后台授权异常: {ex.Message}", "GovernanceApi");
            }

            return false;
        }

        /// <summary>
        /// 检查当前登录账号是否有企业后台授权（同步版本）
        /// GET /api/governance/check/tenant
        /// </summary>
        /// <returns>true=已授权，false=未授权</returns>
        public static bool CheckTenantAuth()
        {
            try
            {
                string dataStr = HttpUtils.SendServerGet(
                    ReplayHttpUtils.BaseUrl + "/api/governance/check/tenant", null);

                if (!string.IsNullOrEmpty(dataStr))
                {
                    var response = JsonConvert.DeserializeObject<GovernanceApiResponse<bool>>(dataStr);
                    return response?.data ?? false;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"检查企业后台授权异常: {ex.Message}", "GovernanceApi");
            }

            return false;
        }
    }
}
