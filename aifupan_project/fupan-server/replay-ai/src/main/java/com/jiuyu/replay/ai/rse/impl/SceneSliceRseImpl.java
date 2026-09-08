package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jiuyu.replay.ai.bo.SaveSceneSliceBo;
import com.jiuyu.replay.ai.entity.SceneSliceEntity;
import com.jiuyu.replay.ai.repository.service.SceneSliceService;
import com.jiuyu.replay.ai.rse.SceneSliceRse;
import com.jiuyu.replay.ai.vo.SceneSliceStatusVo;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.vo.aiagent.SceneSliceVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 场景切片 RSE 实现
 *
 * @author lj
 * @date 2026-07-06
 */
@Slf4j
@Component
@AllArgsConstructor
public class SceneSliceRseImpl implements SceneSliceRse {

    private final SceneSliceService sceneSliceService;
    private final ImgOssUtils imgOssUtils;
    private final SystemKvProducer systemKvProducer;

    @Override
    public SceneSliceStatusVo getStatus(String videoId, Long userId, Long tenantId) {
        // 仅查询是否已有完成记录
        LambdaQueryWrapper<SceneSliceEntity> wrapper = new LambdaQueryWrapper<SceneSliceEntity>()
                .eq(SceneSliceEntity::getVideoId, videoId)
                .eq(SceneSliceEntity::getTenantId, tenantId)
//                .eq(SceneSliceEntity::getStatus, AiEnums.sceneSliceStatus.COMPLETED.getCode())
//                .orderByDesc(SceneSliceEntity::getCreateDate)
                .last("LIMIT 1");
        SceneSliceEntity entity = sceneSliceService.getOne(wrapper);

        if (entity != null) {
            return SceneSliceStatusVo.builder()
                    .status(AiEnums.sceneSliceStatus.COMPLETED.getCode())
                    .build();
        }

        SignUploadUrlVo signUrl = imgOssUtils.getSignUploadUrl("sceneSlice", "jpg");
        int sliceSeconds = getSliceSeconds();
        return SceneSliceStatusVo.builder()
                .status(AiEnums.sceneSliceStatus.PENDING.getCode())
                .signedUrl(signUrl.getSignedUrl())
                .ossKey(signUrl.getOssKey())
                .sliceSeconds(sliceSeconds)
                .build();
    }

    @Override
    public SceneSliceEntity createRecord(SaveSceneSliceBo bo, Long userId, Long tenantId) {
        Date now = new Date();
        SceneSliceEntity entity = SceneSliceEntity.builder()
                .id(SnowflakeManager.nextValue())
                .videoId(bo.getVideoId())
                .ossKey(bo.getOssKey())
                .sliceSeconds(bo.getSliceSeconds())
                .status(AiEnums.sceneSliceStatus.PROCESSING.getCode())
                .userId(userId)
                .tenantId(tenantId)
                .createDate(now)
                .updateDate(now)
                .isDeleted(0)
                .build();
        sceneSliceService.save(entity);
        return entity;
    }

    @Override
    public void updateAnalysisStartTime(Long id) {
        LambdaUpdateWrapper<SceneSliceEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SceneSliceEntity::getId, id)
                .set(SceneSliceEntity::getAnalysisStartTime, new Date())
                .set(SceneSliceEntity::getUpdateDate, new Date());
        sceneSliceService.update(wrapper);
    }

    @Override
    public void updateAiResult(Long id, String aiResult) {
        LambdaUpdateWrapper<SceneSliceEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SceneSliceEntity::getId, id)
                .set(SceneSliceEntity::getAiResult, aiResult)
                .set(SceneSliceEntity::getStatus, AiEnums.sceneSliceStatus.COMPLETED.getCode())
                .set(SceneSliceEntity::getAnalysisTime, new Date())
                .set(SceneSliceEntity::getUpdateDate, new Date());
        sceneSliceService.update(wrapper);
    }

    @Override
    public void updateFail(Long id, String failReason) {
        LambdaUpdateWrapper<SceneSliceEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SceneSliceEntity::getId, id)
                .set(SceneSliceEntity::getFailReason, failReason)
                .set(SceneSliceEntity::getStatus, AiEnums.sceneSliceStatus.FAILED.getCode())
                .set(SceneSliceEntity::getUpdateDate, new Date());
        sceneSliceService.update(wrapper);
    }

    @Override
    public SceneSliceEntity getByVideoId(String videoId, Long tenantId) {
        LambdaQueryWrapper<SceneSliceEntity> wrapper = new LambdaQueryWrapper<SceneSliceEntity>()
                .eq(SceneSliceEntity::getVideoId, videoId)
                .eq(SceneSliceEntity::getTenantId, tenantId)
                .eq(SceneSliceEntity::getIsDeleted, 0)
                .eq(SceneSliceEntity::getStatus, AiEnums.sceneSliceStatus.COMPLETED.getCode())
                .orderByDesc(SceneSliceEntity::getCreateDate)
                .last("LIMIT 1");
        return sceneSliceService.getOne(wrapper);
    }

    /**
     * 根据视频ID查询场景解析列表（已完成状态）
     *
     * @param videoId
     * @param tenantId
     */
    @Override
    public List<SceneSliceVo> listStuckProcessing(String videoId, long tenantId) {
        return sceneSliceService.lambdaQuery()
            .eq(SceneSliceEntity::getVideoId, videoId)
            .eq(SceneSliceEntity::getTenantId, tenantId)
            .eq(SceneSliceEntity::getStatus, AiEnums.sceneSliceStatus.COMPLETED.getCode())
            .eq(SceneSliceEntity::getIsDeleted, 0)
            .orderByDesc(SceneSliceEntity::getId)
            .last("limit 100")
            .list().stream().map(entity -> {
                SceneSliceVo vo = new SceneSliceVo();
                vo.setVideoId(entity.getVideoId());
                vo.setSliceSeconds(entity.getSliceSeconds());
                vo.setAiResult(entity.getAiResult());
                return vo;
            }).toList();
    }

    @Override
    public List<SceneSliceEntity> listStuckProcessing(int timeoutMinutes) {
        Date cutoff = Date.from(LocalDateTime.now().minusMinutes(timeoutMinutes)
                .atZone(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<SceneSliceEntity> wrapper = new LambdaQueryWrapper<SceneSliceEntity>()
                .eq(SceneSliceEntity::getStatus, AiEnums.sceneSliceStatus.PROCESSING.getCode())
                .eq(SceneSliceEntity::getIsDeleted, 0)
                .lt(SceneSliceEntity::getUpdateDate, cutoff);
        return sceneSliceService.list(wrapper);
    }

    private int getSliceSeconds() {
        Integer seconds = systemKvProducer.getValueByKey("scene_slice_default_seconds", 10);
        return ObjectUtil.defaultIfNull(seconds, 10);
    }
}
