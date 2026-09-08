using douyin.Utils;
using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Management;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class SystemUtils
    {

        public static string cpuid;

        /// <summary>
        /// 检查系统有安装VC_redist.x64
        /// </summary>
        /// <returns></returns>
        public static bool IsVCRedistInstalled()
        {
            FileUtils.log("注册表检查系统有安装VC_redist.x64?");
            // 注册表中的关键路径，针对不同版本可能会有不同的注册表项
            string[] vcRedistRegistryPaths = new string[]
            {
                @"SOFTWARE\Microsoft\VisualStudio\14.0\VC\Runtimes\x64",
                @"SOFTWARE\Microsoft\VisualStudio\15.0\VC\Runtimes\x64",
                @"SOFTWARE\Microsoft\VisualStudio\16.0\VC\Runtimes\x64",
                @"SOFTWARE\Microsoft\VisualStudio\17.0\VC\Runtimes\x64",
                @"SOFTWARE\WOW6432Node\Microsoft\VisualStudio\14.0\VC\Runtimes\x64",
                @"SOFTWARE\WOW6432Node\Microsoft\VisualStudio\15.0\VC\Runtimes\x64",
                @"SOFTWARE\WOW6432Node\Microsoft\VisualStudio\16.0\VC\Runtimes\x64",
                @"SOFTWARE\WOW6432Node\Microsoft\VisualStudio\17.0\VC\Runtimes\x64"
            };

            foreach (string regPath in vcRedistRegistryPaths)
            {
                try
                {
                    using (RegistryKey key = Registry.LocalMachine.OpenSubKey(regPath))
                    {
                        if (key != null)
                        {
                            // 如果找到对应的键，说明已安装VC_redist.x64
                            return true;
                        }
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.log("检查注册表时发生错误: " + ex.Message, "检查系统有安装VC_redist.x64", true);
                }
            }

            // 补救检查，如果注册表没有检测到，在来次WMI 查询，这个相对运行速度慢点
            try
            {
                FileUtils.log("注册表没有检测到VC_redist.x64安装，开始WMI查询");
                // 查询安装的程序列表
                var searcher = new ManagementObjectSearcher(@"SELECT * FROM Win32_Product WHERE Name LIKE 'Microsoft Visual C++%Redistributable%'");

                foreach (ManagementObject obj in searcher.Get())
                {
                    string name = obj["Name"]?.ToString();
                    if (name != null && name.Contains("Visual C++ Redistributable") && name.Contains("x64"))
                    {
                        return true;
                    }
                }
            }
            catch (Exception ex)
            {
                FileUtils.log("WMI查询发生错误: " + ex.Message, "检查系统有安装VC_redist.x64", true);
            }
            FileUtils.log("WMI没有检测到VC_redist.x64安装");
            return false;
        }

        /// <summary>
        /// 获取电脑的机器码
        /// </summary>
        /// <returns></returns>
        public static string GenerateMachineCode()
        {
            try
            {
                if(!string.IsNullOrEmpty(cpuid))
                {
                    return cpuid;
                }

                string cpuId = GetCpuId();

                string diskSerial = GetDiskSerial();

                string motherboardSerial = GetMotherboardSerial();

                // 组合硬件信息
                string combined = cpuId + diskSerial + motherboardSerial;

                // 使用 MD5 生成唯一的机器码
                using (MD5 md5 = MD5.Create())
                {
                    byte[] hash = md5.ComputeHash(Encoding.UTF8.GetBytes(combined));
                    cpuid = BitConverter.ToString(hash).Replace("-", "").ToLower();
                    return cpuid;
                }
            }
            catch (Exception ex)
            {
                return "error";
            }
            
        }

        /// <summary>
        /// 获取cpu号
        /// </summary>
        /// <returns></returns>
        public static string GetCpuId()
        {
            try
            {
                string cpuId = string.Empty;
                ManagementClass mc = new ManagementClass("win32_processor");
                ManagementObjectCollection moc = mc.GetInstances();

                foreach (ManagementObject mo in moc)
                {
                    cpuId = mo.Properties["ProcessorId"].Value.ToString();
                    break;
                }
                return cpuId;
            }
            catch (Exception ex)
            {
                FileUtils.log(ex.Message, "获取cpu号报错");
                return string.Empty;
            }
        }

        /// <summary>
        /// 获取网卡号
        /// </summary>
        /// <returns></returns>
        public static string GetDiskSerial()
        {
            try
            {
                string diskSerial = string.Empty;
                ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT * FROM Win32_DiskDrive");

                foreach (ManagementObject mo in searcher.Get())
                {
                    diskSerial = mo["SerialNumber"].ToString().Trim();
                    break;
                }
                return diskSerial;
            }
            catch (Exception ex)
            {
                FileUtils.log(ex.Message, "获取网卡报错");
                return string.Empty;
            }
        }

        /// <summary>
        /// 获取主板号
        /// </summary>
        /// <returns></returns>
        public static string GetMotherboardSerial()
        {
            try
            {
                string motherboardSerial = string.Empty;
                ManagementObjectSearcher searcher = new ManagementObjectSearcher("SELECT * FROM Win32_BaseBoard");

                foreach (ManagementObject mo in searcher.Get())
                {
                    motherboardSerial = mo["SerialNumber"].ToString().Trim();
                    break;
                }
                return motherboardSerial;
            }
            catch (Exception e)
            {
                FileUtils.log(e.Message, "获取主板号报错");
                return string.Empty;
            }
        }
        
        /// <summary>
        /// 检查系统有安装Net8
        /// </summary>
        /// <returns></returns>
        public static bool IsNet8Installed()
        {
            // 检查注册表中的相关键值，以确定.NET 8.0 是否安装
            // 在 Windows 上，.NET 安装信息通常存储在注册表的以下位置
            const string subKey = @"SOFTWARE\dotnet\Setup\InstalledVersions\x64\shared\Microsoft.NETCore.App";
            using (RegistryKey key = Registry.LocalMachine.OpenSubKey(subKey))
            {
                if (key != null)
                {
                    // 查找版本为 8.0 的子键
                    string[] versionNames = key.GetSubKeyNames();
                    foreach (string versionName in versionNames)
                    {
                        if (versionName.StartsWith("8.0"))
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}