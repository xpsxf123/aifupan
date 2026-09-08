package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;

/**
 * 基础设置（tb_basic_settings）跨模块 Feign 接口
 *
 * <p>供 replay-ai 等下游模块按主播三元组（secUid + userId + tenantId）读取
 * BasicSettingsBaseDto 12 字段（含 livingMode / accountType / livingTarget 等），
 * 用于在 AnchorUrlUserVo 等合并视图 VO 中补全继承字段。</p>
 *
 * @author beta
 * @date 2026-06-30
 */
public interface BasicSettingsFeign {

    /**
     * 按主播三元组查询 tb_basic_settings 中 sourceType=ANCHOR 的记录。
     *
     * @param secUid   主播唯一标识（对应 tb_basic_settings.source_id）
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return BasicSettingsVo（含 BasicSettingsBaseDto 12 字段）；不存在返 R.ok(null)
     */
    R<BasicSettingsVo> getByAnchor(String secUid, Long userId, Long tenantId);
}
