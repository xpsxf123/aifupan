package com.jiuyu.replay.order.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OrderTimeVo {

    /**
     * 符合条件查询的用户
     */
    @Schema(description = "userIdsByExpireTime")
    List<Long> userIdsByExpireTime;

    /**
     * 用户对应的订单有效时间
     */
    @Schema(description = "userOrderTime")
    Map<Long,Long> userOrderTime;
}
