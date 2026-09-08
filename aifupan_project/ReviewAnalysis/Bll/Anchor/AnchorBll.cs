using CefSharp;
using CefSharp.DevTools.Target;
using CefSharp.WinForms;
using douyin.Utils;
using juliang;
using MediaInfo;
using Newtonsoft.Json;
using ReviewAnalysis.anchorLive;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor.Entity;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.anchor;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.juliang;
using ReviewAnalysis.Model;
using ReviewAnalysis.plugins.adapters;
using ReviewAnalysis.plugins.core;
using ReviewAnalysis.plugins.dataPullers;
using ReviewAnalysis.ShortVideo.DouYin;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.anchor;
using ReviewAnalysis.vo.trade;
using ReviewAnalysis.WeChatChannels.Services;
using ReviewAnalysis.WeChatChannels.Utils;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Security;
using System.Security.Policy;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Windows.Input;
using ReviewAnalysis.socketAddress.browser;
using ReviewAnalysis.qianchuan;
using ReviewAnalysis.life;
using ReviewAnalysis.juliangApi;
using ReviewAnalysis.enterprise;

namespace ReviewAnalysis.Bll.Anchor
{
    public class AnchorBll
    {

        /// <summary>
        /// 未开始录制的主播列表
        /// </summary>
        public static ConcurrentDictionary<string, AnchorRecordBll> unRecordList = new ConcurrentDictionary<string, AnchorRecordBll>();
        /// <summary>
        /// 已开始录制的主播列表
        /// </summary>
        public static ConcurrentDictionary<string, AnchorRecordBll> recordingList = new ConcurrentDictionary<string, AnchorRecordBll>();
        /// <summary>
        /// 本次开启检测录制时的配置信息
        /// </summary>
        public static Config config;
        /// <summary>
        /// 本次开启检测录制时的视频类型 video_type 0 ts,1 flv,2 mp4 
        /// </summary>
        public static int RecordVideoType;
        /// <summary>
        /// 本次开启检测录制时的清晰度 definition 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public static int RecordDefinition;
        /// <summary>
        /// 当前是否已开启检测
        /// </summary>
        public static volatile bool isDetection = false;

        public static bool IsDetection => isDetection;

        public static ConcurrentDictionary<string, AnchorRecordBll> GetUnRecordList()
        {
            return unRecordList;
        }
        /// <summary>
        /// 当前轮检测是否已全部完成
        /// </summary>
        public static volatile bool currentDetectionDone = false;
        /// <summary>
        /// 第一轮检测是否已经完成，完成了才启动websocket
        /// </summary>
        public static volatile bool firstRoundDone = false;
        public static object _lock = new object();
        /// <summary>
        /// 磁盘检查线程的CancellationToken，用于取消操作
        /// </summary>
        private static CancellationTokenSource _checkDiskCts = null;

        /// <summary>
        /// 开启所有检测录制
        /// </summary>
        /// <param name="VideoType">视频类型 video_type 0 ts,1 flv,2 mp4</param>
        /// <param name="definition">清晰度 definition 0 标清 1高清 2超清 3 蓝光</param>
        public static async void StartDetectionAll(int VideoType, int Definition)
        {
            try
            {
                // 开启cpp录制监听，有录制任务将直接开启录制
                new Thread(() => { CppRecordUtils.run(); }).Start();

                // 记录录制的视频类型和清晰度
                RecordVideoType = VideoType;
                RecordDefinition = Definition;

                ConfigBll configBll = new ConfigBll();
                // 设置本次录制的配置信息
                Config systemConfig = configBll.GetModel();
                config = JsonConvert.DeserializeObject<Config>(JsonConvert.SerializeObject(systemConfig));

                // 将配置中的录制状态修改为正在录制
                configBll.updateConfigRecordStatus(1);

                // 修改检测状态标识
                isDetection = true;

                // 将所有符合条件的主播放到"未开始录制的主播列表unRecordList"
                AddAhchorToUnRecordList();

                // 开启线程检测未开始录制的抖音主播是否已上线
                new Thread(() => StartDetectionDouyinAnchorOnlineTimer()).Start();
                // 开启线程检测未开始录制的快手主播是否已上线
                new Thread(() => StartDetectionKuaishouAnchorOnlineTimer()).Start();
                // 开启线程检测未开始录制的微信视频号主播是否已上线
                new Thread(() => StartDetectionWeChatChannelsAnchorOnlineTimer()).Start();

                // 开启线程检测来客主播直播状态并采集数据
                //new Thread(() => LifeDataCollectionManager.StartDetection()).Start();

                // 开启线程检测企业号主播直播状态并采集数据
                //new Thread(() => EnterpriseDataCollectionManager.StartDetection()).Start();

                // 开启线程检测主播后台直播状态并采集数据
                //new Thread(() => AnchorLiveDataCollectionManager.StartDetection()).Start();

                // 开启线程检测巨量API主播直播状态并采集数据
                //new Thread(() => JuliangApiDataCollectionManager.StartDetection()).Start();

                // 开启线程检测千川主播直播状态并采集数据
                //new Thread(() => QianchuanDataCollectionManager.StartDetection()).Start();

                // 开启线程检测正在录制的主播是否已经下播
                //new Thread(() => StartDetectionIsUnLine()).Start();

                // 开启线程修改正在录制的视频文件大小时长
                new Thread(() => StartDetectionVideo()).Start();

                // 开启线程检测是否磁盘是否小于10G，小于10G停止录制
                _checkDiskCts = new CancellationTokenSource();
                new Thread(() => CheckDisk(_checkDiskCts.Token)) { IsBackground = true}.Start();

                // 开启线程检测C++录制状态是否正常
                new Thread(() => CheckCPPRecordStatus()).Start();

                // 开启线程检测排班录制状态
                new Thread(() => CheckScheduleRecordStatus()).Start();

                // 开启线程检测北京时间录制状态
                new Thread(() => CheckBeijingTimeRecordStatus()).Start();

                // 开启线程检测在线人数
                //new Thread(() => RecordOnlineNum()).Start();

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", "开启所有检测录制发生异常");
            }

        }

        /// <summary>
        /// 检测C++录制状态是否正常
        /// </summary>
        public static void CheckCPPRecordStatus()
        {

            while (isDetection)
            {
                if (recordingList != null && recordingList.Count > 0)
                {
                    foreach (var item in recordingList)
                    {
                        AnchorRecordBll anchorRecordBll = item.Value;
                        AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();

                        // 跳过正在重启录制或停止录制过程中的主播，避免竞态条件
                        if (!anchorRecordBll.isRecording || anchorRecordBll.recordStoping || anchorRecordBll.isRestartRecord)
                        {
                            FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"主动检测C++录制状态===跳过(正在重启或停止中) isRecording={anchorRecordBll.isRecording}, recordStoping={anchorRecordBll.recordStoping}, isRestartRecord={anchorRecordBll.isRestartRecord}");
                            continue;
                        }

                        if (!string.IsNullOrEmpty(anchorRecordBll.taskId))
                        {

                            int status = CppRecordUtils.getTaskStatus(anchorRecordBll.taskId);

                            if (status == 4 || status == 5 || status == 6 || status == 7)
                            {
                                FileUtils.LogRecrd($"{status}==={anchorInfo?.AnchorName}", $"主动检测C++录制状态===状态异常");
                                anchorRecordBll.StopRecord(status);
                            }
                        }
                        else
                        {
                            FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"主动检测C++录制状态==任务id不存在");
                        }
                    }
                }
                Thread.Sleep(3000);
            }
        }

        /// <summary>
        /// 定时同步排班数据
        /// </summary>
        public static async void StartSyncScheduleTimer()
        {
            while (true)
            {
                try
                {
                    await AnchorApi.SyncScheduleFromServer();
                    await Task.Delay(30 * 60 * 1000); // 30分钟同步一次
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "定时同步排班数据发生异常");
                }
            }
        }

        /// <summary>
        /// 检测排班驱动的录制是否需要停止
        /// </summary>
        private static void CheckScheduleRecordStatus()
        {
            while (isDetection)
            {
                try
                {
                    if (recordingList.Count > 0)
                    {
                        foreach (var item in recordingList)
                        {
                            AnchorRecordBll anchorRecordBll = item.Value;

                            // 只检查排班驱动的录制
                            if (!anchorRecordBll.isScheduleRecord)
                            {
                                continue;
                            }

                            // 跳过正在处理中的
                            if (!anchorRecordBll.isRecording || anchorRecordBll.recordStoping)
                            {
                                continue;
                            }

                            AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();

                            bool shouldStop = false;
                            string stopReason = "";

                            // 检查主播是否关闭了排班录制功能
                            if (anchorInfo.isScheduleRecord != null && anchorInfo.isScheduleRecord != 1)
                            {
                                shouldStop = true;
                                stopReason = "主播已关闭排班录制功能";
                            }
                            else
                            {
                                // 获取当前生效的排班
                                AnchorSchedule currentSchedule = AnchorScheduleCacheManager.GetActiveSchedule(anchorInfo.SecUid);

                                if (currentSchedule == null)
                                {
                                    // 情况1：排班时间已结束或排班被删除
                                    shouldStop = true;
                                    stopReason = "排班时间已结束或排班已取消";
                                }
                                else if (!string.IsNullOrEmpty(anchorRecordBll.scheduleId) && currentSchedule.scheduleId != anchorRecordBll.scheduleId)
                                {
                                    // 情况2：排班ID变了（被替换成新排班）
                                    // 更新排班ID，继续录制
                                    anchorRecordBll.scheduleId = currentSchedule.scheduleId;
                                    anchorRecordBll.activeSchedule = currentSchedule;
                                    FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"排班已更新，继续使用新排班：{currentSchedule.scheduleId}");
                                }
                            }

                            if (shouldStop)
                            {
                                FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"排班检测：{stopReason}，停止录制");
                                anchorRecordBll.StopRecord();
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "检测排班录制状态发生异常");
                }

                Thread.Sleep(10 * 1000); // 每10秒检测一次
            }
        }

        /// <summary>
        /// 检测北京时间录制模式是否需要分段
        /// </summary>
        private static void CheckBeijingTimeRecordStatus()
        {
            while (isDetection)
            {
                try
                {
                    if (recordingList.Count > 0)
                    {
                        foreach (var item in recordingList)
                        {
                            AnchorRecordBll anchorRecordBll = item.Value;

                            // 只检查北京时间录制模式的主播
                            if (!anchorRecordBll.isBeijingTimeRecord)
                            {
                                continue;
                            }

                            // 跳过正在处理中的
                            if (!anchorRecordBll.isRecording || anchorRecordBll.recordStoping || anchorRecordBll.isRestartRecord)
                            {
                                continue;
                            }

                            // 只检查分段录制或限制时长录制模式
                            if (anchorRecordBll.recordLimitType != 1 && anchorRecordBll.recordLimitType != 2)
                            {
                                continue;
                            }

                            AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();

                            // 计算墙钟已经过去的时间
                            double elapsedSeconds = (DateTime.Now - anchorRecordBll.segmentStartTime).TotalSeconds;
                            long timeLimitSeconds = anchorRecordBll.recordLimitValue * 60;

                            if (elapsedSeconds >= timeLimitSeconds)
                            {
                                FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"北京时间录制检测：墙钟已过{elapsedSeconds}秒，达到限制{timeLimitSeconds}秒，触发分段");
                                anchorRecordBll.StopRecord();
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", "检测北京时间录制状态发生异常");
                }

                Thread.Sleep(1 * 1000); // 每1秒检测一次
            }
        }

        /// <summary>
        /// 检测在线人数
        /// </summary>
        //public static void RecordOnlineNum()
        //{
        //    while (isDetection)
        //    {
        //        if (recordingList != null && recordingList.Count > 0)
        //        {
        //            foreach (var item in recordingList)
        //            {
        //                AnchorRecordBll anchorRecordBll = item.Value;
        //                AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
        //                if(anchorInfo != null && !string.IsNullOrEmpty(anchorInfo.LiveUrl))
        //                {
        //                    try
        //                    {
        //                        // 采集主播相关信息
        //                        DouYinAnchorInfoEntity douYinAnchorInfoEntity = DouYinAnchorBll.GetDouYinAnchorInfo(anchorInfo.LiveUrl);
        //                        if (douYinAnchorInfoEntity != null)
        //                        {
        //                            // 保存在线人数
        //                            WebsocketEntity onlineWebsocketEntity = new WebsocketEntity();
        //                            onlineWebsocketEntity.url = anchorInfo.LiveUrl;
        //                            onlineWebsocketEntity.batchNumber = douYinAnchorInfoEntity.RoomId;
        //                            onlineWebsocketEntity.secUid = anchorInfo.SecUid;
        //                            onlineWebsocketEntity.videoId = anchorRecordBll.GetVideo().VideoId;
        //                            onlineWebsocketEntity.type = "DouYinLive";

        //                            int peoploNum = 0;
        //                            if (douYinAnchorInfoEntity.CurrentNumber.Contains("万"))
        //                            {
        //                                string[] arr = douYinAnchorInfoEntity.CurrentNumber.Split('万');
        //                                if (int.TryParse(arr[0], out int result))
        //                                {
        //                                    peoploNum = int.Parse(arr[0]) * 10000;
        //                                    anchorInfo.OnlineNumber = peoploNum.ToString();
        //                                }
        //                            }
        //                            else
        //                            {
        //                                if (int.TryParse(douYinAnchorInfoEntity.CurrentNumber, out int result))
        //                                {
        //                                    peoploNum = int.Parse(douYinAnchorInfoEntity.CurrentNumber);
        //                                    anchorInfo.OnlineNumber = peoploNum.ToString();
        //                                }
        //                            }

        //                            RenShu renshu = new RenShu()
        //                            {
        //                                type = "renshu",
        //                                content = $"{peoploNum}",
        //                            };
        //                            WebsocketDataHandle.Push("renshu", onlineWebsocketEntity, renshu);

        //                            // 保存总观看人次
        //                            WebsocketEntity totalWebsocketEntity = new WebsocketEntity();
        //                            totalWebsocketEntity.url = anchorInfo.LiveUrl;
        //                            totalWebsocketEntity.batchNumber = douYinAnchorInfoEntity.RoomId;
        //                            totalWebsocketEntity.secUid = anchorInfo.SecUid;
        //                            totalWebsocketEntity.videoId = anchorRecordBll.GetVideo().VideoId;
        //                            totalWebsocketEntity.type = "DouYinLive";

        //                            int totalPeopleNum = 0;
        //                            if (douYinAnchorInfoEntity.TotalVisits.Contains("万"))
        //                            {
        //                                string[] arr = douYinAnchorInfoEntity.TotalVisits.Split('万');
        //                                if (int.TryParse(arr[0], out int result))
        //                                {
        //                                    totalPeopleNum = int.Parse(arr[0]) * 10000;
        //                                }
        //                            }
        //                            else
        //                            {
        //                                if (int.TryParse(douYinAnchorInfoEntity.TotalVisits, out int result))
        //                                {
        //                                    totalPeopleNum = int.Parse(douYinAnchorInfoEntity.TotalVisits);
        //                                }
        //                            }

        //                            RenShu leijiguankanrenshu = new RenShu()
        //                            {
        //                                type = "leijiguankanrenshu",
        //                                content = $"{totalPeopleNum}",
        //                            };
        //                            WebsocketDataHandle.Push("leijiguankanrenshu", totalWebsocketEntity, leijiguankanrenshu);

        //                            // 修改主播在线人数信息
        //                            AnchorCacheManager.SetAnchorCache(anchorInfo);
        //                        }
        //                    }
        //                    catch (Exception e)
        //                    {
        //                        FileUtils.LogError($"{e}", "检测在线人数发生异常");
        //                    }
        //                }

        //                Random rand = new Random();
        //                Thread.Sleep(rand.Next(500, 1500));
        //            }
        //        }

        //        Thread.Sleep(10 * 1000);
        //    }

        //}

        /// <summary>
        /// 定时检查磁盘空间是否小于10G，小于10G停止录制
        /// </summary>
        public static void CheckDisk(CancellationToken cancellationToken)
        {
            while (isDetection && !cancellationToken.IsCancellationRequested)
            {
                try
                {
                    // 检查是否需要取消
                    cancellationToken.ThrowIfCancellationRequested();

                    string savePath = config.SavePath;
                    string driveLetter = Path.GetPathRoot(savePath).Substring(0, 1);
                    // 使用带超时的磁盘检查，默认超时5秒
                    Dictionary<string, long> dictionary = FileUtils.GetDiskSize(driveLetter);
                    long driveAvailablepace = dictionary["driveAvailablepace"];
                    if (driveAvailablepace < 10)
                    {
                        FileUtils.LogRecrd($"{dictionary}", $"磁盘空间不足,通知前端弹窗");
                        // 停止录制
                        StopDecectorAll();
                        // 通知前端弹窗
                        FrontNotice frontNotice = new FrontNotice();
                        var requestDataObj = new Dictionary<string, object>();
                        requestDataObj["code"] = 0;
                        requestDataObj["status"] = 200;
                        requestDataObj["action"] = "diskWarnStopRecord";
                        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));

                        break;
                    }
                }
                catch (OperationCanceledException)
                {
                    // 收到取消信号，正常退出循环
                    FileUtils.LogRecrd("", $"磁盘检查线程收到取消信号，退出");
                    break;
                }
                catch (Exception e)
                {
                    FileUtils.LogError($"{e}", $"磁盘空间不足,通知前端弹窗发生异常");
                }

                // 使用可取消的等待
                try
                {
                    Task.Delay(1000 * 60, cancellationToken).Wait(cancellationToken);
                }
                catch (OperationCanceledException)
                {
                    FileUtils.LogRecrd("", $"磁盘检查线程等待被取消，退出");
                    break;
                }
            }
        }

        /// <summary>
        /// 将所有符合条件的主播放到"未开始录制的主播列表unRecordList"
        /// </summary>
        private static void AddAhchorToUnRecordList()
        {
            // 获取全部未从录制列表移除的主播列表
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllNotRemoveAnchors();

            if(anchorInfos != null && anchorInfos.Count > 0 )
            {
                // 只添加了开启自动录制检测的主播
                foreach (var anchorInfo in anchorInfos)
                {
                    if(anchorInfo.IsAutoRecord == 1)
                    {
                        AnchorRecordBll anchorRecordBll = new AnchorRecordBll(anchorInfo);
                        unRecordList.AddOrUpdate(anchorInfo.SecUid, anchorRecordBll, (oldKey, oldValue) => anchorRecordBll);
                    }
                }
            }

        }

        /// <summary>
        /// 开启定时检测未开始录制的主播是否已上线-抖音
        /// </summary>
        private static async Task StartDetectionDouyinAnchorOnlineTimer()
        {
            firstRoundDone = false;
            SemaphoreSlim semaphore = new SemaphoreSlim(2, 2); // 控制并发度
            //SemaphoreSlim semaphore = new SemaphoreSlim(1, 1);
            while (isDetection)
            {
                try
                {

                    // 遍历所有未开始录制的主播
                    var tasks = new List<Task>();
                    foreach (var item in unRecordList)
                    {
                        AnchorRecordBll anchorRecordBll = item.Value;
                        AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                        // 只筛选出抖音主播
                        if (anchorInfo != null && anchorInfo.platform == 0)
                        {
                            // 添加进异步任务
                            tasks.Add(ProcessAnchor(anchorRecordBll, semaphore));
                        }
                    }

                    await Task.WhenAll(tasks);

                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"{ex}", "遍历所有未开始录制的主播时发生错误");
                }

                firstRoundDone = true;

                Thread.Sleep(2000);
            }

        }

        /// <summary>
        /// 异步检测主播是否在线任务-抖音
        /// </summary>
        /// <param name="anchorRecordBll">主播录制对象</param>
        /// <param name="semaphore">线程池</param>
        /// <returns></returns>
        private static async Task ProcessAnchor(AnchorRecordBll anchorRecordBll, SemaphoreSlim semaphore)
        {
            await semaphore.WaitAsync(); // 等待许可
            try
            {
                // 开始检测录制
                startRecord(anchorRecordBll);

                int time = config.DetectionFre != 0 ? config.DetectionFre : 2000;
                await Task.Delay(time);
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", "处理主播信息时发生错误");
            }
            finally
            {
                semaphore.Release(); // 释放许可
            }
        }

        /// <summary>
        /// 开启定时检测未开始录制的快手主播是否已上线-快手
        /// </summary>
        private static async Task StartDetectionKuaishouAnchorOnlineTimer()
        {
            firstRoundDone = false;
            SemaphoreSlim semaphore = new SemaphoreSlim(1, 1); // 控制并发度

            while (isDetection)
            {
                try
                {

                    // 遍历所有未开始录制的主播
                    var tasks = new List<Task>();
                    foreach (var item in unRecordList)
                    {
                        AnchorRecordBll anchorRecordBll = item.Value;
                        AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                        // 只筛选出抖音主播
                        if (anchorInfo != null && anchorInfo.platform == 1)
                        {
                            // 添加进异步任务
                            tasks.Add(ProcessKuaishouAnchor(anchorRecordBll, semaphore));
                        }
                    }

                    await Task.WhenAll(tasks);

                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"{ex}", "遍历所有未开始录制的主播时发生错误");
                }

                firstRoundDone = true;

                Thread.Sleep(2000);
            }

        }

        /// <summary>
        /// 异步检测主播是否在线任务-快手
        /// </summary>
        /// <param name="anchorRecordBll">主播录制对象</param>
        /// <param name="semaphore">线程池</param>
        /// <returns></returns>
        private static async Task ProcessKuaishouAnchor(AnchorRecordBll anchorRecordBll, SemaphoreSlim semaphore)
        {
            await semaphore.WaitAsync(); // 等待许可
            try
            {
                // 开始检测录制
                startRecord(anchorRecordBll);

                Random random = new Random();
                int randomNumber = random.Next(10, 16);
                await Task.Delay(randomNumber * 1000);
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", "处理主播信息时发生错误");
            }
            finally
            {
                semaphore.Release(); // 释放许可
            }
        }

        /// <summary>
        /// 开启定时检测未开始录制的微信视频号主播是否已上线-微信视频号
        /// </summary>
        private static async Task StartDetectionWeChatChannelsAnchorOnlineTimer()
        {
            firstRoundDone = false;
            SemaphoreSlim semaphore = new SemaphoreSlim(5, 5); // 控制并发度

            while (isDetection)
            {
                try
                {
                    // 遍历所有未开始录制的主播
                    var tasks = new List<Task>();
                    foreach (var item in unRecordList)
                    {
                        AnchorRecordBll anchorRecordBll = item.Value;
                        AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                        // 只筛选出微信视频号主播
                        if (anchorInfo != null && anchorInfo.platform == 2)
                        {
                            // 添加进异步任务
                            tasks.Add(ProcessWeChatChannelsAnchor(anchorRecordBll, semaphore));
                        }
                    }

                    await Task.WhenAll(tasks);

                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"{ex}", "遍历所有未开始录制的主播时发生错误");
                }

                firstRoundDone = true;

                Thread.Sleep(2000);
            }

        }

        /// <summary>
        /// 异步检测主播是否在线任务-微信视频号
        /// </summary>
        /// <param name="anchorRecordBll">主播录制对象</param>
        /// <param name="semaphore">线程池</param>
        private static async Task ProcessWeChatChannelsAnchor(AnchorRecordBll anchorRecordBll, SemaphoreSlim semaphore)
        {
            await semaphore.WaitAsync(); // 等待许可
            try
            {
                // 开始检测录制
                startRecord(anchorRecordBll);

                Random random = new Random();
                int randomNumber = random.Next(10, 16);
                await Task.Delay(randomNumber * 1000);
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", "处理主播信息时发生错误");
            }
            finally
            {
                semaphore.Release(); // 释放许可
            }
        }

        /// <summary>
        /// 开始检测录制
        /// </summary>
        /// <param name="anchorRecordBll"></param>
        /// <returns></returns>
        private static void startRecord(AnchorRecordBll anchorRecordBll)
        {

            AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();

            if (anchorInfo != null && anchorInfo.IsAutoRecord == 1 && anchorInfo.IsRemoveRecord == 0)
            {
                bool inRecordTime = false;
                AnchorSchedule activeSchedule = null;

                // 判断是否启用排班录制（主播开启了排班录制功能）
                bool useScheduleRecord = anchorInfo.isScheduleRecord != null && anchorInfo.isScheduleRecord == 1;
                
                // 添加日志：记录主播录制配置
                FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 录制配置 - 自动录制:{anchorInfo.IsAutoRecord}, 排班录制:{useScheduleRecord}, 录制时间:{anchorInfo.RecordTime}", "排班录制检测");

                if (useScheduleRecord)
                {
                    // 主播开启了排班录制，检查后台排班
                    activeSchedule = AnchorScheduleCacheManager.GetActiveSchedule(anchorInfo.SecUid);

                    if (activeSchedule != null)
                    {
                        // 有生效的排班，使用排班时间
                        inRecordTime = true;
                        FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 使用后台排班录制 - 排班ID:{activeSchedule.scheduleId}, 开始时间:{activeSchedule.startTime}, 结束时间:{activeSchedule.endTime}", "排班录制检测");
                    }
                    else if (AnchorScheduleCacheManager.HasScheduleToday(anchorInfo.SecUid))
                    {
                        // 今天有排班但当前不在排班时间内，不录制
                        inRecordTime = false;
                        FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 今天有排班但当前不在排班时间内，不录制", "排班录制检测");
                    }
                    else
                    {
                        // 开启了排班录制但今天没有排班，不录制
                        inRecordTime = false;
                        FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 开启了排班录制但今天没有排班，不录制", "排班录制检测");
                    }
                }
                else
                {
                    // 主播未开启排班录制，使用客户端设置
                    FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 未开启排班录制，使用客户端时间设置", "排班录制检测");
                    
                    // 判断当前时间是否在主播录制时间范围内
                    inRecordTime = true;
                    if (!string.IsNullOrEmpty(anchorInfo.RecordTime))
                    {
                        // 存在录制时间范围时，先将是否录制设置为 false。
                        inRecordTime = false;
                        var timeDurationList = anchorInfo.RecordTime.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);
                        foreach (var timeDuration in timeDurationList)
                        {
                            // 获取主播的录制时间范围
                            string[] arr = timeDuration.Split('-');
                            int startSecond = ServerTimeUtils.TimeToSecond(arr[0]);
                            int endSecond = ServerTimeUtils.TimeToSecond(arr[1]);
                            // 获取当前时间
                            string nowTime = DateTime.Now.ToString("HH:mm:ss");
                            int nowSecond = ServerTimeUtils.TimeToSecond(nowTime);

                            if (startSecond >= endSecond)
                            {
                                if (nowSecond < startSecond && nowSecond < endSecond)
                                {
                                    nowSecond += 24 * 60 * 60;
                                }
                                endSecond += 24 * 60 * 60;
                            }

                            if (nowSecond >= startSecond && nowSecond <= endSecond)
                            {
                                inRecordTime = true;
                                break;
                            }
                        }
                    }
                }

                if (inRecordTime)
                {
                    if ((anchorInfo.RecordStatus != 1) && !anchorRecordBll.isRecording)
                    {
                        if (isDetection)
                        {
                            
                            anchorRecordBll.currentParagraphNum = 0;
                            // 标记是否为排班录制
                            if (activeSchedule != null)
                            {
                                anchorRecordBll.isScheduleRecord = true;
                                anchorRecordBll.scheduleId = activeSchedule.scheduleId;
                                anchorRecordBll.activeSchedule = activeSchedule;
                                FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 启动排班录制 - 排班ID:{activeSchedule.scheduleId}", "排班录制检测");
                            }
                            else
                            {
                                anchorRecordBll.isScheduleRecord = false;
                                anchorRecordBll.scheduleId = null;
                                anchorRecordBll.activeSchedule = null;
                                FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 启动普通录制（非排班）", "排班录制检测");

                                // 标记是否为时间点分段录制模式（优先级高于北京时间录制）
                                if (anchorInfo.recordTimeMode != null && anchorInfo.recordTimeMode == 2 && !string.IsNullOrEmpty(anchorInfo.segmentTimePoints))
                                {
                                    anchorRecordBll.isTimePointRecord = true;
                                    anchorRecordBll.segmentTimePointList = anchorRecordBll.ParseTimePoints(anchorInfo.segmentTimePoints);
                                    // 时间点录制复用北京时间录制逻辑，同时标记 isBeijingTimeRecord
                                    anchorRecordBll.isBeijingTimeRecord = true;
                                }
                                else
                                {
                                    anchorRecordBll.isTimePointRecord = false;
                                    anchorRecordBll.segmentTimePointList.Clear();
                                    // 标记是否为北京时间录制模式
                                    anchorRecordBll.isBeijingTimeRecord = anchorInfo.recordTimeMode != null && anchorInfo.recordTimeMode == 1;
                                }
                            }
                           
                            // 开启检测录制
                            if (anchorRecordBll.allowRecord)
                            {
                                anchorRecordBll.StartDetectionOnline();
                                FileUtils.LogRecrd($"主播 {anchorInfo.AnchorName} 开始在线检测录制", "排班录制检测");
                            }
                        }
                    }
                }
            }
        }

        /// <summary>
        /// 开启定时检测正在录制的主播是否已经下播
        /// </summary>
        //private static void StartDetectionIsUnLine()
        //{
        //    while (isDetection)
        //    {
        //        if(recordingList.Count > 0)
        //        {
        //            // 遍历正在录制的主播
        //            foreach (var item in recordingList)
        //            {
        //                AnchorRecordBll anchorRecordBll = item.Value;

        //                if(anchorRecordBll != null)
        //                {
        //                    // 检测主播是否已下线
        //                    anchorRecordBll.CheckIsUnLine();
        //                }
        //                Thread.Sleep(100);
        //            }
        //        }
        //        Thread.Sleep(4000);
        //    }
        //}

        /// <summary>
        /// 开启定时修改正在录制的视频文件大小时长
        /// </summary>
        private static async void StartDetectionVideo()
        {
            int count = 0;
            ConcurrentDictionary<string, UpdateVideoSizeDurationBo> updateVideoSizeDurationDictionary = new ConcurrentDictionary<string, UpdateVideoSizeDurationBo>();

            while (isDetection)
            {
                try
                {
                    if (recordingList.Count > 0)
                    {

                        // 遍历正在录制的主播
                        foreach (var item in recordingList)
                        {
                            AnchorRecordBll anchorRecordBll = item.Value;

                            if (anchorRecordBll != null && anchorRecordBll.isRecording)
                            {
                                // 获取当前录制的视频信息
                                VideoEntity videoInfo = anchorRecordBll.GetVideo();

                                if (videoInfo != null && !string.IsNullOrEmpty(videoInfo.videoId) && !string.IsNullOrEmpty(videoInfo.storagePath))
                                {

                                    FileInfo fileInfo = new FileInfo(videoInfo.storagePath);
                                    if (fileInfo.Exists)
                                    {

                                        long fileSize = 0;
                                        long duration = 0;

                                        fileSize = CppRecordUtils.getTaskSize(anchorRecordBll.taskId);
                                        duration = CppRecordUtils.getTaskTime(anchorRecordBll.taskId);
                                        videoInfo.vedioSizie = fileSize.ToString();
                                        videoInfo.duration = duration.ToString();
                                        VideoCacheManager.SetVideoToCache(videoInfo);


                                        UpdateVideoSizeDurationBo updateVideoSizeDurationBo = new UpdateVideoSizeDurationBo();
                                        updateVideoSizeDurationBo.videoId = videoInfo.videoId;
                                        updateVideoSizeDurationBo.duration = duration;
                                        updateVideoSizeDurationBo.fileSize = fileSize;

                                        updateVideoSizeDurationDictionary.AddOrUpdate(videoInfo.videoId, updateVideoSizeDurationBo, (oldKey, oldValue) => updateVideoSizeDurationBo);

                                    }
                                }
                            }
                            Thread.Sleep(300);
                        }
                    
                        // 同步大小时长到服务器
                        if(updateVideoSizeDurationDictionary.Count > 0 && count > 10)
                        {
                            List< UpdateVideoSizeDurationBo > list = new List<UpdateVideoSizeDurationBo> ();
                            foreach (var item in updateVideoSizeDurationDictionary)
                            {
                                list.Add(item.Value);
                            }
                            await VideoApi.UpdateVideoSizeDurationList(list);

                            count = 0;
                            updateVideoSizeDurationDictionary.Clear();
                        }

                        count++;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"定时修改正在录制的视频文件大小时长发生异常", $"{ex}");
                }
                Thread.Sleep(3000);
            }
        }

        /// <summary>
        /// 停止所有主播的录制检测
        /// </summary>
        public static async Task StopDecectorAll()
        {
            // 先取消磁盘检查线程
            if (_checkDiskCts != null)
            {
                _checkDiskCts.Cancel();
                _checkDiskCts.Dispose();
                _checkDiskCts = null;
            }

            isDetection = false;

            // 等待一轮检测完毕
            //while(isDetection && !currentDetectionDone)
            //{
            //    Thread.Sleep(20);
            //}

            // 将所有主播状态改为未录制和未检测（快速操作）
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllNotRemoveAnchors();
            if (anchorInfos != null && anchorInfos.Count > 0)
            {
                foreach (var anchorInfo in anchorInfos)
                {
                    anchorInfo.RecordStatus = 0;
                    anchorInfo.LiveStatus = 0;
                    AnchorCacheManager.SetAnchorCache(anchorInfo);
                }
            }

            // 快速停止录制进程（不等待完整的结束流程，后台执行数据清理）
            if (recordingList.Count > 0)
            {
                var anchorsToStop = recordingList.Values.ToList();
                recordingList.Clear();
                
                // 后台执行完整的停止流程（数据上传等耗时操作）
                _ = Task.Run(async () =>
                {
                    try
                    {
                        var stopTasks = anchorsToStop.Select(async anchorRecordBll =>
                        {
                            anchorRecordBll.allowRecord = false;
                            anchorRecordBll.KillProcess(); // 快速杀掉进程
                            await anchorRecordBll.StopRecord(); // 执行完整的结束流程
                        }).ToArray();
                        
                        await Task.WhenAll(stopTasks);
                        FileUtils.LogRpa($"所有录制进程已停止并清理完成", "StopDecectorAll");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"后台停止录制发生异常：{ex.Message}", "StopDecectorAll");
                    }
                });
            }

            // unRecordList 中的主播直接清理（后台执行）
            if (unRecordList.Count > 0)
            {
                var anchorsToStop = unRecordList.Values.ToList();
                unRecordList.Clear();
                
                _ = Task.Run(async () =>
                {
                    try
                    {
                        foreach (var anchorRecordBll in anchorsToStop)
                        {
                            anchorRecordBll.allowRecord = false;
                            await anchorRecordBll.StopRecord();
                        }
                        FileUtils.LogRpa($"所有待录制进程已清理完成", "StopDecectorAll");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"后台清理待录制进程发生异常：{ex.Message}", "StopDecectorAll");
                    }
                });
            }

            //将录制状态修改为未录制（快速操作）
            ConfigBll configBll = new ConfigBll();
            configBll.updateConfigRecordStatus(0);
        }

        /// <summary>
        /// 通知前端刷新磁盘空间
        /// </summary>
        private void NoticeFrontRefreshDisk()
        {
            try
            {
                // 通知前端刷新磁盘空间
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = 0;
                requestDataObj["status"] = 200;
                requestDataObj["action"] = "refreshDisk";
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }
            catch (Exception e)
            {
                FileUtils.LogAnalysis($"{e}", $"通知前端刷新磁盘空间发生异常");
            }
        }

        /// <summary>
        /// 删除主播
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public static async Task RemoveAnchor(string secUid)
        {

            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);

            if(anchorInfo == null)
            {
                throw new CustomException("主播信息不存在");
            }

            // 将主播信息同步到服务器
            AnchorApi.UpdateUserAnchorSync(secUid, "", -1, 1);

            // 等待一轮检测完毕
            //while (isDetection && !currentDetectionDone)
            //{
            //    Thread.Sleep(20);
            //}

            // 从录制列表/待录制列表删除主播
            try
            {
                await RemoveAnchorFromList(secUid);
            }
            catch (Exception e)
            {
                FileUtils.LogError($"{e}", $"删除主播-从录制列表/待录制列表删除主播-发生异常=={secUid}");
            }
            

            // 修改主播信息
            anchorInfo.IsRemoveRecord = 1;
            anchorInfo.IsBarrageMonitoring = 0;
            anchorInfo.deleteDate = ServerTimeUtils.getCurrentTimeStr();
            anchorInfo.isScriptQualityInspection = 0;
            anchorInfo.isScriptFidelityMonitor = 0;
            anchorInfo.isInteractionPatrol = 0;
            AnchorCacheManager.SetAnchorCache(anchorInfo);

            // 抖音主播已授权巨量百应
            if (anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth &&
                anchorInfo.platform == 0)
            {
                UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.unAuth);
                // 重新计算授权资源
                JuliangUtils.recountJuliangProperty();
                // 删除缓存文件版本号
                JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);
            }

            // 微信视频号主播
            if (anchorInfo.platform == 2)
            {
                await WeChatChannelsSerivce.UnbindWeChatChannels(int.Parse(anchorInfo.SecUid));
            }
        }

        /// <summary>
        /// 从录制列表/待录制列表删除主播
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        private static async Task RemoveAnchorFromList(string secUid)
        {

            if (recordingList.Count > 0)
            {
                foreach (var item in recordingList)
                {
                    // 获取主播信息
                    AnchorRecordBll anchorRecordBll = item.Value;
                    AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                    if (anchorInfo != null && secUid.Equals(anchorInfo.SecUid))
                    {
                        anchorRecordBll.allowRecord = false;
                        // 将当前对象从已录制集合移除
                        AnchorBll.recordingList.TryRemove(anchorInfo.SecUid, out AnchorRecordBll _);
                        // 杀掉正在录制的进程
                        await anchorRecordBll.StopRecord();
                    }
                }
            }

            if (unRecordList.Count > 0)
            {
                foreach (var item in unRecordList)
                {
                    // 获取主播信息
                    AnchorRecordBll anchorRecordBll = item.Value;
                    AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                    if (anchorInfo != null && secUid.Equals(anchorInfo.SecUid))
                    {
                        anchorRecordBll.allowRecord = false;
                        // 将当前对象从已录制集合移除
                        AnchorBll.unRecordList.TryRemove(anchorInfo.SecUid, out AnchorRecordBll _);
                        // 杀掉正在录制的进程
                        await anchorRecordBll.StopRecord();
                    }
                }
            }
        }


        /// <summary>
        /// 判断主播是否存在于已录制/待录制列表，不存在则添加到待录制列表
        /// </summary>
        private static void NotExistAdd(AnchorInfo addAnchor)
        {
            bool exist = false;

            if (recordingList.Count > 0)
            {
                foreach (var item in recordingList)
                {
                    // 获取主播信息
                    AnchorRecordBll anchorRecordBll = item.Value;
                    AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                    if (anchorInfo != null && anchorInfo.SecUid.Equals(addAnchor.SecUid))
                    {
                        exist = true;
                        break;
                    }
                }
            }
            if (unRecordList.Count > 0)
            {
                foreach (var item in unRecordList)
                {
                    // 获取主播信息
                    AnchorRecordBll anchorRecordBll = item.Value;
                    AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                    if (anchorInfo != null && anchorInfo.SecUid.Equals(addAnchor.SecUid))
                    {
                        exist = true;
                        break;
                    }
                }
            }

            if (!exist)
            {
                // 不存在
                AnchorRecordBll anchorRecordBll = new AnchorRecordBll(addAnchor);
                unRecordList.AddOrUpdate(addAnchor.SecUid, anchorRecordBll, (oldKey, oldValue) => oldValue);
            }
        }

        /// <summary>
        /// 停止单个主播的录制
        /// </summary>
        /// <param name="secUid">主播secuid</param>
        public static async Task StopRecord(string secUid)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);

            if (anchorInfo == null)
            {
                throw new CustomException("主播信息不存在");
            }

            // 等待一轮检测完毕
            //while (isDetection && !currentDetectionDone)
            //{
            //    Thread.Sleep(20);
            //}

            // 从录制列表/待录制列表删除主播
            await RemoveAnchorFromList(secUid);

        }

        /// <summary>
        /// 开启单个主播录制
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        public static void StartRecord(string secUid)
        {

            if(!isDetection)
            {
                throw new Exception("请先开启录制检测");
            }

            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorInfo != null)
            {
                // 等待一轮检测完毕
                //while (isDetection && !currentDetectionDone)
                //{
                //    Thread.Sleep(20);
                //}

                anchorInfo.RecordStatus = 4;
                anchorInfo.LiveStatus = 0;
                AnchorCacheManager.SetAnchorCache(anchorInfo);

                AnchorRecordBll anchorRecordBll = new AnchorRecordBll(anchorInfo);
                unRecordList.AddOrUpdate(anchorInfo.SecUid, anchorRecordBll, (oldKey, oldValue) => anchorRecordBll);
            }
            
        }

        ///<summary>
        /// 开启/关闭自动录制
        /// </summary>
        /// <param name="secUid">主播Id</param>
        /// <param name="isAuto">是否开启自动录制 0否，1是</param>
        public static void OpenOrCloseAutoRecord(string secUid, int isAuto)
        {

            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);

            if(anchorInfo != null)
            {
                // 修改到服务器
                AnchorApi.UpdateUserAnchorSync(secUid, "", isAuto, -1);

                anchorInfo.IsAutoRecord = isAuto;
                AnchorCacheManager.SetAnchorCache(anchorInfo);

                if(isDetection)
                {
                    if(isAuto == 1)
                    {
                        NotExistAdd(anchorInfo);
                    }else
                    {
                        // 从待录制列表删除
                        AnchorBll.unRecordList.TryRemove(anchorInfo.SecUid, out AnchorRecordBll _);
                    }
                }
                
            }

            
        }

        /// <summary>
        /// 添加/修改主播
        /// </summary>
        /// <param name="anchorBo">主播信息</param>
        public async void AddOrUpdateAnchor(AddOrUpdateAnchorBo anchorBo)
        {
            try
            {
                AnchorInfo anchor = null;
                if (string.IsNullOrEmpty(anchorBo.secUid))
                {
                    // 添加
                    try
                    {
                        if (anchorBo.platform == null)
                        {
                            anchorBo.platform = 0;
                        }
                        if (anchorBo.isBarrageMonitoring == null)
                        {
                            anchorBo.isBarrageMonitoring = 0;
                        }
                        if (anchorBo.isDataViewing == null)
                        {
                            anchorBo.isDataViewing = 0;
                        }

                        anchor = await BuildAddAnchor(anchorBo.liveUrl, anchorBo.urlType, (int)anchorBo.platform);
                        if (anchor != null)
                        {
                            AnchorInfo cacheAnchor = AnchorCacheManager.GetAnchorByIdFromCache(anchor.SecUid);

                            FrontNotice frontNotice = new FrontNotice();
                            var requestDataObj = new Dictionary<string, object>();
                            // 如果已经添加过，直接返回
                            if (cacheAnchor != null && cacheAnchor.IsRemoveRecord == 0)
                            {
                                // 通知前端已存在主播
                                requestDataObj["code"] = 0;
                                requestDataObj["status"] = 200;
                                requestDataObj["action"] = "addAnchorEnd";
                                AnchorInfo tempAnchorInfo = JsonConvert.DeserializeObject<AnchorInfo>(JsonConvert.SerializeObject(cacheAnchor));
                                tempAnchorInfo.Id = -999;
                                requestDataObj["data"] = tempAnchorInfo;
                                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                                return;
                            }

                            // 设置主播的信息
                            anchor.IsRemoveRecord = 0;
                            anchor.IsAutoRecord = 1;
                            anchor.RecordStatus = 0;
                            anchor.LiveStatus = 0;
                            anchor.OnlineNumber = "0";
                            anchor.AddTime = ServerTimeUtils.getCurrentTimeStr();
                            anchor.LastRecordTime = "2000-01-01 00:00:00";
                            anchor.IsTop = 0;
                            anchor.AddTopTime = "";
                            anchor.FolderName = WindowsUtils.SanitizeForFolderName(anchor.AnchorName);


                            // 新增的主播弹幕监控位和数据看板改为0
                            anchorBo.isBarrageMonitoring = 0;
                            anchorBo.isDataViewing = 0;

                            // 构建主播剩余配置
                            BuildAnchorInfo(anchor, anchorBo);

                            // 同步到服务器
                            await AnchorApi.AddOrUpdateAnchor(anchor);
                           

                            // 判断是否要自动开启弹幕监控和数据看板
                            try
                            {
                                OpenMonitoringPositionVo openMonitoringPositionVo = await AnchorApi.openMonitoringPosition(anchor.SecUid);
                                if (openMonitoringPositionVo.isBarrageMonitoring != null)
                                {
                                    anchor.IsBarrageMonitoring = openMonitoringPositionVo.isBarrageMonitoring ?? 0;
                                }
                                if (openMonitoringPositionVo.isDataViewing != null)
                                {
                                    anchor.IsDataViewing = openMonitoringPositionVo.isDataViewing ?? 0;
                                }
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogError($"{ex}", $"判断是否要自动开启弹幕监控和数据看板发生异常==={anchor.SecUid}");
                            }


                            // 添加到缓存
                            AnchorCacheManager.SetAnchorCache(anchor);


                            // 判断主播是否存在于已录制/待录制列表，不存在则添加到待录制列表
                            if (isDetection)
                            {
                                NotExistAdd(anchor);
                            }

                            // 同步排班数据
                            // await AnchorApi.SyncScheduleFromServer();

                            // 通知前端添加成功
                            requestDataObj["code"] = 0;
                            requestDataObj["status"] = 200;
                            requestDataObj["action"] = "addAnchorEnd";
                            requestDataObj["data"] = anchor;
                            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        }
                        else
                        {
                            
                            // 通知前端添加失败
                            FrontNotice frontNotice = new FrontNotice();
                            var requestDataObj = new Dictionary<string, object>();
                            requestDataObj["code"] = -1;
                            requestDataObj["status"] = 500;
                            requestDataObj["action"] = "addAnchorEnd";
                            requestDataObj["msg"] = "获取主播数据失败";
                            requestDataObj["data"] = null;

                            FileUtils.LogError($"{JsonConvert.SerializeObject(anchor)}", $"添加主播失败");
                            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", $"添加主播发生异常");

                        // 通知前端添加失败（CustomException 透传服务端业务码，如 70007）
                        FrontNotice frontNotice = new FrontNotice();
                        var requestDataObj = new Dictionary<string, object>();
                        requestDataObj["code"] = (ex as CustomException)?.ErrorCode ?? -1;
                        requestDataObj["status"] = 500;
                        requestDataObj["action"] = "addAnchorEnd";
                        requestDataObj["msg"] = ex.Message;
                        requestDataObj["data"] = null;
                        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                    }
                    
                }
                else
                {
                    // 修改
                    AnchorInfo cachedAnchor = AnchorCacheManager.GetAnchorByIdFromCache(anchorBo.secUid);
                    if (cachedAnchor != null)
                    {
                        // 在缓存副本上构建，避免服务端拒绝时污染活缓存（GetAnchorByIdFromCache 返回活引用）
                        anchor = JsonConvert.DeserializeObject<AnchorInfo>(JsonConvert.SerializeObject(cachedAnchor));
                        // 构建主播剩余配置
                        BuildAnchorInfo(anchor, anchorBo);
                        // 同步到服务器（服务端非0抛 CustomException，下面落缓存被跳过）
                        await AnchorApi.AddOrUpdateAnchor(anchor);
                        // 仅成功后才落缓存
                        AnchorCacheManager.SetAnchorCache(anchor);
                    }
                    else
                    {
                        // 缓存中找不到该主播：通知前端，避免「保存中」无反馈卡死
                        FrontNotice frontNotice = new FrontNotice();
                        var requestDataObj = new Dictionary<string, object>();
                        requestDataObj["code"] = -1;
                        requestDataObj["status"] = 500;
                        requestDataObj["action"] = "addAnchorEnd";
                        requestDataObj["msg"] = "主播不存在或可能被移除";
                        requestDataObj["data"] = null;
                        frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                    }
                }
            }
            catch (Exception ex)
            {
                // 通知前端添加失败（CustomException 透传服务端业务码，如 70007）
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>();
                requestDataObj["code"] = (ex as CustomException)?.ErrorCode ?? -1;
                requestDataObj["status"] = 500;
                requestDataObj["action"] = "addAnchorEnd";
                requestDataObj["msg"] = ex.Message;
                requestDataObj["data"] = null;
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));

                FileUtils.LogError($"{ex}", $"添加/修改主播发生异常");
            }
            
        }

        /// <summary>
        /// 构建主播的额外配置
        /// </summary>
        /// <param name="anchor">主播信息</param>
        /// <param name="anchorBo">主播配置</param>
        private void BuildAnchorInfo(AnchorInfo anchor, AddOrUpdateAnchorBo anchorBo)
        {
            // 基础配置
            anchor.premiereDate = anchorBo.premiereDate;
            anchor.accountStage = anchorBo.accountStage;
            anchor.accountWaterLevel = anchorBo.accountWaterLevel;
            anchor.accountFlow = anchorBo.accountFlow;
            anchor.livingTarget = anchorBo.livingTarget;
            anchor.livingModality = anchorBo.livingModality;
            anchor.marketing = anchorBo.marketing;
            anchor.optimizeDirection = anchorBo.optimizeDirection;
            anchor.learning = anchorBo.learning;
            anchor.livingMode = anchorBo.livingMode;
            anchor.AnchorSituation = anchorBo.anchorSituation;
            anchor.roiAccuracy = anchorBo.roiAccuracy;

            anchor.isAutoDiagnosis = anchorBo.isAutoDiagnosis;
            anchor.diagnosisParams = anchorBo.diagnosisParams;
            anchor.isDataDiagnosis = anchorBo.isDataDiagnosis?? 0;
            anchor.diagnosisGenerateNum = anchorBo.diagnosisGenerateNum;
            anchor.dataDiagnosisParams = anchorBo.dataDiagnosisParams;
            anchor.RemarksName = string.IsNullOrEmpty(anchorBo.remarksName) ? anchor.AnchorName : anchorBo.remarksName;
            anchor.engSerViceType = anchorBo.engSerViceType;
            anchor.pureRecordOnlineNum = anchorBo.pureRecordOnlineNum;
            
            anchor.segmentTimePoints = anchorBo.segmentTimePoints;
            if(!string.IsNullOrEmpty(anchorBo.segmentTimePoints))
            {
                anchor.recordTimeMode = 2;
            }else
            {
                anchor.recordTimeMode = 1;
            }
            anchor.isStatisticsPerformance = anchorBo.isStatisticsPerformance;
            anchor.isScheduleRecord = anchorBo.isScheduleRecord;

            // 话术智能监控字段：直接透传，禁止 ?? 0 兜底（null=不改、显式0=关闭，见改造指引 §5.1）
            anchor.isScriptQualityInspection = anchorBo.isScriptQualityInspection;
            anchor.isScriptFidelityMonitor = anchorBo.isScriptFidelityMonitor;
            anchor.isInteractionPatrol = anchorBo.isInteractionPatrol;
            anchor.standardScriptId = anchorBo.standardScriptId;
            anchor.autoDeleteTime = anchorBo.autoDeleteTime;
            anchor.deleteContent = anchorBo.deleteContent;

            if (!string.IsNullOrEmpty(anchorBo.tradeId))
            {
                anchor.TradeId = anchorBo.tradeId;
            }
            if (anchorBo.recordTime != null)
            {
                // 微信视频号对录制监控时间的范围做有效性检查。
                if (anchorBo.platform == 2 ||
                    anchor.platform == 2)
                {
                    CheckRecordDateTimeRange(anchorBo.recordTime);
                }

                anchor.RecordTime = anchorBo.recordTime;
            }
            if (anchorBo.smsTip != null)
            {
                anchor.SmsTip = (int) anchorBo.smsTip;
            }
            if (anchorBo.isBarrageMonitoring != null)
            {
                // 只有抖音才有弹幕监控
                if (anchorBo.platform == 0)
                {
                    anchor.IsBarrageMonitoring = (int) anchorBo.isBarrageMonitoring;
                }
                else
                {
                    anchor.IsBarrageMonitoring = 0;
                }
            }
            if (anchorBo.isAutoUploadCloud != null)
            {
                anchor.IsAutoUploadCloud = (int)anchorBo.isAutoUploadCloud;
            }
            if (anchorBo.isDataViewing != null)
            {
                anchor.IsDataViewing = (int)anchorBo.isDataViewing;
            }
            if (anchorBo.isAutoRecord != null)
            {
                anchor.IsAutoRecord = (int)anchorBo.isAutoRecord;
            }
            if (anchorBo.accountType != null && string.IsNullOrEmpty(anchorBo.secUid))
            {
                anchor.AccountType = (int)anchorBo.accountType;
                if(anchor.AccountType == 0)
                {
                    // 自有账号，不开启数据看板
                    anchor.IsDataViewing = 0;
                }
            }
            else if(anchorBo.accountType != null)
            {
                anchor.AccountType = (int)anchorBo.accountType;
            }
            if(anchorBo.recordDefinition != null)
            {
                anchor.recordDefinition = anchorBo.recordDefinition;
            }else
            {
                anchor.recordDefinition = -1;
            }
            if (anchorBo.recordLimitType != null)
            {
                anchor.recordLimitType = anchorBo.recordLimitType;
            }
            else
            {
                anchor.recordLimitType = -1;
            }
            if (anchorBo.recordLimitValue != null)
            {
                anchor.recordLimitValue = anchorBo.recordLimitValue;
            }
            else
            {
                anchor.recordLimitType = 0;
            }
            if (anchorBo.isAutoAnalysis != null)
            {
                anchor.isAutoAnalysis = anchorBo.isAutoAnalysis;
            }
            else
            {
                anchor.isAutoAnalysis = 1;
            }
        }

        /// <summary>
        /// 检查微信视频号录制监控时间范围是否有效
        /// </summary>
        /// <param name="recordTime">录制监控时间范围</param>
        private void CheckRecordDateTimeRange(string recordTime)
        {
            var timeDurationList = recordTime.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);
            foreach (var timeDuration in timeDurationList)
            {
                // 获取主播的录制监控时间范围
                string[] arr = timeDuration.Split('-');
                var startTimeSpan = TimeSpan.Parse(arr[0]);
                var endTimeSpan = TimeSpan.Parse(arr[1]);

                var startDateTime = DateTime.Now.AddMinutes(startTimeSpan.TotalMinutes);
                var endDateTime = DateTime.Now.AddMinutes(endTimeSpan.TotalMinutes);

                // 结束时间小于开始时间时，结束时间增加1天，用于支持监控时间范围跨天选择。
                if (endDateTime < startDateTime)
                {
                    endDateTime = endDateTime.AddDays(1);
                }

                var recordMinutes = endDateTime.Subtract(startDateTime).TotalMinutes;

                if (recordMinutes < 10 ||
                    recordMinutes > 240)
                {
                    throw new CustomException($"微信视频号录制监控时间范围无效：{timeDuration}，监控时间范围必须大于10分钟，少于4小时。");
                }
            }
        }

        /// <summary>
        /// 构建添加的主播信息
        /// </summary>
        /// <param name="anchorUrl">主播url</param>
        /// <param name="urlType">主播url类型 0：抖音号链接 1：直播间链接</param>
        /// <param name="platform">平台类型 0：抖音 1：快手 2：视频号</param>
        /// <returns></returns>
        private async Task<AnchorInfo> BuildAddAnchor(string anchorUrl, int urlType, int platform)
        {
            // 判断本地是否存在
            AnchorInfo localAnchor = AnchorCacheManager.GetAnchorInfoByUrl(anchorUrl);
            if (localAnchor != null)
            {
                // 本地存在
                return localAnchor;
            }

            //// 判断服务器是否存在
            //List<string> urls = new List<string>();
            //urls.Add(anchorUrl);
            //List<AnchorUrlInfoVo> anchorUrlInfoVos = await AnchorApi.ListByUniques(urls);
            //if (anchorUrlInfoVos != null && anchorUrlInfoVos.Count > 0)
            //{
            //    foreach (var serverAnchor in anchorUrlInfoVos)
            //    {
            //        if (anchorUrl.Equals(serverAnchor.homeUrl) || anchorUrl.Equals(serverAnchor.liveUrl))
            //        {
            //            // 配置序列化忽略大小写，忽略null值
            //            var settings = new JsonSerializerSettings
            //            {
            //                ContractResolver = new Newtonsoft.Json.Serialization.CamelCasePropertyNamesContractResolver(),
            //                NullValueHandling = NullValueHandling.Ignore
            //            };
            //            AnchorInfo anchorInfo = JsonConvert.DeserializeObject<AnchorInfo>(JsonConvert.SerializeObject(serverAnchor), settings);
            //            return anchorInfo;
            //        }
            //    }
            //}

            try
            {
                // 从抖音获取主播信息
                AnchorInfo anchorInfo = null;

                if(platform == 0)
                {
                    anchorInfo = await DouYinAnchorBll.GetAnchorInfo(anchorUrl, urlType);
                }
                else if(platform == 1)
                {
                    anchorInfo = await KuaiShouAnchorBll.getAddKuaiShouAnchorInfo(anchorUrl);
                }
                else if (platform == 2)
                {
                    anchorInfo = WeChatChannelsAnchorBll.GetLatelyAuthorizationAnchorInfo();
                }

                return anchorInfo;
            }
            catch (Exception ex)
            {
                // 处理异常
                FileUtils.LogError($"{ex}", $"从抖音采集添加主播发生错误");
            }

            return null;
        }

        /// <summary>
        /// 授权主播巨量百应（使用 Fire-and-Forget 模式，避免重复调用卡顿）
        /// </summary>
        /// <param name="secUid">主播 SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        public void AuthorizeJuliang(string secUid, int authType)
        {
        
            lock (_lock)
            {
                if (JuliangUtils.authing)
                {
                    throw new CustomException("请先完成上一次授权");
                }
        
                // 判断资源是否足够
                UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                if (userPropertyEntity == null || userPropertyEntity.rpaAmountNum == null || userPropertyEntity.rpaAmountNum < 1)
                {
                    throw new CustomException("授权数量不足，请联系产品顾问");
                }
        
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }
        
                if(anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth)
                {
                    throw new CustomException("当前主播已经授权，无需重复授权");
                }
        
                UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.authing);
                JuliangUtils.authing = true;
                // 弹出巨量授权窗口
                JuliangUtils.openAuthorizeOrDataScreen(anchorInfo, "", "", false, false, authType);
            }

        }

        /// <summary>
        /// 统一修改巨量百应授权状态：更新内存、写本地缓存、通知服务端（authing中间态除外）。
        /// 所有修改 juliangAuthStatus 的位置必须走此方法，禁止直接赋值。
        /// </summary>
        /// <param name="anchor">主播对象（引用类型，直接修改其属性）</param>
        /// <param name="newStatus">新授权状态（JuliangAuthStatusEnum 整数值）</param>
        public static void UpdateJuliangAuthStatus(AnchorInfo anchor, int newStatus)
        {
            anchor.juliangAuthStatus = newStatus;
            AnchorCacheManager.SetAnchorCache(anchor);
            // authing(4) 是瞬时中间态，不通知服务端
            if (newStatus == JuliangAuthStatusEnum.authing) return;
            try
            {
                var dto = new UpdateAnchorUserDto
                {
                    secUid = anchor.SecUid,
                    authJlbyStatus = newStatus
                };
                AnchorApi.UpdateAnchorUserInfo(dto);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"同步巨量授权状态到服务端异常: {ex.Message}", "巨量授权");
            }
        }

        /// <summary>
        /// 统一修改千川授权状态：更新内存、写本地缓存、通知服务端（authing中间态除外）。
        /// 所有修改 qianchuanAuthStatus 的位置必须走此方法，禁止直接赋值。
        /// </summary>
        /// <param name="anchor">主播对象（引用类型，直接修改其属性）</param>
        /// <param name="newStatus">新授权状态（QianchuanAuthStatusEnum 整数值）</param>
        public static void UpdateQianchuanAuthStatus(AnchorInfo anchor, int newStatus)
        {
            anchor.qianchuanAuthStatus = newStatus;
            AnchorCacheManager.SetAnchorCache(anchor);
            if (newStatus == (int)QianchuanAuthStatusEnum.authing) return;
            try
            {
                var dto = new UpdateAnchorUserDto
                {
                    secUid = anchor.SecUid,
                    authQcStatus = newStatus
                };
                AnchorApi.UpdateAnchorUserInfo(dto);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"同步千川授权状态到服务端异常: {ex.Message}", "千川授权");
            }
        }

        /// <summary>
        /// 统一修改来客授权状态：更新内存、写本地缓存、通知服务端（authing中间态除外）。
        /// 所有修改 lifeAuthStatus 的位置必须走此方法，禁止直接赋值。
        /// </summary>
        public static void UpdateLifeAuthStatus(AnchorInfo anchor, int newStatus)
        {
            anchor.lifeAuthStatus = newStatus;
            AnchorCacheManager.SetAnchorCache(anchor);
            if (newStatus == (int)LifeAuthStatusEnum.authing) return;
            try
            {
                var dto = new UpdateAnchorUserDto
                {
                    secUid = anchor.SecUid,
                    authLifeStatus = newStatus
                };
                AnchorApi.UpdateAnchorUserInfo(dto);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"同步来客授权状态到服务端异常: {ex.Message}", "来客授权");
            }
        }

        /// <summary>
        /// 授权主播千川（使用 Fire-and-Forget 模式，避免重复调用卡顿）
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        public void AuthorizeQianchuan(string secUid, int authType)
        {
            // 快速检查，避免重复调用阻塞
            if (QianchuanUtils.authing)
            {
                throw new CustomException("请先完成上一次授权");
            }

                    lock (_lock)
                    {
                        if (QianchuanUtils.authing)
                        {
                            throw new CustomException("请先完成上一次授权");
                        }

                        // 判断资源是否足够
                        // UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                        // if (userPropertyEntity == null || userPropertyEntity.rpaAmountNum == null || userPropertyEntity.rpaAmountNum < 1)
                        // {
                        //     throw new CustomException("授权数量不足，请联系产品顾问");
                        // }

                        AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                        if (anchorInfo == null)
                        {
                            throw new CustomException("主播信息不存在");
                        }

                        if (anchorInfo.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.auth)
                        {
                            throw new CustomException("当前主播已经授权，无需重复授权");
                        }

                        UpdateQianchuanAuthStatus(anchorInfo, 4);
                        QianchuanUtils.authing = true;
                    }

                    // 锁外执行窗体弹出操作
                    AnchorInfo anchorInfoForForm = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                    // 弹出千川授权窗口
                    QianchuanUtils.openAuthorizeOrDataScreen(anchorInfoForForm, "", "", false, false, authType);
        }

        /// <summary>
        /// 授权主播来客（使用 Fire-and-Forget 模式，避免重复调用卡顿）
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        public void AuthorizeLife(string secUid, int authType)
        {
            if (ReviewAnalysis.life.LifeUtils.authing)
            {
                throw new CustomException("请先完成上一次授权");
            }

            lock (_lock)
            {
                if (ReviewAnalysis.life.LifeUtils.authing)
                {
                    throw new CustomException("请先完成上一次授权");
                }

                // 查询来客授权剩余数量
                AuthUsageVo authUsage = AnchorApi.GetAuthUsage(3);
                if (authUsage == null || authUsage.remainingCount <= 0)
                {
                    throw new CustomException("来客授权数量不足，请联系产品顾问");
                }

                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }

                if (anchorInfo.lifeAuthStatus == (int)LifeAuthStatusEnum.auth)
                {
                    throw new CustomException("当前主播已经授权，无需重复授权");
                }

                UpdateLifeAuthStatus(anchorInfo, 4);
                ReviewAnalysis.life.LifeUtils.authing = true;
            }

            AnchorInfo anchorInfoForForm = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            ReviewAnalysis.life.LifeUtils.openAuthorizeOrDataScreen(anchorInfoForForm, "", "", false, false, authType);
        }

        /// <summary>
        /// 取消授权主播来客
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public void CancelAuthorizeLife(string secUid)
        {
            try
            {
                FileUtils.LogRpa($"开始取消来客授权, secUid={secUid}", "来客授权");

                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }

                // 1. 关闭已打开的来客登录窗体
                try
                {
                    ReviewAnalysis.life.LifeUtils.closeFormBySecUid(secUid);
                    FileUtils.LogRpa($"已关闭来客登录窗体: {anchorInfo.AnchorName}", "来客授权");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"关闭来客窗体异常(非致命): {ex.Message}", "来客授权");
                }

                // 2. 停止来客数据采集轮询
                try
                {
                    ReviewAnalysis.life.LifeDataCollectionManager.StopPolling(secUid);
                    FileUtils.LogRpa($"已停止来客数据采集轮询: {anchorInfo.AnchorName}", "来客授权");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"停止来客数据采集异常(非致命): {ex.Message}", "来客授权");
                }

                // 3. 删除本地Cookie文件
                try
                {
                    string cookiePath = ReviewAnalysis.life.LifeUtils.getCookiePath(secUid);
                    if (File.Exists(cookiePath))
                    {
                        File.Delete(cookiePath);
                        FileUtils.LogRpa($"已删除来客Cookie文件: {cookiePath}", "来客授权");
                    }
                    else
                    {
                        FileUtils.LogRpa($"来客Cookie文件不存在(可能从未授权): {cookiePath}", "来客授权");
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"删除来客Cookie文件异常(非致命): {ex.Message}", "来客授权");
                }

                // 4. 清理 SecUid.json 中对应记录（secUid/cookiePath 置空，status 设为 inactive）
                try
                {
                    ReviewAnalysis.life.LifeUtils.ClearAwemeUserInSecUidJson(secUid);
                    FileUtils.LogRpa($"已清理SecUid.json中的授权记录: {anchorInfo.AnchorName}", "来客授权");
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"清理SecUid.json异常(非致命): {ex.Message}", "来客授权");
                }

                // 5. 更新本地缓存状态并同步服务端
                UpdateLifeAuthStatus(anchorInfo, 0);
                FileUtils.LogRpa($"已更新本地缓存状态为未授权: {anchorInfo.AnchorName}", "来客授权");

                // 6. 删除缓存版本配置
                ReviewAnalysis.life.LifeUtils.deleteAnchorCacheVersion(secUid);

                FileUtils.LogRpa($"取消来客授权成功: {anchorInfo.AnchorName}", "来客授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"取消来客授权异常：{ex}", "CancelAuthorizeLife");
                throw;
            }
        }

        /// <summary>
        /// 授权主播企业号（使用 Fire-and-Forget 模式，避免重复调用卡顿）
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        public void AuthorizeEnterprise(string secUid, int authType)
        {
            if (ReviewAnalysis.enterprise.EnterpriseUtils.authing)
            {
                throw new CustomException("请先完成上一次授权");
            }
                    lock (_lock)
                    {
                        if (ReviewAnalysis.enterprise.EnterpriseUtils.authing)
                        {
                            throw new CustomException("请先完成上一次授权");
                        }

                        UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                        if (userPropertyEntity == null || userPropertyEntity.rpaAmountNum == null || userPropertyEntity.rpaAmountNum < 1)
                        {
                            throw new CustomException("授权数量不足，请联系产品顾问");
                        }

                        AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                        if (anchorInfo == null)
                        {
                            throw new CustomException("主播信息不存在");
                        }

                        if (anchorInfo.enterpriseAuthStatus == 1)
                        {
                            throw new CustomException("当前主播已经授权，无需重复授权");
                        }

                        anchorInfo.enterpriseAuthStatus = 4;
                        AnchorCacheManager.SetAnchorCache(anchorInfo);
                        ReviewAnalysis.enterprise.EnterpriseUtils.authing = true;
                    }

                    AnchorInfo anchorInfoForForm = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                    ReviewAnalysis.enterprise.EnterpriseUtils.openAuthorizeOrDataScreen(anchorInfoForForm, "", "", false, false, authType);

        }

        /// <summary>
        /// 授权主播后台
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="authType">登录类型 0：登录主账号 1：登录子账号</param>
        public void AuthorizeAnchorLive(string secUid, int authType)
        {
            if (ReviewAnalysis.anchorLive.AnchorLiveUtils.authing)
            {
                throw new CustomException("请先完成上一次授权");
            }
                    lock (_lock)
                    {
                        if (ReviewAnalysis.anchorLive.AnchorLiveUtils.authing)
                        {
                            throw new CustomException("请先完成上一次授权");
                        }

                        UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                        if (userPropertyEntity == null || userPropertyEntity.rpaAmountNum == null || userPropertyEntity.rpaAmountNum < 1)
                        {
                            throw new CustomException("授权数量不足，请联系产品顾问");
                        }

                        AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                        if (anchorInfo == null)
                        {
                            throw new CustomException("主播信息不存在");
                        }

                        if (anchorInfo.anchorLiveAuthStatus == 1)
                        {
                            throw new CustomException("当前主播已经授权，无需重复授权");
                        }

                        anchorInfo.anchorLiveAuthStatus = 4;
                        AnchorCacheManager.SetAnchorCache(anchorInfo);
                        ReviewAnalysis.anchorLive.AnchorLiveUtils.authing = true;
                    }

                    AnchorInfo anchorInfoForForm = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                    ReviewAnalysis.anchorLive.AnchorLiveUtils.openAuthorizeOrDataScreen(anchorInfoForForm, "", "", false, false, authType);

        }

        /// <summary>
        /// 取消授权主播企业号
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public void CancelAuthorizeEnterprise(string secUid)
        {
            try
            {
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }

                anchorInfo.enterpriseAuthStatus = 0;
                AnchorCacheManager.SetAnchorCache(anchorInfo);

                ReviewAnalysis.enterprise.EnterpriseUtils.deleteAnchorCacheVersion(secUid);
                ReviewAnalysis.enterprise.EnterpriseUtils.recountEnterpriseProperty();

                FileUtils.LogRpa($"取消企业号授权成功: {anchorInfo.AnchorName}", "企业号授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"取消企业号授权异常：{ex}", "CancelAuthorizeEnterprise");
                throw;
            }
        }

        /// <summary>
        /// 取消授权主播千川
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public void CancelAuthorizeQianchuan(string secUid)
        {
            try
            {
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }

                // 更新授权状态并清理授权文件
                QianchuanUtils.HandleAuthExpired(secUid, (int)QianchuanAuthStatusEnum.unAuth);

                FileUtils.LogRpa($"取消千川授权成功: {anchorInfo.AnchorName}", "千川授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"取消千川授权异常：{ex}", "CancelAuthorizeQianchuan");
                throw;
            }
        }

        /// <summary>
        /// 取消授权主播后台
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public void CancelAuthorizeAnchorLive(string secUid)
        {
            try
            {
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }

                anchorInfo.anchorLiveAuthStatus = 0;
                AnchorCacheManager.SetAnchorCache(anchorInfo);

                // 停止主播后台数据采集轮询
                AnchorLiveDataCollectionManager.StopPolling(secUid);

                // 删除 Cookie 文件
                string cookiePath = AnchorLiveUtils.getCookiePath(secUid);
                if (File.Exists(cookiePath))
                {
                    File.Delete(cookiePath);
                    FileUtils.LogRpa($"已删除主播后台Cookie文件: {cookiePath}", "主播后台授权");
                }

                FileUtils.LogRpa($"取消主播后台授权成功: {anchorInfo.AnchorName}", "主播后台授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"取消主播后台授权异常：{ex}", "CancelAuthorizeAnchorLive");
                throw;
            }
        }

        /// <summary>
        /// 授权抖音
        /// </summary>
        public void AuthorizeDouyin()
        {
            if (DouyinAuth.DouyinAuthUtils.authing)
            {
                throw new CustomException("请先完成上一次授权");
            }
            lock (_lock)
            {
                if (DouyinAuth.DouyinAuthUtils.authing)
                {
                    throw new CustomException("请先完成上一次授权");
                }

                DouyinAuth.DouyinAuthUtils.authing = true;
            }

            // 清除旧 Cookie，确保强制重新扫码，不受旧登录状态影响
            string cookiePath = DouyinAuth.DouyinAuthUtils.getCookiePath();
            if (File.Exists(cookiePath))
            {
                File.Delete(cookiePath);
                FileUtils.LogRpa($"已清除旧抖音Cookie，强制重新授权：{cookiePath}", "抖音授权");
            }

            DouyinAuth.DouyinAuthUtils.openAuthorizeOrDataScreen();
        }

        /// <summary>
        /// 取消授权抖音
        /// </summary>
        public void CancelAuthorizeDouyin()
        {
            try
            {
                // 删除 Cookie 文件
                string cookiePath = DouyinAuth.DouyinAuthUtils.getCookiePath();
                if (File.Exists(cookiePath))
                {
                    File.Delete(cookiePath);
                    FileUtils.LogRpa($"已删除抖音Cookie文件: {cookiePath}", "抖音授权");
                }

                FileUtils.LogRpa("取消抖音授权成功", "抖音授权");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"取消抖音授权异常：{ex}", "CancelAuthorizeDouyin");
                throw;
            }
        }

        /// <summary>
        /// 无窗体拉取巨量数据（直接使用 JuliangedDataPuller HTTP 采集）
        /// </summary>
        public async Task PullJuliangDataAsync(string videoId)
        {
            try
            {
                VideoEntity videoEntity = VideoApi.GetVideoByVideoIdSync(videoId);
                if (videoEntity == null)
                {
                    FileUtils.LogRpa($"视频信息不存在，videoId={videoId}", "pullJuliang");
                    return;
                }

                if (DateTime.TryParseExact(videoEntity.startTime, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out DateTime parsedTime))
                {
                    DateTime now = DateTime.Now;
                    DateTime targetTime = parsedTime.AddDays(7);
                    if (now > targetTime)
                    {
                        FileUtils.LogRpa($"无法对7天外的视频操作，videoId={videoId}", "pullJuliang");
                        return;
                    }
                }

                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(videoEntity.secUid);
                if (anchorInfo == null)
                {
                    FileUtils.LogRpa($"主播信息不存在，secUid={videoEntity.secUid}", "pullJuliang");
                    return;
                }

                if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                {
                    FileUtils.LogRpa($"主播未授权，anchorName={anchorInfo.AnchorName}", "pullJuliang");
                    return;
                }

                FileUtils.LogRpa($"开始无窗体拉取巨量数据，videoId={videoId}", "pullJuliang");

                // 使用 JuliangedDataPuller 直接 HTTP 拉取
                var puller = new JuliangedDataPuller();
                string rawJson = await puller.PullDataAsyncErrorMsg(anchorInfo, videoEntity.batchNumber, videoEntity.videoId);
                if (string.IsNullOrEmpty(rawJson))
                {
                    FileUtils.LogRpa($"巨量数据拉取返回空，videoId={videoId}", "pullJuliang");
                    return;
                }

                // 使用旧逻辑直接解析原始JSON写入finish文件（复用AnchorInstance的静态方法，保证字段100%兼容）
                AnchorInstance.WriteJuliangLegacyByOldLogicStatic(rawJson, videoEntity.videoId, anchorInfo.SecUid, videoEntity.batchNumber, anchorInfo.AnchorName);
                FileUtils.LogRpa("已调用旧逻辑写入巨量finish文件", "pullJuliang");

                // 上传巨量数据
                JuliangDataHandle.dataUpload(videoEntity.batchNumber, videoEntity.videoId, anchorInfo.SecUid);
                FileUtils.LogRpa($"无窗体拉取巨量数据完成并上传，videoId={videoId}", "pullJuliang");

                // 通知前端刷新完成（替代原临时窗体关闭前的回调）
                try
                {
                    FrontNotice frontNotice = new FrontNotice();
                    var requestDataObj = new Dictionary<string, object>();
                    requestDataObj["code"] = 0;
                    requestDataObj["status"] = 200;
                    requestDataObj["action"] = "juliangPullDataSuccess";
                    requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
                    frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
                    FileUtils.LogRpa($"已通知前端巨量数据拉取完成，videoId={videoId}", "pullJuliang");
                }
                catch (Exception noticeEx)
                {
                    FileUtils.LogRpa($"通知前端异常: {noticeEx.Message}", "pullJuliang");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"无窗体拉取巨量数据异常: {ex.Message}", "pullJuliang");
            }
        }

        /// <summary>
        /// 授权微信视频号
        /// </summary>
        /// <param name="redirectUrl">授权成功的返回地址</param>
        public void AuthorizeWeChatChannels(string redirectUrl)
        {
            WeChatChannelsUtils.OpenAuthorizationUrl(redirectUrl);
        }

        /// <summary>
        /// 获取单个直播信息
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public AnchorDto getAnchorInfo(string secUid)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorInfo == null)
            {
                return null;
            }

            AnchorDto anchorDto = JsonConvert.DeserializeObject<AnchorDto>(JsonConvert.SerializeObject(anchorInfo));
            return anchorDto;
        }

        /// <summary>
        /// 获取单个直播信息
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public AiAnchorInfoVo getAiAnchorInfo(string secUid)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if (anchorInfo == null)
            {
                return null;
            }

            AiAnchorInfoVo anchorDto = JsonConvert.DeserializeObject<AiAnchorInfoVo>(JsonConvert.SerializeObject(anchorInfo));
            return anchorDto;
        }

        /// <summary>
        /// 更新ai页面的部分主播字段
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public void updateAiPartial(AiAnchorInfoVo vo)
        { 
            // 保存服务器
            AiAnchorInfoVo aiConfig = AnchorApi.updateAiPartial(vo);
            if (aiConfig == null || aiConfig.isUpdate == null || !(bool) aiConfig.isUpdate || string.IsNullOrEmpty(vo.secUid))
            {
                return;
            }
            
            AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(vo.secUid);
            if (anchor == null)
            {
                return;
            }
            anchor.AccountType = vo.accountType ?? anchor.AccountType;
            anchor.premiereDate = vo.premiereDate ?? anchor.premiereDate;
            anchor.accountStage = vo.accountStage ?? anchor.accountStage;
            anchor.accountWaterLevel = vo.accountWaterLevel ?? anchor.accountWaterLevel;
            anchor.accountFlow = vo.accountFlow ?? anchor.accountFlow;
            anchor.livingTarget = vo.livingTarget ?? anchor.livingTarget;
            anchor.livingModality = vo.livingModality ?? anchor.livingModality;
            anchor.marketing = vo.marketing ?? anchor.marketing;
            anchor.optimizeDirection = vo.optimizeDirection ?? anchor.optimizeDirection;
            anchor.learning = vo.learning ?? anchor.learning;
            anchor.livingMode = vo.livingMode ?? anchor.livingMode;
            anchor.AnchorSituation = vo.anchorSituation ?? anchor.AnchorSituation;
            anchor.roiAccuracy = vo.roiAccuracy ?? anchor.roiAccuracy;

            // 添加到缓存
            AnchorCacheManager.SetAnchorCache(anchor);
        }

        /// <summary>
        /// 取消授权主播巨量百应
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public void cancelAuthorizeJuliang(string secUid)
        {

            lock (_lock)
            {
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                if (anchorInfo == null)
                {
                    throw new CustomException("主播信息不存在");
                }

                if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
                {
                    throw new CustomException("主播未授权");
                }

                // 关闭窗体
                JuliangUtils.closeFormBySecUid(secUid);
                
                // 删除授权Cookie文件
                string juliangCookiePath = JuliangUtils.getCookiePath(secUid);
                if (File.Exists(juliangCookiePath))
                {
                    File.Delete(juliangCookiePath);
                }

                // 删除缓存文件夹版本号
                JuliangUtils.deleteAnchorCacheVersion(secUid);

                // 删除缓存文件夹
                //string cachePath = JuliangUtils.getCachePath(secUid);

                // 延迟删除缓存文件夹，给 CEF 一点时间释放锁
                //Task.Run(async () =>
                //{
                //    await Task.Delay(1000); // 等待1秒，避免占用
                //    try
                //    {
                //        if (Directory.Exists(cachePath))
                //        {
                //            Directory.Delete(cachePath, true);
                //        }
                //    }
                //    catch (Exception ex)
                //    {
                //        FileUtils.LogError($"{ex}", $"删除缓存目录失败: {cachePath}");
                //    }
                //});

                //本地每个主播加个授权版本号标识，取消授权的时候改变这个版本号标识，登录的时候判断如果不是这个版本号的，删除缓存文件夹

                //Task.Run(async () => {
                //    long startTime = ServerTimeUtils.getCurrentTime();
                //    if (Directory.Exists(cachePath))
                //    {

                //        // 尝试10秒
                //        while (true)
                //        {
                //            long currTime = ServerTimeUtils.getCurrentTime();
                //            try
                //            {
                //                if (currTime - startTime > 15000)
                //                {
                //                    break;
                //                }
                //                Directory.Delete(cachePath, true);
                //            }
                //            catch (Exception ex)
                //            {
                //                FileUtils.LogError($"{ex}", $"删除巨量授权缓存失败==={anchorInfo.RemarksName}");
                //            }
                //        }
                //    }
                //});

                //string cachePath = JuliangUtils.getCachePath(secUid) + "\\flag.txt";
                //if (File.Exists(cachePath))
                //{
                //    File.Delete(cachePath);
                //}

                // 加回1个资源
                //UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                //if (userPropertyEntity == null)
                //{
                //    throw new CustomException("网络异常，获取资源失败");
                //}
                //if (userPropertyEntity.useRpaAmountNum == null)
                //{
                //    throw new CustomException("无资源数据，请联系产品顾问");
                //}
                //int useRpaAmountNum = (int)userPropertyEntity.useRpaAmountNum;
                //useRpaAmountNum = useRpaAmountNum - 1 < 0 ? 0 : useRpaAmountNum - 1;
                //UserPropertyApi.updateRpaPropertyUseNum(useRpaAmountNum);
                

                // 修改主播巨量授权状态
                UpdateJuliangAuthStatus(anchorInfo, JuliangAuthStatusEnum.unAuth);
                ModifyLuopanCookieExpiry(anchorInfo);

                // 重新计算巨量监控资源
                JuliangUtils.recountJuliangProperty();
            }
            
        }
        ///// <summary>
        ///// 修改罗盘 Cookie 的过期时间
        ///// </summary>
        public void ModifyLuopanCookieExpiry(AnchorInfo anchorInfo)
        {
            
            try
            {
                var localCookies = new juliang.CookiePersistenceHelper(JuliangUtils.getCookiePath(anchorInfo.SecUid)).LoadCefCookiesFromLocal(false);
                var localLuopanDtCookie = localCookies?.Where(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal))?.ToList();
                if (localLuopanDtCookie == null)
                {
                    FileUtils.log("未找到罗盘 Cookie（LUOPAN_DT），无法修改过期时间");
                    return;
                }

                var newExpiryTime = DateTime.Now.AddDays(-62);
                bool isSuccess = new juliang.CookiePersistenceHelper(JuliangUtils.getCookiePath(anchorInfo.SecUid))
                    .ModifyLocalCookieExpiry("COMPASS_LUOPAN_DT", "jinritemai.com", newExpiryTime);

                if (isSuccess)
                {
                    FileUtils.log($"罗盘 Cookie 过期时间已修改为：{newExpiryTime}");
                }
                else
                {
                    FileUtils.log("罗盘 Cookie 过期时间修改失败");
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"修改 Cookie 过期时间异常：{ex.Message}");
            }
        }

        /// <summary>
        /// 从恢复列表删除主播
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public static void fullRemoveAnchor(string secUid)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
        
            if (anchorInfo == null)
            {
                throw new CustomException("主播信息不存在");
            }
        
            // 将主播信息同步到服务器
            AnchorApi.UpdateUserAnchorSync(secUid, "", -1, 2);
        
            // 修改主播信息
            anchorInfo.IsRemoveRecord = 2;
            AnchorCacheManager.SetAnchorCache(anchorInfo);
        }

        /// <summary>
        /// 启动抖音排班数据采集后台线程
        /// </summary>
        public void StartDouyinScheduleDataCollectThread()
        {
            Thread thread = new Thread(async () =>
            {
                while (true)
                {
                    try
                    {
                        // 1. 从 AnchorCacheManager 获取抖音平台主播
                        List<AnchorInfo> douyinAnchors = AnchorCacheManager.GetNotRemoveAnchorListByPlatForm(0);

                        if (douyinAnchors != null && douyinAnchors.Count > 0)
                        {
                            // 2.获取所有有排班的主播SecUid列表（不限于今天）
                            // List<string> scheduledSecUids = AnchorScheduleCacheManager.GetAllScheduledAnchorSecUids();

                            // 添加日志：显示有排班的主播信息
                            // if (scheduledSecUids.Count > 0)
                            // {
                            //     // 获取主播名称以便日志更易读
                            //     List<string> anchorNames = new List<string>();
                            //     foreach (var secUid in scheduledSecUids)
                            //     {
                            //         var anchor = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
                            //         if (anchor != null)
                            //         {
                            //             anchorNames.Add($"{anchor.AnchorName}({secUid})");
                            //         }
                            //         else
                            //         {
                            //             anchorNames.Add(secUid);
                            //         }
                            //     }
                            //     FileUtils.LogRecrd($"有排班的主播共 {scheduledSecUids.Count} 个: {string.Join(", ", anchorNames)}", "抖音排班数据采集");
                            // }
                            // else
                            // {
                            //     FileUtils.LogRecrd("当前没有排班的主播", "抖音排班数据采集");
                            // }

                            foreach (var anchorInfo in douyinAnchors)
                            {
                                // 3. 检测主播是否已开播（不再依赖排班，对所有抖音主播检测）
                                int onlineStatus = DouYinAnchorBll.GetOnlineStatus(anchorInfo.AnchorUserId);
                                if (onlineStatus == 1)
                                {
                                    // 已开播，采集开播信息
                                    // 先尝试方案一
                                    AnchorInfoWebClient anchorInfoWebClient = AnchorInfoWebClient.Instance;
                                    DouYinAnchorInfoEntity entity = await anchorInfoWebClient.GetLiveInfoByAnchorNumber(anchorInfo.anchorNumber);
                                    if (entity == null)
                                    {
                                        // 继续尝试方案二
                                        entity = await DouYinAnchorBll.GetDouYinAnchorInfo(anchorInfo.LiveUrl);
                                    }

                                    if (entity != null)
                                    {
                                        // 通知采集数据
                                        try
                                        {
                                            var scheduler = JuliangUtils.formMain?.CollectionScheduler;
                                            if (scheduler != null && !string.IsNullOrEmpty(entity.RoomId))
                                            {
                                                // 如果调度器未启动，先启动检测线程
                                                if (!scheduler.IsRunning)
                                                {
                                                    scheduler.StartAnchorOnlineDetection();
                                                    FileUtils.LogRpa("调度器已自动启动", anchorInfo?.AnchorName);
                                                }

                                                string videoId = null;
                                                if (AnchorBll.recordingList.TryGetValue(anchorInfo.SecUid, out AnchorRecordBll temp))
                                                {
                                                    videoId = temp?.GetVideo()?.videoId ?? null;
                                                }
                                                scheduler.StartAnchorCollection(anchorInfo, entity.RoomId, videoId);
                                            }
                                        }
                                        catch (Exception ex)
                                        {
                                            FileUtils.LogRpa($"启动抖音数据采集后台线程,启动采集异常：{ex.Message}", anchorInfo?.AnchorName);
                                        }
                                    }
                                }
                                else
                                {
                                    // 主播已下线，停止数据采集
                                    var scheduler = JuliangUtils.formMain?.CollectionScheduler;
                                    try
                                    {
                                        if (scheduler != null)
                                        {
                                            // 计算视频结束时间，触发最后一轮采集
                                            string videoEndTime = null;
                                            if (recordingList.TryGetValue(anchorInfo.SecUid, out var recordBll))
                                            {
                                                var video = recordBll.GetVideo();
                                                if (video != null)
                                                {
                                                    // 优先取已计算的 endTime，否则从 startTime + duration 计算
                                                    if (!string.IsNullOrEmpty(video.endTime))
                                                    {
                                                        videoEndTime = video.endTime;
                                                    }
                                                    else if (!string.IsNullOrEmpty(video.startTime))
                                                    {
                                                        try
                                                        {
                                                            DateTime start = DateTime.ParseExact(video.startTime, "yyyy-MM-dd HH:mm:ss", null);
                                                            long dur = long.TryParse(video.duration, out long d) ? d : 0;
                                                            videoEndTime = start.AddSeconds(dur).ToString("yyyy-MM-dd HH:mm:ss");
                                                        }
                                                        catch (Exception ex)
                                                        {
                                                            FileUtils.LogRpa($"计算视频结束时间异常：{ex.Message}", anchorInfo?.AnchorName);
                                                        }
                                                    }
                                                }
                                            }

                                            if (!string.IsNullOrEmpty(videoEndTime))
                                            {
                                                await scheduler.CollectOnceAsync(anchorInfo.SecUid, videoEndTime);
                                            }
                                        }
                                    }
                                    catch (Exception ex)
                                    {
                                        FileUtils.LogRpa($"主播下播最后一轮采集异常：{ex.Message}", anchorInfo?.AnchorName);
                                    }
                                    finally
                                    {
                                        // 确保即使采集异常也停止定时器
                                        if (scheduler != null)
                                        {
                                            try
                                            {
                                                scheduler.StopAnchorCollection(anchorInfo.SecUid, true);
                                                FileUtils.LogRpa($"主播已下播，停止数据采集", anchorInfo?.AnchorName);
                                            }
                                            catch (Exception ex)
                                            {
                                                FileUtils.LogRpa($"停止采集调度器异常：{ex.Message}", anchorInfo?.AnchorName);
                                            }
                                        }
                                    }
                                }
                                Thread.Sleep(5000);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex}", "抖音排班数据采集线程发生异常");
                    }

                    Thread.Sleep(30000); // 一轮遍历完成后等待30秒
                }
            })
            {
                IsBackground = true,
                Name = "DouyinScheduleDataCollectThread"
            };
            thread.Start();
        }

        /// <summary>
        /// 从恢复列表添加
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        public async void reAddAnchor(string secUid)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            anchorInfo.IsRemoveRecord = 0;
            anchorInfo.IsAutoRecord = 1;

            anchorInfo.isScriptQualityInspection = 0;
            anchorInfo.isScriptFidelityMonitor = 0;
            anchorInfo.isInteractionPatrol = 0;

            // 同步到服务器
            AnchorApi.AddOrUpdateAnchorSync(anchorInfo);

            // 修改缓存
            AnchorCacheManager.SetAnchorCache(anchorInfo);

            // 判断主播是否存在于已录制/待录制列表，不存在则添加到待录制列表
            if (isDetection && anchorInfo.IsAutoRecord == 1)
            {
                NotExistAdd(anchorInfo);
            }
        }
        /// <summary>
        /// 授权失效的处理
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="juliangStatus">授权状态</param>
        private void loginExpire(AnchorInfo anchorInfo, int juliangStatus)
        {
            if (anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth)
            {
                return;
            }
            // 修改主播的授权状态
            UpdateJuliangAuthStatus(anchorInfo, juliangStatus);
            // 删除本地缓存授权版本号
            JuliangUtils.deleteAnchorCacheVersion(anchorInfo.SecUid);
            // 重新设置资产
            JuliangUtils.recountJuliangProperty();

            // 通知前端
            FrontNotice frontNotice = new FrontNotice();
            var requestDataObj = new Dictionary<string, object>();
            requestDataObj["code"] = 0;
            requestDataObj["status"] = 200;
            requestDataObj["action"] = "juliangAuthExpires";
            requestDataObj.Add("data", JsonConvert.SerializeObject(anchorInfo));
            frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            // 关闭窗体
            //this.closeForm();
        }
        
        /// <summary>
        /// 打开主播的巨量百应页面
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        public void openAnchorJuliang(string secUid)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorInfo == null)
            {
                throw new Exception("主播信息不存在");
            }

            if(anchorInfo.juliangAuthStatus != 1)
            {
                throw new Exception("主播未授权");
            }

            string cachePath = JuliangUtils.getCachePath(anchorInfo.SecUid);
            //string url = "https://compass.jinritemai.com/talent";
            string url = "https://buyin.jinritemai.com/dashboard";

            // 创建新窗体
            Form juliangForm = new Form
            {
                Text = $"巨量百应-{anchorInfo.AnchorName}",
                Size = new Size(1400, 900),
                StartPosition = FormStartPosition.CenterScreen,
                Icon = new Icon(FormMain.icoPath)
            };

            // 配置浏览器设置
            BrowserSettings browserSettings = new BrowserSettings();

            // 配置请求上下文设置，指定缓存路径
            RequestContextSettings requestContextSettings = new RequestContextSettings
            {
                CachePath = cachePath,
                PersistSessionCookies = false
            };
            // 从本地读取Cookie
            var localCookies = new juliang.CookiePersistenceHelper(JuliangUtils.getCookiePath(anchorInfo.SecUid)).LoadCefCookiesFromLocal(false);

            var luopanDtCookie = localCookies?.FirstOrDefault(cookie => cookie.Name.Equals("COMPASS_LUOPAN_DT", StringComparison.Ordinal));
            if (luopanDtCookie == null || string.IsNullOrWhiteSpace(luopanDtCookie.Value))
            {
                //loginExpire(anchorInfo, JuliangAuthStatusEnum.unAuth);
            }
            else if (luopanDtCookie.Expires < DateTime.Now)
            {
                //loginExpire(anchorInfo, JuliangAuthStatusEnum.authExpires);
            }
            else
            {

                //loadHomeFinishHandle(anchorInfo);
            }

            var cefManager = new CefRequestContextManager(cachePath);
            // 创建CEF浏览器实例
            ChromiumWebBrowser browser = new ChromiumWebBrowser(url)
            {
                BrowserSettings = browserSettings,
                RequestContext = new RequestContext(requestContextSettings),//new CefRequestContextManager(cachePath).GetRequestContext(),
                Dock = DockStyle.Fill
            };
            // 注入Cookie到RequestContext
            var cookieManager = browser.RequestContext.GetCookieManager(null);
            //foreach (var cookie in localCookies)
            //{
            //    // 注入Cookie（需拼接完整URL，确保域名匹配）
            //    var injectUrl = $"https://{cookie.Domain.TrimStart('.')}";
            //    cookieManager.SetCookieAsync(injectUrl, cookie);
            //}

            // 添加浏览器到窗体
            juliangForm.Controls.Add(browser);

            // 窗体关闭时释放浏览器资源
            juliangForm.FormClosing += (sender, e) =>
            {
                if (browser != null && !browser.IsDisposed)
                {
                    browser.Dispose();
                }
            };
            //juliangForm.Load += (sender, e) =>
            //{
            //    if (browser != null)
            //    {
            //        // 可选：保存最新Cookie到本地（更新过期时间）

            //        var allCookies = cookieManager.VisitUrlCookiesAsync(
            //            url: "https://buyin.jinritemai.com",       // 目标 URL（带协议，精准定位域名）  https://compass.jinritemai.com
            //            includeHttpOnly: true    // 必须设为 true！登录态通常在 HttpOnly Cookie 中
            //                                     //matchDomain: false        // 模糊匹配（兼容子域名，如 .buyin.jinritemai.com）
            //        ).Result;
            //        new CookiePersistenceHelper(cachePath).SaveCefCookiesToLocal(allCookies, false);
            //        // 强制将内存中的Cookie刷入磁盘
            //        cookieManager.FlushStoreAsync();
            //    }
            //};

            // 显示窗体
            juliangForm.Show();


            //JuliangUtils.authing = true;
            //// 弹出巨量授权窗口
            //JuliangUtils.openAuthorizeOrDataScreen(anchorInfo, "", "", false, false, 1, false);

            //var juliangForm = new JuliangForm()
            //{
            //    anchorInfo = anchorInfo,
            //    //roomId = roomId,
            //    //videoId = videoId,
            //    isTemp = true,
            //    tempLoadDataFlagSet = new HashSet<string>(),
            //    allowLoadDataScreen = true,
            //    isFrontPull = false,
            //    authType = 1
            //};
            //juliangForm.init(false);
            ////juliangForm.showForm();
        }
    }
}
