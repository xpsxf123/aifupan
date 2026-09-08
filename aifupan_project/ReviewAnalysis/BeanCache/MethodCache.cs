using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Attributes;
using ReviewAnalysis.Utils;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using System.Web.UI.WebControls;

namespace ReviewAnalysis.BeanCache
{
    public static class MethodCache
    {
        private static readonly Dictionary<string, ControllerMethod> _methodCache = new Dictionary<string, ControllerMethod>();
        private static readonly Dictionary<Type, object> _controllerInstances = new Dictionary<Type, object>();
        private static readonly object _instanceLock = new object();

        public static void Initialize()
        {
            try
            {
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
            }
            catch (ReflectionTypeLoadException ex)
            {
                var loaderMessages = ex.LoaderExceptions
                    .Where(e => e != null)
                    .Select(e => e.Message)
                    .ToArray();

                var errorMsg = $"ReflectionTypeLoadException: 无法加载以下类型：\n{string.Join("\n", loaderMessages)}, msg = {ex.Message}";
                FileUtils.LogError(errorMsg, "Initialize - Type Load Failed");
                throw ex;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}\n{ex.StackTrace}", "Initialize - Unexpected Error");
                throw ex;
            }
        }

        public static ControllerMethod GetMethod(string uri)
        {
            return _methodCache.TryGetValue(uri, out var method) ? method : null;
        }

        public static object InvokeMethod(string uri, object[] parameters)
        {
            var methodInfo = GetMethod(uri);
            if (methodInfo != null)
            {
                try
                {
                    // 检查方法是否为静态方法
                    bool isStaticMethod = methodInfo.Method.IsStatic;

                    // 静态方法不需要实例
                    object instance = null;
                    if (!isStaticMethod)
                    {
                        // 优化：使用实例缓存，避免每次创建新实例
                        lock (_instanceLock)
                        {
                            if (!_controllerInstances.TryGetValue(methodInfo.ControllerType, out instance) || instance == null)
                            {
                                try
                                {
                                    instance = Activator.CreateInstance(methodInfo.ControllerType);
                                    if (instance == null)
                                    {
                                        throw new Exception($"无法创建 Controller 实例：{methodInfo.ControllerType.Name}");
                                    }
                                    _controllerInstances[methodInfo.ControllerType] = instance;
                                }
                                catch (Exception createEx)
                                {
                                    throw new Exception($"创建 Controller 实例失败：{methodInfo.ControllerType.Name}, 错误：{createEx.Message}", createEx);
                                }
                            }
                        }

                        // 双重检查：确保实例非空
                        if (instance == null)
                        {
                            throw new Exception($"Controller 实例为 null（无法从缓存获取）：{methodInfo.ControllerType.Name}");
                        }
                    }

                    var parameterArray = methodInfo.Method.GetParameters();
                    object[] paramValues = null;
                    if (parameters != null && parameterArray.Length > 0 && parameters.Length > 0)
                    {
                        // 以方法参数数量为准，避免实际传入参数少于方法参数时越界
                        int safeLen = Math.Min(parameters.Length, parameterArray.Length);
                        paramValues = new object[parameterArray.Length];
                        for (int i = 0; i < parameterArray.Length; i++)
                        {
                            // 如果实际传入参数不足，使用类型默认值
                            if (i >= safeLen)
                            {
                                paramValues[i] = parameterArray[i].ParameterType.IsValueType 
                                    ? Activator.CreateInstance(parameterArray[i].ParameterType) 
                                    : null;
                                LogServerUtils.LogError("控制器Http请求出错", "参数缺失",
                                    $"方法 {methodInfo.Method.Name} 的第 {i + 1} 个参数 [{parameterArray[i].Name}] 未提供，使用默认值");
                                continue;
                            }

                            var parameter = parameters[i];
                            var parameterType = parameterArray[i].ParameterType;

                            // 检查参数类型是否匹配，直接赋值
                            if (parameterType.IsInstanceOfType(parameter))
                            {
                                paramValues[i] = parameter;
                            }
                            else
                            {
                                // 对于复杂对象，尝试进行 JSON 反序列化
                                if (parameter is Newtonsoft.Json.Linq.JObject jObject)
                                {
                                    // JSON 对象 → 反序列化为目标类型
                                    paramValues[i] = jObject.ToObject(parameterType);
                                }
                                else if (parameter is Newtonsoft.Json.Linq.JArray jArray)
                                {
                                    // JSON 数组 → 反序列化为 List<T> / 数组等集合类型
                                    paramValues[i] = jArray.ToObject(parameterType);
                                }
                                else if (parameter is IConvertible)
                                {
                                    // Convert.ChangeType only works for primitives/string/decimal/DateTime.
                                    // For complex target types skip straight to JSON deserialization.
                                    bool isSimpleTarget = parameterType.IsPrimitive
                                        || parameterType == typeof(string)
                                        || parameterType == typeof(decimal)
                                        || parameterType == typeof(DateTime)
                                        || parameterType == typeof(Guid);

                                    paramValues[i] = isSimpleTarget
                                        ? Convert.ChangeType(parameter, parameterType)
                                        : JsonConvert.DeserializeObject(parameter.ToString(), parameterType);
                                }
                                else
                                {
                                    // 最终兜底：JSON 往返序列化，处理 List<T>、数组等已反序列化的 .NET 对象
                                    try
                                    {
                                        string json = JsonConvert.SerializeObject(parameter);
                                        paramValues[i] = JsonConvert.DeserializeObject(json, parameterType);
                                    }
                                    catch (Exception convertEx)
                                    {
                                        LogServerUtils.LogError("控制器Http请求出错", "通过控制器请求报错",
                                            $"Cannot convert parameter {i} from {parameter.GetType()} to {parameterType}: {convertEx.Message}");
                                        throw new InvalidCastException(
                                            $"Cannot convert parameter {i} from {parameter.GetType()} to {parameterType}", convertEx);
                                    }
                                }
                            }
                        }
                    }
                    
                    var result = methodInfo.Method.Invoke(instance, paramValues);
                    if (methodInfo.Method.ReturnType == typeof(void))
                    {
                        result = null;
                    }
                    else if (result is Task task)
                    {
                        // 通过 Task.Run 包装避免 SynchronizationContext 捕获，防止死锁
                        Task.Run(() => task).GetAwaiter().GetResult();
                        // 如果是泛型 Task<T>，则通过反射或 dynamic 获取结果
                        if (task.GetType().IsGenericType)
                        {
                            var resultProp = task.GetType().GetProperty("Result");
                            result = resultProp.GetValue(task);
                        }
                        else
                        {
                            result = null;
                        }
                    }

                    HttpReponse httpReponse = new HttpReponse(0, "成功", result);
                    return httpReponse;

                }
                catch (TargetInvocationException tie)
                {
                    // 异步方法抛出异常，检查是否为 AggregateException
                    var aggregateException = tie.InnerException as AggregateException;
                    if (aggregateException?.InnerExceptions.Count > 0)
                    {
                        foreach (var innerException in aggregateException.InnerExceptions)
                        {
                            if (innerException is CustomException customEx)
                            {
                                // 处理 CustomException，多个异常时只取第一个异常信息返回
                                HttpReponse aggregateHttpResponse = new HttpReponse(customEx.ErrorCode, customEx.Message, null);
                                FileUtils.LogError($"出错:{customEx}", "http返回");
                                return aggregateHttpResponse;
                            }
                        }
                    }

                    // 检查是否为 CustomException
                    var customException = tie.InnerException as CustomException;
                    if (customException != null)
                    {
                        // 处理 CustomException
                        HttpReponse customHttpResponse = new HttpReponse(customException.ErrorCode, customException.Message, null);
                        FileUtils.LogError($"出错:{customException}", "http返回");
                        return customHttpResponse;
                    }

                    // 处理其他类型的异常
                    string errorMessage = tie.InnerException != null ? tie.InnerException.Message : tie.Message;
                    HttpReponse httpResponse = new HttpReponse(500, errorMessage, null);
                    FileUtils.LogError($"出错:{tie}", "http返回");
                    return httpResponse;
                }
                catch (Exception ex)
                {
                    if (ex.InnerException != null)
                    {
                        string errorMessage = $"{methodInfo.ControllerType.Name} - {ex.InnerException.Message}";
                        HttpReponse httpReponse = new HttpReponse(500, errorMessage, null);
                        FileUtils.LogError($"Controller调用失败 - URI:{uri}, Controller:{methodInfo.ControllerType.Name}, 异常:{ex}", "http返回");
                        return httpReponse;
                    }
                    else
                    {
                        string errorMessage = $"{methodInfo.ControllerType.Name} - {ex.Message}";
                        HttpReponse httpReponse = new HttpReponse(500, errorMessage, null);
                        FileUtils.LogError($"Controller调用失败 - URI:{uri}, Controller:{methodInfo.ControllerType.Name}, 异常:{ex}", "http返回");
                        return httpReponse;
                    }
                }
            }
            else
            {
                HttpReponse httpReponse = new HttpReponse(404, uri+"找不到对应的方法",null);
                FileUtils.LogError($"出错:找不到对应的方法", "http返回");
                //LogServerUtils.LogError("控制器Http请求出错", "通过控制器请求报错", $"找不到{uri}对应的方法");
                return httpReponse;
            }
        }

        /// <summary>
        /// 清空实例缓存（用于需要重新创建实例的场景）
        /// </summary>
        public static void ClearInstanceCache()
        {
            lock (_instanceLock)
            {
                foreach (var instance in _controllerInstances.Values)
                {
                    if (instance is IDisposable disposable)
                    {
                        try
                        {
                            disposable.Dispose();
                        }
                        catch { /* 忽略释放异常 */ }
                    }
                }
                _controllerInstances.Clear();
            }
        }
    }
}
