package com.jiuyu.replay.generic.vo.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI内容校正配置
 *
 * @author lujie
 * @date 2026-06-01
 */
@Data
public class AiContentCorrectionConfigVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String modelCode;

    private String contentPrompt;
}
