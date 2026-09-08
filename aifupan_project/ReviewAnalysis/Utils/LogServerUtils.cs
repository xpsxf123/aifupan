using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Global;
using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;

namespace ReviewAnalysis.Utils
{
    public class LogServerUtils
    {

        /// <summary>
        /// 新的错误日志接口
        /// </summary>
        /// <param name="actionName"></param>
        /// <param name="actionInfo"></param>
        /// <param name="errorMsg"></param>
        public static void ErrorLog(string actionName, string actionInfo, string errorMsg)
        {
            try
            {
                ClientLogApi.saveLog(actionName, actionInfo, 1, errorMsg);
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "上传日志失败");
            }
        }

        public static void WriteLog(string actionName, string actionInfo,int logType = 0,string errorMessage="") 
        {
            try
            {
                //var request = new HttpRequestMessage();
                //request.Method = HttpMethod.Post;
                //request.Headers.Add("token", ReplayHttpUtils.Token);
                //request.RequestUri = new Uri(Constant.GetApiBaseUrl() + "/openapi/clientlog/save");
                //Dictionary<string, object> parameters = new Dictionary<string, object>();
                //parameters.Add("actionName", actionName);
                //parameters.Add("actionInfo", actionInfo);
                //parameters.Add("errorMsg", errorMessage);
                //parameters.Add("clientVersion", Constant.VERSION);
                //parameters.Add("logType", logType);
                //request.Content = new StringContent(JsonConvert.SerializeObject(parameters), Encoding.UTF8, "application/json");
                //HttpClient Client = new HttpClient();
                //HttpResponseMessage httpRequestMessage = Client.SendAsync(request).Result;
                //if (httpRequestMessage.EnsureSuccessStatusCode().IsSuccessStatusCode)
                //{
                //    string result = httpRequestMessage.Content.ToString().Trim();
                //    Dictionary<string, object> httpResult = JsonConvert.DeserializeObject<Dictionary<string, object>>(result);
                //    if (httpResult.TryGetValue("code", out object code))
                //    {
                //        if (Convert.ToInt32(code) != 0)
                //        {
                //            httpResult.TryGetValue("msg", out object msg);
                //            FileUtils.log($"写入失败，返回信息:{msg.ToString()}", "写入线上日志");
                //        }
                //    }
                //}
                //else
                //{
                //    FileUtils.log($"写入失败，状态码:{httpRequestMessage.EnsureSuccessStatusCode().StatusCode},信息：{httpRequestMessage.EnsureSuccessStatusCode().Content}", "写入线上日志");
                //}
            }
            catch (Exception ex) 
            {
                FileUtils.log($"写入失败，信息：{ex.Message.ToString()}", "写入线上日志");
            }
        }

        /// <summary>
        /// 记录业务情况
        /// </summary>
        /// <param name="actionName">动作名称</param>
        /// <param name="actionInfo">动作描述</param>
        public static void LogInfo(string actionName, string actionInfo) 
        {
            WriteLog(actionName, actionInfo);
        }

        /// <summary>
        /// 错误日志
        /// </summary>
        /// <param name="actionName">动作名称</param>
        /// <param name="actionInfo">动作描述</param>
        /// <param name="errorMsg">错误信息</param>
        public static void LogError(string actionName, string actionInfo,string errorMsg) 
        {
            WriteLog(actionName, actionInfo,1, errorMsg);
        }
        
    }
}
