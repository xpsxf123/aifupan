package com.jiuyu.replay.order.producer.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.entity.PowerRollupWatermarkEntity;
import com.jiuyu.replay.order.entity.TenantPowerRollupEntity;
import com.jiuyu.replay.order.entity.UserPowerRollupEntity;
import com.jiuyu.replay.order.producer.PowerRollupProducer;
import com.jiuyu.replay.order.repository.service.PowerRollupWatermarkService;
import com.jiuyu.replay.order.repository.service.TenantPowerRollupService;
import com.jiuyu.replay.order.repository.service.UserPowerRollupService;
import com.jiuyu.replay.order.vo.PowerAggregateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 算力消耗汇总刷新
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Slf4j
@Service
public class PowerRollupProducerImpl implements PowerRollupProducer {

    /**
     * 算力资产 key，本轮唯一处理的商品类型
     */
    private static final String AI_TOKEN_CODE = "aiTokenNum";

    /**
     * 水位线业务标识
     */
    private static final String WATERMARK_BIZ_CODE = "aiTokenNum";

    /**
     * 批量 upsert 分批大小
     */
    private static final int BATCH_SIZE = 500;

    private final UserPowerRollupService userPowerRollupService;

    private final TenantPowerRollupService tenantPowerRollupService;

    private final PowerRollupWatermarkService powerRollupWatermarkService;

    /**
     * 构造器注入
     *
     * @param userPowerRollupService      用户算力汇总 service
     * @param tenantPowerRollupService    租户算力汇总 service
     * @param powerRollupWatermarkService 水位线 service
     */
    public PowerRollupProducerImpl(UserPowerRollupService userPowerRollupService,
                                   TenantPowerRollupService tenantPowerRollupService,
                                   PowerRollupWatermarkService powerRollupWatermarkService) {
        this.userPowerRollupService = userPowerRollupService;
        this.tenantPowerRollupService = tenantPowerRollupService;
        this.powerRollupWatermarkService = powerRollupWatermarkService;
    }

    /**
     * 增量刷新用户/租户算力消耗汇总表
     * 累加写入与水位线推进必须在本方法这一个事务内提交，否则丧失恰好一次语义
     *
     * @throws BusinessException 水位线行缺失（存量未初始化）时抛出，使定时任务对外呈现为失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshPowerRollup() {
        // 1. 读水位线；不存在说明存量未初始化，抛业务异常终止，禁止退化为全量跑。
        //    这里必须抛异常而非静默 return——否则 XXL-Job Admin 会显示「执行成功」，
        //    运维收不到告警，汇总长期不产出也无人发现。
        PowerRollupWatermarkEntity watermark = powerRollupWatermarkService.getByBizCode(WATERMARK_BIZ_CODE);
        if (watermark == null || watermark.getLastDetailId() == null) {
            log.error("[定时更新算力消耗汇总] 水位线未初始化 bizCode={}，请先执行 sql/replay-41.sql 尾部的存量初始化脚本，本次终止",
                    WATERMARK_BIZ_CODE);
            throw new BusinessException("算力汇总水位线未初始化 bizCode=" + WATERMARK_BIZ_CODE
                    + "，请先执行 sql/replay-41.sql 尾部的存量初始化脚本");
        }
        Long lastDetailId = watermark.getLastDetailId();

        // 2. 唯一一条增量聚合 SQL（PK 范围扫，不 JOIN 任何表）
        List<PowerAggregateVo> rows = userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, lastDetailId);
        if (CollUtil.isEmpty(rows)) {
            log.info("[定时更新算力消耗汇总] 本轮无新增明细 lastDetailId={}，不推进水位线", lastDetailId);
            return;
        }

        // 本轮新上界 = 实际扫描到的明细最大 id（只覆盖本次已聚合的行）
        long newWatermark = lastDetailId;
        for (PowerAggregateVo row : rows) {
            if (row.getMaxDetailId() != null && row.getMaxDetailId() > newWatermark) {
                newWatermark = row.getMaxDetailId();
            }
        }

        // 3. 用户维度：聚合 SQL 已按 (user_id, tenant_id) 分组，每行即唯一 (用户,租户)。
        //    同一用户在不同租户消费会有多行，按复合键各自落表（对应 uk_user_tenant）。
        //    此处仅归一化 tenant_id 的 null → 0（与列 NOT NULL DEFAULT 0 对齐）。
        // 4. 租户维度内存归并（不对明细表跑第二条 GROUP BY），key = tenant_id
        Map<Long, Long> tenantDeltaMap = new HashMap<>(rows.size());
        for (PowerAggregateVo row : rows) {
            long delta = row.getPowerConsume() == null ? 0L : row.getPowerConsume();
            Long tid = row.getTenantId() == null ? 0L : row.getTenantId();
            row.setTenantId(tid);
            tenantDeltaMap.merge(tid, delta, Long::sum);
        }

        Date now = new Date();
        int userRows = accumulateUserRollup(rows, now);
        int tenantRows = accumulateTenantRollup(tenantDeltaMap, now);

        // 5. 同一事务内推进水位线
        watermark.setLastDetailId(newWatermark);
        watermark.setUpdateDate(now);
        powerRollupWatermarkService.updateById(watermark);

        log.info("[定时更新算力消耗汇总] 聚合行数={} 用户表落表={} 租户表落表={} 水位线 {} -> {}",
                rows.size(), userRows, tenantRows, lastDetailId, newWatermark);
    }

    /**
     * 用户维度累加落表，按 (user_id, tenant_id) 复合键（对应 uk_user_tenant）。
     * 同一用户在不同租户消费 → 各自一行。存在则 += delta，不存在则 save 初值 = delta。
     * 为私有方法，由带 @Transactional 的 refreshPowerRollup 直接调用，共用同一事务。
     *
     * @param rows 本轮聚合行（每行含 userId + tenantId + powerConsume(净增量)，SQL 已按 (user,tenant) 去重）
     * @param now  刷新时间
     *
     * @return 落表行数
     */
    private int accumulateUserRollup(List<PowerAggregateVo> rows, Date now) {
        if (CollUtil.isEmpty(rows)) {
            return 0;
        }
        int affected = 0;
        for (List<PowerAggregateVo> batch : CollUtil.split(rows, BATCH_SIZE)) {
            // 按本批 userIds 读现存 (user_id, tenant_id)；不加 is_deleted 过滤，防未来软删漏读 → save → 撞唯一键
            Set<Long> userIds = new HashSet<>();
            for (PowerAggregateVo r : batch) {
                userIds.add(r.getUserId());
            }
            List<UserPowerRollupEntity> exists = userPowerRollupService.list(
                    new LambdaQueryWrapper<UserPowerRollupEntity>()
                            .select(UserPowerRollupEntity::getUserId, UserPowerRollupEntity::getTenantId)
                            .in(UserPowerRollupEntity::getUserId, userIds));
            Set<String> existKeys = new HashSet<>(exists.size());
            for (UserPowerRollupEntity exist : exists) {
                existKeys.add(exist.getUserId() + "_" + exist.getTenantId());
            }

            List<PowerAggregateVo> updateList = new ArrayList<>();
            List<UserPowerRollupEntity> saveList = new ArrayList<>();
            for (PowerAggregateVo r : batch) {
                if (existKeys.contains(r.getUserId() + "_" + r.getTenantId())) {
                    updateList.add(r);
                } else {
                    UserPowerRollupEntity entity = new UserPowerRollupEntity();
                    entity.setId(SnowflakeManager.nextValue());
                    entity.setUserId(r.getUserId());
                    entity.setTenantId(r.getTenantId());
                    entity.setUserPowerConsume(r.getPowerConsume());
                    entity.setIsDeleted(0);
                    entity.setCreateDate(now);
                    entity.setUpdateDate(now);
                    saveList.add(entity);
                }
            }

            if (CollUtil.isNotEmpty(updateList)) {
                affected += userPowerRollupService.accumulateUserPowerConsume(updateList, now);
            }
            if (CollUtil.isNotEmpty(saveList)) {
                userPowerRollupService.saveBatch(saveList);
                affected += saveList.size();
            }
        }
        return affected;
    }

    /**
     * 租户维度累加落表（存在则 += delta，不存在则 save 初值 = delta）
     * 为私有方法，由带 @Transactional 的 refreshPowerRollup 直接调用，共用同一事务
     *
     * @param deltaMap 租户id(tb_tenant.id) → 本轮净增量
     * @param now      刷新时间
     *
     * @return 落表行数
     */
    private int accumulateTenantRollup(Map<Long, Long> deltaMap, Date now) {
        if (CollUtil.isEmpty(deltaMap)) {
            return 0;
        }
        int affected = 0;
        List<Long> allTenantIds = new ArrayList<>(deltaMap.keySet());
        for (List<Long> batchIds : CollUtil.split(allTenantIds, BATCH_SIZE)) {
            // 读现存行不加 is_deleted 过滤，防未来软删漏读 → save → 撞唯一键
            List<TenantPowerRollupEntity> exists = tenantPowerRollupService.list(
                    new LambdaQueryWrapper<TenantPowerRollupEntity>()
                            .select(TenantPowerRollupEntity::getTenantId)
                            .in(TenantPowerRollupEntity::getTenantId, batchIds));
            Set<Long> existIds = new HashSet<>(exists.size());
            for (TenantPowerRollupEntity exist : exists) {
                existIds.add(exist.getTenantId());
            }

            List<PowerAggregateVo> updateList = new ArrayList<>();
            List<TenantPowerRollupEntity> saveList = new ArrayList<>();
            for (Long tenantId : batchIds) {
                Long delta = deltaMap.get(tenantId);
                if (existIds.contains(tenantId)) {
                    PowerAggregateVo vo = new PowerAggregateVo();
                    vo.setTenantId(tenantId);
                    vo.setPowerConsume(delta);
                    updateList.add(vo);
                } else {
                    TenantPowerRollupEntity entity = new TenantPowerRollupEntity();
                    entity.setId(SnowflakeManager.nextValue());
                    entity.setTenantId(tenantId);
                    entity.setTenantPowerConsume(delta);
                    entity.setIsDeleted(0);
                    entity.setCreateDate(now);
                    entity.setUpdateDate(now);
                    saveList.add(entity);
                }
            }

            if (CollUtil.isNotEmpty(updateList)) {
                affected += tenantPowerRollupService.accumulateTenantPowerConsume(updateList, now);
            }
            if (CollUtil.isNotEmpty(saveList)) {
                tenantPowerRollupService.saveBatch(saveList);
                affected += saveList.size();
            }
        }
        return affected;
    }
}
