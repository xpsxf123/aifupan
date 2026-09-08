package com.jiuyu.replay.generic.bo.governance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通知企业管理服务端添加主播的请求参数
 */
@Data
@Schema(description = "通知企业管理服务端添加主播的请求参数")
public class GovernanceAddAnchorBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 直播平台类型（0:抖音 1:快手 2:视频号） */
    @Schema(description = "直播平台类型（0:抖音 1:快手 2:视频号）")
    private Integer platform;

    /** 主播平台账号 */
    @Schema(description = "主播平台账号")
    private String anchorNumber;

    /** 主播唯一标识 */
    @Schema(description = "主播唯一标识")
    private String secUid;

    /** 主页url */
    @Schema(description = "主页url")
    private String homeUrl;

    /** 直播间url */
    @Schema(description = "直播间url")
    private String liveUrl;

    /** 主播名称 */
    @Schema(description = "主播名称")
    private String anchorName;

    /** 主播头像 */
    @Schema(description = "主播头像")
    private String anchorAvatar;

    /** 绑定的行业分类ID */
    @Schema(description = "绑定的行业分类ID")
    private Long tradeId;
}
