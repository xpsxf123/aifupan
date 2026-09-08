package com.jiuyu.replay.words.vo.script;

import lombok.Data;

import java.util.List;

/**
 * T24 standardScriptDetail 响应 Vo。
 *
 * <p>无已确认稿时返回 hasScript=false + speechSpeed=280，其余字段 null。
 * standardScriptId 为 Long 类型，依赖全局 ToStringSerializer 序列化。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class StandardScriptDetailVo {

    /**
     * 是否有已确认稿
     */
    private Boolean hasScript;

    /**
     * 标准稿 ID；hasScript=false 时 null
     */
    private Long standardScriptId;

    /**
     * 主播唯一标识（回显）；hasScript=false 时 null
     */
    private String secUid;

    /**
     * 话术模式 0=非循环 1=循环；hasScript=false 时 null
     */
    private Integer speechMode;

    /**
     * 语速（字/分钟）；无稿时返回默认值 280
     */
    private Integer speechSpeed;

    /**
     * 循环话术预估时长（分钟）；hasScript=false 或非循环时 null
     */
    private Integer cycleDurationMinutes;

    /**
     * 参考脚本原文；hasScript=false 时 null
     */
    private String referenceScript;

    /**
     * 时间轴内容列表；hasScript=false 时 null
     */
    private List<TimeAxisItemVo> timeAxisScript;

    /**
     * 标准稿确认时间（ISO-8601 字符串）；hasScript=false 时 null
     */
    private String createDate;
}
