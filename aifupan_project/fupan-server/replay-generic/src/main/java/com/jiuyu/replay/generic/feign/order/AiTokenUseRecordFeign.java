package com.jiuyu.replay.generic.feign.order;


import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;

/**
 * @author lyw
 */
public interface AiTokenUseRecordFeign {


    /**
     * 新增ai的token使用记录
     *
     * @param aiTokenUseRecordBo
     */
    void saveAiTokenUseRec(AiTokenUseRecordBo aiTokenUseRecordBo);

    /**
     * 根据请求id获取ai的token使用记录
     *
     * @param requestId 请求id
     * @return ai的token使用记录
     */
    AiTokenUseRecordInfoVo getByRequestId(String requestId);
}
