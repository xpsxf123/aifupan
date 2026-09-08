package com.jiuyu.replay.words.bo.script;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * T21 generateStandardScript 入参 Bo。
 *
 * <p>纯 AI 调用，不落库不归属，无需 secUid / anchorUrlUserId。
 * speechMode=1（循环话术）时 cycleDurationMinutes 必填，由 Bll 层业务校验。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class GenerateStandardScriptBo {

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
     * 参考直播脚本原文（最少 200 字符，太短 AI 无法推理生成合规时间轴 JSON）
     */
    @NotBlank(message = "参考脚本不能为空")
    @Size(min = 200, message = "参考脚本不能少于 200 字符")
    private String referenceScript;
}
