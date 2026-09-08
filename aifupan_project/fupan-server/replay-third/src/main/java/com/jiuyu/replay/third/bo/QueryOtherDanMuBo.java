package com.jiuyu.replay.third.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/18 下午7:21
 */
@Data
public class QueryOtherDanMuBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "每页记录数(默认1500)")
    private Integer limit = 1500;

    @Schema(description = "租户id-可以不传(不传用token的)")
    private Long tenantId;

    @Schema(description = "录制用户id-可以不传(不传用token的)")
    private Long userId;

    @Schema(description = "主播id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String secUid;

    @Schema(description = "直播场次号-用于不查询当前场次的标识")
    private String batchNumber;

    @Schema(description = "视频id-用于不查询当前视频的标识", requiredMode = Schema.RequiredMode.REQUIRED)
    private String videoId;

    @Schema(description = "记录时间戳-传当前视频的时间，用做范围查询", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long recordDate;

    @Schema(description = "弹幕用户昵称-等于", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickName;

    @Schema(description = "弹幕用户等级")
    private Long level;

    @Schema(description = "是否福袋弹幕, 0 否； 1 是")
    private Integer isBlessBag;
}
