using douyin.Utils;
using ReviewAnalysis.Bll.Anchor;
using System;
using System.Runtime.InteropServices;

namespace ReviewAnalysis.Utils
{
    public class CppRecordUtils
    {
        // 定义与C++回调匹配的委托
        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate void ExceptionCallback(string taskid, string errorMessage, int errorCode);

        [DllImport("kernel32.dll", SetLastError = true)]
        static extern bool SetDllDirectory(string lpPathName);

        /// <summary>
        /// 添加任务
        /// </summary>
        /// <param name="streamUrl">流地址</param>
        /// <param name="path">存储路径</param>
        /// <param name="id">任务id，不可重复</param>
        /// <param name="duration">录制的时长，秒 -1表示无限制</param>
        /// <param name="platform">平台类型 0：抖音 1：快手</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern int addTask(string streamUrl, IntPtr path, string id, long duration, long platform);

        /// <summary>
        /// 更新任务
        /// </summary>
        /// <param name="streamUrl">流地址</param>
        /// <param name="path">存储路径</param>
        /// <param name="id">任务id，不可重复</param>
        /// <param name="duration">录制的时长，秒</param>
        /// <param name="platform">平台类型 0：抖音 1：快手</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern int updateTask(string streamUrl, IntPtr path, string id, long duration, long platform);

        /// 设置任务状态为停止状态
        /// <param name="id">任务id，不可重复</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern int stopTask(string id);
        /// 设置任务状态为删除状态
        /// <param name="id">任务id，不可重复</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern int deleteTask(string id);
        /// 获取任务状态 暂时不用
        /// <param name="id">任务id，不可重复</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern int getTaskStatus(string id);

        /// 获取录制时长 暂时不用
        /// <param name="id">任务id，不可重复</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern long getTaskTime(string id);
        /// 获取录制文件大小 暂时不用
        /// <param name="id">任务id，不可重复</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern long getTaskSize(string id);
        /// 所有任务设为开始状态
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void startAll();
        /// 所有任务设为停止状态
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void stopAll();
        /// 删除所有任务
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void deleteAll();

        /// <summary>
        /// 开启任务检测，开启这个才能添加任务录屏
        /// </summary>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void run();

        /// <summary>
        /// 停止任务检测，停止所有录制任务
        /// </summary>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void stopRun();

        /// 开启日志
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void setLog();
        /// 注册事件回调
        /// <param name="callback">事件触发后的回调函数</param>
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void RegisterExceptionCallback(ExceptionCallback callback);
        /// 开启事件loop
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void startEventLoop();
        /// 停止事件loop
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void stopEventLoop();
        /// 测试用的，不用管
        [DllImport("librecord.dll", CallingConvention = CallingConvention.Cdecl)]
        public static extern void setupTimerEvent();

        // 保持委托引用防止被GC回收
        private static ExceptionCallback _callbackHolder;

        /// 事件初始化
        public static void Initialize()
        {
            // 创建委托实例并固定
            _callbackHolder = new ExceptionCallback(OnExceptionEvent);
            RegisterExceptionCallback(_callbackHolder);
            startEventLoop();
            // setupTimerEvent();
        }

        /// <summary>
        /// C++事件回调处理方法
        /// </summary>
        /// <param name="taskid">任务id</param>
        /// <param name="errorMessage">错误消息</param>
        /// <param name="errorCode">错误码 0：START, 1：RUNNING, 2：STOP, 3：DEL, 4：READ_ERROR, 5：WRITE_ERROR, 6：TIMEOUT_ERROR, 7：LAYOUT
        /// </param>
        private static void OnExceptionEvent(string taskid, string errorMessage, int errorCode)
        {
            FileUtils.LogRecrd($"{taskid}==={errorCode}==={errorMessage}", $"C++回调触发---信息");
            if (errorCode == 4 || errorCode == 5 || errorCode == 6 || errorCode == 7)
            {
                FileUtils.LogRecrd($"{taskid},{errorCode},{errorMessage}", $"C++错误回调触发---信息");
                // 异常报错
                if (AnchorBll.recordingList.Count > 0)
                {
                    foreach (var item in AnchorBll.recordingList)
                    {
                        AnchorRecordBll anchorRecordBll = item.Value;
                        if (taskid.Equals(anchorRecordBll.taskId))
                        {
                            Model.AnchorInfo anchorInfo = anchorRecordBll.GetAnchorInfo();
                            FileUtils.LogRecrd($"{anchorInfo?.AnchorName}", $"C++错误回调触发-主播");
                            anchorRecordBll.RecordEndHandle(errorCode);
                        }
                    }
                }
            }
            
        }

        private static void HandleException(string taskid, string errorMessage, int errorCode)
        {
            FileUtils.log($"Taskid:{taskid}, Error: {errorMessage}, Code: {errorCode}");
            // 触发C#事件或执行其他逻辑
        }

        public static void Shutdown()
        {
            stopEventLoop();
        }

        /// <summary>
        /// 开启日志和注册事件回调
        /// </summary>
        public static void Init()
        {
            setLog();
            Initialize();
        }

        public static void Test()
        {
            // 设置 DLL 搜索路径
            //SetDllDirectory(@".\libs");
            //FileUtils.log("Hello, World!");


            //setLog();

            //// 初始化事件回调
            //Initialize();


            //// addTask("http://pull-hls-l26.douyincdn.com/third/stream-7462182375015975732_sd.m3u8?expire=6798aa31&sign=e348922c8dbcd1b68e20f3d522e6b576&major_anchor_level=common","../test1.ts","1");
            ////addTask("http://pull-l3.douyincdn.com/third/stream-404832300954812895_sd.m3u8?auth_key=1738151119-0-0-57f77862db50e2eec50d5cf87194da1e&major_anchor_level=common", "../test2.ts", "2");
            //// addTask("http://pull-hls-l11.douyincdn.com/third/stream-7462593328828255013_sd.m3u8?expire=1738152917&sign=183a0012e59dfd27030af39e57f7f8ae&major_anchor_level=common","../test3.ts","3");
            //// addTask("http://pull-hls-l96.douyincdn.com/stage/stream-7462711406027229992_sd.m3u8?expire=1738152916&sign=175bfd32a888041813d0f77d03b2b46d&major_anchor_level=common","../test4.ts","4");
            //// addTask("http://pull-hls-l11.douyincdn.com/stage/stream-7460342589867281192_sd.m3u8?expire=1738058290&sign=7fbafdb6e2eb06aca7ae3b6487a426ad&major_anchor_level=common","../test5.ts","5");

            //startAll();

            //run();

            //Shutdown();
            //FileUtils.log($"Finish!");
        }
    }
}
