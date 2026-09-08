using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Model
{
    public class AudioaAlysis
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 视频唯一标识Id
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 视频的第几段音频 从1开始
        /// </summary>
        public int Paragraph { get; set; }

        /// <summary>
        /// 识别状态  0：成功 1：失败
        /// </summary>
        public int Status { get; set; }

        /// <summary>
        /// 词语json字符串内容
        /// </summary>
        public string DataJson { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }

        private readonly string tableName = "audio_analysis";

        public int save()
        {
            //audito_type  @AudioType
            SQLiteHelperAudioAnalysis sQLiteHelper = new SQLiteHelperAudioAnalysis();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
             new SQLiteParameter("@"+nameof(this.Paragraph), this.Paragraph),
             new SQLiteParameter("@"+nameof(this.Status), this.Status),
             new SQLiteParameter("@"+nameof(this.DataJson), this.DataJson==null?"":this.DataJson),
              new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId==null?"":this.TradeId)
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);
            return this.Id;
        }

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<AudioaAlysis> GetList()
        {
            SQLiteHelperAudioAnalysis sQLiteHelper = new SQLiteHelperAudioAnalysis();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<AudioaAlysis>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 根据视频id删除
        /// </summary>
        /// <param name="fileId">videoId</param>
        public void DeleteModelByVideoId(string videoId)
        {
            SQLiteHelperAudioAnalysis sQLiteHelper = new SQLiteHelperAudioAnalysis();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.VideoId)}", videoId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 根据视频id和行业id删除
        /// </summary>
        /// <param name="videoId">视频id</param>
        public void DeleteModelByFileIdAndTradeId(string videoId, string tradeId)
        {
            SQLiteHelperAudioAnalysis sQLiteHelper = new SQLiteHelperAudioAnalysis();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.VideoId)}", videoId),
               new SQLiteParameter($"@{nameof(this.TradeId)}", tradeId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 删除所有
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelperAudioAnalysis sQLiteHelper = new SQLiteHelperAudioAnalysis();
            sQLiteHelper.Delete(tableName, null);
        }
    }
}
