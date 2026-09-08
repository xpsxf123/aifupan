using douyin.Utils;
using Newtonsoft.Json;
using ReviewAnalysis.Model;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;

namespace ReviewAnalysis.DataCache
{
    /// <summary>
    /// 主播排班缓存管理
    /// </summary>
    public class AnchorScheduleCacheManager
    {
        /// <summary>
        /// 排班缓存：key=SecUid，value=该主播的排班列表
        /// </summary>
        private static ConcurrentDictionary<string, List<AnchorSchedule>> scheduleCache
            = new ConcurrentDictionary<string, List<AnchorSchedule>>();

        /// <summary>
        /// 最后一次同步时间
        /// </summary>
        public static DateTime LastSyncTime { get; private set; } = DateTime.MinValue;

        /// <summary>
        /// 清空排班缓存
        /// </summary>
        public static void ClearCache()
        {
            scheduleCache.Clear();
        }

        /// <summary>
        /// 更新排班缓存数据
        /// </summary>
        /// <param name="schedules">排班列表</param>
        public static void UpdateCache(List<AnchorSchedule> schedules)
        {
            if (schedules == null)
            {
                return;
            }

            // 清空旧数据
            scheduleCache.Clear();

            // 按主播分组存储
            foreach (var schedule in schedules)
            {
                if (string.IsNullOrEmpty(schedule.secUid))
                {
                    continue;
                }

                scheduleCache.AddOrUpdate(
                    schedule.secUid,
                    new List<AnchorSchedule> { schedule },
                    (key, existingList) =>
                    {
                        existingList.Add(schedule);
                        return existingList;
                    }
                );
            }

            LastSyncTime = ServerTimeUtils.getCurrentDateTime();
            FileUtils.LogRecrd($"排班数量: {schedules.Count}, 主播数量: {scheduleCache.Count}", "排班缓存更新完成");
        }

        /// <summary>
        /// 获取主播当前生效的排班
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>当前生效的排班，如果没有则返回null</returns>
        public static AnchorSchedule GetActiveSchedule(string secUid)
        {
            if (string.IsNullOrEmpty(secUid))
            {
                return null;
            }

            if (!scheduleCache.TryGetValue(secUid, out var schedules) || schedules == null || schedules.Count == 0)
            {
                return null;
            }

            DateTime now = ServerTimeUtils.getCurrentDateTime();

            foreach (var schedule in schedules)
            {
                try
                {
                    DateTime startTime = DateTime.Parse(schedule.startTime);
                    DateTime endTime = DateTime.Parse(schedule.endTime);

                    if (now >= startTime && now <= endTime)
                    {
                        return schedule;
                    }
                }
                catch (Exception ex)
                {
                    FileUtils.LogError($"{ex}", $"解析排班时间异常：StartTime={schedule.startTime}, EndTime={schedule.endTime}");
                }
            }

            return null;
        }

        /// <summary>
        /// 检查主播今天是否有排班
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>true=今天有排班，false=今天没有排班</returns>
        public static bool HasScheduleToday(string secUid)
        {
            if (string.IsNullOrEmpty(secUid))
            {
                return false;
            }

            if (!scheduleCache.TryGetValue(secUid, out var schedules) || schedules == null || schedules.Count == 0)
            {
                return false;
            }

            DateTime today = ServerTimeUtils.getCurrentDateTime().Date;
            return schedules.Any(s =>
            {
                try
                {
                    DateTime startTime = DateTime.Parse(s.startTime);
                    DateTime endTime = DateTime.Parse(s.endTime);
                    // 只要排班的开始日期或结束日期覆盖到今天，就算今天有排班
                    return startTime.Date <= today && endTime.Date >= today;
                }
                catch
                {
                    return false;
                }
            });
        }

        /// <summary>
        /// 获取主播今天的所有排班
        /// </summary>
        /// <param name="secUid">主播SecUid</param>
        /// <returns>今天的排班列表</returns>
        public static List<AnchorSchedule> GetTodaySchedules(string secUid)
        {
            if (string.IsNullOrEmpty(secUid))
            {
                return new List<AnchorSchedule>();
            }

            if (!scheduleCache.TryGetValue(secUid, out var schedules) || schedules == null || schedules.Count == 0)
            {
                return new List<AnchorSchedule>();
            }

            DateTime today = ServerTimeUtils.getCurrentDateTime().Date;
            return schedules.Where(s =>
            {
                try
                {
                    DateTime startTime = DateTime.Parse(s.startTime);
                    DateTime endTime = DateTime.Parse(s.endTime);
                    return startTime.Date <= today && endTime.Date >= today;
                }
                catch
                {
                    return false;
                }
            }).ToList();
        }

        /// <summary>
        /// 根据排班ID获取排班信息
        /// </summary>
        /// <param name="scheduleId">排班ID</param>
        /// <returns>排班信息</returns>
        public static AnchorSchedule GetScheduleById(string scheduleId)
        {
            if (string.IsNullOrEmpty(scheduleId))
            {
                return null;
            }

            foreach (var kvp in scheduleCache)
            {
                var schedule = kvp.Value.FirstOrDefault(s => s.scheduleId == scheduleId);
                if (schedule != null)
                {
                    return schedule;
                }
            }

            return null;
        }

        /// <summary>
        /// 获取所有有排班的主播SecUid列表
        /// </summary>
        /// <returns>主播SecUid列表</returns>
        public static List<string> GetAllScheduledAnchorSecUids()
        {
            return scheduleCache.Keys.ToList();
        }

        /// <summary>
        /// 判断当前的secUid是否有排班
        /// </summary>
        /// <param name="secUid"></param>
        /// <returns></returns>
        public static bool GetAnchorIsScheduled(string secUid)
        {
            //if (string.IsNullOrEmpty(secUid) || scheduleCache.IsEmpty)
            //{
            //    return false;
            //}

            //if (!scheduleCache.TryGetValue(secUid, out var schedules))
            //{
            //    return false;
            //}
            //return schedules != null && schedules.Count > 0;
            return true;
        }

        /// <summary>
        /// 获取缓存中的排班总数
        /// </summary>
        /// <returns>排班总数</returns>
        public static int GetTotalScheduleCount()
        {
            int count = 0;
            foreach (var kvp in scheduleCache)
            {
                count += kvp.Value.Count;
            }
            return count;
        }
    }
}
