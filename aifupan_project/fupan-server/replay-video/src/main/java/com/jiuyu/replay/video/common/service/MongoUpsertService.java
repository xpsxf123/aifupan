package com.jiuyu.replay.video.common.service;

import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.video.project.document.VideoContentExtract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * MongoDB Upsert 公共服务
 *
 * @author RayChou
 * @date 2025-08-14
 * @description 提供MongoDB的Upsert操作公共方法，支持各种文档类型的原子性保存或更新
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoUpsertService {

    private final MongoTemplate mongoTemplate;

    /**
     * 视频文案内容的Upsert操作
     *
     * @param videoHash    视频hash值（唯一标识）
     * @param audioContent 文案内容
     * @return 保存或更新后的文案记录
     */
    public VideoContentExtract upsertVideoContent(String videoHash, String audioContent) {
        return upsertVideoContent(videoHash, audioContent, null);
    }

    /**
     * 视频文案内容的Upsert操作（支持原文内容）
     *
     * @param videoHash            视频hash值（唯一标识）
     * @param audioContent         AI优化后的文案内容
     * @param originalAudioContent 原文文案内容（可选，为null时不更新）
     * @return 保存或更新后的文案记录
     */
    public VideoContentExtract upsertVideoContent(String videoHash, String audioContent, String originalAudioContent) {
        if (videoHash == null || videoHash.trim().isEmpty()) {
            throw new IllegalArgumentException("videoHash不能为空");
        }
        if (audioContent == null || audioContent.trim().isEmpty()) {
            throw new IllegalArgumentException("audioContent不能为空");
        }
        LocalDateTime operationTime = LocalDateTime.now();

        Query query = new Query(Criteria.where("video_hash").is(videoHash).and("is_deleted").is(0));

        Update update = new Update()
                .set("audio_content", audioContent)
                .set("update_date", operationTime);

        // 如果提供了原文内容，则保存（兼容老版本，可能为null）
        if (originalAudioContent != null && !originalAudioContent.trim().isEmpty()) {
            update.set("original_audio_content", originalAudioContent);
        }

        // setOnInsert: 只在插入新记录时设置这些字段
        update.setOnInsert("_id", String.valueOf(SnowflakeManager.nextValue()))
                .setOnInsert("created_date", operationTime)
                .setOnInsert("is_deleted", 0);

        // 使用findAndModify：执行upsert并返回结果文档（一次操作完成）
        FindAndModifyOptions options = new FindAndModifyOptions()
                .upsert(true)           // 启用upsert
                .returnNew(true);       // 返回更新后的文档

        VideoContentExtract result = mongoTemplate.findAndModify(query, update, options, VideoContentExtract.class);
        if (result == null) {
            throw new BusinessException("Upsert操作失败，video_hash: " + videoHash);
        }
        return result;
    }

    /**
     * 通用的Upsert操作
     *
     * @param query       查询条件
     * @param update      更新操作
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 保存或更新后的实体
     */
    public <T> T upsert(Query query, Update update, Class<T> entityClass) {
        return upsert(query, update, entityClass, true);
    }

    /**
     * 通用的Upsert操作（可选择是否返回新文档）
     *
     * @param query       查询条件
     * @param update      更新操作
     * @param entityClass 实体类
     * @param returnNew   是否返回更新后的文档
     * @param <T>         实体类型
     * @return 保存或更新后的实体
     */
    public <T> T upsert(Query query, Update update, Class<T> entityClass, boolean returnNew) {
        if (query == null) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
        if (update == null) {
            throw new IllegalArgumentException("更新操作不能为空");
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("实体类不能为空");
        }

        FindAndModifyOptions options = new FindAndModifyOptions()
                .upsert(true)
                .returnNew(returnNew);

        T result = mongoTemplate.findAndModify(query, update, options, entityClass);

        if (result == null) {
            throw new BusinessException("Upsert操作失败，entityClass: " + entityClass.getSimpleName());
        }

        return result;
    }

    /**
     * 批量Upsert操作（适用于大量数据）
     *
     * @param operations  Upsert操作列表
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return 操作结果统计
     */
    public <T> UpsertBatchResult batchUpsert(java.util.List<UpsertOperation> operations, Class<T> entityClass) {
        if (operations == null || operations.isEmpty()) {
            return new UpsertBatchResult(0, 0, 0);
        }

        int successCount = 0;
        int failureCount = 0;
        int totalCount = operations.size();

        for (UpsertOperation operation : operations) {
            try {
                upsert(operation.query(), operation.update(), entityClass, false);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("批量Upsert操作失败，operation: {}, error: {}", operation, e.getMessage());
            }
        }

        return new UpsertBatchResult(totalCount, successCount, failureCount);
    }

    /**
     * Upsert操作封装类
     */
    public record UpsertOperation(Query query, Update update) {
        @Override
        public String toString() {
            return "UpsertOperation{query=" + query + ", update=" + update + "}";
        }
    }

    /**
     * 批量Upsert结果
     */
    public record UpsertBatchResult(int totalCount, int successCount, int failureCount) {

        public boolean isAllSuccess() {
            return failureCount == 0;
        }

        public double getSuccessRate() {
            return totalCount == 0 ? 0.0 : (double) successCount / totalCount;
        }

        @Override
        public String toString() {
            return String.format("UpsertBatchResult{total=%d, success=%d, failure=%d, successRate=%.2f%%}",
                    totalCount, successCount, failureCount, getSuccessRate() * 100);
        }
    }
}
