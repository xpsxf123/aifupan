using douyin.Utils;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Utils
{
    public class SystemSleep
    {
        [DllImport("kernel32.dll")]
        public static extern uint SetThreadExecutionState(ExecutionFlag flags);

        [Flags]
        public enum ExecutionFlag : uint
        {
            // 阻止系统休眠（不关闭显示器）
            SystemRequired = 0x01,
            // 阻止屏幕关闭
            DisplayRequired = 0x02,
            // 持续生效直到主动取消
            Continuous = 0x80000000
        }

        /// <summary>
        /// 长期阻止屏幕关闭
        /// </summary>
        /// <param name="keepScreenOn"></param>
        public static void PreventSleep(bool keepScreenOn = true)
        {
            try
            {
                // 长期阻止（需搭配Continuous标志）
                var flags = keepScreenOn
                    ? ExecutionFlag.SystemRequired | ExecutionFlag.DisplayRequired | ExecutionFlag.Continuous
                    : ExecutionFlag.SystemRequired | ExecutionFlag.Continuous;
                SetThreadExecutionState(flags);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"长期阻止屏幕关闭发生错误");
            }
            
        }

        /// <summary>
        /// 恢复系统默认电源策略
        /// </summary>
        public static void RestoreSleep()
        {
            try
            {
                // 恢复系统默认电源策略
                SetThreadExecutionState(ExecutionFlag.Continuous);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"恢复系统默认电源策略发生错误");
            }
            
        }

        /// <summary>
        /// 单次阻止（无Continuous标志，系统计时器重置一次）
        /// </summary>
        /// <param name="keepScreenOn"></param>
        public static void TemporaryPrevent(bool keepScreenOn = true)
        {
            try
            {
                // 单次阻止（无Continuous标志，系统计时器重置一次）
                var flags = keepScreenOn
                    ? ExecutionFlag.SystemRequired | ExecutionFlag.DisplayRequired
                    : ExecutionFlag.SystemRequired;
                SetThreadExecutionState(flags);
            }
            catch (Exception ex)
            {
                FileUtils.LogError($"{ex}", $"单次阻止屏幕关闭发生错误");
            }
            
        }
    }
}
