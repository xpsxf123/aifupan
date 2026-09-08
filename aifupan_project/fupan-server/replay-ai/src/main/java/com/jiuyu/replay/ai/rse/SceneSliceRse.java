package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.ai.bo.SaveSceneSliceBo;
import com.jiuyu.replay.ai.entity.SceneSliceEntity;
import com.jiuyu.replay.ai.vo.SceneSliceStatusVo;
import com.jiuyu.replay.generic.vo.aiagent.SceneSliceVo;

import java.util.List;

/**
 * 场景切片 RSE
 *
 * @author lj
 * @date 2026-07-06
 */
public interface SceneSliceRse {

    /**
     * 查询场景切片状态（不创建记录）
     */
    SceneSliceStatusVo getStatus(String videoId, Long userId, Long tenantId);

    /**
     * 创建场景切片记录（状态置为处理中）
     */
    SceneSliceEntity createRecord(SaveSceneSliceBo bo, Long userId, Long tenantId);

    /**
     * 更新AI分析开始时间
     */
    void updateAnalysisStartTime(Long id);

    /**
     * 更新AI分析结果
     */
    void updateAiResult(Long id, String aiResult);

    /**
     * 更新为失败状态
     */
    void updateFail(Long id, String failReason);

    /**
     * 根据视频ID查询（供Placeholder解析使用）
     */
    SceneSliceEntity getByVideoId(String videoId, Long tenantId);


    /**
     * 根据视频ID查询场景解析列表（已完成状态）
     */
    List<SceneSliceVo> listStuckProcessing(String videoId, long tenantId);

    /**
     * 查询处理中且超时的记录（供XXL-Job兜底）
     */
    java.util.List<SceneSliceEntity> listStuckProcessing(int timeoutMinutes);
}
