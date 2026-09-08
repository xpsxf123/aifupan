using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    public class DefenderExclusionsManager
    {
        private const string PowerShellExePath = @"C:\Windows\system32\WindowsPowerShell\v1.0\powershell.exe";


        /// <summary>
        /// 设置目录为安全目录不需要windows扫描
        /// </summary>
        /// <param name="folderPath"></param>
        public static void AddFolderToExclusions()
        {
            try
            {
                string ScriptPath = @"script\AddExclusion.ps1";
                // 获取当前执行的 EXE 文件的绝对路径
                string exePath = System.Reflection.Assembly.GetExecutingAssembly().Location;
                string folderPath = Path.GetDirectoryName(exePath);
                // 构建 PowerShell 命令行参数
                string arguments = $"-ExecutionPolicy Bypass -File \"{ScriptPath}\" -FolderPath \"{folderPath}\"";

                // 创建 ProcessStartInfo
                ProcessStartInfo startInfo = new ProcessStartInfo
                {
                    FileName = PowerShellExePath,
                    Arguments = arguments,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                // 创建并启动 Process
                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;
                    process.Start();

                    // 读取标准输出和标准错误
                    string output = process.StandardOutput.ReadToEnd();
                    string error = process.StandardError.ReadToEnd();

                    // 等待子进程完成
                    process.WaitForExit();

                    if (!string.IsNullOrEmpty(error))
                    {
                        throw new Exception($"An error occurred: {error}");
                    }

                    FileUtils.log($"Folder '{folderPath}' added to exclusions.");
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"Failed to add folder to exclusions: {ex.Message}");
            }
        }

        public static void DisableDefender()
        {
            try
            {
               
                // PowerShell 脚本的路径
                string closeScriptPath = @"script\DisableDefender.ps1";

                // 启动 PowerShell 进程
                ProcessStartInfo startInfo = new ProcessStartInfo
                {
                    FileName = PowerShellExePath,
                    Arguments = $"-ExecutionPolicy Bypass -File \"{closeScriptPath}\"",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    CreateNoWindow = true
                };

                using (Process process = new Process())
                {
                    process.StartInfo = startInfo;
                    process.Start();

                    // 等待进程完成
                    process.WaitForExit();

                    // 检查进程是否成功完成
                    if (process.ExitCode == 0)
                    {
                        FileUtils.log("Windows Defender has been disabled.");
                    }
                    else
                    {
                        FileUtils.log("Failed to disable Windows Defender.");
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.log($"An error occurred: {ex.Message}");
            }
        }

    }
}
