package com.jiuyu.replay.api.logic.ai.placeholder;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 占位符 Token — 从 prompt 中提取出的单个占位符信息
 *
 * @author jy
 * @date 2026-06-18
 */
@Data
@AllArgsConstructor
public class PlaceholderToken {

    /**
     * 完整占位符，如 "#{trade}"、 "#{uvTop:10}"
     */
    private String fullKey;

    /**
     * 占位符 key，如 "trade"、"uvTop"
     */
    private String key;

    /**
     * 参数值，如 "#{uvTop:10}" → "10"；无参数时为 null
     */
    private String param;

    /**
     * 在原始 prompt 中的起始位置
     */
    private int start;

    /**
     * 在原始 prompt 中的结束位置
     */
    private int end;
}
