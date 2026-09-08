package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;

import java.util.List;

/**
 * 主播的附加表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-11-22
 */
public interface AnchorUrlDetailsService extends IService<AnchorUrlDetailsEntity> {

    /**
     * 获取要获取关键词的主播id
     *
     * @param duration 最少视频时长
     * @param limit    一次获取数量
     * @return 主播id
     */
    List<String> secUidListByKeywordStatus(Integer duration, Integer limit);
}

