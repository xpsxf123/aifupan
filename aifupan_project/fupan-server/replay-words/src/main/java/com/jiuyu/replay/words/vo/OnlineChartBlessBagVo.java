package com.jiuyu.replay.words.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "福袋信息简略版")
public class OnlineChartBlessBagVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 发放福袋时间
     */
    @Schema(description = "发放福袋时间")
    private String blessBagTime;

    /**
     * 福袋奖励
     */
    @Schema(description = "福袋奖励")
    private String blessBagReward;

    /**
     * 领取条件
     */
    @Schema(description = "领取条件")
    private String getCondition;
    /**
     * 参与人数
     */
    @Schema(description = "参与人数")
    private Integer candidateNum;


    @Schema(description = "相对时间戳（福袋startTime-视频start）")
    private Long relativeTime;


    /**
     * 抽奖开始时间
     */
    @Schema(description = "抽奖开始时间")
    private Long startTime;

    @Schema(description = "当前时间")
    private Long currentTime;

    /**
     * 抽奖结束时间
     */
    @Schema(description = "抽奖结束时间")
    private Long drawTime;

}
