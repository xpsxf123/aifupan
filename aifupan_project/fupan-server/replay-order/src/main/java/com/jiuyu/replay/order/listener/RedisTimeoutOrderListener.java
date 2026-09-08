//package com.jiuyu.replay.order.listener;
//
//import com.jiuyu.replay.order.bll.OrderBll;
//import com.jiuyu.replay.order.bll.OrderPayBll;
//import com.jiuyu.replay.order.constant.OrderProperties;
//import jakarta.annotation.Resource;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//@Component
//public class RedisTimeoutOrderListener extends KeyExpirationEventMessageListener {
//
//    @Resource
//    private OrderProperties orderProperties;
//
//    @Resource
//    private OrderPayBll orderPayBll;
//
//    @Resource
//    private OrderBll orderBll;
//
//    public RedisTimeoutOrderListener(RedisMessageListenerContainer listenerContainer) {
//        super(listenerContainer);
//    }
//
//    @Override
//    @Transactional
//    public void onMessage(Message message, byte[] pattern) {
//        String key = message.toString();
//        System.out.println("order过期的key:" + key);
//        if(key.startsWith(orderProperties.getOrderTimeoutRedisKey())) {
//            // 处理超时订单
//            // 获取订单id
//            Long orderId = Long.valueOf(key.substring(key.lastIndexOf(":") + 1));
//            orderPayBll.orderExpired(orderId);
//        }
//    }
//
//}
