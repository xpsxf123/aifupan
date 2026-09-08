package com.jiuyu.replay.words.vo.script;

import lombok.Data;

import java.util.List;

/**
 * T21 generateStandardScript 响应 Vo。
 *
 * <p>不含 standardScriptId，此接口不落库，timeAxisScript 由前端暂存供 T23 确认。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class StandardScriptVo {

    /**
     * 话术模式（回显）0=非循环 1=循环
     */
    private Integer speechMode;

    /**
     * 语速（回显）字/分钟
     */
    private Integer speechSpeed;

    /**
     * 循环话术预估时长（分钟）；speechMode=0 时为 null
     */
    private Integer cycleDurationMinutes;

    /**
     * 参考脚本原文（回显）
     */
    private String referenceScript;

    /**
     * AI 生成时间轴列表（不含 standardScriptId）
     */
    private List<TimeAxisItemVo> timeAxisScript;
}
