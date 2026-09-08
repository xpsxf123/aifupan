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
    public class TotalOnlineNum
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
        /// 记录的时间
        /// </summary>
        public string RecordDate { get; set; }

        /// <summary>
        /// 总在线人数
        /// </summary>
        public string PeopleNum { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string VideoId { get; set; }
        /// <summary>
        /// 开始记录的时间
        /// </summary>
        public string StartRecordDate { get; set; }

        private readonly string tableName = "total_online_num";

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<TotalOnlineNum> GetList()
        {
            SQLiteHelperTotalOnlineNum sQLiteHelper = new SQLiteHelperTotalOnlineNum();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<TotalOnlineNum>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 保存
        /// </summary>
        /// <returns></returns>
        public int Save() 
        {
            SQLiteHelperTotalOnlineNum sQLiteHelper = new SQLiteHelperTotalOnlineNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
             new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid),
             new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
             new SQLiteParameter("@"+nameof(this.RecordDate), this.RecordDate),
              new SQLiteParameter("@"+nameof(this.PeopleNum), this.PeopleNum),
              new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
              new SQLiteParameter("@"+nameof(this.StartRecordDate), this.StartRecordDate)
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
            SQLiteHelperTotalOnlineNum sQLiteHelper = new SQLiteHelperTotalOnlineNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
             new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid),
             new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
             new SQLiteParameter("@"+nameof(this.RecordDate), this.RecordDate),
              new SQLiteParameter("@"+nameof(this.PeopleNum), this.PeopleNum),
              new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
              new SQLiteParameter("@"+nameof(this.StartRecordDate), this.StartRecordDate)
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
        public void GetModelByBatchNumber(string batchNumber)
        {
            SQLiteHelperTotalOnlineNum sQLiteHelper = new SQLiteHelperTotalOnlineNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.BatchNumber), batchNumber)
            };
            List<TotalOnlineNum> result = sQLiteHelper.GetModelList<TotalOnlineNum>(tableName, "batch_number=@BatchNumber", parameters);
            if (result != null && result.Count() > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }

        }
    }
}
