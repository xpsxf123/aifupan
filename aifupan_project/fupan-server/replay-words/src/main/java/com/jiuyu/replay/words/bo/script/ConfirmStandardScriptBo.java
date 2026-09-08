package com.jiuyu.replay.words.bo.script;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * T23 confirmStandardScript 入参 Bo。
 *
 * <p>按 (tenantId+userId+secUid) 联合定位落库。
 * speechMode=1 时 cycleDurationMinutes 必填，由 Bll 层业务校验。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class ConfirmStandardScriptBo {

    /**
     * 主播唯一标识（secUid）；标准稿主定位字段
     */
    @NotBlank(message = "secUid 不能为空")
    private String secUid;

    /**
     * 话术模式 0=非循环 1=循环
     */
    @NotNull(message = "speechMode 不能为空")
    private Integer speechMode;

    /**
     * 语速（字/分钟，100-500）
     */
    @NotNull(message = "speechSpeed 不能为空")
    @Min(value = 100, message = "语速不能低于 100 字/分钟")
    @Max(value = 500, message = "语速不能高于 500 字/分钟")
    private Integer speechSpeed;

    /**
     * 循环话术预估时长（分钟）；speechMode=1 时必填，由 Bll 层业务校验
     */
    private Integer cycleDurationMinutes;

    /**
     * 参考直播脚本原文（最少 200 字符，与 generateStandardScript 入参约束一致）
     */
    @NotBlank(message = "参考脚本不能为空")
    @Size(min = 200, message = "参考脚本不能少于 200 字符")
    private String referenceScript;

    /**
     * 时间轴内容列表（来自 T21 AI 生成 + 用户编辑）
     */
    @NotEmpty(message = "timeAxisScript 不能为空")
    @Valid
    private List<TimeAxisItemBo> timeAxisScript;
}
