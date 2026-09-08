using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.life;
using ReviewAnalysis.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using douyin.Utils;

namespace ReviewAnalysis.plugins.dataPullers
{
    /// <summary>
    /// 来客数据拉取器（v2：以 SecUid.json 为主，API 仅作兜底）
    /// 职责：调用来客API获取数据，返回原始JSON
    /// </summary>
    public class LifeDataPuller : BaseDataPuller
    {
        public LifeDataPuller() : base("life", "来客") { }

        /// <summary>
        /// 拉取来客数据（v2）：
        /// - 主路径：LifeUtils.FindAwemeUserContext(secUid) 一次拿到 groupId / lifeAccountId / awemeUserId / iscue
        /// - 兜底：SecUid.json 找不到时才调 GetAccountDetail + HomeMenus
        /// - roomId：优先用传入值；为空时才调 GetAccountDetail 拿 current_live_room_id
        /// </summary>
        public override async Task<string> PullDataAsync(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            try
            {
                if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                {
                    FileUtils.LogRpa("PullDataAsync anchorInfo或SecUid为空", _platformName);
                    return null;
                }

                // ---------- Step 1：读本地 cookie ----------
                var cookies = GetCookies(anchorInfo.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"Cookie获取失败（文件不存在或为空），主播【{anchorInfo.AnchorName}】，secUid={anchorInfo.SecUid}", _platformName);
                    return null;
                }

                bool hasSessionId = cookies.ContainsKey("sessionid_ls") || cookies.ContainsKey("sessionid_ss_ls");
                bool hasSidTt = cookies.ContainsKey("sid_tt_ls");
                FileUtils.LogRpa($"[Step1] Cookie加载成功，共{cookies.Count}个，sessionid={hasSessionId}, sid_tt={hasSidTt}，主播【{anchorInfo.AnchorName}】", _platformName);

                // ---------- Step 2：从 SecUid.json 取采集上下文（主路径） ----------
                FileUtils.LogRpa($"[Step2] 查找采集上下文，secUid={anchorInfo.SecUid?.Substring(0, Math.Min(20, anchorInfo.SecUid.Length))}...", _platformName);
                var context = LifeUtils.FindAwemeUserContext(anchorInfo.SecUid);
                string groupId;
                string rootLifeAccountId;
                string awemeUserId;
                bool isClueVersion;
                JObject accountInfo;

                if (context != null && !string.IsNullOrEmpty(context.GroupId) && !string.IsNullOrEmpty(context.LifeAccountId))
                {
                    groupId = context.GroupId;
                    rootLifeAccountId = context.LifeAccountId;
                    awemeUserId = !string.IsNullOrEmpty(context.AwemeUserId) ? context.AwemeUserId : anchorInfo.AnchorUserId;
                    isClueVersion = context.IsClueVersion;

                    accountInfo = new JObject
                    {
                        ["account_id"] = groupId,
                        ["group_id"] = groupId,
                        ["life_account_id"] = rootLifeAccountId,
                        ["aweme_user_id"] = awemeUserId ?? "",
                        ["account_name"] = context.AccountName ?? ""
                    };

                    FileUtils.LogRpa($"[采集上下文] 使用SecUid.json本地上下文: groupId={groupId}, lifeAccountId={rootLifeAccountId}, awemeUserId={awemeUserId}, iscue={isClueVersion}, company=[{context.AccountName}]", _platformName);
                    if (context.Status == "inactive")
                    {
                        FileUtils.LogRpa($"[采集上下文] 告警：当前记录 status=inactive（可能已被其他来客账号覆盖），仍尝试采集，主播【{anchorInfo.AnchorName}】", _platformName);
                    }
                }
                else
                {
                    // ---------- 兜底：SecUid.json 找不到 → 回退到 GetAccountDetail ----------
                    FileUtils.LogRpa($"[采集上下文] SecUid.json 中无匹配记录，回退到 GetAccountDetail API 兜底，主播【{anchorInfo.AnchorName}】", _platformName);
                    var fallback = await GetContextFromApiAsync(cookies).ConfigureAwait(false);
                    if (fallback == null)
                    {
                        FileUtils.LogRpa($"[采集上下文] API兜底也失败（Cookie可能已失效），主播【{anchorInfo.AnchorName}】", _platformName);
                        return null;
                    }
                    groupId = fallback["group_id"]?.ToString();
                    rootLifeAccountId = fallback["life_account_id"]?.ToString();
                    awemeUserId = fallback["aweme_user_id"]?.ToString();
                    if (string.IsNullOrEmpty(awemeUserId) || awemeUserId == "0")
                        awemeUserId = anchorInfo.AnchorUserId ?? "";
                    fallback["aweme_user_id"] = awemeUserId;

                    // 版本判断回退到 HomeMenus
                    isClueVersion = await LifeDataApi.HomeMenus(groupId, rootLifeAccountId, cookies).ConfigureAwait(false);
                    FileUtils.LogRpa($"[采集上下文] 兜底路径：HomeMenus 版本判断 iscue={isClueVersion}", _platformName);

                    accountInfo = fallback;

                    // API 顺带返回了 current_live_room_id 且传入 roomId 为空 → 直接使用
                    var apiLiveRoomId = fallback["current_live_room_id"]?.ToString();
                    if (string.IsNullOrEmpty(roomId) && !string.IsNullOrEmpty(apiLiveRoomId) && apiLiveRoomId != "0")
                    {
                        roomId = apiLiveRoomId;
                        FileUtils.LogRpa($"[采集上下文] 使用API返回的 current_live_room_id={roomId}", _platformName);
                    }
                }

                // ---------- Step 3：roomId 处理（仅在为空时调 API 兜底） ----------
                FileUtils.LogRpa($"[Step3] roomId 处理，传入roomId={roomId}", _platformName);
                if (string.IsNullOrEmpty(roomId) || roomId == "0")
                {
                    FileUtils.LogRpa($"[Step3] roomId为空，尝试调 GetAccountDetail 拿 current_live_room_id，主播【{anchorInfo.AnchorName}】", _platformName);
                    var detail = await LifeDataApi.GetAccountDetail(cookies, groupId).ConfigureAwait(false);
                    if (detail != null && detail.ContainsKey("current_live_room_id"))
                    {
                        var liveRoomId = detail["current_live_room_id"];
                        if (!string.IsNullOrEmpty(liveRoomId) && liveRoomId != "0")
                        {
                            roomId = liveRoomId;
                            FileUtils.LogRpa($"[Step3] 拿到 current_live_room_id={roomId}", _platformName);
                        }
                    }
                }

                // ---------- Step 3.2：GetAccountDetail 仍拿不到 → 按版本兜底拿直播场次ID ----------
                if (string.IsNullOrEmpty(roomId) || roomId == "0")
                {
                    if (isClueVersion)
                    {
                        // 线索版：ClueLiveScreenAnchorRoomList（anchorID=主播抖音uid=awemeUserId），取 roomID
                        FileUtils.LogRpa($"[Step3.2-线索版] current_live_room_id 为空，尝试调 ClueLiveScreenAnchorRoomList 拿 roomID，groupId={groupId}, lifeAccountId={rootLifeAccountId}, anchorID={awemeUserId}，主播【{anchorInfo.AnchorName}】", _platformName);
                        var roomList = await LifeDataApi.ClueLiveScreenAnchorRoomList(cookies, groupId, rootLifeAccountId, awemeUserId).ConfigureAwait(false);
                        if (roomList != null && roomList.Count > 0)
                        {
                            // 优先取正在直播（status==2）的场次；取不到则用第一条（最新）
                            var roomItem = roomList.FirstOrDefault(x => x != null && x.ContainsKey("status") && x["status"] is int s && s == 2);
                            if (roomItem == null)
                            {
                                roomItem = roomList[0];
                                FileUtils.LogRpa($"[Step3.2-线索版] 无正在直播的场次(status==2)，使用最新一场 roomID 兜底", _platformName);
                            }
                            else
                            {
                                FileUtils.LogRpa($"[Step3.2-线索版] 命中正在直播的场次(status==2)", _platformName);
                            }

                            var clueRoomId = roomItem.ContainsKey("roomID") ? roomItem["roomID"]?.ToString() : null;
                            if (!string.IsNullOrEmpty(clueRoomId) && clueRoomId != "0")
                            {
                                roomId = clueRoomId;
                                FileUtils.LogRpa($"[Step3.2-线索版] 拿到 ClueLiveScreenAnchorRoomList roomID={roomId}", _platformName);
                            }
                        }
                        else
                        {
                            FileUtils.LogRpa($"[Step3.2-线索版] ClueLiveScreenAnchorRoomList 返回空，无可用直播场次", _platformName);
                        }
                    }
                    else
                    {
                        // 普通版(EOS)：GetLiveList，取 id
                        FileUtils.LogRpa($"[Step3.2-普通版] current_live_room_id 为空，尝试调 GetLiveList 拿直播场次 id，groupId={groupId}, lifeAccountId={rootLifeAccountId}, awemeUserId={awemeUserId}，主播【{anchorInfo.AnchorName}】", _platformName);
                        var liveList = await LifeDataApi.GetLiveList(cookies, groupId, rootLifeAccountId, awemeUserId).ConfigureAwait(false);
                        if (liveList != null && liveList.Count > 0)
                        {
                            // 优先取正在直播（is_live==true）的场次；取不到则用最新一场（列表按开播时间倒序，第一条最新）
                            var liveItem = liveList.FirstOrDefault(x => x != null && x.ContainsKey("is_live") && x["is_live"] is bool b && b);
                            if (liveItem == null)
                            {
                                liveItem = liveList[0];
                                FileUtils.LogRpa($"[Step3.2-普通版] 无正在直播的场次，使用最新一场 id 兜底", _platformName);
                            }
                            else
                            {
                                FileUtils.LogRpa($"[Step3.2-普通版] 命中正在直播的场次", _platformName);
                            }

                            var listRoomId = liveItem.ContainsKey("id") ? liveItem["id"]?.ToString() : null;
                            if (!string.IsNullOrEmpty(listRoomId) && listRoomId != "0")
                            {
                                roomId = listRoomId;
                                FileUtils.LogRpa($"[Step3.2-普通版] 拿到 GetLiveList id={roomId}", _platformName);
                            }
                        }
                        else
                        {
                            FileUtils.LogRpa($"[Step3.2-普通版] GetLiveList 返回空，无可用直播场次", _platformName);
                        }
                    }
                }

                if (string.IsNullOrEmpty(roomId) || roomId == "0")
                {
                    FileUtils.LogRpa($"[Step3] 未获取到有效的来客roomId（主播可能未在直播），主播【{anchorInfo.AnchorName}】", _platformName);
                    return null;
                }

                string effectiveVideoId = !string.IsNullOrEmpty(videoId) ? videoId : roomId;

                // ---------- Step 4：合并结果头部 ----------
                FileUtils.LogRpa($"[Step4] 拼装采集结果头部，roomId={roomId}, videoId={effectiveVideoId}", _platformName);
                var mergedData = new JObject
                {
                    ["roomId"] = roomId,
                    ["videoId"] = effectiveVideoId,
                    ["collectTime"] = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"),
                    ["data"] = new JObject
                    {
                        ["accountDetail"] = accountInfo
                    }
                };

                // ---------- Step 5：按 iscue 分支采集 ----------
                if (isClueVersion)
                {
                    FileUtils.LogRpa($"[Step5] 来客账户为线索版（iscue=true），进入线索大屏链路，roomId={roomId}, groupId={groupId}, lifeAccountId={rootLifeAccountId}", _platformName);

                    var clueResult = await CallApiWithRetry(async () =>
                    {
                        var result = await LifeDataApi.GetClueScreenDataWithProducts(roomId, groupId, rootLifeAccountId, cookies);
                        return result != null && result.HasValues ? result.ToString(Formatting.None) : null;
                    });

                    if (string.IsNullOrEmpty(clueResult))
                    {
                        FileUtils.LogRpa($"[Step5-线索版] 获取线索大屏数据失败（overview-data或product-list返回空），roomId={roomId}，主播【{anchorInfo.AnchorName}】", _platformName);
                        return null;
                    }

                    mergedData["dataType"] = "liveScreenOverviewData";
                    mergedData["data"]["liveScreenOverviewData"] = JObject.Parse(clueResult);

                    string clueJson = JsonConvert.SerializeObject(mergedData);
                    FileUtils.LogRpa($"[Step5-线索版] 拉取完成，roomId={roomId}，总长={clueJson.Length}字节", _platformName);
                    FileUtils.LogRpa($"[Step6] 来客采集结果JSON: {clueJson}", _platformName);
                    return clueJson;
                }

                FileUtils.LogRpa($"[Step5] 来客账户为普通版（iscue=false），进入EOS大屏链路，roomId={roomId}, groupId={groupId}, awemeUserId={awemeUserId}", _platformName);

                var liveResult = await CallApiWithRetry(async () =>
                {
                    var result = await LifeDataApi.GetLiveScreenDataWithProducts(roomId, accountInfo, cookies);
                    return result != null && result.HasValues ? result.ToString(Formatting.None) : null;
                });

                if (!string.IsNullOrEmpty(liveResult))
                {
                    mergedData["dataType"] = "liveScreenKeyIndex";
                    mergedData["data"]["liveScreenKeyIndex"] = JObject.Parse(liveResult);
                }
                else
                {
                    FileUtils.LogRpa($"[Step5-普通版] 获取EOS大屏数据失败（be-token获取或key_index/product_list失败），roomId={roomId}，主播【{anchorInfo.AnchorName}】", _platformName);
                    return null;
                }

                string json = JsonConvert.SerializeObject(mergedData);
                FileUtils.LogRpa($"[Step5-普通版] 拉取完成，roomId={roomId}，总长={json.Length}字节", _platformName);
                FileUtils.LogRpa($"[Step6] 来客采集结果JSON: {json}", _platformName);
                return json;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取来客数据异常: {ex.Message}", _platformName);
                return null;
            }
        }

        /// <summary>
        /// 兜底：当 SecUid.json 无法定位到上下文时，回退到 GetAccountDetail 拿 group_id / life_account_id / aweme_user_id。
        /// </summary>
        private async Task<JObject> GetContextFromApiAsync(Dictionary<string, string> cookies)
        {
            try
            {
                var accountDetail = await CallApiWithRetry(async () =>
                {
                    var result = await LifeDataApi.GetAccountDetail(cookies, null);
                    return result != null && result.ContainsKey("group_id") ? JsonConvert.SerializeObject(result) : null;
                });
                if (string.IsNullOrEmpty(accountDetail)) return null;

                var accountInfo = JObject.Parse(accountDetail);
                if (string.IsNullOrEmpty(accountInfo["group_id"]?.ToString())) return null;
                return accountInfo;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"GetContextFromApiAsync 异常: {ex.Message}", _platformName);
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
                return GetLifeCookiesFromLocal(secUid) ?? new Dictionary<string, string>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从本地获取来客Cookie
        /// </summary>
        private Dictionary<string, string> GetLifeCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = GetLifeCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    FileUtils.LogRpa($"Cookie文件不存在: {cookiePath}", _platformName);
                    return new Dictionary<string, string>();
                }

                string cookieContent = File.ReadAllText(cookiePath);
                // 兼容新旧格式（LifeCookieFileDto.Parse 内部已处理JArray旧格式和JObject新格式）
                var (pullerFileDto, _) = LifeCookieFileDto.Parse(cookieContent);
                var cookies = pullerFileDto.Cookies;
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
                FileUtils.LogRpa($"从本地获取来客Cookie异常: {ex.Message}", _platformName);
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 获取来客Cookie路径
        /// </summary>
        private string GetLifeCookiePath(string secUid)
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = GetCachePathMd5(secUid);
            return $"{cachePath}\\life-{md5Str}";
        }

        /// <summary>
        /// 计算缓存路径MD5（与 LifeUtils.getCachePathMd5 保持一致）
        /// </summary>
        private string GetCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }

        /// <summary>
        /// 来客Cookie DTO
        /// </summary>
        private class LifeCookieDto
        {
            public string Name { get; set; }
            public string Value { get; set; }
            public string Domain { get; set; }
            public string Path { get; set; }
        }
    }
}
