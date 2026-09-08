package com.jiuyu.replay.third.governance;

import com.jiuyu.replay.common.http.governance.GovernanceHttpServer;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserAccountInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 企业后台员工服务
 *
 * @author HeHui
 * @date 2026-04-01 18:39
 */
@Slf4j
@Service
public class GovernanceEmployeeServiceImpl implements GovernanceEmployeeService {


    private final GovernanceHttpServer httpServer;

    private final ParameterizedTypeReference<R<Void>> voidType = new ParameterizedTypeReference<R<Void>>() {
    };

    public GovernanceEmployeeServiceImpl(GovernanceHttpServer httpServer) {
        this.httpServer = httpServer;
    }


    /**
     * 解绑子账户 通知企业端
     *
     * @param accountId 帐户ID
     *
     * @return {@link R }<{@link Void }>
     */
    @Override
    public R<Void> unbind(long accountId) {
        log.info("同步解绑子账户 ->>>> {}", accountId);
        return httpServer.post("/api/governance/employee-open/unbind", Map.of("id", accountId), null)
            .retrieve().body(voidType);
    }


    /**
     * 绑定子账户 添加员工
     *
     * @param account 子账户
     *
     * @return {@link R }<{@link Void }>
     */
    @Override
    public R<Void> bind(UserAccountInfoVo account) {
        log.info("同步绑定子账户 ->>>> {}", account);
        return httpServer.post("/api/governance/employee-open/bind", account, null)
            .retrieve().body(voidType);
    }


    /**
     * 修改密码
     *
     * @param userId   用户ID
     * @param password 密码
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link Void }>
     */
    @Override
    public R<Void> updatePassword(long userId, String password, long tenantId) {
        log.info("同步修改密码 ->>>> {}", userId);
        return httpServer.post("/api/governance/employee-open/update-password", Map.of("userId", userId, "password", password, "tenantId", tenantId), null)
            .retrieve().body(voidType);
    }
}
