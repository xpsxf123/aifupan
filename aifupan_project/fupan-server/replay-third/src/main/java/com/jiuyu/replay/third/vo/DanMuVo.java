package com.jiuyu.replay.third.vo;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/17 下午8:21
 */

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 弹幕数据
 */
@Data
@Schema(description = "弹幕返回详细参数")
public class DanMuVo implements Serializable {
    private static final long serialVersionUID = 1L;

//    @Schema(description = "租户id")
//    private Long tenantId;
//
//    @Schema(description = "主播id")
//    private String secUid;
//
//    @Schema(description = "消息id")
//    private String msgId;
//
//    @Schema(description = "录制用户id")
//    private Long userId;
//
//    @Schema(description = "视频id")
//    private String videoId;

    @Schema(description = "记录时间戳")
    private Long recordDate;

    @Schema(description = "直播场次号")
    private String batchNumber;

    @Schema(description = "弹幕用户昵称")
    private String nickName;

    @Schema(description = "是否新用户")
    private Boolean isNew;

    @Schema(description = "弹幕用户等级")
    private Long level;

    @Schema(description = "场次中最小粉丝团等级")
    private Long fansLevelMin;

    @Schema(description = "场次中最大粉丝团等级")
    private Long fansLevelMax;

    @Schema(description = "当前粉丝团等级")
    private Long fansLevelCurrent;

    @Schema(description = "弹幕内容")
    private String content;

    @Schema(description = "弹幕发送次数")
    private Integer countSendNum;

    @Schema(description = "弹幕排序")
    private Long sort;

    @Schema(description = "在之前是否显示升级提示")
    private Boolean showUpgradeTips = false;

    @Schema(description = "是否脱敏")
    private Boolean isDesensitization;

    @Schema(description = "是否是福袋弹幕字段")
    private Boolean isBlessBag;
}