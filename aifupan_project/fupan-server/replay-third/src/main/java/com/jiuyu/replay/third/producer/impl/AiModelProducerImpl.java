package com.jiuyu.replay.third.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import com.jiuyu.replay.third.bo.AiModelListBo;
import com.jiuyu.replay.third.entity.AiModelEntity;
import com.jiuyu.replay.third.producer.AiModelProducer;
import com.jiuyu.replay.third.repository.service.AiModelService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * AI模型配置表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Service
public class AiModelProducerImpl implements AiModelProducer {

    @Resource
    private AiModelService aiModelService;


    @Override
    public PageUtils<AiModelListVo> queryPage(AiModelListBo aiModelListBo) {
        QueryWrapper<AiModelEntity> wrapper = new QueryWrapper<>();

        wrapper.lambda()
                .eq(ObjectUtil.isNotEmpty(aiModelListBo.getResourceType()), AiModelEntity::getResourceType, aiModelListBo.getResourceType())
                .eq(ObjectUtil.isNotEmpty(aiModelListBo.getContextSize()), AiModelEntity::getContextSize, aiModelListBo.getContextSize())
                .eq(ObjectUtil.isNotEmpty(aiModelListBo.getUseType()), AiModelEntity::getUseType, aiModelListBo.getUseType())
                .like(ObjectUtil.isNotEmpty(aiModelListBo.getKeyword()), AiModelEntity::getModelName, aiModelListBo.getKeyword())
        ;

        IPage<AiModelEntity> iPage = aiModelService.page(new Query<AiModelEntity>().getPage(aiModelListBo.getPage(), aiModelListBo.getLimit()), wrapper);

        PageUtils<AiModelListVo> pageUtils = new PageUtils<>(aiModelListBo.getPage(), aiModelListBo.getLimit(), iPage);

        List<AiModelEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AiModelListVo> vos = records.stream().map(item -> {
                AiModelListVo aiModelVo = new AiModelListVo();
                BeanUtils.copyProperties(item, aiModelVo);
                return aiModelVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiModelInfoVo info(Long id) {

        AiModelEntity aiModelEntity = aiModelService.getById(id);
        if(aiModelEntity != null) {
            AiModelInfoVo aiModelInfoVo = new AiModelInfoVo();
            BeanUtils.copyProperties(aiModelEntity, aiModelInfoVo);
            return aiModelInfoVo;
        }

        return null;
    }

    /**
     * 新增AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
     public AiModelInfoVo save(AiModelBo aiModelBo) {

         AiModelEntity aiModelEntity = new AiModelEntity();
         BeanUtils.copyProperties(aiModelBo, aiModelEntity);
         aiModelEntity.setId(SnowflakeManager.nextValue());
         aiModelEntity.setCreateDate(new Date());
         aiModelEntity.setUpdateDate(new Date());

         aiModelService.save(aiModelEntity);

         AiModelInfoVo aiModelInfoVo = new AiModelInfoVo();
         BeanUtils.copyProperties(aiModelEntity, aiModelInfoVo);

         return aiModelInfoVo;
     }

    /**
     * 修改AI模型配置表
     * @param aiModelBo AI模型配置表对象
     * @return
     */
    public void update(AiModelBo aiModelBo) {

        AiModelEntity aiModelEntity = new AiModelEntity();
        BeanUtils.copyProperties(aiModelBo, aiModelEntity);
        aiModelEntity.setUpdateDate(new Date());

        aiModelService.updateById(aiModelEntity);
    }

    /**
     * 删除AI模型配置表
     * @param id AI模型配置表id
     * @return
     */
    public void deleteById(Long id) {

        aiModelService.removeById(id);
    }

    /**
     * 根据模型名称查询AI模型配置表
     * @param modelName
     * @return
     */
    @Override
    public AiModelInfoVo getByModelName(String modelName) {
        AiModelEntity aiModelEntity = aiModelService.getOne(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getModelName, modelName)
                .last("limit 1")
        );
        if(aiModelEntity != null) {
            AiModelInfoVo aiModelInfoVo = new AiModelInfoVo();
            BeanUtils.copyProperties(aiModelEntity, aiModelInfoVo);
            return aiModelInfoVo;
        }

        return null;
    }

    @Override
    public AiModelInfoVo getByCode(String code) {
        AiModelEntity aiModelEntity = aiModelService.getOne(new LambdaQueryWrapper<AiModelEntity>()
                .eq(AiModelEntity::getModelCode, code)
                .last("limit 1")
        );
        if(aiModelEntity != null) {
            AiModelInfoVo aiModelInfoVo = new AiModelInfoVo();
            BeanUtils.copyProperties(aiModelEntity, aiModelInfoVo);
            return aiModelInfoVo;
        }

        return null;
    }

    @Override
    public List<AiModelInfoVo> listByCodes(List<String> codes) {
        List<AiModelEntity> list = aiModelService.list(new LambdaQueryWrapper<AiModelEntity>()
                .in(AiModelEntity::getModelCode, codes)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, AiModelInfoVo.class);
        }
        return new ArrayList<>();
    }
}

