package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 主播的附加表
 *
 * @author LJ
 * @date 2025-11-22
 */
@Mapper
public interface AnchorUrlDetailsDao extends BaseMapper<AnchorUrlDetailsEntity> {

    /**
     * 获取要获取关键词的主播id
     *
     * @param duration 最少视频时长
     * @param limit    一次获取数量
     * @return 主播id
     */
    List<String> secUidListByKeywordStatus(Integer duration, Integer limit);

}

