package com.jiuyu.replay.api.service.aiagent.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.service.aiagent.AiAgentQuotaService;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.utils.AiUtils;
import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaReleaseBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaReserveBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaSettleBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.ai.AiModelListVo;
import com.jiuyu.replay.generic.vo.aiagent.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.OrderDetailBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.IsPropertyHaveBo;
import com.jiuyu.replay.order.vo.IsPropertyHaveVo;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.bo.AiModelListBo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * AI Agent 算力额度收口实现：薄封装爱复盘既有 aiToken 资产原语 + requestId 幂等去重。
 *
 * <p><b>复用</b>：预扣 {@link UserPropertyBll#isPropertyHaveAiToken}、扣减 {@link UserPropertyImpl#use}
 * （含钳制到余额 + 经 {@code clearWithholdCache} 释放预扣）、释放 {@link UserPropertyBll#removeTempUserProperty}、
 * 余额 {@link UserPropertyBll#getUserProperty}。<b>不改动任何既有方法/接口</b>。</p>
 *
 * <p><b>幂等</b>（Redis，无新增库表）：</p>
 * <ul>
 *   <li>reserve：按 requestId 缓存预扣结果（TTL=预扣 30min），重试返回同一预扣，不重复占位。</li>
 *   <li>settle：requestId 占位令牌——<b>永不重复扣</b>。占位成功者执行扣减并写回结果；并发/崩溃中途的重试一律返回
 *       duplicate 不再扣（宁少勿多：极端崩溃只会少扣一轮，可监控，绝不双扣）。扣减抛异常（未实际扣）则删占位允许重试。</li>
 *   <li>release：删 Redis 预扣，天然幂等。</li>
 * </ul>
 *
 * @author fupan-server
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentQuotaServiceImpl implements AiAgentQuotaService {

    /** aiToken 资产 code（与既有 AiRelatedLogicImpl 同口径）。 */
    private static final String AI_TOKEN_KEY = "aiTokenNum";
    /** reserveId 编码分隔符（withholdId#redisId；withholdId 自身含 '_'，故用 '#'）。 */
    private static final char RESERVE_SEP = '#';
    private static final String RESERVE_KEY_PREFIX = "aiagent:quota:reserve:";
    private static final String SETTLE_KEY_PREFIX = "aiagent:quota:settle:";
    /** 结算占位/结果 TTL（小时）：覆盖一天内对账重试窗口。 */
    private static final long SETTLE_TTL_HOURS = 25L;
    /** 结算占位令牌（区别于已写回的结果 JSON）。 */
    private static final String SETTLE_INPROGRESS = "PENDING";

    private final UserPropertyBll userPropertyBll;
    private final RedisTemplate<String, Object> redisTemplate;

    private final AiModelBll aiModelBll;
    private final SystemKvBll systemKvBll;
    private final OrderBll orderBll;
    private final OrderDetailBll orderDetailBll;

    /**
     * 缓存计费倍率配置键，缺省 "1"（不打折）
     */
    private static final String KEY_AI_CACHE_TOKEN_RATE = "ai_cache_token_rate";

    /**
     * 模型列表全量拉取的页大小（模型表为小表，单页足够覆盖）
     */
    private static final int MODEL_QUERY_LIMIT = 1000;


    /**
     * AI 算力计费系数配置（全局默认倍率 + 缓存倍率 + 各模型消耗倍率）。
     *
     * <p>供 AI Agent 客户端本地复刻服务端计费公式。系数为全局/模型级配置，与身份无关，
     * 入参仅用于鉴权一致性与日志，不参与数据过滤。</p>
     *
     * @param bo 身份
     *
     * @return 计费系数配置
     */
    @Override
    public R<AiCoefficientConfigVo> coefficientConfig(AiAgentBaseBo bo) {
        R<AiCoefficientConfigVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        AiCoefficientConfigVo vo = new AiCoefficientConfigVo();
        // 全局：默认消耗倍率（AiUtils 内置基准）+ 缓存计费倍率（systemKv）
        vo.setDefaultConsumeMultiple(AiUtils.getDefaultConsumeMultiple());
        vo.setCacheRate(NumberUtil.parseDouble(systemKvBll.getValueByKey(KEY_AI_CACHE_TOKEN_RATE, "1")));

        // 每模型：消耗倍率（模型表为小表，单页全量拉取）
        AiModelListBo query = new AiModelListBo();
        query.setLimit(MODEL_QUERY_LIMIT);
        PageUtils<AiModelListVo> page = aiModelBll.queryPage(query).getData();
        List<AiCoefficientConfigVo.ModelMultiple> models = new ArrayList<>();
        if (page != null && CollUtil.isNotEmpty(page.getList())) {
            for (AiModelListVo m : page.getList()) {
                AiCoefficientConfigVo.ModelMultiple item = new AiCoefficientConfigVo.ModelMultiple();
                item.setModelCode(m.getModelCode());
                item.setModelName(m.getModelName());
                item.setConsumeMultiple(m.getConsumeMultiple());
                models.add(item);
            }
        }
        vo.setModels(models);
        return R.ok(vo);
    }

    @Override
    public R<QuotaBalanceVo> balance(AiAgentBaseBo bo) {
        R<QuotaBalanceVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        UserPropertyTypeInfoVo asset = getAiTokenNum(bo.getUserId());
        long total = asset == null || asset.getTotalQuantity() == null ? 0L : asset.getTotalQuantity();
        long used = asset == null || asset.getUseQuantity() == null ? 0L : asset.getUseQuantity();
        QuotaBalanceVo vo = new QuotaBalanceVo();
        vo.setTotal(total);
        vo.setUsed(used);
        vo.setAvailable(Math.max(0L, total - used));
        fillPackageDates(bo.getUserId(), vo);
        return R.ok(vo);
    }

    /**
     * 补充套餐时间信息：套餐过期时间 + aiToken 资产下次重置时间。
     *
     * <p>套餐 = 当前生效中的非增量包订单（commodityType=1，子账号自动归到主账号，
     * 与 {@link UserPropertyBll#getSharedPropertyByCode} 的 owner 口径一致）。
     * 下次重置时间取该订单明细中 aiTokenNum 资产的 nextReset（不重置的套餐为 null）。</p>
     */
    private void fillPackageDates(Long userId, QuotaBalanceVo vo) {
        OrderInfoVo order = orderBll.orderByUserId(userId);
        if (order == null) {
            return;
        }
        vo.setPackageExpireDate(order.getEndDate());
        R<List<OrderDetailInfoVo>> details = orderDetailBll.listByOrderId(order.getId());
        if (details == null || CollUtil.isEmpty(details.getData())) {
            return;
        }
        details.getData().stream()
                .filter(d -> AI_TOKEN_KEY.equals(d.getCommodityTypeCode()))
                .map(OrderDetailInfoVo::getNextReset)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(vo::setNextResetDate);
    }

    @Override
    public R<QuotaReserveVo> reserve(QuotaReserveBo bo) {
        R<QuotaReserveVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        String key = RESERVE_KEY_PREFIX + bo.getRequestId();
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            // 幂等：同一 requestId 重试返回同一预扣，绝不二次占位。
            return R.ok(JSONUtil.toBean(cached.toString(), QuotaReserveVo.class));
        }
        IsPropertyHaveBo have = new IsPropertyHaveBo();
        have.setUserId(bo.getUserId());
        have.setCode(AI_TOKEN_KEY);
        have.setThisUseNum(bo.getEstimateTokens());
        R<IsPropertyHaveVo> r = userPropertyBll.isPropertyHaveAiToken(have);
        if (r == null || r.getCode() == null || r.getCode() != 0 || r.getData() == null) {
            // 预扣校验自身异常 → 系统错误，交由 anchor 侧 fail-open 处理（放行 + 落 outbox 对账）。
            log.warn("[ai-quota] reserve 校验异常 userId={} requestId={} resp={}", bo.getUserId(), bo.getRequestId(), r);
            return R.error("额度预扣校验失败");
        }
        IsPropertyHaveVo data = r.getData();
        long available = currentAvailable(bo.getUserId());
        QuotaReserveVo vo = new QuotaReserveVo();
        if (!Boolean.TRUE.equals(data.getIsHave())) {
            // 余额不足：放行=false。不缓存（后续充值后重试应重新校验）。
            vo.setAllowed(false);
            vo.setReason("算力不足");
            vo.setAvailable(available);
            return R.ok(vo);
        }
        vo.setAllowed(true);
        vo.setReserveId(encodeReserve(data.getWithholdId(), data.getRedisId()));
        vo.setAvailable(available);
        // 缓存预扣结果，TTL 对齐预扣 TTL（30min），保证重试返回同一预扣不重复占位。
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(vo), UserPropertyBll.WITHHOLD_TIMEOUT, TimeUnit.MINUTES);
        return R.ok(vo);
    }

    @Override
    public R<QuotaSettleVo> settle(QuotaSettleBo bo) {
        R<QuotaSettleVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        String key = SETTLE_KEY_PREFIX + bo.getRequestId();
        // 占位：抢到者唯一执行扣减；抢不到=并发/已结算/崩溃中途，一律不重复扣。
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, SETTLE_INPROGRESS, SETTLE_TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(acquired)) {
            Object exist = redisTemplate.opsForValue().get(key);
            String s = exist == null ? null : exist.toString();
            if (s == null || SETTLE_INPROGRESS.equals(s)) {
                // 并发在途 / 上次崩溃中途：不重复扣，返回 duplicate（settled=false，交对账后续判定）。
                QuotaSettleVo vo = new QuotaSettleVo();
                vo.setSettled(false);
                vo.setDeducted(0L);
                vo.setDuplicate(true);
                return R.ok(vo);
            }
            // 已结算：返回首次结果（标记 duplicate）。
            QuotaSettleVo vo = JSONUtil.toBean(s, QuotaSettleVo.class);
            vo.setDuplicate(true);
            return R.ok(vo);
        }
        try {
            long[] r = deduct(bo.getUserId(), bo.getBilledTokens(), bo.getReserveId());
            QuotaSettleVo vo = new QuotaSettleVo();
            vo.setSettled(true);
            vo.setDeducted(r[0]);
            vo.setBalanceAfter(r[1]);
            vo.setDuplicate(false);
            // 写回结果覆盖占位（后续重试据此返回 duplicate 已结算）。
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(vo), SETTLE_TTL_HOURS, TimeUnit.HOURS);
            log.info("[ai-quota] settle 成功 userId={} requestId={} billed={} deducted={} balanceAfter={}",
                    bo.getUserId(), bo.getRequestId(), bo.getBilledTokens(), r[0], r[1]);
            return R.ok(vo);
        } catch (Exception e) {
            // 扣减抛异常视为未实际扣减 → 删占位允许对账重试。崩溃（进程死）不会走到这，占位保留 → 重试不重复扣。
            redisTemplate.delete(key);
            log.error("[ai-quota] settle 扣减失败 userId={} requestId={} err={}",
                    bo.getUserId(), bo.getRequestId(), e.getMessage(), e);
            return R.error("结算失败");
        }
    }

    @Override
    public R<QuotaReleaseVo> release(QuotaReleaseBo bo) {
        R<QuotaReleaseVo> invalid = validateIdentity(bo);
        if (invalid != null) {
            return invalid;
        }
        String reserveId = bo.getReserveId();
        int idx = reserveId.lastIndexOf(RESERVE_SEP);
        if (idx > 0) {
            String withholdId = reserveId.substring(0, idx);
            Long redisId = parseLongOrNull(reserveId.substring(idx + 1));
            userPropertyBll.removeTempUserProperty(withholdId, redisId);
        }
        // 释放后清掉 reserve 幂等缓存：该 requestId 不应再被当作有效预扣复用。
        redisTemplate.delete(RESERVE_KEY_PREFIX + bo.getRequestId());
        QuotaReleaseVo vo = new QuotaReleaseVo();
        vo.setReleased(true);
        return R.ok(vo);
    }

    /**
     * 扣减：钳制到当前余额（不透支），经 {@link UserPropertyImpl#use} 落账并释放预扣。
     *
     * @return {@code [实际扣减量, 扣减后可用额度]}
     */
    private long[] deduct(Long userId, Long billed, String reserveId) {
        long want = billed == null ? 0L : Math.max(0L, billed);
        UserPropertyTypeInfoVo asset = getAiTokenNum(userId);
        long available = 0L;
        long actual = 0L;
        if (asset != null) {
            long total = asset.getTotalQuantity() == null ? 0L : asset.getTotalQuantity();
            long used = asset.getUseQuantity() == null ? 0L : asset.getUseQuantity();
            available = Math.max(0L, total - used);
            actual = Math.min(want, available);   // 钳制：单轮最多扣到余额见底，不透支为负
        }
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(userId);
        assets.setUserName(userId + "");
        assets.setCode(AI_TOKEN_KEY);
        assets.setNum(-actual);   // 负值=扣减
        if (StrUtil.isNotBlank(reserveId)) {
            int idx = reserveId.lastIndexOf(RESERVE_SEP);
            if (idx > 0) {
                assets.setWithholdId(reserveId.substring(0, idx));
                assets.setRedisId(parseLongOrNull(reserveId.substring(idx + 1)));
            }
        }
        // 落账同时经回调释放预扣（与既有 aiTokenUse 同款；num=0 也照常释放预扣）。
        assets.setClearWithholdCache(userPropertyBll::removeTempUserProperty);
        UserPropertyImpl.use(assets);
        return new long[]{actual, Math.max(0L, available - actual)};
    }

    /**
     * 取用户 aiToken 资产（单 code 缓存直读，与既有预扣 isPropertyHaveAiToken 同一 owner 口径）。
     *
     * <p>原走 {@code getUserProperty} 全量拉取所有资产类型再过滤，按商品类型逐个 Redis GET；改为
     * {@link UserPropertyBll#getSharedPropertyByCode} 仅读单个 aiTokenNum 的总量/已用缓存，降低查询开销。
     * 缓存缺省时返回 total=0/used=0 的资产对象（语义等价于原先的 null，下游按 0 处理）。</p>
     */
    private UserPropertyTypeInfoVo getAiTokenNum(Long userId) {
        return userPropertyBll.getSharedPropertyByCode(userId, AI_TOKEN_KEY);
    }

    private long currentAvailable(Long userId) {
        UserPropertyTypeInfoVo asset = getAiTokenNum(userId);
        if (asset == null) {
            return 0L;
        }
        long total = asset.getTotalQuantity() == null ? 0L : asset.getTotalQuantity();
        long used = asset.getUseQuantity() == null ? 0L : asset.getUseQuantity();
        return Math.max(0L, total - used);
    }

    private static String encodeReserve(String withholdId, Long redisId) {
        return withholdId + RESERVE_SEP + redisId;
    }

    private static Long parseLongOrNull(String s) {
        try {
            return Long.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private <T> R<T> validateIdentity(AiAgentBaseBo bo) {
        if (bo == null || bo.getUserId() == null || bo.getTenantId() == null || bo.getUserType() == null) {
            return R.error("身份参数缺失：userId/tenantId/userType 必填");
        }
        Integer t = bo.getUserType();
        if (t != 0 && t != 1 && t != 2) {
            return R.error("userType 非法，仅支持 0/1/2");
        }
        return null;
    }
}
