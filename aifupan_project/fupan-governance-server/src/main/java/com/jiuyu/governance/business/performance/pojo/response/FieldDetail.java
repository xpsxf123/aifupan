package com.jiuyu.governance.business.performance.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 字段详情（包含当前值、图片URL、修改状态）
 *
 * @param <T> 字段值类型
 * @author lj
 * @date 2026-03-28
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDetail<T> {

    /**
     * 当前值
     */
    private T value;

    /**
     * 凭证图片完整URL
     */
    private String imageUrl;

    /**
     * 是否已修改
     */
    private Boolean modified;
}
