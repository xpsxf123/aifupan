using CefSharp;
using CefSharp.WinForms;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Management.Instrumentation;
using System.Reflection;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    public class FrontNotice
    {

        public static ChromiumWebBrowser webBrowser;
        public static FormMain formMain;

        public static void InitChromiumWebBrowser()
        {
            try
            {
                // 确保 WcfEnabled 已设置（即使使用异步绑定，某些版本也可能需要此设置）
                if (!CefSharpSettings.WcfEnabled)
                {
                    CefSharpSettings.WcfEnabled = true;
                    FileUtils.log("强制启用 WcfEnabled 以支持 JS 对象绑定");
                }

                // 注册 JS 对象 - 使用异步绑定
                webBrowser.JavascriptObjectRepository.Register("frontNotice", new FrontNotice(), isAsync: true, options: BindingOptions.DefaultBinder);
                FileUtils.log("JS 对象注册成功: frontNotice");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "注册 JS 对象失败");
                throw; // 重新抛出异常以便调试
            }

        }

        /// <summary>
        /// 调用前端函数（使用 BeginInvoke 优化，避免窗体卡顿）
        /// </summary>
        /// <param name="requestDataJson">请求的参数体</param>
        /// <param name="waitForResult">是否需要等待前端返回结果，默认不需要</param>
        /// <returns></returns>
        public async Task<string> NoticeJs(string requestDataJson, bool waitForResult = false)
        {
            // 最外层 try-catch：捕获所有异常，避免传播到调用方导致窗体卡顿
            try
            {
                // 检查 webBrowser 状态
                if (webBrowser == null || !webBrowser.IsBrowserInitialized)
                {
                    FileUtils.LogError("", "[FrontNotice] webBrowser 未初始化");
                    return JsonConvert.SerializeObject(new { code = 500, msg = "webBrowser 未初始化" });
                }

                // 根据是否需要等待结果设置超时时间
                int timeoutMs = waitForResult ? 3 * 60 * 60 * 1000 : 10 * 1000;

                // 使用 TaskCompletionSource 接收跨线程结果
                var tcs = new TaskCompletionSource<string>();

                // 使用 BeginInvoke 切换到 UI 线程执行
                if (formMain != null && !formMain.IsDisposed && formMain.IsHandleCreated)
                {
                    formMain.BeginInvoke((MethodInvoker)(async () =>
                    {
                        try
                        {
                            string result = await ExecuteScriptAsync(requestDataJson, timeoutMs).ConfigureAwait(false);
                            tcs.TrySetResult(result);
                        }
                        catch (Exception ex)
                        {
                            tcs.TrySetResult(JsonConvert.SerializeObject(new { code = 500, msg = ex.Message }));
                        }
                    }));
                }
                else
                {
                    // 窗体不可用，直接执行
                    try
                    {
                        string result = await ExecuteScriptAsync(requestDataJson, timeoutMs).ConfigureAwait(false);
                        tcs.TrySetResult(result);
                    }
                    catch (Exception ex)
                    {
                        tcs.TrySetResult(JsonConvert.SerializeObject(new { code = 500, msg = ex.Message }));
                    }
                }

                return await tcs.Task.ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                // 最外层捕获：确保任何异常都不会传播出去
                FileUtils.LogError($"{ex.Message}", "[FrontNotice] NoticeJs 最外层异常");
                return JsonConvert.SerializeObject(new { code = 500, msg = $"通知前端异常：{ex.Message}" });
            }
        }

        /// <summary>
        /// 执行脚本的核心逻辑
        /// </summary>
        private async Task<string> ExecuteScriptAsync(string requestDataJson, int timeoutMs)
        {
            // 先检查前端函数是否存在
            string checkScript = "typeof notifyFromCSharp === 'function'";
            var checkResult = await webBrowser.EvaluateScriptAsync(checkScript).ConfigureAwait(false);

            if (!checkResult.Success || !(checkResult.Result is bool exists) || !exists)
            {
                FileUtils.LogError("", "[FrontNotice] 前端 notifyFromCSharp 函数不存在");
                return JsonConvert.SerializeObject(new { code = 500, msg = "前端 notifyFromCSharp 函数不存在" });
            }

            // 执行通知脚本
            string script = $"var str_Temp = {JsonConvert.SerializeObject(requestDataJson)};\nnotifyFromCSharp(str_Temp);";

            var evalTask = webBrowser.EvaluateScriptAsync(script);
            var timeoutTask = Task.Delay(timeoutMs);
            var completedTask = await Task.WhenAny(evalTask, timeoutTask).ConfigureAwait(false);

            if (completedTask == timeoutTask)
            {
                return JsonConvert.SerializeObject(new { code = 0, msg = "超时了，不阻塞，继续执行" });
            }

            var response = await evalTask;
            if (response != null && response.Success && response.Result != null)
            {
                return response.Result.ToString();
            }

            return JsonConvert.SerializeObject(new { code = 0 });
        }

        /// <summary>
        /// 调用前端函数-二次封装
        /// </summary>
        /// <param name="action"></param>
        /// <param name="obj"></param>
        /// <returns></returns>
        public static async Task<string> NoticeFront(string action, object obj)
        {
            Dictionary<string, object> @params = new Dictionary<string, object>();
            @params.Add("code", 0);
            @params.Add("status", 200);
            @params.Add("action", action);
            @params.Add("data", obj);

            FrontNotice frontNotice = new FrontNotice();
            return await frontNotice.NoticeJs(JsonConvert.SerializeObject(@params));
        }

        /// <summary>
        /// 通知前端视频分析进度（action = videoAnalysisProgress），状态与 docs/视频分析进度通知.md 对齐。
        /// 分析状态变更时由 VideoApi.UpdateVideoAnalysisStatus / UpdateVideoAnalysisStatusSuccess 调用（started / completed / failed），
        /// 巨量视频下载完成后由 AnchorVideoBll.TryDownloadJuliangVideo 调用（downloadDone）。
        /// 覆盖自动分析与手动重新分析的进度推送（查看视频详细为只读流程，不触发）。
        /// </summary>
        /// <param name="videoId">视频 id</param>
        /// <param name="progressStatus">进度状态：started / downloadDone / completed / failed</param>
        /// <param name="msg">失败原因，仅 failed 时有值</param>
        public static void NoticeAnalysisProgress(string videoId, string progressStatus, string msg = null)
        {
            Dictionary<string, object> data = new Dictionary<string, object>
            {
                { "videoId", videoId },
                { "progressStatus", progressStatus }
            };
            if (msg != null)
            {
                data["msg"] = msg;
            }

            _ = NoticeFront("videoAnalysisProgress", data);
        }

        #region 企业号相关通知

        /// <summary>
        /// 发送企业号授权成功通知
        /// </summary>
        public static void sendEnterpriseAuthSuccess(string secUid)
        {
            Task.Run(async () =>
            {
                await NoticeFront("enterpriseAuthSuccess", new { secUid = secUid });
            });
        }

        /// <summary>
        /// 发送企业号授权失败通知
        /// </summary>
        public static void sendEnterpriseAuthFailed(string secUid, string message)
        {
            Task.Run(async () =>
            {
                await NoticeFront("enterpriseAuthFailed", new { secUid = secUid, message = message });
            });
        }

        /// <summary>
        /// 发送企业号数据更新通知
        /// </summary>
        public static void sendEnterpriseDataUpdate(string videoId, string dataType, string dataJson)
        {
            Task.Run(async () =>
            {
                await NoticeFront("enterpriseDataUpdate", new { videoId = videoId, dataType = dataType, data = dataJson });
            });
        }

        /// <summary>
        /// 发送企业号授权过期通知
        /// </summary>
        public static void sendEnterpriseAuthExpires(object anchorInfo)
        {
            Task.Run(async () =>
            {
                await NoticeFront("enterpriseAuthExpires", anchorInfo);
            });
        }

        #endregion

        /// <summary>
        /// 前端调用C#方法
        /// </summary>
        /// <param name="requestDataJson">请求的参数体
        /// {
        ///     int status: 状态 200表示成功
        ///     string className: 类名
        ///     string methodName: 方法名
        ///     object[] methodParameters: 方法参数
        ///     bool methodIsStatic: 方法是否是静态方法
        /// }
        /// </param>
        /// <param name="callback"></param>
        public void HandleJsRequest(string requestDataJson, IJavascriptCallback callback)
        {

            // 取出请求体的值
            JObject requestDataJsonObj = JObject.Parse(requestDataJson);
            string className = (string) requestDataJsonObj["className"];
            string methodName = (string) requestDataJsonObj["methodName"];
            bool methodIsStatic = (bool) requestDataJsonObj["methodIsStatic"];
            JArray jarray = (JArray) requestDataJsonObj["methodParameters"];
            object[] methodParameters = null;
            if (jarray != null && jarray.Count > 0)
            {
                methodParameters = jarray.ToObject<object[]>();
            }

            // 反射调用对应的方法
            JObject responseJsonObject = new JObject();
            try
            {
                object response = Reflection(className, methodName, methodParameters, methodIsStatic);
                if(response != null)
                {
                    responseJsonObject["data"] = JToken.FromObject(response);
                }
                responseJsonObject["msg"] = "处理成功";
                responseJsonObject["code"] = 0;
                responseJsonObject["status"] = 200;
            }
            catch (Exception e)
            {
                responseJsonObject["msg"] = $"发生异常：{e}";
                responseJsonObject["code"] = 500;
                responseJsonObject["status"] = 500;
            }
            

            // 执行回调(需捕获线程上下文)
            if (callback.CanExecute) {
                // 返回结果给前端
                callback.ExecuteAsync(responseJsonObject.ToString());
            }
        }

        /// <summary>
        /// 利用反射执行对应的方法
        /// </summary>
        /// <param name="className">类名</param>
        /// <param name="methodName">方法名</param>
        /// <param name="methodParameters">方法参数数组</param>
        /// <param name="methodIsStatic">是否是静态方法</param>
        /// <returns></returns>
        public object Reflection(string className, string methodName, object[] methodParameters, bool methodIsStatic)
        {

            if (methodParameters == null || methodParameters.Length < 1)
            {
                methodParameters = null;
            }

            // 获取当前程序集
            Assembly assembly = Assembly.GetExecutingAssembly();
            // 根据类名获取类型
            Type type = assembly.GetType(className);
            if (type != null)
            {

                // 根据方法名获取方法信息
                MethodInfo method = type.GetMethod(methodName);
                
                if (method != null)
                {
                    if (methodIsStatic)
                    {
                        // 调用的是静态方法
                        return method.Invoke(null, methodParameters);
                    }
                    else
                    {
                        // 调用的是实例方法
                        // 创建类的实例
                        object instance = Activator.CreateInstance(type);
                        // 调用方法
                        return method.Invoke(instance, methodParameters);
                    }
                }
                else
                {
                    FileUtils.log($" '{methodName}' 方法在 '{className}' 类里面没有找到");
                }
            }
            else
            {
                FileUtils.log($" '{className}' 类没有找到");
            }

            return null;
        }
    }
}
