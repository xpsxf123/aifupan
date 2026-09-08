package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 达人短视频统计项（仅租户已采集视频维度）。
 *
 * @author fupan-server
 */
@Data
public class InfluencerVideoStatsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 达人 id
     */
    private Long influencerId;

    /**
     * 已采集视频数
     */
    private Integer videoCount;

    /**
     * 点赞数汇总
     */
    private Long totalLikeCount;

    /**
     * 评论数汇总
     */
    private Long totalCommentCount;

    /**
     * 分享数汇总
     */
    private Long totalShareCount;

    /**
     * 收藏数汇总
     */
    private Long totalCollectCount;
}
