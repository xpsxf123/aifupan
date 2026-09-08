package com.jiuyu.governance.business;

import com.jiuyu.framework.oauth.client.annotation.Anonymous;
import com.jiuyu.framework.oauth.client.provides.AuthenticationTokenProvide;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统检测API
 *
 * @author HeHui
 * @date 2026-04-14 16:29
 */
@RestController
@RequestMapping("/api/governance/check")
public class SystemCheckController {


    private final AuthenticationTokenProvide authenticationTokenProvide;

    public SystemCheckController(AuthenticationTokenProvide authenticationTokenProvide) {
        this.authenticationTokenProvide = authenticationTokenProvide;
    }

    /**
     * 服务检测
     *
     * @return 响应结果
     */
    @GetMapping("/health")
    @Anonymous
    public ApiResponse<Void> health() {
        return ApiResponse.success();
    }


    /**
     * 租户检测
     *  判断租户是否可用
     * @param accessToken 访问令牌
     * @return 响应结果
     */
    @Anonymous
    @GetMapping("/tenant")
    public ApiResponse<Boolean> tenantCheck(@RequestHeader(value = "Token", required = false) String accessToken) {
        if (EmptyUtil.isEmpty(accessToken)) {
            return ApiResponse.success(false);
        }
        return ApiResponse.success(authenticationTokenProvide.effective(accessToken));
    }
}
