package com.jiuyu.replay.words.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *  租户主播查询参数
 * @author HeHui
 * @date 2026-04-23 17:28
 */
@Getter
@Setter
public class TenantAnchorQueryRequest {


    /**
     *  关键词（搜索抖音号号和名称）
     */
    private String keyword;

    /**
     *  租户ID
     */
    private Long tenantId;


    /**
     * 平台类型 0：抖音 1：快手 2：视频号
     */
    private List<Integer> platformList;


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
     * 千川授权状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authQcStatus;


    /**
     * 授权微信视频号状态 0：未授权 1：已授权 2：微信后台取消授权, 3：用户取消授权
     */
    private Integer authChannelStatus;

    /**
     * 授权来客状态 0：未授权 1：已授权 2：授权过期 3：授权失败 4：授权中 5：授权抖音号不匹配
     */
    private Integer authLifeStatus;


    /**
     * 是否按排班录制 0：否 1：是
     */
    private Integer isScheduleRecord;


    /**
     * 是否统计业绩 0-否 1-是
     */
    private Integer isStatisticsPerformance;
}
