package com.jiuyu.replay.order.repository.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.order.entity.UserPropertyTypeEntity;
import com.jiuyu.replay.order.repository.dao.UserPropertyTypeDao;
import com.jiuyu.replay.order.repository.service.UserPropertyTypeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("userPropertyTypeService")
@AllArgsConstructor
public class UserPropertyTypeServiceImpl extends ServiceImpl<UserPropertyTypeDao, UserPropertyTypeEntity> implements UserPropertyTypeService {

    @Override
    public List<UserPropertyTypeEntity> statisticsProperty(Long propertyId) {
        return baseMapper.statisticsProperty(propertyId);
    }

    @Override
    public void updateCurrent(Long id) {
        this.baseMapper.updateCurrent(id, DateUtil.now());
    }

    @Override
    public List<UserPropertyTypeEntity> listByIsUse() {
        return this.baseMapper.listByIsUse();
    }

    @Override
    public void setPropertyParentId(String commodityTypeCode) {
        this.baseMapper.setPropertyParentId(commodityTypeCode);
    }

    @Override
    public void setPropertyParentIdToNull(String commodityTypeCode) {
        this.baseMapper.setPropertyParentIdToNull(commodityTypeCode);
    }


    /**
     * 清除用户的私有资产
     *
     * @param userPropertyIdMap 用户id > 资产id
     * @param code              资产code
     */
    @Override
    public void clearUserPrivateProperty(Map<Long, Long> userPropertyIdMap, String code) {
        if (EmptyUtil.isEmpty(userPropertyIdMap) || EmptyUtil.isEmpty(code)) {
            return;
        }
        List<UserPropertyTypeEntity> typeEntities = userPropertyIdMap.entrySet().stream().filter(entry -> entry.getValue() > 0).map(entry -> {
            UserPropertyTypeEntity entity = new UserPropertyTypeEntity();
            entity.setPropertyId(entry.getValue());
            entity.setCommodityTypeCode(code);
            entity.setUserId(entry.getKey());
            return entity;
        }).toList();
        if (EmptyUtil.isEmpty(typeEntities)) {
            return;
        }
        this.baseMapper.clearUserPrivateProperty(typeEntities);
    }


    /**
     * 获取用户剩余资产
     *
     * @param userIds       用户ID
     * @param commodityCode 资产类型
     *
     * @return {@link Map }<{@link Long }, {@link Long }> key 用户ID value 剩余资产
     */
    @Override
    public Map<Long, Long> getUserPropertyRemainingMap(Collection<Long> userIds, String commodityCode) {
        if (EmptyUtil.isEmpty(userIds) || EmptyUtil.isEmpty(commodityCode)) {
            return Map.of();
        }
        List<UserPropertyTypeEntity> entities = this.baseMapper.getUserPropertyRemainingMap(userIds, commodityCode);
        if (EmptyUtil.isEmpty(entities)) {
            return Map.of();
        }
        return entities.stream().collect(
                Collectors.toMap(UserPropertyTypeEntity::getUserId, UserPropertyTypeEntity::getTotalQuantity)
        );
    }
}