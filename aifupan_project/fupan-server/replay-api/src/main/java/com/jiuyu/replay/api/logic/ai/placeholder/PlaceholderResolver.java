package com.jiuyu.replay.api.logic.ai.placeholder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.bll.AiPlaceholderBll;
import com.jiuyu.replay.ai.vo.AiPlaceholderVo;
import com.jiuyu.replay.words.bo.AskRequestBo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 占位符解析引擎 + Context 工厂。
 * 是占位符解析的唯一入口：extract → DB 配置查询 → 按优先级排序 resolve。
 *
 * @author jy
 * @date 2026-06-18
 */
@Component
@AllArgsConstructor
@Slf4j
public class PlaceholderResolver {

    private static final Pattern P = Pattern.compile("#\\{([^:}]+)(?::([^}]*))?\\}");

    private final AiPlaceholderBll aiPlaceholderBll;

    /**
     * 提取 prompt 中所有占位符
     */
    public List<PlaceholderToken> extract(String prompt) {
        if (StrUtil.isBlank(prompt)) return Collections.emptyList();
        List<PlaceholderToken> tokens = new ArrayList<>();
        Matcher m = P.matcher(prompt);
        while (m.find()) {
            tokens.add(new PlaceholderToken(
                    m.group(0),
                    m.group(1),
                    m.group(2),
                    m.start(),
                    m.end()
            ));
        }
        return tokens;
    }

    /**
     * 提取 prompt 中走上下文缓存的占位符 key（去重、排序）。
     * 以 PlaceholderEnum.systemPrompt 为准
     * 用于上下文缓存的 Redis key 计算和系统提示词构建。
     */
    public List<String> extractContextCacheKeys(String prompt) {
        List<PlaceholderToken> tokens = extract(prompt);
        if (CollUtil.isEmpty(tokens)) return Collections.emptyList();
        return tokens.stream()
                .map(PlaceholderToken::getKey)
                .distinct()
                .filter(key -> {
                    PlaceholderEnum pe = PlaceholderEnum.of(key);
                    if (pe == null) {
                        return false;
                    }
                    if (pe.getKey().equals(PlaceholderEnum.ANCHOR_DATA.getKey())) {
                        return false;
                    }
                    return pe.isSystemPrompt();
                })
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 替换 prompt 中所有占位符。
     * Context 由调用方创建并传入（复用，避免重复查 BLL）。
     * useModelWay 从 askRequestBo 读取：上下文缓存模式时跳过 systemPrompt=true 的占位符。
     */
    public String resolve(String prompt, AskRequestBo askRequestBo, PlaceholderContext ctx) {
        List<PlaceholderToken> tokens = extract(prompt);
        if (CollUtil.isEmpty(tokens)) return prompt;

        // 查 DB 配置（哪些 key 已配置且启用）
        List<String> fullKeys = tokens.stream()
                .map(PlaceholderToken::getFullKey).distinct().collect(Collectors.toList());
        Map<String, AiPlaceholderVo> configMap = aiPlaceholderBll.batchGetByFullKeys(fullKeys);

        // 上下文缓存模式：跳过已在系统提示词中缓存的数据
        boolean skipCached = askRequestBo.getUseModelWay() != null
                && askRequestBo.getUseModelWay() == 0;

        return resolveSorted(prompt, tokens, configMap, ctx, skipCached);
    }

    /**
     * 按优先级从低到高依次解析，保证基础数据（优先级0）先加载到 Context。
     * 相同 fullKey 的去重：第一次解析后缓存，后续直接替换。
     */
    private String resolveSorted(String prompt, List<PlaceholderToken> tokens,
                                 Map<String, AiPlaceholderVo> configMap,
                                 PlaceholderContext context, boolean skipCached) {
        List<PlaceholderToken> sorted = tokens.stream()
                .sorted(Comparator.comparingInt(t -> {
                    PlaceholderEnum pe = PlaceholderEnum.of(t.getKey());
                    return pe != null ? pe.getPriority() : Integer.MAX_VALUE;
                }))
                .collect(Collectors.toList());

        // 排序后位置变了，用 replace 逐个替换
        String result = prompt;
        Set<String> resolved = new HashSet<>();
        for (PlaceholderToken token : sorted) {
            if (resolved.contains(token.getFullKey())) continue;
            String value = resolveOne(token, configMap, context, skipCached);
            result = result.replace(token.getFullKey(), value);
            resolved.add(token.getFullKey());
        }
        return result;
    }

    /**
     * 解析单个占位符：枚举匹配 → DB 配置校验 → 上下文缓存跳过判断 → 枚举 resolve。
     * 未知 key / 未配置 / 解析异常时保留原文。
     */
    private String resolveOne(PlaceholderToken token,
                              Map<String, AiPlaceholderVo> configMap,
                              PlaceholderContext context, boolean skipCached) {
        // 1. 查枚举
        PlaceholderEnum pe = PlaceholderEnum.of(token.getKey());
        if (pe == null) {
            log.warn("【占位符】未知 key: {}，保留原文", token.getFullKey());
            return token.getFullKey();
        }
        // 2. 查 DB 配置
        AiPlaceholderVo config = configMap.get(token.getFullKey());
        if (config == null) {
            log.warn("【占位符】未配置/已禁用: {}，保留原文", token.getFullKey());
            return token.getFullKey();
        }
        if (config.getStatus() != null && config.getStatus() == 1) {
            return "";
        }
        // 3. 上下文缓存模式：数据已在系统提示词中 → 返回固定占位文字
        if (skipCached && pe.isSystemPrompt()) {
            return pe.getSystemPromptPlaceholder();
        }
        // 4. 枚举解析
        try {
            String value = pe.resolve(token.getParam(), context);
            if (value == null) {
                log.warn("【占位符】{} 尚未实现 resolve()，保留原文", token.getFullKey());
                return "无";
            }
            return value;
        } catch (Exception e) {
            log.error("【占位符】解析异常: {}", token.getFullKey(), e);
            return token.getFullKey();
        }
    }
}
