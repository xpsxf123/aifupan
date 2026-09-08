package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.ai.bo.CustPromptBo;
import com.jiuyu.replay.ai.bo.CustPromptListBo;
import com.jiuyu.replay.ai.entity.CustPromptEntity;
import com.jiuyu.replay.ai.repository.service.CustPromptService;
import com.jiuyu.replay.ai.rse.CustPromptRse;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.CustPromptVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 用户自定义提示词 Rse 实现
 *
 * @author jxy
 * @date 2025-01-21
 */
@Component
public class CustPromptRseImpl implements CustPromptRse {

    @Resource
    private CustPromptService custPromptService;
    @Resource
    private UserFeign userFeign;
    @Resource
    private SystemKvService systemKvService;

    @Override
    public PageUtils<CustPromptVo> queryPage(CustPromptListBo listBo) {
        LambdaQueryWrapper<CustPromptEntity> wrapper = new LambdaQueryWrapper<CustPromptEntity>()
                .eq(ObjectUtil.isNotEmpty(listBo.getUserId()), CustPromptEntity::getUserId, listBo.getUserId())
                .like(ObjectUtil.isNotEmpty(listBo.getPromptTitle()), CustPromptEntity::getPromptTitle, listBo.getPromptTitle())
                .like(ObjectUtil.isNotEmpty(listBo.getPromptContent()), CustPromptEntity::getPromptContent, listBo.getPromptContent())
                .orderByAsc(CustPromptEntity::getPromptSort);
        Page<CustPromptEntity> page = custPromptService.page(new Page<>(listBo.getPage(), listBo.getLimit()), wrapper);

        List<CustPromptVo> voList = BeanUtil.copyToList(page.getRecords(), CustPromptVo.class);
        for (int i = 0; i < voList.size(); i++) {
            CustPromptEntity entity = page.getRecords().get(i);
            if (StrUtil.isNotBlank(entity.getPlaceholderKeys())) {
                voList.get(i).setPlaceholderKeys(Arrays.asList(entity.getPlaceholderKeys().split(",")));
            }
        }
        return new PageUtils<>(voList, (int) page.getTotal(), listBo.getPage(), listBo.getLimit());
    }

    @Override
    public CustPromptVo info(Long id) {
        CustPromptEntity entity = custPromptService.getById(id);
        if (ObjectUtil.isEmpty(entity) || entity.getIsDeleted() == 1) {
            return null;
        }
        CustPromptVo vo = BeanUtil.copyProperties(entity, CustPromptVo.class);
        if (StrUtil.isNotBlank(entity.getPlaceholderKeys())) {
            vo.setPlaceholderKeys(Arrays.asList(entity.getPlaceholderKeys().split(",")));
        }
        return vo;
    }

    @Override
    public CustPromptVo save(CustPromptBo custPromptBo) {
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        Date now = new Date();

        // 校验
        SystemKvEntity kv = systemKvService.getByKey("user_cust_prompt_num_max");
        if (kv != null && NumberUtil.parseInt(kv.getKvValue(), 0) > 0) {
            Long count = custPromptService.lambdaQuery()
                    .eq(CustPromptEntity::getUserId, user.getId())
                    .count();
            if (count >= NumberUtil.parseLong(kv.getKvValue(), 1L)) {
                throw new RuntimeException("自定义提示词数量已达到上限");
            }
        }

        CustPromptEntity entity = BeanUtil.copyProperties(custPromptBo, CustPromptEntity.class);
        if (CollUtil.isNotEmpty(custPromptBo.getPlaceholderKeys())) {
            entity.setPlaceholderKeys(String.join(",", custPromptBo.getPlaceholderKeys()));
        }
        entity.setId(SnowflakeManager.nextValue());
        entity.setUserId(user.getId());
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        entity.setIsDeleted(0);
        if (entity.getPromptSort() == null) {
            entity.setPromptSort(1);
        }

        custPromptService.save(entity);
        CustPromptVo vo = BeanUtil.copyProperties(entity, CustPromptVo.class);
        if (StrUtil.isNotBlank(entity.getPlaceholderKeys())) {
            vo.setPlaceholderKeys(Arrays.asList(entity.getPlaceholderKeys().split(",")));
        }
        return vo;
    }

    @Override
    public void update(CustPromptBo custPromptBo) {
        Date now = new Date();
        CustPromptEntity entity = BeanUtil.copyProperties(custPromptBo, CustPromptEntity.class);
        if (CollUtil.isNotEmpty(custPromptBo.getPlaceholderKeys())) {
            entity.setPlaceholderKeys(String.join(",", custPromptBo.getPlaceholderKeys()));
        }
        entity.setUpdateDate(now);
        if (entity.getPromptSort() == null) {
            entity.setPromptSort(1);
        }
        custPromptService.updateById(entity);
    }

    @Override
    public void deleteById(Long id) {
        custPromptService.removeById(id);
    }
}

