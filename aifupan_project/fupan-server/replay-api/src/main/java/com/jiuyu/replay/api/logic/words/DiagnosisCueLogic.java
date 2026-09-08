package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.bo.ai.DiagnosisCueBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisCueListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueListVo;
import com.jiuyu.replay.generic.vo.common.R;


/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
public interface DiagnosisCueLogic {


    /**
     * ai诊断提示词配置列表
     * @param diagnosisCueListBo ai诊断提示词配置列表查询参数
     * @return
     */
    R<PageUtils<DiagnosisCueListVo>> queryPage(DiagnosisCueListBo diagnosisCueListBo);

    /**
    * ai诊断提示词配置信息
    * @param id ai诊断提示词配置id
    * @return
    */
    R<DiagnosisCueInfoVo> info(Long id);

    /**
     * 新增ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    R<String> save(DiagnosisCueBo diagnosisCueBo);

    /**
     * 修改ai诊断提示词配置
     * @param diagnosisCueBo ai诊断提示词配置对象
     * @return
     */
    R<String> update(DiagnosisCueBo diagnosisCueBo);

    /**
     * 删除ai诊断提示词配置
     * @param id ai诊断提示词配置id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 更新ai诊断提示词状态
     *
     * @param diagnosisCueBo
     * @return
     */
    R<String> updateDiagnosisCueStatus(DiagnosisCueBo diagnosisCueBo);
}

