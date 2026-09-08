package com.jiuyu.replay.ai.api;

import com.jiuyu.replay.ai.bll.DiagnosisCueBll;
import com.jiuyu.replay.generic.feign.ai.DiagnosisCueFeign;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DiagnosisCueFeign 实现
 */
@Service
@AllArgsConstructor
public class DiagnosisCueApi implements DiagnosisCueFeign {

    private final DiagnosisCueBll diagnosisCueBll;

    @Override
    public List<DiagnosisCueInfoVo> listBySourceIds(List<String> sourceIds, int sourceType, Long userId, Long tenantId) {
        return diagnosisCueBll.listBySourceIds(sourceIds, sourceType, userId, tenantId);
    }
}
