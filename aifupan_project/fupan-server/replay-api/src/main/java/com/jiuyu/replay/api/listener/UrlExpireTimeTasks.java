package com.jiuyu.replay.api.listener;


import com.jiuyu.replay.ai.bll.ShareLinkRecordBll;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
public class UrlExpireTimeTasks {


    @Resource
    private ShareLinkRecordBll shareLinkRecordBll ;
    @Resource
    private SystemKvBll systemKvBll;

    //分享链接失效时，自动删除
    @XxlJob("updateUrlExpire")
    public void updateUrlExpire() {
        R<SystemKvInfoVo> urlExpireTime = systemKvBll.getByKey("url_expire_time");
        if (Objects.nonNull(urlExpireTime)&&Objects.nonNull(urlExpireTime.getData())){
            String kvValue = urlExpireTime.getData().getKvValue();
            log.info("获取到url分享链接失效时间为{}",kvValue);
            shareLinkRecordBll.updateUrlExpire(kvValue);
        }

    }
}
