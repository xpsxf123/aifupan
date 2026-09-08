using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.Asr;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.oceanEngineData;
using ReviewAnalysis.vo.user;
using ReviewAnalysis.Websocket;
using ReviewAnalysis.WeChatChannels.Models;
using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using static ReviewAnalysis.Websocket.Entity.WebsocketUtilsEntity;

namespace ReviewAnalysis.WeChatChannels.Services
{
    /// <summary>
    /// 微信视频号直播大屏服务类
    /// </summary>
    public static class WeChatChannelsDashboardService
    {
        private const string dataCollectPath = "dataCollect";
        private const string weChatChannelsDataCollectPath = @"dataCollect\WeChatChannels";
        private const string realTimeDirectory = "realTime";
        private const string finishDirectory = "finish";

        /// <summary>
        /// 上传直播大屏数据
        /// </summary>
        /// <param name="videoId">video Id</param>
        public static bool UploadDashboardData(string videoId)
        {
            try
            {
                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, finishDirectory, $"{videoId}.txt");
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, realTimeDirectory, $"{videoId}.txt");

                // 判断巨量的下播数据是否存在
                string dataJson = null;
                string summaryFilePath = finishFileFullPath;
                if (File.Exists(summaryFilePath))
                {
                    try
                    {
                        // 读取汇总数据文件的所有内容
                        dataJson = File.ReadAllText(summaryFilePath, Encoding.UTF8);
                        string[] fileString = File.ReadAllLines(summaryFilePath, Encoding.UTF8);
                        if (fileString != null)
                        {
                            List<string> tempList = fileString.Where(item => !string.IsNullOrEmpty(item)).ToList();
                            if (tempList != null && tempList.Count > 0)
                            {
                                dataJson = tempList[0];
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRecrd($"{ex.Message}, {ex.StackTrace}", "读取微信视频号汇总数据报错");
                    }
                }
                dataJson = dataJson == null ? null : dataJson.Trim();
                if (string.IsNullOrEmpty(dataJson))
                {
                    FileUtils.LogRecrd($"微信视频号汇总数据没有获取到 videoId = {videoId}");
                    return true;
                }

                JuliangGatherDataEntity juliangGatherDataEntity = null;
                try
                {
                    // 转换成接口请求对象
                    LiveVideoDataViewingResponse liveVideoDataViewing = JsonConvert.DeserializeObject<LiveVideoDataViewingResponse>(dataJson);
                    juliangGatherDataEntity = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(dataJson);
                    juliangGatherDataEntity.volume = liveVideoDataViewing.VolumeStart == null ? null : (liveVideoDataViewing.VolumeStart * 100);
                    juliangGatherDataEntity.purchaseCount = liveVideoDataViewing.PurchaseCountStart;
                    juliangGatherDataEntity.customerUnitPrice = liveVideoDataViewing.CustomerUnitPriceStart == null ? null : (liveVideoDataViewing.CustomerUnitPriceStart * 100);
                    juliangGatherDataEntity.uvValue = liveVideoDataViewing.UvValueStart;
                    juliangGatherDataEntity.goodsConvertRate = liveVideoDataViewing.GoodsConvertRateStart;
                    juliangGatherDataEntity.gpm = liveVideoDataViewing.GpmStart == null ? null : (liveVideoDataViewing.GpmStart * 100);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"格式化json报错{ex}", $"微信视频号汇总数据格式化json报错==={videoId}===dataJson: {dataJson}");
                }
                if (juliangGatherDataEntity == null)
                {
                    FileUtils.LogRecrd("juliangGatherDataEntity == null");
                    return true;
                }

                // 判断巨量的实时数据是否存在
                string realFilePath = realTimeFileFullPath;
                string cosPath = null;
                if (File.Exists(realFilePath))
                {
                    // 获取文件的父路径
                    DirectoryInfo parentDir = new FileInfo(realFilePath).Directory;
                    string zipName = $"{Guid.NewGuid().ToString()}.zip";
                    string zipNamePath = $"{parentDir.FullName}/{zipName}";
                    try
                    {
                        // 获取微信视频号实时数据预上传url
                        SignUploadUrlVo signUploadUrlVo = OceanEngineDataApi.getDiagnosisSignUploadUrl(videoId);

                        if (signUploadUrlVo != null)
                        {
                            FileUtils.ZipFileToOneFile(realFilePath, zipNamePath);

                            bool uploadFlag = UploadUtils.UploadFileAsync(signUploadUrlVo.signedUrl, zipNamePath);
                            if (uploadFlag)
                            {
                                cosPath = signUploadUrlVo.ossKey;
                            }
                        }
                        else
                        {
                            FileUtils.LogRecrd($"videoId = {videoId}", "获取微信视频号实时数据预上传url失败");
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogRpa($"{e.Message}, {e.StackTrace}", "上传微信视频号实时数据报错");
                    }
                    finally
                    {
                        // 删除zip
                        File.Delete(zipNamePath);
                    }

                }

                if (cosPath == null && dataJson == null)
                {
                    FileUtils.log($"videoId = {videoId},cosPath和dataJson都是为null，没有微信视频号数据");
                    return true;
                }

                juliangGatherDataEntity.ossPath = cosPath;
                string id = OceanEngineDataApi.updateOceanEngine(juliangGatherDataEntity);

                if (!string.IsNullOrEmpty(id))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"message = {ex.Message}, StackTrace = {ex.StackTrace}", "微信视频号数据上传报错");
            }

            return false;
        }

        /**
         * 创建微信的文件件
         *
         */
        private static void CreateWeChatDirectory()
        {
            var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, finishDirectory);
            var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, realTimeDirectory);
            Directory.CreateDirectory(finishFileFullPath);
            Directory.CreateDirectory(realTimeFileFullPath);
        }

        /// <summary>
        /// 下载上传的直播大屏数据
        /// </summary>
        /// <param name="videoId">video Id</param>
        public static void DownloadDashboardData(string videoId)
        {
            var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, finishDirectory, $"{videoId}.txt");
            var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, realTimeDirectory, $"{videoId}.txt");

            CreateWeChatDirectory();
            
            if (File.Exists(finishFileFullPath) &&
                File.Exists(realTimeFileFullPath))
            {
                return;
            }

            // 请求接口，获取对应的数据
            OceanEngineDataVo juLinag = OceanEngineDataApi.getOceanEngine(videoId);
            if (juLinag == null)
            {
                return;
            }

            // 判断巨量的实时数据是否存在
            if (!File.Exists(realTimeFileFullPath))
            {
                // 从oss下载
                // oss下载下来的文件是zip格式
                // 获取realFilePath文件的文件夹地址，拼接uuid.zip
                string zipFilePath = Path.Combine(Path.GetDirectoryName(realTimeFileFullPath), $"{Guid.NewGuid().ToString()}.zip");
                try
                {
                    if (OssUtils.DownloadFileAsync(juLinag.ossPath, zipFilePath))
                    {
                        // 解压文件
                        string unzipPath = Path.Combine(Path.GetDirectoryName(realTimeFileFullPath), $"{Guid.NewGuid().ToString()}");
                        try
                        {
                            ZipFile.ExtractToDirectory(zipFilePath, unzipPath);

                            // 获取unzipPath文件夹下的第一个txt文件，并重命名为realFilePath
                            var txtFiles = Directory.GetFiles(unzipPath, "*.txt");
                            if (txtFiles != null && txtFiles.Length > 0)
                            {
                                // 取第一个txt文件
                                string firstTxtFile = txtFiles[0];
                                try
                                {
                                    File.Move(firstTxtFile, realTimeFileFullPath);
                                }
                                catch (Exception ex)
                                {
                                    FileUtils.LogRpa($"{ex}", $"移动微信视频号实时数据txt文件发生异常===={firstTxtFile} 到 {realTimeFileFullPath}");
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"{ex}", $"解压微信视频号实时数据zip文件发生异常===={zipFilePath}");
                        }
                        finally
                        {
                            // 删除unzipPath文件夹
                            try
                            {
                                if (Directory.Exists(unzipPath))
                                {
                                    Directory.Delete(unzipPath, true);
                                }
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogRpa($"{ex}", $"删除微信视频号实时数据解压目录发生异常===={unzipPath}");
                            }
                        }

                        // 删除zip文件
                        try
                        {
                            if (File.Exists(zipFilePath))
                            {
                                File.Delete(zipFilePath);
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"{ex}", $"删除微信视频号实时数据zip文件发生异常===={zipFilePath}");
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"{ex}", $"下载或处理微信视频号实时数据zip文件发生异常===={zipFilePath}");
                }
            }

            // 判断汇总数据是否存在
            if (!File.Exists(finishFileFullPath))
            {
                try
                {
                    // 把juLinag.dataJson写入到文件
                    File.WriteAllText(finishFileFullPath, juLinag.dataJson, Encoding.UTF8);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"{ex}", $"写入微信视频号汇总数据文件发生异常===={finishFileFullPath}");
                }
            }
        }


        /// <summary>
        /// 读取并保存直播大屏数据
        /// </summary>
        /// <param name="video">视频信息</param>
        public static async Task SaveDashboardData(VideoEntity video)
        {
            WeChatChannelsDashboardModel dashboardModel = await WeChatChannelsSerivce.QueryDashboard(video);

            if (dashboardModel == null)
            {
                return;
            }

            // 筛选分钟级数据
            FilterMinuteRowsByDateTime(dashboardModel, video.startTime, video.endTime);
            // 保存视频号数据大屏数据
            await SaveWeChatChannelsDashboardData(video.videoId, dashboardModel);
            SaveToSocketData(video, dashboardModel);
        }

        /// <summary>
        /// 过滤每分钟的数据，只返回录制时间范围内的。接口返回的整个直播的全部数据。
        /// </summary>
        private static void FilterMinuteRowsByDateTime(WeChatChannelsDashboardModel model, string startDateTime, string endDateTime)
        {
            
            if (model.MinuteRows != null && model.MinuteRows.Count > 0)
            {
                var result = new List<LiveDashboardMinuteResponse>();
                long startTime = TimeUtils.getMillisecondByDateTimeStr(startDateTime);
                long endTime = TimeUtils.getMillisecondByDateTimeStr(endDateTime);
                foreach (var row in model.MinuteRows)
                {
                    if (row.GatherTimeStamp >= startTime && row.GatherTimeStamp <= endTime)
                    {
                        result.Add(row);
                    }
                }

                model.MinuteRows = result;
            }
        }

        /// <summary>
        /// 保存微信视频号的直播大屏数据
        /// </summary>
        private static async Task SaveWeChatChannelsDashboardData(string videoId, WeChatChannelsDashboardModel model)
        {
            await SaveRealTimeData(videoId, model.MinuteRows);
            await SaveFinishData(videoId, model.DataViewing);
        }

        /// <summary>
        /// 写入微信视频号的汇总数据
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="dataViewingResponse">汇总数据</param>
        /// <returns></returns>
        private static async Task SaveFinishData(string videoId, LiveVideoDataViewingResponse dataViewingResponse)
        {
            if (dataViewingResponse != null)
            {
                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, finishDirectory, $"{videoId}.txt");
                CheckFilePath(finishFileFullPath);

                // 直接覆盖原文件写入数据
                dataViewingResponse.VideoId = videoId;
                File.WriteAllText(finishFileFullPath, JsonConvert.SerializeObject(dataViewingResponse), Encoding.UTF8);

            }

        }

        /// <summary>
        /// 写入微信视频号的实时数据
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="minuteResponses">实时数据</param>
        /// <returns></returns>
        private static async Task SaveRealTimeData(string videoId, List<LiveDashboardMinuteResponse> minuteResponses)
        {
            if(minuteResponses != null && minuteResponses.Count > 0)
            {
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, realTimeDirectory, $"{videoId}.txt");
                
                if(File.Exists(realTimeFileFullPath))
                {
                    // 存在文件先删除
                    File.Delete(realTimeFileFullPath);
                }
                CheckFilePath(realTimeFileFullPath);
                // 追加写入实时数据
                using (StreamWriter writer = File.AppendText(realTimeFileFullPath))
                {
                    foreach (var item in minuteResponses)
                    {
                        await writer.WriteLineAsync(JsonConvert.SerializeObject(item));
                    }
                }
            }
        }

        /// <summary>
        /// 检查文件路径
        /// </summary>
        /// <param name="filePath">文件路径</param>
        private static void CheckFilePath(string filePath)
        {
            string directoryPath = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(directoryPath))
            {
                Directory.CreateDirectory(directoryPath);
            }
        }

        /// <summary>
        /// 根据视频id获取微信视频号的实时数据
        /// </summary>
        public static List<JuliangRealTimeDataEntity> GetDashboardRealTimeDataList(string videoId)
        {
            var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, weChatChannelsDataCollectPath, realTimeDirectory, $"{videoId}.txt");
            // 判断微信视频号的实时数据是否存在
            string realFilePath = realTimeFileFullPath;
            // 不存在直接从oss下载
            if (!File.Exists(realFilePath))
            {
                // 下载
                DownloadDashboardData(videoId);
            }

            // 判断微信视频号的实时数据是否存在
            if (!File.Exists(realFilePath))
            {
                return new List<JuliangRealTimeDataEntity>();
            }

            // 读取文件
            List<JuliangRealTimeDataEntity> result = new List<JuliangRealTimeDataEntity>();

            // 读取文件中的数据存到result中，每一行就是一个JuliangRealTimeDataEntity对象
            string[] lines = File.ReadAllLines(realFilePath);
            foreach (var line in lines)
            {
                if (string.IsNullOrEmpty(line))
                {
                    continue;
                }
                // json转换判断
                JuliangRealTimeDataEntity item = null;
                try
                {
                    item = JsonConvert.DeserializeObject<JuliangRealTimeDataEntity>(line);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"{ex}", $"解析微信视频号实时数据发生异常===={line}");
                }
                if (item != null)
                {
                    result.Add(item);
                }
            }

            return result;
        }

        /// <summary>
        /// 保存为抖音的 DataWebSocket 格式
        /// </summary>
        private static void SaveToSocketData(VideoEntity video, WeChatChannelsDashboardModel model)
        {
            if (model.MinuteRows == null || model.MinuteRows.Count < 1)
            {
                return;
            }

            // 如果服务器已经存在数据，跳过
            if (ReplayHttpUtils.SocketDataExist(video.batchNumber, long.Parse(video.userId), video.videoId))
            {
                return;
            }

            // 将视频号直播大屏的数据转换成在线曲线用的数据格式
            var result = ConvertToDataWebSocket(model.MinuteRows, video);
            result.batchNumber = video.batchNumber;
            result.videoId = video.videoId;

            string path = Path.Combine(
                Environment.CurrentDirectory,
                dataCollectPath,
                $"{RemoveFileNameIllegalCharacters(video.batchNumber)}-{result.userId}",
                $"{video.videoId}.txt");

            CheckFilePath(path);

            // 判断本地是否生成json的统计
            DataWebSocket data = null;
            lock (WebsocketDataHandle.fileSaveLock)
            {
                try
                {
                    data = result;
                    File.WriteAllText(path, JsonConvert.SerializeObject(data), Encoding.UTF8);
                }
                catch (Exception e)
                {
                    FileUtils.LogError($"{e.Message}", "微信视频号Websocket数据统计成json出错");
                    throw new Exception("微信视频号Websocket数据统计成json出错");
                }
            }
            if (data == null) throw new Exception("微信视频号Websocket数据统计成json出错");
            // 上传cos
            string webSocketFilePath = TencentCosUtils.uploadFile(path);

            if (string.IsNullOrEmpty(webSocketFilePath))
            {
                FileUtils.LogError("微信视频号上传cos出错");
                throw new CustomException("微信视频号上传cos出错");
            }

            if (webSocketFilePath != null)
            {
                var obj = new
                {
                    data.userId,
                    data.secUid,
                    video.batchNumber,
                    video.videoId,
                    cosKey = webSocketFilePath,
                    startDate = data.videoStartTime,
                    endDate = data.videoEndTime,
                    data.totalOnlineNum,
                    data.observationNum,
                    totalBarrageNum = 0,
                    onlineMaxNum = data.maxRenShu
                };

                if (!ReplayHttpUtils.UploadSocketData(JsonConvert.SerializeObject(obj)))
                {
                    FileUtils.LogError("微信视频号上传cos出错");
                    throw new CustomException("微信视频号上传cos出错");
                }
            }
        }

        /// <summary>
        /// 将视频号直播大屏的数据转换成在线曲线用的数据格式。
        /// </summary>
        private static DataWebSocket ConvertToDataWebSocket(List<LiveDashboardMinuteResponse> minuteRows, VideoEntity video)
        {
            var result = new DataWebSocket
            {
                serviceStartTime = video.startTime,
                serviceEndTime = video.endTime,
                videoStartTime = video.startTime,
                videoEndTime = video.endTime,
                secUid = video.secUid,
                version = "2.0",
                userId = ReplayHttpUtils.UserId,
                observationNum = "0",
                totalOnlineNum = "0",
                minRenShu = "0",
                maxRenShu = "0"
            };

            if (minuteRows?.Count > 0)
            {
                result.observationNum = (minuteRows.Max(i => i.WatchNum) - minuteRows.Min(i => i.WatchNum)).ToString();
                result.totalOnlineNum = minuteRows.Max(i => i.WatchNum).ToString();
                result.minRenShu = minuteRows.Min(i => i.MaxOnlineWatchNum).ToString();
                result.maxRenShu = minuteRows.Max(i => i.MaxOnlineWatchNum).ToString();
            }

            result.datas = new List<WebSocketEntity>();
            long videoStartTimeStamp = TimeUtils.getMillisecondByDateTimeStr(video.startTime);
            foreach (var item in minuteRows)
            {
                int videoTime = (int) ((videoStartTimeStamp - item.GatherTimeStamp) / 1000);
                result.datas.Add(new WebSocketEntity
                {
                    renshu = item.MaxOnlineWatchNum.ToString(),
                    time = item.GatherDateTime.ToString("yyyy-MM-dd HH:mm:ss"),
                    leijiguankanrenshu = item.WatchNum.ToString(),
                    guanzhu = item.FollowAnchorUcnt.ToString(),
                    fensituan = item.FansClubJoinUcnt.ToString(),
                    videoTime = videoTime + ""
                });
            }

            return result;
        }

        /// <summary>
        /// 删除文件名中的非法字符
        /// </summary>
        private static string RemoveFileNameIllegalCharacters(string fileName)
        {
            // 使用正则表达式替换非法字符为空字符串，你也可以选择替换为其他字符如 '_' 或 '-'
            string pattern = @"[<>:""/\\|?*]"; // 定义非法字符的正则表达式模式

            return Regex.Replace(fileName, pattern, ""); // 替换非法字符为空字符串
        }
    }
}
