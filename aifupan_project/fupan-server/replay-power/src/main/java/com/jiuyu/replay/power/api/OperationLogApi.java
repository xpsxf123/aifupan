package com.jiuyu.replay.power.api;

import com.jiuyu.replay.common.diff.Business;
import com.jiuyu.replay.common.producer.OperationLogProducer;
import com.jiuyu.replay.generic.feign.common.OperationLogFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;


import java.util.Map;

@Component
@AllArgsConstructor
public class OperationLogApi implements OperationLogFeign {


    @Resource
    private OperationLogProducer operationLogProducer;

    /**
     * 操作记录保存
     *
     * @param businessId
     * @param userId
     * @param allBeforeMap
     * @param allAfterMap
     *
     */
    @Override
    public void saveOptLog(String businessName,Long businessId, Long userId, Map<String, Object> allBeforeMap
            , Map<String, Object> allAfterMap,Long optUserId, String ip,String optName) {
        operationLogProducer.saveOptLog(businessName,businessId, userId
                , allBeforeMap, allAfterMap, optUserId, ip,optName);
    }
}
