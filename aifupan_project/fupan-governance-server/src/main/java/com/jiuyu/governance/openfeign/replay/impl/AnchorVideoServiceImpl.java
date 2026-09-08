package com.jiuyu.governance.openfeign.replay.impl;

import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.AnchorVideoService;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.request.VideoListRequest;
import com.jiuyu.governance.openfeign.replay.response.AnchorVideoListItem;
import com.jiuyu.governance.openfeign.replay.response.ReplayCursorPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *  主播场次视频服务 实现
 * @author HeHui
 * @date 2026-08-04 16:53
 */
@Service
@Slf4j
public class AnchorVideoServiceImpl implements AnchorVideoService {

    private final ReplayHttpServer replayHttpServer;

    public AnchorVideoServiceImpl(ReplayHttpServer replayHttpServer) {
        this.replayHttpServer = replayHttpServer;
    }

    /**
     * 获取主播场次视频列表
     *
     * @param request 请求参数
     *
     * @return 响应结果
     */
    @Override
    public ReplayCursorPage<AnchorVideoListItem> queryVideoList(VideoListRequest request) {
        ApiResponse<ReplayCursorPage<AnchorVideoListItem>> apiResponse = replayHttpServer.post("/internal/ai-agent/video/list", request, null)
            .retrieve()
            .body(ReplayApiResponseType.VIDEO_LIST_RESPONSE);
        if (apiResponse == null) {
            log.error("[主播] query video list error, response is null, body: {}", JsonTemplate.toJson(request));
            return new ReplayCursorPage<>();
        }
        if (apiResponse.failed()) {
            log.error("[主播] query video list error, result error, code: {}, msg: {}, body: {}", apiResponse.getCode(), apiResponse.getMsg(), JsonTemplate.toJson(request));
            return new ReplayCursorPage<>();
        }
        return apiResponse.getData();
    }
}
