using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;
using douyin.Utils;
using ReviewAnalysis.Asr.Local;
using ReviewAnalysis.enums;
using ReviewAnalysis.Global;
using ReviewAnalysis.Utils;

namespace ReviewAnalysis
{
    internal static class Program
    {
        static Mutex mutex = new Mutex(true, "{A56E2E0D-0D3A-4D5B-A94D-4D57A1234567}");

        [DllImport("user32.dll")]
        private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll")]
        private static extern IntPtr FindWindow(string lpClassName, string lpWindowName);

        [DllImport("user32.dll")]
        private static extern bool SetForegroundWindow(IntPtr hWnd);
        [DllImport("user32.dll", SetLastError = true)]
        private static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);
        [DllImport("user32.dll")]
        private static extern bool IsIconic(IntPtr hWnd);

        private const int SW_RESTORE = 9; // 用于还原窗口的常量
        private const int SW_SHOW = 5; // 用于显示窗口的常量

        // 静态服务提供者
        public static IServiceProvider ServiceProvider { get; private set; }

        public static string closePath = Path.GetFullPath("Config\\close");

        /// <summary>
        /// 应用程序的主入口点。
        /// </summary>
        [STAThread]
        static void Main()
        {

            // 初始化服务容器
            //var services = new ServiceCollection();
            //ConfigureServices(services);
            //ServiceProvider = services.BuildServiceProvider();

            // 处理未捕获的异常  
            Application.SetUnhandledExceptionMode(UnhandledExceptionMode.CatchException);
            // 处理UI线程异常 
            Application.ThreadException += Application_ThreadException;
            // 处理非UI线程异常  
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;
            // 捕获 Task 异步任务未处理异常（若用了 async/await）
            System.Threading.Tasks.TaskScheduler.UnobservedTaskException += (sender, e) =>
            {
                try
                {
                    // 标记异常为“已观察”，避免进程终止
                    e.SetObserved();

                    // 记录异常（用修复后的LogError）
                    foreach (var ex in e.Exception.InnerExceptions)
                    {
                        FileUtils.LogError($"未观察到的Task异常：{ex}", "全局异常捕获");
                    }
                }
                catch
                {
                    // 终极兜底：避免捕获逻辑自身抛异常
                    FileUtils.LogError($"未观察到的Task异常：{e.Exception.Message}", "全局异常捕获");
                }
            };

            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
            ServicePointManager.DnsRefreshTimeout = 60 * 1000;

            var monitor = new CefSubprocessMonitor(1800); // 每60秒记录一次
            monitor.Start();

            AppContext.SetSwitch("Switch.System.IO.UseLegacyPathHandling", false);
            bool isNewInstance = false;
            
            // 判断当前客户端是什么版本
            Constant.CLIENT_VERSION = GetClientVersion();

            try
            {
                // 正确使用Mutex
                isNewInstance = mutex.WaitOne(TimeSpan.Zero, true);

                if (isNewInstance)
                {
                    // 这是第一个实例
                    RunApplication();
                }
                else
                {
                    // 已有实例在运行
                    HandleExistingInstance();
                }
            }
            catch (AbandonedMutexException ex)
            {
                FileUtils.LogError($"处理进程时出错: {ex.Message}", "启动项目时出错，直接起一个新的");
                // 处理前一个实例异常退出的情况
                isNewInstance = true;
                Thread.Sleep(1500);
                HandleExistingInstance();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"启动时发生未预期错误: {ex}", "StartupError");
                MessageBox.Show("程序启动失败，请查看日志。", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                // 只有真正获得 Mutex 的实例才需要释放
                if (isNewInstance && mutex != null)
                {
                    try
                    {
                        mutex.ReleaseMutex(); // 安全释放
                    }
                    catch (Exception ex)
                    {
                        // 忽略释放异常（如已释放）
                        FileUtils.LogError($"释放 Mutex 时出错: {ex}", "MutexRelease");
                    }
                    mutex.Dispose();
                }
            }

        }

        private static void RunApplication()
        {
            // 安全删除文件
            FileUtils.DeleteFile(closePath);
            FileUtils.log("完成", "启动");

            // 后台预加载机器码，避免首次调用卡顿
            System.Threading.Tasks.Task.Run(() => SystemUtils.GenerateMachineCode());

            // 后台预热本地 ASR 模型，提前下载模型文件
            ModelManager.WarmupAsync();

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new FormMain());
            // 使用自定义 ApplicationContext
            //var context = new AfpAppContext();
            //Application.Run(context);
        }

        private static void HandleExistingInstance()
        {
            Process currentProcess = Process.GetCurrentProcess();
            string currentProcessPath = currentProcess.MainModule?.FileName;

            foreach (Process process in Process.GetProcessesByName(currentProcess.ProcessName))
            {
                if (process.Id == currentProcess.Id) continue;

                try
                {
                    // 验证是否是同一个程序
                    string targetProcessPath = process.MainModule?.FileName;
                    if (targetProcessPath != currentProcessPath) continue;

                    if (File.Exists(closePath))
                    {
                        // 关闭之前的进程
                        process.Kill();
                        process.WaitForExit(5000);
                        FileUtils.log("完成", "kill");

                        // 关闭后重新启动当前实例
                        RunApplication();
                        return;
                    }
                    else
                    {
                        // 激活已有窗口
                        IntPtr mainWindowHandle = process.MainWindowHandle;

                        if (mainWindowHandle != IntPtr.Zero)
                        {
                            if (IsIconic(mainWindowHandle))
                            {
                                ShowWindowAsync(mainWindowHandle, SW_RESTORE);
                            }
                            else
                            {
                                ShowWindowAsync(mainWindowHandle, SW_SHOW);
                            }

                            SetForegroundWindow(mainWindowHandle);
                        }

                        // 退出当前实例
                        Environment.Exit(0);
                        return;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"处理进程时出错: {ex.Message}", "HandleExistingInstance");
                    continue;
                }
            }

            // 如果没有找到现有实例，就运行当前实例
            RunApplication();
        }

        private static void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            // 记录全局未捕捉的非 UI 线程异常
            var ex = (Exception)e.ExceptionObject;
            FileUtils.LogError($"{ex.Message}{Environment.NewLine}{ex.StackTrace}", "全局未捕捉的非 UI 线程异常");
        }

        private static void Application_ThreadException(object sender, ThreadExceptionEventArgs e)
        {
            // 记录全局未捕获的 UI 线程异常
            FileUtils.LogError($"{e.Exception.Message}{Environment.NewLine}{e.Exception.StackTrace}", "未捕获的 UI 线程异常");
        }
        private static void ShowError(Exception ex)
        {
            // 在UI线程中显示错误
            if (Application.OpenForms.Count > 0)
            {
                var splash = Application.OpenForms[0] as SplashScreen;
                if (splash != null)
                {
                    splash.Invoke(new Action(() =>
                    {
                        splash.Close();
                        MessageBox.Show($"加载失败: {ex.Message}", "错误",
                            MessageBoxButtons.OK, MessageBoxIcon.Error);
                        Application.Exit();
                    }));
                }
            }
        }

        /// <summary>
        /// 获取当前客户端版本
        /// </summary>
        /// <returns></returns>
        private static string GetClientVersion()
        {
            var path = Path.GetFullPath("script\\clientVersion");
            if (!File.Exists(path))
            {
                return ClientVersion.Replay;
            }

            var readAllText = File.ReadAllText(path);
            if (string.IsNullOrEmpty(readAllText) || string.IsNullOrEmpty(readAllText.Trim()))
            {
                return ClientVersion.Replay;
            }

            var version = readAllText.Trim();
            if (version.Equals(ClientVersion.Replay) || version.Equals(ClientVersion.Record))
            {
                return version;
            }

            return ClientVersion.Replay;
        }

        /// <summary>
        /// 注册httpClient服务
        /// </summary>
        /// <param name="services"></param>
        //private static void ConfigureServices(IServiceCollection services)
        //{
        //    // 注册 IHttpClientFactory
        //    services.AddHttpClient();
        //}

    }
}
