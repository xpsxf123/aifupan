package com.jiuyu.replay.api.controller.openapi.governance;

import cn.hutool.core.util.PhoneUtil;
import com.jiuyu.replay.api.controller.openapi.governance.request.*;
import com.jiuyu.replay.api.controller.openapi.governance.response.OpenGovernanceUserDetailsInfo;
import com.jiuyu.replay.api.interceptor.FeatureSignature;
import com.jiuyu.replay.api.logic.order.OrderLogic;
import com.jiuyu.replay.api.logic.power.UserLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.bindingSubAccountBo;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 企业后台管理 - 账户相关API
 *
 * @author HeHui
 * @date 2026-03-23 19:35
 */
@FeatureSignature(client = "governance")
@RestController
@RequestMapping("/replay/openapi/governance/account")
public class GovernanceOpenAccountController {

    private final UserLogic userLogic;

    private final OrderLogic orderLogic;

    public GovernanceOpenAccountController(UserLogic userLogic, OrderLogic orderLogic) {
        this.userLogic = userLogic;
        this.orderLogic = orderLogic;
    }


    /**
     * 基于手机号码绑定子账户
     *
     * @param request 绑定手机号码
     *
     * @return {@link R }<{@link Long }>
     */
    @PostMapping("/bind-sub-mobile")
    public R<Long> bindMobile(@RequestBody @Validated BindMobileSubAccountRequest request) {
        bindingSubAccountBo bindingSubAccountBo = new bindingSubAccountBo();
        bindingSubAccountBo.setCurrentUserId(request.getCurrentUserId());
        bindingSubAccountBo.setSubPhone(request.getMobile());
        bindingSubAccountBo.setNickName(request.getNickname());
        return userLogic.setUpASubAccountResultUserId(bindingSubAccountBo, false);
    }

    /**
     * 基于客户端用户ID绑定子账户
     *
     * @param request 绑定客户端用户ID
     *
     * @return {@link R }<{@link Long }>
     */
    @PostMapping("/bind-sub-user-id")
    public R<Long> bindUserId(@RequestBody @Validated BindUserIdSubAccountRequest request) {
        bindingSubAccountBo bindingSubAccountBo = new bindingSubAccountBo();
        bindingSubAccountBo.setCurrentUserId(request.getCurrentUserId());
        bindingSubAccountBo.setSubUserId(request.getUserId());
        return userLogic.setUpASubAccountResultUserId(bindingSubAccountBo, false);
    }


    /**
     * 基于客户端用户ID解绑子账户
     *
     * @param request 解绑客户端用户ID
     *
     * @return {@link R }<{@link Void }>
     */
    @PostMapping("/unbind")
    public R<Void> unbind(@RequestBody @Validated UnBindUserIdSubAccountRequest request) {
        bindingSubAccountBo bindingSubAccountBo = new bindingSubAccountBo();
        bindingSubAccountBo.setCurrentUserId(request.getCurrentUserId());
        bindingSubAccountBo.setSubUserId(request.getUserId());
        R<String> result = userLogic.unbindingSubAccount(bindingSubAccountBo);
        if (result.fail()) {
            return R.error(result.getCode(), result.getMsg());
        }
        orderLogic.checkUserOrder(request.getUserId());
        return R.ok();
    }


    /**
     * 获取账号信息
     *
     * @param accountId 账户ID
     *
     * @return {@link R }<{@link UserInfoVo }>
     */
    @GetMapping("/info")
    public R<UserInfoVo> getMainAccount(@RequestParam Long accountId) {
        boolean loadPassword = true;
        return userLogic.info(accountId, loadPassword);
    }


    /**
     * 获取账号详情
     *
     * @param accountId 账户ID
     *
     * @return {@link R }<{@link UserDetailsInfoVo }>
     */
    @GetMapping("/detail")
    public R<UserDetailsInfoVo> userDetailByUserId(@RequestParam Long accountId) {
        return userLogic.userDetailByUserId(accountId);
    }


    /**
     * 获取手机号码账号详情
     *
     * @param phone 手机号码
     *
     * @return {@link R }<{@link OpenGovernanceUserDetailsInfo }>
     */
    @GetMapping("/mobile-detail")
    public R<OpenGovernanceUserDetailsInfo> getMobileAccount(@RequestParam String phone) {
        return userLogic.getMobileAccount(phone);
    }


    /**
     * 获取租户下所有用户列表
     *
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link List<OpenGovernanceUserDetailsInfo> }>
     */
    @GetMapping("/user-list")
    public R<List<OpenGovernanceUserDetailsInfo>> getAllUserList(@RequestParam Long tenantId) {
        return userLogic.getAllUserList(tenantId);
    }


    /**
     * 更新手机号码
     *
     * @param request 更新手机号码
     *
     * @return {@link R }<{@link Void }>
     */
    @PostMapping("/update-mobile")
    public R<Void> updateMobile(@RequestBody @Validated UpdateMobileRequest request) {
        if (!PhoneUtil.isMobile(request.getMobile())) {
            return R.error("手机号码格式错误");
        }
        return userLogic.updateMobile(request.getUserId(), request.getMobile(), request.getTenantId());
    }

    /**
     * 更新密码
     *
     * @param request 更新密码
     *
     * @return {@link R }<{@link Void }>
     */
    @PostMapping("/update-password")
    public R<Void> updatePassword(@RequestBody @Validated UpdatePasswordRequest request) {
        return userLogic.updatePasswordByGovernance(request.getUserId(), request.getRawPassword(), request.getTenantId());
    }
}
