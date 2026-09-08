using douyin.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Management;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace ReviewAnalysis.Utils
{
    /// <summary>
    /// 采集本机硬件配置信息，用于上报服务端。每个 WMI 查询独立 try/catch，单个失败不影响其他字段。
    /// </summary>
    public static class ComputerInfoCollector
    {
        public static ComputerConfigData Collect()
        {
            var data = new ComputerConfigData();

            // CPU
            try
            {
                using (var searcher = new ManagementObjectSearcher(
                    "SELECT Name, NumberOfCores, NumberOfLogicalProcessors, MaxClockSpeed FROM Win32_Processor"))
                using (var results = searcher.Get())
                {
                    foreach (ManagementObject mo in results)
                    {
                        using (mo)
                        {
                            data.cpuModel = mo["Name"]?.ToString()?.Trim();
                            data.cpuCores = Convert.ToInt32(mo["NumberOfCores"] ?? 0);
                            data.cpuThreads = Convert.ToInt32(mo["NumberOfLogicalProcessors"] ?? 0);
                            data.cpuFrequencyMhz = Convert.ToInt32(mo["MaxClockSpeed"] ?? 0);
                            break;
                        }
                    }
                }
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] CPU probe failed: {ex.Message}"); }

            // 内存
            try
            {
                using (var searcher = new ManagementObjectSearcher(
                    "SELECT TotalPhysicalMemory FROM Win32_ComputerSystem"))
                using (var results = searcher.Get())
                {
                    foreach (ManagementObject mo in results)
                    {
                        using (mo)
                        {
                            ulong bytes = Convert.ToUInt64(mo["TotalPhysicalMemory"] ?? 0UL);
                            data.ramGb = Math.Round(bytes / (1024.0 * 1024.0 * 1024.0), 2);
                            break;
                        }
                    }
                }
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] RAM probe failed: {ex.Message}"); }

            // GPU
            try
            {
                var gpuNames = new List<string>();
                long maxVramBytes = 0;
                using (var searcher = new ManagementObjectSearcher(
                    "SELECT Name, AdapterRAM FROM Win32_VideoController"))
                using (var results = searcher.Get())
                {
                    foreach (ManagementObject mo in results)
                    {
                        using (mo)
                        {
                            string name = mo["Name"]?.ToString()?.Trim();
                            if (!string.IsNullOrEmpty(name))
                                gpuNames.Add(name);
                            long vram = Convert.ToInt64(mo["AdapterRAM"] ?? 0L);
                            if (vram > maxVramBytes) maxVramBytes = vram;
                        }
                    }
                }
                data.gpuModel = string.Join("; ", gpuNames);
                if (maxVramBytes > 0)
                    data.vramGb = Math.Round(maxVramBytes / (1024.0 * 1024.0 * 1024.0), 2);
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] GPU probe failed: {ex.Message}"); }

            // 安装盘（应用所在盘符）
            try
            {
                string installDrive = System.IO.Path.GetPathRoot(
                    System.Reflection.Assembly.GetEntryAssembly()?.Location
                    ?? Environment.CurrentDirectory);
                var driveInfo = new System.IO.DriveInfo(installDrive);
                if (driveInfo.IsReady)
                {
                    data.diskTotalGb = Math.Round(driveInfo.TotalSize / (1024.0 * 1024.0 * 1024.0), 2);
                    data.diskFreeGb = Math.Round(driveInfo.AvailableFreeSpace / (1024.0 * 1024.0 * 1024.0), 2);
                }
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] Disk probe failed: {ex.Message}"); }

            // OS（Win32_OperatingSystem 获取友好版本名 + 注册表取 UBR）
            try
            {
                using (var searcher = new ManagementObjectSearcher(
                    "SELECT Caption, Version FROM Win32_OperatingSystem"))
                using (var results = searcher.Get())
                {
                    foreach (ManagementObject mo in results)
                    {
                        using (mo)
                        {
                            string caption = mo["Caption"]?.ToString() ?? "";
                            // 去掉 "Microsoft " 前缀
                            if (caption.StartsWith("Microsoft "))
                                caption = caption.Substring("Microsoft ".Length);
                            string version = mo["Version"]?.ToString() ?? "";
                            // 取 build 号（version 格式: 10.0.22631）
                            var parts = version.Split('.');
                            string build = parts.Length >= 3 ? parts[2] : "";
                            // 从注册表取 UBR（Update Build Revision）
                            string ubr = "";
                            try
                            {
                                using (var key = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(
                                    @"SOFTWARE\Microsoft\Windows NT\CurrentVersion"))
                                {
                                    if (key != null)
                                        ubr = key.GetValue("UBR")?.ToString() ?? "";
                                }
                            }
                            catch { }
                            if (!string.IsNullOrEmpty(build) && !string.IsNullOrEmpty(ubr))
                                data.osVersion = $"{caption} {build}.{ubr}";
                            else if (!string.IsNullOrEmpty(build))
                                data.osVersion = $"{caption} {build}";
                            else
                                data.osVersion = caption;
                            break;
                        }
                    }
                }
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] OS probe failed: {ex.Message}"); }

            // MAC（第一个活跃物理网卡）
            try
            {
                var nic = NetworkInterface.GetAllNetworkInterfaces()
                    .FirstOrDefault(n => n.OperationalStatus == OperationalStatus.Up
                        && n.NetworkInterfaceType != NetworkInterfaceType.Loopback
                        && n.NetworkInterfaceType != NetworkInterfaceType.Tunnel
                        && !string.IsNullOrEmpty(n.GetPhysicalAddress()?.ToString()));
                if (nic != null)
                    data.macAddress = nic.GetPhysicalAddress().ToString();
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] MAC probe failed: {ex.Message}"); }

            // 计算机名
            try { data.computerName = Environment.MachineName; }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] Hostname probe failed: {ex.Message}"); }

            // IP
            try
            {
                var host = System.Net.Dns.GetHostEntry(System.Net.Dns.GetHostName());
                var ipv4 = host.AddressList.FirstOrDefault(a =>
                    a.AddressFamily == AddressFamily.InterNetwork
                    && !System.Net.IPAddress.IsLoopback(a));
                if (ipv4 != null)
                    data.ipAddress = ipv4.ToString();
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] IP probe failed: {ex.Message}"); }

            // 屏幕分辨率
            try
            {
                var bounds = System.Windows.Forms.Screen.PrimaryScreen?.Bounds;
                if (bounds.HasValue)
                    data.screenResolution = $"{bounds.Value.Width}x{bounds.Value.Height}";
            }
            catch (Exception ex) { FileUtils.LogAnalysis($"[ComputerInfo] Screen probe failed: {ex.Message}"); }

            return data;
        }
    }

    /// <summary>采集到的硬件配置数据。</summary>
    public class ComputerConfigData
    {
        public string cpuModel;
        public int cpuCores;
        public int cpuThreads;
        public int cpuFrequencyMhz;
        public double ramGb;
        public string gpuModel;
        public double vramGb;
        public double diskTotalGb;
        public double diskFreeGb;
        public string osVersion;
        public string macAddress;
        public string computerName;
        public string ipAddress;
        public string screenResolution;
    }
}
