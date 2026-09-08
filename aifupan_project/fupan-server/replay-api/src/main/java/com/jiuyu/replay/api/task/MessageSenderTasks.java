package com.jiuyu.replay.api.task;

import cn.hutool.core.collection.CollectionUtil;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import com.jiuyu.replay.common.repository.service.MqMessageRecordService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.apache.rocketmq.client.support.RocketMQHeaders;
import org.apache.rocketmq.client.support.RocketMQUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author RayChou
 * @date 2025/6/11 18:51
 */
@Slf4j
@Component
public class MessageSenderTasks {

    @Resource
    MqMessageRecordService mqMessageRecordService;
    @Resource
    private RocketMQClientTemplate rocketMQClientTemplate;
    @Resource
    RedissonClient redissonClient;

    /**
     * 扫描发送消息任务
     *
     * @param param
     * @return
     */
    @XxlJob("scanAndSendMessageJob")
    public ReturnT<String> scanAndSendMessage(String param) {
        RLock lock = redissonClient.getLock(LockKeyPrefix.MQ.getLockKey("scanAndSendMessageJob"));
        try {
            // 尝试获取锁，最多等待3秒，锁过期时间30秒
            if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                // 查询状态为"待发送"的消息（包括立即发送和延时发送）
                List<MqMessageRecordEntity> pendingMessages = mqMessageRecordService.listPendingMessages(100);
                if (CollectionUtil.isEmpty(pendingMessages)) {
                    return ReturnT.SUCCESS;
                }
                int successCount = 0;
                for (MqMessageRecordEntity record : pendingMessages) {
                    // 检查是否应该在当前时间重试
                    if (!shouldRetryNow(record)) {
                        continue;
                    }

                    // 检查延时消息是否到达发送时间
                    if (!shouldSendDelayMessage(record)) {
                        continue;
                    }
                    try {
                        Message<String> message = MessageBuilder.withPayload(record.getMessageBody()).setHeader(RocketMQUtil.toRocketHeaderKey(RocketMQHeaders.KEYS), record.getMessageKey()).build();
                        String destination = record.getMessageTopic() + ":" + record.getMessageTag();
                        // 发送消息到MQ
                        SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(destination, message);
                        String messageType = record.getDelaySendTime() != null ? "延时消息" : "普通消息";
                        log.info("[消息队列-生产者-{}] 发送异步消息 topic:{},tag:{},MessageId:{},messageKey:{},delaySendTime:{},消息体:{},响应信息:{}",
                                messageType, record.getMessageTopic(), record.getMessageTag(), sendReceipt.getMessageId(),
                                record.getMessageKey(), record.getDelaySendTime(), message.getPayload(), sendReceipt);
                        // 更新消息状态为"已发送"
                        record.setMessageId(sendReceipt.getMessageId().toString());
                        record.setMessageStatus((byte) 1);
                        record.setUpdateDate(LocalDateTime.now());
                        mqMessageRecordService.updateById(record);
                        successCount++;
                    } catch (Exception e) {
                        log.error("[消息队列-生产者-普通消息] 发送异步消息失败, messageKey:{}", record.getMessageKey(), e);
                        record.setRetryCount(record.getRetryCount() + 1);
                        // 设置下次重试时间，采用指数退避策略
                        record.setNextRetryTime(calculateNextRetryTime(record.getRetryCount()));
                        record.setUpdateDate(LocalDateTime.now());
                        if (record.getRetryCount() > 10) {
                            record.setMessageStatus((byte) 3); // 发送失败
                        }
                        mqMessageRecordService.updateById(record);
                    }
                }
                if (successCount != 0) {
                    log.info("[消息队列-生产者-普通消息] 本次扫描处理消息{}条，成功发送{}条", pendingMessages.size(), successCount);
                }
            } else {
                log.info("[消息队列-生产者-普通消息] 未获取到锁，跳过本次执行");
            }
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("[消息队列-生产者-普通消息] 扫描发送消息任务异常", e);
            return ReturnT.FAIL;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 判断消息是否应该在当前时间重试
     *
     * @param record 消息记录
     * @return 是否应该重试
     */
    private boolean shouldRetryNow(MqMessageRecordEntity record) {
        // 如果是首次发送，或者没有设置下次重试时间，则直接发送
        if (record.getRetryCount() == 0 || record.getNextRetryTime() == null) {
            return true;
        }
        // 如果当前时间已经超过下次重试时间，则可以重试
        return LocalDateTime.now().isAfter(record.getNextRetryTime());
    }

    /**
     * 判断延时消息是否应该在当前时间发送
     *
     * @param record 消息记录
     * @return 是否应该发送
     */
    private boolean shouldSendDelayMessage(MqMessageRecordEntity record) {
        // 如果没有设置延时发送时间，表示立即发送
        if (record.getDelaySendTime() == null) {
            return true;
        }
        // 如果当前时间已经超过延时发送时间，则可以发送
        boolean shouldSend = LocalDateTime.now().isAfter(record.getDelaySendTime()) || LocalDateTime.now().isEqual(record.getDelaySendTime());

        if (!shouldSend) {
            log.debug("[消息队列-生产者-延时消息] 消息未到发送时间, messageKey:{}, delaySendTime:{}, currentTime:{}",
                    record.getMessageKey(), record.getDelaySendTime(), LocalDateTime.now());
        }

        return shouldSend;
    }

    /**
     * 计算下次重试时间，采用指数退避策略
     *
     * @param retryCount 当前重试次数
     * @return 下次重试时间
     */
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // 基础延迟时间为30秒
        int baseDelaySeconds = 30;
        // 根据重试次数计算延迟，最大延迟不超过30分钟、重试间隔（30秒、1分钟、2分钟、4分钟、8分钟、16分钟、30分钟
        int delaySeconds = Math.min(baseDelaySeconds * (1 << Math.min(retryCount, 6)), 30 * 60);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }
}
