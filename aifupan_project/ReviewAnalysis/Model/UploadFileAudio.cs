using ReviewAnalysis.Db;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Model
{
    public class UploadFileAudio
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 音频名称
        /// </summary>
        public string AudioName { get; set; }

        /// <summary>
        /// 对应的视频主键 
        /// </summary>
        public string FileId { get; set; }

        /// <summary>
        /// 第几段 一个视频可以分割成为多段音频
        /// </summary>
        public int Paragraph { get; set; }

        /// <summary>
        /// 音频格式  0wav、1pcm、2ogg-3opus、4speex、5silk、6mp3、7m4a、8aac、9 amr
        /// </summary>
        public string AudioType { get; set; }

        private readonly string tableName = "upload_file_audio";

        public int save() 
        {
            //audito_type  @AudioType
            SQLiteHelperUploadFileAudio sQLiteHelper = new SQLiteHelperUploadFileAudio();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.AudioName), this.AudioName),
             new SQLiteParameter("@"+nameof(this.FileId), this.FileId),
             new SQLiteParameter("@"+nameof(this.Paragraph), this.Paragraph),
             new SQLiteParameter("@"+nameof(this.AudioType), this.AudioType==null?"":this.AudioType)
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);
            return this.Id;
        }

        /// <summary>
        /// 根据文件id删除
        /// </summary>
        /// <param name="fileId">文件id</param>
        public void DeleteModelByFileId(string fileId)
        {
            SQLiteHelperUploadFileAudio sQLiteHelper = new SQLiteHelperUploadFileAudio();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.FileId)}", fileId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        ///  删除所有
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelperUploadFileAudio sQLiteHelper = new SQLiteHelperUploadFileAudio();
            sQLiteHelper.Delete(tableName, null);
        }
    }
}
