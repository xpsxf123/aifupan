package com.jiuyu.replay.generic.feign.common;

import com.jiuyu.replay.generic.vo.power.UserCacheVo;

import java.util.Map;

public interface OperationLogFeign {

    /**
     * 操作记录保存
     * @param businessId
     * @param userId
     * @param allBeforeMap
     * @param allAfterMap
     *
     */
    void saveOptLog(String businessName,Long businessId, Long userId, Map<String, Object> allBeforeMap
            , Map<String, Object> allAfterMap,Long optUserId, String ip,String optName);
}
