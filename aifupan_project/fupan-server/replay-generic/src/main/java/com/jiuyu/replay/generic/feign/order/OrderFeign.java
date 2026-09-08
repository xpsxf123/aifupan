package com.jiuyu.replay.generic.feign.order;

import com.jiuyu.replay.generic.dto.activity.InviteUserRewardDetailDto;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;

import java.util.List;

public interface OrderFeign {

    /**
     * 获取用户订单信息
     * @param userId
     * @return
     */
    OrderInfoVo currentOrderByUserId(Long userId);

    /**
     * 批量获取用户订单信息
     *
     * @param userId
     * @return
     */
    List<OrderInfoVo> currentOrderByUserIds(List<Long> userId);

    /**
     * 批量添加活动订单
     * @param dtoList
     * @return
     */
    List<InviteUserRewardDetailDto> addActivityOrder(List<InviteUserRewardDetailDto> dtoList);

}
