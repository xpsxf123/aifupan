package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：ai问答返回数据
 * @date ：2025/3/20 下午6:15
 */
@Data
public class AiReturnDataVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 源数据id
     */
    private Long sourceId;

    /**
     * aiToken记录id
     */
    private List<Long> aiTokenId;

    /**
     * 会话id
     */
    private String contextId;

    /**
     * 状态码 0 成功，1失败
     */
    private Integer status;

    /**
     * 输出内容
     */
    private String content;

    /**
     * 思维链内容
     */
    private String reasoningContent;

    /**
     * 总内容
     */
    private String contentAll;

    /**
     * 本次请求的id
     */
    @Schema(description = "本次请求的id")
    private String requestId;

    /**
     * 模型生成结束原因
     通义调用：
     stop:因模型输出自然结束，或触发输入参数中的stop条件而结束时为stop
     length: 因生成长度过长而结束
     tool_calls: 因发生工具调用
     豆包调用:
     stop表示正常生成结束
     length 表示已经到了生成的最大 token 数量
     content_filter 表示模型输出命中审核提前终止
     */
    private String finishReason;

    /**
     * 输入token 数量
     */
    private Integer promptTokens;

    /**
     * 输出 token 数量
     */
    private Integer completionTokens;

    /**
     * 图片的 token 数量
     */
    private Integer imageTokens;

    /**
     * 音频的 token 数量
     */
    private Integer audioTokens;

    /**
     * 视频的 token 数量
     */
    private Integer videoTokens;

    /**
     * 上下文缓存的tokens数
     */
    private Integer cachedTokens;

    /**
     * 输出思维链内容花费的 token
     */
    private Integer reasoningTokens;

    /**
     * 本次请求消耗的总 token 数量
     */
    private Integer totalTokens;

    /**
     * 获取真实消耗的 token 数量
     * @return
     */
    public Integer getRealTotalTokens() {
        return this.totalTokens;
    }
}
