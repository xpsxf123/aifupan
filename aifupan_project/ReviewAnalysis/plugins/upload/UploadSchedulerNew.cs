using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.api.governance;
using ReviewAnalysis.plugins.constant;
using ReviewAnalysis.plugins.core;
using ReviewAnalysis.plugins.models;
using ReviewAnalysis.plugins.utils;
using ReviewAnalysis.ShortVideo.Model;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis.plugins.upload
{
    /// <summary>
    /// 上传结果枚举
    /// </summary>
    public enum UploadResultType
    {
        /// <summary>上传成功</summary>
        Success,
        /// <summary>业务错误（数据不对，无需重试）</summary>
        BusinessError,
        /// <summary>网络异常（可重试）</summary>
        NetworkError
    }

    /// <summary>
    /// Platform目录上传调度器
    /// 定时（10分钟）读取platform目录数据并按平台错峰上传
    /// </summary>
    public class PlatformUploadScheduler : IDisposable
    {
        private readonly Timer _uploadTimer;
        private readonly PlatformDataManager _dataManager;
        private bool _disposed;
        private bool _started;


        public PlatformUploadScheduler()
        {
            _dataManager = new PlatformDataManager();
            _uploadTimer = new Timer(UploadCallback, null, Timeout.Infinite, Timeout.Infinite);
        }

        /// <summary>
        /// 启动定时上传（登录成功后调用）
        /// </summary>
        public void Start()
        {
            if (_started) return;
            _started = true;

            // 从服务器获取上传间隔（单位：分钟），默认10分钟
            int uploadIntervalMinutes = KvHelper.GetIntKvByKey("platform_upload_time", 10);
            // 从服务器获取首次延迟（单位：分钟），默认0.1分钟
            double firstDelayMinutes = KvHelper.GetDoubleKvByKey("platform_upload_firstDelayMinutes", 0.1);

            _uploadTimer.Change(TimeSpan.FromMinutes(firstDelayMinutes), TimeSpan.FromMinutes(uploadIntervalMinutes));
            FileUtils.LogRpa($"上传调度器已启动，首次延迟{firstDelayMinutes}分钟，上传间隔{uploadIntervalMinutes}分钟", "上传调度器");
        }

        /// <summary>
        /// 立即上传
        /// </summary>
        public async Task UploadNowAsync()
        {
            await UploadPendingFilesAsync();
        }

        /// <summary>
        /// 上传回调
        /// </summary>
        private async void UploadCallback(object state)
        {
            try
            {
                await UploadPendingFilesAsync();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"上传调度异常: {ex.Message}", "上传调度器");
            }
        }

        /// <summary>
        /// 上传待上传文件
        /// </summary>
        private async Task UploadPendingFilesAsync()
        {
            try
            {
                var directory = Path.Combine(Environment.CurrentDirectory, PathUtils.PlatformPath);
                if (!Directory.Exists(directory))
                {
                    FileUtils.LogRpa("platform目录不存在，跳过上传", "上传调度器");
                    return;
                }

                var files = Directory.GetFiles(directory, "*.json");

                if (files.Length == 0)
                {
                    FileUtils.LogRpa("没有待上传文件", "上传调度器");
                    return;
                }

                FileUtils.LogRpa($"发现 {files.Length} 个待上传文件，开始上传", "上传调度器");

                foreach (var file in files)
                {
                    var result = UploadFileAsync(file);
                    if (result == UploadResultType.Success)
                    {
                        _dataManager.ArchiveUploadedFile(file, PlatformConstants.DataPrefix);
                        FileUtils.LogRpa($"文件上传并归档成功: {file}", "上传调度器");
                    }
                    else if (result == UploadResultType.BusinessError)
                    {
                        _dataManager.ArchiveFailedFile(file, PlatformConstants.DataPrefix);
                        FileUtils.LogRpa($"文件数据有误，已归档（unUp）: {file}", "上传调度器");
                    }
                    else
                    {
                        FileUtils.LogRpa($"文件上传失败（网络异常），将重试: {file}", "上传调度器");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"上传待上传文件异常: {ex.Message}", "上传调度器");
            }
        }

        /// <summary>
        /// 上传单个文件
        /// </summary>
        private UploadResultType UploadFileAsync(string filePath)
        {
            try
            {
                UnifiedDataModel loadDataFromFile = _dataManager.LoadDataFromFile(filePath);

                if (loadDataFromFile == null)
                {
                    FileUtils.LogRpa($"上传的内容为null", "上传调度器");
                    return UploadResultType.Success;
                }
                // 处理时间
                handleTime(loadDataFromFile);
                // 通过 HttpUtils 统一请求，自动带上 token、webVersion、签名头，内置重试
                var r = ClientPushVideoData.clientPushVideo(loadDataFromFile);

                if (r.success())
                {
                    return UploadResultType.Success;
                }
                else
                {
                    FileUtils.LogRpa($"上传失败，msg: {JsonConvert.SerializeObject(r)}", "上传调度器");
                    return UploadResultType.BusinessError;
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"上传文件异常: {ex.Message}", "上传调度器");
                return UploadResultType.NetworkError;
            }
        }

        private void handleTime(UnifiedDataModel baseModel)
        {
            // 处理视频开始时间和结束时间
            if (baseModel.oceanEngineProcessList != null && baseModel.oceanEngineProcessList.Count > 0)
            {
                var times = baseModel.oceanEngineProcessList
                    .Select(p => DateTime.TryParse(p.gatherDateTime, out var dt) ? dt : (DateTime?)null)
                    .Where(dt => dt.HasValue)
                    .Select(dt => dt.Value)
                    .ToList();

                if (times.Count > 0)
                {
                    var minTime = times.Min();
                    var maxTime = times.Max();

                    if (!string.IsNullOrEmpty(baseModel.startTime)
                        && DateTime.TryParse(baseModel.startTime, out var existStart))
                    {
                        if (existStart > minTime) baseModel.startTime = minTime.ToString("yyyy-MM-dd HH:mm:ss");
                    }
                    else
                    {
                        baseModel.startTime = minTime.ToString("yyyy-MM-dd HH:mm:ss");
                    }

                    if (!string.IsNullOrEmpty(baseModel.endTime)
                        && DateTime.TryParse(baseModel.endTime, out var existEnd))
                    {
                        if (existEnd < maxTime) baseModel.endTime = maxTime.ToString("yyyy-MM-dd HH:mm:ss");
                    }
                    else
                    {
                        baseModel.endTime = maxTime.ToString("yyyy-MM-dd HH:mm:ss");
                    }
                }
            }
        }

        public void Dispose()
        {
            if (_disposed) return;
            _uploadTimer?.Dispose();
            _disposed = true;
        }
    }
}
