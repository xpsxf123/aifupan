using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using ReviewAnalysis.plugins.interfaces;
using ReviewAnalysis.Model;
using douyin.Utils;
using ReviewAnalysis.Bll;
using ReviewAnalysis.Bll.Anchor;
using ReviewAnalysis.DataCache;

namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 采集调度器状态
    /// </summary>
    public enum SchedulerStatus
    {
        /// <summary>已停止</summary>
        Stopped,
        /// <summary>启动中</summary>
        Starting,
        /// <summary>运行中</summary>
        Running,
        /// <summary>停止中</summary>
        Stopping
    }

    /// <summary>
    /// 采集调度器
    /// 管理主播实例（AnchorInstance）和采集模块
    /// </summary>
    public class CollectionScheduler : IDisposable
    {
        #region 字段

        // 旧架构：模块管理（保留兼容）
        private readonly SemaphoreSlim _concurrencyLimit;
        private readonly Dictionary<string, IDataCollector> _modules;
        private readonly Dictionary<string, ModuleConfig> _configs;
        private int _moduleIndex = 0;

        // 新架构：主播实例管理
        private readonly Dictionary<string, AnchorInstance> _anchorInstances;
        private readonly object _instanceLock = new object();

        private bool _disposed = false;

        // 调度器状态
        private SchedulerStatus _status = SchedulerStatus.Stopped;
        private readonly object _statusLock = new object();

        /// <summary>
        /// 当前调度器状态
        /// </summary>
        public SchedulerStatus Status
        {
            get
            {
                lock (_statusLock)
                {
                    return _status;
                }
            }
        }

        /// <summary>
        /// 调度器是否正在运行
        /// </summary>
        public bool IsRunning
        {
            get
            {
                lock (_statusLock)
                {
                    return _status == SchedulerStatus.Running;
                }
            }
        }

        // 配置
        public int MaxConcurrency { get; set; } = 3;
        public int StaggerDelayMs { get; set; } = 5000;  // 模块错峰间隔

        // 检测线程
        private Thread _detectionThread;
        private bool _isDetectionRunning = false;

        #endregion

        #region 构造函数

        public CollectionScheduler(int maxConcurrency = 3)
        {
            MaxConcurrency = maxConcurrency;
            _concurrencyLimit = new SemaphoreSlim(maxConcurrency);
            _modules = new Dictionary<string, IDataCollector>();
            _configs = new Dictionary<string, ModuleConfig>();
            _anchorInstances = new Dictionary<string, AnchorInstance>();
        }

        #endregion

        #region 主播实例管理（新架构）

        /// <summary>
        /// 启动主播采集
        /// 创建并启动一个 AnchorInstance
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="videoId">视频ID</param>
        public void StartAnchorCollection(AnchorInfo anchorInfo, string roomId, string videoId)
        {
            if (anchorInfo == null || string.IsNullOrEmpty(anchorInfo.SecUid))
            {
                FileUtils.LogRpa("启动主播采集失败：anchorInfo或secUid为空", "CollectionScheduler");
                return;
            }

            var secUid = anchorInfo.SecUid;

            lock (_instanceLock)
            {
                // 如果已存在，检查是否需要重启
                if (_anchorInstances.TryGetValue(secUid, out var existingInstance))
                {
                    // 检查 roomId 和 videoId 是否相同
                    if (existingInstance.RoomId != roomId)
                    {
                        FileUtils.LogRpa($"主播 {anchorInfo.AnchorName}({secUid}) 已有采集实例，参数已变更(旧roomId={existingInstance.RoomId},新roomId={roomId})，停止旧实例", "CollectionScheduler");
                        existingInstance.Stop();
                        _anchorInstances.Remove(secUid);
                    }
                    else if (videoId != null && existingInstance.VideoId != videoId)
                    {
                        FileUtils.LogRpa($"主播 {anchorInfo.AnchorName}({secUid}) 已有采集实例，参数已变更(旧VideoId={existingInstance.VideoId},新VideoId={videoId})，更新videoId", "CollectionScheduler");
                        existingInstance.SetVideoId(videoId);
                    }
                    return;
                }

                // 创建新实例
                var instance = new AnchorInstance(anchorInfo, roomId, videoId);
                _anchorInstances[secUid] = instance;
                instance.Start();

                FileUtils.LogRpa($"主播 {anchorInfo.AnchorName}({secUid}) 采集已启动，roomId={roomId}, videoId={videoId}", "CollectionScheduler");
            }
        }

        /// <summary>
        /// 停止主播采集
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="forceClose">是否强制关闭</param>
        public void StopAnchorCollection(string secUid, bool forceClose = false)
        {
            if (string.IsNullOrEmpty(secUid)) return;

            lock (_instanceLock)
            {
                if (_anchorInstances.TryGetValue(secUid, out var instance))
                {
                    // 是否是强制关闭 || 没有在排班的
                    if (forceClose)
                    {
                        var anchorName = instance.AnchorInfo?.AnchorName;
                        instance.Stop();
                        _anchorInstances.Remove(secUid);
                        FileUtils.LogRpa($"主播 {anchorName}({secUid}) 采集已停止", "CollectionScheduler");
                    }
                }
            }
        }

        /// <summary>
        /// 触发指定主播执行一次全平台数据采集，用于停止前拉取最后一轮数据
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <param name="videoEndTime">视频结束时间，用于覆盖 realTime 的 gatherDateTime</param>
        public async Task CollectOnceAsync(string secUid, string videoEndTime)
        {
            if (string.IsNullOrEmpty(secUid)) return;

            AnchorInstance instance;
            lock (_instanceLock)
            {
                if (!_anchorInstances.TryGetValue(secUid, out instance) || instance == null)
                    return;
            }
            await instance.CollectOnceAsync(videoEndTime);
        }

        /// <summary>
        /// 创建临时 AnchorInstance，执行一次性采集+上传，然后销毁（不启动定时器，不写旧版巨量文件）
        /// </summary>
        /// <param name="anchorInfo">主播信息</param>
        /// <param name="roomId">直播间ID（批次号）</param>
        public async Task CollectAndUploadOnceAsync(AnchorInfo anchorInfo, string roomId)
        {
            if (anchorInfo == null || string.IsNullOrEmpty(roomId))
            {
                FileUtils.LogRpa("CollectAndUploadOnceAsync: anchorInfo或roomId为空", "CollectionScheduler");
                return;
            }

            var instance = new AnchorInstance(anchorInfo, roomId, null);
            await instance.CollectAndUploadOnceAsync();
            instance.Dispose();

            FileUtils.LogRpa($"一次性采集+上传完成，主播={anchorInfo.AnchorName}({anchorInfo.SecUid})，roomId={roomId}", "CollectionScheduler");
        }

        /// <summary>
        /// 获取主播实例
        /// </summary>
        public AnchorInstance GetAnchorInstance(string secUid)
        {
            lock (_instanceLock)
            {
                return _anchorInstances.TryGetValue(secUid, out var instance) ? instance : null;
            }
        }

        /// <summary>
        /// 获取所有正在采集的主播SecUid
        /// </summary>
        public IEnumerable<string> GetActiveAnchorSecUids()
        {
            lock (_instanceLock)
            {
                return _anchorInstances.Keys.ToList();
            }
        }

        /// <summary>
        /// 获取正在采集的主播数量
        /// </summary>
        public int GetActiveAnchorCount()
        {
            lock (_instanceLock)
            {
                return _anchorInstances.Count;
            }
        }

        /// <summary>
        /// 停止所有主播采集
        /// </summary>
        public void StopAllAnchorCollections()
        {
            lock (_instanceLock)
            {
                foreach (var instance in _anchorInstances.Values)
                {
                    try
                    {
                        instance.Stop();
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogRpa($"停止主播 {instance.AnchorInfo?.AnchorName}({instance.SecUid}) 实例异常: {ex.Message}", "CollectionScheduler");
                    }
                }
                _anchorInstances.Clear();
                FileUtils.LogRpa("所有主播采集已停止", "CollectionScheduler");
            }
        }

        #endregion

        #region 模块管理（旧架构，保留兼容）

        /// <summary>
        /// 注册模块
        /// </summary>
        /// <param name="collector">采集器</param>
        /// <param name="config">配置（可选）</param>
        public void RegisterModule(IDataCollector collector, ModuleConfig config = null)
        {
            if (collector == null)
            {
                throw new ArgumentNullException(nameof(collector));
            }

            var moduleId = collector.ModuleId;
            if (string.IsNullOrEmpty(moduleId))
            {
                throw new ArgumentException("模块ID不能为空");
            }

            if (_modules.ContainsKey(moduleId))
            {
                FileUtils.LogRpa($"模块 {moduleId} 已注册，将被覆盖");
            }

            // 创建或复制配置
            config = config ?? ModuleConfig.Default;

            // 自动计算错峰offset
            var adjustedConfig = config.Clone();
            adjustedConfig.OffsetMs = _moduleIndex * StaggerDelayMs;

            // 注入配置和并发控制器
            collector.Initialize(adjustedConfig, _concurrencyLimit);

            _modules[moduleId] = collector;
            _configs[moduleId] = adjustedConfig;
            _moduleIndex++;

            FileUtils.LogRpa($"模块 {moduleId} ({collector.ModuleName}) 已注册，offset={adjustedConfig.OffsetMs}ms");
        }

        /// <summary>
        /// 获取模块
        /// </summary>
        public IDataCollector GetModule(string moduleId)
        {
            return _modules.TryGetValue(moduleId, out var collector) ? collector : null;
        }

        /// <summary>
        /// 获取所有模块ID
        /// </summary>
        public IEnumerable<string> GetAllModuleIds()
        {
            return _modules.Keys;
        }

        /// <summary>
        /// 获取模块数量
        /// </summary>
        public int GetModuleCount()
        {
            return _modules.Count;
        }

        #endregion

        #region 采集控制

        /// <summary>
        /// 启动所有模块
        /// </summary>
        public void StartAll(string secUid, string roomId, string videoId = null)
        {
            FileUtils.LogRpa($"启动所有模块，secUid={secUid}, roomId={roomId}, videoId={videoId}");

            foreach (var kvp in _modules.OrderBy(m => m.Value.ModuleName))
            {
                try
                {
                    kvp.Value.Start(secUid, roomId, videoId);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"启动模块 {kvp.Key} 失败：{ex.Message}", "CollectionScheduler错误");
                }
            }
        }

        /// <summary>
        /// 停止所有模块
        /// </summary>
        public void StopAll()
        {
            FileUtils.LogRpa("停止所有模块");

            foreach (var kvp in _modules)
            {
                try
                {
                    kvp.Value.Stop();
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"停止模块 {kvp.Key} 失败：{ex.Message}", "CollectionScheduler错误");
                }
            }
        }

        /// <summary>
        /// 启动指定模块
        /// </summary>
        public void StartModule(string moduleId, string secUid, string roomId, string videoId = null)
        {
            var collector = GetModule(moduleId);
            if (collector == null)
            {
                FileUtils.LogRpa($"模块 {moduleId} 不存在");
                return;
            }

            collector.Start(secUid, roomId, videoId);
        }

        /// <summary>
        /// 停止指定模块
        /// </summary>
        public void StopModule(string moduleId)
        {
            var collector = GetModule(moduleId);
            if (collector == null)
            {
                FileUtils.LogRpa($"模块 {moduleId} 不存在");
                return;
            }

            collector.Stop();
        }

        /// <summary>
        /// 重启指定模块
        /// </summary>
        public void RestartModule(string moduleId, string secUid, string roomId, string videoId = null)
        {
            StopModule(moduleId);
            StartModule(moduleId, secUid, roomId, videoId);
        }

        #endregion

        #region 状态查询

        /// <summary>
        /// 获取模块状态
        /// </summary>
        public ModuleStatus GetModuleStatus(string moduleId)
        {
            var collector = GetModule(moduleId);
            if (collector == null)
            {
                return null;
            }

            return new ModuleStatus
            {
                ModuleId = moduleId,
                ModuleName = collector.ModuleName,
                IsRunning = collector.IsRunning,
                VideoId = collector.GetEffectiveVideoId()
            };
        }

        /// <summary>
        /// 获取所有模块状态
        /// </summary>
        public IEnumerable<ModuleStatus> GetAllModuleStatus()
        {
            return _modules.Keys.Select(GetModuleStatus).Where(s => s != null);
        }

        /// <summary>
        /// 获取调度器详细状态信息
        /// </summary>
        public SchedulerInfo GetSchedulerInfo()
        {
            lock (_statusLock)
            {
                lock (_instanceLock)
                {
                    return new SchedulerInfo
                    {
                        Status = _status,
                        IsRunning = _status == SchedulerStatus.Running,
                        ActiveAnchorCount = _anchorInstances.Count,
                        ActiveAnchorSecUids = _anchorInstances.Keys.ToList(),
                        IsDetectionRunning = _isDetectionRunning,
                        Disposed = _disposed
                    };
                }
            }
        }

        #endregion

        #region 主播上线检测

        /// <summary>
        /// 启动主播上线检测（用于数据采集）
        /// 检测条件：抖音平台
        /// </summary>
        public void StartAnchorOnlineDetection()
        {
            lock (_statusLock)
            {
                if (_status == SchedulerStatus.Running || _status == SchedulerStatus.Starting)
                {
                    FileUtils.LogRpa("主播上线检测已在运行中", "CollectionScheduler");
                    return;
                }

                _status = SchedulerStatus.Running;
            }
        }

        /// <summary>
        /// 停止主播上线检测
        /// </summary>
        public void StopAnchorOnlineDetection()
        {
            lock (_statusLock)
            {
                if (_status == SchedulerStatus.Stopped || _status == SchedulerStatus.Stopping)
                {
                    return;
                }
                _status = SchedulerStatus.Stopping;
            }

            _isDetectionRunning = false;

            lock (_statusLock)
            {
                _status = SchedulerStatus.Stopped;
            }

            FileUtils.LogRpa("主播上线检测已停止", "CollectionScheduler");
        }


        #endregion

        #region IDisposable

        public void Dispose()
        {
            if (_disposed) return;

            lock (_statusLock)
            {
                if (_status == SchedulerStatus.Stopping || _status == SchedulerStatus.Stopped)
                {
                    return;
                }
                _status = SchedulerStatus.Stopping;
            }

            // 停止主播上线检测
            _isDetectionRunning = false;

            // 停止所有主播采集
            StopAllAnchorCollections();

            // 停止所有模块（旧架构）
            StopAll();
            
            _concurrencyLimit?.Dispose();
            _disposed = true;

            lock (_statusLock)
            {
                _status = SchedulerStatus.Stopped;
            }
        }

        #endregion
    }

    /// <summary>
    /// 模块状态
    /// </summary>
    public class ModuleStatus
    {
        public string ModuleId { get; set; }
        public string ModuleName { get; set; }
        public bool IsRunning { get; set; }
        public string VideoId { get; set; }
    }

    /// <summary>
    /// 调度器状态信息
    /// </summary>
    public class SchedulerInfo
    {
        /// <summary>当前状态</summary>
        public SchedulerStatus Status { get; set; }
        /// <summary>是否运行中</summary>
        public bool IsRunning { get; set; }
        /// <summary>活跃主播数量</summary>
        public int ActiveAnchorCount { get; set; }
        /// <summary>活跃主播SecUid列表</summary>
        public List<string> ActiveAnchorSecUids { get; set; }
        /// <summary>检测线程是否运行</summary>
        public bool IsDetectionRunning { get; set; }
        /// <summary>是否已释放</summary>
        public bool Disposed { get; set; }
    }
}
