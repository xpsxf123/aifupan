using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.dataPullers;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.life;
using ReviewAnalysis.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Utils;
using douyin.Utils;

namespace ReviewAnalysis.plugins.collectors
{
    /// <summary>
    /// 来客数据采集器（简化版）
    /// 职责单一：只负责调用API获取数据，返回原始JSON
    /// </summary>
    public class LifeCollectorSimple : IPlatformCollector
    {
        public string PlatformId => "life";
        public string PlatformName => "来客";

        private readonly LifeDataPuller _dataPuller;

        // 解绑校验去重：已触发过解绑校验的 secUid。结论确定时常驻（避免重复昂贵的逐公司枚举）；
        // 结论不确定/异常时会被移除，以便下次调度重试。
        private static readonly ConcurrentDictionary<string, bool> _unbindCheckedSecUids = new ConcurrentDictionary<string, bool>();

        public LifeCollectorSimple()
        {
            _dataPuller = new LifeDataPuller();
        }

        /// <summary>
        /// 检查是否有采集条件
        /// 包括：来客授权状态 + Cookie有效性
        /// </summary>
        public bool CanCollect(AnchorInfo anchorInfo)
        {
            if (anchorInfo == null) return false;

            // 1. 检查来客授权状态（失效后 MarkCookieExpired 会把状态改为 authExpires=2，避免重复采集循环）
            if (anchorInfo.lifeAuthStatus != (int)LifeAuthStatusEnum.auth)
            {
                FileUtils.LogRpa($"来客采集跳过：主播未授权或授权已失效，lifeAuthStatus={anchorInfo.lifeAuthStatus}(0=未授权/2=已过期/3=授权失败)，主播【{anchorInfo.AnchorName}】", "来客采集器");
                return false;
            }

            // 2. 检查本地Cookie有效性
            int cookieStatus = LifeUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
            if (cookieStatus != (int)LifeAuthStatusEnum.auth)
            {
                string reason;
                switch (cookieStatus)
                {
                    case -1: reason = "Cookie文件不存在"; break;
                    case 0: reason = "Cookie文件中无sessionid_ls/sessionid_ss_ls"; break;
                    default: reason = $"未知状态({cookieStatus})"; break;
                }
                FileUtils.LogRpa($"来客采集跳过：Cookie无效或过期，原因={reason}，主播【{anchorInfo.AnchorName}】，secUid={anchorInfo.SecUid}", "来客采集器");
                return false;
            }

            // 3. 检查 SecUid.json 中是否有该拖音号的有效记录（含groupId/lifeAccountId）。
            //    无记录 = 该拖音号已从来客解绑（或授权时未写入），来客不会产生数据。
            //    直接跳过，避免回退 GetAccountDetail 兜底导致 4000200 无限重试失败。
            var ctx = LifeUtils.FindAwemeUserContext(anchorInfo.SecUid);
            if (ctx == null || string.IsNullOrEmpty(ctx.GroupId) || string.IsNullOrEmpty(ctx.LifeAccountId))
            {
                FileUtils.LogRpa($"来客采集跳过：SecUid.json 中无该拖音号的有效记录（可能已从来客解绑或授权未写入），主播【{anchorInfo.AnchorName}】，secUid={anchorInfo.SecUid}", "来客采集器");
                // 方案A：异步触发一次"解绑校验+清理"（去重，不阻塞采集主流程）。
                // TryAdd 成功=本 secUid 首次触发；已存在=已触发过且结论确定，不重复枚举。
                if (_unbindCheckedSecUids.TryAdd(anchorInfo.SecUid, true))
                {
                    var target = anchorInfo;
                    FileUtils.LogRpa($"[解绑巡检] 触发解绑校验（异步），主播【{anchorInfo.AnchorName}】，secUid={anchorInfo.SecUid}", "来客采集器");
                    _ = Task.Run(() => VerifyAndCleanupIfUnbound(target));
                }
                return false;
            }

            FileUtils.LogRpa($"来客采集检查通过，主播【{anchorInfo.AnchorName}】，lifeAuthStatus={anchorInfo.lifeAuthStatus}", "来客采集器");
            return true;
        }

        /// <summary>
        /// 采集数据
        /// </summary>
        public async Task<string> CollectAsync(AnchorInfo anchorInfo, string roomId, string videoId = null)
        {
            var sw = System.Diagnostics.Stopwatch.StartNew();
            string collectResult = "未知";
            try
            {
                FileUtils.LogRpa($"===== [来客采集] 开始 主播【{anchorInfo?.AnchorName}】 secUid={anchorInfo?.SecUid?.Substring(0, Math.Min(20, anchorInfo?.SecUid?.Length ?? 0))}..., roomId={roomId}, videoId={videoId} =====", "来客采集器");

                if (string.IsNullOrEmpty(roomId))
                {
                    FileUtils.LogRpa($"来客采集失败：roomId为空，主播【{anchorInfo?.AnchorName}】", "来客采集器");
                    collectResult = "失败(roomId为空)";
                    return null;
                }

                FileUtils.LogRpa($"[来客采集] 即将调用 PullDataAsync，主播【{anchorInfo?.AnchorName}】，roomId={roomId}", "来客采集器");

                var json = await _dataPuller.PullDataAsync(anchorInfo, roomId, videoId);

                // ========================================================================
                // 采集失败后的Cookie失效反馈闭环（双重复检）
                // ------------------------------------------------------------------------
                // 场景：本地Cookie文件存在且有sessionid_ls，但服务端已主动让Cookie失效
                //       （如异地登录、安全风控、被动过期等）。
                //       此时API返回status_code=4000100，PullDataAsync返回null。
                //
                // 原缺陷：复检仅用本地检查（CheckAuthStatusByCookie），本地文件没变必然通过，
                //         导致失效Cookie无限重试。
                //
                // 修正方案：采集返回空数据时，双重复检：
                //   第一层：本地复检（快速排除文件被删除等场景）
                //   第二层：API复检（调GetAccountDetail验证Cookie在服务端是否真正有效）
                //   两层都通过 → 临时API错误，保持状态不变
                //   任一失败 → 标记authExpires + 同步服务端
                // ========================================================================
                if (string.IsNullOrEmpty(json) && anchorInfo != null)
                {
                    FileUtils.LogRpa($"[失效反馈] 采集返回空数据，开始双重复检，主播【{anchorInfo.AnchorName}】，secUid={anchorInfo.SecUid}", "来客采集器");

                    // 第一层：本地复检
                    int recheckStatus = LifeUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
                    if (recheckStatus != (int)LifeAuthStatusEnum.auth)
                    {
                        string localReason;
                        switch (recheckStatus)
                        {
                            case -1: localReason = "Cookie文件不存在"; break;
                            case 0: localReason = "Cookie文件中无sessionid_ls/sessionid_ss_ls"; break;
                            default: localReason = $"未知状态({recheckStatus})"; break;
                        }
                        FileUtils.LogRpa($"[失效反馈] 本地复检不通过：{localReason}，标记授权过期，主播【{anchorInfo.AnchorName}】", "来客采集器");
                        MarkCookieExpired(anchorInfo);
                        return json;
                    }

                    FileUtils.LogRpa($"[失效反馈] 本地复检通过，继续API复检...，主播【{anchorInfo.AnchorName}】", "来客采集器");

                    // 第二层：API复检（调GetAccountDetail验证服务端有效性）
                    bool apiValid = await RecheckCookieViaApi(anchorInfo.SecUid);
                    if (!apiValid)
                    {
                        FileUtils.LogRpa($"[失效反馈] API复检不通过：Cookie在服务端已失效，标记授权过期，主播【{anchorInfo.AnchorName}】", "来客采集器");
                        MarkCookieExpired(anchorInfo);
                    }
                    else
                    {
                        FileUtils.LogRpa($"[失效反馈] API复检通过：Cookie在服务端有效，判定为临时API错误，保持状态不变，主播【{anchorInfo.AnchorName}】", "来客采集器");
                    }
                }

                if (!string.IsNullOrEmpty(json))
                    collectResult = $"成功({json.Length}字节)";
                else if (collectResult == "未知")
                    collectResult = "失败(PullData返回空)";
                return json;
            }
            catch (Exception ex)
            {
                collectResult = $"异常({ex.Message})";
                FileUtils.LogRpa($"来客采集异常：{ex.Message}，主播【{anchorInfo?.AnchorName}】", "来客采集器");
                return null;
            }
            finally
            {
                sw.Stop();
                FileUtils.LogRpa($"===== [来客采集] 结束 主播【{anchorInfo?.AnchorName}】 结果={collectResult} 耗时={sw.ElapsedMilliseconds}ms =====", "来客采集器");
            }
        }

        /// <summary>
        /// API复检：调 GroupAccountListShop 验证 Cookie 在服务端是否真正有效（最多重试 3 次）。
        /// 避免单次网络抖动/短暂限流误判为失效。
        /// GroupAccountListShop 返回非 null 即表示接口 status_code==0（Cookie 有效，即使商户列表为空）；
        /// 返回 null 表示 status_code!=0 或响应为空（Cookie 失效或网络异常）。
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>true=Cookie在服务端有效；false=连续 3 次失败，判定已失效</returns>
        private async Task<bool> RecheckCookieViaApi(string secUid)
        {
            const int MaxRetries = 3;
            const int RetryDelayMs = 2000;

            try
            {
                var cookies = LifeDataHandle.GetCookiesFromLocal(secUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"[失效反馈] API复检：从本地读取Cookie为空，secUid={secUid}", "来客采集器");
                    return false;
                }

                for (int attempt = 1; attempt <= MaxRetries; attempt++)
                {
                    try
                    {
                        FileUtils.LogRpa($"[失效反馈] API复检 第 {attempt}/{MaxRetries} 次尝试（GroupAccountListShop），secUid={secUid}", "来客采集器");
                        var accountList = await LifeDataApi.GroupAccountListShop(cookies);
                        if (accountList != null)
                        {
                            FileUtils.LogRpa($"[失效反馈] API复检第{attempt}次通过：status_code==0，商户数={accountList.Count}", "来客采集器");
                            return true;
                        }
                        FileUtils.LogRpa($"[失效反馈] API复检第{attempt}次失败：GroupAccountListShop返回null（status_code!=0或响应空）", "来客采集器");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"[失效反馈] API复检第{attempt}次异常：{ex.Message}", "来客采集器");
                    }

                    if (attempt < MaxRetries)
                    {
                        await Task.Delay(RetryDelayMs);
                    }
                }

                FileUtils.LogRpa($"[失效反馈] API复检连续 {MaxRetries} 次均失败 → 判定 Cookie 在服务端已真正失效，secUid={secUid}", "来客采集器");
                return false;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[失效反馈] API复检外层异常：{ex.Message}，保守起见判定为有效（避免误杀）", "来客采集器");
                return true;
            }
        }

        /// <summary>
        /// 标记Cookie失效：删除本地 cookie 文件 + 清空 SecUid.json + 更新本地缓存 + 同步服务端。
        /// 若 SecUid.json 中能找到 authBatchId，则同批次的所有兄弟主播一同失效（同一个 cookie session）。
        /// </summary>
        private void MarkCookieExpired(AnchorInfo anchorInfo)
        {
            try
            {
                // 1. 查找当前主播的 authBatchId，用于定位兄弟
                string authBatchId = null;
                try
                {
                    var ctx = LifeUtils.FindAwemeUserContext(anchorInfo.SecUid);
                    authBatchId = ctx?.AuthBatchId;
                    if (!string.IsNullOrEmpty(authBatchId))
                        FileUtils.LogRpa($"[失效反馈] 定位到 authBatchId={authBatchId}，将联动失效同批次兄弟，主播【{anchorInfo.AnchorName}】", "来客采集器");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"[失效反馈] 查找 authBatchId 异常（仅处理当前主播）: {ex.Message}", "来客采集器");
                }

                // 2. 拼装待失效 secUid 列表（包含当前）
                var affectedSecUids = new List<string>();
                if (!string.IsNullOrEmpty(authBatchId))
                {
                    affectedSecUids = LifeUtils.FindSecUidsByAuthBatchId(authBatchId);
                }
                if (!affectedSecUids.Contains(anchorInfo.SecUid))
                    affectedSecUids.Add(anchorInfo.SecUid);
                FileUtils.LogRpa($"[失效反馈] 本次失效共涉及 {affectedSecUids.Count} 个主播（含当前）", "来客采集器");

                // 3. 逐个处理：删文件 + 清 JSON + 更缓存 + 同服端
                foreach (var secUid in affectedSecUids)
                {
                    try
                    {
                        // 3.1 删本地 cookie 文件
                        LifeUtils.DeleteLocalCookieFile(secUid);

                        // 3.2 清 SecUid.json 对应记录
                        LifeUtils.ClearAwemeUserInSecUidJson(secUid);

                        // 3.3 更新本地主播缓存 + 同步服务端
                        AnchorInfo target = (secUid == anchorInfo.SecUid)
                            ? anchorInfo
                            : AnchorCacheManager.GetAllNotRemoveAnchors()?.FirstOrDefault(a => a.SecUid == secUid);

                        if (target != null)
                        {
                            AnchorBll.UpdateLifeAuthStatus(target, (int)LifeAuthStatusEnum.authExpires);
                        }
                        else
                        {
                            FileUtils.LogRpa($"[失效反馈] 兄弟 secUid={secUid} 本地主播缓存未找到，仅同步服务端", "来客采集器");
                        }

                        var updateDto = new UpdateAnchorUserDto
                        {
                            secUid = secUid,
                            authLifeStatus = (int)LifeAuthStatusEnum.authExpires
                        };
                        AnchorApi.UpdateAnchorUserInfo(updateDto);

                        // 3.4 通知前端授权失效（参照巨量方案，每个失效主播单独通知）
                        try
                        {
                            var frontNotice = new FrontNotice();
                            var requestDataObj = new Dictionary<string, object>();
                            requestDataObj["code"] = 0;
                            requestDataObj["status"] = 200;
                            requestDataObj["action"] = "lifeAuthExpires";
                            var dataDict = new Dictionary<string, object>
                            {
                                ["secUid"] = secUid,
                                ["anchorName"] = target?.AnchorName ?? "",
                                ["msg"] = "来客授权已过期，请重新授权"
                            };
                            requestDataObj["data"] = dataDict;
                            _ = frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                            FileUtils.LogRpa($"[失效反馈] 已通知前端 lifeAuthExpires: secUid={secUid}, anchorName={(target?.AnchorName ?? "?")}", "来客采集器");
                        }
                        catch (Exception noticeEx)
                        {
                            FileUtils.LogRpa($"[失效反馈] 通知前端 lifeAuthExpires 异常（不阻断）: {noticeEx.Message}", "来客采集器");
                        }

                        FileUtils.LogRpa($"[失效反馈] 已标记失效: secUid={secUid}, anchorName={(target?.AnchorName ?? "?")}", "来客采集器");
                    }
                    catch (Exception exOne)
                    {
                        FileUtils.LogRpa($"[失效反馈] 处理单个 secUid={secUid} 异常: {exOne.Message}", "来客采集器");
                    }
                }

                FileUtils.LogRpa($"[失效反馈] 本次失效处理完成，共 {affectedSecUids.Count} 个主播，触发主播【{anchorInfo.AnchorName}】", "来客采集器");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[失效反馈] 标记授权过期异常：{ex.Message}", "来客采集器");
            }
        }

        /// <summary>
        /// 解绑校验+清理（方案A：由 CanCollect 命中"SecUid.json无记录"时异步触发一次）。
        /// 对"本地有有效cookie但SecUid.json无记录"的拖音号，逐公司枚举校验是否已从来客解绑：
        /// - 不确定（接口失败/异常）：移出去重集，下次调度重试。
        /// - 仍绑定（情况①，只是未写入SecUid.json）：保留去重集，本次不清理。
        /// - 确认已解绑：删本地cookie + 清SecUid.json + 本地缓存/服务端置未授权(unAuth)，强制重新授权。
        /// </summary>
        private static async Task VerifyAndCleanupIfUnbound(AnchorInfo anchor)
        {
            try
            {
                var cookies = LifeDataHandle.GetCookiesFromLocal(anchor.SecUid);
                if (cookies == null || cookies.Count == 0)
                {
                    FileUtils.LogRpa($"[解绑巡检] 本地无cookie，无法逐公司枚举校验，主播【{anchor.AnchorName}】", "来客采集器");
                    _unbindCheckedSecUids.TryRemove(anchor.SecUid, out _);
                    return;
                }

                string awemeUserId = LifeDataHandle.GetAwemeUserIdFromCookieFile(anchor.SecUid);
                if (string.IsNullOrEmpty(awemeUserId)) awemeUserId = anchor.AnchorUserId;
                if (string.IsNullOrEmpty(awemeUserId))
                {
                    FileUtils.LogRpa($"[解绑巡检] 无法确定awemeUserId，跳过，主播【{anchor.AnchorName}】", "来客采集器");
                    return;
                }

                FileUtils.LogRpa($"[解绑巡检] 主播【{anchor.AnchorName}】有cookie但SecUid.json无记录，开始逐公司枚举校验，awemeUserId={awemeUserId}", "来客采集器");
                var check = await IsAwemeBoundAnywhere(cookies, awemeUserId);

                if (check.Bound == null)
                {
                    FileUtils.LogRpa($"[解绑巡检] 校验结果不确定（接口失败），移出去重集，下次调度重试，主播【{anchor.AnchorName}】", "来客采集器");
                    _unbindCheckedSecUids.TryRemove(anchor.SecUid, out _);
                    return;
                }

                if (check.Bound == true)
                {
                    // 方案B：仍绑定但SecUid.json缺记录 → 用枚举查到的公司上下文自动补录，使下次采集可正常通过。
                    FileUtils.LogRpa($"[解绑巡检] awemeUserId={awemeUserId} 仍绑定在公司 groupId={check.GroupId} 下（未写入SecUid.json），尝试自动补录（方案B），主播【{anchor.AnchorName}】", "来客采集器");
                    bool restored = LifeUtils.EnsureAwemeUserInSecUidJson(check.GroupId, awemeUserId, anchor.SecUid, check.Nickname);
                    if (restored)
                    {
                        FileUtils.LogRpa($"[解绑巡检] 补录成功：SecUid.json 已回填该拖音号（groupId={check.GroupId}），下次调度将正常采集，主播【{anchor.AnchorName}】", "来客采集器");
                    }
                    else
                    {
                        FileUtils.LogRpa($"[解绑巡检] 补录失败（SecUid.json 无该 groupId 公司条目或写入异常），移出去重集下次重试，主播【{anchor.AnchorName}】", "来客采集器");
                        _unbindCheckedSecUids.TryRemove(anchor.SecUid, out _);
                    }
                    return;
                }

                FileUtils.LogRpa($"[解绑巡检] 确认已从来客解绑（所有公司均无此awemeUserId），执行清理：删cookie+置未授权，主播【{anchor.AnchorName}】", "来客采集器");
                LifeDataCollectionManager.StopPolling(anchor.SecUid);
                LifeUtils.DeleteLocalCookieFile(anchor.SecUid);
                LifeUtils.ClearAwemeUserInSecUidJson(anchor.SecUid);
                AnchorBll.UpdateLifeAuthStatus(anchor, (int)LifeAuthStatusEnum.unAuth);
                FileUtils.LogRpa($"[解绑巡检] 清理完成：主播【{anchor.AnchorName}】已置未授权，需重新授权登录", "来客采集器");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[解绑巡检] 异常：{ex.Message}，主播【{anchor.AnchorName}】", "来客采集器");
                _unbindCheckedSecUids.TryRemove(anchor.SecUid, out _);
            }
        }

        /// <summary>
        /// 解绑校验结果：Bound（true=仍绑定/false=确认解绑/null=不确定）+ 命中时的公司上下文（供方案B补录）。
        /// </summary>
        private class UnbindCheckResult
        {
            public bool? Bound;
            public string GroupId = "";
            public string Nickname = "";
        }

        /// <summary>
        /// 用登录cookie逐公司枚举，判断指定 awemeUserId 是否仍绑定在该来客账号名下的任意一家公司。
        /// Bound=true 时附带命中的 groupId/昵称，供方案B自动补录 SecUid.json。
        /// </summary>
        private static async Task<UnbindCheckResult> IsAwemeBoundAnywhere(Dictionary<string, string> cookies, string awemeUserId)
        {
            var companyList = await LifeDataApi.GroupAccountListShop(cookies);
            if (companyList == null)
            {
                FileUtils.LogRpa($"[解绑巡检] GroupAccountListShop 返回null（接口失败），无法判定", "来客采集器");
                return new UnbindCheckResult { Bound = null };
            }

            bool anyEnumFailed = false;
            int companyIndex = 0;
            FileUtils.LogRpa($"[解绑巡检] 逐公司枚举开始，共 {companyList.Count} 家公司，目标 awemeUserId={awemeUserId}", "来客采集器");
            foreach (var company in companyList)
            {
                companyIndex++;
                string accountName = company.ContainsKey("account_name") ? company["account_name"] : "";
                string groupId = company.ContainsKey("account_id") ? company["account_id"] : "";
                string lifeAccountId = company.ContainsKey("life_account_id") ? company["life_account_id"] : "";
                string defaultAweme = company.ContainsKey("aweme_user_id") ? company["aweme_user_id"] : "";

                if (!string.IsNullOrEmpty(defaultAweme) && defaultAweme == awemeUserId)
                {
                    FileUtils.LogRpa($"[解绑巡检] 命中默认号：公司[{accountName}] groupId={groupId} 默认aweme={defaultAweme} == 目标 → 仍绑定", "来客采集器");
                    return new UnbindCheckResult { Bound = true, GroupId = groupId };
                }

                if (string.IsNullOrEmpty(groupId) || string.IsNullOrEmpty(lifeAccountId))
                    continue;

                bool normalOk = false, clueOk = false;
                int normalCount = 0, clueCount = 0;
                try
                {
                    var normalUsers = await LifeDataApi.GetAwemeUsers(cookies, groupId, lifeAccountId);
                    if (normalUsers != null)
                    {
                        normalOk = true;
                        normalCount = normalUsers.Count;
                        var hitNormal = normalUsers.FirstOrDefault(u => u != null && u.ContainsKey("aweme_user_id") && u["aweme_user_id"] == awemeUserId);
                        if (hitNormal != null)
                        {
                            string nick = hitNormal.ContainsKey("nick_name") ? hitNormal["nick_name"] : (hitNormal.ContainsKey("nickname") ? hitNormal["nickname"] : "");
                            FileUtils.LogRpa($"[解绑巡检] 命中：公司[{accountName}] groupId={groupId} 普通版列表含目标 awemeUserId={awemeUserId} → 仍绑定", "来客采集器");
                            return new UnbindCheckResult { Bound = true, GroupId = groupId, Nickname = nick };
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"[解绑巡检] GetAwemeUsers 异常 groupId={groupId}: {ex.Message}", "来客采集器");
                }

                try
                {
                    var clueUsers = await LifeDataApi.ClueGetAwemeUsers(cookies, groupId, lifeAccountId);
                    if (clueUsers != null)
                    {
                        clueOk = true;
                        clueCount = clueUsers.Count;
                        var hitClue = clueUsers.FirstOrDefault(u => u != null && u.ContainsKey("douyinUID") && u["douyinUID"] == awemeUserId);
                        if (hitClue != null)
                        {
                            string nick = hitClue.ContainsKey("douyinNickname") ? hitClue["douyinNickname"] : "";
                            FileUtils.LogRpa($"[解绑巡检] 命中：公司[{accountName}] groupId={groupId} 线索版列表含目标 awemeUserId={awemeUserId} → 仍绑定", "来客采集器");
                            return new UnbindCheckResult { Bound = true, GroupId = groupId, Nickname = nick };
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"[解绑巡检] ClueGetAwemeUsers 异常 groupId={groupId}: {ex.Message}", "来客采集器");
                }

                if (!normalOk && !clueOk)
                {
                    anyEnumFailed = true;
                    FileUtils.LogRpa($"[解绑巡检] 第{companyIndex}/{companyList.Count}家 公司[{accountName}] groupId={groupId} 两个枚举接口都失败，本公司无法确认", "来客采集器");
                }
                else
                {
                    FileUtils.LogRpa($"[解绑巡检] 第{companyIndex}/{companyList.Count}家 公司[{accountName}] groupId={groupId} 未命中（普通版{normalCount}个/线索版{clueCount}个）", "来客采集器");
                }
            }

            if (anyEnumFailed)
            {
                FileUtils.LogRpa($"[解绑巡检] 部分公司枚举失败，为避免误判，判定为不确定", "来客采集器");
                return new UnbindCheckResult { Bound = null };
            }

            FileUtils.LogRpa($"[解绑巡检] 逐公司枚举结束，{companyList.Count}家公司均未含 awemeUserId={awemeUserId} → 判定已解绑", "来客采集器");
            return new UnbindCheckResult { Bound = false };
        }

        /// <summary>
        /// 检查Cookie是否有效
        /// </summary>
        public async Task<bool> CheckCookieAsync(string secUid)
        {
            return await Task.Run(() =>
            {
                try
                {
                    var cookies = _dataPuller.GetType()
                        .GetMethod("GetCookies", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance)
                        ?.Invoke(_dataPuller, new object[] { secUid }) as Dictionary<string, string>;
                    return cookies != null && cookies.Count > 0;
                }
                catch
                {
                    return false;
                }
            });
        }
    }
}
