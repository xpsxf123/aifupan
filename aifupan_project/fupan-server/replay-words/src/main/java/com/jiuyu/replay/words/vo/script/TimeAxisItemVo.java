package com.jiuyu.replay.words.vo.script;

import lombok.Data;

/**
 * 时间轴条目 Vo（T21/T24 响应体中的时间轴列表元素）。
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class TimeAxisItemVo {

    /**
     * 时间段，如 "00:00-05:00"
     */
    private String timeRange;

    /**
     * 段落标题
     */
    private String title;

    /**
     * 话术内容
     */
    private String content;
}
