package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.AnchorCruxWordsRelaEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 主播关键词关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
public interface AnchorCruxWordsRelaService extends IService<AnchorCruxWordsRelaEntity> {

    /**
     * 获取主播关键词
     *
     * @param secUids 主播secuid
     */
    Map<String, List<String>> getAnchorWords(Collection<String> secUids);


    /**
     * 根据关键词查询主播secUid
     *
     * @param keyword 关键词
     */
    List<String> queryKeyWordsAnchor(String keyword);

    /**
     * 获取主播关键词
     *
     * @param secUids 主播secuid
     */
    default Map<String, String> getAnchorWordsMap(Collection<String> secUids) {
        return getAnchorWords(secUids).entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> String.join(",", entry.getValue())));
    }
}
