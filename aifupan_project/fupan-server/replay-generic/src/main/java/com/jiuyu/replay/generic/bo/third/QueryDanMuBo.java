package com.jiuyu.replay.generic.bo.third;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/16 下午3:49
 */
@Schema(description = "弹幕搜索条件")
@Data
public class QueryDanMuBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "每页记录数(默认50)，queryType为0时向上查询limit条，为1时向下查询limit条，为2时上下总查询 limit * 2 条; ", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer limit = 50;

    @Schema(description = "页数(默认1)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page = 1;

    @Schema(description = "查询类型(默认1)，0向上查询； 1向下查询； 2上下都查询，上下查询必须要传recordDate参数； 3昵称查询，不分页； 4搜索查询 向下搜索 ", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer queryType = 1;

    @Schema(description = "租户id-可以不传(不传用token的)")
    private Long tenantId;

    @Schema(description = "录制用户id-可以不传(不传用token的)")
    private Long userId;

    @Schema(description = "直播场次号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String batchNumber;

    @Schema(description = "视频id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String videoId;

    @Schema(description = "记录时间戳", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long recordDate;

    @Schema(description = "开始时间")
    private Long startTime;

    @Schema(description = "结束时间")
    private Long endTime;

    @Schema(description = "弹幕内容-包含")
    private String contentLike;

    @Schema(description = "弹幕用户昵称-包含")
    private String nickNameLike;

    @Schema(description = "弹幕用户昵称-等于")
    private String nickName;

    @Schema(description = "用户等级--小于")
    private Long minLevel;

    @Schema(description = "用户等级--等于")
    private Long level;

    @Schema(description = "用户等级--大于等于")
    private Long maxLevel;

    @Schema(description = "粉丝团等级--小于等于")
    private Long minFansLevel;

    @Schema(description = "粉丝团等级--大于")
    private Long maxFansLevel;

    @Schema(description = "最初粉丝团等级--小于等于")
    private Long minInitFansLevel;

    @Schema(description = "最初粉丝团等级--大于")
    private Long maxInitFansLevel;

    @Schema(description = "最终粉丝团等级--小于等于")
    private Long minFinallyFansLevel;

    @Schema(description = "最终粉丝团等级--大于")
    private Long maxFinallyFansLevel;

    @Schema(description = "是否新用户, 0 否； 1 是")
    private Integer isNew;

    @Schema(description = "是否重要弹幕, 0 否； 1 是")
    private Integer important;

    @Schema(description = "是否福袋弹幕, 0 否； 1 是")
    private Integer isBlessBag;
}
