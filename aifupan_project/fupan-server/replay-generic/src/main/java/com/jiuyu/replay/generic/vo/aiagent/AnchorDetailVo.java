package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 主播详情（基础信息 + 配置信息 + 基础设置），所有字典字段随响应解析为中文文案。
 *
 * @author fupan-server
 */
@Data
public class AnchorDetailVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== ① 基础信息 =====

    /**
     * 主播唯一标识
     */
    private String secUid;
    private String anchorName;
    private String anchorAvatar;

    /**
     * 平台 code + 文案
     */
    private Integer platform;
    private String platformLabel;

    private String anchorNumber;

    private Long tradeId;
    private String tradeName;

    /**
     * 自有/同行 code + 文案
     */
    private Integer accountType;
    private String accountTypeLabel;

    private String addDate;
    private String lastRecordTime;

    // ===== ② 配置信息（字典 code + 解析文案）=====

    private Integer accountStage;
    private String accountStageLabel;

    private Integer accountWaterLevel;
    private String accountWaterLevelLabel;

    private Integer accountFlow;
    private String accountFlowLabel;

    // ===== ③ 基础设置 =====

    /**
     * 基础设置；不存在时为 null
     */
    private AnchorBasicSettingsVo basicSettings;
}
