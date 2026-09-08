package com.jiuyu.governance.business.performance.pojo.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

/**
 * 客户端批量推送直播业绩数据请求
 *
 * @author lj
 * @date 2026-05-29
 */
@Getter
@Setter
@NoArgsConstructor
public class ClientPushVideoListRequest extends ClientPushVideoRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标租户ID列表
     */
    //@NotEmpty(message = "租户ID列表不能为空")
    private List<Long> tenantIds;
}
