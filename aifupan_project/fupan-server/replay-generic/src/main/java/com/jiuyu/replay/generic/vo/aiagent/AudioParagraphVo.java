package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 视频音频段落全文项（仅段落序号 + 全文文本，不含逐词时间戳）。
 *
 * @author fupan-server
 */
@Data
public class AudioParagraphVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 段落序号（从 1 开始，即第几分钟段）
     */
    private Integer paragraph;

    /**
     * 这一分钟段落对应的时间
     */
    private String time;

    /**
     * 这一分钟段落的全文文本
     */
    private String content;
}
