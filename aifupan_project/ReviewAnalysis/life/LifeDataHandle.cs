using Newtonsoft.Json;
using ReviewAnalysis.Model;
using douyin.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using ServerTimeUtils = ReviewAnalysis.Utils.ServerTimeUtils;
using FrontNotice = ReviewAnalysis.Utils.FrontNotice;

using ReviewAnalysis.api;
using ReviewAnalysis.vo.oceanEngineData;
using ReviewAnalysis.upload;
using System.IO.Compression;
using ReviewAnalysis.vo;

namespace ReviewAnalysis.life
{
    public class LifeDataHandle
    {
        private const string dataCollectPath = "dataCollect";
        private const string lifeDataCollectPath = @"dataCollect\Life";
        private const string realTimeDirectory = "realTime";
        private const string finishDirectory = "finish";

        /// <summary>
        /// 生成数据文件名（batchNumber_videoId 或 batchNumber）
        /// </summary>
        private static string GetLifeDataFileName(string batchNumber, string videoId)
        {
            return string.IsNullOrEmpty(videoId) ? $"{batchNumber}.txt" : $"{batchNumber}_{videoId}.txt";
        }

        public static void WriteRealTimeData(LifeDataCollectEventArgs data)
        {
            try
            {
                if (data == null || string.IsNullOrEmpty(data.batchNumber))
                {
                    return;
                }

                var realTimeData = new LifeRealTimeDataEntity
                {
                    roomId = data.batchNumber,
                    videoId = data.videoId,
                    secUid = data.secUid,
                    dataType = data.key,
                    dataJson = data.dataJson,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                WriteDataToLocal(realTimeData);
                NotifyFrontend(data);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入来客实时数据异常: {ex.Message}", "来客数据处理");
            }
        }

        public static void WriteGatherData(LifeDataCollectEventArgs data)
        {
            try
            {
                if (data == null || string.IsNullOrEmpty(data.batchNumber))
                {
                    return;
                }

                var gatherData = new LifeGatherDataEntity
                {
                    roomId = data.batchNumber,
                    videoId = data.videoId,
                    secUid = data.secUid,
                    dataType = data.key,
                    dataJson = data.dataJson,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                WriteDataToLocal(gatherData);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入来客汇总数据异常: {ex.Message}", "来客数据处理");
            }
        }

        private static void WriteDataToLocal(object data)
        {
            try
            {
                string directory = Path.GetFullPath("lifeData");
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                string fileName = $"{directory}\\life_data_{DateTime.Now:yyyyMMdd}.json";
                string jsonData = JsonConvert.SerializeObject(data, Formatting.Indented);

                using (StreamWriter writer = new StreamWriter(fileName, true, System.Text.Encoding.UTF8))
                {
                    writer.WriteLine(jsonData);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入来客数据到本地文件异常: {ex.Message}", "来客数据处理");
            }
        }

        private static void NotifyFrontend(LifeDataCollectEventArgs data)
        {
            try
            {
                var frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>
                {
                    { "code", 0 },
                    { "status", 200 },
                    { "action", "lifeDataUpdate" },
                    { "data", new
                        {
                            roomId = data.batchNumber,
                            videoId = data.videoId,
                            dataType = data.key,
                            data = JsonConvert.DeserializeObject(data.dataJson)
                        }
                    }
                };
                frontNotice.NoticeJs(JsonConvert.SerializeObject(requestDataObj));
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"通知前端来客数据异常: {ex.Message}", "来客数据处理");
            }
        }

        public static Dictionary<string, string> GetCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = LifeUtils.getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return new Dictionary<string, string>();
                }

                string cookieContent = File.ReadAllText(cookiePath);

                // 兼容新旧格式：新格式为带元数据的对象，旧格式为纯Cookie数组
                var (fileDto, isNewFormat) = LifeCookieFileDto.Parse(cookieContent);
                var cookies = fileDto.Cookies;

                if (cookies == null || cookies.Count == 0)
                {
                    // 双重兼容：尝试直接反序列化为旧格式
                    cookies = JsonConvert.DeserializeObject<List<LifeCookieDto>>(cookieContent);
                    if (cookies == null || cookies.Count == 0)
                    {
                        return new Dictionary<string, string>();
                    }
                }

                var result = new Dictionary<string, string>();
                foreach (var cookie in cookies)
                {
                    result[cookie.Name] = cookie.Value;
                }
                return result;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取来客Cookie异常: {ex.Message}", "来客数据处理");
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 从Cookie文件中读取 awemeUserId 元数据（新格式）。
        /// 旧格式文件或读取失败时返回 null。
        /// </summary>
        public static string GetAwemeUserIdFromCookieFile(string secUid)
        {
            try
                       {
                string cookiePath = LifeUtils.getCookiePath(secUid);
                if (!File.Exists(cookiePath))
                {
                    return null;
                }

                string cookieContent = File.ReadAllText(cookiePath);
                var (fileDto, isNewFormat) = LifeCookieFileDto.Parse(cookieContent);

                if (isNewFormat && !string.IsNullOrEmpty(fileDto.AwemeUserId))
                {
                    return fileDto.AwemeUserId;
                }

                return null;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从Cookie文件读取awemeUserId异常: {ex.Message}", "来客数据处理");
                return null;
            }
        }

        public static async Task PullLifeData(AnchorInfo anchorInfo, string roomId, string videoId, string beToken)
        {
            try
            {
                // 若 videoId 为空，使用 roomId 作为默认值
                string effectiveVideoId = !string.IsNullOrEmpty(videoId) ? videoId : roomId;

                var cookies = GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa("本地无来客Cookie，无法拉取数据", "来客数据处理");
                    return;
                }

                var accountDetail = await LifeDataApi.GetAccountDetail(cookies);
                if (accountDetail == null || !accountDetail.ContainsKey("group_id"))
                {
                    FileUtils.LogRpa("获取来客账户信息失败", "来客数据处理");
                    return;
                }

                FileUtils.LogRpa($"获取到来客账户信息: account_id={accountDetail["account_id"]}, group_id={accountDetail["group_id"]}", "来客数据处理");

                // 优先使用API返回的current_live_room_id作为正确的来客roomId
                if (accountDetail.ContainsKey("current_live_room_id"))
                {
                    var liveRoomId = accountDetail["current_live_room_id"];
                    if (!string.IsNullOrEmpty(liveRoomId) && liveRoomId != "0")
                    {
                        roomId = liveRoomId;
                        FileUtils.LogRpa($"使用API返回的来客roomId: {roomId}", "来客数据处理");
                    }
                }

                if (string.IsNullOrEmpty(roomId) || roomId == "0")
                {
                    FileUtils.LogRpa("未获取到有效的来客roomId，跳过本次采集", "来客数据处理");
                    return;
                }

                if (string.IsNullOrEmpty(beToken))
                {
                    var groupId = accountDetail["group_id"];
                    var awemeUserId = accountDetail["aweme_user_id"];
                    var lifeAccountId = accountDetail["life_account_id"];

                    var encryptedToken = await LifeDataApi.GetEncryptedToken(groupId, awemeUserId, lifeAccountId, cookies);
                    if (string.IsNullOrEmpty(encryptedToken))
                    {
                        FileUtils.LogRpa("获取encrypted_token失败", "来客数据处理");
                        return;
                    }
                    FileUtils.LogRpa($"获取encrypted_token成功: {encryptedToken}", "来客数据处理");

                    var tokenResult = await LifeDataApi.GetLiveToken(encryptedToken);
                    if (tokenResult == null || !tokenResult.ContainsKey("be-token"))
                    {
                        FileUtils.LogRpa("获取be-token失败", "来客数据处理");
                        return;
                    }
                    beToken = tokenResult["be-token"];
                    FileUtils.LogRpa($"获取be-token成功", "来客数据处理");
                }

                var liveData = await LifeDataApi.LiveScreenKeyIndex(beToken, roomId);
                if (liveData != null && liveData.Count > 0)
                {
                    FileUtils.LogRpa("获取来客直播大屏数据成功", "来客数据处理");

                    // 保存到正确的目录（dataCollect\Life\finish\ 和 dataCollect\Life\realTime\）
                    SaveLifeData(effectiveVideoId, anchorInfo.SecUid, roomId, liveData);

                    // 通知前端实时数据更新
                    var dataArgs = new LifeDataCollectEventArgs
                    {
                        batchNumber = roomId,
                        videoId = effectiveVideoId,
                        secUid = anchorInfo.SecUid,
                        key = "liveScreenKeyIndex",
                        dataJson = JsonConvert.SerializeObject(liveData),
                        lifeForm = null
                    };
                    NotifyFrontend(dataArgs);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取来客数据异常: {ex.Message}", "来客数据处理");
            }
        }

        /// <summary>
        /// 上传来客数据到服务器（参考巨量dataUpload实现）
        /// </summary>
        /// <param name="batchNumber">批次号</param>
        /// <param name="videoId">视频ID</param>
        /// <param name="secUid">主播secUid</param>
        /// <returns></returns>
        public static bool dataUpload(string batchNumber, string videoId, string secUid)
        {
            try
            {
                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, lifeDataCollectPath, finishDirectory, GetLifeDataFileName(batchNumber, videoId));
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, lifeDataCollectPath, realTimeDirectory, GetLifeDataFileName(batchNumber, videoId));

                // 判断来客汇总数据是否存在
                string dataJson = null;
                string summaryFilePath = finishFileFullPath;
                if (File.Exists(summaryFilePath))
                {
                    try
                    {
                        dataJson = File.ReadAllText(summaryFilePath, System.Text.Encoding.UTF8);
                        string[] fileString = File.ReadAllLines(summaryFilePath, System.Text.Encoding.UTF8);
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
                        FileUtils.LogRecrd($"{ex.Message}, {ex.StackTrace}", "读取来客汇总数据报错");
                    }
                }
                dataJson = dataJson == null ? null : dataJson.Trim();
                if (string.IsNullOrEmpty(dataJson))
                {
                    FileUtils.LogRecrd($"来客汇总数据没有获取到 videoId = {videoId}");
                    return true;
                }

                LifeGatherDataEntity lifeGatherDataEntity = null;
                try
                {
                    lifeGatherDataEntity = JsonConvert.DeserializeObject<LifeGatherDataEntity>(dataJson);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRecrd($"格式化json报错{ex}", $"来客汇总数据格式化json报错==={videoId}===dataJson: {dataJson}");
                }
                if (lifeGatherDataEntity == null)
                {
                    FileUtils.LogRecrd("lifeGatherDataEntity == null");
                    return true;
                }

                // 判断来客实时数据是否存在
                string realFilePath = realTimeFileFullPath;
                string cosPath = null;
                if (File.Exists(realFilePath))
                {
                    DirectoryInfo parentDir = new FileInfo(realFilePath).Directory;
                    string zipName = $"{Guid.NewGuid().ToString()}.zip";
                    string zipNamePath = $"{parentDir.FullName}/{zipName}";
                    try
                    {
                        // 获取预上传url
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
                            FileUtils.LogRecrd($"videoId = {videoId}", "获取来客实时数据预上传url失败");
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogRpa($"{e.Message}, {e.StackTrace}", "上传来客实时数据报错");
                    }
                    finally
                    {
                        File.Delete(zipNamePath);
                    }
                }

                if (cosPath == null && dataJson == null)
                {
                    FileUtils.log($"videoId = {videoId},cosPath和dataJson都是为null，没有来客数据");
                    return true;
                }

                lifeGatherDataEntity.ossPath = cosPath;
                // 使用与巨量相同的API更新数据，但传入不同的数据类型
                string id = LifeDataApi.updateLifeEngine(lifeGatherDataEntity);

                if (!string.IsNullOrEmpty(id))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRecrd($"message = {ex.Message}, StackTrace = {ex.StackTrace}", "来客数据上传报错");
            }

            return false;
        }

        /// <summary>
        /// 保存来客数据到本地文件（参考巨量格式）
        /// </summary>
        /// <param name="videoId">视频ID</param>
        /// <param name="liveData">来客直播数据</param>
        public static void SaveLifeData(string videoId, string secUid, string batchNumber, Dictionary<string, string> liveData)
        {
            try
            {
                // 构建汇总数据
                var gatherData = new LifeGatherDataEntity
                {
                    videoId = videoId,
                    secUid = secUid,
                    batchNumber = batchNumber
                };

                // 映射字段
                if (liveData.ContainsKey("AcuTotalTd")) gatherData.AcuTotalTd = liveData["AcuTotalTd"];
                if (liveData.ContainsKey("AvgOrderGmv")) gatherData.AvgOrderGmv = liveData["AvgOrderGmv"];
                if (liveData.ContainsKey("ClientAvgWatchDuration")) gatherData.ClientAvgWatchDuration = liveData["ClientAvgWatchDuration"];
                if (liveData.ContainsKey("ClientLiveShowCntTd")) gatherData.ClientLiveShowCntTd = liveData["ClientLiveShowCntTd"];
                if (liveData.ContainsKey("ClientShareCntTd")) gatherData.ClientShareCntTd = liveData["ClientShareCntTd"];
                if (liveData.ContainsKey("CurrentUserCnt")) gatherData.CurrentUserCnt = liveData["CurrentUserCnt"];
                if (liveData.ContainsKey("FansClubJoinUv")) gatherData.FansClubJoinUv = liveData["FansClubJoinUv"];
                if (liveData.ContainsKey("FansNum")) gatherData.FansNum = liveData["FansNum"];
                if (liveData.ContainsKey("GPM")) gatherData.GPM = liveData["GPM"];
                if (liveData.ContainsKey("GmvRefundRatio")) gatherData.GmvRefundRatio = liveData["GmvRefundRatio"];
                if (liveData.ContainsKey("GoodsCtrUv")) gatherData.GoodsCtrUv = liveData["GoodsCtrUv"];
                if (liveData.ContainsKey("GoodsCvrUv")) gatherData.GoodsCvrUv = liveData["GoodsCvrUv"];
                if (liveData.ContainsKey("LiveCommentUcntTd")) gatherData.LiveCommentUcntTd = liveData["LiveCommentUcntTd"];
                if (liveData.ContainsKey("LiveCtr")) gatherData.LiveCtr = liveData["LiveCtr"];
                if (liveData.ContainsKey("LiveCvr")) gatherData.LiveCvr = liveData["LiveCvr"];
                if (liveData.ContainsKey("LiveFollowAnchorCnt")) gatherData.LiveFollowAnchorCnt = liveData["LiveFollowAnchorCnt"];
                if (liveData.ContainsKey("LiveFollowAnchorUcnt")) gatherData.LiveFollowAnchorUcnt = liveData["LiveFollowAnchorUcnt"];
                if (liveData.ContainsKey("LiveServerWatchUcnt")) gatherData.LiveServerWatchUcnt = liveData["LiveServerWatchUcnt"];
                if (liveData.ContainsKey("OrderCntRefundRatio")) gatherData.OrderCntRefundRatio = liveData["OrderCntRefundRatio"];
                if (liveData.ContainsKey("PayGmv")) gatherData.PayGmv = liveData["PayGmv"];
                if (liveData.ContainsKey("PayOrderCnt")) gatherData.PayOrderCnt = liveData["PayOrderCnt"];
                if (liveData.ContainsKey("PayUvAll")) gatherData.PayUvAll = liveData["PayUvAll"];
                if (liveData.ContainsKey("PcuTotalTd")) gatherData.PcuTotalTd = liveData["PcuTotalTd"];
                if (liveData.ContainsKey("ProductClickUvAll")) gatherData.ProductClickUvAll = liveData["ProductClickUvAll"];
                if (liveData.ContainsKey("ProductFee")) gatherData.ProductFee = liveData["ProductFee"];
                if (liveData.ContainsKey("ServerCommentCntTd")) gatherData.ServerCommentCntTd = liveData["ServerCommentCntTd"];
                if (liveData.ContainsKey("ServerLikeCntTotal")) gatherData.ServerLikeCntTotal = liveData["ServerLikeCntTotal"];
                if (liveData.ContainsKey("ServerWatchCntTd")) gatherData.ServerWatchCntTd = liveData["ServerWatchCntTd"];
                if (liveData.ContainsKey("UnfollowUv")) gatherData.UnfollowUv = liveData["UnfollowUv"];
                if (liveData.ContainsKey("ViolationPv")) gatherData.ViolationPv = liveData["ViolationPv"];

                // 保存汇总数据
                var finishFileFullPath = Path.Combine(Environment.CurrentDirectory, lifeDataCollectPath, finishDirectory, GetLifeDataFileName(batchNumber, videoId));
                CheckFilePath(finishFileFullPath);
                File.WriteAllText(finishFileFullPath, JsonConvert.SerializeObject(gatherData), System.Text.Encoding.UTF8);

                // 保存实时数据（当前只有一条数据，后续可扩展为分钟级数据）
                var realTimeFileFullPath = Path.Combine(Environment.CurrentDirectory, lifeDataCollectPath, realTimeDirectory, GetLifeDataFileName(batchNumber, videoId));
                CheckFilePath(realTimeFileFullPath);
                
                var realTimeData = new LifeRealTimeDataEntity
                {
                    roomId = batchNumber,
                    videoId = videoId,
                    secUid = secUid,
                    dataType = "liveScreenKeyIndex",
                    dataJson = JsonConvert.SerializeObject(liveData),
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };
                
                using (StreamWriter writer = File.AppendText(realTimeFileFullPath))
                {
                    writer.WriteLine(JsonConvert.SerializeObject(realTimeData));
                }

                FileUtils.LogRpa($"来客数据已保存到本地，videoId: {videoId}", "来客数据处理");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"保存来客数据异常: {ex.Message}", "来客数据处理");
            }
        }

        /// <summary>
        /// 检查文件路径
        /// </summary>
        private static void CheckFilePath(string filePath)
        {
            string directoryPath = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(directoryPath))
            {
                Directory.CreateDirectory(directoryPath);
            }
        }
    }

    public class LifeDataCollectEventArgs : EventArgs
    {
        public string batchNumber { get; set; }
        public string videoId { get; set; }
        public string secUid { get; set; }
        public string key { get; set; }
        public string dataJson { get; set; }
        public LifeForm lifeForm { get; set; }
    }

    public class LifeRealTimeDataEntity
    {
        public string roomId { get; set; }
        public string videoId { get; set; }
        public string secUid { get; set; }
        public string dataType { get; set; }
        public string dataJson { get; set; }
        public string createTime { get; set; }
    }

    public class LifeGatherDataEntity
    {
        public string roomId { get; set; }
        public string videoId { get; set; }
        public string secUid { get; set; }
        public string batchNumber { get; set; }
        public string dataType { get; set; }
        public string dataJson { get; set; }
        public string createTime { get; set; }
        public string ossPath { get; set; }
        // 来客直播大屏字段
        public string AcuTotalTd { get; set; }
        public string AvgOrderGmv { get; set; }
        public string ClientAvgWatchDuration { get; set; }
        public string ClientLiveShowCntTd { get; set; }
        public string ClientShareCntTd { get; set; }
        public string CurrentUserCnt { get; set; }
        public string FansClubJoinUv { get; set; }
        public string FansNum { get; set; }
        public string GPM { get; set; }
        public string GmvRefundRatio { get; set; }
        public string GoodsCtrUv { get; set; }
        public string GoodsCvrUv { get; set; }
        public string LiveCommentUcntTd { get; set; }
        public string LiveCtr { get; set; }
        public string LiveCvr { get; set; }
        public string LiveFollowAnchorCnt { get; set; }
        public string LiveFollowAnchorUcnt { get; set; }
        public string LiveServerWatchUcnt { get; set; }
        public string OrderCntRefundRatio { get; set; }
        public string PayGmv { get; set; }
        public string PayOrderCnt { get; set; }
        public string PayUvAll { get; set; }
        public string PcuTotalTd { get; set; }
        public string ProductClickUvAll { get; set; }
        public string ProductFee { get; set; }
        public string ServerCommentCntTd { get; set; }
        public string ServerLikeCntTotal { get; set; }
        public string ServerWatchCntTd { get; set; }
        public string UnfollowUv { get; set; }
        public string ViolationPv { get; set; }
    }
}