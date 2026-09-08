package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.ai.bo.GlobalProblemBo;
import com.jiuyu.replay.ai.bo.GlobalProblemListBo;
import com.jiuyu.replay.ai.entity.GlobalProblemEntity;
import com.jiuyu.replay.ai.repository.service.GlobalProblemService;
import com.jiuyu.replay.ai.rse.GlobalProblemRse;
import com.jiuyu.replay.ai.vo.GlobalProblemListVo;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 全局提示词 Rse 实现
 *
 * @author lj
 * @date 2026-05-21
 */
@Component
public class GlobalProblemRseImpl implements GlobalProblemRse {

    @Resource
    private GlobalProblemService globalProblemService;

    @Override
    public PageUtils<GlobalProblemListVo> queryPage(GlobalProblemListBo listBo) {
        LambdaQueryWrapper<GlobalProblemEntity> wrapper = new LambdaQueryWrapper<GlobalProblemEntity>()
                .eq(ObjectUtil.isNotEmpty(listBo.getTradeId()), GlobalProblemEntity::getTradeId, listBo.getTradeId())
                .eq(ObjectUtil.isNotEmpty(listBo.getCueType()), GlobalProblemEntity::getCueType, listBo.getCueType())
                .eq(ObjectUtil.isNotEmpty(listBo.getApplyTo()), GlobalProblemEntity::getApplyTo, listBo.getApplyTo())
                .eq(ObjectUtil.isNotEmpty(listBo.getProblemType()), GlobalProblemEntity::getProblemType, listBo.getProblemType())
                .eq(GlobalProblemEntity::getIsDeleted, 0)
                .orderByDesc(GlobalProblemEntity::getCreateDate);

        Page<GlobalProblemEntity> page = globalProblemService.page(
                new Page<>(listBo.getPage(), listBo.getLimit()), wrapper);

        List<GlobalProblemListVo> voList = BeanUtil.copyToList(page.getRecords(), GlobalProblemListVo.class);
        return new PageUtils<>(voList, (int) page.getTotal(), listBo.getLimit(), listBo.getPage());
    }

    @Override
    public GlobalProblemListVo info(Long id) {
        GlobalProblemEntity entity = globalProblemService.getById(id);
        if (ObjectUtil.isEmpty(entity) || entity.getIsDeleted() == 1) {
            return null;
        }
        return BeanUtil.copyProperties(entity, GlobalProblemListVo.class);
    }

    @Override
    public GlobalProblemListVo save(GlobalProblemBo bo) {
        Date now = new Date();
        GlobalProblemEntity entity = BeanUtil.copyProperties(bo, GlobalProblemEntity.class);
        entity.setId(SnowflakeManager.nextValue());
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setIsDeleted(0);

        globalProblemService.save(entity);
        return BeanUtil.copyProperties(entity, GlobalProblemListVo.class);
    }

    @Override
    public void update(GlobalProblemBo bo) {
        Date now = new Date();
        GlobalProblemEntity entity = BeanUtil.copyProperties(bo, GlobalProblemEntity.class);
        entity.setUpdateDate(now);
        globalProblemService.updateById(entity);
    }

    @Override
    public void deleteById(Long id) {
        globalProblemService.update(new LambdaUpdateWrapper<GlobalProblemEntity>()
                .eq(GlobalProblemEntity::getId, id)
                .set(GlobalProblemEntity::getIsDeleted, 1)
                .set(GlobalProblemEntity::getUpdateDate, new Date()));
    }

    @Override
    public List<GlobalProblemListVo> listByConditions(Integer cueType, Integer applyTo, Integer problemType) {
        LambdaQueryWrapper<GlobalProblemEntity> wrapper = new LambdaQueryWrapper<GlobalProblemEntity>()
                .eq(GlobalProblemEntity::getIsDeleted, 0);
        if (cueType != null) {
            wrapper.eq(GlobalProblemEntity::getCueType, cueType);
        }
        if (applyTo != null) {
            wrapper.eq(GlobalProblemEntity::getApplyTo, applyTo);
        }
        if (problemType != null) {
            wrapper.eq(GlobalProblemEntity::getProblemType, problemType);
        }
        List<GlobalProblemEntity> list = globalProblemService.list(wrapper);
        return BeanUtil.copyToList(list, GlobalProblemListVo.class);
    }
}
