package com.jiuyu.replay.generic.vo.reward;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(description = "客户端用户邀请奖励记录")
public class ClientUserRewardRecordVo {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String userNickName;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 使用类型
     */
    @Schema(description = "使用类型")
    private String progress;
    /**
     * 奖励详情列表
     */
    @Schema(description = "奖励详情列表")
    private List<ClientUserRewardRecordDetailVo> rewardDetailList;
    /**
     * 奖励发放时间
     */
    @Schema(description = "奖励发放时间")
    private Date rewardDate;
}
