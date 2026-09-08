package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 达人统计概览入参（单达人档案下钻，服务端一次聚合，见设计 doc17 §6.20）。
 *
 * <p>范围由 {@code scope} 决定：1=本租户可见作品（默认，要求该达人已被本租户订阅；订阅即统计其全部作品，未订阅返回全零）、
 * 2=该达人全库爬取作品（跨租户读公共库）。
 * 统计时间窗 = publishStartTime~publishEndTime，不传默认近 3 个月；<b>跨度硬上限 1 年（≤366 天）</b>，
 * 服务端兜底夹取，越界不做无界扫描。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfluencerAnalyticsOverviewBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 达人 id（tb_video_influencer_info.id，= tb_video_info.author_id 的原值）
     */
    @NotNull(message = "influencerId 不能为空")
    private Long influencerId;

    /**
     * 平台类型：1-抖音 2-快手 3-视频号；不传=不按平台收窄
     */
    private Byte platformType;

    /**
     * 统计时间窗-起（yyyy-MM-dd HH:mm:ss，按 publish_time >=）；不传默认 (止 - 3 个月)
     */
    private String publishStartTime;

    /**
     * 统计时间窗-止（yyyy-MM-dd HH:mm:ss，按 publish_time <=）；不传默认当前时间
     */
    private String publishEndTime;

    /**
     * 统计口径：1=本租户可见作品（默认，须已订阅该达人） 2=该达人全库爬取作品
     */
    private Byte scope;
}
