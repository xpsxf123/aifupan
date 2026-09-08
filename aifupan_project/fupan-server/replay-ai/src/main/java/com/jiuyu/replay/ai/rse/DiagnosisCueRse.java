package com.jiuyu.replay.ai.rse;

import com.jiuyu.replay.ai.bo.SaveDiagnosisBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueListBo;
import com.jiuyu.replay.generic.bo.ai.SaveDiagnosisCueBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueListVo;
import com.jiuyu.replay.generic.vo.ai.UnreadDiagnosisReportVo;

import java.util.List;


/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
public interface DiagnosisCueRse {


    /**
     * ai诊断提示词配置列表
     * @param diagnosisCueListBo ai诊断提示词配置列表查询参数
     * @return
     */
    PageUtils<DiagnosisCueListVo> queryPage(DiagnosisCueListBo diagnosisCueListBo);

    /**
    * ai诊断提示词配置信息
    * @param id ai诊断提示词配置id
    * @return
    */
    DiagnosisCueInfoVo info(Long id);

    /**
     * 新增ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
     DiagnosisCueInfoVo save(DiagnosisCueBo diagnosisCueBo);

    /**
     * 修改ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    void update(DiagnosisCueBo diagnosisCueBo);

    /**
     * 删除ai诊断提示词配置
     * @param id ai诊断提示词配置id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取ai诊断提示词配置列表
     *
     * @param sourceId
     * @param sourceType
     * @param diagnosisType
     * @return
     */
    List<DiagnosisCueInfoVo> listDiagnosis(String sourceId, Integer sourceType, Integer diagnosisType, Long tenantId, Long userId);

    /**
     * 保存ai诊断提示词配置
      * @param saveDiagnosisCueBo
     */
    void saveDiagnosisCue(SaveDiagnosisCueBo saveDiagnosisCueBo);

    /**
     * 获取要自动提问的诊断问题
     * @param videoId
     * @param secUid
     * @return
     */
    List<DiagnosisCueInfoVo> setAutoDiagnosisQuestions(String videoId, String secUid);

    /**
     * 设置自动数据诊断问题
     *
     * @param videoId     视频id
     * @param secUid      用户id
     * @param cueWordsIds 问题ids
     * @return 要生成的问题
     */
    List<DiagnosisCueInfoVo> setAutoDataDiagnosisQuestions(String videoId, String secUid, List<Long> cueWordsIds);


    /**
     * 查询视频是否需要生成诊断报告
     *
     * @param sourceId
     * @param diagnosisType
     * @return
     */
    String isGenerateDiagnosisFile(String sourceId, Integer diagnosisType);

    /**
     * 保存诊断报告
     *
     * @param saveDiagnosisBo
     * @return
     */
    List<DiagnosisCueInfoVo> saveDiagnosis(SaveDiagnosisBo saveDiagnosisBo);

    /**
     * 获取用户待分析和分析中的诊断报告
     * @return
     */
    List<DiagnosisCueInfoVo> handleDiagnosisByUser();

    /**
     * 根据id获取问答记录
     * @param sourceIds
     * @param sourceType
     * @param userId
     * @param tenantId
     * @return
     */
    List<DiagnosisCueInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId);

    /**
     * 处理诊断报告
     *
     * @param sourceId
     * @param sourceType
     */
    void handleDiagnosis(String sourceId, Integer sourceType);

    /**
     * 删除对应的诊断报告和模型
     *
     * @param cueBo
     */
    void deleteBySourceId(SaveDiagnosisCueBo cueBo);

    /**
     * 批量删除诊断报告
     *
     * @param ids 诊断报告id
     */
    void deleteBatch(List<Long> ids);

    /**
     * 批量更新已读状态
     *
     * @param ids    诊断报告id列表
     * @param isRead 是否已读 0未读 1已读
     */
    void updateReadStatus(List<Long> ids, Integer isRead);

    /**
     * 根据 sourceId + cueWordsIds 批量更新已读状态
     *
     * @param sourceId    来源id
     * @param cueWordsIds 提示词id列表
     * @param userId      用户id
     * @param tenantId    租户id
     * @param isRead      是否已读
     */
    void updateReadStatusBySourceAndCueWords(String sourceId, List<Long> cueWordsIds, Long userId, Long tenantId, Integer isRead);

    /**
     * 查询未读的数据诊断报告
     *
     * @param userId   用户id
     * @param tenantId 租户id
     * @return 未读数据诊断记录列表
     */
    List<UnreadDiagnosisReportVo> listUnreadDataDiagnosis(Long userId, Long tenantId, Integer videoSliceType);
}

