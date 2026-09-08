using douyin.Utils;
using global::ReviewAnalysis.Attributes;
using global::ReviewAnalysis.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;


    namespace ReviewAnalysis.BeanCache
    {
        /// <summary>
        /// WinForm 适配版 - 控制器方法缓存容器（异步、UI线程安全、取消支持）
        /// </summary>
        public static class MethodAsyncCache
        {
            #region 常量 & 线程安全缓存
            private const int DEFAULT_SUCCESS_CODE = 0;
            private const int DEFAULT_NOT_FOUND_CODE = 404;
            private const int DEFAULT_ERROR_CODE = 500;
            private const string DEFAULT_SUCCESS_MSG = "成功";
            private const string URI_SEPARATOR = "/";

            // 线程安全缓存
            private static readonly ConcurrentDictionary<string, ControllerMethod> _methodCache = new ConcurrentDictionary<string, ControllerMethod>();
            // 初始化标记 + 锁（WinForm多线程启动防护）
            private static bool _isInitialized = false;
            private static readonly object _initLock = new object();
            // WinForm UI线程同步上下文（用于切换UI线程）
            private static SynchronizationContext _uiSyncContext;
            #endregion

            #region WinForm 初始化适配（异步+UI线程安全）
            /// <summary>
            /// 初始化方法缓存（WinForm异步版，不阻塞UI线程）
            /// </summary>
            /// <param name="ownerForm">所属WinForm窗体（用于获取UI同步上下文）</param>
            /// <param name="cancellationToken">取消令牌（UI端可取消初始化）</param>
            public static async Task InitializeAsync(Form ownerForm, CancellationToken cancellationToken = default)
            {

                // 双重检查锁定：避免重复初始化
                if (_isInitialized) return;
                lock (_initLock)
                {
                    if (_isInitialized) return;
                    cancellationToken.ThrowIfCancellationRequested();
                }

                try
                {
                    // 异步执行反射扫描（脱离UI线程，避免阻塞）
                    await Task.Run(() =>
                    {
                        cancellationToken.ThrowIfCancellationRequested();

                        //// 1. 扫描当前程序集所有控制器类型
                        //var controllerTypes = Assembly.GetExecutingAssembly()
                        //    .GetTypes()
                        //    .Where(t => t.IsClass && !t.IsAbstract && t.GetCustomAttribute<RestControllerAttribute>() != null)
                        //    .ToList();

                        //foreach (var controllerType in controllerTypes)
                        //{
                        //    cancellationToken.ThrowIfCancellationRequested();

                        //    var restAttr = controllerType.GetCustomAttribute<RestControllerAttribute>();
                        //    var controllerBaseUri = NormalizeUri(restAttr?.Value ?? string.Empty);

                        //    // 2. 过滤控制器的公共实例方法
                        //    //var actionMethods = controllerType.GetMethods(BindingFlags.Public | BindingFlags.Instance | BindingFlags.DeclaredOnly)
                        //    //    .Where(m => !m.IsStatic && !m.IsAbstract);
                        //    var actionMethods = controllerType.GetMethods();

                        //    foreach (var method in actionMethods)
                        //    {
                        //        cancellationToken.ThrowIfCancellationRequested();
                        //        string sUri;
                        //        // 3. 处理HTTP注解
                        //        var httpGetAttr = method.GetCustomAttribute<HttpGetAttribute>();
                        //        if (httpGetAttr == null) continue;
                        //        sUri = httpGetAttr.Value;
                        //        var httpPostAttr = method.GetCustomAttribute<HttpPostAttribute>();
                        //        if (httpPostAttr == null) continue;
                        //        sUri = httpPostAttr.Value;
                        //        // 4. 规范化URI并缓存
                        //        var actionUri = NormalizeUri(sUri);
                        //        var fullUri = $"{controllerBaseUri}{actionUri}".TrimEnd('/');
                        //        var controllerMethod = new ControllerMethod(controllerType, method);
                        //        _methodCache.TryAdd(fullUri, controllerMethod);
                        //    }
                        //}


                        var types = Assembly.GetExecutingAssembly().GetTypes();
                        foreach (var type in types)
                        {
                            // 支持 RestControllerAttribute 和 RestAsyncControllerAttribute
                            var restControllerAttribute = type.GetCustomAttribute<RestControllerAttribute>();
                            var restAsyncControllerAttribute = restControllerAttribute == null
                                ? type.GetCustomAttribute<RestAsyncControllerAttribute>()
                                : null;

                            string controllerValue = null;
                            if (restControllerAttribute != null)
                            {
                                controllerValue = restControllerAttribute.Value;
                            }
                            else if (restAsyncControllerAttribute != null)
                            {
                                controllerValue = restAsyncControllerAttribute.Value;
                            }

                            if (controllerValue != null)
                            {
                                var methods = type.GetMethods();
                                foreach (var method in methods)
                                {
                                    var httpPostAttribute = method.GetCustomAttribute<HttpPostAttribute>();
                                    if (httpPostAttribute != null)
                                    {
                                        var uri = controllerValue + httpPostAttribute.Value;
                                        _methodCache[uri] = new ControllerMethod(type, method);
                                        continue;
                                    }

                                    var httpGetAttribute = method.GetCustomAttribute<HttpGetAttribute>();
                                    if (httpGetAttribute != null)
                                    {
                                        var uri = controllerValue + httpGetAttribute.Value;
                                        _methodCache[uri] = new ControllerMethod(type, method);
                                    }
                                }
                            }
                        }
                        foreach (KeyValuePair<string, ControllerMethod> kvp in _methodCache)
                        {
                            MemberInfo memberInfo = kvp.Value.Method;
                        }

                        // 标记初始化完成
                        _isInitialized = true;

                        // 异步日志（自动切换到UI线程）
                        //LogToUi($"方法缓存初始化完成，加载 {_methodCache.Count} 个接口路由", LogLevel.Info);
                    }, cancellationToken).ConfigureAwait(false);
                }
                catch (OperationCanceledException)
                {
                    //LogToUi("方法缓存初始化被取消", LogLevel.Warn);
                    throw;
                }
                catch (ReflectionTypeLoadException ex)
                {
                    var loaderErrors = ex.LoaderExceptions
                        .Where(e => e != null)
                        .Select(e => $"  - {e.Message}")
                        .ToList();
                    var errorMsg = $"反射类型加载失败：\n{string.Join("\n", loaderErrors)}\n原异常：{ex.Message}";

                    //LogToUi(errorMsg, LogLevel.Error);
                    await FileUtils.LogErrorAsync(errorMsg, "MethodCache.InitializeAsync");
                    throw new InvalidOperationException("MethodCache初始化失败（反射类型加载异常）", ex);
                }
                catch (Exception ex)
                {
                    var errorMsg = $"MethodCache初始化异常：{ex.Message}\n堆栈：{ex.StackTrace}";
                    //LogToUi(errorMsg, LogLevel.Error);
                    await FileUtils.LogErrorAsync(errorMsg, "MethodCache.InitializeAsync");
                    throw new InvalidOperationException("MethodCache初始化失败", ex);
                }
            }
            #endregion

        #region WinForm 异步调用方法（无阻塞UI）
        /// <summary>
        /// WinForm 异步执行控制器方法（无阻塞UI，支持取消、UI线程回调）
        ///
        /// 关于UI线程的重要说明：
        /// 1. 如果异步方法需要打开窗体或操作UI控件，可以通过以下方式：
        ///    a. 直接调用UI操作（本方法已配置 ConfigureAwait(true) 保持UI上下文）
        ///    b. 使用 ownerForm.Invoke 或 ownerForm.BeginInvoke 切换到UI线程
        ///    c. 使用 await Task.Run() 将耗时操作放到后台线程
        ///
        /// 示例：
        /// <code>
        /// public async Task OpenLogForm()
        /// {
        ///     // 方式1：直接调用（本方法已保持UI上下文）
        ///     LogForm.Instance.Show();
        ///
        ///     // 方式2：显式切换到UI线程
        ///     ownerForm.Invoke((Action)(() => LogForm.Instance.Show()));
        ///
        ///     // 方式3：耗时操作放到后台线程
        ///     await Task.Run(() => {
        ///         // CPU密集型操作
        ///     }).ConfigureAwait(true); // 保持UI上下文以便后续UI操作
        /// }
        /// </code>
        /// </summary>
        /// <param name="uri">接口路由</param>
        /// <param name="parameters">方法参数</param>
        /// <param name="ownerForm">所属窗体（用于UI线程切换）</param>
        /// <param name="cancellationToken">取消令牌（UI端可取消）</param>
        /// <param name="serviceProvider">DI容器（可选，创建控制器实例）</param>
        /// <returns>统一HTTP响应</returns>
        public static async Task<HttpReponse> InvokeMethodAsync(
            string uri,
            object[] parameters,
            Form ownerForm,
            CancellationToken cancellationToken = default,
            IServiceProvider serviceProvider = null)
            {
                try
                {
                    // 0. 前置校验
                    cancellationToken.ThrowIfCancellationRequested();
                    if (string.IsNullOrWhiteSpace(uri))
                    {
                        var errorResp = CreateErrorResponse(DEFAULT_ERROR_CODE, "请求URI不能为空");
                        //LogToUi($"调用失败：{errorResp.Message}", LogLevel.Error, ownerForm);
                        return errorResp;
                    }

                    // 1. 获取缓存方法（UI线程安全）
                    var methodMeta = await Task.Run(() => GetMethod(uri), cancellationToken).ConfigureAwait(false);
                    if (methodMeta == null)
                    {
                        var errorMsg = $"未找到URI [{uri}] 对应的控制器方法";
                        //LogToUi(errorMsg, LogLevel.Error, ownerForm);
                        FileUtils.LogError(errorMsg, "MethodCache.InvokeMethodAsync");
                        return CreateErrorResponse(DEFAULT_NOT_FOUND_CODE, errorMsg);
                    }

                    // 2. 异步创建控制器实例（脱离UI线程）
                    var controllerInstance = await Task.Run(() =>
                            CreateControllerInstance(methodMeta.ControllerType, serviceProvider),
                        cancellationToken).ConfigureAwait(false);

                    // 3. 异步预处理参数（避免大参数转换阻塞UI）
                    var invokeParams = await Task.Run(() =>
                            PrepareInvokeParameters(methodMeta, parameters),
                        cancellationToken).ConfigureAwait(false);

                    // 4. 异步执行方法（支持同步/异步方法）
                    object methodResult = await ExecuteMethodAsync(methodMeta, controllerInstance, invokeParams, cancellationToken)
                            .ConfigureAwait(false);

                    // 5. 封装成功响应
                    var successResult = methodMeta.Method.ReturnType == typeof(void) ? null : methodResult;
                    var successResp = new HttpReponse(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MSG, successResult);

                    // 日志回显UI（可选）
                    //LogToUi($"调用URI [{uri}] 成功", LogLevel.Info, ownerForm);
                    return successResp;
                }
                catch (OperationCanceledException)
                {
                    //LogToUi($"调用URI [{uri}] 被取消", LogLevel.Warn, ownerForm);
                    return CreateErrorResponse(DEFAULT_ERROR_CODE, "操作已取消");
                }
                catch (TargetInvocationException tie)
                {
                    // 解包反射内部异常
                    var errorResp = HandleInvocationInnerException(tie, uri);
                    //LogToUi($"调用URI [{uri}] 业务异常：{errorResp.Message}", LogLevel.Error, ownerForm);
                    return errorResp;
                }
                catch (CustomException ex)
                {
                    var errorMsg = $"执行URI [{uri}] 异常：{ex.Message}";
                    //LogToUi(errorMsg, LogLevel.Error, ownerForm);
                    FileUtils.LogError($"{errorMsg}\n堆栈：{ex.StackTrace}", "MethodCache.InvokeMethodAsync");
                    return CreateErrorResponse(DEFAULT_ERROR_CODE, ex.Message);
                }
                catch (Exception ex)
                {
                    var errorMsg = $"执行URI [{uri}] 异常：{ex.Message}";
                    //LogToUi(errorMsg, LogLevel.Error, ownerForm);
                    FileUtils.LogError($"{errorMsg}\n堆栈：{ex.StackTrace}", "MethodCache.InvokeMethodAsync");
                    return CreateErrorResponse(DEFAULT_ERROR_CODE, errorMsg);
                }
            }
            #endregion

            #region 核心辅助方法（WinForm适配）
            /// <summary>
            /// 获取缓存方法（线程安全）
            /// </summary>
            //private static ControllerMethod GetMethod(string uri)
            //{
            //    if (string.IsNullOrWhiteSpace(uri)) return null;
            //    var normalizedUri = NormalizeUri(uri).TrimEnd('/');
            //    _methodCache.TryGetValue(normalizedUri, out var method);
            //    return method;
            //}
        public static ControllerMethod GetMethod(string uri)
        {
            return _methodCache.TryGetValue(uri, out var method) ? method : null;
        }

        /// <summary>
        /// 异步执行方法（支持同步/异步控制器方法）
        /// </summary>
        private static async Task<object> ExecuteMethodAsync(
                ControllerMethod methodMeta,
                object controllerInstance,
                object[] invokeParams,
                CancellationToken cancellationToken)
            {
                cancellationToken.ThrowIfCancellationRequested();

                // 执行方法调用
                var invokeResult = methodMeta.Method.Invoke(controllerInstance, invokeParams);

                // 如果是异步方法（Task/Task<T>），等待执行完成
                if (invokeResult is Task task)
                {
                    // 不使用 ConfigureAwait(false)，保持同步上下文以支持UI操作
                    // 这样异步方法内部可以直接打开窗体或操作UI控件
                    await task.ConfigureAwait(true);

                    // 获取Task<T>的返回值
                    if (task.GetType().IsGenericType && task.GetType().GetGenericTypeDefinition() == typeof(Task<>))
                    {
                        var resultProperty = task.GetType().GetProperty("Result");
                        return resultProperty?.GetValue(task);
                    }
                    return null; // Task无返回值
                }

                // 同步方法直接返回结果
                return invokeResult;
            }

            /// <summary>
            /// 日志输出到UI（自动切换到UI线程）
            /// </summary>
            private static void LogToUi(string message, LogLevel level = LogLevel.Info, Form ownerForm = null)
            {
                // 优先使用传入的窗体，其次使用全局同步上下文
                var targetForm = ownerForm;
                if (targetForm == null || targetForm.IsDisposed) return;

                // 切换到UI线程执行
                if (targetForm.InvokeRequired)
                {
                    targetForm.BeginInvoke(new Action(() => LogToUi(message, level, targetForm)));
                    return;
                }

                // 示例：输出到WinForm的TextBox（可替换为项目实际日志控件）
                try
                {
                    var logTextBox = targetForm.Controls.Find("txtLog", true).FirstOrDefault() as TextBox;
                    if (logTextBox != null && !logTextBox.IsDisposed)
                    {
                        logTextBox.AppendText($"[{DateTime.Now:HH:mm:ss}] [{level}] {message}\r\n");
                        // 自动滚动到最后一行
                        logTextBox.SelectionStart = logTextBox.Text.Length;
                        logTextBox.ScrollToCaret();
                    }
                }
                catch
                {
                    // 忽略UI控件操作异常（如窗体已关闭）
                }

                // 同时写入文件日志
                if (level == LogLevel.Error)
                    FileUtils.LogError(message, "MethodCache.UI");
                else
                    FileUtils.log(message, "MethodCache.UI");
            }

            /// <summary>
            /// 规范化URI
            /// </summary>
            private static string NormalizeUri(string uri)
            {
                if (string.IsNullOrWhiteSpace(uri)) return URI_SEPARATOR;
                uri = uri.Trim();
                return uri.StartsWith(URI_SEPARATOR) ? uri : $"{URI_SEPARATOR}{uri}";
            }

            /// <summary>
            /// 创建控制器实例
            /// </summary>
            private static object CreateControllerInstance(Type controllerType, IServiceProvider serviceProvider)
            {
                try
                {
                    if (serviceProvider != null)
                    {
                        var instance = serviceProvider.GetService(controllerType);
                        if (instance != null) return instance;
                    }
                    return Activator.CreateInstance(controllerType);
                }
                catch (Exception ex)
                {
                    throw new InvalidOperationException($"创建控制器 [{controllerType.FullName}] 实例失败：{ex.Message}", ex);
                }
            }

            /// <summary>
            /// 预处理调用参数
            /// </summary>
            private static object[] PrepareInvokeParameters(ControllerMethod methodMeta, object[] inputParams)
            {
                var methodParams = methodMeta.Method.GetParameters();
                var invokeParams = new object[methodParams.Length];

                if (inputParams == null || inputParams.Length == 0)
                {
                    for (int i = 0; i < methodParams.Length; i++)
                    {
                        invokeParams[i] = methodParams[i].HasDefaultValue ? methodParams[i].DefaultValue : null;
                    }
                    return invokeParams;
                }

                if (inputParams.Length > methodParams.Length)
                {
                    throw new ArgumentException($"输入参数数量 [{inputParams.Length}] 超过方法参数数量 [{methodParams.Length}]");
                }

                for (int i = 0; i < inputParams.Length; i++)
                {
                    var inputParam = inputParams[i];
                    var targetParamType = methodParams[i].ParameterType;

                    if (inputParam == null)
                    {
                        invokeParams[i] = null;
                        continue;
                    }

                    if (targetParamType.IsInstanceOfType(inputParam))
                    {
                        invokeParams[i] = inputParam;
                        continue;
                    }

                    if (inputParam is JObject jObject)
                    {
                        invokeParams[i] = jObject.ToObject(targetParamType);
                        continue;
                    }

                    if (inputParam is JArray jArray)
                    {
                        invokeParams[i] = jArray.ToObject(targetParamType);
                        continue;
                    }

                    if (inputParam is IConvertible)
                    {
                        // Convert.ChangeType only works for primitives / string / decimal / DateTime.
                        // For complex target types (classes, structs) it always throws — skip straight to JSON.
                        bool isSimpleTarget = targetParamType.IsPrimitive
                            || targetParamType == typeof(string)
                            || targetParamType == typeof(decimal)
                            || targetParamType == typeof(DateTime)
                            || targetParamType == typeof(Guid);

                        if (isSimpleTarget)
                            invokeParams[i] = Convert.ChangeType(inputParam, targetParamType);
                        else
                            invokeParams[i] = JsonConvert.DeserializeObject(inputParam.ToString(), targetParamType);
                        continue;
                    }

                    // Fallback: JSON round-trip for List<T> and other already-deserialized .NET objects
                    invokeParams[i] = JsonConvert.DeserializeObject(JsonConvert.SerializeObject(inputParam), targetParamType);
                }

                for (int i = inputParams.Length; i < methodParams.Length; i++)
                {
                    invokeParams[i] = methodParams[i].HasDefaultValue ? methodParams[i].DefaultValue : null;
                }

                return invokeParams;
            }

            /// <summary>
            /// 处理反射调用内部异常
            /// </summary>
            private static HttpReponse HandleInvocationInnerException(TargetInvocationException tie, string uri)
            {
                var innerEx = tie.InnerException ?? tie;
                var errorMsg = $"执行URI [{uri}] 业务异常：{innerEx.Message}";

                if (innerEx is CustomException customEx)
                {
                    FileUtils.LogError($"{errorMsg}（错误码：{customEx.ErrorCode}）", "MethodCache.InvokeMethodAsync");
                    return new HttpReponse(customEx.ErrorCode, customEx.Message, null);
                }

                FileUtils.LogError($"{errorMsg}\n堆栈：{innerEx.StackTrace}", "MethodCache.InvokeMethodAsync");
                return CreateErrorResponse(DEFAULT_ERROR_CODE, errorMsg);
            }

            /// <summary>
            /// 创建错误响应
            /// </summary>
            private static HttpReponse CreateErrorResponse(int code, string message)
            {
                return new HttpReponse(code, message ?? string.Empty, null);
            }
            #endregion

            #region 辅助枚举/类
            /// <summary>
            /// 日志级别
            /// </summary>
            public enum LogLevel
            {
                Info,
                Warn,
                Error
            }

            ///// <summary>
            ///// 控制器方法元数据
            ///// </summary>
            //public class ControllerMethod
            //{
            //    public Type ControllerType { get; }
            //    public MethodInfo Method { get; }

            //    public ControllerMethod(Type controllerType, MethodInfo method)
            //    {
            //        ControllerType = controllerType ?? throw new ArgumentNullException(nameof(controllerType));
            //        Method = method ?? throw new ArgumentNullException(nameof(method));
            //    }
            //}

            ///// <summary>
            ///// 统一HTTP响应对象
            ///// </summary>
            //public class HttpResponse
            //{
            //    public int Code { get; }
            //    public string Message { get; }
            //    public object Data { get; }

            //    public HttpResponse(int code, string message, object data)
            //    {
            //        Code = code;
            //        Message = message ?? string.Empty;
            //        Data = data;
            //    }
            //}

        /// <summary>
        /// HTTP方法注解基类
        /// </summary>
        [AttributeUsage(AttributeTargets.Method, Inherited = false)]
        public class HttpAsyncMethodAttribute : Attribute
        {
            public string Name { get; }
            public string Value { get; }

            public HttpAsyncMethodAttribute(string name, string value)
            {
                Name = name;
                Value = value;
            }
        }

        // 适配原有注解
        [AttributeUsage(AttributeTargets.Method, Inherited = false)]
        public class HttpAsyncGetAttribute : HttpAsyncMethodAttribute
        {
            public HttpAsyncGetAttribute(string name, string value) : base(name,value) { }
        }
        [AttributeUsage(AttributeTargets.Method, Inherited = false)]
        public class HttpAsyncPostAttribute : HttpAsyncMethodAttribute
        {
            public HttpAsyncPostAttribute(string name, string value) : base(name, value) { }
        }
        #endregion
    }

    //// 自定义异常（保持原有逻辑）
    //public class CustomException : Exception
    //{
    //    public int ErrorCode { get; }
    //    public CustomException(string message, int errorCode = 500) : base(message) => ErrorCode = errorCode;
    //}
}
