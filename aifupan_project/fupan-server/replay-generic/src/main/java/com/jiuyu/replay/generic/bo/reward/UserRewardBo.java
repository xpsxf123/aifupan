package com.jiuyu.replay.generic.bo.reward;

import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "客户端获取奖励列表请求对象")
public class UserRewardBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;


}
