package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 主播列表项。
 *
 * @author fupan-server
 */
@Data
public class AnchorItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主播 ID
     */
    private Long id;

    /**
     * 主播唯一标识（后续详情/视频接口用它定位）
     */
    private String secUid;

    /**
     * 主播名称
     */
    private String anchorName;

    /**
     * 主播头像 URL
     */
    private String anchorAvatar;

    /**
     * 平台类型 code
     */
    private Integer platform;

    /**
     * 平台类型文案（抖音/快手/视频号）
     */
    private String platformLabel;

    /**
     * 主播账号
     */
    private String anchorNumber;

    /**
     * 行业 ID
     */
    private Long tradeId;

    /**
     * 行业名称
     */
    private String tradeName;

    /**
     * 归属类型 code（0自有/1同行）
     */
    private Integer accountType;

    /**
     * 归属类型文案
     */
    private String accountTypeLabel;

    /**
     * 添加时间
     */
    private String addDate;

    /**
     * 最后开始录制时间
     */
    private String lastRecordTime;

    /**
     * 是否开启弹幕监控
     */
    private Boolean enableBarrageMonitoring;

    /**
     * 是否开启数据看板
     */
    private Boolean enableDataViewing;

    /**
     * 是否开启自动分析
     */
    private Boolean enableAutoAnalysis;

    /**
     * 用户ID
     */
    private Long userId;
}
