package com.jiuyu.replay.generic.feign.third;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.governance.GovernanceProductItemVo;

import java.util.List;

/**
 * 企业后台（governance）商品查询服务接口
 *
 * @author jy
 * @date 2026-08-06
 */
public interface GovernanceProductService {

    /**
     * 查询本场直播商品数据（最多50条，按sort升序）
     *
     * @param batchNumber 直播场次批次号
     * @param tenantId    租户ID
     * @param startTime   视频开始时间（yyyy-MM-dd HH:mm:ss）
     * @param endTime     视频结束时间（yyyy-MM-dd HH:mm:ss）
     * @return 商品列表
     */
    R<List<GovernanceProductItemVo>> queryProductPage(String batchNumber, Long tenantId,
                                                       String startTime, String endTime);
}
