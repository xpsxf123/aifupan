package com.jiuyu.governance.openfeign.replay.consts;

import com.jiuyu.governance.openfeign.replay.response.TenantPropertyQuotaResponse;
import lombok.Getter;

import java.util.function.Function;

/**
 * 资产类型
 *
 * @author HeHui
 * @date 2026-04-10 12:14
 */
@Getter
public enum PropertyType {

    ENTERPRISE_PERSON_NUM("enterprisePersonNum", "人员数量", TenantPropertyQuotaResponse::getTotalEnterprisePersonNum),

    ENTERPRISE_SUBSIDIARIES_NUM("enterpriseSubsidiariesNum", "子公司数量", TenantPropertyQuotaResponse::getTotalEnterpriseSubsidiariesNum),
    ;
    private final String code;
    private final String desc;
    private final Function<TenantPropertyQuotaResponse, Long> propertyTotal;

    PropertyType(String code, String desc, Function<TenantPropertyQuotaResponse, Long> propertyTotal) {
        this.code = code;
        this.desc = desc;
        this.propertyTotal = propertyTotal;
    }
}
