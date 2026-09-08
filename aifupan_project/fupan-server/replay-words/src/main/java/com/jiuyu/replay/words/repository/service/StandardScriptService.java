package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.StandardScriptEntity;

/**
 * 标准直播稿 Service SPI。
 *
 * <p>提供标准稿的 CRUD 原子操作，供 ScriptMonitorStandardScriptBll 和 AnchorUrlBll
 * 调用，避免 Bll-to-Bll 互调。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
public interface StandardScriptService extends IService<StandardScriptEntity> {

    /**
     * 按 (tenantId, userId, secUid) 查找有效（is_deleted=0）标准稿。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param secUid   主播唯一标识
     * @return 有效标准稿 entity；不存在返回 null
     */
    StandardScriptEntity findValid(Long tenantId, Long userId, String secUid);

    /**
     * 软删除标准稿（is_deleted=1）。
     *
     * <p>2026-06-17 起 confirmStandardScript 改为 UPDATE 模式（同一 (tenant, user, secuid)
     * 永远只一行），本方法当前无活跃调用方，保留为通用 Service 能力供未来下线/归档场景使用。</p>
     *
     * @param id 标准稿 ID
     */
    void softDelete(Long id);

    /**
     * 按 (tenantId, userId, secUid) 查找对应的 AnchorUrlUser 并校验 accountType。
     *
     * <p>查到 accountType=1（竞品/同行）时抛 BusinessException(70004)；
     * 查不到（新增直播间场景）时跳过校验，允许继续。</p>
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @param secUid   主播唯一标识
     * @return AnchorUrlUserEntity 或 null（新增直播间场景）
     */
    AnchorUrlUserEntity validateAccountAndLoadAnchor(Long tenantId, Long userId, String secUid);
}
