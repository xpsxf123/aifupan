package com.jiuyu.governance.business.rbac.controller;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeMobileLoginRequest;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeMobileRequest;
import com.jiuyu.governance.business.rbac.pojo.request.EmployeeOauthPasswordLoginRequest;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeOauthResponse;
import com.jiuyu.governance.business.rbac.service.impl.EmployeeOauthManage;
import com.jiuyu.governance.common.utils.RequestUtil;
import com.jiuyu.governance.plugins.oauth.ClientUser;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 企业端 - 登录API
 *
 * @author HeHui
 * @date 2026-03-24 17:07
 */
@RestController
@RequestMapping("/api/governance/oauth")
@RequiredArgsConstructor
public class EmployeeOauthController {

    private final EmployeeOauthManage oauthManage;


    /**
     * 手机号码 + 密码登录
     *
     * @param request 请求
     *
     * @return {@link ApiResponse }<{@link EmployeeOauthResponse }>
     */
    @PostMapping("/password")
    public ApiResponse<EmployeeOauthResponse> passwordLogin(@RequestBody @Validated EmployeeOauthPasswordLoginRequest request, HttpServletRequest servletRequest, @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) {
        return oauthManage.passwordLogin(request, RequestUtil.getIp(servletRequest), userAgent);
    }


    /**
     * 获取登录验证码
     *
     * @param request 手机号码
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @PostMapping("/code")
    public ApiResponse<Void> sendCode(@RequestBody @Validated EmployeeMobileRequest request) {
        return oauthManage.sendCode(request.getMobile());
    }


    /**
     * 手机号码 + 验证码登录
     *
     * @param request 手机号码
     *
     * @return {@link ApiResponse }<{@link EmployeeOauthResponse }>
     */
    @PostMapping("/code-login")
    public ApiResponse<EmployeeOauthResponse> codeLogin(@RequestBody @Validated EmployeeMobileLoginRequest request,HttpServletRequest servletRequest, @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) {
        return oauthManage.codeLogin(request.getMobile(), request.getCode(), RequestUtil.getIp(servletRequest), userAgent);
    }



    /**
     * 客户端认证登录
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link EmployeeOauthResponse }>
     */
    @PostMapping("/client-auth")
    @ClientUser
    public ApiResponse<EmployeeOauthResponse> clientAuth(AccessUser accessUser, HttpServletRequest servletRequest, @RequestHeader(HttpHeaders.USER_AGENT) String userAgent) {
        return oauthManage.clientAuth(accessUser.userId(), accessUser.currentTenantId(), RequestUtil.getIp(servletRequest), userAgent);
    }

    /**
     * 登出
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @GetMapping("/logout")
    @GovernanceUser
    public ApiResponse<Void> logout(AccessUser accessUser) {
        return oauthManage.logout(accessUser);
    }

}
