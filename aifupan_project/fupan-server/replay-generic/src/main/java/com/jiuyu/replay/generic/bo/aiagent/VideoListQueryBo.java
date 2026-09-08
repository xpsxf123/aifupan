package com.jiuyu.replay.generic.bo.aiagent;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 视频列表查询入参（多条件过滤 + 游标分页）。
 *
 * @author fupan-server
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoListQueryBo extends AiAgentBaseBo {

    private static final long serialVersionUID = 1L;


    /**
     * 视频唯一标识集合（最多 500 个）
     */
    private List<String> videoIds;


    /**
     * 批次编号集合（最多 500 个）
     */
    private List<String> batchNumbers;

    /**
     * 上一页返回的 nextCursor；首页传 null
     */
    private Long cursor;

    /**
     * 每页条数，范围 1~200，默认 50
     */
    private Integer pageSize;

    /**
     * 主播 secUid 集合（最多 500 个）
     */
    @Size(max = 500, message = "secUidList 最多 500 个")
    private List<String> secUidList;

    /**
     * 主播名称集合（服务端内部转换为 secUid 过滤，最多 500 个）
     */
    @Size(max = 500, message = "anchorNameList 最多 500 个")
    private List<String> anchorNameList;

    /**
     * 录制开始时间-起（yyyy-MM-dd HH:mm:ss）
     */
    private String startDate;

    /**
     * 录制开始时间-止
     */
    private String endDate;

    /**
     * 是否自有：0=自有 1=同行；不传=全部
     */
    private Integer accountType;

    /**
     * 行业 ID（支持父行业展开）
     */
    private Long tradeId;

    /**
     * 视频切片类型：0=原视频 1=复盘切片视频 2=短视频切片视频
     */
    private Integer videoSliceType;

    /**
     * 数据源类型：1=自己录制（userId+tenantId）2=全租户（仅 tenantId，仅 userType=0 生效，子账号自动回落为 1）
     * 3=云空间（已分享，uploadStatus=1，全租户可见）4=自己录制+云空间（两者并集）；不传默认 1
     */
    private Integer dataSourceType = 1;


    /**
     * 是否分析完成：true=已完成 false=未完成
     */
    private Boolean analysisComplete = true;


    /**
     * 简单查询：true=只返回视频本身信息， false=视频完全统计口径信息
     */
    private Boolean simple = false;
}
