package com.jiuyu.replay.words.repository.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.words.entity.AnchorCruxWordsRelaEntity;
import com.jiuyu.replay.words.repository.dao.AnchorCruxWordsRelaDao;
import com.jiuyu.replay.words.repository.service.AnchorCruxWordsRelaService;
import com.jiuyu.replay.words.repository.service.AnchorCruxWordsService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 主播关键词关联
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Service("anchorCruxWordsRelaService")
public class AnchorCruxWordsRelaServiceImpl extends ServiceImpl<AnchorCruxWordsRelaDao, AnchorCruxWordsRelaEntity> implements AnchorCruxWordsRelaService {

    private final AnchorCruxWordsService anchorCruxWordsService;

    public AnchorCruxWordsRelaServiceImpl(AnchorCruxWordsService anchorCruxWordsService) {
        this.anchorCruxWordsService = anchorCruxWordsService;
    }

    /**
     * 获取主播关键词
     *
     * @param secUids 主播secuid
     */
    @Override
    public Map<String, List<String>> getAnchorWords(Collection<String> secUids) {
        if (EmptyUtil.isEmpty(secUids)) {
            return Map.of();
        }
        Map<String, List<Long>> secUidKeywordMap = super.lambdaQuery().in(AnchorCruxWordsRelaEntity::getSecUid, secUids)
            .eq(AnchorCruxWordsRelaEntity::getIsDeleted, 0)
            .select(AnchorCruxWordsRelaEntity::getSecUid, AnchorCruxWordsRelaEntity::getKeywordId)
            .list()
            .stream()
            .collect(Collectors.groupingBy(AnchorCruxWordsRelaEntity::getSecUid, Collectors.mapping(AnchorCruxWordsRelaEntity::getKeywordId, Collectors.toList())));
        if (EmptyUtil.isEmpty(secUidKeywordMap)) {
            return Map.of();
        }
        Set<Long> keywordIds = secUidKeywordMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, String> anchorWordsMap = anchorCruxWordsService.getWordsNameMap(keywordIds);
        return secUidKeywordMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(anchorWordsMap::get).filter(EmptyUtil::isNotEmpty).collect(Collectors.toList())));
    }


    /**
     * 根据关键词查询主播secUid
     *
     * @param keyword 关键词
     */
    @Override
    public List<String> queryKeyWordsAnchor(String keyword) {
        if (EmptyUtil.isEmpty(keyword)) {
            return List.of();
        }
        List<Long> keywordIds = anchorCruxWordsService.searchKeywords(keyword);
        if (EmptyUtil.isEmpty(keywordIds)) {
            return List.of();
        }
        return CollUtil.split(keywordIds, 1000).stream().flatMap(ids -> {
            return new BatchQuery<>((limit ,idx) -> {
                return super.lambdaQuery().in(AnchorCruxWordsRelaEntity::getKeywordId, ids)
                    .gt(idx != null, AnchorCruxWordsRelaEntity::getId, idx)
                    .eq(AnchorCruxWordsRelaEntity::getIsDeleted, 0)
                    .select(AnchorCruxWordsRelaEntity::getSecUid, AnchorCruxWordsRelaEntity::getId)
                    .last("limit " + limit)
                    .list();
            }, AnchorCruxWordsRelaEntity::getId).get().stream().map(AnchorCruxWordsRelaEntity::getSecUid);
        }).distinct().toList();
    }
}
