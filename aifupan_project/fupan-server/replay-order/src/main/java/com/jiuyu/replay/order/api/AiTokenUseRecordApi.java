package com.jiuyu.replay.order.api;

import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.order.AiTokenUseRecordFeign;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.order.producer.AiTokenUseRecordProducer;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author lyw
 */
@Component
@AllArgsConstructor()
public class AiTokenUseRecordApi implements AiTokenUseRecordFeign {

    @Resource
    private AiTokenUseRecordProducer aiTokenUseRecordProducer;

    @Override
    public void saveAiTokenUseRec(AiTokenUseRecordBo aiTokenUseRecordBo) {
        aiTokenUseRecordProducer.save(aiTokenUseRecordBo);

    }

    @Override
    public AiTokenUseRecordInfoVo getByRequestId(String requestId) {
        return aiTokenUseRecordProducer.getByRequestId(requestId);
    }
}
