package com.jiuyu.replay.words.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jiuyu.replay.words.entity.VideoDataViewingConfuseEntity;
import com.jiuyu.replay.words.repository.service.VideoDataViewingConfuseService;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
public class ViewingScheduledTasks {

    @Resource
    private VideoDataViewingConfuseService videoDataViewingConfuseService;

    /**
     * 每5分钟，关闭超过60分钟还没数据的看盘数据
     */
    // 使用调度器配置执行cron时间 @Scheduled(cron = "0 0/5 * * * ? ")
    @XxlJob("batchTrainView")
    public void batchTrain() {

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(60);
        String dateStr = localDateTime.format(dateTimeFormatter);

        UpdateWrapper<VideoDataViewingConfuseEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("data_status", 0);
        wrapper.le("create_date", dateStr);

        wrapper.set("data_status", 2);
        wrapper.set("update_date", new Date());

        this.videoDataViewingConfuseService.update(wrapper);

    }

}