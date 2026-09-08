using CefSharp.Web;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Asr;
using ReviewAnalysis.api;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.life
{
    public class LifeUtils
    {
        public static volatile ConcurrentDictionary<string, LifeForm> lifeFormList = new ConcurrentDictionary<string, LifeForm>();
        public static volatile ConcurrentDictionary<string, LifeForm> tempLifeFormList = new ConcurrentDictionary<string, LifeForm>();
        public static FormMain formMain;
        public static volatile bool authing;

        /* 原 openAuthorizeOrDataScreen 方法已按业务要求保留注释：该版本只调度并初始化 LifeForm，未保证 bHide=false 时窗体可见。
        public static void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide = false)
        {
            _ = Task.Run(() =>
            {
                try
                {
                    if (Form.ActiveForm != null && Form.ActiveForm.InvokeRequired)
                    {
                        Form.ActiveForm.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitLifeForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
                        }));
                    }
                    else if (formMain != null && !formMain.IsDisposed && formMain.IsHandleCreated)
                    {
                        formMain.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitLifeForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
                        }));
                    }
                    else
                    {
                        Thread uiThread = new Thread(() =>
                        {
                            LifeForm lifeForm = new LifeForm()
                            {
                                anchorInfo = anchorInfo,
                                isTemp = isTemp
                            };
                            lifeForm.Init(bHide);
                            Application.Run(lifeForm);
                        });
                        uiThread.SetApartmentState(ApartmentState.STA);
                        uiThread.Start();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"打开来客授权窗体异常: {ex.Message}", "来客登录");
                }
            }).ConfigureAwait(false);
        }
        */

        public static void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide = false)
        {
            _ = Task.Run(() =>
            {
                try
                {
                    FileUtils.LogRpa($"来客授权调度开始: secUid={anchorInfo?.SecUid}, bHide={bHide}, Form.ActiveForm={Form.ActiveForm?.Name ?? "null"}, formMain={formMain?.Name ?? "null"}", "来客登录");

                    if (TryBeginInvoke(Form.ActiveForm, () => CreateAndInitLifeForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide)))
                    {
                        FileUtils.LogRpa("来客授权路径: 通过Form.ActiveForm.BeginInvoke调度到UI线程", "来客登录");
                        return;
                    }

                    if (TryBeginInvoke(formMain, () => CreateAndInitLifeForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide)))
                    {
                        FileUtils.LogRpa("来客授权路径: 通过formMain.BeginInvoke调度到UI线程", "来客登录");
                        return;
                    }

                    FileUtils.LogRpa("来客授权路径: Form.ActiveForm和formMain均不可用，创建新STA线程", "来客登录");
                    Thread uiThread = new Thread(() =>
                    {
                        LifeForm lifeForm = new LifeForm()
                        {
                            anchorInfo = anchorInfo,
                            isTemp = isTemp
                        };
                        lifeForm.Init(bHide);
                        Application.Run(lifeForm);
                    });
                    uiThread.SetApartmentState(ApartmentState.STA);
                    uiThread.Start();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"打开来客授权窗体异常: {ex.Message}", "来客登录");
                }
            }).ConfigureAwait(false);
        }

        private static bool TryBeginInvoke(Control control, Action action)
        {
            if (control == null || control.IsDisposed || !control.IsHandleCreated)
            {
                return false;
            }

            if (control.InvokeRequired)
            {
                control.BeginInvoke(action);
            }
            else
            {
                action();
            }

            return true;
        }

        /* 原 CreateAndInitLifeForm 方法已按业务要求保留注释：该版本只执行 Init(bHide)，未在 bHide=false 时显式 Show/Activate/BringToFront。
        private static void CreateAndInitLifeForm(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide)
        {
            try
            {
                if (isTemp)
                {
                    if (anchorInfo.lifeAuthStatus != 2)
                    {
                        return;
                    }
                    LifeForm lifeForm = new LifeForm()
                    {
                        anchorInfo = anchorInfo,
                        isTemp = true,
                        addTempTime = ServerTimeUtils.getCurrentTime()
                    };
                    tempLifeFormList.TryAdd(anchorInfo.SecUid, lifeForm);
                    lifeForm.Init(bHide);
                }
                else
                {
                    LifeForm lifeForm = null;
                    foreach (var item in lifeFormList)
                    {
                        if (anchorInfo.SecUid.Equals(item.Value.anchorInfo.SecUid))
                        {
                            lifeForm = item.Value;
                            break;
                        }
                    }

                    if (lifeForm == null)
                    {
                        lifeForm = new LifeForm()
                        {
                            anchorInfo = anchorInfo,
                            isTemp = false
                        };
                        lifeFormList.TryAdd(anchorInfo.SecUid, lifeForm);
                    }

                    lifeForm.Init(bHide);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建来客窗体异常: {ex.Message}", "来客登录");
            }
        }
        */

        private static void CreateAndInitLifeForm(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide)
        {
            try
            {
                if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
                {
                    FileUtils.LogRpa("创建来客窗体失败：主播信息为空", "来客登录");
                    return;
                }

                LifeForm lifeForm;
                if (isTemp)
                {
                    if (anchorInfo.lifeAuthStatus != 2)
                    {
                        return;
                    }

                    lifeForm = new LifeForm()
                    {
                        anchorInfo = anchorInfo,
                        isTemp = true,
                        addTempTime = ServerTimeUtils.getCurrentTime()
                    };
                    tempLifeFormList.TryAdd(anchorInfo.SecUid, lifeForm);
                }
                else
                {
                    lifeForm = null;
                    foreach (var item in lifeFormList)
                    {
                        if (anchorInfo.SecUid.Equals(item.Value.anchorInfo.SecUid))
                        {
                            lifeForm = item.Value;
                            break;
                        }
                    }

                    if (lifeForm == null || lifeForm.IsDisposed)
                    {
                        lifeForm = new LifeForm()
                        {
                            anchorInfo = anchorInfo,
                            isTemp = false
                        };
                        lifeFormList[anchorInfo.SecUid] = lifeForm;
                    }
                    else
                    {
                        lifeForm.anchorInfo = anchorInfo;
                        lifeForm.isTemp = false;
                    }
                }

                lifeForm.Init(bHide);
                ShowLifeFormIfNeeded(lifeForm, bHide);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建来客窗体异常: {ex.Message}", "来客登录");
            }
        }

        private static void ShowLifeFormIfNeeded(LifeForm lifeForm, bool bHide)
        {
            if (lifeForm == null || lifeForm.IsDisposed || bHide)
            {
                return;
            }

            if (!lifeForm.Visible)
            {
                lifeForm.Show();
            }

            if (lifeForm.WindowState == FormWindowState.Minimized)
            {
                lifeForm.WindowState = FormWindowState.Normal;
            }

            lifeForm.Activate();
            lifeForm.BringToFront();
        }

        public static string getCachePath(string secUid)
        {
            string version = getAnchorCacheVersion(secUid);
            string cachePath = Path.GetFullPath("googleCache");
            string md5Str = getCachePathMd5(secUid);
            return $"{cachePath}\\life-{md5Str}-{version}";
        }

        public static string getCookiePath(string secUid)
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = getCachePathMd5(secUid);
            return $"{cachePath}\\life-{md5Str}";
        }

        public static int CheckAuthStatusByCookie(string secUid)
        {
            var cookiePath = getCookiePath(secUid);
            if (!File.Exists(cookiePath))
            {
                return -1;
            }

            try
            {
                string cookieContent = File.ReadAllText(cookiePath);

                // 兼容新旧格式
                var (fileDto, isNewFormat) = LifeCookieFileDto.Parse(cookieContent);
                var cookies = fileDto.Cookies;

                if (cookies == null || cookies.Count == 0)
                {
                    // 双重兼容：旧格式直接反序列化
                    cookies = JsonConvert.DeserializeObject<List<LifeCookieDto>>(cookieContent);
                    if (cookies == null || cookies.Count == 0)
                    {
                        return (int)LifeAuthStatusEnum.unAuth;
                    }
                }

                var sessionIdCookie = cookies.FirstOrDefault(c => c.Name == "sessionid_ls" || c.Name == "sessionid_ss_ls");
                if (sessionIdCookie == null || string.IsNullOrWhiteSpace(sessionIdCookie.Value))
                {
                    return (int)LifeAuthStatusEnum.unAuth;
                }

                return (int)LifeAuthStatusEnum.auth;
            }
            catch
            {
                return (int)LifeAuthStatusEnum.unAuth;
            }
        }

        // -----------------------------------------------------------------
        // SecUid.json 全局关系映射文件 读写方法
        // -----------------------------------------------------------------

        /// <summary>
        /// 获取 SecUid.json 文件路径
        /// </summary>
        private static string GetSecUidJsonPath()
        {
            string configDir = Path.GetFullPath("dataCollect\\config");
            return Path.Combine(configDir, "SecUid.json");
        }

        /// <summary>
        /// 读取 SecUid.json 最终版结构：List&lt;LifeAccountRootDto&gt;。
        /// 兼容旧格式（LifeAccountMappingDto 数组 / LifeSecUidRootDto 对象）自动转换。
        /// 文件不存在时返回空列表。
        /// </summary>
        public static List<LifeAccountRootDto> LoadSecUidData()
        {
            try
            {
                string path = GetSecUidJsonPath();
                if (!File.Exists(path)) return new List<LifeAccountRootDto>();

                string content = File.ReadAllText(path);
                if (string.IsNullOrWhiteSpace(content)) return new List<LifeAccountRootDto>();

                var token = Newtonsoft.Json.Linq.JToken.Parse(content);
                if (token is Newtonsoft.Json.Linq.JArray arr)
                {
                    // 先尝试新格式
                    var newList = arr.ToObject<List<LifeAccountRootDto>>();
                    if (newList != null && newList.Count > 0 && newList[0].Accounts != null)
                        return newList;

                    // 回退到旧格式（LifeAccountMappingDto 数组）→ 转换
                    var oldList = arr.ToObject<List<LifeAccountMappingDto>>();
                    if (oldList != null && oldList.Count > 0)
                    {
                        FileUtils.LogRpa($"SecUid.json 检测到旧数组格式，转换中...共{oldList.Count}条", "来客映射");
                        return ConvertOldMappingsToNew(oldList);
                    }
                }
                else if (token is Newtonsoft.Json.Linq.JObject obj)
                {
                    // v2 格式（带 version/accounts/activeMap）→ 转换
                    var accounts = obj["accounts"]?.ToObject<List<LifeAccountMappingDto>>();
                    if (accounts != null && accounts.Count > 0)
                    {
                        FileUtils.LogRpa($"SecUid.json 检测到v2对象格式，转换中...共{accounts.Count}条", "来客映射");
                        return ConvertOldMappingsToNew(accounts);
                    }
                }

                return new List<LifeAccountRootDto>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"LoadSecUidData 异常: {ex.Message}", "来客映射");
                return new List<LifeAccountRootDto>();
            }
        }

        /// <summary>
        /// 保存 SecUid.json（最终版格式）。
        /// </summary>
        public static void SaveSecUidData(List<LifeAccountRootDto> data)
        {
            try
            {
                if (data == null) data = new List<LifeAccountRootDto>();
                string path = GetSecUidJsonPath();
                string dir = Path.GetDirectoryName(path);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                    Directory.CreateDirectory(dir);

                string json = JsonConvert.SerializeObject(data, Formatting.Indented);
                File.WriteAllText(path, json, System.Text.Encoding.UTF8);

                int totalUsers = data.Sum(d => d.Accounts?.Sum(a => a.AwemeUsers?.Count ?? 0) ?? 0);
                FileUtils.LogRpa($"SecUid.json 已保存，来客账号数={data.Count}, 总拖音号数={totalUsers}", "来客映射");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"SaveSecUidData 异常: {ex.Message}", "来客映射");
            }
        }

        /// <summary>
        /// 将旧版 LifeAccountMappingDto 列表转换为新版 LifeAccountRootDto 列表。
        /// 按 LifeAccountId 分组 → 每组一个 LifeAccountRootDto。
        /// </summary>
        private static List<LifeAccountRootDto> ConvertOldMappingsToNew(List<LifeAccountMappingDto> oldList)
        {
            var result = new List<LifeAccountRootDto>();
            var grouped = oldList.GroupBy(m => m.LifeAccountId ?? "");
            foreach (var group in grouped)
            {
                var root = new LifeAccountRootDto { LifeAccountId = group.Key };
                foreach (var old in group)
                {
                    var company = new LifeCompanyDto
                    {
                        GroupId = old.GroupId ?? "",
                        AccountName = old.AccountName ?? "",
                        Iscue = old.IsClueVersion
                    };
                    if (old.AwemeUsers != null)
                        company.AwemeUsers = old.AwemeUsers;
                    else if (old.OtherUid != null)
                        company.AwemeUsers = old.OtherUid.Select(uid => new LifeAwemeUserInfoDto { AwemeUserId = uid }).ToList();
                    root.Accounts.Add(company);
                }
                result.Add(root);
            }
            return result;
        }

        /// <summary>
        /// （旧接口兼容）读取 SecUid.json 返回 LifeAccountMappingDto 列表（供现有采集代码读取）。
        /// </summary>
        public static List<LifeAccountMappingDto> LoadSecUidMapping()
        {
            try
            {
                string path = GetSecUidJsonPath();
                if (!File.Exists(path)) return new List<LifeAccountMappingDto>();
                string content = File.ReadAllText(path);
                if (string.IsNullOrWhiteSpace(content)) return new List<LifeAccountMappingDto>();

                var token = Newtonsoft.Json.Linq.JToken.Parse(content);
                if (token is Newtonsoft.Json.Linq.JArray arr)
                {
                    // 尝试旧格式直接反序列化
                    var list = arr.ToObject<List<LifeAccountMappingDto>>();
                    if (list != null && list.Count > 0 && !string.IsNullOrEmpty(list[0].LifeAccountId))
                        return list;
                }
                else if (token is Newtonsoft.Json.Linq.JObject obj)
                {
                    var accounts = obj["accounts"]?.ToObject<List<LifeAccountMappingDto>>();
                    if (accounts != null) return accounts;
                }
                return new List<LifeAccountMappingDto>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"LoadSecUidMapping(旧接口) 异常: {ex.Message}", "来客映射");
                return new List<LifeAccountMappingDto>();
            }
        }

        /// <summary>
        /// （旧接口兼容）保存 SecUid.json。
        /// </summary>
        public static void SaveSecUidMapping(List<LifeAccountMappingDto> mappings)
        {
            try
            {
                string path = GetSecUidJsonPath();
                string dir = Path.GetDirectoryName(path);
                if (!string.IsNullOrEmpty(dir) && !Directory.Exists(dir))
                    Directory.CreateDirectory(dir);
                string json = JsonConvert.SerializeObject(mappings ?? new List<LifeAccountMappingDto>(), Formatting.Indented);
                File.WriteAllText(path, json, System.Text.Encoding.UTF8);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"SaveSecUidMapping(旧接口) 异常: {ex.Message}", "来客映射");
            }
        }

        /// <summary>
        /// （旧接口兼容）Upsert。
        /// </summary>
        public static void UpsertSecUidMapping(LifeAccountMappingDto newMapping)
        {
            var mappings = LoadSecUidMapping();
            var existing = mappings.FirstOrDefault(m =>
                (!string.IsNullOrEmpty(m.LifeAccountId) && m.LifeAccountId == newMapping.LifeAccountId) ||
                (!string.IsNullOrEmpty(m.SecUid) && m.SecUid == newMapping.SecUid));
            if (existing != null)
            {
                if (!string.IsNullOrEmpty(newMapping.GroupId)) existing.GroupId = newMapping.GroupId;
                if (!string.IsNullOrEmpty(newMapping.RootLifeAccountId)) existing.RootLifeAccountId = newMapping.RootLifeAccountId;
                if (!string.IsNullOrEmpty(newMapping.SecUid)) existing.SecUid = newMapping.SecUid;
                if (!string.IsNullOrEmpty(newMapping.AwemeUserId)) existing.AwemeUserId = newMapping.AwemeUserId;
                if (!string.IsNullOrEmpty(newMapping.LifeAccountId)) existing.LifeAccountId = newMapping.LifeAccountId;
                var uidSet = new HashSet<string>(existing.OtherUid ?? new List<string>());
                foreach (var uid in newMapping.OtherUid ?? new List<string>())
                    if (!string.IsNullOrEmpty(uid)) uidSet.Add(uid);
                existing.OtherUid = uidSet.ToList();
            }
            else
            {
                mappings.Add(newMapping);
            }
            SaveSecUidMapping(mappings);
        }

        /// <summary>
        /// （旧接口兼容）根据 LifeAccountId 查找。
        /// </summary>
        public static LifeAccountMappingDto FindSecUidMapping(string lifeAccountId)
        {
            var mappings = LoadSecUidMapping();
            return mappings.FirstOrDefault(m => m.LifeAccountId == lifeAccountId);
        }

        /// <summary>
        /// 兄弟 cookie 复用（S2/S3 场景）：
        /// 当本地不存在 targetSecUid 对应的 life-{md5} cookie 文件时，在 SecUid.json 里
        /// 找到相同商户下另一个已授权主播的 cookie 文件路径，供复制。
        /// </summary>
        public static string FindBrotherCookiePathByAweme(string targetSecUid)
        {
            if (string.IsNullOrEmpty(targetSecUid)) return null;
            try
            {
                // 1) 从本地主播缓存找 targetSecUid → AnchorUserId
                var allAnchors = DataCache.AnchorCacheManager.GetAllNotRemoveAnchors();
                var targetAnchor = allAnchors?.FirstOrDefault(a => a.platform == 0 && !string.IsNullOrEmpty(a.SecUid) && a.SecUid == targetSecUid);
                if (targetAnchor == null || string.IsNullOrEmpty(targetAnchor.AnchorUserId))
                {
                    FileUtils.LogRpa($"FindBrotherCookie 本地主播缓存未找到 secUid={targetSecUid} 或其 AnchorUserId 为空", "来客映射");
                    return null;
                }
                string targetAwemeUserId = targetAnchor.AnchorUserId;

                // 2) 从 SecUid.json 找到目标 UID 所在的 rootRecord，获取 authBatchId
                var data = LoadSecUidData();
                string targetBatchId = null;
                LifeAccountRootDto targetRoot = null;
                foreach (var root in data)
                {
                    if (root.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company.AwemeUsers != null && company.AwemeUsers.Any(u => u.AwemeUserId == targetAwemeUserId))
                        {
                            targetRoot = root;
                            targetBatchId = root.AuthBatchId;
                            break;
                        }
                    }
                    if (targetRoot != null) break;
                }
                if (targetRoot == null)
                {
                    FileUtils.LogRpa($"FindBrotherCookie SecUid.json 未找到 awemeUserId={targetAwemeUserId} 所属记录", "来客映射");
                    return null;
                }

                // 3) 确定搜索范围：
                //    - 有 authBatchId → 搜索所有相同 batchId 的来客账号下的所有主播（跨 life_account_id）
                //    - 无 authBatchId（旧数据）→ 只搜同一个 rootRecord 下的主播
                var brotherRoots = new List<LifeAccountRootDto>();
                if (!string.IsNullOrEmpty(targetBatchId))
                {
                    brotherRoots = data.Where(r => r.AuthBatchId == targetBatchId).ToList();
                    FileUtils.LogRpa($"FindBrotherCookie 按 authBatchId=[{targetBatchId}] 搜索，匹配到 {brotherRoots.Count} 个来客账号", "来客映射");
                }
                else
                {
                    brotherRoots.Add(targetRoot);
                    FileUtils.LogRpa($"FindBrotherCookie authBatchId为空（旧数据），只搜同一 life_account_id=[{targetRoot.LifeAccountId}]", "来客映射");
                }

                // 4) 遍历所有兄弟范围内的主播，找已有 cookie 文件的
                foreach (var root in brotherRoots)
                {
                    if (root.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company.AwemeUsers == null) continue;
                        foreach (var brother in company.AwemeUsers)
                        {
                            if (string.IsNullOrEmpty(brother.AwemeUserId) || brother.AwemeUserId == targetAwemeUserId) continue;
                            var brotherAnchor = allAnchors.FirstOrDefault(a => a.platform == 0 && a.AnchorUserId == brother.AwemeUserId);
                            if (brotherAnchor == null || string.IsNullOrEmpty(brotherAnchor.SecUid)) continue;
                            string brotherCookiePath = getCookiePath(brotherAnchor.SecUid);
                            if (File.Exists(brotherCookiePath))
                            {
                                FileUtils.LogRpa($"FindBrotherCookie 找到可用兄弟 cookie: brotherUid={brother.AwemeUserId}, 公司=[{company.AccountName}], path={brotherCookiePath}", "来客映射");
                                return brotherCookiePath;
                            }
                        }
                    }
                }

                FileUtils.LogRpa($"FindBrotherCookie 同批次内无已生成的兄弟 cookie, targetAwemeUserId={targetAwemeUserId}, batchId={targetBatchId ?? "null"}", "来客映射");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FindBrotherCookiePathByAweme 异常: {ex.Message}", "来客映射");
                return null;
            }
        }

        public static string getCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }

        /// <summary>
        /// 采集专用：根据 secUid 从 SecUid.json 定位到具体 awemeUsers[] 记录，
        /// 拼装出采集所需的完整上下文（groupId / lifeAccountId / awemeUserId / iscue 等）。
        /// 找不到返回 null，调用方自行決定是否回退到 API 兜底。
        /// </summary>
        /// <param name="secUid">目标主播 SecUid</param>
        /// <returns>上下文对象；找不到返回 null</returns>
        public static LifeAwemeUserContextDto FindAwemeUserContext(string secUid)
        {
            if (string.IsNullOrEmpty(secUid)) return null;
            try
            {
                var data = LoadSecUidData();
                if (data == null || data.Count == 0)
                {
                    FileUtils.LogRpa($"FindAwemeUserContext SecUid.json 为空，secUid={secUid}", "来客采集");
                    return null;
                }

                foreach (var root in data)
                {
                    if (root?.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company?.AwemeUsers == null) continue;
                        foreach (var u in company.AwemeUsers)
                        {
                            if (!string.IsNullOrEmpty(u.SecUid) && u.SecUid == secUid)
                            {
                                var ctx = new LifeAwemeUserContextDto
                                {
                                    SecUid = u.SecUid,
                                    AwemeUserId = u.AwemeUserId,
                                    GroupId = company.GroupId,
                                    LifeAccountId = root.LifeAccountId,
                                    AccountName = company.AccountName,
                                    IsClueVersion = company.Iscue,
                                    CookiePath = u.CookiePath,
                                    Status = u.Status,
                                    AuthBatchId = root.AuthBatchId
                                };
                                FileUtils.LogRpa($"FindAwemeUserContext 命中: secUid={secUid}, groupId={ctx.GroupId}, lifeAccountId={ctx.LifeAccountId}, awemeUserId={ctx.AwemeUserId}, iscue={ctx.IsClueVersion}, status={ctx.Status}, company=[{ctx.AccountName}]", "来客采集");
                                return ctx;
                            }
                        }
                    }
                }

                FileUtils.LogRpa($"FindAwemeUserContext SecUid.json 中未找到匹配 secUid={secUid} 的记录", "来客采集");
                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FindAwemeUserContext 异常: {ex.Message}", "来客采集");
                return null;
            }
        }

        /// <summary>
        /// 取消授权时清理 SecUid.json 中对应记录：将匹配到的 secUid/cookiePath 置空，status 设为 inactive。
        /// </summary>
        /// <param name="secUid">取消授权的主播 SecUid</param>
        public static void ClearAwemeUserInSecUidJson(string secUid)
        {
            if (string.IsNullOrEmpty(secUid)) return;
            try
            {
                var data = LoadSecUidData();
                bool changed = false;
                foreach (var root in data)
                {
                    if (root.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company.AwemeUsers == null) continue;
                        foreach (var u in company.AwemeUsers)
                        {
                            if (u.SecUid == secUid)
                            {
                                u.SecUid = "";
                                u.CookiePath = "";
                                u.Status = "inactive";
                                changed = true;
                                FileUtils.LogRpa($"ClearAwemeUserInSecUidJson 已清除: awemeUserId={u.AwemeUserId}, secUid={secUid}", "来客映射");
                            }
                        }
                    }
                }
                if (changed)
                {
                    SaveSecUidData(data);
                }
                else
                {
                    FileUtils.LogRpa($"ClearAwemeUserInSecUidJson 未找到匹配记录: secUid={secUid}", "来客映射");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"ClearAwemeUserInSecUidJson 异常: {ex.Message}", "来客映射");
            }
        }

        /// <summary>
        /// 根据 authBatchId 查找同批次的所有已授权兄弟 secUid（包含传入自身）。
        /// 同批次 = 同一次 cookie session，一个失效则全部失效。
        /// </summary>
        /// <param name="authBatchId">授权批次ID</param>
        /// <returns>同批次的所有 secUid 列表（仅包含 secUid 不为空的记录）</returns>
        public static List<string> FindSecUidsByAuthBatchId(string authBatchId)
        {
            var result = new List<string>();
            if (string.IsNullOrEmpty(authBatchId)) return result;
            try
            {
                var data = LoadSecUidData();
                foreach (var root in data)
                {
                    if (root?.AuthBatchId != authBatchId) continue;
                    if (root.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company?.AwemeUsers == null) continue;
                        foreach (var u in company.AwemeUsers)
                        {
                            if (!string.IsNullOrEmpty(u.SecUid) && !result.Contains(u.SecUid))
                                result.Add(u.SecUid);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"FindSecUidsByAuthBatchId 异常: {ex.Message}", "来客映射");
            }
            return result;
        }

        /// <summary>
        /// 删除本地 life-{md5} cookie 文件。文件不存在时无声失败。
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <returns>true=成功删除或文件本不存在；false=删除异常</returns>
        public static bool DeleteLocalCookieFile(string secUid)
        {
            if (string.IsNullOrEmpty(secUid)) return false;
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (File.Exists(cookiePath))
                {
                    File.Delete(cookiePath);
                    FileUtils.LogRpa($"DeleteLocalCookieFile 已删除: {cookiePath}", "来客映射");
                }
                else
                {
                    FileUtils.LogRpa($"DeleteLocalCookieFile 文件不存在（已被删除或从未写入）: {cookiePath}", "来客映射");
                }
                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"DeleteLocalCookieFile 异常: secUid={secUid}, {ex.Message}", "来客映射");
                return false;
            }
        }

        /// <summary>
        /// 授权成功后更新 SecUid.json 中指定主播的 secUid/cookiePath/status。
        /// 用于保证当前授权主播的 SecUid.json 与本地 cookie 文件一致，
        /// 即使 LifeAccountResolver 后续步骤失败也能保证当前主播信息正确。
        /// </summary>
        /// <param name="awemeUserId">拖音UID</param>
        /// <param name="secUid">主播 SecUid</param>
        /// <param name="cookieFileName">cookie 文件名（如 life-abc123）</param>
        public static void UpdateAwemeUserInSecUidJson(string awemeUserId, string secUid, string cookieFileName)
        {
            FileUtils.LogRpa($"UpdateAwemeUserInSecUidJson 调用: awemeUserId=[{awemeUserId ?? "null"}], secUid=[{(secUid?.Length > 20 ? secUid.Substring(0, 20) + "..." : secUid ?? "null")}], cookieFileName=[{cookieFileName ?? "null"}]", "来客映射");
            if (string.IsNullOrEmpty(awemeUserId) && string.IsNullOrEmpty(secUid))
            {
                FileUtils.LogRpa("UpdateAwemeUserInSecUidJson 两个匹配条件都为空，跳过", "来客映射");
                return;
            }
            try
            {
                var data = LoadSecUidData();
                if (data == null || data.Count == 0)
                {
                    FileUtils.LogRpa("UpdateAwemeUserInSecUidJson SecUid.json 数据为空，无法更新", "来客映射");
                    return;
                }

                bool changed = false;
                int totalChecked = 0;
                foreach (var root in data)
                {
                    if (root.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company.AwemeUsers == null) continue;
                        foreach (var u in company.AwemeUsers)
                        {
                            totalChecked++;
                            // 匹配条件：awemeUserId 相等，或 secUid 相等（且非空）
                            bool matchByUid = !string.IsNullOrEmpty(awemeUserId) && !string.IsNullOrEmpty(u.AwemeUserId) && u.AwemeUserId == awemeUserId;
                            bool matchBySecUid = !string.IsNullOrEmpty(secUid) && !string.IsNullOrEmpty(u.SecUid) && u.SecUid == secUid;
                            if (matchByUid || matchBySecUid)
                            {
                                FileUtils.LogRpa($"UpdateAwemeUserInSecUidJson 匹配成功: u.AwemeUserId=[{u.AwemeUserId}], matchByUid={matchByUid}, matchBySecUid={matchBySecUid}, 公司=[{company.AccountName}]", "来客映射");
                                u.SecUid = secUid ?? "";
                                u.CookiePath = cookieFileName ?? "";
                                u.Status = "active";
                                changed = true;
                            }
                        }
                    }
                }

                FileUtils.LogRpa($"UpdateAwemeUserInSecUidJson 检查完成: totalChecked={totalChecked}, changed={changed}", "来客映射");
                if (changed)
                {
                    SaveSecUidData(data);
                    FileUtils.LogRpa($"UpdateAwemeUserInSecUidJson 已写盘: awemeUserId={awemeUserId}, secUid={(secUid?.Length > 20 ? secUid.Substring(0, 20) + "..." : secUid)}, cookiePath={cookieFileName}", "来客映射");
                }
                else
                {
                    FileUtils.LogRpa($"UpdateAwemeUserInSecUidJson 未找到匹配记录! 检查 awemeUserId=[{awemeUserId}] 是否存在于 SecUid.json 的 awemeUsers[] 中", "来客映射");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"UpdateAwemeUserInSecUidJson 异常: {ex.Message}\n{ex.StackTrace}", "来客映射");
            }
        }

        /// <summary>
        /// 解绑校验发现"仍绑定但SecUid.json缺记录"时的自动补录（方案B）：
        /// 在指定 groupId 公司下确保存在该拖音号记录（无则新增，有则补全），
        /// 并回填 secUid/cookiePath/status=active，使后续采集可通过 FindAwemeUserContext 命中。
        /// </summary>
        /// <param name="groupId">命中的公司 groupId</param>
        /// <param name="awemeUserId">拖音UID</param>
        /// <param name="secUid">主播 SecUid</param>
        /// <param name="nickname">拖音昵称（新增条目时使用）</param>
        /// <returns>补录/更新成功返回 true；SecUid.json 中无该 groupId 公司或异常返回 false</returns>
        public static bool EnsureAwemeUserInSecUidJson(string groupId, string awemeUserId, string secUid, string nickname)
        {
            if (string.IsNullOrEmpty(groupId) || string.IsNullOrEmpty(awemeUserId) || string.IsNullOrEmpty(secUid))
            {
                FileUtils.LogRpa($"EnsureAwemeUserInSecUidJson 参数不全，跳过: groupId=[{groupId ?? "null"}], awemeUserId=[{awemeUserId ?? "null"}]", "来客映射");
                return false;
            }
            try
            {
                var data = LoadSecUidData();
                if (data == null || data.Count == 0)
                {
                    FileUtils.LogRpa("EnsureAwemeUserInSecUidJson SecUid.json 为空，无法补录", "来客映射");
                    return false;
                }

                string cookieFileName = $"life-{getCachePathMd5(secUid)}";
                foreach (var root in data)
                {
                    if (root?.Accounts == null) continue;
                    foreach (var company in root.Accounts)
                    {
                        if (company == null || company.GroupId != groupId) continue;

                        if (company.AwemeUsers == null) company.AwemeUsers = new List<LifeAwemeUserInfoDto>();
                        var u = company.AwemeUsers.FirstOrDefault(x => x != null && x.AwemeUserId == awemeUserId);
                        if (u == null)
                        {
                            u = new LifeAwemeUserInfoDto { AwemeUserId = awemeUserId, Nickname = nickname ?? "" };
                            company.AwemeUsers.Add(u);
                            FileUtils.LogRpa($"EnsureAwemeUserInSecUidJson 新增条目: awemeUserId={awemeUserId}, 公司=[{company.AccountName}]", "来客映射");
                        }
                        u.SecUid = secUid;
                        u.CookiePath = cookieFileName;
                        u.Status = "active";
                        SaveSecUidData(data);
                        FileUtils.LogRpa($"EnsureAwemeUserInSecUidJson 补录完成: awemeUserId={awemeUserId}, groupId={groupId}, cookiePath={cookieFileName}, 公司=[{company.AccountName}]", "来客映射");
                        return true;
                    }
                }

                FileUtils.LogRpa($"EnsureAwemeUserInSecUidJson 未找到 groupId={groupId} 的公司条目，无法补录", "来客映射");
                return false;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"EnsureAwemeUserInSecUidJson 异常: {ex.Message}", "来客映射");
                return false;
            }
        }

        public static string getAnchorCacheVersion(string secUid)
        {
            string configDirectoryPath = Path.GetFullPath("Config");
            if (!Directory.Exists(configDirectoryPath))
            {
                Directory.CreateDirectory(configDirectoryPath);
            }

            Dictionary<string, string> dictionary = new Dictionary<string, string>();
            string configPath = $"{configDirectoryPath}\\life-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt";
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
            }

            if (!dictionary.TryGetValue(secUid, out string version))
            {
                version = ServerTimeUtils.getCurrentTime().ToString();
                dictionary[secUid] = version;
                File.WriteAllText(configPath, JsonConvert.SerializeObject(dictionary));
            }

            return version;
        }

        public static void deleteAnchorCacheVersion(string secUid)
        {
            string configPath = Path.GetFullPath($"Config\\life-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt");
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                var dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
                dictionary.Remove(secUid);

                if (dictionary.Count > 0)
                {
                    File.WriteAllText(configPath, JsonConvert.SerializeObject(dictionary));
                }
                else
                {
                    File.Delete(configPath);
                }
            }
        }

        public static Dictionary<string, string> getLifeAnchorCacheConfig()
        {
            string configPath = Path.GetFullPath($"Config\\life-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt");
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                return JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
            }
            return null;
        }

        public static void closeFormBySecUid(string secUid)
        {
            try
            {
                lifeFormList.TryGetValue(secUid, out LifeForm lifeForm);
                if (lifeForm != null)
                {
                    lifeFormList.TryRemove(secUid, out _);
                    lifeForm.closeForm();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"根据主播secUid关闭来客窗体发生异常==={secUid}");
            }
        }

        public static void closeTimeoutTempForm()
        {
            Task.Run(() =>
            {
                while (true)
                {
                    try
                    {
                        if (tempLifeFormList.Count > 0)
                        {
                            long currTime = ServerTimeUtils.getCurrentTime();
                            foreach (var item in tempLifeFormList)
                            {
                                LifeForm lifeForm = item.Value;
                                if (lifeForm != null && currTime - lifeForm.addTempTime > 40 * 1000)
                                {
                                    lifeForm.closeForm();
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"{ex}", $"定时关闭超时来客临时窗体失败");
                    }

                    Thread.Sleep(5000);
                }
            });
        }

        public static void recountLifeProperty()
        {
            int useNumber = 0;
            var anchorInfos = DataCache.AnchorCacheManager.GetAllNotRemoveAnchors();
            if (anchorInfos != null && anchorInfos.Count > 0)
            {
                foreach (var item in anchorInfos)
                {
                    if (item.lifeAuthStatus == (int)LifeAuthStatusEnum.auth)
                    {
                        useNumber++;
                    }
                }
            }
            api.UserPropertyApi.updateRpaPropertyUseNum(useNumber);
        }

        public static async Task<List<Dictionary<string, string>>> GetLifeAccountList(string roomId, string secUid)
        {
            var cookies = LifeDataHandle.GetCookiesFromLocal(secUid);
            return await Task.FromResult(new List<Dictionary<string, string>>());
        }
    }
}