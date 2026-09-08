package com.jiuyu.replay.ai.bll;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.bo.AiPlaceholderBo;
import com.jiuyu.replay.ai.bo.AiPlaceholderListBo;
import com.jiuyu.replay.ai.rse.AiPlaceholderRse;
import com.jiuyu.replay.ai.vo.AiPlaceholderVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI占位符配置 Bll
 *
 * @author jy
 * @date 2026-06-16
 */
@Component
@AllArgsConstructor
public class AiPlaceholderBll {

    private final AiPlaceholderRse aiPlaceholderRse;

    /**
     * 分页查询占位符配置列表
     */
    public PageUtils<AiPlaceholderVo> queryPage(AiPlaceholderListBo listBo) {
        return aiPlaceholderRse.queryPage(listBo);
    }

    /** 按 ID 查单个占位符配置 */
    public AiPlaceholderVo info(Long id) {
        return aiPlaceholderRse.info(id);
    }

    /** 新增占位符配置，返回含 ID 的完整 VO */
    public AiPlaceholderVo save(AiPlaceholderBo bo) {
        return aiPlaceholderRse.save(bo);
    }

    /** 更新占位符配置 */
    public void update(AiPlaceholderBo bo) {
        aiPlaceholderRse.update(bo);
    }

    /** 按 ID 软删除占位符配置 */
    public void deleteById(Long id) {
        aiPlaceholderRse.deleteById(id);
    }

    /** 查全部已启用的占位符配置 */
    public List<AiPlaceholderVo> listAll() {
        return aiPlaceholderRse.listAll();
    }

    /** 客户端获取占位符配置列表 */
    public List<AiPlaceholderVo> listForClient() {
        return aiPlaceholderRse.listForClient();
    }

    /**
     * 按完整 key（如 "#{trade}"）查单个已启用配置
     */
    public AiPlaceholderVo getByKey(String fullKey) {
        return aiPlaceholderRse.getByKey(fullKey);
    }

    /**
     * 批量按完整 key 查询，两轮匹配：
     * 1. 精确匹配 placeholderKey
     * 2. 未命中 key 的 fallback：去掉参数部分（如 "#{uvTop:20}" → "#{uvTop}"），
     * 查 #{key} 格式的配置（当 #{key:param} 在 DB 中没有单独配置时，回退到 #{key} 的配置）
     */
    public Map<String, AiPlaceholderVo> batchGetByFullKeys(List<String> fullKeys) {
        if (CollUtil.isEmpty(fullKeys)) return Map.of();

        // 第一轮：精确匹配
        List<AiPlaceholderVo> list = aiPlaceholderRse.listByKeys(fullKeys);
        Map<String, AiPlaceholderVo> result = new HashMap<>();
        for (AiPlaceholderVo vo : list) {
            result.put(vo.getPlaceholderKey(), vo);
        }

        // 第二轮：未命中的 key 尝试 fallback 到 #{key}
        List<String> missed = fullKeys.stream()
                .filter(k -> !result.containsKey(k))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(missed)) {
            List<String> fallbackKeys = new ArrayList<>();
            Map<String, String> fallbackMap = new LinkedHashMap<>(); // fallbackKey → originalKey
            for (String fk : missed) {
                String fb = fallbackFullKey(fk);
                if (fb != null && !fb.equals(fk)) {
                    fallbackKeys.add(fb);
                    fallbackMap.put(fb, fk);
                }
            }
            if (CollUtil.isNotEmpty(fallbackKeys)) {
                List<AiPlaceholderVo> fallbackList = aiPlaceholderRse.listByKeys(fallbackKeys);
                for (AiPlaceholderVo vo : fallbackList) {
                    String originalKey = fallbackMap.get(vo.getPlaceholderKey());
                    if (originalKey != null) {
                        result.put(originalKey, vo);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 去掉参数部分： "#{uvTop:20}" → "#{uvTop}"；无参数则返回原值
     */
    private String fallbackFullKey(String fullKey) {
        if (StrUtil.isBlank(fullKey)) return fullKey;
        int colonIdx = fullKey.lastIndexOf(':');
        int endIdx = fullKey.lastIndexOf('}');
        if (colonIdx > 2 && endIdx > colonIdx) {
            return fullKey.substring(0, colonIdx) + "}";
        }
        return fullKey;
    }
}
