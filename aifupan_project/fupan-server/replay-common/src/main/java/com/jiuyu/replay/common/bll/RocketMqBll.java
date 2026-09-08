package com.jiuyu.replay.common.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import com.jiuyu.replay.common.repository.service.MqMessageRecordService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.apache.rocketmq.client.support.RocketMQHeaders;
import org.apache.rocketmq.client.support.RocketMQUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author RayChou
 * @date 2025/6/3 16:03
 */
@Slf4j
@Component
public class RocketMqBll {

    @Resource
    MqMessageRecordService mqMessageRecordService;
    @Resource
    private RocketMQClientTemplate rocketMQClientTemplate;
    @Value("${rocketmq.producer.topic}")
    private String topic;

    /**
     * 异步发送普通消息
     *
     * @param userId     用户id
     * @param messageTag 消息标签
     * @param messageKey 消息key
     * @param body       消息内容
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean syncSendNormalMessage(Long userId, String messageTag, String messageKey, String body) {
        return syncSendNormalMessage(userId, messageTag, messageKey, body, null);
    }

    /**
     * 异步发送延时消息
     *
     * @param userId        用户id
     * @param messageTag    消息标签
     * @param messageKey    消息key
     * @param body          消息内容
     * @param delaySendTime 延时发送时间（为null表示立即发送）
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean syncSendNormalMessage(Long userId, String messageTag, String messageKey, String body, LocalDateTime delaySendTime) {
        MqMessageRecordEntity mqMessageRecordEntity = new MqMessageRecordEntity();
        mqMessageRecordEntity.setId(SnowflakeManager.nextValue());
        mqMessageRecordEntity.setUserId(userId);
        mqMessageRecordEntity.setMessageTopic(topic);
        mqMessageRecordEntity.setMessageTag(messageTag);
        mqMessageRecordEntity.setMessageKey(RocketMQUtil.toRocketHeaderKey(messageKey));
        mqMessageRecordEntity.setMessageBody(body);
        mqMessageRecordEntity.setMessageStatus((byte) 0);
        mqMessageRecordEntity.setDelaySendTime(delaySendTime);
        mqMessageRecordEntity.setCreateDate(LocalDateTime.now());
        mqMessageRecordEntity.setUpdateDate(LocalDateTime.now());
        mqMessageRecordEntity.setRetryCount(0);
        return mqMessageRecordService.save(mqMessageRecordEntity);
    }

    /**
     * 异步发送普通消息到指定 topic（覆盖默认 {@code ${rocketmq.producer.topic}}）。
     *
     * <p>用于业务隔离场景（如话术智能监控独立 topic），写入 tb_mq_message_record 时使用
     * 传入的 topic，{@code MessageSenderTasks.scanAndSendMessage} 扫描时按 entity
     * 的 messageTopic 字段路由发送到对应 RocketMQ topic。</p>
     *
     * @param userId     用户 id
     * @param topic      自定义 topic 名（覆盖默认值；不可为空）
     * @param messageTag 消息标签
     * @param messageKey 消息 key
     * @param body       消息内容
     * @return 写入本地消息表是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean syncSendNormalMessageToTopic(Long userId, String topic, String messageTag, String messageKey, String body) {
        MqMessageRecordEntity mqMessageRecordEntity = new MqMessageRecordEntity();
        mqMessageRecordEntity.setId(SnowflakeManager.nextValue());
        mqMessageRecordEntity.setUserId(userId);
        mqMessageRecordEntity.setMessageTopic(topic);
        mqMessageRecordEntity.setMessageTag(messageTag);
        mqMessageRecordEntity.setMessageKey(RocketMQUtil.toRocketHeaderKey(messageKey));
        mqMessageRecordEntity.setMessageBody(body);
        mqMessageRecordEntity.setMessageStatus((byte) 0);
        mqMessageRecordEntity.setDelaySendTime(null);
        mqMessageRecordEntity.setCreateDate(LocalDateTime.now());
        mqMessageRecordEntity.setUpdateDate(LocalDateTime.now());
        mqMessageRecordEntity.setRetryCount(0);
        return mqMessageRecordService.save(mqMessageRecordEntity);
    }

    /**
     * 写本地消息表并立即业务直发 MQ；直发失败保留 status=0 等 XXL-Job 兜底重试。
     *
     * <p>区别于 {@link #syncSendNormalMessageToTopic}（只写表不发送），本方法在
     * 事务内写表后立即调用 {@code rocketMQClientTemplate.syncSendNormalMessage}。
     * 直发失败时 catch 吞异常（warn log），事务提交保留 status=0 供 XXL-Job 兜底。</p>
     *
     * @param userId     用户 ID（写入 mq_message_record.user_id）
     * @param topic      目标 topic（覆盖默认值）
     * @param messageTag 消息标签
     * @param messageKey 消息唯一 key（IdUtil.simpleUUID() 生成）
     * @param body       消息体 JSON
     * @return 写本地消息表是否成功（true=写表成功；直发结果不影响返回值）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean syncSendAndDeliverToTopic(Long userId, String topic, String messageTag, String messageKey, String body) {
        // 1. 写本地消息表（status=0 待发送）
        MqMessageRecordEntity record = new MqMessageRecordEntity();
        record.setId(SnowflakeManager.nextValue());
        record.setUserId(userId);
        record.setMessageTopic(topic);
        record.setMessageTag(messageTag);
        record.setMessageKey(RocketMQUtil.toRocketHeaderKey(messageKey));
        record.setMessageBody(body);
        record.setMessageStatus((byte) 0);
        record.setDelaySendTime(null);
        record.setCreateDate(LocalDateTime.now());
        record.setUpdateDate(LocalDateTime.now());
        record.setRetryCount(0);
        boolean saved = mqMessageRecordService.save(record);
        if (!saved) {
            return false;
        }
        // 2. 业务直发（失败不回滚，XXL-Job 兜底）
        try {
            Message<String> message = MessageBuilder.withPayload(body)
                    .setHeader(RocketMQUtil.toRocketHeaderKey(RocketMQHeaders.KEYS), record.getMessageKey())
                    .build();
            String destination = topic + ":" + messageTag;
            SendReceipt receipt = rocketMQClientTemplate.syncSendNormalMessage(destination, message);
            record.setMessageId(receipt.getMessageId().toString());
            record.setMessageStatus((byte) 1);
            record.setUpdateDate(LocalDateTime.now());
            mqMessageRecordService.updateById(record);
            log.info("[MQ 直发] 成功 messageKey={} messageId={}", messageKey, receipt.getMessageId());
        } catch (Exception e) {
            log.warn("[MQ 直发] 失败，status=0 保留等 XXL-Job 兜底重试 messageKey={}", messageKey, e);
            // 直发失败不抛、不回滚；事务提交 status=0
        }
        return true;
    }

    /**
     * 获取已发送未消费的消息根据消息key
     *
     * @param messageKey
     */
    public MqMessageRecordEntity getMessageRecordByMessageKey(String messageKey) {
        return mqMessageRecordService.getOne(new LambdaQueryWrapper<>(MqMessageRecordEntity.class).select(MqMessageRecordEntity::getId, MqMessageRecordEntity::getMessageStatus).eq(MqMessageRecordEntity::getMessageKey, messageKey));
    }

    /**
     * 更新消息状态从已发送到已消费
     *
     * @param mqMessageRecordEntity
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateMessageStatusById(MqMessageRecordEntity mqMessageRecordEntity) {
        mqMessageRecordEntity.setMessageStatus((byte) 2);
        mqMessageRecordEntity.setUpdateDate(LocalDateTime.now());
        return mqMessageRecordService.updateById(mqMessageRecordEntity);
    }
}
