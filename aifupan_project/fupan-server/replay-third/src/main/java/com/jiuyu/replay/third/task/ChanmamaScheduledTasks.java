package com.jiuyu.replay.third.task;

import com.jiuyu.replay.third.chanmama.ThirdDataUtils;
import com.jiuyu.replay.third.constant.ChanmamaProperties;
import com.jiuyu.replay.third.repository.service.ChanmamaAccountService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChanmamaScheduledTasks {

    @Resource
    private ChanmamaAccountService chanmamaAccountService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ChanmamaProperties chanmamaProperties;
    @Resource
    private ThirdDataUtils chanmamaUtils;

    /**
     * 每天凌晨，刷新第三方数据平台查询次数
     */
//    @Scheduled(cron = "0 0 0 * * ?")
//    public void refreshChanmamaQueryNum() {
//
//        Set<String> accountKeys = RedisOperationUtils.scanKeys(chanmamaProperties.getTokenRedisKey() + "*");
//        if(accountKeys.size() > 0) {
//            for (String accountKey : accountKeys) {
//                Object obj = redisTemplate.opsForValue().get(accountKey);
//                if(obj != null) {
//                    ChanmamaCache chanmamaCache = (ChanmamaCache) obj;
//                    ChanmamaAccountEntity chanmamaAccountEntity = this.chanmamaAccountService.getById(chanmamaCache.getId());
//                    if(chanmamaAccountEntity != null) {
//                        chanmamaCache.setRemainingQueryNum(chanmamaAccountEntity.getEveryDayQueryNum());
//                        redisTemplate.opsForValue().set(chanmamaProperties.getTokenRedisKey() + chanmamaAccountEntity.getId(), chanmamaCache, Duration.ofMinutes(30));
//                    }
//                }
//            }
//        }
//    }

    /**
     * 每5分钟，刷新第三方数据平台登录token
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0/50 0/5 * * * ?")
    @XxlJob("refreshChanmamaToken")
    public void refreshChanmamaToken() {

        chanmamaUtils.refreshChanmamaToken();

    }


}
