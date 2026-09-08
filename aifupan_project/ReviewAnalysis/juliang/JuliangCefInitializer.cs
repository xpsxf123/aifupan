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
    public static class JuliangCefInitializer
    {

        // 线程锁：确保初始化仅执行一次
        private static readonly object _initLock = new object();
        // 标记是否初始化成功（避免重复初始化）
        private static bool _isInitSuccess = false;

        /// <summary>
        /// 安全初始化 Cef（线程安全，支持重复调用）
        /// </summary>
        /// <returns>是否初始化成功</returns>
        public static bool SafeInitialize()
        {
            // 已初始化成功，直接返回
            if (_isInitSuccess && (bool)Cef.IsInitialized)
            {
                FileUtils.log("Cef 已初始化成功");
                return true;
            }

            lock (_initLock)
            {
                try
                {
                    // 未初始化或初始化失败，重新初始化
                    if ((bool)!Cef.IsInitialized)
                    {
                        var settings = new CefSettings
                        {
                            // 关键配置：确保缓存目录可读写（避免权限问题）
                            CachePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "JuliangCefSharp_Cache"),
                            PersistSessionCookies = true,
                            IgnoreCertificateErrors = true,
                            //JavascriptEnabled = true,
                            // 新增：启用日志（方便排查初始化失败原因）
                            LogFile = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "JuliangCefSharp_Log.txt"),
                            LogSeverity = LogSeverity.Info // 日志级别：Info（包含初始化细节）
                        };

                        // 核心：调用初始化并检查返回值（Cef.Initialize 返回 bool 表示是否成功）
                        _isInitSuccess = Cef.Initialize(settings, performDependencyCheck: true, browserProcessHandler: null);
                        if (_isInitSuccess)
                        {
                            FileUtils.log("Cef 初始化成功");
                        }
                        else
                        {
                            FileUtils.log("Cef 初始化失败（无异常，但返回 false）");
                            return false;
                        }
                    }
                    else
                    {
                        _isInitSuccess = true;
                    }

                    return _isInitSuccess;
                }
                catch (Exception ex)
                {
                    FileUtils.log($"Cef 初始化抛出异常：{ex.Message}\n堆栈：{ex.StackTrace}");
                    _isInitSuccess = false;
                    return false;
                }
            }
        }
    }
}
