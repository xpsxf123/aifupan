package com.jiuyu.replay.system.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.bo.DictDataBo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.system.entity.DictDataEntity;
import com.jiuyu.replay.system.entity.DictTypeEntity;
import com.jiuyu.replay.system.producer.DictDataProducer;
import com.jiuyu.replay.system.repository.service.DictDataService;
import com.jiuyu.replay.system.repository.service.DictTypeService;
import com.jiuyu.replay.system.vo.DictDataInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Service
public class DictDataProducerImpl implements DictDataProducer {

    @Resource
    private DictDataService dictDataService;
    @Resource
    private DictTypeService dictTypeService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public PageUtils<DictDataListVo> queryPage(DictDataListBo dictDataListBo) {
        LambdaQueryWrapper<DictDataEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ObjectUtil.isNotEmpty(dictDataListBo.getKeyword()), DictDataEntity::getLabel, dictDataListBo.getKeyword())
                .eq(ObjectUtil.isNotEmpty(dictDataListBo.getTypeId()), DictDataEntity::getTypeId, dictDataListBo.getTypeId())
                .eq(ObjectUtil.isNotEmpty(dictDataListBo.getValue()), DictDataEntity::getValue, dictDataListBo.getValue())
                .eq(ObjectUtil.isNotEmpty(dictDataListBo.getLabel()), DictDataEntity::getLabel, dictDataListBo.getLabel())
                .eq(StringUtils.isEmpty(dictDataListBo.getIsAll()) || dictDataListBo.getIsAll() == 0, DictDataEntity::getStatus, 0)
        ;

        if (!StringUtils.isEmpty(dictDataListBo.getTypeLogo())) {
            DictTypeEntity dictTypeEntity = this.dictTypeService.getOne(new LambdaQueryWrapper<DictTypeEntity>()
                    .eq(DictTypeEntity::getLogo, dictDataListBo.getTypeLogo()));
            if (dictTypeEntity != null) {
                wrapper.eq(DictDataEntity::getTypeId, dictTypeEntity.getId());
            }else{
                PageUtils<DictDataListVo> utils = new PageUtils<>();
                utils.setTotalPage(0);
                utils.setTotalCount(0);
                return utils;
            }
        }
        wrapper.orderByAsc(DictDataEntity::getTypeId, DictDataEntity::getSort);

        IPage<DictDataEntity> iPage = this.dictDataService.page(new Query<DictDataEntity>().getPage(dictDataListBo.getPage(), dictDataListBo.getLimit(), null, true), wrapper);

        PageUtils<DictDataListVo> pageUtils = new PageUtils<>(dictDataListBo.getPage(), dictDataListBo.getLimit(), iPage);

        List<DictDataEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            Set<Long> typeIds = records.stream().map(DictDataEntity::getTypeId).collect(Collectors.toSet());
            List<DictTypeEntity> dictTypeEntities = dictTypeService.listByIds(typeIds);

            List<DictDataListVo> dictDataVos = records.stream().map(item -> {
                DictDataListVo dictDataVo = new DictDataListVo();
                BeanUtils.copyProperties(item, dictDataVo);
                for (DictTypeEntity dictTypeEntity : dictTypeEntities) {
                    if (dictTypeEntity.getId().equals(dictDataVo.getTypeId())) {
                        dictDataVo.setTypeName(dictTypeEntity.getName());
                        break;
                    }
                }
                return dictDataVo;
            }).collect(Collectors.toList());

            pageUtils.setList(dictDataVos);
        }

        return pageUtils;
    }

    @Override
    public DictDataInfoVo info(Long id) {

        DictDataEntity dictDataEntity = dictDataService.getById(id);
        if (dictDataEntity != null) {
            DictDataInfoVo dictDataInfoVo = new DictDataInfoVo();
            BeanUtils.copyProperties(dictDataEntity, dictDataInfoVo);
            return dictDataInfoVo;
        }

        return null;
    }

    /**
     * 新增字典
     *
     * @param dictDataBo 字典对象
     * @return
     */
    @Override
    public DictDataInfoVo save(DictDataBo dictDataBo) {

        // 删除缓存
        DictTypeEntity dictTypeEntity = this.dictTypeService.getById(dictDataBo.getTypeId());
        if(dictTypeEntity != null) {
            this.redisTemplate.delete("replay:dict:type-logo:" + dictTypeEntity.getLogo());
        }

        DictDataEntity dictDataEntity = new DictDataEntity();
        BeanUtils.copyProperties(dictDataBo, dictDataEntity);
        dictDataEntity.setId(SnowflakeManager.nextValue());
        dictDataEntity.setCreateDate(new Date());
        dictDataEntity.setUpdateDate(new Date());

        dictDataService.save(dictDataEntity);

        DictDataInfoVo dictDataInfoVo = new DictDataInfoVo();
        BeanUtils.copyProperties(dictDataEntity, dictDataInfoVo);

        return dictDataInfoVo;
    }

    /**
     * 修改字典
     *
     * @param dictDataBo 字典对象
     * @return
     */
    @Override
    public void update(DictDataBo dictDataBo) {

        // 删除新字典类型缓存
        if(dictDataBo.getTypeId() != null) {
            DictTypeEntity dictTypeEntity = this.dictTypeService.getById(dictDataBo.getTypeId());
            if(dictTypeEntity != null) {
                this.redisTemplate.delete("replay:dict:type-logo:" + dictTypeEntity.getLogo());
            }
        }
        // 删除旧字典类型缓存
        DictDataEntity oldDictDataEntity = this.dictDataService.getById(dictDataBo.getId());
        if(oldDictDataEntity != null) {
            DictTypeEntity dictTypeEntity = this.dictTypeService.getById(oldDictDataEntity.getTypeId());
            if(dictTypeEntity != null) {
                this.redisTemplate.delete("replay:dict:type-logo:" + dictTypeEntity.getLogo());
            }
        }

        DictDataEntity dictDataEntity = new DictDataEntity();
        BeanUtils.copyProperties(dictDataBo, dictDataEntity);
        dictDataEntity.setUpdateDate(new Date());

        dictDataService.updateById(dictDataEntity);


    }

    /**
     * 删除字典
     *
     * @param id 字典id
     * @return
     */
    @Override
    public void deleteById(Long id) {

        DictDataEntity dictDataEntity = this.dictDataService.getById(id);
        if(dictDataEntity != null) {
            DictTypeEntity dictTypeEntity = this.dictTypeService.getById(dictDataEntity.getTypeId());
            if(dictTypeEntity != null) {
                this.redisTemplate.delete("replay:dict:type-logo:" + dictTypeEntity.getLogo());
            }
        }

        dictDataService.removeById(id);
    }

    @Override
    public List<DictDataListVo> listByTypeLogo(String typeLogo) {

        String redisKey = "replay:dict:type-logo:" + typeLogo;
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return JSON.parseArray((String) obj, DictDataListVo.class);
        }

        DictTypeEntity dictTypeEntity = this.dictTypeService.getOne(new LambdaQueryWrapper<DictTypeEntity>()
                .eq(DictTypeEntity::getLogo, typeLogo));
        if(dictTypeEntity != null) {
            List<DictDataEntity> dictDataEntities = this.dictDataService.list(new LambdaQueryWrapper<DictDataEntity>()
                    .eq(DictDataEntity::getTypeId, dictTypeEntity.getId())
                    .orderByAsc(DictDataEntity::getSort)
            );
            if(dictDataEntities != null && dictDataEntities.size() > 0) {
                List<DictDataListVo> dictDataListVos = dictDataEntities.stream().map(item -> {
                    DictDataListVo dictDataListVo = new DictDataListVo();
                    BeanUtils.copyProperties(item, dictDataListVo);
                    return dictDataListVo;
                }).collect(Collectors.toList());

                // 存到缓存
                this.redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(dictDataListVos), Duration.ofDays(5));

                return dictDataListVos;
            }
        }


        return null;
    }

    @Override
    public List<DictDataListVo> listDictDataTree(Long typeId) {
        List<DictDataEntity> list = dictDataService.lambdaQuery()
                .eq(ObjectUtil.isNotEmpty(typeId), DictDataEntity::getTypeId, typeId)
                .orderByAsc(DictDataEntity::getSort)
                .list();
        if (list != null && !list.isEmpty()) {
            List<DictDataListVo> dictDataListVos = BeanUtil.copyToList(list, DictDataListVo.class);
            Map<Long, String> map = dictTypeService.lambdaQuery()
                    .in(DictTypeEntity::getId, dictDataListVos.stream().map(DictDataListVo::getTypeId).distinct().collect(Collectors.toList()))
                    .list().stream()
                    .collect(Collectors.toMap(DictTypeEntity::getId, DictTypeEntity::getName, (a, b) -> a));
            dictDataListVos.forEach(item -> {
                if (item.getTypeId() != null) {
                    item.setTypeName(map.get(item.getTypeId()));
                }
            });
            return dictDataListVos;
        }
        return List.of();
    }

    @Override
    public List<DictDataListVo> dictDataListByIds(List<Long> ids) {
        return dictDataService.lambdaQuery()
                .in(DictDataEntity::getId, ids)
                .list()
                .stream()
                .map(item -> BeanUtil.copyProperties(item, DictDataListVo.class))
                .collect(Collectors.toList());
    }
}

