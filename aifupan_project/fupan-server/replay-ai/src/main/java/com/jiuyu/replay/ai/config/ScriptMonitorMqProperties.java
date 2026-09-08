package com.jiuyu.replay.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 话术智能监控独立 RocketMQ 配置。
 *
 * <p>与活动/订单/SMS 等业务隔离的专属 topic + consumer-group，
 * 避免 AI 长任务（5-10min）拖累其他业务的 consumer thread 池。
 * 配置前缀 {@code rocketmq.script-monitor}，由 {@link ScriptMonitorMqConfig} 注册。</p>
 *
 * <p>endpoints / access-key / secret-key 复用全局 {@code rocketmq.producer.*} 配置
 * （同一 RocketMQ 实例同账号），只新增 topic + consumer-group + tag 维度。</p>
 *
 * @author beta
 * @date 2026-06-01
 */
@Data
@ConfigurationProperties(prefix = "rocketmq.script-monitor")
public class ScriptMonitorMqProperties {

    /**
     * 独立 topic 名（生产 + 消费同一 topic）
     */
    private String topic;

    /**
     * 独立 consumer-group 名（与活动/订单等业务 consumer-group 物理隔离）
     */
    private String consumerGroup;

    /**
     * 业务标签集合。
     */
    private BusinessTags businessTags = new BusinessTags();

    /**
     * 业务标签子结构（按监控类型区分，与 MonitorTypeEnum 对齐）。
     */
    @Data
    public static class BusinessTags {

        /**
         * 话术质检 tag（B4 当前实现，monitorType=0）
         */
        private String tagQualityInspection;

        /**
         * 话术还原度 tag（B3 预留，monitorType=1）
         */
        private String tagFidelityMonitor;

        /**
         * 互动巡检 tag（后续批次预留，monitorType=2）
         */
        private String tagInteractionPatrol;
    }
}
