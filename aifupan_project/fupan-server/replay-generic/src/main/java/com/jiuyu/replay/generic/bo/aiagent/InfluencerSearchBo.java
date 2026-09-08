package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 全库达人搜索入参（跨租户读全局爬取库 tb_video_influencer_info，keyset 游标流式分页）。
 *
 * <p>面向"找对标达人账号"场景：按昵称/账号关键词、平台、粉丝量过滤，可按粉丝/获赞排序。</p>
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfluencerSearchBo extends AiAgentBaseBo {

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
     * 昵称 / 平台账号 关键词（模糊匹配）
     */
    @Size(max = 100, message = "keyword 长度不能超过 100")
    private String keyword;

    /**
     * 粉丝数下限（>=）
     */
    private Long followersCountMin;

    /**
     * 排序字段：0-粉丝数 1-获赞数
     */
    private Integer sortField;

    /**
     * 是否降序，默认 true
     */
    private Boolean sortDesc = true;
}
