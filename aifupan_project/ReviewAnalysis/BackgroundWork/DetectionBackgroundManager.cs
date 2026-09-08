using ReviewAnalysis.Bll.Anchor;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.BackgroundWork
{
    public class DetectionBackgroundManager
    {
        // 任务控制变量
        public static bool _isRunning = false; // 标记任务是否运行
        public static readonly object _lockObj = new object(); // 线程安全锁
        public static CancellationTokenSource _cts; // 取消令牌源（控制任务停止）
        /// <summary>
        /// 长时间处理任务（后台线程执行）
        /// </summary>
        /// <param name="token">取消令牌（用于接收停止信号）</param>
        public static void LongRunningTask(CancellationToken token, int videoType, int definition)
        {
            // 检查是否收到取消信号，若有则抛出取消异常
            token.ThrowIfCancellationRequested();

            AnchorBll.StartDetectionAll(videoType, definition);
           
        }
    }
}
