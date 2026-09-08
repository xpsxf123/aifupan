package com.jiuyu.replay.power.producer.impl;

import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.entity.DeptUserEntity;
import com.jiuyu.replay.power.producer.DeptUserProducer;
import com.jiuyu.replay.power.repository.service.DeptUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 部门对应下的用户
 *
 * @author jxy
 * @date 2024-07-08
 */
@Service
public class DeptUserProducerImpl implements DeptUserProducer {

    @Resource
    private DeptUserService deptUserService;

    @Override
    public List<DeptUserEntity> listByDeptId(Long deptId) {
        return deptUserService.lambdaQuery()
                .eq(DeptUserEntity::getDeptId, deptId)
                .eq(DeptUserEntity::getIsDeleted, 0)
                .list();
    }

    @Override
    public List<DeptUserEntity> listByUserId(Long userId) {
        return deptUserService.lambdaQuery()
                .eq(DeptUserEntity::getUserId, userId)
                .eq(DeptUserEntity::getIsDeleted, 0)
                .list();
    }

    @Override
    public DeptUserEntity info(Long id) {
        return deptUserService.getById(id);
    }

    @Override
    public DeptUserEntity save(DeptUserEntity entity) {
        entity.setId(SnowflakeManager.nextValue());
        Date now = new Date();
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setIsDeleted(0);
        deptUserService.save(entity);
        return entity;
    }

    @Override
    public void update(DeptUserEntity entity) {
        entity.setUpdateDate(new Date());
        deptUserService.updateById(entity);
    }

    @Override
    public void deleteById(Long id) {
        DeptUserEntity entity = new DeptUserEntity();
        entity.setId(id);
        entity.setIsDeleted(1);
        entity.setUpdateDate(new Date());
        deptUserService.updateById(entity);
    }
}
