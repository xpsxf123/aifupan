using CefSharp;
using CefSharp.WinForms;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.juliang
{
    /// <summary>
    /// 非静态版 CefRequestContext 管理器（每个CEF实例绑定一个）
    /// </summary>
    public sealed class CefRequestContextManager : IDisposable
    {
        // 实例内线程锁（保护RequestContext操作）
        private readonly object _instanceLock = new object();
        // 当前实例的RequestContext
        private IRequestContext _requestContext;
        // 标记是否已初始化/释放
        private bool _isInitialized = false;
        private bool _isDisposed = false;

        // ========== 实例独立配置（构造传入） ==========
        /// <summary>当前实例的缓存路径（每个实例独立）</summary>
        public string CachePath { get; private set; }
        /// <summary>是否持久化Session Cookie</summary>
        public bool PersistSessionCookies { get; private set; } = true;
        /// <summary>是否已初始化Cef内核（全局仅需一次）</summary>
        private static bool _isCefGlobalInitialized = false;
        /// <summary>Cef全局初始化锁</summary>
        private static readonly object _cefGlobalLock = new object();

        #region 构造函数（每个实例独立传参）
        /// <summary>
        /// 构造函数（指定缓存路径）
        /// </summary>
        /// <param name="cachePath">当前实例的缓存路径（必填，确保独立）</param>
        /// <param name="persistSessionCookies">是否持久化Session Cookie</param>
        /// <exception cref="ArgumentNullException">缓存路径为空时抛出</exception>
        public CefRequestContextManager(string cachePath, bool persistSessionCookies = true)
        {
            if (string.IsNullOrWhiteSpace(cachePath))
                throw new ArgumentNullException(nameof(cachePath), "每个CEF实例的缓存路径不能为空");

            // 初始化实例配置
            CachePath = cachePath;
            PersistSessionCookies = persistSessionCookies;

            // 确保缓存目录存在（每个实例的路径独立创建）
            EnsureCacheDirectoryExists();
        }
        #endregion

        #region 核心方法：初始化当前实例的RequestContext
        /// <summary>
        /// 初始化当前实例的RequestContext（每个实例仅初始化一次）
        /// </summary>
        /// <returns>当前实例的RequestContext</returns>
        /// <exception cref="ObjectDisposedException">已释放时抛出</exception>
        public IRequestContext InitializeRequestContext()
        {
            CheckDisposed();

            lock (_instanceLock)
            {
                // 已初始化则直接返回
                if (_isInitialized && _requestContext != null && !_requestContext.IsDisposed)
                {
                    return _requestContext;
                }

                // 全局初始化Cef内核（仅执行一次）
                //EnsureCefGlobalInitialized();

                // 创建当前实例的RequestContext（独立缓存路径）
                var requestContextSettings = new RequestContextSettings
                {
                    PersistSessionCookies = PersistSessionCookies,
                    CachePath = CachePath
                };
                _requestContext = new RequestContext(requestContextSettings);

                // 标记实例已初始化
                _isInitialized = true;
                FileUtils.log($"✅ CEF实例上下文初始化完成，缓存路径：{CachePath}");

                return _requestContext;
            }
        }
        #endregion

        #region 核心方法：获取当前实例的RequestContext
        /// <summary>
        /// 获取当前实例的RequestContext（未初始化则自动初始化）
        /// </summary>
        /// <returns>当前实例的RequestContext</returns>
        public IRequestContext GetRequestContext()
        {
            CheckDisposed();

            lock (_instanceLock)
            {
                return _requestContext ?? InitializeRequestContext();
            }
        }
        #endregion

        #region 辅助方法
        /// <summary>确保Cef内核全局仅初始化一次</summary>
        private void EnsureCefGlobalInitialized()
        {
            lock (_cefGlobalLock)
            {
                if (_isCefGlobalInitialized) return;

                var cefSettings = new CefSettings
                {
                    IgnoreCertificateErrors = true,
                    //JavascriptEnabled = true,
                    LogSeverity = LogSeverity.Info
                };
                Cef.Initialize(cefSettings);
                _isCefGlobalInitialized = true;
                FileUtils.log("✅ Cef内核全局初始化完成");
            }
        }

        /// <summary>确保当前实例的缓存目录存在</summary>
        private void EnsureCacheDirectoryExists()
        {
            if (!Directory.Exists(CachePath))
            {
                Directory.CreateDirectory(CachePath);
                FileUtils.log($"✅ 创建CEF实例缓存目录：{CachePath}");
            }
        }

        /// <summary>检查当前实例是否已释放</summary>
        private void CheckDisposed()
        {
            if (_isDisposed)
                throw new ObjectDisposedException(nameof(CefRequestContextManager), $"CEF上下文管理器（缓存路径：{CachePath}）已释放");
        }

        /// <summary>清除当前实例的缓存（切换账号/重置登录态）</summary>
        public void ClearInstanceCache()
        {
            CheckDisposed();

            lock (_instanceLock)
            {
                // 释放当前实例的RequestContext
                if (_requestContext != null && !_requestContext.IsDisposed)
                {
                    _requestContext.Dispose();
                    _requestContext = null;
                }

                // 删除当前实例的缓存目录
                if (Directory.Exists(CachePath))
                {
                    try
                    {
                        Directory.Delete(CachePath, recursive: true);
                        Directory.CreateDirectory(CachePath); // 重建空目录
                        FileUtils.log($"✅ 清空CEF实例缓存目录：{CachePath}");
                    }
                    catch (Exception ex)
                    {
                        FileUtils.log($"❌ 清空CEF实例缓存失败：{ex.Message}");
                    }
                }

                // 重置初始化标记（可重新初始化）
                _isInitialized = false;
            }
        }
        #endregion

        #region 资源释放（每个实例独立释放）
        /// <summary>释放当前实例的RequestContext（不释放全局Cef内核）</summary>
        public void Dispose()
        {
            lock (_instanceLock)
            {
                if (_isDisposed) return;

                // 释放当前实例的RequestContext
                if (_requestContext != null && !_requestContext.IsDisposed)
                {
                    _requestContext.Dispose();
                    _requestContext = null;
                    FileUtils.log($"✅ 释放CEF实例上下文（缓存路径：{CachePath}）");
                }

                _isDisposed = true;
                _isInitialized = false;
            }
        }

        /// <summary>全局释放Cef内核（程序退出时调用一次）</summary>
        public static void ShutdownCefGlobal()
        {
            lock (_cefGlobalLock)
            {
                if (_isCefGlobalInitialized && (bool)Cef.IsInitialized)
                {
                    Cef.Shutdown();
                    _isCefGlobalInitialized = false;
                    FileUtils.log("✅ 全局释放Cef内核完成");
                }
            }
        }
        #endregion
    }
}
