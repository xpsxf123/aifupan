package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 相似主播信息VO
 *
 * @author RayChou
 * @date 2025-10-27
 * @description 相似主播信息返回对象
 */
@Data
@Schema(description = "相似主播信息VO")
public class SimilarAnchorVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 相似度
     */
    @Schema(description = "相似度")
    private String similarScore;

    /**
     * 平均UV价值
     */
    @Schema(description = "平均UV价值")
    private String liveAverageUv;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数")
    private String followerCount;

    /**
     * 平均场观
     */
    @Schema(description = "平均场观")
    private String liveAverageUser;

    /**
     * 场均销售额
     */
    @Schema(description = "场均销售额")
    private String liveAverageAmount;

    /**
     * 直播销售总额
     */
    @Schema(description = "直播销售总额")
    private String totalAmount;

    /**
     * 销售指数
     */
    @Schema(description = "销售指数")
    private String liveTotalAmountCmmInd;

    /**
     * 直播场次
     */
    @Schema(description = "直播场次")
    private Integer liveCount;

    /**
     * 平均停留时长
     */
    @Schema(description = "平均停留时长")
    private String liveAverageOnline;

    /**
     * 蝉妈妈唯一账号ID
     */
    @Schema(description = "蝉妈妈唯一账号ID")
    private String authorId;

    /**
     * 抖音账号
     */
    @Schema(description = "抖音账号")
    private String uniqueId;

    /**
     * 抖音唯一账号ID
     */
    @Schema(description = "抖音唯一账号ID")
    private String secUid;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;
}

