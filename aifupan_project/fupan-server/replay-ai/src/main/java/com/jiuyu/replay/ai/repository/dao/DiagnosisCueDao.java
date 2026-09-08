package com.jiuyu.replay.ai.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.ai.entity.DiagnosisCueEntity;
import com.jiuyu.replay.generic.vo.ai.UnreadDiagnosisReportVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ai诊断提示词配置
 *
 * @author lj
 * @email
 * @date 2025-05-19 14:02:59
 */
@Mapper
public interface DiagnosisCueDao extends BaseMapper<DiagnosisCueEntity> {

    List<UnreadDiagnosisReportVo> listUnreadDataDiagnosis(@Param("userId") Long userId,
                                                          @Param("tenantId") Long tenantId,
                                                          @Param("videoSliceType") Integer videoSliceType);
}
