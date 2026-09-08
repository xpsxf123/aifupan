package com.jiuyu.governance.openfeign.replay.impl;

import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.AnchorInfoService;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.request.TenantAnchorQueryRequest;
import com.jiuyu.governance.openfeign.replay.response.AnchorVideoInfoResponse;
import com.jiuyu.governance.openfeign.replay.response.TenantAnchorInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  主播服务
 * @author HeHui
 * @date 2026-04-24 19:01
 */
@Service
@Slf4j
public class AnchorInfoServiceImpl implements AnchorInfoService {

    private final ReplayHttpServer replayHttpServer;

    public AnchorInfoServiceImpl(ReplayHttpServer replayHttpServer) {
        this.replayHttpServer = replayHttpServer;
    }

    /**
     * 获取租户主播信息
     *
     * @param queryRequest 查询参数
     *
     * @return {@link List<TenantAnchorInfoResponse>}
     */
    @Override
    public List<TenantAnchorInfoResponse> getTenantAnchors(TenantAnchorQueryRequest queryRequest) {
        ApiResponse<List<TenantAnchorInfoResponse>> apiResponse = replayHttpServer.post("/replay/openapi/governance/tenant/all-anchor-info", queryRequest, null)
            .retrieve()
            .body(ReplayApiResponseType.TENANT_ANCHOR_INFO_LIST_RESPONSE_TYPE);
        if (apiResponse == null) {
            log.error("[主播] query tenant all anchors error, response is null, body: {}", JsonTemplate.toJson(queryRequest));
            return List.of();
        }
        if (apiResponse.failed()) {
            log.error("[主播] query tenant all anchors error, result error, code: {}, msg: {}, body: {}", apiResponse.getCode(), apiResponse.getMsg(), JsonTemplate.toJson(queryRequest));
            return List.of();
        }
        return apiResponse.getData();
    }

    /**
     * 根据视频ID获取视频完整信息
     *
     * @param videoId 爱复盘视频唯一标识
     * @return 视频信息（含 tenantId 等全部字段），不存在返回 null
     */
    @Override
    public AnchorVideoInfoResponse getVideoByVideoId(String videoId) {
        ApiResponse<AnchorVideoInfoResponse> apiResponse = replayHttpServer
                .get("/replay/openapi/governance/video/infoByVideoId?videoId=" + videoId, null)
                .retrieve()
                .body(ReplayApiResponseType.ANCHOR_VIDEO_INFO_TYPE);
        if (apiResponse == null || apiResponse.failed() || apiResponse.getData() == null) {
            log.warn("[主播] 根据videoId查询视频信息失败, videoId={}", videoId);
            return null;
        }
        return apiResponse.getData();
    }
}
