package com.jiuyu.governance.openfeign.replay.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 视频列表请求（6.4 POST /internal/ai-agent/video/list）
 * <p>
 * 按条件筛选当前数据范围内的录制视频，游标分页。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-14
 */
@Getter
@Setter
public class VideoListRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 游标；首页传 null 或不传，后续传上一页返回的 nextCursor
     */
    private Long cursor;

    /**
     * 每页条数；不同接口默认值不同（列表默认 50，弹幕默认 200），详见各接口约束
     */
    private Integer pageSize;

    /**
     * 调用代表的用户 ID
     */
    private Long userId;

    /**
     * 用户所属租户（团队）ID，数据隔离边界
     */
    private Long tenantId;

    /**
     * 用户类型：0=普通用户(主账号) / 1=后台管理员 / 2=子账号
     * <p>
     * 数据范围由该字段决定：0、1 可见整个租户；2 仅可见本人（在租户内进一步按 userId 收敛）。
     * </p>
     */
    private Integer userType;

    /**
     * 视频唯一标识集合（最多 500 个）
     */
    private List<String> videoIds;


    /**
     * 批次编号集合（最多 500 个）
     */
    private List<String> batchNumbers;

    /**
     * 直播间 secUid 集合（最多 500）
     */
    private List<String> secUidList;

    /**
     * 直播间名称集合（最多 500）
     */
    private List<String> anchorNameList;

    /**
     * 录制开始时间范围-起，格式 yyyy-MM-dd HH:mm:ss
     */
    private String startDate;

    /**
     * 录制开始时间范围-止，格式 yyyy-MM-dd HH:mm:ss
     */
    private String endDate;

    /**
     * 归属类型：0=自有 1=同行
     */
    private Integer accountType;

    /**
     * 行业 ID（含子行业，规则同直播间列表）
     */
    private Long tradeId;

    /**
     * 视频切片类型：0=原视频 1=复盘切片视频 2=短视频切片视频
     */
    private Integer videoSliceType;


    /**
     * 提取直播稿完成：true=已完成 false=不限制
     */
    private Boolean analysisComplete = true;

    /**
     * 数据源类型： 不传默认 1
     * 1=自己录制
     * 3=云空间
     * 4=自己录制+云空间；
     */
    private Integer dataSourceType = 1;


    /**
     * 简单查询：true=只返回视频本身信息， false=视频完全统计口径信息
     */
    private Boolean simple = false;
}
