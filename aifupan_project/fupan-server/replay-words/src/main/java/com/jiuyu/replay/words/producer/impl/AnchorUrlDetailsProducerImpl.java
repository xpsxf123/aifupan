package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import com.jiuyu.replay.words.producer.AnchorUrlDetailsProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlDetailsService;
import com.jiuyu.replay.words.vo.anchor.AnchorUrlDetailsVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 主播的附加表
 *
 * @author LJ
 * @date 2025-11-22
 */
@Service
public class AnchorUrlDetailsProducerImpl implements AnchorUrlDetailsProducer {

    @Resource
    private AnchorUrlDetailsService anchorUrlDetailsService;

    @Override
    public PageUtils<AnchorUrlDetailsEntity> queryPage(Integer page, Integer limit) {
        QueryWrapper<AnchorUrlDetailsEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_date");
        IPage<AnchorUrlDetailsEntity> iPage = anchorUrlDetailsService.page(
                new Query<AnchorUrlDetailsEntity>().getPage(page, limit), wrapper);
        return new PageUtils<>(page, limit, iPage);
    }

    @Override
    public AnchorUrlDetailsVo getBySecUid(String secUid) {
        AnchorUrlDetailsEntity entity = anchorUrlDetailsService.getOne(new LambdaQueryWrapper<AnchorUrlDetailsEntity>()
                .eq(AnchorUrlDetailsEntity::getSecUid, secUid)
        );
        if (entity == null) {
            return null;
        }
        return BeanUtil.copyProperties(entity, AnchorUrlDetailsVo.class);
    }

    @Override
    public AnchorUrlDetailsEntity save(AnchorUrlDetailsEntity entity) {
        if (ObjectUtil.isNotEmpty(entity)) {
            entity.setCreateDate(new Date());
            entity.setUpdateDate(new Date());
            anchorUrlDetailsService.save(entity);
        }
        return entity;
    }

    @Override
    public AnchorUrlDetailsEntity update(AnchorUrlDetailsEntity entity) {
        if (ObjectUtil.isNotEmpty(entity)) {
            entity.setUpdateDate(new Date());
            anchorUrlDetailsService.updateById(entity);
        }
        return entity;
    }

    @Override
    public AnchorUrlDetailsEntity saveOrUpdate(AnchorUrlDetailsEntity entity) {
        if (ObjectUtil.isNotEmpty(entity)) {
            entity.setUpdateDate(new Date());
            if (ObjectUtil.isEmpty(entity.getCreateDate())) {
                entity.setCreateDate(new Date());
            }
            anchorUrlDetailsService.saveOrUpdate(entity);
        }
        return entity;
    }

    @Override
    public void deleteBySecUid(String secUid) {
        anchorUrlDetailsService.removeById(secUid);
    }

    @Override
    public void deleteBySecUidList(List<String> secUidList) {
        if (ObjectUtil.isNotEmpty(secUidList)) {
            anchorUrlDetailsService.removeBatchByIds(secUidList);
        }
    }

    @Override
    public List<String> secUidListByKeywordStatus(Integer duration, Integer limit) {
        return anchorUrlDetailsService.secUidListByKeywordStatus(duration, limit);
    }
}

