package com.jiuyu.governance.openfeign.replay;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.openfeign.replay.response.SystemKvResponse;

/**
 * 系统服务接口
 *
 * @author lujie
 * @date 2026/4/23 14:55
 */
public interface SystemService {

    /**
     * 根据 key 获取系统键值对
     *
     * @param key 键
     *
     * @return {@link ApiResponse }<{@link SystemKvResponse }>
     */
    ApiResponse<SystemKvResponse> getByKey(String key);
}
