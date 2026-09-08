using CefSharp.Web;
using douyin.Utils;
using juliang;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.Bll.Anchor.Entity;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using System.Windows.Forms;

namespace ReviewAnalysis.qianchuan
{
    public class QianchuanUtils
    {
        /// <summary>
        /// 千川窗口列表  主播secuid-窗体信息
        /// </summary>
        public static volatile ConcurrentDictionary<string, QianchuanForm> qianchuanFormList = new ConcurrentDictionary<string, QianchuanForm>();
        /// <summary>
        /// 临时千川窗体列表，如果超时还没关闭窗体，手动关闭窗体
        /// </summary>
        public static volatile ConcurrentDictionary<string, QianchuanForm> tempQianchuanFormList = new ConcurrentDictionary<string, QianchuanForm>();
        /// <summary>
        /// 程序主窗体
        /// </summary>
        public static FormMain formMain;
        /// <summary>
        /// 是否正在授权中
        /// </summary>
        public static volatile bool authing;


        /// <summary>
        /// 打开授权页面或加载数据大屏页面
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间id</param>
        /// <param name="videoId">视频id</param>
        /// <param name="isTemp">是否临时加载，如果是临时的，同时需要加载专业版，并且加载完后需要关闭</param>
        /// <param name="isFrontPull">是否是前端发起的拉取数据，如果是，在获取完数据窗体关闭前回调给前端</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        public static void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide = false)
        {
            // 同步创建并模态显示窗体
            if (Form.ActiveForm != null && Form.ActiveForm.InvokeRequired)
            {
                Form.ActiveForm.Invoke(new Action(() =>
                {
                    CreateAndShowQianchuanFormModal(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
                }));
            }
            else if (formMain != null && !formMain.IsDisposed && formMain.IsHandleCreated)
            {
                formMain.Invoke(new Action(() =>
                {
                    CreateAndShowQianchuanFormModal(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
                }));
            }
            else
            {
                // 在主线程上直接创建（如果已经在UI线程）
                CreateAndShowQianchuanFormModal(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide);
            }
        }

        /// <summary>
        /// 创建并模态显示千川窗体
        /// </summary>
        private static void CreateAndShowQianchuanFormModal(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide)
        {
            QianchuanForm qianchuanForm = null;

            if (isTemp)
            {
                if (anchorInfo.qianchuanAuthStatus != 2)
                {
                    return;
                }

                qianchuanForm = new QianchuanForm()
                {
                    anchorInfo = anchorInfo,
                    isTemp = true,
                    addTempTime = ServerTimeUtils.getCurrentTime()
                };
                tempQianchuanFormList.TryAdd(anchorInfo.SecUid, qianchuanForm);
            }
            else
            {
                // 查找现有窗体
                foreach (var item in qianchuanFormList)
                {
                    if (anchorInfo.SecUid.Equals(item.Value.anchorInfo.SecUid))
                    {
                        qianchuanForm = item.Value;
                        break;
                    }
                }

                if (qianchuanForm == null)
                {
                    // 重新授权前，删除旧的千川 Cookie 文件和 aavid 文件，避免旧 Cookie 导致浏览器自动跳转
                    DeleteQianchuanCookieFile(anchorInfo.SecUid);

                    qianchuanForm = new QianchuanForm()
                    {
                        anchorInfo = anchorInfo,
                        isTemp = false
                    };
                    qianchuanFormList.TryAdd(anchorInfo.SecUid, qianchuanForm);
                }
            }

            // 先初始化再模态显示（Init方法中已设置窗体属性）
            try
            {
                qianchuanForm.Init(bHide);
                
                // 如果已授权，最大化窗体
                if (anchorInfo != null && anchorInfo.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.auth)
                {
                    qianchuanForm.WindowState = FormWindowState.Maximized;
                    FileUtils.LogRpa("主播已授权，窗体最大化显示", "千川登录");
                }
                
                qianchuanForm.ShowDialog();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"千川窗体初始化失败: {ex.Message}", "千川登录");
                // 重置全局授权状态，允许下次授权
                QianchuanUtils.authing = false;
                // 窗体可能已被释放，不需要再关闭
            }
        }

        /// <summary>
        /// 获取主播直播间id
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <returns></returns>
        public static string GetAnchorBatchNum(AnchorInfo anchorInfo)
        {
            DouYinAnchorInfoEntity douYinAnchorInfoEntity = DouYinAnchorBll.GetDouYinAnchorInfoSync(anchorInfo.LiveUrl);

            if (douYinAnchorInfoEntity != null)
            {
                return douYinAnchorInfoEntity.RoomId;
            }

            return null;
        }

        /// <summary>
        /// 获取主播授权的浏览器缓存路径
        /// </summary>
        /// <param name="anchorUserId">主播secUid</param>
        /// <returns></returns>
        public static string getCachePath(string secUid)
        {
            string version = getAnchorCacheVersion(secUid);

            string cachePath = Path.GetFullPath("googleCache");
            string md5Str = getCachePathMd5(secUid);
            return $"{cachePath}\\qianchuan-{md5Str}-{version}";
        }

        /// <summary>
        /// 根据Cookie列表判断千川登录状态
        /// </summary>
        /// <param name="cookies">Cookie列表</param>
        /// <returns>QianchuanAuthStatusEnum 值</returns>
        public static int GetAuthStatusByCookies(List<CookieDto> cookies)
        {
            try
            {
                if (cookies == null || cookies.Count == 0)
                {
                    return (int)QianchuanAuthStatusEnum.unAuth;
                }

                //// 1. 检查 COMPASS_LUOPAN_DT Cookie
                //var luopanDtCookie = cookies.FirstOrDefault(c => c.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
                //if (luopanDtCookie == null || string.IsNullOrWhiteSpace(luopanDtCookie.Value))
                //{
                //    return (int)QianchuanAuthStatusEnum.unAuth;
                //}
                //if (luopanDtCookie.Expires != DateTime.MinValue && luopanDtCookie.Expires < DateTime.Now.AddHours(-1))
                //{
                //    return (int)QianchuanAuthStatusEnum.authExpires;
                //}

                // 2. 检查 sessionid / sid_guard 作为辅助判断
                var sessionCookie = cookies.FirstOrDefault(c =>
                    c.Name.Equals("sessionid", StringComparison.OrdinalIgnoreCase) ||
                    c.Name.Equals("sid_guard", StringComparison.OrdinalIgnoreCase));
                if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
                {
                    // COMPASS_LUOPAN_DT 存在但 sessionid 不存在，视为过期
                    return (int)QianchuanAuthStatusEnum.authExpires;
                }
                if (sessionCookie.Expires != DateTime.MinValue && sessionCookie.Expires < DateTime.Now)
                {
                    return (int)QianchuanAuthStatusEnum.authExpires;
                }

                return (int)QianchuanAuthStatusEnum.auth;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"根据Cookie判断千川登录状态异常：{ex.Message}", "千川工具");
                return (int)QianchuanAuthStatusEnum.unAuth;
            }
        }

        /// <summary>
        /// 通过Cookie检测本地是否有授权标识记录
        /// </summary>
        /// <returns></returns>
        public static int CheckAuthStatusByCookie(string secUid)
        {
            try
            {
                string cookiePath = getCookiePath(secUid);
                if (string.IsNullOrEmpty(cookiePath) || !File.Exists(cookiePath))
                {
                    return (int)QianchuanAuthStatusEnum.unAuth;
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var cookies = JsonConvert.DeserializeObject<List<CookieDto>>(cookieContent);
                if (cookies == null || cookies.Count == 0)
                {
                    return (int)QianchuanAuthStatusEnum.unAuth;
                }

                int status = GetAuthStatusByCookies(cookies);

                // Cookie有效但qcaavid缺失视为未授权
                if (status == (int)QianchuanAuthStatusEnum.auth && !CheckQcaavidValid(secUid))
                {
                    FileUtils.LogRpa($"千川Cookie有效但qcaavid缺失，视为未授权: {secUid}", "千川工具");
                    return (int)QianchuanAuthStatusEnum.unAuth;
                }

                return status;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检测千川Cookie授权状态异常：{ex.Message}", "千川工具");
                return (int)QianchuanAuthStatusEnum.unAuth;
            }
        }

        /// <summary>
        /// 获取主播缓存文件夹的MD5
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public static string getCachePathMd5(string secUid)
        {
            return MD5Utils.create($"{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}-{secUid}");
        }

        /// <summary>
        /// 获取主播授权的缓存版本号
        /// </summary>
        /// <param name="anchorUserId">主播secUid</param>
        /// <returns></returns>
        public static string getAnchorCacheVersion(string secUid)
        {
            // 获取配置文件目录，不存在则创建
            string configDirectoryPath = Path.GetFullPath("Config");
            if (!Directory.Exists(configDirectoryPath))
            {
                Directory.CreateDirectory(configDirectoryPath);
            }

            // secUid-时间戳
            Dictionary<string, string> dictionary = new Dictionary<string, string>();
            string configPath = $"{configDirectoryPath}\\qianchuan-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt";
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
            }

            // 获取版本号，如果没有则创建
            if (!dictionary.TryGetValue(secUid, out string version))
            {
                version = ServerTimeUtils.getCurrentTime().ToString();
                dictionary[secUid] = version;
                File.WriteAllText(configPath, JsonConvert.SerializeObject(dictionary));
            }

            return version;
        }

        /// <summary>
        /// 删除主播授权的缓存版本号
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public static void deleteAnchorCacheVersion(string secUid)
        {
            string configPath = Path.GetFullPath($"Config\\qianchuan-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt");
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                Dictionary<string, string> dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
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

        /// <summary>
        /// 获取主播授权配置信息
        /// </summary>
        /// <returns></returns>
        public static Dictionary<string, string> getQianchuanAnchorCacheConfig()
        {
            string configPath = Path.GetFullPath($"Config\\qianchuan-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt");
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                return JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
            }
            return null;
        }

        /// <summary>
        /// 根据主播secUid关闭窗体
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        public static void closeFormBySecUid(string secUid)
        {
            try
            {
                qianchuanFormList.TryGetValue(secUid, out QianchuanForm qianchuanForm);
                if (qianchuanForm != null)
                {
                    qianchuanFormList.TryRemove(secUid, out _);
                    qianchuanForm.closeForm();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"根据主播secUid关闭窗体发生异常==={secUid}");
            }

        }

        /// <summary>
        /// 开启定时关闭超时千川临时窗体
        /// </summary>
        public static void closeTimeoutTempForm()
        {
            try
            {
                Task.Run(() => {
                    while (true)
                    {
                        try
                        {
                            if (tempQianchuanFormList.Count > 0)
                            {
                                long currTime = ServerTimeUtils.getCurrentTime();
                                foreach (var item in tempQianchuanFormList)
                                {
                                    QianchuanForm qianchuanForm = item.Value;
                                    // 关闭打开时间超过40秒的临时窗体
                                    if (qianchuanForm != null && currTime - qianchuanForm.addTempTime > 40 * 1000)
                                    {
                                        qianchuanForm.closeForm();
                                    }
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"{ex}", $"定时关闭超时千川临时窗体失败");
                        }

                        Thread.Sleep(5000);
                    }
                });
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"开启定时关闭超时千川临时窗体失败");
            }
        }

        /// <summary>
        /// 重新计算千川的资产
        /// </summary>
        public static void recountQianchuanProperty()
        {
            int useNumber = 0;
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllNotRemoveAnchors();
            if (anchorInfos != null && anchorInfos.Count > 0)
            {
                foreach (var item in anchorInfos)
                {
                    if (item.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.auth)
                    {
                        useNumber++;
                    }
                }
            }
            UserPropertyApi.updateRpaPropertyUseNum(useNumber);
        }

        /// <summary>
        /// 拉取千川数据
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="aavid">千川账户ID</param>
        /// <param name="anchorId">主播ID</param>
        public static async Task PullQianchuanData(AnchorInfo anchorInfo, string roomId, string aavid, string anchorId)
        {
            await QianchuanDataHandle.PullQianchuanData(anchorInfo, roomId, aavid, anchorId);
        }

        /// <summary>
        /// 获取千川账户列表
        /// </summary>
        /// <param name="roomId">直播间 ID</param>
        /// <param name="secUid">主播 SecUid</param>
        /// <returns>账户列表</returns>
        public static async Task<List<Dictionary<string, string>>> GetQianchuanAccountList(string roomId, string secUid)
        {
            var cookies = QianchuanDataHandle.GetCookiesFromLocal(secUid);
            return await QianchuanDataApi.GetAccountUserList(roomId, cookies, secUid);
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
                var cookies = JsonConvert.DeserializeObject<List<CookieDto>>(cookieContent);
                
                if (cookies == null || cookies.Count == 0)
                    return false;

                // 检查关键 Cookie（sessionid 或 sid_guard）
                var sessionCookie = cookies.FirstOrDefault(c => 
                    c.Name.Equals("sessionid", StringComparison.OrdinalIgnoreCase) ||
                    c.Name.Equals("sid_guard", StringComparison.OrdinalIgnoreCase));

                if (sessionCookie == null || string.IsNullOrWhiteSpace(sessionCookie.Value))
                    return false;

                // 检查是否过期（DateTime.MinValue 表示无过期时间）
                if (sessionCookie.Expires != DateTime.MinValue && sessionCookie.Expires < DateTime.Now)
                    return false;

                // 检查 qcaavid 是否有效（Cookie有效但qcaavid缺失视为未授权）
                if (!CheckQcaavidValid(secUid))
                    return false;

                return true;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// 检查 qcaavid 是否有效（文件存在且非空）
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <returns>true=有效，false=缺失或为空</returns>
        public static bool CheckQcaavidValid(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = getCachePathMd5(secUid);
                string aavidPath = Path.Combine(cachePath, $"qcaavid-{md5Str}");

                if (!File.Exists(aavidPath))
                {
                    FileUtils.LogRpa($"千川qcaavid文件不存在: {aavidPath}", "千川工具");
                    return false;
                }

                string aavid = File.ReadAllText(aavidPath).Trim();
                if (string.IsNullOrEmpty(aavid))
                {
                    FileUtils.LogRpa($"千川qcaavid文件为空: {aavidPath}", "千川工具");
                    return false;
                }

                return true;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"检查千川qcaavid有效性异常：{ex.Message}", "千川工具");
                return false;
            }
        }

        /// <summary>
        /// 获取千川独立的 Cookie 文件路径（不与巨量共用）
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <returns>Cookie 文件路径</returns>
        public static string getCookiePath(string secUid)
        {
            try
            {
                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = getCachePathMd5(secUid);
                return $"{cachePath}\\qianchuan-{md5Str}";
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"获取千川 Cookie 路径异常：{ex.Message}", "千川工具");
                return null;
            }
        }

        /// <summary>
        /// 将主播的千川授权状态更新为指定状态，并清理授权文件
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <param name="authStatus">目标授权状态，默认为授权过期</param>
        public static void HandleAuthExpired(string secUid, int authStatus = (int)QianchuanAuthStatusEnum.authExpires)
        {
            if (string.IsNullOrEmpty(secUid)) return;

            try
            {
                var anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null) return;

                // 1. 修改主播缓存中的授权状态并同步服务端
                AnchorBll.UpdateQianchuanAuthStatus(anchorInfo, authStatus);

                // 2. 删除千川的授权文件
                deleteAnchorCacheVersion(secUid);
                DeleteQianchuanCookieFile(secUid);

                FileUtils.LogRpa($"千川授权状态已更新，主播【{anchorInfo.AnchorName}】，状态: {authStatus}", "千川授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"处理千川授权状态异常: {ex.Message}", "千川授权");
            }
        }

        /// <summary>
        /// 删除千川 Cookie 文件和 aavid 文件（重新授权前清理旧文件）
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        public static void DeleteQianchuanCookieFile(string secUid)
        {
            try
            {
                // 删除千川 Cookie 文件
                string cookiePath = getCookiePath(secUid);
                if (!string.IsNullOrEmpty(cookiePath) && File.Exists(cookiePath))
                {
                    File.Delete(cookiePath);
                    FileUtils.LogRpa($"已删除旧千川 Cookie 文件：{cookiePath}", "千川登录");
                }

                // 删除 aavid 文件
                string cachePath = Path.GetFullPath("dataCollect\\config");
                string md5Str = getCachePathMd5(secUid);
                string aavidPath = Path.Combine(cachePath, $"qcaavid-{md5Str}");
                if (!string.IsNullOrEmpty(aavidPath) && File.Exists(aavidPath))
                {
                    File.Delete(aavidPath);
                    FileUtils.LogRpa($"已删除旧千川 aavid 文件：{aavidPath}", "千川登录");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"删除千川 Cookie 文件异常: {ex.Message}", "千川登录");
            }
        }

    }
}
