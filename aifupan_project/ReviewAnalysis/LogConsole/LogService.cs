using douyin.Utils;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.LogConsole
{
    // 日志服务（单例，处理日志收集、跨线程更新界面）
    public class LogService
    {
        // 单例实例（确保全局唯一，避免多实例冲突）
        public static LogService Instance = new LogService();
        // 线程安全队列（缓存日志，避免跨线程操作界面冲突）
        private readonly ConcurrentQueue<LogModel> _logQueue = new ConcurrentQueue<LogModel>();
        private readonly ConcurrentQueue<LogModel> _pendingLogs = new ConcurrentQueue<LogModel>(); // 句柄未创建时的日志缓存
        // 日志更新委托（跨线程更新 RichTextBox 用）
        private Action<LogModel> _logUpdateAction;
        // 日志筛选级别（默认显示所有）
        public LogLevel FilterLevel { get; set; } = LogLevel.Info;
        private bool _isStarted = false; // 日志服务启动标记（句柄创建后才启动）
        private readonly object _lockObj = new object(); // 线程安全锁
                                                         // 线程取消令牌（控制后台线程退出）
        private CancellationTokenSource _cts;
        // 后台日志处理线程
        private Thread _logThread;
        private bool _isPaused = false; // 日志更新暂停标记（默认未暂停）
        private bool _stopFileWriteWhenPaused = false; // 暂停时是否停止文件写入（默认不停止）

        /// <summary>
        /// 暂停/恢复日志更新（界面+可选文件写入）
        /// </summary>
        /// <param name="isPaused">true=暂停，false=恢复</param>
        /// <param name="stopFileWrite">暂停时是否停止文件写入（默认不停止）</param>
        public void ToggleLogPause(bool isPaused, bool stopFileWrite = false)
        {
            lock (_lockObj)
            {
                _isPaused = isPaused;
                _stopFileWriteWhenPaused = stopFileWrite;

                // 暂停时唤醒后台线程（避免线程卡在 Wait 状态）
                if (isPaused)
                {
                    Monitor.Pulse(_lockObj);
                }
            }

            // 记录日志（无论是否暂停文件写入，都强制记录此状态日志）
            string status = isPaused ? "暂停" : "恢复";
            string fileWriteTip = stopFileWrite ? "（文件写入已停止）" : "（文件写入正常）";
            FileUtils.log($"{status}日志更新 {fileWriteTip}", "LogService.TogglePause");
        }

        /// <summary>
        /// 获取当前暂停状态
        /// </summary>
        public bool IsLogPaused => _isPaused;

        // 原有 Start、Stop、SubmitLog 方法不变，修改 ProcessLogQueue 方法：
        private void ProcessLogQueue(object obj)
        {
            if (!(obj is CancellationToken cancellationToken))
            {
                FileUtils.LogError("日志线程参数错误：未获取到 CancellationToken", "LogService");
                return;
            }

            try
            {
                while (!cancellationToken.IsCancellationRequested)
                {
                    // 核心：检查是否暂停，暂停时休眠等待（释放锁，避免阻塞其他操作）
                    lock (_lockObj)
                    {
                        while (_isPaused)
                        {
                            // 暂停时每秒检查一次状态（可调整间隔），同时响应唤醒信号
                            Monitor.Wait(_lockObj, 1000);
                        }
                    }

                    // 正常处理日志队列（原有逻辑不变）
                    if (_logQueue.TryDequeue(out var log))
                    {
                        try
                        {
                            // 暂停且需停止文件写入时，跳过文件写入（仅 UI 暂停时不影响）
                            if (!_isPaused || !_stopFileWriteWhenPaused)
                            {
                                // 执行 UI 更新委托（原有逻辑）
                                Action<LogModel> updateAction;
                                lock (_lockObj)
                                {
                                    updateAction = _logUpdateAction;
                                }
                                updateAction?.Invoke(log);
                            }
                        }
                        catch (InvalidAsynchronousStateException ex)
                        {
                            FileUtils.LogError($"日志更新失败：{ex.Message}", "LogService");
                            break;
                        }
                        catch (Exception ex)
                        {
                            FileUtils.LogError($"日志处理异常：{ex.Message}", "LogService");
                        }
                    }
                    else
                    {
                        // 队列空时休眠，减少 CPU 占用
                        cancellationToken.WaitHandle.WaitOne(50);
                    }
                    cancellationToken.WaitHandle.WaitOne(50);
                    //Thread.Sleep(50);
                }
            }
            catch (ThreadAbortException)
            {
                FileUtils.log("日志服务线程已终止", "LogService");
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"日志线程异常退出：{ex.Message}", "LogService");
            }
        }

        // 「仅文件日志服务」的 ProcessFileLogQueue 方法同步添加暂停逻辑（可选）
        private void ProcessFileLogQueue(object obj)
        {
            if (!(obj is CancellationToken cancellationToken))
            {
                FileUtils.LogError("文件日志线程参数错误", "LogService");
                return;
            }

            try
            {
                while (!cancellationToken.IsCancellationRequested)
                {
                    lock (_lockObj)
                    {
                        while (_isPaused && _stopFileWriteWhenPaused)
                        {
                            Monitor.Wait(_lockObj, 1000); // 暂停且停止文件写入时，休眠
                        }
                    }

                    if (_logQueue.TryDequeue(out var log))
                    {
                        // 暂停且停止文件写入时，跳过写入
                        if (!_isPaused || !_stopFileWriteWhenPaused)
                        {
                            string logText = $"[{log.Time:yyyy-MM-dd HH:mm:ss.fff}] " +
                                            $"[{log.Level.ToString().PadRight(7)}] " +
                                            $"[{log.Source.PadRight(15)}] " +
                                            log.Message;
                            //FileUtils.WriteToFile(logText);
                        }
                    }
                    else
                    {
                        cancellationToken.WaitHandle.WaitOne(50);
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"文件日志线程异常：{ex.Message}", "LogService");
            }
        }

        // 初始化：传入界面日志更新方法
        public void Init(Action<LogModel> logUpdateAction)
        {
            lock (_lockObj)
            {
                _logUpdateAction = logUpdateAction ?? throw new ArgumentNullException(nameof(logUpdateAction));
                // 避免重复启动
                if (_logThread != null && _logThread.IsAlive) return;
                // 初始化取消令牌
                _cts = new CancellationTokenSource();
            }
            // 启动后台线程（不变）
            //new Thread(ProcessLogQueue)
            //{
            //    IsBackground = true,
            //    Name = "LogProcessThread",
            //    Priority = ThreadPriority.BelowNormal
            //}.Start();
            // 创建后台线程（传入取消令牌）
            _logThread = new Thread(ProcessLogQueue)
            {
                IsBackground = true,
                Name = "LogProcessThread",
                Priority = ThreadPriority.BelowNormal // 降低线程优先级，不影响 UI 响应
            };
            _logThread.Start(_cts.Token);
            FileUtils.log("日志服务启动成功", "LogService");
        }

        // 提交日志到队列（供 FileUtils 调用）
        public void SubmitLog(LogLevel level, string message, string source)
        {
            if (_cts != null && !_cts.IsCancellationRequested)
            {
                var log = new LogModel
                {
                    Time = DateTime.Now,
                    Level = level,
                    Message = message,
                    Source = source
                };
                _logQueue.Enqueue(log);
            } 
        }

        // 后台线程处理日志队列，更新界面
        //private void ProcessLogQueue()
        //{
        //    while (true)
        //    {
        //        if (_logQueue.TryDequeue(out var log))
        //        {
        //            // 筛选日志（只显示当前选择级别及以上的日志）
        //            if (log.Level >= FilterLevel)
        //            {
        //                _logUpdateAction?.Invoke(log);
        //            }
        //        }
        //        else
        //        {
        //            // 队列空时休眠100ms，减少CPU占用
        //            //System.Threading.Thread.Sleep(100);
        //        }
        //        System.Threading.Thread.Sleep(500);
        //    }
        //}
        /// <summary>
        /// 停止日志服务（终止后台线程+释放资源）
        /// </summary>
        public void Stop()
        {
            lock (_lockObj)
            {
                // 取消令牌触发线程退出
                if (_cts != null && !_cts.IsCancellationRequested)
                {
                    _cts.Cancel();
                    _cts.Dispose(); // 释放取消令牌资源
                }

                // 等待线程退出（最多等待1秒，避免阻塞）
                if (_logThread != null && _logThread.IsAlive)
                {
                    _logThread.Join(1000);
                    if (_logThread.IsAlive)
                    {
                        FileUtils.LogError("日志服务线程未正常退出，强制终止", "LogService");
                        _logThread.Abort(); // 极端情况强制终止（不推荐，但避免资源泄漏）
                    }
                }

                // 重置状态
                _logThread = null;
                _logUpdateAction = null;
                //_pendingLogs(); // 清空缓存日志
                FileUtils.log("日志服务已停止", "LogService");
            }
        }
    }
}
