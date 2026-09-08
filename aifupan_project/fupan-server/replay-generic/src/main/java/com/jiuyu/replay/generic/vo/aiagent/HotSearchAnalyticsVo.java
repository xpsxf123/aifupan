package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 爆款选题拆解聚合（某爆款关键词下的选题分析，服务端一次聚合，见接口文档 §6.21）。
 *
 * <p>互动量口径：{@code engagement = 赞 + 评 + 享 + 藏}。爬取库无 play_count，故<b>不提供</b>播放量 / 互动率 / 完播类指标。</p>
 *
 * <p>样本 = 该 searchId + 时间窗 + 平台 下按 engagement 降序的前 N 条；命中上限时 {@link #truncated} 为 true，
 * 前端须如实标注"统计基于前 N 条"。小样本（如 &lt;20 / &lt;5）不在服务端特殊处理，此处照常返回真实数字。</p>
 *
 * <p>除 {@link #tenantExtractedCount} 外，全部指标为<b>全网爬取库口径</b>，不是本租户口径。</p>
 *
 * @author fupan-server
 */
@Data
public class HotSearchAnalyticsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 爆款搜索 id（回显）
     */
    private Long searchId;

    /**
     * 爆款关键词（回显，取自 tb_video_hot_search）
     */
    private String searchKeyword;

    /**
     * 该爆款词的平台类型：1-抖音 2-快手 3-视频号（回显）
     */
    private Byte platformType;

    /**
     * 该爆款词最后同步时间（回显，yyyy-MM-dd HH:mm:ss）
     */
    private String lastSyncTime;

    /**
     * 实际生效的统计窗口-起（服务端兜底 / 夹取后的值，yyyy-MM-dd HH:mm:ss）
     */
    private String windowStart;

    /**
     * 实际生效的统计窗口-止（yyyy-MM-dd HH:mm:ss）
     */
    private String windowEnd;

    /**
     * 参与统计的作品数
     */
    private Integer sampleCount;

    /**
     * 样本是否被截断（命中服务端上限，此时全部指标基于互动量 Top N 子集）
     */
    private Boolean truncated;

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
     * 爆款门槛线 = 样本互动量降序第 ceil(n × 10%) 位的值（Top10% 下限）；样本不足 10 条时等于最高值
     */
    private Long topDecileThreshold;

    /**
     * 集中度 = 互动量最高的前 10 条之和 / 样本总互动量（0~1，越高越"赢家通吃"）；总互动量为 0 时返 0
     */
    private Double top10Share;

    /**
     * 互动稳健离散度（MAD 中位绝对偏差 / 中位数；抗长尾爆款离群，越小越均匀；中位为 0 时退回 标准差/均值）
     */
    private Double engagementCV;

    /**
     * 互动结构：点赞总量
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
     * 趋势粒度："day"（窗口跨度 ≤15 天）或 "week"（>15 天），由服务端按窗口跨度自动决定并回显
     */
    private String trendGranularity;

    /**
     * 互动趋势（按 {@link #trendGranularity} 聚合，升序）
     */
    private List<TrendPoint> trendSeries;

    /**
     * 时长分布（固定 5 桶，含 count=0 桶以稳定坐标轴）
     */
    private List<DurationBucket> durationBuckets;

    /**
     * 发布节奏热力（周 × 时段，仅非零单元）
     */
    private List<HeatCell> publishHeatmap;

    /**
     * 该选题下的头部达人 Top10（按作品数主序、互动量次序）
     */
    private List<AuthorStat> topAuthors;

    /**
     * 该选题下的高频 #标签 Top20（从标题确定性抽取，不做分词/造词）
     */
    private List<TagStat> topTags;

    /**
     * 样本内<b>本租户</b>已提取成功文案的条数（文案可用性，唯一的租户口径字段）
     */
    private Integer tenantExtractedCount;

    /**
     * 互动趋势点
     */
    @Data
    public static class TrendPoint implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 粒度起始日（yyyy-MM-dd；week 粒度下为该周周一）
         */
        private String date;

        /**
         * 该点互动量合计
         */
        private Long engagement;

        /**
         * 该点作品数
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
     * 时长分布桶（比 §6.20 多 avgEngagement：业务要的是"哪个片长更容易爆"，不是"哪个片长条数多"）
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

        /**
         * 该桶单条平均互动量（count=0 时为 0）
         */
        private Long avgEngagement;

        public DurationBucket() {
        }

        public DurationBucket(String bucket, Integer count, Long avgEngagement) {
            this.bucket = bucket;
            this.count = count;
            this.avgEngagement = avgEngagement;
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
     * 选题下的头部达人
     */
    @Data
    public static class AuthorStat implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 达人 id（= tb_video_info.author_id，与 §6.19 列表项的 authorId 同口径，可交叉跳转）
         */
        private String authorId;

        /**
         * 达人昵称（取样本内最后出现的非空值，可能为空）
         */
        private String authorName;

        /**
         * 平台类型：1-抖音 2-快手 3-视频号
         */
        private Byte platformType;

        /**
         * 头像 URL（来自 tb_video_hot_search_video 冗余列，对上榜账号必然有值）
         */
        private String avatar;

        /**
         * 粉丝数（同上，爬取时点快照）
         */
        private Long followersCount;

        /**
         * 平台用户 id（= secUid；同上）
         */
        private String platformUserId;

        /**
         * 该达人在样本内的作品数
         */
        private Integer videoCount;

        /**
         * 该达人在样本内的互动量合计
         */
        private Long sumEngagement;

        /**
         * 该达人在样本内的单条最高互动量
         */
        private Long maxEngagement;
    }

    /**
     * 选题下的高频 #标签
     */
    @Data
    public static class TagStat implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 标签文本（不含 #，已小写归一）
         */
        private String tag;

        /**
         * 命中该标签的作品数（同一条视频内重复标签只计 1 次）
         */
        private Integer count;

        /**
         * 命中该标签的作品互动量合计
         */
        private Long sumEngagement;
    }
}
