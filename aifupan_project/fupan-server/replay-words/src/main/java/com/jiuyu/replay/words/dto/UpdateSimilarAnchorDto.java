package com.jiuyu.replay.words.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改热榜主播对象
 *
 * @author HeHui
 * @date 2026-01-28 17:42
 */
@Getter
@Setter
public class UpdateSimilarAnchorDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7551318412964820484L;

    /**
     * ID（tb_similar_anchor主键）
     */
    @NotNull(message = "ID不能为空")
    @Schema(description = "ID（tb_similar_anchor主键）")
    private Long id;


    /**
     * 平均UV价值
     */
    @Schema(description = "平均UV价值")
    private String liveAverageUv;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数")
    private String followerCount;

    /**
     * 平均场观
     */
    @Schema(description = "平均场观")
    private String liveAverageUser;

    /**
     * 场均销售额
     */
    @Schema(description = "场均销售额")
    private String liveAverageAmount;

    /**
     * 直播销售总额
     */
    @Schema(description = "直播销售总额")
    private String totalAmount;

    /**
     * 销售指数
     */
    @Schema(description = "销售指数")
    private String liveTotalAmountCmmInd;

    /**
     * 直播场次
     */
    @Schema(description = "直播场次")
    private Integer liveCount;

    /**
     * 平均停留时长
     */
    @Schema(description = "平均停留时长")
    private String liveAverageOnline;


    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;

    /**
     * 主播头像
     */
    @Schema(description = "主播头像")
    private String anchorAvatar;

}
