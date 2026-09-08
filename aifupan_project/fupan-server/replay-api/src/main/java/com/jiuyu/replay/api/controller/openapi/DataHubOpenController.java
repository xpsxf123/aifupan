package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.bo.datahub.DataHubAnalysisStatsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubBusinessAccountsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubLoginStatsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubOrderReconQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubTenantIdsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubTenantMappingQueryBo;
import com.jiuyu.replay.api.service.datahub.DataHubOpenService;
import com.jiuyu.replay.api.vo.datahub.DataHubAuthorizationsVo;
import com.jiuyu.replay.api.vo.datahub.DataHubBusinessAccountsVo;
import com.jiuyu.replay.api.vo.datahub.DataHubOrderReconVo;
import com.jiuyu.replay.api.vo.datahub.DataHubTenantAnalysisVo;
import com.jiuyu.replay.api.vo.datahub.DataHubTenantMappingVo;
import com.jiuyu.replay.api.vo.datahub.DataHubTenantMembersVo;
import com.jiuyu.replay.api.vo.datahub.DataHubUserLoginStatsVo;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Data Hub 数据开放控制器
 * 面向 Business Data Hub 开放售前客观数据查询能力
 *
 * 说明：
 * 1. 接口统一走 `/internal/data-hub` 路径
 * 2. 统一复用 `@APIKey` 做请求鉴权（独立 app-id）
 */
@RestController
@APIKey
@Slf4j
@AllArgsConstructor
@RequestMapping("/internal/data-hub")
@Tag(name = "Data Hub 数据开放接口")
@Validated
public class DataHubOpenController {

    private static final String LOG_PREFIX = "[DATA-HUB][INBOUND]";

    private final DataHubOpenService dataHubOpenService;

    /**
     * 客户→租户映射
     *
     * @param bo 查询条件（phones / userIds 至少一项）
     *
     * @return {@link R }<{@link DataHubTenantMappingVo }>
     */
    @PostMapping("/customer/tenant-mapping")
    @Operation(summary = "客户→租户映射")
    public R<DataHubTenantMappingVo> tenantMapping(@Valid @RequestBody DataHubTenantMappingQueryBo bo) {
        log.info("{} tenant mapping received", LOG_PREFIX);
        return dataHubOpenService.tenantMapping(bo);
    }

    /**
     * 租户成员名单
     *
     * @param bo 租户id列表
     *
     * @return {@link R }<{@link DataHubTenantMembersVo }>
     */
    @PostMapping("/tenant/members")
    @Operation(summary = "租户成员名单")
    public R<DataHubTenantMembersVo> tenantMembers(@Valid @RequestBody DataHubTenantIdsQueryBo bo) {
        log.info("{} tenant members received", LOG_PREFIX);
        return dataHubOpenService.tenantMembers(bo);
    }

    /**
     * 成员登录状态聚合
     *
     * @param bo 用户id列表 + 可选时间窗
     *
     * @return {@link R }<{@link DataHubUserLoginStatsVo }>
     */
    @PostMapping("/user/login-stats")
    @Operation(summary = "成员登录状态聚合")
    public R<DataHubUserLoginStatsVo> loginStats(@Valid @RequestBody DataHubLoginStatsQueryBo bo) {
        log.info("{} login stats received", LOG_PREFIX);
        return dataHubOpenService.loginStats(bo);
    }

    /**
     * 业务账号汇总与明细
     *
     * @param bo 租户id列表 + 是否含已移除
     *
     * @return {@link R }<{@link DataHubBusinessAccountsVo }>
     */
    @PostMapping("/tenant/business-accounts")
    @Operation(summary = "业务账号汇总与明细")
    public R<DataHubBusinessAccountsVo> businessAccounts(@Valid @RequestBody DataHubBusinessAccountsQueryBo bo) {
        log.info("{} business accounts received", LOG_PREFIX);
        return dataHubOpenService.businessAccounts(bo);
    }

    /**
     * 授权状态查询
     *
     * @param bo 租户id列表
     *
     * @return {@link R }<{@link DataHubAuthorizationsVo }>
     */
    @PostMapping("/tenant/authorizations")
    @Operation(summary = "授权状态查询")
    public R<DataHubAuthorizationsVo> authorizations(@Valid @RequestBody DataHubTenantIdsQueryBo bo) {
        log.info("{} authorizations received", LOG_PREFIX);
        return dataHubOpenService.authorizations(bo);
    }

    /**
     * 账号分析状态聚合
     *
     * @param bo 租户id列表 + 可选时间窗
     *
     * @return {@link R }<{@link DataHubTenantAnalysisVo }>
     */
    @PostMapping("/tenant/analysis-stats")
    @Operation(summary = "账号分析状态聚合")
    public R<DataHubTenantAnalysisVo> analysisStats(@Valid @RequestBody DataHubAnalysisStatsQueryBo bo) {
        log.info("{} analysis stats received", LOG_PREFIX);
        return dataHubOpenService.analysisStats(bo);
    }

    /**
     * 订单增量对账拉取
     *
     * @param bo 增量与游标条件
     *
     * @return {@link R }<{@link DataHubOrderReconVo }>
     */
    @PostMapping("/order/reconciliation")
    @Operation(summary = "订单增量对账拉取")
    public R<DataHubOrderReconVo> orderReconciliation(@Valid @RequestBody DataHubOrderReconQueryBo bo) {
        log.info("{} order reconciliation received", LOG_PREFIX);
        return dataHubOpenService.orderReconciliation(bo);
    }
}
