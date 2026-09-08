package com.jiuyu.replay.words.vo.oceanEngineData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 巨量统计数据视图对象
 *
 * @author liaoxin
 * @date 2025-01-27
 */
@Data
@Schema(description = "巨量统计数据视图对象")
public class JuliangStatisticsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频开始时间 自然时间戳
     */
    @Schema(description = "视频开始时间 自然时间戳")
    private Long startVideoTime;

    /**
     * 成交件数最小值
     */
    @Schema(description = "成交件数最小值")
    private Integer payComboCntMin;

    /**
     * 成交件数最大值
     */
    @Schema(description = "成交件数最大值")
    private Integer payComboCntMax;

    /**
     * 一段时间内的成交件数
     */
    @Schema(description = "一段时间内的成交件数")
    private Integer rangePayComboCnt;

    /**
     * 成交金额最小值，单位：分
     */
    @Schema(description = "成交金额最小值，单位：分")
    private Integer payAmtMin;

    /**
     * 成交金额最大值，单位：分
     */
    @Schema(description = "成交金额最大值，单位：分")
    private Integer payAmtMax;

    /**
     * 一段时间内成交金额，单位：分
     */
    @Schema(description = "一段时间内成交金额，单位：分")
    private Integer rangePayAmt;

    /**
     * 新增直播团人数最小值
     */
    @Schema(description = "新增直播团人数最小值")
    private Integer fansClubJoinUcntMin;

    /**
     * 新增直播团人数最大值
     */
    @Schema(description = "新增直播团人数最大值")
    private Integer fansClubJoinUcntMax;

    /**
     * 一段时间内新增直播团人数
     */
    @Schema(description = "一段时间内新增直播团人数")
    private Integer rangeFansClubJoinUcnt;

    /**
     * 新增粉丝数最小值
     */
    @Schema(description = "新增粉丝数最小值")
    private Integer followAnchorUcntMin;

    /**
     * 新增粉丝数最大值
     */
    @Schema(description = "新增粉丝数最大值")
    private Integer followAnchorUcntMax;

    /**
     * 一段时间内新增粉丝数
     */
    @Schema(description = "一段时间内新增粉丝数")
    private Integer rangeFollowAnchorUcnt;
}