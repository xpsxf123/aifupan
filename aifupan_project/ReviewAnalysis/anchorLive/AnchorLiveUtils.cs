using douyin.Utils;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ReviewAnalysis.anchorLive
{
    /// <summary>
    /// 主播后台工具类
    /// </summary>
    public class AnchorLiveUtils
    {
        public static volatile ConcurrentDictionary<string, AnchorLiveForm> anchorLiveFormList = new ConcurrentDictionary<string, AnchorLiveForm>();
        public static FormMain formMain;
        public static volatile bool authing;

        /// <summary>
        /// 打开主播后台授权或数据页面
        /// </summary>
        public static void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide = false)
        {
            _ = Task.Run(() =>
            {
                try
                {
                    if (formMain != null && !formMain.IsDisposed && formMain.IsHandleCreated)
                    {
                        formMain.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitAnchorLiveForm(anchorInfo, bHide);
                        }));
                    }
                    else if (Form.ActiveForm != null && Form.ActiveForm.InvokeRequired)
                    {
                        Form.ActiveForm.BeginInvoke(new Action(() =>
                        {
                            CreateAndInitAnchorLiveForm(anchorInfo, bHide);
                        }));
                    }
                    else
                    {
                        Thread uiThread = new Thread(() =>
                        {
                            try
                            {
                                AnchorLiveForm anchorLiveForm = new AnchorLiveForm()
                                {
                                    anchorInfo = anchorInfo
                                };
                                anchorLiveForm.Init(bHide);
                                Application.Run(anchorLiveForm);
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogRpa($"主播后台STA线程异常: {ex.Message}\n{ex.StackTrace}", "主播后台登录");
                                authing = false;
                            }
                        });
                        uiThread.SetApartmentState(ApartmentState.STA);
                        uiThread.Start();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"打开主播后台授权窗体异常: {ex.Message}\n{ex.StackTrace}", "主播后台登录");
                    authing = false;
                }
            }).ConfigureAwait(false);
        }

        /// <summary>
        /// 创建并初始化主播后台窗体
        /// </summary>
        private static void CreateAndInitAnchorLiveForm(AnchorInfo anchorInfo, bool bHide)
        {
            try
            {
                AnchorLiveForm anchorLiveForm = new AnchorLiveForm()
                {
                    anchorInfo = anchorInfo
                };
                anchorLiveForm.Init(bHide);
                anchorLiveForm.Show();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"创建主播后台窗体异常: {ex.Message}", "主播后台登录");
                authing = false;
            }
        }

        /// <summary>
        /// 获取 Cookie 文件路径
        /// </summary>
        public static string getCookiePath(string secUid)
        {
            return AnchorLiveDataHandle.getCookiePath(secUid);
        }

        /// <summary>
        /// 检查 Cookie 是否有效
        /// </summary>
        public static bool CheckCookieValid(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                
                if (!File.Exists(cookiePath))
                    return false;

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = Newtonsoft.Json.JsonConvert.DeserializeObject<List<AnchorLiveCookieDto>>(cookieContent);
                
                if (cookies == null || cookies.Count == 0)
                    return false;

                // 检查是否有 sessionid
                var sessionCookie = cookies.FirstOrDefault(c => 
                    c.Name.Equals("sessionid", StringComparison.OrdinalIgnoreCase));

                if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
                    return false;

                // 检查是否过期
                if (sessionCookie.Expires.HasValue && sessionCookie.Expires.Value < DateTime.Now)
                    return false;

                return true;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 通过 Cookie 检测授权状态
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>0=未授权, 1=已授权, 2=授权过期, -1=检测失败</returns>
        public static int CheckAuthStatusByCookie(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);

                if (!File.Exists(cookiePath))
                    return (int)AnchorLiveAuthStatusEnum.unAuth;

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = Newtonsoft.Json.JsonConvert.DeserializeObject<List<AnchorLiveCookieDto>>(cookieContent);

                if (cookies == null || cookies.Count == 0)
                    return (int)AnchorLiveAuthStatusEnum.unAuth;

                // 检查是否有 sessionid
                var sessionCookie = cookies.FirstOrDefault(c =>
                    c.Name.Equals("sessionid", StringComparison.OrdinalIgnoreCase));

                if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
                    return (int)AnchorLiveAuthStatusEnum.unAuth;

                // 检查是否过期
                if (sessionCookie.Expires.HasValue && sessionCookie.Expires.Value < DateTime.Now)
                    return (int)AnchorLiveAuthStatusEnum.authExpires;

                return (int)AnchorLiveAuthStatusEnum.auth;
            }
            catch
            {
                return -1; // 检测失败
            }
        }

        /// <summary>
        /// 更新主播授权状态到缓存
        /// </summary>
        public static void UpdateAnchorAuthStatus(AnchorInfo anchorInfo, AnchorLiveAuthStatusEnum status)
        {
            try
            {
                AnchorCacheManager.SetAnchorCache(anchorInfo);
                FileUtils.LogRpa($"主播【{anchorInfo.AnchorName}】授权状态更新为：{status}", "主播后台工具");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"更新主播授权状态异常：{ex.Message}", "主播后台工具");
            }
        }
    }
}
