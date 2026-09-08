package com.jiuyu.replay.api.listener;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderPayInfoVo;
import com.jiuyu.replay.order.bll.CommodityTypeBll;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.OrderPayBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.constant.OrderProperties;
import com.jiuyu.replay.power.bll.UserBll;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class StartupEventListener {

    private final OrderPayBll orderPayBll;
    private final OrderProperties orderProperties;
    private final CommonProperties commonProperties;
    private final RedisTemplate redisTemplate;
    private final CommodityTypeBll commodityTypeBll;
    private final OrderBll orderBll;
    private final UserPropertyBll userPropertyBll;
    private final UserBll userBll;

    @EventListener
    public void handleApplicationReady(ApplicationReadyEvent event) {
        // 添加父子用户管理
//        this.addParentAndChild();

        // 把用户资产缓存到redis中
//        this.addUserPropertyCache();

        // 启动时，把用户现在用的套餐订单缓存到redis中
//        this.setUserOrderCache(null);

        // 检查订单是否过期
//        this.checkOrderExpired();
    }

//    private void addParentAndChild() {
//        // 删除全部的父子管理
////        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userParentIdCacheKey, "*"));
//        R<List<UserVo>> listR = userBll.listAllByParent();
//        if (ObjectUtil.isNotEmpty(listR.getData())){
//            for (UserVo user : listR.getData()) {
//                redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userParentIdCacheKey, user.getId()), user.getParentId());
//            }
//        }
//    }

    /**
     * 启动期一次性用户资产缓存预热。
     * <p>显式绑定平台线程池 {@code threadPoolTaskExecutor}：本任务为启动期一次性预热，不需虚拟线程化，
     * 防止 {@code spring.threads.virtual.enabled=true} 后默认 @Async 切到虚拟线程导致的行为漂移。</p>
     */
    @Async("threadPoolTaskExecutor")
    public void addUserPropertyCache(){
        userPropertyBll.addUserPropertyCache();
    }

    public void setUserOrderCache(List<Long> userIds){
        // 获取用户当前使用的套餐
        R<List<OrderInfoVo>> validOrders = orderBll.getNotExpirationOrders(userIds);
        if (ObjectUtil.isNotEmpty(validOrders.getData())){
            List<OrderInfoVo> data = validOrders.getData();
            orderBll.setUserPackageCache(data);
        }
    }

    /**
     * 检查订单是否过期
     */
    public void checkOrderExpired(){
        log.info("开始检查订单是否过期");
        R<List<OrderPayInfoVo>> validOrders = orderPayBll.getValidOrders();
        if (validOrders.getCode() == 0 && validOrders.getData() != null){
            List<OrderPayInfoVo> data = validOrders.getData();
            // 获取当前时间
            DateTime currentTime = DateUtil.date();
            data.forEach(item -> {
                DateTime expireTime = new DateTime(item.getCreateDate());
                DateTime endTime = expireTime.offsetNew(DateField.MINUTE, commonProperties.getOrderTimeoutMinutes().intValue());
                if (currentTime.isAfter(endTime)) {
                    log.info("订单已过期，订单id：{}", item.getId());
                    orderPayBll.orderExpired(item.getId());
                }else{
                    log.info("未到订单过期时间，订单id：{}", item.getId());
                    long off = (endTime.getTime() - currentTime.getTime()) / 1000;
                    redisTemplate.opsForValue().set(orderProperties.getOrderTimeoutRedisKey() + item.getId(), item.getId(), Duration.ofSeconds(off));
                }
            });
        }
        log.info("检查订单是否过期完成");
    }
}