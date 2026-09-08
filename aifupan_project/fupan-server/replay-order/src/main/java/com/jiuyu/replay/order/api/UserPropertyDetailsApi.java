package com.jiuyu.replay.order.api;

import com.jiuyu.replay.generic.dto.order.UserResourceConsumptionDto;
import com.jiuyu.replay.generic.feign.order.UserPropertyDetailsFeign;
import com.jiuyu.replay.order.rse.UserPropertyDetailsRse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户资产消费记录API实现
 *
 * @author AI Assistant
 */
@Service
@AllArgsConstructor
public class UserPropertyDetailsApi implements UserPropertyDetailsFeign {

    private final UserPropertyDetailsRse userPropertyDetailsRse;

    @Override
    public List<UserResourceConsumptionDto> getYesterdayResourceConsumption(List<Long> userIds, Long parentUserId) {
        return userPropertyDetailsRse.getYesterdayResourceConsumption(userIds, parentUserId);
    }

    @Override
    public List<UserResourceConsumptionDto> getMonthlyResourceConsumption(List<Long> userIds, Long parentUserId) {
        return userPropertyDetailsRse.getMonthlyResourceConsumption(userIds, parentUserId);
    }
}
