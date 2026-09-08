package com.jiuyu.replay.api.task;

import com.jiuyu.replay.common.cache.HybridLockService;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.common.repository.service.CacheFallbackDataService;
import com.jiuyu.replay.power.producer.UserTokenProducer;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis健康检查服务，定期检查Redis状态并自动恢复
 *
 * @author RayChou
 * @date 2025/7/2
 */
@Slf4j
@Component
public class RedisHealthCheckerTasks {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ResilientRedisTemplate<?, ?> resilientRedisTemplate;

    @Resource
    private HybridLockService hybridLockService;

    @Resource
    private UserTokenProducer userTokenProducer;

    @Resource
    private CacheFallbackDataService cacheFallbackDataService;

    @Value("${redis.health-check.key:health:check:key}")
    private String healthCheckKey;

    /**
     * 统一的Redis状态控制接口
     * 支持自动健康检查、手动降级和手动恢复
     *
     * @param param 任务参数：
     *              - "check" 或 空值: 自动健康检查（默认）
     *              - "degrade" 或 "down": 手动降级
     *              - "recover" 或 "up": 手动恢复
     */
    @XxlJob("redisControl")
    public ReturnT<String> redisControl() {
        String param = XxlJobHelper.getJobParam();

        // 解析参数，默认为健康检查
        if (param == null || param.trim().isEmpty() || "check".equalsIgnoreCase(param)) {
            return performHealthCheck();
        } else if ("degrade".equalsIgnoreCase(param) || "down".equalsIgnoreCase(param)) {
            return performManualDegrade();
        } else if ("recover".equalsIgnoreCase(param) || "up".equalsIgnoreCase(param)) {
            return performManualRecover();
        } else {
            log.warn("[Redis降级] 未知参数: {}，执行默认健康检查", param);
            return performHealthCheck();
        }
    }

    /**
     * 执行健康检查（原checkRedisHealth逻辑）
     */
    private ReturnT<String> performHealthCheck() {
        if (resilientRedisTemplate.isDegraded() || hybridLockService.isDegraded()) {
            log.debug("[Redis降级] 执行Redis健康检查...");
            try {
                // 尝试写入Redis，验证连接是否恢复
                stringRedisTemplate.opsForValue().set(healthCheckKey, "1", 10, TimeUnit.SECONDS);

                // 检查并退出降级模式
                boolean cacheRecovered = resilientRedisTemplate.checkAndExitDegradedMode();
                boolean lockRecovered = hybridLockService.checkAndExitDegradedMode();

                // 如果服务恢复，执行恢复后处理
                handleServiceRecovery(cacheRecovered, lockRecovered);

            } catch (Exception e) {
                log.debug("[Redis降级] Redis仍然不可用: {}", e.getMessage());
            }
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 执行手动降级
     */
    private ReturnT<String> performManualDegrade() {
        log.info("[Redis降级] 开始执行手动降级任务...");

        boolean cacheCurrentlyDegraded = resilientRedisTemplate.isDegraded();
        boolean lockCurrentlyDegraded = hybridLockService.isDegraded();

        if (cacheCurrentlyDegraded && lockCurrentlyDegraded) {
            log.info("[Redis降级] Redis缓存和锁服务已处于降级状态，无需重复降级");
            return ReturnT.SUCCESS;
        }

        try {
            // 手动触发缓存服务降级
            if (!cacheCurrentlyDegraded) {
                resilientRedisTemplate.setDegraded(true);
                log.info("[Redis降级] 手动触发Redis缓存服务降级成功");
            } else {
                log.info("[Redis降级] Redis缓存服务已处于降级状态");
            }

            // 手动触发锁服务降级
            if (!lockCurrentlyDegraded) {
                hybridLockService.setDegraded(true);
                log.info("[Redis降级] 手动触发Redis锁服务降级成功");
            } else {
                log.info("[Redis降级] Redis锁服务已处于降级状态");
            }

            log.info("[Redis降级] 手动降级任务执行完成");
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("[Redis降级] 手动降级任务执行失败: {}", e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "手动降级失败: " + e.getMessage());
        }
    }

    /**
     * 执行手动恢复
     */
    private ReturnT<String> performManualRecover() {
        log.info("[Redis降级] 开始执行手动恢复任务...");

        boolean cacheCurrentlyDegraded = resilientRedisTemplate.isDegraded();
        boolean lockCurrentlyDegraded = hybridLockService.isDegraded();

        if (!cacheCurrentlyDegraded && !lockCurrentlyDegraded) {
            log.info("[Redis降级] Redis缓存和锁服务已处于正常状态，无需恢复");
            return ReturnT.SUCCESS;
        }

        try {
            // 手动恢复缓存服务
            if (cacheCurrentlyDegraded) {
                resilientRedisTemplate.setDegraded(false);
                log.info("[Redis降级] 手动恢复Redis缓存服务成功");
            } else {
                log.info("[Redis降级] Redis缓存服务已处于正常状态");
            }

            // 手动恢复锁服务
            if (lockCurrentlyDegraded) {
                hybridLockService.setDegraded(false);
                log.info("[Redis降级] 手动恢复Redis锁服务成功");
            } else {
                log.info("[Redis降级] Redis锁服务已处于正常状态");
            }

            // 统一执行恢复后处理（包括清理和数据同步）
            handleServiceRecovery(cacheCurrentlyDegraded, lockCurrentlyDegraded);

            log.info("[Redis降级] 手动恢复任务执行完成");
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("[Redis降级] 手动恢复任务执行失败: {}", e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "手动恢复失败: " + e.getMessage());
        }
    }

    /**
     * 统一处理服务恢复后的所有操作
     * 包括：本地资源清理 + 数据同步
     *
     * @param cacheRecovered 缓存服务是否恢复
     * @param lockRecovered  锁服务是否恢复
     */
    private void handleServiceRecovery(boolean cacheRecovered, boolean lockRecovered) {
        try {
            // 如果有服务恢复，执行恢复后处理
            if (cacheRecovered || lockRecovered) {
                log.info("[Redis降级] 开始执行恢复后处理...");

                // 1. 清理本地资源（统一处理所有恢复场景）
                if (cacheRecovered) {
                    resilientRedisTemplate.clearCacheAfterRecovery();
                    log.info("[Redis降级] 清空本地缓存完成");
                }

                if (lockRecovered) {
                    hybridLockService.cleanupLocalLocks();
                    log.info("[Redis降级] 清理本地锁完成");
                }

                // 2. 执行数据同步（所有恢复场景都需要）
                log.info("[Redis降级] 开始执行恢复后数据同步...");

                // 同步token数据
                String tokenResult = userTokenProducer.syncDegradedTokenDataToRedis();
                log.info("[Redis降级] 同步Token数据: {}", tokenResult);

                // 同步缓存数据
                String cacheResult = cacheFallbackDataService.syncDegradedCacheDataToRedis();
                log.info("[Redis降级] 同步缓存数据: {}", cacheResult);

                log.info("[Redis降级] 恢复后处理完成");
            }

        } catch (Exception e) {
            log.error("[Redis降级] 服务恢复后处理失败: {}", e.getMessage(), e);
        }
    }
}