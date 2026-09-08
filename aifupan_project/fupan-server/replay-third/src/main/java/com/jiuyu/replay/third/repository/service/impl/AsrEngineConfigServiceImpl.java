package com.jiuyu.replay.third.repository.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.third.entity.AsrEngineConfigEntity;
import com.jiuyu.replay.third.repository.dao.AsrEngineConfigDao;
import com.jiuyu.replay.third.repository.service.AsrEngineConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * ASR 引擎按租户 / 用户 / 语言优先级配置 Service 实现。
 *
 * @author hehh
 * @date 2026-05-25
 */
@Service("asrEngineConfigService")
public class AsrEngineConfigServiceImpl
        extends ServiceImpl<AsrEngineConfigDao, AsrEngineConfigEntity>
        implements AsrEngineConfigService {

    /**
     * 哨兵值：用户 ID = 0 表示租户级默认配置。
     */
    private static final Long USER_ID_TENANT_DEFAULT = 0L;

    /**
     * 哨兵值：language 为空串表示不限语言。
     */
    private static final String LANGUAGE_ANY = "";

    @Override
    public List<String> resolveEngines(Long tenantId, Long userId, String language) {
        if (tenantId == null || userId == null) {
            return Collections.emptyList();
        }
        String lang = language == null ? LANGUAGE_ANY : language;

        // 一次查询取出 4 档候选；都在唯一键 uk_tenant_user_lang 上命中
        List<AsrEngineConfigEntity> rows = lambdaQuery()
                .eq(AsrEngineConfigEntity::getTenantId, tenantId)
                .eq(AsrEngineConfigEntity::getIsDeleted, 0)
                .and(w -> w
                        .or(c -> c.eq(AsrEngineConfigEntity::getUserId, userId)
                                .eq(AsrEngineConfigEntity::getLanguage, lang))
                        .or(c -> c.eq(AsrEngineConfigEntity::getUserId, userId)
                                .eq(AsrEngineConfigEntity::getLanguage, LANGUAGE_ANY))
                        .or(c -> c.eq(AsrEngineConfigEntity::getUserId, USER_ID_TENANT_DEFAULT)
                                .eq(AsrEngineConfigEntity::getLanguage, lang))
                        .or(c -> c.eq(AsrEngineConfigEntity::getUserId, USER_ID_TENANT_DEFAULT)
                                .eq(AsrEngineConfigEntity::getLanguage, LANGUAGE_ANY)))
                .list();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        AsrEngineConfigEntity hit = pickByPriority(rows, userId, lang);
        return hit == null ? Collections.emptyList() : parseEngines(hit.getEngines());
    }

    /**
     * 按优先级从候选行中选第一档命中：
     * userId+lang &gt; userId &gt; tenant+lang &gt; tenant。
     */
    private AsrEngineConfigEntity pickByPriority(List<AsrEngineConfigEntity> rows,
                                                 Long userId, String lang) {
        AsrEngineConfigEntity userExact = null;
        AsrEngineConfigEntity userAny = null;
        AsrEngineConfigEntity tenantExact = null;
        AsrEngineConfigEntity tenantAny = null;
        for (AsrEngineConfigEntity row : rows) {
            boolean isUserRow = Objects.equals(row.getUserId(), userId);
            boolean isLangExact = Objects.equals(row.getLanguage(), lang);
            boolean isLangAny = LANGUAGE_ANY.equals(row.getLanguage());
            if (isUserRow && isLangExact) {
                userExact = row;
            } else if (isUserRow && isLangAny) {
                userAny = row;
            } else if (USER_ID_TENANT_DEFAULT.equals(row.getUserId()) && isLangExact) {
                tenantExact = row;
            } else if (USER_ID_TENANT_DEFAULT.equals(row.getUserId()) && isLangAny) {
                tenantAny = row;
            }
        }
        if (userExact != null) {
            return userExact;
        }
        if (userAny != null) {
            return userAny;
        }
        if (tenantExact != null) {
            return tenantExact;
        }
        return tenantAny;
    }

    /**
     * 解析逗号分隔的引擎串；保留顺序、去重、去空。
     */
    private List<String> parseEngines(String engines) {
        if (StrUtil.isBlank(engines)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String part : Arrays.asList(engines.split(","))) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return new ArrayList<>(set);
    }
}
