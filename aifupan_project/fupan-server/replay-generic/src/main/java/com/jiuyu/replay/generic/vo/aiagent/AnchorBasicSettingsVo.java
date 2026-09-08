package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 主播基础设置（源自 tb_basic_settings），字典字段同时返回原始 code 与解析文案。
 *
 * @author fupan-server
 */
@Data
public class AnchorBasicSettingsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 首播日期
     */
    private String premiereDate;

    /**
     * 账号阶段 code（字典 account_stage）
     */
    private Integer accountStage;
    private String accountStageLabel;

    /**
     * 账号水平 code（字典 account_water_level）
     */
    private Integer accountWaterLevel;
    private String accountWaterLevelLabel;

    /**
     * 流量结构 code（字典 account_flow）
     */
    private Integer accountFlow;
    private String accountFlowLabel;

    /**
     * 直播目标 code（字典 living_target）
     */
    private Integer livingTarget;
    private String livingTargetLabel;

    /**
     * 直播形态 code（字典 living_modality）
     */
    private Integer livingModality;
    private String livingModalityLabel;

    /**
     * 营销方式 code（字典 marketing）
     */
    private Integer marketing;
    private String marketingLabel;

    /**
     * 直播模式 code（字典 living_mode）
     */
    private Integer livingMode;
    private String livingModeLabel;

    /**
     * 优化方向（多选字典 optimize_direction 解析文案）
     */
    private String optimizeDirection;

    /**
     * 学习方向（多选字典 learning 解析文案）
     */
    private String learning;

    /**
     * 账号情况描述（自由文本）
     */
    private String anchorSituation;
}
