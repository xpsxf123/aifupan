package com.jiuyu.replay.power.task;

import com.jiuyu.replay.power.constant.PowerProperties;
import com.jiuyu.replay.power.producer.UserLoginLogProducer;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserScheduledTasks {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PowerProperties powerProperties;
    @Resource
    private UserLoginLogProducer userLoginLogProducer;

    /**
     * 每10秒，检查用户上一次请求时间是否大于30秒，是则表示用户已经离线，修改用户为离线状态
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0/10 * * * * ?")
    @XxlJob("checkUserLogin")
    public void checkUserLogin() {

//        Set<String> keys = RedisOperationUtils.scanKeys(powerProperties.getUserRedisKey() + "*");
//
//        if(keys != null && !keys.isEmpty()) {
//            long currentTime = new Date().getTime();
//            for (String key : keys) {
//                UserCacheVo userCacheVo = (UserCacheVo) this.redisTemplate.opsForValue().get(key);
//                if(userCacheVo != null && (userCacheVo.getUserType() == null || userCacheVo.getUserType() == 0 || userCacheVo.getUserType() == 2)) {
//                    if(StringUtils.isEmpty(userCacheVo.getRequestTime()) || currentTime - userCacheVo.getRequestTime() > 30000) {
//                        if (ObjectUtil.equal(userCacheVo.getOnlineStatus(), 1)){
//                            // 添加日志
//                            UserLoginLogBo bo = new UserLoginLogBo(userCacheVo, userCacheVo.getIp(), "30秒没有检测到心跳，自动改为离线状态", 1, 0);
//                            userLoginLogProducer.save(bo);
//                        }
//                        userCacheVo.setOnlineStatus(0);
//                        this.redisTemplate.opsForValue().set(key, userCacheVo,30L, TimeUnit.DAYS);
//                    }
//                }
//            }
//        }

    }
}
