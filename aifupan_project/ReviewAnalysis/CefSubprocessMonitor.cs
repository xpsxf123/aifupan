using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Timers;
using douyin.Utils;

namespace ReviewAnalysis
{
    public class CefSubprocessMonitor
    {
        private readonly Timer _timer;
        private readonly string _logFile;

        public CefSubprocessMonitor(int intervalSeconds = 30, string logFile = "CefSubprocessMonitor.log")
        {
            _timer = new Timer(intervalSeconds * 1000);
            _timer.Elapsed += OnTimerElapsed;
            _logFile = logFile;
        }

        public void Start()
        {
            _timer.Start();
            Log("=== CefSubprocessMonitor Started ===");
        }

        public void Stop()
        {
            _timer.Stop();
            Log("=== CefSubprocessMonitor Stopped ===");
        }

        private void OnTimerElapsed(object sender, ElapsedEventArgs e)
        {
            try
            {
                var processes = Process.GetProcessesByName("CefSharp.BrowserSubprocess");
                if (processes.Length == 0)
                {
                    Log("No CefSharp.BrowserSubprocess running.");
                    return;
                }

                foreach (var p in processes)
                {
                    try
                    {
                        string info = string.Format(
                            "PID={0} | Memory={1:N0} KB | Handles={2} | StartTime={3}",
                            p.Id,
                            p.WorkingSet64 / 1024,
                            p.HandleCount,
                            p.StartTime);

                        Log(info);
                    }
                    catch (Exception ex)
                    {
                        Log("Error reading process info: " + ex.Message);
                    }
                }
            }
            catch (Exception ex)
            {
                Log("Monitor error: " + ex.Message);
            }
        }

        private void Log(string message)
        {
            string logLine = $"{DateTime.Now:yyyy-MM-dd HH:mm:ss} {message}";
            FileUtils.log(logLine);
            try
            {
                File.AppendAllText(_logFile, logLine + Environment.NewLine, Encoding.UTF8);
            }
            catch
            {
                // ignore logging errors
            }
        }
    }
}
