package com.jiuyu.replay.words.bo.script;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 时间轴条目 Bo（T23 confirmStandardScript 入参列表元素）。
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class TimeAxisItemBo {

    /**
     * 时间段，如 "00:00-05:00"
     */
    @NotBlank(message = "时间段不能为空")
    private String timeRange;

    /**
     * 段落标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 话术内容
     */
    @NotBlank(message = "话术内容不能为空")
    private String content;
}
