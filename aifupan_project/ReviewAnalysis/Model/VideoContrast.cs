using ReviewAnalysis.Asr;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Db;
using ReviewAnalysis.Dto;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.Model
{
    public class VideoContrast
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 对比唯一标识uuid
        /// </summary>
        public string ContrastId { get; set; }
        /// <summary>
        /// 是否已分享 0：否 1：是
        /// </summary>
        public int IsShard { get; set; }
        /// <summary>
        /// 在线对比复盘的地址
        /// </summary>
        public string ShareUrl { get; set; }

        /// <summary>
        /// 对比视频一Id
        /// </summary>
        public string VideoOneId { get; set; }

        /// <summary>
        /// 对比视频二Id
        /// </summary>
        public string VideoTwoId { get; set; }

        /// <summary>
        /// 对比主播一Id
        /// </summary>
        public string AnchorOneId { get; set; }

        /// <summary>
        /// 对比主播二Id
        /// </summary>
        public string AnchorTwoId { get; set; }

        /// <summary>
        /// 对比时间
        /// </summary>
        public string ContrastTime { get; set; }

        /// <summary>
        /// 对比文件一Id
        /// </summary>
        public string FileOneId { get; set; }

        /// <summary>
        /// 对比文件二Id
        /// </summary>
        public string FileTwoId { get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }
        /// <summary>
        /// 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
        /// </summary>
        public int DeleteStatus { get; set; }
        /// <summary>
        /// 租户id
        /// </summary>
        public long TenantId { get; set; }
        /// <summary>
        /// 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
        /// </summary>
        public int? syncScene { get; set; }
        /// <summary>
        /// 对比类型 0：视频对比 1：文件对比
        /// </summary>
        public int contrastType { get; set; }

        private readonly string tableName = "video_contrast";

        public int Save()
        {
            //audito_type  @AudioType
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.ContrastId), this.ContrastId),
             new SQLiteParameter("@"+nameof(this.VideoOneId), this.VideoOneId),
             new SQLiteParameter("@"+nameof(this.VideoTwoId), this.VideoTwoId),
             new SQLiteParameter("@"+nameof(this.AnchorOneId), this.AnchorOneId),
             new SQLiteParameter("@"+nameof(this.AnchorTwoId), this.AnchorTwoId),
             new SQLiteParameter("@"+nameof(this.ContrastTime), this.ContrastTime ?? ""),
             new SQLiteParameter("@"+nameof(this.FileOneId), this.FileOneId),
             new SQLiteParameter("@"+nameof(this.FileTwoId), this.FileTwoId),
             new SQLiteParameter("@"+nameof(this.UserId),this.UserId==null?"":this.UserId),
             new SQLiteParameter("@"+nameof(this.IsShard), this.IsShard),
              new SQLiteParameter("@"+nameof(this.ShareUrl),this.ShareUrl==null?"":this.ShareUrl),
             new SQLiteParameter("@"+nameof(this.DeleteStatus), this.DeleteStatus),
             new SQLiteParameter("@"+nameof(this.TenantId), this.TenantId)
             };
            this.Id = sQLiteHelper.Insert(tableName, parameters);
            return this.Id;
        }

        public void update()
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.ContrastId), this.ContrastId),
             new SQLiteParameter("@"+nameof(this.VideoOneId), this.VideoOneId),
             new SQLiteParameter("@"+nameof(this.VideoTwoId), this.VideoTwoId),
             new SQLiteParameter("@"+nameof(this.AnchorOneId), this.AnchorOneId),
             new SQLiteParameter("@"+nameof(this.AnchorTwoId), this.AnchorTwoId),
             new SQLiteParameter("@"+nameof(this.ContrastTime), this.ContrastTime ?? ""),
             new SQLiteParameter("@"+nameof(this.FileOneId), this.FileOneId),
             new SQLiteParameter("@"+nameof(this.FileTwoId), this.FileTwoId),
             new SQLiteParameter("@"+nameof(this.UserId),this.UserId==null?"":this.UserId),
             new SQLiteParameter("@"+nameof(this.IsShard), this.IsShard),
              new SQLiteParameter("@"+nameof(this.ShareUrl),this.ShareUrl==null?"":this.ShareUrl),
             new SQLiteParameter("@"+nameof(this.DeleteStatus), this.DeleteStatus),
             new SQLiteParameter("@"+nameof(this.TenantId), this.TenantId)
             };
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.Id),this.Id)
            };
            sQLiteHelper.Update(tableName, parameters, whereParameters);
        }

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<VideoContrast> GetList()
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<VideoContrast>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 根据id获取信息
        /// </summary>
        /// <param name="id"></param>
        public void GetModelById(int id)
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.Id), id)
            };
            List<VideoContrast> result = sQLiteHelper.GetModelList<VideoContrast>(tableName, "id=@Id", parameters);
            if (result != null && result.Count() > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }
        }

        /// <summary>
        /// 根据唯一标识获取对比信息
        /// </summary>
        /// <param name="contrastId"></param>
        public void GetModelByConstrastId(string contrastId)
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.ContrastId), contrastId)
            };
            List<VideoContrast> result = sQLiteHelper.GetModelList<VideoContrast>(tableName, "contrast_id=@ContrastId", parameters);
            if (result != null && result.Count() > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }
        }


        /// <summary>
        /// 删除视频关联的数据
        /// </summary>
        /// <param name="videoId"></param>
        public void DeleteModelByVideoId(string videoId)
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.VideoOneId)}", videoId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);

            SQLiteParameter[] whereParamters1 = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.VideoTwoId)}", videoId)
            };
            sQLiteHelper.Delete(tableName, whereParamters1);

        }

        /// <summary>
        /// 根据视频唯一标识获取对比列表
        /// </summary>
        /// <param name="videoId">视频唯一标识</param>
        public List<VideoContrast> ListByVideoId(string videoId)
        {
            this.UserId = ReplayHttpUtils.UserId;
            this.TenantId = ReplayHttpUtils.ActiveTenantId;
            List<VideoContrast> videoContrasts = GetList();
            if(videoContrasts != null && videoContrasts.Count > 0)
            {
                videoContrasts.RemoveAll(item => (!videoId.Equals(item.VideoOneId) && !videoId.Equals(item.VideoTwoId)) || !string.IsNullOrEmpty(item.FileOneId));

                return videoContrasts;
            }
            

            return null;
        }

        /// <summary>
        /// 删除文件关联的数据
        /// </summary>
        /// <param name="fileId"></param>
        public void DeleteModelByFileId(string fileId)
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.FileOneId)}", fileId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);

            SQLiteParameter[] whereParamters1 = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.FileTwoId)}", fileId)
            };
            sQLiteHelper.Delete(tableName, whereParamters1);
        }

        /// <summary>
        ///  删除所有对比数据
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelperVideoContrast sQLiteHelper = new SQLiteHelperVideoContrast();
            sQLiteHelper.Delete(tableName, null);
        }

        
    }
}
