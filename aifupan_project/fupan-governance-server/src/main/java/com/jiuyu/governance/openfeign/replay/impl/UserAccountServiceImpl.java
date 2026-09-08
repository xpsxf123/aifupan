package com.jiuyu.governance.openfeign.replay.impl;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.UserAccountService;
import com.jiuyu.governance.openfeign.replay.request.UpdateMobileRequest;
import com.jiuyu.governance.openfeign.replay.response.ReplayUserDetailsInfo;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountDetailResponse;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * replay 用户账户服务实现
 *
 * @author HeHui
 * @date 2026-03-23 20:01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final ReplayHttpServer replayHttpServer;

    /**
     * 绑定子账户
     *
     * @param mainAccountId 主帐户id
     * @param mobile        子账户手机号码
     * @param nickname      子账户昵称
     *
     * @return {@link ApiResponse }<{@link Long }>
     */
    @Override
    public ApiResponse<Long> bind(long mainAccountId, String mobile, String nickname) {
        log.info("绑定子账户: 主账户id={}, 子账户手机号码={}, 子账户昵称={}", mainAccountId, mobile, nickname);
        return replayHttpServer.post("/replay/openapi/governance/account/bind-sub-mobile", Map.of("currentUserId", mainAccountId, "mobile", mobile, "nickname", nickname), null)
            .retrieve().body(ReplayApiResponseType.LONG_TYPE);
    }

    /**
     * 绑定子账户
     *
     * @param mainAccountId 主帐户id
     * @param subAccountId  子账户id
     *
     * @return {@link ApiResponse }<{@link Long }>
     */
    @Override
    public ApiResponse<Long> bind(long mainAccountId, long subAccountId) {
        log.info("绑定子账户: 主账户id={}, 子账户id={}", mainAccountId, subAccountId);
        return replayHttpServer.post("/replay/openapi/governance/account/bind-sub-user-id", Map.of("currentUserId", mainAccountId, "userId", subAccountId), null)
            .retrieve().body(ReplayApiResponseType.LONG_TYPE);
    }

    /**
     * 解绑子账户
     *
     * @param mainAccountId 主帐户id
     * @param subAccountId  子账户id
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> unbind(long mainAccountId, long subAccountId) {
        log.info("解绑子账户: 主账户id={}, 子账户id={}", mainAccountId, subAccountId);
        return replayHttpServer.post("/replay/openapi/governance/account/unbind", Map.of("currentUserId", mainAccountId, "userId", subAccountId), null)
            .retrieve().body(ReplayApiResponseType.VOID_TYPE);
    }


    /**
     * 获取租户的主账户
     *
     * @param tenantId 租户id
     *
     * @return {@link ApiResponse }<{@link ReplayAccountResponse }>
     */
    @Override
    public ApiResponse<ReplayAccountResponse> getTenantMainAccount(long tenantId) {
        return replayHttpServer.get("/replay/openapi/governance/tenant/main-account?tenantId=" + tenantId, null)
            .retrieve().body(ReplayApiResponseType.ACCOUNT_RESPONSE_TYPE);
    }


    /**
     * 获取账户详情
     *
     * @param accountId 账户id
     *
     * @return {@link ApiResponse }<{@link ReplayAccountDetailResponse }>
     */
    @Override
    public ApiResponse<ReplayAccountDetailResponse> getMainAccountDetail(long accountId) {
        return replayHttpServer.get("/replay/openapi/governance/account/detail?accountId=" + accountId, null)
            .retrieve().body(ReplayApiResponseType.ACCOUNT_DETAIL_RESPONSE_TYPE);
    }

    /**
     * 获取租户的所有用户列表
     *
     * @param tenantId 租户id
     *
     * @return {@link ApiResponse }<{@link List <ReplayAccountDetailResponse> }>
     */
    @Override
    public ApiResponse<List<ReplayAccountDetailResponse>> getTenantAllUserList(long tenantId) {
        return replayHttpServer.get("/replay/openapi/governance/account/user-list?tenantId=" + tenantId, null)
            .retrieve().body(ReplayApiResponseType.LIST_ACCOUNT_DETAIL_RESPONSE_TYPE);
    }

    /**
     * 获取账户信息
     *
     * @param accountId 账户id
     *
     * @return {@link ApiResponse }<{@link ReplayAccountResponse }>
     */
    @Override
    public ApiResponse<ReplayAccountResponse> getAccount(long accountId) {
        return replayHttpServer.get("/replay/openapi/governance/account/info?accountId=" + accountId, null)
            .retrieve().body(ReplayApiResponseType.ACCOUNT_RESPONSE_TYPE);
    }

    /**
     * 更新手机号码
     *
     * @param accountId 账户id
     * @param newMobile 新手机号码
     * @param tenantId  租户id
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> updateMobile(long accountId, String newMobile, long tenantId) {
        log.info("更新手机号码: 账户id={}, 新手机号码={}, 租户id={}", accountId, newMobile, tenantId);
        UpdateMobileRequest updateMobileRequest = new UpdateMobileRequest();
        updateMobileRequest.setUserId(accountId);
        updateMobileRequest.setMobile(newMobile);
        updateMobileRequest.setTenantId(tenantId);
        return replayHttpServer.post("/replay/openapi/governance/account/update-mobile", updateMobileRequest, null)
            .retrieve().body(ReplayApiResponseType.VOID_TYPE);
    }


    /**
     * 获取用户信息
     *
     * @param mobile 手机号码
     *
     * @return {@link ApiResponse }<{@link ReplayUserDetailsInfo }>
     */
    @Override
    public ApiResponse<ReplayUserDetailsInfo> getUserInfo(String mobile) {
        if (EmptyUtil.isEmpty(mobile)) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请输入手机号码");
        }
        return replayHttpServer.get("/replay/openapi/governance/account/mobile-detail?phone=" + mobile, null)
            .retrieve().body(ReplayApiResponseType.USER_DETAILS_INFO_TYPE);
    }

    /**
     * 修改密码
     *
     * @param accountId 账户id
     * @param rawPassword 明文密码
     * @param tenantId  租户id
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> updatePassword(long accountId, String rawPassword, long tenantId) {
        if (EmptyUtil.isEmpty(rawPassword)) {
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "请输入密码");
        }
        log.info("同步修改密码: 账户id={}, 密码={}, 租户id={}", accountId, rawPassword, tenantId);
        return replayHttpServer.post("/replay/openapi/governance/account/update-password", Map.of("userId", accountId, "rawPassword", rawPassword, "tenantId", tenantId), null)
            .retrieve().body(ReplayApiResponseType.VOID_TYPE);
    }
}
