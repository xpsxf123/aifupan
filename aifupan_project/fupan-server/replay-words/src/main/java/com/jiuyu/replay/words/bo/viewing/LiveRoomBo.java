package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(name = "直播间视图")
public class LiveRoomBo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 直播间ID
     */
    @Schema(description = "直播间ID")
    private String roomId;

    /**
     * 直播间标题
     */
    @Schema(description = "直播间标题")
    private String roomTitle;

    /**
     * 直播开始时间
     */
    @Schema(description = "直播开始时间")
    private String beginTime;

    /**
     * 直播结束时间
     */
    @Schema(description = "直播结束时间")
    private String roomFinishTime;

    /**
     * 直播时长(秒)
     */
    @Schema(description = "直播时长(秒)")
    private Integer liveDuration;

    /**
     * 累计观看用户数
     */
    @Schema(description = "累计观看用户数")
    private Integer totalUser;

    /**
     * 是否品牌专属直播间(0:否 1:是)
     */
    @Schema(description = "是否品牌专属直播间(0:否 1:是)")
    private Boolean isBrandExclusive;

    /**
     * 数据抓取时间
     */
    @Schema(description = "数据抓取时间")
    private String crawlTime;

    /**
     * 平均停留时间(秒)
     */
    @Schema(description = "平均停留时间(秒)")
    private Integer averageResidenceTime;

    /**
     * 平均在线百分比
     */
    @Schema(description = "平均在线百分比")
    private Double averageOnlinePercent;

    /**
     * 平均在线人数
     */
    @Schema(description = "平均在线人数")
    private Integer averageUserCount;

    /**
     * 是否带货
     */
    @Schema(description = "是否带货")
    private Boolean isTakeProduct;

    /**
     * 销售额(单位:分)
     */
    @Schema(description = "销售额(单位:分)")
    private Integer volume;

    /**
     * 点赞总数
     */
    @Schema(description = "点赞总数")
    private Integer likeCount;

    /**
     * 直播封面图URL
     */
    @Schema(description = "直播封面图URL")
    private String liveCover;

    /**
     * 直播间分享链接
     */
    @Schema(description = "直播间分享链接")
    private String shareUrl;

    /**
     * 直播间状态(4:已结束)
     */
    @Schema(description = "直播间状态(4:已结束)")
    private Integer status;

    /**
     * 同时在线峰值人数
     */
    @Schema(description = "同时在线峰值人数")
    private Integer userPeak;

    /**
     * 弹幕总数
     */
    @Schema(name = "弹幕总数")
    private Integer barrageCount;

    /**
     * 关联商品数量
     */
    @Schema(description = "关联商品数量")
    private Integer productSize;

    /**
     * GMV更新时间戳
     */
    @Schema(description = "GMV更新时间戳")
    private Long gmvUpdateTime;

    /**
     * 直播间流量信息
     */
    @Schema(description = "直播间流量信息")
    private LiveTrafficBo liveTrafficVo;

    /**
     * 直播间作者信息
     */
    @Schema(description = "直播间作者信息")
    private LiveAuthorBo liveAuthorVo;

    /**
     * 直播间商品信息
     */
    @Schema(description = "直播间商品信息")
    private List<LiveProductBo> liveProductVos;
    /**
     * 采集的账户类型 0：蝉妈妈 1：考古家
     */
    @Schema(description = "采集的账户类型 0：蝉妈妈 1：考古家")
    private Integer accountType;
    /**
     * 校验情况 0：未发生异常，不需要校验  1：已经校验  2：检测到异常，未校验 3：未校验
     */
    @Schema(description = "校验情况 0：未发生异常，不需要校验  1：已经校验  2：检测到异常，未校验 3：未校验")
    private Integer isVerified;

    /**
     * 直播间用户来源信息
     */
    @Schema(description ="直播间用户来源信息")
    private List<LiveUserSourceBo> liveUserSourceVos;

    /**
     * 直播间用户年龄段信息
     */
    @Schema(description ="直播间用户年龄段信息")
    private List<LivePortraitAgeBo> livePortraitAgeVos;

    /**
     * 直播间用户省份信息
     */
    @Schema(description="直播间用户省份信息")
    private List<LivePortraitProvinceBo> livePortraitProvinceVos;

    /**
     * 直播间用户城市信息
     */
    @Schema(description="直播间用户城市信息")
    private List<LivePortraitCityBo> livePortraitCityVos;

    /**
     * 直播间用户性别信息
     */
    @Schema(description="直播间用户性别信息")
    private List<LivePortraitGenderBo> livePortraitGenderVos;
}
