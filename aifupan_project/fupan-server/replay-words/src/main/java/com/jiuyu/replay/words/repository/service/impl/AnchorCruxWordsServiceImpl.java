package com.jiuyu.replay.words.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.words.entity.AnchorCruxWordsEntity;
import com.jiuyu.replay.words.repository.dao.AnchorCruxWordsDao;
import com.jiuyu.replay.words.repository.service.AnchorCruxWordsService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主播关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Service("anchorCruxWordsService")
public class AnchorCruxWordsServiceImpl extends ServiceImpl<AnchorCruxWordsDao, AnchorCruxWordsEntity> implements AnchorCruxWordsService {


    /**
     * 获取关键词名称
     *
     * @param keywordIds 关键词ID
     */
    @Override
    public Map<Long, String> getWordsNameMap(Collection<Long> keywordIds) {
        if (EmptyUtil.isEmpty(keywordIds)) {
            return Map.of();
        }
        return super.lambdaQuery()
            .in(AnchorCruxWordsEntity::getId, keywordIds)
            .select(AnchorCruxWordsEntity::getId, AnchorCruxWordsEntity::getKeyword)
            .list().stream()
            .collect(Collectors.toMap(AnchorCruxWordsEntity::getId, AnchorCruxWordsEntity::getKeyword));
    }

    /**
     * 搜索关键词
     *
     * @param keyword 关键词
     */
    @Override
    public List<Long> searchKeywords(String keyword) {
        if (EmptyUtil.isEmpty(keyword)) {
            return List.of();
        }
        return new BatchQuery<>((limit, idx) -> {
            return super.lambdaQuery().eq(AnchorCruxWordsEntity::getKeyword, keyword)
                .gt(idx != null, AnchorCruxWordsEntity::getId, idx)
                .select(AnchorCruxWordsEntity::getId)
                .last("limit " + limit)
                .list();
        }, AnchorCruxWordsEntity::getId).get().stream().map(AnchorCruxWordsEntity::getId).toList();
    }
}
