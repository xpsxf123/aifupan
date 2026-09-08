package com.jiuyu.replay.order.dto;

import lombok.Getter;
import lombok.Setter;

/**
 *  是否已付费用户
 * @author HeHui
 * @date 2026-06-29 16:18
 */
@Getter
@Setter
public class PaidUserDto {

    /**
     * 是否已付费用户
     */
    private Boolean paid = false;

    /**
     * 是否已录制场次
     */
    private Boolean record = false;
}
