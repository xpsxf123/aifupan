package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.system.entity.SeoSlugHistoryEntity;
import com.jiuyu.replay.system.repository.dao.SeoSlugHistoryDao;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SEO slug 变更历史。
 *
 * @author claude
 * @date 2026-08-18
 */
@Service
public class SeoSlugHistoryServiceImpl
        extends ServiceImpl<SeoSlugHistoryDao, SeoSlugHistoryEntity>
        implements SeoSlugHistoryService {

    @Override
    public void record(int entityType, Long entityId, String oldSlug) {
        if (entityId == null || oldSlug == null || oldSlug.isBlank()) {
            return;
        }

        // 先查再写而不是直接 insert 靠唯一键兜底：反复改名（x→y→x→y）是正常运营行为，
        // 让它每次都抛 DuplicateKeyException 再捕获，会把正常路径写成异常路径，
        // 日志里也会出现一堆看着像故障的堆栈。
        //
        // 并发下两个人同时改到同一个 old_slug 仍可能撞唯一键——那是 slug 变更这种
        // 低频运营操作里可以接受的概率，且唯一键会让其中一个事务失败回滚，不会写脏。
        SeoSlugHistoryEntity existing = getOne(
                new LambdaQueryWrapper<SeoSlugHistoryEntity>()
                        .eq(SeoSlugHistoryEntity::getEntityType, entityType)
                        .eq(SeoSlugHistoryEntity::getOldSlug, oldSlug)
                        .last("LIMIT 1"));

        if (existing != null) {
            // 旧 slug 被另一个实体重新用过又改走：归属更新为最新的那个
            update(new LambdaUpdateWrapper<SeoSlugHistoryEntity>()
                    .eq(SeoSlugHistoryEntity::getId, existing.getId())
                    .set(SeoSlugHistoryEntity::getEntityId, entityId)
                    .set(SeoSlugHistoryEntity::getCreateDate, LocalDateTime.now()));
            return;
        }

        SeoSlugHistoryEntity entity = new SeoSlugHistoryEntity();
        entity.setId(SnowflakeManager.nextValue());
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setOldSlug(oldSlug);
        entity.setCreateDate(LocalDateTime.now());
        save(entity);
    }

    @Override
    public Long resolveEntityId(int entityType, String oldSlug) {
        if (oldSlug == null || oldSlug.isBlank()) {
            return null;
        }
        SeoSlugHistoryEntity entity = getOne(
                new LambdaQueryWrapper<SeoSlugHistoryEntity>()
                        .select(SeoSlugHistoryEntity::getEntityId)
                        .eq(SeoSlugHistoryEntity::getEntityType, entityType)
                        .eq(SeoSlugHistoryEntity::getOldSlug, oldSlug)
                        .last("LIMIT 1"));
        return entity == null ? null : entity.getEntityId();
    }
}
