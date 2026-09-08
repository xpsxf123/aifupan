using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliangApi;
using ReviewAnalysis.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using douyin.Utils;

namespace ReviewAnalysis.plugins.dataPullers
{
    /// <summary>
    /// 巨量百应数据拉取器
    /// 职责：调用巨量API获取数据，返回原始JSON
    /// </summary>
    public class JuliangedDataPuller : BaseDataPuller
    {
        public JuliangedDataPuller() : base("juliang", "巨量百应") { }

        /// <summary>
        /// 拉取巨量数据（并行调用6个API）
        /// </summary>
        public override async Task<string> PullDataAsync(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("roomId为空，无法拉取数据", _platformName);
                    return null;
                }

                var cookies = GetCookies(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"Cookie获取失败，主播【{anchorInfo?.AnchorName}】", _platformName);
                    return null;
                }

                string cookieStr = BuildCookieString(cookies);

                FileUtils.LogRpa($"开始拉取巨量数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", _platformName);

                // 并行调用API（带错误响应检查）
                var tasks = new Task<string>[]
                {
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.LiveTalentAsync(roomId, cookieStr), "巨量达人", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.ProductPaymentDataAsync(roomId, cookieStr), "巨量商品", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.LiveBasicScreenAsync(roomId, cookieStr), "巨量基础大屏", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.PayUserPortraitAsync(roomId, cookieStr), "巨量支付画像", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.WatchUserPortraitAsync(roomId, cookieStr), "巨量观看画像", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.FlowOrderSourceAsync(roomId, cookieStr), "巨量流量来源", anchorInfo)
                };

                var results = await Task.WhenAll(tasks);

                // 合并结果
                var mergedData = MergeApiResults(results, roomId);

                // 为商品数据补充趋势数据(statisticsCurve)
                //await EnrichProductTrendsAsync(mergedData, roomId, cookieStr);

                string json = JsonConvert.SerializeObject(mergedData);

                FileUtils.LogRpa($"巨量数据拉取完成，roomId={roomId}", _platformName);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取巨量数据异常: {ex.Message}", _platformName);
                return null;
            }
        }
        
        /// <summary>
        /// 拉取巨量数据（带错误处理）
        /// </summary>
        /// <param name="anchorInfo"></param>
        /// <param name="roomId"></param>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public async Task<string> PullDataAsyncErrorMsg(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa("roomId为空，无法拉取数据", _platformName);
                    return null;
                }

                var cookies = GetCookies(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"Cookie获取失败，主播【{anchorInfo?.AnchorName}】", _platformName);
                    return null;
                }

                string cookieStr = BuildCookieString(cookies);

                FileUtils.LogRpa($"开始拉取巨量数据，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", _platformName);

                // 并行调用API（带错误响应检查）
                var tasks = new Task<string>[]
                {
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.LiveTalentAsync(roomId, cookieStr), "巨量达人", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.ProductPaymentDataAsync(roomId, cookieStr), "巨量商品", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.LiveBasicScreenAsync(roomId, cookieStr), "巨量基础大屏", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.PayUserPortraitAsync(roomId, cookieStr), "巨量支付画像", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.WatchUserPortraitAsync(roomId, cookieStr), "巨量观看画像", anchorInfo),
                    CallApiWithRetryAndCheck(() => JuliangApiDataApi.FlowOrderSourceAsync(roomId, cookieStr), "巨量流量来源", anchorInfo)
                };

                var results = await Task.WhenAll(tasks);

                // 判断是否全部为null
                bool flag = true;
                for (int i = 0; i < results.Length; i++)
                {
                    if (!string.IsNullOrEmpty(results[i]))
                    {
                        flag = false;
                        break;
                    }
                }
                if (flag)
                {
                    return null;
                }

                // 合并结果
                var mergedData = MergeApiResults(results, roomId);

                // 为商品数据补充趋势数据(statisticsCurve)
                //await EnrichProductTrendsAsync(mergedData, roomId, cookieStr);

                string json = JsonConvert.SerializeObject(mergedData);

                FileUtils.LogRpa($"巨量数据拉取完成，roomId={roomId}", _platformName);

                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取巨量数据异常: {ex.Message}", _platformName);
                return null;
            }
        }

        /// <summary>
        /// 获取Cookie（内联实现）
        /// </summary>
        protected override Dictionary<string, string> GetCookies(string secUid)
        {
            try
            {
                // 1. 检查巨量 Cookie 文件
                string cookiePath = GetJuliangCookiePath(secUid);
                bool fileExists = File.Exists(cookiePath);

                if (!fileExists)
                {
                    FileUtils.LogRpa($"巨量Cookie文件不存在，无法采集: {cookiePath}", _platformName);
                    return new Dictionary<string, string>();
                }

                // 2. 获取巨量 Cookie
                var cookies = GetJuliangCookiesFromLocal(secUid);

                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("巨量Cookie文件存在但内容为空", _platformName);
                    return new Dictionary<string, string>();
                }

                // 3. 获取 sessionid（优先从巨量文件，备用从千川）
                // 注意：巨量文件和千川文件的 sessionid 可能不同，经测试巨量 API 需要使用巨量文件中的 sessionid
                string sessionId = GetJuliangCookie(secUid, "sessionid");
                string sessionIdSource = "巨量";
                if (string.IsNullOrEmpty(sessionId))
                {
                    // 备用：从千川文件获取
                    sessionId = GetQianchuanCookie(secUid, "sessionid");
                    sessionIdSource = "千川";
                }

                if (!string.IsNullOrEmpty(sessionId))
                {
                    cookies["sessionid"] = sessionId;
                }
                else
                {
                    // sessionid 缺失时记录警告，但继续采集（部分API可能不需要）
                    FileUtils.LogRpa("sessionid获取失败（千川和巨量文件都没有），部分API可能返回错误", _platformName);
                }

                // 简洁汇总日志

                return cookies;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从本地获取巨量Cookie
        /// </summary>
        private Dictionary<string, string> GetJuliangCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = GetJuliangCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return new Dictionary<string, string>();
                }

                var helper = new CookiePersistenceHelper(cookiePath);
                var cookies = helper.LoadCefCookiesFromLocal(false);
                JuliangUtils.MigrateCompassLuopanDt(cookies, JuliangUtils.getCookiePath(secUid));
                if (cookies == null || cookies.Count == 0)
                {
                    return new Dictionary<string, string>();
                }

                var result = new Dictionary<string, string>();
                foreach (var cookie in cookies)
                {
                    result[cookie.Name] = cookie.Value;
                }
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取巨量Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从千川获取指定Cookie
        /// </summary>
        private string GetQianchuanCookie(string secUid, string cookieName)
        {
            try
            {
                string cookiePath = GetQianchuanCookiePath(secUid);
                
                if (!File.Exists(cookiePath))
                {
                    return null;
                }

                var helper = new qianchuan.CookiePersistenceHelper(cookiePath);
                var localCookies = helper.LoadCefCookiesFromLocal(false);
                
                if (localCookies == null || localCookies.Count == 0)
                {
                    return null;
                }

                var cookie = localCookies.FirstOrDefault(c => c.Name == cookieName);
                
                return cookie?.Value;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川Cookie异常: {ex.Message}", _platformName);
                return null;
            }
        }

        /// <summary>
        /// 从巨量文件获取指定Cookie
        /// </summary>
        private string GetJuliangCookie(string secUid, string cookieName)
        {
            try
            {
                string cookiePath = GetJuliangCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return null;
                }

                var helper = new CookiePersistenceHelper(cookiePath);
                var localCookies = helper.LoadCefCookiesFromLocal(false);
                if (localCookies == null || localCookies.Count == 0)
                {
                    return null;
                }

                var cookie = localCookies.FirstOrDefault(c => c.Name == cookieName);
                return cookie?.Value;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从巨量文件获取Cookie异常: {ex.Message}", _platformName);
                return null;
            }
        }

        /// <summary>
        /// 带重试和错误响应检查的API调用。
        /// 网络异常/超时/返回null → 重试最多 maxRetry 次；
        /// 业务错误（IsErrorResponse）→ 区分处理：服务器错误触发授权过期流程，其他错误记录日志后返回null。
        /// </summary>
        private async Task<string> CallApiWithRetryAndCheck(Func<Task<string>> apiCall, string apiName, AnchorInfo anchorInfo = null, int maxRetry = 3)
        {
            for (int i = 0; i < maxRetry; i++)
            {
                try
                {
                    var result = await apiCall();
                    if (!string.IsNullOrEmpty(result))
                    {
                        // 检查是否为错误响应
                        if (JuliangApiDataApi.IsErrorResponse(result))
                        {
                            // 服务器错误 = 授权过期（罗盘Token失效），触发重新授权流程
                            if (JuliangApiDataApi.IsAuthExpired(result))
                            {
                                HandleAuthExpired(anchorInfo, apiName, result);
                            }
                            else
                            {
                                FileUtils.LogRpa($"{apiName} API返回错误响应，主播【{anchorInfo?.AnchorName}】: {result}", _platformName);
                            }
                            return null;
                        }
                        return result;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"{apiName} API调用失败（第{i + 1}次），主播【{anchorInfo?.AnchorName}】: {ex.Message}", _platformName);
                    if (i < maxRetry - 1) await Task.Delay(1000 * (i + 1));
                }
            }
            return null;
        }

        /// <summary>
        /// 获取巨量Cookie路径
        /// </summary>
        private string GetJuliangCookiePath(string secUid)
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = GetCachePathMd5(secUid);
            return $"{cachePath}\\jlby-{md5Str}";
        }

        /// <summary>
        /// 获取千川Cookie路径
        /// </summary>
        private string GetQianchuanCookiePath(string secUid)
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = GetCachePathMd5(secUid);
            return $"{cachePath}\\qianchuan-{md5Str}";
        }

        /// <summary>
        /// 计算缓存路径MD5（与 JuliangedUtils.getCachePathMd5 保持一致）
        /// </summary>
        private string GetCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }

        /// <summary>
        /// 合并6个API的结果
        /// </summary>
        private JObject MergeApiResults(string[] results, string roomId)
        {
            var merged = new JObject
            {
                ["roomId"] = roomId,
                ["collectTime"] = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                ["data"] = new JObject()
            };

            string[] apiNames = { "liveTalent", "liveProduct", "liveBasicScreen", "payUserPortrait", "watchUserPortrait", "trafficSource" };

            for (int i = 0; i < results.Length; i++)
            {
                if (!string.IsNullOrEmpty(results[i]))
                {
                    try
                    {
                        var data = JObject.Parse(results[i]);
                        merged["data"][apiNames[i]] = data;
                    }
                    catch
                    {
                        // 解析失败，跳过
                    }
                }
            }

            return merged;
        }

        /// <summary>
        /// 为商品数据补充趋势数据(statisticsCurve)
        /// 从商品列表中提取productId，调用ProductOverallTrendHelper获取趋势数据
        /// </summary>
        private async Task EnrichProductTrendsAsync(JObject mergedData, string roomId, string cookieStr)
        {
            try
            {
                var liveProduct = mergedData["data"]?["liveProduct"];
                if (liveProduct == null) return;

                // 获取商品列表（兼容两种结构）
                JArray productList = liveProduct["product_list"] as JArray;
                if (productList == null && liveProduct["data"] != null)
                {
                    productList = liveProduct["data"]["product_list"] as JArray;
                }

                if (productList == null || productList.Count == 0) return;

                // 从cookieStr中提取COMPASS_LUOPAN_DT
                string luopanDt = null;
                if (!string.IsNullOrEmpty(cookieStr))
                {
                    var cookieParts = cookieStr.Split(';');
                    foreach (var part in cookieParts)
                    {
                        var kv = part.Trim().Split('=');
                        if (kv.Length == 2 && kv[0].Trim() == "COMPASS_LUOPAN_DT")
                        {
                            luopanDt = kv[1].Trim();
                            break;
                        }
                    }
                }

                if (string.IsNullOrEmpty(luopanDt))
                {
                    FileUtils.LogRpa("COMPASS_LUOPAN_DT为空，跳过商品趋势数据获取", _platformName);
                    return;
                }

                string trendCookie = $"LUOPAN_DT={luopanDt}";
                var trendHelper = new ProductOverallTrendHelper(roomId, trendCookie);

                int enrichedCount = 0;
                foreach (var item in productList)
                {
                    try
                    {
                        string productId = GetProductFieldValue(item, "product_id");
                        if (string.IsNullOrEmpty(productId)) continue;

                        var trendResult = await trendHelper.GetProductOverallTrendAsync(productId);
                        if (trendResult != null && (trendResult.LeftTrends.Count > 0 || trendResult.RightTrends.Count > 0))
                        {
                            item["statisticsCurve"] = JsonConvert.SerializeObject(trendResult);
                            enrichedCount++;
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"获取商品趋势数据异常: {ex.Message}", _platformName);
                    }
                }

                FileUtils.LogRpa($"商品趋势数据补充完成：{enrichedCount}/{productList.Count}", _platformName);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"补充商品趋势数据异常: {ex.Message}", _platformName);
            }
        }

        /// <summary>
        /// 从商品JToken中获取字段值
        /// </summary>
        private string GetProductFieldValue(JToken item, string fieldName)
        {
            var field = item[fieldName];
            if (field == null) return null;

            // 如果是对象且有 value 属性
            if (field is JObject obj && obj["value"] != null)
            {
                return obj["value"]?.ToString();
            }

            return field.ToString();
        }

        /// <summary>
        /// 处理巨量授权过期（API 返回"服务器错误"时触发）。
        /// 执行流程：修改本地 Cookie 过期时间 → 更新内存授权状态 → 同步服务器 → 通知前端。
        /// 与旧版 JuliangReachData 和 JuliangForm.loginExpire 逻辑保持一致。
        /// </summary>
        /// <param name="anchorInfo">主播信息（可能为 null）</param>
        /// <param name="apiName">触发过期的 API 名称（用于日志）</param>
        /// <param name="responseText">API 错误响应原文（用于日志）</param>
        private void HandleAuthExpired(AnchorInfo anchorInfo, string apiName, string responseText)
        {
            if (anchorInfo == null) return;

            try
            {
                // 1. 修改本地罗盘 Cookie 过期时间（设为 62 天前，使其失效）
                string cookiePath = JuliangUtils.getCookiePath(anchorInfo.SecUid);
                var localCookies = new CookiePersistenceHelper(cookiePath).LoadCefCookiesFromLocal(false);
                var luopanDtCookie = localCookies
                    ?.Where(c => c.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal))
                    ?.ToList();
                if (luopanDtCookie != null && luopanDtCookie.Count > 0)
                {
                    var newExpiryTime = DateTime.Now.AddDays(-62);
                    new CookiePersistenceHelper(cookiePath)
                        .ModifyLocalCookieExpiry("COMPASS_LUOPAN_DT", "jinritemai.com", newExpiryTime);
                    FileUtils.LogRpa($"罗盘 Cookie 过期时间已修改（62天前），主播【{anchorInfo.AnchorName}】", _platformName);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"修改罗盘 Cookie 过期时间异常: {ex.Message}", _platformName);
            }

            // 2. 检查当前状态，如果已经是非授权状态则跳过
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth) return;

            // 3. 更新内存授权状态并同步服务端
            var t = JuliangAuthStatusEnum.authExpires;
            if (responseText.Contains("\"msg\":\"达人没有直播间权限\",\"st\":625"))
            {
                t = JuliangAuthStatusEnum.accountMismatched;
            }
            AnchorBll.UpdateJuliangAuthStatus(anchorInfo, t);
            JuliangUtils.recountJuliangProperty();

            // 5. 通知前端授权过期
            try
            {
                var notice = new FrontNotice();
                var data = new Dictionary<string, object>
                {
                    ["code"] = 0,
                    ["status"] = 200,
                    ["action"] = "juliangAuthExpires",
                    ["data"] = JsonConvert.SerializeObject(anchorInfo)
                };
                notice.NoticeJs(JsonConvert.SerializeObject(data));
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知前端巨量授权过期异常: {ex.Message}", _platformName);
            }

            FileUtils.LogRpa($"{apiName} API 返回服务器错误（罗盘Token失效），已触发授权过期处理，主播【{anchorInfo.AnchorName}】，响应: {responseText}", _platformName);
        }
    }
}
