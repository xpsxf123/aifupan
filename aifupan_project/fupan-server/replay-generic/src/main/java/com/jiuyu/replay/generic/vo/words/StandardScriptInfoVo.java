package com.jiuyu.replay.generic.vo.words;

import lombok.Data;

import java.io.Serializable;

/**
 * 标准直播稿跨模块传输 VO（replay-generic 声明，replay-ai 消费，replay-words 生产）。
 *
 * <p>禁止跨模块传递 {@code StandardScriptEntity}（规避跨模块直接依赖实体层）；
 * 只传本 VO 数据载体。Long 类型 ID 字段由全局 {@code JacksonSerializerConfig} 序列化为 String，
 * 禁止字段级 {@code @JsonSerialize}。</p>
 *
 * @author beta
 * @date 2026-06-12
 */
@Data
public class StandardScriptInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标准稿 ID（Snowflake）
     */
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 确认操作用户 ID
     */
    private Long userId;

    /**
     * 主播唯一标识
     */
    private String secUid;

    /**
     * 话术模式：0=非循环，1=循环
     */
    private Integer speechMode;

    /**
     * 循环话术预估时长（分钟）；speechMode=1 时必有
     */
    private Integer cycleDurationMinutes;

    /**
     * 时间轴 JSON String {@code [{"timeRange","title","content"}]}
     */
    private String timeAxisScript;
}
