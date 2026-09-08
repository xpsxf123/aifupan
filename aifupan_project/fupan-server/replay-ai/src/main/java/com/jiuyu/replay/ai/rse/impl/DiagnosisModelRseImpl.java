package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.ai.entity.DiagnosisModelEntity;
import com.jiuyu.replay.ai.repository.service.DiagnosisModelService;
import com.jiuyu.replay.ai.rse.DiagnosisModelRse;
import com.jiuyu.replay.common.constant.CustomizeConstant;
import com.jiuyu.replay.common.repository.service.impl.SystemKvServiceImpl;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelListVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Slf4j
@Service
public class DiagnosisModelRseImpl implements DiagnosisModelRse {

    @Resource
    private DiagnosisModelService diagnosisModelService;
    @Autowired
    private SystemKvServiceImpl systemKvService;


    @Override
    public PageUtils<DiagnosisModelListVo> queryPage(DiagnosisModelListBo diagnosisModelListBo) {
        QueryWrapper<DiagnosisModelEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(diagnosisModelListBo.getKeyword())){
            wrapper.like("name", diagnosisModelListBo.getKeyword());
        }

        IPage<DiagnosisModelEntity> iPage = diagnosisModelService.page(new Query<DiagnosisModelEntity>().getPage(diagnosisModelListBo.getPage(), diagnosisModelListBo.getLimit()), wrapper);

        PageUtils<DiagnosisModelListVo> pageUtils = new PageUtils<>(diagnosisModelListBo.getPage(), diagnosisModelListBo.getLimit(), iPage);

        List<DiagnosisModelEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<DiagnosisModelListVo> vos = records.stream().map(item -> {
                DiagnosisModelListVo diagnosisModelVo = new DiagnosisModelListVo();
                BeanUtils.copyProperties(item, diagnosisModelVo);
                return diagnosisModelVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public DiagnosisModelInfoVo info(Long id) {

        DiagnosisModelEntity diagnosisModelEntity = diagnosisModelService.getById(id);
        if(diagnosisModelEntity != null) {
            DiagnosisModelInfoVo diagnosisModelInfoVo = new DiagnosisModelInfoVo();
            BeanUtils.copyProperties(diagnosisModelEntity, diagnosisModelInfoVo);
            return diagnosisModelInfoVo;
        }

        return null;
    }

    /**
     * 新增ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
     public DiagnosisModelInfoVo save(DiagnosisModelBo diagnosisModelBo) {

         DiagnosisModelEntity diagnosisModelEntity = new DiagnosisModelEntity();
         BeanUtils.copyProperties(diagnosisModelBo, diagnosisModelEntity);
         diagnosisModelEntity.setId(SnowflakeManager.nextValue());
         diagnosisModelEntity.setCreateDate(new Date());
         diagnosisModelEntity.setUpdateDate(new Date());

         diagnosisModelService.save(diagnosisModelEntity);

         DiagnosisModelInfoVo diagnosisModelInfoVo = new DiagnosisModelInfoVo();
         BeanUtils.copyProperties(diagnosisModelEntity, diagnosisModelInfoVo);

         return diagnosisModelInfoVo;
     }

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    public void update(DiagnosisModelBo diagnosisModelBo) {

        DiagnosisModelEntity diagnosisModelEntity = new DiagnosisModelEntity();
        BeanUtils.copyProperties(diagnosisModelBo, diagnosisModelEntity);
        diagnosisModelEntity.setUpdateDate(new Date());

        diagnosisModelService.updateById(diagnosisModelEntity);
    }

    /**
     * 删除ai诊断中的模型设置-主播和视频
     * @param id ai诊断中的模型设置-主播和视频id
     * @return
     */
    public void deleteById(Long id) {

        diagnosisModelService.removeById(id);
    }

    @Override
    public void saveOrUpdate(DiagnosisModelBo diagnosisModelBo) {
        DiagnosisModelEntity entity = BeanUtil.copyProperties(diagnosisModelBo, DiagnosisModelEntity.class);
        DiagnosisModelEntity one = diagnosisModelService.getOne(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getSourceId, diagnosisModelBo.getSourceId())
                .eq(DiagnosisModelEntity::getSourceType, diagnosisModelBo.getSourceType())
        );
        if (one != null){
           entity.setId(one.getId());
        }
        diagnosisModelService.saveOrUpdate(entity);
    }

    @Override
    public DiagnosisModelInfoVo getBySource(String sourceId, Integer sourceType, Long activeTenantId, Long userId, int diagnosisType) {
        DiagnosisModelEntity diagnosisModelEntity = diagnosisModelService.getOne(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getSourceId, sourceId)
                .eq(DiagnosisModelEntity::getSourceType, sourceType)
                .eq(DiagnosisModelEntity::getTenantId, activeTenantId)
                .eq(DiagnosisModelEntity::getUserId, userId)
                .eq(DiagnosisModelEntity::getDiagnosisType, diagnosisType)
        );
        if (diagnosisModelEntity != null){
            return BeanUtil.copyProperties(diagnosisModelEntity, DiagnosisModelInfoVo.class);
        }
        return null;
    }

    @Override
    public List<DiagnosisModelInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId) {
        List<DiagnosisModelEntity> list = diagnosisModelService.list(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getSourceType, sourceType)
                .eq(DiagnosisModelEntity::getUserId, userId)
                .eq(DiagnosisModelEntity::getTenantId, tenantId)
                .in(DiagnosisModelEntity::getSourceId, sourceIds)
        );
        return BeanUtil.copyToList(list, DiagnosisModelInfoVo.class);
    }

    @Override
    public void updateDiagnosisModel(DiagnosisModelBo diagnosisModelBo) {
        DiagnosisModelEntity one = diagnosisModelService.getOne(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getUserId, diagnosisModelBo.getUserId())
                .eq(DiagnosisModelEntity::getTenantId, diagnosisModelBo.getTenantId())
                .eq(DiagnosisModelEntity::getSourceId, diagnosisModelBo.getSourceId())
                .eq(DiagnosisModelEntity::getSourceType, diagnosisModelBo.getSourceType())
                .eq(DiagnosisModelEntity::getDiagnosisType, diagnosisModelBo.getDiagnosisType())
                .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue())
        );

        Date now = new Date();
        if (one != null){
            diagnosisModelService.update(new LambdaUpdateWrapper<DiagnosisModelEntity>()
                    .eq(DiagnosisModelEntity::getId, one.getId())
                    .set(DiagnosisModelEntity::getModelId, diagnosisModelBo.getModelId())
                    .set(DiagnosisModelEntity::getUpdateDate, now)
                    .set(DiagnosisModelEntity::getUpdateUserId, diagnosisModelBo.getUserId())
            );
        }else{
            diagnosisModelBo.setUpdateUserId(diagnosisModelBo.getUserId());
            diagnosisModelBo.setCreateUserId(diagnosisModelBo.getUserId());
            save(diagnosisModelBo);
        }
    }

    @Override
    public void addVideoDiagnosisModel(String secUid, String videoId, Long userId, Long activeTenantId, Integer diagnosisType) {
        DiagnosisModelEntity one = diagnosisModelService.getOne(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getSourceId, secUid)
                .eq(DiagnosisModelEntity::getSourceType, 0)
                .eq(DiagnosisModelEntity::getUserId, userId)
                .eq(DiagnosisModelEntity::getTenantId, activeTenantId)
                .eq(DiagnosisModelEntity::getDiagnosisType, diagnosisType)
                .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue())
        );

        if (one != null){

            DiagnosisModelEntity video = diagnosisModelService.getOne(new LambdaQueryWrapper<DiagnosisModelEntity>()
                    .eq(DiagnosisModelEntity::getSourceId, videoId)
                    .eq(DiagnosisModelEntity::getSourceType, 1)
                    .eq(DiagnosisModelEntity::getUserId, userId)
                    .eq(DiagnosisModelEntity::getTenantId, activeTenantId)
                    .eq(DiagnosisModelEntity::getDiagnosisType, diagnosisType)
                    .last("limit 1")
            );

            if (video == null) {
                DiagnosisModelBo bo = new DiagnosisModelBo();
                bo.setSourceId(videoId);
                bo.setSourceType(1);
                bo.setModelId(one.getModelId());
                bo.setDiagnosisType(diagnosisType);
                bo.setUserId(userId);
                bo.setTenantId(activeTenantId);
                bo.setUpdateUserId(userId);
                bo.setCreateUserId(userId);
                save(bo);
            }
        }
    }
}

