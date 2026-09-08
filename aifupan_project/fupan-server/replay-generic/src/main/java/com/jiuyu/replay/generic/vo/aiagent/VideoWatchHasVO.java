package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Getter;
import lombok.Setter;

/**
 *  数据看板是否有某些指标数据
 * @author HeHui
 * @date 2026-07-30 21:32
 */
@Getter
@Setter
public class VideoWatchHasVO {

    /**
     * 总观看
     */
    private Integer watchNum;


    /**
     * 是否有投放 roi数据
     */
    private Boolean hasRoi;
}
