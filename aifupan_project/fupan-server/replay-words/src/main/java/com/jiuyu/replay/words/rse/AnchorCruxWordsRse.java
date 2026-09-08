package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.words.entity.AnchorCruxWordsEntity;
import com.jiuyu.replay.words.entity.AnchorCruxWordsRelaEntity;
import com.jiuyu.replay.words.vo.AnchorCruxWordsVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主播关键词RSE接口
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
public interface AnchorCruxWordsRse {

    /**
     * 根据主播secUid查询关键词列表
     *
     * @param secUid 主播secUid
     * @return 关键词列表
     */
    List<AnchorCruxWordsVo> listBySecUid(String secUid);

    /**
     * 获取已有关键词的主播secUid集合
     *
     * @param secUids 主播secUid集合
     * @return 已有关键词的主播secUid集合
     */
    Set<String> listExistKeywordSecUids(Collection<String> secUids);

    /**
     * 根据关键词列表批量获取已存在的关键词
     *
     * @param keywords 关键词列表
     * @return 关键词->实体的映射
     */
    Map<String, AnchorCruxWordsEntity> listByKeywords(List<String> keywords);

    /**
     * 批量保存关键词
     *
     * @param keywords 关键词实体列表
     */
    void saveBatchKeywords(List<AnchorCruxWordsEntity> keywords);

    /**
     * 批量保存主播关键词关联
     *
     * @param relaList 关联实体列表
     */
    void saveBatchRela(List<AnchorCruxWordsRelaEntity> relaList);

    /**
     * 根据主播secUid删除主播关键词
     * @param secUid 主播secUid
     */
    void deleteAnchorKeywords(String secUid);

    /**
     * 查询主播已关联的关键词ID集合
     * @param secUid 主播secUid
     * @param keywordIds 关键词ID列表
     * @return 已关联的关键词ID集合
     */
    Set<Long> listExistRelaKeywordIds(String secUid, List<Long> keywordIds);

    /**
     * 根据主播secUid和关键词ID列表移除关联关系
     * @param secUid 主播secUid
     * @param keywordIds 关键词ID列表
     * @return 移除的数量
     */
    int removeByKeywordIds(String secUid, List<Long> keywordIds);
}