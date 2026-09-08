package com.jiuyu.governance.common.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 下拉选项
 *
 * @author HeHui
 * @date 2026-03-18 17:32
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LabelOption {

    /**
     * 选项值
     */
    private Long key;

    /**
     * 选项标签
     */
    private String label;
}
