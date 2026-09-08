package com.jiuyu.replay.system.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.system.entity.SeoSlugHistoryEntity;

/**
 * SEO slug 变更历史，供官网旧地址 301。
 *
 * @author claude
 * @date 2026-08-18
 */
public interface SeoSlugHistoryService extends IService<SeoSlugHistoryEntity> {

    /**
     * 记录一次 slug 变更。
     *
     * <p>只在「保存时检测到 slug 真的变了」的路径上调用。
     * <b>逻辑删除时不要调用</b>——删除后旧地址应当 404 而不是 301，内容确实没了。
     *
     * <p>只记旧值，新值永远从实体表现查：这样多次改名（x→y→z）时，
     * 查 x 和查 y 都一步跳到最终的 z，不产生 301 跳转链（跳转链的权重传递有衰减）。
     *
     * <p>反复改名（x→y→x→y）会撞 {@code uk_type_old_slug} 唯一键，
     * 此时更新已有记录的 entityId 为最新归属，而不是插入新行。
     *
     * @param entityType 对象类型，取 {@code SeoConstant.SLUG_ENTITY_*}
     * @param entityId   实体主键
     * @param oldSlug    被替换掉的旧 slug；为空则不记录
     */
    void record(int entityType, Long entityId, String oldSlug);

    /**
     * 按旧 slug 反查实体 ID。
     *
     * <p><b>调用方必须先查当前表、查不到再调本方法</b>，顺序不能反。该顺序自动处理三个边界：
     * <ul>
     *   <li>slug 改回原值（x→y→x）：查 x 命中当前表，历史表那条永不生效</li>
     *   <li>A 改 x→y 后新实体 B 占用了 x：查 x 命中当前表的 B，不会错误 301 到 A</li>
     *   <li>A 改 x→y 后被删除：命中历史表 → 回查实体 → 已删除 → 由调用方返回 404</li>
     * </ul>
     *
     * @param entityType 对象类型
     * @param oldSlug    旧 slug
     * @return 实体 ID；无记录时返回 null。<b>返回非 null 不代表实体仍存在</b>，
     *         调用方须再查一次实体并判断是否已删除/已下架
     */
    Long resolveEntityId(int entityType, String oldSlug);
}
