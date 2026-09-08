package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 *  租户主播信息响应
 * @author HeHui
 * @date 2026-04-23 17:18
 */
@Getter
@Setter
public class TenantAnchorInfoResponse {


    /**
     * ID
     */
    private Long id;

    /**
     * 主播唯一标识
     */
    private String secUid;
    /**
     * 主页url
     */
    private String homeUrl;
    /**
     * 直播间url
     */
    private String liveUrl;
    /**
     * 主播名称
     */
    private String anchorName;
    /**
     * 主播头像
     */
    private String anchorAvatar;
    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    private Integer platform;
    /**
     * DouYinHomeLive(个人主页地址),DouYinLive（直播地址） 抖音直播 TiktokLive 国外版本抖音  KuaiShouLive 快手   其他待定
     */
    private String platformResource;
    /**
     * 主播userId
     */
    private String anchorUserId;
    /**
     * webSocketId
     */
    private String webSocketId;
    /**
     * 主播抖音号
     */
    private String anchorNumber;

    /**
     * 第三方数据平台是否已经收录 0:否 1:是
     */
    private Integer chanmamaInclude;

    /**
     * 系统行业id
     */
    private Long systemTradeId;
    /**
     * AI纠正后的行业id
     */
    private Long aiCorrectTradeId;


    /**
     * 账号归属类型 0：自由账号 1：同行账号
     */
    private Integer accountType;


    /**
     *  监控用户（客户端）ID集合
     */
    private List<Long> userIds;


    /**
     * 行业id集合（客户端所选）
     */
    private List<Long> tradeIds;


    /**
     * 授权巨量百应状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authJlbyStatus;
    /**
     * 授权巨量百应状态修改时间
     */
    private Date authJlbyStatusTime;
    /**
     * 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authQcStatus;
    /**
     * 千川授权状态修改时间
     */
    private Date authQcStatusTime;
    /**
     * 授权微信视频号状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权
     */
    private Integer authChannelStatus;
    /**
     * 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authLifeStatus;

    /**
     * 授权来客状态修改时间
     */
    private Date authLifeStatusTime;


    /**
     * 是否按排班录制 0：否 1：是
     */
    private Integer isScheduleRecord;


    /**
     * 是否统计业绩 0-否 1-是
     */
    private Integer isStatisticsPerformance;

}
