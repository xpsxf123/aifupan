package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchListSubscriptionQueryBo;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 视频基础信息表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-13
 */
@Mapper
public interface VideoInfoDao extends BaseMapper<VideoInfoEntity> {

    /**
     * 批量查询视频信息
     * 根据多个(platformType, platformVideoId)组合查询
     *
     * @param  videoIds 视频查询条件列表
     *
     * @return 视频信息实体列表
     */
    List<VideoInfoEntity> batchQueryByPlatformAndVideoIds(@Param("videoIds") Collection<String> videoIds, @Param("platformTypes") Collection<Byte> platformTypes);

    /**
     * 查询视频信息列表根据视频ID和条件
     *
     * @param tenantId                       租户ID
     * @param searchId                       搜索ID
     * @param queryBo                        查询条件
     * @param subscriptionLikeCountThreshold 订阅点赞阈值
     * @return
     */
    Page<VideoHotSearchResultVo> queryVideoListByIdsAndCondition(
            @Param("page") Page<VideoHotSearchResultVo> page,
            @Param("tenantId") Long tenantId, @Param("searchId") Long searchId,
            @Param("queryBo") VideoHotSearchListSubscriptionQueryBo queryBo,
            @Param("subscriptionLikeCountThreshold") Integer subscriptionLikeCountThreshold,
            @Param("videoUpdateStartTime") LocalDateTime videoUpdateStartTime,
            @Param("videoUpdateEndTime") LocalDateTime videoUpdateEndTime
    );

    /**
     * 统计视频总数根据搜索Id和点赞量
     *
     * @param searchId  搜索ID
     * @param likeCount 点赞量
     * @return
     */
    Integer countVideoNumberBySearchIdAndLikeCount(@Param("searchId") Long searchId, @Param("likeCount") Integer likeCount);

    /**
     * 批量根据搜索ID和点赞数统计视频数量
     *
     * @param queryParams 查询参数列表，包含searchId和likeCount
     * @return 查询结果列表，包含searchId、likeCount和对应的视频数量
     */
    List<VideoCountResult> batchCountVideoNumberBySearchIdAndLikeCount(@Param("queryParams") List<VideoCountQuery> queryParams);

    /**
     * 批量根据达人平台用户ID统计视频数量
     *
     * @param authorIds 达人平台用户ID列表
     * @return 查询结果列表，包含authorId和对应的视频数量
     */
    List<AuthorVideoCountResult> batchCountVideoNumberByAuthorIds(@Param("authorIds") List<String> authorIds);

    /**
     * 批量统计租户已采集的达人短视频数量（tb_video_user_video JOIN tb_video_info，按 author_id 聚合）。
     *
     * @param tenantId  租户 id
     * @param userId    子账号 id（userType=2 时传，其余传 null）
     * @param authorIds 达人 id 字符串集合（= tb_video_info.author_id）
     * @return 每个 author_id 对应的已采集视频数
     */
    List<AuthorVideoCountResult> batchCountCollectedByAuthorIds(@Param("tenantId") Long tenantId,
                                                                @Param("userId") Long userId,
                                                                @Param("authorIds") Collection<String> authorIds);

    /**
     * 批量统计达人短视频互动汇总（数量 + 点赞/评论/分享/收藏 SUM），范围为全库爬取作品。
     *
     * <p>租户口径（仅统计本租户已订阅达人）由调用方先把 authorIds 收窄为「已订阅达人」再传入，
     * 本查询不再关联 tb_video_user_video —— 订阅了达人即可见该达人全部作品。</p>
     *
     * @param authorIds        达人 id 字符串集合
     * @param publishStartTime 发布时间-起（可空）
     * @param publishEndTime   发布时间-止（可空）
     *
     * @return 每个 author_id 对应的统计
     */
    List<AuthorVideoStatsResult> batchStatsByAuthorIds(@Param("authorIds") Collection<String> authorIds,
                                                       @Param("publishStartTime") String publishStartTime,
                                                       @Param("publishEndTime") String publishEndTime);

    /**
     * 本租户可见范围内的短视频检索：可见范围 = 本租户已订阅达人（tb_video_user_influencer_subscription）
     * 名下的全部 tb_video_info，与该租户是否提取过文案无关。
     *
     * <p>支持 keyword / 平台 / 发布时间过滤 + 动态排序 + 复合 keyset 游标分页，语义与全库检索一致。
     * {@code tenantExtracted} 按「本租户文案提取成功」（tb_video_user_video.extract_status=3）做 EXISTS / NOT EXISTS 过滤。</p>
     *
     * <p><b>authorIds 必须非空</b>：由调用方在 Java 侧算好「订阅达人 ∩ 达人维度过滤」后传入。
     * 不在 SQL 内用子查询 CAST 订阅表的 influencer_id 去比 author_id —— CAST 结果携带连接默认字符序，
     * 与列字符序不一致时会抛 error 1267 Illegal mix of collations。</p>
     *
     * @param tenantId         租户 id（仅用于 tenantExtracted 的 EXISTS 子查询）
     * @param authorIds        达人 id 字符串集合（= author_id，非空）
     * @param platformTypes    平台类型集合（可空）
     * @param tenantExtracted  本租户文案提取成功过滤：null=不过滤 true=仅提取成功 false=仅未提取成功
     * @param keyword          标题/描述模糊词（可空）
     * @param publishStartTime 发布时间起（可空）
     * @param publishEndTime   发布时间止（可空）
     * @param sortField        排序字段：0=id 1=点赞 2=评论 3=分享 4=收藏 5=发布时间
     * @param desc             是否降序
     * @param idOnly           是否仅按 id keyset（sortField=0 或首页无排序值时）
     * @param cursorSortVal    上一页末行排序列值（复合 keyset 用；idOnly 时忽略）
     * @param cursorId         上一页末行 id（keyset 用；首页 null）
     * @param limit            取数条数（页大小 + 1）
     * @return 视频明细行（保持排序）
     */
    List<VideoInfoEntity> pageSubscribedInfluencerVideos(@Param("tenantId") Long tenantId,
                                                         @Param("authorIds") Collection<String> authorIds,
                                                         @Param("platformTypes") Collection<Byte> platformTypes,
                                                         @Param("tenantExtracted") Boolean tenantExtracted,
                                                         @Param("keyword") String keyword,
                                                         @Param("publishStartTime") String publishStartTime,
                                                         @Param("publishEndTime") String publishEndTime,
                                                         @Param("sortField") int sortField,
                                                         @Param("desc") boolean desc,
                                                         @Param("idOnly") boolean idOnly,
                                                         @Param("cursorSortVal") Object cursorSortVal,
                                                         @Param("cursorId") Long cursorId,
                                                         @Param("limit") int limit);

    /**
     * 取本租户在给定视频集合中「文案提取成功」的视频 id（tb_video_user_video.extract_status=3）。
     *
     * <p>tb_video_user_video 仅在该租户发起过提取时才有记录（成功、失败都会落行），
     * 因此这里是判定「本租户能否读该视频文案」的唯一依据。</p>
     *
     * @param tenantId 租户 id
     * @param videoIds 视频 id 集合（调用方分批传入）
     * @return 本租户提取成功的视频 id 列表
     */
    List<Long> listTenantExtractedVideoIds(@Param("tenantId") Long tenantId,
                                           @Param("videoIds") Collection<Long> videoIds);

    /**
     * 批量取指定视频在当前租户下的采集时间（单表聚合，MAX(created_date)）。
     *
     * @param tenantId 租户 id
     * @param userId   子账号 id（userType=2 时传，其余传 null）
     * @param videoIds 视频 id 集合
     * @return 视频 id + 采集时间
     */
    List<VideoCollectTimeRow> listVideoCollectTimes(@Param("tenantId") Long tenantId,
                                                    @Param("userId") Long userId,
                                                    @Param("videoIds") Collection<Long> videoIds);

    /**
     * 按 tb_video_info 主键批量查询 id + video_hash（用于批量取 Mongo 原文）。
     *
     * @param ids 视频主键集合
     * @return id 与 video_hash 映射行
     */
    List<VideoHashRow> listVideoHashByIds(@Param("ids") Collection<Long> ids);

    /**
     * 某爆款关键词下的视频流式分页（keyset 游标，tb_video_hot_search_video JOIN tb_video_info）。
     *
     * <p>返回视频 id + 排序值（sortVal，CAST 成字符串供上层编码游标）；按 (排序列, video_id) keyset 游标。
     * keysetDesc 统一控制排序方向与游标比较方向：true → 列/id 降序、取更小；false → 升序、取更大。</p>
     *
     * @param searchId        爆款搜索 id
     * @param keyword         标题/描述关键词（可空）
     * @param tags            标题内 #标签集合（可空，任一命中）
     * @param likeCountMin    点赞下限（可空）
     * @param commentCountMin 评论下限（可空）
     * @param shareCountMin   分享下限（可空）
     * @param collectCountMin 收藏下限（可空）
     * @param durationMin     时长下限（可空）
     * @param durationMax     时长上限（可空）
     * @param authorIds       达人 id 字符串集合（= v.author_id，可空；按达人维度收窄）
     * @param sortField       排序字段：0-sort_order 1-点赞 2-评论 3-分享 4-收藏 5-发布时间
     * @param keysetDesc      keyset 方向（true 降序取更小 / false 升序取更大）
     * @param lastSortVal     游标：上一页最后一条的排序值（首页 null）
     * @param lastVideoId     游标：上一页最后一条的 video_id（首页 null）
     * @param limit           条数（页大小 + 1 用于判断 hasMore）
     * @return 视频 id + 排序值行（按排序）
     */
    List<HotSearchVideoRow> pageHotSearchVideoRows(@Param("searchId") Long searchId,
                                                   @Param("keyword") String keyword,
                                                   @Param("tags") Collection<String> tags,
                                                   @Param("likeCountMin") Long likeCountMin,
                                                   @Param("commentCountMin") Long commentCountMin,
                                                   @Param("shareCountMin") Long shareCountMin,
                                                   @Param("collectCountMin") Long collectCountMin,
                                                   @Param("durationMin") Integer durationMin,
                                                   @Param("durationMax") Integer durationMax,
                                                   @Param("publishStartTime") String publishStartTime,
                                                   @Param("publishEndTime") String publishEndTime,
                                                   @Param("authorIds") Collection<String> authorIds,
                                                   @Param("sortField") Integer sortField,
                                                   @Param("keysetDesc") boolean keysetDesc,
                                                   @Param("lastSortVal") String lastSortVal,
                                                   @Param("lastVideoId") Long lastVideoId,
                                                   @Param("limit") int limit);

    /**
     * 取单达人在时间窗内的作品互动明细列（供服务端聚合统计概览，见 doc17 §6.20）。
     *
     * <p>只投影统计所需列（不取 video_url/cover 等重字段），单达人 + 时间窗 + LIMIT 兜底，内存里算派生指标，
     * 避免宽表 JOIN + GROUP BY、也避免前端拉全量。始终直读公共爬取库：租户口径（scope=1）由调用方先校验
     * 「该达人是否被本租户订阅」，订阅即可见该达人全部作品，因此本查询不再关联 tb_video_user_video。</p>
     *
     * @param authorId         达人 id 字符串（= tb_video_info.author_id）
     * @param platformType     平台类型（可空）
     * @param publishStartTime 发布时间-起（yyyy-MM-dd HH:mm:ss，可空）
     * @param publishEndTime   发布时间-止（可空）
     * @param limit            条数兜底上限（防单达人海量作品打爆内存）
     * @return 作品互动明细行
     */
    List<ClipMetricRow> listAuthorClipMetrics(@Param("authorId") String authorId,
                                              @Param("platformType") Byte platformType,
                                              @Param("publishStartTime") String publishStartTime,
                                              @Param("publishEndTime") String publishEndTime,
                                              @Param("limit") int limit);

    /**
     * 取某爆款关键词下、时间窗内互动量最高的前 N 条作品明细（供服务端聚合选题拆解，见接口文档 §6.21）。
     *
     * <p>只投影统计所需列（不取 video_url/cover 等重字段），按 engagement = 赞+评+享+藏 降序 + LIMIT 兜底，
     * 内存里算派生指标。爆款库为公共爬取数据、无 tenant_id，本查询<b>不做租户裁剪</b>。</p>
     *
     * <p>排序表达式无索引可用，必然 filesort；成本由 {@code search_id} 过滤后的行数决定 ——
     * 依赖 tb_video_hot_search_video 上的 search_id 索引（见 docs/ai-agent-search-optimization-backlog.md P0）。</p>
     *
     * @param searchId         爆款搜索 id
     * @param platformType     平台类型（可空）
     * @param publishStartTime 发布时间-起（yyyy-MM-dd HH:mm:ss，可空）
     * @param publishEndTime   发布时间-止（可空）
     * @param limit            条数兜底上限（命中即视为样本被截断）
     * @return 作品明细行（按互动量降序）
     */
    List<HotSearchClipRow> listHotSearchClipMetrics(@Param("searchId") Long searchId,
                                                    @Param("platformType") Byte platformType,
                                                    @Param("publishStartTime") String publishStartTime,
                                                    @Param("publishEndTime") String publishEndTime,
                                                    @Param("limit") int limit);

    /**
     * 统计本租户在给定视频集合中「文案提取成功」的条数（tb_video_user_video.extract_status=3）。
     *
     * <p>与 {@link #listTenantExtractedVideoIds} 的区别：只回 COUNT，不把 id 列表拉回内存 ——
     * 大样本（上万条）下只需要计数时用本方法。调用方负责分批传入，避免超长 IN。</p>
     *
     * @param tenantId 租户 id
     * @param videoIds 视频 id 集合（调用方分批传入）
     * @return 本租户提取成功的视频数（去重）
     */
    Long countTenantExtractedVideoIds(@Param("tenantId") Long tenantId,
                                      @Param("videoIds") Collection<Long> videoIds);

    /**
     * 爆款选题拆解作品明细行（§6.21 专用投影，比 {@link ClipMetricRow} 多达人与平台维度）
     */
    @Data
    class HotSearchClipRow {
        private Long videoId;
        private String title;
        private String authorId;
        private String authorName;
        private Byte platformType;
        /**
         * 以下 influencerXxx 来自 tb_video_hot_search_video 的冗余达人列 —— 跟着爆款视频一起同步，
         * 对上榜账号必然有值；而 tb_video_influencer_info 只由达人订阅链路填充，爆款账号多数查不到。
         */
        private String influencerNickname;
        private String influencerAvatar;
        private Long influencerFollowersCount;
        private String influencerPlatformUserId;
        private Byte influencerPlatformType;
        private Long likeCount;
        private Long commentCount;
        private Long shareCount;
        private Long collectCount;
        private Integer duration;
        private LocalDateTime publishTime;
    }

    /**
     * 单达人作品互动明细行（统计概览专用投影）
     */
    @Data
    class ClipMetricRow {
        private Long videoId;
        private String title;
        private Long likeCount;
        private Long commentCount;
        private Long shareCount;
        private Long collectCount;
        private Integer duration;
        private LocalDateTime publishTime;
    }

    /**
     * 爆款视频游标分页行（video_id + 排序值字符串）
     */
    @Data
    class HotSearchVideoRow {
        private Long videoId;
        private String sortVal;
    }


    /**
     * 视频数量查询参数
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class VideoCountQuery {
        private Long searchId;
        private Integer likeCount;
    }

    /**
     * 视频数量查询结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class VideoCountResult {
        private Long searchId;
        private Integer likeCount;
        private Integer videoCount;
    }

    /**
     * 达人视频数量查询结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class AuthorVideoCountResult {
        private String authorId;
        private Integer videoCount;
    }

    /**
     * 达人视频互动统计结果（租户已采集维度）
     */
    @Data
    class AuthorVideoStatsResult {
        private String authorId;
        private Integer videoCount;
        private Long totalLikeCount;
        private Long totalCommentCount;
        private Long totalShareCount;
        private Long totalCollectCount;
    }

    /**
     * 视频采集时间行（租户维度）
     */
    @Data
    class VideoCollectTimeRow {
        private Long videoId;
        private java.time.LocalDateTime collectTime;
    }

    /**
     * 视频 id 与 video_hash 映射行
     */
    @Data
    class VideoHashRow {
        private Long id;
        private String videoHash;
    }
}
