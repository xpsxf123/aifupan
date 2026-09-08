using CefSharp;
using douyin.Utils;
using ReviewAnalysis.Model;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.enterprise
{
    /// <summary>
    /// 企业号工具类
    /// </summary>
    public class EnterpriseUtils
    {
        /// <summary>
        /// 企业号窗体列表 主播secuid-窗体信息
        /// </summary>
        public static volatile ConcurrentDictionary<string, EnterpriseForm> enterpriseFormList = new ConcurrentDictionary<string, EnterpriseForm>();

        /// <summary>
        /// 临时企业号窗体列表
        /// </summary>
        public static volatile ConcurrentDictionary<string, EnterpriseForm> tempEnterpriseFormList = new ConcurrentDictionary<string, EnterpriseForm>();

        /// <summary>
        /// 程序主窗体
        /// </summary>
        public static FormMain formMain;

        /// <summary>
        /// 是否正在授权中
        /// </summary>
        public static volatile bool authing;

        private static readonly object _formLock = new object();

        /// <summary>
        /// Cookie存储根目录
        /// </summary>
        private const string CookieRootPath = "enterprise\\cookies";

        /// <summary>
        /// 获取Cookie存储路径
        /// </summary>
        public static string getCookiePath(string secUid)
        {
            string directory = Path.Combine(Environment.CurrentDirectory, CookieRootPath);
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
            return Path.Combine(directory, $"enterprise-{GetMD5Hash(secUid)}.json");
        }

        /// <summary>
        /// 计算MD5哈希
        /// </summary>
        private static string GetMD5Hash(string input)
        {
            using (MD5 md5 = MD5.Create())
            {
                byte[] inputBytes = Encoding.UTF8.GetBytes(input);
                byte[] hashBytes = md5.ComputeHash(inputBytes);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hashBytes.Length; i++)
                {
                    sb.Append(hashBytes[i].ToString("x2"));
                }
                return sb.ToString();
            }
        }

        /// <summary>
        /// 打开授权页面或加载数据大屏页面
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间id</param>
        /// <param name="videoId">视频id</param>
        /// <param name="isTemp">是否临时加载</param>
        /// <param name="isFrontPull">是否是前端发起的拉取数据</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        /// <param name="bHide">是否隐藏窗体</param>
        public static void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide = false)
        {
            // 使用 Fire-and-Forget 模式在后台线程创建窗体，避免阻塞调用方
            _ = Task.Run(() =>
            {
                try
                {
                    // 确保在UI线程上创建和显示窗体
                    if (Form.ActiveForm != null && Form.ActiveForm.InvokeRequired)
                    {
                        Form.ActiveForm.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitEnterpriseForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
                        }));
                    }
                    else if (formMain != null && !formMain.IsDisposed && formMain.IsHandleCreated)
                    {
                        formMain.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitEnterpriseForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
                        }));
                    }
                    else
                    {
                        // 如果没有找到UI线程，使用Application.Run在新的UI线程上创建窗体
                        Thread uiThread = new Thread(() =>
                        {
                            EnterpriseForm enterpriseForm = new EnterpriseForm()
                            {
                                anchorInfo = anchorInfo,
                                isTemp = isTemp
                            };
                            enterpriseForm.Init(bHide);
                            Application.Run(enterpriseForm);
                        });
                        uiThread.SetApartmentState(ApartmentState.STA);
                        uiThread.Start();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"打开企业号授权窗体异常: {ex.Message}", "企业号授权");
                }
            }).ConfigureAwait(false);
        }

        /// <summary>
        /// 创建并初始化企业号窗体
        /// </summary>
        private static void CreateAndInitEnterpriseForm(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide)
        {
            try
            {
                if (isTemp)
                {
                    if (anchorInfo.enterpriseAuthStatus != (int)EnterpriseAuthStatusEnum.auth)
                    {
                        return;
                    }

                    EnterpriseForm enterpriseForm = new EnterpriseForm()
                    {
                        anchorInfo = anchorInfo,
                        isTemp = true,
                        addTempTime = ServerTimeUtils.getCurrentTime()
                    };
                    tempEnterpriseFormList.TryAdd(anchorInfo.SecUid, enterpriseForm);
                    enterpriseForm.Init(bHide);
                }
                else
                {
                    EnterpriseForm enterpriseForm = null;
                    foreach (var item in enterpriseFormList)
                    {
                        if (anchorInfo.SecUid.Equals(item.Value.anchorInfo.SecUid))
                        {
                            enterpriseForm = item.Value;
                            break;
                        }
                    }

                    if (enterpriseForm == null)
                    {
                        enterpriseForm = new EnterpriseForm()
                        {
                            anchorInfo = anchorInfo,
                            isTemp = false
                        };
                        enterpriseFormList.TryAdd(anchorInfo.SecUid, enterpriseForm);
                    }

                    enterpriseForm.Init(bHide);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建企业号窗体异常: {ex.Message}", "企业号授权");
            }
        }

        /// <summary>
        /// 检查授权状态
        /// </summary>
        public static int CheckAuthStatusByCookie(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return (int)EnterpriseAuthStatusEnum.unAuth;
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = Newtonsoft.Json.JsonConvert.DeserializeObject<List<EnterpriseCookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                {
                    return (int)EnterpriseAuthStatusEnum.unAuth;
                }

                var sessionCookie = cookies.Find(c => c.name == "sessionid");
                if (sessionCookie == null || string.IsNullOrEmpty(sessionCookie.value))
                {
                    return (int)EnterpriseAuthStatusEnum.unAuth;
                }

                // 检查是否过期
                if (sessionCookie.expires.HasValue && sessionCookie.expires.Value < DateTime.Now)
                {
                    return (int)EnterpriseAuthStatusEnum.authExpires;
                }

                return (int)EnterpriseAuthStatusEnum.auth;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查企业号授权状态异常: {ex.Message}", "企业号授权");
                return (int)EnterpriseAuthStatusEnum.unAuth;
            }
        }

        /// <summary>
        /// 清除Cookie
        /// </summary>
        public static void ClearCookies(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (File.Exists(cookiePath))
                {
                    File.Delete(cookiePath);
                    FileUtils.LogRpa($"已清除企业号Cookie: {cookiePath}", "企业号授权");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"清除企业号Cookie异常: {ex.Message}", "企业号授权");
            }
        }

        /// <summary>
        /// 删除主播缓存版本
        /// </summary>
        public static void deleteAnchorCacheVersion(string secUid)
        {
            try
            {
                // 从窗体列表中移除
                if (enterpriseFormList.ContainsKey(secUid))
                {
                    enterpriseFormList.TryRemove(secUid, out _);
                }
                if (tempEnterpriseFormList.ContainsKey(secUid))
                {
                    tempEnterpriseFormList.TryRemove(secUid, out _);
                }
                FileUtils.LogRpa($"已删除企业号主播缓存: {secUid}", "企业号授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"删除企业号主播缓存异常: {ex.Message}", "企业号授权");
            }
        }

        /// <summary>
        /// 重新统计企业号属性
        /// </summary>
        public static void recountEnterpriseProperty()
        {
            try
            {
                int authCount = 0;
                var anchors = AnchorCacheManager.GetAllAnchors();
                foreach (var anchor in anchors)
                {
                    if (anchor.enterpriseAuthStatus == 1)
                    {
                        authCount++;
                    }
                }
                FileUtils.LogRpa($"企业号授权统计: 已授权{authCount}个主播", "企业号授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"统计企业号属性异常: {ex.Message}", "企业号授权");
            }
        }
    }
}
