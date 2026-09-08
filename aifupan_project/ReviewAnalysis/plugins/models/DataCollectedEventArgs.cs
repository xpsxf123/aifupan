using System;

namespace ReviewAnalysis.plugins.models
{
    /// <summary>
    /// 数据采集完成事件参数
    /// </summary>
    public class DataCollectedEventArgs : EventArgs
    {
        /// <summary>
        /// 模块ID
        /// </summary>
        public string ModuleId { get; set; }

        /// <summary>
        /// 模块名称
        /// </summary>
        public string ModuleName { get; set; }

        /// <summary>
        /// 主播ID
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 批次号（通常是roomId）
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        /// 视频ID
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 直播间ID
        /// </summary>
        public string RoomId { get; set; }

        /// <summary>
        /// 数据类型：0=实时数据，1=汇总数据
        /// </summary>
        public int DataType { get; set; }

        /// <summary>
        /// 数据JSON
        /// </summary>
        public string DataJson { get; set; }

        /// <summary>
        /// 采集时间
        /// </summary>
        public DateTime CollectTime { get; set; }

        /// <summary>
        /// 是否采集成功
        /// </summary>
        public bool Success { get; set; }

        /// <summary>
        /// 错误信息
        /// </summary>
        public string ErrorMessage { get; set; }

        /// <summary>
        /// 创建成功事件
        /// </summary>
        public static DataCollectedEventArgs SuccessEvent(
            string moduleId, string moduleName,
            string secUid, string batchNumber, string videoId, string roomId,
            int dataType, string dataJson)
        {
            return new DataCollectedEventArgs
            {
                ModuleId = moduleId,
                ModuleName = moduleName,
                SecUid = secUid,
                BatchNumber = batchNumber,
                VideoId = videoId,
                RoomId = roomId,
                DataType = dataType,
                DataJson = dataJson,
                CollectTime = DateTime.Now,
                Success = true
            };
        }

        /// <summary>
        /// 创建失败事件
        /// </summary>
        public static DataCollectedEventArgs FailEvent(
            string moduleId, string moduleName,
            string secUid, string batchNumber, string videoId, string roomId,
            string errorMessage)
        {
            return new DataCollectedEventArgs
            {
                ModuleId = moduleId,
                ModuleName = moduleName,
                SecUid = secUid,
                BatchNumber = batchNumber,
                VideoId = videoId,
                RoomId = roomId,
                CollectTime = DateTime.Now,
                Success = false,
                ErrorMessage = errorMessage
            };
        }
    }
}
