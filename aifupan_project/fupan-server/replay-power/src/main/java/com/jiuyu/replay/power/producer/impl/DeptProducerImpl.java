package com.jiuyu.replay.power.producer.impl;

import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.power.entity.DeptEntity;
import com.jiuyu.replay.power.producer.DeptProducer;
import com.jiuyu.replay.power.repository.service.DeptService;
import com.jiuyu.replay.power.vo.DeptInfoVo;
import com.jiuyu.replay.power.vo.DeptListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门表
 *
 * @author jxy
 * @date 2024-07-08
 */
@Service
public class DeptProducerImpl implements DeptProducer {

    @Resource
    private DeptService deptService;

    @Override
    public List<DeptListVo> list() {
        return deptService.lambdaQuery()
                .eq(DeptEntity::getIsDeleted, 0)
                .list()
                .stream()
                .map(entity -> {
                    DeptListVo vo = new DeptListVo();
                    BeanUtils.copyProperties(entity, vo);
                    return vo;
                })
                .toList();
    }

    @Override
    public List<DeptListVo> treeList() {
        List<DeptListVo> deptList = list();
        Map<Long, List<DeptListVo>> childrenMap = deptList.stream()
                .filter(vo -> vo.getParentId() != null && vo.getParentId() != 0L)
                .collect(Collectors.groupingBy(DeptListVo::getParentId));
        deptList.forEach(vo -> vo.setChildren(childrenMap.get(vo.getId())));
        return deptList.stream()
                .filter(vo -> vo.getParentId() == null || vo.getParentId() == 0L)
                .toList();
    }

    @Override
    public DeptInfoVo info(Long id) {
        DeptEntity entity = deptService.getById(id);
        if (entity == null) {
            return null;
        }
        DeptInfoVo vo = new DeptInfoVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public DeptInfoVo save(DeptEntity entity) {
        entity.setId(SnowflakeManager.nextValue());
        Date now = new Date();
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setIsDeleted(0);
        deptService.save(entity);
        DeptInfoVo vo = new DeptInfoVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public void update(DeptEntity entity) {
        entity.setUpdateDate(new Date());
        deptService.updateById(entity);
    }

    @Override
    public void deleteById(Long id) {
        DeptEntity entity = new DeptEntity();
        entity.setId(id);
        entity.setIsDeleted(1);
        entity.setUpdateDate(new Date());
        deptService.updateById(entity);
    }
}
