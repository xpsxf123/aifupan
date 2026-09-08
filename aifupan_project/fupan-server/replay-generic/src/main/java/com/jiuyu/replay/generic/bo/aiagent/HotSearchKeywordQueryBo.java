package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 爆款关键词列表入参（全库 tb_video_hot_search，keyset 游标流式分页）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HotSearchKeywordQueryBo extends AiAgentBaseBo {

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
     * 平台类型集合（1-抖音 2-快手 3-视频号），支持 in
     */
    private List<Byte> platformTypeList;

    /**
     * 关键词（模糊匹配 search_keyword）
     */
    @Size(max = 100, message = "keyword 长度不能超过 100")
    private String keyword;

    /**
     * 最后同步时间-起（yyyy-MM-dd HH:mm:ss，>=）
     */
    private String syncStartTime;

    /**
     * 最后同步时间-止（yyyy-MM-dd HH:mm:ss，<=）
     */
    private String syncEndTime;

    /**
     * 排序字段：0-视频数 1-最后同步时间
     */
    private Integer sortField;

    /**
     * 是否降序，默认 true
     */
    private Boolean sortDesc = true;

    /**
     * 是否只查当前租户（tenantId）订阅监控的爆款关键词：false-全库公共爆款词（默认）；true-仅本租户订阅词
     */
    private Boolean loadMe = false;

    /**
     * 行业 id（= tb_trade.id，与达人 / 主播域的行业 id 同一套主数据）；不传=不按行业收窄。
     *
     * <p><b>仅 {@code loadMe=true} 时可用</b> —— 行业维度来自租户订阅表 tb_video_user_hot_subscription.industry_id，
     * 全库爆款词表 tb_video_hot_search 没有这个字段。{@code loadMe!=true} 时传本参数直接报错，
     * 不做静默忽略（静默忽略会让调用方以为已按行业过滤，拿到的却是全量）。</p>
     */
    private Long industryId;
}
