using System;
using System.Collections.Generic;
using System.IO;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using douyin.Utils;

namespace ReviewAnalysis.plugins.core
{
    /// <summary>
    /// 主播下播后平台数据补采集的去重记录存储。
    /// 以「场次（batchNumber）→ 最近尝试时间」维度记录，存本地 JSON 文件（一个用户一个文件），不用 SQLite。
    /// 文件路径沿用 PathUtils 的 {ActiveTenantId}-{UserId} 用户隔离约定，切号/切租户自然读新文件、不串扰。
    /// </summary>
    public class SupplementCollectRecordStore
    {
        // 文件读写与字典操作的锁（调度器串行单线程，此处保留线程安全兜底）
        private static readonly object _lock = new object();

        /// <summary>
        /// 获取当前用户（租户+用户）的去重记录文件完整路径。
        /// </summary>
        /// <returns>去重记录文件路径</returns>
        private static string GetFilePath()
        {
            string fileName = $"supplement_collect_{ReplayHttpUtils.ActiveTenantId}-{ReplayHttpUtils.UserId}.json";
            return Path.Combine(Environment.CurrentDirectory, "dataCollect", "config", fileName);
        }

        /// <summary>
        /// 读取去重记录（调用方需已持有锁）：batchNumber → 最近尝试时间（yyyy-MM-dd HH:mm:ss）。
        /// 文件不存在或损坏时视为空字典。
        /// </summary>
        /// <returns>去重记录字典</returns>
        private static Dictionary<string, string> ReadAllUnlocked()
        {
            string filePath = GetFilePath();
            if (!File.Exists(filePath))
            {
                return new Dictionary<string, string>();
            }

            try
            {
                string json = File.ReadAllText(filePath);
                var dict = JsonConvert.DeserializeObject<Dictionary<string, string>>(json);
                return dict ?? new Dictionary<string, string>();
            }
            catch (Exception ex)
            {
                FileUtils.LogRpa($"读取补采集去重记录异常: {ex.Message}", "SupplementCollectRecordStore");
                return new Dictionary<string, string>();
            }
        }

        /// <summary>
        /// 获取某场次的最近尝试时间。
        /// </summary>
        /// <param name="batchNumber">直播场次号（roomId）</param>
        /// <returns>最近尝试时间；无记录或记录非法时返回 null</returns>
        public static DateTime? GetLastAttemptTime(string batchNumber)
        {
            if (string.IsNullOrEmpty(batchNumber))
            {
                return null;
            }

            lock (_lock)
            {
                var dict = ReadAllUnlocked();
                if (dict.TryGetValue(batchNumber, out string timeStr) && !string.IsNullOrEmpty(timeStr))
                {
                    if (DateTime.TryParse(timeStr, out DateTime time))
                    {
                        return time;
                    }
                }
                return null;
            }
        }

        /// <summary>
        /// 更新某场次的最近尝试时间（仅对真正执行了补采的场次调用，无论成败，失败靠下一轮重试）。
        /// 写入流程：先读全量 → 删除超过窗口天数的过期记录 → 更新当前场次 → 写回文件，避免文件无限增长。
        /// </summary>
        /// <param name="batchNumber">直播场次号（roomId）</param>
        /// <param name="now">本次尝试时间</param>
        /// <param name="windowDays">过期清理窗口（天）</param>
        public static void UpdateAttemptTime(string batchNumber, DateTime now, int windowDays)
        {
            if (string.IsNullOrEmpty(batchNumber))
            {
                return;
            }

            lock (_lock)
            {
                var dict = ReadAllUnlocked();

                // 删除 lastAttemptTime 距今超过窗口天数的过期记录
                DateTime cutoff = now.AddDays(-windowDays);
                var expiredKeys = new List<string>();
                foreach (var kv in dict)
                {
                    if (DateTime.TryParse(kv.Value, out DateTime time) && time < cutoff)
                    {
                        expiredKeys.Add(kv.Key);
                    }
                }
                foreach (var key in expiredKeys)
                {
                    dict.Remove(key);
                }

                // 更新当前场次的最近尝试时间
                dict[batchNumber] = now.ToString("yyyy-MM-dd HH:mm:ss");

                string filePath = GetFilePath();
                try
                {
                    string dir = Path.GetDirectoryName(filePath);
                    if (!Directory.Exists(dir))
                    {
                        Directory.CreateDirectory(dir);
                    }
                    string json = JsonConvert.SerializeObject(dict, Formatting.Indented);
                    File.WriteAllText(filePath, json);
                }
                catch (Exception ex)
                {
                    FileUtils.LogRpa($"写入补采集去重记录异常: {ex.Message}", "SupplementCollectRecordStore");
                }
            }
        }
    }
}
