//package com.jiuyu.replay.third.listener;
//
//import com.jiuyu.replay.common.constant.AudioSecretProperties;
//import com.jiuyu.replay.common.constant.TencentAudioProperties;
//import com.jiuyu.replay.common.tencent.TencentAudioUtils;
//import jakarta.annotation.Resource;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//public class RedisTimeoutThirdListener extends KeyExpirationEventMessageListener {
//
//    @Resource
//    private TencentAudioProperties tencentAudioProperties;
//    @Resource
//    private RedisTemplate<String, Object> redisTemplate;
//    @Resource
//    private TencentAudioUtils tencentAudioUtils;
//
//    public RedisTimeoutThirdListener(RedisMessageListenerContainer listenerContainer) {
//        super(listenerContainer);
//    }
//
//    @Override
//    public void onMessage(Message message, byte[] pattern) {
//        String key = message.toString();
//        System.out.println("third过期的key:" + key);
//
//        if(key.startsWith(tencentAudioProperties.getRedisTimeoutKeyPrefix())) {
//            // 如果qps被正常加回，会被直接删除，这里也就不会触发
//
//            // 拿到语音识别secretId
//            String[] idAndSecretId = key.substring(key.lastIndexOf(":")).split("-");
//            String secretId = idAndSecretId[1];
//
//            List<AudioSecretProperties> secret = tencentAudioProperties.getSecret();
//            for (AudioSecretProperties audioSecretProperties : secret) {
//                if(audioSecretProperties.getSecretId().equals(secretId)) {
//                    // 余量没有正确加回，在这里进行补加
//                    Integer number = (Integer) redisTemplate.opsForValue().get(tencentAudioProperties.getRedisKeyPrefix() + secretId);
//                    if(number < audioSecretProperties.getQpsNum()) {
//                        number += 1;
//                        redisTemplate.opsForValue().set(tencentAudioProperties.getRedisKeyPrefix() + secretId, number);
//                    }
//                }
//            }
//
//
//        }
//    }
//}
