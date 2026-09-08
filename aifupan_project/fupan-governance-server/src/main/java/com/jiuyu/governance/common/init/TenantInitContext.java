package com.jiuyu.governance.common.init;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 租户初始化上下文
 *
 * @author HeHui
 * @date 2026-03-24 10:37
 */
public class TenantInitContext {

    /**
     * 客户端- 主账户ID
     */
    @Getter
    private final long mainAccountId;


    /**
     * 租户管理员ID
     */
    @Getter
    @Setter
    private Long tenantId;


    private final Map<String, Object> data;

    public TenantInitContext(long mainAccountId) {
        this.mainAccountId = mainAccountId;
        this.data = new HashMap<>();
    }




    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }


    /**
     * 获取租户管理员ID
     *
     * @return {@link Long}
     */
    public Long getTenantAdminId() {
        return (Long) data.get("adminId");
    }

    /**
     * 获取租户ID
     *
     * @return {@link Long}
     */
    public Long getCompanyId() {
        return (Long) data.get("companyId");
    }
 }
