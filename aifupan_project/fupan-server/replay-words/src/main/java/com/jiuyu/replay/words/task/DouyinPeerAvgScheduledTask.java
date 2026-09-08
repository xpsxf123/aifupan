package com.jiuyu.replay.words.task;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.words.bll.DouyinPeerAvgBll;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 抖音同行直播基础数据平均值 — XXL-Job 定时任务
 *
 * @author jy
 * @date 2026-06-24
 */
@Component
@Slf4j
public class DouyinPeerAvgScheduledTask {

    @Resource
    private DouyinPeerAvgBll douyinPeerAvgBll;

    /**
     * 预计算同行平均值。
     * XXL-Job 参数：计算过去 N 个完整天的数据（如 "7"），兜底 30 天。
     */
    @XxlJob("computeDouyinPeerAvg")
    public void computeDouyinPeerAvg() {
        String param = com.xxl.job.core.context.XxlJobHelper.getJobParam();
        int days = 30;
        if (StrUtil.isNotBlank(param)) {
            try {
                days = Integer.parseInt(param.trim());
            } catch (NumberFormatException e) {
                log.warn("computeDouyinPeerAvg 参数解析失败: {}, 使用兜底 30 天", param);
            }
        }
        log.info("computeDouyinPeerAvg 开始，计算过去 {} 个完整天", days);
        douyinPeerAvgBll.computeAndSavePeerAvg(days);
        log.info("computeDouyinPeerAvg 完成");
    }
}
