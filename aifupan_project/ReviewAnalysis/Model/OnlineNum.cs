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
    public class OnlineNum
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
        /// 在线人数
        /// </summary>
        public string PeopleNum { get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 在线人数数据 2024-11-12 17:30:47@2503_2024-11-12 17:31:09@9750
        /// </summary>
        public string PeopleNumData { get; set; }

        private readonly string tableName = "online_num";

        /// <summary>
        /// 保存在线人数
        /// </summary>
        /// <returns></returns>
        public int Save()
        {
            SQLiteHelperOnlineNum sQLiteHelper = new SQLiteHelperOnlineNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
             new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid),
             new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
             new SQLiteParameter("@"+nameof(this.RecordDate), this.RecordDate),
              new SQLiteParameter("@"+nameof(this.PeopleNum), this.PeopleNum),
              new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
              new SQLiteParameter("@"+nameof(this.PeopleNumData), this.PeopleNumData)
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);

            return this.Id;
        }

        /// <summary>
        /// 修改在线人数
        /// </summary>
        /// <returns></returns>
        public void Update()
        {
            SQLiteHelperOnlineNum sQLiteHelper = new SQLiteHelperOnlineNum();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.UserId), this.UserId),
             new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid),
             new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
             new SQLiteParameter("@"+nameof(this.RecordDate), this.RecordDate),
              new SQLiteParameter("@"+nameof(this.PeopleNum), this.PeopleNum),
              new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
              new SQLiteParameter("@"+nameof(this.PeopleNumData), this.PeopleNumData)
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
        public List<OnlineNum> GetList()
        {
            SQLiteHelperOnlineNum sQLiteHelper = new SQLiteHelperOnlineNum();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<OnlineNum>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 根据id删除
        /// </summary>
        /// <param name="id">id</param>
        public void DeleteModelById(int id)
        {
            SQLiteHelperOnlineNum sQLiteHelper = new SQLiteHelperOnlineNum();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.Id)}", id)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }
    }
}
