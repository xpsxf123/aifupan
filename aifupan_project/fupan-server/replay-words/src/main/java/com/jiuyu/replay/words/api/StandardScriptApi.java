package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 标准直播稿 SPI 实现（薄包装层）。
 *
 * <p>实现 {@link StandardScriptFeign}，将 replay-words 内部 {@link StandardScriptService#findValid}
 * 的查询结果映射为跨模块 VO，避免 Entity 直接跨模块传输。</p>
 *
 * @author beta
 * @date 2026-06-12
 */
@Component
@AllArgsConstructor
public class StandardScriptApi implements StandardScriptFeign {

    private final StandardScriptService standardScriptService;

    /**
     * 按 (tenantId, userId, secUid) 查询有效标准稿，映射为 {@link StandardScriptInfoVo}。
     *
     * @param tenantId 租户 ID（租户隔离必传）
     * @param userId   用户 ID
     * @param secUid   主播唯一标识
     * @return 有效标准稿 VO；不存在返回 null
     */
    @Override
    public StandardScriptInfoVo findValid(Long tenantId, Long userId, String secUid) {
        StandardScriptEntity entity = standardScriptService.findValid(tenantId, userId, secUid);
        if (entity == null) {
            return null;
        }
        StandardScriptInfoVo vo = new StandardScriptInfoVo();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
