using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ReviewAnalysis.vo.ai
{
    public class VideoDataViewingConfuseVo
    {

        /// <summary>
        /// ID
        /// </summary>
        public long? id {  get; set; }

        /// <summary>
        /// 视频id
        /// </summary>
        public String videoId {  get; set; }

        /// <summary>
        /// 用户id
        /// </summary>
        public long? userId {  get; set; }

        /// <summary>
        /// 租户id
        /// </summary>
        public long? tenantId {  get; set; }

        /// <summary>
        /// 总观看人次
        /// </summary>
        public int? totalWatchNum {  get; set; }

        /// <summary>
        /// 平均在线人数
        /// </summary>
        public int? averageOnlineNum {  get; set; }

        /// <summary>
        /// 平均停留时间(秒)
        /// </summary>
        public int? averageResidenceTime {  get; set; }

        /// <summary>
        /// 新增粉丝数
        /// </summary>
        public int? incrementFollowerCount {  get; set; }

        /// <summary>
        /// 粉丝转化率
        /// </summary>
        public Double? convertFanRate {  get; set; }

        /// <summary>
        /// 互动率
        /// </summary>
        public Double? interactionPercent {  get; set; }

        /// <summary>
        /// 销售额(单位:分)
        /// </summary>
        public int? volume {  get; set; }

        /// <summary>
        /// 销量
        /// </summary>
        public int? purchaseCount {  get; set; }

        /// <summary>
        /// 客单价（单位：分）
        /// </summary>
        public int? customerUnitPrice {  get; set; }

        /// <summary>
        /// uv价值
        /// </summary>
        public Double? uvValue {  get; set; }

        /// <summary>
        /// 带货转换率
        /// </summary>
        public Double? goodsConvertRate {  get; set; }

        /// <summary>
        /// 数据状态 0：正在拉取 1：拉取成功 2：拉取失败
        /// </summary>
        public int? dataStatus {  get; set; }

        /// <summary>
        /// 关联的数据看盘id
        /// </summary>
        public long? videoDataViewingId {  get; set; }

        /// <summary>
        /// 主播抖音号
        /// </summary>
        public String anchorNumber {  get; set; }

        /// <summary>
        /// 数据抓取时间
        /// </summary>
        public string crawlTime {  get; set; }

        /// <summary>
        /// 创建时间
        /// </summary>
        public string createDate {  get; set; }

        /// <summary>
        /// 最后修改时间
        /// </summary>
        public string updateDate {  get; set; }

        /// <summary>
        /// 是否已删除
        /// </summary>
        public int? isDeleted {  get; set; }

    }
}

