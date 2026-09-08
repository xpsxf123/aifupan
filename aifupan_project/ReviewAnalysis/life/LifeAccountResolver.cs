using Newtonsoft.Json;
using douyin.Utils;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;

namespace ReviewAnalysis.life
{
    /// <summary>
    /// 授权后处理器：拿到 cookie 后，扫描所有公司，写入 SecUid.json（最终版格式），生成主播 cookie 文件。
    /// </summary>
    public static class LifeAccountResolver
    {
        /// <summary>
        /// 授权后主入口。
        /// </summary>
        public static async Task<LifeAccountResolveResult> ResolveAllAccountsAsync(
            Dictionary<string, string> cookieDict,
            List<LifeCookieDto> mergedCookies,
            string currentAuthSecUid)
        {
            var result = new LifeAccountResolveResult();

            try
            {
                if (cookieDict == null || cookieDict.Count == 0)
                {
                    FileUtils.LogRpa("[LifeAccountResolver] cookie为空，中止", "来客授权");
                    return result;
                }

                // 1. GroupAccountListShop 拿所有公司列表
                FileUtils.LogRpa("[LifeAccountResolver] Step1: GroupAccountListShop 获取所有公司列表", "来客授权");
                var companyList = await LifeDataApi.GroupAccountListShop(cookieDict).ConfigureAwait(false);
                if (companyList == null || companyList.Count == 0)
                {
                    FileUtils.LogRpa("[LifeAccountResolver] 公司列表为空", "来客授权");
                    return result;
                }

                result.TotalCompanyCount = companyList.Count;
                FileUtils.LogRpa($"[LifeAccountResolver] 共 {companyList.Count} 家公司待处理", "来客授权");

                // 2. 加载现有 SecUid.json（累积写入）
                var secUidData = LifeUtils.LoadSecUidData();
                // 生成本次授权的批次ID（同一次授权的所有来客账号共享此 batchId，用于确认兄弟关系）
                string authBatchId = $"batch-{DateTime.Now:yyyyMMddHHmmss}-{Guid.NewGuid().ToString("N").Substring(0, 6)}";
                FileUtils.LogRpa($"[LifeAccountResolver] 生成授权批次ID: authBatchId={authBatchId}", "来客授权");
                // 本地主播缓存
                var allAnchors = AnchorCacheManager.GetAllNotRemoveAnchors() ?? new List<AnchorInfo>();

                // 3. 遍历每家公司
                foreach (var company in companyList)
                {
                    string accountName = company.ContainsKey("account_name") ? company["account_name"] : "";
                    string groupId = company.ContainsKey("account_id") ? company["account_id"] : "";           // key_account_id = groupId
                    string lifeAccountId = company.ContainsKey("life_account_id") ? company["life_account_id"] : ""; // life_account_id
                    string companyAwemeUserId = company.ContainsKey("aweme_user_id") ? company["aweme_user_id"] : ""; // 商户默认抖音uid，"0"或空表示未开通来客

                    if (string.IsNullOrEmpty(groupId) || string.IsNullOrEmpty(lifeAccountId))
                    {
                        FileUtils.LogRpa($"[LifeAccountResolver] 公司 {accountName} 缺少 groupId/lifeAccountId，跳过", "来客授权");
                        result.FailedCompanyNames.Add($"{accountName}(信息缺失)");
                        continue;
                    }

                    // aweme_user_id="0" 或空 → 该商户未绑定任何抖音号 / 未开通来客，跳过并写入msg
                    if (string.IsNullOrEmpty(companyAwemeUserId) || companyAwemeUserId == "0")
                    {
                        string msg0 = $"aweme_user_id={companyAwemeUserId ?? "null"}，该商户未绑定抖音号或未开通来客";
                        FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] {msg0}，跳过后续接口", "来客授权");
                        result.FailedCompanyNames.Add($"{accountName}(未开通来客)");
                        WriteFailedCompanyToJson(secUidData, lifeAccountId, groupId, accountName, false, msg0, authBatchId);
                        continue;
                    }

                    FileUtils.LogRpa($"[LifeAccountResolver] ------ 处理公司 [{accountName}] groupId={groupId}, lifeAccountId={lifeAccountId}, awemeUserId={companyAwemeUserId} ------", "来客授权");

                    // 3.1 判断线索版 or 普通版
                    bool isClueVersion = await LifeDataApi.CluePcUserInfo(cookieDict, groupId, lifeAccountId).ConfigureAwait(false);
                    FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] isClueVersion={isClueVersion}", "来客授权");

                    // 3.2 拿菜单文字
                    string menusText = await LifeDataApi.GetHomeMenusText(cookieDict, groupId, lifeAccountId).ConfigureAwait(false);
                    if (string.IsNullOrEmpty(menusText))
                    {
                        string msg1 = "菜单文字为空，无法判断版本";
                        FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] {msg1}，跳过", "来客授权");
                        result.FailedCompanyNames.Add($"{accountName}(菜单为空)");
                        WriteFailedCompanyToJson(secUidData, lifeAccountId, groupId, accountName, isClueVersion, msg1, authBatchId);
                        continue;
                    }

                    bool hasClueMenu = menusText.Contains("线索经营")
                                       && (menusText.Contains("线索数据") || menusText.Contains("数据分析"));
                    bool hasNormalMenu = menusText.Contains("直播专业版") && menusText.Contains("直播管理");
                    FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 菜单判断: hasClueMenu={hasClueMenu}, hasNormalMenu={hasNormalMenu}, 菜单长度={menusText.Length}", "来客授权");

                    // 3.3 按规则决定接口
                    List<LifeAwemeUserInfoDto> awemeUsers = null;
                    if (isClueVersion)
                    {
                        if (hasClueMenu)
                        {
                            FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 线索版 → ClueGetAwemeUsers", "来客授权");
                            var clueUsers = await LifeDataApi.ClueGetAwemeUsers(cookieDict, groupId, lifeAccountId).ConfigureAwait(false);
                            awemeUsers = ConvertClueUsers(clueUsers);
                        }
                        else if (hasNormalMenu)
                        {
                            FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 线索版降级 → GetAwemeUsers", "来客授权");
                            var normalUsers = await LifeDataApi.GetAwemeUsers(cookieDict, groupId, lifeAccountId).ConfigureAwait(false);
                            awemeUsers = ConvertNormalUsers(normalUsers);
                        }
                        else
                        {
                            string msg2 = "未包含：『线索经营+线索数据/数据分析』也无『直播专业版+直播管理』";
                            FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 线索版但{msg2}，跳过", "来客授权");
                            result.FailedCompanyNames.Add($"{accountName}(线索版无直播菜单)");
                            WriteFailedCompanyToJson(secUidData, lifeAccountId, groupId, accountName, isClueVersion, msg2, authBatchId);
                            continue;
                        }
                    }
                    else
                    {
                        if (hasNormalMenu)
                        {
                            FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 普通版 → GetAwemeUsers", "来客授权");
                            var normalUsers = await LifeDataApi.GetAwemeUsers(cookieDict, groupId, lifeAccountId).ConfigureAwait(false);
                            awemeUsers = ConvertNormalUsers(normalUsers);
                        }
                        else
                        {
                            string msg3 = "普通版未包含：『直播专业版+直播管理』";
                            FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] {msg3}，跳过", "来客授权");
                            result.FailedCompanyNames.Add($"{accountName}(普通版无直播菜单)");
                            WriteFailedCompanyToJson(secUidData, lifeAccountId, groupId, accountName, isClueVersion, msg3, authBatchId);
                            continue;
                        }
                    }

                    if (awemeUsers == null || awemeUsers.Count == 0)
                    {
                        string msg4 = "接口未返回任何主播";
                        FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] {msg4}，跳过", "来客授权");
                        result.FailedCompanyNames.Add($"{accountName}(接口无主播)");
                        WriteFailedCompanyToJson(secUidData, lifeAccountId, groupId, accountName, isClueVersion, msg4, authBatchId);
                        continue;
                    }

                    FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 拿到 {awemeUsers.Count} 个主播", "来客授权");

                    // 3.4 反查本地主播缓存 → 填充 secUid / cookiePath / 生成 cookie 文件
                    int companyGenerated = 0;
                    int companySynced = 0;
                    foreach (var u in awemeUsers)
                    {
                        if (string.IsNullOrEmpty(u.AwemeUserId)) continue;

                        var matchedAnchor = allAnchors.FirstOrDefault(a =>
                            a.platform == 0 &&
                            !string.IsNullOrEmpty(a.AnchorUserId) &&
                            a.AnchorUserId == u.AwemeUserId);

                        if (matchedAnchor == null || string.IsNullOrEmpty(matchedAnchor.SecUid))
                        {
                            // 未录入系统 → secUid/cookiePath 留空
                            u.SecUid = "";
                            u.CookiePath = "";
                            continue;
                        }

                        // 填充 secUid
                        u.SecUid = matchedAnchor.SecUid;

                        // 计算 cookiePath（只存文件名）
                        string md5 = LifeUtils.getCachePathMd5(matchedAnchor.SecUid);
                        string cookieFileName = $"life-{md5}";
                        u.CookiePath = cookieFileName;

                        // 生成/覆盖 cookie 文件
                        try
                        {
                            string fullCookiePath = LifeUtils.getCookiePath(matchedAnchor.SecUid);
                            string dir = Path.GetDirectoryName(fullCookiePath);
                            if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                                Directory.CreateDirectory(dir);

                            var cookieFileDto = new LifeCookieFileDto
                            {
                                AwemeUserId = u.AwemeUserId,
                                SecUid = matchedAnchor.SecUid,
                                Cookies = mergedCookies
                            };
                            File.WriteAllText(fullCookiePath, cookieFileDto.ToJson(), System.Text.Encoding.UTF8);
                            companyGenerated++;
                            FileUtils.LogRpa($"[LifeAccountResolver] 已生成cookie: 主播【{matchedAnchor.AnchorName}】, file={cookieFileName}", "来客授权");
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"[LifeAccountResolver] 写cookie异常: {matchedAnchor.AnchorName}, {ex.Message}", "来客授权");
                        }

                        // 同步服务器授权状态
                        try
                        {
                            AnchorBll.UpdateLifeAuthStatus(matchedAnchor, (int)LifeAuthStatusEnum.auth);
                            companySynced++;
                        }
                        catch (Exception syncEx)
                        {
                            FileUtils.LogRpa($"[LifeAccountResolver] 同步授权状态异常: {matchedAnchor.AnchorName}, {syncEx.Message}", "来客授权");
                        }
                    }

                    // 3.5 组装 SecUid.json 新结构
                    // 找到或创建对应的 LifeAccountRootDto（按 life_account_id 分组）
                    var rootRecord = secUidData.FirstOrDefault(r => r.LifeAccountId == lifeAccountId);
                    if (rootRecord == null)
                    {
                        rootRecord = new LifeAccountRootDto { LifeAccountId = lifeAccountId, AuthBatchId = authBatchId };
                        secUidData.Add(rootRecord);
                    }
                    else
                    {
                        // 重新授权时更新批次ID
                        rootRecord.AuthBatchId = authBatchId;
                        FileUtils.LogRpa($"[LifeAccountResolver] 更新已有记录的 authBatchId: life_account_id={lifeAccountId}, authBatchId={authBatchId}", "来客授权");
                    }

                    // 找到或创建对应的 LifeCompanyDto（按 groupId）
                    var companyRecord = rootRecord.Accounts.FirstOrDefault(c => c.GroupId == groupId);
                    if (companyRecord == null)
                    {
                        companyRecord = new LifeCompanyDto { GroupId = groupId };
                        rootRecord.Accounts.Add(companyRecord);
                    }
                    companyRecord.AccountName = accountName;
                    companyRecord.Iscue = isClueVersion;
                    companyRecord.AwemeUsers = awemeUsers;

                    result.SuccessCompanyCount++;
                    result.GeneratedCookieCount += companyGenerated;
                    result.SyncedAnchorCount += companySynced;
                    FileUtils.LogRpa($"[LifeAccountResolver] [{accountName}] 完成: cookie={companyGenerated}, 同步={companySynced}", "来客授权");
                }

                // 4. 标记 status：当次授权的来客账号下的 UID 全部为 active，其他来客账号中同一 UID 设为 inactive
                var currentLifeAccountIds = new HashSet<string>(
                    companyList.Where(c => c.ContainsKey("life_account_id") && !string.IsNullOrEmpty(c["life_account_id"]))
                              .Select(c => c["life_account_id"]));
                FileUtils.LogRpa($"[LifeAccountResolver] MarkStatus 当前授权来客账号IDs: [{string.Join(",", currentLifeAccountIds)}]", "来客授权");
                MarkStatus(secUidData, currentLifeAccountIds);

                // 5. 写盘
                LifeUtils.SaveSecUidData(secUidData);

                FileUtils.LogRpa($"[LifeAccountResolver] 全部完成: 总公司={result.TotalCompanyCount}, 成功={result.SuccessCompanyCount}, cookie={result.GeneratedCookieCount}, 同步={result.SyncedAnchorCount}", "来客授权");
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"[LifeAccountResolver] 主流程异常: {ex.Message}\n{ex.StackTrace}", "来客授权");
                return result;
            }
        }

        /// <summary>
        /// 标记 status：
        /// - 当次授权的来客账号（currentLifeAccountIds）下的所有拖音号 → active
        /// - 其他来客账号中出现的同一 UID → inactive
        /// - 其他来客账号中独有的 UID（不重复）→ 保持原有 status 不变
        /// </summary>
        private static void MarkStatus(List<LifeAccountRootDto> data, HashSet<string> currentLifeAccountIds)
        {
            // 1) 收集当次授权来客账号下的所有 UID
            var currentActiveUids = new HashSet<string>();
            foreach (var root in data)
            {
                if (!currentLifeAccountIds.Contains(root.LifeAccountId ?? "")) continue;
                foreach (var company in root.Accounts ?? new List<LifeCompanyDto>())
                    foreach (var u in company.AwemeUsers ?? new List<LifeAwemeUserInfoDto>())
                        if (!string.IsNullOrEmpty(u.AwemeUserId))
                            currentActiveUids.Add(u.AwemeUserId);
            }

            // 2) 遍历所有记录标记
            foreach (var root in data)
            {
                if (root.Accounts == null) continue;
                bool isCurrent = currentLifeAccountIds.Contains(root.LifeAccountId ?? "");
                foreach (var company in root.Accounts)
                {
                    if (company.AwemeUsers == null) continue;
                    foreach (var u in company.AwemeUsers)
                    {
                        if (string.IsNullOrEmpty(u.AwemeUserId)) continue;
                        if (isCurrent)
                        {
                            // 当次授权的来客账号 → 全部 active
                            u.Status = "active";
                        }
                        else
                        {
                            // 其他来客账号：若该 UID 在当次授权里已标记 active → inactive
                            if (currentActiveUids.Contains(u.AwemeUserId))
                                u.Status = "inactive";
                            // 否则保持原有值（可能是之前授权时设的 active）
                            else if (string.IsNullOrEmpty(u.Status))
                                u.Status = "active";
                        }
                    }
                }
            }
        }

        /// <summary>
        /// 将不符合条件的公司也写入 SecUid.json（带 msg 字段说明原因，无 awemeUsers）。
        /// </summary>
        private static void WriteFailedCompanyToJson(List<LifeAccountRootDto> secUidData, string lifeAccountId, string groupId, string accountName, bool isClueVersion, string msg, string authBatchId)
        {
            var rootRecord = secUidData.FirstOrDefault(r => r.LifeAccountId == lifeAccountId);
            if (rootRecord == null)
            {
                rootRecord = new LifeAccountRootDto { LifeAccountId = lifeAccountId, AuthBatchId = authBatchId };
                secUidData.Add(rootRecord);
            }
            else
            {
                rootRecord.AuthBatchId = authBatchId;
            }

            var companyRecord = rootRecord.Accounts.FirstOrDefault(c => c.GroupId == groupId);
            if (companyRecord == null)
            {
                companyRecord = new LifeCompanyDto { GroupId = groupId };
                rootRecord.Accounts.Add(companyRecord);
            }
            companyRecord.AccountName = accountName;
            companyRecord.Iscue = isClueVersion;
            companyRecord.Msg = msg;
            // awemeUsers 保持为空（无法获取主播列表）
            if (companyRecord.AwemeUsers == null)
                companyRecord.AwemeUsers = new List<LifeAwemeUserInfoDto>();
        }

        /// <summary>
        /// 线索版接口返回 → LifeAwemeUserInfoDto
        /// 字段映射：douyinUID→awemeUserId, douyinNickname→nickname
        /// </summary>
        private static List<LifeAwemeUserInfoDto> ConvertClueUsers(List<Dictionary<string, string>> clueUsers)
        {
            var list = new List<LifeAwemeUserInfoDto>();
            if (clueUsers == null) return list;
            foreach (var u in clueUsers)
            {
                list.Add(new LifeAwemeUserInfoDto
                {
                    AwemeUserId = u.ContainsKey("douyinUID") ? u["douyinUID"] : "",
                    Nickname = u.ContainsKey("douyinNickname") ? u["douyinNickname"] : ""
                });
            }
            return list;
        }

        /// <summary>
        /// 普通版接口返回 → LifeAwemeUserInfoDto
        /// 字段映射：aweme_user_id→awemeUserId, nick_name→nickname
        /// </summary>
        private static List<LifeAwemeUserInfoDto> ConvertNormalUsers(List<Dictionary<string, string>> normalUsers)
        {
            var list = new List<LifeAwemeUserInfoDto>();
            if (normalUsers == null) return list;
            foreach (var u in normalUsers)
            {
                list.Add(new LifeAwemeUserInfoDto
                {
                    AwemeUserId = u.ContainsKey("aweme_user_id") ? u["aweme_user_id"] : "",
                    Nickname = u.ContainsKey("nick_name") ? u["nick_name"] : ""
                });
            }
            return list;
        }
    }

    /// <summary>
    /// ResolveAllAccountsAsync 执行结果统计。
    /// </summary>
    public class LifeAccountResolveResult
    {
        public int TotalCompanyCount { get; set; }
        public int SuccessCompanyCount { get; set; }
        public int GeneratedCookieCount { get; set; }
        public int SyncedAnchorCount { get; set; }
        public List<string> FailedCompanyNames { get; set; } = new List<string>();
        public bool AnySuccess => SuccessCompanyCount > 0;
    }
}
