package com.jiuyu.replay.video.project.task;

import com.jiuyu.replay.video.project.producer.VideoHotSearchEmailAccountProducer;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 热搜邮箱账号池定时任务
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 热搜邮箱账号池管理定时任务，包括Redis续期、数据同步、超时释放、自动修复、容量告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoHotSearchEmailAccountTask {

    private final VideoHotSearchEmailAccountProducer emailAccountProducer;

    /**
     * Redis 续期定时任务
     * 建议每天凌晨执行一次
     */
    @XxlJob("renewRedisExpirationJob")
    public void renewRedisExpiration() {
        log.info("【定时任务】开始 Redis 续期");
        emailAccountProducer.renewRedisExpiration();
        log.info("【定时任务】Redis 续期完成");
    }

    /**
     * 同步 Redis 到数据库定时任务
     * 建议每30分钟执行一次
     */
    @XxlJob("syncRedisToDatabaseJob")
    public void syncRedisToDatabase() {
        log.info("【定时任务】开始同步 Redis 到数据库");
        emailAccountProducer.syncRedisToDatabase();
        log.info("【定时任务】同步 Redis 到数据库完成");
    }

    /**
     * 释放超时账号定时任务
     * 建议每5分钟执行一次
     */
    @XxlJob("releaseTimeoutAccountsJob")
    public void releaseTimeoutAccounts() {
        log.info("【定时任务】开始释放超时账号");
        emailAccountProducer.releaseTimeoutAccounts();
        log.info("【定时任务】释放超时账号完成");
    }

    /**
     * 自动修复账号定时任务
     * 建议每1小时执行一次
     */
    @XxlJob("autoRepairAccountsJob")
    public void autoRepairAccounts() {
        log.info("【定时任务】开始自动修复账号");
        emailAccountProducer.autoRepairAccounts();
        log.info("【定时任务】自动修复账号完成");
    }

    /**
     * 账号池容量告警定时任务
     * 建议每40分钟执行一次
     */
    @XxlJob("poolCapacityAlertJob")
    public void poolCapacityAlert() {
        log.info("【定时任务】开始检查账号池容量");
        emailAccountProducer.poolCapacityAlert();
        log.info("【定时任务】账号池容量检查完成");
    }

    /**
     * 全量重新加载数据库数据到 Redis 定时任务
     * 不配置执行
     */
    @XxlJob("reloadAllAccountsFromDatabaseJob")
    public void reloadAllAccountsFromDatabase() {
        log.info("【定时任务】开始全量重新加载数据库数据到 Redis");
        emailAccountProducer.reloadAllAccountsFromDatabase();
        log.info("【定时任务】全量重新加载完成");
    }
}

