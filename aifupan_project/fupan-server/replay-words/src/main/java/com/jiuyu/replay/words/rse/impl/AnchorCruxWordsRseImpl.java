package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.words.entity.AnchorCruxWordsEntity;
import com.jiuyu.replay.words.entity.AnchorCruxWordsRelaEntity;
import com.jiuyu.replay.words.repository.service.AnchorCruxWordsRelaService;
import com.jiuyu.replay.words.repository.service.AnchorCruxWordsService;
import com.jiuyu.replay.words.rse.AnchorCruxWordsRse;
import com.jiuyu.replay.words.vo.AnchorCruxWordsVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 主播关键词RSE接口实现类
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Service
public class AnchorCruxWordsRseImpl implements AnchorCruxWordsRse {

    @Resource
    private AnchorCruxWordsService anchorCruxWordsService;
    @Resource
    private AnchorCruxWordsRelaService anchorCruxWordsRelaService;

    @Override
    public List<AnchorCruxWordsVo> listBySecUid(String secUid) {
        if (!StringUtils.hasText(secUid)) {
            return new LinkedList<>();
        }

        // 查询关联表获取该主播的所有关键词id
        LambdaQueryWrapper<AnchorCruxWordsRelaEntity> relaWrapper = new LambdaQueryWrapper<>();
        relaWrapper.eq(AnchorCruxWordsRelaEntity::getSecUid, secUid);
        List<AnchorCruxWordsRelaEntity> relaList = anchorCruxWordsRelaService.list(relaWrapper);

        if (relaList == null || relaList.isEmpty()) {
            return new LinkedList<>();
        }

        // 获取所有关键词id
        List<Long> keywordIds = relaList.stream()
                .map(AnchorCruxWordsRelaEntity::getKeywordId)
                .distinct()
                .collect(Collectors.toList());

        // 查询关键词表
        LambdaQueryWrapper<AnchorCruxWordsEntity> wordsWrapper = new LambdaQueryWrapper<>();
        wordsWrapper.in(AnchorCruxWordsEntity::getId, keywordIds);
        List<AnchorCruxWordsEntity> wordsList = anchorCruxWordsService.list(wordsWrapper);

        if (wordsList == null || wordsList.isEmpty()) {
            return new LinkedList<>();
        }

        // 将关键词列表转为map，方便查找
        Map<Long, AnchorCruxWordsEntity> wordsMap = wordsList.stream()
                .collect(Collectors.toMap(AnchorCruxWordsEntity::getId, e -> e));

        // 组装返回结果
        List<AnchorCruxWordsVo> result = new LinkedList<>();
        for (AnchorCruxWordsRelaEntity rela : relaList) {
            AnchorCruxWordsEntity words = wordsMap.get(rela.getKeywordId());
            if (words != null) {
                AnchorCruxWordsVo vo = new AnchorCruxWordsVo();
                vo.setId(words.getId());
                vo.setSecUid(rela.getSecUid());
                vo.setKeyword(words.getKeyword());
                vo.setUserId(rela.getUserId());
                vo.setTenantId(rela.getTenantId());
                vo.setCreateDate(rela.getCreateDate());
                vo.setUpdateDate(rela.getUpdateDate());
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public Set<String> listExistKeywordSecUids(Collection<String> secUids) {
        if (secUids == null || secUids.isEmpty()) {
            return new HashSet<>();
        }

        LambdaQueryWrapper<AnchorCruxWordsRelaEntity> relaWrapper = new LambdaQueryWrapper<>();
        relaWrapper.in(AnchorCruxWordsRelaEntity::getSecUid, secUids)
                .select(AnchorCruxWordsRelaEntity::getSecUid);
        List<AnchorCruxWordsRelaEntity> existKeywordAnchors = anchorCruxWordsRelaService.list(relaWrapper);

        if (existKeywordAnchors == null || existKeywordAnchors.isEmpty()) {
            return new HashSet<>();
        }

        return existKeywordAnchors.stream()
                .map(AnchorCruxWordsRelaEntity::getSecUid)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, AnchorCruxWordsEntity> listByKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<AnchorCruxWordsEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AnchorCruxWordsEntity::getKeyword, keywords);
        List<AnchorCruxWordsEntity> list = anchorCruxWordsService.list(wrapper);

        if (list == null || list.isEmpty()) {
            return new HashMap<>();
        }

        return list.stream()
                .collect(Collectors.toMap(AnchorCruxWordsEntity::getKeyword, e -> e, (o1, o2) -> o1));
    }

    @Override
    public void saveBatchKeywords(List<AnchorCruxWordsEntity> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        anchorCruxWordsService.saveBatch(keywords);
    }

    @Override
    public void saveBatchRela(List<AnchorCruxWordsRelaEntity> relaList) {
        if (relaList == null || relaList.isEmpty()) {
            return;
        }
        anchorCruxWordsRelaService.saveBatch(relaList);
    }

    @Override
    public void deleteAnchorKeywords(String secUid) {
        if(secUid == null) {
            return;
        }
        anchorCruxWordsRelaService.remove(new LambdaQueryWrapper<AnchorCruxWordsRelaEntity>().eq(AnchorCruxWordsRelaEntity::getSecUid, secUid));
    }

    @Override
    public Set<Long> listExistRelaKeywordIds(String secUid, List<Long> keywordIds) {
        if (secUid == null || keywordIds == null || keywordIds.isEmpty()) {
            return new HashSet<>();
        }

        LambdaQueryWrapper<AnchorCruxWordsRelaEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorCruxWordsRelaEntity::getSecUid, secUid)
                .in(AnchorCruxWordsRelaEntity::getKeywordId, keywordIds)
                .select(AnchorCruxWordsRelaEntity::getKeywordId);
        List<AnchorCruxWordsRelaEntity> list = anchorCruxWordsRelaService.list(wrapper);

        if (list == null || list.isEmpty()) {
            return new HashSet<>();
        }

        return list.stream()
                .map(AnchorCruxWordsRelaEntity::getKeywordId)
                .collect(Collectors.toSet());
    }

    @Override
    public int removeByKeywordIds(String secUid, List<Long> keywordIds) {
        if (!StringUtils.hasText(secUid) || keywordIds == null || keywordIds.isEmpty()) {
            return 0;
        }

        LambdaQueryWrapper<AnchorCruxWordsRelaEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorCruxWordsRelaEntity::getSecUid, secUid)
                .in(AnchorCruxWordsRelaEntity::getKeywordId, keywordIds);

        boolean result = anchorCruxWordsRelaService.remove(wrapper);
        return result ? keywordIds.size() : 0;
    }

}
