using CefSharp;
using Microsoft.Win32;
using Newtonsoft.Json;
using ReviewAnalysis.Global;
using ReviewAnalysis.HttpServer;
using ReviewAnalysis.LogConsole;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using System;
using System.CodeDom.Compiler;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net.Http;
using System.Security.AccessControl;
using System.Security.Policy;
using System.Security.Principal;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace douyin.Utils
{
    public static class FileUtils
    {

        /// <summary>
        /// 将内容写入到文本文件
        /// </summary>
        /// <param name="filePath">文件路径</param>
        /// <param name="content">内容</param>
        public static void WriteTxtFile(string filePath, string content)
        {
           
            // 确保文件夹存在
            string folderPath = Path.GetDirectoryName(filePath);
            if (!Directory.Exists(folderPath))
            {
                Directory.CreateDirectory(folderPath);
            }
            File.WriteAllText(filePath, content);
        }

        /// <summary>
        /// 打印堆栈信息
        /// </summary>
        /// <returns></returns>
        public static string GetFormattedStack()
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                StackTrace trace = new StackTrace(true);
                foreach (StackFrame frame in trace.GetFrames())
                {
                    sb.AppendLine($"在 {frame.GetFileName()} 的第 {frame.GetFileLineNumber()} 行");
                    sb.AppendLine($"调用方法: {frame.GetMethod()?.DeclaringType?.Name}.{frame.GetMethod()?.Name}");
                }
                return sb.ToString();
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"打印堆栈报错");
            }

            return "";
        }

        /// <summary>
        /// 检测路径是否存在
        /// </summary>
        /// <param name="filePath"></param>
        /// <returns></returns>
        public static bool isExits(string filePath,out string absolutePath) 
        {
            // 将相对路径转换为绝对路径
            absolutePath = Path.GetFullPath(filePath);
            if (!Directory.Exists(absolutePath)) {
                return false;
            }
            return true;
        }

        /// <summary>
        /// 查询是否存在文件
        /// </summary>
        /// <param name="filePath">文件的相对路径</param>
        /// <param name="absolutePath">文件的绝对路径</param>
        /// <returns></returns>
        public static bool isExitsFile(string filePath, out string absolutePath) 
        {
            absolutePath = Path.GetFullPath(filePath);
            if (!File.Exists(absolutePath))
            {
                return false;
            }
            return true;
        }

        /// <summary>
        /// 创建
        /// </summary>
        /// <param name="filePath"></param>
        /// <returns></returns>
        public static bool createDirectory(string filePath) 
        {
            string absolutePath = "";
            if (isExits(filePath, out absolutePath)) 
            {
                return true;
            }
            try
            {
                Directory.CreateDirectory(absolutePath);
                return true;
            }
            catch (Exception e)
            {
                LogError(e.Message, "createDirectory");
                return false;
            }
            
        }

        /// <summary>
        /// 创建文件，路径不存在则创建
        /// </summary>
        /// <param name="filePath">文件路径</param>
        public static void FileCreate(string filePath)
        {
            // 获取文件夹路径
            string directoryPath = Path.GetDirectoryName(filePath);

            // 检查文件夹是否存在，如果不存在则创建
            if (!Directory.Exists(directoryPath))
            {
                Directory.CreateDirectory(directoryPath);
            }

            // 检查文件是否存在，如果不存在则创建空文件
            if (!File.Exists(filePath))
            {
                // 创建一个新的空文件
                using (FileStream fs = File.Create(filePath))
                {
                    log($"文件 '{filePath}' 不存在，已创建文件。", "FileCreate");
                }
            }
        }

        /// <summary>
        /// 递归删除指定目录下的所有文件和子目录。
        /// </summary>
        /// <param name="directoryPath">要清理的目录的路径。</param>
        public static void DeleteAllFilesAndSubdirectories(string directoryPath)
        {
            try
            {
                // 检查目录是否存在
                if (Directory.Exists(directoryPath))
                {
                    // 删除所有文件
                    string[] files = Directory.GetFiles(directoryPath);
                    foreach (string file in files)
                    {
                        File.Delete(file);
                    }

                    // 删除所有子目录
                    string[] subdirectories = Directory.GetDirectories(directoryPath);
                    foreach (string subdirectory in subdirectories)
                    {
                        DeleteAllFilesAndSubdirectories(subdirectory);
                        Directory.Delete(subdirectory, true);
                    }
                }
            }
            catch (Exception ex)
            {
                // 处理可能出现的异常，如权限问题等
                LogError($"删除文件和目录失败: {ex.Message}", "DeleteAllFilesAndSubdirectories");
            }
        }

        public static void DeleteFile(string filePath)
        {
            // 安全删除文件
            try
            {
                if (File.Exists(filePath))
                {
                    File.Delete(filePath);
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"删除文件失败: {ex.Message}");
                LogError($"删除文件失败: {ex.Message}", "文件删除失败");
            }
        }


        /// <summary>
        /// 获取指定盘符的磁盘大小信息。
        /// </summary>
        /// <param name="driveLetter">盘符字母，例如 "C"。</param>
        public static Dictionary<string,long> GetDiskSize(string driveLetter)
        {
            Dictionary<string, long> result = new Dictionary<string, long>();
            try
            {
              
                // 获取所有驱动器信息
                var drives = DriveInfo.GetDrives();

                // 查找指定盘符的驱动器
                var drive = drives.FirstOrDefault(d => d.Name.StartsWith(driveLetter + ":\\", StringComparison.OrdinalIgnoreCase));

                if (drive != null)
                {
                    //Console.WriteLine($"驱动器: {drive.Name}");
                    //Console.WriteLine($"类型: {drive.DriveType}");

                    // 获取磁盘总大小
                    long totalSize = drive.TotalSize;
                    result.Add("driveTotalSize", totalSize / (1024 * 1024 * 1024));
                    //Console.WriteLine($"总大小: {totalSize / (1024 * 1024 * 1024)} GB");

                    // 获取已使用的空间
                    long usedSpace = drive.TotalSize - drive.AvailableFreeSpace;
                    result.Add("driveUsedSpace", usedSpace / (1024 * 1024 * 1024));
                    //Console.WriteLine($"已使用空间: {usedSpace / (1024 * 1024 * 1024)} GB");

                    // 获取可用的空间
                    long availableSpace = drive.AvailableFreeSpace;
                    result.Add("driveAvailablepace", availableSpace / (1024 * 1024 * 1024));
                    //Console.WriteLine($"可用空间: {availableSpace / (1024 * 1024 * 1024)} GB");
                    return result;
                }
                else
                {
                    log($"未找到指定盘符的驱动器: {driveLetter}", "GetDiskSize");
                    result.Add("driveTotalSize", 0);
                    return result;
                }
            }
            catch (Exception ex)
            {
                LogError($"获取磁盘信息时发生错误: {ex.Message}", "GetDiskSize");
                result.Add("driveTotalSize", 0);
                return result;
            }
        }

        /// <summary>
        /// 把字符串追加到文件中
        /// </summary>
        /// <param name="filePath"></param>
        /// <param name="jsonString"></param>
        public static void AppendJsonToFile(string filePath, string jsonString)
        {
            try
            {
                // 确保文件存在，如果不存在则创建文件
                if (!File.Exists(filePath))
                {
                    File.Create(filePath).Close();
                }

                // 将 JSON 字符串追加到文件中
                File.AppendAllText(filePath, jsonString + Environment.NewLine, Encoding.UTF8);
            }
            catch (Exception ex)
            {
                log($"字符串追加到文件中报错path={filePath}, string={jsonString}");
            }
        }

        /// <summary>
        /// 读取文件的最后一行的整数，不是整数会取上一行
        /// </summary>
        /// <param name="filePath"></param>
        /// <returns></returns>
        public static string GetLastNonEmptyLine(string filePath)
        {
            // 检查文件是否存在
            if (!File.Exists(filePath))
            {
                return null;
            }

            // 使用 StreamReader 逐行读取文件
            string lastNonEmptyLine = null;

            try
            {
                using (StreamReader reader = new StreamReader(filePath))
                {
                    string line;
                    string[] temp = null;
                    while ((line = reader.ReadLine()) != null)
                    {
                        string trimmedLine = line.Trim();
                        if (!string.IsNullOrEmpty(trimmedLine))
                        {
                            temp = trimmedLine.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                            if (temp.Length > 1 && int.TryParse(temp[temp.Length - 1], out int result))
                            {
                                lastNonEmptyLine = trimmedLine; // 更新最后非空行
                            }
                        }
                    }
                }
            }
            catch (IOException)
            {
                // 文件读取过程中出现异常
                return null;
            }

            // 如果找到有效的最后一行，返回它；否则返回 null
            return lastNonEmptyLine;
        }

        public static string GetLastNonEmptyLine2(string filePath)
        {
            // 检查文件是否存在
            if (!File.Exists(filePath))
            {
                return null;
            }

            try
            {
                using (FileStream fs = new FileStream(filePath, FileMode.Open, FileAccess.Read))
                {
                    long position = fs.Length;
                    byte[] buffer = new byte[1];
                    StringBuilder lineBuilder = new StringBuilder();

                    // 从文件末尾逐字节读取
                    while (position > 0)
                    {
                        position--;
                        fs.Seek(position, SeekOrigin.Begin);
                        fs.Read(buffer, 0, 1);

                        char currentChar = (char)buffer[0];
                        if (currentChar == '\n' || currentChar == '\r')
                        {
                            // 找到一行
                            if (lineBuilder.Length > 0)
                            {
                                string line = lineBuilder.ToString().Trim();
                                if (!string.IsNullOrEmpty(line))
                                {
                                    string[] temp = line.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                                    if (temp.Length > 1 && int.TryParse(temp[temp.Length - 1], out int result))
                                    {
                                        return line; // 返回符合条件的最后非空行
                                    }
                                }
                                lineBuilder.Clear();
                            }
                        }
                        else
                        {
                            lineBuilder.Insert(0, currentChar);
                        }
                    }

                    // 处理文件的第一行
                    if (lineBuilder.Length > 0)
                    {
                        string line = lineBuilder.ToString().Trim();
                        if (!string.IsNullOrEmpty(line))
                        {
                            string[] temp = line.Split(new string[] { ">>>" }, StringSplitOptions.RemoveEmptyEntries);
                            if (temp.Length > 1 && int.TryParse(temp[temp.Length - 1], out int result))
                            {
                                return line; // 返回符合条件的最后非空行
                            }
                        }
                    }
                }
            }
            catch (IOException)
            {
                // 文件读取过程中出现异常
                return null;
            }

            // 如果没有找到符合条件的行，返回 null
            return null;
        }

        private static string BaseLogFileName => $"log_{DateTime.Now:yyyyMMdd}";
        private const long MaxFileSize = 10 * 1024 * 1024; // 10MB

        #region 分析日志
        private static object _lockObjectAnalysis = new object();
        private static string LogAnalysisDirectory => Path.Combine("logs", "analysis");

        private static int CurrentLogFileNumberAnalysis = 1;

        public static void LogAnalysis(string content, string action = "默认", bool isDebugger = false)
        {
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            //确保 logs 文件夹存在
            if (!Directory.Exists(LogAnalysisDirectory))
            {
                Directory.CreateDirectory(LogAnalysisDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextAnalysisLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";

            lock (_lockObjectAnalysis)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumberAnalysis++;
                    currentLogFileName = GetNextAnalysisLogFileName();
                }
                // 将消息追加到当前日志文件中
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }
            }
        }

        public static void ZipFileToOneFile(string filePath, string zipFilePath)
        {
            // 创建或覆盖 zip 文件
            using (FileStream zipToCreate = new FileStream(zipFilePath, FileMode.Create))
            using (ZipArchive archive = new ZipArchive(zipToCreate, ZipArchiveMode.Update))
            {
                // 添加文件到 ZIP 文件
                archive.CreateEntryFromFile(filePath, Path.GetFileName(filePath));
            }
        }

        /// <summary>
        /// 将字符串内容直接保存到 zip 文件中的指定文本文件
        /// </summary>
        /// <param name="contentList"></param>
        /// <param name="zipFilePath"></param>
        /// <param name="fileNameInZip"></param>
        public static void SaveListToZip(List<string> contentList, string zipFilePath, string fileNameInZip)
        {
            // 确保输出目录存在
            string outputDirectory = Path.GetDirectoryName(zipFilePath);
            if (!Directory.Exists(outputDirectory))
            {
                Directory.CreateDirectory(outputDirectory);
            }

            // 将 List<string> 转换为字符串，每个元素之间用换行符分隔
            string content = string.Join(Environment.NewLine, contentList);

            // 使用 MemoryStream 创建 ZIP 文件
            using (MemoryStream zipMemoryStream = new MemoryStream())
            {
                // 创建 ZipArchive 并将字符串写入到其中
                using (ZipArchive zip = new ZipArchive(zipMemoryStream, ZipArchiveMode.Create, true))
                {
                    // 创建一个新的文本文件条目
                    var zipEntry = zip.CreateEntry(fileNameInZip);

                    // 打开条目的流并写入内容
                    using (StreamWriter writer = new StreamWriter(zipEntry.Open(), Encoding.UTF8))
                    {
                        writer.Write(content);
                    }
                }

                // 将 MemoryStream 中的 ZIP 内容写入到文件系统中的 ZIP 文件
                File.WriteAllBytes(zipFilePath, zipMemoryStream.ToArray());
            }
        }

        private static string GetNextAnalysisLogFileName()
        {
            return Path.Combine(LogAnalysisDirectory, $"{BaseLogFileName}_{CurrentLogFileNumberAnalysis}.txt");
        }
        #endregion 分析日志



        #region 巨量日志
        private static object _lockObjectRpa = new object();
        private static string LogRpaDirectory => Path.Combine("logs", "rpa");

        private static int CurrentLogFileNumberRpa = 1;

        public static void LogRpa(string content, string action = "默认", bool isDebugger = false)
        {
            // 新逻辑：提交到日志服务，界面实时显示
            LogService.Instance.SubmitLog(LogLevel.Warning, content, action);
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            //确保 logs 文件夹存在
            if (!Directory.Exists(LogRpaDirectory))
            {
                Directory.CreateDirectory(LogRpaDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextRpaLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";

            lock (_lockObjectRpa)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumberRpa++;
                    currentLogFileName = GetNextRpaLogFileName();
                }
                // 将消息追加到当前日志文件中
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }
            }
        }
        private static readonly SemaphoreSlim _asyncLogRpaLock = new SemaphoreSlim(initialCount: 1, maxCount: 1);
        public static Task LogRpaAsync(string content, string action = "默认", bool isDebugger = false)
        {
            return Task.Run(async () =>
            {
                // 内部可嵌套 async/await，但外部方法无 async 修饰
                await _asyncLogRpaLock.WaitAsync();
                try
                {
                    // 新逻辑：提交到日志服务，界面实时显示
                    LogService.Instance.SubmitLog(LogLevel.Warning, content, action);
                    //if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
                    ////确保 logs 文件夹存在
                    //if (!Directory.Exists(LogRpaDirectory))
                    //{
                    //    Directory.CreateDirectory(LogRpaDirectory);
                    //}

                    //// 确定当前的日志文件名
                    //string currentLogFileName = GetNextRpaLogFileName();

                    //// 将消息追加到文件中，自动换行
                    //string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
                    //logMessage += $"动作:{action}\n";
                    //logMessage += $"{content}\n\n";

                    //lock (_lockObjectRpa)
                    //{
                    //    // 检查文件大小，如果超过了最大大小，则创建新的文件
                    //    if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                    //    {
                    //        // 文件大小超过了限制，创建新的文件
                    //        CurrentLogFileNumberRpa++;
                    //        currentLogFileName = GetNextRpaLogFileName();
                    //    }
                    //    // 将消息追加到当前日志文件中
                    //    try
                    //    {
                    //        // 将消息追加到当前日志文件中
                    //        File.AppendAllText(currentLogFileName, logMessage);
                    //    }
                    //    catch (IOException ex)
                    //    {
                    //        Console.WriteLine($"日志文件写入失败，失败信息:{ex.Message.ToString()}");
                    //    }
                    //}

                }
                finally
                {
                    _asyncLogRpaLock.Release();
                }
            });
        }

        private static string GetNextRpaLogFileName()
        {
            return Path.Combine(LogRpaDirectory, $"{BaseLogFileName}_{CurrentLogFileNumberRpa}.txt");
        }
        #endregion 巨量日志



        #region 录制日志
        private static object _lockObjectRecord = new object();
        private static string LogRecordDirectory => Path.Combine("logs", "record");
      
        private static int CurrentLogFileNumberRecord = 1;
      
        public static void LogRecrd(string content, string action = "默认", bool isDebugger = false)
        {
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            //确保 logs 文件夹存在
            if (!Directory.Exists(LogRecordDirectory))
            {
                Directory.CreateDirectory(LogRecordDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextRecordLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";

            lock (_lockObjectRecord)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumberRecord++;
                    currentLogFileName = GetNextRecordLogFileName();
                }
                // 将消息追加到当前日志文件中
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }
            }
        }

        private static string GetNextRecordLogFileName()
        {
            return Path.Combine(LogRecordDirectory, $"{BaseLogFileName}_{CurrentLogFileNumberRecord}.txt");
        }
        #endregion 录制日志

        #region Http请求日志

        private static object _lockObjectHttp = new object();
        private static string LogHttpDirectory => Path.Combine("logs", "http");

        private static int CurrentLogFileNumberHttp = 1;

        public static void LogHttp(string content, string action = "默认", bool isDebugger = false)
        {
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            //确保 logs 文件夹存在
            if (!Directory.Exists(LogHttpDirectory))
            {
                Directory.CreateDirectory(LogHttpDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextHttpLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";

            lock (_lockObjectHttp)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumberHttp++;
                    currentLogFileName = GetNextHttpLogFileName();
                }
                // 将消息追加到当前日志文件中
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }
            }
        }

        private static string GetNextHttpLogFileName()
        {
            return Path.Combine(LogHttpDirectory, $"{BaseLogFileName}_{CurrentLogFileNumberHttp}.txt");
        }

        #endregion

        #region 其他日志
        private static object _lockObject = new object();
        private static string LogFilePath => Path.Combine("logs");

        private static int CurrentLogFileNumber = 1;
        public static void log(string content, string action = "默认", bool isDebugger = false)
        {
            // 新逻辑：提交到日志服务，界面实时显示
            LogService.Instance.SubmitLog(LogLevel.Info, content, action);
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            // 确保 logs 文件夹存在
            string logDirectory = "logs";
            if (!Directory.Exists(logDirectory))
            {
                Directory.CreateDirectory(logDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";
            lock (_lockObject)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumber++;
                    currentLogFileName = GetNextLogFileName();
                }
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }
            }

        }

        private static string GetNextLogFileName()
        {
            return Path.Combine(LogFilePath, $"{BaseLogFileName}_{CurrentLogFileNumber}.txt");
        }
        #endregion

        #region 错误日志
        private static object _lockObjectError = new object();
        private static string LogErrorDirectory => Path.Combine("logs", "error");

        private static int CurrentLogFileNumberError = 1;

        public static void LogError(string content, string action = "默认", bool isDebugger = false)
        {
            LogService.Instance.SubmitLog(LogLevel.Error, content, action);
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            //确保 logs 文件夹存在
            if (!Directory.Exists(LogErrorDirectory))
            {
                Directory.CreateDirectory(LogErrorDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextErrorLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";

            lock (_lockObjectError)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumberError++;
                    currentLogFileName = GetNextErrorLogFileName();
                }
                // 将消息追加到当前日志文件中
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }
            }
        }

        private static string GetNextErrorLogFileName()
        {
            return Path.Combine(LogErrorDirectory, $"{BaseLogFileName}_{CurrentLogFileNumberError}.txt");
        }
        #endregion 错误日志

        #region 异步日志方法
        private static readonly SemaphoreSlim _asyncLogLock = new SemaphoreSlim(initialCount: 1, maxCount: 1);
        private static readonly SemaphoreSlim _asyncLogErrorLock = new SemaphoreSlim(initialCount: 1, maxCount: 1);

        public static Task LogAsync(string content, string action = "默认", bool isDebugger = false)
        {
            return Task.Run(() =>
            {
                log(content, action, isDebugger);
            });
        }

        public static Task LogErrorAsync(string content, string action = "默认", bool isDebugger = false)
        {
            return Task.Run(() =>
            {
                LogError(content, action, isDebugger);
            });
        }
        #endregion 异步日志方法

        #region ffmpeg日志
        private static object _lockObjectFFmpeg = new object();
        private static string LogFFmpegDirectory => Path.Combine("logs", "ffmpeg");

        private static int CurrentLogFileNumberFFmpeg = 1;

        public static void LogFFmpeg(string content, string action = "默认", bool isDebugger = false)
        {
            if (isDebugger && !(isDebugger && Constant.GetDebuggerEnv())) return;
            // 确保 logs 文件夹存在
            if (!Directory.Exists(LogFFmpegDirectory))
            {
                Directory.CreateDirectory(LogFFmpegDirectory);
            }

            // 确定当前的日志文件名
            string currentLogFileName = GetNextFFmpegLogFileName();

            // 将消息追加到文件中，自动换行
            string logMessage = $"{DateTime.Now:HH:mm:ss}\n";
            logMessage += $"动作:{action}\n";
            logMessage += $"{content}\n\n";

            lock (_lockObjectFFmpeg)
            {
                // 检查文件大小，如果超过了最大大小，则创建新的文件
                if (File.Exists(currentLogFileName) && new FileInfo(currentLogFileName).Length >= MaxFileSize)
                {
                    // 文件大小超过了限制，创建新的文件
                    CurrentLogFileNumberFFmpeg++;
                    currentLogFileName = GetNextFFmpegLogFileName();
                }
                try
                {
                    // 将消息追加到当前日志文件中
                    File.AppendAllText(currentLogFileName, logMessage, Encoding.UTF8);
                }
                catch (IOException ex)
                {
                    // 日志文件写入失败，忽略Console输出避免特殊字符闪退
                }

            }
        }

        private static string GetNextFFmpegLogFileName()
        {
            return Path.Combine(LogFFmpegDirectory, $"{BaseLogFileName}_{CurrentLogFileNumberFFmpeg}.txt");
        }
        #endregion

        #region  文件下载


        public static async void DownLoadUpdateFile(string downFileUrl,string token) 
        {
            // 指定的本地文件夹路径
            string destinationFolder = @"updateTemp";

            // 设置请求头部的 token
            //string token = "adfd995ea6e54072bb76f62ceb50dcec";

            // 确保目标文件夹存在
            EnsureDirectoryExists(destinationFolder);

            // 获取文件名
            string fileName = Path.GetFileName(new Uri(downFileUrl).LocalPath);
            string[] fileNameArray=fileName.Split('.');
            string endFileName = "servicePack" + fileNameArray[fileNameArray.Length - 1];
            // 完整的目标文件路径
            string destinationFilePath = Path.Combine(destinationFolder, endFileName);

            // 使用 HttpClient 下载文件
            await DownloadFileAsync(downFileUrl, destinationFilePath, token);
        }

        /// <summary>
        /// 确保指定的文件夹存在，如果不存在则创建。
        /// </summary>
        /// <param name="folderPath">文件夹路径</param>
        private static void EnsureDirectoryExists(string folderPath)
        {
            if (!Directory.Exists(folderPath))
            {
                Directory.CreateDirectory(folderPath);
            }
        }

        /// <summary>
        /// 异步下载文件并保存到本地。
        /// </summary>
        /// <param name="url">文件的 URL 地址</param>
        /// <param name="destinationFilePath">本地文件的完整路径</param>
        /// <param name="token">自定义的 token</param>
        /// <returns>异步任务</returns>
        public static async Task DownloadFileAsync(string url, string destinationFilePath, string token)
        {
            using (var httpClient = new HttpClient())
            {
                if (token!="") 
                {
                    // 设置请求头部的自定义 token 键名和值
                    httpClient.DefaultRequestHeaders.Add("token", token);
                }
                // 发送 GET 请求获取文件内容
                using (var responseMessage = await httpClient.GetAsync(url, HttpCompletionOption.ResponseHeadersRead))
                {
                    responseMessage.EnsureSuccessStatusCode(); // 确保 HTTP 响应状态码为成功

                    // 将响应体的内容写入本地文件
                    using (var fileStream = new FileStream(destinationFilePath, FileMode.Create, FileAccess.Write, FileShare.None))
                    {
                        await responseMessage.Content.CopyToAsync(fileStream);
                        log("下载完成", "DownLoadUpdateFile");
                    }
                }
            }
        }
        #endregion

        #region  文件解压
        /// <summary>
        /// 文件解压
        /// </summary>
        /// <param name="compressedFile">压缩包的全路径</param>
        /// <param name="compressedPath">解压目录的全路径</param>
        public static void Extract(string compressedFile, string compressedPath) 
        {
            if (Directory.Exists(compressedPath))
            {
                Directory.Delete(compressedPath, true);

            }
            Directory.CreateDirectory(compressedPath);
            if (!File.Exists(compressedFile))
            {
                log("没有找到可以解压的文件包", "加压文件");
                return;
            }
            ZipFile.ExtractToDirectory(compressedFile, compressedPath);
        }


        /// <summary>
        /// 调用更新客户端
        /// </summary>
        /// <param name="cmd"></param>
        /// <returns></returns>
        public static string CallUpdate(string cmd)
        {
            string callUrl = "http://127.0.0.1:5002/" + cmd;
            WebClient webClient = new WebClient();
            string result = webClient.GetHtml(callUrl);
            return result;
        }

        /// <summary>
        /// 检测是否有更新文件的压缩包存在，如果存在解压，并且将文件移动到updateExe文件夹下
        /// </summary>
        public static void UpdateReviewAnalysis() 
        {
            try
            {
                string updateServicePackPath = @"updateTemp\updateServicePack.zip";
                if (File.Exists(updateServicePackPath))
                {
                    string compressedFile = Path.GetFullPath(updateServicePackPath);
                    string compressedPath = Path.GetFullPath("updateTemp\\updateServicePack");
                    //解压到updateTemp\\updateServicePack路径
                    Extract(compressedFile, compressedPath);
                    string updateExeFile= compressedPath + "\\ReviewAnalysisUpdate.exe";
                    string updateExePath=Path.GetFullPath("updateExe");
                    if (!File.Exists(updateExeFile)) 
                    {
                        return;
                    }
                    if (!Directory.Exists(updateExePath)) 
                    {
                        Directory.CreateDirectory(updateExePath);
                    }
                    if (File.Exists(updateExeFile)) 
                    {
                        Thread.Sleep(1000);
                        string moveFile= updateExePath + "\\ReviewAnalysisUpdate.exe";
                        if (File.Exists(moveFile)) 
                        {
                            //删除已经存在的要移动的文件
                            File.Delete(moveFile);
                        }
                        File.Move(updateExeFile, moveFile);
                        //移动成功后，删除压缩文件
                        //File.Delete(compressedFile);
                        //删除已经存在的加压出来的文件夹
                       Directory.Delete(compressedPath, true);
                    }
                }
            }
            catch (Exception ex) 
            {
                log($"错误信息:{ex.Message}", "检测是否有更新文件存在");
            }
        }

        /// <summary>
        /// 打开更新监听的软件
        /// </summary>
        /// <param name="type">0：爱复盘主程序，1：补丁包</param>
        public static void StartUpdate(int type = 0)
        {
            string absolutePath = Path.GetFullPath("updateExe");
            string updateExePath = absolutePath + "\\ReviewAnalysisUpdate.exe";
            FileUtils.log($"启动更新程序，type={type}");
            if (File.Exists(updateExePath))
            {
                {
                    ProcessStartInfo startInfo = new ProcessStartInfo
                    {
                        FileName = updateExePath,
                        UseShellExecute = true,
                        Verb = "runas",
                        Arguments = "" + type,
                        WorkingDirectory = absolutePath // B 软件所在的目录
                    };
                    Process.Start(startInfo);
                    if(startInfo != null)
                    {
                        FileUtils.log("更新程序启动成功！，退出当前程序");
                        // 在UI线程调用Cef.Shutdown，避免跨线程异常
                        Environment.Exit(0);
                    }
                    else
                    {
                        FileUtils.log("更新程序启动失败！！！");
                    }
                }
            }
            else
            {
                // 显示询问框
                DialogResult result = MessageBox.Show(
                    "缺失‘更新客户端’，这将会影响软件的更新，点击确定直接下载‘更新客户端’，您确定要继续吗？",
                    "确认提示",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Question);

                // 根据用户的回答执行相应的逻辑
                if (result == DialogResult.Yes)
                {
                    PerformNextStep();
                }
            }
        }

        /// <summary>
        /// 执行bat文件
        /// </summary>
        /// <param name="batFilePath"></param>
        public static void Execute(string batFilePath)
        {
            ProcessStartInfo processStartInfo = new ProcessStartInfo
            {
                FileName = batFilePath,
                UseShellExecute = false,
                CreateNoWindow = true, // 设置为 true 隐藏窗口
                RedirectStandardOutput = true,
                RedirectStandardError = true,
            };

            using (Process process = new Process())
            {
                process.StartInfo = processStartInfo;

                try
                {
                    process.Start();

                    // 可选：读取输出信息
                    string output = process.StandardOutput.ReadToEnd();
                    string error = process.StandardError.ReadToEnd();

                    process.WaitForExit();

                    // 处理输出
                    if (!string.IsNullOrEmpty(output))
                    {
                        log("Output: " + output, "ExecuteCommand");
                    }

                    if (!string.IsNullOrEmpty(error))
                    {
                        LogError("Error: " + error, "ExecuteCommand");
                    }
                }
                catch (Exception ex)
                {
                    LogError("An error occurred: " + ex.Message, "ExecuteCommand");
                }
            }
        }

        private static void PerformNextStep()
        {
            // 这里执行下一步的操作
            MessageBox.Show("正在下载，请耐心等待", "操作结果", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }




        #endregion


        #region  打开文件夹

        /// <summary>
        /// 打开系统文件夹
        /// </summary>
        /// <param name="filePath"></param>
        public static void openFile(string filePath)
        {
            System.Diagnostics.Process.Start("explorer.exe", $"/select,\"{filePath}\"");
        }

        #endregion


        // 检查文件夹权限
        public static bool CheckPermission(string path)
        {
            try
            {
                // 获取文件夹的访问控制列表
                DirectorySecurity acl = Directory.GetAccessControl(path);
                // 检查当前用户是否有权限
                AuthorizationRuleCollection rules = acl.GetAccessRules(true, true, typeof(NTAccount));
                foreach (AuthorizationRule rule in rules)
                {
                    if (rule.IdentityReference.Value.Contains("SYSTEM"))
                    {
                        return true; // SYSTEM账户有权限
                    }
                }
                return false;
            }
            catch (Exception ex)
            {
                LogError($"权限检查出错: {ex.Message}", "HasSystemWritePermission");
                return false;
            }
        }

        // 检查系统还原是否启用
        public static bool IsSystemRestoreEnabled(string drive)
        {
            try
            {
                // 通过注册表检查系统还原状态
                RegistryKey key = Registry.LocalMachine.OpenSubKey(@"SOFTWARE\Microsoft\Windows NT\CurrentVersion\SystemRestore");
                if (key != null)
                {
                    object value = key.GetValue("RPSessionInterval");
                    return value != null && (int)value > 0;
                }
                return false;
            }
            catch
            {
                return false;
            }
        }

        // 检查驱动器是否为NTFS格式
        public static bool IsNtfsDrive(string drive)
        {
            DriveInfo driveInfo = new DriveInfo(drive);
            return driveInfo.DriveFormat.Equals("NTFS", StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>
        /// 检查指定文件夹是否可读写
        /// </summary>
        /// <param name="folderPath">文件夹路径</param>
        /// <returns>是否可读写</returns>
        public static bool CanReadAndWrite(string folderPath)
        {
            // 1. 检查路径是否存在
            if (!Directory.Exists(folderPath))
            {
                log("文件夹不存在", "CanReadAndWrite");
                return false;
            }

            // 2. 构造临时文件路径
            string testFilePath = Path.Combine(folderPath, "test_access.tmp");

            try
            {
                // 3. 尝试创建并写入临时文件
                File.WriteAllText(testFilePath, "Test write access.");

                // 4. 尝试读取临时文件
                string content = File.ReadAllText(testFilePath);
                if (content != "Test write access.")
                {
                    log("写入与读取内容不一致", "CanReadAndWrite");
                    return false;
                }

                // 5. 删除临时文件
                File.Delete(testFilePath);

                return true;
            }
            catch (UnauthorizedAccessException)
            {
                LogError("权限不足，无法访问文件夹", "CanReadAndWrite");
                return false;
            }
            catch (IOException ex)
            {
                LogError($"I/O 错误: {ex.Message}", "CanReadAndWrite");
                return false;
            }
            catch (Exception ex)
            {
                LogError($"发生错误: {ex.Message}", "CanReadAndWrite");
                return false;
            }
        }

        /// <summary>
        /// 递归扫描文件夹
        /// </summary>
        public static void ScanDirectory(string folderPath, List<string> result)
        {
            try
            {
                // 1. 检查当前文件夹是否包含 .mp4 文件
                string[] mp4Files = Directory.GetFiles(folderPath, "*.mp4");
                if (mp4Files.Length > 0)
                {
                    result.Add(folderPath);
                }

                // 2. 递归扫描子文件夹
                string[] subFolders = Directory.GetDirectories(folderPath);
                foreach (string subFolder in subFolders)
                {
                    ScanDirectory(subFolder, result);
                }
            }
            catch (UnauthorizedAccessException)
            {
                // 无权限访问，跳过
            }
            catch (IOException)
            {
                // I/O 错误，跳过
            }
            catch (Exception)
            {
                // 其他异常，跳过
            }
        }
        /// <summary>
        /// 递归扫描文件夹中MP4文件
        /// </summary>
        public static void ScanDirectoryMP4(string folderPath, List<string> result)
        {
            try
            {
                // 1. 获取当前文件夹下的所有 .mp4 文件
                string[] mp4Files = Directory.GetFiles(folderPath, "*.mp4");
                foreach (string file in mp4Files)
                {
                    result.Add(file);
                }

                // 2. 递归扫描子文件夹
                string[] subFolders = Directory.GetDirectories(folderPath);
                foreach (string subFolder in subFolders)
                {
                    ScanDirectoryMP4(subFolder, result);
                }
            }
            catch (UnauthorizedAccessException)
            {
                // 无权限访问，跳过
            }
            catch (IOException)
            {
                // I/O 错误，跳过
            }
            catch (Exception)
            {
                // 其他异常，跳过
            }
        }
    }
}
