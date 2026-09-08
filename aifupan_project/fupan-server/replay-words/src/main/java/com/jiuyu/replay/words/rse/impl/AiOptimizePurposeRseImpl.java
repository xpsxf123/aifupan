package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.AiOptimizePurposeSaveBo;
import com.jiuyu.replay.words.bo.AiOptimizePurposeUpdateBo;
import com.jiuyu.replay.words.entity.AiOptimizePurposeEntity;
import com.jiuyu.replay.words.repository.service.AiOptimizePurposeService;
import com.jiuyu.replay.words.rse.AiOptimizePurposeRse;
import com.jiuyu.replay.generic.vo.words.AiOptimizePurposeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI优化目的RSE接口实现类
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Service
public class AiOptimizePurposeRseImpl implements AiOptimizePurposeRse {

    @Resource
    private AiOptimizePurposeService aiOptimizePurposeService;

    @Override
    public Long save(AiOptimizePurposeSaveBo saveBo) {
        AiOptimizePurposeEntity entity = BeanConvertUtils.convert(saveBo, AiOptimizePurposeEntity.class);
        entity.setId(SnowflakeManager.nextValue());
        entity.setCreateDate(new Date());
        entity.setUpdateDate(new Date());
        aiOptimizePurposeService.save(entity);
        return entity.getId();
    }

    @Override
    public boolean update(AiOptimizePurposeUpdateBo updateBo) {
        if (updateBo.getId() == null) {
            return false;
        }
        AiOptimizePurposeEntity entity = BeanConvertUtils.convert(updateBo, AiOptimizePurposeEntity.class);
        entity.setUpdateDate(new Date());
        return aiOptimizePurposeService.updateById(entity);
    }

    @Override
    public AiOptimizePurposeVo getBySourceId(String sourceId) {
        List<AiOptimizePurposeEntity> entityList = aiOptimizePurposeService.list(
                new LambdaQueryWrapper<AiOptimizePurposeEntity>()
                        .eq(AiOptimizePurposeEntity::getSourceId, sourceId)
        );
        if (entityList == null || entityList.isEmpty()) {
            return null;
        }
        return BeanConvertUtils.convert(entityList.get(0), AiOptimizePurposeVo.class);
    }

    @Override
    public Set<String> listExistSourceIds(List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return new HashSet<>();
        }
        List<AiOptimizePurposeEntity> entityList = aiOptimizePurposeService.list(
                new LambdaQueryWrapper<AiOptimizePurposeEntity>()
                        .in(AiOptimizePurposeEntity::getSourceId, sourceIds)
                        .select(AiOptimizePurposeEntity::getSourceId)
        );
        if (entityList == null || entityList.isEmpty()) {
            return new HashSet<>();
        }
        return entityList.stream()
                .map(AiOptimizePurposeEntity::getSourceId)
                .collect(Collectors.toSet());
    }
}
