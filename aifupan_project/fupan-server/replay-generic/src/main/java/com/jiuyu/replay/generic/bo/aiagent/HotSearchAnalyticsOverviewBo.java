package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 爆款选题拆解聚合入参（某爆款关键词下的选题分析，服务端一次聚合，见接口文档 §6.21）。
 *
 * <p>统计范围 = 该 searchId 下的爆款视频（tb_video_hot_search_video JOIN tb_video_info），
 * 属<b>全网爬取库口径</b>、不做租户裁剪；租户口径只体现在 {@code tenantExtractedCount} 一个出参字段上。</p>
 *
 * <p>统计时间窗 = publishStartTime~publishEndTime（按 publish_time 过滤），不传默认近 7 天；
 * <b>跨度硬上限 1 年（≤366 天）</b>，服务端兜底夹取，越界不做无界扫描。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HotSearchAnalyticsOverviewBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 爆款搜索 id（tb_video_hot_search.id，来自 §6.18）
     */
    @NotNull(message = "searchId 不能为空")
    private Long searchId;

    /**
     * 平台类型：1-抖音 2-快手 3-视频号；不传=不按平台收窄
     */
    private Byte platformType;

    /**
     * 统计时间窗-起（yyyy-MM-dd HH:mm:ss，按 publish_time >=）；不传默认 (止 - 7 天)
     */
    private String publishStartTime;

    /**
     * 统计时间窗-止（yyyy-MM-dd HH:mm:ss，按 publish_time <=）；不传默认当前时间
     */
    private String publishEndTime;

    /**
     * 保留位：调用方声明自己按租户口径消费本结果。
     *
     * <p>当前实现下 {@code tenantExtractedCount} 恒定按 tenantId 计算，本字段不改变任何返回值，
     * 仅用于与 §6.19 的 {@code tenantScopeExtract} 保持调用侧语义一致。</p>
     */
    private Boolean tenantScopeExtract = false;
}
