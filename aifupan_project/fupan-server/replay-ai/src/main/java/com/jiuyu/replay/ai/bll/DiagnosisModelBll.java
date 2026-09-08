package com.jiuyu.replay.ai.bll;

import com.jiuyu.replay.ai.rse.DiagnosisModelRse;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelListBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelListVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * ai诊断中的模型设置-主播和视频
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Component
@AllArgsConstructor
public class DiagnosisModelBll {

    private final DiagnosisModelRse diagnosisModelProducer;
    private final UserFeign userFeign;


    /**
     * ai诊断中的模型设置-主播和视频列表
     * @param diagnosisModelListBo ai诊断中的模型设置-主播和视频列表查询参数
     * @return
     */
    public R<PageUtils<DiagnosisModelListVo>> queryPage(DiagnosisModelListBo diagnosisModelListBo) {

        return R.ok("获取成功", diagnosisModelProducer.queryPage(diagnosisModelListBo));
    }

    /**
    * ai诊断中的模型设置-主播和视频信息
    * @param id ai诊断中的模型设置-主播和视频id
    * @return
    */
    public R<DiagnosisModelInfoVo> info(Long id) {

        DiagnosisModelInfoVo diagnosisModelInfoVo = diagnosisModelProducer.info(id);
        return R.ok("获取成功", diagnosisModelInfoVo);
    }

    /**
     * 新增ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    public R<String> save(DiagnosisModelBo diagnosisModelBo) {

        DiagnosisModelInfoVo diagnosisModelInfoVo = diagnosisModelProducer.save(diagnosisModelBo);
        return R.ok("添加成功");
    }

    /**
     * 修改ai诊断中的模型设置-主播和视频
     * @param diagnosisModelBo ai诊断中的模型设置-主播和视频对象
     * @return
     */
    public R<String> update(DiagnosisModelBo diagnosisModelBo) {

        diagnosisModelProducer.update(diagnosisModelBo);
        return R.ok("修改成功");
    }

    /**
     * 删除ai诊断中的模型设置-主播和视频
     * @param id ai诊断中的模型设置-主播和视频id
     * @return
     */
    public R<String> delete(Long id) {

        diagnosisModelProducer.deleteById(id);
        return R.ok("删除成功");
    }


    public R<String> saveOrUpdate(DiagnosisModelBo diagnosisModelBo) {
        diagnosisModelProducer.saveOrUpdate(diagnosisModelBo);
        return R.ok("编辑成功");
    }

    public DiagnosisModelInfoVo getBySource(String sourceId, Integer sourceType, Long activeTenantId, Long userId, int diagnosisType) {
        return diagnosisModelProducer.getBySource(sourceId, sourceType, activeTenantId, userId, diagnosisType);
    }

    public List<DiagnosisModelInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId) {
        return diagnosisModelProducer.listBySourceIds(sourceIds, sourceType, userId, tenantId);
    }

    /**
     * 更新诊断报告使用的模型
     * @param diagnosisModelBo
     */
    public void updateDiagnosisModel(DiagnosisModelBo diagnosisModelBo) {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");
        diagnosisModelBo.setUserId(user.getId());
        diagnosisModelBo.setTenantId(user.getActiveTenantId());
        if (diagnosisModelBo.getDiagnosisType() == null) {
            diagnosisModelBo.setDiagnosisType(AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode());
        }
        diagnosisModelProducer.updateDiagnosisModel(diagnosisModelBo);
    }
}

