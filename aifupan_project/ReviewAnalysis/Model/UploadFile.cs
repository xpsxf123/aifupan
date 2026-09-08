using ReviewAnalysis.Db;
using ReviewAnalysis.Dto;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using ReviewAnalysis.Asr;
using ReviewAnalysis.Utils;
using ReviewAnalysis.entity.uploadFile;
using ReviewAnalysis.entity.video;

namespace ReviewAnalysis.Model
{
    public class UploadFile
    {
        /// <summary>
        /// 主键
        /// </summary>
        [JsonProperty(PropertyName = "id")]
        public int Id { get; set; }
        /// <summary>
        /// 文件的唯一标识id
        /// </summary>
        [JsonProperty(PropertyName = "fileId")]
        public string FileId { get; set; }
        /// <summary>
        /// 文件名称
        /// </summary>
        [JsonProperty(PropertyName = "fileName")]
        public string FileName { get; set; }
        /// <summary>
        /// 文件类型 0：视频 1：音频 2：文本
        /// </summary>
        [JsonProperty(PropertyName = "fileType")]
        public int FileType { get; set; }
        /// <summary>
        /// 文件原路径
        /// </summary>
        [JsonProperty(PropertyName = "originalPath")]
        public string OriginalPath { get; set; }
        /// <summary>
        /// 文件新路径
        /// </summary>
        [JsonProperty(PropertyName = "nowPath")]
        public string NowPath { get; set; }
        /// <summary>
        /// 分析状态 analysis_status 0：未分析 1：分析中 2：分析完成 3：分析失败
        /// </summary>
        [JsonProperty(PropertyName = "analysisStatus")]
        public int AnalysisStatus { get; set; }
        /// <summary>
        /// 分析完成时间 analysis_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        [JsonProperty(PropertyName = "analysisTime")]
        public string AnalysisTime { get; set; }
        /// <summary>
        /// 上传时间 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        [JsonProperty(PropertyName = "uploadTime")]
        public string UploadTime { get; set; }
        /// <summary>
        /// 文件大小，单位b
        /// </summary>
        [JsonProperty(PropertyName = "fileSize")]
        public string FileSize { get; set; }
        /// <summary>
        /// 文件时长，单位：秒
        /// </summary>
        [JsonProperty(PropertyName = "fileDuration")]
        public int FileDuration { get; set; }
        /// <summary>
        /// 文件字数，文本文件有
        /// </summary>
        [JsonProperty(PropertyName = "fileWordNum")]
        public int FileWordNum { get; set; }
        /// <summary>
        /// 分析失败原因
        /// </summary>
        [JsonProperty(PropertyName = "errorReason")]
        public string ErrorReason { get; set; }
        /// <summary>
        /// 行业id
        /// </summary>
        [JsonProperty(PropertyName = "tradeId")]
        public string TradeId { get; set; }
        /// <summary>
        /// 平台类型
        /// </summary>
        [JsonProperty(PropertyName = "platformType")]
        public string PlatformType { get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        [JsonProperty(PropertyName = "userId")]
        public string UserId { get; set; }
        /// <summary>
        /// 租户id
        /// </summary>
        [JsonProperty(PropertyName = "tenantId")]
        public string TenantId { get; set; }
        /// <summary>
        /// 是否已上传到服务器 0：否 1：是
        /// </summary>
        [JsonProperty(PropertyName = "uploadStatus")]
        public int UploadStatus { get; set; }
        /// <summary>
        /// 是否已标注过词语 0：否 1：是
        /// </summary>
        [JsonProperty(PropertyName = "isMark")]
        public int IsMark { get; set; }
        /// <summary>
        /// 在线复盘url
        /// </summary>
        [JsonProperty(PropertyName = "shareUrl")]
        public string ShareUrl { get; set; }
        /// <summary>
        /// 在线文件的url
        /// </summary>
        [JsonProperty(PropertyName = "playUrl")]
        public string PlayUrl { get; set; }
        /// <summary>
        /// 占用云空间的大小，单位：M
        /// </summary>
        [JsonProperty(PropertyName = "cloudStore")]
        public int CloudStore { get; set; }

        /// <summary>
        /// 文件切片类型 0：原文件 1：复盘切片视频 2：短视频切片视频
        /// </summary>
        public int? fileSliceType { get; set; }
        /// <summary>
        /// 文件切片信息
        /// </summary>
        public VideoSliceEntity videoSliceInfo { get; set; }
        /// <summary>
        /// 切片视频所属原文件信息
        /// </summary>
        public UploadFileEntity parentFileInfo { get; set; }


        private readonly string tableName = "upload_file";

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<UploadFile> GetList()
        {
            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<UploadFile>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 插入数据 返回插入的主键
        /// </summary>
        public void Save(bool syncServer = true)
        {
            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.FileId), this.FileId),
             new SQLiteParameter("@"+nameof(this.FileName), this.FileName),
             new SQLiteParameter("@"+nameof(this.FileType), this.FileType),
             new SQLiteParameter("@"+nameof(this.OriginalPath), this.OriginalPath),
             new SQLiteParameter("@"+nameof(this.NowPath), this.NowPath),
             new SQLiteParameter("@"+nameof(this.AnalysisStatus), this.AnalysisStatus),
             new SQLiteParameter("@"+nameof(this.AnalysisTime), this.AnalysisTime),
             new SQLiteParameter("@"+nameof(this.UploadTime), this.UploadTime),
             new SQLiteParameter("@"+nameof(this.FileSize), this.FileSize ),
             new SQLiteParameter("@"+nameof(this.FileDuration), this.FileDuration ),
             new SQLiteParameter("@"+nameof(this.FileWordNum), this.FileWordNum ),
             new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId ),
             new SQLiteParameter("@"+nameof(this.PlatformType), this.PlatformType ),
             new SQLiteParameter("@"+nameof(this.UserId),this.UserId==null?"":this.UserId),
             new SQLiteParameter("@"+nameof(this.UploadStatus), this.UploadStatus),
             new SQLiteParameter("@"+nameof(this.IsMark), this.IsMark),
             new SQLiteParameter("@"+nameof(this.ShareUrl),this.ShareUrl==null?"":this.ShareUrl),
             new SQLiteParameter("@"+nameof(this.PlayUrl),this.PlayUrl==null?"":this.PlayUrl),
             new SQLiteParameter("@"+nameof(this.CloudStore), this.CloudStore)
             };
            sQLiteHelper.Insert(tableName, parameters);

            this.Id = sQLiteHelper.SelectLastId(tableName);

            if(syncServer)
            {
                ReplayHttpUtils.SaveFileToServer(this);
            }
            
        }

        public void update()
        {
            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.FileId), this.FileId),
              new SQLiteParameter("@"+nameof(this.FileName), this.FileName),
             new SQLiteParameter("@"+nameof(this.FileType), this.FileType),
             new SQLiteParameter("@"+nameof(this.OriginalPath), this.OriginalPath),
             new SQLiteParameter("@"+nameof(this.NowPath), this.NowPath),
             new SQLiteParameter("@"+nameof(this.AnalysisStatus), this.AnalysisStatus),
             new SQLiteParameter("@"+nameof(this.AnalysisTime), this.AnalysisTime),
             new SQLiteParameter("@"+nameof(this.UploadTime), this.UploadTime),
             new SQLiteParameter("@"+nameof(this.FileSize), this.FileSize ),
              new SQLiteParameter("@"+nameof(this.FileDuration), this.FileDuration ),
              new SQLiteParameter("@"+nameof(this.FileWordNum), this.FileWordNum ),
             new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId ),
             new SQLiteParameter("@"+nameof(this.PlatformType), this.PlatformType ),
             new SQLiteParameter("@"+nameof(this.UserId),this.UserId==null?"":this.UserId),
             new SQLiteParameter("@"+nameof(this.UploadStatus), this.UploadStatus),
             new SQLiteParameter("@"+nameof(this.IsMark), this.IsMark),
             new SQLiteParameter("@"+nameof(this.ShareUrl),this.ShareUrl==null?"":this.ShareUrl),
             new SQLiteParameter("@"+nameof(this.PlayUrl),this.PlayUrl==null?"":this.PlayUrl),
             new SQLiteParameter("@"+nameof(this.CloudStore), this.CloudStore)
             };
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.Id),this.Id)
            };
            sQLiteHelper.Update(tableName, parameters, whereParameters);
            // 同步到服务器
            ReplayHttpUtils.UpdateFileToServer(this);
        }

        /// <summary>
        /// 修改文件的分析状态
        /// </summary>
        /// <param name="fileId">视频id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：识别失败</param>
        /// <param name="errorReason">分析失败原因</param>
        /// <exception cref="NotImplementedException"></exception>
        public void UpdateAnalysisStatus(string fileId, int analysisStatus, string errorReason)
        {
            GetModelByFileId(fileId);
            this.AnalysisStatus = analysisStatus;

            List<SQLiteParameter> list = new List<SQLiteParameter>();
            list.Add(new SQLiteParameter("@AnalysisStatus", analysisStatus));
            if(errorReason != null)
            {
                this.ErrorReason = errorReason;
                list.Add(new SQLiteParameter("@ErrorReason", errorReason));
            }
            
            if (analysisStatus == 2)
            {
                this.AnalysisTime = ServerTimeUtils.getCurrentTimeStr();

                //if (this.FileType == 0 || this.FileType == 1)
                //{
                //    this.IsMark = 1;
                //    list.Add(new SQLiteParameter("@IsMark", 1));
                //}
                this.IsMark = 1;
                list.Add(new SQLiteParameter("@IsMark", 1));
                list.Add(new SQLiteParameter("@AnalysisTime", this.AnalysisTime));
            }

            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            SQLiteParameter[] parameters = list.ToArray();
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@FileId", fileId)
            };
            sQLiteHelper.Update(tableName, parameters, whereParameters);

            // 同步修改到服务器
            ReplayHttpUtils.UpdateFileToServer(this);
        }

        public PageDto<UploadFileDto> GetPage(UploadFilePageQueryDto uploadFilePageQueryDto)
        {

            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            if (uploadFilePageQueryDto.PageIndex < 1)
            {
                uploadFilePageQueryDto.PageIndex = 1;
            }
            if (uploadFilePageQueryDto.PageSize < 1)
            {
                uploadFilePageQueryDto.PageSize = 10;
            }

            List<SQLiteParameter> list = new List<SQLiteParameter>();

            /// 构建条件
            string sqlWhereStr = " 1 = 1 ";

            list.Add(new SQLiteParameter("@UserId", uploadFilePageQueryDto.UserId));
            sqlWhereStr += " and user_id=@UserId ";

            if (uploadFilePageQueryDto.AnalysisStatus != -1)
            {
                list.Add(new SQLiteParameter("@AnalysisStatus", uploadFilePageQueryDto.AnalysisStatus));
                sqlWhereStr += " and analysis_status=@AnalysisStatus ";
            }
            if (uploadFilePageQueryDto.FileName != "")
            {
                list.Add(new SQLiteParameter("@FileName", $"%{uploadFilePageQueryDto.FileName}%"));
                sqlWhereStr += " and file_name like @FileName ";
            }
            if (!string.IsNullOrEmpty(uploadFilePageQueryDto.AnalysisStartDate))
            {
                uploadFilePageQueryDto.AnalysisStartDate = uploadFilePageQueryDto.AnalysisStartDate + " 00:00:00";
                list.Add(new SQLiteParameter("@AnalysisStartDate", uploadFilePageQueryDto.AnalysisStartDate));
                sqlWhereStr += " and analysis_time >= @AnalysisStartDate ";
            }
            if (!string.IsNullOrEmpty(uploadFilePageQueryDto.AnalysisEndDate))
            {
                uploadFilePageQueryDto.AnalysisEndDate = uploadFilePageQueryDto.AnalysisEndDate + " 23:59:59";
                list.Add(new SQLiteParameter("@AnalysisEndDate", uploadFilePageQueryDto.AnalysisEndDate));
                sqlWhereStr += " and analysis_time <= @AnalysisEndDate ";
            }
            if (!string.IsNullOrEmpty(uploadFilePageQueryDto.UpdateStartDate))
            {
                uploadFilePageQueryDto.UpdateStartDate = uploadFilePageQueryDto.UpdateStartDate + " 00:00:00";
                list.Add(new SQLiteParameter("@UpdateStartDate", uploadFilePageQueryDto.UpdateStartDate));
                sqlWhereStr += " and upload_time >= @UpdateStartDate ";
            }
            if (!string.IsNullOrEmpty(uploadFilePageQueryDto.UpdateEndDate))
            {
                uploadFilePageQueryDto.UpdateEndDate = uploadFilePageQueryDto.UpdateEndDate + " 23:59:59";
                list.Add(new SQLiteParameter("@UpdateEndDate", uploadFilePageQueryDto.UpdateEndDate));
                sqlWhereStr += " and upload_time <= @RecordEndDate ";
            }

            SQLiteParameter[] parameters = list.ToArray();

            if (uploadFilePageQueryDto.AnalysisStatus == 2)
            {
                return sQLiteHelper.ExecutePagedQueryOrderByTime<UploadFileDto>(tableName, uploadFilePageQueryDto.PageSize, uploadFilePageQueryDto.PageIndex, sqlWhereStr, parameters);
            }
            else
            {
                return sQLiteHelper.ExecutePagedQuery<UploadFileDto>(tableName, uploadFilePageQueryDto.PageSize, uploadFilePageQueryDto.PageIndex, sqlWhereStr, parameters);
            }
        }

        //public void GetModelById(int id)
        //{
        //    SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
        //    SQLiteParameter[] parameters = new SQLiteParameter[]
        //    {
        //       new SQLiteParameter("@"+nameof(this.Id), id)
        //    };
        //    List<UploadFile> result = sQLiteHelper.GetModelList<UploadFile>(tableName, "id=@Id", parameters);
        //    if (result != null && result.Count() > 0)
        //    {
        //        sQLiteHelper.AssignPropertiesFrom(this, result[0]);
        //    }
        //}

        /// <summary>
        /// 根据fileId获取文件信息
        /// </summary>
        /// <param name="fileId"></param>
        public void GetModelByFileId(string fileId)
        {
            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.FileId), fileId)
            };
            List<UploadFile> result = sQLiteHelper.GetModelList<UploadFile>(tableName, "file_id=@FileId", parameters);
            if (result != null && result.Count() > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }
        }

        /// <summary>
        /// 设置文件的行业
        /// </summary>
        /// <param name="fileId">文件id</param>
        /// <param name="tradeId">行业id</param>
        public void UpdateTradeId(string fileId, string tradeId)
        {
            List<SQLiteParameter> list = new List<SQLiteParameter>();
            list.Add(new SQLiteParameter("@TradeId", tradeId));

            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            SQLiteParameter[] parameters = list.ToArray();
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@FileId", fileId)
            };
            sQLiteHelper.Update(tableName, parameters, whereParameters);
        }

        /// <summary>
        ///  根据文件id删除文件
        /// </summary>
        /// <param name="fileId">文件id</param>
        public void DeleteById(string fileId)
        {
            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.FileId)}", fileId)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 删除所有文件
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelperUploadFile sQLiteHelper = new SQLiteHelperUploadFile();
            sQLiteHelper.Delete(tableName, null);
        }
    }
}
