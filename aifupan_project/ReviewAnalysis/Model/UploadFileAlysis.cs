using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ReviewAnalysis.Model
{
    public class UploadFileAlysis
    {
        /// <summary>
        /// 主键
        /// </summary>
        [JsonProperty(PropertyName = "id")]
        public int Id { get; set; }

        /// <summary>
        /// 文件id 
        /// </summary>
        [JsonProperty(PropertyName = "fileId")]
        public string FileId { get; set; }

        /// <summary>
        /// 文件的第几段音频 从1开始
        /// </summary>
        [JsonProperty(PropertyName = "paragraph")]
        public int Paragraph { get; set; }

        /// <summary>
        /// 识别状态  0：成功 1：失败
        /// </summary>
        [JsonProperty(PropertyName = "status")]
        public int Status { get; set; }

        /// <summary>
        /// 词语json字符串内容
        /// </summary>
        [JsonProperty(PropertyName = "dataJson")]
        public string DataJson { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty(PropertyName = "tradeId")]
        public string TradeId { get; set; }

        private readonly string tableName = "upload_file_analysis";

        public int save()
        {
            //audito_type  @AudioType
            SQLiteHelperUploadFileAnalysis sQLiteHelper = new SQLiteHelperUploadFileAnalysis();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.FileId), this.FileId),
             new SQLiteParameter("@"+nameof(this.Paragraph), this.Paragraph),
             new SQLiteParameter("@"+nameof(this.Status), this.Status),
             new SQLiteParameter("@"+nameof(this.DataJson), this.DataJson==null?"":this.DataJson),
             new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId)
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);
            return this.Id;
        }

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<UploadFileAlysis> GetList()
        {
            SQLiteHelperUploadFileAnalysis sQLiteHelper = new SQLiteHelperUploadFileAnalysis();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<UploadFileAlysis>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 根据文件id删除
        /// </summary>
        /// <param name="fileId">文件id</param>
        public void DeleteModelByFileId(string fileId)
        {
            SQLiteHelperUploadFileAnalysis sQLiteHelper = new SQLiteHelperUploadFileAnalysis();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.FileId)}", fileId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 根据文件id和行业id删除
        /// </summary>
        /// <param name="fileId">文件id</param>
        public void DeleteModelByFileIdAndTradeId(string fileId, string tradeId)
        {
            SQLiteHelperUploadFileAnalysis sQLiteHelper = new SQLiteHelperUploadFileAnalysis();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.FileId)}", fileId),
               new SQLiteParameter($"@{nameof(this.TradeId)}", tradeId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        ///  删除所有分析记录
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelperUploadFileAnalysis sQLiteHelper = new SQLiteHelperUploadFileAnalysis();
            sQLiteHelper.Delete(tableName, null);
        }

    }
}
