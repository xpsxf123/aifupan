package com.jiuyu.replay.third.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.third.entity.AsrEngineConfigEntity;

import java.util.List;

/**
 * ASR 引擎按租户 / 用户 / 语言优先级配置 Service。
 *
 * @author hehh
 * @date 2026-05-25
 */
public interface AsrEngineConfigService extends IService<AsrEngineConfigEntity> {

    /**
     * 解析当前用户可用的 ASR 引擎列表。
     *
     * <p>优先级匹配（高 → 低）：</p>
     * <ol>
     *   <li>{@code (tenantId, userId, language)}</li>
     *   <li>{@code (tenantId, userId, '')}</li>
     *   <li>{@code (tenantId, 0, language)}</li>
     *   <li>{@code (tenantId, 0, '')}</li>
     * </ol>
     *
     * <p>任一档命中即返回其 {@code engines} 解析后的有序列表；
     * 全部未命中时由调用方兜底（本方法返回空列表）。</p>
     *
     * @param tenantId 租户 ID（必填）
     * @param userId   用户 ID（必填）
     * @param language 语言编码（可为 {@code null}，按空串处理）
     * @return 命中的引擎列表（保留顺序，去重去空）；未命中返回空列表
     */
    List<String> resolveEngines(Long tenantId, Long userId, String language);
}
