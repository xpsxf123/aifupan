using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 进程工具类 - 使用CreateProcess API + STARTUPINFOEX精确控制句柄继承
    /// </summary>
    public class WebSocketProcessUtils
    {
        #region Win32 API 声明

        [StructLayout(LayoutKind.Sequential)]
        public struct SECURITY_ATTRIBUTES
        {
            public int nLength;
            public IntPtr lpSecurityDescriptor;
            public int bInheritHandle;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        public struct STARTUPINFO
        {
            public int cb;
            public string lpReserved;
            public string lpDesktop;
            public string lpTitle;
            public int dwX;
            public int dwY;
            public int dwXSize;
            public int dwYSize;
            public int dwXCountChars;
            public int dwYCountChars;
            public int dwFillAttribute;
            public int dwFlags;
            public short wShowWindow;
            public short cbReserved2;
            public IntPtr lpReserved2;
            public IntPtr hStdInput;
            public IntPtr hStdOutput;
            public IntPtr hStdError;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        public struct STARTUPINFOEX
        {
            public STARTUPINFO StartupInfo;
            public IntPtr lpAttributeList;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct PROCESS_INFORMATION
        {
            public IntPtr hProcess;
            public IntPtr hThread;
            public int dwProcessId;
            public int dwThreadId;
        }

        private const int STARTF_USESTDHANDLES = 0x00000100;
        private const int STARTF_USESHOWWINDOW = 0x00000001;
        private const short SW_HIDE = 0;
        private const int CREATE_NO_WINDOW = 0x08000000;
        private const int EXTENDED_STARTUPINFO_PRESENT = 0x00080000;
        private const int HANDLE_FLAG_INHERIT = 0x00000001;
        private const int PROC_THREAD_ATTRIBUTE_HANDLE_LIST = 0x00020002;

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool CreatePipe(
            out IntPtr hReadPipe,
            out IntPtr hWritePipe,
            ref SECURITY_ATTRIBUTES lpPipeAttributes,
            int nSize);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool SetHandleInformation(IntPtr hObject, int dwMask, int dwFlags);

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
        private static extern bool CreateProcess(
            string lpApplicationName,
            StringBuilder lpCommandLine,
            IntPtr lpProcessAttributes,
            IntPtr lpThreadAttributes,
            bool bInheritHandles,
            int dwCreationFlags,
            IntPtr lpEnvironment,
            string lpCurrentDirectory,
            ref STARTUPINFOEX lpStartupInfo,
            out PROCESS_INFORMATION lpProcessInformation);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool InitializeProcThreadAttributeList(
            IntPtr lpAttributeList,
            int dwAttributeCount,
            int dwFlags,
            ref IntPtr lpSize);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool UpdateProcThreadAttribute(
            IntPtr lpAttributeList,
            int dwFlags,
            IntPtr Attribute,
            IntPtr lpValue,
            IntPtr cbSize,
            IntPtr lpPreviousValue,
            IntPtr lpReturnSize);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool DeleteProcThreadAttributeList(IntPtr lpAttributeList);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool CloseHandle(IntPtr hObject);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool TerminateProcess(IntPtr hProcess, int uExitCode);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern int WaitForSingleObject(IntPtr hHandle, int dwMilliseconds);

        [DllImport("kernel32.dll", SetLastError = true)]
        private static extern bool GetExitCodeProcess(IntPtr hProcess, out int lpExitCode);

        private const int STILL_ACTIVE = 259;

        #endregion

        /// <summary>
        /// 子进程信息封装
        /// </summary>
        public class ChildProcessInfo : IDisposable
        {
            public IntPtr ProcessHandle { get; set; }
            public IntPtr ThreadHandle { get; set; }
            public int ProcessId { get; set; }
            public IntPtr StdOutReadHandle { get; set; }
            public IntPtr StdErrReadHandle { get; set; }
            public StreamReader StdOutReader { get; set; }
            public StreamReader StdErrReader { get; set; }
            public Thread OutputReadThread { get; set; }
            public Thread ErrorReadThread { get; set; }

            private volatile bool _isDisposed;
            public bool IsDisposed => _isDisposed;

            /// <summary>
            /// 检查进程是否已退出
            /// </summary>
            public bool HasExited
            {
                get
                {
                    if (ProcessHandle == IntPtr.Zero) return true;
                    int exitCode;
                    if (GetExitCodeProcess(ProcessHandle, out exitCode))
                    {
                        return exitCode != STILL_ACTIVE;
                    }
                    return true;
                }
            }

            /// <summary>
            /// 终止进程
            /// </summary>
            public void Kill()
            {
                if (ProcessHandle != IntPtr.Zero && !HasExited)
                {
                    TerminateProcess(ProcessHandle, 0);
                }
            }

            /// <summary>
            /// 等待进程退出
            /// </summary>
            public void WaitForExit(int milliseconds = -1)
            {
                if (ProcessHandle != IntPtr.Zero)
                {
                    WaitForSingleObject(ProcessHandle, milliseconds);
                }
            }

            public void Dispose()
            {
                if (_isDisposed) return;
                _isDisposed = true;

                try
                {
                    Kill();
                    WaitForExit(1000);
                }
                catch { }

                try { StdOutReader?.Close(); } catch { }
                try { StdErrReader?.Close(); } catch { }

                if (StdOutReadHandle != IntPtr.Zero)
                {
                    CloseHandle(StdOutReadHandle);
                    StdOutReadHandle = IntPtr.Zero;
                }
                if (StdErrReadHandle != IntPtr.Zero)
                {
                    CloseHandle(StdErrReadHandle);
                    StdErrReadHandle = IntPtr.Zero;
                }
                if (ProcessHandle != IntPtr.Zero)
                {
                    CloseHandle(ProcessHandle);
                    ProcessHandle = IntPtr.Zero;
                }
                if (ThreadHandle != IntPtr.Zero)
                {
                    CloseHandle(ThreadHandle);
                    ThreadHandle = IntPtr.Zero;
                }
            }
        }

        /// <summary>
        /// 启动子进程，使用STARTUPINFOEX精确指定只继承管道句柄（不继承其他任何句柄如视频文件）
        /// </summary>
        /// <param name="exePath">可执行文件路径</param>
        /// <param name="arguments">命令行参数</param>
        /// <param name="onOutputReceived">接收stdout输出的回调</param>
        /// <param name="onErrorReceived">接收stderr输出的回调（可选）</param>
        /// <returns>子进程信息对象</returns>
        public static ChildProcessInfo StartProcessWithoutInheritHandles(
            string exePath,
            string arguments,
            Action<string> onOutputReceived,
            Action<string> onErrorReceived = null)
        {
            IntPtr stdOutReadHandle = IntPtr.Zero;
            IntPtr stdOutWriteHandle = IntPtr.Zero;
            IntPtr stdErrReadHandle = IntPtr.Zero;
            IntPtr stdErrWriteHandle = IntPtr.Zero;
            IntPtr lpAttributeList = IntPtr.Zero;
            IntPtr handleListPtr = IntPtr.Zero;
            ChildProcessInfo processInfo = null;

            try
            {
                // 创建管道的安全属性 - 设置为可继承
                SECURITY_ATTRIBUTES sa = new SECURITY_ATTRIBUTES();
                sa.nLength = Marshal.SizeOf(sa);
                sa.lpSecurityDescriptor = IntPtr.Zero;
                sa.bInheritHandle = 1; // TRUE - 管道句柄可继承

                // 创建stdout管道
                if (!CreatePipe(out stdOutReadHandle, out stdOutWriteHandle, ref sa, 0))
                {
                    throw new Exception($"创建stdout管道失败: {Marshal.GetLastWin32Error()}");
                }

                // 设置读取端为不可继承（只有写入端给子进程继承）
                if (!SetHandleInformation(stdOutReadHandle, HANDLE_FLAG_INHERIT, 0))
                {
                    throw new Exception($"设置stdout读取端句柄属性失败: {Marshal.GetLastWin32Error()}");
                }

                // 创建stderr管道
                if (!CreatePipe(out stdErrReadHandle, out stdErrWriteHandle, ref sa, 0))
                {
                    throw new Exception($"创建stderr管道失败: {Marshal.GetLastWin32Error()}");
                }

                // 设置读取端为不可继承
                if (!SetHandleInformation(stdErrReadHandle, HANDLE_FLAG_INHERIT, 0))
                {
                    throw new Exception($"设置stderr读取端句柄属性失败: {Marshal.GetLastWin32Error()}");
                }

                // ======= 关键：使用STARTUPINFOEX精确指定要继承的句柄列表 =======
                
                // 获取AttributeList所需大小
                IntPtr lpSize = IntPtr.Zero;
                InitializeProcThreadAttributeList(IntPtr.Zero, 1, 0, ref lpSize);
                if (lpSize == IntPtr.Zero)
                {
                    throw new Exception($"获取AttributeList大小失败: {Marshal.GetLastWin32Error()}");
                }

                // 分配AttributeList内存
                lpAttributeList = Marshal.AllocHGlobal(lpSize);
                if (!InitializeProcThreadAttributeList(lpAttributeList, 1, 0, ref lpSize))
                {
                    throw new Exception($"初始化AttributeList失败: {Marshal.GetLastWin32Error()}");
                }

                // 创建要继承的句柄数组（只包含管道写入端）
                IntPtr[] handleList = new IntPtr[] { stdOutWriteHandle, stdErrWriteHandle };
                int handleListSize = IntPtr.Size * handleList.Length;
                handleListPtr = Marshal.AllocHGlobal(handleListSize);
                Marshal.Copy(handleList, 0, handleListPtr, handleList.Length);

                // 设置PROC_THREAD_ATTRIBUTE_HANDLE_LIST - 精确指定只继承这些句柄
                if (!UpdateProcThreadAttribute(
                    lpAttributeList,
                    0,
                    (IntPtr)PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
                    handleListPtr,
                    (IntPtr)handleListSize,
                    IntPtr.Zero,
                    IntPtr.Zero))
                {
                    throw new Exception($"设置句柄继承列表失败: {Marshal.GetLastWin32Error()}");
                }

                // 设置启动信息
                STARTUPINFOEX siEx = new STARTUPINFOEX();
                siEx.StartupInfo.cb = Marshal.SizeOf(siEx);
                siEx.StartupInfo.dwFlags = STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
                siEx.StartupInfo.wShowWindow = SW_HIDE;
                siEx.StartupInfo.hStdInput = IntPtr.Zero;
                siEx.StartupInfo.hStdOutput = stdOutWriteHandle;
                siEx.StartupInfo.hStdError = stdErrWriteHandle;
                siEx.lpAttributeList = lpAttributeList;

                PROCESS_INFORMATION pi;

                // 构建命令行
                StringBuilder commandLine = new StringBuilder();
                commandLine.Append("\"").Append(exePath).Append("\"");
                if (!string.IsNullOrEmpty(arguments))
                {
                    commandLine.Append(" ").Append(arguments);
                }

                // 获取工作目录
                string workingDir = Path.GetDirectoryName(exePath);

                // 启动进程 - 使用EXTENDED_STARTUPINFO_PRESENT标志，只继承指定的句柄
                bool success = CreateProcess(
                    null,
                    commandLine,
                    IntPtr.Zero,
                    IntPtr.Zero,
                    true,  // 必须为true才能继承句柄，但只会继承HANDLE_LIST中指定的
                    CREATE_NO_WINDOW | EXTENDED_STARTUPINFO_PRESENT,
                    IntPtr.Zero,
                    workingDir,
                    ref siEx,
                    out pi);

                if (!success)
                {
                    throw new Exception($"CreateProcess失败: {Marshal.GetLastWin32Error()}");
                }

                // 清理AttributeList
                DeleteProcThreadAttributeList(lpAttributeList);
                Marshal.FreeHGlobal(lpAttributeList);
                lpAttributeList = IntPtr.Zero;
                Marshal.FreeHGlobal(handleListPtr);
                handleListPtr = IntPtr.Zero;

                // 关闭子进程端的管道句柄（父进程不需要）
                CloseHandle(stdOutWriteHandle);
                stdOutWriteHandle = IntPtr.Zero;
                CloseHandle(stdErrWriteHandle);
                stdErrWriteHandle = IntPtr.Zero;

                // 创建进程信息对象
                processInfo = new ChildProcessInfo
                {
                    ProcessHandle = pi.hProcess,
                    ThreadHandle = pi.hThread,
                    ProcessId = pi.dwProcessId,
                    StdOutReadHandle = stdOutReadHandle,
                    StdErrReadHandle = stdErrReadHandle
                };

                // 创建StreamReader用于读取管道
                var stdOutStream = new FileStream(
                    new Microsoft.Win32.SafeHandles.SafeFileHandle(stdOutReadHandle, false),
                    FileAccess.Read, 4096, false);
                processInfo.StdOutReader = new StreamReader(stdOutStream, Encoding.UTF8);

                var stdErrStream = new FileStream(
                    new Microsoft.Win32.SafeHandles.SafeFileHandle(stdErrReadHandle, false),
                    FileAccess.Read, 4096, false);
                processInfo.StdErrReader = new StreamReader(stdErrStream, Encoding.UTF8);

                // 启动后台线程读取stdout
                processInfo.OutputReadThread = new Thread(() =>
                {
                    try
                    {
                        string line;
                        while (!processInfo.IsDisposed && (line = processInfo.StdOutReader.ReadLine()) != null)
                        {
                            onOutputReceived?.Invoke(line);
                        }
                    }
                    catch (Exception ex)
                    {
                        if (!processInfo.IsDisposed)
                        {
                            FileUtils.LogError($"读取子进程stdout异常: {ex.Message}");
                        }
                    }
                });
                processInfo.OutputReadThread.IsBackground = true;
                processInfo.OutputReadThread.Start();

                // 启动后台线程读取stderr
                if (onErrorReceived != null)
                {
                    processInfo.ErrorReadThread = new Thread(() =>
                    {
                        try
                        {
                            string line;
                            while (!processInfo.IsDisposed && (line = processInfo.StdErrReader.ReadLine()) != null)
                            {
                                onErrorReceived?.Invoke(line);
                            }
                        }
                        catch (Exception ex)
                        {
                            if (!processInfo.IsDisposed)
                            {
                                FileUtils.LogError($"读取子进程stderr异常: {ex.Message}");
                            }
                        }
                    });
                    processInfo.ErrorReadThread.IsBackground = true;
                    processInfo.ErrorReadThread.Start();
                }

                return processInfo;
            }
            catch (Exception)
            {
                // 清理资源
                if (lpAttributeList != IntPtr.Zero)
                {
                    DeleteProcThreadAttributeList(lpAttributeList);
                    Marshal.FreeHGlobal(lpAttributeList);
                }
                if (handleListPtr != IntPtr.Zero) Marshal.FreeHGlobal(handleListPtr);
                if (stdOutReadHandle != IntPtr.Zero) CloseHandle(stdOutReadHandle);
                if (stdOutWriteHandle != IntPtr.Zero) CloseHandle(stdOutWriteHandle);
                if (stdErrReadHandle != IntPtr.Zero) CloseHandle(stdErrReadHandle);
                if (stdErrWriteHandle != IntPtr.Zero) CloseHandle(stdErrWriteHandle);
                processInfo?.Dispose();
                throw;
            }
        }
    }
}
