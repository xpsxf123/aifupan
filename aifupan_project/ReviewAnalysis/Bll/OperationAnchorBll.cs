using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.anchor;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.enumeration.anchor;
using ReviewAnalysis.anchorLive;
using ReviewAnalysis.enterprise;
using ReviewAnalysis.life;
using ReviewAnalysis.juliang;
using ReviewAnalysis.Model;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.anchor;
using ReviewAnalysis.Websocket.Entity;
using Swan;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using System.Web.UI.WebControls;
using System.Windows.Forms;
using ReviewAnalysis.qianchuan;

namespace ReviewAnalysis.Bll
{
    
    public class OperationAnchorBll
    {
        public static ConcurrentDictionary<string, Process> dicProcesss = new ConcurrentDictionary<string, Process>();
        public static  ConcurrentDictionary<string, List<string>> dicDanMu = new ConcurrentDictionary<string, List<string>>();

        /// <summary>
        /// 是否已经开始上线检测
        /// </summary>
        private static bool isDecector = false;

        /// <summary>
        /// 录制之后的视频类型 video_type 0 ts,1 flv,2 mp4 
        /// </summary>
        private static int RecordVideoType;

        /// <summary>
        /// 录制之后的清晰度 definition 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        private static int RecordDefinition;

        /// <summary>
        /// 是否正在检测之中  此字段用来标识  检测 主播上线或者线下过程中  一次while循环是否已经结束，false 一次循环已经结束，true 正在检测
        /// </summary>
        private static bool isDecetorIng = false;



        /// <summary>
        /// 分页获取主播列表
        /// </summary>
        /// <param name="anchorListBo">查询参数</param>
        /// <returns></returns>
        public AnchorPageDto<AnchorDto> GetPage(AnchorListBo anchorListBo)
        {
            AnchorPageDto<AnchorDto> result = new AnchorPageDto<AnchorDto>();
            // 从缓存取出符合条件的主播列表
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAnchorPage(anchorListBo);
            result.Total = anchorInfos.Count;

            // 分页
            if (anchorListBo.pageIndex == null || anchorListBo.pageIndex <= 0)
            {
                anchorListBo.pageIndex = 1;
            }
            if (anchorListBo.pageSize == null || anchorListBo.pageSize <= 0)
            {
                anchorListBo.pageSize = 10;
            }
            anchorInfos = pageAnchorList(anchorInfos, (int)anchorListBo.pageIndex, (int)anchorListBo.pageSize);

            List<AnchorDto> anchorDtos = new List<AnchorDto>();
            if(anchorInfos != null && anchorInfos.Count > 0)
            {
                foreach(AnchorInfo anchorInfo in anchorInfos)
                {
                    AnchorDto anchorDto = JsonConvert.DeserializeObject<AnchorDto>(JsonConvert.SerializeObject(anchorInfo));

                    if(anchorInfo.IsRemoveRecord == 0)
                    {
                        // 填充主播当前录制的视频信息
                        FillRecordVideoInfo(anchorDto);

                        // 判断当前主播是否在录制范围内
                        CheckRecordTime(anchorDto);
                    }

                    // 计算排班状态
                    if (anchorInfo.isScheduleRecord != 1)
                    {
                        anchorDto.ScheduleStatus = 0;
                    }
                    else
                    {
                        var activeSchedule = AnchorScheduleCacheManager.GetActiveSchedule(anchorInfo.SecUid);
                        anchorDto.ScheduleStatus = activeSchedule != null ? 2 : 1;
                    }

                    anchorDtos.Add(anchorDto);
                }
            }

            if(anchorDtos.Count > 0 && anchorListBo.isRemoveRecord == 0)
            {
                // 填充主播昨日/前日录制记录
                FillAnchorYesterdayRecord(anchorDtos);

                // 统计直播中和录制中的主播数量
                int currentLiveNum;
                int currentRecordNum;
                CountRecordingNum(anchorDtos, out currentLiveNum, out currentRecordNum);
                result.CurrentLiveNum = currentLiveNum;
                result.CurrentRecordNum = currentRecordNum;
            }



            result.PageTotal = 1;
            result.DataList = anchorDtos;
            return result;
        }

        /// <summary>
        /// 分页获取主播列表-异步
        /// </summary>
        /// <param name="anchorListBo">查询参数</param>
        /// <returns></returns>
        public async Task<AnchorPageDto<AnchorDto>> GetPageAsync(AnchorListBo anchorListBo)
        {
            AnchorPageDto<AnchorDto> result = new AnchorPageDto<AnchorDto>();
            // 从缓存取出符合条件的主播列表
            List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAnchorPage(anchorListBo);
            result.Total = anchorInfos.Count;

            // 分页
            if (anchorListBo.pageIndex == null || anchorListBo.pageIndex <= 0)
            {
                anchorListBo.pageIndex = 1;
            }
            if (anchorListBo.pageSize == null || anchorListBo.pageSize <= 0)
            {
                anchorListBo.pageSize = 10;
            }
            anchorInfos = pageAnchorList(anchorInfos, (int)anchorListBo.pageIndex, (int)anchorListBo.pageSize);

            List<AnchorDto> anchorDtos = new List<AnchorDto>();
            if(anchorInfos != null && anchorInfos.Count > 0)
            {
                foreach(AnchorInfo anchorInfo in anchorInfos)
                {
                    AnchorDto anchorDto = JsonConvert.DeserializeObject<AnchorDto>(JsonConvert.SerializeObject(anchorInfo));

                    if(anchorInfo.IsRemoveRecord == 0)
                    {
                        // 填充主播当前录制的视频信息
                        FillRecordVideoInfo(anchorDto);

                        // 判断当前主播是否在录制范围内
                        CheckRecordTime(anchorDto);
                    }

                    // 计算排班状态
                    if (anchorInfo.isScheduleRecord != 1)
                    {
                        anchorDto.ScheduleStatus = 0;
                    }
                    else
                    {
                        var activeSchedule = AnchorScheduleCacheManager.GetActiveSchedule(anchorInfo.SecUid);
                        anchorDto.ScheduleStatus = activeSchedule != null ? 2 : 1;
                    }

                    anchorDtos.Add(anchorDto);
                }
            }

            if(anchorDtos.Count > 0 && anchorListBo.isRemoveRecord == 0)
            {
                // 填充主播昨日/前日录制记录
                await FillAnchorYesterdayRecordAsync(anchorDtos);

                // 统计直播中和录制中的主播数量
                int currentLiveNum;
                int currentRecordNum;
                CountRecordingNum(anchorDtos, out currentLiveNum, out currentRecordNum);
                result.CurrentLiveNum = currentLiveNum;
                result.CurrentRecordNum = currentRecordNum;
            }



            result.PageTotal = 1;
            result.DataList = anchorDtos;
            return result;
        }

        /// <summary>
        /// 分页列表
        /// </summary>
        /// <param name="anchorInfos">原始主播列表</param>
        /// <param name="pageIndex">分页索引</param>
        /// <param name="pageSize">分页大小</param>
        /// <returns></returns>
        private List<AnchorInfo> pageAnchorList(List<AnchorInfo> anchorInfos, int pageIndex, int pageSize)
        {
            List<AnchorInfo> result = null;
            // 计算分页参数
            int skip = (pageIndex - 1) * pageSize; // 跳过多少条

            // 安全分页：避免越界和空列表
            if (anchorInfos.Count == 0)
            {
                result = new List<AnchorInfo>(); // 确保返回空列表
            }
            else if (skip >= anchorInfos.Count)
            {
                result = new List<AnchorInfo>(); // 超出总页数返回空
            }
            else
            {
                result = anchorInfos.Skip(skip).Take(pageSize).ToList();
            }

            return result; 
        }

        /// <summary>
        /// 判断当前主播是否在录制范围内
        /// </summary>
        /// <param name="anchorDto"></param>
        private void CheckRecordTime(AnchorDto anchorDto)
        {
            bool inRecordTime = true;
            if (!string.IsNullOrEmpty(anchorDto.RecordTime))
            {
                // 存在录制时间范围时，先将是否录制设置为 false。
                inRecordTime = false;
                var timeDurationList = anchorDto.RecordTime.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);
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

            anchorDto.InRecordTime = inRecordTime;
        }

        /// <summary>
        /// 统计直播中和录制中的主播数量
        /// </summary>
        private void CountRecordingNum(List<AnchorDto> anchorDtos, out int currentLiveNum, out int currentRecordNum)
        {
            currentLiveNum = 0;
            currentRecordNum = 0;
            foreach (AnchorDto anchorDto in anchorDtos)
            {
                if (anchorDto.LiveStatus == 2)
                {
                    currentLiveNum += 1;
                }
                if (anchorDto.RecordStatus == 1)
                {
                    currentRecordNum += 1;
                }
            }
        }

        /// <summary>
        /// 填充主播昨日录制记录
        /// </summary>
        /// <param name="anchorDtos"></param>
        private void FillAnchorYesterdayRecord(List<AnchorDto> anchorDtos)
        {

            // 获取今天23:59:59时间戳
            long todayEndSeconds = GetTodayEndUnixTimestamp();
            // 获取当前时间戳
            long currentTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            // 获取30分钟后的时间戳
            long timestamp = currentTimestamp + 30 * 60;

            List<string> notRecordDataSecUidList = new List<string>();
            foreach (AnchorDto anchorDto in anchorDtos)
            {
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(anchorDto.SecUid);
                // 判断是否需要从服务器拉取新的数据
                if(anchorInfo.YesterdayRecordTime == null || anchorInfo.YesterdayRecordTime == 0 ||  currentTimestamp > anchorInfo.YesterdayRecordTime)
                {
                    notRecordDataSecUidList.Add(anchorInfo.SecUid);
                }else
                {
                    anchorDto.YesterdayRecordList = anchorInfo.YesterdayRecordList;
                    anchorDto.YesterdayRecordNum = anchorInfo.YesterdayRecordNum;
                    anchorDto.YesterdayAverageObservationNum = anchorInfo.YesterdayAverageObservationNum;
                    anchorDto.YesterdayAverageVolumeStart = anchorInfo.YesterdayAverageVolumeStart;
                    anchorDto.YesterdayAverageVolumeEnd = anchorInfo.YesterdayAverageVolumeEnd;

                    anchorDto.DayBeforeRecordList = anchorInfo.DayBeforeRecordList;
                    anchorDto.DayBeforeRecordNum = anchorInfo.DayBeforeRecordNum;
                    anchorDto.DayBeforeAverageObservationNum = anchorInfo.DayBeforeAverageObservationNum;
                    anchorDto.DayBeforeAverageVolumeStart = anchorInfo.DayBeforeAverageVolumeStart;
                    anchorDto.DayBeforeAverageVolumeEnd = anchorInfo.DayBeforeAverageVolumeEnd;
                }
            }

            // 从服务器拉取新的昨日录制数据
            if(notRecordDataSecUidList.Count > 0)
            {
                List<AnchorYesterdayRecordVo> anchorYesterdayRecordVos = AnchorApi.ListAnchorYesterdayRecord(notRecordDataSecUidList);
                if(anchorYesterdayRecordVos != null && anchorYesterdayRecordVos.Count > 0)
                {
                    foreach (var anchorYesterdayRecordVo in anchorYesterdayRecordVos)
                    {
                        // 设置到缓存里面去
                        AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(anchorYesterdayRecordVo.secUid);
                        if(todayEndSeconds > timestamp)
                        {
                            anchorInfo.YesterdayRecordTime = timestamp;
                        }
                        else
                        {
                            anchorInfo.YesterdayRecordTime = todayEndSeconds;
                        }
                        
                        anchorInfo.YesterdayRecordList = anchorYesterdayRecordVo.yesterdayRecordList;
                        anchorInfo.YesterdayRecordNum = anchorYesterdayRecordVo.yesterdayRecordNum;
                        anchorInfo.YesterdayAverageObservationNum = anchorYesterdayRecordVo.yesterdayAverageObservationNum;
                        anchorInfo.YesterdayAverageVolumeStart = anchorYesterdayRecordVo.yesterdayAverageVolumeStart;
                        anchorInfo.YesterdayAverageVolumeEnd = anchorYesterdayRecordVo.yesterdayAverageVolumeEnd;

                        anchorInfo.DayBeforeRecordList = anchorYesterdayRecordVo.dayBeforeRecordList;
                        anchorInfo.DayBeforeRecordNum = anchorYesterdayRecordVo.dayBeforeRecordNum ?? 0;
                        anchorInfo.DayBeforeAverageObservationNum = anchorYesterdayRecordVo.dayBeforeAverageObservationNum ?? 0;
                        anchorInfo.DayBeforeAverageVolumeStart = anchorYesterdayRecordVo.dayBeforeAverageVolumeStart ?? 0;
                        anchorInfo.DayBeforeAverageVolumeEnd = anchorYesterdayRecordVo.dayBeforeAverageVolumeEnd ?? 0;

                        AnchorCacheManager.SetAnchorCache(anchorInfo);
                    }
                }

            }

        }

        /// <summary>
        /// 填充主播昨日录制记录-异步
        /// </summary>
        /// <param name="anchorDtos"></param>
        private async Task FillAnchorYesterdayRecordAsync(List<AnchorDto> anchorDtos)
        {

            // 获取今天23:59:59时间戳
            long todayEndSeconds = GetTodayEndUnixTimestamp();
            // 获取当前时间戳
            long currentTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            // 获取30分钟后的时间戳
            long timestamp = currentTimestamp + 30 * 60;

            List<string> notRecordDataSecUidList = new List<string>();
            foreach (AnchorDto anchorDto in anchorDtos)
            {
                AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(anchorDto.SecUid);
                // 判断是否需要从服务器拉取新的数据
                if(anchorInfo.YesterdayRecordTime == null || anchorInfo.YesterdayRecordTime == 0 ||  currentTimestamp > anchorInfo.YesterdayRecordTime)
                {
                    notRecordDataSecUidList.Add(anchorInfo.SecUid);
                }else
                {
                    anchorDto.YesterdayRecordList = anchorInfo.YesterdayRecordList;
                    anchorDto.YesterdayRecordNum = anchorInfo.YesterdayRecordNum;
                    anchorDto.YesterdayAverageObservationNum = anchorInfo.YesterdayAverageObservationNum;
                    anchorDto.YesterdayAverageVolumeStart = anchorInfo.YesterdayAverageVolumeStart;
                    anchorDto.YesterdayAverageVolumeEnd = anchorInfo.YesterdayAverageVolumeEnd;

                    anchorDto.DayBeforeRecordList = anchorInfo.DayBeforeRecordList;
                    anchorDto.DayBeforeRecordNum = anchorInfo.DayBeforeRecordNum;
                    anchorDto.DayBeforeAverageObservationNum = anchorInfo.DayBeforeAverageObservationNum;
                    anchorDto.DayBeforeAverageVolumeStart = anchorInfo.DayBeforeAverageVolumeStart;
                    anchorDto.DayBeforeAverageVolumeEnd = anchorInfo.DayBeforeAverageVolumeEnd;
                }
            }

            // 从服务器拉取新的昨日录制数据
            if(notRecordDataSecUidList.Count > 0)
            {
                List<AnchorYesterdayRecordVo> anchorYesterdayRecordVos = await AnchorApi.ListAnchorYesterdayRecordAsync(notRecordDataSecUidList);
                if(anchorYesterdayRecordVos != null && anchorYesterdayRecordVos.Count > 0)
                {
                    foreach (var anchorYesterdayRecordVo in anchorYesterdayRecordVos)
                    {
                        // 设置到缓存里面去
                        AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(anchorYesterdayRecordVo.secUid);
                        if(todayEndSeconds > timestamp)
                        {
                            anchorInfo.YesterdayRecordTime = timestamp;
                        }
                        else
                        {
                            anchorInfo.YesterdayRecordTime = todayEndSeconds;
                        }
                        
                        anchorInfo.YesterdayRecordList = anchorYesterdayRecordVo.yesterdayRecordList;
                        anchorInfo.YesterdayRecordNum = anchorYesterdayRecordVo.yesterdayRecordNum;
                        anchorInfo.YesterdayAverageObservationNum = anchorYesterdayRecordVo.yesterdayAverageObservationNum;
                        anchorInfo.YesterdayAverageVolumeStart = anchorYesterdayRecordVo.yesterdayAverageVolumeStart;
                        anchorInfo.YesterdayAverageVolumeEnd = anchorYesterdayRecordVo.yesterdayAverageVolumeEnd;

                        anchorInfo.DayBeforeRecordList = anchorYesterdayRecordVo.dayBeforeRecordList;
                        anchorInfo.DayBeforeRecordNum = anchorYesterdayRecordVo.dayBeforeRecordNum ?? 0;
                        anchorInfo.DayBeforeAverageObservationNum = anchorYesterdayRecordVo.dayBeforeAverageObservationNum ?? 0;
                        anchorInfo.DayBeforeAverageVolumeStart = anchorYesterdayRecordVo.dayBeforeAverageVolumeStart ?? 0;
                        anchorInfo.DayBeforeAverageVolumeEnd = anchorYesterdayRecordVo.dayBeforeAverageVolumeEnd ?? 0;

                        AnchorCacheManager.SetAnchorCache(anchorInfo);
                    }
                }

            }

        }

        /// <summary>
        /// 获取今天23:59:59时间戳
        /// </summary>
        /// <returns></returns>
        private long GetTodayEndUnixTimestamp()
        {
            // 获取当前日期的00:00:00（本地时间）
            DateTime todayStart = DateTime.Now.Date;

            // 计算当天的23:59:59本地时间
            DateTime todayEndLocal = todayStart.AddSeconds(24 * 3600 - 1);

            // 转换为UTC时间
            DateTime todayEndUtc = todayEndLocal.ToUniversalTime();

            // 计算与Unix纪元（1970-01-01 UTC）的时间差
            DateTime epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            TimeSpan span = todayEndUtc - epoch;

            // 返回秒级时间戳
            return (long)span.TotalSeconds;
        }

        /// <summary>
        /// 填充主播当前录制的视频信息
        /// </summary>
        /// <param name="anchorDto">主播信息</param>
        private void FillRecordVideoInfo(AnchorDto anchorDto)
        {
            // 从录制列表中获取当前主播在录制的视频
            if(AnchorBll.recordingList != null && AnchorBll.recordingList.Count > 0)
            {
                AnchorBll.recordingList.TryGetValue(anchorDto.SecUid, out AnchorRecordBll anchorRecordBll);
                if(anchorRecordBll != null)
                {
                    VideoEntity anchorVideo = anchorRecordBll.GetVideo();
                    if(anchorVideo != null)
                    {
                        anchorDto.Duration = ConvertSecondsToTimeFormat(Convert.ToDouble(anchorVideo.duration));
                        anchorDto.Paragraph = (int)anchorVideo.paragraph;
                        anchorDto.CurrentRecordStartTime = anchorVideo.startTime;
                        //将字节转换为M
                        anchorDto.VedioSizie = (Convert.ToInt64(anchorVideo.vedioSizie) / 1024 / 1024).ToString();
                        anchorDto.CurrentRecordStatus = (int)anchorVideo.isRecording;
                    }
                }
            }
            

            //// 获取主播的视频列表
            //List<VideoEntity> list = VideoCacheManager.ListVideoBySecUid(anchorDto.SecUid);

            //if (list != null && list.Count > 0)
            //{
            //    VideoEntity anchorVideo = list.FirstOrDefault();

            //    anchorDto.Duration = ConvertSecondsToTimeFormat(Convert.ToDouble(anchorVideo.duration));
            //    anchorDto.Paragraph = (int)anchorVideo.paragraph;
            //    anchorDto.CurrentRecordStartTime = anchorVideo.startTime;
            //    //将字节转换为M
            //    anchorDto.VedioSizie = (Convert.ToInt64(anchorVideo.vedioSizie) / 1024 / 1024).ToString();
            //    anchorDto.CurrentRecordStatus = (int)anchorVideo.isRecording;
            //}
        }


        /// <summary>
        /// 将秒数转换成 小时:分:秒的格式
        /// </summary>
        /// <param name="totalSeconds">秒数</param>
        /// <returns></returns>
        private string ConvertSecondsToTimeFormat(double totalSeconds)
        {
            int roundedSeconds = (int)Math.Round(totalSeconds);
            int hours = roundedSeconds / 3600;
            int minutes = (roundedSeconds % 3600) / 60;
            int seconds = roundedSeconds % 60;
            return $"{hours:D2}:{minutes:D2}:{seconds:D2}";
        }


        public static void KillBarrageGrabProcesses()
        {
            // 获取所有正在运行的进程
            Process[] processes = Process.GetProcessesByName("BarrageGrab");
            foreach (Process process in processes)
            {
                try
                {
                    process.Kill();
                    FileUtils.log($"Killed process {process.Id}", "杀死websocket进程",true);
                }
                catch (Exception ex)
                {
                    FileUtils.log($"Failed to kill process {process.Id}: {ex.Message}", "杀死websocket进程",true);
                }
            }
        }
        

        /// <summary>
        /// 开启/关闭弹幕监控
        /// </summary>
        /// <param name="secUid"></param>
        /// <param name="isBarrageMonitoring"></param>
        /// <exception cref="CustomException"></exception>
        public void UpdateBarrageMonitoring(string secUid, int isBarrageMonitoring)
        {
            AnchorInfo anchorTemp = AnchorCacheManager.GetAnchorByIdFromCache(secUid);

            if (anchorTemp == null) throw new CustomException("获取主播失败", 6001);

            if(anchorTemp.platform != 0)
            {
                throw new CustomException("暂支持抖音主播获取弹幕", 500);
            }

            if (anchorTemp.IsBarrageMonitoring == isBarrageMonitoring)
            {
                return;
            }

            if (anchorTemp.RecordStatus == 1) throw new CustomException("正在录制中不允许开启/关闭弹幕监控！", 6000);


            // 同步主播信息到服务器
            AnchorApi.UpdateBarrageMonitoringSync(secUid, isBarrageMonitoring);

            // 更新本地缓存
            AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            anchor.IsBarrageMonitoring = isBarrageMonitoring;
            AnchorCacheManager.SetAnchorCache(anchor);


            //if (isBarrageMonitoring == 0)
            //{
            //    // 关闭弹幕监控不需要做别的操作
            //    AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            //    anchor.IsBarrageMonitoring = 0;
            //    AnchorCacheManager.SetAnchorCache(anchor);
            //}
            //else
            //{
            //    // 检查弹幕监控的主播是否超出数量
            //    UserPropertyEntity userProperty = UserPropertyHttpUtils.GetUserProperty(ReplayHttpUtils.Token).Result;
            //    if (userProperty.TotalAnchorBarrageNum == 0)
            //    {
            //        throw new CustomException("监控弹幕直播间数量不足！", 6001);
            //    }

            //    int currentNum = 0;
            //    List<AnchorInfo> anchorInfos = AnchorCacheManager.GetAllNotRemoveAnchors();
            //    foreach (var item in anchorInfos)
            //    {
            //        if (item.IsBarrageMonitoring == 1 && item.SecUid != secUid)
            //        {
            //            currentNum++;
            //        }
            //    }
            //    // 判断当前弹幕监控的数量
            //    if (currentNum >= userProperty.TotalAnchorBarrageNum)
            //    {
            //        throw new CustomException("监控弹幕直播间数量不足！", 6001);
            //    }
            //    // 关闭弹幕监控不需要做别的操作
            //    AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            //    anchor.IsBarrageMonitoring = 1;
            //    AnchorCacheManager.SetAnchorCache(anchor);
            //}

            // 通知服务器
            //dynamic result = ReplayHttpUtils.UpdateBarrageMonitoring(secUid, isBarrageMonitoring);
            //if(result.code != 0)
            //{
            //    // 接口报错，还原回之前的弹幕监控状态
            //    AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            //    anchor.IsBarrageMonitoring = anchorTemp.IsBarrageMonitoring;
            //    AnchorCacheManager.SetAnchorCache(anchor);
            //    throw new CustomException(result.msg, result.code);
            //}
        }


        /// <summary>
        /// 打开保存视频文件的目录
        /// </summary>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public bool OpenFolder(string secUid)
        {
            ConfigBll configBll = new ConfigBll();
            Config config = configBll.GetModel();
            //当前的保存视频目录
            string folderPath = config.SavePath;
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            string anchorName = anchorInfo.AnchorName;
            string platform = GetPlatFormNameByConfig(anchorInfo.LiveUrl);
            if (string.IsNullOrEmpty(platform))
            {
                platform = GetPlatFormNameByConfig(anchorInfo.AppShareUrl);
            }

            folderPath = folderPath + "\\" + anchorInfo.FolderName;
            if (!System.IO.Directory.Exists(folderPath))
            {
                Directory.CreateDirectory(folderPath);

            }
            //explorer.exe是Windows操作系统中的文件资源管理器程序，用于浏览文件系统、管理文件和文件夹
            Process.Start("explorer.exe", folderPath);
            return true;
        }

        /// <summary>
        /// 通过配置获取直播平台
        /// </summary>
        /// <param name="url"></param>
        /// <returns></returns>
        private string GetPlatFormNameByConfig(string url)
        {
            List<Dictionary<string, object>> urlMappings = GetMappings(url);
            foreach (Dictionary<string, object> item in urlMappings)
            {
                string liveUrl = item["url"].ToString();
                if (url != null && url.Contains(liveUrl))
                {
                    string platformName = item["platformName"].ToString();
                    return platformName;

                }
            }
            return null;
        }

        private List<Dictionary<string, object>> GetMappings(string url)
        {
            string configFilePath = "Config/config.json";
            if (!File.Exists(configFilePath))
            {
                throw new Exception("缺少配置文件");
            }
            string json = File.ReadAllText(configFilePath);
            List<Dictionary<string, object>> urlMappings = JsonConvert.DeserializeObject<List<Dictionary<string, object>>>(json);
            return urlMappings;
        }


        /// <summary>
        /// 预览指定文件
        /// </summary>
        /// <param name="anchorId"></param>
        public void PreviewVideo(string secUid)
        {
            AnchorInfo model = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if (model != null && !string.IsNullOrEmpty(model.StreamUrl))
            {
                // 获取所有正在运行的进程
                Process[] processes = Process.GetProcessesByName("ffplay");
                // 终止所有找到的ffmpeg进程
                foreach (Process process in processes)
                {
                    try
                    {
                        process.Kill();
                        FileUtils.log($"Killed process {process.Id}");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.log($"Failed to kill process {process.Id}: {ex.Message}");
                    }
                }
                Thread thread = new Thread(() => PlayVideo(model.StreamUrl));
                thread.Start();
            }
            else
            {
                throw new Exception($"secuid:{secUid},找不到相关的实体，请检测secuid是否正确");
            }
        }


        /// <summary>
        /// 根据视频地址播放视频
        /// </summary>
        /// <param name="videoFilePath"></param>
        private void PlayVideo(string videoFilePath)
        {
            string ffplayPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "tools\\ffplay.exe");
            // 创建一个ProcessStartInfo对象，用于设置ffplay的启动信息
            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = ffplayPath; // ffplay的可执行文件名
            int windowWidth = 800; // 设置窗口宽度
            int windowHeight = 600; // 设置窗口高度 
            string arguments = $"-i \"{videoFilePath}\" -x {windowWidth} -y {windowHeight}";
            startInfo.Arguments = arguments; // 设置窗口大小和视频文件路径
            startInfo.UseShellExecute = false; // 不使用外壳程序
            startInfo.CreateNoWindow = true;

            // 创建一个Process对象并启动ffplay
            using (Process process = new Process())
            {
                process.StartInfo = startInfo;
                process.Start(); // 启动ffplay
                // 等待ffplay进程结束
                process.WaitForExit();
            }
        }

        /// <summary>
        /// 更新监控位的开启/关闭状态
        /// </summary>
        /// <returns></returns>
        public async Task updateAnchor()
        {
            try
            {
                // 没有token，返回
                if (string.IsNullOrEmpty(ReplayHttpUtils.Token))
                {
                    return;
                }
                // 从服务器获取主播列表
                List<CurrentMonitoringPositionVo> anchorUrlEntities = await AnchorApi.getCurrentMonitoringPosition();
                if (anchorUrlEntities != null && anchorUrlEntities.Count > 0)
                {
                    foreach (var serveAnchorItem in anchorUrlEntities)
                    {
                        AnchorInfo anchor = AnchorCacheManager.GetAnchorByIdFromCache(serveAnchorItem.anchorUrlSecUid);
                        if (anchor == null) continue;
                        anchor.IsBarrageMonitoring = serveAnchorItem.isBarrageMonitoring ?? 0;
                        // 添加到缓存
                        AnchorCacheManager.SetAnchorCache(anchor);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError(ex.Message, "获取主播对应的弹幕监控位出错");
            }
        }

        /// <summary>
        /// 将服务器上的主播同步到本地
        /// </summary>
        public void syncServerAnchor()
        {
            try
            {
                // 清空AnchorCaches缓存
                AnchorCacheManager.AnchorCachesClear();
                List<AnchorEntity> anchorEntities = AnchorApi.ClientAnchorList();
                if (anchorEntities != null && anchorEntities.Count > 0)
                {
                    foreach (var serveAnchorItem in anchorEntities)
                    {
                        AnchorInfo anchor = new AnchorInfo();
                        anchor.SecUid = serveAnchorItem.anchorUrlSecUid;
                        anchor.AnchorAvatar = serveAnchorItem.anchorInfo.anchorAvatar;
                        anchor.AnchorPlatform = serveAnchorItem.anchorInfo.platformResource;
                        anchor.HomeUrl = serveAnchorItem.anchorInfo.homeUrl;
                        anchor.AnchorName = serveAnchorItem.anchorInfo.anchorName;
                        anchor.LiveStatus = 0;
                        anchor.IsAutoRecord = serveAnchorItem.isAutoRecord;
                        anchor.LiveUrl = serveAnchorItem.anchorInfo.liveUrl;
                        anchor.AddTime = !string.IsNullOrEmpty(serveAnchorItem.updateDate) ? serveAnchorItem.updateDate : ServerTimeUtils.getCurrentTimeStr();
                        anchor.TradeId = string.IsNullOrEmpty(serveAnchorItem.tradeId) ? "1" : serveAnchorItem.tradeId;
                        anchor.AnchorUserId = serveAnchorItem.anchorInfo.anchorUserId;
                        anchor.WebSocketId = serveAnchorItem.anchorInfo.webSocketId;
                        anchor.IsRemoveRecord = serveAnchorItem.isRemoveRecord;
                        anchor.IsBarrageMonitoring = serveAnchorItem.isBarrageMonitoring;
                        anchor.IsAutoUploadCloud = serveAnchorItem.isAutoUploadCloud;
                        anchor.IsTop = serveAnchorItem.isTop;
                        anchor.AddTopTime = serveAnchorItem.addTopTime;
                        anchor.LastRecordTime = serveAnchorItem.lastRecordTime;
                        anchor.RecordTime = serveAnchorItem.recordTime;
                        anchor.SmsTip = serveAnchorItem.smsTip;
                        anchor.IsDataViewing = serveAnchorItem.isDataViewing;
                        anchor.FolderName = string.IsNullOrEmpty(serveAnchorItem.folderName) ? WindowsUtils.SanitizeForFolderName(anchor.AnchorName) : serveAnchorItem.folderName;
                        anchor.isAutoDiagnosis = serveAnchorItem.isAutoDiagnosis;
                        anchor.diagnosisParams = serveAnchorItem.diagnosisParams;
                        anchor.RemarksName = serveAnchorItem.remarksName;
                        anchor.juliangAuthStatus = serveAnchorItem.authJlbyStatus??0;
                        anchor.qianchuanAuthStatus = serveAnchorItem.authQcStatus??0;
                        anchor.lifeAuthStatus = serveAnchorItem.authLifeStatus??0;
                        anchor.platform = serveAnchorItem.anchorInfo.platform;
                        anchor.deleteDate = serveAnchorItem.deleteDate;
                        anchor.recordDefinition = serveAnchorItem.recordDefinition;
                        anchor.recordLimitType = serveAnchorItem.recordLimitType;
                        anchor.recordLimitValue = serveAnchorItem.recordLimitValue;
                        anchor.isAutoAnalysis = serveAnchorItem.isAutoAnalysis;
                        anchor.anchorNumber = serveAnchorItem.anchorInfo.anchorNumber;
                        anchor.WeChatChannelsAuthStatus = serveAnchorItem.WeChatChannelsAuthStatus;
                        anchor.isDataDiagnosis = serveAnchorItem.isDataDiagnosis;
                        anchor.dataDiagnosisParams = serveAnchorItem.dataDiagnosisParams;
                        anchor.diagnosisGenerateNum = serveAnchorItem.diagnosisGenerateNum;
                        anchor.engSerViceType = serveAnchorItem.engSerViceType;
                        anchor.pureRecordOnlineNum = serveAnchorItem.pureRecordOnlineNum;
                        anchor.isScheduleRecord = serveAnchorItem.isScheduleRecord;
                        anchor.recordTimeMode = serveAnchorItem.recordTimeMode;
                        anchor.segmentTimePoints = serveAnchorItem.segmentTimePoints;
                        anchor.isStatisticsPerformance = serveAnchorItem.isStatisticsPerformance;
                        anchor.autoDeleteTime = serveAnchorItem.autoDeleteTime;
                        anchor.deleteContent = serveAnchorItem.deleteContent;


                        // 基础配置
                        anchor.AccountType = serveAnchorItem.accountType ?? 0;
                        anchor.premiereDate = serveAnchorItem.premiereDate;
                        anchor.accountStage = serveAnchorItem.accountStage;
                        anchor.accountWaterLevel = serveAnchorItem.accountWaterLevel;
                        anchor.accountFlow = serveAnchorItem.accountFlow;
                        anchor.livingTarget = serveAnchorItem.livingTarget;
                        anchor.livingModality = serveAnchorItem.livingModality;
                        anchor.marketing = serveAnchorItem.marketing;
                        anchor.optimizeDirection = serveAnchorItem.optimizeDirection;
                        anchor.learning = serveAnchorItem.learning;
                        anchor.livingMode = serveAnchorItem.livingMode;
                        anchor.AnchorSituation = serveAnchorItem.anchorSituation;
                        anchor.roiAccuracy = serveAnchorItem.roiAccuracy;

                        // 添加到缓存
                        AnchorCacheManager.SetAnchorCache(anchor);

                    }

                    // 设置主播巨量百应的授权状态（根据本地Cookie）
                    UserPropertyEntity userPropertyEntity = UserPropertyApi.GetPropertyInfoSync();
                    long? totalRpaAmountNum = userPropertyEntity?.totalRpaAmountNum;
                    long? originalRpaAmountNum = totalRpaAmountNum;

                    List<AnchorInfo> allNotRemoveAnchors = AnchorCacheManager.GetAllNotRemoveAnchors();
                    if (allNotRemoveAnchors != null && allNotRemoveAnchors.Count > 0)
                    {
                        foreach (var anchorInfo in allNotRemoveAnchors)
                        {
                            // 设置主播巨量百应的授权状态（根据本地Cookie）
                            int juliangStatus = JuliangUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
                            if (juliangStatus < 0) juliangStatus = JuliangAuthStatusEnum.unAuth;
                            if (juliangStatus == JuliangAuthStatusEnum.auth
                                && totalRpaAmountNum != null && totalRpaAmountNum > 0)
                            {
                                totalRpaAmountNum--;
                            }
                            // 仅一方为auth时才同步，双方都不是auth或双方都是auth则跳过
                            bool serverJuliangAuth = anchorInfo.juliangAuthStatus == JuliangAuthStatusEnum.auth;
                            bool localJuliangAuth = juliangStatus == JuliangAuthStatusEnum.auth;
                            if (serverJuliangAuth != localJuliangAuth)
                            {
                                AnchorBll.UpdateJuliangAuthStatus(anchorInfo, juliangStatus);
                            }
                            // 设置主播企业号的授权状态（根据本地Cookie）
                            int enterpriseStatus = EnterpriseUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
                            if (enterpriseStatus == (int)EnterpriseAuthStatusEnum.auth)
                            {
                                anchorInfo.enterpriseAuthStatus = (int)EnterpriseAuthStatusEnum.auth;
                            }

                            // 设置主播后台的授权状态（根据本地Cookie）
                            bool cookieValid = AnchorLiveUtils.CheckCookieValid(anchorInfo.SecUid);
                            if (cookieValid)
                            {
                                anchorInfo.anchorLiveAuthStatus = (int)AnchorLiveAuthStatusEnum.auth;
                            }

                            // 设置来客的授权状态（根据本地Cookie）
                            int lifeStatus = LifeUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
                            if (lifeStatus < 0) lifeStatus = (int)LifeAuthStatusEnum.unAuth;
                            bool serverLifeAuth = anchorInfo.lifeAuthStatus == (int)LifeAuthStatusEnum.auth;
                            bool localLifeAuth = lifeStatus == (int)LifeAuthStatusEnum.auth;
                            if (serverLifeAuth != localLifeAuth)
                            {
                                AnchorBll.UpdateLifeAuthStatus(anchorInfo, lifeStatus);
                            }

                            // 设置千川的授权状态（根据本地Cookie）
                            int qianChuanStatus = QianchuanUtils.CheckAuthStatusByCookie(anchorInfo.SecUid);
                            bool serverQcAuth = anchorInfo.qianchuanAuthStatus == (int)QianchuanAuthStatusEnum.auth;
                            bool localQcAuth = qianChuanStatus == (int)QianchuanAuthStatusEnum.auth;
                            if (serverQcAuth != localQcAuth)
                            {
                                AnchorBll.UpdateQianchuanAuthStatus(anchorInfo, qianChuanStatus);
                            }
                        }
                    }

                    // 更新巨量授权资产使用数
                    if (originalRpaAmountNum != null)
                    {
                        UserPropertyApi.updateRpaPropertyUseNum((int)(originalRpaAmountNum.Value - (totalRpaAmountNum ?? 0)));
                    }

                    // 删除废弃的巨量百应授权文件夹
                    Task.Run(() =>
                    {
                        // 获取谷歌缓存文件夹下的所有文件夹
                        string[] subDirectories = Directory.GetDirectories(Path.GetFullPath("googleCache"));
                        if(subDirectories != null && subDirectories.Length > 0)
                        {
                            // 筛选出主播授权的文件夹
                            List<string> jlbyFolders = subDirectories.Where(dir => Path.GetFileName(dir).StartsWith("jlby", StringComparison.OrdinalIgnoreCase)).ToList();
                            if(jlbyFolders != null && jlbyFolders.Count > 0)
                            {
                                // 获取主播授权配置
                                Dictionary<string, string> dictionary = JuliangUtils.getJuliangAnchorCacheConfig();
                                if (dictionary != null && dictionary.Count > 0)
                                {
                                    foreach (var dict in dictionary)
                                    {
                                        string md5 = JuliangUtils.getCachePathMd5(dict.Key);
                                        foreach (var jlbtFolder in jlbyFolders)
                                        {
                                            if (jlbtFolder.Contains(md5) && !jlbtFolder.Contains(dict.Value))
                                            {
                                                // md5匹配但是版本号不匹配，说明是旧的授权缓存文件夹，可以删除
                                                if(Directory.Exists(jlbtFolder))
                                                {
                                                   Directory.Delete(jlbtFolder, true);
                                                }
                                        }
                                        }
                                        
                                    }

                                }
                            }

                            
                        }
                    });

                }
            } catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"从服务器获取主播列表发生异常");
            }
            

        }


        /// <summary>
        /// 开启/关闭自动上传云空间
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="isAutoUploadCloud">自动上传云空间 0否 1是</param>
        public void OpenOrCloseAutoUploadCloud(string secUid, int isAutoUploadCloud)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorInfo != null)
            {

                AnchorApi.UpdateAutoUploadCloudSync(secUid, isAutoUploadCloud);

                anchorInfo.IsAutoUploadCloud = isAutoUploadCloud;
                AnchorCacheManager.SetAnchorCache(anchorInfo);
            }else
            {
                throw new Exception("主播信息不存在");
            }
        }

        /// <summary>
        /// 置顶主播
        /// </summary>
        /// <param name="secUid">主播secuid</param>
        /// <param name="action">动作 0：取消置顶 1：置顶</param>
        public void topAnchor(string secUid, int action)
        {
            AnchorInfo anchorInfo = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorInfo != null )
            {
                anchorInfo.IsTop = action;
                if(action == 0)
                {
                    anchorInfo.AddTopTime = "";
                }else
                {
                    anchorInfo.AddTopTime = ServerTimeUtils.getCurrentTimeStr();
                }
                ReplayHttpUtils.UpdateAnchorTop(secUid, action, anchorInfo.AddTopTime);
                AnchorCacheManager.SetAnchorCache(anchorInfo);
            }
        }

        /// <summary>
        /// 开启/关闭主播的数据看板
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="isDataViewing">是否开启数据看板 0：否 1：是</param>
        public void OpenOrCloseDataViewing(string secUid, int isDataViewing)
        {

            AnchorInfo anchorCache = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if(anchorCache == null)
            {
                throw new Exception("主播信息不存在");
            }
            
            anchorCache.IsDataViewing = isDataViewing;
            AnchorApi.AddOrUpdateAnchorSync(anchorCache);
            // 设置到缓存
            AnchorCacheManager.SetAnchorCache(anchorCache);
            
        }

        /// <summary>
        /// 修改主播的纯录制版是否获取在线人数状态
        /// </summary>
        /// <param name="secUid">主播id</param>
        /// <param name="pureRecordOnlineNum">纯录制版是否获取在线人数 0：否 1：是</param>
        public void updatePureRecordOnlineNum(string secUid, int pureRecordOnlineNum)
        {

            AnchorInfo anchorCache = AnchorCacheManager.GetAnchorByIdFromCache(secUid);
            if (anchorCache == null)
            {
                throw new Exception("主播信息不存在");
            }

            anchorCache.pureRecordOnlineNum = pureRecordOnlineNum;
            AnchorApi.AddOrUpdateAnchorSync(anchorCache);
            // 设置到缓存
            AnchorCacheManager.SetAnchorCache(anchorCache);

        }
    }
}
