using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor.Entity;
using ReviewAnalysis.Bll.VedioModels;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using Swan;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using MediaInfo;
using System.Runtime.InteropServices;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.api;
using ReviewAnalysis.bo.video;
using ReviewAnalysis.Websocket;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.life;
using ReviewAnalysis.enterprise;
using ReviewAnalysis.anchorLive;
using ReviewAnalysis.juliangApi;
using ReviewAnalysis.enumeration.anchor;
using CefSharp.DevTools.DOM;
using Newtonsoft.Json;
using ReviewAnalysis.Dto;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.WeChatChannels.Services;
using ReviewAnalysis.enums;

namespace ReviewAnalysis.Bll.Anchor
{
    public class AnchorRecordBll
    {
        /// <summary>
        /// 主播信息
        /// </summary>
        private AnchorInfo anchorInfo {  get; set; }

        /// <summary>
        /// 录制的清晰度 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public int recordDefinition;
        /// <summary>
        /// 录制形式 0：无限制录制 1：限制时长录制(只录一段) 2：时长分段录制
        /// </summary>
        public int recordLimitType;
        /// <summary>
        /// 录制的时长，单位：分钟
        /// </summary>
        public int recordLimitValue;
        /// <summary>
        /// 是否自动分析视频 0：否 1：是
        /// </summary>
        public int isAutoAnalysis;

        /// <summary>
        /// 当前录制的是第几段，从0开始
        /// </summary>
        public int currentParagraphNum = 0;
        /// <summary>
        /// 视频的推流地址
        /// </summary>
        private string streamUrl;
        /// <summary>
        /// 任务id
        /// </summary>
        public volatile string taskId;
        /// <summary>
        /// 是否是重启的录制
        /// </summary>
        public bool isRestartRecord = false;
        /// <summary>
        /// 重启重试的次数
        /// </summary>
        public int restartCount = 0;
        /// <summary>
        /// 录制错误回调次数
        /// </summary>
        public int recordErrorCount = 0;
        /// <summary>
        /// 是否开启新的任务id录制
        /// </summary>
        public volatile bool startNewTask = true;
        /// <summary>
        /// 限制录制的时长，秒
        /// </summary>
        private long recordSecond;
        /// <summary>
        /// 采集到的主播直播信息
        /// </summary>
        private DouYinAnchorInfoEntity douYinAnchorInfoEntity;
        /// <summary>
        /// 当前录制的视频id
        /// </summary>
        private string videoId { get; set; }
        /// <summary>
        /// 当前录制视频的保存路径
        /// </summary>
        private string fileFullName {  get; set; }
        /// <summary>
        /// 是否正在录制
        /// </summary>
        public volatile bool isRecording = false;
        /// <summary>
        /// 是否允许录制
        /// </summary>
        public volatile bool allowRecord = true;
        /// <summary>
        /// 是否正在处理停止录制后的处理
        /// </summary>
        public volatile bool recordStoping = false;
        /// <summary>
        /// 用于取消检测是否下线的线程
        /// </summary>
        public volatile CancellationTokenSource cts;
        /// <summary>
        /// 当前录制的ffmepg进程
        /// </summary>
        private Process recordProcess;
        /// <summary>
        /// ffmpeg工具路径
        /// </summary>
        private string ffmpegPath = "tools\\ffmpeg.exe";

        /// <summary>
        /// 是否是排班驱动的录制
        /// </summary>
        public bool isScheduleRecord = false;

        /// <summary>
        /// 当前使用的排班ID（用于检测排班是否变化）
        /// </summary>
        public string scheduleId;

        /// <summary>
        /// 当前生效的排班信息
        /// </summary>
        public AnchorSchedule activeSchedule;

        /// <summary>
        /// 是否是北京时间录制模式
        /// </summary>
        public bool isBeijingTimeRecord = false;

        /// <summary>
        /// 当前分段的开始时间（墙钟时间），用于北京时间录制模式判断分段
        /// </summary>
        public DateTime segmentStartTime;

        /// <summary>
        /// 是否是时间点分段录制模式
        /// </summary>
        public bool isTimePointRecord = false;

        /// <summary>
        /// 时间点分段录制的时间点列表（已排序的 TimeSpan 列表）
        /// </summary>
        public List<TimeSpan> segmentTimePointList = new List<TimeSpan>();


        /// <summary>
        /// 锁对象
        /// </summary>
        private readonly object _lockObject = new object();
        private static readonly SemaphoreSlim _semaphore = new SemaphoreSlim(1, 1);

        public AnchorRecordBll(){ }
        /// <summary>
        /// 构造器，赋值主播信息
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>

        public AnchorRecordBll(AnchorInfo anchorInfo)
        {
            this.anchorInfo = anchorInfo;
        }

        /// <summary>
        /// 获取主播信息
        /// </summary>
        /// <returns></returns>
        public AnchorInfo GetAnchorInfo()
        {
            this.anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(this.anchorInfo.SecUid);
            return this.anchorInfo;
        }

        /// <summary>
        /// 获取视频信息
        /// </summary>
        /// <returns></returns>
        public VideoEntity GetVideo()
        {
            VideoEntity videoEntity = VideoCacheManager.GetVideosByVideoId(this.videoId);
            return videoEntity;
        }

        /// <summary>
        /// 开启检测在线
        /// </summary>
        public async void StartDetectionOnline()
        {

            // 获取锁
            await _semaphore.WaitAsync();

            this.anchorInfo = this.GetAnchorInfo();

            try
            {

                if (!AnchorBll.isDetection || !this.allowRecord || this.isRecording)
                {
                    return;
                }

                // 主播是否在线
                int onlineStatus = await getOnlineStatus();
                bool isOnline = onlineStatus == 1;

                if (isOnline)
                {
                    // 主播在线
                    if(this.anchorInfo.platform == 0 && !this.isRestartRecord)
                    {
                        // 抖音主播，校验最新抖音号是否有变化，有则更新
                        FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "校验抖音号是否是最新的");
                        await verifyAnchorNumber();
                    }

                    if (this.douYinAnchorInfoEntity == null || this.isRestartRecord)
                    {
                        // 主播在线，采集直播流等信息
                        this.douYinAnchorInfoEntity = await getLiveAnchorInfo();
                    }

                    if (this.douYinAnchorInfoEntity != null)
                    {
                        FileUtils.LogRecrd($"{JsonConvert.SerializeObject(this.douYinAnchorInfoEntity)}", $"直播信息获取完成");

                        // 设置本次录制的参数
                        if (!this.isRestartRecord)
                        {
                            setRecordParam();
                            if(this.recordLimitValue <= 0)
                            {
                                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"需要录制的时间小于一分钟，不进行录制");
                                return;
                            }
                            FileUtils.LogRecrd($"{this.recordDefinition}=={this.recordLimitType}=={this.recordLimitValue}", $"设置本次录制参数完成=={this.anchorInfo.AnchorName}");
                        }

                        // 获取视频流地址
                        this.streamUrl = GetStreamUrl(AnchorBll.config.LiveSource, recordDefinition);
                        FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"获取流地址完成==={this.streamUrl}");

                        if (!string.IsNullOrEmpty(this.streamUrl))
                        {
                            try
                            {
                                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"校验是否需要发送开播提醒");
                                if (!this.isRestartRecord && (this.anchorInfo.SmsTip == 1 || this.anchorInfo.SmsTip == 3))
                                {
                                    UserInfoVo userInfoVo = await UserApi.GetUserInfoVo();
                                    FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"获取会员信息完成");
                                    if (userInfoVo != null && userInfoVo.packageLevel != null && userInfoVo.packageLevel > 0)
                                    {
                                        // 发送上播提醒短信
                                        AnchorApi.SendSwitchAnchorMsg(this.anchorInfo.AnchorName, 0);
                                        FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"发送开播提醒完成");
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"发送上播短信发生异常");
                            }

                            // 采集成功，将代理IP数重置为10
                            ProxyIPUtils.forceUpdateNum = 10;
                            // 赋值直播场次号给主播信息
                            this.anchorInfo.BatchNumber = this.douYinAnchorInfoEntity.RoomId;
                            AnchorCacheManager.SetAnchorCache(this.anchorInfo);
                            // 开启录制
                            StartRecord();
                        }
                        else
                        {
                            FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"开启录制===获取视频流地址失败");
                            FileUtils.LogRecrd($"{JsonConvert.SerializeObject(this.douYinAnchorInfoEntity)}", $"采集到的主播和视频流信息========{this.anchorInfo?.AnchorName}");
                            this.douYinAnchorInfoEntity = null;

                            if (this.isRestartRecord)
                            {
                                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"当前是重启录制，获取视频流地址失败，走正常结束流程");
                                this.isRecording = true;
                                StopRecord();
                            }
                        }

                    }
                    else
                    {
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"开启录制===采集的主播信息失败");
                        this.douYinAnchorInfoEntity = null;
                        if (this.isRestartRecord)
                        {
                            FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"当前是重启录制，采集的主播信息失败，走正常结束流程");
                            this.isRecording = true;
                            StopRecord();
                        }
                    }

                }
                else
                {
                    this.douYinAnchorInfoEntity = null;
                    string onlineStatusStr = onlineStatus == 0 ? "检测失败" : "离线";
                    if (!isRestartRecord)
                    {
                        // 将主播状态改成离线
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"当前是第一次开启录制，采集的主播状态：{onlineStatusStr}");
                        this.anchorInfo.LiveStatus = onlineStatus == 0 ? 0 : 4;
                        this.anchorInfo.RecordStatus = 0;
                        AnchorCacheManager.SetAnchorCache(this.anchorInfo);

                        this.isRecording = false;
                    }
                    else
                    {
                        // 当前是重启录制，走正常结束流程
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"当前是重启录制，采集的主播状态：{onlineStatusStr}，走正常结束流程");
                        this.isRecording = true;
                        StopRecord();
                        //if(this.isRecording)
                        //{
                        //    StopRecord();
                        //}
                        //else
                        //{
                        //    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"当前主播没在录制，将当前对象从已录制集合移除");
                        //    reAddUnRecordList();
                        //}

                    }
                }
            }
            catch (Exception ex)
            {
                this.douYinAnchorInfoEntity = null;
                if (!isRestartRecord)
                {
                    // 将主播状态改成离线
                    FileUtils.LogRecrd($"{ex}", $"开启检测在线发生错误，当前是第一次开启录制，主播：{anchorInfo?.AnchorName}");
                    this.anchorInfo.LiveStatus = 0;
                    this.anchorInfo.RecordStatus = 0;
                    AnchorCacheManager.SetAnchorCache(this.anchorInfo);

                    this.isRecording = false;
                    
                }
                else
                {
                    FileUtils.LogRecrd($"{ex}", $"开启检测在线发生错误，当前是重启录制，走正常结束流程，主播：{anchorInfo?.AnchorName}");
                    // 为了正常走结束流程，把isRecording改成true
                    this.isRecording = true;
                    // 走结束流程
                    StopRecord();
                    //if (this.isRecording)
                    //{
                    //    StopRecord();
                    //}
                    //else
                    //{
                    //    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"当前主播没在录制，将当前对象从已录制集合移除");
                    //    reAddUnRecordList();
                    //}
                }
                
            }
            finally
            {
                // 释放锁
                _semaphore.Release();
            }

        }

        /// <summary>
        /// 校验抖音号是否是最新的
        /// </summary>
        private async Task verifyAnchorNumber()
        {
            try
            {
                AnchorInfoWebClient anchorInfoWebClient = AnchorInfoWebClient.Instance;
                AnchorBaseInfoDto anchorBaseInfoDto = await anchorInfoWebClient.GetAnchorBySecUid(this.anchorInfo.SecUid);
                if (anchorBaseInfoDto != null && !string.IsNullOrEmpty(anchorBaseInfoDto.anchorNumber) && !anchorBaseInfoDto.anchorNumber.Equals(this.anchorInfo.anchorNumber))
                {
                    this.anchorInfo.anchorNumber = anchorBaseInfoDto.anchorNumber;
                    if (!string.IsNullOrEmpty(anchorBaseInfoDto.anchorName))
                    {
                        this.anchorInfo.AnchorName = anchorBaseInfoDto.anchorName;
                    }
                    if (!string.IsNullOrEmpty(anchorBaseInfoDto.anchorUserId))
                    {
                        this.anchorInfo.AnchorUserId = anchorBaseInfoDto.anchorUserId;
                    }
                    if (!string.IsNullOrEmpty(anchorBaseInfoDto.anchorAvatar))
                    {
                        this.anchorInfo.AnchorAvatar = anchorBaseInfoDto.anchorAvatar;
                    }
                    // 同步到本地缓存
                    AnchorCacheManager.SetAnchorCache(this.anchorInfo);

                    // 更新到服务器
                    AnchorApi.updateAnchorBaseInfo(anchorBaseInfoDto);

                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"校验抖音号是否是最新的发生异常");
            }
            
        }

        /// <summary>
        /// 设置本次录制的参数
        /// </summary>
        private void setRecordParam()
        {
            // 排班录制：强制使用限制时长录制模式，时长用排班结束时间-开始时间计算
            if (this.isScheduleRecord && this.activeSchedule != null)
            {
                // 清晰度
                if (anchorInfo.recordDefinition != null && anchorInfo.recordDefinition != -1)
                {
                    this.recordDefinition = (int)anchorInfo.recordDefinition;
                }
                else
                {
                    this.recordDefinition = AnchorBll.RecordDefinition;
                }

                // 录制限制类型：强制为限制时长录制
                this.recordLimitType = 1;

                // 计算排班时长（分钟）
                this.recordLimitValue = CalculateScheduleDuration();

                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"使用排班参数：清晰度={this.recordDefinition}, 限制类型=限制时长录制, 时长={this.recordLimitValue}分钟");
                return;
            }

            // 非排班录制：使用主播/全局设置
            if (anchorInfo.recordDefinition != null && anchorInfo.recordDefinition != -1)
            {
                this.recordDefinition = (int)anchorInfo.recordDefinition;
            }
            else
            {
                this.recordDefinition = AnchorBll.RecordDefinition;
            }
            if (anchorInfo.recordLimitType != null && anchorInfo.recordLimitType != -1)
            {
                this.recordLimitType = (int)anchorInfo.recordLimitType;
            }
            else
            {
                this.recordLimitType = AnchorBll.config.LimitType;
            }
            if (anchorInfo.recordLimitValue != null && anchorInfo.recordLimitValue != -1)
            {
                this.recordLimitValue = (int)anchorInfo.recordLimitValue;
            }
            else
            {
                this.recordLimitValue = AnchorBll.config.LimitValue;
            }

            // [第二优先级] 时间点分段录制模式：复用北京时间录制逻辑，用时长分段模式
            if (this.isTimePointRecord && this.segmentTimePointList.Count > 0)
            {
                // 强制使用时长分段录制模式，由北京时间检测线程控制分段
                this.recordLimitType = 2;
                this.recordLimitValue = CalcMinutesToNextTimePoint();
                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"使用时间点分段录制模式：时间点={string.Join(",", this.segmentTimePointList)}，到下一个时间点={this.recordLimitValue}分钟");
                return;
            }

            // [第三优先级] 北京时间录制模式：如果主播开启了北京时间录制，并且是分段录制或限制时长录制，记录标记
            if (this.isBeijingTimeRecord && (this.recordLimitType == 1 || this.recordLimitType == 2))
            {
                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"使用北京时间录制模式：录制形式={this.recordLimitType}, 时长={this.recordLimitValue}分钟");
            }

        }

        /// <summary>
        /// 计算排班时长（分钟）
        /// </summary>
        /// <returns>排班时长，单位：分钟</returns>
        private int CalculateScheduleDuration()
        {
            if (this.activeSchedule == null)
            {
                return 0;
            }

            try
            {
                DateTime startTime = DateTime.Now;
                DateTime endTime = DateTime.Parse(this.activeSchedule.endTime);

                if ((endTime - startTime).TotalSeconds < 60)
                {
                    // 小于60秒的不进行录制
                    return 0;
                }

                double durationMinutes = (endTime - startTime).TotalMinutes;

                // 最小1分钟
                return Math.Max((int)durationMinutes, 1);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"计算排班时长发生异常==={this.anchorInfo?.AnchorName}");
                return 0;
            }
        }

        /// <summary>
        /// 解析时间点字符串为 TimeSpan 列表，并排序
        /// </summary>
        /// <param name="timePointsStr">格式："09:30,10:40,11:20"</param>
        /// <returns>排序后的 TimeSpan 列表</returns>
        public List<TimeSpan> ParseTimePoints(string timePointsStr)
        {
            var result = new List<TimeSpan>();
            if (string.IsNullOrEmpty(timePointsStr)) return result;

            foreach (var point in timePointsStr.Split(','))
            {
                var trimmed = point.Trim();
                if (TimeSpan.TryParse(trimmed, out TimeSpan ts))
                {
                    result.Add(ts);
                }
            }
            result.Sort();
            return result;
        }

        /// <summary>
        /// 计算当前时间到下一个时间点的分钟数
        /// </summary>
        /// <returns>到下一个时间点的分钟数，最小1分钟</returns>
        public int CalcMinutesToNextTimePoint()
        {
            if (this.segmentTimePointList == null || this.segmentTimePointList.Count == 0)
            {
                return AnchorBll.config.LimitValue;
            }

            TimeSpan now = DateTime.Now.TimeOfDay;

            // 查找今天剩余的下一个时间点
            foreach (var tp in this.segmentTimePointList)
            {
                if (tp > now)
                {
                    int minutes = (int)Math.Ceiling((tp - now).TotalMinutes);
                    return Math.Max(minutes, 1);
                }
            }

            // 所有时间点都已过，跨天取第一个时间点（明天的第一个时间点）
            TimeSpan first = this.segmentTimePointList[0];
            int crossDayMinutes = (int)Math.Ceiling((TimeSpan.FromHours(24) - now + first).TotalMinutes);
            return Math.Max(crossDayMinutes, 1);
        }

        /// <summary>
        /// 获取直播视频流地址
        /// </summary>
        /// <param name="liveSource">直播源  0 m3u8  1 flv</param>
        /// <param name="recordDefinition">视频清晰度 0 标清 1高清 2超清 3 蓝光</param>
        /// <returns></returns>
        private string GetStreamUrl(int liveSource, int recordDefinition)
        {

            string streaUrl = "";

            List<DouYinAnchorInfoEntity.StreamInfo> streamInfos = this.douYinAnchorInfoEntity.StreamInfos;
            if (streamInfos == null || streamInfos.Count == 0)
            {
                return streaUrl;
            }


            List<DouYinAnchorInfoEntity.StreamInfo> list = new List<DouYinAnchorInfoEntity.StreamInfo>();

            // 将符合的直播源添加进集合
            foreach (DouYinAnchorInfoEntity.StreamInfo streamInfo in streamInfos)
            {
                if (streamInfo.LiveSource == liveSource)
                {
                    list.Add(streamInfo);
                }
            }
            // 没有符合的，全部添加
            if (list.Count < 1)
            {
                foreach (DouYinAnchorInfoEntity.StreamInfo streamInfo in streamInfos)
                {
                    list.Add(streamInfo);
                }
            }

            List<DouYinAnchorInfoEntity.StreamInfo> listSort = list.OrderBy(dto => dto.Quality).ToList();

            if (listSort.Count < recordDefinition + 1)
            {
                DouYinAnchorInfoEntity.StreamInfo streamInfo = listSort[listSort.Count - 1];
                streaUrl = streamInfo.StreaUrl;
            }
            else
            {
                DouYinAnchorInfoEntity.StreamInfo streamInfo = listSort[recordDefinition];
                streaUrl = streamInfo.StreaUrl;
            }

            //if(!string.IsNullOrEmpty(streaUrl) && this.anchorInfo.platform == 2)
            //{
            //    // 当前的流是视频号的，检测是否有效
            //    return CheckStreamByFFmpeg(streaUrl) ? streaUrl : null;
            //}

            return streaUrl;
        }

        /// <summary>
        /// 检查流是否有效
        /// </summary>
        /// <param name="streamUrl">流地址</param>
        /// <returns></returns>
        private bool CheckStreamByFFmpeg(string streamUrl)
        {
            // 使用FFmpeg探测流信息
            string arguments = $"-i \"{streamUrl}\" -t 5 -f null -";

            var processInfo = new ProcessStartInfo
            {
                FileName = ffmpegPath,
                Arguments = arguments,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                UseShellExecute = false,
                CreateNoWindow = true
            };

            try
            {
                using (var process = Process.Start(processInfo))
                {
                    // 使用异步读取避免缓冲区死锁
                    var outputTask = process.StandardOutput.ReadToEndAsync();
                    var errorTask = process.StandardError.ReadToEndAsync();

                    // 等待进程退出，设置超时
                    bool exited = process.WaitForExit(30000); // 30秒超时
                    
                    if (!exited)
                    {
                        // 超时，强制终止进程
                        try { process.Kill(); } catch { }
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"视频号直播流检测是否有效超时");
                        return false;
                    }

                    // 进程退出后，等待异步读取完成（带超时保护）
                    bool readCompleted = Task.WaitAll(new[] { outputTask, errorTask }, 3000);
                    if (!readCompleted)
                    {
                        FileUtils.LogRecrd("读取进程输出超时", $"直播流检测读取超时=={this.anchorInfo?.AnchorName}");
                        return false;
                    }
                    string errorResult = errorTask.Result;

                    // 检查输出中是否包含成功信息
                    if(!string.IsNullOrEmpty(errorResult) && (errorResult.Contains("Stream") || errorResult.Contains("Duration:")))
                    {
                        return true;
                    }
                    FileUtils.LogRecrd($"{errorResult}", $"直播流无效=={this.anchorInfo?.AnchorName}");
                    return false;
                }
            }
            catch(Exception ex) 
            {
                FileUtils.LogRecrd($"{ex}", $"检测直播流是否有效发生异常=={this.anchorInfo?.AnchorName}");
                return false;
            }
        }

        private bool IsStreamHealthy(string streamUrl, int maxRetries = 3, int retryDelayMs = 3000)
        {
            // -t 3 强制解码 3 秒数据，确保 ffmpeg 读到第一个 .ts 段并触发 SPS 检测
            string args = $"-probesize 100k -analyzeduration 500000 -i \"{streamUrl}\" -t 3 -f null -";

            for (int attempt = 1; attempt <= maxRetries; attempt++)
            {
                try
                {
                    var psi = new ProcessStartInfo
                    {
                        FileName = ffmpegPath,
                        Arguments = args,
                        RedirectStandardError = true,
                        UseShellExecute = false,
                        CreateNoWindow = true
                    };

                    using (var proc = Process.Start(psi))
                    {
                        var stderrTask = proc.StandardError.ReadToEndAsync();
                        bool exited = proc.WaitForExit(8000);

                        if (!exited)
                        {
                            try { proc.Kill(); } catch { }
                            FileUtils.LogRecrd($"流预检超时 attempt={attempt}: {streamUrl}", "IsStreamHealthy");
                            if (attempt < maxRetries) Thread.Sleep(retryDelayMs);
                            continue;
                        }

                        string stderr = stderrTask.Wait(2000) ? stderrTask.Result : string.Empty;

                        if (stderr.Contains("non-existing SPS") || stderr.Contains("Invalid data found"))
                        {
                            FileUtils.LogRecrd($"流预检失败-SPS缺失 attempt={attempt}: {streamUrl}", "IsStreamHealthy");
                            if (attempt < maxRetries) Thread.Sleep(retryDelayMs);
                            continue;
                        }

                        if (!stderr.Contains("Stream #") && !stderr.Contains("Duration:"))
                        {
                            FileUtils.LogRecrd($"流预检失败-无有效流 attempt={attempt}: {streamUrl}", "IsStreamHealthy");
                            if (attempt < maxRetries) Thread.Sleep(retryDelayMs);
                            continue;
                        }

                        FileUtils.LogRecrd($"流预检通过 attempt={attempt}: {streamUrl}", "IsStreamHealthy");
                        return true;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"流预检异常 attempt={attempt}: {ex.Message}", "IsStreamHealthy");
                    if (attempt < maxRetries) Thread.Sleep(retryDelayMs);
                }
            }

            FileUtils.LogRecrd($"流预检全部失败，放弃录制: {streamUrl}", "IsStreamHealthy");
            return false;
        }

        /// <summary>
        /// 设置主播直播的信息，包括直播流等信息
        /// </summary>
        private async Task<DouYinAnchorInfoEntity> getLiveAnchorInfo()
        {
            if (this.anchorInfo.platform == 0)
            {
                // 抖音
                // 先尝试方案一
                AnchorInfoWebClient anchorInfoWebClient = AnchorInfoWebClient.Instance;
                DouYinAnchorInfoEntity entity = await anchorInfoWebClient.GetLiveInfoByAnchorNumber(this.anchorInfo.anchorNumber);
                if(entity != null)
                {
                    return entity;
                }
                // 继续尝试方案二
                return await DouYinAnchorBll.GetDouYinAnchorInfo(this.anchorInfo.LiveUrl);
            }
            else if (this.anchorInfo.platform == 1)
            {
                // 快手
                return await KuaiShouAnchorBll.GetLiveAnchorInfo(this.anchorInfo.AnchorUserId);
            }
            else if (this.anchorInfo.platform == 2)
            {
                // 微信视频号
                return await WeChatChannelsAnchorBll.GetLiveAnchorInfo(this.anchorInfo.SecUid);
            }

            return null;
        }

        /// <summary>
        /// 获取主播的在线状态
        /// </summary>
        /// <returns> 0：检测失败 1：在线 2：离线</returns>
        private async Task<int> getOnlineStatus()
        {
            int onlineStatus = 0;
            if (this.anchorInfo.platform == 0)
            {
                // 抖音
                if (!string.IsNullOrEmpty(this.anchorInfo.AnchorUserId))
                {
                    // 当前主播已经有主播userId，可以直接用短接口查是否在线
                    onlineStatus = DouYinAnchorBll.GetOnlineStatus(this.anchorInfo.AnchorUserId);
                    FileUtils.LogRecrd($"{onlineStatus}", $"采集到的抖音主播状态===={this.anchorInfo?.AnchorName}");
                }
                else
                {
                    // 采集主播信息，拿到主播userId
                    this.douYinAnchorInfoEntity = await DouYinAnchorBll.GetDouYinAnchorInfo(this.anchorInfo.LiveUrl);
                    if (this.douYinAnchorInfoEntity != null && !string.IsNullOrEmpty(this.douYinAnchorInfoEntity.AnchorId))
                    {
                        // 更新主播信息
                        this.anchorInfo.AnchorUserId = this.douYinAnchorInfoEntity.AnchorId;
                        AnchorCacheManager.SetAnchorCache(this.anchorInfo);
                        // 同步主播的AnchorId信息到服务器
                        ReplayHttpUtils.SaveOrUpdateAnchor(anchorInfo);
                        // 查询主播是否在线
                        onlineStatus = DouYinAnchorBll.GetOnlineStatus(this.anchorInfo.AnchorUserId);
                        FileUtils.LogRecrd($"{onlineStatus}", $"采集到的抖音主播状态===={this.anchorInfo?.AnchorName}");
                    }
                }
            }
            else if (this.anchorInfo.platform == 1)
            {
                // 快手
                onlineStatus = await KuaiShouAnchorBll.GetIsOnline(this.anchorInfo.AnchorUserId);
                FileUtils.LogRecrd($"{onlineStatus}", $"采集到的快手主播状态===={this.anchorInfo?.AnchorName}");
            }
            else if (this.anchorInfo.platform == 2)
            {
                // 微信视频号
                onlineStatus = await WeChatChannelsAnchorBll.GetOnlineStatus(this.anchorInfo);
                FileUtils.LogRecrd($"{onlineStatus}", $"采集到的微信视频号主播状态===={this.anchorInfo?.AnchorName}");
            }

            return onlineStatus;
        }

        /// <summary>
        /// 开启录制
        /// </summary>
        private async void StartRecord()
        {
            try
            {
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"录制任务开始执行");
                // 将主播状态改成在线
                this.anchorInfo.LiveStatus = 2;

                // 设置主播的开始录制时间，以及状态
                if (this.startNewTask) //新任务才会设置
                {
                    this.anchorInfo.StartTime = ServerTimeUtils.getCurrentTimeStr();
                }
                this.anchorInfo.RecordStatus = 1;
                this.anchorInfo.OnlineNumber = "0";
                AnchorCacheManager.SetAnchorCache(this.anchorInfo);

                bool addParagraphNum = true;
                if (isRestartRecord && !File.Exists(this.fileFullName))
                {
                    // 当前是重启录制但是主视频文件不存在，说明录制没成功，去掉任务id，重新开始录制
                    FileUtils.LogRecrd($"{anchorInfo?.AnchorName}==={this.fileFullName}", $"当前是重启录制但是主视频文件不存在，说明录制没成功，去掉任务id，重新开始录制");
                    this.startNewTask = true;
                    addParagraphNum = false;
                }

                var recordFormat = this.anchorInfo.platform;
                if (this.anchorInfo.platform == 2)
                {
                    // 抖音、微信视频号使用 m3u8 格式，ts 文件
                    recordFormat = 0;
                }else if(this.anchorInfo.platform == 0 && AnchorBll.config.LiveSource == 1)
                {
                    // 抖音，但是直播流是flv
                    recordFormat = 1;
                }

                if (this.startNewTask)
                {
                    FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"新起录制");

                    if (this.isRestartRecord && addParagraphNum)
                    {
                        this.currentParagraphNum++;
                    }

                    // 设置录制的时长
                    if(recordLimitType == 0)
                    {
                        this.recordSecond = -1;
                    }else if(recordLimitType == 1 || recordLimitType == 2)
                    {
                        if (this.isBeijingTimeRecord)
                        {
                            // 北京时间录制模式（含时间点分段）：不让C++控制时长，由C#定时检测控制分段
                            this.recordSecond = -1;
                        }
                        else
                        {
                            this.recordSecond = recordLimitValue * 60;
                        }
                    }

                    // 记录当前分段的开始时间（墙钟时间）
                    this.segmentStartTime = DateTime.Now;

                    // 构建视频存储路径
                    string directoryPath = "";
                    string fileName = "";
                    BuildFileName(out directoryPath, out fileName);

                    // 保存视频记录到缓存和数据库
                    this.fileFullName = directoryPath + "\\" + fileName;
                    this.videoId = SaveAnchorVideo(this.fileFullName, fileName);

                    // 1.创建任务id
                    this.taskId = Guid.NewGuid().ToString();

                    // 通知C++开始录制
                    int code = CppRecordUtils.addTask(this.streamUrl, ToIntPtr(this.fileFullName), this.taskId, this.recordSecond, recordFormat);
                    FileUtils.LogRecrd($"code: {code}，streamUrl: {this.streamUrl}，fileFullName: {this.fileFullName}，taskId: {this.taskId}，recordSecond: {this.recordSecond}", "开启cpp录制结果");
                    if (code != 0)
                    {
                        // 开启录制失败
                        FileUtils.LogRecrd($"streamUrl: {this.streamUrl}，fileFullName: {this.fileFullName}，taskId: {this.taskId}，recordSecond: {this.recordSecond}", "开启cpp录制失败");
                        StopRecord();
                    }
                    else
                    {
                        this.isRecording = true;
                        this.isRestartRecord = false; // 重启流程完成，重置标志位
                        this.restartCount = 0;
                        this.recordErrorCount = 0;
                        // 将当前对象从待录制集合移除
                        AnchorBll.unRecordList.TryRemove(this.anchorInfo.SecUid, out AnchorRecordBll _);
                        // 将当前对象加入到已录制集合
                        AnchorBll.recordingList.AddOrUpdate(this.anchorInfo.SecUid, this, (oldKey, oldValue) => this);

                        // 检测主播是否下线 
                        cts = new CancellationTokenSource();
                        checkAnchorIsUnLine();

                        // 启动各平台数据采集（巨量、千川等）（新架构）
                        try
                        {
                            VideoEntity videoEntity = this.GetVideo();
                            var scheduler = JuliangUtils.formMain?.CollectionScheduler;
                            if (videoEntity != null && scheduler != null)
                            {
                                scheduler.StartAnchorCollection(this.anchorInfo, this.anchorInfo.BatchNumber, videoEntity.videoId);
                                FileUtils.LogRpa($"启动平台数据采集（巨量、千川等），roomId={this.anchorInfo.BatchNumber}, videoId={videoEntity.videoId}", this.anchorInfo?.AnchorName);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"启动平台数据采集异常：{ex.Message}", this.anchorInfo?.AnchorName);
                        }
                        this.anchorInfo.LastRecordTime = ServerTimeUtils.getCurrentTimeStr();
                        AnchorCacheManager.SetAnchorCache(this.anchorInfo);
                        ReplayHttpUtils.UpdateAnchorLastRecordTime(this.anchorInfo.SecUid, this.anchorInfo.LastRecordTime);

                        // 打印录制列表日志
                        printLog();
                    }
                }
                else
                {
                    FileUtils.LogRecrd($"{this.taskId}", $"继续录制==={this.anchorInfo?.AnchorName}");
                    // 通知C++继续录制
                    int code = CppRecordUtils.updateTask(this.streamUrl, ToIntPtr(this.fileFullName), this.taskId, this.recordSecond, recordFormat);
                    FileUtils.LogRecrd($"code: {code}，streamUrl: {this.streamUrl}，fileFullName: {this.fileFullName}，taskId: {this.taskId}，recordSecond: {this.recordSecond}", "继续cpp录制结果");
                    if (code != 0)
                    {
                        // 继续录制失败
                        FileUtils.LogRecrd($"streamUrl: {this.streamUrl}，fileFullName: {this.fileFullName}，taskId: {this.taskId}，recordSecond: {this.recordSecond}", "继续cpp录制失败");
                        StopRecord();
                    }else
                    {
                        this.isRecording = true;
                        this.isRestartRecord = false; // 重启流程完成，重置标志位
                        this.restartCount = 0;
                        // 检测主播是否下线 
                        cts = new CancellationTokenSource();
                        checkAnchorIsUnLine();

                        // 启动各平台数据采集（巨量、千川等）（新架构）
                        try
                        {
                            VideoEntity videoEntity = this.GetVideo();
                            var scheduler = JuliangUtils.formMain?.CollectionScheduler;
                            if (videoEntity != null && scheduler != null)
                            {
                                scheduler.StartAnchorCollection(this.anchorInfo, this.anchorInfo.BatchNumber, videoEntity.videoId);
                                FileUtils.LogRpa($"启动平台数据采集（巨量、千川等），roomId={this.anchorInfo.BatchNumber}, videoId={videoEntity.videoId}", this.anchorInfo?.AnchorName);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"启动平台数据采集异常：{ex.Message}", this.anchorInfo?.AnchorName);
                        }

                        printLog();
                    }
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"开启录制发生错误，主播：{anchorInfo?.AnchorName}");
                // 杀掉录制进程
                if(!string.IsNullOrEmpty(this.taskId))
                {
                    StopRecord();
                }
                else if(this.startNewTask)
                {
                    this.anchorInfo.RecordStatus = 0;
                    this.anchorInfo.LiveStatus = 0;
                    AnchorCacheManager.SetAnchorCache(this.anchorInfo);

                    this.isRecording = false;
                }
                //throw;
            }

        }

        /// <summary>
        /// 将主播从已录制集合移除，并检测是否需要移入待录制集合
        /// </summary>
        private void reAddUnRecordList()
        {
            // 将当前对象从已录制集合移除
            AnchorBll.recordingList.TryRemove(this.anchorInfo.SecUid, out AnchorRecordBll _);
            AnchorInfo currentAnchor = this.GetAnchorInfo();
            if (currentAnchor != null && currentAnchor.IsAutoRecord == 1 && AnchorBll.isDetection)
            {
                // 将当前对象加入到待录制集合
                FileUtils.LogRecrd($"{currentAnchor?.AnchorName}", "此时系统还在录制检测中，重新将主播添加进待检测列表");
                if (!AnchorBll.unRecordList.ContainsKey(currentAnchor.SecUid))
                {
                    AnchorRecordBll anchorRecordBll = new AnchorRecordBll(currentAnchor);
                    AnchorBll.unRecordList.AddOrUpdate(anchorInfo.SecUid, anchorRecordBll, (oldKey, oldValue) => anchorRecordBll);
                }
            }
        }

        /// <summary>
        /// 主动检测主播是否下线
        /// </summary>
        private void checkAnchorIsUnLine()
        {
            Task.Run(async () =>
            {
                while (AnchorBll.isDetection && isRecording && cts != null && !cts.IsCancellationRequested)
                {

                    try
                    {
                        if (cts != null)
                        {
                            this.anchorInfo = GetAnchorInfo();
                            if(this.anchorInfo.platform == 0)
                            {
                                await Task.Delay(10000, cts.Token);
                            }
                            else if (this.anchorInfo.platform == 1)
                            {
                                await Task.Delay(30000, cts.Token);
                            }
                            else if (this.anchorInfo.platform == 2)
                            {
                                await Task.Delay(30000, cts.Token);
                            }
                        }
                    }
                    catch (OperationCanceledException)
                    {
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"主动检测主播下线线程被取消");
                    }

                    try
                    {
                        await CheckIsUnLine();
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRecrd($"{ex}", $"主动检测主播是否下线异常==={this.anchorInfo?.AnchorName}");
                    }

                    
                }
            });
        }

        /// <summary>
        /// 打印待录制和录制中的集合明细
        /// </summary>
        private void printLog()
        {
            try
            {
                string unRecordStr = "";
                foreach (var item in AnchorBll.unRecordList)
                {
                    AnchorRecordBll anchorRecordBll = item.Value;
                    unRecordStr += anchorRecordBll.GetAnchorInfo()?.AnchorName + "====";

                }
                FileUtils.LogRecrd($"{unRecordStr}", $"待录制的集合详情");

                string recordingStr = "";
                foreach (var item in AnchorBll.recordingList)
                {
                    AnchorRecordBll anchorRecordBll = item.Value;
                    recordingStr += anchorRecordBll.GetAnchorInfo()?.AnchorName + "====";
                }
                FileUtils.LogRecrd($"{recordingStr}", $"正在录制的集合详情");
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"打印待录制和录制中的集合明细发生异常");
            }
            

        }

        /// <summary>
        /// 将字符串转成IntPtr类型
        /// </summary>
        /// <param name="resourceStr">原字符串</param>
        /// <returns></returns>
        private IntPtr ToIntPtr(string resourceStr)
        {
            // 将字符串转换为 UTF - 8 字节数组
            byte[] utf8Bytes = Encoding.UTF8.GetBytes(resourceStr);
            // 分配非托管内存并复制字节数组
            IntPtr unmanagedPtr = Marshal.AllocHGlobal(utf8Bytes.Length + 1);
            Marshal.Copy(utf8Bytes, 0, unmanagedPtr, utf8Bytes.Length);
             Marshal.WriteByte(unmanagedPtr + utf8Bytes.Length, 0); // 添加字符串终止符

            return unmanagedPtr;

        }

        /// <summary>
        /// 录制进程结束后的处理
        /// </summary>
        /// <param name="curErrorCode">结束的错误码，如果为7将不再继续录制，重新开始新的录制</param>
        /// <returns></returns>
        public async Task RecordEndHandle(int curErrorCode = 4)
        {
            // 获取锁
            await _semaphore.WaitAsync();

            // 标志位：是否需要在锁释放后重新开启检测
            bool needRestartDetection = false;

            try
            {
                this.anchorInfo = this.GetAnchorInfo();
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "录制进程结束后的处理");
                
                // 添加详细日志
                FileUtils.LogRpa($"RecordEndHandle开始 - curErrorCode:{curErrorCode}, allowRecord:{this.allowRecord}, isRecording:{this.isRecording}, restartCount:{this.restartCount}", this.anchorInfo?.AnchorName);

                // 关闭主动检测主播是否下线
                this.recordStoping = true;
                if(cts != null)
                {
                    cts.Cancel();
                    cts = null;
                }

                if(!this.isRecording)
                {
                    // 当前没在录制，不做处理
                    FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "当前没在录制，或已被处理过，不做处理");
                    return;
                }

                // 修改主播状态
                if (this.allowRecord)
                {
                    this.anchorInfo.RecordStatus = 3;
                }
                else
                {
                    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "是手动停止单个主播的");
                    this.anchorInfo.RecordStatus = 2;
                }
                this.anchorInfo.LiveStatus = 0;
                AnchorCacheManager.SetAnchorCache(this.anchorInfo);

                // 如果录制错误回调次数超过3次，同步到服务端
                if(curErrorCode == 4 || curErrorCode == 5 || curErrorCode == 6)
                {
                    if(this.anchorInfo.platform == 2)
                    {
                        if(!string.IsNullOrEmpty(this.streamUrl))
                        {
                            this.recordErrorCount++;
                        }
                        
                    }else
                    {
                        this.recordErrorCount++;
                    }
                }
                if (this.recordErrorCount > 3)
                {
                    VideoEntity videoEntity = this.GetVideo();
                    if(videoEntity != null &&(videoEntity.recordErrorStatus == null || videoEntity.recordErrorStatus == 0)) {
                        videoEntity.recordErrorStatus = 7;
                        await VideoApi.SaveOrUpdateVideoAsync(videoEntity);
                    }
                }

                this.restartCount++;
                if (this.restartCount > 3)
                {
                    // 已超过重启录制重试次数，停止录制
                    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "已超过重启录制重试次数，停止录制");

                    // 检测是否需要重新加入待检测列表
                    AnchorInfo currentAnchor = AnchorCacheManager.GetAnchorByIdFromCache(anchorInfo.SecUid);
                    if (currentAnchor != null && currentAnchor.IsAutoRecord == 1)
                    {
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "已超过重启录制重试次数，停止录制，此时还在开启检测，重新将主播添加进待检测列表");
                        await HandleRecordEndCommon(reAddToUnRecordList: true);
                    }else
                    {
                        await HandleRecordEndCommon(reAddToUnRecordList: false);
                    }
                    return;
                }

                // 手动停止录制：结束录制并清理
                if (!this.allowRecord)
                {
                    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "主播是手动停止录制，结束");
                    await HandleRecordEndCommon(reAddToUnRecordList: false);
                    return;
                }

                int onlineStatus = await getOnlineStatus();
                FileUtils.LogRpa($"结束录制后主播的在线状态 - onlineStatus:{onlineStatus} (0=未知, 1=在线, 2=下线), 主播名:{this.anchorInfo?.AnchorName}", this.anchorInfo?.AnchorName);
                FileUtils.LogRecrd($"{onlineStatus}", $"结束录制后主播的状态==={this.anchorInfo?.AnchorName}");

                if (onlineStatus == 0)
                {
                    // 检测在线状态失败，重试15次
                    if(this.anchorInfo.platform == 0)
                    {
                        for (int i = 0; i < 15; i++)
                        {
                            onlineStatus = await getOnlineStatus();
                            FileUtils.LogRecrd($"{onlineStatus}===={this.anchorInfo?.AnchorName}", $"检测在线状态失败，重试15次，当前是第{i + 1}次");
                            if (onlineStatus != 0)
                            {
                                break;
                            }
                            Thread.Sleep(10000);
                        }
                    }

                    if (onlineStatus == 0)
                    {
                        // 3分钟都获取主播状态失败
                        FileUtils.LogRecrd($"{onlineStatus}===={this.anchorInfo?.AnchorName}", "15次获取主播状态都失败");
                        await HandleRecordEndCommon(reAddToUnRecordList: true);
                        return;
                    }
                }

                // 已停止检测，或主播已下线，结束
                if (!AnchorBll.isDetection || onlineStatus == 2)
                {
                    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "已停止检测，或主播已下线，结束");

                    // 发送下播提醒短信
                    if (onlineStatus == 2 && (this.anchorInfo.SmsTip == 2 || this.anchorInfo.SmsTip == 3))
                    {
                        UserInfoVo userInfoVo = await UserApi.GetUserInfoVo();
                        if (userInfoVo != null && userInfoVo.packageLevel != null && userInfoVo.packageLevel > 0)
                        {
                            FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "已停止检测，或主播已下线--发送短信");
                            AnchorApi.SendSwitchAnchorMsg(this.anchorInfo.AnchorName, 1);
                        }
                    }

                    await HandleRecordEndCommon(reAddToUnRecordList: true);
                }
                else
                {
                    // 没有停止检测，并且主播没有离线

                    // 手动停止：结束录制并清理
                    if (!this.allowRecord)
                    {
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "主播还在线，但已手动停止录制，结束");
                        await HandleRecordEndCommon(reAddToUnRecordList: false);
                        return;
                    }

                    // 获取视频时长
                    long duration = GetVideoDuration();

                    // 根据录制限制类型处理不同场景
                    needRestartDetection = await HandleRecordLimitType(curErrorCode, duration);
                }
                
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"{ex}", $"录制进程结束后的处理发生异常");
            }
            finally
            {
                this.recordStoping = false;
                // 释放锁
                _semaphore.Release();

                // 在锁释放后再开启检测，避免潜在的竞态条件
                if (needRestartDetection)
                {
                    StartDetectionOnline();
                }
            }

        }

        /// <summary>
        /// 录制结束的公共处理逻辑
        /// </summary>
        /// <param name="reAddToUnRecordList">是否需要重新加入待录制列表</param>
        private async Task HandleRecordEndCommon(bool reAddToUnRecordList = false)
        {
            // 重置排班相关标记
            this.isScheduleRecord = false;
            this.scheduleId = null;
            this.activeSchedule = null;
            this.isBeijingTimeRecord = false;
            this.isTimePointRecord = false;
            this.segmentTimePointList.Clear();

            // 更新视频信息，计算结束时间、视频时长大小等
            await UpdateVideoForRecordEnd();

            // 停止巨量、千川。。。的采集（endTime 已在上一步计算好）
            await stopCollectionScheduler();

            if (reAddToUnRecordList)
            {
                // 将当前对象从已录制集合移除，并检测是否需要移入待录制集合
                reAddUnRecordList();
            }
            else
            {
                // 将当前对象从已录制集合移除
                AnchorBll.recordingList.TryRemove(this.anchorInfo.SecUid, out AnchorRecordBll _);
            }

            // 打印录制列表日志
            printLog();

            this.isRecording = false;
        }

        /// <summary>
        /// 停止巨量、千川。。。的采集
        /// </summary>
        private async Task stopCollectionScheduler()
        {
            var scheduler = JuliangUtils.formMain?.CollectionScheduler;
            VideoEntity videoEntity = this.GetVideo();
            bool isLastSegment = !this.allowRecord;

            // UpdateVideoForRecordEnd 已计算好 endTime，直接取
            string videoEndTime = videoEntity?.endTime;

            // 触发最后一轮数据采集
            if (scheduler != null && !string.IsNullOrEmpty(videoEndTime))
            {
                try
                {
                    FileUtils.LogRpa($"触发最后一轮数据采集 - videoEndTime:{videoEndTime}", this.anchorInfo?.AnchorName);
                    await scheduler.CollectOnceAsync(this.anchorInfo.SecUid, videoEndTime);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"最后一轮数据采集异常：{ex.Message}", this.anchorInfo?.AnchorName);
                }
            }

            // 如果是最后一个分段，延迟30秒等待巨量API数据就绪
            if (isLastSegment)
            {
                FileUtils.LogRpa($"最后一个分段，延迟30秒等待巨量API数据就绪", this.anchorInfo?.AnchorName);
                await Task.Delay(30000);
            }

            // 停止平台数据采集（巨量、千川等）（新架构）
            try
            {
                FileUtils.LogRpa($"准备停止平台数据采集 - allowRecord:{this.allowRecord}", this.anchorInfo?.AnchorName);

                if (scheduler != null)
                {
                    scheduler.StopAnchorCollection(this.anchorInfo.SecUid, true);
                    FileUtils.LogRpa($"已停止平台数据采集（巨量、千川等）", this.anchorInfo?.AnchorName);
                }
                else
                {
                    FileUtils.LogRpa($"采集器scheduler为null，无需停止", this.anchorInfo?.AnchorName);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"停止平台数据采集异常：{ex.Message}", this.anchorInfo?.AnchorName);
            }

            // 上传巨量数据（直播结束后上传）
            try
            {
                if (videoEntity != null && !string.IsNullOrEmpty(this.anchorInfo.BatchNumber) && !string.IsNullOrEmpty(videoEntity.videoId))
                {
                    FileUtils.LogRpa($"准备上传巨量数据 - videoId:{videoEntity.videoId}, batchNumber:{this.anchorInfo.BatchNumber}, isLastSegment:{isLastSegment}, allowRecord:{this.allowRecord}", this.anchorInfo?.AnchorName);

                    // 检查数据文件是否存在及内容
                    string summaryFilePath = JuliangDataHandle.getDataFilePath(videoEntity.videoId, 1);

                    if (isLastSegment)
                    {
                        // 延迟后再次检查数据
                        if (File.Exists(summaryFilePath))
                        {
                            string dataJsonAfter = File.ReadAllText(summaryFilePath, Encoding.UTF8);
                            if (!string.IsNullOrEmpty(dataJsonAfter))
                            {
                                try
                                {
                                    var tempDataAfter = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(dataJsonAfter);
                                    FileUtils.LogRpa($"延迟后数据检查 - averageOnlineNum:{tempDataAfter?.averageOnlineNum}, totalWatchNum:{tempDataAfter?.totalWatchNum}", this.anchorInfo?.AnchorName);
                                }
                                catch (Exception ex)
                                {
                                    FileUtils.LogRpa($"延迟后解析数据文件失败：{ex.Message}", this.anchorInfo?.AnchorName);
                                }
                            }
                        }
                    }

                    JuliangDataHandle.dataUpload(this.anchorInfo.BatchNumber, videoEntity.videoId, this.anchorInfo?.SecUid);
                    FileUtils.LogRpa($"直播结束上传巨量数据完成，videoId={videoEntity.videoId}", this.anchorInfo?.AnchorName);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"直播结束上传巨量数据异常：{ex.Message}", this.anchorInfo?.AnchorName);
            }
        }

        /// <summary>
        /// 准备重启录制的状态设置
        /// </summary>
        /// <param name="startNewTask">是否开启新任务</param>
        /// <param name="resetParagraphNum">是否重置分段序号</param>
        /// <returns>是否需要重启检测</returns>
        private bool PrepareForRestartRecord(bool startNewTask, bool resetParagraphNum = false)
        {
            this.startNewTask = startNewTask;
            this.isRestartRecord = true;
            if (resetParagraphNum)
            {
                this.currentParagraphNum = 0;
            }
            this.isRecording = false;
            return true; // 返回needRestartDetection
        }

        /// <summary>
        /// 准备重启录制并更新视频信息
        /// </summary>
        /// <param name="startNewTask">是否开启新任务</param>
        /// <param name="resetParagraphNum">是否重置分段序号</param>
        /// <returns>是否需要重启检测</returns>
        private async Task<bool> PrepareForRestartRecordWithUpdate(bool startNewTask, bool resetParagraphNum = false)
        {
            this.startNewTask = startNewTask;
            this.isRestartRecord = true;
            if (resetParagraphNum)
            {
                this.currentParagraphNum = 0;
            }

            // 更新视频信息，计算结束时间、视频时长大小等
            await UpdateVideoForRecordEnd();

            this.isRecording = false;
            return true; // 返回needRestartDetection
        }

        /// <summary>
        /// 根据录制限制类型处理不同场景
        /// </summary>
        /// <param name="curErrorCode">当前错误码</param>
        /// <param name="duration">视频时长（秒）</param>
        /// <returns>是否需要重启检测</returns>
        private async Task<bool> HandleRecordLimitType(int curErrorCode, long duration)
        {
            // 无限制录制模式
            if (recordLimitType == 0)
            {
                return await HandleUnlimitedRecordMode(curErrorCode);
            }
            // 时长分段录制模式
            else if (recordLimitType == 2)
            {
                return await HandleSegmentedRecordMode(curErrorCode, duration);
            }
            // 限制时长录制模式
            else if (recordLimitType == 1)
            {
                return await HandleLimitedRecordMode(curErrorCode, duration);
            }

            return false;
        }

        /// <summary>
        /// 处理无限制录制模式
        /// </summary>
        private async Task<bool> HandleUnlimitedRecordMode(int curErrorCode)
        {
            if (curErrorCode == 7)
            {
                // 错误码为7，重新起一个任务id录制
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是无限制录制，错误码为7，重新起一个任务id录制");
                return await PrepareForRestartRecordWithUpdate(startNewTask: true, resetParagraphNum: true);
            }
            else
            {
                // 继续录制
                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", "主播还在线，当前是无限制录制，继续录制");
                return PrepareForRestartRecord(startNewTask: false);
            }
        }

        /// <summary>
        /// 处理时长分段录制模式
        /// </summary>
        private async Task<bool> HandleSegmentedRecordMode(int curErrorCode, long duration)
        {
            long timeLimitSeconds = recordLimitValue * 60;

            // 北京时间录制模式：使用墙钟时间判断是否需要分段
            long effectiveDuration = duration;
            if (this.isBeijingTimeRecord)
            {
                effectiveDuration = (long)(DateTime.Now - this.segmentStartTime).TotalSeconds;
                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"北京时间录制模式：墙钟已过{effectiveDuration}秒，限制{timeLimitSeconds}秒，视频时长{duration}秒");
            }

            if (curErrorCode == 7)
            {
                // 错误码为7，重新起一个任务id录制
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是时长分段录制，错误码为7，重新起一个任务id录制");
                return await PrepareForRestartRecordWithUpdate(startNewTask: true, resetParagraphNum: true);
            }
            else if (effectiveDuration >= timeLimitSeconds)
            {
                // 已超出限制，重新起一个任务id录制
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是时长分段录制，已超出限制，重新起一个任务id录制");

                // 如果是时间点分段录制，重新计算到下一个时间点的时长
                if (this.isTimePointRecord)
                {
                    this.recordLimitValue = CalcMinutesToNextTimePoint();
                    FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"时间点分段录制，下一段时长={this.recordLimitValue}分钟");
                }

                return await PrepareForRestartRecordWithUpdate(startNewTask: true, resetParagraphNum: false);
            }
            else
            {
                // 未超出限制，继续用之前的任务id录制
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是时长分段录制，未超出限制，继续用之前的任务id录制");
                return PrepareForRestartRecord(startNewTask: false);
            }
        }

        /// <summary>
        /// 处理限制时长录制模式
        /// </summary>
        private async Task<bool> HandleLimitedRecordMode(int curErrorCode, long duration)
        {
            long timeLimitSeconds = recordLimitValue * 60;

            // 北京时间录制模式或排班录制：使用墙钟时间判断
            long effectiveDuration = duration;
            if (this.isBeijingTimeRecord || this.isScheduleRecord)
            {
                effectiveDuration = (long)(DateTime.Now - this.segmentStartTime).TotalSeconds;
                FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"墙钟时间判断模式：墙钟已过{effectiveDuration}秒，限制{timeLimitSeconds}秒，视频时长{duration}秒");
            }

            // 未超出限制（容错1分钟）
            if (Math.Abs(effectiveDuration - timeLimitSeconds) > 60)
            {
                if (curErrorCode == 7)
                {
                    // 错误码为7，重新起一个任务id录制
                    FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是限制时长录制，错误码为7，重新起一个任务id录制");
                    return PrepareForRestartRecord(startNewTask: true);
                }
                else
                {
                    // 继续用之前的任务id录制
                    FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是限制时长录制，未超出限制，继续用之前的任务id录制");
                    return PrepareForRestartRecord(startNewTask: false);
                }
            }
            else
            {
                // 已超出限制，结束录制
                FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", "主播还在线，当前是限制时长录制，已超出限制，结束");
                if(this.isScheduleRecord)
                {
                    await HandleRecordEndCommon(reAddToUnRecordList: true);
                }
                else
                {
                    await HandleRecordEndCommon(reAddToUnRecordList: false);
                }
                
                return false;
            }
        }

        /// <summary>
        /// 获取视频的时长，秒
        /// </summary>
        /// <returns></returns>
        private long GetVideoDuration()
        {
            long duration = 0;
            try
            {
                VideoEntity videoInfo = this.GetVideo();
                FileInfo fileInfo = new FileInfo(videoInfo.storagePath);
                if (fileInfo.Exists)
                {

                    duration = long.TryParse(videoInfo.duration, out long val) ? val : 0;

                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"获取视频的时长发生异常", $"{ex}");
            }
            

            return duration;
        }

        /// <summary>
        /// 录制结束，更新视频状态
        /// </summary>
        private async Task UpdateVideoForRecordEnd()
        {
            try
            {
                VideoEntity videoInfo = this.GetVideo();
                //更新视频的录制状态
                videoInfo.isRecording = 2;

                long duration = long.TryParse(videoInfo.duration, out long val) ? val : 0;
                FileInfo fileInfo = new FileInfo(videoInfo.storagePath);

                if(!fileInfo.Exists)
                {
                    // 视频文件不存在，直接删除记录
                    FileUtils.LogRecrd($"{videoInfo.storagePath}", $"视频文件不存在，直接删除记录");
                    // 从服务器删除
                    await VideoApi.DelVideoById(videoInfo.videoId);
                    // 从缓存清除
                    VideoCacheManager.DelVideoCache(videoInfo);
                    return;
                }

                long fileSize = fileInfo.Length;
                videoInfo.vedioSizie = fileSize.ToString();

                // 更新录制结束时间
                DateTime originalDateTime = DateTime.ParseExact(videoInfo.startTime, "yyyy-MM-dd HH:mm:ss", null);
                DateTime newDateTime = originalDateTime.AddSeconds(int.Parse(videoInfo.duration));
                videoInfo.endTime = newDateTime.ToString("yyyy-MM-dd HH:mm:ss");


                FileUtils.LogRecrd($"{JsonConvert.SerializeObject(videoInfo)}", $"录制结束后的视频处理===视频信息");

                if (duration < 60)
                {
                    // 视频时长小于60秒，丢弃
                    FileUtils.LogRecrd($"{videoInfo.storagePath}", $"视频小于60秒，删除");
                    // 从服务器删除
                    await VideoApi.DelVideoById(videoInfo.videoId);
                    // 从缓存清除
                    VideoCacheManager.DelVideoCache(videoInfo);

                    if (File.Exists(videoInfo.storagePath))
                    {
                        int i = 10;
                        while (i >= 0)
                        {
                            try
                            {
                                File.Delete(videoInfo.storagePath);
                                break;
                            }
                            catch (Exception e)
                            {
                                FileUtils.LogRecrd($"{e}", $"视频删除失败，正在重试");
                            }
                            Thread.Sleep(1000);
                            i--;
                        }

                    }

                    return;
                }

                // 转成MP4 - 将任务加入队列
                try
                {
                    // 加入MP4转换队列，由队列管理器异步处理
                    Mp4ConvertQueueManager.AddToQueue(videoInfo);
                }
                catch (Exception e)
                {
                    FileUtils.LogRecrd($"{e}", $"录制结束加入MP4转换队列发生异常");
                }

                // 保存视频信息
                VideoCacheManager.SetVideoToCache(videoInfo);

                // 同步视频数据到服务器
                UpdateVideoSizeDurationBo updateVideoSizeDurationBo = new UpdateVideoSizeDurationBo();
                updateVideoSizeDurationBo.videoId = videoInfo.videoId;
                updateVideoSizeDurationBo.duration = duration;
                updateVideoSizeDurationBo.fileSize = fileSize;
                updateVideoSizeDurationBo.endTime = videoInfo.endTime;
                updateVideoSizeDurationBo.isRecording = 0;
                bool isSuccess = await VideoApi.UpdateVideoSizeDuration(updateVideoSizeDurationBo);
                if (isSuccess)
                {
                    FileUtils.LogRecrd($"{videoInfo.storagePath}", $"录制结束后的视频处理===同步视频数据到服务器成功");
                    // 同步到服务器成功
                    videoInfo.isRecording = 0;
                    VideoCacheManager.SetVideoToCache(videoInfo);

                    // 检查是否需要停止分析，修改视频分析状态
                    checkStopAnalysis(videoInfo);

                    // 判断是否需要生成数据看盘
                    try
                    {
                        this.anchorInfo = this.GetAnchorInfo();
                        
                        if (ReplayHttpUtils.UserClientVersion.Equals(ClientVersion.Replay) && this.anchorInfo.platform == 0 && this.anchorInfo.IsDataViewing == 1 
                            && anchorInfo.juliangAuthStatus != JuliangAuthStatusEnum.auth && anchorInfo.isAutoAnalysis != null && anchorInfo.isAutoAnalysis == 1)
                        {
                            // 生成数据看盘
                            int onlineStatus = DouYinAnchorBll.GetOnlineStatus(this.anchorInfo.AnchorUserId);
                            int anchorOnlineStatus = onlineStatus != 1 ? 1 : 0;
                            DataViewingApi.CreateDataViewing(videoInfo.videoId, anchorOnlineStatus);
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogRecrd($"{e}", $"生成数据看盘发生错误");
                    }

                    // 上传websocket统计的json数据
                    new Thread(()=>
                    {
                        try
                        {
                            WebsocketDataHandle.UploadSocketData(videoInfo.videoId, videoInfo.batchNumber, videoInfo.startTime, videoInfo.endTime, videoInfo.secUid, long.TryParse(videoInfo.userId, out long tempUserId) ? tempUserId : 0, true);
                        }
                        catch (Exception e)
                        {
                            FileUtils.LogError($"{e}", "上传websocket统计的json数据报错");
                        }
                    }).Start();
                }
                else
                {
                    FileUtils.LogRecrd($"{videoInfo.storagePath}", $"录制结束后的视频处理===同步视频数据到服务器失败，将同步失败的视频定时重新发送到服务器");
                    // 将断网导致同步失败的视频定时重新发送到服务器
                    videoInfo.isRecording = 2;
                    VideoCacheManager.SetVideoToCache(videoInfo);
                    VideoApi.SendErrorVideoToServer();
                }

            }
            catch (Exception e)
            {
                FileUtils.LogRecrd($"{e}", $"更新视频大小时长状态发生错误");
            }
        }

        /// <summary>
        /// 检查是否需要停止分析，修改视频分析状态
        /// </summary>
        /// <param name="videoInfo">视频信息</param>
        /// <returns></returns>
        public static async Task checkStopAnalysis(VideoEntity videoInfo)
        {
            try
            {
                UserInfoVo userInfoVo = await UserApi.GetUserInfoVo();
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(videoInfo.secUid);

                // 26.05.07 删除免费版需手动分析的功能-漫总要求
                // if (userInfoVo != null && userInfoVo.packageLevel != null && userInfoVo.packageLevel <= 0)
                // {
                //     await VideoApi.UpdateVideoAnalysisStatus(videoInfo.videoId, 4, "免费版需手动分析");
                // }
                // else 
                if (anchorInfo.isAutoAnalysis != null && anchorInfo.isAutoAnalysis == 0)
                {
                    await VideoApi.UpdateVideoAnalysisStatus(videoInfo.videoId, 4, "开了手动分析");
                }

                VideoCacheManager.SetVideoToCache(videoInfo);
            }
            catch (Exception e)
            {
                FileUtils.LogRecrd($"{e}", $"检查是否需要停止分析，修改视频分析状态发生异常");
            }
            

        }

        /// <summary>
        /// 检测主播是否已下线，已下线则杀掉录制进程
        /// </summary>
        public async Task CheckIsUnLine()
        {

            this.anchorInfo = this.GetAnchorInfo();

            if (this.isRecording && !this.recordStoping)
            {
                try
                {
                    // 检查主播是否在线
                    int onlineStatus = await getOnlineStatus();
                    if (onlineStatus == 2)
                    {
                        FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"主动检测到主播已下线");

                        // 主播已下线，再检测5次，确认已下线
                        bool isOnline = false;
                        if(this.anchorInfo.platform == 0)
                        {
                            for (int i = 0; i < 5; i++)
                            {
                                int confirmOnlineStatus = await getOnlineStatus();
                                FileUtils.LogRecrd($"{confirmOnlineStatus}===={this.anchorInfo?.AnchorName}", $"主动检测到主播已下线，重试5次以确认主播真正的状态，正在重试第{i + 1}次，");
                                if (confirmOnlineStatus == 1)
                                {
                                    isOnline = true;
                                    break;
                                }
                                Thread.Sleep(2000);
                            }
                        }

                        if (!isOnline)
                        {
                            FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"主播已确认下线");
                            // 不在线，杀掉录制进程
                            StopRecord();
                        }else
                        {
                            FileUtils.LogRecrd($"{this.anchorInfo.AnchorName}", $"主播不是真正的下线，不做处理继续录制");
                        }
                        
                    }
                }catch (Exception e)
                {
                    FileUtils.LogRecrd($"{e}", $"检测主播是否已下线，已下线则杀掉录制进程发生错误，主播：{anchorInfo?.AnchorName}");
                }
                
            }
        }

        /// <summary>
        /// 杀掉录制进程
        /// </summary>
        public void KillProcess()
        {
            lock(_lockObject)
            {
                if (this.recordProcess != null && !this.recordProcess.HasExited)
                {
                    this.recordProcess.Kill();
                }
            }
        }

        /// <summary>
        /// 停止cpp录制任务
        /// </summary>
        public async Task StopRecord(int errorCode = 4)
        {
            string taskId = null;
            bool lockTaken = false;

            try
            {
                Monitor.Enter(_lockObject, ref lockTaken);
                if (lockTaken)
                {
                    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}--{this.taskId}", $"C#调用C++停止录制");
                    string logStr = FileUtils.GetFormattedStack();
                    FileUtils.LogRecrd($"{logStr}", $"C#调用C++停止录制==堆栈信息");

                    if (!string.IsNullOrEmpty(this.taskId))
                    {
                        taskId = this.taskId;
                    }
                    else
                    {
                        FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"C#调用C++停止录制操作失败,原因：taskId为空");
                    }
                }
                else
                {
                    FileUtils.LogRecrd($"{this.anchorInfo?.AnchorName}", $"C#调用C++停止录制====未获取到锁，放弃任务");
                }
            }
            finally
            {
                if (lockTaken)
                {
                    Monitor.Exit(_lockObject);
                }
            }

            if (!string.IsNullOrEmpty(taskId))
            {
                await Task.Run(async () =>
                {
                    int i = 0;
                    while (i < 3)
                    {
                        try
                        {
                            await FileUtils.LogRpaAsync($"{taskId}---{this.anchorInfo?.AnchorName}", $"C#调用C++停止录制，当前是第{i + 1}次");
                            int code = -1;
                            using (var cts = new CancellationTokenSource(TimeSpan.FromMinutes(1)))
                            {
                                code = await Task.Run(() => CppRecordUtils.stopTask(taskId), cts.Token);
                            }
                            FileUtils.LogRecrd($"{code}---{taskId}---{this.anchorInfo?.AnchorName}", $"C#调用C++停止录制结果");
                            if (code == 0)
                            {
                                await RecordEndHandle(errorCode);
                                break;
                            }
                            else
                            {
                                await FileUtils.LogRpaAsync($"{code}---{taskId}---{this.anchorInfo?.AnchorName}", $"C#调用C++停止失败，即将重试第{i + 1}次");
                                await Task.Delay(3000);
                            }
                        }
                        catch (Exception ex)
                        {
                            await FileUtils.LogRpaAsync($"{ex}", $"C#调用C++停止发生异常==={taskId}==={this.anchorInfo?.AnchorName}");
                        }

                        i++;

                        if (i == 3)
                        {
                            await FileUtils.LogRpaAsync($"{taskId}---{this.anchorInfo?.AnchorName}", $"C#调用C++停止录制多次都失败，直接结束");
                            await RecordEndHandle(errorCode);
                            break;
                        }
                    }
                });
            }
        }

        /// <summary>
        /// 执行录制线程
        /// </summary>
        /// <param name="command">录制命令</param>
        /// <returns></returns>
        private Process ExecuteFFmpegCommand(string command)
        {
            var process = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = this.ffmpegPath,
                    Arguments = command,
                    RedirectStandardInput = true, // 添加这一行以重定向标准输入
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true,
                    StandardOutputEncoding = Encoding.UTF8, // 设置输出编码为UTF-8
                    StandardErrorEncoding = Encoding.UTF8  // 设置错误输出编码为UTF-8
                }
            };

            // 订阅输出数据接收事件
            process.OutputDataReceived += (sender, args) =>
            {
                if (!string.IsNullOrEmpty(args.Data))
                {
                    // 打印日志
                    FileUtils.log($"输出数据 - {DateTime.Now:yyyy-MM-dd HH:mm:ss} - {args.Data}");
                }
            };

            // 订阅错误数据接收事件
            process.ErrorDataReceived += (sender, args) =>
            {
                if (!string.IsNullOrEmpty(args.Data))
                {
                    // 打印日志
                    string anchorName = anchorInfo?.AnchorName;
                    FileUtils.LogRecrd($"{DateTime.Now:yyyy-MM-dd HH:mm:ss} - {args.Data}", $"FFmepg错误日志-{anchorName}");
                    FileUtils.log($"错误数据 - {DateTime.Now:yyyy-MM-dd HH:mm:ss} - {args.Data}");
                }
            };

            process.Start();
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();

            return process;
        }


        /// <summary>
        /// 构建录制命令
        /// </summary>
        /// <returns></returns>
        private string BuildRecordCommand()
        {
            // 录制参数
            string customOptions = $"-loglevel error " +
                     //$"-reconnect 1 " +
                     //$"-max_delay 8000000 " +
                     //$"-rtbufsize 32M " +
                     //$"-thread_queue_size 2048 " +
                     //$"-err_detect ignore_err " +
                     $"-rw_timeout 10000000 " +
                     "-headers \"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3\" " +
                     $"-cookies \"ttwid={this.douYinAnchorInfoEntity.Ttwid}; __ac_nonce={this.douYinAnchorInfoEntity.AcNonce}\" ";

            string timeStr = this.recordSecond == -1 ? "" : $" -t {this.recordSecond}";

            return $"{customOptions} -i \"{this.streamUrl}\" {timeStr}  -c:v copy -c:a copy \"{this.fileFullName}\""; 
        }

        /// <summary>
        /// 构建继续录制命令
        /// </summary>
        /// <returns></returns>
        private string BuildContinueRecordCommand()
        {
            // 录制参数
            string customOptions = $"-loglevel error " +
                     //$"-reconnect 1 " +
                     //$"-max_delay 8000000 " +
                     //$"-rtbufsize 32M " +
                     //$"-thread_queue_size 2048 " +
                     //$"-err_detect ignore_err " +
                     $"-rw_timeout 10000000 " +
                     "-headers \"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3\" " +
                     $"-cookies \"ttwid={this.douYinAnchorInfoEntity.Ttwid}; __ac_nonce={this.douYinAnchorInfoEntity.AcNonce}\" ";

            string prefix = this.fileFullName.Substring(0, this.fileFullName.LastIndexOf("."));
            string suffix = this.fileFullName.Substring(this.fileFullName.LastIndexOf("."));

            string filePath = prefix + "_" + ServerTimeUtils.getCurrentTimeStr("yyyyMMddHHmmss") + suffix;

            string timeStr = this.recordSecond == -1 ? "" : $" -t {this.recordSecond}";

            return $"{customOptions} -i \"{this.streamUrl}\" {timeStr}  -c:v copy -c:a copy \"{filePath}\"";
        }

        /// <summary>
        /// 构建视频存储路径
        /// </summary>
        /// <returns></returns>
        private void BuildFileName(out string directoryPath, out string fileName)
        {
            // 生成文件目录
            directoryPath = AnchorBll.config.SavePath + "\\" + this.anchorInfo.FolderName + "\\" + ServerTimeUtils.getCurrentTimeStr("yyyyMMdd");

            //检测此文件目录是否存在
            if (!Directory.Exists(directoryPath))
            {
                //创建视频的存储目录
                Directory.CreateDirectory(directoryPath);
            }

            // 生成文件名
            fileName = this.anchorInfo.FolderName + "_" + ServerTimeUtils.getCurrentTimeStr("yyyy年MM月dd日HH时mm分ss秒");
            fileName += $"_第{currentParagraphNum + 1}段";
            string suffixName = ".ts";
            if(this.anchorInfo.platform == 1 || (this.anchorInfo.platform == 0 && AnchorBll.config.LiveSource == 1))
            {
                suffixName = ".flv";
            }
            //if(AnchorBll.RecordVideoType == 1)
            //{
            //    suffixName = ".flv";
            //}else if(AnchorBll.RecordVideoType == 2)
            //{
            //    suffixName = ".mp4";
            //}
            fileName += suffixName;

        }

    
        /// <summary>
        /// 创建视频，保存视频记录到缓存和数据库
        /// </summary>
        /// <param name="fileFullName">完整的视频路径</param>
        /// <param name="fileName">视频文件名</param>
        /// <returns></returns>
        private string SaveAnchorVideo(string fileFullName, string fileName)
        {

            this.anchorInfo = this.GetAnchorInfo();

            VideoEntity videoEntity = new VideoEntity();
            videoEntity.duration = "0";
            videoEntity.startTime = ServerTimeUtils.getServerCurrentTimeStr();
            videoEntity.endTime = videoEntity.startTime;
            videoEntity.vedioSizie = "0";

            //1.保存视频文件
            videoEntity.videoName = fileName;
            videoEntity.anchorId = this.anchorInfo.Id;
            videoEntity.batchNumber = this.anchorInfo.BatchNumber;
            videoEntity.storagePath = fileFullName;
            videoEntity.paragraph = this.currentParagraphNum;
            videoEntity.sourceUrl = streamUrl;
            videoEntity.definition = AnchorBll.RecordDefinition;
            videoEntity.sourceType = AnchorBll.config.LiveSource;
            videoEntity.subsectionType = AnchorBll.config.LimitType;
            videoEntity.videoType = AnchorBll.RecordVideoType;
            videoEntity.liveTitle = this.douYinAnchorInfoEntity.RoomTitle;
            videoEntity.isRecording = 1;
            videoEntity.secUid = this.anchorInfo.SecUid;
            videoEntity.tradeId = this.anchorInfo.TradeId;
            videoEntity.analysisStatus = 0;
            // 设置平台类型
            if(this.anchorInfo.platform == 0)
            {
                videoEntity.platformType = "1";
            }else if (this.anchorInfo.platform == 1)
            {
                videoEntity.platformType = "2";
            }else if (this.anchorInfo.platform == 2)
            {
                videoEntity.platformType = "3";
            }
            videoEntity.videoId = Guid.NewGuid().ToString();
            videoEntity.userId = ReplayHttpUtils.UserId;
            videoEntity.tenantId = ReplayHttpUtils.ActiveTenantId;
            videoEntity.videoSliceType = 0;

            // 保存到服务器
            VideoApi.SaveOrUpdateVideo(videoEntity);
            // 添加到缓存
            VideoCacheManager.SetVideoToCache(videoEntity);

            return videoEntity.videoId;
        }




    }
}
