package com.jiuyu.replay.system.producer.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.system.bo.DictTypeBo;
import com.jiuyu.replay.system.bo.DictTypeListBo;
import com.jiuyu.replay.system.entity.DictTypeEntity;
import com.jiuyu.replay.system.producer.DictTypeProducer;
import com.jiuyu.replay.system.repository.service.DictTypeService;
import com.jiuyu.replay.system.vo.DictTypeInfoVo;
import com.jiuyu.replay.system.vo.DictTypeListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 字典类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Service
public class DictTypeProducerImpl implements DictTypeProducer {

    @Resource
    private DictTypeService dictTypeService;


    @Override
    public PageUtils<DictTypeListVo> queryPage(DictTypeListBo dictTypeListBo) {
        LambdaQueryWrapper<DictTypeEntity> wrapper = new LambdaQueryWrapper<DictTypeEntity>()
                .and(ObjUtil.isNotEmpty(dictTypeListBo.getKeyword()), e ->
                        e.like(DictTypeEntity::getName, dictTypeListBo.getKeyword()).or().like(DictTypeEntity::getLogo, dictTypeListBo.getKeyword()));
        IPage<DictTypeEntity> iPage = dictTypeService.page(new Query<DictTypeEntity>().getPage(dictTypeListBo.getPage(), dictTypeListBo.getLimit()), wrapper);

        PageUtils<DictTypeListVo> pageUtils = new PageUtils<>(dictTypeListBo.getPage(), dictTypeListBo.getLimit(), iPage);

        List<DictTypeEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<DictTypeListVo> vos = records.stream().map(item -> {
                DictTypeListVo dictTypeVo = new DictTypeListVo();
                BeanUtils.copyProperties(item, dictTypeVo);
                return dictTypeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public DictTypeInfoVo info(Long id) {

        DictTypeEntity dictTypeEntity = dictTypeService.getById(id);
        if(dictTypeEntity != null) {
            DictTypeInfoVo dictTypeInfoVo = new DictTypeInfoVo();
            BeanUtils.copyProperties(dictTypeEntity, dictTypeInfoVo);
            return dictTypeInfoVo;
        }

        return null;
    }

    /**
     * 新增字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
     public DictTypeInfoVo save(DictTypeBo dictTypeBo) {

         DictTypeEntity dictTypeEntity = new DictTypeEntity();
         BeanUtils.copyProperties(dictTypeBo, dictTypeEntity);
         dictTypeEntity.setId(SnowflakeManager.nextValue());
         dictTypeEntity.setCreateDate(new Date());
         dictTypeEntity.setUpdateDate(new Date());

         dictTypeService.save(dictTypeEntity);

         DictTypeInfoVo dictTypeInfoVo = new DictTypeInfoVo();
         BeanUtils.copyProperties(dictTypeEntity, dictTypeInfoVo);

         return dictTypeInfoVo;
     }

    /**
     * 修改字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    public void update(DictTypeBo dictTypeBo) {

        DictTypeEntity dictTypeEntity = new DictTypeEntity();
        BeanUtils.copyProperties(dictTypeBo, dictTypeEntity);
        dictTypeEntity.setUpdateDate(new Date());

        dictTypeService.updateById(dictTypeEntity);
    }

    /**
     * 删除字典类型
     * @param id 字典类型id
     * @return
     */
    public void deleteById(Long id) {

        dictTypeService.removeById(id);
    }


}

