package com.jiuyu.replay.api.task;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.bo.SendSocketErrorData;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.OrderDetailBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.producer.OrderProducer;
import com.jiuyu.replay.power.bll.BindingAccountBll;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bo.UserBo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.third.sms.SmsService;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.SocketCollectMessageBll;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.vo.SocketCollectMessageInfoVo;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
@Slf4j
public class OrderScheduledTasks {

    private final OrderBll orderBll;
    private final UserBll userBll;
    private final OrderDetailBll orderDetailBll;
    private final UserPropertyBll userPropertyBll;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BindingAccountBll bindingAccountBll;
    private final WordsProperties wordsProperties;
    private final SocketCollectMessageBll uploadSocketMessage;
    private final SocketCollectMessageBll socketCollectMessageBll;
    private final AnchorUrlBll anchorUrlBll;
    private final OrderProducer orderProducer;
    private final DataSourceTransactionManager dataSourceTransactionManager;

    private final TransactionDefinition transactionDefinition;

    private final SmsService smsService;

    private final Environment environment;

    private final DictDataFeign dictDataFeign;

    private final static Map<String, Map<String, String>> webhookUrlMap = new HashMap<>();

    // 新增资产过期任务异常通知webhook配置
    private static final String EXPIRED_ORDER_EXCEPTION_WEBHOOK = "https://oapi.dingtalk.com/robot/send?access_token=dadd3a62b1fd8c7529d5592cfa1636a0ca19e5da3cace1f7dd1d3733dede1c9c";

    static {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("url", "https://oapi.dingtalk.com/robot/send?access_token=dadd3a62b1fd8c7529d5592cfa1636a0ca19e5da3cace1f7dd1d3733dede1c9c");
        hashMap.put("errorStr", "websocket地址错误");
        hashMap.put("causeOfError", "js计算获取的websocket地址连接失败");
        webhookUrlMap.put("jsSocketAddress", hashMap);

        Map<String, String> hashMap1 = new HashMap<>();
        hashMap1.put("url", "https://oapi.dingtalk.com/robot/send?access_token=dadd3a62b1fd8c7529d5592cfa1636a0ca19e5da3cace1f7dd1d3733dede1c9c");
        hashMap1.put("errorStr", "websocket地址错误");
        hashMap1.put("causeOfError", "浏览器获取的websocket地址连接失败");
        webhookUrlMap.put("browserSocketAddress", hashMap1);

        Map<String, String> hashMap2 = new HashMap<>();
        hashMap2.put("url", "https://oapi.dingtalk.com/robot/send?access_token=dadd3a62b1fd8c7529d5592cfa1636a0ca19e5da3cace1f7dd1d3733dede1c9c");
        hashMap2.put("errorStr", "websocket地址超时");
        hashMap2.put("causeOfError", "浏览器获取websocket地址超时，30秒没有成功获取Websocket地址");
        webhookUrlMap.put("browserGetSocketAddressError", hashMap2);
    }


    /**
     * 每10分钟查询一次订单的过期时间
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0 2 * * ? ")
    // @XxlJob("batchSaveAudioLog")
    @Transactional
    public void batchSaveAudioLog() {
        log.info("==============================开始执行定时器，查询有多少订单过期===================================");
        R<List<OrderInfoVo>> orderListR = orderBll.listByExpiredOrder();
        if (orderListR.getCode() == 0 && ObjectUtil.isNotEmpty(orderListR.getData())) {
            List<OrderInfoVo> orderList = orderListR.getData();
            log.info("订单过期的数量={}", orderList.size());
            log.info("查询有{}订单过期，走下一步", orderList.size());
            for (OrderInfoVo order : orderList) {
                log.info("轮询订单，订单={}", JSONUtil.toJsonStr(order));
                if (order.getCommodityType() == 1 && order.getStatus() == 2) {
                    R<UserInfoVo> userInfoVoR = userBll.infoByClient(order.getUserId());

                    // 清空用户弹幕监控和自动录制
                    // 查询用户的子账号
                    List<Long> userIds = new ArrayList<>();
                    userIds.add(order.getUserId());
                    R<List<Long>> subUserListR = userBll.getUserChild(List.of(order.getUserId()));
                    if (subUserListR.getCode() == 0 && ObjectUtil.isNotEmpty(subUserListR.getData())) {
                        userIds.addAll(subUserListR.getData());
                    }
                    userIds.forEach(userId -> {
                        // 清空用户弹幕监控和自动录制
                        userPropertyBll.updateByPropertyNum(userId, "anchorBarrageNum", 0L);
                    });
                    anchorUrlBll.closeAnchorBarrageNum(userIds);
//                    anchorUrlBll.closeAutoUploadCloud(userIds);

                    if (userInfoVoR.getCode() == 0) {
                        UserInfoVo userInfoVo = userInfoVoR.getData();
                        order.setUserId(userInfoVo.getId());
                        order.setUserName(userInfoVo.getUsername());
                    } else {
                        // 订单用户不存在，直接结束订单
                        orderBll.orderStop(order.getId(), null, true);
                    }
                } else {
                    // 订单不是版本或者不是在用版本，直接结束订单，不清除order缓存
                    orderBll.orderStop(order.getId(), null, false);
                }
                orderBll.expiredOrderProcessing(order);
                log.info("订单处理完毕");
            }
        }
        log.info("订单过期的处理已经处理");
        log.info("开始查询在月底重置的订单详情");
        orderDetailBll.expiredOrderProcessing(null);
        log.info("============================================订单定时器已经处理=====================================");
    }


    /**
     * 处理过期订单的定时任务，每60分钟执行一次，用于检查并处理已过期的订单。
     * <p>
     * 该任务主要功能包括：
     * 1. 查询指定时间点前已过期的订单；
     * 2. 根据订单类型（版本套餐订单或其他订单）执行不同的处理逻辑；
     * 3. 对版本套餐订单进行权限清理等特殊处理；
     * 4. 对其他订单直接结束；
     * 5. 执行资产重置操作（如月底资产清零）。
     * </p>
     * <p>
     * 业务逻辑说明：
     * - 版本套餐订单（commodityType=1 且 status=2）需要特殊处理，例如清空用户权限；
     * - 其他类型的订单则直接停用；
     * - 使用手动事务管理确保数据一致性；
     * - 支持通过任务参数自定义过期时间点，默认为当前时间。
     * </p>
     *
     * <p>依赖组件：</p>
     * <ul>
     *   <li>{@link XxlJobHelper}：获取任务参数和记录日志</li>
     *   <li>{@link OrderBll#loadExpiredOrder}：分批加载过期订单</li>
     *   <li>{@link OrderDetailBll#expiredOrderProcessing}：执行资产重置逻辑</li>
     * </ul>
     *
     * <p>注意事项：</p>
     * <ul>
     *   <li>若传入的 expiredTime 晚于当前时间，则自动修正为当前时间；</li>
     *   <li>使用函数式接口封装用户信息查询与订单类型判断逻辑；</li>
     *   <li>采用手动事务控制，保证每个批次处理的数据一致性。</li>
     * </ul>
     */
    @XxlJob("batchSaveAudioLog")
    public void expiredOrder() {
        try {
            // 获取任务参数，支持自定义过期时间
            String jobParam = XxlJobHelper.getJobParam();
            LocalDateTime startNow = LocalDateTime.now();
            // 默认当天的最后一小时
            LocalDateTime maxExpireTime = startNow.withHour(23).withMinute(59).withSecond(59);
            LocalDateTime expiredTime = maxExpireTime;
            if (JSONUtil.isTypeJSONObject(jobParam)) {
                cn.hutool.json.JSONObject entries = JSONUtil.parseObj(jobParam);
                Integer aboutToExpiredHours = entries.getInt("aboutToExpiredHours");
                if (aboutToExpiredHours != null && aboutToExpiredHours > 0) {
                    maxExpireTime = startNow.plusHours(aboutToExpiredHours);
                    expiredTime = maxExpireTime;
                }
                LocalDateTime overTime = entries.getLocalDateTime("expiredTime", null);
                log.info("[资产过期Job] customize expired time: {}", overTime);
                if (overTime != null && overTime.isAfter(maxExpireTime)) {
                    overTime = maxExpireTime;
                    log.info("[资产过期Job] customize expired gt now... correctTo now: {}", maxExpireTime);
                    XxlJobHelper.log("customize expired gt now... correctTo now: {}", overTime);
                }
                expiredTime = overTime != null ? overTime : maxExpireTime;
            }

            // 定时任务中断判断
            BiPredicate<LocalDateTime, Integer> stopTaskPredicate = (now, num) -> LocalDateTimeUtil.between(startNow, now, ChronoUnit.MINUTES) > num;

            // 定义用户信息查询函数，根据用户ID批量获取用户列表
            Function<List<Long>, Map<Long, UserListVo>> queryUserMap = userIds -> {
                if (EmptyUtil.isEmpty(userIds)) {
                    return Map.of();
                }
                R<List<UserListVo>> userResult = userBll.listByIds(userIds);
                if (userResult.fail() || CollUtil.isEmpty(userResult.getData())) {
                    return Map.of();
                }
                return userResult.getData().stream().collect(Collectors.toMap(UserListVo::getId, Function.identity(), (v1, v2) -> v1));
            };

            // 判断是否为版本套餐订单的谓词条件
            Predicate<OrderInfoVo> versionOrderPredicate = order -> order.getCommodityType() == 1 && order.getStatus() == 2;

            // 初始化计数器，用于统计处理批次和成功批次
            AtomicInteger semaphore = new AtomicInteger(0);
            AtomicInteger successNum = new AtomicInteger(0);
            AtomicInteger totalRowNum = new AtomicInteger(0);
            AtomicInteger successRowNum = new AtomicInteger(0);

            // 避免可能存在的并发冲突死锁
            List<Long> skipUserIds = new ArrayList<>(12400);
            // 分批加载并处理过期订单
            // startNow 当前小时的最后一分钟
            LocalDateTime orderExpiredTime = startNow.withMinute(59).withSecond(59);
            orderBll.loadExpiredOrder(orderExpiredTime, rows -> {
                List<List<OrderInfoVo>> splitList = CollUtil.split(rows.stream().filter(row -> {
                    // 非套餐版本 允许处理
                    if (!versionOrderPredicate.test(row)) {
                        return true;
                    }
                    if (skipUserIds.contains(row.getUserId())) {
                        return false;
                    }
                    skipUserIds.add(row.getUserId());
                    return true;
                }).toList(), 50);
                for (List<OrderInfoVo> orderList : splitList) {
                    // 检查是否需要停止任务
                    if (stopTaskPredicate.test(LocalDateTime.now(), 30)) {
                        log.info("[资产过期Job] expired order process stop.......");
                        break;
                    }
                    // 检查是否需要停止任务
                    if (stopTaskPredicate.test(LocalDateTime.now(), 30)) {
                        log.info("[资产过期Job] expired order process stop.......");
                        return;
                    }
                    // 对每个批次进行处理 load的一批允许比较大 为了防止异常数据影响一整批订单 缩小单批次的数据范围
                    List<Long> orderIds = orderList.stream().map(OrderInfoVo::getId).toList();
                    int batch = semaphore.incrementAndGet();
                    if (batch == 1) {
                        log.info("[资产过期Job] expired order process start.......");
                    }
                    XxlJobHelper.log("批次：{} size:{} expired order query rows:{}", batch, orderIds.size(), orderIds);
                    totalRowNum.addAndGet(orderIds.size());

                    // 手动开启事务
                    TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                    try {
                        // 执行订单过期处理逻辑
                        this.expiredOrderProcess(orderList, versionOrderPredicate, queryUserMap, null);
                        // 提交事务
                        dataSourceTransactionManager.commit(transactionStatus);
                        successNum.incrementAndGet();
                        successRowNum.addAndGet(orderIds.size());
                        log.info("[资产过期Job] expired order process success: batchNo: {}", batch);
                        XxlJobHelper.log("批次：{} expired order process success", batch);
                    } catch (Throwable e) {
                        log.error("[资产过期Job] expired order process error: orderIds: {}", orderIds, e);
                        XxlJobHelper.log("批次：{} expired order process error: orderIds: {} error:{}", batch, orderIds, e.getMessage());
                        // 回滚事务
                        dataSourceTransactionManager.rollback(transactionStatus);

                        // 发送异常通知
                        sendExceptionNotification("资产过期任务处理异常", e, "订单ID列表: " + orderIds);
                    }
                }
                ;
            }, () -> stopTaskPredicate.test(LocalDateTime.now(), 30));


            // 输出处理结果日志
            if (semaphore.get() > 0) {
                log.info("[资产过期Job] expired order process over...... total batch: {}, success batch: {}, total row: {}, success row: {}", semaphore.get(), successNum.get(), totalRowNum.get(), successRowNum.get());
                XxlJobHelper.log("[资产过期Job] expired order process over...... total batch: {}, success batch: {}, total row: {}, success row: {}", semaphore.get(), successNum.get(), totalRowNum.get(), successRowNum.get());
            }

            // 执行资产重置逻辑（如月底资产清零）
            log.info("[资产重置] rest order start >>>>>>>> all rows");
            XxlJobHelper.log("[资产重置] rest order start >>>>>>>>>>>>>>>> all rows");
            semaphore.set(0);
            successNum.set(0);
            totalRowNum.set(0);
            successRowNum.set(0);

            // 加载并处理过期订单详情 为了保证数据一致性 根据订单ID进行分组
            Map<Long, List<OrderDetailInfoVo>> orderDetailMap = orderProducer.loadExpiredAllOrderDetail(expiredTime).stream().collect(Collectors.groupingBy(OrderDetailInfoVo::getOrderId));
            if (EmptyUtil.isEmpty(orderDetailMap)) {
                return;
            }
            // 用户ai算力重置
            Map<Long, Long> userAiTokenRestNum = new ConcurrentHashMap<>(orderDetailMap.size() * 2);
            // 用户智能分析时长重置
            Map<Long, Long> userAiAnalysisRestTime = new ConcurrentHashMap<>(orderDetailMap.size() * 2);
            List<List<Map.Entry<Long, List<OrderDetailInfoVo>>>> splitList = CollUtil.split(orderDetailMap.entrySet(), 100);
            for (List<Map.Entry<Long, List<OrderDetailInfoVo>>> entryList : splitList) {
                // 检查是否需要停止任务
                if (stopTaskPredicate.test(LocalDateTime.now(), 55)) {
                    log.info("[资产重置] rest order stop.......");
                    break;
                }
                List<Long> orderIds = entryList.stream().map(Map.Entry::getKey).toList();
                Map<Long, OrderVo> orderBaseInfoMap = orderBll.getOrderBaseInfoMap(orderIds);
                if (EmptyUtil.isEmpty(orderBaseInfoMap)) {
                    continue;
                }

                int batchNo = semaphore.incrementAndGet();

                List<OrderDetailInfoVo> list = entryList.stream().filter(entry -> orderBaseInfoMap.containsKey(entry.getKey())).flatMap(entry -> entry.getValue().stream()).toList();
                List<Long> ids = list.stream().map(OrderDetailInfoVo::getId).toList();
                totalRowNum.addAndGet(ids.size());
                log.info("[资产重置] rest order start >>>>>>>>>>>>>>>> batch: {}, size:{} orderIds:{}", batchNo, ids.size(), orderIds);
                XxlJobHelper.log("[资产重置] rest order start >>>>>>>>>>>>>>>> batch: {}, size:{} orderIds:{}", batchNo, ids.size(), orderIds);
                TransactionStatus transaction = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                    // 执行订单详情过期处理
                    orderDetailBll.executeOrderDetailExpired(list, batchNo);
                    dataSourceTransactionManager.commit(transaction);
                    successNum.incrementAndGet();
                    successRowNum.addAndGet(ids.size());
                    log.info("[资产重置] rest order end >>>>>>>>>>>>>>>> batch: {} complete", batchNo);
                    XxlJobHelper.log("[资产重置] rest order end >>>>>>>>>>>>>>>> batch: {} complete", batchNo);
                    // 跳过免费版本
                    list.stream().filter(orderDetail -> !Objects.equals(orderBaseInfoMap.get(orderDetail.getOrderId()).getOrderType(), 0))
                        .forEach(orderDetail -> {
                            orderDetail.setUserId(orderBaseInfoMap.get(orderDetail.getOrderId()).getUserId());
                            if (Objects.equals(orderDetail.getCommodityTypeCode(), OrderEnums.commodityTypeCode.AI_ANALYSIS_TIME.getCode())) {
                                userAiAnalysisRestTime.compute(orderDetail.getUserId(), (key, old) -> old == null ? orderDetail.getTotalNumber() : old + orderDetail.getTotalNumber());
                            }
                            if (Objects.equals(orderDetail.getCommodityTypeCode(), OrderEnums.commodityTypeCode.AI_TOKEN_NUM.getCode())) {
                                userAiTokenRestNum.compute(orderDetail.getUserId(), (key, old) -> old == null ? orderDetail.getTotalNumber() : old + orderDetail.getTotalNumber());
                            }
                        });
                } catch (Throwable e) {
                    log.error("[资产重置] rest order process error: ids: {}", ids, e);
                    dataSourceTransactionManager.rollback(transaction);
                    XxlJobHelper.log("rest order process error: ids: {} error:{}", ids, e.getMessage());

                    // 发送异常通知
                    sendExceptionNotification("资产重置任务处理异常", e, "订单详情ID列表: " + ids);
                }
            }


            List<Long> restUserIds = Stream.concat(userAiAnalysisRestTime.keySet().stream(), userAiTokenRestNum.keySet().stream()).distinct().toList();
            Map<Long, String> userPhoneMap = userBll.getUserPhoneMap(restUserIds);
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProd = activeProfiles.length > 0 && "prod".equals(activeProfiles[0]);
            List<String> allowMobile = dictDataFeign.getConfigList("development_staff", "allow_receive_mobile", String.class);
            // 发生分析时长重置短信
            if (EmptyUtil.isNotEmpty(userAiAnalysisRestTime)) {
                ThreadUtil.execute(() -> {
                    Map<Long, Long> userPropertyRemainingMap = userPropertyBll.getUserPropertyRemainingMap(userAiAnalysisRestTime.keySet(), OrderEnums.commodityTypeCode.AI_ANALYSIS_TIME.getCode());
                    if (EmptyUtil.isNotEmpty(userPropertyRemainingMap)) {
                        userAiAnalysisRestTime.forEach((userId, restTime) -> {
                            String userPhone = userPhoneMap.get(userId);
                            if (EmptyUtil.isEmpty(userPhone)) {
                                return;
                            }
                            Long remainingTime = userPropertyRemainingMap.get(userId);
                            if (EmptyUtil.isEmpty(remainingTime)) {
                                return;
                            }
                            BigDecimal base = BigDecimal.valueOf(60);
                            Map<String, String> param = Map.of("restTime", BigDecimal.valueOf(restTime).divide(base, 1, RoundingMode.HALF_UP).toString(), "incrementalPackage", BigDecimal.valueOf(remainingTime - restTime).divide(base, 1, RoundingMode.HALF_UP).toString(), "remainingTime", BigDecimal.valueOf(remainingTime).divide(base, 1, RoundingMode.HALF_UP).toString());
                            if ((isProd && EmptyUtil.isEmpty(allowMobile)) || (EmptyUtil.isNotEmpty(allowMobile) && allowMobile.contains(userPhone))) {
                                smsService.send("user_rest_ai_analysis_time_sms_code", userPhone, param, null);
                            } else {
                                XxlJobHelper.log("[重置分析时长短信] rest user aiAnalysisTime Property. send userId: {} mobile: {}, restTime: {} incrementalPackage: {} remainingTime: {}", userId, userPhone, param.get("restTime"), param.get("incrementalPackage"), param.get("remainingTime"));
                            }
                        });
                    }
                });
            }
            // 发送ai算力重置短信
            if (EmptyUtil.isNotEmpty(userAiTokenRestNum)) {
                ThreadUtil.execute(() -> {
                    Map<Long, Long> userPropertyRemainingMap = userPropertyBll.getUserPropertyRemainingMap(userAiTokenRestNum.keySet(), OrderEnums.commodityTypeCode.AI_TOKEN_NUM.getCode());
                    if (EmptyUtil.isNotEmpty(userPropertyRemainingMap)) {
                        userAiTokenRestNum.forEach((userId, restNum) -> {
                            String userPhone = userPhoneMap.get(userId);
                            if (EmptyUtil.isEmpty(userPhone)) {
                                return;
                            }
                            Long remainingNum = userPropertyRemainingMap.get(userId);
                            if (EmptyUtil.isEmpty(remainingNum)) {
                                return;
                            }
                            BigDecimal base = BigDecimal.valueOf(10000);
                            Map<String, String> param = Map.of("restNum", BigDecimal.valueOf(restNum).divide(base, 1, RoundingMode.HALF_UP).toString(), "incrementalPackage", BigDecimal.valueOf(remainingNum - restNum).divide(base, 1, RoundingMode.HALF_UP).toString(), "remainingNum", BigDecimal.valueOf(remainingNum).divide(base, 1, RoundingMode.HALF_UP).toString());
                            if ((isProd && EmptyUtil.isEmpty(allowMobile)) || (EmptyUtil.isNotEmpty(allowMobile) && allowMobile.contains(userPhone))) {
                                smsService.send("user_rest_ai_token_num_sms_code", userPhone, param, null);
                            } else {
                                XxlJobHelper.log("[重置ai算力短信] rest user aiTokenNum Property. send userId: {} mobile: {}, restNum: {} incrementalPackage: {} remainingNum: {}", userId, userPhone, param.get("restNum"), param.get("incrementalPackage"), param.get("remainingNum"));
                            }
                        });
                    }
                });
            }

            // 输出处理结果日志
            if (semaphore.get() > 0) {
                log.info("[资产重置] rest order process over...... total batch: {}, success batch: {}, total row: {}, success row: {}", semaphore.get(), successNum.get(), totalRowNum.get(), successRowNum.get());
                XxlJobHelper.log("[资产重置] rest order process over...... total batch: {}, success batch: {}, total row: {}, success row: {}", semaphore.get(), successNum.get(), totalRowNum.get(), successRowNum.get());
            }
        } catch (Exception e) {
            log.error("[资产过期Job] 任务执行过程中发生未捕获异常", e);
            XxlJobHelper.log("[资产过期Job] 任务执行过程中发生未捕获异常: {}", e.getMessage());

            // 发送异常通知
            sendExceptionNotification("资产过期任务执行异常", e, "任务执行过程中发生未捕获异常");
            throw e; // 重新抛出异常，让XXL-JOB框架感知任务失败
        }
    }

    /**
     * 发送异常通知到钉钉群
     *
     * @param title     异常标题
     * @param throwable 异常对象
     * @param extraInfo 额外信息
     */
    private void sendExceptionNotification(String title, Throwable throwable, String extraInfo) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "markdown");

            Map<String, String> markdown = new HashMap<>();
            markdown.put("title", title);

            StringBuilder text = new StringBuilder();
            text.append("## ").append(title).append("\n\n");
            text.append("### 异常时间\n").append(DateUtil.now()).append("\n\n");
            text.append("### 异常信息\n").append(throwable.getMessage()).append("\n\n");
            text.append("### 额外信息\n").append(extraInfo).append("\n\n");
            text.append("### 堆栈跟踪\n```java\n");

            // 添加堆栈跟踪信息，限制长度避免消息过长
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                text.append("  at ").append(stackTrace[i].toString()).append("\n");
            }

            if (stackTrace.length > 10) {
                text.append("  ... ").append(stackTrace.length - 10).append(" more\n");
            }

            text.append("```\n");

            markdown.put("text", text.toString());
            message.put("markdown", markdown);

            String messageJson = JSONUtil.toJsonStr(message);
            sendPostRequest(EXPIRED_ORDER_EXCEPTION_WEBHOOK, messageJson);
        } catch (Exception e) {
            log.error("发送异常通知失败", e);
        }
    }


    /**
     * 处理过期订单的具体逻辑
     *
     * <p>
     * 主要处理流程：
     * 1. 将订单分为版本套餐订单和非版本套餐订单
     * 2. 对版本套餐订单进行特殊处理：
     * - 获取用户及其子账号列表
     * - 清空用户弹幕监控权限
     * - 检查用户是否存在，不存在则停用订单
     * 3. 对非版本套餐订单直接停用
     * 4. 对所有订单执行过期处理逻辑
     * </p>
     *
     * @param orderList             订单列表
     * @param versionOrderPredicate 版本订单判断条件
     * @param queryUserMap          用户信息查询函数
     * @param executorService       执行器服务
     */
    private void expiredOrderProcess(List<OrderInfoVo> orderList, Predicate<OrderInfoVo> versionOrderPredicate, Function<List<Long>, Map<Long, UserListVo>> queryUserMap, ExecutorService executorService) {

        // 根据是否为版本套餐将订单分组
        Map<Boolean, List<OrderInfoVo>> orderTypeMap = orderList.stream().collect(Collectors.groupingBy(versionOrderPredicate::test));
        List<OrderInfoVo> versionOrderList = orderTypeMap.get(true);

        // 根据是否为版本套餐分组处理
        if (EmptyUtil.isNotEmpty(versionOrderList)) {
            List<Long> userIds = versionOrderList.stream().map(OrderInfoVo::getUserId).distinct().toList();
            List<Long> subUserIds = userBll.getUserChild(userIds).getData();
            if (EmptyUtil.isNotEmpty(subUserIds) || EmptyUtil.isNotEmpty(userIds)) {
                List<Long> allUserIds = Stream.of(userIds, subUserIds).filter(EmptyUtil::isNotEmpty).flatMap(Collection::stream).distinct().toList();
                // 清空用户弹幕监控和自动录制相关属性
                anchorUrlBll.closeAnchorBarrageNum(allUserIds);
                userPropertyBll.clearPrivateProperty(allUserIds, "anchorBarrageNum");
            }
            subUserIds = null;
            Map<Long, UserListVo> userMap = queryUserMap.apply(userIds);

            // 筛选出用户不存在的版本订单并停用
            Map<Long, Long> notUserVersionOrder = versionOrderList.stream().filter(order -> !userMap.containsKey(order.getUserId())).collect(Collectors.toMap(OrderInfoVo::getId, order -> -1L));
            if (EmptyUtil.isNotEmpty(notUserVersionOrder)) {
                orderBll.batchStopOrder(notUserVersionOrder, true);
            }
        }
        versionOrderList = null;

        // 非版本套餐订单统一停用处理
        List<OrderInfoVo> notVersionOrderList = orderTypeMap.get(false);
        if (EmptyUtil.isNotEmpty(notVersionOrderList)) {
            orderBll.batchStopOrder(notVersionOrderList.stream().collect(Collectors.toMap(OrderInfoVo::getId, order -> -1L)), false);
        }
        orderBll.batchExpiredOrderProcessing(orderList);
//        AtomicBoolean hasError = new AtomicBoolean(false);
//        // 所有订单执行通用的过期处理逻辑
//        CompletableFuture<?>[] futures = orderList.stream().map(order -> {
//            if (hasError.get()) {
//                return CompletableFuture.completedFuture(null);
//            }
//            return CompletableFuture.runAsync(() -> {
//                // 无需处理了，直接返回
//                if (hasError.get()) {
//                    return;
//                }
//                orderBll.expiredOrderProcessing(order);
//            }, executorService).whenComplete((v, t) -> {
//                if (t != null) {
//                    log.error("[资产重置] rest order process task error. orderId:{}", order.getId(), t);
//                    hasError.set(true);
//                }
//            });
//        }).toArray(CompletableFuture[]::new);
//        CompletableFuture.allOf(futures).join();
//        if (hasError.get()) {
//            throw new RRException("[资产重置] rest order process task error");
//        }
        // orderList.forEach(orderBll::expiredOrderProcessing);
    }


    /**
     * 轮询检查子账号数量是否正确
     */
//    @Scheduled(cron = "0 1,11,21,31,41,51 * * * ? ")
//    @Scheduled(cron = "0 1,6,11,16,21,26,31,36,41,46,51,56 * * * ? ")
    @Transactional
    public void changeParentAndChild() {
        log.info("===================================开始检查用户的父子账号数量=====================================");
        // 查询所有的父账号，父账号中包含子账号
        R<List<UserInfoVo>> listR = userBll.listParentUser(null);
        if (listR.getCode() == 0 && ObjectUtil.isNotEmpty(listR.getData())) {
            log.info("查询有{}个用户", listR.getData().size());
            List<UserInfoVo> userList = listR.getData();
            for (UserInfoVo user : userList) {
                if (user.getChildAccountCount() == null) continue;
                log.info("开始检查用户={}", JSONUtil.toJsonStr(user));
                // 获取用户的资产
                List<UserPropertyTypeInfoVo> userPropertyList = userPropertyBll.getUserProperty(user.getId());
                UserPropertyTypeInfoVo subAccountCount = userPropertyList.stream()
                    .filter(item -> item.getCommodityTypeCode().equals("subAccountCount"))
                    .findFirst().orElse(null);
                // 判断他的资产里面有没有子账号数量
                if (subAccountCount == null || subAccountCount.getTotalQuantity() == null || subAccountCount.getTotalQuantity() == 0) {
                    log.info("用户{}-{}没有子账号数量，解绑所有的子账号， user={}", user.getId(), user.getUsername(), JSONUtil.toJsonStr(user));
                    // 设置子账号前的操作
                    for (UserVo child : user.getChildUserList()) {
                        this.unbindingSubAccount(user, child);
                    }
                } else {
                    log.info("用户{}-{}有子账号数量，检查是否需要解绑， user={}", user.getId(), user.getUsername(), JSONUtil.toJsonStr(user));
                    Long surplus = ObjectUtil.defaultIfNull(subAccountCount.getTotalQuantity(), 0L);
                    log.info("用户{}-{}的子账号数量={},已经绑定的子账号数量={}", user.getId(), user.getUsername(), surplus, user.getChildAccountCount());
                    // 判断是否需要解绑,资产的总子账号数量 < 用户单前的子账号数量
                    if (surplus < user.getChildAccountCount()) {
                        // 解绑掉绑定多余的子账号
                        // 解绑的顺序:绑定最早的账号先开始解绑
                        long index = user.getChildAccountCount() - surplus;
                        log.info("用户{}-{}的子账号数量不足，需要解绑的子账号数量={}", user.getId(), user.getUsername(), index);
                        R<List<UserVo>> unbindUstList = bindingAccountBll.getUnbindUstList(user.getId(), user.getChildUserList());
                        if (unbindUstList.getCode() == 0 && ObjectUtil.isNotEmpty(unbindUstList.getData())) {
                            if (surplus < unbindUstList.getData().size()) {
                                // 解绑子账号前的操作
                                for (int i = 0; i < index; i++) {
                                    this.unbindingSubAccount(user, unbindUstList.getData().get(i));
                                }
                            }
                        }
                    }
                }
            }
        }
        log.info("=====================================用户父子账号数量检查完毕======================================");
    }

    /**
     * 定时发送钉钉机器人消息
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "05 * * * * ? ")
    @XxlJob("sendDingDingRobotMessage")
    @Transactional
    public void sendDingDingRobotMessage() {
        Set<String> webhookUrlKeys = webhookUrlMap.keySet();
        if (webhookUrlKeys.isEmpty()) return;
        List<String> keys = webhookUrlKeys.stream()
            .filter(item -> redisTemplate.hasKey(RedisCacheKey.getRedisKey(RedisCacheKey.websocketErrorCacheKey, item)))
            .map(item -> RedisCacheKey.getRedisKey(RedisCacheKey.websocketErrorCacheKey, item))
            .toList();
        if (ObjectUtil.isNotEmpty(keys)) {
            for (String key : keys) {
                String[] split = key.split(":");
                String k = split[split.length - 1];
                Map<String, String> map = webhookUrlMap.get(k);
                if (map == null) continue;
                List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
                if (ObjectUtil.isNotEmpty(list)) {
                    final Date[] startDate = {null};
                    final Date[] endDate = {null};
                    List<SendSocketErrorData> dataList = list.stream()
                        .map(item -> JSONUtil.toBean(item.toString(), SendSocketErrorData.class))
                        .peek(item -> {
                            if (startDate[0] == null || startDate[0].compareTo(item.getCurrentDate()) > 0) {
                                startDate[0] = item.getCurrentDate();
                            }
                            if (endDate[0] == null || endDate[0].compareTo(item.getCurrentDate()) < 0) {
                                endDate[0] = item.getCurrentDate();
                            }
                        })
                        .toList();
                    List<String> list1 = dataList.stream().map(SendSocketErrorData::getCpuId).distinct().toList();
                    int sum = dataList.stream().mapToInt(SendSocketErrorData::getAnchorErrorNum).sum();

                    String errorStr = map.get("errorStr");
                    String time = DateUtil.format(startDate[0], "yyyy-MM-dd HH:mm:ss");
                    if (startDate[0].compareTo(endDate[0]) != 0) {
                        time = time + "~" + DateUtil.format(endDate[0], "yyyy-MM-dd HH:mm:ss");
                    }
                    String num = String.valueOf(list1.size());
                    String anchorNum = String.valueOf(sum);
                    String causeOfError = map.get("causeOfError");
                    // 发送 POST 请求
                    sendPostRequest(map.get("url"), buildSendMessageBody(errorStr, time, num, anchorNum, causeOfError));

                    this.redisTemplate.delete(key);
                }
            }
        }
    }

    /**
     * 每2分钟批量插入在线人数
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0/5 * * * ? ")
    @XxlJob("batchSaveOnlineNum")
    public void batchSaveOnlineNum() throws Exception {
        this.redisTemplate.opsForList().size(wordsProperties.getRedisOnlineNumInfo());
        if (Boolean.TRUE.equals(this.redisTemplate.hasKey(wordsProperties.getRedisOnlineNumInfo()))) {

            List<Object> onlineNumList = this.redisTemplate.opsForList().range(wordsProperties.getRedisOnlineNumInfo(), 0, -1);
            if (onlineNumList != null && !onlineNumList.isEmpty()) {
                List<OnlineNumBo> onlineNumBos = BeanUtil.copyToList(onlineNumList, OnlineNumBo.class);
                Map<String, List<OnlineNumBo>> listMap = onlineNumBos.stream()
                    .filter(item -> ObjectUtil.isNotEmpty(item.getVideoId()))
                    .peek(item -> item.setPeopleNum(CommonUtils.removeWanAdd(item.getPeopleNum())))
                    .collect(Collectors.groupingBy(item -> item.getUserId() + "&" + item.getBatchNumber() + "&" + item.getVideoId()));
                for (String k : listMap.keySet()) {
                    List<OnlineNumBo> v = listMap.get(k);

                    String[] split = k.split("&");

                    String batchNumber = split[1];
                    Long userid = Long.valueOf(split[0]);
                    String videoId = split[2];
                    List<OnlineNumBo> list = v.stream().sorted(Comparator.comparing(OnlineNumBo::getRecordDate)).toList();
                    OnlineNumBo onlineNumBo = list.get(0);

                    UploadSocketMessageBo bo = new UploadSocketMessageBo();
                    String cosKey = "socketMessageFile/" + new SimpleDateFormat("yyyy/MM/dd/").format(new Date()) + cn.hutool.core.lang.UUID.randomUUID() + ".zip";
                    R<SocketCollectMessageInfoVo> byBatch = uploadSocketMessage.getByBatch(batchNumber, userid, videoId);
                    if (byBatch != null && byBatch.getCode() == 0 && ObjectUtil.isNotEmpty(byBatch.getData())) {
                        BeanUtil.copyProperties(byBatch.getData(), bo);
                        // 获取文件的内容
//                        if (ObjectUtil.isNotEmpty(byBatch.getData().getFileAddress())){
//                            bo.setWebSocketData(ReplayFileUtils.readFirstTxtFromZip(wordsProperties.getServerWebsocketPath() + byBatch.getData().getFileAddress()));
//                        }
                        if (ObjectUtil.isNotEmpty(byBatch.getData().getCosKey())) {
                            String filePath = UUID.randomUUID() + ".zip";
                            cosKey = byBatch.getData().getCosKey();

                            // 从cos上下载文件
                            R<SocketDataBo> websocketDataR = socketCollectMessageBll.getWebsocketData(videoId, cosKey);
                            if (websocketDataR.getCode() == 0 && ObjectUtil.isNotEmpty(websocketDataR.getData())) {
                                bo.setWebSocketData(JSONUtil.toJsonStr(websocketDataR.getData()));
                            }
//                            if (TencentCosUtils.downloadCosFile(tencentCosTokenVo, wordsProperties.getServerWebsocketPath()+"/"+ filePath, byBatch.getData().getCosKey())){
//                                bo.setWebSocketData(ReplayFileUtils.readFirstTxtFromZip(wordsProperties.getServerWebsocketPath() + byBatch.getData().getFileAddress()));
//                                // 删除本地的视频
//                                FileUtil.del(wordsProperties.getServerWebsocketPath() + filePath);
//                            }
                        }
                        bo.setEndDate(DateUtil.parse(list.get(list.size() - 1).getRecordDate()));
                    } else {
                        bo.setUserId(userid);
                        bo.setSecUid(onlineNumBo.getSecUid());
                        bo.setTenantId(onlineNumBo.getTenantId());
                        bo.setBatchNumber(batchNumber);
                        bo.setStartDate(DateUtil.parse(list.get(0).getRecordDate()));
                        bo.setEndDate(DateUtil.parse(list.get(list.size() - 1).getRecordDate()));
                    }

                    if (ObjectUtil.isNotEmpty(bo.getWebSocketData())) {
                        JSON parse = JSONUtil.parse(bo.getWebSocketData());
                        SocketDataBo dataBo = parse.toBean(SocketDataBo.class);
                        if (ObjectUtil.isEmpty(dataBo.getDatas())) dataBo.setDatas(new ArrayList<>());
                        for (OnlineNumBo numBo : list) {
                            SocketProcessDataBo e = new SocketProcessDataBo();
                            e.setTime(numBo.getRecordDate());
                            e.setRenshu(numBo.getPeopleNum());
                            dataBo.getDatas().add(e);
                        }
                        bo.setWebSocketData(JSONUtil.toJsonStr(dataBo));
                    } else {
                        SocketDataBo socketDataBo = new SocketDataBo(bo);
                        socketDataBo.setVersion("1.0");
                        ArrayList<SocketProcessDataBo> datas = new ArrayList<>();
                        socketDataBo.setDatas(datas);
                        // setdatas
                        for (OnlineNumBo data : list) {
                            SocketProcessDataBo socketProcessDataBo = new SocketProcessDataBo();
                            socketProcessDataBo.setTime(data.getRecordDate());
                            socketProcessDataBo.setRenshu(data.getPeopleNum());
                            datas.add(socketProcessDataBo);
                        }
                        bo.setWebSocketData(JSONUtil.toJsonStr(socketDataBo));
                    }

                    UploadSocketDataBo bo1 = BeanUtil.copyProperties(bo, UploadSocketDataBo.class);
                    // 上传socket数据到cos上
                    if (ObjectUtil.isNotEmpty(bo.getWebSocketData())) {
                        ByteArrayOutputStream zipStream = ReplayFileUtils.createZipStream(bo.getWebSocketData(), bo.getVideoId() + ".txt");
                        if (TencentCosUtils.putStreamObject(TencentCosUtils.getPrivateCosBucketName(), new ByteArrayInputStream(zipStream.toByteArray()), cosKey)) {
                            // 添加缓存
                            socketCollectMessageBll.setWebsocketRedisData(videoId, bo.getWebSocketData());
                            bo1.setCosKey(cosKey);
                            socketCollectMessageBll.uploadSocketData(bo1);
                        } else {
                            log.error("在线人数上传cos失败");
                        }
                    }

                }
            }
            this.redisTemplate.delete(wordsProperties.getRedisOnlineNumInfo());
        }
    }

    private String buildSendMessageBody(String errorStr, String time, String num, String anchorNum, String causeOfError) {
        Map<String, Object> map = new HashMap<>();
        map.put("msgtype", "markdown");
        HashMap<String, String> markdown = new HashMap<>();
        markdown.put("title", "socket地址故障");
        String string = StrUtil.format("""
            ## {}\s
            ### 时间: {}\s
            ### 错误电脑个数: {}个\s
            ### 错误主播个数: {}个\s
            ### 错误原因: {}\s
            """, errorStr, time, num, anchorNum, causeOfError);
        markdown.put("text", string);
        map.put("markdown", markdown);
        return JSONObject.toJSONString(map);
    }

    public void sendPostRequest(String webhookUrl, String message) {
        // 非生产环境不发送订订消息
        if (!environment.matchesProfiles("prod")) {
            return;
        }
        try {
            // 创建 HttpClient 对象
            HttpClient client = HttpClient.newHttpClient();

            // 创建 HttpRequest 对象
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();

            // 发送请求并获取响应
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 打印响应内容
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unbindingSubAccount(UserInfoVo user, UserVo child) {
        log.info("解绑子账号，user={}, child={}", JSONUtil.toJsonStr(user), JSONUtil.toJsonStr(child));
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(user.getId());
        assets.setUserName(user.getNickName());
        assets.setNum(1L);
        assets.setCode("subAccountCount");
        UserPropertyImpl.use(assets);
        log.info("父账号的子账号数量加一");

        userPropertyBll.unbindingSubAccount(user.getId(), child.getId());
        // 修改绑定记录
        bindingAccountBll.unbindingSubAccount(user.getId(), child.getId(), "父账号可添加的子账号不够，自动解绑");
        // 修改用户的账号类型、parentId字段
        UserBo userBo = new UserBo();
        userBo.setUserId(child.getId());
        userBo.setUserType(0);
        userBo.setParentId(0L);
        userBll.updateParentId(userBo);

        log.info("检查子账号是否有版本");
        R<List<OrderInfoVo>> orderByUserId = orderBll.getOrderByUserId(child.getId());
        boolean isNextNewOrder = false;
        if (orderByUserId.getCode() == 0) {
            if (ObjectUtil.isNotEmpty(orderByUserId.getData())) {
                List<OrderInfoVo> orderList = orderByUserId.getData();
                // 判断是否有正在使用的版本
                if (orderList.stream().noneMatch(o -> o.getCommodityType() == 1 && o.getStatus() == 2)) {
                    // 没有，就下免费的版本
                    isNextNewOrder = true;
                }
            } else {
                // 没有，就下免费的版本
                isNextNewOrder = true;
            }
        }
        log.info("检查子账号是否有版本:{}", isNextNewOrder);
        if (isNextNewOrder) {
            CreateOrderBo createOrderBo = new CreateOrderBo();
            createOrderBo.setUserId(child.getId());
            createOrderBo.setUserName(child.getNickName());
            orderBll.createNewOrder(createOrderBo);
        }
    }
}
