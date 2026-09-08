package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 全库短视频搜索入参（跨租户读全局爬取库 tb_video_info，keyset 游标流式分页）。
 *
 * <p>面向"对标/参考"类场景：按达人、标题/描述关键词、标题内 #标签、互动指标、发布时间等过滤，可按指标排序。
 * 该库为公共爬取数据、无 tenant_id，不涉及租户私有层。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoGlobalSearchBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 上一页返回的 nextCursor（不透明令牌）；首页传 null
     */
    @Size(max = 512, message = "cursor 非法")
    private String cursor;

    /**
     * 每页条数，范围 1~200，默认 50
     */
    private Integer pageSize;

    /**
     * 达人 id 集合（= tb_video_info.author_id），支持 in（最多 500）；不传=全库
     */
    @Size(max = 500, message = "influencerIdList 最多 500 个")
    private List<Long> influencerIdList;

    /**
     * 平台用户 id 集合（= 直播间 secUid），支持 in（最多 500 个）；按达人 platform_user_id 反查其短视频。
     * 与 influencerIdList / platformAccountList 之间 AND 收窄（取交集）
     */
    @Size(max = 500, message = "platformUserIdList 最多 500 个")
    private List<String> platformUserIdList;

    /**
     * 平台账号集合（抖音号/快手号/视频号），支持 in（最多 500 个）；按达人 platform_account 反查其短视频。
     * 与 influencerIdList / platformUserIdList 之间 AND 收窄（取交集）
     */
    @Size(max = 500, message = "platformAccountList 最多 500 个")
    private List<String> platformAccountList;

    /**
     * 平台类型集合（1-抖音 2-快手 3-视频号），支持 in
     */
    private List<Byte> platformTypeList;

    /**
     * 标题/描述关键词（模糊匹配）
     */
    @Size(max = 100, message = "keyword 长度不能超过 100")
    private String keyword;

    /**
     * 标签集合，匹配标题内 #标签（title LIKE '%#标签%'，任一命中即可，最多 20 个）
     */
    @Size(max = 20, message = "tagList 最多 20 个")
    private List<String> tagList;

    /**
     * 点赞数下限（>=）
     */
    private Long likeCountMin;

    /**
     * 评论数下限（>=）
     */
    private Long commentCountMin;

    /**
     * 分享数下限（>=）
     */
    private Long shareCountMin;

    /**
     * 收藏数下限（>=）
     */
    private Long collectCountMin;

    /**
     * 时长下限（秒，>=）
     */
    private Integer durationMin;

    /**
     * 时长上限（秒，<=）
     */
    private Integer durationMax;

    /**
     * 发布时间-起（yyyy-MM-dd HH:mm:ss）
     */
    private String publishStartTime;

    /**
     * 发布时间-止（yyyy-MM-dd HH:mm:ss）
     */
    private String publishEndTime;

    /**
     * 文案提取状态集合（0-未提取 1-已提取），支持 in。
     *
     * <p>口径随 {@code collectedOnly} 切换：{@code false}（全库）按 tb_video_info.extract_status 原值过滤；
     * {@code true}（租户范围）按<b>本租户</b>是否提取成功（tb_video_user_video.extract_status=3）过滤，
     * 此时 0 和 1 都传 = 不过滤。</p>
     */
    private List<Byte> extractStatusList;

    /**
     * 排序字段：0-默认(id) 1-点赞 2-评论 3-分享 4-收藏 5-发布时间
     */
    private Integer sortField;

    /**
     * 是否降序，默认 true
     */
    private Boolean sortDesc = true;

    /**
     * 是否加载原文/AI 音频转文字，默认 false。
     *
     * <p>{@code collectedOnly=false}（全库爬取库）原样返回；{@code collectedOnly=true}（租户范围）
     * 仅返回该视频在本租户下已提取成功（tb_video_user_video.extract_status=3）的文案，未授权的为 null。</p>
     */
    private Boolean withAudioText = false;

    /**
     * 是否仅限本租户可见范围，默认 false。
     *
     * <p>false=全库爬取作品（跨租户公共库，不返回文案）；true=仅本租户<b>已订阅达人</b>
     * （tb_video_user_influencer_subscription）名下的全部作品 —— 订阅即可见该达人全部作品，
     * 与本租户是否提取过文案无关。keyword / 排序 / keyset 分页等全部照常复用。</p>
     */
    private Boolean collectedOnly = false;

    /**
     * 采集账号id - 由哪一个子账户订阅的达人（collectedOnly=true 时按订阅记录 user_id 收窄）
     */
    private Long collectAccountId;
}
