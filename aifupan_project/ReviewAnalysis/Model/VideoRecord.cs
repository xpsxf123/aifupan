using ReviewAnalysis.Asr;
using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;


namespace ReviewAnalysis.Model
{
    public class VideoRecord
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
        /// 视频id 
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }

        /// <summary>
        /// 文件保存路径
        /// </summary>
        public string StoreFilePath { get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long TenantId { get; set; }
        /// <summary>
        /// 创建时间
        /// </summary>
        public string CreateDate { get; set; }


        private readonly string tableName = "video_record";

        /// <summary>
        /// 保存记录
        /// </summary>
        /// <returns></returns>
        public int Save()
        {
            SQLiteHelperVideoRecord sQLiteHelper = new SQLiteHelperVideoRecord();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
             new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
             new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId),
             new SQLiteParameter("@"+nameof(this.StoreFilePath), this.StoreFilePath),
              new SQLiteParameter("@"+nameof(this.TenantId), this.TenantId),
              new SQLiteParameter("@"+nameof(this.CreateDate), this.CreateDate)
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);

            return this.Id;
        }

        /// <summary>
        /// 修改记录
        /// </summary>
        /// <returns></returns>
        public void Update()
        {
            SQLiteHelperVideoRecord sQLiteHelper = new SQLiteHelperVideoRecord();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
             new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
             new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId),
             new SQLiteParameter("@"+nameof(this.StoreFilePath), this.StoreFilePath),
              new SQLiteParameter("@"+nameof(this.TenantId), this.TenantId),
              new SQLiteParameter("@"+nameof(this.CreateDate), this.CreateDate)
             };

            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.Id), this.Id)
            };
            sQLiteHelper.Update(tableName, parameters, whereParamters);

        }

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<VideoRecord> GetList()
        {
            SQLiteHelperVideoRecord sQLiteHelper = new SQLiteHelperVideoRecord();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<VideoRecord>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 根据id删除
        /// </summary>
        /// <param name="id">id</param>
        public void DeleteModelById(int id)
        {
            SQLiteHelperVideoRecord sQLiteHelper = new SQLiteHelperVideoRecord();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.Id)}", id)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 根据视频id删除关联的分析记录
        /// </summary>
        /// <param name="videoId">视频id</param>
        public void DeleteModelByVideoId(string videoId)
        {
            SQLiteHelperVideoRecord sQLiteHelper = new SQLiteHelperVideoRecord();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.VideoId)}", videoId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 根据视频id获取最后一条分析记录
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <returns></returns>
        public void GetByVideoId(string videoId)
        {
            SQLiteHelperVideoRecord sQLiteHelper = new SQLiteHelperVideoRecord();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.VideoId)}",videoId),
            };
            List<VideoRecord> list = sQLiteHelper.GetModelList<VideoRecord>(this.tableName, "video_id=@VideoId", whereParamters);
            if (list != null && list.Count > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, list[0]);
            }

        }
    }
}
