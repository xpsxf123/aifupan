using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Model
{
    public class RecordLogs
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 目前用来记录主播的个人主页地址
        /// </summary>
        public string FileName { get; set; }

        private readonly string tableName = "record_logs";

        public void save() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.FileName), this.FileName)
            };
            sQLiteHelper.Insert(tableName, parameters);
        }

        public bool isExitsFileByFileName(string fileName) 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.FileName), fileName)
            };
            return Convert.ToInt32(sQLiteHelper.GetCountFunction(tableName, "id", parameters)) > 0;
        }

        public List<RecordLogs> GetAll() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            return sQLiteHelper.GetModelList<RecordLogs>(this.tableName);
        }

        public bool DeleteAll() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            return sQLiteHelper.Delete(tableName) > 0;
        }

        public bool Delete() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.Id), this.Id)
            };
            return sQLiteHelper.Delete(tableName) > 0;
        }
    }
}
