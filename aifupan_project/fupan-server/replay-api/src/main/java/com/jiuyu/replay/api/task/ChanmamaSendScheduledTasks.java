package com.jiuyu.replay.api.task;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.third.bo.chanmama.ChanmamaQueryBo;
import com.jiuyu.replay.third.chanmama.ThirdDataUtils;
import com.jiuyu.replay.words.bll.ChanmamaSendRecordBll;
import com.jiuyu.replay.words.bll.VideoDataViewingBll;
import com.jiuyu.replay.words.bll.VideoDataViewingConfuseBll;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordBo;
import com.jiuyu.replay.words.bo.viewing.DelaySendChanmamaBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class ChanmamaSendScheduledTasks {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private ThirdDataUtils chanmamaUtils;
    @Resource
    private ChanmamaSendRecordBll chanmamaSendRecordBll;
    @Resource
    private VideoDataViewingConfuseBll videoDataViewingConfuseBll;
    @Resource
    private VideoDataViewingBll videoDataViewingBll;

    /**
     * 定时检查发送第三方数据平台查询
     * @return
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0/3 * * * ?")
    @XxlJob("sendChanmamaQuery")
    public void sendChanmamaQuery() {

        Map<Object, Object> entries = this.redisTemplate.opsForHash().entries("replay:chanmama:send");
        if(entries.size() > 0) {
            long currentTime = new Date().getTime();

            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                try {
                    String delaySendChanmamaBoStr = (String) entry.getValue();
                    if(!StringUtils.isEmpty(delaySendChanmamaBoStr)) {
                        DelaySendChanmamaBo delaySendChanmamaBo = JSONObject.parseObject(delaySendChanmamaBoStr, DelaySendChanmamaBo.class);
                        if(delaySendChanmamaBo != null) {
                            Long createTime = delaySendChanmamaBo.getCreateTime();
                            if(currentTime - createTime >= 8 * 60 * 1000) {
                                // 超过十分钟，才发送
                                ChanmamaQueryBo chanmamaQueryBo = JSONObject.parseObject(delaySendChanmamaBo.getChanmamaQueryBoJson(), ChanmamaQueryBo.class);
                                ChanmamaSendRecordBo chanmamaSendRecordBo = JSONObject.parseObject(delaySendChanmamaBo.getChanmamaSendRecordBoJson(), ChanmamaSendRecordBo.class);

                                // 判断数据库是否有需要发请求的数据看板记录，如果有才发
                                R<List<VideoDataViewingConfuseInfoVo>> viewingListR = videoDataViewingConfuseBll.listByRequestId(chanmamaSendRecordBo.getRequestId());
                                List<VideoDataViewingConfuseInfoVo> viewingList = viewingListR.getData();
                                if(viewingList == null || viewingList.isEmpty()) {
                                    chanmamaSendRecordBo.setResponseStatus("没有需要发送请求的数据看板，不发请求");
                                    chanmamaSendRecordBo.setResponseBody("没有需要发送请求的数据看板，不发请求");
                                    this.chanmamaSendRecordBll.save(chanmamaSendRecordBo);

                                    // 从redis中删除
                                    this.redisTemplate.opsForHash().delete("replay:chanmama:send", entry.getKey());
                                    continue;
                                }

                                // 判断如果有巨量百应的数据，从列表剔除
                                Iterator<VideoDataViewingConfuseInfoVo> iterator = viewingList.iterator();
                                while (iterator.hasNext()) {
                                    VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = iterator.next();
                                    Boolean flag = checkJlbyDataExistCreate(videoDataViewingConfuseInfoVo.getUserId(), videoDataViewingConfuseInfoVo.getVideoId());
                                    if(flag) {
                                        iterator.remove();
                                    }
                                }

                                if(!viewingList.isEmpty()) {
                                    // 发起查询
                                    log.info("==发起查询start=={}", chanmamaQueryBo.getRequestId());
                                    R<String> queryR = chanmamaUtils.queryData(chanmamaQueryBo);
                                    log.info("==发起查询end=={}", chanmamaQueryBo.getRequestId());

                                    // 保存查询记录
                                    chanmamaSendRecordBo.setResponseStatus(queryR.getCode().toString());
                                    chanmamaSendRecordBo.setResponseBody(queryR.getData());
                                }else {
                                    chanmamaSendRecordBo.setResponseStatus("没有需要发送请求的数据看板，不发请求");
                                    chanmamaSendRecordBo.setResponseBody("没有需要发送请求的数据看板，不发请求");
                                }

                                this.chanmamaSendRecordBll.save(chanmamaSendRecordBo);

                                // 从redis中删除
                                this.redisTemplate.opsForHash().delete("replay:chanmama:send", entry.getKey());
                            }

                        }
                    }
                }catch (Exception e) {
                    log.info("==发起查询错误=={}", JSON.toJSONString(entry));
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 查询同租户下有没有对应的巨量百应数据，如果有就直接创建数据看盘
     * @param userId 用户id
     * @param videoId 视频id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean checkJlbyDataExistCreate(Long userId, String videoId) {
        try {
            // 检查是否有对应的巨量百应数据
            R<VideoDataViewingConfuseInfoVo> viewingConfuseInfoVoR = videoDataViewingBll.checkJlbyDataExist(videoId);
            if(viewingConfuseInfoVoR.getCode() == StatusCode.SUCCESS.getCode() && viewingConfuseInfoVoR.getData() != null) {
                VideoDataViewingConfuseInfoVo viewingConfuseInfoVo = viewingConfuseInfoVoR.getData();
                VideoDataViewingConfuseBo viewingConfuseBo = BeanConvertUtils.convert(viewingConfuseInfoVo, VideoDataViewingConfuseBo.class);
                // 删除原数据看板数据
                videoDataViewingConfuseBll.deleteByVideoId(videoId);
                // 拷贝对应的巨量百应数据
                videoDataViewingBll.copyTenantJlbyData(viewingConfuseBo, userId, videoId);
                return true;
            }
        }catch (Exception e) {
            log.info("==定时检查发送第三方数据平台查询，检查是否有巨量百应数据发送异常=={}", videoId);
        }

        return false;
    }
}
