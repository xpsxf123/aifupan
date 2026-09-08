package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.entity.SourceStarEntity;
import com.jiuyu.replay.words.repository.service.SourceStarService;
import com.jiuyu.replay.words.rse.SourceStarRse;
import com.jiuyu.replay.generic.vo.words.SourceStarVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 星标RSE接口实现类
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Service
public class SourceStarRseImpl implements SourceStarRse {

    @Resource
    private SourceStarService sourceStarService;

    @Override
    public Long save(String sourceId, Integer sourceType, Long userId, Long tenantId) {
        SourceStarEntity entity = new SourceStarEntity();
        entity.setId(SnowflakeManager.nextValue());
        entity.setSourceId(sourceId);
        entity.setSourceType(sourceType);
        entity.setUserId(userId);
        entity.setTenantId(tenantId);
        entity.setCreateDate(new Date());
        entity.setUpdateDate(new Date());
        sourceStarService.save(entity);
        return entity.getId();
    }

    @Override
    public boolean remove(String sourceId, Integer sourceType) {
        LambdaQueryWrapper<SourceStarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SourceStarEntity::getSourceId, sourceId)
                .eq(SourceStarEntity::getSourceType, sourceType);
        return sourceStarService.remove(wrapper);
    }

    @Override
    public SourceStarVo getBySourceIdAndType(String sourceId, Integer sourceType) {
        LambdaQueryWrapper<SourceStarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SourceStarEntity::getSourceId, sourceId)
                .eq(SourceStarEntity::getSourceType, sourceType)
                .last("LIMIT 1");
        SourceStarEntity entity = sourceStarService.getOne(wrapper);
        if (entity == null) {
            return null;
        }
        return BeanConvertUtils.convert(entity, SourceStarVo.class);
    }

    @Override
    public boolean existsStar(String sourceId, Integer sourceType) {
        LambdaQueryWrapper<SourceStarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SourceStarEntity::getSourceId, sourceId)
                .eq(SourceStarEntity::getSourceType, sourceType);
        return sourceStarService.count(wrapper) > 0;
    }

    @Override
    public Set<String> listExistSourceIds(List<String> sourceIds, Integer sourceType) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return new HashSet<>();
        }

        LambdaQueryWrapper<SourceStarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SourceStarEntity::getSourceId, sourceIds)
                .eq(SourceStarEntity::getSourceType, sourceType)
                .select(SourceStarEntity::getSourceId);
        List<SourceStarEntity> entityList = sourceStarService.list(wrapper);

        if (entityList == null || entityList.isEmpty()) {
            return new HashSet<>();
        }

        return entityList.stream()
                .map(SourceStarEntity::getSourceId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<SourceStarVo> listBySourceIds(List<String> sourceIds, Integer sourceType) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SourceStarEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SourceStarEntity::getSourceId, sourceIds)
                .eq(SourceStarEntity::getSourceType, sourceType);
        List<SourceStarEntity> entityList = sourceStarService.list(wrapper);

        if (entityList == null || entityList.isEmpty()) {
            return new ArrayList<>();
        }

        return entityList.stream().map(item -> {
            return BeanConvertUtils.convert(item, SourceStarVo.class);
        }).collect(Collectors.toList());
    }
}
