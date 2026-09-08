package com.jiuyu.replay.generic.vo.words;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 定制提示词返回响应
 *
 * @author HeHui
 * @date 2026-03-05 16:39
 */
@Getter
@Setter
public class CustomizeCueWordsResponse {

    /**
     * 全局通用
     */
    private List<CueWordsListVo> universal;

    /**
     * 租户定制
     */
    private List<CueWordsListVo> tenantCustomize;
}
