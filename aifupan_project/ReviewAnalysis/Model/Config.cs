
using ReviewAnalysis.Db;
using System.Data.SQLite;

namespace ReviewAnalysis.Model
{
    public class Config
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 客户端序列号 每个客户端的唯一标识
        /// </summary>
        public string SerialNumber { get; set; }

        /// <summary>
        /// 检测频率  单位 毫秒/次
        /// </summary>
        public int DetectionFre { get; set; }

        /// <summary>
        /// 视频存储位置 
        /// </summary>
        public string SavePath { get; set; }

        /// <summary>
        /// 直播源  0 m3u8  1 flv
        /// </summary>
        public int LiveSource { get; set; }

        /// <summary>
        /// 限制类型 limit_type 0不限制  1限制时长，达到限制时长后将不在录制，2按时长分段，达到时长后自动分段录制，单位 分;3限制大小，达到限制大小后将停止录制，4按大小分段，达到大小后自动分段录制
        /// </summary>
        public int LimitType { get; set; }

        /// <summary>
        /// 录制限制大小 分钟或M  单位根据 limitType来确定
        /// </summary>
        public int LimitValue { get; set; }

        /// <summary>
        /// 0否 1是（此字段表示软件是否正在录制当中）
        /// </summary>
        public int IsRocord { get; set; }


        /// <summary>
        /// 主播pk的时候是否自动分段 is_subection
        /// </summary>
        public int IsSubection { get; set; }

        /// <summary>
        /// 隐藏时长  0否 1是
        /// </summary>
        public int HideDuration { get; set; }

        /// <summary>
        /// 隐藏大小  0否 1是
        /// </summary>
        public int HideSize { get; set; }

        /// <summary>
        /// 开播提示  0否  1是，设置此提示，将会在软件右下角自动弹出已经添加了的主播的直播提示
        /// </summary>
        public int LiveNotice { get; set; }

        /// <summary>
        /// 是否在主播列表显示在线人数
        /// </summary>
        public int OnlineNumber { get; set; }

        /// <summary>
        /// 自动删除时间 -1不删除 0马上删除 N天后删除（全局默认 -1，序列化到 systemConfig.txt）
        /// </summary>
        public string autoDeleteTime { get; set; }

        /// <summary>
        /// 删除内容 ts源视频/mp4成品/all都删（全局默认 ts，序列化到 systemConfig.txt）
        /// </summary>
        public string deleteContent { get; set; }

        private readonly string tableName = "config";

        /// <summary>
        /// 获取配置
        /// </summary>
        /// <returns></returns>
        public void GetModel() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            sQLiteHelper.GetModelById<Config>(this,tableName, 1);
           
        }

        public void update() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] updateParameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.DetectionFre),this.DetectionFre),
               new SQLiteParameter("@"+nameof(this.SavePath),this.SavePath),
               new SQLiteParameter("@"+nameof(this.LimitType),this.LimitType),
               new SQLiteParameter("@"+nameof(this.LimitValue),this.LimitValue),
               new SQLiteParameter("@"+nameof(this.IsRocord),this.IsRocord),
               new SQLiteParameter("@"+nameof(this.IsSubection),this.IsSubection),
               new SQLiteParameter("@"+nameof(this.HideDuration),this.HideDuration),
               new SQLiteParameter("@"+nameof(this.HideSize),this.HideSize),
               new SQLiteParameter("@"+nameof(this.LiveSource),this.LiveSource),
               new SQLiteParameter("@"+nameof(this.LiveNotice),this.LiveNotice),
               new SQLiteParameter("@"+nameof(this.OnlineNumber),this.OnlineNumber),
               new SQLiteParameter("@"+nameof(this.SerialNumber),this.SerialNumber)
            };
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
              new SQLiteParameter("@Id",this.Id)
            };
            sQLiteHelper.Update(tableName, updateParameters, whereParameters);
        }

        
    }
}
