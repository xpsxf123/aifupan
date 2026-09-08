package com.jiuyu.replay.third.governance;


import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserAccountInfoVo;
import com.jiuyu.replay.generic.vo.power.UserVo;

/**
 * 企业后台员工服务
 *
 * @author HeHui
 * @date 2026-04-01 17:47
 */
public interface GovernanceEmployeeService {


    /**
     * 解绑子账户 通知企业端
     *
     * @param accountId 帐户ID
     *
     * @return {@link R }<{@link Void }>
     */
    R<Void> unbind(long accountId);

    /**
     * 绑定子账户 添加员工
     *
     * @param account 子账户
     *
     * @return {@link R }<{@link Void }>
     */
    R<Void> bind(UserAccountInfoVo account);


    /**
     * 修改密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link Void }>
     */
    R<Void> updatePassword(long userId, String password, long tenantId);
}
