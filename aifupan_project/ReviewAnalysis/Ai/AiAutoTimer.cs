using System;
using System.Threading;
using System.Threading.Tasks;
using douyin.Utils;
using ReviewAnalysis.Ai.auto;

namespace ReviewAnalysis.Ai
{
    public class AiAutoTimer
    {

        public static volatile string diagnosisTaskId = Guid.NewGuid().ToString();
        public static volatile string videoContentTaskId = Guid.NewGuid().ToString();
        public static volatile string htmlTaskId = Guid.NewGuid().ToString();
        public static volatile int timeoutIndex = 0;
        private static volatile bool shouldStop = false;
        
        
        public static Thread autoTimerThread;


        public static void start()
        {
            if (autoTimerThread != null)
            {
                Stop();
                // 等待线程真正停止
                Thread.Sleep(100);
            
                // 重置标志
                shouldStop = false;
                autoTimerThread = null;
            }

            autoTimerThread = new Thread(async () =>
            {
                // 内容诊断
                await DiagnosisAuto.initPut();
                await DiagnosisAuto.UpdateMaxConcurrentTasks();
                
                // 把全部的待分析的自然、优化原文重新set到队列中
                await VideoContentAuto.initPut();
                await VideoContentAuto.UpdateMaxConcurrentTasks();
                
                // 初始化生成html的方法
                await HtmlAuto.initPut();
                await HtmlAuto.UpdateMaxConcurrentTasks();
                
                
                // 初始化纠正Ai内容的方法
                await CorrectAiContentAuto.initPut();
                await CorrectAiContentAuto.UpdateMaxConcurrentTasks();
                
                int index = 0;
                while (true)
                {
                    
                    // 内容诊断
                    try
                    {
                        DiagnosisAuto.startOne();
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError(ex.Message, "诊断问题自动分析。error");
                    }
                    
                    // 原文自动分析
                    try
                    {
                        await VideoContentAuto.startOne(timeoutIndex);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError(ex.Message, "自然、优化原文报错");
                    }
                    
                    // html自动生成
                    try
                    {
                        await HtmlAuto.startOne(timeoutIndex);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError(ex.Message, "生成html报错，error");
                    }
                    
                    // 纠正Ai内容
                    try
                    {
                        await CorrectAiContentAuto.startOne(timeoutIndex);
                    }
                    catch (Exception ex)
                    {
                        FileUtils.LogError(ex.Message, "纠正Ai内容报错");
                    }

                    for (var i = 0; i < 15; i++)
                    {
                        if (shouldStop)
                        {
                            break;
                        }
                        await Task.Delay(1000);
                    }
                    index++;
                }
            });
            autoTimerThread.Start();
        }

        public static void Stop()
        {
            shouldStop = true;  // 设置停止标志
        
            // 等待线程结束
            if (autoTimerThread != null && autoTimerThread.IsAlive)
            {
                bool stopped = autoTimerThread.Join(TimeSpan.FromSeconds(20));
                if (!stopped)
                {
                    #if NETFRAMEWOR 
                    // 强制终止
                    autoTimerThread.Abort();
                    #endif
                }
            }
        
            autoTimerThread = null;
        }

        /// <summary>
        /// 修改taskId,使线程停止
        /// </summary>
        public static void stop()
        {
            diagnosisTaskId = Guid.NewGuid().ToString();
            videoContentTaskId = Guid.NewGuid().ToString();
            htmlTaskId = Guid.NewGuid().ToString();
        }


    }
}
