package com.jiuyu.replay.generic.vo.reward;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "客户端用户邀请奖励记录奖励详情")
public class ClientUserRewardRecordDetailVo {

    /**
     * 奖励类型
     */
    @Schema(description = "奖励类型")
    private String rewardType;
    /**
     * 奖励单位
     */
    @Schema(description = "奖励单位")
    private String rewardUnit;
    /**
     * 奖励数量
     */
    @Schema(description = "奖励数量")
    private Long rewardNumber;
}
