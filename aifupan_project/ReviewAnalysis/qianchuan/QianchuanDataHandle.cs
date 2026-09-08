using Newtonsoft.Json;
using ReviewAnalysis.api;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using douyin.Utils;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.qianchuan
{
    /// <summary>
    /// 千川数据处理类
    /// </summary>
    public class QianchuanDataHandle
    {
        /// <summary>
        /// 写入实时数据
        /// </summary>
        /// <param name="data">数据事件参数</param>
        public static void WriteRealTimeData(QianchuanDataCollectEventArgs data)
        {
            try
            {
                if (data == null || string.IsNullOrEmpty(data.batchNumber) || string.IsNullOrEmpty(data.videoId))
                {
                    return;
                }

                // 构建实时数据实体
                QianchuanRealTimeDataEntity realTimeData = new QianchuanRealTimeDataEntity
                {
                    roomId = data.batchNumber,
                    videoId = data.videoId,
                    secUid = data.secUid,
                    dataType = data.key,
                    dataJson = data.dataJson,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                // 保存到缓存
                QianchuanRealTimeDataQueueEntity queueEntity = new QianchuanRealTimeDataQueueEntity
                {
                    realTimeData = realTimeData,
                    addTime = ServerTimeUtils.getCurrentTime()
                };

                // 这里可以添加到队列或直接处理
                // 暂时先写入本地文件作为示例
                WriteDataToLocal(realTimeData);

                // 通知前端
                NotifyFrontend(data);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入千川实时数据异常: {ex.Message}", "千川数据处理");
            }
        }

        /// <summary>
        /// 写入汇总数据
        /// </summary>
        /// <param name="data">数据事件参数</param>
        public static void WriteGatherData(QianchuanDataCollectEventArgs data)
        {
            try
            {
                if (data == null || string.IsNullOrEmpty(data.batchNumber) || string.IsNullOrEmpty(data.videoId))
                {
                    return;
                }

                // 构建汇总数据实体
                QianchuanGatherDataEntity gatherData = new QianchuanGatherDataEntity
                {
                    roomId = data.batchNumber,
                    videoId = data.videoId,
                    secUid = data.secUid,
                    dataType = data.key,
                    dataJson = data.dataJson,
                    createTime = ServerTimeUtils.getCurrentTimeStr()
                };

                // 保存到缓存
                // 暂时先写入本地文件作为示例
                WriteDataToLocal(gatherData);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入千川汇总数据异常: {ex.Message}", "千川数据处理");
            }
        }

        /// <summary>
        /// 写入数据到本地文件
        /// </summary>
        /// <param name="data">数据对象</param>
        private static void WriteDataToLocal(object data)
        {
            try
            {
                string directory = Path.GetFullPath("qianchuanData");
                if (!Directory.Exists(directory))
                {
                    Directory.CreateDirectory(directory);
                }

                string fileName = $"{directory}\\qianchuan_data_{DateTime.Now:yyyyMMdd}.json";
                string jsonData = JsonConvert.SerializeObject(data, Formatting.Indented);

                using (StreamWriter writer = new StreamWriter(fileName, true, Encoding.UTF8))
                {
                    writer.WriteLine(jsonData);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入千川数据到本地文件异常: {ex.Message}", "千川数据处理");
            }
        }

        /// <summary>
        /// 通知前端
        /// </summary>
        /// <param name="data">数据事件参数</param>
        private static void NotifyFrontend(QianchuanDataCollectEventArgs data)
        {
            try
            {
                FrontNotice frontNotice = new FrontNotice();
                var requestDataObj = new Dictionary<string, object>
                {
                    { "code", 0 },
                    { "status", 200 },
                    { "action", "qianchuanDataUpdate" },
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
                FileUtils.LogRpa($"通知前端千川数据异常: {ex.Message}", "千川数据处理");
            }
        }

        /// <summary>
        /// 从Cookie获取数据
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>Cookie字典</returns>
        public static Dictionary<string, string> GetCookiesFromLocal(string secUid)
        {
            try
            {
                string cookiePath = QianchuanUtils.getCookiePath(secUid);
                FileUtils.LogRpa($"从本地加载千川Cookie，路径: {cookiePath}", "千川数据处理");
                
                var localCookies = new CookiePersistenceHelper(cookiePath).LoadCefCookiesFromLocal(false);
                if (localCookies == null || localCookies.Count == 0)
                {
                    FileUtils.LogRpa($"千川Cookie文件不存在或为空: {cookiePath}", "千川数据处理");
                    return new Dictionary<string, string>();
                }

                Dictionary<string, string> cookies = new Dictionary<string, string>();
                foreach (var cookie in localCookies)
                {
                    cookies[cookie.Name] = cookie.Value;
                }
                
                FileUtils.LogRpa($"成功加载千川Cookie，共 {cookies.Count} 个", "千川数据处理");
                return cookies;
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"从本地获取千川Cookie异常: {ex.Message}", "千川数据处理");
                return new Dictionary<string, string>();
            }
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
            try
            {
                // aavid 校验
                if (string.IsNullOrEmpty(aavid))
                {
                    FileUtils.LogRpa($"主播{anchorInfo?.AnchorName}千川aavid为空，无法拉取大屏数据", "千川数据处理");
                    return;
                }

                // anchorId 校验
                if (string.IsNullOrEmpty(anchorId))
                {
                    FileUtils.LogRpa($"主播{anchorInfo?.AnchorName}千川anchorId为空，无法拉取大屏数据", "千川数据处理");
                    return;
                }

                // 获取本地Cookie
                Dictionary<string, string> cookies = GetCookiesFromLocal(anchorInfo.SecUid);
                if (cookies.Count == 0)
                {
                    FileUtils.LogRpa("本地无千川Cookie，无法拉取数据", "千川数据处理");
                    return;
                }

                // 获取千川账户列表
                var accountList = await QianchuanDataApi.GetAccountUserList(roomId, cookies, anchorInfo.SecUid);
                if (accountList.Count > 0)
                {
                    FileUtils.LogRpa($"获取到 {accountList.Count} 个千川账户", "千川数据处理");
                    foreach (var account in accountList)
                    {
                        var id = account["id"];
                        var name = account["name"];
                        FileUtils.LogRpa($"账户ID: {id}, 账户名称: {name}", "千川数据处理");
                    }
                }

                // 获取全域大屏数据
                var overviewData = await QianchuanDataApi.GetOverviewBoardData(roomId, aavid, anchorId, cookies, anchorInfo.SecUid);
                if (overviewData.Count > 0)
                {
                    FileUtils.LogRpa("获取千川大屏数据成功", "千川数据处理");
                    foreach (var item in overviewData)
                    {
                        FileUtils.LogRpa($"{item.Key}: {item.Value}", "千川数据处理");
                    }

                    // 构建数据事件参数
                    QianchuanDataCollectEventArgs dataArgs = new QianchuanDataCollectEventArgs
                    {
                        batchNumber = roomId,
                        videoId = string.Empty,
                        secUid = anchorInfo.SecUid,
                        key = "overviewBoard",
                        dataJson = JsonConvert.SerializeObject(overviewData),
                        qianchuanForm = null
                    };

                    // 处理数据
                    WriteRealTimeData(dataArgs);
                    WriteGatherData(dataArgs);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"拉取千川数据异常: {ex.Message}", "千川数据处理");
            }
        }
    }

    /// <summary>
    /// 千川数据收集事件参数
    /// </summary>
    public class QianchuanDataCollectEventArgs : EventArgs
    {
        /// <summary>
        /// 直播间ID
        /// </summary>
        public string batchNumber { get; set; }

        /// <summary>
        /// 视频ID
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 数据类型
        /// </summary>
        public string key { get; set; }

        /// <summary>
        /// 数据JSON
        /// </summary>
        public string dataJson { get; set; }

        /// <summary>
        /// 千川窗体
        /// </summary>
        public QianchuanForm qianchuanForm { get; set; }
    }

    /// <summary>
    /// 千川实时数据实体
    /// </summary>
    public class QianchuanRealTimeDataEntity
    {
        /// <summary>
        /// 直播间ID
        /// </summary>
        public string roomId { get; set; }

        /// <summary>
        /// 视频ID
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 数据类型
        /// </summary>
        public string dataType { get; set; }

        /// <summary>
        /// 数据JSON
        /// </summary>
        public string dataJson { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createTime { get; set; }
    }

    /// <summary>
    /// 千川汇总数据实体
    /// </summary>
    public class QianchuanGatherDataEntity
    {
        /// <summary>
        /// 直播间ID
        /// </summary>
        public string roomId { get; set; }

        /// <summary>
        /// 视频ID
        /// </summary>
        public string videoId { get; set; }

        /// <summary>
        /// 主播SecUid
        /// </summary>
        public string secUid { get; set; }

        /// <summary>
        /// 数据类型
        /// </summary>
        public string dataType { get; set; }

        /// <summary>
        /// 数据JSON
        /// </summary>
        public string dataJson { get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createTime { get; set; }
    }

    /// <summary>
    /// 千川实时数据队列实体
    /// </summary>
    public class QianchuanRealTimeDataQueueEntity
    {
        /// <summary>
        /// 实时数据
        /// </summary>
        public QianchuanRealTimeDataEntity realTimeData { get; set; }

        /// <summary>
        /// 添加时间
        /// </summary>
        public long addTime { get; set; }
    }
}