package com.jiuyu.replay.api.service.datahub;

import com.jiuyu.replay.api.bo.datahub.DataHubAnalysisStatsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubBusinessAccountsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubLoginStatsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubOrderReconQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubTenantIdsQueryBo;
import com.jiuyu.replay.api.bo.datahub.DataHubTenantMappingQueryBo;
import com.jiuyu.replay.api.vo.datahub.DataHubAuthorizationsVo;
import com.jiuyu.replay.api.vo.datahub.DataHubBusinessAccountsVo;
import com.jiuyu.replay.api.vo.datahub.DataHubOrderReconVo;
import com.jiuyu.replay.api.vo.datahub.DataHubTenantAnalysisVo;
import com.jiuyu.replay.api.vo.datahub.DataHubTenantMappingVo;
import com.jiuyu.replay.api.vo.datahub.DataHubTenantMembersVo;
import com.jiuyu.replay.api.vo.datahub.DataHubUserLoginStatsVo;
import com.jiuyu.replay.generic.vo.common.R;

/**
 * Data Hub 数据开放服务
 * 面向 Business Data Hub 的售前客观数据查询编排
 */
public interface DataHubOpenService {

    /**
     * 客户→租户映射
     *
     * @param bo 查询条件（phones / userIds 至少一项）
     *
     * @return {@link R }<{@link DataHubTenantMappingVo }>
     */
    R<DataHubTenantMappingVo> tenantMapping(DataHubTenantMappingQueryBo bo);

    /**
     * 租户成员名单
     *
     * @param bo 租户id列表
     *
     * @return {@link R }<{@link DataHubTenantMembersVo }>
     */
    R<DataHubTenantMembersVo> tenantMembers(DataHubTenantIdsQueryBo bo);

    /**
     * 成员登录状态聚合
     *
     * @param bo 用户id列表 + 可选时间窗
     *
     * @return {@link R }<{@link DataHubUserLoginStatsVo }>
     */
    R<DataHubUserLoginStatsVo> loginStats(DataHubLoginStatsQueryBo bo);

    /**
     * 业务账号汇总与明细
     *
     * @param bo 租户id列表 + 是否含已移除
     *
     * @return {@link R }<{@link DataHubBusinessAccountsVo }>
     */
    R<DataHubBusinessAccountsVo> businessAccounts(DataHubBusinessAccountsQueryBo bo);

    /**
     * 授权状态查询
     *
     * @param bo 租户id列表
     *
     * @return {@link R }<{@link DataHubAuthorizationsVo }>
     */
    R<DataHubAuthorizationsVo> authorizations(DataHubTenantIdsQueryBo bo);

    /**
     * 账号分析状态聚合
     *
     * @param bo 租户id列表 + 可选时间窗
     *
     * @return {@link R }<{@link DataHubTenantAnalysisVo }>
     */
    R<DataHubTenantAnalysisVo> analysisStats(DataHubAnalysisStatsQueryBo bo);

    /**
     * 订单增量对账拉取
     *
     * @param bo 增量与游标条件
     *
     * @return {@link R }<{@link DataHubOrderReconVo }>
     */
    R<DataHubOrderReconVo> orderReconciliation(DataHubOrderReconQueryBo bo);
}
