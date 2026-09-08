package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 批量达人短视频统计入参（仅租户已采集视频维度）。
 *
 * <p>按达人 id 批量返回：已采集视频数 + 点赞/评论/分享/收藏汇总。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfluencerVideoStatsBatchBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;

    /**
     * 达人 id 集合，支持 in（1~500 个）
     */
    @NotEmpty(message = "influencerIdList 不能为空")
    @Size(max = 500, message = "influencerIdList 最多 500 个")
    private List<Long> influencerIdList;

    /**
     * 视频发布时间-起（yyyy-MM-dd HH:mm:ss，>=）；用于把统计限定在某发布时间段
     */
    private String publishStartTime;

    /**
     * 视频发布时间-止（yyyy-MM-dd HH:mm:ss，<=）
     */
    private String publishEndTime;

    /**
     * 是否仅限本租户可见范围，默认 false。
     *
     * <p>false=按全库爬取作品统计；true=仅统计本租户<b>已订阅达人</b>的作品（订阅即可见该达人全部作品），
     * 未订阅的达人各项统计恒为 0。</p>
     */
    private Boolean collectedOnly = false;
}
