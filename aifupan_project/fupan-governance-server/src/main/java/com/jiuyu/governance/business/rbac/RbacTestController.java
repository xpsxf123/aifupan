package com.jiuyu.governance.business.rbac;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.client.annotation.RequiredLogin;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.plugins.oauth.ClientUser;
import com.jiuyu.governance.plugins.oauth.GovernanceUser;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import com.jiuyu.governance.plugins.oauth.SystemUser;
import com.jiuyu.governance.business.rbac.pojo.entity.Menu;
import com.jiuyu.governance.business.rbac.pojo.request.TestPageQueryRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 测试
 *  示范中所有注解都支持添加到类上
 *  优先取方法上的，方法没有才会取类上的
 * @author HeHui
 * @date 2026-03-17 16:24
 */
//@RestController
//@RequestMapping("/api/governance/test")
public class RbacTestController {


    /**
     * 测试 限制只允许爱复盘后台管理用户进入
     *
     * @param accessUser 访问用户信息
     * @return ApiResponse<AccessUser>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @SystemUser
    @GetMapping("/test1")
    public ApiResponse<AccessUser> test1(AccessUser accessUser) {
        return ApiResponse.success(accessUser);
    }

    /**
     * 测试 限制只允许客户端用户进入
     *
     * @param accessUser 访问用户信息
     * @return ApiResponse<AccessUser>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @ClientUser
    @GetMapping("/test2")
    public ApiResponse<AccessUser> test2(AccessUser accessUser) {
        return ApiResponse.success(accessUser);
    }

    /**
     * 测试 限制只允许企业管理后台用户进入
     *
     * @param accessUser 访问用户信息
     * @return ApiResponse<AccessUser>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @GovernanceUser
    @GetMapping("/test3")
    public ApiResponse<AccessUser> test3(AccessUser accessUser) {
        return ApiResponse.success(accessUser);
    }


    /**
     * 测试 登录用户权限校验 任何用户都可以访问
     *
     * @return ApiResponse<Void>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @RequiredLogin
    @GetMapping("/test4")
    public ApiResponse<AccessUser> test4(AccessUser accessUser) {
        return ApiResponse.success(accessUser);
    }

    /**
     * 测试 限制只能企业后台管理用户和客户端用户进入
     *
     * @return ApiResponse<Void>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @RequiredLogin({OauthConstant.GOVERNANCE_USER, OauthConstant.CLIENT_USER})
    @GetMapping("/test5")
    public ApiResponse<AccessUser> test5(AccessUser accessUser) {
        return ApiResponse.success(accessUser);
    }


    /**
     * 测试 登录用户权限校验
     *  Permissions 验证的是操作权限 自动根据不同的用户类型去执行不同的权限验证逻辑
     * @return ApiResponse<Void>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @RequiredLogin
    @GetMapping("/test6")
    public ApiResponse<AccessUser> test6(AccessUser accessUser) {
        return ApiResponse.success(accessUser);
    }



    /**
     * 测试 无需登录
     * @return ApiResponse<Void>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @GetMapping("/test7")
    public ApiResponse<Void> test7() {
        return ApiResponse.success();
    }

    /**
     * 测试 分页查询
     * @return ApiResponse<PageData<Menu>>
     *
     * @author HeHui
     * @date 2026-03-17 16:24
     */
    @PostMapping("/list")
    public ApiResponse<PageData<Menu>> page(@RequestBody TestPageQueryRequest request) {
        return ApiResponse.success(PageData.empty());
    }

}
