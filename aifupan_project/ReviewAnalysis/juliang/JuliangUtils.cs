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
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.enumeration.juliang;
using ReviewAnalysis.juliang.entity;
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

namespace ReviewAnalysis.juliang
{
    public class JuliangUtils
    {
        /// <summary>
        /// 巨量百应窗口列表  主播secuid-窗体信息
        /// </summary>
        public static volatile ConcurrentDictionary<string, JuliangForm> juliangFormList = new ConcurrentDictionary<string, JuliangForm>();
        /// <summary>
        /// 临时巨量百应窗体列表，如果超时还没关闭窗体，手动关闭窗体
        /// </summary>
        public static volatile ConcurrentDictionary<string, JuliangForm> tempJuliangFormList = new ConcurrentDictionary<string, JuliangForm>();
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
        public static void openAuthorizeOrDataScreen(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide = true)
        {
            JuliangForm juliangForm = null;
            CreateAndInitJuliangForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide, ref juliangForm);
            // 使用 Fire-and-Forget 模式在后台线程创建窗体，避免阻塞调用方
            // 窗体创建通过 BeginInvoke 异步提交到 UI 线程执行，不等待完成
            //
            //JuliangForm juliangForm = null;

            //if (formMain != null && !formMain.IsDisposed && formMain.IsHandleCreated)
            //{
            //    // 使用 BeginInvoke 异步提交到 UI 线程，不等待窗体创建完成
            //    formMain.BeginInvoke(new Action(() =>
            //    {
            //        CreateAndInitJuliangForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide, ref juliangForm);
            //    }));
            //}
            //else
            //{
            //    CreateAndInitJuliangForm(anchorInfo, roomId, videoId, isTemp, isFrontPull, authType, bHide, ref juliangForm);
            //}
        }

        /// <summary>
        /// 创建并初始化巨量窗体
        /// </summary>
        private static void CreateAndInitJuliangForm(AnchorInfo anchorInfo, string roomId, string videoId, bool isTemp, bool isFrontPull, int authType, bool bHide, ref JuliangForm juliangForm)
        {
            if(isTemp)
            {
                if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                {
                    return;
                }
                // 临时的，加载完后需要关闭
                juliangForm = new JuliangForm()
                {
                    anchorInfo = anchorInfo,
                    roomId = roomId,
                    videoId = videoId,
                    isTemp = true,
                    tempLoadDataFlagSet = new HashSet<string>(),
                    allowLoadDataScreen = true,
                    isFrontPull = isFrontPull,
                    authType = authType
                };
                juliangForm.init(bHide);
            }
            else
            {
                foreach (var item in juliangFormList)
                {
                    if (anchorInfo.SecUid.Equals(item.Value.anchorInfo.SecUid))
                    {
                        juliangForm = item.Value;
                        juliangForm.roomId = roomId;
                        juliangForm.videoId = videoId;
                        juliangForm.allowLoadDataScreen = true;
                        juliangForm.isFrontPull = isFrontPull;
                        juliangForm.authType = authType;
                        break;
                    }
                }

                if (juliangForm == null)
                {
                    juliangForm = new JuliangForm()
                    {
                        anchorInfo = anchorInfo,
                        roomId = roomId,
                        videoId = videoId,
                        isTemp = false,
                        allowLoadDataScreen = true,
                        isFrontPull = isFrontPull,
                        authType = authType
                    };

                }

                juliangForm.init(bHide);
                //int iAuthStatus = juliangForm.CheckAuthStatusByCookie();
                //if (iAuthStatus == JuliangAuthStatusEnum.auth)
                //{
                //    FileUtils.LogRpa($"本地Cookie检测到，授权登录==={anchorInfo?.AnchorName}");
                //    juliangForm.loadHomeFinishHandle(anchorInfo);
                //    return;
                //}
                //if (iAuthStatus == JuliangAuthStatusEnum.authExpires)
                //{
                //    FileUtils.LogRpa($"本地Cookie检测到，授权登录已过期==={anchorInfo?.AnchorName}");
                //    juliangForm.loginExpire(anchorInfo, JuliangAuthStatusEnum.authExpires);
                //}
            }
        }

        /// <summary>
        /// 数据监听
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        public static void OnDataCollectReached(object sender, DataCollectEventArgs data)
        {

            if(data == null)// || string.IsNullOrEmpty(data.batchNumber) || string.IsNullOrEmpty(data.videoId)
            {
                return;
            }

            try
            {
                //if (data.key.Equals(JuliangDataKeyTypeEnum.bigScreenDataBase))
                //{
                //    // 大屏数据-基础版

                //    // 写入实时数据
                //    JuliangDataHandle.writeRealTimeData(data);
                //    // 写入基础版的汇总数据
                //    JuliangDataHandle.writeGatherDataBase(data);
                //}
                //else if (data.key.Equals(JuliangDataKeyTypeEnum.bigScreenDataPro))
                //{
                //    // 大屏数据-专业版
                //    JuliangDataHandle.writeGatherDataPro(data);

                //}
                //else if (data.key.Equals(JuliangDataKeyTypeEnum.flowOrderSource))
                //{
                //    // 流量结构
                //    JuliangDataHandle.writeFlowSourceData(data);
                //}
                //else if (data.key.Equals(JuliangDataKeyTypeEnum.payUserPortrait))
                //{
                //    // 下单用户的用户画像
                //    JuliangDataHandle.writeUserPortrait(data, 0);
                //}
                //else if (data.key.Equals(JuliangDataKeyTypeEnum.watchUserPortrait))
                //{
                //    // 观看用户的用户画像
                //    JuliangDataHandle.writeUserPortrait(data, 1);
                //}

                // 如果是临时的，并且已加载完数据，关闭窗体
                JuliangForm juliangForm = data.juliangForm;
                if (juliangForm != null && juliangForm.isTemp)
                {
                    HashSet<string> set = juliangForm.tempLoadDataFlagSet;
                    if (set.Contains(JuliangDataKeyTypeEnum.bigScreenDataBase) && set.Contains(JuliangDataKeyTypeEnum.bigScreenDataPro) && set.Contains(JuliangDataKeyTypeEnum.flowOrderSource)
                        && set.Contains(JuliangDataKeyTypeEnum.payUserPortrait) && set.Contains(JuliangDataKeyTypeEnum.watchUserPortrait))
                    {
                        juliangForm.closeForm();
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"处理巨量数据发生异常==={JsonConvert.SerializeObject(data)}");
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

            if(douYinAnchorInfoEntity != null)
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
            return $"{cachePath}\\jlby-{md5Str}-{version}";
        }
        /// <summary>
        /// 获取主播授权的浏览器cookie路径
        /// </summary>
        /// <param name="secUid"></param>
        /// <returns></returns>
        public static string getCookiePath(string secUid)
        {
            string cachePath = Path.GetFullPath("dataCollect\\config");
            string md5Str = getCachePathMd5(secUid);
            return $"{cachePath}\\jlby-{md5Str}";
        }

        /// <summary>
        /// 通过Cookie检测本地是否有授权标识记录
        /// </summary>
        /// <returns></returns>
        public static int CheckAuthStatusByCookie(string secUid)
        {
            var localCookies = new CookiePersistenceHelper(getCookiePath(secUid)).LoadCefCookiesFromLocal(false);
            if (localCookies == null)
            {
                return -1;
            }

            MigrateCompassLuopanDt(localCookies, getCookiePath(secUid));
            var luopanDtCookie = localCookies?.FirstOrDefault(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
            if (luopanDtCookie == null || string.IsNullOrWhiteSpace(luopanDtCookie.Value))
            {
                return JuliangAuthStatusEnum.unAuth;
            }
            if (luopanDtCookie.Expires < DateTime.Now.AddHours(-1))
            {
                return JuliangAuthStatusEnum.authExpires;
            }
            else
                return JuliangAuthStatusEnum.auth;

        }
        
        public static void MigrateCompassLuopanDt(List<CefSharp.Cookie> localCookies, string cookiePath)
        {
            if (localCookies == null)
            {
                return;
            }   
            var luopanDt = localCookies.FirstOrDefault(c =>
                c.Name.Equals("LUOPAN_DT", StringComparison.Ordinal));
            if (luopanDt != null && !string.IsNullOrWhiteSpace(luopanDt.Value))
                return;

            var compassDt = localCookies.FirstOrDefault(c =>
                c.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
            if (compassDt == null || string.IsNullOrWhiteSpace(compassDt.Value))
                return;

            var newCookie = new CefSharp.Cookie
            {
                Name = "LUOPAN_DT",
                Value = compassDt.Value,
                Domain = compassDt.Domain,
                Path = compassDt.Path ?? "/",
                Expires = compassDt.Expires,
                Secure = compassDt.Secure,
                HttpOnly = compassDt.HttpOnly
            };
            localCookies.Add(newCookie);
            new CookiePersistenceHelper(cookiePath).SaveCefCookiesToLocal(localCookies, false);
            FileUtils.LogRpa("已将 COMPASS_LUOPAN_DT 迁移为 LUOPAN_DT", "Cookie迁移");
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
            string configPath = $"{configDirectoryPath}\\jlby-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt";
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
            string configPath = Path.GetFullPath($"Config\\jlby-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt");
            if (File.Exists(configPath))
            {
                string jsonString = File.ReadAllText(configPath);
                Dictionary<string, string> dictionary = JsonConvert.DeserializeObject<Dictionary<string, string>>(jsonString);
                dictionary.Remove(secUid);

                if (dictionary.Count > 0)
                {
                    File.WriteAllText(configPath, JsonConvert.SerializeObject(dictionary));
                }else
                {
                    File.Delete(configPath);
                }
                
            }
        }

        /// <summary>
        /// 获取主播授权配置信息
        /// </summary>
        /// <returns></returns>
        public static Dictionary<string, string> getJuliangAnchorCacheConfig()
        {
            string configPath = Path.GetFullPath($"Config\\jlby-{ReplayHttpUtils.UserId}-{ReplayHttpUtils.ActiveTenantId}.txt");
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
                juliangFormList.TryGetValue(secUid, out JuliangForm juliangForm);
                if (juliangForm != null)
                {
                    juliangFormList.TryRemove(secUid, out _);
                    juliangForm.closeForm();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"根据主播secUid关闭窗体发生异常==={secUid}");
            }
            
        }

        /// <summary>
        /// 开启定时关闭超时巨量临时窗体
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
                            if (tempJuliangFormList.Count > 0)
                            {
                                long currTime = ServerTimeUtils.getCurrentTime();
                                foreach (var item in tempJuliangFormList)
                                {
                                    JuliangForm juliangForm = item.Value;
                                    // 关闭打开时间超过40秒的临时窗体
                                    if (juliangForm != null && currTime - juliangForm.addTempTime > 40 * 1000)
                                    {
                                        juliangForm.closeForm();
                                    }
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"{ex}", $"定时关闭超时巨量临时窗体失败");
                        }
                                                    
                        Thread.Sleep(5000);
                    }
                });
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"开启定时关闭超时巨量临时窗体失败");
            }
        }

        /// <summary>
        /// 重新计算巨量百应的资产
        /// </summary>
        public static void recountJuliangProperty()
        {
            int useNumber = 0;
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllNotRemoveAnchors();
            if (anchorInfos != null && anchorInfos.Count > 0)
            {
                foreach (var item in anchorInfos)
                {
                    if (item.juliangAuthStatus == JuliangAuthStatusEnum.auth)
                    {
                        useNumber++;
                    }
                }
            }
            UserPropertyApi.updateRpaPropertyUseNum(useNumber);
        }

    }
}
