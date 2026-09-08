package com.jiuyu.governance.business.performance.task;

import com.jiuyu.governance.business.performance.pojo.bo.PendingVideoGroup;
import com.jiuyu.governance.business.performance.pojo.bo.VideoProcessContext;
import com.jiuyu.governance.business.performance.pojo.entity.LiveVideo;
import com.jiuyu.governance.business.performance.service.LiveSessionService;
import com.jiuyu.governance.business.performance.service.LiveVideoService;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 视频处理定时任务
 *
 * @author ：lujie
 * @date ：2026/3/20 16:23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoTask {

    private static final String VIDEO_TASK_LOCK_KEY = "governance:task:video:lock";
    /** 锁过期时间（10分钟） */
    private static final long LOCK_EXPIRE_MINUTES = 10;
    /** 处理超时时间（30分钟） */
    private static final int PROCESS_TIMEOUT_MINUTES = 30;
    /** 每次处理的分组数量 */
    private static final int BATCH_GROUP_LIMIT = 10;

    private final RedisTemplate<String, String> redisTemplate;
    private final LiveVideoService liveVideoService;
    private final LiveSessionService liveSessionService;

    /**
     * 视频处理任务
     * 在调度中心新建任务时，JobHandler 填写 "videoJobHandler"
     */
    @XxlJob("clientPerformanceJobHandler")
    public void handleVideo() {
        // 获取调度中心传入的参数
        String param = XxlJobHelper.getJobParam();
        XxlJobHelper.log("【视频处理任务】开始执行，参数: {}", param);

        String lockValue = UUID.randomUUID().toString();
        
        try {
            // 1. 判断是否已经在执行，使用redis来做分布式锁
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    VIDEO_TASK_LOCK_KEY, 
                    lockValue, 
                    LOCK_EXPIRE_MINUTES, 
                    TimeUnit.MINUTES
            );
            
            if (Boolean.FALSE.equals(acquired)) {
                log.info("【视频处理任务】已有任务在执行中，跳过本次执行");
                XxlJobHelper.log("【视频处理任务】已有任务在执行中，跳过本次执行");
                XxlJobHelper.handleSuccess("跳过执行：已有任务在运行");
                return;
            }

            try {
                log.info("【视频处理任务】获取锁成功，开始执行...");
                XxlJobHelper.log("【视频处理任务】获取锁成功，开始执行...");

                // 2. 先把视频(live_video)中处理中且超时的视频，重新设置成待处理
                boolean resetResult = liveVideoService.resetTimeoutProcessingVideos(PROCESS_TIMEOUT_MINUTES);
                XxlJobHelper.log("【视频处理任务】重置超时视频：{}", resetResult ? "成功" : "无超时数据");

                // 3. 处理视频
                processVideos();

                XxlJobHelper.log("【视频处理任务】执行成功");
                XxlJobHelper.handleSuccess("执行成功");
            } finally {
                // 安全释放锁
                releaseLockSafely(lockValue);
            }
        } catch (Exception e) {
            log.error("【视频处理任务】执行异常", e);
            XxlJobHelper.log("【视频处理任务】执行异常: {}", e.getMessage());
            XxlJobHelper.handleFail("执行失败: " + e.getMessage());
        }
    }

    /**
     * 安全释放锁（Lua脚本保证原子性）
     * 只有锁的值与传入的lockValue一致时才删除，避免误删其他实例的锁
     *
     * @param lockValue 锁的值
     */
    private void releaseLockSafely(String lockValue) {
        String script = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;
        try {
            redisTemplate.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(VIDEO_TASK_LOCK_KEY),
                    lockValue
            );
            log.info("【视频处理任务】释放锁");
        } catch (Exception e) {
            log.warn("【视频处理任务】释放锁异常", e);
        }
    }

    /**
     * 处理视频核心逻辑
     */
    private void processVideos() {
        // 3.1 获取待处理视频的batchNumber，tenantId，返回list
        List<PendingVideoGroup> pendingGroups = liveVideoService.getPendingVideoGroups(BATCH_GROUP_LIMIT);
        
        if (pendingGroups.isEmpty()) {
            XxlJobHelper.log("【视频处理任务】无待处理视频");
            log.info("【视频处理任务】无待处理视频");
            return;
        }

        XxlJobHelper.log("【视频处理任务】获取待处理分组数: {}", pendingGroups.size());
        log.info("【视频处理任务】获取待处理分组数: {}", pendingGroups.size());

        // 遍历每个分组进行处理
        for (PendingVideoGroup group : pendingGroups) {
            processVideoGroup(group);
        }
    }

    /**
     * 处理单个视频分组（统一处理）
     *
     * @param group 视频分组信息
     */
    private void processVideoGroup(PendingVideoGroup group) {
        Long tenantId = group.getTenantId();
        String batchNumber = group.getBatchNumber();
        List<Long> videoIds = null;
        String failReason = null;
        boolean success = false;

        try {
            // 3.2 根据batchNumber和tenantId把视频中状态为待处理的视频，统一设置成处理中
            List<LiveVideo> videos = liveVideoService.fetchAndLockPendingVideos(tenantId, batchNumber);
            
            if (videos.isEmpty()) {
                return;
            }

            // 提取视频ID列表
            videoIds = videos.stream()
                    .map(LiveVideo::getId)
                    .collect(Collectors.toList());

            XxlJobHelper.log("【视频处理任务】处理分组 tenantId={}, batchNumber={}, 视频数={}", tenantId, batchNumber, videos.size());
            log.info("【视频处理任务】处理分组 tenantId={}, batchNumber={}, 视频数={}", tenantId, batchNumber, videos.size());

            // 3.3 统一处理视频
            doProcessVideos(videos);

            // 执行到这里说明没有报错，标记成功
            success = true;

        } catch (Exception e) {
            failReason = e.getMessage();
            log.error("【视频处理任务】处理分组异常, tenantId={}, batchNumber={}", tenantId, batchNumber, e);
            XxlJobHelper.log("【视频处理任务】处理分组异常, tenantId={}, batchNumber={}, error={}", tenantId, batchNumber, e.getMessage());
        } finally {
            // 3.4 统一更新处理结果
            if (videoIds != null && !videoIds.isEmpty()) {
                if (success) {
                    liveVideoService.batchUpdateProcessSuccess(videoIds);
                    XxlJobHelper.log("【视频处理任务】分组处理成功, tenantId={}, batchNumber={}, count={}", tenantId, batchNumber, videoIds.size());
                } else {
                    liveVideoService.batchUpdateProcessFailed(videoIds, failReason);
                    XxlJobHelper.log("【视频处理任务】分组处理失败, tenantId={}, batchNumber={}, reason={}", tenantId, batchNumber, failReason);
                }
            }
        }
    }

    /**
     * 统一处理视频
     * <p>
     * 1. 生成场次数据（live_session），把所有视频的业绩数据合并成一场次的业绩数据
     * 2. 查询租户对应的场次是否已经存在
     * 3. 如果不存在，则创建新的场次
     * 4. 如果存在且来源是系统推送，则更新场次数据
     * 5. 如果存在且来源是手动录入，则不更新业绩数据，记录到session_original_value
     * 6. 合并视频OSS数据并上传
     * </p>
     *
     * @param videos 视频列表
     */
    private void doProcessVideos(List<LiveVideo> videos) {

        if (videos == null || videos.isEmpty()) {
            return;
        }

        // 创建上下文
        VideoProcessContext ctx = VideoProcessContext.of(videos);

        // ====== 阶段1：事务外准备数据（网络 I/O） ======
        // 1.1 查询直播间信息
        LiveRoom liveRoom = liveSessionService.findLiveRoom(ctx.getTenantId(), videos.get(0).getSecUid());
        if (liveRoom == null){
            throw new BusinessException("直播间不存在");
        }
        ctx.setLiveRoom(liveRoom);

        // 1.2 加载视频 OSS 过程数据（耗时操作，放在事务外），结果填充至 ctx.processData & ctx.ossUrl
        liveSessionService.loadVideoOssData(ctx);

        // ====== 阶段2：事务内保存场次和商品 ======
        liveSessionService.saveInTransaction(ctx);

        // ====== 阶段3：事务内计算业绩 ======
        liveSessionService.calculatePerformanceInTransaction(ctx);
    }
}
