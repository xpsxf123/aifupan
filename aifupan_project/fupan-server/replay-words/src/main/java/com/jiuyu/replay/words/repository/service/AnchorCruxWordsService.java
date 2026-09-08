package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.AnchorCruxWordsEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 主播关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
public interface AnchorCruxWordsService extends IService<AnchorCruxWordsEntity> {

    /**
     * 获取关键词名称
     *
     * @param keywordIds 关键词ID
     */
    Map<Long, String> getWordsNameMap(Collection<Long> keywordIds);


    /**
     * 搜索关键词
     *
     * @param keyword 关键词
     */
    List<Long> searchKeywords(String keyword);
}
