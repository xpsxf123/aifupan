using System;
using System.Diagnostics;
using System.Security.Principal;
using System.Threading.Tasks;
using douyin.Utils;

namespace ReviewAnalysis.Utils
{
    internal class AddFirewall
    {

        public static int ManageFirewallRulesAsync()
        {
            FileUtils.log("【Windows防火墙规则管理】", "防火墙规则", true);
            FileUtils.log("规则名称：爱复盘", "防火墙规则", true);
            FileUtils.log("协议/端口：TCP 5001", "防火墙规则", true);
            try
            {
                // 清理现有规则
                CleanupExistingRulesAsync();

                // 添加新规则
                AddNewRulesAsync();
                FileUtils.log("已成功更新 TCP 端口 5001 的\"爱复盘\"防火墙规则。", "防火墙规则");
                return 0;
            }
            catch (Exception ex)
            {
                FileUtils.log($"操作失败: {ex.Message}", "防火墙规则",true);
                return -1;
            }
        }

        private static bool IsRunningAsAdministrator()
        {
            using (var identity = WindowsIdentity.GetCurrent())
            {
                var principal = new WindowsPrincipal(identity);
                return principal.IsInRole(WindowsBuiltInRole.Administrator);
            }
        }

        private static void CleanupExistingRulesAsync()
        {
            FileUtils.log("正在检查并清理现有的\"爱复盘\"规则...");

            // 删除入站规则
            if (FirewallRuleExistsAsync("爱复盘", "in"))
            {
                FileUtils.log("发现已存在的入站规则，正在删除...");
                DeleteFirewallRuleAsync("爱复盘", "in");
                FileUtils.log("入站规则删除完成。");
            }
            else
            {
                FileUtils.log("未找到同名入站规则。");
            }

            // 删除出站规则
            if (FirewallRuleExistsAsync("爱复盘", "out"))
            {
                FileUtils.log("发现已存在的出站规则，正在删除...");
                DeleteFirewallRuleAsync("爱复盘", "out");
                FileUtils.log("出站规则删除完成。");
            }
            else
            {
                FileUtils.log("未找到同名出站规则。");
            }
        }

        private static void AddNewRulesAsync()
        {
            FileUtils.log("正在添加新的防火墙规则...");

            // 添加入站规则
            FileUtils.log("正在添加入站规则...");
            AddFirewallRuleAsync("爱复盘", "in", "allow", "TCP", "5001-5020,23564-23583,45001-45010");

            // 添加出站规则
            FileUtils.log("正在添加出站规则...");
            AddFirewallRuleAsync("爱复盘", "out", "allow", "TCP", "5001-5020,23564-23583,45001-45010");
        }

        private static bool FirewallRuleExistsAsync(string ruleName, string direction)
        {
            try
            {
                using (var process = new Process())
                {
                    process.StartInfo = new ProcessStartInfo
                    {
                        FileName = "netsh",
                        Arguments = $"advfirewall firewall show rule name=\"{ruleName}\" dir={direction}",
                        UseShellExecute = false,
                        CreateNoWindow = true,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true
                    };

                    process.Start();
                    process.WaitForExit();

                    return process.ExitCode == 0;
                }
            }
            catch
            {
                return false;
            }
        }

        private static void DeleteFirewallRuleAsync(string ruleName, string direction)
        {
            using (var process = new Process())
            {
                process.StartInfo = new ProcessStartInfo
                {
                    FileName = "netsh",
                    Arguments = $"advfirewall firewall delete rule name=\"{ruleName}\" dir={direction}",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                process.Start();
                process.WaitForExit();
            }
        }

        private static void AddFirewallRuleAsync(string ruleName, string direction, string action, string protocol, string port)
        {
            using (var process = new Process())
            {
                process.StartInfo = new ProcessStartInfo
                {
                    FileName = "netsh",
                    Arguments = $"advfirewall firewall add rule name=\"{ruleName}\" dir={direction} action={action} protocol={protocol} localport={port} profile=any",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };

                process.Start();
                process.WaitForExit();
            }
        }

    }
}
