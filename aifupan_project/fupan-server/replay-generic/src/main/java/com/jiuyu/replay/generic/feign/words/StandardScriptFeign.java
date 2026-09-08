package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;

/**
 * 标准直播稿跨模块查询 SPI（replay-generic 声明，replay-words 实现）。
 *
 * <p>replay-ai 通过本接口查询标准稿，禁止跨模块直连 replay-words 的 Dao / Service。</p>
 *
 * @author beta
 * @date 2026-06-12
 */
public interface StandardScriptFeign {

    /**
     * 按 (tenantId, userId, secUid) 查询有效（is_deleted=0）标准直播稿。
     *
     * @param tenantId 租户 ID（租户隔离必传）
     * @param userId   用户 ID
     * @param secUid   主播唯一标识
     * @return 有效标准稿 VO；不存在或已删除返回 null
     */
    StandardScriptInfoVo findValid(Long tenantId, Long userId, String secUid);
}
