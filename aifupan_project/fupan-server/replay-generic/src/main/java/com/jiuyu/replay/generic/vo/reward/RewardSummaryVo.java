package com.jiuyu.replay.generic.vo.reward;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "奖励汇总")
public class RewardSummaryVo {

    /**
     * 奖励label
     */
    @Schema(description = "奖励label")
    private String rewardLabel;
    /**
     * 奖励的数量
     */
    @Schema(description = "奖励的数量")
    private Long rewardNum;
    /**
     * 奖励的单位
     */
    @Schema(description = "奖励的单位")
    private String rewardUnit;
}
