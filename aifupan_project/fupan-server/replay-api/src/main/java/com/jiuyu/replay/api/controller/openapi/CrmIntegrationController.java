package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.service.crm.CrmIntegrationService;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bo.crm.*;
import com.jiuyu.replay.power.vo.crm.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CRM 集成查询控制器
 * 对销售智能体开放客户聚合查询和订单查询能力
 *
 * 说明：
 * 1. 当前接口统一走内部 `/internal/crm` 路径
 * 2. 统一复用 `@APIKey` 做请求鉴权
 */
@RestController
@APIKey
@Slf4j
@AllArgsConstructor
@RequestMapping("/internal/crm")
@Tag(name = "CRM 集成查询接口")
@Validated
public class CrmIntegrationController {

    private static final String LOG_PREFIX = "[CRM-INTEGRATION][INBOUND]";

    private final CrmIntegrationService crmIntegrationService;

    /**
     * 批量聚合查询客户信息
     *
     * @param bo 查询条件，包含手机号列表
     *
     * @return {@link R }<{@link CrmBatchAggregateQueryVo }>
     */
    @PostMapping("/customer/batch-aggregate-query")
    @Operation(summary = "批量聚合查询客户信息")
    public R<CrmBatchAggregateQueryVo> batchAggregateQuery(@Valid @RequestBody CrmBatchAggregateQueryBo bo) {
        log.info("{} batch aggregate query received, requestSize={}", LOG_PREFIX, countPhones(bo));
        return crmIntegrationService.batchAggregateQuery(bo);
    }

    /**
     * 按手机号查询订单
     *
     * @param phone 客户手机号
     *
     * @return {@link R }<{@link CrmOrderQueryByPhoneVo }>
     */
    @GetMapping("/order/query-by-phone")
    @Operation(summary = "按手机号查询订单")
    public R<CrmOrderQueryByPhoneVo> queryOrderByPhone(@RequestParam String phone) {
        if (EmptyUtil.isEmpty(phone)) {
            return R.error("缺少手机号码");
        }
        log.info("{} query order by phone received, phone={}", LOG_PREFIX, CommonUtils.maskPhone(phone));
        return crmIntegrationService.queryOrderByPhone(phone);
    }

    /**
     * 同步客户画像字段
     *
     * @param bo 画像字段同步请求
     *
     * @return {@link R }<{@link CrmProfileSyncVo }>
     */
    @PostMapping("/customer/profile-sync")
    @Operation(summary = "同步客户画像字段")
    public R<CrmProfileSyncVo> profileSync(@Valid @RequestBody CrmProfileSyncBo bo) {
        log.info("{} profile sync received, phone={}",
            LOG_PREFIX, CommonUtils.maskPhone(bo == null ? null : bo.getPhone()));
        return crmIntegrationService.profileSync(bo);
    }

    /**
     * 写入 AI 画像
     *
     * @param bo AI 画像写入请求
     *
     * @return {@link R }<{@link CrmAiProfileUpsertVo }>
     */
    @PostMapping("/ai-profile/upsert")
    @Operation(summary = "写入AI画像")
    public R<CrmAiProfileUpsertVo> upsertAiProfile(@Valid @RequestBody CrmAiProfileUpsertBo bo) {
        log.info("{} ai profile upsert received, phone={}, profileId={}",
            LOG_PREFIX,
            CommonUtils.maskPhone(bo == null ? null : bo.getPhone()),
            bo == null ? null : bo.getProfileId());
        return crmIntegrationService.upsertAiProfile(bo);
    }

    /**
     * 回传客户阶段事件
     *
     * @param bo 阶段事件回传请求
     *
     * @return {@link R }<{@link CrmStageEventVo }>
     */
    @PostMapping("/customer/stage-event")
    @Operation(summary = "回传客户阶段事件")
    public R<CrmStageEventVo> saveStageEvent(@Valid @RequestBody CrmStageEventBo bo) {
        log.info("{} stage event received, phone={}, eventId={}",
            LOG_PREFIX,
            CommonUtils.maskPhone(bo == null ? null : bo.getPhone()),
            bo == null ? null : bo.getEventId());
        return crmIntegrationService.saveStageEvent(bo);
    }

    /**
     * 批量查询客户行为信号（试用期使用深度）
     *
     * @param bo 查询条件，包含手机号列表
     *
     * @return {@link R }<{@link CrmBehaviorSignalsVo }>
     */
    @PostMapping("/customer/behavior-signals")
    @Operation(summary = "批量查询客户行为信号")
    public R<CrmBehaviorSignalsVo> behaviorSignals(@Valid @RequestBody CrmBehaviorSignalsQueryBo bo) {
        log.info("{} behavior signals received, requestSize={}",
            LOG_PREFIX, bo == null || bo.getPhones() == null ? 0 : bo.getPhones().size());
        return crmIntegrationService.behaviorSignals(bo);
    }

    /**
     * 统计手机号数量
     *
     * @param bo 批量查询参数
     *
     * @return 手机号数量
     */
    private int countPhones(CrmBatchAggregateQueryBo bo) {
        return bo == null || bo.getPhones() == null ? 0 : bo.getPhones().size();
    }
}
