using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.plugins.processors;
using ReviewAnalysis.plugins.adapters;
using ReviewAnalysis.plugins.collectors;
using ReviewAnalysis.plugins.models;
using ReviewAnalysis.plugins.upload;
using ReviewAnalysis.plugins.utils;
using ReviewAnalysis.plugins.constant;
using ReviewAnalysis.upload;
using ReviewAnalysis.api;
using ReviewAnalysis.Model;
using ReviewAnalysis.Asr;
using ReviewAnalysis.juliang;
using ReviewAnalysis.juliang.entity;
using ReviewAnalysis.Utils;
using douyin.Utils;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 主播实例
    /// 管理单个主播的所有平台定时器、数据缓存、数据处理和写入
    /// </summary>
    public class AnchorInstance : IDisposable
    {
        #region 字段

        private readonly string _secUid;
        public string _roomId { get; set; }
        public string _anchorUserId { get; set; }
        public string _videoId { get; set; }
        private readonly AnchorInfo _anchorInfo;
        private readonly List<Timer> _timers;
        private readonly Dictionary<string, IPlatformCollector> _collectors;
        private readonly Dictionary<string, IDataAdapter> _adapters;
        private bool _disposed;

        // 数据缓存：平台ID -> 统一数据模型列表
        private readonly Dictionary<string, List<UnifiedDataModel>> _unifiedModels;
        // 原始JSON缓存：平台ID -> 最近一次采集的原始JSON（用于旧格式兼容写入）
        private readonly Dictionary<string, string> _rawJsonCache;
        private readonly object _dataLock = new object();

        /// <summary>最近一次采集写入的 gatherDateTime，用于保证升序</summary>
        private DateTime _lastCollectTime = DateTime.MinValue;


        // 平台数据管理器和上传调度器
        private readonly PlatformDataManager _dataManager;
        private readonly PlatformUploadScheduler _uploadScheduler;

        #endregion

        #region 属性

        public string SecUid => _secUid;
        public string RoomId => _roomId;
        public string VideoId => _videoId;
        public AnchorInfo AnchorInfo => _anchorInfo;

        public void SetVideoId(string v)
        {
            _videoId = v;
        }

        #endregion

        #region 构造函数

        public AnchorInstance(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            _anchorInfo = anchorInfo;
            _secUid = anchorInfo?.SecUid;
            _anchorUserId = anchorInfo?.AnchorUserId;
            _roomId = roomId;
            _videoId = videoId;
            _timers = new List<Timer>();
            _collectors = new Dictionary<string, IPlatformCollector>();
            _adapters = new Dictionary<string, IDataAdapter>();
            
            // 统一模型缓存
            _unifiedModels = new Dictionary<string, List<UnifiedDataModel>>
            {
                { "juliang", new List<UnifiedDataModel>() },
                { "qianchuan", new List<UnifiedDataModel>() },
                { "enterprise", new List<UnifiedDataModel>() },
                { "life", new List<UnifiedDataModel>() },
                { "anchorLive", new List<UnifiedDataModel>() }
            };

            // 原始JSON缓存
            _rawJsonCache = new Dictionary<string, string>();
            
            // 初始化平台数据管理器和上传调度器
            _dataManager = new PlatformDataManager();
            _uploadScheduler = new PlatformUploadScheduler();
        }

        #endregion

        #region 启动/停止

        /// <summary>
        /// 启动所有平台采集
        /// </summary>
        public void Start()
        {
            FileUtils.LogRpa($"主播【{_anchorInfo?.AnchorName}】启动采集，roomId={_roomId}", "主播实例");

            // 注册采集器和适配器
            RegisterCollectors();
            RegisterAdapters();

            // 启动统一采集定时器（60秒间隔，采集后立即处理）
            StartCollectAllTimer(0, 60000);
        }

        /// <summary>
        /// 停止所有采集
        /// </summary>
        public void Stop()
        {
            FileUtils.LogRpa($"主播【{_anchorInfo?.AnchorName}】停止采集", "主播实例");
            Dispose();
        }

        #endregion

        #region 注册

        /// <summary>
        /// 注册采集器
        /// </summary>
        private void RegisterCollectors()
        {
            _collectors["juliang"] = new JuliangedCollector();
            _collectors["qianchuan"] = new QianchuanCollectorSimple();
            _collectors["enterprise"] = new EnterpriseCollectorSimple();
            _collectors["life"] = new LifeCollectorSimple();
            _collectors["anchorLive"] = new AnchorLiveCollectorSimple();
        }

        /// <summary>
        /// 注册适配器
        /// </summary>
        private void RegisterAdapters()
        {
            _adapters["juliang"] = new JuliangAdapter();
            _adapters["qianchuan"] = new QianchuanAdapter();
            _adapters["enterprise"] = new EnterpriseAdapter();
            _adapters["life"] = new LifeAdapter();
            _adapters["anchorLive"] = new AnchorLiveAdapter();
        }

        #endregion

        #region 采集定时器

        /// <summary>
        /// 启动统一采集定时器（同时采集所有平台）
        /// </summary>
        private void StartCollectAllTimer(int delayMs, int intervalMs)
        {
            var timer = new Timer(async _ => await CollectAllAsync(), null, delayMs, intervalMs);
            _timers.Add(timer);
        }

        /// <summary>
        /// 手动触发一次全平台数据采集，用于停止前拉取最后一轮数据
        /// </summary>
        /// <param name="videoEndTime">视频结束时间</param>
        public async Task CollectOnceAsync(string videoEndTime = null)
        {
            DateTime serverNow = DateTime.Parse(ServerTimeUtils.getCurrentTimeStr());

            // Step 1 — 10秒规则：视频结束时间在服务器时间之前且相差≤10秒，使用结束时间；否则使用当前时间
            DateTime candidate = serverNow;
            if (!string.IsNullOrEmpty(videoEndTime))
            {
                DateTime videoEnd = DateTime.Parse(videoEndTime);
                if (videoEnd <= serverNow && (serverNow - videoEnd).TotalSeconds <= 10)
                    candidate = videoEnd;
            }

            // Step 2 — 升序约束
            if (candidate < _lastCollectTime)
                candidate = serverNow;

            // 透传覆盖时间，避免共享字段竞态
            await CollectAllAsync(candidate.ToString("yyyy-MM-dd HH:mm:ss"));
        }

        /// <summary>
        /// 一次性采集并上传（自行注册采集器/适配器，不启动定时器，不写旧版巨量文件）
        /// </summary>
        public async Task CollectAndUploadOnceAsync()
        {
            FileUtils.LogRpa($"开始，secUid = {_secUid}", "拉取商品数据");
            RegisterCollectors();
            RegisterAdapters();
            await CollectAllAsync();
            FileUtils.LogRpa($"完成数据采集，准备上传，secUid = {_secUid}", "拉取商品数据");
            await _uploadScheduler.UploadNowAsync();
            FileUtils.LogRpa($"完成数据上传，secUid = {_secUid}", "拉取商品数据");
        }

        /// <summary>
        /// 采集所有平台数据（并行采集）
        /// </summary>
        /// <param name="finalCollectEndTime">停止前最后一轮采集的覆盖时间，正常定时采集时为 null</param>
        private async Task CollectAllAsync(string finalCollectEndTime = null)
        {
            try
            {
                FileUtils.LogRpa($"开始采集所有平台数据，主播【{_anchorInfo?.AnchorName}】，roomId={_roomId}", "主播实例");

                _lastCollectTime = DateTime.Parse(ServerTimeUtils.getCurrentTimeStr());

                var platforms = new[] { "juliang", "qianchuan", "enterprise", "life", "anchorLive" };
                var tasks = platforms.Select(p => CollectAsync(p)).ToArray();

                await Task.WhenAll(tasks);

                FileUtils.LogRpa($"所有平台采集完成，开始处理数据，主播【{_anchorInfo?.AnchorName}】", "主播实例");

                // 采集完成后立即处理数据
                ProcessCollectedData(finalCollectEndTime);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"采集所有平台异常: {ex.Message}，主播【{_anchorInfo?.AnchorName}】", "主播实例");
            }
        }

        /// <summary>
        /// 采集指定平台数据
        /// </summary>
        private async Task CollectAsync(string platformId)
        {
            try
            {
                if (_collectors.TryGetValue(platformId, out var collector))
                {
                    // 先检查是否有采集条件（授权状态 + Cookie有效性）
                    if (!collector.CanCollect(_anchorInfo))
                    {
                        return;
                    }

                    var json = await collector.CollectAsync(_anchorInfo, _roomId, _videoId);
                    if (string.IsNullOrEmpty(json))
                    {
                        FileUtils.LogRpa($"采集{platformId}返回空数据，主播【{_anchorInfo?.AnchorName}】", "主播实例");
                        return;
                    }

                    if (!_adapters.TryGetValue(platformId, out var adapter))
                    {
                        FileUtils.LogRpa($"采集{platformId}无适配器，跳过数据转换，主播【{_anchorInfo?.AnchorName}】", "主播实例");
                        return;
                    }

                    var model = adapter.Adapt(json, _secUid, _roomId, _videoId);
                    if (model == null)
                    {
                        FileUtils.LogRpa($"采集{platformId}数据适配失败，原始JSON长度={json.Length}，主播【{_anchorInfo?.AnchorName}】", "主播实例");
                        return;
                    }

                    lock (_dataLock)
                    {
                        _unifiedModels[platformId].Add(model);
                        _rawJsonCache[platformId] = json;
                    }
                    FileUtils.LogRpa($"采集{platformId}数据成功，主播【{_anchorInfo?.AnchorName}】，缓存数量={_unifiedModels[platformId].Count}", "主播实例");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"采集{platformId}数据异常: {ex.Message}，主播【{_anchorInfo?.AnchorName}】", "主播实例");
            }
        }

        #endregion

        #region 数据处理

        /// <summary>
        /// 处理缓存的采集数据
        /// </summary>
        /// <param name="finalCollectEndTime">停止前最后一轮采集的覆盖时间，正常定时采集时为 null</param>
        private void ProcessCollectedData(string finalCollectEndTime = null)
        {
            try
            {
                FileUtils.LogRpa($"开始批量存储统一模型数据，主播【{_anchorInfo?.AnchorName}】", "主播实例");

                // 处理巨量+千川数据（合并存储）
                ProcessJuliangQianchuanCache(finalCollectEndTime);

                FileUtils.LogRpa($"批量存储完成，主播【{_anchorInfo?.AnchorName}】", "主播实例");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"批量存储异常: {ex.Message}", "主播实例");
            }
        }

        /// <summary>
        /// 处理巨量+千川缓存数据（合并存储）
        /// </summary>
        /// <param name="finalCollectEndTime">停止前最后一轮采集的覆盖时间，正常定时采集时为 null</param>
        private void ProcessJuliangQianchuanCache(string finalCollectEndTime = null)
        {
            lock (_dataLock)
            {   
                var platforms = new[] { "juliang", "qianchuan", "enterprise", "life", "anchorLive" };
                bool flag = false;
                List<UnifiedDataModel> models = platforms.Select(item =>
                    {
                        var unifiedDataModels = _unifiedModels[item];
                        if (unifiedDataModels == null || unifiedDataModels.Count == 0)
                        {
                            return null;
                        }

                        if (item == "juliang" || item == "qianchuan" || item == "life")
                        {
                            flag =  true;
                        }
                        return unifiedDataModels[unifiedDataModels.Count - 1];
                    })
                    .Where(item => item != null)
                    .ToList();

                // 合并数据（千川可能为null）
                var mergedModel = DataMerger.MergeAll(models);
                
                FileUtils.LogRpa($"合并数据完成，主播【{_anchorInfo?.AnchorName}】， mergedModel = {mergedModel != null}, videoId = {_videoId}, _roomId = {_roomId}", "主播实例");
                // 情况1：巨量数据存在，合并千川后保存（巨量为主）
                if (mergedModel != null)
                {
                    // 使用 PlatformDataManager 保存（自动合并过程数据，文件以 data 开头）
                    _dataManager.SavePlatformData("data", _secUid, _roomId, mergedModel);

                    // 有 巨量 和 videoId 时，同时写入旧格式文件（兼容 JuliangDataHandle.dataUpload）
                    if (flag && !string.IsNullOrEmpty(_videoId))
                    {
                        // 统一用新模型写入finish数值字段
                        WriteJuliangLegacyFinish(mergedModel);

                        // 处理人群画像和流量结构（从原始JSON中提取）
                        if (_rawJsonCache.TryGetValue("juliang", out var rawJson) && !string.IsNullOrEmpty(rawJson))
                        {
                            var mergedJo = JObject.Parse(rawJson);
                            var dataArgs = new DataCollectEventArgs
                            {
                                videoId = _videoId,
                                secUid = _secUid,
                                batchNumber = _roomId,
                                anchorName = _anchorInfo?.AnchorName
                            };

                            // 成交用户画像
                            var payUserPortrait = mergedJo["data"]?["payUserPortrait"]?.ToString();
                            if (!string.IsNullOrEmpty(payUserPortrait))
                            {
                                dataArgs.data = payUserPortrait;
                                JuliangDataHandle.writeUserPortrait(dataArgs, 0);
                            }

                            // 观看用户画像
                            var watchUserPortrait = mergedJo["data"]?["watchUserPortrait"]?.ToString();
                            if (!string.IsNullOrEmpty(watchUserPortrait))
                            {
                                dataArgs.data = watchUserPortrait;
                                JuliangDataHandle.writeUserPortrait(dataArgs, 1);
                            }

                            // 流量来源
                            var trafficSource = mergedJo["data"]?["trafficSource"]?.ToString();
                            if (!string.IsNullOrEmpty(trafficSource))
                            {
                                dataArgs.data = trafficSource;
                                JuliangDataHandle.writeFlowSourceData(dataArgs);
                            }
                        }

                        // 保留realTime追加
                        AppendJuliangLegacyRealTime(mergedModel, finalCollectEndTime);
                    }
                }
                

                // 清空已处理的缓存
                foreach (var platform in platforms)
                {
                    _unifiedModels[platform]?.Clear();
                }
            }
        }

        #endregion

        #region 写入方法

        /// <summary>
        /// 写入巨量旧格式 finish 汇总数据文件（兼容 JuliangDataHandle.dataUpload）
        /// </summary>
        private void WriteJuliangLegacyFinish(UnifiedDataModel model)
        {
            WriteJuliangLegacyFinishStatic(model, _videoId, _secUid, _roomId, _anchorInfo?.AnchorName);
        }

        /// <summary>
        /// 写入巨量旧格式 finish 汇总数据文件（静态版本，供外部无窗体调用）
        /// </summary>
        public static void WriteJuliangLegacyFinishStatic(UnifiedDataModel model, string videoId, string secUid, string roomId, string anchorName = null)
        {
            try
            {
                string filePath = JuliangDataHandle.getDataFilePath(videoId, 1);

                // 读取已有的汇总数据（合并）
                JuliangGatherDataEntity gatherData;
                if (File.Exists(filePath))
                {
                    try
                    {
                        string json = File.ReadAllText(filePath, Encoding.UTF8);
                        gatherData = JsonConvert.DeserializeObject<JuliangGatherDataEntity>(json)
                                     ?? new JuliangGatherDataEntity { isTakeProduct = 1 };
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"读取旧格式finish文件异常，创建新对象: {ex.Message}", "主播实例");
                        gatherData = new JuliangGatherDataEntity { isTakeProduct = 1 };
                    }
                }
                else
                {
                    gatherData = new JuliangGatherDataEntity { isTakeProduct = 1 };
                    // 创建目录
                    string dirPath = Path.GetDirectoryName(filePath);
                    if (!Directory.Exists(dirPath))
                    {
                        Directory.CreateDirectory(dirPath);
                    }
                }

                // 映射字段（有值才覆盖，避免将已有值刷成null）
                gatherData.videoId = videoId;
                gatherData.secUid = model.secUid ?? secUid;
                gatherData.batchNumber = model.batchNumber ?? roomId;

                // 基础字段：模型有值才覆盖
                if (model.viewCount.HasValue)
                    gatherData.totalWatchNum = (int?)model.viewCount.Value;

                if (model.salesRevenue.HasValue)
                    gatherData.volume = (int?)(model.salesRevenue.Value * 100);

                if (model.refund.HasValue)
                    gatherData.refundAmount = Math.Round(model.refund.Value * 100, 2);

                if (model.investment.HasValue)
                    gatherData.launchRoiAmount = model.investment.Value * 100;

                if (model.payComboCnt.HasValue)
                    gatherData.purchaseCount = model.payComboCnt.Value;

                if (model.followCount.HasValue)
                    gatherData.incrementFollowerCount = model.followCount.Value;

                if (model.averageOnlineNum.HasValue && model.averageOnlineNum.Value > 0)
                    gatherData.averageOnlineNum = model.averageOnlineNum.Value;

                if (model.averageResidenceTime.HasValue && model.averageResidenceTime.Value > 0)
                    gatherData.averageResidenceTime = model.averageResidenceTime.Value;

                if (model.overallCostRoi.HasValue)
                    gatherData.overallCostRoi = model.overallCostRoi.Value;

                if (model.netTransactionRoi.HasValue)
                    gatherData.netTransactionRoi = model.netTransactionRoi.Value;

                // 比率字段（模型中是百分比，需/100转小数）
                if (model.conversionRate.HasValue)
                    gatherData.goodsConvertRate = model.conversionRate.Value / 100.0;

                if (model.uvValue.HasValue)
                    gatherData.uvValue = model.uvValue.Value;

                if (model.roi.HasValue)
                    gatherData.roi = model.roi.Value;

                // 百分比转小数的字段
                if (model.followRate.HasValue)
                    gatherData.convertFanRate = model.followRate.Value / 100.0;

                if (model.thousandSales.HasValue)
                    gatherData.gpm = model.thousandSales.Value * 100;

                if (model.interactionRate.HasValue)
                    gatherData.interactionPercent = model.interactionRate.Value / 100.0;

                if (model.showWatchCntRatio.HasValue)
                    gatherData.showWatchCntRatio = model.showWatchCntRatio.Value / 100.0;

                // 客单价（有销售额和销量才计算）
                if (model.salesRevenue.HasValue && model.payComboCnt.HasValue && model.payComboCnt.Value > 0)
                    gatherData.customerUnitPrice = Math.Round(model.salesRevenue.Value * 100 / model.payComboCnt.Value, 2);

                // 写入文件
                File.WriteAllText(filePath, JsonConvert.SerializeObject(gatherData), Encoding.UTF8);
                FileUtils.LogRpa($"巨量旧格式finish已写入，主播【{anchorName}】: {filePath}", "主播实例");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"写入巨量旧格式finish文件异常，主播【{anchorName}】: {ex.Message}", "主播实例");
            }
        }

        /// <summary>
        /// 使用统一模型解析原始JSON写入finish文件（静态版本，供外部无窗体调用）
        /// </summary>
        public static void WriteJuliangLegacyByOldLogicStatic(string rawJson, string videoId, string secUid, string roomId, string anchorName)
        {
            try
            {
                // 使用 JuliangAdapter 将原始JSON转为统一模型
                var adapter = new adapters.JuliangAdapter();
                var model = adapter.Adapt(rawJson, secUid, roomId, videoId);

                // 统一用新模型写入finish
                WriteJuliangLegacyFinishStatic(model, videoId, secUid, roomId, anchorName);

                // 用户画像和流量来源仍需旧方法处理（后续可继续迁移）
                var mergedJo = JObject.Parse(rawJson);
                var dataArgs = new DataCollectEventArgs
                {
                    videoId = videoId,
                    secUid = secUid,
                    batchNumber = roomId,
                    anchorName = anchorName
                };

                var payUserPortrait = mergedJo["data"]?["payUserPortrait"]?.ToString();
                if (!string.IsNullOrEmpty(payUserPortrait))
                {
                    dataArgs.data = payUserPortrait;
                    JuliangDataHandle.writeUserPortrait(dataArgs, 0);
                }

                var watchUserPortrait = mergedJo["data"]?["watchUserPortrait"]?.ToString();
                if (!string.IsNullOrEmpty(watchUserPortrait))
                {
                    dataArgs.data = watchUserPortrait;
                    JuliangDataHandle.writeUserPortrait(dataArgs, 1);
                }

                var trafficSource = mergedJo["data"]?["trafficSource"]?.ToString();
                if (!string.IsNullOrEmpty(trafficSource))
                {
                    dataArgs.data = trafficSource;
                    JuliangDataHandle.writeFlowSourceData(dataArgs);
                }

                FileUtils.LogRpa("巨量旧格式finish已通过统一模型写入", "主播实例");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"统一模型写入巨量finish异常: {ex.Message}", "主播实例");
            }
        }

        /// <summary>
        /// 追加巨量旧格式 realTime 实时数据文件（兼容 JuliangDataHandle.dataUpload）
        /// </summary>
        private void AppendJuliangLegacyRealTime(UnifiedDataModel model, string finalCollectEndTime = null)
        {
            AppendJuliangLegacyRealTimeStatic(model, _videoId, finalCollectEndTime);
        }

        /// <summary>
        /// 追加巨量旧格式 realTime 实时数据文件（静态版本，供外部无窗体调用）
        /// </summary>
        /// <param name="model">统一数据模型</param>
        /// <param name="videoId">视频ID</param>
        /// <param name="finalCollectEndTime">停止前最后一轮采集覆盖时间，正常采集时为 null</param>
        public static void AppendJuliangLegacyRealTimeStatic(UnifiedDataModel model, string videoId, string finalCollectEndTime = null)
        {
            try
            {
                if (model.oceanEngineProcessList == null || model.oceanEngineProcessList.Count == 0)
                    return;

                string filePath = JuliangDataHandle.getDataFilePath(videoId, 0);
                string dirPath = Path.GetDirectoryName(filePath);
                if (!Directory.Exists(dirPath))
                {
                    Directory.CreateDirectory(dirPath);
                }

                // 取最新一条过程数据
                var latest = model.oceanEngineProcessList.Last();

                // 记录关键字段，便于排查本段数据为0的问题
                FileUtils.LogRpa($"准备写入realTime: viewCount={latest.viewCount ?? model.viewCount}, salesRevenue={latest.salesRevenue ?? model.salesRevenue}, payComboCnt={latest.payComboCnt ?? model.payComboCnt}", "主播实例");

                var realTimeEntity = new JuliangRealTimeDataEntity
                {
                    watchNum = (int?)(latest.viewCount ?? model.viewCount) ?? 0,
                    payAmt = (int?)((latest.salesRevenue ?? model.salesRevenue) * 100) ?? 0,
                    payComboCnt = latest.payComboCnt ?? model.payComboCnt ?? 0,
                    fansClubJoinUcnt = latest.fansClubJoinUcnt ?? 0,
                    followAnchorUcnt = latest.followCount ?? model.followCount ?? 0,
                    qianchuanCost = (decimal)Math.Round((latest.investment ?? model.investment ?? 0) * 100, 2),
                    refundAmt = (decimal)Math.Round((latest.refund ?? model.refund ?? 0) * 100, 2),
                    totalRoi = (decimal)(model.roi ?? 0),
                    gatherDateTime = !string.IsNullOrEmpty(finalCollectEndTime)
                        ? finalCollectEndTime
                        : (latest.gatherDateTime ?? ServerTimeUtils.getCurrentTimeStr()),
                    gatherTimeStamp = !string.IsNullOrEmpty(finalCollectEndTime)
                        ? new DateTimeOffset(DateTime.Parse(finalCollectEndTime), TimeSpan.FromHours(8)).ToUnixTimeMilliseconds()
                        : ServerTimeUtils.getCurrentTime()
                };

                // 追加一行 JSON
                File.AppendAllText(filePath, JsonConvert.SerializeObject(realTimeEntity) + Environment.NewLine, Encoding.UTF8);
                FileUtils.LogRpa($"巨量旧格式realTime已追加: {filePath}, watchNum={realTimeEntity.watchNum}, payAmt={realTimeEntity.payAmt}, payComboCnt={realTimeEntity.payComboCnt}", "主播实例");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"追加巨量旧格式realTime文件异常: {ex.Message}", "主播实例");
            }
        }

        #endregion

        #region IDisposable

        public void Dispose()
        {
            if (_disposed) return;

            // 停止所有定时器
            foreach (var timer in _timers)
            {
                timer?.Dispose();
            }
            _timers.Clear();

            // flush 剩余缓存数据
            try
            {
                ProcessCollectedData();
                FileUtils.LogRpa("采集停止前已flush剩余缓存数据", "主播实例");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"flush剩余缓存数据异常: {ex.Message}", "主播实例");
            }

            // 上传未上传数据并等待完成
            try
            {
                if (_uploadScheduler != null)
                {
                    _uploadScheduler.UploadNowAsync().GetAwaiter().GetResult();
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"关闭时上传数据异常: {ex.Message}", "主播实例");
            }

            // 释放上传调度器
            _uploadScheduler?.Dispose();

            _disposed = true;
        }

        #endregion

        #region 统一采集上传

        /// <summary>
        /// 统一数据采集上传：采集所有平台数据（巨量/千川/来客/企业/抖音直播），
        /// 上传到 fupan-governance-server（企业后台）。若提供 videoId，同时上传到 fupan-server（爱复盘后台）。
        /// </summary>
        /// <param name="anchorInfo">主播信息（用于 Cookie/授权）</param>
        /// <param name="batchNumber">直播批次号，同时作为 roomId</param>
        /// <param name="videoId">视频唯一标识（可选，为空时跳过 fupan-server 上传）</param>
        /// <returns>采集和上传任务</returns>
        public static async Task CollectAndUploadAsync(AnchorInfo anchorInfo, string batchNumber, string videoId)
        {
            // 参数校验
            if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
            {
                FileUtils.LogRpa("CollectAndUploadAsync: anchorInfo或secUid为空", "CollectAndUploadAsync");
                return;
            }
            if (string.IsNullOrEmpty(batchNumber))
            {
                FileUtils.LogRpa("CollectAndUploadAsync: batchNumber为空", "CollectAndUploadAsync");
                return;
            }

            // videoId 为空时跳过 fupan-server 上传，governance 上传照常执行
            bool hasVideoId = !string.IsNullOrEmpty(videoId);

            try
            {
                FileUtils.LogRpa(
                    $"开始统一采集上传，主播={anchorInfo.AnchorName}, secUid={anchorInfo.SecUid}, batchNumber={batchNumber}, videoId={videoId}, fupanUpload={hasVideoId}",
                    "CollectAndUploadAsync");

                using (var instance = new AnchorInstance(anchorInfo, batchNumber, videoId))
                {
                    // Step 1: 注册采集器和适配器
                    instance.RegisterCollectors();
                    instance.RegisterAdapters();

                    // Step 2: 全平台数据采集（并行采集 + 数据合并 + 写本地文件）
                    await instance.CollectAllAsync();
                    FileUtils.LogRpa($"全平台采集完成，secUid={anchorInfo.SecUid}", "CollectAndUploadAsync");

                    // Step 3: 有 videoId 时，提前读取平台数据文件用于 fupan-server 上传
                    // 必须在 UploadNowAsync 之前读取，因为后者成功后会归档/删除文件
                    UnifiedDataModel platformModel = null;
                    if (hasVideoId)
                    {
                        platformModel = ReadLatestPlatformModel(anchorInfo.SecUid, batchNumber);
                    }

                    // Step 4: 上传到 fupan-governance-server（企业后台）— 全平台 UnifiedDataModel
                    try
                    {
                        await instance._uploadScheduler.UploadNowAsync();
                        FileUtils.LogRpa($"Governance上传完成，secUid={anchorInfo.SecUid}", "CollectAndUploadAsync");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"Governance上传失败: {ex.Message}", "CollectAndUploadAsync");
                    }

                    // Step 5: 上传到 fupan-server（爱复盘后台）— 仅在有 videoId 时执行
                    if (hasVideoId)
                    {
                        if (platformModel != null)
                        {
                            await UploadToFupanServerAsync(platformModel, videoId, batchNumber, anchorInfo.SecUid);
                        }
                        else
                        {
                            // 平台文件不存在时，尝试读取旧格式 finish 文件（巨量/千川/来客场景兜底）
                            string finishFilePath = JuliangDataHandle.getDataFilePath(videoId, 1);
                            if (File.Exists(finishFilePath))
                            {
                                FileUtils.LogRpa("平台文件未找到，使用finish文件兜底上传", "CollectAndUploadAsync");
                                JuliangDataHandle.dataUpload(batchNumber, videoId, anchorInfo.SecUid);
                            }
                            else
                            {
                                FileUtils.LogRpa("未找到任何数据文件，跳过fupan-server上传", "CollectAndUploadAsync");
                            }
                        }
                    }
                }

                FileUtils.LogRpa($"统一采集上传完成，secUid={anchorInfo.SecUid}", "CollectAndUploadAsync");
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"统一采集上传异常: {ex.Message}\n{ex.StackTrace}", "CollectAndUploadAsync");
            }
        }

        /// <summary>
        /// 读取最新的平台数据文件（UnifiedDataModel）
        /// </summary>
        /// <param name="secUid">主播 secUid</param>
        /// <param name="batchNumber">直播批次号（roomId）</param>
        /// <returns>UnifiedDataModel，未找到返回 null</returns>
        private static UnifiedDataModel ReadLatestPlatformModel(string secUid, string batchNumber)
        {
            try
            {
                string platformDir = Path.Combine(Environment.CurrentDirectory, PathUtils.PlatformPath);
                if (!Directory.Exists(platformDir))
                {
                    FileUtils.LogRpa($"平台目录不存在: {platformDir}", "CollectAndUploadAsync");
                    return null;
                }

                string searchPattern = $"{PlatformConstants.DataPrefix}_{secUid}_{batchNumber}_*.json";
                var files = Directory.GetFiles(platformDir, searchPattern);
                var latestFile = files.OrderByDescending(f => File.GetLastWriteTime(f)).FirstOrDefault();

                if (string.IsNullOrEmpty(latestFile))
                {
                    FileUtils.LogRpa($"未找到平台文件，pattern={searchPattern}", "CollectAndUploadAsync");
                    return null;
                }

                string json = File.ReadAllText(latestFile, Encoding.UTF8);
                if (string.IsNullOrEmpty(json))
                {
                    FileUtils.LogRpa("平台文件内容为空", "CollectAndUploadAsync");
                    return null;
                }

                return JsonConvert.DeserializeObject<UnifiedDataModel>(json);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"读取平台文件异常: {ex.Message}", "CollectAndUploadAsync");
                return null;
            }
        }

        /// <summary>
        /// 上传全平台数据到 fupan-server（爱复盘后台）
        /// </summary>
        /// <param name="model">UnifiedDataModel（全平台合并后的数据）</param>
        /// <param name="videoId">视频唯一标识</param>
        /// <param name="batchNumber">直播批次号</param>
        /// <param name="secUid">主播 secUid</param>
        private static Task UploadToFupanServerAsync(UnifiedDataModel model, string videoId, string batchNumber, string secUid)
        {
            try
            {
                // 将 UnifiedDataModel 转换为 JuliangGatherDataEntity
                var gatherData = ConvertModelToGatherEntity(model, videoId, secUid, batchNumber);

                // 注意：不传 ossPath，仅更新截止直播间的累计指标数据（tb_video_data_viewing_confuse），
                // 不更新过程数据（oss_path 和 tb_video_data_viewing_paragraph）

                // 调用 fupan-server 接口上传
                string result = OceanEngineDataApi.updateOceanEngine(gatherData);
                if (!string.IsNullOrEmpty(result))
                {
                    FileUtils.LogRpa($"Fupan-server上传成功, videoId={videoId}", "CollectAndUploadAsync");
                }
                else
                {
                    FileUtils.LogRpa($"Fupan-server上传返回空, videoId={videoId}", "CollectAndUploadAsync");
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"Fupan-server上传异常: {ex.Message}", "CollectAndUploadAsync");
            }
            return Task.CompletedTask;
        }

        /// <summary>
        /// 将 UnifiedDataModel 转换为 JuliangGatherDataEntity
        /// 映射逻辑与 WriteJuliangLegacyFinishStatic 保持一致，确保字段关系和单位转换正确
        /// </summary>
        /// <param name="model">全平台合并后的 UnifiedDataModel</param>
        /// <param name="videoId">视频唯一标识</param>
        /// <param name="secUid">主播 secUid</param>
        /// <param name="batchNumber">直播批次号</param>
        /// <returns>JuliangGatherDataEntity（对应 fupan-server 的 OceanEngineDataBo）</returns>
        private static JuliangGatherDataEntity ConvertModelToGatherEntity(UnifiedDataModel model, string videoId, string secUid, string batchNumber)
        {
            var gatherData = new JuliangGatherDataEntity { isTakeProduct = 1 };

            // 标识字段
            gatherData.videoId = videoId;
            gatherData.secUid = model.secUid ?? secUid;
            gatherData.batchNumber = model.batchNumber ?? batchNumber;

            // 基础数值字段：模型有值才覆盖
            if (model.viewCount.HasValue)
                gatherData.totalWatchNum = (int?)model.viewCount.Value;

            // 金额字段：元 → 分（×100）
            if (model.salesRevenue.HasValue)
                gatherData.volume = (int?)(model.salesRevenue.Value * 100);

            if (model.refund.HasValue)
                gatherData.refundAmount = Math.Round(model.refund.Value * 100, 2);

            if (model.investment.HasValue)
                gatherData.launchRoiAmount = model.investment.Value * 100;

            if (model.payComboCnt.HasValue)
                gatherData.purchaseCount = model.payComboCnt.Value;

            if (model.followCount.HasValue)
                gatherData.incrementFollowerCount = model.followCount.Value;

            if (model.averageOnlineNum.HasValue && model.averageOnlineNum.Value > 0)
                gatherData.averageOnlineNum = model.averageOnlineNum.Value;

            if (model.averageResidenceTime.HasValue && model.averageResidenceTime.Value > 0)
                gatherData.averageResidenceTime = model.averageResidenceTime.Value;

            if (model.overallCostRoi.HasValue)
                gatherData.overallCostRoi = model.overallCostRoi.Value;

            if (model.netTransactionRoi.HasValue)
                gatherData.netTransactionRoi = model.netTransactionRoi.Value;

            // 比率字段：模型中是百分比，需 /100 转小数
            if (model.conversionRate.HasValue)
                gatherData.goodsConvertRate = model.conversionRate.Value / 100.0;

            if (model.uvValue.HasValue)
                gatherData.uvValue = model.uvValue.Value;

            if (model.roi.HasValue)
                gatherData.roi = model.roi.Value;

            if (model.followRate.HasValue)
                gatherData.convertFanRate = model.followRate.Value / 100.0;

            // gpm：千次观看成交金额，需 ×100
            if (model.thousandSales.HasValue)
                gatherData.gpm = model.thousandSales.Value * 100;

            if (model.interactionRate.HasValue)
                gatherData.interactionPercent = model.interactionRate.Value / 100.0;

            if (model.showWatchCntRatio.HasValue)
                gatherData.showWatchCntRatio = model.showWatchCntRatio.Value / 100.0;

            // 客单价：销售额(元) / 销量 × 100（转换为分）
            if (model.salesRevenue.HasValue && model.payComboCnt.HasValue && model.payComboCnt.Value > 0)
                gatherData.customerUnitPrice = Math.Round(model.salesRevenue.Value * 100 / model.payComboCnt.Value, 2);

            return gatherData;
        }

        /// <summary>
        /// 上传巨量实时数据到 COS（静态版本，供 CollectAndUploadAsync 调用）
        /// 仅在有 realTime 文件时执行（仅巨量平台有此数据）
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        /// <returns>COS 对象的 ossKey，无实时数据或上传失败时返回 null</returns>
        private static Task<string> UploadRealTimeToCosAsync(string videoId)
        {
            try
            {
                string realFilePath = JuliangDataHandle.getDataFilePath(videoId, 0);
                if (!File.Exists(realFilePath))
                {
                    FileUtils.LogRpa($"无实时数据文件，跳过COS上传, videoId={videoId}", "CollectAndUploadAsync");
                    return Task.FromResult<string>(null);
                }

                // 获取 COS 预签名上传 URL
                var signUploadUrlVo = OceanEngineDataApi.getDiagnosisSignUploadUrl(videoId);
                if (signUploadUrlVo == null)
                {
                    FileUtils.LogRpa($"获取COS预上传URL失败, videoId={videoId}", "CollectAndUploadAsync");
                    return Task.FromResult<string>(null);
                }

                // 压缩实时数据文件
                DirectoryInfo parentDir = new FileInfo(realFilePath).Directory;
                string zipName = $"{Guid.NewGuid()}.zip";
                string zipPath = $"{parentDir.FullName}/{zipName}";

                try
                {
                    FileUtils.ZipFileToOneFile(realFilePath, zipPath);

                    // 上传到 COS
                    bool uploadFlag = UploadUtils.UploadFileAsync(signUploadUrlVo.signedUrl, zipPath);
                    if (uploadFlag)
                    {
                        FileUtils.LogRpa($"实时数据COS上传成功, ossKey={signUploadUrlVo.ossKey}", "CollectAndUploadAsync");
                        return Task.FromResult(signUploadUrlVo.ossKey);
                    }
                }
                finally
                {
                    // 清理临时 zip 文件
                    if (File.Exists(zipPath))
                        File.Delete(zipPath);
                }

                return Task.FromResult<string>(null);
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"COS上传异常: {ex.Message}", "CollectAndUploadAsync");
                return Task.FromResult<string>(null);
            }
        }

        #endregion

    }
}
