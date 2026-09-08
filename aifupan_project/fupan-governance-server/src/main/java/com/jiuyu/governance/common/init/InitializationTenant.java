package com.jiuyu.governance.common.init;


import com.jiuyu.framework.oauth.AccessUser;
import org.springframework.core.Ordered;

/**
 * 租户初始化
 *
 * @author HeHui
 * @date 2026-03-24 10:37
 */
@FunctionalInterface
public interface InitializationTenant extends Ordered {


    /**
     * 最小企业版本， 来源依据： com.jiuyu.replay.ai.bll.CustPromptBll#save
     */
    int MIN_GOVERNANCE_LEVEL = 20;


    /**
     * 初始化
     *
     * @param tenantContext 租户初始化上下文
     * @param accessUser    访问用户
     */
    void init(TenantInitContext tenantContext, AccessUser accessUser);


    /**
     * 默认顺序
     *
     * @return 顺序
     */
    @Override
    default int getOrder() {
        return 30;
    }
}
