package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author ：lujie
 * @description：基础设置实体类
 * @date ：2025/1/7
 */
@Data
@TableName("tb_basic_settings")
@Schema(description = "基础设置")
public class BasicSettingsEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "来源类型 0：主播，1：视频，2：文件，")
    private Integer sourceType;

    @Schema(description = "用户表id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "账号归属类型 0：自由账号 1：同行账号")
    private Integer accountType;

    @Schema(description = "首播日期")
    private Date premiereDate;

    @Schema(description = "账号阶段，使用字典account_stage的值")
    private Integer accountStage;

    @Schema(description = "账号水平， 使用字典account_water_level的值")
    private Integer accountWaterLevel;

    @Schema(description = "流量结构，使用字典 account_flow的值")
    private Integer accountFlow;

    @Schema(description = "直播目标 使用字典 living_target的值")
    private Integer livingTarget;

    @Schema(description = "直播形式 使用字典 living_modality的值")
    private Integer livingModality;

    @Schema(description = "营销组件 使用字典 marketing的值")
    private Integer marketing;

    @Schema(description = "优化方向多选 使用字典 optimize_direction的值")
    private String optimizeDirection;

    @Schema(description = "学习方向多选 使用字典 learning的值")
    public String learning;

    @Schema(description = "直播间模式 使用字典 living_mode的值")
    private Integer livingMode;

    @Schema(description = "主播账号情况描述")
    private String anchorSituation;

    @Schema(description = "ROI观测精度 使用字典 roi_accuracy的值")
    private String roiAccuracy;

    @Schema(description = "创建时间")
    private LocalDateTime createDate;

    @Schema(description = "更新时间")
    private LocalDateTime updateDate;

}