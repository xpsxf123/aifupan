using Newtonsoft.Json;
using ReviewAnalysis.bo.anchor;
using ReviewAnalysis.Db;
using ReviewAnalysis.Dto;
using ReviewAnalysis.vo.anchor;
using System;
using System.Collections.Generic;
using System.Data.SQLite;
using System.Linq;
using ReviewAnalysis.vo.common;

namespace ReviewAnalysis.Model
{
    public class AnchorInfo
    {
        /// <summary>
        /// 主键
        /// </summary>
        public int Id { get; set; }

        /// <summary>
        /// 主播名称
        /// </summary>
        public string AnchorName { get; set; }

        /// <summary>
        /// 主播头像地址
        /// </summary>
        public string AnchorAvatar { get; set; }

        /// <summary>
        /// DouYinLive 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手 WeChatChannelsLive 微信视频号   其他待定
        /// </summary>
        public string AnchorPlatform { get; set; }

        /// <summary>
        /// 主播个人主页 
        /// </summary>
        public string HomeUrl { get; set; }

        /// <summary>
        /// 直播地址
        /// </summary>
        public string LiveUrl { get; set; }

        ///<<summary>
        /// app分享直播地址
        /// </summary>       
        public string AppShareUrl { get; set; }


        /// <summary>
        /// 直播状态 0未检测 2直播中，4未直播 
        /// </summary>
        public int LiveStatus { get; set; }

        /// <summary>
        /// 录播状态 record_status 0未开始录制，1正在录制 2手动停止录制，3录制完成，4手动开启
        /// </summary>
        public int RecordStatus { get; set; }

        /// <summary>
        /// 主播在各平台的唯一标识
        /// </summary>
        public string SecUid { get; set; }

        /// <summary>
        /// 检测在线直播时，是否录制视频 is_auto_record  0否，1是
        /// </summary>
        public int IsAutoRecord { get; set; }

        /// <summary>
        /// 主播直播时的在线人数 
        /// </summary>
        public string OnlineNumber { get; set; }

        /// <summary>
        /// 当前正在直播的批次编号,batch_number 每次开启录播就会更新
        /// </summary>
        public string BatchNumber { get; set; }

        /// <summary>
        /// 添加主播的时间
        /// </summary>
        public string AddTime { get; set; }

        /// <summary>
        /// 开始录制时间
        /// </summary>
        public string StartTime { get; set; }

        /// <summary>
        /// 最新一次的直播流地址
        /// </summary>
        public string StreamUrl { get; set; }

        /// <summary>
        /// 行业id
        /// </summary>
        public string TradeId { get; set; }

        /// <summary>
        /// 主播userId（快手的为webId）
        /// </summary>
        public string AnchorUserId { get; set; }

        /// <summary>
        /// webSocketId
        /// </summary>
        public string WebSocketId { get; set; }

        /// <summary>
        /// 是否保存弹幕 0不保存，1保存
        /// </summary>
        public int IsBarrageMonitoring { get; set; } = 0;

        /// <summary>
        /// 是否从录制列表移除了 0：否 1：是 2：已从恢复列表删除
        /// </summary>
        public int IsRemoveRecord { get; set; }

        /// <summary>
        /// 是否自动上传到云空间 0：否 1：是
        /// </summary>
        public int IsAutoUploadCloud { get; set; }

        /// <summary>
        /// 直播源地址
        /// </summary>
        public string SourceUrl { get; set; }

        /// <summary>
        /// 是否置顶 0：否 1：是
        /// </summary>
        public int IsTop { get; set; }

        /// <summary>
        /// 加入置顶的时间
        /// </summary>
        public string AddTopTime { get; set; }
        /// <summary>
        /// 最后开始录制时间
        /// </summary>
        public string LastRecordTime { get; set; }

        /// <summary>
        /// 昨日录制数据记录的时间戳
        /// </summary>
        public long? YesterdayRecordTime { get; set; }

        /// <summary>
        /// 昨日录制列表
        /// </summary>
        public List<AnchorYesterdayRecordItemVo> YesterdayRecordList { get; set; }

        /// <summary>
        /// 昨日录制数量
        /// </summary>
        public int YesterdayRecordNum { get; set; }

        /// <summary>
        /// 昨日平均场观
        /// </summary>
        public int YesterdayAverageObservationNum { get; set; }
        /// <summary>
        /// 昨日平均销售额区间范围-起始
        /// </summary>
        public int YesterdayAverageVolumeStart { get; set; }
        /// <summary>
        /// 昨日平均销售额区间范围-结束
        /// </summary>
        public int YesterdayAverageVolumeEnd { get; set; }

        /// <summary>
        /// 前日录制列表
        /// </summary>
        public List<AnchorYesterdayRecordItemVo> DayBeforeRecordList { get; set; }

        /// <summary>
        /// 前日录制数量
        /// </summary>
        public int DayBeforeRecordNum { get; set; }

        /// <summary>
        /// 前日平均场观
        /// </summary>
        public int DayBeforeAverageObservationNum { get; set; }
        /// <summary>
        /// 前日平均销售额区间范围-起始
        /// </summary>
        public int DayBeforeAverageVolumeStart { get; set; }
        /// <summary>
        /// 前日平均销售额区间范围-结束
        /// </summary>
        public int DayBeforeAverageVolumeEnd { get; set; }

        /// <summary>
        /// 录制时间，如：06:00:00-19:00:00
        /// </summary>
        public string RecordTime { get; set; }
        /// <summary>
        /// 上下播短信提醒 0：不提醒 1：上播提醒 2：下播提醒 3：上下播提醒
        /// </summary>
        public int SmsTip { get; set; }
        /// <summary>
        /// 是否开启数据看板 0：否 1：是
        /// </summary>
        public int IsDataViewing { get; set; }
        /// <summary>
        /// 文件夹名称(主播名去掉特殊符号，如果只有特殊符号用uuid)
        /// </summary>
        public string FolderName { get; set; }
        /// <summary>
        /// 是否自动诊断 0：否 1：是
        /// </summary>
        public int isAutoDiagnosis { get; set; }
        /// <summary>
        /// 主播备注名称
        /// </summary>
        public string RemarksName { get; set; }

        /// <summary>
        /// 诊断参数
        /// </summary>
        public DiagnosisParams diagnosisParams { get; set; }

        /// <summary>
        /// 巨量百应授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int juliangAuthStatus { get; set; }

        /// <summary>
        /// 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int qianchuanAuthStatus { get; set; }

        /// <summary>
        /// 来客授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
        /// </summary>
        public int lifeAuthStatus { get; set; }

        /// <summary>
        /// 企业号授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配 6：子账号无权限
        /// </summary>
        public int enterpriseAuthStatus { get; set; }

        /// <summary>
        /// 主播后台授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中
        /// </summary>
        public int anchorLiveAuthStatus { get; set; }

        /// <summary>
        /// 平台类型 0：抖音 1：快手 2：视频号
        /// </summary>
        public int platform { get; set; }
        /// <summary>
        /// 主播抖音号/快手自定义id
        /// </summary>
        public string anchorNumber { get; set; }
        /// <summary>
        /// 从录制列表移除主播的时间
        /// </summary>
        public string deleteDate { get; set; }
        /// <summary>
        /// 录制的清晰度 -1：跟随系统 0 标清 1高清 2超清 3 蓝光
        /// </summary>
        public int? recordDefinition { get; set; }
        /// <summary>
        /// 录制形式 -1：跟随系统 0：无限制录制 1：限制时长录制(只录一段) 2：时长分段录制
        /// </summary>
        public int? recordLimitType { get; set; }
        /// <summary>
        /// 录制的时长，单位：分钟
        /// </summary>
        public int? recordLimitValue { get; set; }
        /// <summary>
        /// 是否自动分析视频 0：否 1：是
        /// </summary>
        public int? isAutoAnalysis { get; set; }
        /// <summary>
        /// 微信视频号授权状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权
        /// </summary>
        [JsonProperty("authChannelStatus")]
        public int WeChatChannelsAuthStatus { get; set; }
        
                
        /// <summary>
        /// 是否开启数据诊断 0：否 1：是
        /// </summary>
        public int? isDataDiagnosis { get; set; }
        
        /// <summary>
        /// 自动生成数据诊断剩余场次
        /// </summary>
        public int? diagnosisGenerateNum { get; set; }
        
        /// <summary>
        /// 数据诊断参数
        /// </summary>
        public DiagnosisParams dataDiagnosisParams { get; set; }
        
        
        
        
        
        
        /// <summary>
        /// 账号归属类型 0：自有账号 1：同行账号
        /// </summary>
        public int AccountType { get; set; }
        
        /// <summary>
        /// 首播日期
        /// </summary>
        public string premiereDate { get; set; }
        
        /// <summary>
        /// 账号阶段，使用字典account_stage的值
        /// </summary>
        public int? accountStage { get; set; }
        
        /// <summary>
        /// 账号水平， 使用字典account_water_level的值
        /// </summary>
        public int? accountWaterLevel { get; set; }

        /// <summary>
        /// 浏览结构，使用字典 account_flow的值
        /// </summary>
        public int? accountFlow { get; set; }
        
        /// <summary>
        /// 直播目标，使用字典living_target的值
        /// </summary>
        public int? livingTarget { get; set; }
        
        /// <summary>
        /// 直播方式，使用字典living_modality的值
        /// </summary>
        public int? livingModality { get; set; }
        
        /// <summary>
        /// 营销方式，使用字典marketing的值
        /// </summary>
        public int? marketing { get; set; }
        
        /// <summary>
        /// 优化方向，使用字典optimize_direction的值
        /// </summary>
        public string optimizeDirection { get; set; }
        
        /// <summary>
        /// 学习方向，使用字典learning的值
        /// </summary>
        public string learning { get; set; }
        
        /// <summary>
        /// 直播模式，使用字典living_mode的值
        /// </summary>
        public int? livingMode { get; set; }
        
        /// <summary>
        /// 主播账号情况描述
        /// </summary>
        public string AnchorSituation { get; set; }
        /// <summary>
        /// ROI观测精度，使用字典roi_accuracy的值
        /// </summary>
        public string roiAccuracy { get; set; }
        /// <summary>
        /// 一句话识别引擎模型，如：16k_zh
        /// </summary>
        public string engSerViceType { get; set; }
        /// <summary>
        /// 纯录制版是否获取在线人数 0：否 1：是
        /// </summary>
        public int? pureRecordOnlineNum { get; set; }
        /// <summary>
        /// 是否按排班录制 0：否 1：是
        /// </summary>
        public int? isScheduleRecord { get; set; }

        /// <summary>
        /// 录制时间模式 0：按视频时长录制 1：按北京时间录制（默认）  2：按时间点分段录制
        /// </summary>
        public int? recordTimeMode { get; set; }

        /// <summary>
        /// 时间点分段录制的时间点列表，格式："09:30,10:40,11:20"，多个时间点用逗号分隔
        /// </summary>
        public string segmentTimePoints { get; set; }

        /// <summary>
        /// 是否统计业绩 0-否 1-是
        /// </summary>
        public int? isStatisticsPerformance { get; set; }

        /// <summary>
        /// 话术质检开关 0：关闭 1：开启（透传服务端，不本地持久化）
        /// </summary>
        public int? isScriptQualityInspection { get; set; }

        /// <summary>
        /// 话术还原度开关 0：关闭 1：开启（透传服务端，不本地持久化）
        /// </summary>
        public int? isScriptFidelityMonitor { get; set; }

        /// <summary>
        /// 互动巡检开关 0：关闭 1：开启（透传服务端，不本地持久化）
        /// </summary>
        public int? isInteractionPatrol { get; set; }

        /// <summary>
        /// 已确认标准直播稿 ID；isScriptFidelityMonitor=1 时必传（透传服务端，不本地持久化）
        /// </summary>
        public long? standardScriptId { get; set; }

        /// <summary>
        /// 自动删除时间 -1不删除 0马上删除 N天后删除，空=跟随全局配置（透传服务端，不本地持久化）
        /// </summary>
        public string autoDeleteTime { get; set; }

        /// <summary>
        /// 删除内容 ts源视频/mp4成品/all都删，空=跟随全局配置（透传服务端，不本地持久化）
        /// </summary>
        public string deleteContent { get; set; }


        private readonly string tableName = "anchor_info";
        public void save()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.AnchorName), this.AnchorName),
             new SQLiteParameter("@"+nameof(this.AnchorAvatar), this.AnchorAvatar),
             new SQLiteParameter("@"+nameof(this.AnchorPlatform), this.AnchorPlatform),
             new SQLiteParameter("@"+nameof(this.HomeUrl), this.HomeUrl==null?"": this.HomeUrl),
             new SQLiteParameter("@"+nameof(this.LiveUrl), this.LiveUrl==null?"":this.LiveUrl),
             new SQLiteParameter("@"+nameof(this.AppShareUrl), this.AppShareUrl==null?"":this.AppShareUrl),
             new SQLiteParameter("@"+nameof(this.LiveStatus), this.LiveStatus),
             new SQLiteParameter("@"+nameof(this.RecordStatus), this.RecordStatus),
             new SQLiteParameter("@"+nameof(this.IsAutoRecord),this.IsAutoRecord),
             new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid==null?"":this.SecUid),
             new SQLiteParameter("@"+nameof(this.StartTime), this.StartTime==null?"":this.StartTime),
             new SQLiteParameter("@"+nameof(this.OnlineNumber), this.StartTime==null?"":this.OnlineNumber),
             new SQLiteParameter("@"+nameof(this.BatchNumber),this.BatchNumber==null?"":this.BatchNumber),
             new SQLiteParameter("@"+nameof(this.AddTime),this.AddTime==null?"":this.AddTime),
             new SQLiteParameter("@"+nameof(this.StreamUrl),this.StreamUrl==null?"":this.StreamUrl),
             new SQLiteParameter("@"+nameof(this.TradeId),this.TradeId==null?"":this.TradeId),
             new SQLiteParameter("@"+nameof(this.AnchorUserId),this.AnchorUserId==null?"":this.AnchorUserId),
             new SQLiteParameter("@"+nameof(this.WebSocketId),this.WebSocketId==null?"":this.WebSocketId),
             new SQLiteParameter("@"+nameof(this.IsRemoveRecord),this.IsRemoveRecord),
             new SQLiteParameter("@"+nameof(this.SourceUrl),this.SourceUrl==null?"":this.SourceUrl),
             new SQLiteParameter("@"+nameof(this.IsBarrageMonitoring),this.IsBarrageMonitoring),
             new SQLiteParameter("@"+nameof(this.IsAutoUploadCloud),this.IsAutoUploadCloud),
             new SQLiteParameter("@"+nameof(this.IsTop),this.IsTop),
             new SQLiteParameter("@"+nameof(this.AddTopTime),this.AddTopTime==null?"":this.AddTopTime),
             new SQLiteParameter("@"+nameof(this.LastRecordTime),this.LastRecordTime==null?"":this.LastRecordTime)
             };
            sQLiteHelper.Insert(tableName, parameters);
        }

        public void update()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
             new SQLiteParameter("@"+nameof(this.AnchorName), this.AnchorName),
             new SQLiteParameter("@"+nameof(this.AnchorAvatar), this.AnchorAvatar),
             new SQLiteParameter("@"+nameof(this.AnchorPlatform), this.AnchorPlatform),
             new SQLiteParameter("@"+nameof(this.HomeUrl), this.HomeUrl==null?"": this.HomeUrl),
             new SQLiteParameter("@"+nameof(this.LiveUrl), this.LiveUrl==null?"":this.LiveUrl),
             new SQLiteParameter("@"+nameof(this.AppShareUrl), this.AppShareUrl==null?"":this.AppShareUrl),
             new SQLiteParameter("@"+nameof(this.LiveStatus), this.LiveStatus),
             new SQLiteParameter("@"+nameof(this.RecordStatus), this.RecordStatus),
             new SQLiteParameter("@"+nameof(this.IsAutoRecord),this.IsAutoRecord),
             new SQLiteParameter("@"+nameof(this.SecUid), this.SecUid==null?"":this.SecUid),
              new SQLiteParameter("@"+nameof(this.StartTime), this.StartTime==null?"":this.StartTime),
                new SQLiteParameter("@"+nameof(this.OnlineNumber), this.StartTime==null?"":this.OnlineNumber),
             new SQLiteParameter("@"+nameof(this.BatchNumber),this.BatchNumber==null?"":this.BatchNumber),
              new SQLiteParameter("@"+nameof(this.StreamUrl),this.StreamUrl==null?"":this.StreamUrl),
              new SQLiteParameter("@"+nameof(this.TradeId),this.TradeId==null?"":this.TradeId),
             new SQLiteParameter("@"+nameof(this.AnchorUserId),this.AnchorUserId==null?"":this.AnchorUserId),
             new SQLiteParameter("@"+nameof(this.WebSocketId),this.WebSocketId==null?"":this.WebSocketId),
             new SQLiteParameter("@"+nameof(this.IsRemoveRecord),this.IsRemoveRecord),
             new SQLiteParameter("@"+nameof(this.SourceUrl),this.SourceUrl==null?"":this.SourceUrl),
             new SQLiteParameter("@"+nameof(this.IsBarrageMonitoring),this.IsBarrageMonitoring),
             new SQLiteParameter("@"+nameof(this.IsAutoUploadCloud),this.IsAutoUploadCloud),
             new SQLiteParameter("@"+nameof(this.IsTop),this.IsTop),
             new SQLiteParameter("@"+nameof(this.AddTopTime),this.AddTopTime==null?"":this.AddTopTime),
             new SQLiteParameter("@"+nameof(this.LastRecordTime),this.LastRecordTime==null?"":this.LastRecordTime)
             };
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.Id), this.Id)
            };
            // 暂时注释
            sQLiteHelper.Update(tableName, parameters, whereParamters);
        }

        public void InitRecordAndLiveStatus() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            //必须使用变量赋值，不然直接写0 参数值未null
            //在 C# 中，整数常量 0 可以被解释为多种类型（如 int, byte, long 等），具体取决于上下文。因此，当你直接在构造函数中使用 0 时，编译器可能无法确定你想要的确切类型。
            int RecordStatus = 0;
            int LiveStatus = 0;
            SQLiteParameter[] parameters = new SQLiteParameter[]
            {
                new SQLiteParameter("@"+nameof(this.LiveStatus), LiveStatus),
                new SQLiteParameter("@"+nameof(this.RecordStatus),RecordStatus),
             };
            sQLiteHelper.Update(tableName, parameters, null);
        }

        /// <summary>
        /// 分页查询
        /// </summary>
        /// <param name="PageIndex"></param>
        /// <param name="PageSize"></param>
        /// <returns></returns>
        public PageDto<AnchorInfo> GetPage(int PageIndex, int PageSize) 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            if (PageIndex<1) 
            {
                PageIndex = 1;
            }
            if (PageSize < 1) 
            {
                PageSize = 10;
            }
            if (!string.IsNullOrEmpty(this.AnchorName) && this.RecordStatus != -1)
            {
                SQLiteParameter[] parameters = new SQLiteParameter[]
                {
                   new SQLiteParameter("@AnchorName",$"%{this.AnchorName}%"),
                   new SQLiteParameter("@RecordStatus",this.RecordStatus)
                };
                return sQLiteHelper.ExecutePagedQuery<AnchorInfo>(tableName, PageSize, PageIndex, " anchor_name like @AnchorName and record_status=@RecordStatus", parameters);
            }
            else if (!string.IsNullOrEmpty(this.AnchorName) && this.RecordStatus == -1)
            {
                SQLiteParameter[] parameters = new SQLiteParameter[]
                  {
                   new SQLiteParameter("@AnchorName",$"%{this.AnchorName}%")
                  };
                return sQLiteHelper.ExecutePagedQuery<AnchorInfo>(tableName, PageSize, PageIndex, " anchor_name like @AnchorName ", parameters);
            }
            else if(string.IsNullOrEmpty(this.AnchorName) && this.RecordStatus != -1)
            {
                SQLiteParameter[] parameters = new SQLiteParameter[]
                 {
                   new SQLiteParameter("@RecordStatus",this.RecordStatus)
                 };
                return sQLiteHelper.ExecutePagedQuery<AnchorInfo>(tableName, PageSize, PageIndex, " record_status=@RecordStatus", parameters);
            }
            return sQLiteHelper.ExecutePagedQuery<AnchorInfo>(tableName, PageSize, PageIndex, null, null);
        }

        public List<AnchorInfo> GetAllAnchor() 
        {
            List<AnchorInfo> result = new List<AnchorInfo>();
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            string[] orderFile = new string[]
            {
                " record_status","id"
            };
            result = sQLiteHelper.GetModelListOrder<AnchorInfo>(tableName, orderFile);
            return result;
        }

        /// <summary>
        /// 根据条件获取列表
        /// </summary>
        /// <returns></returns>
        public List<AnchorInfo> GetList() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            sQLiteHelper.GenerateSqlAndParameters(this, out string whereClause, out SQLiteParameter[] parameters);
            return sQLiteHelper.GetModelList<AnchorInfo>(tableName, whereClause, parameters);
        }

        /// <summary>
        /// 根据平台的唯一标识和平台类型获取主播的数量
        /// </summary>
        /// <returns></returns>
        public int GetAnchorCountByPlatFromAndSecUid() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] whereParamters = new SQLiteParameter[] 
            {
               new SQLiteParameter($"@{nameof(this.SecUid)}",this.SecUid),
               new SQLiteParameter($"@{nameof(this.AnchorPlatform)}",this.AnchorPlatform)
            };
            Object result= sQLiteHelper.GetCountFunction(this.tableName, "id", whereParamters);
            return Convert.ToInt32(result);
        }

        public List<AnchorInfo> GetAnchorModelByPlatFromAndSecUid()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.SecUid)}",this.SecUid),
            };
            List<AnchorInfo> list= sQLiteHelper.GetModelList<AnchorInfo>(this.tableName, "sec_uid=@SecUid", whereParamters);
            return list;
        }

        /// <summary>
        /// 根据secuid获取实体
        /// </summary>
        public void GetModelBySecuid() 
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.SecUid)}",this.SecUid),
            };
            List<AnchorInfo> list = sQLiteHelper.GetModelList<AnchorInfo>(this.tableName, "sec_uid=@SecUid", whereParamters);
            if (list != null && list.Count > 0) 
            {
               sQLiteHelper.AssignPropertiesFrom(this,list[0]);
            }
        }

        public void GetModelById()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            SQLiteParameter[] whereParamters = new SQLiteParameter[]
            {
               new SQLiteParameter($"@{nameof(this.Id)}",this.Id)
            };
            sQLiteHelper.GetModelById(this, tableName, Id);
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
        /// 删除所有主播
        /// </summary>
        public void DeleteAll()
        {
            SQLiteHelper sQLiteHelper = new SQLiteHelper();
            sQLiteHelper.Delete(tableName, null);
        }
    }
}
