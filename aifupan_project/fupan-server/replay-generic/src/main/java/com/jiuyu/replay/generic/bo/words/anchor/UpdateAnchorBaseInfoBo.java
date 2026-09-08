package com.jiuyu.replay.generic.bo.words.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "更新主播基础信息")
public class UpdateAnchorBaseInfoBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主播SecUid
     */
    @Schema(description = "主播SecUid")
    private String secUid;
    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;
    /**
     * 主播抖音用户id
     */
    @Schema(description = "主播抖音用户id")
    private String anchorUserId;
    /**
     * 主播抖音号
     */
    @Schema(description = "主播抖音号")
    private String anchorNumber;
    /**
     * 主播头像地址
     */
    @Schema(description = "主播头像地址")
    private String anchorAvatar;
}
