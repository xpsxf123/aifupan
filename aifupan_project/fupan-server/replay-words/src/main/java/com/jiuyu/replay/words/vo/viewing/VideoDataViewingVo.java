package com.jiuyu.replay.words.vo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 视频看盘数据信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Data
@Schema(description = "视频看盘数据信息")
public class VideoDataViewingVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 总观看人次
	 */
	@Schema(description = "总观看人次")
	private Integer totalWatchNum;
	/**
	 * 平均在线人数
	 */
	@Schema(description = "平均在线人数")
	private Integer averageOnlineNum;
	/**
	 * 平均停留时间(秒)
	 */
	@Schema(description = "平均停留时间(秒)")
	private Integer averageResidenceTime;
	/**
	 * 新增粉丝数
	 */
	@Schema(description = "新增粉丝数")
	private Integer incrementFollowerCount;
	/**
	 * 粉丝转化率
	 */
	@Schema(description = "粉丝转化率")
	private Double convertFanRate;
	/**
	 * 互动率
	 */
	@Schema(description = "互动率")
	private Double interactionPercent;
	/**
	 * 销售额区间范围-起始(单位:元)
	 */
	@Schema(description = "销售额区间范围-起始(单位:元)")
	private Integer volumeStart;
	/**
	 * 销售额区间范围-结束(单位:元)
	 */
	@Schema(description = "销售额区间范围-结束(单位:元)")
	private Integer volumeEnd;
	/**
	 * 销量区间范围-起始
	 */
	@Schema(description = "销量区间范围-起始")
	private Integer purchaseCountStart;
	/**
	 * 销量区间范围-结束
	 */
	@Schema(description = "销量区间范围-结束")
	private Integer purchaseCountEnd;
	/**
	 * 客单价区间范围-起始（单位：元）
	 */
	@Schema(description = "客单价区间范围-起始（单位：元）")
	private Double customerUnitPriceStart;
	/**
	 * 客单价区间范围-结束（单位：元）
	 */
	@Schema(description = "客单价区间范围-结束（单位：元）")
	private Double customerUnitPriceEnd;
	/**
	 * uv价值区间范围-起始
	 */
	@Schema(description = "uv价值区间范围-起始")
	private Double uvValueStart;
	/**
	 * uv价值区间范围-结束
	 */
	@Schema(description = "uv价值区间范围-结束")
	private Double uvValueEnd;
	/**
	 * 带货转换率区间范围-起始
	 */
	@Schema(description = "带货转换率区间范围-起始")
	private Double goodsConvertRateStart;
	/**
	 * 带货转换率区间范围-结束
	 */
	@Schema(description = "带货转换率区间范围-结束")
	private Double goodsConvertRateEnd;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;
	/**
	 * 直播开始时间
	 */
	@Schema(description = "直播开始时间")
	private Date liveStartDate;
	/**
	 * 直播结束时间
	 */
	@Schema(description = "直播结束时间")
	private Date liveEndDate;
	/**
	 * 直播开始时间的时间戳
	 */
	@Schema(description = "直播开始时间的时间戳")
	private Long liveStartDateStamp;
	/**
	 * 直播结束时间的时间戳
	 */
	@Schema(description = "直播结束时间的时间戳")
	private Long liveEndDateStamp;
	/**
	 * 是否带货 0：否 1：是
	 */
	@Schema(description = "是否带货 0：否 1：是")
	private Integer isTakeProduct;
	/**
	 * 主播抖音号
	 */
	@Schema(description = "主播抖音号")
	private String anchorNumber;
	/**
	 * 直播状态 0：在播 1：已结束
	 */
	@Schema(description = "直播状态 0：在播 1：已结束")
	private Integer liveStatus;
	/**
	 * 直播场次号
	 */
	@Schema(description = "直播场次号")
	private String batchNumber;
	/**
	 * 请求id
	 */
	@Schema(description = "请求id")
	private String requestId;
	/**
	 * 总观看人次（混淆后的数据）
	 */
	@Schema(description = "总观看人次（混淆后的数据）")
	private Integer totalWatchNumConfuse;
	/**
	 * 平均在线人数（混淆后的数据）
	 */
	@Schema(description = "平均在线人数（混淆后的数据）")
	private Integer averageOnlineNumConfuse;
	/**
	 * 平均停留时间(秒)（混淆后的数据）
	 */
	@Schema(description = "平均停留时间(秒)（混淆后的数据）")
	private Integer averageResidenceTimeConfuse;
	/**
	 * 新增粉丝数（混淆后的数据）
	 */
	@Schema(description = "新增粉丝数（混淆后的数据）")
	private Integer incrementFollowerCountConfuse;
	/**
	 * 粉丝转化率（混淆后的数据）
	 */
	@Schema(description = "粉丝转化率（混淆后的数据）")
	private Double convertFanRateConfuse;
	/**
	 * 互动率（混淆后的数据）
	 */
	@Schema(description = "互动率（混淆后的数据）")
	private Double interactionPercentConfuse;
	/**
	 * 销售额区间范围-起始(单位:元)（混淆后的数据）
	 */
	@Schema(description = "销售额区间范围-起始(单位:元)（混淆后的数据）")
	private Integer volumeStartConfuse;
	/**
	 * 销售额区间范围-结束(单位:元)（混淆后的数据）
	 */
	@Schema(description = "销售额区间范围-结束(单位:元)（混淆后的数据）")
	private Integer volumeEndConfuse;
	/**
	 * 销量区间范围-起始（混淆后的数据）
	 */
	@Schema(description = "销量区间范围-起始（混淆后的数据）")
	private Integer purchaseCountStartConfuse;
	/**
	 * 销量区间范围-结束（混淆后的数据）
	 */
	@Schema(description = "销量区间范围-结束（混淆后的数据）")
	private Integer purchaseCountEndConfuse;
	/**
	 * 客单价区间范围-起始（单位：元）（混淆后的数据）
	 */
	@Schema(description = "客单价区间范围-起始（单位：元）（混淆后的数据）")
	private Double customerUnitPriceStartConfuse;
	/**
	 * 客单价区间范围-结束（单位：元）（混淆后的数据）
	 */
	@Schema(description = "客单价区间范围-结束（单位：元）（混淆后的数据）")
	private Double customerUnitPriceEndConfuse;
	/**
	 * uv价值区间范围-起始（混淆后的数据）
	 */
	@Schema(description = "uv价值区间范围-起始（混淆后的数据）")
	private Double uvValueStartConfuse;
	/**
	 * uv价值区间范围-结束（混淆后的数据）
	 */
	@Schema(description = "uv价值区间范围-结束（混淆后的数据）")
	private Double uvValueEndConfuse;
	/**
	 * 带货转换率区间范围-起始（混淆后的数据）
	 */
	@Schema(description = "带货转换率区间范围-起始（混淆后的数据）")
	private Double goodsConvertRateStartConfuse;
	/**
	 * 带货转换率区间范围-结束（混淆后的数据）
	 */
	@Schema(description = "带货转换率区间范围-结束（混淆后的数据）")
	private Double goodsConvertRateEndConfuse;
	/**
	 * 看播流量结构
	 */
	@Schema(description = "看播流量结构")
	private String watchFlowList;
	/**
	 * 看播用户画像
	 */
	@Schema(description = "看播用户画像")
	private String watchUserPortrait;
}
