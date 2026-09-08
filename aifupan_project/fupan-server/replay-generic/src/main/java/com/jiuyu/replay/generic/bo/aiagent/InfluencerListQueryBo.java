package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 达人列表查询入参（租户达人订阅维度 + 游标分页）。
 *
 * <p>达人范围 = 当前租户「达人订阅」（tb_video_user_influencer_subscription）；
 * <b>只按租户过滤、不过滤用户</b>。所有过滤条件均可选，命中即叠加。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfluencerListQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 上一页返回的 nextCursor（达人 id）；首页传 null
     */
    private Long cursor;

    /**
     * 每页条数，范围 1~200，默认 50
     */
    private Integer pageSize;

    /**
     * 平台类型集合（1-抖音 2-快手 3-视频号），支持 in；不传=全部
     */
    private List<Byte> platformTypeList;

    /**
     * 达人昵称 / 抖音号 模糊匹配关键词
     */
    private String keyword;

    /**
     * 达人 id 集合，支持 in（最多 500 个）；用于按已知 id 精确捞取
     */
    @Size(max = 500, message = "influencerIdList 最多 500 个")
    private List<Long> influencerIdList;

    /**
     * 采集时间-起（yyyy-MM-dd HH:mm:ss，>=）；按达人最后同步时间 last_sync_time 过滤
     */
    private String collectStartTime;

    /**
     * 采集时间-止（yyyy-MM-dd HH:mm:ss，<=）
     */
    private String collectEndTime;


    /**
     * 行业 id（来自当前租户对该达人的订阅 tb_video_user_influencer_subscription.industry_id）
     */
    private Long industryId;

    /**
     * 采集账号id - 由哪一个子账户采集的视频
     */
    private Long collectAccountId;
}
