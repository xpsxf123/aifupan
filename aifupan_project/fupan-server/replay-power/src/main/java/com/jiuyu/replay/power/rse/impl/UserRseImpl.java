package com.jiuyu.replay.power.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.vo.power.SubUserListVo;
import com.jiuyu.replay.power.entity.UserBusinessEntity;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.repository.service.UserBusinessService;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.rse.UserRse;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserRseImpl implements UserRse {

    @Resource
    private UserService userService;
    @Resource
    private UserBusinessService userBusinessService;

    @Override
    public List<SubUserListVo> getSubUserListByUserId(Long userId) {

        List<UserEntity> userEntities = this.userService.list(new QueryWrapper<UserEntity>().eq("parent_id", userId));
        if(userEntities == null || userEntities.isEmpty()) {
            return null;
        }

        return BeanConvertUtils.convertList(userEntities, SubUserListVo.class);
    }

    @Override
    public void saveClientVersion(Long userId, String clientVersion) {
        if (userId == null || clientVersion == null) {
            return;
        }
        UserBusinessEntity business = userBusinessService.getById(userId);
        if (business == null) {
            business = new UserBusinessEntity();
            business.setUserId(userId);
            business.setCreateDate(new Date());
        }
        business.setClientVersion(clientVersion);
        business.setUpdateDate(new Date());
        userBusinessService.saveOrUpdate(business);
    }

    @Override
    public String getClientVersion(Long userId) {
        UserBusinessEntity business = userBusinessService.lambdaQuery()
                .eq(UserBusinessEntity::getUserId, userId)
                .last("limit 1")
                .one();
        if (business == null) {
            return OrderEnums.clientVersion.REPLAY.getCode();
        }
        return business.getClientVersion();
    }
}
