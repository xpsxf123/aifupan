package com.jiuyu.governance.openfeign.replay;


import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.openfeign.replay.response.ReplayUserDetailsInfo;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountDetailResponse;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountResponse;

import java.util.List;

/**
 * replay 用户账户服务
 *
 * @author HeHui
 * @date 2026-03-23 14:12
 */
public interface UserAccountService {


    /**
     * 绑定子账户
     *
     * @param mainAccountId 主帐户id
     * @param mobile        子账户手机号码
     * @param nickname      子账户昵称
     *
     * @return {@link ApiResponse }<{@link Long }>
     */
    ApiResponse<Long> bind(long mainAccountId, String mobile, String nickname);


    /**
     * 绑定子账户
     *
     * @param mainAccountId 主帐户id
     * @param subAccountId  子账户id
     *
     * @return {@link ApiResponse }<{@link Long }>
     */
    ApiResponse<Long> bind(long mainAccountId, long subAccountId);


    /**
     * 解绑子账户
     *
     * @param mainAccountId 主帐户id
     * @param subAccountId  子账户id
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> unbind(long mainAccountId, long subAccountId);


    /**
     * 获取租户的主账户
     *
     * @param tenantId 租户id
     *
     * @return {@link ApiResponse }<{@link ReplayAccountResponse }>
     */
    ApiResponse<ReplayAccountResponse> getTenantMainAccount(long tenantId);


    /**
     * 获取账户详情
     *
     * @param accountId 账户id
     *
     * @return {@link ApiResponse }<{@link ReplayAccountDetailResponse }>
     */
    ApiResponse<ReplayAccountDetailResponse> getMainAccountDetail(long accountId);

    /**
     * 获取租户的所有用户列表
     *
     * @param tenantId 租户id
     *
     * @return {@link ApiResponse }<{@link List<ReplayAccountDetailResponse> }>
     */
    ApiResponse<List<ReplayAccountDetailResponse>> getTenantAllUserList(long tenantId);

    /**
     * 获取账户信息
     *
     * @param accountId 账户id
     *
     * @return {@link ApiResponse }<{@link ReplayAccountResponse }>
     */
    ApiResponse<ReplayAccountResponse> getAccount(long accountId);


    /**
     * 更新手机号码
     *
     * @param accountId 账户id
     * @param newMobile 新手机号码
     * @param tenantId  租户id
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> updateMobile(long accountId, String newMobile, long tenantId);


    /**
     * 获取用户信息
     *
     * @param mobile 手机号码
     *
     * @return {@link ApiResponse }<{@link ReplayUserDetailsInfo }>
     */
    ApiResponse<ReplayUserDetailsInfo> getUserInfo(String mobile);


    /**
     * 修改密码
     *
     * @param accountId 账户id
     * @param rawPassword 明文密码
     * @param tenantId  租户id
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> updatePassword(long accountId, String rawPassword, long tenantId);
}
