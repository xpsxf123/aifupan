package com.jiuyu.replay.order.api;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.dto.activity.InviteUserRewardDetailDto;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/27 上午11:32
 */
@Service
@AllArgsConstructor
@Slf4j
public class OrderApi implements OrderFeign {

    private final OrderBll orderBll;
    private final UserPropertyBll userPropertyBll;
    private final UserFeign userFeign;

    @Override
    public OrderInfoVo currentOrderByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDto userDto = userFeign.userById(userId);
        if (ObjectUtil.isEmpty(userDto)) {
            return null;
        }
        R<OrderInfoVo> orderInfoVoR = orderBll.currentOrderByUserId(userDto.getParentId() != null && userDto.getParentId() > 0 ? userDto.getParentId() : userId);
        if (orderInfoVoR.getData() != null) {
            return orderInfoVoR.getData();
        }
        return null;
    }

    @Override
    public List<OrderInfoVo> currentOrderByUserIds(List<Long> userIds) {
        return orderBll.currentOrderByUserIds(userIds);
    }

    @Override
    public List<InviteUserRewardDetailDto> addActivityOrder(List<InviteUserRewardDetailDto> dtoList) {

        try {
            dtoList = orderBll.addActivityOrder(dtoList);
        } catch (Exception e) {
            log.error("addActivityOrder error", e);
            throw e;
        } finally {
            // 判断result中是否有orderId，如果没有
            if (ObjectUtil.isNotEmpty(dtoList)) {
                List<Long> userIds = dtoList.stream()
                        .filter(dto -> ObjectUtil.isEmpty(dto.getOrderId()) || dto.getOrderId() == 0)
                        .map(InviteUserRewardDetailDto::getUserId)
                        .distinct()
                        .toList();
                userPropertyBll.deleteUserPropertyCache(userIds);
            }
        }
        return dtoList;
    }
}
