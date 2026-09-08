package com.jiuyu.replay.order.rse;

import com.jiuyu.replay.generic.dto.order.UserResourceConsumptionDto;

import java.util.List;

/**
 * 用户资产消费记录RSE接口
 *
 * @author AI Assistant
 */
public interface UserPropertyDetailsRse {

    /**
     * 获取用户列表的昨日资源消耗统计
     *
     * @param userIds 用户ID列表
     * @param parentUserId 父id
     * @return 用户资源消耗统计列表
     */
    List<UserResourceConsumptionDto> getYesterdayResourceConsumption(List<Long> userIds, Long parentUserId);

    /**
     * 获取用户列表的本月资源消耗统计
     *
     * @param userIds 用户ID列表
     * @param parentUserId 父id
     * @return 用户资源消耗统计列表
     */
    List<UserResourceConsumptionDto> getMonthlyResourceConsumption(List<Long> userIds, Long parentUserId);
}
