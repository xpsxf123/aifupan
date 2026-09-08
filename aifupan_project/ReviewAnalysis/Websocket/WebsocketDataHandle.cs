using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using ReviewAnalysis.Websocket.Entity;
using ReviewAnalysis.Websocket.utils;
using Swan.Formatters;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Diagnostics.PerformanceData;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Runtime.InteropServices.ComTypes;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Timers;
using System.Web.UI.WebControls;
using System.Windows.Forms;
using static ReviewAnalysis.Websocket.Entity.WebsocketUtilsEntity;
using static System.Net.Mime.MediaTypeNames;

namespace ReviewAnalysis.Websocket
{
    public class WebsocketDataHandle
    {
        /// <summary>
        /// Websocket的存储的taskid
        /// </summary>
        private static string saveTaskId = null;

        /// <summary>
        /// 最大弹幕条数
        /// </summary>
        private static int maxListLength = 10000;

        /// <summary>
        /// Websocket采集数据的保存地址
        /// </summary>
        public static string savePath = Path.GetFullPath("dataCollect");

        private static System.Timers.Timer timeout = null;

        public static volatile object fileSaveLock = new object();  // 用于线程同步

        private static string[] fileNames = { "renshu", "guanzhu", "fensituan", "leijiguankanrenshu" };

        /// <summary>
        /// 记录从软件打开后一共获取了多少条Websocket记录
        /// </summary>
        private static long websocketNum = 0;

        /// <summary>
        /// 用于保护list并发访问的锁对象
        /// </summary>
        private static readonly object listLock = new object();

        /// <summary>
        /// websocket写入的全部数据
        /// </summary>
        public static volatile ConcurrentQueue<WebsocketDataEntity> dataQueue = new ConcurrentQueue<WebsocketDataEntity>();

        /// <summary>
        /// 要写到文件中的数据
        /// </summary>
        private static volatile List<WebsocketDataEntity> list = new List<WebsocketDataEntity>();

        // 添加一个静态字典用于存储锁对象
        private static readonly ConcurrentDictionary<string, object> videoLocks = new ConcurrentDictionary<string, object>();

        public static void Init()
        {
            string uuid = Guid.NewGuid().ToString();
            saveTaskId = uuid;
            Task.Run(() =>
            {
                string taskId = uuid;
                while (true)
                {
                    // taskid不对跳出循环
                    if (taskId != saveTaskId) break;
                    // list的数据超出不在存入
                    if (list.Count > maxListLength) continue;
                    // 把队列的数据转入list中
                    convertToList();
                    // list的数据超出就存入到文件中
                    if (list.Count > maxListLength) SaveListToFile();
                    Thread.Sleep(3000);
                }
            });

            timeout = new System.Timers.Timer(60000);
            timeout.Elapsed += (object sender, ElapsedEventArgs e) => {
                SaveListToFile();
            };
            // 启动定时器
            timeout.Start();
        }

        public static void Push(string messageType, WebsocketEntity webData, WebsocketSaveEntity data)
        {
            try
            {
                string fileName = messageType == "danmu"
                    ? $"danmu-{webData.videoId}"
                    : messageType;
                WebsocketDataEntity en = new WebsocketDataEntity()
                {
                    url = webData.url,
                    secUid = webData.secUid,
                    batchNumber = webData.batchNumber,
                    videoId = webData.videoId,
                    pushDate = ServerTimeUtils.getCurrentTimeStr(),
                    fileSavePath = $"{savePath}\\{webData.batchNumber}-{ReplayHttpUtils.UserId}\\{fileName}.txt",
                    messageType = messageType,
                    data = data
                };
                dataQueue.Enqueue(en);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"把Websocket采集到的数据添加到队列中报错");
            }
        }

        public static void PushError(string messageType, WebsocketEntity webData, WebsocketSaveEntity data)
        {
            try
            {
                foreach (var item in fileNames)
                {
                    WebsocketDataEntity en = new WebsocketDataEntity()
                    {
                        url = webData.url,
                        secUid = webData.secUid,
                        batchNumber = webData.batchNumber,
                        videoId = webData.videoId,
                        pushDate = ServerTimeUtils.getCurrentTimeStr(),
                        fileSavePath = $"{savePath}\\{webData.batchNumber}-{ReplayHttpUtils.UserId}\\{item}.txt",
                        messageType = "error",
                        data = data
                    };
                    dataQueue.Enqueue(en);
                }

            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"把Websocket采集到的数据添加到队列中报错");
            }
        }

        private static void convertToList()
        {
            try
            {
                lock (listLock)
                {
                    while (dataQueue.TryDequeue(out WebsocketDataEntity result))
                    {
                        websocketNum++;
                        list.Add(result);
                        if (websocketNum % 200 == 0)
                        {
                            FileUtils.log("websocket又保存了200条数据");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"把队列的数据转入list中报错");
            }
        }


        public static void SaveListToFile()
        {
            lock (fileSaveLock)
            {
                try
                {
                    // 在listLock保护下快速取出数据并清空list，减少锁持有时间
                    List<IGrouping<string, WebsocketDataEntity>> groupList = null;
                    int dataCount = 0;
                    lock (listLock)
                    {
                        if (list.Count > 0)
                        {
                            dataCount = list.Count;
                            groupList = list.GroupBy(s => s.fileSavePath + "$" + s.messageType).ToList();
                            list.Clear();
                        }
                    }

                    if (groupList != null && groupList.Count > 0)
                    {
                        FileUtils.log($"{dataCount}条", "websocket数据准备写入文件");
                        string[] totalQuantity = { "fensituan", "guanzhu", "dianzan" };
                        string[] justContent = { "renshu", "leijiguankanrenshu", "error" };
                        string[] totalLimitationQuantity = { "danmu" };
                        string[] dataJsonAll = { };
                        foreach (IGrouping<string, WebsocketDataEntity> group in groupList)
                        {
                            string key = group.Key;
                            string[] temp = key.Split(new char[] { '$' }, StringSplitOptions.RemoveEmptyEntries);
                            string filePath = temp[0];
                            string type = temp[1];
                            FileUtils.FileCreate(filePath);

                            List<string> lines = null;
                            if (totalQuantity.Contains(type))
                            {
                                lines = TotalQuantity(filePath, group);
                            }
                            else if (justContent.Contains(type))
                            {
                                lines = JustContent(filePath, group);
                            }
                            else if (totalLimitationQuantity.Contains(type))
                            {

                                if (type == "danmu")
                                {
                                    WebsocketDataEntity tempObj = group.First();
                                    DanMu danmu = (DanMu)tempObj.data;
                                    lines = TotalLimitationQuantity(filePath, group, danmu.dataMaxNum);
                                }
                                else
                                {
                                    lines = TotalLimitationQuantity(filePath, group, -1);
                                }
                            }
                            else if (dataJsonAll.Contains(type))
                            {
                                lines = DataJsonAll(filePath, group);
                            }
                            if (lines != null && lines.Count > 0) File.AppendAllLines(filePath, lines, Encoding.UTF8);
                        }
                        FileUtils.log("websocket数据写入文件成功");
                        // 记录弹幕的数量
                        saveDanMuNum();
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex.Message}", $"websocket数据写入文件失败");
                }
            }
        }

        public static List<string> TotalQuantity(string filePath, IGrouping<string, WebsocketDataEntity> group)
        {
            try
            {
                int count = 0;
                string content = FileUtils.GetLastNonEmptyLine2(filePath);
                if (content != null)
                {
                    try
                    {
                        string[] temp1 = content.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                        if (temp1 != null && temp1.Length > 1)
                        {
                            count = int.Parse(temp1[1]);
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex.Message}", $"获取文件中的最后一条数据失败");
                    }
                }
                List<string> lines = new List<string>();
                foreach (WebsocketDataEntity item in group)
                {
                    count++;
                    lines.Add($"{item.pushDate}>>>{count}");
                }
                return lines;
            }
            catch(Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"获取要把保存的数据失败TotalQuantity");
            }
            return new List<string> { };
        }


        public static List<string> TotalLimitationQuantity(string filePath, IGrouping<string, WebsocketDataEntity> group, int maxNum)
        {
            try
            {
                int count = 0;
                string content = FileUtils.GetLastNonEmptyLine2(filePath);
                if (content != null)
                {
                    try
                    {
                        string[] temp1 = content.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                        if (temp1 != null && temp1.Length > 1)
                        {
                            count = int.Parse(temp1[temp1.Length - 1]);
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError($"{ex.Message}", $"获取文件中的最后一条数据失败");
                    }
                }
                List<string> lines = new List<string>();
                foreach (WebsocketDataEntity item in group)
                {
                    if (maxNum != -1 && maxNum <= count)
                    {
                        break;
                    }
                    count++;
                    string line = item.data.ToJsonString();
                    lines.Add($"{item.pushDate}>>>{line}>>>{count}");
                }
                return lines;
            }
            catch( Exception ex )
            {
                FileUtils.LogError($"{ex.Message}", $"获取要把保存的数据失败TotalLimitationQuantity");
            }
            return new List<string> { };
        }

        public static List<string> JustContent(string filePath, IGrouping<string, WebsocketDataEntity> group)
        {
            try
            {
                List<string> lines = new List<string>();
                foreach (WebsocketDataEntity item in group)
                {
                    string line = item?.data?.content;
                    lines.Add($"{item.pushDate}>>>{line}");
                }
                return lines;
            }
            catch ( Exception ex )
            {
                FileUtils.LogError($"{ex.Message}", $"获取要把保存的数据失败JustContent");
            }
            return new List<string> { };
        }

        public static List<string> DataJsonAll(string filePath, IGrouping<string, WebsocketDataEntity> group)
        {
            try
            {
                List<string> lines = new List<string>();
                foreach (WebsocketDataEntity item in group)
                {
                    string line = item.data.ToJsonString();
                    lines.Add($"{item.pushDate}>>>{line}");
                }
                return lines;
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"获取要把保存的数据失败DataJsonAll");
            }
            return new List<string> { };
        }

        public static void saveDanMuNum()
        {
            try
            {
                if (WebsocketConnection.websocketList.Count > 0)
                {
                    foreach (string key in WebsocketConnection.websocketList.Keys)
                    {
                        if (WebsocketConnection.websocketList.TryGetValue(key, out WebsocketEntity item))
                        {

                            String filePath = $"{savePath}\\{item.batchNumber}-{ReplayHttpUtils.UserId}\\danmu-{item.videoId}.txt";
                            int count = -1;
                            string content = FileUtils.GetLastNonEmptyLine2(filePath);
                            try
                            {
                                if (!string.IsNullOrEmpty(content))
                                {
                                    string[] temp1 = content.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                                    if (temp1 != null && temp1.Length > 1)
                                    {
                                        count = int.Parse(temp1[temp1.Length - 1]);
                                    }
                                }
                            }
                            catch (Exception ex)
                            {

                            }
                            item.danMuNum = count;
                        }
                    }
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"saveDanMuNum报错");
            }
        }


        /// <summary>
        /// 上传websocket统计的json数据
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="batchNumber"></param>
        /// <param name="videoStartTime"></param>
        /// <param name="videoEndTime"></param>
        /// <param name="secUid"></param>
        /// <param name="userId"></param>
        /// <param name="Pulling"></param>
        public static void UploadSocketData(string videoId, string batchNumber, string videoStartTime, string videoEndTime, string secUid, long userId, bool Pulling)
        {
            // 获取或创建videoId对应的锁对象
            var videoLock = videoLocks.GetOrAdd(videoId, _ => new object());
            
            lock (videoLock)
            {
                try
                {
                    // 写入文件
                    WebsocketDataHandle.Close();

                    List<String> danmuList = null;

                    // 查询websocket采集的数据是否存在服务器
                    if (!ReplayHttpUtils.SocketDataExist(batchNumber, userId, videoId))
                    //if (true)
                    {
                        // 判断当前文件夹是否存在
                        if (!Directory.Exists($"{savePath}\\{batchNumber}-{userId}"))
                        {
                            return;
                        }
                        // 不存在就向服务器发一份数据
                        string path = $"{savePath}\\{batchNumber}-{userId}\\{videoId}.txt";
                        // 判断本地是否生成json的统计
                        DataWebSocket data = null;
                        lock (WebsocketDataHandle.fileSaveLock)
                        {
                            try
                            {
                                data = WebsocketUtils.FileStrToJson(fileNames, videoId, batchNumber, videoStartTime, videoEndTime, secUid, userId);
                                File.WriteAllText(path, JsonConvert.SerializeObject(data), Encoding.UTF8);
                            }
                            catch (Exception e)
                            {
                                FileUtils.LogError($"{e.Message}", "Websocket数据统计成json出错");
                                throw new Exception("Websocket数据统计成json出错");
                            }
                        }
                        if (data == null) throw new Exception("Websocket数据统计成json出错");
                        // 上传cos
                        string webSocketFilePath = TencentCosUtils.uploadFile(path);

                        if (string.IsNullOrEmpty(webSocketFilePath))
                        {
                            FileUtils.LogError("上传cos出错");
                            throw new CustomException("上传cos出错");
                        }

                        // 获取弹幕数量
                        danmuList = WebsocketUtils.getDanMuData(batchNumber, userId, videoId, videoStartTime, videoEndTime);

                        // 保存累计观看人数
                        //TotalOnlineNumBll totalOnlineNumBll = new TotalOnlineNumBll();
                        //totalOnlineNumBll.SaveOrUpdate(secUid, batchNumber, data.totalOnlineNum, videoId, videoStartTime, videoEndTime);
                        //保存场观
                        //VideoViewershipNumBll videoViewershipNumBll = new VideoViewershipNumBll();
                        //videoViewershipNumBll.SaveOrUpdate(secUid, batchNumber, data.observationNum, danmuList.Count.ToString(), videoId, videoStartTime, videoEndTime);

                        if (webSocketFilePath != null)
                        {
                            var obj = new
                            {
                                userId,
                                secUid,
                                batchNumber,
                                videoId,
                                cosKey = webSocketFilePath,
                                startDate = videoStartTime,
                                endDate = videoEndTime,
                                data.totalOnlineNum,
                                data.observationNum,
                                totalBarrageNum = danmuList.Count,
                                onlineMaxNum = data.maxRenShu
                            };
                            if (!ReplayHttpUtils.UploadSocketData(JsonConvert.SerializeObject(obj)))
                            {
                                FileUtils.LogError("上传cos出错");
                                throw new CustomException("上传cos出错");
                            }
                        }
                    }

                    // 处理弹幕数据-如图服务器没有查询到弹幕就上传上去
                    if (!ReplayHttpUtils.existsBarrage(secUid, batchNumber, videoId))
                    //if (true)
                    {
                        if (danmuList == null)
                        {
                            danmuList = WebsocketUtils.getDanMuData(batchNumber, userId, videoId, videoStartTime, videoEndTime);
                        }
                        if (danmuList != null && danmuList.Count > 0)
                        {
                            string zipName = $"{Guid.NewGuid().ToString()}-danmu.zip";
                            string zipNamePath = $"{savePath}/{batchNumber}-{userId}/{zipName}";
                            FileUtils.SaveListToZip(danmuList, zipNamePath, "danmu.txt");

                            // 上传弹幕数据
                            ReplayHttpUtils.uploadDanMuData(zipNamePath, secUid, batchNumber, videoId);
                            // 删除弹幕的zip包
                            File.Delete(zipNamePath);
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"videoId={videoId},错误信息：{ex.Message}\n堆栈{ex.StackTrace}", "上传websocket采集的数据错误");
                    throw ex;
                }
            }
        }


        public static DataWebSocket GetWebsocketData(VideoEntity videoEntity)
        {
            try
            {
                string jsonPath = $"{WebsocketDataHandle.savePath}\\{videoEntity.batchNumber}-{videoEntity.userId}\\{videoEntity.videoId}.txt";
                if (!File.Exists(jsonPath))
                {
                    // 判断原视频数据文件是否存在，存在则直接拷贝
                    if(videoEntity.videoSliceType != null && (videoEntity.videoSliceType == 1 || videoEntity.videoSliceType == 2))
                    {
                        string parentVideoJsonPath = $"{WebsocketDataHandle.savePath}\\{videoEntity.batchNumber}-{videoEntity.userId}\\{videoEntity.videoSliceInfo.sourceParentId}.txt";
                        if(File.Exists(parentVideoJsonPath))
                        {
                            File.Copy(parentVideoJsonPath, jsonPath);
                        }
                        else
                        {
                            WebsocketUtils.downloadWebsocketData(videoEntity.batchNumber, videoEntity.userId, videoEntity.videoId);
                            File.Copy(parentVideoJsonPath, jsonPath);
                        }
                    }else
                    {
                        WebsocketUtils.downloadWebsocketData(videoEntity.batchNumber, videoEntity.userId, videoEntity.videoId);
                    }
                    
                }
                if (File.Exists(jsonPath))
                {
                    string jsonNum = File.ReadAllText(jsonPath, Encoding.UTF8);
                    return JsonConvert.DeserializeObject<DataWebSocket>(jsonNum);
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex.Message}", $"获取Websocket的json文件报错");
            }
            return null;
        }

        public static void Close()
        {
            if (dataQueue.Count > 0)
            {
                // 把队列中剩余的数据写到list中
                convertToList();
            }

            // 保存到文件中
            SaveListToFile();
        }
    }
}
