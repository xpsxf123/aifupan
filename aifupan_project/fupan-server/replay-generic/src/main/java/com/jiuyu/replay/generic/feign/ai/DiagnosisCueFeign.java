package com.jiuyu.replay.generic.feign.ai;

import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;

import java.util.List;

/**
 * 诊断提示词 Feign 接口
 */
public interface DiagnosisCueFeign {

    /**
     * 根据来源id列表批量查询诊断记录
     *
     * @param sourceIds  来源id列表
     * @param sourceType 来源类型
     * @param userId     用户id
     * @param tenantId   租户id
     * @return 诊断记录列表
     */
    List<DiagnosisCueInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId);
}
