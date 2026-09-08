package com.jiuyu.replay.api.logic.ai.placeholder;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分钟段落数据 — 结构化缓存，供格式化输出 + 后续数值计算。
 *
 * @author jy
 * @date 2026-06-18
 */
@Data
public class MinuteSegmentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 段落序号（从 1 开始）
     */
    private int index;

    // —— 时间 ——

    /**
     * 视频相对开始时间（ms，距视频开始）
     */
    private Long startTempTime;
    /**
     * 视频相对结束时间（ms，距视频开始）
     */
    private Long endTempTime;
    /**
     * 自然开始时间戳（ms）
     */
    private Long startVideoTime;
    /**
     * 自然结束时间戳（ms）
     */
    private Long endVideoTime;
    /**
     * 自然开始时间
     */
    private Date startTime;
    /**
     * 自然结束时间
     */
    private Date endTime;
    /**
     * HH:mm:ss 格式开始时间
     */
    private String textStart;
    /**
     * HH:mm:ss 格式结束时间
     */
    private String textEnd;

    // —— 段落内容 ——

    /**
     * 段落转录文本
     */
    private String content;

    // —— 在线人数 ——

    /**
     * 本段在线人数
     */
    private String onlineNum;
    /**
     * 比上段在线人数变化值（绝对值）
     */
    private int onlineNumChange;
    /**
     * 比上段在线人数趋势："增加" 或 "减少"
     */
    private String onlineNumTrend;

    // —— 弹幕 ——

    /**
     * 本段弹幕条数
     */
    private int barrageNum;

    // —— 语速 ——

    /**
     * 语速（字/分钟），根据去掉标点后的内容长度 + 段落时长计算
     */
    private double speechSpeed;

    // —— 巨量数据 ——

    /**
     * 成交人数
     */
    private Integer dealCount;
    /**
     * 成交金额（元）
     */
    private Double sales;
    /**
     * 成交率（%），成交人数 / 在线人数 * 100
     */
    private Double dealRate;
    /**
     * 互动率（%），弹幕条数 / 在线人数 * 100
     */
    private Double interactionRate;
    /**
     * UV 价值，销售额 / 在线人数
     */
    private Double uvValue;

    // —— 巨量投放数据 ——

    /**
     * 投放消耗
     */
    private Double qianchuanCost;

    /**
     * 净成交ROI
     */
    private Double netTransactionRoi;
}
