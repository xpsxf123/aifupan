package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelListBo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelListVo;

import java.util.List;


/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
public interface DiagnosisModelRse {


    /**
     * ai诊断中的模型设置-主播和视频列表
     * @param diagnosisModelListBo ai诊断中的模型设置-主播和视频列表查询参数
     * @return
     */
    PageUtils<DiagnosisModelListVo> queryPage(DiagnosisModelListBo diagnosisModelListBo);

    /**
    * ai诊断中的模型设置-主播和视频信息
    * @param id ai诊断中的模型设置-主播和视频id
    * @return
    */
    DiagnosisModelInfoVo info(Long id);

    /**
     * 新增ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
     DiagnosisModelInfoVo save(DiagnosisModelBo diagnosisModelBo);

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    void update(DiagnosisModelBo diagnosisModelBo);

    /**
     * 删除ai诊断中的模型设置-主播和视频
     * @param id ai诊断中的模型设置-主播和视频id
     * @return
     */
    void deleteById(Long id);

    /**
     * 新增或修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo
     */
    void saveOrUpdate(DiagnosisModelBo diagnosisModelBo);

    /**
     * 根据来源id、来源类型、租户id、用户id获取ai诊断中的模型设置-主播和视频
     *
     * @param sourceId
     * @param sourceType
     * @param activeTenantId
     * @param userId
     * @param diagnosisType
     * @return
     */
    DiagnosisModelInfoVo getBySource(String sourceId, Integer sourceType, Long activeTenantId, Long userId, int diagnosisType);

    List<DiagnosisModelInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId);

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo
     */
    void updateDiagnosisModel(DiagnosisModelBo diagnosisModelBo);

    /**
     * 新增视频的ai诊断中的模型设置
     *
     * @param secUid
     * @param videoId
     * @param id
     * @param activeTenantId
     */
    void addVideoDiagnosisModel(String secUid, String videoId, Long id, Long activeTenantId, Integer diagnosisType);
}

