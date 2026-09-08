package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelListBo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelListVo;


/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
public interface DiagnosisModelLogic {


    /**
     * ai诊断中的模型设置-主播和视频列表
     * @param diagnosisModelListBo ai诊断中的模型设置-主播和视频列表查询参数
     * @return
     */
    R<PageUtils<DiagnosisModelListVo>> queryPage(DiagnosisModelListBo diagnosisModelListBo);

    /**
    * ai诊断中的模型设置-主播和视频信息
    * @param id ai诊断中的模型设置-主播和视频id
    * @return
    */
    R<DiagnosisModelInfoVo> info(Long id);

    /**
     * 新增ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    R<String> save(DiagnosisModelBo diagnosisModelBo);

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    R<String> update(DiagnosisModelBo diagnosisModelBo);

    /**
     * 删除ai诊断中的模型设置-主播和视频
     * @param id ai诊断中的模型设置-主播和视频id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 新增或修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo
     * @return
     */
    R<String> saveOrUpdate(DiagnosisModelBo diagnosisModelBo);
}

