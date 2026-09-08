using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Qiniu.CDN;
using ReviewAnalysis.Ai.impl;
using ReviewAnalysis.Ai.Model;
using ReviewAnalysis.api;
using ReviewAnalysis.Dto;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Model;
using ReviewAnalysis.upload;
using ReviewAnalysis.Utils;
using ReviewAnalysis.vo;
using ReviewAnalysis.vo.barrage;
using ReviewAnalysis.vo.oceanEngineData;
using ReviewAnalysis.Websocket.Entity;
using Swan.Formatters;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Security.Cryptography.Xml;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.juliang
{
    public class JuliangDataHandle
    {
        /// <summary>
        /// 数据队列，用于存储采集的实时巨量数据
        /// </summary>
        public static volatile ConcurrentQueue<JuliangRealTimeDataQueueEntity> juliangRealTimeDataQueue = new ConcurrentQueue<JuliangRealTimeDataQueueEntity>();
        /// <summary>
        /// 存储视频ID对应的锁对象
        /// </summary>
        private static readonly ConcurrentDictionary<string, object> _videoLocks = new ConcurrentDictionary<string, object>();
        /// <summary>
        /// 存储上次写入实时数据的时间戳
        /// </summary>
        public static volatile ConcurrentDictionary<string, long> realTimeDataDict = new ConcurrentDictionary<string, long>();
        /// <summary>
        /// 写入实时数据的锁对象
        /// </summary>
        private static readonly object realTimeWriteLock = new object();

        /// <summary>
        /// 写入实时数据
        /// </summary>
        /// <param name="data"></param>
        public static void writeRealTimeData(DataCollectEventArgs data)
        {
            try
            {
                lock (realTimeWriteLock)
                {
                    // 是临时窗体 或 相比上次写入时间超过20秒 再写数据
                    bool isWtiteFlag = false;

                    if (data.juliangForm.isTemp)
                    {
                        isWtiteFlag = true;
                    }
                    else if (!realTimeDataDict.ContainsKey(data.videoId))
                    {
                        isWtiteFlag = true;
                    }
                    else
                    {
                        realTimeDataDict.TryGetValue(data.videoId, out long writeTime);
                        long currTime = ServerTimeUtils.getCurrentTime();
                        if (currTime - writeTime >= 20000)
                        {
                            isWtiteFlag = true;
                        }
                    }

                    if (isWtiteFlag)
                    {
                        JuliangRealTimeDataQueueEntity juliangRealTimeDataQueueEntity = new JuliangRealTimeDataQueueEntity();
                        juliangRealTimeDataQueueEntity.secUid = data.secUid;
                        juliangRealTimeDataQueueEntity.batchNumber = data.batchNumber;
                        juliangRealTimeDataQueueEntity.videoId = data.videoId;

                        // 设置实时数据
                        JuliangRealTimeDataEntity juliangRealTimeDataEntity = new JuliangRealTimeDataEntity();
                        if (!string.IsNullOrEmpty(data.data))
                        {
                            JObject jo = JObject.Parse(data.data);
                            // 检查是否包含data键（巨量数据格式）
                            if (jo.ContainsKey("data"))
                            {
                                var dataObj = jo["data"];
                                // 检查必要的字段是否存在
                                if (dataObj != null && dataObj["pay_cnt"] != null && dataObj["pay_cnt"]["value"] != null &&
                                    dataObj["gmv"] != null && dataObj["fans_club_ucnt"] != null && dataObj["fans_club_ucnt"]["value"] != null &&
                                    dataObj["incr_fans_cnt"] != null && dataObj["incr_fans_cnt"]["value"] != null &&
                                    dataObj["online_user_ucnt"] != null && dataObj["online_user_ucnt"]["value"] != null)
                                {
                                    juliangRealTimeDataEntity.payComboCnt = (int)dataObj["pay_cnt"]["value"];
                                    juliangRealTimeDataEntity.payAmt = (int)dataObj["gmv"];
                                    juliangRealTimeDataEntity.fansClubJoinUcnt = (int)dataObj["fans_club_ucnt"]["value"];
                                    juliangRealTimeDataEntity.followAnchorUcnt = (int)dataObj["incr_fans_cnt"]["value"];
                                    juliangRealTimeDataEntity.watchNum = (int)dataObj["online_user_ucnt"]["value"];
                                    
                                    // 解析千川数据字段
                                    if (jo["整体支付ROI"] != null)
                                    {
                                        decimal.TryParse(jo["整体支付ROI"].ToString(), out decimal roi);
                                        juliangRealTimeDataEntity.totalRoi = roi;
                                    }
                                    if (jo["千川消耗"] != null)
                                    {
                                        decimal.TryParse(jo["千川消耗"].ToString(), out decimal cost);
                                        juliangRealTimeDataEntity.qianchuanCost = cost;
                                    }
                                    if (jo["退款金额"] != null)
                                    {
                                        decimal.TryParse(jo["退款金额"].ToString(), out decimal refund);
                                        juliangRealTimeDataEntity.refundAmt = refund;
                                    }
                                    
                                    long time = ServerTimeUtils.getCurrentTime();
                                    juliangRealTimeDataEntity.gatherTimeStamp = time;
                                    juliangRealTimeDataEntity.gatherDateTime = ServerTimeUtils.getTimeStrByTime(time);

                                    juliangRealTimeDataQueueEntity.juliangRealTimeData = juliangRealTimeDataEntity;
                                    // 加入到队列等待消费写入磁盘
                                    JuliangDataHandle.pushRealTimeDataQueue(juliangRealTimeDataQueueEntity);
                                }
                                else
                                {
                                    FileUtils.LogRpa("数据格式不完整，缺少必要字段", "巨量实时数据处理");
                                }
                            }
                            else
                            {
                                // 千川数据格式，不处理实时数据
                                FileUtils.LogRpa("千川数据格式，跳过实时数据处理", "巨量实时数据处理");
                            }
                        }
                        
                    }
                }
                

            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"处理巨量实时数据发生异常===={JsonConvert.SerializeObject(data)}");
            }
        }

        /// <summary>
        /// 将实时数据添加到队列
        /// </summary>
        /// <param name="juliangRealTimeDataQueueEntity"></param>
        public static void pushRealTimeDataQueue(JuliangRealTimeDataQueueEntity juliangRealTimeDataQueueEntity)
        {
            juliangRealTimeDataQueue.Enqueue(juliangRealTimeDataQueueEntity);
        }

        /// <summary>
        /// 开启消费巨量实时数据队列
        /// </summary>
        public static void startConsumeQueue()
        {
            try
            {
                Task.Run(() =>
                {
                    while (true)
                    {
                        try
                        {
                            while (juliangRealTimeDataQueue.TryDequeue(out JuliangRealTimeDataQueueEntity juliangRealTimeDataQueueEntity))
                            {
                                try
                                {
                                    string filePath = getDataFilePath(juliangRealTimeDataQueueEntity.videoId, 0);

                                    // 创建目录（如果不存在）
                                    string directoryPath = Path.GetDirectoryName(filePath);
                                    if (!Directory.Exists(directoryPath))
                                    {
                                        Directory.CreateDirectory(directoryPath);
                                    }

                                    using (StreamWriter sw = File.AppendText(filePath))
                                    {
                                        sw.Write(JsonConvert.SerializeObject(juliangRealTimeDataQueueEntity.juliangRealTimeData)); // 追加内容 
                                        sw.WriteLine(); // 追加换行符
                                    }

                                    // 记录写入时间
                                    long writeTime = ServerTimeUtils.getCurrentTime();
                                    realTimeDataDict.AddOrUpdate(juliangRealTimeDataQueueEntity.videoId, writeTime, (oldKey, oldValue) => writeTime);
                                }
                                catch (Exception exc)
                                {
                                    FileUtils.LogRpa($"{exc}", $"消费单条巨量实时数据发生异常");
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogRpa($"{ex}", $"遍历消费巨量实时数据队列发生异常");
                        }

                        Thread.Sleep(5000);
                    }
                });
            }
            catch(Exception e)
            {
                FileUtils.LogRpa($"{e}", $"开启消费巨量实时数据队列发生异常");
            }
            
        }

        /// <summary>
        /// 写入流量结构数据
        /// </summary>
        /// <param name="data"></param>
        public static void writeFlowSourceData(DataCollectEventArgs data)
        {
            try
            {
                if (!string.IsNullOrEmpty(data.data))
                {
                    // 获取或创建视频ID对应的锁对象
                    var lockObj = _videoLocks.GetOrAdd(data.videoId, id => new object());

                    try
                    {
                        // 获取锁
                        if (Monitor.TryEnter(lockObj, TimeSpan.FromSeconds(180)))
                        {
                            // 获取本地存储的汇总数据对象
                            JuliangGatherDataEntity juliangGatherDataEntity = getLocalJuliangGatherDataObj(data.videoId);

                            // 解析JSON字符串
                            JObject jObj = JObject.Parse(data.data);
                            if (!(jObj["data"] is JObject dataObj))
                            {
                                FileUtils.LogRpa("流量结构数据中data字段为空或格式错误", "巨量流量结构");
                                return;
                            }
                            JArray flowSourceList = dataObj["groups"] as JArray;

                            if (flowSourceList != null && flowSourceList.Count > 0)
                            {
                                // 设置支付的流量结构
                                juliangGatherDataEntity.payFlowList = parseFlowSourceData(flowSourceList, 0);
                                // 设置观看的流量结构
                                juliangGatherDataEntity.watchFlowList = parseFlowSourceData(flowSourceList, 1);

                                // 将对象重新写回文件
                                writeJuliangGatherFile(juliangGatherDataEntity, data);
                            }
                        }
                    }
                    catch (Exception exc)
                    {
                        FileUtils.LogRpa($"{exc}", $"处理巨量流量结构数据发生异常===={JsonConvert.SerializeObject(data)}");
                    }
                    finally
                    {
                        // 释放锁
                        Monitor.Exit(lockObj);
                        _videoLocks.TryRemove(data.videoId, out _);
                    }
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"处理巨量流量结构数据发生异常===={JsonConvert.SerializeObject(data)}");
            }
            
        }

        /// <summary>
        /// 解析流量结构数据
        /// </summary>
        /// <param name="flowSourceList">流量信息列表</param>
        /// <param name="type">流量结构类型 0：支付  1：观看</param>
        /// <returns></returns>
        private static List<JuliangFlowSourceEntity> parseFlowSourceData(JArray flowSourceList, int type)
        {
            var result = new List<JuliangFlowSourceEntity>();

            if(flowSourceList != null && flowSourceList.Count > 0 )
            {

                string ratioKey = type == 0 ? "pay_ratio" : "watch_ratio";

                foreach (var group in flowSourceList)
                {
                    result.Add(parseNode(group, ratioKey));
                }
            }
            
            return result;
        }

        /// <summary>
        /// 递归解析单个流量结构节点
        /// </summary>
        /// <param name="node">当前节点</param>
        /// <param name="ratioKey">流量结构类型的比例key 支付：pay_ratio 观看：watch_ratio</param>
        /// <returns></returns>
        private static JuliangFlowSourceEntity parseNode(JToken node, string ratioKey)
        {
            var juliangFlowSourceEntity = new JuliangFlowSourceEntity
            {
                channelName = node.Value<string>("channel_name"),
                ratio = node[ratioKey]?["value"]?.Value<double>() ?? 0,
                subFlow = new List<JuliangFlowSourceEntity>()
            };

            // 递归解析sub_flow
            if (node["sub_flow"] is JArray subFlows && subFlows.Count > 0)
            {
                foreach (var sub in subFlows)
                {
                    juliangFlowSourceEntity.subFlow.Add(parseNode(sub, ratioKey));
                }
            }
            else
            {
                juliangFlowSourceEntity.subFlow = null; // 没有子节点时设为null
            }

            return juliangFlowSourceEntity;
        }

        /// <summary>
        /// 获取本地存储的汇总数据对象
        /// </summary>
        /// <param name="videoId"></param>
        private static JuliangGatherDataEntity getLocalJuliangGatherDataObj(string videoId)
        {
            JuliangGatherDataEntity juliangGatherDataEntity;

            string filePath = getDataFilePath(videoId, 1);
            if(File.Exists(filePath))
            {
                // 数据文件已存在，读取里面的数据
                string objJsonStr = null;
                
                // 尝试多次读取，避免文件被锁定的问题
                int maxAttempts = 5;
                int attempt = 0;
                bool success = false;
                
                while (attempt < maxAttempts && !success)
                {
                    try
                    {
                        // 使用File.ReadAllText，确保文件被正确关闭
                        objJsonStr = File.ReadAllText(filePath, Encoding.UTF8);
                        success = true;
                    }
                    catch (IOException ex)
                    {
                        attempt++;
                        if (attempt < maxAttempts)
                        {
                            // 等待一段时间后重试
                            Thread.Sleep(100);
                        }
                        else
                        {
                            FileUtils.LogRpa($"多次尝试读取文件失败: {ex.Message}", "文件读取错误");
                            // 如果读取失败，返回新对象
                            juliangGatherDataEntity = new JuliangGatherDataEntity();
                            juliangGatherDataEntity.isTakeProduct = 1;
                            return juliangGatherDataEntity;
                        }
                    }
                }
                
                if(!string.IsNullOrEmpty(objJsonStr))
                {
                    juliangGatherDataEntity = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(objJsonStr);
                    return juliangGatherDataEntity;
                }
            }
            else
            {
                // 创建目录（如果不存在）
                string directoryPath = Path.GetDirectoryName(filePath);
                if (!Directory.Exists(directoryPath))
                {
                    Directory.CreateDirectory(directoryPath);
                }
            }

            juliangGatherDataEntity = new JuliangGatherDataEntity();
            juliangGatherDataEntity.isTakeProduct = 1;
            return juliangGatherDataEntity;
        }

        /// <summary>
        /// 将数据对象写入到汇总数据文件
        /// </summary>
        /// <param name="juliangGatherDataEntity">数据对象</param>
        /// <param name="data">数据信息</param>
        private static void writeJuliangGatherFile(JuliangGatherDataEntity juliangGatherDataEntity, DataCollectEventArgs data)
        {
            juliangGatherDataEntity.videoId = data.videoId;
            juliangGatherDataEntity.secUid = data.secUid;
            juliangGatherDataEntity.batchNumber = data.batchNumber;

            string filePath = getDataFilePath(data.videoId, 1);
            // 创建目录（如果不存在）
            string directoryPath = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(directoryPath))
            {
                Directory.CreateDirectory(directoryPath);
            }

            // 转成json字符串写入到文件，使用安全的文件写入方式
            string jsonContent = JsonConvert.SerializeObject(juliangGatherDataEntity);
            
            // 尝试多次写入，避免文件被锁定的问题
            int maxAttempts = 5;
            int attempt = 0;
            bool success = false;
            
            while (attempt < maxAttempts && !success)
            {
                try
                {
                    // 使用File.WriteAllText的重载，确保文件被正确关闭
                    File.WriteAllText(filePath, jsonContent, Encoding.UTF8);
                    success = true;
                }
                catch (IOException ex)
                {
                    attempt++;
                    if (attempt < maxAttempts)
                    {
                        // 等待一段时间后重试
                        Thread.Sleep(100);
                    }
                    else
                    {
                        FileUtils.LogRpa($"多次尝试写入文件失败: {ex.Message}", "文件写入错误");
                        throw;
                    }
                }
            }
        }

        /// <summary>
        /// 巨量数据上传
        /// </summary>
        /// <param name="videoId"></param>
        public static bool dataUpload(string batchNumber, string videoId, string secUid)
        {
            try
            {
                // 判断巨量的下播数据是否存在
                string dataJson = null;
                string summaryFilePath = getDataFilePath(videoId, 1); // 汇总数据type应为1
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
                        FileUtils.LogRpa($"{ex.Message}, {ex.StackTrace}", "读取巨量汇总数据报错");
                    }
                }
                dataJson = dataJson == null ? null : dataJson.Trim();
                if (string.IsNullOrEmpty(dataJson))
                {
                    FileUtils.LogRpa($"汇总数据没有获取到 videoId = {videoId}");
                    return true;
                }

                JuliangGatherDataEntity juliangGatherDataEntity = null;
                try
                {
                    juliangGatherDataEntity = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(dataJson);
                }
                catch(Exception ex)
                {
                    FileUtils.LogRpa($"格式化json报错 Message = {ex.Message}， dataJson = {dataJson}", "格式化json报错");
                }
                if (juliangGatherDataEntity == null)
                {
                    FileUtils.LogRpa( "juliangGatherDataEntity == null");
                    return true;
                }

                // 判断巨量的实时数据是否存在
                string realFilePath = getDataFilePath(videoId, 0);
                string cosPath = null;
                if (File.Exists(realFilePath))
                {
                    // 获取文件的父路径
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
                            FileUtils.LogRpa($"videoId = {videoId}", "获取预上传url失败");
                        }
                    }
                    catch (Exception e)
                    {
                        FileUtils.LogRpa($"{e.Message}, {e.StackTrace}", "上传巨量实时数据报错");
                    }
                    finally
                    {
                        // 删除zip
                        File.Delete(zipNamePath);
                    }

                }

                if (cosPath == null && dataJson == null)
                {
                    FileUtils.log($"videoId = {videoId},cosPath和dataJson都是为null，没有巨量数据");
                    return true;
                }

                juliangGatherDataEntity.ossPath = cosPath;
                string id = OceanEngineDataApi.updateOceanEngine(juliangGatherDataEntity);

                if (!string.IsNullOrEmpty(id))
                {
                    return true;
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogRpa($"message = {ex.Message}, StackTrace = {ex.StackTrace}", "巨量数据上传报错");
            }
            return false;
        }

        /// <summary>
        /// 获取视频关联的本地巨量数据文件地址
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="type">类型 0：实时数据 1：汇总数据</param>
        /// <returns></returns>
        public static string getDataFilePath(string videoId, int type)
        {
            Directory.CreateDirectory($"dataCollect\\juliang\\{(type == 0 ? "realTime" : "finish")}");
            return Path.GetFullPath($"dataCollect\\juliang\\{(type == 0 ? "realTime" : "finish")}\\{videoId}.txt");
        }

        /// <summary>
        /// 写入用户画像数据
        /// </summary>
        /// <param name="data"></param>
        /// <param name="type">类型 0：下单用户的用户画像 1：观看用户的用户画像</param>
        public static void writeUserPortrait(DataCollectEventArgs data, int type)
        {
            try
            {
                if (!string.IsNullOrEmpty(data.data))
                {
                    // 获取或创建视频ID对应的锁对象
                    var lockObj = _videoLocks.GetOrAdd(data.videoId, id => new object());
                    bool lockAcquired = false;

                    try
                    {
                        // 获取锁
                        lockAcquired = Monitor.TryEnter(lockObj, TimeSpan.FromSeconds(180));
                        if (lockAcquired)
                        {
                            // 获取本地存储的汇总数据对象
                            JuliangGatherDataEntity juliangGatherDataEntity = getLocalJuliangGatherDataObj(data.videoId);

                            // 解析JSON字符串
                            JObject jObj = JObject.Parse(data.data);
                            // 封装成用户画像对象
                            JuliangUserPortraitEntity juliangUserPortraitEntity = parseUserPortrait(jObj);

                            // 防护：parseUserPortrait 返回 null 的情况
                            if (juliangUserPortraitEntity == null)
                            {
                                FileUtils.LogRpa("用户画像解析结果为null，跳过写入", "巨量用户画像");
                            }
                            else
                            {
                                // 只要至少有一个维度有数据就写入（不再要求三个维度同时非空）
                                bool hasData = (juliangUserPortraitEntity.agePortrait?.Count > 0)
                                            || (juliangUserPortraitEntity.genderPortrait?.Count > 0)
                                            || (juliangUserPortraitEntity.provincePortrait?.Count > 0);

                                if (hasData)
                                {
                                    if (type == 0)
                                    {
                                        juliangGatherDataEntity.payUserPortrait = juliangUserPortraitEntity;
                                    }
                                    else
                                    {
                                        juliangGatherDataEntity.watchUserPortrait = juliangUserPortraitEntity;
                                        juliangGatherDataEntity.isTakeProduct = 1;
                                    }
                                    writeJuliangGatherFile(juliangGatherDataEntity, data);
                                }
                            }

                        }
                    }
                    catch (Exception exc)
                    {
                        FileUtils.LogRpa($"{exc}", $"处理巨量用户画像数据发生异常===={JsonConvert.SerializeObject(data)}");
                    }
                    finally
                    {
                        // 只有在成功获取锁的情况下才释放锁
                        if (lockAcquired)
                        {
                            Monitor.Exit(lockObj);
                        }
                        // 不要立即移除锁对象，因为可能有其他线程正在等待
                        // _videoLocks.TryRemove(data.videoId, out _);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"处理巨量用户画像数据发生异常===={JsonConvert.SerializeObject(data)}");
            }
        }

        /// <summary>
        /// 封装成用户画像对象
        /// </summary>
        /// <param name="json">json对象</param>
        /// <returns></returns>
        private static JuliangUserPortraitEntity parseUserPortrait(JObject json)
        {

            if(json.ContainsKey("data") && json["data"] is JObject data)
            {
                return new JuliangUserPortraitEntity
                {
                    agePortrait = parseUserPortraitItemList(data["age_distribution"]),
                    genderPortrait = parseUserPortraitItemList(data["gender_distribution"]),
                    provincePortrait = parseUserPortraitItemList(data["province_distribution"])
                };
            }
            return null;
        }

        /// <summary>
        /// 解析封装用户画像列表
        /// </summary>
        /// <param name="arr">用户画像数据</param>
        /// <returns></returns>
        private static List<JuliangUserPortraitItemEntity> parseUserPortraitItemList(JToken arr)
        {
            List<JuliangUserPortraitItemEntity> list = new List<JuliangUserPortraitItemEntity>();
            if (arr is JArray array && array.Count > 0)
            {
                foreach (var item in array)
                {
                    list.Add(new JuliangUserPortraitItemEntity
                    {
                        label = item.Value<string>("index_display"),
                        value = item["value"]?["value"]?.Value<double>() ?? 0
                    });
                }
            }
            return list;
        }

        /// <summary>
        /// 写入基础版的汇总数据
        /// </summary>
        /// <param name="data"></param>
        public static void writeGatherDataBase(DataCollectEventArgs data)
        {
            try
            {
                if (!string.IsNullOrEmpty(data.data))
                {
                    // 获取或创建视频ID对应的锁对象
                    var lockObj = _videoLocks.GetOrAdd(data.videoId, id => new object());

                    try
                    {
                        // 获取锁
                        if (Monitor.TryEnter(lockObj, TimeSpan.FromSeconds(180)))
                        {
                            // 获取本地存储的汇总数据对象
                            JuliangGatherDataEntity juliangGatherDataEntity = getLocalJuliangGatherDataObj(data.videoId);

                            // 解析JSON字符串
                            JObject jObj = JObject.Parse(data.data);
                            // 解析基础版的汇总数据
                            parseGatherDataBase(juliangGatherDataEntity, jObj);
                            // 重新写回到文件
                            writeJuliangGatherFile(juliangGatherDataEntity, data);

                        }
                    }
                    catch (Exception exc)
                    {
                        FileUtils.LogRpa($"{exc}", $"处理巨量基础版的汇总数据发生异常===={JsonConvert.SerializeObject(data)}");
                    }
                    finally
                    {
                        // 释放锁
                        Monitor.Exit(lockObj);
                        _videoLocks.TryRemove(data.videoId, out _);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"处理巨量基础版的汇总数据发生异常===={JsonConvert.SerializeObject(data)}");
            }
        }

        /// <summary>
        /// 解析基础版的汇总数据
        /// </summary>
        /// <param name="juliangGatherDataEntity">汇总数据对象</param>
        /// <param name="jObj">基础版的json数据</param>
        private static void parseGatherDataBase(JuliangGatherDataEntity juliangGatherDataEntity, JObject jObj)
        {
            // 处理千川数据格式
            if (jObj.ContainsKey("整体支付ROI"))
            {
                // 解析整体支付ROI
                string roiValue = jObj["整体支付ROI"]?.Value<string>();
                if (!string.IsNullOrEmpty(roiValue) && roiValue != "-")
                {
                    if (double.TryParse(roiValue, out double roi))
                    {
                        juliangGatherDataEntity.roi = roi;
                    }
                }
                else
                {
                    juliangGatherDataEntity.roi =0;
                }
                
                // 解析千川消耗（投放金额）
                string costValue = jObj["千川消耗"]?.Value<string>();
                if (!string.IsNullOrEmpty(costValue) && costValue != "-")
                {
                    if (double.TryParse(costValue, out double cost))
                    {
                        juliangGatherDataEntity.launchRoiAmount = cost;
                    }
                }
                else
                {
                    juliangGatherDataEntity.launchRoiAmount = 0;
                }

                // 解析退款金额
                string refundValue = jObj["退款金额"]?.Value<string>();
                if (!string.IsNullOrEmpty(refundValue) && refundValue != "-")
                {
                    if (double.TryParse(refundValue, out double refund))
                    {
                        juliangGatherDataEntity.refundAmount = refund;
                    }
                }
                else
                {
                    juliangGatherDataEntity.refundAmount = 0;
                }
                
                // 解析整体消耗
                string overallCostRoiValue = jObj["整体消耗"]?.Value<string>();
                if (!string.IsNullOrEmpty(overallCostRoiValue) && overallCostRoiValue != "-")
                {
                    if (double.TryParse(overallCostRoiValue, out double overallCostRoi))
                    {
                        juliangGatherDataEntity.overallCostRoi = overallCostRoi;
                    }
                }
                else
                {
                    juliangGatherDataEntity.overallCostRoi = 0;
                }
                
                // 解析净成交ROI
                string netTransactionRoiValue = jObj["净成交ROI"]?.Value<string>();
                if (!string.IsNullOrEmpty(netTransactionRoiValue) && netTransactionRoiValue != "-")
                {
                    if (double.TryParse(netTransactionRoiValue, out double netTransactionRoi))
                    {
                        juliangGatherDataEntity.netTransactionRoi = netTransactionRoi;
                    }
                }
                else
                {
                    juliangGatherDataEntity.netTransactionRoi = 0;
                }
            }
            // 处理基础版数据格式
            if (jObj["data"] is JObject data)
            {

                // 基础字段：API返回了才覆盖（包括0值）
                var totalWatchNumToken = data?["online_user_ucnt"]?["value"];
                if (totalWatchNumToken != null)
                    juliangGatherDataEntity.totalWatchNum = totalWatchNumToken.Value<int>();

                var averageOnlineNumToken = data?["online_user_cnt"]?["value"];
                if (averageOnlineNumToken != null)
                {
                    int onlineNum = averageOnlineNumToken.Value<int>();
                    // 只有大于 0 时才覆盖，避免用 0 覆盖已有的有效值
                    if (onlineNum > 0)
                    {
                        juliangGatherDataEntity.averageOnlineNum = onlineNum;
                    }
                }

                var averageResidenceTimeToken = data?["avg_watch_duration"]?["value"];
                if (averageResidenceTimeToken != null)
                {
                    int residenceTime = averageResidenceTimeToken.Value<int>();
                    // 只有大于 0 时才覆盖，避免用 0 覆盖已有的有效值
                    if (residenceTime > 0)
                    {
                        juliangGatherDataEntity.averageResidenceTime = residenceTime;
                    }
                }

                var incrementFollowerCountToken = data?["incr_fans_cnt"]?["value"];
                if (incrementFollowerCountToken != null)
                    juliangGatherDataEntity.incrementFollowerCount = incrementFollowerCountToken.Value<int>();

                var volumeToken = data?["gmv"];
                if (volumeToken != null)
                    juliangGatherDataEntity.volume = volumeToken.Value<int>();

                var purchaseCountToken = data?["pay_cnt"]?["value"];
                if (purchaseCountToken != null)
                    juliangGatherDataEntity.purchaseCount = purchaseCountToken.Value<int>();

                var gpmToken = data?["gpm"]?["value"];
                if (gpmToken != null)
                    juliangGatherDataEntity.gpm = gpmToken.Value<int>();

                var showWatchCntRatioToken = data?["live_show_watch_cnt_ratio"]?["value"];
                if (showWatchCntRatioToken != null)
                    juliangGatherDataEntity.showWatchCntRatio = showWatchCntRatioToken.Value<int>();

                // 计算字段：有基础数据且分母不为0时才计算
                if (juliangGatherDataEntity.totalWatchNum.HasValue && juliangGatherDataEntity.totalWatchNum.Value > 0)
                {
                    // 转粉率 = 新增粉丝量 / 观看人数
                    if (juliangGatherDataEntity.incrementFollowerCount.HasValue)
                        juliangGatherDataEntity.convertFanRate = (double)juliangGatherDataEntity.incrementFollowerCount.Value / juliangGatherDataEntity.totalWatchNum.Value;

                    // uv价值 = 销售额(元) / 观看人数
                    if (juliangGatherDataEntity.volume.HasValue)
                        juliangGatherDataEntity.uvValue = (double)juliangGatherDataEntity.volume.Value / 100.0 / juliangGatherDataEntity.totalWatchNum.Value;

                    // 带货转化率 = 销量 / 观看人数
                    if (juliangGatherDataEntity.purchaseCount.HasValue)
                        juliangGatherDataEntity.goodsConvertRate = (double)juliangGatherDataEntity.purchaseCount.Value / juliangGatherDataEntity.totalWatchNum.Value;
                }

                // 客单价 = 销售额 / 销量
                if (juliangGatherDataEntity.volume.HasValue && juliangGatherDataEntity.purchaseCount.HasValue && juliangGatherDataEntity.purchaseCount.Value > 0)
                {
                    juliangGatherDataEntity.customerUnitPrice = (double)juliangGatherDataEntity.volume.Value / juliangGatherDataEntity.purchaseCount.Value;
                }
            }
        }

        /// <summary>
        /// 写入专业版的汇总数据
        /// </summary>
        /// <param name="data"></param>
        public static void writeGatherDataPro(DataCollectEventArgs data)
        {
            try
            {
                if (!string.IsNullOrEmpty(data.data))
                {
                    // 获取或创建视频ID对应的锁对象
                    var lockObj = _videoLocks.GetOrAdd(data.videoId, id => new object());

                    try
                    {
                        // 获取锁
                        if (Monitor.TryEnter(lockObj, TimeSpan.FromSeconds(180)))
                        {
                            // 获取本地存储的汇总数据对象
                            JuliangGatherDataEntity juliangGatherDataEntity = getLocalJuliangGatherDataObj(data.videoId);

                            // 解析JSON字符串
                            JObject jObj = JObject.Parse(data.data);

                            // 设置互动率
                            JArray dataList = (JArray)jObj?["data"]?["core_data"];
                            if(dataList != null && dataList.Count > 0)
                            {
                                foreach (var item in dataList)
                                {
                                    if (item["index_name"].Value<string>().Equals("watch_interact_ucnt_ratio"))
                                    {
                                        var interactionPercentToken = item["value"]?["value"];
                                        if (interactionPercentToken != null)
                                            juliangGatherDataEntity.interactionPercent = interactionPercentToken.Value<double>();
                                    }
                                    if (item["index_name"].Value<string>().Equals("live_show_watch_cnt_ratio"))
                                    {
                                        var showWatchCntRatioToken = item["value"]?["value"];
                                        if (showWatchCntRatioToken != null)
                                            juliangGatherDataEntity.showWatchCntRatio = showWatchCntRatioToken.Value<double>();
                                    }
                                    if (item["index_name"].Value<string>().Equals("stat_cost"))
                                    {
                                        var statCostToken = item["value"]?["value"];
                                        if (statCostToken != null)
                                            juliangGatherDataEntity.launchRoiAmount = statCostToken.Value<double>();
                                    }
                                    if (item["index_name"].Value<string>().Equals("real_refund_amt"))
                                    {
                                        var realRefundAmtToken = item["value"]?["value"];
                                        if (realRefundAmtToken != null)
                                            juliangGatherDataEntity.refundAmount = realRefundAmtToken.Value<double>();
                                    }
                                }
                            }

                            // 重新写回到文件
                            writeJuliangGatherFile(juliangGatherDataEntity, data);

                        }
                    }
                    catch (Exception exc)
                    {
                        FileUtils.LogRpa($"{exc}", $"处理巨量专业版的汇总数据发生异常===={JsonConvert.SerializeObject(data)}");
                    }
                    finally
                    {
                        // 释放锁
                        Monitor.Exit(lockObj);
                        _videoLocks.TryRemove(data.videoId, out _);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"处理巨量专业版的汇总数据发生异常===={JsonConvert.SerializeObject(data)}");
            }
        }
        
        /// <summary>
        /// 将授权标识写入文件
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="path"></param>
        public static void writeAuthFlag(string videoId, string path)
        {
            try
            {
                // 获取或创建视频ID对应的锁对象
                var lockObj = _videoLocks.GetOrAdd(videoId, id => new object());

                try
                {
                    // 获取锁
                    if (Monitor.TryEnter(lockObj, TimeSpan.FromSeconds(180)))
                    {
                        //File.WriteAllText($"{path}\\flag.txt", ServerTimeUtils.getCurrentTime().ToString());
                    }
                }
                catch (Exception exc)
                {
                    FileUtils.LogRpa($"{exc}", $"写入巨量授权标识文件发生异常===={videoId}===={path}");
                }
                finally
                {
                    // 释放锁
                    Monitor.Exit(lockObj);
                    _videoLocks.TryRemove(videoId, out _);
                }
            }
            catch(Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"写入巨量授权标识文件发生异常===={videoId}===={path}");
            }
        }

        /// <summary>
        /// 根据视频id获取巨量的实时数据
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static List<JuliangRealTimeDataEntity> getJuliangRealTimeList(string videoId)
        {
            // 判断巨量的实时数据是否存在
            string realFilePath = getDataFilePath(videoId, 0);
            // 不存在直接从oss下载
            if (!File.Exists(realFilePath))
            {
                // 下载
                downloadFile(videoId);
            }

            // 判断巨量的实时数据是否存在
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
                if(string.IsNullOrEmpty(line))
                {
                    continue;
                }
                // json转换判断
                JuliangRealTimeDataEntity item = null;
                try{
                    item = JsonConvert.DeserializeObject<JuliangRealTimeDataEntity>(line);
                }
                catch(Exception ex)
                {
                    FileUtils.LogRpa($"{ex}", $"解析巨量实时数据发生异常===={line}");
                }
                if(item != null)
                {
                    result.Add(item);
                }
            }

            return result;
        }

        /// <summary>
        /// 根据视频id获取巨量的汇总数据
        /// </summary>
        /// <param name="videoId"></param>
        /// <returns></returns>
        public static JuliangGatherDataEntity getJuliangFinishData(string videoId)
        {
            // 判断巨量的实时数据是否存在
            string finishPath = getDataFilePath(videoId, 1);
            // 不存在直接从oss下载
            if (!File.Exists(finishPath))
            {
                // 下载
                downloadFile(videoId);
            }

            // 判断巨量的实时数据是否存在
            if (!File.Exists(finishPath))
            {
                return null;
            }

            // 读取文件
            List<JuliangRealTimeDataEntity> result = new List<JuliangRealTimeDataEntity>();

            // 读取文件中的数据存到result中，每一行就是一个JuliangRealTimeDataEntity对象
            string lines = File.ReadAllText(finishPath);
            JuliangGatherDataEntity finish = null;
            if (!string.IsNullOrEmpty(lines)){
                finish = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(lines);
            }
            return finish;
        }

        /// <summary>
        /// 根据视频id下载对应的巨量数据
        /// </summary>
        /// <param name="videoId"></param>
        public static void downloadFile(string videoId)
        {
            try
            {
                string realFilePath = getDataFilePath(videoId, 0);// 实时数据
                string summaryFilePath = getDataFilePath(videoId, 1); // 汇总数据

                // 判断两个文件是否存在，如果都存在就不调用接口，不下载
                if (File.Exists(realFilePath) && File.Exists(summaryFilePath))
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
                if (!File.Exists(realFilePath))
                {
                    // 从oss下载
                    // oss下载下来的文件是zip格式
                    // 获取realFilePath文件的文件夹地址，拼接uuid.zip
                    string zipFilePath = Path.Combine(Path.GetDirectoryName(realFilePath), $"{Guid.NewGuid().ToString()}.zip");
                    try
                    {
                        if (OssUtils.DownloadFileAsync(juLinag.ossPath, zipFilePath))
                        {
                            // 解压文件
                            string unzipPath = Path.Combine(Path.GetDirectoryName(realFilePath), $"{Guid.NewGuid().ToString()}");
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
                                        File.Move(firstTxtFile, realFilePath);
                                    }
                                    catch (Exception ex)
                                    {
                                        FileUtils.LogRpa($"{ex}", $"移动巨量实时数据txt文件发生异常===={firstTxtFile} 到 {realFilePath}");
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                FileUtils.LogRpa($"{ex}", $"解压巨量实时数据zip文件发生异常===={zipFilePath}");
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
                                    FileUtils.LogRpa($"{ex}", $"删除解压目录发生异常===={unzipPath}");
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
                                FileUtils.LogRpa($"{ex}", $"删除zip文件发生异常===={zipFilePath}");
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"{ex}", $"下载或处理巨量实时数据zip文件发生异常===={zipFilePath}");
                    }
                }

                // 判断巨量的汇总数据是否存在
                if (!File.Exists(summaryFilePath))
                {
                    try
                    {
                        // 把juLinag.dataJson写入到文件
                        File.WriteAllText(summaryFilePath, juLinag.dataJson, Encoding.UTF8);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"{ex}", $"写入巨量汇总数据文件发生异常===={summaryFilePath}");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"{ex}", $"downloadFile方法发生异常，videoId={videoId}");
            }
        }

        /// <summary>
        /// 获取一段时间内方成交数量
        /// </summary>
        /// <param name="videoId"></param>
        /// <param name="audioaAlyses"></param>
        /// <param name="startTime"></param>
        /// <returns></returns>
        public static List<OceanEngineDataDto> getParagraphJuLiang(string videoId, List<AudioaAlysis> audioaAlyses, string startTime, string platformType)
        {
            List<AudioaAlysesStatisticsDto> list = new List<AudioaAlysesStatisticsDto>();
            long allStartTime = DateUtils.StringToTimestamp(startTime);
            int onlineNumIndex = 0;

            List<OceanEngineDataDto> result = new List<OceanEngineDataDto>();
            if(audioaAlyses == null || audioaAlyses.Count < 1)
            {
                return result;
            }

            for (int i = 0; i < audioaAlyses.Count; i++)
            {
                long startTempTime = 0, endTempTime = 0, startVideoTime = 0, endVideoTime = 0;
                AudioaAlysis item = audioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    if (res.items != null && res.items.Count > 0)
                    {
                        startTempTime = res.items[0]?.startTime ?? 0;
                        endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
                        if (i < audioaAlyses.Count - 1)
                        {
                            AudioaAlysis item2 = audioaAlyses[i + 1];
                            if (!string.IsNullOrEmpty(item2.DataJson))
                            {
                                var res2 = JsonConvert.DeserializeObject<dynamic>(item2.DataJson);
                                if (res2.items != null && res2.items.Count > 0)
                                {
                                    endTempTime = res2.items[0]?.startTime ?? 0;
                                }
                            }
                        }
                        startVideoTime = allStartTime + startTempTime;
                        endVideoTime = allStartTime + endTempTime;
                    }
                }
                list.Add(new AudioaAlysesStatisticsDto()
                {
                    startVideoTime = startVideoTime,
                    endVideoTime = endVideoTime,
                    textStart = ServerTimeUtils.getTimeStrByTime(startVideoTime)
                });
            }

            VideoSentence fileStream = new VideoSentence(null, null);
            Dictionary<long, JuliangStatisticsDto> juliang = fileStream.getJuliangData(platformType, videoId, list);
           
            if (juliang == null || juliang.Count == 0)
            {
                return result;
            }
            for(int i = 0; i < list.Count; i++)
            {
                AudioaAlysesStatisticsDto item = list[i];
                juliang.TryGetValue(item.startVideoTime, out JuliangStatisticsDto dto);
                int payComboCnt = 0;
                double salesCount = 0;
                int watchNum = 0;

                if (dto != null)
                {
                    payComboCnt = dto.rangePayComboCnt;
                    salesCount = dto.rangePayAmt / 100.0;
                    watchNum = dto.rangeWatchNum;
                }
                result.Add(new OceanEngineDataDto()
                {
                    date = item.textStart,
                    dateTime = item.startVideoTime + "",
                    payComboCnt = payComboCnt,
                    salesCount = salesCount,
                    watchNum = watchNum
                });
            }
            return result;
        }

        /// <summary>
        /// 获取段落级投放消耗和净成交ROI曲线数据
        /// </summary>
        public static (List<BarrageDoubleData> qianchuanCostList, List<BarrageDoubleData> netTransactionRoiList) getOceanEngineCurves(
            string videoId, List<AudioaAlysis> audioaAlyses, string startTime, string platformType)
        {
            List<BarrageDoubleData> qianchuanCostList = new List<BarrageDoubleData>();
            List<BarrageDoubleData> netTransactionRoiList = new List<BarrageDoubleData>();

            if (audioaAlyses == null || audioaAlyses.Count < 1)
            {
                return (qianchuanCostList, netTransactionRoiList);
            }

            long allStartTime = DateUtils.StringToTimestamp(startTime);
            List<AudioaAlysesStatisticsDto> list = new List<AudioaAlysesStatisticsDto>();

            for (int i = 0; i < audioaAlyses.Count; i++)
            {
                long startTempTime = 0, endTempTime = 0, startVideoTime = 0, endVideoTime = 0;
                AudioaAlysis item = audioaAlyses[i];
                if (!string.IsNullOrEmpty(item.DataJson))
                {
                    var res = JsonConvert.DeserializeObject<dynamic>(item.DataJson);
                    if (res.items != null && res.items.Count > 0)
                    {
                        startTempTime = res.items[0]?.startTime ?? 0;
                        endTempTime = res.items[res.items.Count - 1]?.endTime ?? 0;
                        if (i < audioaAlyses.Count - 1)
                        {
                            AudioaAlysis item2 = audioaAlyses[i + 1];
                            if (!string.IsNullOrEmpty(item2.DataJson))
                            {
                                var res2 = JsonConvert.DeserializeObject<dynamic>(item2.DataJson);
                                if (res2.items != null && res2.items.Count > 0)
                                {
                                    endTempTime = res2.items[0]?.startTime ?? 0;
                                }
                            }
                        }
                        startVideoTime = allStartTime + startTempTime;
                        endVideoTime = allStartTime + endTempTime;
                    }
                }
                list.Add(new AudioaAlysesStatisticsDto()
                {
                    startVideoTime = startVideoTime,
                    endVideoTime = endVideoTime,
                    textStart = ServerTimeUtils.getTimeStrByTime(startVideoTime)
                });
            }

            VideoSentence fileStream = new VideoSentence(null, null);
            Dictionary<long, JuliangStatisticsDto> juliang = fileStream.getJuliangData(platformType, videoId, list);

            if (juliang == null || juliang.Count == 0)
            {
                return (qianchuanCostList, netTransactionRoiList);
            }

            for (int i = 0; i < list.Count; i++)
            {
                AudioaAlysesStatisticsDto item = list[i];
                juliang.TryGetValue(item.startVideoTime, out JuliangStatisticsDto dto);

                double qianchuanCostYuan = 0;
                double netTransactionRoi = 0;

                if (dto != null)
                {
                    // 投放消耗：分 → 元
                    qianchuanCostYuan = Math.Round((double)(dto.rangeQianchuanCost / 100.0m), 2);

                    // 净成交ROI = (payAmt - refundAmt) / qianchuanCost，单位一致，直接除
                    decimal netAmt = dto.rangePayAmt - dto.rangeRefundAmt;
                    if (netAmt > 0 && dto.rangeQianchuanCost > 0)
                    {
                        netTransactionRoi = Math.Round((double)(netAmt / dto.rangeQianchuanCost), 2);
                    }
                }

                qianchuanCostList.Add(new BarrageDoubleData()
                {
                    date = item.textStart,
                    dateTime = item.startVideoTime + "",
                    value = qianchuanCostYuan
                });

                netTransactionRoiList.Add(new BarrageDoubleData()
                {
                    date = item.textStart,
                    dateTime = item.startVideoTime + "",
                    value = netTransactionRoi
                });
            }

            return (qianchuanCostList, netTransactionRoiList);
        }
    }
}
