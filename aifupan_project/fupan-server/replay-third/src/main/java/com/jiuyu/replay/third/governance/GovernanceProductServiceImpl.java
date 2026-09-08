package com.jiuyu.replay.third.governance;

import com.jiuyu.replay.common.http.governance.GovernanceHttpServer;
import com.jiuyu.replay.generic.feign.third.GovernanceProductService;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.governance.GovernanceProductItemVo;
import com.jiuyu.replay.generic.vo.governance.GovernanceProductPageResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业后台（governance）商品查询服务实现
 * 通过 GovernanceHttpServer 调用企业管理服务端 REST 接口
 *
 * @author jy
 * @date 2026-08-06
 */
@Slf4j
@Service
public class GovernanceProductServiceImpl implements GovernanceProductService {

    private final GovernanceHttpServer httpServer;

    /** 分页响应类型 */
    private final ParameterizedTypeReference<R<GovernanceProductPageResponseVo>> pageType =
            new ParameterizedTypeReference<R<GovernanceProductPageResponseVo>>() {
            };

    public GovernanceProductServiceImpl(GovernanceHttpServer httpServer) {
        this.httpServer = httpServer;
    }

    /**
     * 查询本场直播商品数据（最多50条，按sort升序）
     *
     * @param batchNumber 直播场次批次号
     * @param tenantId    租户ID
     * @param startTime   视频开始时间（yyyy-MM-dd HH:mm:ss）
     * @param endTime     视频结束时间（yyyy-MM-dd HH:mm:ss）
     * @return 商品列表
     */
    @Override
    public R<List<GovernanceProductItemVo>> queryProductPage(String batchNumber, Long tenantId,
                                                              String startTime, String endTime) {
        Map<String, Object> body = new HashMap<>();
        body.put("batchNumber", batchNumber);
        body.put("page", 1);
        body.put("limit", 50);
        body.put("sortBy", "sort");
        body.put("sortOrder", "asc");
        if (tenantId != null) {
            body.put("tenantId", tenantId);
        }
        if (StrUtil.isNotBlank(startTime)) {
            body.put("startTime", startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            body.put("endTime", endTime);
        }

        R<GovernanceProductPageResponseVo> result = httpServer
                .post("/api/governance/performance/video/productPageApi", body, null)
                .retrieve()
                .body(pageType);

        if (result != null && result.success() && result.getData() != null) {
            List<GovernanceProductItemVo> records = result.getData().getList();
            if (records != null) {
                return R.ok(records);
            }
        }
        return R.ok(Collections.emptyList());
    }
}
