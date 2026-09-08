using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 简易异步锁（模拟lock语义）
    /// </summary>
    public class AsyncLock
    {
        private readonly SemaphoreSlim _semaphore = new SemaphoreSlim(1, 1);
        private readonly Task<IDisposable> _releaser;

        public AsyncLock()
        {
            // 释放锁的Disposable对象（using自动释放）
            _releaser = Task.FromResult((IDisposable)new Releaser(this));
        }

        // 异步获取锁
        public Task<IDisposable> LockAsync(CancellationToken token = default)
        {
            var waitTask = _semaphore.WaitAsync(token);
            return waitTask.IsCompleted
                ? _releaser
                : waitTask.ContinueWith((_, state) => (IDisposable)state,
                    _releaser.Result, token, TaskContinuationOptions.ExecuteSynchronously, TaskScheduler.Default);
        }

        // 辅助类：释放锁
        private sealed class Releaser : IDisposable
        {
            private readonly AsyncLock _asyncLock;
            public Releaser(AsyncLock asyncLock) => _asyncLock = asyncLock;
            public void Dispose() => _asyncLock._semaphore.Release();
        }
    }
}
