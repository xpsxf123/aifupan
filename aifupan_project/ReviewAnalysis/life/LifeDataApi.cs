using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using douyin.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.life
{
    public class LifeDataApi
    {
        private static readonly Dictionary<string, string> DefaultHeaders = new Dictionary<string, string>
        {
            ["host"] = "life.douyin.com",
            ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
            ["Accept"] = "*/*",
            ["sec-ch-ua-platform"] = "\"Windows\"",
            ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
            ["sec-ch-ua-mobile"] = "?0",
            ["x-use-prefetch"] = "1",
            ["agw-js-conv"] = "str",
            ["rpc-persist-life-merchant-switch-role"] = "1",
            ["x-tt-trace-log"] = "01",
            ["x-secsdk-csrf-token"] = "",
            ["content-type"] = "application/json",
            ["sec-gpc"] = "1",
            ["sec-fetch-site"] = "same-origin",
            ["sec-fetch-mode"] = "cors",
            ["sec-fetch-dest"] = "empty",
            ["accept-language"] = "zh-CN,zh;q=0.8",
            ["priority"] = "u=1, i"
        };

        public static async Task<Dictionary<string, string>> GetAccountDetail(Dictionary<string, string> cookies, string groupId = null)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("GetAccountDetail Cookie为空", "来客数据API");
                    return null;
                }

                bool hasSessionId = cookies.ContainsKey("sessionid_ls") || cookies.ContainsKey("sessionid_ss_ls");
                if (!hasSessionId)
                {
                    FileUtils.LogRpa($"GetAccountDetail Cookie中缺少sessionid_ls/sessionid_ss_ls，当前CookieKeys={string.Join(",", cookies.Keys)}", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = !string.IsNullOrEmpty(groupId) 
                        ? $"https://life.douyin.com/p/home?groupid={groupId}" 
                        : "https://life.douyin.com/p/home"
                };

                // 参照 laike.py: 有 groupid 时传 accountId + groupId，没有时只传 cpa + _isPrefetchRequest
                string url;
                if (!string.IsNullOrEmpty(groupId))
                {
                    url = $"https://life.douyin.com/life/gate/v1/account/detail?cpa=true&accountId={groupId}&groupId={groupId}&_isPrefetchRequest=1";
                    FileUtils.LogRpa($"GetAccountDetail 带groupId请求, groupId={groupId}", "来客数据API");
                }
                else
                {
                    url = "https://life.douyin.com/life/gate/v1/account/detail?cpa=true&_isPrefetchRequest=1";
                    FileUtils.LogRpa($"GetAccountDetail 不带groupId请求", "来客数据API");
                }
                FileUtils.LogRpa($"GetAccountDetail 开始请求, sessionid_ls={(cookies.ContainsKey("sessionid_ls") ? cookies["sessionid_ls"].Substring(0, Math.Min(16, cookies["sessionid_ls"].Length)) + "..." : "无")}", "来客数据API");

                var response = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("GetAccountDetail 响应为空（网络异常或被拦截）", "来客数据API");
                    return null;
                }

                var jsonData = JObject.Parse(response);
                var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;

                // 如果第一次失败且错误码为2132000427，尝试带account_id重试
                if (statusCode == 2132000427)
                {
                    FileUtils.LogRpa($"GetAccountDetail 首次请求返回2132000427，尝试通过GetUserDetail获取account_id后重试", "来客数据API");
                    FileUtils.LogRpa($"GetAccountDetail 完整响应: {response}", "来客数据API");

                    var userDetail = await GetUserDetail(cookies).ConfigureAwait(false);
                    if (userDetail != null && userDetail.ContainsKey("account_id") && !string.IsNullOrEmpty(userDetail["account_id"]) && userDetail["account_id"] != "0")
                    {
                        string retryAccountId = userDetail["account_id"];
                        FileUtils.LogRpa($"GetAccountDetail 通过GetUserDetail获取到account_id={retryAccountId}（user_id={(userDetail.ContainsKey("user_id") ? userDetail["user_id"] : "")})，尝试带参数重试", "来客数据API");

                        // 参照 laike.py: 带 groupid 时同时传 accountId 和 groupId 两个参数，referer 也要带 groupid
                        headers["referer"] = $"https://life.douyin.com/p/home?groupid={retryAccountId}";
                        var retryUrl = $"https://life.douyin.com/life/gate/v1/account/detail?cpa=true&accountId={retryAccountId}&groupId={retryAccountId}&_isPrefetchRequest=1";
                        var retryResponse = await SendGetRequest(retryUrl, cookies, headers).ConfigureAwait(false);
                        if (!string.IsNullOrEmpty(retryResponse))
                        {
                            jsonData = JObject.Parse(retryResponse);
                            statusCode = jsonData["status_code"]?.Value<int>() ?? -1;
                            FileUtils.LogRpa($"GetAccountDetail 重试结果: status_code={statusCode}", "来客数据API");
                            if (statusCode != 0)
                            {
                                FileUtils.LogRpa($"GetAccountDetail 重试仍失败: {retryResponse}", "来客数据API");
                                return null;
                            }
                        }
                        else
                        {
                            FileUtils.LogRpa("GetAccountDetail 重试响应为空", "来客数据API");
                            return null;
                        }
                    }
                    else
                    {
                        FileUtils.LogRpa($"GetAccountDetail GetUserDetail也失败，无法获取account_id", "来客数据API");
                        return null;
                    }
                }
                else if (statusCode != 0)
                {
                    var statusMsg = jsonData["status_msg"]?.ToString();
                    FileUtils.LogRpa($"GetAccountDetail 业务错误: status_code={statusCode}, status_msg={statusMsg}", "来客数据API");
                    FileUtils.LogRpa($"GetAccountDetail 完整响应: {response}", "来客数据API");
                    return null;
                }

                if (jsonData["data"] == null)
                {
                    FileUtils.LogRpa("GetAccountDetail data节点为null", "来客数据API");
                    return null;
                }

                var detail = jsonData["data"]["detail"] as JObject;

                // 参照 laike.py L133: 'account_id': data.get("account_id"),  # 来客id = groupid
                // Python main L683: groupid = laike_info.get("account_id")
                // Python 明确用 data.account_id 作为 groupid，不读 data.group_id，此处与 Python 保持一致
                string accountId = jsonData["data"]?["account_id"]?.ToString();
                string respGroupId = accountId;

                var result = new Dictionary<string, string>
                {
                    ["account_id"] = accountId,
                    ["account_name"] = jsonData["data"]?["account_name"]?.ToString(),
                    ["app_key"] = jsonData["data"]?["app_key"]?.ToString(),
                    ["aweme_user_id"] = detail?["aweme_user_id"]?.ToString(),
                    ["life_account_id"] = detail?["life_account_id"]?.ToString(),
                    ["owner_user_id"] = detail?["owner_user_id"]?.ToString(),
                    ["group_id"] = respGroupId,
                    ["current_live_room_id"] = detail?["current_live_room_id"]?.ToString()
                };

                FileUtils.LogRpa($"GetAccountDetail 成功: group_id={result["group_id"]}, life_account_id={result["life_account_id"]}, aweme_user_id={result["aweme_user_id"]}", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取来客账户详情异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        public static async Task<Dictionary<string, string>> GetUserDetail(Dictionary<string, string> cookies)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = "https://life.douyin.com/p/home?is_new_connect=0&is_new_user=0"
                };

                var url = "https://life.douyin.com/life/gate/v1/user/detail/?detail=true&accountId";
                FileUtils.LogRpa($"GetUserDetail 开始请求", "来客数据API");
                var response = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("GetUserDetail 响应为空", "来客数据API");
                    return null;
                }

                FileUtils.LogRpa($"GetUserDetail 响应: {response}", "来客数据API");
                var jsonData = JObject.Parse(response);

                // 响应结构: {"data":{"ag_id":61,"user_id":xxx}, "detail":{"account_id":0,...}}
                var dataNode = jsonData["data"];
                var detailNode = jsonData["detail"];

                // account_id 优先从 detail 取，如果为0或空则从 data.user_id 取
                string accountId = detailNode?["account_id"]?.ToString();
                if (string.IsNullOrEmpty(accountId) || accountId == "0")
                {
                    accountId = dataNode?["user_id"]?.ToString();
                }

                var result = new Dictionary<string, string>
                {
                    ["account_id"] = accountId,
                    ["avatar"] = detailNode?["avatar"]?.ToString(),
                    ["email"] = detailNode?["email"]?.ToString(),
                    ["mobile"] = detailNode?["mobile"]?.ToString(),
                    ["mobile_id"] = detailNode?["mobile_id"]?.ToString(),
                    ["nick_name"] = detailNode?["nick_name"]?.ToString(),
                    ["user_id"] = (dataNode?["user_id"] ?? detailNode?["user_id"])?.ToString(),
                    ["ag_id"] = dataNode?["ag_id"]?.ToString()
                };

                FileUtils.LogRpa($"GetUserDetail 成功: account_id={result["account_id"]}, nick_name={result["nick_name"]}", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取来客用户详情异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 获取 encrypted_token
        /// 参照 laike.py get_encrypted_token：
        ///   - URL参数：aweme_id, root_life_account_id, life_biz_view_id=22, life_account_biz_ids=""
        ///   - referer：https://life.douyin.com/p/home?groupid={account_id}
        /// </summary>
        /// <param name="accountId">来客主页ID（account_id，非group_id）</param>
        /// <param name="awemeUserId">抖音UID</param>
        /// <param name="lifeAccountId">来客账户ID</param>
        public static async Task<string> GetEncryptedToken(string accountId, string awemeUserId, string lifeAccountId, Dictionary<string, string> cookies)
        {
            try
            {
                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://life.douyin.com/p/home?groupid={accountId}"
                };

                var url = "https://life.douyin.com/life/gate/v2/account/encrypt_bc_accounts/";
                var queryParams = $"?aweme_id={awemeUserId}&root_life_account_id={lifeAccountId}&life_biz_view_id=22&life_account_biz_ids=";

                var response = await SendGetRequest(url + queryParams, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa($"GetEncryptedToken 响应为空, url={url + queryParams}", "来客数据API");
                    return null;
                }

                // 记录响应前200字符方便排查
                var preview = response.Length > 200 ? response.Substring(0, 200) : response;
                FileUtils.LogRpa($"GetEncryptedToken 响应: {preview}", "来客数据API");

                var jsonData = JObject.Parse(response);
                var encryptedToken = jsonData["data"]?["encrypted_token"]?.ToString();

                if (string.IsNullOrEmpty(encryptedToken))
                {
                    FileUtils.LogRpa($"GetEncryptedToken encrypted_token为空, 完整响应: {response}", "来客数据API");
                }

                return encryptedToken;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取encrypted_token异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        public static async Task<Dictionary<string, string>> GetLiveToken(string encryptedToken)
        {
            try
            {
                var headers = new Dictionary<string, string>
                {
                    ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["upgrade-insecure-requests"] = "1",
                    ["sec-fetch-site"] = "same-site",
                    ["sec-fetch-mode"] = "navigate",
                    ["sec-fetch-user"] = "?1",
                    ["sec-fetch-dest"] = "document",
                    ["referer"] = "https://life.douyin.com/",
                    ["accept-language"] = "zh-CN,zh;q=0.9",
                    ["priority"] = "u=0, i"
                };

                var url = $"https://eos.douyin.com/data/life/live/jump/?btoken={Uri.EscapeDataString(encryptedToken)}&enter_method=life_home_page_v2";

                using (var handler = new HttpClientHandler
                {
                    AllowAutoRedirect = false
                })
                using (var client = new HttpClient(handler))
                {
                    foreach (var header in headers)
                    {
                        var keyLower = header.Key.ToLower();
                        if (keyLower != "host")
                        {
                            client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                        }
                    }

                    var response = await client.GetAsync(url).ConfigureAwait(false);
                    var respCookies = response.Headers;

                    var result = new Dictionary<string, string>();
                    if (response.Headers.Contains("Set-Cookie"))
                    {
                        foreach (var cookie in response.Headers.GetValues("Set-Cookie"))
                        {
                            FileUtils.LogRpa($"GetLiveToken Set-Cookie原始值: {cookie.Substring(0, Math.Min(100, cookie.Length))}", "来客数据API");

                            // 跳过 Max-Age=0 的删除指令（服务端用于清除旧cookie，不应覆盖前面设置的有效值）
                            if (cookie.IndexOf("Max-Age=0", StringComparison.OrdinalIgnoreCase) >= 0)
                            {
                                continue;
                            }

                            var parts = cookie.Split(';');
                            if (parts.Length > 0)
                            {
                                var nameValue = parts[0].Split(new[] { '=' }, 2);
                                if (nameValue.Length == 2)
                                {
                                    var cookieName = nameValue[0].Trim();
                                    var cookieValue = nameValue[1].Trim();
                                    // 只保存非空值，避免过期cookie（expires=过去时间）覆盖有效值
                                    if (!string.IsNullOrEmpty(cookieValue))
                                    {
                                        result[cookieName] = cookieValue;
                                    }
                                }
                            }
                        }
                    }

                    if (result.Count == 0)
                    {
                        FileUtils.LogRpa($"GetLiveToken 响应中无Set-Cookie头或无有效Cookie，HTTP状态码={(int)response.StatusCode}（可能encrypted_token已过期）", "来客数据API");
                    }
                    else if (!result.ContainsKey("be-token"))
                    {
                        FileUtils.LogRpa($"GetLiveToken 响应Cookie中无be-token，返回的Keys={string.Join(",", result.Keys)}", "来客数据API");
                    }

                    return result;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取直播Token异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 获取 be-token（封装 GetEncryptedToken + GetLiveToken 两步流程）
        /// </summary>
        /// <param name="accountInfo">账户信息（需包含 group_id / aweme_user_id / life_account_id）</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>be-token 字符串，失败返回 null</returns>
        public static async Task<string> GetBeTokenAsync(JObject accountInfo, Dictionary<string, string> cookies)
        {
            try
            {
                if (accountInfo == null)
                {
                    FileUtils.LogRpa("GetBeTokenAsync accountInfo为空", "来客数据API");
                    return null;
                }

                var groupId = accountInfo["group_id"]?.ToString();
                var accountId = accountInfo["account_id"]?.ToString();
                var awemeUserId = accountInfo["aweme_user_id"]?.ToString();
                var lifeAccountId = accountInfo["life_account_id"]?.ToString();

                if (string.IsNullOrEmpty(accountId) || string.IsNullOrEmpty(awemeUserId) || string.IsNullOrEmpty(lifeAccountId))
                {
                    FileUtils.LogRpa($"GetBeTokenAsync accountInfo缺少必要字段: account_id={accountId}, aweme_user_id={awemeUserId}, life_account_id={lifeAccountId}", "来客数据API");
                    return null;
                }

                FileUtils.LogRpa($"GetBeTokenAsync 开始获取encrypted_token, account_id={accountId}, group_id={groupId}, aweme_user_id={awemeUserId}, life_account_id={lifeAccountId}", "来客数据API");

                // ⚠ 参照 laike.py：get_encrypted_token 的第一个参数是 account_id（来客主页ID），不是 group_id
                //    laike.py: get_encrypted_token(groupid=account_id, aweme_user_id, root_life_account_id)
                var encryptedToken = await GetEncryptedToken(accountId, awemeUserId, lifeAccountId, cookies).ConfigureAwait(false);
                if (string.IsNullOrEmpty(encryptedToken))
                {
                    FileUtils.LogRpa("GetBeTokenAsync encryptedToken为空（GetEncryptedToken失败，Cookie可能在服务端已失效）", "来客数据API");
                    return null;
                }

                FileUtils.LogRpa($"GetBeTokenAsync encrypted_token获取成功(长度={encryptedToken.Length})，开始获取be-token", "来客数据API");

                var tokenResult = await GetLiveToken(encryptedToken).ConfigureAwait(false);
                if (tokenResult == null)
                {
                    FileUtils.LogRpa("GetBeTokenAsync GetLiveToken返回null（eos.douyin.com请求失败）", "来客数据API");
                    return null;
                }
                if (!tokenResult.ContainsKey("be-token") || string.IsNullOrEmpty(tokenResult["be-token"]))
                {
                    FileUtils.LogRpa($"GetBeTokenAsync be-token为空或不存在，返回的CookieKeys={string.Join(",", tokenResult.Keys)}", "来客数据API");
                    return null;
                }

                FileUtils.LogRpa($"GetBeTokenAsync be-token获取成功(长度={tokenResult["be-token"].Length})", "来客数据API");
                return tokenResult["be-token"];
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取be-token异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        public static async Task<Dictionary<string, string>> LiveScreenKeyIndex(string beToken, string roomId)
        {
            try
            {
                var headers = new Dictionary<string, string>
                {
                    ["host"] = "eos.douyin.com",
                    ["accept"] = "application/json, text/plain, */*",
                    ["accept-language"] = "zh-CN,zh;q=0.9",
                    ["content-type"] = "application/json",
                    ["origin"] = "https://eos.douyin.com",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://eos.douyin.com/dp/liveScreen?room_id={roomId}",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-secsdk-csrf-token"] = "",
                    ["x-tt-trace-log"] = "01"
                };

                var cookies = new Dictionary<string, string>
                {
                    ["be-token"] = beToken,
                    ["be-token-scene"] = "1"
                };

                var url = "https://eos.douyin.com/life/api/live_screen/v5/key_index";
                var data = JsonConvert.SerializeObject(new { room_id = roomId }, Formatting.None);

                FileUtils.LogRpa($"LiveScreenKeyIndex 开始请求, roomId={roomId}, be-token长度={beToken?.Length ?? 0}", "来客数据API");

                var response = await SendPostRequest(url, data, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("LiveScreenKeyIndex 响应为空（be-token可能已过期）", "来客数据API");
                    return null;
                }

                var jsonData = JObject.Parse(response);

                // 检查业务状态码
                var apiCode = jsonData["code"]?.Value<int>();
                var apiMsg = jsonData["message"]?.ToString();
                if (apiCode != null && apiCode != 0)
                {
                    FileUtils.LogRpa($"LiveScreenKeyIndex 业务错误: code={apiCode}, message={apiMsg}（be-token可能已失效）", "来客数据API");
                    return null;
                }

                var dataObj = jsonData["data"] as JObject;

                if (dataObj == null)
                {
                    FileUtils.LogRpa($"LiveScreenKeyIndex data节点为null, 响应预览={response.Substring(0, Math.Min(200, response.Length))}", "来客数据API");
                    return null;
                }

                var result = new Dictionary<string, string>();

                foreach (var item in dataObj.Properties())
                {
                    var key = item.Name;                          // 原始 PascalCase 英文字段名（作为唯一 key）
                    var value = item.Value["value"]?.ToString();
                    // var name = item.Value["name"]?.ToString(); // 接口返回中文名，仅参考

                    result[key] = value;
                }

                FileUtils.LogRpa($"LiveScreenKeyIndex 解析完成，共 {result.Count} 个指标, roomId={roomId}", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取直播大屏核心数据异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 直播大屏-商品列表数据（EOS）
        /// 直接使用 be-token 作为 Cookie 请求商品列表
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="beToken">已获取的 be-token</param>
        /// <returns>商品列表（每个商品为字段->值的字典，已知字段key转为中文名），失败返回null</returns>
        public static async Task<List<Dictionary<string, object>>> LiveScreenProductList(string roomId, string beToken)
        {
            try
            {
                if (string.IsNullOrEmpty(beToken))
                {
                    FileUtils.LogRpa("LiveScreenProductList beToken为空", "来客数据API");
                    return null;
                }

                // 使用 be-token 组装EOS大屏专用 cookies
                var eosCookies = new Dictionary<string, string>
                {
                    ["be-token"] = beToken,
                    ["be-token-scene"] = "1"
                };

                // 字段中文名对照表
                var indexItem = new Dictionary<string, string>
                {
                    ["product_id"] = "商品 ID",
                    ["product_image"] = "商品图片",
                    ["product_name"] = "商品名称",
                    ["product_tag"] = "商品标签",
                    ["product_type"] = "商品类型",
                    ["product_url"] = "商品链接",
                    ["price"] = "商品售价",
                    ["seckill_price"] = "秒杀价",
                    ["pay_order_gmv_all"] = "支付金额",
                    ["pay_order_cnt_all"] = "支付订单",
                    ["pay_uv_all"] = "支付成功用户数",
                    ["goods_show_cnt"] = "商品曝光次数",
                    ["goods_click_cnt"] = "商品点击次数",
                    ["project_detail_show_pv_all"] = "商品详情页访问量",
                    ["groupon_confirm_show_pv_all"] = "提单页访问量",
                    ["groupon_confirm_ratio"] = "商品提单率",
                    ["goods_cvr"] = "商品转化率",
                    ["goods_show_pay_pv_ratio"] = "商品曝光 - 成交转化率",
                    ["refund_order_cnt_all"] = "退款订单数",
                    ["refund_order_ratio"] = "退款率",
                    ["sold_start_time"] = "上架时间",
                    ["stock"] = "剩余库存",
                    ["first_category"] = "商品一级品类",
                    ["second_category"] = "商品二级品类",
                    ["third_category"] = "商品三级品类"
                };

                var headers = new Dictionary<string, string>
                {
                    ["host"] = "eos.douyin.com",
                    ["accept"] = "application/json, text/plain, */*",
                    ["accept-language"] = "zh-CN,zh;q=0.9",
                    ["content-type"] = "application/json",
                    ["origin"] = "https://eos.douyin.com",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://eos.douyin.com/dp/liveScreen?room_id={roomId}",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-secsdk-csrf-token"] = "",
                    ["x-tt-trace-log"] = "01"
                };

                var url = "https://eos.douyin.com/life/api/live_screen/v5/product_list";
                var data = JsonConvert.SerializeObject(new { minute = 0, room_id = roomId }, Formatting.None);

                FileUtils.LogRpa($"LiveScreenProductList 开始请求, roomId={roomId}, be-token长度={beToken?.Length ?? 0}", "来客数据API");

                // 3. 使用 eosCookies（be-token）请求商品列表
                var response = await SendPostRequest(url, data, eosCookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("LiveScreenProductList 返回空响应", "来客数据API");
                    return null;
                }

                var jsonData = JObject.Parse(response);
                var dataArray = jsonData["data"] as JArray;
                if (dataArray == null)
                {
                    FileUtils.LogRpa("LiveScreenProductList data为空", "来客数据API");
                    return null;
                }

                var result = new List<Dictionary<string, object>>();
                foreach (var good in dataArray.OfType<JObject>())
                {
                    var item = new Dictionary<string, object>();
                    foreach (var prop in good.Properties())
                    {
                        var key = prop.Name;
                        object value;
                        if (prop.Value is JValue jv)
                            value = jv.Value;
                        else
                            value = prop.Value; // 对象/数组保留原始 JToken

                        var outKey = indexItem.ContainsKey(key) ? indexItem[key] : key;
                        item[outKey] = value;
                    }
                    result.Add(item);
                }

                FileUtils.LogRpa($"LiveScreenProductList 解析完成，共{result.Count}个商品", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"LiveScreenProductList 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 线索大屏-商品列表数据（life.douyin.com/clue）
        /// </summary>
        /// <param name="rootLifeAccountId">根来客账号ID（params.root_life_account_id）</param>
        /// <param name="roomId">直播间ID（data.roomID）</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>商品列表（字段key转为中文名），失败返回null</returns>
        /// <summary>
        /// 线索版 - 直播大屏 - 商品列表
        /// 参考 01授权拿到所有主播列表--新.py 的 clue_live_screen_product_list 方法
        /// POST https://life.douyin.com/clue/bff/pc/analysis/live-screen/product-list
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <param name="roomId">直播间ID</param>
        /// <returns>商品列表，失败返回null</returns>
        public static async Task<List<Dictionary<string, object>>> LiveScreenProductListClue(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId, string roomId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("LiveScreenProductListClue Cookie为空", "来客数据API");
                    return null;
                }

                // 字段中文名对照表
                var indexItem = new Dictionary<string, string>
                {
                    ["productID"] = "商品id",
                    ["createTime"] = "商品创建时间",
                    ["updateTime"] = "商品更新时间",
                    ["price"] = "价格",
                    ["productName"] = "商品名称",
                    ["coverURL"] = "商品图片",
                    ["project_detail_show_uv_all"] = "详情页曝光人数",
                    ["live_life_pay_order_uv_all"] = "订单人数",
                    ["clue_uv"] = "填手机号数",
                    ["product_show_uv_all"] = "曝光人数",
                    ["live_life_product_show_count_all"] = "曝光次数",
                    ["live_life_pay_order_gmv_all"] = "GMV",
                    ["live_life_product_click_count_all"] = "点击次数",
                    ["project_detail_show_pv_all"] = "详情页曝光次数",
                    ["product_click_uv_all"] = "点击人数",
                    ["clue_count"] = "线索留资次数",
                    ["live_life_pay_order_count_all"] = "订单数"
                };

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_l",
                    ["accept"] = "application/json,*/*;q=0.8",
                    ["accept-language"] = "zh-CN,zh;q=0.8",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["content-type"] = "application/json;charset=UTF-8",
                    ["origin"] = "https://life.douyin.com",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/liteapp/leads_analysis/live-screen?groupid={groupId}&room_id={roomId}&show_aside=0&show_header=0",
                    ["rpc-persist-life-biz-view-id"] = "0",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["rpc-persist-life-platform"] = "pc",
                    ["rpc-persist-lite-app-id"] = "100258",
                    ["rpc-persist-terminal-type"] = "1",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-gpc"] = "1",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-client-tz"] = "Asia/Shanghai",
                    ["x-edition"] = "life",
                    ["x-secsdk-csrf-token"] = "",
                    ["x-tt-trace-log"] = "01"
                };

                // params 拼接到 URL 查询字符串
                string url = $"https://life.douyin.com/clue/bff/pc/analysis/live-screen/product-list?root_life_account_id={rootLifeAccountId}";
                // data（紧凑 JSON，等价于 separators=(',', ':')）
                string body = JsonConvert.SerializeObject(new { roomID = roomId }, Formatting.None);

                string response = await SendPostRequest(url, body, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("LiveScreenProductListClue 返回空响应", "来客数据API");
                    return null;
                }

                var json = JObject.Parse(response);
                int statusCode = json["status_code"]?.Value<int>() ?? -1;
                if (statusCode != 0)
                {
                    string statusMsg = json["status_msg"]?.ToString() ?? "";
                    // 业务失败时打完整响应体（方便定位权限/参数/风控问题）
                    string respPreview = response.Length > 500 ? response.Substring(0, 500) + "..." : response;
                    FileUtils.LogRpa($"LiveScreenProductListClue 业务失败 status_code={statusCode}, status_msg=[{statusMsg}], roomId={roomId}, groupId={groupId}, lifeAccountId={rootLifeAccountId}, response={respPreview}", "来客数据API");
                    return null;
                }

                var products = json["data"]?["products"] as JArray;
                if (products == null || products.Count == 0)
                {
                    FileUtils.LogRpa("LiveScreenProductListClue products为空", "来客数据API");
                    return new List<Dictionary<string, object>>();
                }

                var result = new List<Dictionary<string, object>>();
                foreach (var product in products.OfType<JObject>())
                {
                    var metrics = product["stats"]?["metrics"] as JObject;

                    // 组装原始字段值（含特殊处理）
                    var rawItem = new Dictionary<string, object>
                    {
                        ["productID"] = product["productID"]?.ToString(),
                        ["createTime"] = ConvertTimestamp(product["createTime"]?.Value<long>()),
                        ["updateTime"] = ConvertTimestamp(product["updateTime"]?.Value<long>()),
                        ["price"] = FormatPrice(product["price"]?.Value<long>()),
                        ["productName"] = product["productName"]?.ToString(),
                        ["coverURL"] = product["coverURL"]?.ToString(),
                        ["project_detail_show_uv_all"] = metrics?["project_detail_show_uv_all"]?.ToString(),
                        ["live_life_pay_order_uv_all"] = metrics?["live_life_pay_order_uv_all"]?.ToString(),
                        ["clue_uv"] = metrics?["clue_uv"]?.ToString(),
                        ["product_show_uv_all"] = metrics?["product_show_uv_all"]?.ToString(),
                        ["live_life_product_show_count_all"] = metrics?["live_life_product_show_count_all"]?.ToString(),
                        ["live_life_pay_order_gmv_all"] = metrics?["live_life_pay_order_gmv_all"]?.ToString(),
                        ["live_life_product_click_count_all"] = metrics?["live_life_product_click_count_all"]?.ToString(),
                        ["project_detail_show_pv_all"] = metrics?["project_detail_show_pv_all"]?.ToString(),
                        ["product_click_uv_all"] = metrics?["product_click_uv_all"]?.ToString(),
                        ["clue_count"] = metrics?["clue_count"]?.ToString(),
                        ["live_life_pay_order_count_all"] = metrics?["live_life_pay_order_count_all"]?.ToString()
                    };

                    // 映射为中文名 key
                    var itemZh = new Dictionary<string, object>();
                    foreach (var kv in rawItem)
                    {
                        if (indexItem.ContainsKey(kv.Key))
                            itemZh[indexItem[kv.Key]] = kv.Value;
                    }
                    result.Add(itemZh);
                }

                FileUtils.LogRpa($"LiveScreenProductListClue 解析完成，共{result.Count}个商品", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"LiveScreenProductListClue 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 毫秒时间戳转日期时间字符串（对应 Python convert_timestamp）
        /// </summary>
        private static string ConvertTimestamp(long? timestampMs)
        {
            if (timestampMs == null || timestampMs <= 0)
                return null;
            // 毫秒 -> 北京时间
            var dt = DateTimeOffset.FromUnixTimeMilliseconds(timestampMs.Value).LocalDateTime;
            return dt.ToString("yyyy-MM-dd HH:mm:ss");
        }

        /// <summary>
        /// 分转元，格式化为 ¥x.xx（对应 Python f"¥{int(price)/100}"）
        /// </summary>
        private static string FormatPrice(long? priceFen)
        {
            if (priceFen == null)
                return null;
            return $"¥{(double)priceFen.Value / 100}";
        }

        /// <summary>
        /// 线索大屏组合数据：LiveScreenOverviewData（核心指标）+ LiveScreenProductListClue（商品列表）
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="groupId">商户组ID</param>
        /// <param name="rootLifeAccountId">根来客账号ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>JObject，包含 keyIndex 和 productList 两个节点；失败返回 null</returns>
        public static async Task<JObject> GetClueScreenDataWithProducts(string roomId, string groupId, string rootLifeAccountId, Dictionary<string, string> cookies)
        {
            FileUtils.LogRpa($"[线索大屏] GetClueScreenDataWithProducts 开始, roomId={roomId}, groupId={groupId}, lifeAccountId={rootLifeAccountId}", "来客数据API");
            var result = new JObject();

            // 1. 线索大屏核心指标
            FileUtils.LogRpa($"[线索大屏] Step1 获取核心指标 LiveScreenOverviewData...", "来客数据API");
            var keyIndex = await LiveScreenOverviewData(roomId, groupId, rootLifeAccountId, cookies).ConfigureAwait(false);
            if (keyIndex != null && keyIndex.Count > 0)
            {
                result["keyIndex"] = JObject.FromObject(keyIndex);
                FileUtils.LogRpa($"[线索大屏] Step1 核心指标获取成功，共 {keyIndex.Count} 项", "来客数据API");
            }
            else
            {
                FileUtils.LogRpa("[线索大屏] Step1 核心指标获取失败或为空", "来客数据API");
            }

            // 2. 线索版商品列表
            FileUtils.LogRpa($"[线索大屏] Step2 获取商品列表 LiveScreenProductListClue...", "来客数据API");
            var products = await LiveScreenProductListClue(cookies, groupId, rootLifeAccountId, roomId).ConfigureAwait(false);
            if (products != null && products.Count > 0)
            {
                result["productList"] = JArray.FromObject(products);
                FileUtils.LogRpa($"[线索大屏] Step2 商品列表获取成功，共 {products.Count} 个商品", "来客数据API");
            }
            else
            {
                FileUtils.LogRpa("[线索大屏] Step2 商品列表获取失败或为空", "来客数据API");
            }

            if (result.Count == 0)
            {
                FileUtils.LogRpa($"[线索大屏] 两个接口都失败，返回null", "来客数据API");
                return null;
            }

            FileUtils.LogRpa($"[线索大屏] 完成: keyIndex={(keyIndex?.Count ?? 0)}项, productList={(products?.Count ?? 0)}个, roomId={roomId}", "来客数据API");
            return result;
        }

        /// <summary>
        /// EOS大屏组合数据：LiveScreenKeyIndex（核心指标）+ LiveScreenProductList（商品列表）
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="accountInfo">账户信息（需包含 group_id / aweme_user_id / life_account_id）</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>JObject，包含 keyIndex 和 productList 两个节点；失败返回 null</returns>
        public static async Task<JObject> GetLiveScreenDataWithProducts(string roomId, JObject accountInfo, Dictionary<string, string> cookies)
        {
            FileUtils.LogRpa($"[EOS大屏] GetLiveScreenDataWithProducts 开始, roomId={roomId}, group_id={accountInfo?["group_id"]}, aweme_user_id={accountInfo?["aweme_user_id"]}", "来客数据API");
            var result = new JObject();

            // 1. 获取 be-token
            FileUtils.LogRpa($"[EOS大屏] Step1 获取 be-token...", "来客数据API");
            string beToken = await GetBeTokenAsync(accountInfo, cookies).ConfigureAwait(false);
            if (string.IsNullOrEmpty(beToken))
            {
                FileUtils.LogRpa("[EOS大屏] Step1 获取be-token失败，中止采集", "来客数据API");
                return null;
            }
            FileUtils.LogRpa($"[EOS大屏] Step1 be-token 获取成功，长度={beToken.Length}", "来客数据API");

            // 2. EOS大屏核心指标
            FileUtils.LogRpa($"[EOS大屏] Step2 获取核心指标 LiveScreenKeyIndex...", "来客数据API");
            var keyIndex = await LiveScreenKeyIndex(beToken, roomId).ConfigureAwait(false);
            if (keyIndex != null && keyIndex.Count > 0)
            {
                result["keyIndex"] = JObject.FromObject(keyIndex);
                FileUtils.LogRpa($"[EOS大屏] Step2 核心指标获取成功，共 {keyIndex.Count} 项", "来客数据API");
            }
            else
            {
                FileUtils.LogRpa("[EOS大屏] Step2 核心指标获取失败或为空", "来客数据API");
            }

            // 3. EOS版商品列表（复用已获取的 be-token）
            FileUtils.LogRpa($"[EOS大屏] Step3 获取商品列表 LiveScreenProductList...", "来客数据API");
            var products = await LiveScreenProductList(roomId, beToken).ConfigureAwait(false);
            if (products != null && products.Count > 0)
            {
                result["productList"] = JArray.FromObject(products);
                FileUtils.LogRpa($"[EOS大屏] Step3 商品列表获取成功，共 {products.Count} 个商品", "来客数据API");
            }
            else
            {
                FileUtils.LogRpa("[EOS大屏] Step3 商品列表获取失败或为空", "来客数据API");
            }

            if (result.Count == 0)
            {
                FileUtils.LogRpa($"[EOS大屏] 两个接口都失败，返回null", "来客数据API");
                return null;
            }

            FileUtils.LogRpa($"[EOS大屏] 完成: keyIndex={(keyIndex?.Count ?? 0)}项, productList={(products?.Count ?? 0)}个, roomId={roomId}", "来客数据API");
            return result;
        }

        /// <summary>
        /// EOS 大屏核心指标中文名对照表（仅供参考说明，不再写入结果字典）
        /// </summary>
        private static Dictionary<string, string> GetIndexNameMap()
        {
            return new Dictionary<string, string>
            {
                ["AcuTotalTd"] = "平均每分钟在线",
                ["AvgOrderGmv"] = "单均价",
                ["ClientAvgWatchDuration"] = "人均观看时长",
                ["ClientLiveShowCntTd"] = "累计曝光次数",
                ["ClientShareCntTd"] = "累计分享次数",
                ["CurrentUserCnt"] = "当前在线人数",
                ["DislikePv"] = "Dislike次数",
                ["FansClubJoinUv"] = "新加粉丝团人数",
                ["FansNum"] = "粉丝数",
                ["GPM"] = "千次观看成交金额",
                ["GmvRefundRatio"] = "GMV实时退款率",
                ["GoodsCtrUv"] = "商品CTR",
                ["GoodsCvrUv"] = "商品转化率",
                ["LiveCommentUcntTd"] = "累计评论人数",
                ["LiveCtr"] = "直播间CTR",
                ["LiveCvr"] = "直播间CVR",
                ["LiveFollowAnchorCnt"] = "累计关注次数",
                ["LiveFollowAnchorUcnt"] = "新增粉丝人数",
                ["LiveServerWatchUcnt"] = "累计观看人数",
                ["OrderCntRefundRatio"] = "订单数实时退款率",
                ["PayGmv"] = "直播间成交金额",
                ["PayOrderCnt"] = "成交订单数",
                ["PayUvAll"] = "成交人数",
                ["PcuTotalTd"] = "单分钟最高在线人数",
                ["ProductClickUvAll"] = "商品点击人数",
                ["ProductFee"] = "入库商品总GMV",
                ["ServerCommentCntTd"] = "累计评论次数",
                ["ServerLikeCntTotal"] = "累计点赞次数",
                ["ServerWatchCntTd"] = "累计看播次数",
                ["UnfollowUv"] = "取关粉丝数",
                ["ViolationPv"] = "违规次数"
            };
        }

        private static async Task<string> SendGetRequest(string url, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler
                {
                    UseCookies = false
                })
                using (var client = new HttpClient(handler))
                {
                    client.Timeout = TimeSpan.FromSeconds(30);

                    if (cookies != null && cookies.Count > 0)
                    {
                        var cookieStr = string.Join("; ", cookies.Select(c => $"{c.Key}={c.Value}"));
                        client.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", cookieStr);
                    }

                    foreach (var header in headers)
                    {
                        var keyLower = header.Key.ToLower();
                        // 不过滤 content-type：抖音API即使对GET请求也要求带 content-type: application/json
                        if (keyLower != "host")
                        {
                            client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                        }
                    }

                    var cookieKeys = cookies != null ? string.Join(",", cookies.Keys) : "(null)";
                    FileUtils.LogRpa($"GET {url} | CookieKeys={cookieKeys}", "来客数据API");

                    var response = await client.GetAsync(url).ConfigureAwait(false);
                    var body = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    FileUtils.LogRpa($"GET响应 status={(int)response.StatusCode}, len={body?.Length ?? 0}", "来客数据API");
                    return body;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GET请求异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        private static async Task<string> SendPostRequest(string url, string jsonData, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler
                {
                    UseCookies = false
                })
                using (var client = new HttpClient(handler))
                {
                    client.Timeout = TimeSpan.FromSeconds(30);

                    if (cookies != null && cookies.Count > 0)
                    {
                        var cookieStr = string.Join("; ", cookies.Select(c => $"{c.Key}={c.Value}"));
                        client.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", cookieStr);
                    }

                    foreach (var header in headers)
                    {
                        var keyLower = header.Key.ToLower();
                        if (keyLower != "content-type" && keyLower != "host")
                        {
                            client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                        }
                    }

                    var cookieKeys = cookies != null ? string.Join(",", cookies.Keys) : "(null)";
                    FileUtils.LogRpa($"POST {url} | CookieKeys={cookieKeys} | body={jsonData}", "来客数据API");

                    var content = new StringContent(jsonData, Encoding.UTF8, "application/json");
                    var response = await client.PostAsync(url, content).ConfigureAwait(false);
                    var body = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    FileUtils.LogRpa($"POST响应 status={(int)response.StatusCode}, len={body?.Length ?? 0}", "来客数据API");
                    return body;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"POST请求异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 来客数据更新接口（参考巨量updateOceanEngine实现）
        /// </summary>
        /// <param name="lifeGatherDataEntity">来客汇总数据实体</param>
        /// <returns></returns>
        public static string updateLifeEngine(LifeGatherDataEntity lifeGatherDataEntity)
        {
            return HttpUtils.SendServerPost(ReplayHttpUtils.BaseUrl + "/words/lifeData/updateLifeEngine", lifeGatherDataEntity);
        }

        /// <summary>
        /// 线索大屏-团购版-直播大屏核心指标数据
        /// </summary>
        /// <param name="roomId">直播间ID</param>
        /// <param name="groupId">商户组ID</param>
        /// <param name="rootLifeAccountId">根来客账号ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>指标字典（key为中文名），失败返回null</returns>
        public static async Task<Dictionary<string, string>> LiveScreenOverviewData(string roomId, string groupId, string rootLifeAccountId, Dictionary<string, string> cookies)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("LiveScreenOverviewData Cookie为空", "来客数据API");
                    return null;
                }

                // 指标项定义：key -> (中文名, 格式类型, 格式规则)
                var indexItem = GetLiveScreenOverviewIndexItem();

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_l",
                    ["accept"] = "application/json,*/*;q=0.8",
                    ["accept-language"] = "zh-CN,zh;q=0.8",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["content-type"] = "application/json;charset=UTF-8",
                    ["origin"] = "https://life.douyin.com",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/liteapp/leads_analysis/live-screen?room_id={roomId}&show_aside=0&show_header=0&enter_from=home_live&groupid={groupId}",
                    ["rpc-persist-life-biz-view-id"] = "0",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["rpc-persist-life-platform"] = "pc",
                    ["rpc-persist-lite-app-id"] = "100258",
                    ["rpc-persist-terminal-type"] = "1",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-gpc"] = "1",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-client-tz"] = "Asia/Shanghai",
                    ["x-edition"] = "life",
                    ["x-tt-trace-log"] = "01"
                };

                string url = $"https://life.douyin.com/clue/bff/pc/analysis/live-screen/overview-data?root_life_account_id={rootLifeAccountId}";
                string body = JsonConvert.SerializeObject(new { roomID = roomId });

                string response = await SendPostRequest(url, body, cookies, headers).ConfigureAwait(false);
                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("LiveScreenOverviewData 返回空响应", "来客数据API");
                    return null;
                }

                var json = JObject.Parse(response);
                int statusCode = json["status_code"]?.Value<int>() ?? -1;
                if (statusCode != 0)
                {
                    string statusMsg = json["status_msg"]?.ToString() ?? "";
                    string respPreview = response.Length > 500 ? response.Substring(0, 500) + "..." : response;
                    FileUtils.LogRpa($"LiveScreenOverviewData 业务失败 status_code={statusCode}, status_msg=[{statusMsg}], roomId={roomId}, groupId={groupId}, lifeAccountId={rootLifeAccountId}, response={respPreview}", "来客数据API");
                    return null;
                }

                var metrics = json["data"]?["statRow"]?["metrics"] as JObject;
                if (metrics == null || metrics.Count == 0)
                {
                    FileUtils.LogRpa("LiveScreenOverviewData metrics为空", "来客数据API");
                    return null;
                }

                var result = new Dictionary<string, string>();
                foreach (var prop in metrics.Properties())
                {
                    if (!indexItem.ContainsKey(prop.Name))
                        continue;

                    var meta = indexItem[prop.Name];
                    string valueStr = prop.Value?.Type == JTokenType.Integer
                        ? prop.Value.Value<long>().ToString()
                        : prop.Value?.Type == JTokenType.Float
                            ? prop.Value.Value<double>().ToString()
                            : prop.Value?.ToString();

                    // 格式化处理
                    if (meta.FormatType == "percent")
                    {
                        if (double.TryParse(valueStr, out double pctVal))
                            valueStr = $"{pctVal * 100:F2}%";
                    }
                    else if (meta.FormatRule == "duration")
                    {
                        if (double.TryParse(valueStr, out double sec))
                            valueStr = FormatSeconds(sec);
                    }

                    result[ToPascalCase(prop.Name)] = valueStr;
                }

                FileUtils.LogRpa($"LiveScreenOverviewData 解析完成，共{result.Count}项指标", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"LiveScreenOverviewData 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 指标元数据
        /// </summary>
        private class IndexItemMeta
        {
            /// <summary>中文说明</summary>
            public string Description { get; set; }
            public string FormatType { get; set; }  // "percent" 或 null
            public string FormatRule { get; set; }  // "duration" 或 null
        }

        /// <summary>
        /// 下划线命名转 PascalCase（首字母大写），如 live_avg_watch_duration_by_room → LiveAvgWatchDurationByRoom
        /// </summary>
        private static string ToPascalCase(string snake)
        {
            if (string.IsNullOrEmpty(snake)) return snake;
            var parts = snake.Split('_');
            var sb = new StringBuilder();
            for (int i = 0; i < parts.Length; i++)
            {
                if (parts[i].Length > 0)
                    sb.Append(char.ToUpper(parts[i][0]) + parts[i].Substring(1));
            }
            return sb.ToString();
        }

        /// <summary>
        /// 线索大屏核心指标定义表
        /// </summary>
        private static Dictionary<string, IndexItemMeta> GetLiveScreenOverviewIndexItem()
        {
            return new Dictionary<string, IndexItemMeta>
            {
                ["live_avg_watch_duration_by_room"] = new IndexItemMeta { Description = "人均观看时长", FormatRule = "duration" },
                ["im_message_conversation_count"] = new IndexItemMeta { Description = "私信人数" },
                ["live_life_icon_click_count_all"] = new IndexItemMeta { Description = "风车房子点击次数" },
                ["live_comment_uv_by_room"] = new IndexItemMeta { Description = "直播间评论人数" },
                ["live_gift_amount"] = new IndexItemMeta { Description = "打赏金额" },
                ["clue_cost"] = new IndexItemMeta { Description = "线索成本" },
                ["live_follow_uv_by_room"] = new IndexItemMeta { Description = "涨粉量" },
                ["live_like_uv_by_room"] = new IndexItemMeta { Description = "直播间点赞人数" },
                ["live_fans_club_join_uv_by_room"] = new IndexItemMeta { Description = "加粉丝团人数" },
                ["live_single_watch_over_1m_count"] = new IndexItemMeta { Description = ">1分钟观看次数" },
                ["live_fans_enter_rate_by_room"] = new IndexItemMeta { Description = "粉丝", FormatType = "percent" },
                ["live_fans_avg_watch_duration"] = new IndexItemMeta { Description = "粉丝停留", FormatRule = "duration" },
                ["live_oto_pay_order_count"] = new IndexItemMeta { Description = "营销订单数" },
                ["message_clue_uv"] = new IndexItemMeta { Description = "私信留资人数" },
                ["live_watch_comment_rate"] = new IndexItemMeta { Description = "评论率", FormatType = "percent" },
                ["live_gift_count"] = new IndexItemMeta { Description = "打赏次数" },
                ["live_show_uv_by_room"] = new IndexItemMeta { Description = "直播间曝光人数" },
                ["live_avg_online_uv_by_room"] = new IndexItemMeta { Description = "平均在线人数" },
                ["clue_convert_rate"] = new IndexItemMeta { Description = "线索转化率", FormatType = "percent" },
                ["live_share_uv_by_room"] = new IndexItemMeta { Description = "直播间分享人数" },
                ["live_watch_share_rate"] = new IndexItemMeta { Description = "分享率", FormatType = "percent" },
                ["live_share_count"] = new IndexItemMeta { Description = "直播间分享次数" },
                ["live_interaction_rate"] = new IndexItemMeta { Description = "直播间互动率", FormatType = "percent" },
                ["uv_realtime"] = new IndexItemMeta { Description = "实时在线人数" },
                ["pay_order_gmv_per1k_watch_uv"] = new IndexItemMeta { Description = "千次观看GMV" },
                ["live_order_clue_uv"] = new IndexItemMeta { Description = "填手机号" },
                ["live_watch_follow_rate"] = new IndexItemMeta { Description = "关注率", FormatType = "percent" },
                ["live_fans_club_join_rate"] = new IndexItemMeta { Description = "加团率", FormatType = "percent" },
                ["form_clue_cost"] = new IndexItemMeta { Description = "表单成本" },
                ["live_fans_watch_rate"] = new IndexItemMeta { Description = "累计观看人数粉丝占比", FormatType = "percent" },
                ["live_like_count"] = new IndexItemMeta { Description = "直播间点赞次数" },
                ["live_life_product_click_rate_all"] = new IndexItemMeta { Description = "商品点击率", FormatType = "percent" },
                ["live_life_icon_click_rate_all"] = new IndexItemMeta { Description = "小房子点击次数点击率", FormatType = "percent" },
                ["clue_uv"] = new IndexItemMeta { Description = "全场景留资人数" },
                ["live_minute_max_watch_uv"] = new IndexItemMeta { Description = "最高在线人数" },
                ["live_enter_rate"] = new IndexItemMeta { Description = "曝光进入率", FormatType = "percent" },
                ["form_clue_uv"] = new IndexItemMeta { Description = "表单提交人数" },
                ["live_watch_count"] = new IndexItemMeta { Description = "直播间观看次数" },
                ["live_life_product_click_count_all"] = new IndexItemMeta { Description = "商品点击次数" },
                ["order_cost"] = new IndexItemMeta { Description = "订单成本" },
                ["live_life_pay_order_uv_all"] = new IndexItemMeta { Description = "订单人数" },
                ["live_comment_count"] = new IndexItemMeta { Description = "直播间评论次数" },
                ["stat_cost"] = new IndexItemMeta { Description = "营销消耗" },
                ["uv_with_preview"] = new IndexItemMeta { Description = "看过" },
                ["live_watch_uv_by_room"] = new IndexItemMeta { Description = "直播间累计观看人数" },
                ["live_life_product_show_count_all"] = new IndexItemMeta { Description = "商品曝光次数" },
                ["live_show_count"] = new IndexItemMeta { Description = "直播间曝光次数" },
                ["pay_order_uv_convert_rate"] = new IndexItemMeta { Description = "订单人数转化率", FormatType = "percent" },
                ["live_interact_uv_by_room"] = new IndexItemMeta { Description = "直播间互动人数" },
                ["live_life_pay_order_count_all"] = new IndexItemMeta { Description = "团购订单数" },
                ["live_interaction_count"] = new IndexItemMeta { Description = "互动次数" },
                ["live_watch_like_rate"] = new IndexItemMeta { Description = "点赞率", FormatType = "percent" },
                ["live_life_pay_order_gmv_all"] = new IndexItemMeta { Description = "直播间成交金额" }
            };
        }

        /// <summary>
        /// 秒数格式化为时长字符串
        /// </summary>
        private static string FormatSeconds(double seconds)
        {
            if (seconds < 60)
                return $"{seconds:F0}秒";
            double minutes = Math.Floor(seconds / 60);
            double remainSec = seconds % 60;
            if (minutes < 60)
                return remainSec > 0 ? $"{minutes:F0}分{remainSec:F0}秒" : $"{minutes:F0}分";
            double hours = Math.Floor(minutes / 60);
            double remainMin = minutes % 60;
            return remainMin > 0 ? $"{hours:F0}时{remainMin:F0}分" : $"{hours:F0}时";
        }

        /// <summary>
        /// 侧边栏接口：判断是否包含"线索经营"菜单
        /// </summary>
        /// <param name="groupId">商户组ID</param>
        /// <param name="rootLifeAccountId">根来客账号ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>存在"线索经营"返回true，否则false</returns>
        public static async Task<bool> HomeMenus(string groupId, string rootLifeAccountId, Dictionary<string, string> cookies)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("HomeMenus Cookie为空", "来客数据API");
                    return false;
                }

                var headers = new Dictionary<string, string>
                {
                    ["host"] = "life.douyin.com",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["x-use-prefetch"] = "1",
                    ["agw-js-conv"] = "str",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["accept"] = "*/*",
                    ["content-type"] = "application/json",
                    ["x-tt-trace-log"] = "01",
                    ["sec-gpc"] = "1",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-dest"] = "empty",
                    ["referer"] = $"https://life.douyin.com/p/home?groupid={groupId}",
                    ["accept-language"] = "zh-CN,zh;q=0.8",
                    ["priority"] = "u=1, i"
                };

                string url = $"https://life.douyin.com/napi/v1/web/home/menus?root_life_account_id={rootLifeAccountId}&_isPrefetchRequest=1";

                string response = await SendGetRequestWithHeaders(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("HomeMenus 返回空响应", "来客数据API");
                    return false;
                }

                bool contains = response.Contains("线索经营");
                FileUtils.LogRpa($"HomeMenus 检查线索经营结果: {contains}", "来客数据API");
                return contains;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"HomeMenus 异常: {ex.Message}", "来客数据API");
                return false;
            }
        }

        /// <summary>
        /// 带自定义Headers的GET请求
        /// </summary>
        private static async Task<string> SendGetRequestWithHeaders(string url, Dictionary<string, string> cookies, Dictionary<string, string> headers)
        {
            try
            {
                using (var handler = new HttpClientHandler
                {
                    UseCookies = false
                })
                using (var client = new HttpClient(handler))
                {
                    client.Timeout = TimeSpan.FromSeconds(30);

                    if (cookies != null && cookies.Count > 0)
                    {
                        var cookieStr = string.Join("; ", cookies.Select(c => $"{c.Key}={c.Value}"));
                        client.DefaultRequestHeaders.TryAddWithoutValidation("Cookie", cookieStr);
                    }

                    if (headers != null)
                    {
                        foreach (var header in headers)
                        {
                            if (header.Key.ToLower() != "content-type" && header.Key.ToLower() != "host")
                            {
                                client.DefaultRequestHeaders.TryAddWithoutValidation(header.Key, header.Value);
                            }
                        }
                    }

                    var response = await client.GetAsync(url).ConfigureAwait(false);
                    return await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"HomeMenus GET请求异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        // =================================================================
        // bc_bind_relationships —— 获取来客商家下所有绑定的抖音号
        // =================================================================

        /// <summary>
        /// 调用 bc_bind_relationships 接口，获取当前来客商家下所有绑定的抖音号列表。
        /// 返回 aweme_user_id 列表（可能为空列表，表示无绑定或接口失败）。
        /// </summary>
        /// <param name="rootLifeAccountId">来客根账户ID</param>
        /// <param name="cookies">Cookie字典</param>
        /// <returns>aweme_user_id 列表，失败时返回 null</returns>
        public static async Task<List<string>> GetBindRelationships(string rootLifeAccountId, Dictionary<string, string> cookies)
        {
            try
            {
                if (string.IsNullOrEmpty(rootLifeAccountId))
                {
                    FileUtils.LogRpa("GetBindRelationships rootLifeAccountId 为空", "来客数据API");
                    return null;
                }

                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("GetBindRelationships Cookie 为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://life.douyin.com/p/merchant/live/list?groupid={rootLifeAccountId}"
                };

                // 分页拉取（has_more=true 时继续拉取下一页）
                var allAwemeUserIds = new List<string>();
                int pageIndex = 1;
                int pageCount = 20;
                bool hasMore = true;

                while (hasMore)
                {
                    var url = $"https://life.douyin.com/life/gate/v2/account/bc_bind_relationships?page_index={pageIndex}&page_count={pageCount}&search_keyword=&root_life_account_id={rootLifeAccountId}";
                    FileUtils.LogRpa($"GetBindRelationships 请求 pageIndex={pageIndex}, url={url}", "来客数据API");

                    var response = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);
                    if (string.IsNullOrEmpty(response))
                    {
                        FileUtils.LogRpa($"GetBindRelationships 响应为空, pageIndex={pageIndex}", "来客数据API");
                        break;
                    }

                    var jsonData = JObject.Parse(response);
                    var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;
                    if (statusCode != 0)
                    {
                        var statusMsg = jsonData["status_msg"]?.ToString();
                        FileUtils.LogRpa($"GetBindRelationships 业务错误: status_code={statusCode}, status_msg={statusMsg}", "来客数据API");
                        break;
                    }

                    var awemeUsers = jsonData["aweme_users"] as JArray;
                    if (awemeUsers != null && awemeUsers.Count > 0)
                    {
                        foreach (var user in awemeUsers)
                        {
                            var awemeUserId = user["aweme_user_id"]?.ToString();
                            if (!string.IsNullOrEmpty(awemeUserId) && !allAwemeUserIds.Contains(awemeUserId))
                            {
                                allAwemeUserIds.Add(awemeUserId);
                            }
                        }
                    }

                    hasMore = jsonData["has_more"]?.Value<bool>() ?? false;
                    var totalCount = jsonData["total_count"]?.Value<int>() ?? 0;
                    FileUtils.LogRpa($"GetBindRelationships pageIndex={pageIndex}, 本页={awemeUsers?.Count ?? 0}, 已累计={allAwemeUserIds.Count}, 总数={totalCount}, hasMore={hasMore}", "来客数据API");

                    pageIndex++;

                    // 安全限制：最多拉取10页
                    if (pageIndex > 10)
                    {
                        FileUtils.LogRpa("GetBindRelationships 已拉取10页，达到安全上限，停止分页", "来客数据API");
                        break;
                    }
                }

                FileUtils.LogRpa($"GetBindRelationships 完成，共获取 {allAwemeUserIds.Count} 个 aweme_user_id", "来客数据API");
                return allAwemeUserIds;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetBindRelationships 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 获取公司列表信息
        /// 参考 01授权拿到所有主播列表--新.py 的 groupAccountList_shop 方法
        /// GET https://life.douyin.com/life/gate/v1/account/groupAccountList/
        /// </summary>
        /// <param name="cookies">认证Cookie（包含 sessionid_ls/sessionid_ss_ls）</param>
        /// <returns>公司列表，每项包含 account_name/account_id/aweme_user_id/life_account_id/owner_user_id，失败返回null</returns>
        public static async Task<List<Dictionary<string, string>>> GroupAccountListShop(Dictionary<string, string> cookies)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("GroupAccountListShop Cookie为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = "https://life.douyin.com/p/login"
                };

                string url = "https://life.douyin.com/life/gate/v1/account/groupAccountList/?page_index=1&page_size=20&name=&support_bd_switch=true";
                FileUtils.LogRpa("GroupAccountListShop 开始请求公司列表", "来客数据API");

                var responseBody = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(responseBody))
                {
                    FileUtils.LogRpa("GroupAccountListShop 响应为空（网络异常或被拦截）", "来客数据API");
                    return null;
                }

                var jsonData = JObject.Parse(responseBody);
                var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;

                if (statusCode != 0)
                {
                    FileUtils.LogRpa($"GroupAccountListShop 接口返回失败: status_code={statusCode}, body={responseBody}", "来客数据API");
                    return null;
                }

                var dataList = jsonData["data"]?["list"] as JArray;
                if (dataList == null || dataList.Count == 0)
                {
                    FileUtils.LogRpa("GroupAccountListShop 公司列表为空", "来客数据API");
                    return new List<Dictionary<string, string>>();
                }

                FileUtils.LogRpa($"GroupAccountListShop 一共{dataList.Count}个商户", "来客数据API");

                var accountList = new List<Dictionary<string, string>>();
                foreach (var item in dataList)
                {
                    var detail = item["detail"] as JObject;
                    if (detail == null) continue;

                    var account = new Dictionary<string, string>
                    {
                        ["account_name"] = detail["life_account_name"]?.Value<string>() ?? "",
                        ["account_id"] = detail["key_account_id"]?.Value<string>() ?? "",       // 来客id = groupid
                        ["aweme_user_id"] = detail["aweme_user_id"]?.Value<string>() ?? "0",    // 抖0就是没有开通来客
                        ["life_account_id"] = detail["life_account_id"]?.Value<string>() ?? "", // 来客账户id
                        ["owner_user_id"] = detail["owner_user_id"]?.Value<string>() ?? ""      // 学习中心的id
                    };

                    FileUtils.LogRpa($"GroupAccountListShop 商户: name={account["account_name"]}, groupid={account["account_id"]}, aweme_user_id={account["aweme_user_id"]}, life_account_id={account["life_account_id"]}", "来客数据API");
                    accountList.Add(account);
                }

                FileUtils.LogRpa($"GroupAccountListShop 完成，共获取 {accountList.Count} 个商户信息", "来客数据API");
                return accountList;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GroupAccountListShop 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 线索版 - 直播大屏 - 拖音主播列表
        /// 参考 01授权拿到所有主播列表--新.py 的 clue_get_aweme_users 方法
        /// POST https://life.douyin.com/clue/bff/pc/analysis/common/account
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid（用于referer）</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <returns>主播列表，每项包含 douyinUID/lifeAccountID/isLiving/douyinNickname/avatarURL/douyinID，失败返回null</returns>
        public static async Task<List<Dictionary<string, string>>> ClueGetAwemeUsers(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("ClueGetAwemeUsers Cookie为空", "来客数据API");
                    return null;
                }
                if (string.IsNullOrEmpty(rootLifeAccountId))
                {
                    FileUtils.LogRpa("ClueGetAwemeUsers rootLifeAccountId为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_l",
                    ["Accept"] = "application/json,*/*;q=0.8",
                    ["accept-language"] = "zh-CN,zh;q=0.8",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["content-type"] = "application/json;charset=UTF-8",
                    ["origin"] = "https://life.douyin.com",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/liteapp/leads_analysis/live-screen?groupid={groupId}",
                    ["rpc-persist-life-biz-view-id"] = "0",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["rpc-persist-life-platform"] = "pc",
                    ["rpc-persist-lite-app-id"] = "100258",
                    ["rpc-persist-terminal-type"] = "1",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-gpc"] = "1",
                    ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-client-tz"] = "Asia/Shanghai",
                    ["x-edition"] = "life",
                    ["x-secsdk-csrf-token"] = "",
                    ["x-tt-trace-log"] = "01"
                };

                // 请求体: {"page":"1","size":"200","keyword":""}
                var requestBody = JsonConvert.SerializeObject(new { page = "1", size = "200", keyword = "" });

                string url = $"https://life.douyin.com/clue/bff/pc/analysis/common/account?root_life_account_id={rootLifeAccountId}";
                FileUtils.LogRpa($"ClueGetAwemeUsers 开始请求线索版主播列表, groupId={groupId}, rootLifeAccountId={rootLifeAccountId}", "来客数据API");

                var responseBody = await SendPostRequest(url, requestBody, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(responseBody))
                {
                    FileUtils.LogRpa("ClueGetAwemeUsers 响应为空", "来客数据API");
                    return null;
                }

                var jsonData = JObject.Parse(responseBody);
                var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;
                if (statusCode != 0)
                {
                    FileUtils.LogRpa($"ClueGetAwemeUsers 接口返回失败: status_code={statusCode}, body={responseBody}", "来客数据API");
                    return null;
                }

                var data = jsonData["data"] as JObject;
                if (data == null)
                {
                    FileUtils.LogRpa("ClueGetAwemeUsers data为空", "来客数据API");
                    return new List<Dictionary<string, string>>();
                }

                var total = data["total"]?.Value<string>() ?? "0";
                FileUtils.LogRpa($"ClueGetAwemeUsers 一共{total}个主播", "来客数据API");

                var relations = data["relations"] as JArray;
                if (relations == null || relations.Count == 0)
                {
                    FileUtils.LogRpa("ClueGetAwemeUsers relations为空", "来客数据API");
                    return new List<Dictionary<string, string>>();
                }

                var allUserList = new List<Dictionary<string, string>>();
                foreach (var user in relations)
                {
                    var item = new Dictionary<string, string>
                    {
                        ["douyinUID"] = user["douyinUID"]?.Value<string>() ?? "",
                        ["lifeAccountID"] = user["lifeAccountID"]?.Value<string>() ?? "",
                        ["isLiving"] = (user["isLiving"]?.Value<bool>() ?? false).ToString(),
                        ["douyinNickname"] = user["douyinNickname"]?.Value<string>() ?? "",
                        ["avatarURL"] = user["avatarURL"]?.Value<string>() ?? "",
                        ["douyinID"] = user["douyinID"]?.Value<string>() ?? ""
                    };
                    FileUtils.LogRpa($"ClueGetAwemeUsers 主播: nickname={item["douyinNickname"]}, douyinID={item["douyinID"]}, douyinUID={item["douyinUID"]}, isLiving={item["isLiving"]}", "来客数据API");
                    allUserList.Add(item);
                }

                FileUtils.LogRpa($"ClueGetAwemeUsers 完成，共获取 {allUserList.Count} 个主播", "来客数据API");
                return allUserList;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"ClueGetAwemeUsers 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 普通版 - 直播管理 - 获取公司下的拖音主播列表（含完整主播信息）
        /// 参考 01授权拿到所有主播列表--新.py 的 get_aweme_users 方法
        /// GET https://life.douyin.com/life/gate/v2/account/bc_bind_relationships
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid（用于referer）</param>
        /// <param name="lifeAccountId">来客账户id（root_life_account_id）</param>
        /// <returns>主播列表，每项包含 aweme_id/nick_name/aweme_user_avatar/aweme_user_id/key_account_id/life_account_id，失败返回null</returns>
        public static async Task<List<Dictionary<string, string>>> GetAwemeUsers(Dictionary<string, string> cookies, string groupId, string lifeAccountId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("GetAwemeUsers Cookie为空", "来客数据API");
                    return null;
                }
                if (string.IsNullOrEmpty(lifeAccountId))
                {
                    FileUtils.LogRpa("GetAwemeUsers lifeAccountId为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_m",
                    ["Accept"] = "application/json, text/plain, */*",
                    ["accept-language"] = "zh-CN,zh;q=0.9",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/merchant/live/list?groupid={groupId}&life_biz_view_id=22&life_account_biz_ids=",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["sec-ch-ua"] = "\"Not:A-Brand\";v=\"99\", \"Microsoft Edge\";v=\"145\", \"Chromium\";v=\"145\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0",
                    ["x-secsdk-csrf-token"] = ""
                };

                string baseUrl = "https://life.douyin.com/life/gate/v2/account/bc_bind_relationships";
                int pageIndex = 1;
                const int pageCount = 20;
                var allUserList = new List<Dictionary<string, string>>();

                FileUtils.LogRpa($"GetAwemeUsers 开始拉取主播列表, groupId={groupId}, lifeAccountId={lifeAccountId}", "来客数据API");

                while (true)
                {
                    string url = $"{baseUrl}?page_index={pageIndex}&page_count={pageCount}&search_keyword=&root_life_account_id={lifeAccountId}&life_biz_view_id=22&life_account_biz_ids=";

                    var responseBody = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);
                    if (string.IsNullOrEmpty(responseBody))
                    {
                        FileUtils.LogRpa($"GetAwemeUsers 第{pageIndex}页响应为空", "来客数据API");
                        break;
                    }

                    var jsonData = JObject.Parse(responseBody);
                    var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;
                    if (statusCode != 0)
                    {
                        FileUtils.LogRpa($"GetAwemeUsers 接口异常: status_code={statusCode}, body={responseBody}", "来客数据API");
                        break;
                    }

                    var hasMore = jsonData["has_more"]?.Value<bool>() ?? false;
                    var totalCount = jsonData["total_count"]?.Value<int>() ?? 0;
                    FileUtils.LogRpa($"GetAwemeUsers 第{pageIndex}页, has_more={hasMore}, total_count={totalCount}", "来客数据API");

                    var awemeUsers = jsonData["aweme_users"] as JArray;
                    if (awemeUsers != null)
                    {
                        foreach (var user in awemeUsers)
                        {
                            var item = new Dictionary<string, string>
                            {
                                ["aweme_id"] = user["aweme_id"]?.Value<string>() ?? "",
                                ["nick_name"] = user["nick_name"]?.Value<string>() ?? "",
                                ["aweme_user_avatar"] = user["aweme_user_avatar"]?.Value<string>() ?? "",
                                ["aweme_user_id"] = user["aweme_user_id"]?.Value<string>() ?? "",
                                ["key_account_id"] = user["key_account_id"]?.Value<string>() ?? "",
                                ["life_account_id"] = user["life_account_id"]?.Value<string>() ?? ""
                            };
                            FileUtils.LogRpa($"GetAwemeUsers 主播: nick_name={item["nick_name"]}, aweme_id={item["aweme_id"]}, aweme_user_id={item["aweme_user_id"]}", "来客数据API");
                            allUserList.Add(item);
                        }
                    }

                    if (!hasMore) break;
                    pageIndex++;

                    // 安全限制：最多拉取20页
                    if (pageIndex > 20)
                    {
                        FileUtils.LogRpa("GetAwemeUsers 已拉取20页，达到安全上限，停止分页", "来客数据API");
                        break;
                    }
                }

                FileUtils.LogRpa($"GetAwemeUsers 拓取完成, 总共获取 {allUserList.Count} 个主播", "来客数据API");
                return allUserList;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetAwemeUsers 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 获取左边栏文字信息
        /// 参考 01授权拿到所有主播列表--新.py 的 get_home_menus_text 方法
        /// GET https://life.douyin.com/napi/v1/web/home/menus
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <returns>侧边栏菜单的完整JSON文本（用于判断是否包含「线索经营」「直播专业版」等关键字），失败返回空字符串</returns>
        public static async Task<string> GetHomeMenusText(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("GetHomeMenusText Cookie为空", "来客数据API");
                    return "";
                }

                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["referer"] = $"https://life.douyin.com/p/home?groupid={groupId}"
                };

                string url = $"https://life.douyin.com/napi/v1/web/home/menus?root_life_account_id={rootLifeAccountId}&_isPrefetchRequest=1";
                FileUtils.LogRpa($"GetHomeMenusText 开始请求, groupId={groupId}, rootLifeAccountId={rootLifeAccountId}", "来客数据API");

                var responseBody = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(responseBody))
                {
                    FileUtils.LogRpa("GetHomeMenusText 响应为空", "来客数据API");
                    return "";
                }

                var jsonData = JObject.Parse(responseBody);
                var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;

                if (statusCode == 0)
                {
                    FileUtils.LogRpa($"GetHomeMenusText 请求成功, 响应长度={responseBody.Length}", "来客数据API");
                    return responseBody;
                }
                else
                {
                    FileUtils.LogRpa($"GetHomeMenusText 接口返回失败: status_code={statusCode}", "来客数据API");
                    return "";
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetHomeMenusText 异常: {ex.Message}", "来客数据API");
                return "";
            }
        }

        /// <summary>
        /// 判断是否为线索版本
        /// 参考 01授权拿到所有主播列表--新.py 的 clue_pc_user_info 方法
        /// GET https://life.douyin.com/clue/bff/pc/user/info
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <returns>true=已开通线索版, false=未开通或请求失败</returns>
        public static async Task<bool> CluePcUserInfo(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("CluePcUserInfo Cookie为空", "来客数据API");
                    return false;
                }

                var headers = new Dictionary<string, string>(DefaultHeaders)
                {
                    ["Accept"] = "application/json,*/*;q=0.8",
                    ["x-client-tz"] = "Asia/Shanghai",
                    ["ac-tag"] = "smb_s",
                    ["rpc-persist-lite-app-id"] = "100258",
                    ["rpc-persist-life-merchant-role"] = "69247121",
                    ["rpc-persist-life-platform"] = "pc",
                    ["x-edition"] = "life",
                    ["referer"] = $"https://life.douyin.com/p/liteapp/leads_analysis/home?groupid={groupId}"
                };

                string url = $"https://life.douyin.com/clue/bff/pc/user/info?root_life_account_id={rootLifeAccountId}";
                FileUtils.LogRpa($"CluePcUserInfo 开始请求, groupId={groupId}, rootLifeAccountId={rootLifeAccountId}", "来客数据API");

                var responseBody = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(responseBody))
                {
                    FileUtils.LogRpa("CluePcUserInfo 响应为空", "来客数据API");
                    return false;
                }

                var jsonData = JObject.Parse(responseBody);
                var statusCode = jsonData["status_code"]?.Value<int>() ?? -1;
                var statusMsg = jsonData["status_msg"]?.Value<string>() ?? "";

                if (statusCode == 0 && statusMsg == "")
                {
                    // 已开通线索版
                    var rootName = jsonData["data"]?["rootLifeAccountName"]?.Value<string>() ?? "";
                    FileUtils.LogRpa($"CluePcUserInfo 已开通线索版, rootLifeAccountName={rootName}", "来客数据API");
                    return true;
                }
                else
                {
                    // 未开通线索版，典型响应: {"status_code":201008,"status_msg":"方案未初始化",...}
                    FileUtils.LogRpa($"CluePcUserInfo 未开通线索版: status_code={statusCode}, status_msg={statusMsg}", "来客数据API");
                    return false;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"CluePcUserInfo 异常: {ex.Message}", "来客数据API");
                return false;
            }
        }

        /// <summary>
        /// 直播大屏 - 获取直播间信息
        /// 参考 01授权拿到所有主播列表--新.py 的 live_screen_room_info 方法
        /// POST https://eos.douyin.com/life/api/live_screen/v5/room_info
        /// </summary>
        /// <param name="cookies">认证Cookie（包含 be-token 或 session cookie）</param>
        /// <param name="roomId">直播间ID</param>
        /// <returns>直播间信息字典，失败返回null</returns>
        public static async Task<Dictionary<string, object>> LiveScreenRoomInfo(Dictionary<string, string> cookies, string roomId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("LiveScreenRoomInfo Cookie为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>
                {
                    ["host"] = "eos.douyin.com",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["x-tt-trace-log"] = "01",
                    ["x-secsdk-csrf-token"] = "",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["content-type"] = "application/json",
                    ["Accept"] = "application/json, text/plain, */*",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["origin"] = "https://eos.douyin.com",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-dest"] = "empty",
                    ["referer"] = $"https://eos.douyin.com/dp/liveScreen?room_id={roomId}",
                    ["accept-language"] = "zh-CN,zh;q=0.9"
                };

                // 请求体: {"room_id":"xxx"}
                var requestBody = JsonConvert.SerializeObject(new { room_id = roomId });

                string url = "https://eos.douyin.com/life/api/live_screen/v5/room_info";
                FileUtils.LogRpa($"LiveScreenRoomInfo 请求, roomId={roomId}", "来客数据API");

                var responseBody = await SendPostRequest(url, requestBody, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(responseBody))
                {
                    FileUtils.LogRpa("LiveScreenRoomInfo 响应为空", "来客数据API");
                    return null;
                }

                var jsonData = JObject.Parse(responseBody);
                var code = jsonData["code"]?.Value<int>() ?? -1;
                var message = jsonData["message"]?.Value<string>() ?? "";

                if (code != 0 || message != "success")
                {
                    FileUtils.LogRpa($"LiveScreenRoomInfo 接口返回失败: code={code}, message={message}, body={responseBody}", "来客数据API");
                    return null;
                }

                var data = jsonData["data"] as JObject;
                if (data == null)
                {
                    FileUtils.LogRpa("LiveScreenRoomInfo data为空", "来客数据API");
                    return null;
                }

                // 解析结果
                var result = new Dictionary<string, object>
                {
                    ["user_id"] = data["user_id"]?.Value<string>(),
                    ["nickname"] = data["nickname"]?.Value<string>(),
                    ["title"] = data["title"]?.Value<string>(),
                    ["create_time"] = ConvertTimestamp(data["create_time"]?.Value<string>()),
                    ["start_time"] = ConvertTimestamp(data["start_time"]?.Value<string>()),
                    ["finish_time"] = ConvertTimestamp(data["finish_time"]?.Value<string>()),
                    ["rt_mp_pull_url"] = data["rt_mp_pull_url"]?.Value<string>(),
                    ["cover"] = data["cover"]?.Value<string>(),
                    ["is_live"] = data["is_live"]?.Value<bool>() ?? false,
                    ["duration"] = FormatSeconds(data["duration"]?.Value<long>() ?? 0),
                    ["type"] = data["type"]?.Value<int>() ?? 0,
                    ["play_back_url"] = data["play_back_url"]?.Value<string>(),
                    ["v_codec"] = data["v_codec"]?.Value<string>(),
                    ["stream_id"] = data["stream_id"]?.Value<string>()
                };

                FileUtils.LogRpa($"LiveScreenRoomInfo 成功, user_id={result["user_id"]}, nickname={result["nickname"]}, is_live={result["is_live"]}, title={result["title"]}", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"LiveScreenRoomInfo 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 线索版-->直播大屏-->直播场次列表
        /// 参考 01授权拿到所有主播列表--新.py 的 clue_live_screen_anchor_room_list 方法
        /// POST https://life.douyin.com/clue/bff/pc/analysis/live-screen/anchor-room-list
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <param name="anchorId">主播的抖音uid</param>
        /// <returns>直播场次列表，失败返回null</returns>
        public static async Task<List<Dictionary<string, object>>> ClueLiveScreenAnchorRoomList(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId, string anchorId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("ClueLiveScreenAnchorRoomList Cookie为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_l",
                    ["accept"] = "application/json,*/*;q=0.8",
                    ["accept-language"] = "zh-CN,zh;q=0.8",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["content-type"] = "application/json;charset=UTF-8",
                    ["origin"] = "https://life.douyin.com",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/liteapp/leads_analysis/live-screen?groupid={groupId}",
                    ["rpc-persist-life-biz-view-id"] = "0",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["rpc-persist-life-platform"] = "pc",
                    ["rpc-persist-lite-app-id"] = "100258",
                    ["rpc-persist-terminal-type"] = "1",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-gpc"] = "1",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-client-tz"] = "Asia/Shanghai",
                    ["x-edition"] = "life",
                    ["x-secsdk-csrf-token"] = "",
                    ["x-tt-trace-log"] = "01"
                };

                string url = $"https://life.douyin.com/clue/bff/pc/analysis/live-screen/anchor-room-list?root_life_account_id={rootLifeAccountId}";
                string body = JsonConvert.SerializeObject(new { anchorID = anchorId }, Formatting.None);

                FileUtils.LogRpa($"ClueLiveScreenAnchorRoomList 请求, groupId={groupId}, rootLifeAccountId={rootLifeAccountId}, anchorId={anchorId}", "来客数据API");

                string response = await SendPostRequest(url, body, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("ClueLiveScreenAnchorRoomList 响应为空", "来客数据API");
                    return null;
                }

                var json = JObject.Parse(response);
                int statusCode = json["status_code"]?.Value<int>() ?? -1;
                if (statusCode != 0)
                {
                    FileUtils.LogRpa($"ClueLiveScreenAnchorRoomList 接口返回失败: status_code={statusCode}, body={response}", "来客数据API");
                    return null;
                }

                var roomInfos = json["data"]?["roomInfos"] as JArray;
                if (roomInfos == null || roomInfos.Count == 0)
                {
                    FileUtils.LogRpa("ClueLiveScreenAnchorRoomList roomInfos为空", "来客数据API");
                    return new List<Dictionary<string, object>>();
                }

                var result = new List<Dictionary<string, object>>();
                foreach (var room in roomInfos.OfType<JObject>())
                {
                    var item = new Dictionary<string, object>
                    {
                        ["roomID"] = room["roomID"]?.ToString(),
                        ["startTime"] = ConvertTimestamp(room["startTime"]?.ToString()),
                        ["endTime"] = ConvertTimestamp(room["endTime"]?.ToString()),
                        ["status"] = room["status"]?.Value<int>() ?? 0,
                        ["isPrivate"] = room["isPrivate"]?.Value<bool>() ?? false,
                        ["anchorID"] = room["anchorID"]?.ToString(),
                        ["title"] = room["title"]?.ToString(),
                        ["streamURL"] = room["streamURL"]?.ToString(),
                        ["anchorAvatarURL"] = room["anchorAvatarURL"]?.ToString(),
                        ["anchorNickname"] = room["anchorNickname"]?.ToString(),
                        ["roomStats"] = room["roomStats"]?.ToString(),
                        ["anchorAwemeID"] = room["anchorAwemeID"]?.ToString(),
                        ["anchorSecUID"] = room["anchorSecUID"]?.ToString()
                    };
                    result.Add(item);
                }

                FileUtils.LogRpa($"ClueLiveScreenAnchorRoomList 完成，共{result.Count}个直播场次", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"ClueLiveScreenAnchorRoomList 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 线索大屏 - 直播大屏 - 核心指标数据
        /// 参考 01授权拿到所有主播列表--新.py 的 clue_live_screen_overview_data 方法
        /// POST https://life.douyin.com/clue/bff/pc/analysis/live-screen/overview-data
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <param name="roomId">直播间ID</param>
        /// <returns>核心指标数据字典（key=中文指标名，value=格式化后的值），失败返回null</returns>
        public static async Task<Dictionary<string, object>> ClueLiveScreenOverviewData(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId, string roomId)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("ClueLiveScreenOverviewData Cookie为空", "来客数据API");
                    return null;
                }

                // 指标定义：key=接口返回字段名，name=中文名，format=格式化规则(percent/duration)
                var indexItem = new Dictionary<string, (string Name, string Format)>
                {
                    ["live_avg_watch_duration_by_room"] = ("人均观看时长", "duration"),
                    ["im_message_conversation_count"] = ("私信人数", null),
                    ["live_life_icon_click_count_all"] = ("风车房子点击次数", null),
                    ["live_comment_uv_by_room"] = ("直播间评论人数", null),
                    ["live_gift_amount"] = ("打赏金额", null),
                    ["clue_cost"] = ("线索成本", null),
                    ["live_follow_uv_by_room"] = ("涨粉量", null),
                    ["live_like_uv_by_room"] = ("直播间点赞人数", null),
                    ["live_fans_club_join_uv_by_room"] = ("加粉丝团人数", null),
                    ["live_single_watch_over_1m_count"] = (">1分钟观看次数", null),
                    ["live_fans_enter_rate_by_room"] = ("粉丝", "percent"),
                    ["live_fans_avg_watch_duration"] = ("粉丝停留", "duration"),
                    ["live_oto_pay_order_count"] = ("营销订单数", null),
                    ["message_clue_uv"] = ("私信留资人数", null),
                    ["live_watch_comment_rate"] = ("评论率", "percent"),
                    ["live_gift_count"] = ("打赏次数", null),
                    ["live_show_uv_by_room"] = ("直播间曝光人数", null),
                    ["live_avg_online_uv_by_room"] = ("平均在线人数", null),
                    ["clue_convert_rate"] = ("线索转化率", "percent"),
                    ["live_share_uv_by_room"] = ("直播间分享人数", null),
                    ["live_watch_share_rate"] = ("分享率", "percent"),
                    ["live_share_count"] = ("直播间分享次数", null),
                    ["live_interaction_rate"] = ("直播间互动率", "percent"),
                    ["uv_realtime"] = ("实时在线人数", null),
                    ["pay_order_gmv_per1k_watch_uv"] = ("千次观看GMV", null),
                    ["live_order_clue_uv"] = ("填手机号", null),
                    ["live_watch_follow_rate"] = ("关注率", "percent"),
                    ["live_fans_club_join_rate"] = ("加团率", "percent"),
                    ["form_clue_cost"] = ("表单成本", null),
                    ["live_fans_watch_rate"] = ("粉丝占比", "percent"),
                    ["live_like_count"] = ("直播间点赞次数", null),
                    ["live_life_product_click_rate_all"] = ("商品点击率", "percent"),
                    ["live_life_icon_click_rate_all"] = ("小房子点击率", "percent"),
                    ["clue_uv"] = ("全场景留资人数", null),
                    ["live_minute_max_watch_uv"] = ("最高在线人数", null),
                    ["live_enter_rate"] = ("曝光进入率", "percent"),
                    ["form_clue_uv"] = ("表单提交人数", null),
                    ["live_watch_count"] = ("直播间观看次数", null),
                    ["live_life_product_click_count_all"] = ("商品点击次数", null),
                    ["order_cost"] = ("订单成本", null),
                    ["live_life_pay_order_uv_all"] = ("订单人数", null),
                    ["live_comment_count"] = ("直播间评论次数", null),
                    ["stat_cost"] = ("营销消耗", null),
                    ["uv_with_preview"] = ("看过", null),
                    ["live_watch_uv_by_room"] = ("直播间累计观看人数", null),
                    ["live_life_product_show_count_all"] = ("商品曝光次数", null),
                    ["live_show_count"] = ("直播间曝光次数", null),
                    ["pay_order_uv_convert_rate"] = ("订单人数转化率", "percent"),
                    ["live_interact_uv_by_room"] = ("直播间互动人数", null),
                    ["live_life_pay_order_count_all"] = ("团购订单数", null),
                    ["live_interaction_count"] = ("互动次数", null),
                    ["live_watch_like_rate"] = ("点赞率", "percent"),
                    ["live_life_pay_order_gmv_all"] = ("直播间成交金额", null)
                };

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_l",
                    ["accept"] = "application/json,*/*;q=0.8",
                    ["accept-language"] = "zh-CN,zh;q=0.8",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["content-type"] = "application/json;charset=UTF-8",
                    ["origin"] = "https://life.douyin.com",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/liteapp/leads_analysis/live-screen?room_id={roomId}&show_aside=0&show_header=0&enter_from=home_live&groupid={groupId}",
                    ["rpc-persist-life-biz-view-id"] = "0",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["rpc-persist-life-platform"] = "pc",
                    ["rpc-persist-lite-app-id"] = "100258",
                    ["rpc-persist-terminal-type"] = "1",
                    ["sec-ch-ua"] = "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Brave\";v=\"150\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["sec-gpc"] = "1",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36",
                    ["x-client-tz"] = "Asia/Shanghai",
                    ["x-edition"] = "life",
                    ["x-secsdk-csrf-token"] = "",
                    ["x-tt-trace-log"] = "01"
                };

                string url = $"https://life.douyin.com/clue/bff/pc/analysis/live-screen/overview-data?root_life_account_id={rootLifeAccountId}";
                string body = JsonConvert.SerializeObject(new { roomID = roomId }, Formatting.None);

                FileUtils.LogRpa($"ClueLiveScreenOverviewData 请求, groupId={groupId}, rootLifeAccountId={rootLifeAccountId}, roomId={roomId}", "来客数据API");

                string response = await SendPostRequest(url, body, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("ClueLiveScreenOverviewData 响应为空", "来客数据API");
                    return null;
                }

                var json = JObject.Parse(response);
                int statusCode = json["status_code"]?.Value<int>() ?? -1;
                if (statusCode != 0)
                {
                    FileUtils.LogRpa($"ClueLiveScreenOverviewData 接口返回失败: status_code={statusCode}, body={response}", "来客数据API");
                    return null;
                }

                // 解析 data.statRow.metrics
                var metrics = json["data"]?["statRow"]?["metrics"] as JObject;
                if (metrics == null)
                {
                    FileUtils.LogRpa("ClueLiveScreenOverviewData metrics为空", "来客数据API");
                    return new Dictionary<string, object>();
                }

                var result = new Dictionary<string, object>();
                foreach (var kv in metrics)
                {
                    string metricKey = kv.Key;
                    if (!indexItem.ContainsKey(metricKey))
                        continue;

                    var (name, format) = indexItem[metricKey];
                    double rawValue = kv.Value?.Value<double>() ?? 0;
                    object formattedValue;

                    if (format == "percent")
                    {
                        // 百分比格式化: v * 100 保留两位小数 + %
                        formattedValue = $"{rawValue * 100:F2}%";
                    }
                    else if (format == "duration")
                    {
                        // 时长格式化: 秒数 -> xx天xx小时xx分xx秒
                        formattedValue = FormatSeconds((long)rawValue);
                    }
                    else
                    {
                        formattedValue = rawValue;
                    }

                    result[name] = formattedValue;
                }

                FileUtils.LogRpa($"ClueLiveScreenOverviewData 完成，共解析{result.Count}个指标", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"ClueLiveScreenOverviewData 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 将秒数格式化为 xx天xx小时xx分xx秒
        /// </summary>
        private static string FormatSeconds(long seconds)
        {
            if (seconds <= 0) return "0秒";
            long days = seconds / 86400;
            long remainder = seconds % 86400;
            long hours = remainder / 3600;
            remainder = remainder % 3600;
            long minutes = remainder / 60;
            long secs = remainder % 60;

            var sb = new StringBuilder();
            if (days > 0) sb.Append($"{days}天");
            if (hours > 0) sb.Append($"{hours}小时");
            if (minutes > 0) sb.Append($"{minutes}分");
            if (secs > 0) sb.Append($"{secs}秒");
            return sb.Length > 0 ? sb.ToString() : "0秒";
        }

        /// <summary>
        /// 普通版来客-->直播管理-->直播场次列表
        /// 参考 01授权拿到所有主播列表--新.py 的 get_live_list 方法
        /// GET https://life.douyin.com/life/infra/v1/content/live/get_live_list
        /// </summary>
        /// <param name="cookies">认证Cookie</param>
        /// <param name="groupId">公司groupid</param>
        /// <param name="rootLifeAccountId">来客账户id</param>
        /// <param name="awemeUserId">主播的抖音uid</param>
        /// <param name="pageIndex">页码，默认1</param>
        /// <param name="pageSize">每页条数，默认10（可选10/20/30/40/50）</param>
        /// <returns>直播场次列表，失败返回null</returns>
        public static async Task<List<Dictionary<string, object>>> GetLiveList(Dictionary<string, string> cookies, string groupId, string rootLifeAccountId, string awemeUserId, int pageIndex = 1, int pageSize = 10)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa("GetLiveList Cookie为空", "来客数据API");
                    return null;
                }

                var headers = new Dictionary<string, string>
                {
                    ["ac-tag"] = "smb_m",
                    ["accept"] = "application/json, text/plain, */*",
                    ["accept-language"] = "zh-CN,zh;q=0.9",
                    ["agw-js-conv"] = "str",
                    ["cache-control"] = "no-cache",
                    ["pragma"] = "no-cache",
                    ["priority"] = "u=1, i",
                    ["referer"] = $"https://life.douyin.com/p/merchant/live/list?groupid={groupId}&life_biz_view_id=22&life_account_biz_ids=",
                    ["rpc-persist-life-merchant-switch-role"] = "1",
                    ["sec-ch-ua"] = "\"Not:A-Brand\";v=\"99\", \"Microsoft Edge\";v=\"145\", \"Chromium\";v=\"145\"",
                    ["sec-ch-ua-mobile"] = "?0",
                    ["sec-ch-ua-platform"] = "\"Windows\"",
                    ["sec-fetch-dest"] = "empty",
                    ["sec-fetch-mode"] = "cors",
                    ["sec-fetch-site"] = "same-origin",
                    ["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0",
                    ["x-secsdk-csrf-token"] = ""
                };

                // 拼接查询参数
                string url = "https://life.douyin.com/life/infra/v1/content/live/get_live_list"
                    + $"?page_index={pageIndex}"
                    + $"&page_size={pageSize}"
                    + "&sort_key=start_time"
                    + "&is_asc=false"
                    + $"&aweme_id={awemeUserId}"
                    + "&content_tag=2"
                    + $"&poi_life_account_ids={rootLifeAccountId}"
                    + $"&root_life_account_id={rootLifeAccountId}"
                    + "&life_biz_view_id=22"
                    + "&life_account_biz_ids=";

                FileUtils.LogRpa($"GetLiveList 请求, groupId={groupId}, rootLifeAccountId={rootLifeAccountId}, awemeUserId={awemeUserId}, page={pageIndex}", "来客数据API");

                string response = await SendGetRequest(url, cookies, headers).ConfigureAwait(false);

                if (string.IsNullOrEmpty(response))
                {
                    FileUtils.LogRpa("GetLiveList 响应为空", "来客数据API");
                    return null;
                }

                var json = JObject.Parse(response);
                int statusCode = json["status_code"]?.Value<int>() ?? -1;
                if (statusCode != 0)
                {
                    FileUtils.LogRpa($"GetLiveList 接口返回失败: status_code={statusCode}, body={response}", "来客数据API");
                    return null;
                }

                // 分页信息
                var pagination = json["data"]?["pagination"];
                var totalCount = pagination?["total_count"]?.Value<int>() ?? 0;
                FileUtils.LogRpa($"GetLiveList 当前页:{pageIndex}, {pageSize}条/页, 共{totalCount}条数据", "来客数据API");

                var list = json["data"]?["list"] as JArray;
                if (list == null || list.Count == 0)
                {
                    FileUtils.LogRpa("GetLiveList list为空", "来客数据API");
                    return new List<Dictionary<string, object>>();
                }

                var result = new List<Dictionary<string, object>>();
                foreach (var item in list.OfType<JObject>())
                {
                    var liveItem = new Dictionary<string, object>
                    {
                        ["cover_image"] = item["cover_image"]?.ToString(),
                        ["end_time"] = item["end_time"]?.ToString(),
                        ["id"] = item["id"]?.ToString(),
                        ["is_live"] = item["is_live"]?.Value<bool>() ?? false,
                        ["start_time"] = item["start_time"]?.ToString(),
                        ["title"] = item["title"]?.ToString(),
                        ["user_id"] = item["user_id"]?.ToString(),
                        ["user_image"] = item["user_image"]?.ToString(),
                        ["user_nickname"] = item["user_nickname"]?.ToString()
                    };

                    // 解析 measures 中的指标数据
                    var measures = item["measures"] as JObject;
                    if (measures != null)
                    {
                        foreach (var kv in measures)
                        {
                            var metric = kv.Value as JObject;
                            if (metric != null)
                            {
                                liveItem[kv.Key] = metric["num"]?.ToString();
                            }
                        }
                    }

                    result.Add(liveItem);
                }

                FileUtils.LogRpa($"GetLiveList 完成，共{result.Count}个直播场次", "来客数据API");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetLiveList 异常: {ex.Message}", "来客数据API");
                return null;
            }
        }

        /// <summary>
        /// 将时间戳转换为可读时间字符串 (yyyy-MM-dd HH:mm:ss)
        /// 支持10位(秒)和13位(毫秒)时间戳
        /// </summary>
        private static string ConvertTimestamp(string timestamp)
        {
            if (string.IsNullOrEmpty(timestamp)) return null;
            try
            {
                long ts = long.Parse(timestamp);
                if (ts <= 0) return null;
                // 13位毫秒时间戳
                if (timestamp.Length == 13)
                    ts = ts / 1000;
                var dt = DateTimeOffset.FromUnixTimeSeconds(ts).LocalDateTime;
                return dt.ToString("yyyy-MM-dd HH:mm:ss");
            }
            catch
            {
                return null;
            }
        }
    }
}