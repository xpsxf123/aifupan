package com.jiuyu.replay.api.service.datahub.impl;

import cn.hutool.core.date.DateUtil;
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
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.producer.OrderProducer;
import com.jiuyu.replay.order.vo.datahub.DataHubOrderItemVo;
import com.jiuyu.replay.power.producer.BindingAccountProducer;
import com.jiuyu.replay.power.producer.TenantProducer;
import com.jiuyu.replay.power.producer.UserLoginLogProducer;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.vo.BindingAccountVo;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.datahub.DataHubLoginStatsVo;
import com.jiuyu.replay.power.vo.datahub.DataHubMemberVo;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.vo.datahub.DataHubAnalysisStatsVo;
import com.jiuyu.replay.words.vo.datahub.DataHubAnchorAccountVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data Hub 数据开放服务实现
 *
 * <p>职责边界：</p>
 * <ol>
 *   <li>Controller 仅负责入口日志与路由；</li>
 *   <li>本类负责入参解析、跨模块 Producer 编排与对外契约字段映射（int 枚举 → 字符串枚举）；</li>
 *   <li>数据查询由各业务模块 Producer 承担。</li>
 * </ol>
 */
@Service
@Slf4j
public class DataHubOpenServiceImpl implements DataHubOpenService {

    private static final String LOG_PREFIX = "[DATA-HUB]";
    private static final String NOT_FOUND = "NOT_FOUND";
    private static final String NOT_MAPPED = "NOT_MAPPED";
    private static final String TIME_FORMAT_ERROR = "时间格式不合法，应为 yyyy-MM-dd HH:mm:ss";
    private static final int MAX_BATCH_SIZE = 200;

    private final UserProducer userProducer;
    private final TenantProducer tenantProducer;
    private final BindingAccountProducer bindingAccountProducer;
    private final UserLoginLogProducer userLoginLogProducer;
    private final AnchorUrlUserProducer anchorUrlUserProducer;
    private final AnchorVideoProducer anchorVideoProducer;
    private final OrderProducer orderProducer;

    /**
     * 订单租户快照口径的生产切换时间（yyyy-MM-dd HH:mm:ss）；
     * 未配置时全部按 BACKFILLED 保守标记
     */
    private final Date orderTenantSnapshotSince;

    /**
     * 构造方法
     *
     * @param userProducer             用户查询
     * @param tenantProducer           租户查询
     * @param bindingAccountProducer   绑定关系查询
     * @param userLoginLogProducer     登录日志聚合
     * @param anchorUrlUserProducer    业务账号查询
     * @param anchorVideoProducer      分析记录聚合
     * @param orderProducer            订单查询
     * @param orderTenantSnapshotSince 订单租户快照切换时间配置（可空）
     */
    public DataHubOpenServiceImpl(UserProducer userProducer,
                                  TenantProducer tenantProducer,
                                  BindingAccountProducer bindingAccountProducer,
                                  UserLoginLogProducer userLoginLogProducer,
                                  AnchorUrlUserProducer anchorUrlUserProducer,
                                  AnchorVideoProducer anchorVideoProducer,
                                  OrderProducer orderProducer,
                                  @Value("${jiuyu.datahub.order-tenant-snapshot-since:}") String orderTenantSnapshotSince) {
        this.userProducer = userProducer;
        this.tenantProducer = tenantProducer;
        this.bindingAccountProducer = bindingAccountProducer;
        this.userLoginLogProducer = userLoginLogProducer;
        this.anchorUrlUserProducer = anchorUrlUserProducer;
        this.anchorVideoProducer = anchorVideoProducer;
        this.orderProducer = orderProducer;
        this.orderTenantSnapshotSince = StringUtils.hasText(orderTenantSnapshotSince)
                ? DateUtil.parse(orderTenantSnapshotSince.trim())
                : null;
    }

    @Override
    public R<DataHubTenantMappingVo> tenantMapping(DataHubTenantMappingQueryBo bo) {
        List<String> phones = normalizeStrings(bo == null ? null : bo.getPhones());
        List<Long> userIds = normalizeIds(bo == null ? null : bo.getUserIds());
        if (phones.isEmpty() && userIds.isEmpty()) {
            return R.error("phones与userIds至少传一个");
        }
        if (phones.size() + userIds.size() > MAX_BATCH_SIZE) {
            return R.error("phones与userIds合计不能超过200");
        }
        log.info("{} tenant mapping, phones={}, userIds={}", LOG_PREFIX, phones.size(), userIds.size());

        Map<String, DataHubMemberVo> byPhone = userProducer.listDataHubMemberByPhones(phones).stream()
                .collect(Collectors.toMap(DataHubMemberVo::getPhone, Function.identity(), (a, b) -> a));
        Map<Long, DataHubMemberVo> byId = userProducer.listDataHubMemberByIds(userIds).stream()
                .collect(Collectors.toMap(DataHubMemberVo::getId, Function.identity(), (a, b) -> a));

        DataHubTenantMappingVo result = new DataHubTenantMappingVo();
        for (String phone : phones) {
            DataHubTenantMappingVo.Item item = buildMappingItem(byPhone.get(phone));
            item.setPhone(phone);
            result.getItems().add(item);
        }
        for (Long userId : userIds) {
            DataHubTenantMappingVo.Item item = buildMappingItem(byId.get(userId));
            if (item.getUserId() == null) {
                item.setUserId(userId);
            }
            result.getItems().add(item);
        }
        return R.ok(result);
    }

    @Override
    public R<DataHubTenantMembersVo> tenantMembers(DataHubTenantIdsQueryBo bo) {
        List<Long> tenantIds = normalizeIds(bo.getTenantIds());
        log.info("{} tenant members, tenantIds={}", LOG_PREFIX, tenantIds.size());

        Map<Long, TenantInfoVo> tenantMap = tenantProducer.listTenantInfo(tenantIds).stream()
                .collect(Collectors.toMap(TenantInfoVo::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<DataHubMemberVo>> membersByTenant = userProducer.listDataHubMemberByActiveTenantIds(tenantIds)
                .stream()
                .filter(m -> m.getActiveTenantId() != null)
                .collect(Collectors.groupingBy(DataHubMemberVo::getActiveTenantId));

        // 绑定/解绑记录按租户主账号查（含已解绑成员）
        List<Long> mainUserIds = tenantMap.values().stream()
                .map(TenantInfoVo::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, List<BindingAccountVo>> bindingsByParent = bindingAccountProducer.listByParentUserIds(mainUserIds)
                .stream()
                .collect(Collectors.groupingBy(BindingAccountVo::getParentUserId));

        // 已解绑成员不在 active_tenant_id 名单里，需要单独补查用户快照
        Set<Long> unboundUserIds = new LinkedHashSet<>();
        for (TenantInfoVo tenant : tenantMap.values()) {
            for (BindingAccountVo binding : bindingsByParent.getOrDefault(tenant.getUserId(), List.of())) {
                if (Integer.valueOf(1).equals(binding.getBindingStatus())) {
                    unboundUserIds.add(binding.getChildUserId());
                }
            }
        }
        Map<Long, DataHubMemberVo> unboundMemberMap = unboundUserIds.isEmpty()
                ? Map.of()
                : userProducer.listDataHubMemberByIds(unboundUserIds).stream()
                        .collect(Collectors.toMap(DataHubMemberVo::getId, Function.identity(), (a, b) -> a));

        DataHubTenantMembersVo result = new DataHubTenantMembersVo();
        for (Long tenantId : tenantIds) {
            DataHubTenantMembersVo.Item item = new DataHubTenantMembersVo.Item();
            item.setTenantId(tenantId);
            TenantInfoVo tenant = tenantMap.get(tenantId);
            if (tenant == null) {
                item.setMissingReason(NOT_FOUND);
                result.getItems().add(item);
                continue;
            }
            Map<Long, BindingAccountVo> bindingByChild = bindingsByParent
                    .getOrDefault(tenant.getUserId(), List.of()).stream()
                    .collect(Collectors.toMap(BindingAccountVo::getChildUserId, Function.identity(), (a, b) -> a));

            // 当前成员（active_tenant_id 命中，含冻结/已删除）
            Set<Long> currentIds = new LinkedHashSet<>();
            for (DataHubMemberVo member : membersByTenant.getOrDefault(tenantId, List.of())) {
                currentIds.add(member.getId());
                item.getMembers().add(buildMember(member, bindingByChild.get(member.getId()), false));
            }
            // 已解绑成员（绑定记录 bindingStatus=1 且当前不在名单中）
            for (BindingAccountVo binding : bindingsByParent.getOrDefault(tenant.getUserId(), List.of())) {
                if (!Integer.valueOf(1).equals(binding.getBindingStatus())
                        || currentIds.contains(binding.getChildUserId())) {
                    continue;
                }
                item.getMembers().add(buildMember(
                        unboundMemberMap.get(binding.getChildUserId()), binding, true));
            }
            result.getItems().add(item);
        }
        return R.ok(result);
    }

    @Override
    public R<DataHubUserLoginStatsVo> loginStats(DataHubLoginStatsQueryBo bo) {
        List<Long> userIds = normalizeIds(bo.getUserIds());
        Date windowStart;
        Date windowEnd;
        try {
            windowStart = parseTime(bo.getStartTime());
            windowEnd = parseTime(bo.getEndTime());
        } catch (Exception ex) {
            return R.error(TIME_FORMAT_ERROR);
        }
        boolean hasWindow = windowStart != null || windowEnd != null;
        log.info("{} login stats, userIds={}, window={}~{}", LOG_PREFIX, userIds.size(), windowStart, windowEnd);

        Map<Long, DataHubMemberVo> memberMap = userProducer.listDataHubMemberByIds(userIds).stream()
                .collect(Collectors.toMap(DataHubMemberVo::getId, Function.identity(), (a, b) -> a));
        Map<Long, DataHubLoginStatsVo> totalMap = userLoginLogProducer
                .aggregateSuccessLogin(userIds, null, null).stream()
                .collect(Collectors.toMap(DataHubLoginStatsVo::getUserId, Function.identity(), (a, b) -> a));
        Map<Long, DataHubLoginStatsVo> windowMap = hasWindow
                ? userLoginLogProducer.aggregateSuccessLogin(userIds, windowStart, windowEnd).stream()
                        .collect(Collectors.toMap(DataHubLoginStatsVo::getUserId, Function.identity(), (a, b) -> a))
                : Map.of();

        DataHubUserLoginStatsVo result = new DataHubUserLoginStatsVo();
        result.setWindowStart(windowStart);
        result.setWindowEnd(windowEnd);
        for (Long userId : userIds) {
            DataHubUserLoginStatsVo.Item item = new DataHubUserLoginStatsVo.Item();
            item.setUserId(userId);
            DataHubMemberVo member = memberMap.get(userId);
            if (member == null) {
                item.setMissingReason(NOT_FOUND);
                result.getItems().add(item);
                continue;
            }
            DataHubLoginStatsVo total = totalMap.get(userId);
            long totalCount = total == null || total.getLoginCount() == null ? 0L : total.getLoginCount();
            item.setTotalLoginCount(totalCount);
            item.setHasLoggedIn(Integer.valueOf(1).equals(member.getIsLoggedIn()) || totalCount > 0);
            if (total != null) {
                item.setFirstLoginAt(total.getFirstLoginDate());
                item.setLastLoginAt(total.getLastLoginDate());
            }
            if (hasWindow) {
                DataHubLoginStatsVo window = windowMap.get(userId);
                item.setWindowLoginCount(window == null || window.getLoginCount() == null
                        ? 0L : window.getLoginCount());
            }
            result.getItems().add(item);
        }
        return R.ok(result);
    }

    @Override
    public R<DataHubBusinessAccountsVo> businessAccounts(DataHubBusinessAccountsQueryBo bo) {
        List<Long> tenantIds = normalizeIds(bo.getTenantIds());
        boolean includeRemoved = Boolean.TRUE.equals(bo.getIncludeRemoved());
        log.info("{} business accounts, tenantIds={}, includeRemoved={}", LOG_PREFIX, tenantIds.size(), includeRemoved);

        Map<Long, TenantInfoVo> tenantMap = tenantProducer.listTenantInfo(tenantIds).stream()
                .collect(Collectors.toMap(TenantInfoVo::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<DataHubAnchorAccountVo>> accountsByTenant = anchorUrlUserProducer
                .listDataHubAccountsByTenantIds(tenantIds, includeRemoved).stream()
                .collect(Collectors.groupingBy(DataHubAnchorAccountVo::getTenantId));

        DataHubBusinessAccountsVo result = new DataHubBusinessAccountsVo();
        result.setObservedAt(new Date());
        for (Long tenantId : tenantIds) {
            DataHubBusinessAccountsVo.Item item = new DataHubBusinessAccountsVo.Item();
            item.setTenantId(tenantId);
            if (!tenantMap.containsKey(tenantId)) {
                item.setMissingReason(NOT_FOUND);
                result.getItems().add(item);
                continue;
            }
            List<DataHubAnchorAccountVo> accounts = accountsByTenant.getOrDefault(tenantId, List.of());
            // 平台汇总恒为在册口径（未移除），与明细的 includeRemoved 无关
            accounts.stream()
                    .filter(a -> Integer.valueOf(0).equals(a.getIsRemoveRecord()))
                    .collect(Collectors.groupingBy(a -> mapPlatform(a.getPlatform()), Collectors.counting()))
                    .forEach((platform, count) -> {
                        DataHubBusinessAccountsVo.PlatformCount pc = new DataHubBusinessAccountsVo.PlatformCount();
                        pc.setPlatform(platform);
                        pc.setTotalCount(count.intValue());
                        item.getSummary().add(pc);
                    });
            for (DataHubAnchorAccountVo account : accounts) {
                DataHubBusinessAccountsVo.Account vo = new DataHubBusinessAccountsVo.Account();
                vo.setBusinessAccountId(account.getSecUid());
                vo.setNickname(account.getAnchorName());
                vo.setPlatform(mapPlatform(account.getPlatform()));
                vo.setAccountType(mapAccountType(account.getAccountType()));
                vo.setAddedByUserId(account.getAddedByUserId());
                vo.setAddedAt(account.getCreateDate());
                boolean removed = !Integer.valueOf(0).equals(account.getIsRemoveRecord());
                vo.setRemoved(removed);
                vo.setRemovedAt(removed ? account.getDeleteDate() : null);
                item.getAccounts().add(vo);
            }
            result.getItems().add(item);
        }
        return R.ok(result);
    }

    @Override
    public R<DataHubAuthorizationsVo> authorizations(DataHubTenantIdsQueryBo bo) {
        List<Long> tenantIds = normalizeIds(bo.getTenantIds());
        log.info("{} authorizations, tenantIds={}", LOG_PREFIX, tenantIds.size());

        Map<Long, TenantInfoVo> tenantMap = tenantProducer.listTenantInfo(tenantIds).stream()
                .collect(Collectors.toMap(TenantInfoVo::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<DataHubAnchorAccountVo>> accountsByTenant = anchorUrlUserProducer
                .listDataHubAccountsByTenantIds(tenantIds, false).stream()
                .collect(Collectors.groupingBy(DataHubAnchorAccountVo::getTenantId));

        DataHubAuthorizationsVo result = new DataHubAuthorizationsVo();
        for (Long tenantId : tenantIds) {
            DataHubAuthorizationsVo.Item item = new DataHubAuthorizationsVo.Item();
            item.setTenantId(tenantId);
            if (!tenantMap.containsKey(tenantId)) {
                item.setMissingReason(NOT_FOUND);
                result.getItems().add(item);
                continue;
            }
            for (DataHubAnchorAccountVo account : accountsByTenant.getOrDefault(tenantId, List.of())) {
                addAuthorization(item, account, "JULIANG_BAIYING",
                        mapGeneralAuthStatus(account.getAuthJlbyStatus()), account.getAuthJlbyStatusTime());
                addAuthorization(item, account, "QIANCHUAN",
                        mapGeneralAuthStatus(account.getAuthQcStatus()), account.getAuthQcStatusTime());
                addAuthorization(item, account, "WECHAT_CHANNELS",
                        mapChannelAuthStatus(account.getAuthChannelStatus()), null);
                addAuthorization(item, account, "DOUYIN_LAIKE",
                        mapGeneralAuthStatus(account.getAuthLifeStatus()), account.getAuthLifeStatusTime());
            }
            result.getItems().add(item);
        }
        return R.ok(result);
    }

    @Override
    public R<DataHubTenantAnalysisVo> analysisStats(DataHubAnalysisStatsQueryBo bo) {
        List<Long> tenantIds = normalizeIds(bo.getTenantIds());
        Date windowStart;
        Date windowEnd;
        try {
            windowStart = parseTime(bo.getStartTime());
            windowEnd = parseTime(bo.getEndTime());
        } catch (Exception ex) {
            return R.error(TIME_FORMAT_ERROR);
        }
        log.info("{} analysis stats, tenantIds={}, window={}~{}", LOG_PREFIX, tenantIds.size(), windowStart, windowEnd);

        Map<Long, TenantInfoVo> tenantMap = tenantProducer.listTenantInfo(tenantIds).stream()
                .collect(Collectors.toMap(TenantInfoVo::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<DataHubAnalysisStatsVo>> statsByTenant = anchorVideoProducer
                .aggregateDataHubAnalysisStats(tenantIds, windowStart, windowEnd).stream()
                .collect(Collectors.groupingBy(DataHubAnalysisStatsVo::getTenantId));

        DataHubTenantAnalysisVo result = new DataHubTenantAnalysisVo();
        result.setWindowStart(windowStart);
        result.setWindowEnd(windowEnd);
        for (Long tenantId : tenantIds) {
            DataHubTenantAnalysisVo.Item item = new DataHubTenantAnalysisVo.Item();
            item.setTenantId(tenantId);
            if (!tenantMap.containsKey(tenantId)) {
                item.setMissingReason(NOT_FOUND);
                result.getItems().add(item);
                continue;
            }
            for (DataHubAnalysisStatsVo stats : statsByTenant.getOrDefault(tenantId, List.of())) {
                DataHubTenantAnalysisVo.Account account = new DataHubTenantAnalysisVo.Account();
                account.setBusinessAccountId(stats.getSecUid());
                account.setAnalysisCount(stats.getAnalysisCount());
                account.setFirstAnalyzedAt(stats.getFirstAnalyzedDate());
                account.setLastAnalyzedAt(stats.getLastAnalyzedDate());
                item.getAccounts().add(account);
            }
            result.getItems().add(item);
        }
        return R.ok(result);
    }

    @Override
    public R<DataHubOrderReconVo> orderReconciliation(DataHubOrderReconQueryBo bo) {
        List<Long> tenantIds = normalizeIds(bo == null ? null : bo.getTenantIds());
        Date updatedAfter;
        try {
            updatedAfter = parseTime(bo == null ? null : bo.getUpdatedAfter());
        } catch (Exception ex) {
            return R.error(TIME_FORMAT_ERROR);
        }
        int pageSize = bo == null || bo.getPageSize() == null ? 200 : bo.getPageSize();
        Date cursorUpdateDate = null;
        Long cursorId = null;
        String cursor = bo == null ? null : bo.getCursor();
        if (StringUtils.hasText(cursor)) {
            int sep = cursor.lastIndexOf('_');
            if (sep <= 0 || sep == cursor.length() - 1) {
                return R.error("cursor格式不合法");
            }
            try {
                cursorUpdateDate = new Date(Long.parseLong(cursor.substring(0, sep)));
                cursorId = Long.parseLong(cursor.substring(sep + 1));
            } catch (NumberFormatException ex) {
                return R.error("cursor格式不合法");
            }
        }
        log.info("{} order reconciliation, tenantIds={}, updatedAfter={}, cursor={}, pageSize={}",
                LOG_PREFIX, tenantIds.size(), updatedAfter, cursor, pageSize);

        List<DataHubOrderItemVo> orders = orderProducer.listDataHubOrders(
                tenantIds, updatedAfter, cursorUpdateDate, cursorId, pageSize + 1);
        boolean hasMore = orders.size() > pageSize;
        if (hasMore) {
            orders = orders.subList(0, pageSize);
        }

        DataHubOrderReconVo result = new DataHubOrderReconVo();
        result.setHasMore(hasMore);
        for (DataHubOrderItemVo order : orders) {
            result.getItems().add(buildOrderItem(order));
        }
        if (hasMore && !orders.isEmpty()) {
            DataHubOrderItemVo last = orders.get(orders.size() - 1);
            result.setNextCursor(last.getUpdateDate().getTime() + "_" + last.getId());
        }
        return R.ok(result);
    }

    /**
     * 组装单条映射结果（不含 phone 回显）
     *
     * @param member 用户快照（null 表示未命中）
     *
     * @return 映射结果项
     */
    private DataHubTenantMappingVo.Item buildMappingItem(DataHubMemberVo member) {
        DataHubTenantMappingVo.Item item = new DataHubTenantMappingVo.Item();
        if (member == null) {
            item.setMissingReason(NOT_FOUND);
            return item;
        }
        item.setUserId(member.getId());
        item.setSourceUpdatedAt(member.getUpdateDate());
        item.setRelationStatus(Integer.valueOf(2).equals(member.getUserType()) ? "SUB" : "MAIN");
        item.setAccountStatus(Integer.valueOf(1).equals(member.getStatus()) ? "FROZEN" : "NORMAL");
        if (member.getActiveTenantId() == null) {
            item.setMissingReason(NOT_MAPPED);
            return item;
        }
        item.setTenantId(member.getActiveTenantId());
        return item;
    }

    /**
     * 组装成员项
     *
     * @param member  用户快照（已解绑成员可能为 null——用户已注销）
     * @param binding 绑定记录（主账号或早期数据可能为 null）
     * @param unbound 是否已解绑成员
     *
     * @return 成员项
     */
    private DataHubTenantMembersVo.Member buildMember(DataHubMemberVo member,
                                                      BindingAccountVo binding,
                                                      boolean unbound) {
        DataHubTenantMembersVo.Member vo = new DataHubTenantMembersVo.Member();
        if (binding != null) {
            vo.setUserId(binding.getChildUserId());
            vo.setJoinedAt(binding.getBindingDate());
            if (Integer.valueOf(1).equals(binding.getBindingStatus())) {
                vo.setLeftAt(binding.getUnbindDate());
            }
        }
        if (member != null) {
            vo.setUserId(member.getId());
            vo.setRole(Integer.valueOf(2).equals(member.getUserType()) ? "SUB" : "MAIN");
            vo.setDisplayName(member.getNickName());
            vo.setMaskedPhone(CommonUtils.maskPhone(member.getPhone()));
            vo.setHasLoggedIn(Integer.valueOf(1).equals(member.getIsLoggedIn()));
            vo.setRegisteredAt(member.getCreateDate());
            if (vo.getJoinedAt() == null && !Integer.valueOf(2).equals(member.getUserType())) {
                vo.setJoinedAt(member.getCreateDate());
            }
        } else {
            vo.setRole("SUB");
        }
        if (unbound) {
            vo.setStatus("UNBOUND");
        } else if (member != null && Integer.valueOf(1).equals(member.getIsDeleted())) {
            vo.setStatus("DELETED");
        } else if (member != null && Integer.valueOf(1).equals(member.getStatus())) {
            vo.setStatus("FROZEN");
        } else {
            vo.setStatus("NORMAL");
        }
        return vo;
    }

    /**
     * 追加一条授权状态明细
     *
     * @param item            租户授权项
     * @param account         业务账号快照
     * @param authType        授权类型
     * @param status          字符串状态
     * @param statusUpdatedAt 状态变更时间
     */
    private void addAuthorization(DataHubAuthorizationsVo.Item item,
                                  DataHubAnchorAccountVo account,
                                  String authType,
                                  String status,
                                  Date statusUpdatedAt) {
        DataHubAuthorizationsVo.Authorization auth = new DataHubAuthorizationsVo.Authorization();
        auth.setBusinessAccountId(account.getSecUid());
        auth.setOperatorUserId(account.getAddedByUserId());
        auth.setAuthType(authType);
        auth.setStatus(status);
        auth.setStatusUpdatedAt(statusUpdatedAt);
        item.getAuthorizations().add(auth);
    }

    /**
     * 组装订单事实项（int 枚举 → 字符串枚举）
     *
     * @param order 订单模块数据项
     *
     * @return 订单事实项
     */
    private DataHubOrderReconVo.Item buildOrderItem(DataHubOrderItemVo order) {
        DataHubOrderReconVo.Item item = new DataHubOrderReconVo.Item();
        item.setOrderId(order.getId());
        item.setUserId(order.getUserId());
        boolean hasTenant = order.getTenantId() != null && order.getTenantId() > 0;
        item.setTenantId(hasTenant ? order.getTenantId() : null);
        if (!hasTenant) {
            item.setTenantAttribution("UNKNOWN");
        } else if (orderTenantSnapshotSince != null
                && order.getCreateDate() != null
                && !order.getCreateDate().before(orderTenantSnapshotSince)) {
            item.setTenantAttribution("SNAPSHOT");
        } else {
            item.setTenantAttribution("BACKFILLED");
        }
        item.setStatus(mapOrderStatus(order.getStatus()));
        item.setOrderType(mapOrderType(order.getOrderType()));
        item.setCommodityType(mapCommodityType(order.getCommodityType()));
        item.setSource(mapOrderSource(order.getSource()));
        item.setIsTrial(Integer.valueOf(1).equals(order.getTrialOrder()));
        item.setPriceCent(order.getTotalPrice());
        item.setPaidAt(order.getPayDate());
        item.setServiceStartAt(order.getStartDate());
        item.setServiceEndAt(order.getEndDate());
        item.setCreatedAt(order.getCreateDate());
        item.setUpdatedAt(order.getUpdateDate());
        item.setCommodityName(order.getCommodityName());
        item.setTitle(order.getTitle());
        return item;
    }

    /**
     * 平台编码 → 字符串枚举
     *
     * @param platform 0抖音 1快手 2视频号
     *
     * @return 平台枚举
     */
    private String mapPlatform(Integer platform) {
        if (platform == null) {
            return "UNKNOWN";
        }
        return switch (platform) {
            case 0 -> "DOUYIN";
            case 1 -> "KUAISHOU";
            case 2 -> "WECHAT_CHANNELS";
            default -> "UNKNOWN";
        };
    }

    /**
     * 账号归属类型 → 字符串枚举
     *
     * @param accountType 0自有 1同行
     *
     * @return 归属类型枚举
     */
    private String mapAccountType(Integer accountType) {
        if (accountType == null) {
            return null;
        }
        return switch (accountType) {
            case 0 -> "OWN";
            case 1 -> "PEER";
            default -> "UNKNOWN";
        };
    }

    /**
     * 通用授权状态（巨量百应/千川/抖音来客）→ 字符串枚举
     *
     * @param status 0~5
     *
     * @return 授权状态枚举
     */
    private String mapGeneralAuthStatus(Integer status) {
        if (status == null) {
            return "NOT_AUTHORIZED";
        }
        return switch (status) {
            case 0 -> "NOT_AUTHORIZED";
            case 1 -> "AUTHORIZED";
            case 2 -> "EXPIRED";
            case 3 -> "FAILED";
            case 4 -> "AUTHORIZING";
            case 5 -> "ACCOUNT_MISMATCH";
            default -> "UNKNOWN";
        };
    }

    /**
     * 微信视频号授权状态 → 字符串枚举
     *
     * @param status 0~3
     *
     * @return 授权状态枚举
     */
    private String mapChannelAuthStatus(Integer status) {
        if (status == null) {
            return "NOT_AUTHORIZED";
        }
        return switch (status) {
            case 0 -> "NOT_AUTHORIZED";
            case 1 -> "AUTHORIZED";
            case 2 -> "CANCELED_BY_WECHAT";
            case 3 -> "CANCELED_BY_USER";
            default -> "UNKNOWN";
        };
    }

    /**
     * 订单状态 → 字符串枚举（细粒度；与事件侧折叠枚举的对照见接口文档）
     *
     * @param status 0~8
     *
     * @return 状态枚举
     */
    private String mapOrderStatus(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "UNPAID";
            case 1 -> "NOT_STARTED";
            case 2 -> "ACTIVE";
            case 3 -> "EXPIRED";
            case 4 -> "REFUNDED";
            case 5 -> "ENDED_BY_UPGRADE";
            case 6 -> "CLOSED_TIMEOUT";
            case 7 -> "FROZEN";
            case 8 -> "CANCELED_MANUAL";
            default -> "UNKNOWN";
        };
    }

    /**
     * 订单类型 → 字符串枚举
     *
     * @param orderType 0~7
     *
     * @return 类型枚举
     */
    private String mapOrderType(Integer orderType) {
        if (orderType == null) {
            return "UNKNOWN";
        }
        return switch (orderType) {
            case 0 -> "FREE";
            case 1 -> "UPGRADE";
            case 2 -> "FREE_TO_PAID";
            case 3 -> "RENEWAL";
            case 4 -> "INCREMENT";
            case 5 -> "VERSION_ACTIVITY";
            case 6 -> "EDIT";
            case 7 -> "COMMODITY_ACTIVITY";
            default -> "UNKNOWN";
        };
    }

    /**
     * 商品类型 → 字符串枚举
     *
     * @param commodityType 0~3
     *
     * @return 商品类型枚举
     */
    private String mapCommodityType(Integer commodityType) {
        if (commodityType == null) {
            return "UNKNOWN";
        }
        return switch (commodityType) {
            case 0 -> "INCREMENT";
            case 1 -> "NORMAL_VERSION";
            case 2 -> "ACTIVITY_VERSION";
            case 3 -> "INVITE_ACTIVITY";
            default -> "UNKNOWN";
        };
    }

    /**
     * 订单来源 → 字符串枚举
     *
     * @param source 0~3
     *
     * @return 来源枚举
     */
    private String mapOrderSource(Integer source) {
        if (source == null) {
            return "UNKNOWN";
        }
        return switch (source) {
            case 0 -> "NORMAL";
            case 1 -> "MANUAL";
            case 2 -> "INVITE_REWARD";
            case 3 -> "INVITE_CODE";
            default -> "UNKNOWN";
        };
    }

    /**
     * 归一化字符串列表（trim + 去空 + 去重，保序）
     *
     * @param values 原始列表
     *
     * @return 归一化列表
     */
    private List<String> normalizeStrings(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 归一化 ID 列表（去 null + 去重，保序）
     *
     * @param values 原始列表
     *
     * @return 归一化列表
     */
    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 解析时间字符串
     *
     * @param value yyyy-MM-dd HH:mm:ss
     *
     * @return 解析结果，为空时返回 {@code null}
     */
    private Date parseTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return DateUtil.parse(value.trim());
    }
}
