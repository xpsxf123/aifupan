package com.jiuyu.replay.generic.vo.words;

import lombok.Getter;
import lombok.Setter;

/**
 *  提示词信息摘要
 * @author HeHui
 * @date 2026-07-22 19:08
 */
@Getter
@Setter
public class CueWordsDescVo extends CueWordsNameVo {

    /**
     * 提示词概要
     */
    private String outline;

    public CueWordsDescVo() {
    }

    public CueWordsDescVo(Long id, String cueWord, String outline) {
        super(id, cueWord);
        this.outline = outline;
    }
}
