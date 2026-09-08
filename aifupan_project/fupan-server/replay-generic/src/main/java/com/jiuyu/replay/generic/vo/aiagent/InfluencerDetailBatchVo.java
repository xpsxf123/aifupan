package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量达人档案出参（见接口文档 §6.22）。
 *
 * <p>把「查到的」与「没查到的」分开返回 —— 爆款上榜账号不一定在爬取达人库里有档案
 * （爆款同步链路只把达人信息冗余写进 tb_video_hot_search_video，<b>不落 tb_video_influencer_info</b>），
 * 命中率天然低于 100%。调用方据 {@link #notFoundAuthorIds} 决定降级展示，不必靠比对数量猜。</p>
 *
 * @author fupan-server
 */
@Data
public class InfluencerDetailBatchVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 查到档案的达人（按入参顺序，去重后）。
     *
     * <p>租户维度字段（{@code industryId} / {@code industryName} / {@code lastCollectTime} / {@code accountType}）
     * 恒为 null —— 本接口读全库爬取达人库，不含租户订阅信息，口径同 §6.17。</p>
     */
    private List<InfluencerItemVo> list;

    /**
     * 没查到档案的 author_id（原样回显）：包含「非数字、解析不了」与「库里无此达人」两类。
     */
    private List<String> notFoundAuthorIds;
}
