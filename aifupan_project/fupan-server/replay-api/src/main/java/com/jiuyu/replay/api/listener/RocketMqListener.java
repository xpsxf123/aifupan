package com.jiuyu.replay.api.listener;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.common.entity.MqMessageRecordEntity;
import com.jiuyu.replay.common.lock.DistributedLock;
import com.jiuyu.replay.common.properties.RocketMqProperties;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.dto.activity.UserInviteMqDto;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.vo.CreateOrderVo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.reward.bll.ClientInviteRewardRecordBll;
import com.jiuyu.replay.third.sms.SmsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * rocketmq消息队列监听器-策略模式处理不同业务标签
 *
 * @author RayChou
 * @date 2025/6/3 11:23
 */
@Slf4j
@Service
@RocketMQMessageListener(consumerGroup = "${rocketmq.push-consumer.consumer-group:}", namespace = "${rocketmq.push-consumer.namespace:}")
public class RocketMqListener implements RocketMQListener {
    @Resource
    ClientInviteRewardRecordBll clientInviteRewardRecordBll;
    @Resource
    OrderBll orderBll;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    RocketMqBll rocketMqBll;
    @Resource
    RocketMqProperties rocketMqProperties;
    @Resource
    private DistributedLock distributedLock;
    @Resource
    private SmsService smsService;
    @Resource
    private UserFeign userFeign;
    @Resource
    private SalesFeign salesFeign;
    @Resource
    private UserBll userBll;

    // 业务处理策略映射
    private final Map<String, MessageProcessor> processorMap = new HashMap<>();

    // 初始化业务处理器映射
    @PostConstruct
    public void init() {
        processorMap.put(rocketMqProperties.getTagUserInviteActivity(), this::processUserInviteActivity);
        processorMap.put(rocketMqProperties.getTagUserFirstOrder(), this::processUserFirstOrder);
        processorMap.put(rocketMqProperties.getTagUserRegisterSendEmail(), this::processUserRegisterSendEmail);
        // 可以在这里添加更多的业务处理器
    }

    @Override
    public ConsumeResult consume(MessageView messageView) {
        String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
        log.info("[消息队列-消费者] 收到消息 topic:{},tag:{},MessageId:{},messageKey:{},消息体:{}", messageView.getTopic(), messageView.getTag().orElse(""), messageView.getMessageId(), messageView.getKeys(), body);

        Optional<String> first = messageView.getKeys().stream().findFirst();
        if (!first.isPresent()) {
            log.error("[消息队列-消费者] 消息缺少messageKey, MessageId:{}", messageView.getMessageId());
            return ConsumeResult.FAILURE;
        }

        final String messageKey = first.get();

        RLock lock = redissonClient.getLock(LockKeyPrefix.MQ.getLockKey("activityIdempotence:" + messageKey));

        try {
            // 尝试获取分布式锁，避免并发处理同一消息
            boolean isLocked = lock.tryLock(30, 60, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("[消息队列-消费者] 获取锁失败, messageKey:{}", messageKey);
                return ConsumeResult.FAILURE;
            }

            // 检查消息是否已处理
            MqMessageRecordEntity messageRecord = rocketMqBll.getMessageRecordByMessageKey(messageKey);
            if (Objects.isNull(messageRecord)) {
                log.error("[消息队列-消费者] 未找到消息记录, messageKey:{}", messageKey);
                return ConsumeResult.FAILURE;
            }
            // 根据不同状态进行处理
            // 消息状态：0：待发送 1：已发送 2：已消费 3：发送失败 4：消费失败
            switch (messageRecord.getMessageStatus()) {
                case 0: // 待发送
                    // 消息还未被标记为已发送，稍后重试
                    log.info("[消息队列-消费者] 消息尚未被标记为已发送, messageKey:{}, 稍后重试", messageKey);
                    return ConsumeResult.FAILURE;
                case 1: // 已发送
                    // 正常状态，继续处理
                    break;
                case 2: // 已消费
                    // 消息已成功处理，直接返回成功
                    log.info("[消息队列-消费者] 消息已处理成功, messageKey:{}", messageKey);
                    return ConsumeResult.SUCCESS;
                case 3: // 发送失败
                    // 发送失败的消息不应该被消费
                    log.warn("[消息队列-消费者] 尝试消费发送失败的消息, messageKey:{}", messageKey);
                    return ConsumeResult.SUCCESS; // 返回SUCCESS避免重试
                case 4: // 消费失败
                    // 之前消费失败的消息，可以根据业务需求决定是否重试
                    log.warn("[消息队列-消费者] 尝试消费之前消费失败的消息, messageKey:{}", messageKey);
                    return ConsumeResult.FAILURE;
                default:
                    log.error("[消息队列-消费者] 未知的消息状态: {}, messageKey:{}", messageRecord.getMessageStatus(), messageKey);
                    return ConsumeResult.FAILURE;
            }
            // 获取业务标签
            Optional<String> tagOptional = messageView.getTag();
            if (tagOptional.isEmpty() || StrUtil.isBlank(tagOptional.get())) {
                log.error("[消息队列-消费者] 消息缺少业务标签, messageKey:{}", messageKey);
                return ConsumeResult.FAILURE;
            }

            String businessTag = tagOptional.get();

            // 根据业务标签路由到对应的处理器
            MessageProcessor processor = processorMap.get(businessTag);
            if (processor == null) {
                log.error("[消息队列-消费者] 未找到对应的业务处理器, tag:{}, messageKey:{}", businessTag, messageKey);
                return ConsumeResult.FAILURE;
            }

            // 处理业务逻辑
            boolean processResult = processor.process(body);
            if (!processResult) {
                log.error("[消息队列-消费者] 业务处理失败, tag:{}, messageKey:{}", businessTag, messageKey);
                return ConsumeResult.FAILURE;
            }

            // 更新消息状态
            rocketMqBll.updateMessageStatusById(messageRecord);
            log.info("[消息队列-消费者] 消息处理成功, messageKey:{}", messageKey);
            return ConsumeResult.SUCCESS;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[消息队列-消费者] 处理消息被中断, messageKey:{}", messageKey, e);
            return ConsumeResult.FAILURE;
        } catch (Exception e) {
            log.error("[消息队列-消费者] 处理消息异常, messageKey:{}, 异常:", messageKey, e);
            return ConsumeResult.FAILURE;
        } finally {
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 消息处理器接口
     */
    @FunctionalInterface
    private interface MessageProcessor {
        /**
         * 处理消息
         *
         * @param messageBody 消息体
         * @return 处理结果
         */
        boolean process(String messageBody);
    }

    /**
     * 处理用户邀请活动消息
     */
    private boolean processUserInviteActivity(String messageBody) {
        try {
            UserInviteMqDto dto = JSON.parseObject(messageBody, UserInviteMqDto.class);
            String fingerprint = dto.getFingerprint();
            if (StrUtil.isBlank(fingerprint) || Objects.equals(fingerprint, "error")) {
                return true;
            }
            return distributedLock.executeWithLock(LockKeyPrefix.USER.getLockKey("fingerprintInfo:" + fingerprint), () -> clientInviteRewardRecordBll.giveUserInviteAward(dto));
        }catch (BusinessException | RRException e){
            return false;
        }catch (Exception e) {
            log.error("[用户邀请活动] 处理失败, 消息体:{}", messageBody, e);
            return false;
        }
    }

    /**
     * 处理用户注册后下单
     *
     * @param messageBody 消息体
     * @return 处理结果
     */
    private boolean processUserFirstOrder(String messageBody) {
        try {
            CreateOrderBo cBo = JSON.parseObject(messageBody, CreateOrderBo.class);
            // 下单
            CreateOrderVo order = orderBll.createOrder(cBo);
            // 手动的单，直接订单完成接口
            if (ObjectUtil.isNotEmpty(order)) {
                orderBll.successOrder(order.getOrderId(), true);
                // 发送邀请有礼消息 在注册下单完成之后
                rocketMqBll.syncSendNormalMessage(cBo.getUserId(), rocketMqProperties.getTagUserInviteActivity(), IdUtil.simpleUUID(), JSON.toJSONString(cBo.getUserInviteMqDto()));
            }
            return true;
        } catch (Exception ex) {
            log.error("[用户注册后下单] 处理失败, 消息体:{}", messageBody, ex);
            return false;
        }
    }

    /**
     * 处理用户注册完成后的短信发送
     *
     * @param messageBody 用户id
     * @return 处理结果
     */
    private boolean processUserRegisterSendEmail(String messageBody) {
        try {
            if (StrUtil.isEmpty(messageBody)) {
                log.warn("[用户注册完成后短信发送] 参数为null");
                return true;
            }
            Long userId = NumberUtil.parseLong(messageBody, null);
            if (ObjectUtil.isNull(userId)) {
                log.error("[用户注册完成后短信发送] 参数错误 messageBody = {}", messageBody);
                return true;
            }
            UserDto userDto = userFeign.userById(userId);
            if (ObjectUtil.isNull(userDto)) {
                log.error("[用户注册完成后短信发送] 用户没有查询到 userId = {}", userId);
                return true;
            }

            SalesInfoVo sales = salesFeign.getByUserId(userId);
            if (ObjectUtil.isNull(sales)) {
                log.warn("[用户注册完成后短信发送] 没有查询到销售，不发送短信");
                return true;
            }

            if (ObjectUtil.isEmpty(sales.getSalesIntroductionUrl())) {
                log.warn("[用户注册完成后短信发送] 销售查询到了，但是没有设置获客助手链接");
                return true;
            }

            // 发生短信
            SmsResult smsResult = userBll.sendCustomerAcquisitionMsg(userDto.getPhone(), sales.getSalesIntroductionUrl());
            return smsResult.isSuccess();
        } catch (Exception ex) {
            log.error("[用户注册完成后短信发送] 处理失败, 消息体:{}", messageBody, ex);
            return false;
        }
    }


}