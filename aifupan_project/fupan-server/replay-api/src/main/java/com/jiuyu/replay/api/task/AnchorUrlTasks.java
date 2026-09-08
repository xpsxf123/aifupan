package com.jiuyu.replay.api.task;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.words.AnchorUrlLogic;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.words.bll.AnchorCruxWordsBll;
import com.jiuyu.replay.words.vo.AnchorVideoSimpleVo;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * &#064;description：主播相关的task
 * @date ：2025/11/22 10:57
 */
@Component
@AllArgsConstructor
@Slf4j
public class AnchorUrlTasks {

    private final RedisTemplate<String, String> redisTemplate;
    private final AnchorCruxWordsBll anchorCruxWordsBll;
    private final AnchorUrlLogic anchorUrlLogic;

    /**
     * 定时处理主播关键词
     */
    @XxlJob("timingHandleAnchorKeywords")
    public void timingHandleAnchorKeywords() {
        new Thread(() -> {
            try {
                log.info("[定时处理主播关键词] 成功获取锁，后台线程开始执行任务...");
                CommonUtils.measureTime(this::startHandleKeywordsJob, "[定时处理主播关键词] 后台任务执行完成");
            } catch (Exception e) {
                log.error("[定时处理主播关键词] 后台任务异常", e);
            }
        }, "anchorKeyword-job-thread").start();

    }

    private void startHandleKeywordsJob() {
        ExecutorService executor = null;
        try {
            int threadCount = 10;

            executor = Executors.newFixedThreadPool(threadCount);
            log.info("[后台任务-定时处理主播关键词] 线程池创建成功: {}", threadCount);

            List<AnchorVideoSimpleVo> anchorVideoSimpleVos = anchorCruxWordsBll.listRecentAnchorWithoutKeyword(3000);

            log.info("[后台任务-定时处理主播关键词] 查询到 {} 条待处理 secUid", anchorVideoSimpleVos.size());

            CountDownLatch latch = new CountDownLatch(anchorVideoSimpleVos.size());

            for (AnchorVideoSimpleVo simpleVo : anchorVideoSimpleVos) {
                executor.submit(() -> {
                    try {
                        List<String> keywords = anchorUrlLogic.startGetAnchorKeywords(simpleVo.getSecUid(), simpleVo.getVideoId());
                        if (ObjectUtil.isNotEmpty(keywords)) {
                            anchorCruxWordsBll.saveAnchorKeywords(simpleVo.getSecUid(), keywords, null, null);
                        }
                    } catch (Exception e) {
                        log.error("[后台任务-定时处理主播关键词] 处理 secUid {} 异常", simpleVo, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待本批次完成
            latch.await(6, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("[后台任务-定时处理主播关键词] 出现异常", e);
        } finally {
            if (executor != null) {
                executor.shutdown();
                try {
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
