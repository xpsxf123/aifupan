package com.jiuyu.replay.words.vo.script;

import lombok.Data;

/**
 * T23 confirmStandardScript 响应 Vo。
 *
 * <p>standardScriptId 为 Long 类型，依赖 JacksonSerializerConfig 全局 ToStringSerializer
 * 序列化为 19 位字符串输出给前端，禁止在字段级加 @JsonSerialize 注解。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@Data
public class StandardScriptConfirmVo {

    /**
     * 已确认标准稿 ID（Snowflake 19 位，全局序列化为 String 输出前端）
     */
    private Long standardScriptId;
}
