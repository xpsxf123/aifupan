package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Data
@TableName("tb_video_data_viewing_confuse")
public class VideoDataViewingConfuseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 视频id
	 */
	private String videoId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 总观看人次
	 */
	private Integer totalWatchNum;
	/**
	 * 平均在线人数
	 */
	private Integer averageOnlineNum;
	/**
	 * 平均停留时间(秒)
	 */
	private Integer averageResidenceTime;
	/**
	 * 新增粉丝数
	 */
	private Integer incrementFollowerCount;
	/**
	 * 粉丝转化率
	 */
	private Double convertFanRate;
	/**
	 * 互动率
	 */
	private Double interactionPercent;
	/**
	 * 销售额区间范围-起始(单位:元)
	 */
	private Integer volumeStart;
	/**
	 * 销售额区间范围-结束(单位:元)
	 */
	private Integer volumeEnd;
	/**
	 * 销量区间范围-起始
	 */
	private Integer purchaseCountStart;
	/**
	 * 销量区间范围-结束
	 */
	private Integer purchaseCountEnd;
	/**
	 * 客单价区间范围-起始（单位：元）
	 */
	private Double customerUnitPriceStart;
	/**
	 * 客单价区间范围-结束（单位：元）
	 */
	private Double customerUnitPriceEnd;
	/**
	 * uv价值区间范围-起始
	 */
	private Double uvValueStart;
	/**
	 * uv价值区间范围-结束
	 */
	private Double uvValueEnd;
	/**
	 * 带货转换率区间范围-起始
	 */
	private Double goodsConvertRateStart;
	/**
	 * 带货转换率区间范围-结束
	 */
	private Double goodsConvertRateEnd;
	/**
	 * 数据状态 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：自动生成但主播未下播 7：已收录但直播列表为空 8：正确数据整理中
	 */
	private Integer dataStatus;
	/**
	 * 关联的数据看盘id
	 */
	private Long videoDataViewingId;
	/**
	 * 是否带货 0：否 1：是
	 */
	private Integer isTakeProduct;
	/**
	 * 直播场次号
	 */
	private String batchNumber;
	/**
	 * 请求id
	 */
	private String requestId;
	/**
	 * 主播抖音号
	 */
	private String anchorNumber;
	/**
	 * 数据抓取时间
	 */
	private Date crawlTime;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;
	/**
	 * 看播流量结构
	 */
	private String watchFlowList;
	/**
	 * 成交流量结构
	 */
	private String payFlowList;
	/**
	 * 成交用户画像
	 */
	private String payUserPortrait;
	/**
	 * 看播用户画像
	 */
	private String watchUserPortrait;
	/**
	 * 巨量oss存储地址
	 */
	private String ossPath;
	/**
	 * 数据来源类型 0：蝉妈妈 1：巨量百应
	 */
	private Integer dataSourceType;
	/**
	 * 主播secUid
	 */
	private String secUid;
	/**
	 * 千次观看成交金额范围-起始 （单位：元）
	 */
	private Double gpmStart;
	/**
	 * 千次观看成交金额范围-结束 （单位：元）
	 */
	private Double gpmEnd;
	/**
	 * 曝光-观看率
	 */
	private Double showWatchCntRatio;
	/**
	 * ROI
	 */
	private Double roi;
	/**
	 * 投放ROI金额（单位：元），接口传分，入库÷100转元
	 */
	private Double launchRoiAmount;
	/**
	 * 退款金额（单位：元），接口传分，入库÷100转元
	 */
	private Double refundAmount;
	/**
	 * 整体消耗ROI
	 */
	private Double overallCostRoi;
	/**
	 * 净成交ROI
	 */
	private Double netTransactionRoi;

}
