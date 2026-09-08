package com.jiuyu.replay.generic.vo.words;

import lombok.Getter;
import lombok.Setter;

/**
 *  提示词信息名称
 * @author HeHui
 * @date 2026-07-22 19:08
 */
@Getter
@Setter
public class CueWordsNameVo {

    /**
     * 提示词id
     */
    private Long id;


    /**
     * 提示词名称
     */
    private String name;

    public CueWordsNameVo() {
    }

    public CueWordsNameVo(Long id, String cueWord) {
        this.id = id;
        this.name = cueWord;
    }
}
