package com.jiuyu.replay.words.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.AiTrainEntity;
import com.jiuyu.replay.words.producer.SocketCollectMessageProducer;
import com.jiuyu.replay.words.repository.service.AiTrainService;
import com.jiuyu.replay.words.repository.service.OnlineNumService;
import com.jiuyu.replay.words.repository.service.SocketCollectMessageService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class WordsScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(WordsScheduledTasks.class);
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private OnlineNumService onlineNumService;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;
    @Resource
    private SocketCollectMessageService socketCollectMessageService;
    @Resource
    private AiTrainService aiTrainService;

    /**
     * 每1小时自动完成超时的训练
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0 0/1 * * ? ")
    @XxlJob("batchTrainWords")
    public void batchTrain() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String toDay = sdf.format(calendar.getTime());

        List<AiTrainEntity> aiTrainEntities = aiTrainService.list(
                new QueryWrapper<AiTrainEntity>()
                        .eq("ai_status", 0)
                        .le("create_date", toDay));

        if(aiTrainEntities != null && aiTrainEntities.size() > 0) {
            for (AiTrainEntity aiTrainEntity : aiTrainEntities) {
                aiTrainEntity.setAiStatus(2);
                // 设置进步幅度
                double progressRange = 0.001 + (0.01 - 0.001) * Math.random();
                aiTrainEntity.setProgressRange(progressRange);
                aiTrainEntity.setUpdateDate(new Date());
            }
            this.aiTrainService.updateBatchById(aiTrainEntities);
        }
    }

//    /**
//     * 每2分钟批量插入在线人数
//     */
//    @Scheduled(cron = "0 0/1 * * * ? ")
//    public void batchSaveOnlineNum() {
//        Set<String> keys = RedisOperationUtils.scanKeys(wordsProperties.getRedisOnlineNumInfo() + "*");
//
//        if(keys != null && !keys.isEmpty()) {
//            List<Object> onlineNumList = this.redisTemplate.opsForValue().multiGet(keys);
//            if(onlineNumList != null && !onlineNumList.isEmpty()) {
//                List<OnlineNumBo> onlineNumBos = BeanUtil.copyToList(onlineNumList, OnlineNumBo.class);
//                Map<String, List<OnlineNumBo>> listMap = onlineNumBos.stream()
//                        .filter(item -> ObjectUtil.isNotEmpty(item.getVideoId()))
//                        .peek(item -> item.setPeopleNum(CommonUtils.removeWanAdd(item.getPeopleNum())))
//                        .collect(Collectors.groupingBy(item -> item.getUserId() + "&" + item.getBatchNumber() + "&" + item.getVideoId()));
//                listMap.forEach((k, v) -> {
//                    String[] split = k.split("&");
//
//                    String batchNumber = split[1];
//                    Long userid = Long.valueOf(split[0]);
//                    String videoId = split[2];
//                    List<OnlineNumBo> list = v.stream().sorted(Comparator.comparing(OnlineNumBo::getRecordDate)).toList();
//                    OnlineNumBo onlineNumBo = list.get(0);
//
//                    UploadSocketMessageBo bo = new UploadSocketMessageBo();
//
//                    SocketCollectMessageInfoVo byBatch = socketCollectMessageProducer.getByBatch(batchNumber, userid, videoId);
//                    if (byBatch != null){
//                        BeanUtil.copyProperties(byBatch, bo);
//                        // 获取文件的内容
//                        if (ObjectUtil.isNotEmpty(byBatch.getFileAddress())){
//                            bo.setWebSocketData(ReplayFileUtils.readFirstTxtFromZip(wordsProperties.getServerWebsocketPath() + byBatch.getFileAddress()));
//                        }
//                        bo.setEndDate(DateUtil.parse(list.get(list.size() - 1).getRecordDate()));
//                    }else{
//                        bo.setUserId(userid);
//                        bo.setSecUid(onlineNumBo.getSecUid());
//                        bo.setBatchNumber(batchNumber);
//                        bo.setStartDate(DateUtil.parse(list.get(0).getRecordDate()));
//                        bo.setEndDate(DateUtil.parse(list.get(list.size() - 1).getRecordDate()));
//                    }
//
//                    if (ObjectUtil.isNotEmpty(bo.getWebSocketData())){
//                        JSON parse = JSONUtil.parse(bo.getWebSocketData());
//                        SocketDataBo dataBo = parse.toBean(SocketDataBo.class);
//                        if (ObjectUtil.isEmpty(dataBo.getDatas())) dataBo.setDatas(new ArrayList<>());
//                        for (OnlineNumBo numBo : list) {
//                            SocketProcessDataBo e = new SocketProcessDataBo();
//                            e.setTime(numBo.getRecordDate());
//                            e.setRenshu(numBo.getPeopleNum());
//                            dataBo.getDatas().add(e);
//                        }
//                        bo.setWebSocketData(JSONUtil.toJsonStr(dataBo));
//                    }else{
//                        SocketDataBo socketDataBo = new SocketDataBo(bo);
//                        socketDataBo.setVersion("1.0");
//                        ArrayList<SocketProcessDataBo> datas = new ArrayList<>();
//                        socketDataBo.setDatas(datas);
//
//                        // setdatas
//                        for (OnlineNumBo data : list) {
//                            SocketProcessDataBo socketProcessDataBo = new SocketProcessDataBo();
//                            socketProcessDataBo.setTime(data.getRecordDate());
//                            socketProcessDataBo.setRenshu(data.getPeopleNum());
//                            datas.add(socketProcessDataBo);
//                        }
//                        bo.setWebSocketData(JSONUtil.toJsonStr(socketDataBo));
//                    }
//
//                    try {
//                        socketCollectMessageProducer.uploadSocketMessage(bo);
//                    } catch (IOException e) {
//                        log.error("上传websocket采集的信息报错，{}", e.getMessage());
//                    }
//                });
//            }
//
//            this.redisTemplate.delete(keys);
//        }
//    }
}