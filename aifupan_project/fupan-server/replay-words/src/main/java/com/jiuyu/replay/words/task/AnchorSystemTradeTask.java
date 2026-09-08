package com.jiuyu.replay.words.task;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnchorSystemTradeTask {

    private final AnchorUrlService anchorUrlService;

    /**
     * 将AI纠正行业设置为系统行业定时任务
     * 获取所有有AI纠正行业id但是没有系统行业，且添加AI纠正行业时间距今已超过7天的主播
     * 将AI纠正行业id设置到系统行业
     */
    private static final int BATCH_SIZE = 1000;

    @XxlJob("syncAiTradeToSystemTrade")
    public void syncAiTradeToSystemTrade() {
        log.info("【定时任务】开始执行AI纠正行业同步到系统行业任务");

        // 计算7天前的时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = calendar.getTime();

        int batchNo = 0;
        int totalUpdated = 0;
        Date now = new Date();

        while (true) {
            // 分批查询符合条件的主播：有AI纠正行业id、没有系统行业id、添加AI纠正行业时间超过7天
            // 因为每次更新后systemTradeId不再为null，已处理的记录不会再被查出，所以始终查第1页
            LambdaQueryWrapper<AnchorUrlEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.isNotNull(AnchorUrlEntity::getAiCorrectTradeId)
                    .isNull(AnchorUrlEntity::getSystemTradeId)
                    .isNotNull(AnchorUrlEntity::getAddAiTradeDate)
                    .le(AnchorUrlEntity::getAddAiTradeDate, sevenDaysAgo);

            Page<AnchorUrlEntity> page = anchorUrlService.page(new Page<>(1, BATCH_SIZE), wrapper);
            List<AnchorUrlEntity> anchorList = page.getRecords();

            if (ObjectUtil.isEmpty(anchorList)) {
                break;
            }

            batchNo++;
            log.info("【定时任务】第{}批次，查询到{}条数据", batchNo, anchorList.size());

            for (AnchorUrlEntity anchor : anchorList) {
                // 将AI纠正行业id设置为系统行业id
                anchor.setSystemTradeId(anchor.getAiCorrectTradeId());
                anchor.setUpdateSystemTradeDate(now);
                anchor.setUpdateDate(now);
            }

            // 批量更新
            boolean result = anchorUrlService.updateBatchById(anchorList);
            if (result) {
                totalUpdated += anchorList.size();
                log.info("【定时任务】第{}批次，成功更新{}条", batchNo, anchorList.size());
            } else {
                log.error("【定时任务】第{}批次更新失败", batchNo);
                break;
            }

            // 已经是最后一页，退出循环
            if (anchorList.size() < BATCH_SIZE) {
                break;
            }
        }

        if (totalUpdated == 0) {
            log.info("【定时任务】没有符合条件的主播需要处理");
        } else {
            log.info("【定时任务】执行完成，共更新{}个主播的系统行业", totalUpdated);
        }
    }

}
