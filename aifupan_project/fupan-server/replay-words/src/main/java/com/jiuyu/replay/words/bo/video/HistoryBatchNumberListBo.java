package com.jiuyu.replay.words.bo.video;

import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/10 下午4:04
 */
@Data
public class HistoryBatchNumberListBo {

    /**
     * 数据类型:0-截图,1-看板
     */
    private Integer dataType;

    /**
     * 分析状态 0：未分析 1：分析中 2：分析完成 3分析错误
     */
    private Integer analysisStatus;

    /**
     * 主播唯一Id
     */
    private String secUid;

    /**
     * 用户Id
     */
    private Long userId;

    /**
     * 租户Id
     */
    private Long tenantId;

    /**
     * 视频Id
     */
    private String videoId;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 视频上传状态 0：未上传 1：已上传
     */
    private Integer uploadStatus;

    /**
     * 条数
     */
    private Integer limit;
}
