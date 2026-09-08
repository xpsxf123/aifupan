package com.jiuyu.replay.words.bo.anchor;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 切换行业第三方采集
 *
 * @author HeHui
 * @date 2026-02-02 10:46
 */
@Getter
@Setter
public class SwitchThirdCollect {

    /**
     * 行业id
     */
    @NotNull(message = "行业id不能为空")
    private Long tradeId;


    /**
     * 是否采集
     */
    @NotNull(message = "是否采集不能为空")
    private Boolean collect;
}
