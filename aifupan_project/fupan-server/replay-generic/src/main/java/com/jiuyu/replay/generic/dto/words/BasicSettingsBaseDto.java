package com.jiuyu.replay.generic.dto.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/1/20 10:24
 */
@Data
public class BasicSettingsBaseDto {

    @Schema(description = "账号归属类型 0：自由账号 1：同行账号")
    public Integer accountType;

    @Schema(description = "首播日期")
    public Date premiereDate;

    @Schema(description = "账号阶段，使用字典account_stage的值")
    public Integer accountStage;

    @Schema(description = "账号水平， 使用字典account_water_level的值")
    public Integer accountWaterLevel;

    @Schema(description = "流量结构，使用字典 account_flow的值")
    public Integer accountFlow;

    @Schema(description = "直播目标 使用字典 living_target的值")
    public Integer livingTarget;

    @Schema(description = "直播形式 使用字典 living_modality的值")
    public Integer livingModality;

    @Schema(description = "营销组件 使用字典 marketing的值")
    public Integer marketing;

    @Schema(description = "优化方向多选 使用字典 optimize_direction的值")
    public String optimizeDirection;

    @Schema(description = "学习方向多选 使用字典 learning的值")
    public String learning;

    @Schema(description = "直播间模式 使用字典 living_mode的值")
    public Integer livingMode;

    @Schema(description = "主播账号情况描述")
    public String anchorSituation;

    @Schema(description = "ROI观测精度 使用字典 roi_accuracy的值")
    public String roiAccuracy;
}
