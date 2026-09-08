package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 爆款视频列表入参（某爆款关键词下的短视频，tb_video_hot_search_video JOIN tb_video_info，keyset 游标流式分页）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HotSearchVideoQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 爆款搜索 id（tb_video_hot_search.id）
     */
    @NotNull(message = "searchId 不能为空")
    private Long searchId;

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
     * 平台用户 id 集合（= 直播间 secUid），支持 in（最多 500 个）；按达人 platform_user_id 反查其短视频。
     * 与 platformAccountList 之间 AND 收窄（取交集）
     */
    @Size(max = 500, message = "platformUserIdList 最多 500 个")
    private List<String> platformUserIdList;

    /**
     * 达人 id 集合（= tb_video_info.author_id 原值，与 §6.21 topAuthors[].authorId 同口径），最多 500 个。
     *
     * <p><b>直接对 author_id 过滤，不反查达人表</b> —— 与 platformUserIdList / platformAccountList 的区别在这：
     * 后两者要先经 tb_video_influencer_info 反查 id，而爆款上榜账号多数不在达人库里
     * （爆款同步链路不落该表），用它们查作品会返回空页。要「看某个上榜账号在本选题下的作品」请用本参数。</p>
     *
     * <p>与另外两个达人维度参数之间 AND 收窄（取交集）。</p>
     */
    @Size(max = 500, message = "authorIdList 最多 500 个")
    private List<String> authorIdList;

    /**
     * 平台账号集合（抖音号/快手号/视频号），支持 in（最多 500 个）；按达人 platform_account 反查其短视频。
     * 与 platformUserIdList 之间 AND 收窄（取交集）
     */
    @Size(max = 500, message = "platformAccountList 最多 500 个")
    private List<String> platformAccountList;

    /**
     * 标题/描述关键词（模糊匹配）
     */
    @Size(max = 100, message = "keyword 长度不能超过 100")
    private String keyword;

    /**
     * 标签集合，匹配标题内 #标签（最多 20 个）
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
     * 视频发布时间-起（yyyy-MM-dd HH:mm:ss，>=）
     */
    private String publishStartTime;

    /**
     * 视频发布时间-止（yyyy-MM-dd HH:mm:ss，<=）
     */
    private String publishEndTime;

    /**
     * 排序字段：0-默认(爆款内排序 sort_order) 1-点赞 2-评论 3-分享 4-收藏 5-发布时间
     */
    private Integer sortField;

    /**
     * 是否降序，默认 true
     */
    private Boolean sortDesc = true;

    /**
     * 是否加载原文/AI 音频转文字，默认 false
     */
    private Boolean withAudioText = false;

    /**
     * 提取状态是否走「本租户口径」，默认 false。
     *
     * <p>false（默认，模型工具口径）：{@code extractStatus} 取 tb_video_info 原值（全网是否提取过），文案不设授权闸。<br>
     * true（前端口径）：{@code extractStatus} 只在<b>本租户</b>提取成功时为 1，文案同样按提取授权放行 ——
     * 与 §6.12 / §6.15(collectedOnly) / §6.16 口径一致，保证前端「看文案」入口不是假入口。</p>
     *
     * <p>注意：true 时结果因租户而异，首页缓存键会并入 tenantId 分片。</p>
     */
    private Boolean tenantScopeExtract = false;
}
