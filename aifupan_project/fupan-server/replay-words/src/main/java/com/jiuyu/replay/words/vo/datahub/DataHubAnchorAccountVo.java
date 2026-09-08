package com.jiuyu.replay.words.vo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Hub 业务账号数据项（模块内传输对象）
 * 承载 tb_anchor_url_user（添加关系 + 四类授权状态）与 tb_anchor_url（账号档案）的合并快照
 */
@Data
@Schema(description = "Data Hub 业务账号数据项")
public class DataHubAnchorAccountVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    @Schema(description = "租户ID")
    private Long tenantId;

    /**
     * 业务账号平台侧稳定ID（sec_uid）
     */
    @Schema(description = "业务账号 sec_uid")
    private String secUid;

    /**
     * 账号昵称
     */
    @Schema(description = "账号昵称")
    private String anchorName;

    /**
     * 平台 0抖音 1快手 2视频号
     */
    @Schema(description = "平台 0抖音 1快手 2视频号")
    private Integer platform;

    /**
     * 账号归属类型 0自有账号 1同行账号
     */
    @Schema(description = "账号归属类型 0自有账号 1同行账号")
    private Integer accountType;

    /**
     * 添加人用户ID
     */
    @Schema(description = "添加人用户ID")
    private Long addedByUserId;

    /**
     * 是否已从列表移除 0否 1是 2已从恢复列表删除
     */
    @Schema(description = "是否已从列表移除 0否 1是 2已从恢复列表删除")
    private Integer isRemoveRecord;

    /**
     * 添加时间
     */
    @Schema(description = "添加时间")
    private Date createDate;

    /**
     * 移除时间
     */
    @Schema(description = "移除时间")
    private Date deleteDate;

    /**
     * 巨量百应授权状态 0未授权 1已授权 2授权过期 3授权失败 4授权中 5授权抖音号不匹配
     */
    @Schema(description = "巨量百应授权状态")
    private Integer authJlbyStatus;

    /**
     * 巨量百应授权状态变更时间
     */
    @Schema(description = "巨量百应授权状态变更时间")
    private Date authJlbyStatusTime;

    /**
     * 千川授权状态 0未授权 1已授权 2授权过期 3授权失败 4授权中 5授权抖音号不匹配
     */
    @Schema(description = "千川授权状态")
    private Integer authQcStatus;

    /**
     * 千川授权状态变更时间
     */
    @Schema(description = "千川授权状态变更时间")
    private Date authQcStatusTime;

    /**
     * 微信视频号授权状态 0未授权 1已授权 2微信后台取消授权 3用户取消授权（无变更时间字段）
     */
    @Schema(description = "微信视频号授权状态")
    private Integer authChannelStatus;

    /**
     * 抖音来客授权状态 0未授权 1已授权 2授权过期 3授权失败 4授权中 5授权抖音号不匹配
     */
    @Schema(description = "抖音来客授权状态")
    private Integer authLifeStatus;

    /**
     * 抖音来客授权状态变更时间
     */
    @Schema(description = "抖音来客授权状态变更时间")
    private Date authLifeStatusTime;
}
