package com.jiuyu.replay.ai.listener;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.bll.ScriptMonitorMqHandler;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 话术智能监控独立 RocketMQ 消费者。
 *
 * <p>订阅独立 topic {@code ${rocketmq.script-monitor.topic}} + 独立 consumer-group
 * {@code ${rocketmq.script-monitor.consumer-group}}，与活动/订单/SMS 等业务隔离。
 * 收到消息后委托 {@link ScriptMonitorMqHandler} 按 tag 路由处理（B4 当前仅 quality-inspection）。</p>
 *
 * <p>幂等机制：先查 tb_mq_message_record 的 messageStatus，再调 Handler 内部按 reportId 二次校验。</p>
 *
 * @author beta
 * @date 2026-06-01
 */
@Slf4j
@Service
@AllArgsConstructor
@RocketMQMessageListener(
        consumerGroup = "${rocketmq.script-monitor.consumer-group}",
        topic = "${rocketmq.script-monitor.topic}",
        endpoints = "${rocketmq.producer.endpoints}",
        accessKey = "${rocketmq.producer.access-key}",
        secretKey = "${rocketmq.producer.secret-key}",
        namespace = "${rocketmq.push-consumer.namespace:}"
)
public class ScriptMonitorMqListener implements RocketMQListener {

    /**
     * 诊断用：当前正在处理但未返回的消息数（in-flight）。
     * 入口 incrementAndGet，出口（finally）decrementAndGet。
     * grep 日志 [mq-concurrency] 可观测并发 in-flight 上限。
     */
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger(0);

    private final RocketMqBll rocketMqBll;
    private final ScriptMonitorMqHandler scriptMonitorMqHandler;
    private final RedissonClient redissonClient;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        long startNanos = System.nanoTime();
        int inFlightBegin = IN_FLIGHT.incrementAndGet();
        String diagKey = messageView.getKeys().stream().findFirst().orElse("?");
        String threadName = Thread.currentThread().getName();
        log.info("[mq-concurrency] BEGIN msgKey={} thread={} inFlight={}",
                diagKey, threadName, inFlightBegin);
        try {
            return consumeInternal(messageView);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            int inFlightEnd = IN_FLIGHT.decrementAndGet();
            log.info("[mq-concurrency] END   msgKey={} thread={} elapsedMs={} inFlightAfter={}",
                    diagKey, threadName, elapsedMs, inFlightEnd);
        }
    }

    /**
     * 原 consume 逻辑（in-flight 诊断包装的内层方法）。
     */
    private ConsumeResult consumeInternal(MessageView messageView) {
        String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
        log.info("[script-monitor-mq] 收到消息 topic={} tag={} msgId={} messageKey={} body={}",
                messageView.getTopic(), messageView.getTag().orElse(""),
                messageView.getMessageId(), messageView.getKeys(), body);

        Optional<String> firstKey = messageView.getKeys().stream().findFirst();
        if (firstKey.isEmpty()) {
            log.error("[script-monitor-mq] 缺少 messageKey msgId={}", messageView.getMessageId());
            return ConsumeResult.FAILURE;
        }
        final String messageKey = firstKey.get();

        RLock lock = redissonClient.getLock(LockKeyPrefix.MQ.getLockKey("scriptMonitorIdempotence:" + messageKey));
        try {
            // 加分布式锁防并发处理同一消息
            if (!lock.tryLock(30, 60, TimeUnit.SECONDS)) {
                log.warn("[script-monitor-mq] 获取锁失败 messageKey={}", messageKey);
                return ConsumeResult.FAILURE;
            }

            // 校验本地消息表状态（沿用项目通用幂等模式）
            MqMessageRecordEntity record = rocketMqBll.getMessageRecordByMessageKey(messageKey);
            if (Objects.isNull(record)) {
                log.error("[script-monitor-mq] 未找到消息记录 messageKey={}", messageKey);
                return ConsumeResult.FAILURE;
            }

            switch (record.getMessageStatus()) {
                case 0:
                    log.info("[script-monitor-mq] 消息尚未标记已发送 messageKey={} 稍后重试", messageKey);
                    return ConsumeResult.FAILURE;
                case 1:
                    break;
                case 2:
                    log.info("[script-monitor-mq] 消息已成功消费 messageKey={}", messageKey);
                    return ConsumeResult.SUCCESS;
                case 3:
                    log.warn("[script-monitor-mq] 消费发送失败的消息 messageKey={}", messageKey);
                    return ConsumeResult.SUCCESS;
                case 4:
                    log.warn("[script-monitor-mq] 重试之前消费失败的消息 messageKey={}", messageKey);
                    return ConsumeResult.FAILURE;
                default:
                    log.error("[script-monitor-mq] 未知 status={} messageKey={}",
                            record.getMessageStatus(), messageKey);
                    return ConsumeResult.FAILURE;
            }

            Optional<String> tagOpt = messageView.getTag();
            if (tagOpt.isEmpty() || StrUtil.isBlank(tagOpt.get())) {
                log.error("[script-monitor-mq] 缺少业务 tag messageKey={}", messageKey);
                return ConsumeResult.FAILURE;
            }
            String tag = tagOpt.get();

            // 委托 Handler 处理（业务异常返 SUCCESS 不重试，系统异常返 FAILURE 重试）
            ConsumeResult result = scriptMonitorMqHandler.handle(tag, body, messageKey);
            if (result == ConsumeResult.SUCCESS) {
                rocketMqBll.updateMessageStatusById(record);
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[script-monitor-mq] 处理中断 messageKey={}", messageKey, e);
            return ConsumeResult.FAILURE;
        } catch (org.apache.ibatis.exceptions.TooManyResultsException e) {
            // 历史脏数据：fix(messageKey=UUID) 之前的旧 messageKey 在 tb_mq_message_record 有重复行；
            // broker 重试这种死信消息会一直撞，返回 SUCCESS 让 broker 不再重试，跳过该死信。
            log.warn("[script-monitor-mq] messageKey 重复（历史脏数据 fix 前残留），跳过 messageKey={}",
                    messageKey, e);
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            log.error("[script-monitor-mq] 处理异常 messageKey={}", messageKey, e);
            return ConsumeResult.FAILURE;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
