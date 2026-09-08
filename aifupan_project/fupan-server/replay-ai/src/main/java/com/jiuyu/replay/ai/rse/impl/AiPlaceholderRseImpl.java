package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.ai.bo.AiPlaceholderBo;
import com.jiuyu.replay.ai.bo.AiPlaceholderListBo;
import com.jiuyu.replay.ai.entity.AiPlaceholderEntity;
import com.jiuyu.replay.ai.repository.service.AiPlaceholderService;
import com.jiuyu.replay.ai.rse.AiPlaceholderRse;
import com.jiuyu.replay.ai.vo.AiPlaceholderVo;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * AI占位符配置 Rse 实现
 *
 * @author jy
 * @date 2026-06-16
 */
@Component
@AllArgsConstructor
public class AiPlaceholderRseImpl implements AiPlaceholderRse {

    private final AiPlaceholderService aiPlaceholderService;

    /**
     * 分页查询，支持关键词模糊匹配 name/placeholderKey，按 sort 升序
     */
    @Override
    public PageUtils<AiPlaceholderVo> queryPage(AiPlaceholderListBo listBo) {
        LambdaQueryWrapper<AiPlaceholderEntity> wrapper = new LambdaQueryWrapper<AiPlaceholderEntity>()
                .and(ObjectUtil.isNotEmpty(listBo.getKeyword()), w -> w
                        .like(AiPlaceholderEntity::getName, listBo.getKeyword())
                        .or()
                        .like(AiPlaceholderEntity::getPlaceholderKey, listBo.getKeyword()))
                .eq(ObjUtil.isNotEmpty(listBo.getCategory()), AiPlaceholderEntity::getCategory, listBo.getCategory())
                .eq(ObjUtil.isNotEmpty(listBo.getIsFrontendShow()), AiPlaceholderEntity::getIsFrontendShow, listBo.getIsFrontendShow())
                .eq(ObjUtil.isNotEmpty(listBo.getStatus()), AiPlaceholderEntity::getStatus, listBo.getStatus())
                .orderByAsc(AiPlaceholderEntity::getSort);
        Page<AiPlaceholderEntity> page = aiPlaceholderService.page(new Page<>(listBo.getPage(), listBo.getLimit()), wrapper);

        List<AiPlaceholderVo> voList = BeanUtil.copyToList(page.getRecords(), AiPlaceholderVo.class);
        return new PageUtils<>(voList, (int) page.getTotal(), listBo.getPage(), listBo.getLimit());
    }

    /** 按 ID 查单个占位符，已删除返回 null */
    @Override
    public AiPlaceholderVo info(Long id) {
        AiPlaceholderEntity entity = aiPlaceholderService.getById(id);
        if (ObjectUtil.isEmpty(entity) || entity.getIsDeleted() == 1) {
            return null;
        }
        return BeanUtil.copyProperties(entity, AiPlaceholderVo.class);
    }

    /** 新增占位符：Snowflake 生成 ID、手工设时间戳/isDeleted/sort，返回 VO */
    @Override
    public AiPlaceholderVo save(AiPlaceholderBo bo) {
        Date now = new Date();
        AiPlaceholderEntity entity = BeanUtil.copyProperties(bo, AiPlaceholderEntity.class);
        entity.setId(SnowflakeManager.nextValue());
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setIsDeleted(0);
        if (entity.getSort() == null) {
            entity.setSort(0);
        }
        aiPlaceholderService.save(entity);
        return BeanUtil.copyProperties(entity, AiPlaceholderVo.class);
    }

    /** 按 ID 更新占位符，自动刷新 updateDate */
    @Override
    public void update(AiPlaceholderBo bo) {
        Date now = new Date();
        AiPlaceholderEntity entity = BeanUtil.copyProperties(bo, AiPlaceholderEntity.class);
        entity.setUpdateDate(now);
        aiPlaceholderService.updateById(entity);
    }

    /** 按 ID 软删除占位符 */
    @Override
    public void deleteById(Long id) {
        aiPlaceholderService.removeById(id);
    }

    /** 客户端获取占位符配置列表：前端展示、未删除、已启用，按 sort 升序 */
    @Override
    public List<AiPlaceholderVo> listForClient() {
        LambdaQueryWrapper<AiPlaceholderEntity> wrapper = new LambdaQueryWrapper<AiPlaceholderEntity>()
                .eq(AiPlaceholderEntity::getIsFrontendShow, 1)
                .eq(AiPlaceholderEntity::getIsDeleted, 0)
                .eq(AiPlaceholderEntity::getStatus, 0)
                .orderByAsc(AiPlaceholderEntity::getSort);
        List<AiPlaceholderEntity> list = aiPlaceholderService.list(wrapper);
        return BeanUtil.copyToList(list, AiPlaceholderVo.class);
    }

    /** 查全部已启用（status=0）的占位符，按 sort 升序 */
    @Override
    public List<AiPlaceholderVo> listAll() {
        LambdaQueryWrapper<AiPlaceholderEntity> wrapper = new LambdaQueryWrapper<AiPlaceholderEntity>()
                .eq(AiPlaceholderEntity::getStatus, 0)
                .orderByAsc(AiPlaceholderEntity::getSort);
        List<AiPlaceholderEntity> list = aiPlaceholderService.list(wrapper);
        return BeanUtil.copyToList(list, AiPlaceholderVo.class);
    }

    /**
     * 按完整 key 查单个已启用未删除的占位符
     */
    @Override
    public AiPlaceholderVo getByKey(String fullKey) {
        LambdaQueryWrapper<AiPlaceholderEntity> wrapper = new LambdaQueryWrapper<AiPlaceholderEntity>()
                .eq(AiPlaceholderEntity::getPlaceholderKey, fullKey)
                .eq(AiPlaceholderEntity::getStatus, 0)
                .eq(AiPlaceholderEntity::getIsDeleted, 0);
        AiPlaceholderEntity entity = aiPlaceholderService.getOne(wrapper);
        if (entity == null) return null;
        return BeanUtil.copyProperties(entity, AiPlaceholderVo.class);
    }

    /**
     * 批量按完整 key 查已启用未删除的占位符，用于 Resolver 的两轮匹配
     */
    @Override
    public List<AiPlaceholderVo> listByKeys(List<String> fullKeys) {
        if (CollUtil.isEmpty(fullKeys)) return List.of();
        LambdaQueryWrapper<AiPlaceholderEntity> wrapper = new LambdaQueryWrapper<AiPlaceholderEntity>()
                .in(AiPlaceholderEntity::getPlaceholderKey, fullKeys)
                .eq(AiPlaceholderEntity::getStatus, 0)
                .eq(AiPlaceholderEntity::getIsDeleted, 0);
        List<AiPlaceholderEntity> list = aiPlaceholderService.list(wrapper);
        return BeanUtil.copyToList(list, AiPlaceholderVo.class);
    }
}
