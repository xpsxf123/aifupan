package com.jiuyu.replay.third.governance;

import com.jiuyu.replay.common.http.governance.GovernanceHttpServer;
import com.jiuyu.replay.generic.bo.governance.GovernanceAddAnchorBo;
import com.jiuyu.replay.generic.feign.third.GovernanceLiveRoomService;
import com.jiuyu.replay.generic.vo.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 企业管理服务端-直播间主播服务实现
 * 通过 GovernanceHttpServer 调用企业管理服务端 REST 接口
 */
@Slf4j
@Service
public class GovernanceLiveRoomServiceImpl implements GovernanceLiveRoomService {

    private final GovernanceHttpServer httpServer;

    private final ParameterizedTypeReference<R<Void>> voidType = new ParameterizedTypeReference<R<Void>>() {
    };

    public GovernanceLiveRoomServiceImpl(GovernanceHttpServer httpServer) {
        this.httpServer = httpServer;
    }

    /**
     * 通知企业管理服务端添加主播
     *
     * @param bo 添加主播请求参数
     * @return 调用结果
     */
    @Override
    public R<Void> addAnchor(GovernanceAddAnchorBo bo) {
        Map<String, Object> body = new HashMap<>();
        body.put("secUid", bo.getSecUid());
        body.put("anchorName", bo.getAnchorName());
        if (bo.getPlatform() != null) {
            body.put("platform", bo.getPlatform());
        }
        if (bo.getAnchorNumber() != null) {
            body.put("anchorNumber", bo.getAnchorNumber());
        }
        if (bo.getHomeUrl() != null) {
            body.put("homeUrl", bo.getHomeUrl());
        }
        if (bo.getLiveUrl() != null) {
            body.put("liveUrl", bo.getLiveUrl());
        }
        if (bo.getAnchorAvatar() != null) {
            body.put("anchorAvatar", bo.getAnchorAvatar());
        }
        if (bo.getTradeId() != null) {
            body.put("tradeId", bo.getTradeId());
        }
        return httpServer.post("/api/governance/client/live-room/anchor", body, null)
                .retrieve().body(voidType);
    }
}
