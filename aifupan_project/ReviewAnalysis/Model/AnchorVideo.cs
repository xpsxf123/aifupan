using ReviewAnalysis.Asr;
using ReviewAnalysis.Bll;
using ReviewAnalysis.DataCache;
using ReviewAnalysis.Db;
using ReviewAnalysis.Dto;
using ReviewAnalysis.entity.video;
using ReviewAnalysis.Utils;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Drawing.Printing;
using System.Linq;
using System.Web.UI.WebControls;
using System.Windows.Forms;

namespace ReviewAnalysis.Model
{
    public class AnchorVideo
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 视频唯一标识id
        /// </summary>
        public string VideoId { get; set; }

        /// <summary>
        /// 视频文件名称
        /// </summary>
        public string VideoName { get; set; }

        /// <summary>
        /// 开始录制时间 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string StartTime { get; set;}


        /// <summary>
        /// 第几段视频（每次直播可以分段录制）
        /// </summary>
        public int Paragraph { get; set; }


        /// <summary>
        /// 分段类型 0不分段，1按时长分段，2按大小分段
        /// </summary>
        public int SubsectionType { get; set; }


        /// <summary>
        /// 每段视频的时长 (单位:秒)
        /// </summary>
        public string Duration { get; set; }

        /// <summary>
        /// 视频大小  (单位：M)
        /// </summary>
        public string VedioSizie { get; set; }

        /// <summary>
        /// 结束录制时间 end_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string EndTime { get; set; }


        /// <summary>
        /// 归属批次号 批次号是指：在录制一个视频中，如果采用分段录制，那么这几段视频归属于同一批次 目前是直播间的id roomId
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        ///视频类型  0 ts,1 flv,2 mp4
        /// </summary>
        public int VideoType { get; set; }

        /// <summary>
        /// 清晰度 definition 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public int Definition { get; set; }

        /// <summary>
        /// 存储路径 
        /// </summary>
        public string StoragePath { get; set; }

        /// <summary>
        /// 直播源类型 source_type 0 m3u8 1 flv
        /// </summary>
        public int SourceType { get; set; }

        /// <summary>
        /// 直播源地址 
        /// </summary>
        public string SourceUrl { get; set; }

        /// <summary>
        /// 主播表主键 
        /// </summary>
        public int AnchorId { get; set; }

        /// <summary>
        /// 直播标题 live_title
        /// </summary>
        public string LiveTitle { get; set; }

        /// <summary>
        /// 是否正在录制 is_recording 0否 1是
        /// </summary>
        public int IsRecording { get; set; }

        /// <summary>
        /// 分析完成时间 analysis_time 格式：yyyy-MM-dd hh:mm:ss
        /// </summary>
        public string AnalysisTime { get; set; }

        /// <summary>
        /// 分析状态 analysis_status 0：未分析 1：分析中 2：分析完成 3：分析失败
        /// </summary>
        public int AnalysisStatus { get; set; }
        /// <summary>
        /// 分析失败原因
        /// </summary>
        public string ErrorReason { get; set; }

        /// <summary>
        /// 主播的唯一Id
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }
        /// <summary>
        /// 平台类型
        /// </summary>
        public string PlatformType { get; set; }
        /// <summary>
        /// 用户id
        /// </summary>
        public string UserId { get; set; }
        /// <summary>
        /// 是否已上传到服务器 0：否 1：是 2：上传中
        /// </summary>
        public int UploadStatus { get; set; }
        /// <summary>
        /// 是否已标注词语 0：否 1：是
        /// </summary>
        public int IsMark { get; set; }
        /// <summary>
        /// 在线复盘url
        /// </summary>
        public string ShareUrl { get; set; }
        /// <summary>
        /// 在线文件的url
        /// </summary>
        public string PlayUrl { get; set; }
        /// <summary>
        /// 占用云空间的大小，单位：M
        /// </summary>
        public int CloudStore { get; set; }
        /// <summary>
        /// 视频删除状态 0：未删除 1：已从复盘列表删除 2：已从复盘列表和云空间删除
        /// </summary>
        public int DeleteStatus { get; set; }
        /// <summary>
        /// 租户id
        /// </summary>
        public long TenantId { get; set; }
        /// <summary>
        /// 最后修改时间
        /// </summary>
        public string UpdateDate { get; set; }

        /// <summary>
        /// 视频切片类型 0：原视频 1：复盘切片视频 2：短视频切片视频
        /// </summary>
        public int? videoSliceType { get; set; }
        /// <summary>
        /// 视频切片信息
        /// </summary>
        public VideoSliceEntity videoSliceInfo { get; set; }
        /// <summary>
        /// 切片视频所属原视频信息
        /// </summary>
        public VideoEntity parentVideoInfo { get; set; }
        /// <summary>
        /// 原视频下的所有切片视频信息
        /// </summary>
        public List<VideoSliceEntity> sliceList { get; set; }
        /// <summary>
        /// 是否有AI优化目的 0：否 1：是
        /// </summary>
        public int? hasAiOptimizePurpose { get; set; }
        /// <summary>
        /// 云空间备注
        /// </summary>
        public string cloudRemarks { get; set; }
        /// <summary>
        /// 重命名
        /// </summary>
        public string videoRename { get; set; }
        /// <summary>
        /// 云空间重命名
        /// </summary>
        public string cloudRename { get; set; }




        private readonly string tableName = "anchor_video";

        /// <summary>
        /// 插入数据 返回插入的主键
        /// </summary>
        public void save() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.VideoId), this.VideoId),
             new SQLiteParameter("@"+nameof(this.VideoName), this.VideoName),
             new SQLiteParameter("@"+nameof(this.StartTime), this.StartTime),
             new SQLiteParameter("@"+nameof(this.Paragraph), this.Paragraph),
             new SQLiteParameter("@"+nameof(this.SubsectionType), this.SubsectionType),
             new SQLiteParameter("@"+nameof(this.Duration), this.Duration),
             new SQLiteParameter("@"+nameof(this.EndTime), this.EndTime==null?"":this.EndTime),
             new SQLiteParameter("@"+nameof(this.BatchNumber), this.BatchNumber),
             new SQLiteParameter("@"+nameof(this.VideoType), this.VideoType),
             new SQLiteParameter("@"+nameof(this.StoragePath), this.StoragePath),
             new SQLiteParameter("@"+nameof(this.SourceType), this.SourceType),
             new SQLiteParameter("@"+nameof(this.SourceUrl), this.SourceUrl),
             new SQLiteParameter("@"+nameof(this.VedioSizie),this.VedioSizie==null?"":this.VedioSizie),
             new SQLiteParameter("@"+nameof(this.AnchorId), this.AnchorId),
             new SQLiteParameter("@"+nameof(this.IsRecording), this.IsRecording),
             new SQLiteParameter("@"+nameof(this.Definition),this.Definition),
             new SQLiteParameter("@"+nameof(this.LiveTitle),this.LiveTitle),
             new SQLiteParameter("@"+nameof(this.AnalysisTime), this.AnalysisTime==null?"":this.AnalysisTime),
             new SQLiteParameter("@"+nameof(this.AnalysisStatus), this.AnalysisStatus),
             new SQLiteParameter("@"+nameof(this.ErrorReason), this.ErrorReason==null?"":this.ErrorReason),
              new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid==null?"":this.SecUid),
               new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId==null?"":this.TradeId),
               new SQLiteParameter("@"+nameof(this.PlatformType), this.PlatformType==null?"":this.PlatformType),
             new SQLiteParameter("@"+nameof(this.UserId),this.UserId==null?"":this.UserId),
             new SQLiteParameter("@"+nameof(this.UploadStatus), this.UploadStatus),
             new SQLiteParameter("@"+nameof(this.IsMark), this.IsMark),
             new SQLiteParameter("@"+nameof(this.ShareUrl),this.ShareUrl==null?"":this.ShareUrl),
             new SQLiteParameter("@"+nameof(this.PlayUrl),this.PlayUrl==null?"":this.PlayUrl),
             new SQLiteParameter("@"+nameof(this.CloudStore), this.CloudStore),
             new SQLiteParameter("@"+nameof(this.DeleteStatus), this.DeleteStatus),
             new SQLiteParameter("@"+nameof(this.TenantId), this.TenantId)
             };
            this.Id= sQLiteHelper.Insert(tableName, parameters);
        }

        public void update()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.VideoName), this.VideoName),
             new SQLiteParameter("@"+nameof(this.StartTime), this.StartTime),
             new SQLiteParameter("@"+nameof(this.Paragraph), this.Paragraph),
             new SQLiteParameter("@"+nameof(this.SubsectionType), this.SubsectionType),
             new SQLiteParameter("@"+nameof(this.Duration), this.Duration),
             new SQLiteParameter("@"+nameof(this.EndTime), this.EndTime==null?"":this.EndTime),
             new SQLiteParameter("@"+nameof(this.VideoType), this.VideoType),
             new SQLiteParameter("@"+nameof(this.StoragePath), this.StoragePath),
             new SQLiteParameter("@"+nameof(this.SourceType), this.SourceType),
             new SQLiteParameter("@"+nameof(this.SourceUrl), this.SourceUrl),
             new SQLiteParameter("@"+nameof(this.VedioSizie),this.VedioSizie==null?"":this.VedioSizie),
             new SQLiteParameter("@"+nameof(this.IsRecording), this.IsRecording),
             new SQLiteParameter("@"+nameof(this.Definition),this.Definition),
             new SQLiteParameter("@"+nameof(this.AnchorId), this.AnchorId),
              new SQLiteParameter("@"+nameof(this.LiveTitle),this.LiveTitle),
             new SQLiteParameter("@"+nameof(this.AnalysisTime), this.AnalysisTime==null?"":this.AnalysisTime),
             new SQLiteParameter("@"+nameof(this.AnalysisStatus), this.AnalysisStatus),
             new SQLiteParameter("@"+nameof(this.ErrorReason), this.ErrorReason==null?"":this.ErrorReason),
             new SQLiteParameter("@"+nameof(this.TradeId), this.TradeId==null?"":this.TradeId),
             new SQLiteParameter("@"+nameof(this.PlatformType), this.PlatformType),
             new SQLiteParameter("@"+nameof(this.UserId),this.UserId==null?"":this.UserId),
             new SQLiteParameter("@"+nameof(this.UploadStatus), this.UploadStatus),
             new SQLiteParameter("@"+nameof(this.IsMark), this.IsMark),
             new SQLiteParameter("@"+nameof(this.ShareUrl),this.ShareUrl==null?"":this.ShareUrl),
             new SQLiteParameter("@"+nameof(this.PlayUrl),this.PlayUrl==null?"":this.PlayUrl),
             new SQLiteParameter("@"+nameof(this.CloudStore), this.CloudStore),
             new SQLiteParameter("@"+nameof(this.DeleteStatus), this.DeleteStatus),
             new SQLiteParameter("@"+nameof(this.TenantId), this.TenantId)
             };
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.Id),this.Id)
            };
            sQLiteHelper.Update(tableName, parameters, whereParameters);
        }

        public void GetModelByName(string videoName)
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.VideoName), videoName)
            };
            List<AnchorVideo> result= sQLiteHelper.GetModelList<AnchorVideo>(tableName, "video_name=@VideoName", parameters);
            if (result != null && result.Count() > 0) 
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }
        }

        //public void GetModelById(int id)
        //{
        //    SQLiteHelper sQLiteHelper = new SQLiteHelper();
        //    SQLiteParameter[] parameters = new SQLiteParameter[]
        //    {
        //       new SQLiteParameter("@"+nameof(this.Id), id)
        //    };
        //    List<AnchorVideo> result = sQLiteHelper.GetModelList<AnchorVideo>(tableName, "id=@Id", parameters);
        //    if (result != null && result.Count() > 0)
        //    {
        //        sQLiteHelper.AssignPropertiesFrom(this, result[0]);
        //    }
        //}

        /// <summary>
        /// 根据视频唯一标识获取视频信息
        /// </summary>
        /// <param name="videoId"></param>
        public void GetModelByVideoId(string videoId)
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter("@"+nameof(this.VideoId), videoId)
            };
            List<AnchorVideo> result = sQLiteHelper.GetModelList<AnchorVideo>(tableName, "video_id=@VideoId", parameters);
            if (result != null && result.Count() > 0)
            {
                sQLiteHelper.AssignPropertiesFrom(this, result[0]);
            }
        }

        //public void GetModelById()
        //{
        //    SQLiteHelper sQLiteHelper = new SQLiteHelper();
        //    SQLiteParameter[] parameters = new SQLiteParameter[]
        //    {
        //       new SQLiteParameter("@"+nameof(this.Id), this.Id)
        //    };
        //    List<AnchorVideo> result = sQLiteHelper.GetModelList<AnchorVideo>(tableName, "id=@Id", parameters);
        //    if (result != null && result.Count() > 0)
        //    {
        //        sQLiteHelper.AssignPropertiesFrom(this, result[0]);
        //    }
        //}

        public List<AnchorVideo> GetList()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] whereParameters);
            return sQLiteHelper.GetModelList<AnchorVideo>(tableName, whereClause, whereParameters);
        }

        /// <summary>
        /// 获取该主播最新的一条数据
        /// </summary>
        /// <param name="anchorId"></param>
        public void UpdateByFileName() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
           {
            
             new SQLiteParameter("@"+nameof(this.Duration), this.Duration),
             new SQLiteParameter("@"+nameof(this.EndTime), this.EndTime==null?"":this.EndTime),
             new SQLiteParameter("@"+nameof(this.VedioSizie),this.VedioSizie==null?"":this.VedioSizie)
            };
            SQLiteParameter[] whereParameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.VideoName), this.VideoName)
            };
            //FileUtils.log("根据名称更新文件大小和时间");
            sQLiteHelper.Update(tableName, parameters, whereParameters);
        }

        /// <summary>
        /// 初始化视频表的录制状态
        /// </summary>
        public void InitVideoRecordStatus() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            //必须使用变量赋值，不然直接写0 参数值未null
            //在 C# 中，整数常量 0 可以被解释为多种类型（如 int, byte, long 等），具体取决于上下文。因此，当你直接在构造函数中使用 0 时，编译器可能无法确定你想要的确切类型。
            int IsRecording = 0;
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.IsRecording)}",IsRecording)
            };
            sQLiteHelper.Update(tableName, parameters,null);
        }

        /// <summary>
        /// 获取最大的段落数
        /// </summary>
        /// <returns></returns>
        public int GetMaxParagraph() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.BatchNumber)}",this.BatchNumber)
            };
            object maxParagraph= sQLiteHelper.GetMaxFunction(tableName, "paragraph", parameters);
            if (maxParagraph == null||maxParagraph.ToString()=="")
            {
                return 1;
            }
            else 
            {
                return Convert.ToInt32(maxParagraph);
            }
        }


        public void DeleteModel() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.Id)}",this.Id)
            };
            sQLiteHelper.Delete(tableName, whereParamters);
        }

        /// <summary>
        /// 分页获取主播视频列表
        /// </summary>
        /// <param name="anchorVideoPageQueryDto"></param>
        /// <returns></returns>
        public PageDto<AnchorVideoDto> GetPage(AnchorVideoPageQueryDto anchorVideoPageQueryDto)
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            if (anchorVideoPageQueryDto.PageIndex < 1)
            {
                anchorVideoPageQueryDto.PageIndex = 1;
            }
            if (anchorVideoPageQueryDto.PageSize < 1)
            {
                anchorVideoPageQueryDto.PageSize = 10;
            }

            List<SQLiteParameter> list = new List<SQLiteParameter>();

            /// 构建条件
            string sqlWhereStr = " is_recording = 0 and vedio_sizie > 0 ";

            if(anchorVideoPageQueryDto.SecUid != "-1" )
            {
                list.Add(new SQLiteParameter("@SecUid", anchorVideoPageQueryDto.SecUid));
                sqlWhereStr += " and sec_uid=@SecUid ";
            }
            if (anchorVideoPageQueryDto.AnalysisStatus != -1)
            {
                list.Add(new SQLiteParameter("@AnalysisStatus", anchorVideoPageQueryDto.AnalysisStatus));
                sqlWhereStr += " and analysis_status=@AnalysisStatus ";
            }
            if(!string.IsNullOrEmpty(anchorVideoPageQueryDto.AnalysisStartDate))
            {
                anchorVideoPageQueryDto.AnalysisStartDate = anchorVideoPageQueryDto.AnalysisStartDate + " 00:00:00";
                list.Add(new SQLiteParameter("@AnalysisStartDate", anchorVideoPageQueryDto.AnalysisStartDate));
                sqlWhereStr += " and analysis_time >= @AnalysisStartDate ";
            }
            if (!string.IsNullOrEmpty(anchorVideoPageQueryDto.AnalysisEndDate))
            {
                anchorVideoPageQueryDto.AnalysisEndDate = anchorVideoPageQueryDto.AnalysisEndDate + " 23:59:59";
                list.Add(new SQLiteParameter("@AnalysisEndDate", anchorVideoPageQueryDto.AnalysisEndDate));
                sqlWhereStr += " and analysis_time <= @AnalysisEndDate ";
            }
            if (!string.IsNullOrEmpty(anchorVideoPageQueryDto.RecordStartDate))
            {
                anchorVideoPageQueryDto.RecordStartDate = anchorVideoPageQueryDto.RecordStartDate + " 00:00:00";
                list.Add(new SQLiteParameter("@RecordStartDate", anchorVideoPageQueryDto.RecordStartDate));
                sqlWhereStr += " and start_time >= @RecordStartDate ";
            }
            if (!string.IsNullOrEmpty(anchorVideoPageQueryDto.RecordEndDate))
            {
                anchorVideoPageQueryDto.RecordEndDate = anchorVideoPageQueryDto.RecordEndDate + " 23:59:59";
                list.Add(new SQLiteParameter("@RecordEndDate", anchorVideoPageQueryDto.RecordEndDate));
                sqlWhereStr += " and start_time <= @RecordEndDate ";
            }

            SQLiteParameter[] parameters = list.ToArray();

            if(anchorVideoPageQueryDto.AnalysisStatus == 2)
            {
                return sQLiteHelper.ExecutePagedQueryOrderByTime<AnchorVideoDto>(tableName, anchorVideoPageQueryDto.PageSize, anchorVideoPageQueryDto.PageIndex, sqlWhereStr, parameters);
            }
            else
            {
                return sQLiteHelper.ExecutePagedQuery<AnchorVideoDto>(tableName, anchorVideoPageQueryDto.PageSize, anchorVideoPageQueryDto.PageIndex, sqlWhereStr, parameters);
            }

        }


        /// <summary>
        /// 删除所有视频
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            sQLiteHelper.Delete(tableName, null);
        }

        /// <summary>
        /// 获取分析状态为1的视频列表
        /// </summary>
        /// <returns></returns>
        public List<AnchorVideo> GetAnalysisStatusIsOne()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            string sql = $"SELECT * FROM {tableName} where analysis_status = 1";
            System.Data.DataTable dataTable = sQLiteHelper.ExecuteQuery(sql);
            return sQLiteHelper.ConvertToEntityList<AnchorVideo>(dataTable);

        }

        /// <summary>
        /// 获取录制状态为1的视频列表
        /// </summary>
        /// <returns></returns>
        public List<AnchorVideo> GetIsRecordingIsOne()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            string sql = $"SELECT * FROM {tableName} where is_recording = 1";
            System.Data.DataTable dataTable = sQLiteHelper.ExecuteQuery(sql);
            return sQLiteHelper.ConvertToEntityList<AnchorVideo>(dataTable);

        }

        /// <summary>
        /// 修改视频的分析状态
        /// </summary>
        /// <param name="videoId">视频id</param>
        /// <param name="analysisStatus">分析状态 0：未分析 1：分析中 2：分析完成 3：识别失败</param>
        /// <param name="errorReason">分析失败原因</param>
        /// <exception cref="NotImplementedException"></exception>
        //public void UpdateAnalysisStatus(int videoId, int analysisStatus, string errorReason)
        //{
        //    List<SQLiteParameter> list = new List<SQLiteParameter>();
        //    list.Add(new SQLiteParameter("@AnalysisStatus", analysisStatus));
        //    list.Add(new SQLiteParameter("@ErrorReason", errorReason));
        //    if (analysisStatus == 1)
        //    {
        //        list.Add(new SQLiteParameter("@AnalysisTime", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss")));
        //    }

        //    SQLiteHelper sQLiteHelper = new SQLiteHelper();
        //    SQLiteParameter[] parameters = list.ToArray();
        //    SQLiteParameter[] whereParameters = new SQLiteParameter[]
        //    {
        //        new SQLiteParameter("@Id", videoId)
        //    };
        //    sQLiteHelper.Update(tableName, parameters, whereParameters);
        //}
    }

  
}
