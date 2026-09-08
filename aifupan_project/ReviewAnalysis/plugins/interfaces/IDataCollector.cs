using System;
using System.Threading;
using System.Threading.Tasks;
using ReviewAnalysis.plugins.models;
using ReviewAnalysis.plugins.core;

namespace ReviewAnalysis.plugins.interfaces
{
    /// <summary>
    /// 数据采集器接口
    /// </summary>
    public interface IDataCollector
    {
        /// <summary>
        /// 模块ID（唯一标识）
        /// </summary>
        string ModuleId { get; }

        /// <summary>
        /// 模块名称
        /// </summary>
        string ModuleName { get; }

        /// <summary>
        /// 数据采集完成事件
        /// </summary>
        event EventHandler<DataCollectedEventArgs> OnDataCollected;

        /// <summary>
        /// 初始化采集器
        /// </summary>
        /// <param name="config">模块配置</param>
        /// <param name="concurrencyLimit">并发控制器</param>
        void Initialize(ModuleConfig config, SemaphoreSlim concurrencyLimit);

        /// <summary>
        /// 启动采集
        /// </summary>
        /// <param name="secUid">主播ID</param>
        /// <param name="roomId">直播间ID</param>
        /// <param name="videoId">视频ID（可选）</param>
        void Start(string secUid, string roomId, string videoId = null);

        /// <summary>
        /// 停止采集
        /// </summary>
        void Stop();

        /// <summary>
        /// 执行一次采集
        /// </summary>
        /// <returns>采集是否成功</returns>
        Task<bool> PollOnceAsync();

        /// <summary>
        /// 获取当前采集状态
        /// </summary>
        bool IsRunning { get; }

        /// <summary>
        /// 获取当前有效的videoId
        /// </summary>
        string GetEffectiveVideoId();
    }
}
