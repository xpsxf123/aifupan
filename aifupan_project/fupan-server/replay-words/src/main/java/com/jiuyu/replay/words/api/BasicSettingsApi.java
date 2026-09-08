package com.jiuyu.replay.words.api;

import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.generic.feign.words.BasicSettingsFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.BasicSettingsVo;
import com.jiuyu.replay.words.producer.BasicSettingsProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 基础设置 Feign 实现
 *
 * <p>{@link BasicSettingsFeign} SPI 的 replay-words 端实现，
 * 直接复用 {@link BasicSettingsProducer#getBySourceUser} 既有查询通道。</p>
 *
 * @author beta
 * @date 2026-06-30
 */
@Service
@RequiredArgsConstructor
public class BasicSettingsApi implements BasicSettingsFeign {

    private final BasicSettingsProducer basicSettingsProducer;

    /**
     * 按主播三元组查询 tb_basic_settings 中 sourceType=ANCHOR 的记录。
     *
     * @param secUid   主播唯一标识
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return BasicSettingsVo；不存在返 R.ok(null)
     */
    @Override
    public R<BasicSettingsVo> getByAnchor(String secUid, Long userId, Long tenantId) {
        BasicSettingsVo vo = basicSettingsProducer.getBySourceUser(
                secUid, WordsEnum.basicSettingsType.ANCHOR.getCode(), userId, tenantId);
        return R.ok(vo);
    }
}
