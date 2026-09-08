package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.bo.words.CruxTypeScaleBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.entity.CruxTypeEntity;
import com.jiuyu.replay.words.entity.DataModelEntity;
import com.jiuyu.replay.words.entity.ModelCruxEntity;
import com.jiuyu.replay.words.entity.TradeEntity;
import com.jiuyu.replay.words.producer.DataModelProducer;
import com.jiuyu.replay.words.repository.service.CruxTypeService;
import com.jiuyu.replay.words.repository.service.DataModelService;
import com.jiuyu.replay.words.repository.service.ModelCruxService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.vo.DataModelInfoVo;
import com.jiuyu.replay.words.vo.DataModelItemVo;
import com.jiuyu.replay.words.vo.DataModelListVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 罗盘数据模型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Service
public class DataModelProducerImpl implements DataModelProducer {

    @Resource
    private DataModelService dataModelService;
    @Resource
    private ModelCruxService modelCruxService;
    @Resource
    private CruxTypeService cruxTypeService;
    @Resource
    private TradeService tradeService;

    @Override
    public PageUtils<DataModelListVo> queryPage(DataModelListBo dataModelListBo) {
        QueryWrapper<DataModelEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(dataModelListBo.getKeyword())){
            wrapper.like("name", dataModelListBo.getKeyword());
        }

        IPage<DataModelEntity> iPage = dataModelService.page(new Query<DataModelEntity>().getPage(dataModelListBo.getPage(), dataModelListBo.getLimit()), wrapper);

        PageUtils<DataModelListVo> pageUtils = new PageUtils<>(dataModelListBo.getPage(), dataModelListBo.getLimit(), iPage);

        List<DataModelEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<DataModelListVo> vos = records.stream().map(item -> {
                DataModelListVo dataModelVo = new DataModelListVo();
                BeanUtils.copyProperties(item, dataModelVo);
                return dataModelVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public DataModelInfoVo info(Long id) {

        DataModelEntity dataModelEntity = dataModelService.getById(id);
        if(dataModelEntity != null) {
            DataModelInfoVo dataModelInfoVo = new DataModelInfoVo();
            BeanUtils.copyProperties(dataModelEntity, dataModelInfoVo);
            return dataModelInfoVo;
        }

        return null;
    }

    /**
     * 新增罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
     public DataModelInfoVo save(DataModelBo dataModelBo) {

         DataModelEntity dataModelEntity = new DataModelEntity();
         BeanUtils.copyProperties(dataModelBo, dataModelEntity);
         dataModelEntity.setId(SnowflakeManager.nextValue());
         dataModelEntity.setCreateDate(new Date());
         dataModelEntity.setUpdateDate(new Date());

         dataModelService.save(dataModelEntity);

         DataModelInfoVo dataModelInfoVo = new DataModelInfoVo();
         BeanUtils.copyProperties(dataModelEntity, dataModelInfoVo);

         return dataModelInfoVo;
     }

    /**
     * 修改罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    public void update(DataModelBo dataModelBo) {

        DataModelEntity dataModelEntity = new DataModelEntity();
        BeanUtils.copyProperties(dataModelBo, dataModelEntity);
        dataModelEntity.setUpdateDate(new Date());

        dataModelService.updateById(dataModelEntity);
    }

    /**
     * 删除罗盘数据模型
     * @param id 罗盘数据模型id
     * @return
     */
    public void deleteById(Long id) {

        dataModelService.removeById(id);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> saveDataModel(DataModelSaveBo dataModelBo) {

        // 遍历CruxTypeScaleBo，判断是否展示罗盘并添加一条数据进modelCrux
        List<CruxTypeScaleBo> cruxTypeScaleBos = dataModelBo.getCruxTypeScaleBos();
        double sum = 0;
        for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
            sum += cruxTypeScaleBos.get(i).getScale();
        }
        if (sum != 1){
            return R.error(4001,"相加数值不等于100%");
        }

        DataModelEntity dataModelEntity = new DataModelEntity();
        dataModelEntity.setId(SnowflakeManager.nextValue());
        dataModelEntity.setName(dataModelBo.getName());
        dataModelEntity.setType(dataModelBo.getType());
        dataModelEntity.setTradeId(dataModelBo.getTradeId());
        dataModelEntity.setCreateDate(new Date());
        dataModelEntity.setUpdateDate(new Date());
        dataModelService.save(dataModelEntity);

        List<ModelCruxEntity> modelCruxEntities = new ArrayList<>();
        for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
            CruxTypeScaleBo cruxTypeScaleBo = cruxTypeScaleBos.get(i);
            if (cruxTypeScaleBo != null){
                ModelCruxEntity modelCruxEntity = new ModelCruxEntity();
                modelCruxEntity.setId(SnowflakeManager.nextValue());
                modelCruxEntity.setModelId(dataModelEntity.getId());
                modelCruxEntity.setCruxTypeId(cruxTypeScaleBo.getCruxTypeId());
                modelCruxEntity.setScale(cruxTypeScaleBo.getScale());
                modelCruxEntity.setCreateDate(new Date());
                modelCruxEntity.setUpdateDate(new Date());
                modelCruxEntities.add(modelCruxEntity);
            }
        }
        modelCruxService.saveBatch(modelCruxEntities);

        return R.ok("添加成功");
    }

    @Override
    public R<PageUtils<DataModelSaveBo>> modelCruxTypeList(DataModelListBo dataModelBo) {

        QueryWrapper<DataModelEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(dataModelBo.getKeyword())){
            wrapper.like("name", dataModelBo.getKeyword());
        }

        wrapper.eq("type",dataModelBo.getType());

        IPage<DataModelEntity> iPage = dataModelService.page(new Query<DataModelEntity>().getPage(dataModelBo.getPage(), dataModelBo.getLimit()), wrapper);

        PageUtils<DataModelSaveBo> pageUtils = new PageUtils<>(dataModelBo.getPage(), dataModelBo.getLimit(), iPage);

        List<DataModelEntity> dataModelList = iPage.getRecords();
        if(dataModelList != null && dataModelList.size() > 0) {

            // 行业
            Set<Long> tradeIds = dataModelList.stream().map(DataModelEntity::getTradeId).collect(Collectors.toSet());
            List<TradeEntity> tradeEntities = this.tradeService.listByIds(tradeIds);

            // 关键词类型
            List<Long> modelIds = dataModelList.stream().map(DataModelEntity::getId).collect(Collectors.toList());
            List<ModelCruxEntity> modelCruxEntities = this.modelCruxService.list(new QueryWrapper<ModelCruxEntity>().in("model_id", modelIds));
            List<CruxTypeEntity> cruxTypeEntities = null;
            if(modelCruxEntities != null && modelCruxEntities.size() > 0) {
                List<Long> cruxTypeIds = modelCruxEntities.stream().map(ModelCruxEntity::getCruxTypeId).collect(Collectors.toList());
                cruxTypeEntities = this.cruxTypeService.listByIds(cruxTypeIds);
            }

            List<CruxTypeEntity> finalCruxTypeEntities = cruxTypeEntities;

            List<DataModelSaveBo> saveBoList = dataModelList.stream().map(item -> {
                DataModelSaveBo dataModelSaveBo = new DataModelSaveBo();
                BeanUtils.copyProperties(item, dataModelSaveBo);
                // 封装行业信息
                if (tradeEntities != null) {
                    for (TradeEntity tradeEntity : tradeEntities) {
                        if (tradeEntity.getId().equals(dataModelSaveBo.getTradeId())) {
                            dataModelSaveBo.setTradeName(tradeEntity.getName());
                            break;
                        }
                    }
                }
                List<CruxTypeScaleBo> cruxTypeScaleBos = new LinkedList<>();
                // 封装关键词类型信息
                if(modelCruxEntities != null && modelCruxEntities.size() > 0 && finalCruxTypeEntities != null && finalCruxTypeEntities.size() > 0) {
                    for (ModelCruxEntity modelCruxEntity : modelCruxEntities) {
                        if(modelCruxEntity.getModelId().equals(dataModelSaveBo.getId())) {
                            for (CruxTypeEntity finalCruxTypeEntity : finalCruxTypeEntities) {
                                if(finalCruxTypeEntity.getId().equals(modelCruxEntity.getCruxTypeId())) {
                                    CruxTypeScaleBo cruxTypeScaleBo = new CruxTypeScaleBo();
                                    cruxTypeScaleBo.setCruxTypeId(finalCruxTypeEntity.getId());
                                    cruxTypeScaleBo.setCruxTypeName(finalCruxTypeEntity.getName());
                                    cruxTypeScaleBo.setScale(modelCruxEntity.getScale());
                                    cruxTypeScaleBos.add(cruxTypeScaleBo);
                                }
                            }
                        }
                    }
                }
                dataModelSaveBo.setCruxTypeScaleBos(cruxTypeScaleBos);

                return dataModelSaveBo;
            }).collect(Collectors.toList());

            pageUtils.setList(saveBoList);
        }


        return R.ok(pageUtils);
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateDataModel(DataModelSaveBo dataModelBo) {

        List<CruxTypeScaleBo> cruxTypeScaleBos = dataModelBo.getCruxTypeScaleBos();
//        如果没有关键词ID带回，需要通过模型ID去查旗下关键词
        double sum = 0;
        for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
            sum += cruxTypeScaleBos.get(i).getScale();
        }
        if (sum != 1){
            return R.error(4001,"相加数值不等于100%");
        }

        DataModelEntity dataModelEntity = new DataModelEntity();
        BeanUtils.copyProperties(dataModelBo,dataModelEntity);
        dataModelEntity.setUpdateDate(new Date());
        dataModelService.updateById(dataModelEntity);

        // 删除关键词后再新增
        QueryWrapper<ModelCruxEntity> wrapper = new QueryWrapper<ModelCruxEntity>().eq("model_id", dataModelBo.getId());
        List<ModelCruxEntity> modelCruxList = modelCruxService.list(wrapper);
        modelCruxService.removeByIds(modelCruxList);

        List<ModelCruxEntity> modelCruxEntities = new ArrayList<>();
        for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
            CruxTypeScaleBo cruxTypeScaleBo = cruxTypeScaleBos.get(i);
            if (cruxTypeScaleBo != null){
                ModelCruxEntity modelCruxEntity = new ModelCruxEntity();
                modelCruxEntity.setId(SnowflakeManager.nextValue());
                modelCruxEntity.setModelId(dataModelEntity.getId());
                modelCruxEntity.setCruxTypeId(cruxTypeScaleBo.getCruxTypeId());
                modelCruxEntity.setScale(cruxTypeScaleBo.getScale());
                modelCruxEntity.setCreateDate(new Date());
                modelCruxEntity.setUpdateDate(new Date());
                modelCruxEntities.add(modelCruxEntity);
            }
        }
        modelCruxService.saveBatch(modelCruxEntities);

        return R.ok("修改成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> deleteDataModel(Long id) {

        dataModelService.removeById(id);
        // 删除关键词
        QueryWrapper<ModelCruxEntity> wrapper = new QueryWrapper<ModelCruxEntity>().eq("model_id", id);
        List<ModelCruxEntity> modelCruxList = modelCruxService.list(wrapper);
        modelCruxService.removeByIds(modelCruxList);

        return R.ok("删除成功");
    }

    @Override
    public R<DataModelSaveBo> infoDataModel(Long id) {
        DataModelSaveBo dataModelSaveBo = new DataModelSaveBo();

        DataModelEntity dataModelById = dataModelService.getById(id);
        BeanUtils.copyProperties(dataModelById,dataModelSaveBo);
        List<ModelCruxEntity> modelIds = modelCruxService.list(new QueryWrapper<ModelCruxEntity>().eq("model_id", id));
        List<CruxTypeScaleBo> cruxTypeScaleBos = new ArrayList<>();
        for (int i = 0; i < modelIds.size(); i++) {
            CruxTypeScaleBo cruxTypeScaleBo = new CruxTypeScaleBo();
            CruxTypeEntity crux = cruxTypeService.getById(modelIds.get(i).getCruxTypeId());
            cruxTypeScaleBo.setScale(modelIds.get(i).getScale());
            cruxTypeScaleBo.setCruxTypeName(crux.getName());
            cruxTypeScaleBo.setCruxTypeId(crux.getId());
            cruxTypeScaleBos.add(cruxTypeScaleBo);
        }
        dataModelSaveBo.setCruxTypeScaleBos(cruxTypeScaleBos);

        return R.ok(dataModelSaveBo);
    }


    @Override
    public DataModelEntity saveModel(TradeInfoVo tradeBo) {
        DataModelEntity dataModelEntity = new DataModelEntity();
        dataModelEntity.setId(tradeBo.getTradeModelId());
        dataModelEntity.setName(tradeBo.getModelName());
        dataModelEntity.setType(1);
        dataModelEntity.setTradeId(tradeBo.getId());
        dataModelEntity.setCreateDate(new Date());
        dataModelEntity.setUpdateDate(new Date());
        dataModelService.save(dataModelEntity);

        DataModelEntity dataModel = dataModelService.getById(dataModelEntity.getId());

        return dataModel;
    }

    @Override
    public void updateModel(TradeBo tradeBo) {

        DataModelEntity dataModelById = dataModelService.getById(tradeBo.getTradeModelId());
        DataModelEntity dataModelEntity = new DataModelEntity();
        if (dataModelById != null){
            dataModelEntity.setName(tradeBo.getModelName());
            dataModelEntity.setId(tradeBo.getTradeModelId());
            dataModelEntity.setUpdateDate(new Date());
            dataModelService.updateById(dataModelEntity);
        }else{
            dataModelEntity.setId(tradeBo.getTradeModelId());
            dataModelEntity.setName(tradeBo.getModelName());
            dataModelEntity.setType(1);
            dataModelEntity.setTradeId(tradeBo.getId());
            dataModelEntity.setCreateDate(new Date());
            dataModelEntity.setUpdateDate(new Date());
            dataModelService.save(dataModelEntity);
        }

        // 删除关键词后再新增
        QueryWrapper<ModelCruxEntity> wrapper = new QueryWrapper<ModelCruxEntity>().eq("model_id", tradeBo.getTradeModelId());
        List<ModelCruxEntity> modelCruxList = modelCruxService.list(wrapper);
        modelCruxService.removeByIds(modelCruxList);

        List<CruxTypeScaleBo> cruxTypeScaleBos = tradeBo.getCruxTypeScaleBos();
//        如果没有关键词ID带回，需要通过模型ID去查旗下关键词
        List<ModelCruxEntity> modelCruxEntities = new ArrayList<>();
        for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
            CruxTypeScaleBo cruxTypeScaleBo = cruxTypeScaleBos.get(i);
            if (cruxTypeScaleBo != null){
                ModelCruxEntity modelCruxEntity = new ModelCruxEntity();
                modelCruxEntity.setId(SnowflakeManager.nextValue());
                modelCruxEntity.setModelId(dataModelEntity.getId());
                modelCruxEntity.setCruxTypeId(cruxTypeScaleBo.getCruxTypeId());
                modelCruxEntity.setScale(cruxTypeScaleBo.getScale());
                modelCruxEntity.setCreateDate(new Date());
                modelCruxEntity.setUpdateDate(new Date());
                modelCruxEntities.add(modelCruxEntity);
            }
        }
        modelCruxService.saveBatch(modelCruxEntities);
    }

    @Override
    public DataModelSaveBo infoModel(Long id) {
        DataModelSaveBo dataModelSaveBo = new DataModelSaveBo();

        DataModelEntity dataModelById = dataModelService.getOne(new QueryWrapper<DataModelEntity>().eq("trade_id",id));
        if (dataModelById != null){
            BeanUtils.copyProperties(dataModelById,dataModelSaveBo);
            List<ModelCruxEntity> modelIds = modelCruxService.list(new QueryWrapper<ModelCruxEntity>().eq("model_id", dataModelById.getId()));
            List<CruxTypeScaleBo> cruxTypeScaleBos = new ArrayList<>();
            if (modelIds != null && modelIds.size() > 0){
                for (int i = 0; i < modelIds.size(); i++) {
                    CruxTypeScaleBo cruxTypeScaleBo = new CruxTypeScaleBo();
                    CruxTypeEntity crux = cruxTypeService.getById(modelIds.get(i).getCruxTypeId());
                    cruxTypeScaleBo.setScale(modelIds.get(i).getScale());
                    cruxTypeScaleBo.setCruxTypeName(crux.getName());
                    cruxTypeScaleBo.setCruxTypeId(crux.getId());
                    cruxTypeScaleBos.add(cruxTypeScaleBo);
                }
            }
            dataModelSaveBo.setCruxTypeScaleBos(cruxTypeScaleBos);
        }


        return dataModelSaveBo;
    }

    @Override
    public void deleteTradeModel(Long modelId) {
        dataModelService.removeById(modelId);
        // 删除关键词类型
        QueryWrapper<ModelCruxEntity> wrapper = new QueryWrapper<ModelCruxEntity>().eq("model_id", modelId);
        List<ModelCruxEntity> modelCruxList = modelCruxService.list(wrapper);
        modelCruxService.removeByIds(modelCruxList);
    }

    @Override
    public List<DataModelInfoVo> listByTradeIds(List<Long> tradeIds) {
        QueryWrapper<DataModelEntity> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.in("trade_id", tradeIds).or().eq("type", 0);
        });
        List<DataModelEntity> dataModelEntities = this.dataModelService.list(wrapper);

        return handelDataModel(dataModelEntities);
    }

    /**
     * 封装模型详细信息
     * @param dataModelEntities 模型列表
     * @return
     */
    private List<DataModelInfoVo> handelDataModel(List<DataModelEntity> dataModelEntities) {
        if(dataModelEntities != null && dataModelEntities.size() > 0) {

            // 获取模型和关键词的关联关系
            List<Long> modelIds = dataModelEntities.stream().map(DataModelEntity::getId).collect(Collectors.toList());
            List<ModelCruxEntity> modelCruxEntities = this.modelCruxService.list(new QueryWrapper<ModelCruxEntity>().in("model_id", modelIds));
            // 获取所有关键词分类信息
            List<CruxTypeEntity> cruxTypeEntities = new LinkedList<>();
            if(modelCruxEntities != null && modelCruxEntities.size() > 0) {
                Set<Long> cruxTypeIds = modelCruxEntities.stream().map(ModelCruxEntity::getCruxTypeId).collect(Collectors.toSet());
                cruxTypeEntities = this.cruxTypeService.listByIds(cruxTypeIds);
                if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
                    cruxTypeEntities.sort(Comparator.comparingInt(CruxTypeEntity::getSort));
                }
            }

            List<CruxTypeEntity> finalCruxTypeEntities = cruxTypeEntities;
            List<DataModelInfoVo> dataModelVos = dataModelEntities.stream().map(item -> {
                DataModelInfoVo dataModelVo = new DataModelInfoVo();
                BeanUtils.copyProperties(item, dataModelVo);

                List<DataModelItemVo> modelItemList = new LinkedList<>();
                if(modelCruxEntities != null && finalCruxTypeEntities != null) {
                    for (ModelCruxEntity modelCruxEntity : modelCruxEntities) {
                        if(modelCruxEntity.getModelId().equals(dataModelVo.getId())) {
                            for (CruxTypeEntity cruxTypeEntity : finalCruxTypeEntities) {
                                if(cruxTypeEntity.getId().equals(modelCruxEntity.getCruxTypeId())) {
                                    DataModelItemVo dataModelItemVo = new DataModelItemVo();
                                    dataModelItemVo.setCruxTypeId(cruxTypeEntity.getId());
                                    dataModelItemVo.setCruxTypeName(cruxTypeEntity.getName());
                                    dataModelItemVo.setCruxTypeScale(modelCruxEntity.getScale());
                                    modelItemList.add(dataModelItemVo);
                                }
                            }
                        }
                    }
                }
                dataModelVo.setModelItemList(modelItemList);
                dataModelVo.setDefaultShow(0);
                return dataModelVo;
            }).toList();
            return dataModelVos;
        }
        return null;
    }

    @Override
    public List<DataModelInfoVo> listByIdsAndGeneral(Collection<Long> modelIds) {
        QueryWrapper<DataModelEntity> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.eq("type", 0);
            if(modelIds != null && modelIds.size() > 0) {
                w.or().in("id", modelIds);
            }
        });
        List<DataModelEntity> dataModelEntities = this.dataModelService.list(wrapper);

        return handelDataModel(dataModelEntities);
    }
}
