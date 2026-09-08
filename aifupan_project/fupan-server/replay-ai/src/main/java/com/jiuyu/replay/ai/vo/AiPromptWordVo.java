package com.jiuyu.replay.ai.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2026/2/28 11:16
 */
@Data
public class AiPromptWordVo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 提示词
     */
    private String prompt;

    /**
     * 站位符： 违规原因
     */
    private String reason;

    /**
     * 站位符： 优化动作
     */
    private Integer optimizeActions;
}
