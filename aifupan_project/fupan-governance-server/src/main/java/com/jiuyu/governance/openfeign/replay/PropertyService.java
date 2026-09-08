package com.jiuyu.governance.openfeign.replay;


import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.openfeign.replay.consts.PropertyType;
import com.jiuyu.governance.openfeign.replay.response.TenantPropertyQuotaResponse;

import java.util.function.Function;

/**
 * 资产服务
 *
 * @author HeHui
 * @date 2026-04-10 11:17
 */
public interface PropertyService {


    /**
     * 获取租户资产额度
     *
     * @param tenantId 租户ID
     *
     * @return {@link ApiResponse }<{@link TenantPropertyQuotaResponse }>
     */
    ApiResponse<TenantPropertyQuotaResponse> getTenantPropertyQuota(long tenantId);


    /**
     * 修改租户资产额度
     * <pre>
     * 该方法采用模板方法模式，通过回调函数实现资产额度的扣减操作。
     * 执行流程：参数校验 -> 获取当前额度 -> 额度校验 -> 执行业务回调 -> 更新剩余额度
     * </pre>
     * 注意：totalUseQuote 参数表示累计已使用的总额度，而非本次使用的额度。
     * 最终剩余额度 = 总额度 - totalUseQuote
     *
     * @param tenantId        租户ID
     * @param totalUseQuote   总使用额度（累计已使用的额度，非本次使用量）
     * @param getPropertyType 资产类型枚举，用于确定要操作的资产种类及获取对应额度的方法
     * @param callback        业务回调函数，参数为布尔值（true表示额度充足，false表示额度不足），返回业务操作结果
     *                        当额度校验通过后，会以 true 调用此回调执行业务逻辑
     *
     * @return {@link ApiResponse }<{@link T }> 业务操作结果
     *
     * @throws BusinessException 当回调返回 null、远程调用失败或额度更新失败时抛出
     */
    <T> ApiResponse<T> editTenantPropertyQuota(long tenantId, long totalUseQuote, PropertyType getPropertyType, Function<Boolean, ApiResponse<T>> callback);
}
