package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 达人统计概览（单达人档案下钻，服务端一次聚合，见设计 doc17 §6.20）。
 *
 * <p>互动量口径：engagement = 赞 + 评 + 享 + 藏。播放相关字段（{@link #sumPlay}/{@link #avgPlay}/{@link #engagementRate}）
 * 在爬取库补齐 play_count 前恒为 null。赛道基准 {@link #radarBaseline} 在达人品类字段 + 达人库基准作业到位前恒为 null。</p>
 *
 * @author fupan-server
 */
@Data
public class InfluencerAnalyticsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参与统计的作品数（时间窗内、去重后）
     */
    private Integer sampleCount;

    /**
     * 单条互动量（赞+评+享+藏）均值
     */
    private Long avgEngagement;

    /**
     * 单条互动量中位数
     */
    private Long medianEngagement;

    /**
     * 单条互动量最高值
     */
    private Long maxEngagement;

    /**
     * 互动量最高的单条作品 id
     */
    private Long maxEngagementVideoId;

    /**
     * 互动量最高的单条作品标题
     */
    private String maxEngagementTitle;

    /**
     * 更新频率（条/周，按作品实际发布跨度折算）
     */
    private Double postingPerWeek;

    /**
     * 爆款数（单条互动量 ≥ breakoutThreshold）
     */
    private Integer breakoutCount;

    /**
     * 爆款率（breakoutCount / sampleCount）
     */
    private Double breakoutRate;

    /**
     * 爆款阈值（口径 = 3 × 中位互动量；中位为 0 时不识别爆款）
     */
    private Long breakoutThreshold;

    /**
     * 互动稳健离散度（MAD 中位绝对偏差 / 中位数；抗长尾爆款离群，越小越稳；中位为 0 时退回 标准差/均值）。
     * 雷达 consistency 由它换算：consistency = 100 / (1 + CV)。
     */
    private Double engagementCV;

    /**
     * 互动结构（环图）：点赞总量
     */
    private Long sumLike;

    /**
     * 互动结构：评论总量
     */
    private Long sumComment;

    /**
     * 互动结构：分享总量
     */
    private Long sumShare;

    /**
     * 互动结构：收藏总量
     */
    private Long sumCollect;

    /**
     * 时长分布（固定桶，含 count=0 桶以稳定坐标轴）
     */
    private List<DurationBucket> durationBuckets;

    /**
     * 发布节奏热力（周 × 时段，仅非零单元）
     */
    private List<HeatCell> publishHeatmap;

    /**
     * 互动趋势（按周降采样，最多约 16 个点，升序）
     */
    private List<TrendPoint> trendSeries;

    /**
     * 能力雷达该达人值（0–100 归一化）
     */
    private Radar radar;

    /**
     * 同赛道达人库中位基准（对标虚线）；品类 + 基准作业到位前为 null
     */
    private Radar radarBaseline;

    /**
     * 播放总量（play_count 到位后返回，否则 null）
     */
    private Long sumPlay;

    /**
     * 平均播放量（play_count 到位后返回，否则 null）
     */
    private Double avgPlay;

    /**
     * 真·互动率 = 互动量/播放量（play_count 到位后返回，否则 null）
     */
    private Double engagementRate;

    /**
     * 时长分布桶
     */
    @Data
    public static class DurationBucket implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 桶标签，如 "0-15s"
         */
        private String bucket;

        /**
         * 落入该桶的作品数
         */
        private Integer count;

        public DurationBucket() {
        }

        public DurationBucket(String bucket, Integer count) {
            this.bucket = bucket;
            this.count = count;
        }
    }

    /**
     * 发布热力单元
     */
    @Data
    public static class HeatCell implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 星期几：1-7（周一至周日）
         */
        private Integer weekday;

        /**
         * 时段槽：0-5（每 4 小时一段，0=[0,4) ... 5=[20,24)）
         */
        private Integer slot;

        /**
         * 该单元的作品数
         */
        private Integer count;

        public HeatCell() {
        }

        public HeatCell(Integer weekday, Integer slot, Integer count) {
            this.weekday = weekday;
            this.slot = slot;
            this.count = count;
        }
    }

    /**
     * 互动趋势点（周维度）
     */
    @Data
    public static class TrendPoint implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 周起始日（yyyy-MM-dd，周一）
         */
        private String date;

        /**
         * 该周互动量合计
         */
        private Long engagement;

        /**
         * 该周作品数
         */
        private Integer videoCount;

        public TrendPoint() {
        }

        public TrendPoint(String date, Long engagement, Integer videoCount) {
            this.date = date;
            this.engagement = engagement;
            this.videoCount = videoCount;
        }
    }

    /**
     * 能力雷达 6 轴（0–100）
     */
    @Data
    public static class Radar implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 影响力（粉丝规模）
         */
        private Double reach;

        /**
         * 产能（更新频率）
         */
        private Double output;

        /**
         * 爆发力（爆款率）
         */
        private Double breakout;

        /**
         * 互动力（均互动/条）
         */
        private Double engagement;

        /**
         * 传播力（分享占比）
         */
        private Double virality;

        /**
         * 稳定性 = 100 / (1 + robustCV)（平滑衰减，CV 越大越低但不硬归零）
         */
        private Double consistency;
    }
}
