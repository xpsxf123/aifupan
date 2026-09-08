using douyin.Utils;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Concurrent;
using System.IO;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.DataCache
{
    /// <summary>
    /// MP4转换队列管理器
    /// 用于管理视频录制完成后自动转成MP4的任务队列
    /// </summary>
    public class Mp4ConvertQueueManager
    {
        /// <summary>
        /// MP4转换任务队列
        /// </summary>
        private static ConcurrentQueue<VideoEntity> _convertQueue = new ConcurrentQueue<VideoEntity>();

        /// <summary>
        /// 当前正在转换的视频ID
        /// </summary>
        private static volatile string _currentConvertingVideoId = null;

        /// <summary>
        /// 队列消费线程是否正在运行
        /// </summary>
        private static volatile bool _isConsuming = false;

        /// <summary>
        /// 用于线程安全的锁对象
        /// </summary>
        private static readonly object _lockObject = new object();

        /// <summary>
        /// 获取当前正在转换的视频ID
        /// </summary>
        /// <returns>当前正在转换的视频ID，如果没有则返回null</returns>
        public static string GetCurrentConvertingVideoId()
        {
            return _currentConvertingVideoId;
        }

        /// <summary>
        /// 将视频任务添加到MP4转换队列
        /// </summary>
        /// <param name="video">视频信息</param>
        /// <returns>是否成功添加到队列</returns>
        public static bool AddToQueue(VideoEntity video)
        {
            if (video == null || string.IsNullOrEmpty(video.videoId))
            {
                FileUtils.LogAnalysis("视频信息为空或视频ID为空，无法加入MP4转换队列");
                return false;
            }

            // 检查1：如果磁盘已经存在MP4文件，则不加入队列
            string mp4Path = GetMp4Path(video);
            if (!string.IsNullOrEmpty(mp4Path) && File.Exists(mp4Path))
            {
                FileUtils.LogAnalysis($"{video.videoId}", $"MP4文件已存在，不加入转换队列：{mp4Path}");
                return false;
            }

            // 检查2：如果当前视频ID与当前转换任务视频ID一致，则不加入队列
            if (!string.IsNullOrEmpty(_currentConvertingVideoId) && _currentConvertingVideoId.Equals(video.videoId))
            {
                FileUtils.LogAnalysis($"{video.videoId}", "视频正在转换中，不重复加入队列");
                return false;
            }

            // 加入队列
            _convertQueue.Enqueue(video);
            FileUtils.LogAnalysis($"{video.videoId}", $"视频已加入MP4转换队列，当前队列长度：{_convertQueue.Count}");

            // 确保消费线程正在运行
            StartConsumer();

            return true;
        }

        /// <summary>
        /// 启动队列消费线程
        /// </summary>
        private static void StartConsumer()
        {
            lock (_lockObject)
            {
                if (!_isConsuming)
                {
                    _isConsuming = true;
                    Task.Run(() => ConsumeQueue());
                }
            }
        }

        /// <summary>
        /// 消费队列，执行MP4转换
        /// 限制同时只能有一个视频在转MP4
        /// </summary>
        private static void ConsumeQueue()
        {
            try
            {
                while (_convertQueue.TryDequeue(out VideoEntity video))
                {
                    try
                    {
                        // 转换前再次检查：如果磁盘已经存在MP4文件，则不转换直接结束
                        string mp4Path = GetMp4Path(video);
                        if (!string.IsNullOrEmpty(mp4Path) && File.Exists(mp4Path))
                        {
                            FileUtils.LogAnalysis($"{video.videoId}", $"转换前检查-MP4文件已存在，跳过转换：{mp4Path}");
                            continue;
                        }

                        // 检查源视频文件是否存在
                        if (!File.Exists(video.storagePath))
                        {
                            FileUtils.LogAnalysis($"{video.videoId}", $"源视频文件不存在，跳过转换：{video.storagePath}");
                            continue;
                        }

                        // 记录当前任务的视频ID
                        _currentConvertingVideoId = video.videoId;
                        FileUtils.LogAnalysis($"{video.videoId}", "开始自动转换MP4");

                        // 执行转换
                        int platform = 1;
                        if (!string.IsNullOrEmpty(video.platformType) && int.TryParse(video.platformType, out int pType))
                        {
                            platform = pType;
                        }
                        VideoUtils.ConvertToMP4(video.storagePath, video.videoName, platform);

                        FileUtils.LogAnalysis($"{video.videoId}", "自动转换MP4完成");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogAnalysis($"{ex}", $"自动转换MP4发生异常，视频ID：{video?.videoId}");
                    }
                    finally
                    {
                        // 任务结束后清空任务视频ID
                        _currentConvertingVideoId = null;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.LogAnalysis($"{ex}", "MP4转换队列消费线程发生异常");
            }
            finally
            {
                lock (_lockObject)
                {
                    _isConsuming = false;
                }
            }
        }

        /// <summary>
        /// 根据视频信息获取MP4文件路径
        /// </summary>
        /// <param name="video">视频信息</param>
        /// <returns>MP4文件路径</returns>
        private static string GetMp4Path(VideoEntity video)
        {
            if (video == null || string.IsNullOrEmpty(video.storagePath))
            {
                return null;
            }

            string outputDirectoryPath = video.storagePath.Substring(0, video.storagePath.LastIndexOf("\\"));
            outputDirectoryPath += "\\mp4\\";
            outputDirectoryPath += video.videoName + ".mp4";

            return outputDirectoryPath;
        }

        /// <summary>
        /// 获取当前队列长度
        /// </summary>
        /// <returns>队列中的任务数量</returns>
        public static int GetQueueCount()
        {
            return _convertQueue.Count;
        }

        /// <summary>
        /// 清空队列
        /// </summary>
        public static void ClearQueue()
        {
            while (_convertQueue.TryDequeue(out _)) { }
            _currentConvertingVideoId = null;
            FileUtils.LogAnalysis("MP4转换队列已清空");
        }
    }
}
