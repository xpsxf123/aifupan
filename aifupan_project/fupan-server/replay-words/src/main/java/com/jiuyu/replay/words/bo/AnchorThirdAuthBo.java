package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主播三方授权表详情查询BO
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Data
@Schema(description = "主播三方授权表详情查询")
public class AnchorThirdAuthBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Schema(description = "主键id")
    private Long id;

    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;

    /**
     * 授权渠道
     */
    @Schema(description = "授权渠道，1为巨量")
    private Integer authChannel;

    /**
     * 主播id
     */
    @Schema(description = "主播id")
    private String secUid;

    /**
     * 授权状态,0为未授权,1为已授权,2为已失效
     */
    @Schema(description = "授权状态,0为未授权,1为已授权,2为已失效")
    private Integer authStatus;

    /**
     * 授权关键信息json（适应不同三方）
     */
    @Schema(description = "授权关键信息json（适应不同三方）")
    private String authInfo;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    /**
     * 创建者id
     */
    @Schema(description = "创建者id")
    private Long createUserId;

    /**
     * 修改者id
     */
    @Schema(description = "修改者id")
    private Long updateUserId;

    /**
     * 是否删除，0为未删除，1位删除
     */
    @Schema(description = "是否删除，0为未删除，1为删除")
    private Integer isDeleted;

}