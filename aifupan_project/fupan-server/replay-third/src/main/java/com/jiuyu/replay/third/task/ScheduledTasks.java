package com.jiuyu.replay.third.task;

import com.jiuyu.replay.common.constant.TencentAudioProperties;
import com.jiuyu.replay.third.repository.service.AudioLogService;
import com.jiuyu.replay.third.repository.service.LogAudioCollectService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    @Resource
    private AudioLogService audioLogService;
    @Resource
    private TencentAudioProperties tencentAudioProperties;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private LogAudioCollectService logAudioCollectService;

    /**
     * 每13秒，统计qps峰值和用户量，插入数据库
     */
//    @Scheduled(cron = "0/13 * * * * ?")
//    public void qpsCount() {
//
//        // 拿到所有redis中的日志
//        Set<String> keys = RedisOperationUtils.scanKeys(tencentAudioProperties.getQpsLogKeyPrefix() + "*");
//        if(keys != null && keys.size() > 0) {
//            List<String> logStrList = redisTemplate.opsForValue().multiGet(keys);
//            if(logStrList != null && logStrList.size() > 0) {
//
//                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//                // 统计出每个时间点
//                Set<String> timeList = new HashSet<>();
//                for (String item : logStrList) {
//                    Date date = new Date(Long.parseLong(item.split("-")[1]));
//                    timeList.add(simpleDateFormat.format(date));
//                }
//
//                if(timeList.size() > 0) {
//                    List<LogAudioCollectEntity> logAudioCollectEntities = timeList.stream().map(timeItem -> {
//                        LogAudioCollectEntity logAudioCollectEntity = new LogAudioCollectEntity();
//                        logAudioCollectEntity.setId(SnowflakeManager.nextValue());
//                        int qpsNum = 0;
//                        Set<Long> userIdSet = new HashSet<>();
//                        for (String item : logStrList) {
//                            Date date = new Date(Long.parseLong(item.split("-")[1]));
//                            String time = simpleDateFormat.format(date);
//                            if (time.equals(timeItem)) {
//                                long userId = Long.parseLong(item.split("-")[0]);
//                                userIdSet.add(userId);
//
//                                qpsNum++;
//                            }
//
//                        }
//                        try {
//                            logAudioCollectEntity.setCallDate(simpleDateFormat.parse(timeItem));
//                        } catch (ParseException e) {
//                            throw new RuntimeException(e);
//                        }
//                        logAudioCollectEntity.setQpsNumCount(qpsNum);
//                        logAudioCollectEntity.setUserNumCount(userIdSet.size());
//                        logAudioCollectEntity.setCreateDate(new Date());
//                        logAudioCollectEntity.setUpdateDate(new Date());
//                        return logAudioCollectEntity;
//                    }).collect(Collectors.toList());
//
//                    this.logAudioCollectService.saveBatch(logAudioCollectEntities);
//                }
//
//                // 删除key
//                redisTemplate.delete(keys);
//            }
//        }
//
//    }

    /**
     * 每10秒，批量插入语音识别接口日志到数据库--1.9.10以后版本废弃
     */
//    @Scheduled(cron = "0/10 * * * * ?")
//    public void batchSaveAudioLog() {
//
//        // 拿到所有redis中的日志
//        Set<String> keys = RedisOperationUtils.scanKeys(tencentAudioProperties.getLogKeyPrefix() + "*");
//        if(keys != null && keys.size() > 0) {
//            List<String> logStrList = redisTemplate.opsForValue().multiGet(keys);
//            if(logStrList != null && logStrList.size() > 0) {
//                // 保存数据到数据库
//                List<AudioLogEntity> audioLogEntities = logStrList.stream().map(item -> {
//                    AudioLogEntity audioLogEntity = new AudioLogEntity();
//                    audioLogEntity.setId(SnowflakeManager.nextValue());
//                    audioLogEntity.setUserId(Long.valueOf(item.split("-")[0]));
//                    Date date = new Date(Long.parseLong(item.split("-")[1]));
//                    audioLogEntity.setCallDate(date);
//                    audioLogEntity.setCreateDate(new Date());
//                    audioLogEntity.setUpdateDate(new Date());
//                    audioLogEntity.setIsDeleted(0);
//                    return audioLogEntity;
//                }).toList();
//                this.audioLogService.saveBatch(audioLogEntities);
//
//                // 删除key
//                redisTemplate.delete(keys);
//            }
//        }
//
//    }
}
