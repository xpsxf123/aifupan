package com.jiuyu.replay.words.task;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.lock.DistributedLock;
import com.jiuyu.replay.common.tencent.TencentVodUtils;
import com.jiuyu.replay.words.entity.AnchorVideoRecycleEntity;
import com.jiuyu.replay.words.repository.dao.AnchorVideoRecycleDao;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * VOD 回收站兜底清理定时任务。
 * <p>根据 XXL-Job 参数 N（天数），扫描 tb_anchor_video_recycle 中 vod_deleted=0 且
 * delete_time &lt;= now-N 的记录，调用腾讯云 VOD 物理删除接口，成功后标记 vod_deleted=1。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VodRecycleCleanupTask {

    private final AnchorVideoRecycleDao anchorVideoRecycleDao;
    private final TencentVodUtils tencentVodUtils;
    private final DistributedLock distributedLock;

    private static final int BATCH_SIZE = 100;
    private static final int DEFAULT_DAYS = 30;

    @XxlJob("cleanupVodRecycle")
    public void cleanupVodRecycle() {
        distributedLock.executeWithLock("task:vodRecycleCleanup", () -> {
            String param = XxlJobHelper.getJobParam();
            int days = StrUtil.isBlank(param) ? DEFAULT_DAYS : Integer.parseInt(param.trim());
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);

            log.info("[VOD回收站清理] 开始执行，删除 {} 天前（{}）的记录", days, cutoff);
            XxlJobHelper.log("[VOD回收站清理] 开始执行，删除 {} 天前（{}）的记录", days, cutoff);

            int batchNo = 0;
            int totalProcessed = 0;
            int totalDeleted = 0;
            int totalSkipped = 0;
            int totalFailed = 0;

            while (true) {
                LambdaQueryWrapper<AnchorVideoRecycleEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AnchorVideoRecycleEntity::getVodDeleted, 0)
                        .le(AnchorVideoRecycleEntity::getDeleteTime, cutoff)
                        .orderByAsc(AnchorVideoRecycleEntity::getId);

                Page<AnchorVideoRecycleEntity> page =
                        anchorVideoRecycleDao.selectPage(new Page<>(1, BATCH_SIZE), wrapper);
                java.util.List<AnchorVideoRecycleEntity> records = page.getRecords();

                if (ObjectUtil.isEmpty(records)) {
                    break;
                }

                batchNo++;
                log.info("[VOD回收站清理] 第{}批次，查询到{}条记录", batchNo, records.size());

                for (AnchorVideoRecycleEntity record : records) {
                    totalProcessed++;

                    if (StrUtil.isBlank(record.getFileId())) {
                        log.warn("[VOD回收站清理] 记录 id={} fileId 为空，跳过", record.getId());
                        totalSkipped++;
                        continue;
                    }

                    String result;
                    try {
                        result = tencentVodUtils.deleteFile(record.getFileId());
                    } catch (Exception e) {
                        log.error("[VOD回收站清理] 调用 VOD 删除异常，id={}, fileId={}",
                                record.getId(), record.getFileId(), e);
                        totalFailed++;
                        continue;
                    }

                    if ("success".equals(result)) {
                        LambdaUpdateWrapper<AnchorVideoRecycleEntity> updateWrapper =
                                new LambdaUpdateWrapper<>();
                        updateWrapper.eq(AnchorVideoRecycleEntity::getId, record.getId())
                                .set(AnchorVideoRecycleEntity::getVodDeleted, 1);

                        anchorVideoRecycleDao.update(null, updateWrapper);
                        totalDeleted++;
                        log.debug("[VOD回收站清理] 成功删除，id={}, fileId={}",
                                record.getId(), record.getFileId());
                    } else {
                        log.error("[VOD回收站清理] VOD 删除返回失败，id={}, fileId={}, result={}",
                                record.getId(), record.getFileId(), result);
                        totalFailed++;
                    }
                }

                if (records.size() < BATCH_SIZE) {
                    break;
                }
            }

            log.info("[VOD回收站清理] 执行完成。总计处理:{} 成功:{} 跳过:{} 失败:{}",
                    totalProcessed, totalDeleted, totalSkipped, totalFailed);
            XxlJobHelper.log("[VOD回收站清理] 执行完成。总计处理:{} 成功:{} 跳过:{} 失败:{}",
                    totalProcessed, totalDeleted, totalSkipped, totalFailed);
        });
    }
}
