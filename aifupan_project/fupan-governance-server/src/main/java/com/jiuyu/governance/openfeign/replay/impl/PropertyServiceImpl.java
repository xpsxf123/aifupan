package com.jiuyu.governance.openfeign.replay.impl;

import com.jiuyu.framework.lock.ResourceLock;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.PropertyService;
import com.jiuyu.governance.openfeign.replay.consts.PropertyType;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.request.TenantPropertyQuotaEditRequest;
import com.jiuyu.governance.openfeign.replay.response.TenantPropertyQuotaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * 资产服务实现 - 基于远程调用
 *
 * @author HeHui
 * @date 2026-04-10 11:56
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final ReplayHttpServer httpServer;

    /**
     * 获取租户资产额度
     *
     * @param tenantId 租户ID
     *
     * @return {@link ApiResponse }<{@link TenantPropertyQuotaResponse }>
     */
    @Override
    public ApiResponse<TenantPropertyQuotaResponse> getTenantPropertyQuota(long tenantId) {
        return httpServer.get("/replay/openapi/governance/tenant/property-tenant?tenantId=" + tenantId, null)
            .retrieve().body(ReplayApiResponseType.QUOTA_RESPONSE_TYPE);
    }

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
    @ResourceLock(prefix = "tenant:property:edit", key = "#tenantId", message = "您的操作太频繁啦,请稍后重试!", releaseLock = true)
    @Override
    public <T> ApiResponse<T> editTenantPropertyQuota(long tenantId, long totalUseQuote, PropertyType getPropertyType, Function<Boolean, ApiResponse<T>> callback) {
        // 参数校验：资产类型不能为空
        if (getPropertyType == null) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "请传入获取资产类型的方法!");
        }

        // 获取租户当前资产额度信息
        ApiResponse<TenantPropertyQuotaResponse> tenantPropertyQuotaResponse = this.getTenantPropertyQuota(tenantId);
        if (tenantPropertyQuotaResponse.failed()) {
            log.warn("[资产] failed to obtain tenant asset limit. tenantId:{}", tenantId);
            return ApiResponse.failed(tenantPropertyQuotaResponse.getCode(), tenantPropertyQuotaResponse.getMsg());
        }
        TenantPropertyQuotaResponse propertyQuota = tenantPropertyQuotaResponse.getData();
        if (propertyQuota == null) {
            log.warn("[资产] failed to obtain tenant asset limit data is empty. tenantId:{}", tenantId);
            return callback.apply(false);
        }

        // 根据资产类型获取对应的总额度
        Long total = getPropertyType.getPropertyTotal().apply(propertyQuota);
        if (total == null || total <= 0) {
            log.warn("[资产] failed to obtain tenant asset limit propertyType: {} , quota is {}. tenantId:{}", getPropertyType, total, tenantId);
            return callback.apply(false);
        }

        // 校验剩余额度是否充足：剩余额度 = 总额度 - 已使用额度
        if (totalUseQuote > total) {
            log.warn("[资产] tenant asset limit propertyType: {} , totalQuota {} < totalUseQuote {}. tenantId:{}", getPropertyType, total, totalUseQuote, tenantId);
            return callback.apply(false);
        }

        // 额度充足，执行回调中的业务逻辑
        ApiResponse<T> callbackResponse = callback.apply(true);

        // 验证回调执行结果：null 表示开发错误，直接抛出异常
        if (callbackResponse == null) {
            log.error("[资产] tenant asset limit check after execute callback response is null. tenantId:{}。 谁调用的？代码写错了！！", tenantId);
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "执行资产逻辑错误, 请联系技术支持");
        }
        if (callbackResponse.failed()) {
            log.error("[资产] tenant asset limit check after execute callback response code:{} {} failed. tenantId:{}", callbackResponse.getCode(), callbackResponse.getMsg(), tenantId);
            return callbackResponse;
        }

        // 计算剩余可用额度并记录日志
        long leftQuota = total - totalUseQuote;
        log.info("[资产] tenant asset limit propertyType: {} , use after leftQuota {}. tenantId:{}", getPropertyType, leftQuota, tenantId);

        // 构造额度更新请求，更新租户的剩余额度
        TenantPropertyQuotaEditRequest quotaEditRequest = new TenantPropertyQuotaEditRequest();
        quotaEditRequest.setTenantId(tenantId);
        quotaEditRequest.setCode(getPropertyType.getCode());
        quotaEditRequest.setQuantity(leftQuota);

        // 调用远程接口更新租户资产额度
        ApiResponse<Void> editResponse = httpServer.post("/replay/openapi/governance/tenant/property-num", quotaEditRequest, null).retrieve().body(ReplayApiResponseType.VOID_TYPE);
        if (editResponse == null) {
            log.error("[资产] tenant asset limit, rest edit quota response is null tenantId:{} code: {}, quantity: {} ", tenantId, getPropertyType.getCode(), leftQuota);
            throw new BusinessException(BizErrorCode.QUOTA_EXCEEDED, "执行失败,请联系技术支持！");
        }
        if (editResponse.failed()) {
            log.error("[资产] tenant asset limit, rest edit quota response code:{} {} failed.tenantId:{} code: {}, quantity: {} ", editResponse.getCode(), editResponse.getMsg(), tenantId, getPropertyType.getCode(), leftQuota);
            throw new BusinessException(BizErrorCode.QUOTA_EXCEEDED, editResponse.getMsg());
        }
        log.info("[资产] tenant asset limit, rest edit quota success. tenantId:{} code: {}, quantity: {} ", tenantId, getPropertyType.getCode(), leftQuota);
        // 返回业务回调的执行结果
        return callbackResponse;
    }
}
