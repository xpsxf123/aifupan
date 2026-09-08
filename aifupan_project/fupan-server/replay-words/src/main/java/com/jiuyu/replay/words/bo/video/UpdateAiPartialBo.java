package com.jiuyu.replay.words.bo.video;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：liwj
 * @description：TODO
 * @date ：2025/9/13 15:02
 */
@Data
@Schema(description = "更新ai页面的部分主播字段入参")
public class UpdateAiPartialBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主播标识")
    @NotBlank(message = "主播标识不能为空", groups = {update.class})
    private String secUid;

    @Schema(description = "视频id")
    @NotBlank(message = "视频id不能为空", groups = {updateNew.class})
    private String videoId;

    private Long userId;

    private Long tenantId;

    @Schema(description = "账号阶段，使用字典account_stage的值")
    private Integer accountStage;

    @Schema(description = "账号水平， 使用字典account_water_level的值")
    private Integer accountWaterLevel;

    @Schema(description = "流量结构，使用字典 account_flow的值")
    private Integer accountFlow;

    @Schema(description = "主播账号情况描述")
    private String anchorSituation;

    @Schema(description = "账号归属类型 0：自由账号 1：同行账号")
    private Integer accountType;

    @Schema(description = "ROI观测精度，使用字典roi_accuracy的值")
    private String roiAccuracy;

    public static interface update {
    }

    public static interface updateNew {
    }

}
