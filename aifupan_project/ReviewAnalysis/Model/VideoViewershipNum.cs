using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Model
{
    public class VideoViewershipNum
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }

        /// <summary>
        /// 主播secuid 
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 直播场次号
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 总在线人数
        /// </summary>
        public string ViewershipNum { get; set; }

        /// <summary>
        /// 弹幕总数
        /// </summary>
        public string BarrageNum { get; set; }

        /// <summary>
        /// 记录的时间
        /// </summary>
        public string UpdateDate { get; set; }

        /// <summary>
        /// 开始记录的时间
        /// </summary>
        public string CreateDate { get; set; }

        private readonly string tableName = "video_viewership_num";

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<VideoViewershipNum> GetList()
        {
            SQLiteHelperVideoViewershipNum sQLiteHelper = new SQLiteHelperVideoViewershipNum();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<VideoViewershipNum>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 保存
        /// </summary>
        /// <returns></returns>
        public int Save()
        {
            SQLiteHelperVideoViewershipNum sQLiteHelper = new SQLiteHelperVideoViewershipNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
                new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid),
                new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
                new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
                new SQLiteParameter("@"+nameof(this.ViewershipNum), this.ViewershipNum),
                new SQLiteParameter("@"+nameof(this.BarrageNum), this.BarrageNum),
                new SQLiteParameter("@"+nameof(this.CreateDate), this.CreateDate),
                new SQLiteParameter("@"+nameof(this.UpdateDate), this.UpdateDate),
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);

            //ReplayHttpUtils.UpdateTotalOnlineNum(this);

            return this.Id;
        }

        /// <summary>
        /// 修改
        /// </summary>
        /// <returns></returns>
        public void Update()
        {
            SQLiteHelperVideoViewershipNum sQLiteHelper = new SQLiteHelperVideoViewershipNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
                new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid),
                new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
                new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
                new SQLiteParameter("@"+nameof(this.ViewershipNum), this.ViewershipNum),
                new SQLiteParameter("@"+nameof(this.BarrageNum), this.BarrageNum),
                new SQLiteParameter("@"+nameof(this.CreateDate), this.CreateDate),
                new SQLiteParameter("@"+nameof(this.UpdateDate), this.UpdateDate),
             };

            SQLiteParameter[] whereParameters = new SQLiteParameter[]
           {
                new SQLiteParameter("@"+nameof(this.Id),this.Id)
           };
            sQLiteHelper.Update(tableName, parameters, whereParameters);

            //ReplayHttpUtils.UpdateTotalOnlineNum(this);
        }

        /// <summary>
        /// 根据直播场次号获取信息
        /// </summary>
        /// <param name="batchNumber"></param>
        public void GetModelByBatchNumber(string batchNumber, string videoId)
        {
            SQLiteHelperVideoViewershipNum sQLiteHelper = new SQLiteHelperVideoViewershipNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.BatchNumber), batchNumber),
               new SQLiteParameter("@"+nameof(this.VideoId), videoId)
            };
            List<VideoViewershipNum> result = sQLiteHelper.GetModelList<VideoViewershipNum>(tableName, "batch_number=@BatchNumber AND video_id=@VideoId", parameters);
            if (result != null && result.Count() > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }

        }
    }
}
