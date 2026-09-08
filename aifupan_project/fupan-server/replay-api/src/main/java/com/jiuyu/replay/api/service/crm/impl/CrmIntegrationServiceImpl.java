package com.jiuyu.replay.api.service.crm.impl;

import cn.hutool.core.date.DateUtil;
import com.jiuyu.replay.api.service.crm.CrmIntegrationService;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.generic.feign.order.UserPropertyDetailsFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.producer.AiTokenUseRecordProducer;
import com.jiuyu.replay.order.producer.OrderProducer;
import com.jiuyu.replay.order.producer.UserPropertyDetailsProducer;
import com.jiuyu.replay.order.repository.service.UserPropertyDetailsService;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bo.crm.CrmAiProfileBo;
import com.jiuyu.replay.power.bo.crm.CrmAiProfileUpsertBo;
import com.jiuyu.replay.power.bo.crm.CrmBatchAggregateQueryBo;
import com.jiuyu.replay.power.bo.crm.CrmBehaviorSignalsQueryBo;
import com.jiuyu.replay.power.bo.crm.CrmBusinessSnapshotUpdateBo;
import com.jiuyu.replay.power.bo.crm.CrmProfileSyncBo;
import com.jiuyu.replay.power.bo.crm.CrmStageEventBo;
import com.jiuyu.replay.power.bo.crm.CrmStageEventSaveBo;
import com.jiuyu.replay.power.bo.crm.CrmUserDetailsSyncBo;
import com.jiuyu.replay.power.producer.SalesProducer;
import com.jiuyu.replay.power.producer.TenantProducer;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.repository.service.CrmAiProfileService;
import com.jiuyu.replay.power.repository.service.CrmStageEventService;
import com.jiuyu.replay.power.repository.service.UserDetailsService;
import com.jiuyu.replay.power.vo.TenantInfoVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.power.vo.crm.CrmAiProfileInfoVo;
import com.jiuyu.replay.power.vo.crm.CrmAiProfileUpsertVo;
import com.jiuyu.replay.power.vo.crm.CrmBatchAggregateQueryVo;
import com.jiuyu.replay.power.vo.crm.CrmBehaviorSignalItemVo;
import com.jiuyu.replay.power.vo.crm.CrmBehaviorSignalsVo;
import com.jiuyu.replay.power.vo.crm.CrmCustomerAggregateItemVo;
import com.jiuyu.replay.power.vo.crm.CrmOrderQueryByPhoneVo;
import com.jiuyu.replay.power.vo.crm.CrmProfileSyncVo;
import com.jiuyu.replay.power.vo.crm.CrmStageEventVo;
import com.jiuyu.replay.power.vo.crm.CrmUserDetailsSyncVo;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.producer.SyncContrastProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CRM 集成服务实现
 * 负责按手机号聚合客户、销售和订单信息，并接收智能体侧回流写入请求。
 *
 * <p>职责边界：</p>
 * <ol>
 *   <li>Controller 仅负责入口日志与路由；</li>
 *   <li>Service 负责参数校验、手机号归一化、聚合与写入逻辑；</li>
 *   <li>数据库写入/幂等由下游模块的 Service/Mapper 承担。</li>
 * </ol>
 */
@Service
@Slf4j
@AllArgsConstructor
public class CrmIntegrationServiceImpl implements CrmIntegrationService {

    private static final int MAX_BATCH_SIZE = 50;
    private static final String CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND";
    private static final String LOG_PREFIX = "[CRM-INTEGRATION]";

    /** 使用类型 0: 运营助手 */
    private static final Integer ASSISTANT_TYPE_OP = 0;
    /** 账号归属类型 0: 自由账号（"自有账号"） */
    private static final Integer ACCOUNT_TYPE_OWN = 0;

    private final UserBll userBll;
    private final OrderBll orderBll;
    private final OrderProducer orderProducer;
    private final UserDetailsService userDetailsService;
    private final SalesProducer salesProducer;
    private final TenantProducer tenantProducer;
    private final UserProducer userProducer;
    private final CrmAiProfileService crmAiProfileService;
    private final CrmStageEventService crmStageEventService;
    private final AiTokenUseRecordProducer aiTokenUseRecordProducer;
    private final UserPropertyDetailsProducer userPropertyDetailsProducer;
    private final AnchorUrlUserProducer anchorUrlUserProducer;
    private final AnchorVideoProducer anchorVideoProducer;
    private final SyncContrastProducer syncContrastProducer;

    /**
     * 批量聚合查询客户信息
     *
     * @param bo 查询条件，包含手机号列表
     *
     * @return {@link R }<{@link CrmBatchAggregateQueryVo }>
     */
    @Override
    public R<CrmBatchAggregateQueryVo> batchAggregateQuery(CrmBatchAggregateQueryBo bo) {
        List<String> phones = normalizePhones(bo);
        log.info("{} batch aggregate query start, size={}", LOG_PREFIX, phones.size());
        if (phones.isEmpty()) {
            log.warn("{} batch aggregate query rejected, phones empty", LOG_PREFIX);
            return R.error("phones不能为空");
        }
        if (phones.size() > MAX_BATCH_SIZE) {
            log.warn("{} batch aggregate query rejected, size={} exceeds limit={}",
                    LOG_PREFIX, phones.size(), MAX_BATCH_SIZE);
            return R.error("phones数量不能超过50");
        }

        // 1. 批量查用户
        List<UserDto> userList = userBll.listByPhones(phones);
        Map<String, UserDto> userByPhone = userList.stream()
                .collect(Collectors.toMap(UserDto::getPhone, Function.identity()));

        // 2. 批量查销售、订单（仅对命中的用户）
        List<Long> userIds = userList.stream().map(UserDto::getId).toList();
        Map<Long, SalesInfoVo> salesByUserId = salesProducer.listByUserIds(userIds).stream()
                .collect(Collectors.toMap(s -> s.getUserId(), Function.identity(), (a, b) -> a));
        Map<Long, List<OrderInfoVo>> ordersByUserId = orderProducer.listAllOrdersByUserIds(userIds);

        // 3. 按原始 phones 顺序组装结果
        List<CrmCustomerAggregateItemVo> customers = new ArrayList<>(phones.size());
        for (String phone : phones) {
            customers.add(buildCustomerAggregateItem(phone, userByPhone, salesByUserId, ordersByUserId));
        }
        CrmBatchAggregateQueryVo result = new CrmBatchAggregateQueryVo();
        result.setCustomers(customers);
        log.info("{} batch aggregate query success, customerCount={}, hitCount={}",
                LOG_PREFIX, customers.size(), userIds.size());
        return R.ok(result);
    }

    /**
     * 按手机号查询订单
     *
     * @param phone 客户手机号
     *
     * @return {@link R }<{@link CrmOrderQueryByPhoneVo }>
     */
    @Override
    public R<CrmOrderQueryByPhoneVo> queryOrderByPhone(String phone) {
        log.info("{} query order by phone start, phone={}", LOG_PREFIX, CommonUtils.maskPhone(phone));
        if (!StringUtils.hasText(phone)) {
            log.warn("{} query order by phone rejected, phone empty", LOG_PREFIX);
            return R.error("phone不能为空");
        }

        String normalizedPhone = phone.trim();

        CrmOrderQueryByPhoneVo result = new CrmOrderQueryByPhoneVo();
        result.setPhone(normalizedPhone);

        UserVo user = unwrap(userBll.getByPhone(normalizedPhone));
        if (user == null) {
            result.setExists(Boolean.FALSE);
            result.setMissingReason(CUSTOMER_NOT_FOUND);
            log.info("{} query order by phone miss, phone={}, reason={}",
                    LOG_PREFIX, CommonUtils.maskPhone(normalizedPhone), CUSTOMER_NOT_FOUND);
            return R.ok(result);
        }

        List<OrderInfoVo> orders = defaultOrders(unwrap(orderBll.getOrderByUserId(user.getId())));
        result.setExists(Boolean.TRUE);
        result.setUserId(user.getId());
        result.setOrders(orders);
        log.info("{} query order by phone success, phone={}, userId={}, orderCount={}",
                LOG_PREFIX, CommonUtils.maskPhone(normalizedPhone), user.getId(), orders.size());
        return R.ok(result);
    }

    /**
     * 同步客户画像字段
     *
     * @param bo 画像字段同步请求
     *
     * @return {@link R }<{@link CrmProfileSyncVo }>
     */
    @Override
    public R<CrmProfileSyncVo> profileSync(CrmProfileSyncBo bo) {
        log.info("{} profile sync start, phone={}",
                LOG_PREFIX, CommonUtils.maskPhone(bo == null ? null : bo.getPhone()));
        if (bo == null || !StringUtils.hasText(bo.getPhone())) {
            log.warn("{} profile sync rejected, phone empty", LOG_PREFIX);
            return R.error("phone不能为空");
        }
        if (!StringUtils.hasText(bo.getSource())) {
            log.warn("{} profile sync rejected, phone={}, source empty",
                    LOG_PREFIX, CommonUtils.maskPhone(bo.getPhone()));
            return R.error("source不能为空");
        }
        if (!StringUtils.hasText(bo.getUpdatedAt())) {
            log.warn("{} profile sync rejected, phone={}, updatedAt empty",
                    LOG_PREFIX, CommonUtils.maskPhone(bo.getPhone()));
            return R.error("updatedAt不能为空");
        }

        String phone = bo.getPhone().trim();
        UserVo user = unwrap(userBll.getByPhone(phone));
        if (user == null) {
            log.info("{} profile sync miss, phone={}, reason={}",
                    LOG_PREFIX, CommonUtils.maskPhone(phone), CUSTOMER_NOT_FOUND);
            return R.error(CUSTOMER_NOT_FOUND);
        }

        Integer userBelongType;
        try {
            userBelongType = mapLeadType(bo.getLeadType());
        } catch (IllegalArgumentException ex) {
            log.warn("{} profile sync rejected, phone={}, leadType invalid, leadType={}",
                    LOG_PREFIX, CommonUtils.maskPhone(phone), bo.getLeadType());
            return R.error("leadType不合法");
        }

        CrmUserDetailsSyncBo syncBo = new CrmUserDetailsSyncBo();
        syncBo.setUserId(user.getId());
        syncBo.setUserAmbition(bo.getDealIntent());
        syncBo.setUserBelongType(userBelongType);

        CrmUserDetailsSyncVo syncResult = userDetailsService.saveCrmProfileFields(syncBo);

        CrmProfileSyncVo result = new CrmProfileSyncVo();
        result.setUserId(user.getId());
        result.setPhone(phone);
        result.setSource(bo.getSource().trim());
        result.setUpdatedAt(bo.getUpdatedAt().trim());
        if (syncResult != null) {
            result.setUserAmbition(syncResult.getUserAmbition());
            result.setUserBelongType(syncResult.getUserBelongType());
        }
        result.setUpdatedFields(List.of("userAmbition", "userBelongType"));
        log.info("{} profile sync success, phone={}, userId={}",
                LOG_PREFIX, CommonUtils.maskPhone(phone), user.getId());
        return R.ok(result);
    }

    /**
     * 写入 AI 画像
     *
     * @param bo AI 画像写入请求
     *
     * @return {@link R }<{@link CrmAiProfileUpsertVo }>
     */
    @Override
    public R<CrmAiProfileUpsertVo> upsertAiProfile(CrmAiProfileUpsertBo bo) {
        log.info("{} ai profile upsert start, phone={}, profileId={}",
                LOG_PREFIX,
                CommonUtils.maskPhone(bo == null ? null : bo.getPhone()),
                bo == null ? null : bo.getProfileId());
        if (bo == null || !StringUtils.hasText(bo.getPhone())) {
            log.warn("{} ai profile upsert rejected, phone empty", LOG_PREFIX);
            return R.error("phone不能为空");
        }
        if (!StringUtils.hasText(bo.getProfileId())) {
            log.warn("{} ai profile upsert rejected, phone={}, profileId empty",
                    LOG_PREFIX, CommonUtils.maskPhone(bo.getPhone()));
            return R.error("profileId不能为空");
        }
        if (!StringUtils.hasText(bo.getProfileJson())) {
            log.warn("{} ai profile upsert rejected, phone={}, profileJson empty",
                    LOG_PREFIX, CommonUtils.maskPhone(bo.getPhone()));
            return R.error("profileJson不能为空");
        }

        String phone = bo.getPhone().trim();
        UserVo user = unwrap(userBll.getByPhone(phone));
        if (user == null) {
            log.info("{} ai profile upsert miss, phone={}, profileId={}, reason={}",
                    LOG_PREFIX, CommonUtils.maskPhone(phone), bo.getProfileId(), CUSTOMER_NOT_FOUND);
            return R.error(CUSTOMER_NOT_FOUND);
        }

        CrmAiProfileBo saveBo = new CrmAiProfileBo();
        saveBo.setUserId(user.getId());
        saveBo.setProfileId(bo.getProfileId().trim());
        saveBo.setSource(StringUtils.hasText(bo.getSource()) ? bo.getSource().trim() : null);
        saveBo.setUpdatedAt(parseDateTime(bo.getUpdatedAt()));
        saveBo.setProfileJson(bo.getProfileJson());
        saveBo.setSummary(StringUtils.hasText(bo.getSummary()) ? bo.getSummary().trim() : null);
        CrmAiProfileInfoVo infoVo = crmAiProfileService.saveOrUpdateByProfileId(saveBo);

        CrmAiProfileUpsertVo result = new CrmAiProfileUpsertVo();
        result.setUserId(infoVo == null ? user.getId() : infoVo.getUserId());
        result.setProfileId(infoVo == null ? saveBo.getProfileId() : infoVo.getProfileId());
        result.setPhone(phone);
        result.setSource(infoVo == null ? saveBo.getSource() : infoVo.getSource());
        result.setUpdatedAt(bo.getUpdatedAt() == null ? null : bo.getUpdatedAt().trim());
        log.info("{} ai profile upsert success, phone={}, userId={}, profileId={}",
                LOG_PREFIX, CommonUtils.maskPhone(phone), result.getUserId(), result.getProfileId());
        return R.ok(result);
    }

    /**
     * 回传客户阶段事件
     *
     * @param bo 阶段事件回传请求
     *
     * @return {@link R }<{@link CrmStageEventVo }>
     */
    @Override
    public R<CrmStageEventVo> saveStageEvent(CrmStageEventBo bo) {
        log.info("{} stage event start, phone={}, eventId={}, stageCode={}",
                LOG_PREFIX,
                CommonUtils.maskPhone(bo == null ? null : bo.getPhone()),
                bo == null ? null : bo.getEventId(),
                bo == null ? null : bo.getStageCode());
        if (bo == null || !StringUtils.hasText(bo.getPhone())) {
            log.warn("{} stage event rejected, phone empty", LOG_PREFIX);
            return R.error("phone不能为空");
        }
        if (!StringUtils.hasText(bo.getEventId())) {
            log.warn("{} stage event rejected, phone={}, eventId empty",
                    LOG_PREFIX, CommonUtils.maskPhone(bo.getPhone()));
            return R.error("eventId不能为空");
        }
        if (!StringUtils.hasText(bo.getOccurredAt())) {
            log.warn("{} stage event rejected, phone={}, occurredAt empty",
                    LOG_PREFIX, CommonUtils.maskPhone(bo.getPhone()));
            return R.error("occurredAt不能为空");
        }

        String phone = bo.getPhone().trim();
        UserVo user = unwrap(userBll.getByPhone(phone));
        if (user == null) {
            log.info("{} stage event miss, phone={}, eventId={}, reason={}",
                    LOG_PREFIX, CommonUtils.maskPhone(phone), bo.getEventId(), CUSTOMER_NOT_FOUND);
            return R.error(CUSTOMER_NOT_FOUND);
        }

        CrmStageEventSaveBo saveBo = new CrmStageEventSaveBo();
        saveBo.setUserId(user.getId());
        saveBo.setEventId(bo.getEventId().trim());
        saveBo.setSource(StringUtils.hasText(bo.getSource()) ? bo.getSource().trim() : null);
        saveBo.setOccurredAt(parseDateTime(bo.getOccurredAt()));
        saveBo.setStageCode(StringUtils.hasText(bo.getStageCode()) ? bo.getStageCode().trim() : null);
        saveBo.setStageLabel(StringUtils.hasText(bo.getStageLabel()) ? bo.getStageLabel().trim() : null);
        saveBo.setConfidence(bo.getConfidence());
        saveBo.setSummary(StringUtils.hasText(bo.getSummary()) ? bo.getSummary().trim() : null);
        saveBo.setFactsJson(bo.getFactsJson());
        saveBo.setRawJson(bo.getRawJson());
        crmStageEventService.saveOrUpdateByEventId(saveBo);

        CrmBusinessSnapshotUpdateBo snapshot = resolveBusinessSnapshot(bo, user.getId());
        boolean businessUpdated = snapshot != null;
        if (snapshot != null) {
            crmStageEventService.updateBusinessSnapshot(snapshot);
        }

        CrmStageEventVo result = new CrmStageEventVo();
        result.setUserId(user.getId());
        result.setPhone(phone);
        result.setEventId(saveBo.getEventId());
        result.setBusinessUpdated(businessUpdated);
        log.info("{} stage event success, phone={}, userId={}, eventId={}, stageCode={}, businessUpdated={}",
                LOG_PREFIX,
                CommonUtils.maskPhone(phone),
                user.getId(),
                result.getEventId(),
                StringUtils.hasText(bo.getStageCode()) ? bo.getStageCode().trim() : null,
                result.getBusinessUpdated());
        return R.ok(result);
    }

    /**
     * 从预加载的批量数据中组装单个客户聚合结果
     */
    private CrmCustomerAggregateItemVo buildCustomerAggregateItem(
            String phone,
            Map<String, UserDto> userByPhone,
            Map<Long, SalesInfoVo> salesByUserId,
            Map<Long, List<OrderInfoVo>> ordersByUserId) {
        CrmCustomerAggregateItemVo item = new CrmCustomerAggregateItemVo();
        item.setPhone(phone);

        UserDto user = userByPhone.get(phone);
        if (user == null) {
            item.setExists(Boolean.FALSE);
            item.setMissingReason(CUSTOMER_NOT_FOUND);
            return item;
        }

        item.setExists(Boolean.TRUE);
        item.setUserId(user.getId());
        item.setCustomerName(user.getNickName());
        item.setSales(salesByUserId.get(user.getId()));
        item.setOrders(defaultOrders(ordersByUserId.get(user.getId())));
        return item;
    }

    /**
     * 归一化手机号列表
     *
     * @param bo 批量查询参数
     *
     * @return 去空、去重后的手机号列表
     */
    private List<String> normalizePhones(CrmBatchAggregateQueryBo bo) {
        if (bo == null || bo.getPhones() == null) {
            return Collections.emptyList();
        }

        return bo.getPhones().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * 兜底订单列表
     *
     * @param orders 原始订单列表
     *
     * @return 非空订单列表
     */
    private List<OrderInfoVo> defaultOrders(List<OrderInfoVo> orders) {
        return orders == null ? new ArrayList<>() : new ArrayList<>(orders);
    }

    /**
     * 将 CRM 客户类型映射为系统客户类型枚举
     *
     * @param leadType CRM 客户类型
     *
     * @return 系统客户类型
     */
    private Integer mapLeadType(String leadType) {
        if (!StringUtils.hasText(leadType)) {
            return null;
        }

        return switch (leadType.trim()) {
            case "个人" -> 0;
            case "工作室" -> 1;
            case "企业", "品牌旗舰及定制" -> 2;
            default -> throw new IllegalArgumentException("leadType不合法");
        };
    }

    /**
     * 仅对白名单阶段码生成销售快照更新参数，避免无控制覆盖现有快照。
     *
     * @param bo     阶段事件
     * @param userId 客户 ID
     *
     * @return 命中受控映射时返回快照更新参数，否则返回 {@code null}
     */
    private CrmBusinessSnapshotUpdateBo resolveBusinessSnapshot(CrmStageEventBo bo, Long userId) {
        if (bo == null || userId == null || !StringUtils.hasText(bo.getStageCode())) {
            return null;
        }

        String stageCode = bo.getStageCode().trim();
        Integer accordingStatus = switch (stageCode) {
            case "FOLLOW_UP_DONE" -> 1;
            case "WAIT_NEXT_CONTACT" -> 2;
            default -> null;
        };
        if (accordingStatus == null) {
            return null;
        }

        CrmBusinessSnapshotUpdateBo snapshot = new CrmBusinessSnapshotUpdateBo();
        snapshot.setUserId(userId);
        snapshot.setAccordingStatus(accordingStatus);
        snapshot.setAccordingContent(StringUtils.hasText(bo.getSummary())
                ? bo.getSummary().trim()
                : StringUtils.hasText(bo.getStageLabel()) ? bo.getStageLabel().trim() : null);
        snapshot.setAccordingDate(parseDateTime(bo.getOccurredAt()));
        return snapshot;
    }

    /**
     * 解析业务时间字符串
     *
     * @param value 时间字符串
     *
     * @return 解析后的时间，为空时返回 {@code null}
     */
    private Date parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return DateUtil.parse(value.trim());
    }

    /**
     * 提取统一返回对象中的数据体
     *
     * @param result 通用返回对象
     * @param <T>    数据类型
     *
     * @return 数据体，为空时返回 {@code null}
     */
    private <T> T unwrap(R<T> result) {
        return result == null ? null : result.getData();
    }

    /**
     * 批量查询客户行为信号（试用期使用深度）
     *
     * @param bo 查询条件，包含手机号列表
     *
     * @return {@link R }<{@link CrmBehaviorSignalsVo }>
     */
    @Override
    public R<CrmBehaviorSignalsVo> behaviorSignals(CrmBehaviorSignalsQueryBo bo) {
        List<String> phones = normalizeBehaviorSignalPhones(bo);
        log.info("{} behavior signals query start, size={}", LOG_PREFIX, phones.size());
        if (phones.isEmpty()) {
            log.warn("{} behavior signals query rejected, phones empty", LOG_PREFIX);
            return R.error("phones不能为空");
        }
        if (phones.size() > MAX_BATCH_SIZE) {
            log.warn("{} behavior signals query rejected, size={} exceeds limit={}",
                    LOG_PREFIX, phones.size(), MAX_BATCH_SIZE);
            return R.error("phones数量不能超过50");
        }

        // 1. 按手机号批量查用户
        List<UserDto> userList = userBll.listByPhones(phones);
        Map<String, UserDto> userByPhone = userList.stream()
                .collect(Collectors.toMap(UserDto::getPhone, Function.identity(), (a, b) -> a));

        // 2. 按 phones 顺序逐条聚合（每条独立计算，子系统失败降级零值）
        List<CrmBehaviorSignalItemVo> signals = new ArrayList<>(phones.size());
        int hitCount = 0;
        for (String phone : phones) {
            UserDto user = userByPhone.get(phone);
            CrmBehaviorSignalItemVo item = buildBehaviorSignalItem(phone, user);
            signals.add(item);
            if (Boolean.TRUE.equals(item.getExists())) {
                hitCount++;
            }
        }

        CrmBehaviorSignalsVo result = new CrmBehaviorSignalsVo();
        result.setSignals(signals);
        log.info("{} behavior signals query success, total={}, hitCount={}",
                LOG_PREFIX, signals.size(), hitCount);
        return R.ok(result);
    }

    /**
     * 组装单个手机号的行为信号
     *
     * @param phone 手机号（回显用）
     * @param user  已加载的用户（null 表示未命中）
     *
     * @return 单条行为信号
     */
    private CrmBehaviorSignalItemVo buildBehaviorSignalItem(String phone, UserDto user) {
        CrmBehaviorSignalItemVo item = new CrmBehaviorSignalItemVo();
        item.setPhone(phone);

        if (user == null) {
            applyZeroSignal(item, Boolean.FALSE);
            return item;
        }

        item.setExists(Boolean.TRUE);

        // 定位团队（租户）
        TenantInfoVo tenant = tenantProducer.infoByUserId(user.getId());
        if (tenant == null || tenant.getId() == null) {
            log.info("{} behavior signals user has no tenant, phone={}, userId={}",
                    LOG_PREFIX, CommonUtils.maskPhone(phone), user.getId());
            applyZeroSignal(item, Boolean.TRUE);
            return item;
        }

        Long tenantId = tenant.getId();

        // customerUnitSize: 团队账号合计（按 tb_user.active_tenant_id 统计未删除用户）
        item.setCustomerUnitSize(safeIntCount(userProducer.countByActiveTenantId(tenantId)));

        // addedAnchorCount / ownAnchorCount: 主播账号统计
        item.setAddedAnchorCount(safeIntCount(anchorUrlUserProducer.countByTenantId(tenantId)));
        item.setOwnAnchorCount(safeIntCount(
                anchorUrlUserProducer.countByTenantIdAndAccountType(tenantId, ACCOUNT_TYPE_OWN)));

        // hasUsedOpAssistant / hasUsedCompareReplay
        item.setHasUsedOpAssistant(Boolean.TRUE.equals(
                aiTokenUseRecordProducer.existsByTenantIdAndAssistantType(tenantId, ASSISTANT_TYPE_OP)));
        item.setHasUsedCompareReplay(Boolean.TRUE.equals(
                syncContrastProducer.existsByTenantId(tenantId)));

        // serviceDurationHours: 仅主账号注册至今
        item.setServiceDurationHours(calcServiceDurationHours(tenant.getUserId()));

        // aiAnalysisTimeConsumed: 团队累计 AI 语音分析时长（秒）
        item.setAiAnalysisTimeConsumed(safeLong(anchorVideoProducer.sumAnalyzedDurationByTenantId(tenantId)));

        // Token 消耗：全量 / 近 30 天 / 近 7 天
        Date now = new Date();
        Date since30d = naturalDaysAgo(now, 30);
        Date since7d = naturalDaysAgo(now, 7);
        Long mainUserId = Objects.equals(user.getUserType(), 0) ? user.getId() : user.getParentId();
        item.setTotalAiTokensUsed(safeLong(
                userPropertyDetailsProducer.sumTotalTokensByTenant(mainUserId, null)));
        item.setRecentAiTokensUsed30d(safeLong(
            userPropertyDetailsProducer.sumTotalTokensByTenant(mainUserId, since30d)));
        item.setRecentAiTokensUsed7d(safeLong(
            userPropertyDetailsProducer.sumTotalTokensByTenant(mainUserId, since7d)));

        return item;
    }

    /**
     * 把行为信号字段填零（未命中或无租户场景）
     *
     * @param item   行为信号项
     * @param exists 是否存在客户
     */
    private void applyZeroSignal(CrmBehaviorSignalItemVo item, Boolean exists) {
        item.setExists(exists);
        item.setCustomerUnitSize(0);
        item.setAddedAnchorCount(0);
        item.setOwnAnchorCount(0);
        item.setHasUsedOpAssistant(Boolean.FALSE);
        item.setHasUsedCompareReplay(Boolean.FALSE);
        item.setServiceDurationHours(0);
        item.setAiAnalysisTimeConsumed(0L);
        item.setTotalAiTokensUsed(0L);
        item.setRecentAiTokensUsed30d(0L);
        item.setRecentAiTokensUsed7d(0L);
    }

    /**
     * 计算主账号注册至今的服务时长（小时）
     *
     * @param masterUserId 团队主账号用户id
     *
     * @return 服务时长（小时），未命中返回 0
     */
    private Integer calcServiceDurationHours(Long masterUserId) {
        if (masterUserId == null) {
            return 0;
        }
        UserVo master = unwrap(userBll.getById(masterUserId));
        if (master == null || master.getCreateDate() == null) {
            return 0;
        }
        long millis = System.currentTimeMillis() - master.getCreateDate().getTime();
        if (millis <= 0) {
            return 0;
        }
        return (int) Duration.ofMillis(millis).toHours();
    }

    /**
     * 取自然日窗口起点：今日 0 点回退 days 天
     *
     * @param now  当前时间
     * @param days 回退自然日数
     *
     * @return 窗口起点
     */
    private Date naturalDaysAgo(Date now, int days) {
        LocalDate startDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().minusDays(days - 1L);
        return Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Integer 计数兜底
     *
     * @param value 计数原始值
     *
     * @return 非 null 计数
     */
    private Integer safeIntCount(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Long 求和兜底
     *
     * @param value 求和原始值
     *
     * @return 非 null 求和
     */
    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 归一化行为信号查询手机号（去空、去重、保留入参顺序）
     *
     * @param bo 查询参数
     *
     * @return 去重后的手机号列表
     */
    private List<String> normalizeBehaviorSignalPhones(CrmBehaviorSignalsQueryBo bo) {
        if (bo == null || bo.getPhones() == null) {
            return Collections.emptyList();
        }
        return bo.getPhones().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }
}
