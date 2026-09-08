package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.ai.bo.SaveDiagnosisBo;
import com.jiuyu.replay.ai.entity.DiagnosisCueEntity;
import com.jiuyu.replay.ai.entity.DiagnosisModelEntity;
import com.jiuyu.replay.ai.repository.dao.DiagnosisCueDao;
import com.jiuyu.replay.ai.repository.service.DiagnosisCueService;
import com.jiuyu.replay.ai.repository.service.DiagnosisModelService;
import com.jiuyu.replay.ai.rse.DiagnosisCueRse;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueListBo;
import com.jiuyu.replay.generic.bo.ai.SaveDiagnosisCueBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoDetailFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.CueWordsFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueListVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueVo;
import com.jiuyu.replay.generic.vo.ai.UnreadDiagnosisReportVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email
 * @date 2025-05-19 14:02:59
 */
@Service
@AllArgsConstructor
public class DiagnosisCueRseImpl implements DiagnosisCueRse {

    private final DiagnosisCueDao diagnosisCueDao;
    private final DiagnosisCueService diagnosisCueService;
    private final DiagnosisModelService diagnosisModelService;
    private final UserFeign userFeign;
    private final CueWordsFeign cueWordsFeign;
    private final AnchorVideoDetailFeign anchorVideoDetailFeign;
    private final AnchorVideoFeign anchorVideoFeign;
    private final SystemKvService systemKvService;


    @Override
    public PageUtils<DiagnosisCueListVo> queryPage(DiagnosisCueListBo diagnosisCueListBo) {
        QueryWrapper<DiagnosisCueEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(diagnosisCueListBo.getKeyword())) {
            wrapper.like("name", diagnosisCueListBo.getKeyword());
        }

        IPage<DiagnosisCueEntity> iPage = diagnosisCueService.page(new Query<DiagnosisCueEntity>().getPage(diagnosisCueListBo.getPage(), diagnosisCueListBo.getLimit()), wrapper);

        PageUtils<DiagnosisCueListVo> pageUtils = new PageUtils<>(diagnosisCueListBo.getPage(), diagnosisCueListBo.getLimit(), iPage);

        List<DiagnosisCueEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<DiagnosisCueListVo> vos = records.stream().map(item -> {
                DiagnosisCueListVo diagnosisCueVo = new DiagnosisCueListVo();
                BeanUtils.copyProperties(item, diagnosisCueVo);
                return diagnosisCueVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public DiagnosisCueInfoVo info(Long id) {

        DiagnosisCueEntity diagnosisCueEntity = diagnosisCueService.getById(id);
        if (diagnosisCueEntity != null) {
            DiagnosisCueInfoVo diagnosisCueInfoVo = new DiagnosisCueInfoVo();
            BeanUtils.copyProperties(diagnosisCueEntity, diagnosisCueInfoVo);
            return diagnosisCueInfoVo;
        }

        return null;
    }

    /**
     * 新增ai诊断提示词配置
     *
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    public DiagnosisCueInfoVo save(DiagnosisCueBo diagnosisCueBo) {

        DiagnosisCueEntity diagnosisCueEntity = new DiagnosisCueEntity();
        BeanUtils.copyProperties(diagnosisCueBo, diagnosisCueEntity);
        diagnosisCueEntity.setId(SnowflakeManager.nextValue());
        diagnosisCueEntity.setCreateDate(new Date());
        diagnosisCueEntity.setUpdateDate(new Date());

        diagnosisCueService.save(diagnosisCueEntity);

        DiagnosisCueInfoVo diagnosisCueInfoVo = new DiagnosisCueInfoVo();
        BeanUtils.copyProperties(diagnosisCueEntity, diagnosisCueInfoVo);

        return diagnosisCueInfoVo;
    }

    /**
     * 修改ai诊断提示词配置
     *
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    public void update(DiagnosisCueBo diagnosisCueBo) {

        DiagnosisCueEntity diagnosisCueEntity = new DiagnosisCueEntity();
        BeanUtils.copyProperties(diagnosisCueBo, diagnosisCueEntity);
        diagnosisCueEntity.setUpdateDate(new Date());

        diagnosisCueService.updateById(diagnosisCueEntity);
    }

    /**
     * 删除ai诊断提示词配置
     *
     * @param id ai诊断提示词配置id
     * @return
     */
    public void deleteById(Long id) {

        diagnosisCueService.removeById(id);
    }

    @Override
    public List<DiagnosisCueInfoVo> listDiagnosis(String sourceId, Integer sourceType, Integer diagnosisType, Long tenantId, Long userId) {
        List<DiagnosisCueEntity> list = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, sourceId)
                .eq(ObjectUtil.isNotEmpty(sourceType), DiagnosisCueEntity::getSourceType, sourceType)
                .eq(DiagnosisCueEntity::getTenantId, tenantId)
                .eq(DiagnosisCueEntity::getUserId, userId)
                .eq(ObjectUtil.isNotEmpty(diagnosisType), DiagnosisCueEntity::getDiagnosisType, diagnosisType)
        );
        if (list != null) {
            return BeanUtil.copyToList(list, DiagnosisCueInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public void saveDiagnosisCue(SaveDiagnosisCueBo saveDiagnosisCueBo) {
        Date now = new Date();

        // 保存模型
        DiagnosisModelEntity modelEntity = BeanUtil.copyProperties(saveDiagnosisCueBo, DiagnosisModelEntity.class);
        modelEntity.setId(SnowflakeManager.nextValue());
        modelEntity.setUpdateDate(now);
        modelEntity.setCreateDate(now);
        DiagnosisModelEntity one = diagnosisModelService.getOne(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getSourceId, saveDiagnosisCueBo.getSourceId())
                .eq(DiagnosisModelEntity::getSourceType, saveDiagnosisCueBo.getSourceType())
                .eq(DiagnosisModelEntity::getUserId, saveDiagnosisCueBo.getUserId())
                .eq(DiagnosisModelEntity::getTenantId, saveDiagnosisCueBo.getTenantId())
                .last("limit 1")
        );
        if (one != null) {
            modelEntity.setModelId(saveDiagnosisCueBo.getModelId());
            modelEntity.setId(one.getId());
            modelEntity.setCreateDate(one.getCreateDate());
        }
        diagnosisModelService.saveOrUpdate(modelEntity);


        if (saveDiagnosisCueBo.getSelectCueWordsIdsList() == null)
            saveDiagnosisCueBo.setSelectCueWordsIdsList(new ArrayList<>());

        // 获取当前用户已选择的cueWordsId
        List<DiagnosisCueInfoVo> list = listDiagnosis(saveDiagnosisCueBo.getSourceId(), saveDiagnosisCueBo.getSourceType(), saveDiagnosisCueBo.getDiagnosisType(), saveDiagnosisCueBo.getTenantId(), saveDiagnosisCueBo.getUserId());

        saveDiagnosisCueBo.setUpdateDate(now);
        saveDiagnosisCueBo.setCreateDate(now);

        List<DiagnosisCueEntity> entityList = saveDiagnosisCueBo.getSelectCueWordsIdsList().stream().map(cueWordsId -> {
            DiagnosisCueEntity entity = BeanUtil.copyProperties(saveDiagnosisCueBo, DiagnosisCueEntity.class);
            entity.setId(SnowflakeManager.nextValue());
            entity.setCueWordsId(cueWordsId);
            return entity;
        }).toList();


        if (list != null) {

            // 筛选出要删除的记录
            List<Long> deleteIds = list.stream()
                    .filter(item -> !saveDiagnosisCueBo.getSelectCueWordsIdsList().contains(item.getCueWordsId()))
                    .map(DiagnosisCueInfoVo::getId)
                    .toList();

            // 删除
            if (!deleteIds.isEmpty()) {
                diagnosisCueService.removeByIds(deleteIds);
            }

            // 筛选出要修改的记录
            Map<Long, DiagnosisCueInfoVo> oldMap = list.stream()
                    .collect(Collectors.toMap(DiagnosisCueInfoVo::getCueWordsId, item -> item, (t, t2) -> t));
            entityList.forEach(item -> {
                DiagnosisCueInfoVo diagnosisCueEntity = oldMap.get(item.getCueWordsId());
                if (diagnosisCueEntity != null) {
                    item.setId(diagnosisCueEntity.getId());
                    item.setCreateUserId(diagnosisCueEntity.getCreateUserId());
                    item.setCreateDate(diagnosisCueEntity.getCreateDate());
                    item.setQaStatus(AiEnums.qaStatus.WAITING.getCode());
                    if (ObjectUtil.equals(saveDiagnosisCueBo.getSourceType(), 1)) {
                        item.setQaStatus(diagnosisCueEntity.getQaStatus());
                    }
                } else {
                    item.setQaStatus(AiEnums.qaStatus.WAITING.getCode());
                }
                item.setSelectBoard(saveDiagnosisCueBo.getSelectBoard());
                item.setSelectDataScreenshot(saveDiagnosisCueBo.getSelectDataScreenshot());
                item.setIsSelected(1);
            });
        }

        if (ObjectUtil.isNotEmpty(entityList)) {
            diagnosisCueService.saveOrUpdateBatch(entityList);
        }
    }

    @Override
    public List<DiagnosisCueInfoVo> setAutoDiagnosisQuestions(String videoId, String secUid) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        List<DiagnosisCueInfoVo> result = new ArrayList<>();

        // 1、查询当前的视频是否存在自动诊断提示词配置
        List<DiagnosisCueEntity> videoCues = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, videoId)
                .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.VIDEO.getCode()) // 1表示视频
                .eq(DiagnosisCueEntity::getUserId, user.getId())
                .eq(DiagnosisCueEntity::getDiagnosisType, AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode())
                .eq(DiagnosisCueEntity::getTenantId, user.getActiveTenantId())
        );

        // 1.1 存在就返回这些配置
        if (videoCues != null && !videoCues.isEmpty()) {
            return result;
        }

        // 2、查询对应主播secUid是否有配置诊断提示词
        List<DiagnosisCueEntity> anchorCues = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, secUid)
                .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.ANCHOR.getCode()) // 0表示主播
                .eq(DiagnosisCueEntity::getUserId, user.getId())
                .eq(DiagnosisCueEntity::getTenantId, user.getActiveTenantId())
                .eq(DiagnosisCueEntity::getDiagnosisType, AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode())
        );

        // 2.1 不存在就返回空list
        if (anchorCues.isEmpty()) {
            return result;
        }

        // 3、把查询出来的提示词保存到DiagnosisCue中，sourceType是1
        List<DiagnosisCueEntity> newVideoCues = new ArrayList<>();
        Date now = new Date();


        for (DiagnosisCueEntity anchorCue : anchorCues) {
            DiagnosisCueEntity videoCue = new DiagnosisCueEntity();
            BeanUtils.copyProperties(anchorCue, videoCue);
            videoCue.setId(SnowflakeManager.nextValue());
            videoCue.setUpdateUserId(user.getId());
            videoCue.setCreateUserId(user.getId());
            videoCue.setSourceId(videoId);
            videoCue.setSourceType(AiEnums.diagnosisSourceType.VIDEO.getCode()); // 设置为视频类型
            videoCue.setCreateDate(now);
            videoCue.setUpdateDate(now);
            videoCue.setQaStatus(AiEnums.qaStatus.WAITING.getCode()); // 设置为未处理状态
            videoCue.setDiagnosisType(AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode());
            newVideoCues.add(videoCue);
        }

        // 批量保存新创建的视频提示词配置
        if (!newVideoCues.isEmpty()) {
            diagnosisCueService.saveBatch(newVideoCues);
        }

        // 4、返回视频的诊断提示词
        List<DiagnosisCueInfoVo> diagnosisCueInfoVos = BeanUtil.copyToList(newVideoCues, DiagnosisCueInfoVo.class);

        if (ObjectUtil.isNotEmpty(diagnosisCueInfoVos)) {
            List<CueWordsInfoVo> cueWordsInfoVos = cueWordsFeign.listByIds(diagnosisCueInfoVos.stream().map(DiagnosisCueVo::getCueWordsId).toList());
            if (CollectionUtil.isNotEmpty(cueWordsInfoVos)) {
                DataUtils.setFieldNameById(diagnosisCueInfoVos, "cueWordsId", "cueType", cueWordsInfoVos, "id", "cueType");
            }
        }

        return diagnosisCueInfoVos;
    }

    @Override
    public List<DiagnosisCueInfoVo> setAutoDataDiagnosisQuestions(String videoId, String secUid, List<Long> cueWordsIds) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        List<DiagnosisCueInfoVo> result = new ArrayList<>();

        // 1、查询当前的视频是否存在自动诊断提示词配置
        List<DiagnosisCueEntity> videoCues = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, videoId)
                .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.VIDEO.getCode()) // 1表示视频
                .eq(DiagnosisCueEntity::getUserId, user.getId())
                .eq(DiagnosisCueEntity::getDiagnosisType, AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode())
                .eq(DiagnosisCueEntity::getTenantId, user.getActiveTenantId())
        );

        // 1.1 存在就返回这些配置
        if (videoCues != null && !videoCues.isEmpty()) {
            return result;
        }

        // 3、把查询出来的提示词保存到DiagnosisCue中，sourceType是1
        List<DiagnosisCueEntity> newVideoCues = new ArrayList<>();
        Date now = new Date();


        for (Long cueWordsId : cueWordsIds) {
            DiagnosisCueEntity videoCue = new DiagnosisCueEntity();
            videoCue.setId(SnowflakeManager.nextValue());
            videoCue.setSourceId(videoId);
            videoCue.setSourceType(AiEnums.diagnosisSourceType.VIDEO.getCode()); // 设置为视频类型
            videoCue.setCueWordsId(cueWordsId);
            videoCue.setQaStatus(AiEnums.qaStatus.WAITING.getCode()); // 设置为未处理状态
            videoCue.setDiagnosisType(AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode());
            videoCue.setSelectDataScreenshot(1);
            videoCue.setSelectBoard(1);
            videoCue.setUserId(user.getId());
            videoCue.setTenantId(user.getActiveTenantId());
            videoCue.setUpdateUserId(user.getId());
            videoCue.setCreateUserId(user.getId());
            videoCue.setCreateDate(now);
            videoCue.setUpdateDate(now);
            newVideoCues.add(videoCue);
        }

        // 批量保存新创建的视频提示词配置
        if (!newVideoCues.isEmpty()) {
            diagnosisCueService.saveBatch(newVideoCues);
        }

        // 4、返回视频的诊断提示词
        return BeanUtil.copyToList(newVideoCues, DiagnosisCueInfoVo.class);
    }

    @Override
    public String isGenerateDiagnosisFile(String sourceId, Integer diagnosisType) {

        AnchorVideoDetailInfoVo videoDetail = anchorVideoDetailFeign.getAndSave(sourceId);

        if (diagnosisType == AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode()) {
            return "yes";
        } else if (diagnosisType == AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode()) {
            // 判断视频是否已经生成过诊断报告
            if (videoDetail != null && videoDetail.getHasDiagnosisReport() == 1) {
                return "no";
            }

            AnchorVideoInfoVo videoInfoVo = ResultUtil.getResult(anchorVideoFeign.GetByVideoId(sourceId));
            if (videoInfoVo == null) {
                return "no";
            }

            long count1 = diagnosisCueService.count(new LambdaQueryWrapper<DiagnosisCueEntity>()
                    .eq(DiagnosisCueEntity::getSourceId, videoInfoVo.getSecUid())
                    .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.ANCHOR.getCode())
                    .eq(DiagnosisCueEntity::getIsSelected, 1)
            );
            if (count1 == 0) {
                return "no";
            }

            // 查询是否还有未处理的或者处理错误的
            long count = diagnosisCueService.count(new LambdaQueryWrapper<DiagnosisCueEntity>()
                    .eq(DiagnosisCueEntity::getSourceId, sourceId)
                    .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.VIDEO.getCode())
                    .eq(DiagnosisCueEntity::getIsSelected, 1)
                    .ne(DiagnosisCueEntity::getQaStatus, AiEnums.qaStatus.SUCCESS.getCode())
            );

            return count == 0 ? "yes" : "no";
        }
        return "no";
    }

    @Override
    public List<DiagnosisCueInfoVo> saveDiagnosis(SaveDiagnosisBo saveDiagnosisBo) {
        RRException.isNotEmpty(saveDiagnosisBo, "参数不能为空");
        RRException.isNotEmpty(saveDiagnosisBo.getSourceId(), "来源id不能为空");
        RRException.isNotEmpty(saveDiagnosisBo.getCueWordsIds(), "提示词id不能为空");
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        ArrayList<DiagnosisCueEntity> entityList = new ArrayList<>();
        Date now = new Date();

        Map<Long, DiagnosisCueEntity> cueMap = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, saveDiagnosisBo.getSourceId())
                .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.VIDEO.getCode())
                .in(DiagnosisCueEntity::getCueWordsId, saveDiagnosisBo.getCueWordsIds())
        ).stream().collect(Collectors.toMap(DiagnosisCueEntity::getCueWordsId, item -> item, (t, t2) -> t));

        // 获取已存在的cueWordsId, 并且不是待处理和处理中的
        List<Long> updateCueIds = saveDiagnosisBo.getCueWordsIds().stream().filter(cueMap::containsKey).toList();

        if (ObjectUtil.isNotEmpty(updateCueIds)) {
            updateCueIds.forEach(item -> {
                DiagnosisCueEntity entity = cueMap.get(item);
                if (ObjectUtil.isNotEmpty(entity) && entity.getQaStatus() != AiEnums.qaStatus.WAITING.getCode() && entity.getQaStatus() != AiEnums.qaStatus.GENERATING.getCode()) {
                    entity.setQaStatus(AiEnums.qaStatus.WAITING.getCode());
                    entityList.add(entity);
                }
            });
        }

        List<Long> cueIds = saveDiagnosisBo.getCueWordsIds().stream().filter(item -> !cueMap.containsKey(item)).toList();
        if (ObjectUtil.isNotEmpty(cueIds)) {
            List<CueWordsInfoVo> cueWordsInfoVos = cueWordsFeign.listByIds(cueIds);
            if (ObjectUtil.isEmpty(cueWordsInfoVos) || cueWordsInfoVos.size() != cueIds.size()) {
                RRException.create("提示词id错误");
            }
            cueWordsInfoVos.forEach(cue -> {
                DiagnosisCueEntity entity = new DiagnosisCueEntity();
                entity.setId(SnowflakeManager.nextValue());
                entity.setSourceId(saveDiagnosisBo.getSourceId());
                entity.setSourceType(AiEnums.diagnosisSourceType.VIDEO.getCode());
                entity.setCueWordsId(cue.getId());
                entity.setQaStatus(AiEnums.qaStatus.WAITING.getCode());
                entity.setUserId(user.getId());
                entity.setTenantId(user.getActiveTenantId());
                entity.setUpdateUserId(user.getId());
                entity.setUpdateDate(now);
                entity.setCreateUserId(user.getId());
                entity.setCreateDate(now);
                entityList.add(entity);
            });
        }


        if (ObjectUtil.isNotEmpty(entityList)) {
            diagnosisCueService.saveOrUpdateBatch(entityList);
        }

        List<DiagnosisCueEntity> videoCues = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, saveDiagnosisBo.getSourceId())
                .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.VIDEO.getCode()) // 1表示视频
                .in(DiagnosisCueEntity::getCueWordsId, saveDiagnosisBo.getCueWordsIds())
        );

        List<CueWordsInfoVo> cueWordsInfoVos = cueWordsFeign.listByIds(saveDiagnosisBo.getCueWordsIds());
        if (ObjectUtil.isEmpty(cueWordsInfoVos) || cueWordsInfoVos.size() != saveDiagnosisBo.getCueWordsIds().size()) {
            RRException.create("提示词id错误");
        }
        List<DiagnosisCueInfoVo> diagnosisCueInfoVos = BeanUtil.copyToList(videoCues, DiagnosisCueInfoVo.class);
        DataUtils.setFieldNameById(diagnosisCueInfoVos, "cueWordsId", "cueType", cueWordsInfoVos, "id", "cueType");

        return diagnosisCueInfoVos;
    }

    @Override
    public List<DiagnosisCueInfoVo> handleDiagnosisByUser() {
        handleDiagnosis(null, 1);
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        List<DiagnosisCueEntity> videoCues = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getUserId, user.getId())
                .eq(DiagnosisCueEntity::getTenantId,  user.getActiveTenantId())
                .eq(DiagnosisCueEntity::getSourceType, AiEnums.diagnosisSourceType.VIDEO.getCode()) // 1表示视频
                .in(DiagnosisCueEntity::getQaStatus, AiEnums.qaStatus.WAITING.getCode(), AiEnums.qaStatus.GENERATING.getCode())
                .orderByDesc(DiagnosisCueEntity::getQaStatus)
                .orderByAsc(DiagnosisCueEntity::getCreateDate)
        );
        List<DiagnosisCueInfoVo> diagnosisCueInfoVos = BeanUtil.copyToList(videoCues, DiagnosisCueInfoVo.class);
        if (CollectionUtil.isNotEmpty(diagnosisCueInfoVos)) {
            List<CueWordsInfoVo> cueWordsInfoVos = cueWordsFeign.listByIds(diagnosisCueInfoVos.stream().map(DiagnosisCueVo::getCueWordsId).toList());
            if (CollectionUtil.isNotEmpty(cueWordsInfoVos)) {
                DataUtils.setFieldNameById(diagnosisCueInfoVos, "cueWordsId", "cueType", cueWordsInfoVos, "id", "cueType");
            }
        }
        return diagnosisCueInfoVos;
    }

    @Override
    public List<DiagnosisCueInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId) {
        List<DiagnosisCueEntity> list = diagnosisCueService.list(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceType, sourceType)
                .eq(DiagnosisCueEntity::getUserId, userId)
                .eq(DiagnosisCueEntity::getTenantId, tenantId)
                .in(DiagnosisCueEntity::getSourceId, sourceIds)
        );
        return BeanUtil.copyToList(list, DiagnosisCueInfoVo.class);
    }

    @Override
    public void handleDiagnosis(String sourceId, Integer sourceType) {

        if (sourceType == 1) {
            SystemKvEntity byKey = systemKvService.getByKey("diagnosis_timeout_report");
            if (byKey == null) {
                return;
            }
            int timeout = NumberUtil.parseInt(byKey.getKvValue(), 30) * 60;
            diagnosisCueService.update(new LambdaUpdateWrapper<DiagnosisCueEntity>()
                    .eq(ObjectUtil.isNotEmpty(sourceId), DiagnosisCueEntity::getSourceId, sourceId)
                    .eq(DiagnosisCueEntity::getSourceType, sourceType)
                    .eq(DiagnosisCueEntity::getQaStatus, AiEnums.qaStatus.GENERATING.getCode())
                    .apply("TIMESTAMPDIFF(SECOND, qa_handle_time, NOW()) > " + timeout)
                    .set(DiagnosisCueEntity::getQaStatus, AiEnums.qaStatus.FAIL.getCode())
                    .set(DiagnosisCueEntity::getErrorContent, "客户端被关闭")
            );
        }
    }

    @Override
    public void deleteBySourceId(SaveDiagnosisCueBo cueBo) {

        diagnosisModelService.remove(new LambdaQueryWrapper<DiagnosisModelEntity>()
                .eq(DiagnosisModelEntity::getUserId, cueBo.getUserId())
                .eq(DiagnosisModelEntity::getTenantId, cueBo.getTenantId())
                .eq(DiagnosisModelEntity::getSourceId, cueBo.getSourceId())
                .eq(DiagnosisModelEntity::getSourceType, cueBo.getSourceType())
        );

        diagnosisCueService.remove(new LambdaQueryWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getUserId, cueBo.getUserId())
                .eq(DiagnosisCueEntity::getTenantId, cueBo.getTenantId())
                .eq(DiagnosisCueEntity::getSourceId, cueBo.getSourceId())
                .eq(DiagnosisCueEntity::getSourceType, cueBo.getSourceType())
        );


    }

    @Override
    public void deleteBatch(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        diagnosisCueService.removeByIds(ids);
    }

    @Override
    public void updateReadStatus(List<Long> ids, Integer isRead) {
        if (ObjectUtil.isEmpty(ids)) {
            return;
        }
        diagnosisCueService.update(new LambdaUpdateWrapper<DiagnosisCueEntity>()
                .in(DiagnosisCueEntity::getId, ids)
                .set(DiagnosisCueEntity::getIsRead, isRead)
        );
    }

    @Override
    public void updateReadStatusBySourceAndCueWords(String sourceId, List<Long> cueWordsIds, Long userId, Long tenantId, Integer isRead) {
        if (ObjectUtil.isEmpty(sourceId) || ObjectUtil.isEmpty(cueWordsIds)) {
            return;
        }
        diagnosisCueService.update(new LambdaUpdateWrapper<DiagnosisCueEntity>()
                .eq(DiagnosisCueEntity::getSourceId, sourceId)
                .in(DiagnosisCueEntity::getCueWordsId, cueWordsIds)
                .eq(DiagnosisCueEntity::getUserId, userId)
                .eq(DiagnosisCueEntity::getTenantId, tenantId)
                .set(DiagnosisCueEntity::getIsRead, isRead)
        );
    }

    @Override
    public List<UnreadDiagnosisReportVo> listUnreadDataDiagnosis(Long userId, Long tenantId, Integer videoSliceType) {
        return diagnosisCueDao.listUnreadDataDiagnosis(userId, tenantId, videoSliceType);
    }
}

