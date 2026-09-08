package com.jiuyu.replay.generic.vo.third;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 弹幕条目返回 VO（跨模块传输精简版）。
 *
 * <p>供 {@link com.jiuyu.replay.generic.feign.third.TableStoreFeign#queryDanMuSearchData} 返回，
 * 字段为互动巡检切片分析所需子集，不含 replay-third 模块内部字段。</p>
 *
 * @author beta
 * @date 2026-06-04
 */
@Data
@Schema(description = "弹幕条目返回 VO（互动巡检切片数据源）")
public class DanMuItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 弹幕文本内容
     */
    @Schema(description = "弹幕文本内容")
    private String content;

    /**
     * 弹幕时间戳（毫秒，视频内偏移）
     */
    @Schema(description = "弹幕时间戳（毫秒，视频内偏移）")
    private Long recordDate;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String nickName;

    /**
     * 用户等级
     */
    @Schema(description = "用户等级")
    private Long level;

    /**
     * 是否新用户
     */
    @Schema(description = "是否新用户")
    private Boolean isNew;

    /**
     * 当前粉丝团等级
     */
    @Schema(description = "当前粉丝团等级")
    private Long fansLevelCurrent;
}
