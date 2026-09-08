package com.jiuyu.replay.words.producer.impl;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.generic.bo.words.CruxTypeScaleBo;
import com.jiuyu.replay.words.vo.ModelCruxListVo;
import com.jiuyu.replay.words.vo.ModelCruxInfoVo;
import com.jiuyu.replay.words.bo.ModelCruxBo;
import com.jiuyu.replay.words.bo.ModelCruxListBo;
import com.jiuyu.replay.words.repository.service.ModelCruxService;
import com.jiuyu.replay.words.entity.ModelCruxEntity;
import com.jiuyu.replay.words.producer.ModelCruxProducer;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 模型-关键词类型-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Service
public class ModelCruxProducerImpl implements ModelCruxProducer {

    @Resource
    private ModelCruxService modelCruxService;


    @Override
    public PageUtils<ModelCruxListVo> queryPage(ModelCruxListBo modelCruxListBo) {
        QueryWrapper<ModelCruxEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(modelCruxListBo.getKeyword())){
            wrapper.like("name", modelCruxListBo.getKeyword());
        }

        IPage<ModelCruxEntity> iPage = modelCruxService.page(new Query<ModelCruxEntity>().getPage(modelCruxListBo.getPage(), modelCruxListBo.getLimit()), wrapper);

        PageUtils<ModelCruxListVo> pageUtils = new PageUtils<>(modelCruxListBo.getPage(), modelCruxListBo.getLimit(), iPage);

        List<ModelCruxEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ModelCruxListVo> vos = records.stream().map(item -> {
                ModelCruxListVo modelCruxVo = new ModelCruxListVo();
                BeanUtils.copyProperties(item, modelCruxVo);
                return modelCruxVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ModelCruxInfoVo info(Long id) {

        ModelCruxEntity modelCruxEntity = modelCruxService.getById(id);
        if(modelCruxEntity != null) {
            ModelCruxInfoVo modelCruxInfoVo = new ModelCruxInfoVo();
            BeanUtils.copyProperties(modelCruxEntity, modelCruxInfoVo);
            return modelCruxInfoVo;
        }

        return null;
    }

    /**
     * 新增模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
     public ModelCruxInfoVo save(ModelCruxBo modelCruxBo) {

         ModelCruxEntity modelCruxEntity = new ModelCruxEntity();
         BeanUtils.copyProperties(modelCruxBo, modelCruxEntity);
         modelCruxEntity.setId(SnowflakeManager.nextValue());
         modelCruxEntity.setCreateDate(new Date());
         modelCruxEntity.setUpdateDate(new Date());

         modelCruxService.save(modelCruxEntity);

         ModelCruxInfoVo modelCruxInfoVo = new ModelCruxInfoVo();
         BeanUtils.copyProperties(modelCruxEntity, modelCruxInfoVo);

         return modelCruxInfoVo;
     }

    /**
     * 修改模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
    public void update(ModelCruxBo modelCruxBo) {

        ModelCruxEntity modelCruxEntity = new ModelCruxEntity();
        BeanUtils.copyProperties(modelCruxBo, modelCruxEntity);
        modelCruxEntity.setUpdateDate(new Date());

        modelCruxService.updateById(modelCruxEntity);
    }

    /**
     * 删除模型-关键词类型-关联表
     * @param id 模型-关键词类型-关联表id
     * @return
     */
    public void deleteById(Long id) {

        modelCruxService.removeById(id);
    }

    @Override
    public void saveBatchModel(List<CruxTypeScaleBo> cruxTypeScaleBos, Long dataModelId) {

        List<ModelCruxEntity> modelCruxEntities = new ArrayList<>();
        for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
            CruxTypeScaleBo cruxTypeScaleBo = cruxTypeScaleBos.get(i);
            if (cruxTypeScaleBo != null){
                ModelCruxEntity modelCruxEntity = new ModelCruxEntity();
                modelCruxEntity.setId(SnowflakeManager.nextValue());
                modelCruxEntity.setModelId(dataModelId);
                modelCruxEntity.setCruxTypeId(cruxTypeScaleBo.getCruxTypeId());
                modelCruxEntity.setScale(cruxTypeScaleBo.getScale());
                modelCruxEntity.setCreateDate(new Date());
                modelCruxEntity.setUpdateDate(new Date());
                modelCruxEntities.add(modelCruxEntity);
            }
        }
        modelCruxService.saveBatch(modelCruxEntities);
    }

}

