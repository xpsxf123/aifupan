package com.jiuyu.replay.order.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.order.entity.UserPropertyTypeEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户资产类型总明细
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
public interface UserPropertyTypeService extends IService<UserPropertyTypeEntity> {


    List<UserPropertyTypeEntity> statisticsProperty(Long id);

    void updateCurrent(Long id);

    List<UserPropertyTypeEntity> listByIsUse();

    /**
     * 设置ParentId字段的值
     * @param commodityTypeCode
     */
    void setPropertyParentId(String commodityTypeCode);

    /**
     * 设置ParentId字段的值为0
     * @param commodityTypeCode
     */
    void setPropertyParentIdToNull(String commodityTypeCode);


    /**
     * 清除用户的私有资产
     * @param userPropertyIdMap  用户id > 资产id
     * @param code 资产code
     */
    void clearUserPrivateProperty(Map<Long, Long> userPropertyIdMap, String code);


    /**
     * 获取用户剩余资产
     *
     * @param userIds       用户ID
     * @param commodityCode 资产类型
     *
     * @return {@link Map }<{@link Long }, {@link Long }> key 用户ID value 剩余资产
     */
    Map<Long, Long> getUserPropertyRemainingMap(Collection<Long> userIds, String commodityCode);
}

