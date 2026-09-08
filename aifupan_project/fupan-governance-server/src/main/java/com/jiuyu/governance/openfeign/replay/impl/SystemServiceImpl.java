package com.jiuyu.governance.openfeign.replay.impl;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.SystemService;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.response.SystemKvResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统服务实现
 *
 * @author lujie
 * @date 2026/4/23
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final ReplayHttpServer replayHttpServer;

    /**
     * 根据 key 获取系统键值对
     *
     * @param key 键
     *
     * @return {@link ApiResponse }<{@link SystemKvResponse }>
     */
    @Override
    public ApiResponse<SystemKvResponse> getByKey(String key) {
        return replayHttpServer.get("/replay/openapi/governance/system/getByKey?key=" + key, null)
            .retrieve()
            .body(ReplayApiResponseType.SYSTEM_KV_RESPONSE_TYPE);
    }
}
