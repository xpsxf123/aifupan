package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import com.jiuyu.replay.words.vo.anchor.AnchorUrlDetailsVo;

import java.util.List;

/**
 * 主播的附加表
 *
 * @author LJ
 * @date 2025-11-22
 */
public interface AnchorUrlDetailsProducer {

    /**
     * 分页查询主播附加信息列表
     *
     * @param page  当前页
     * @param limit 每页记录数
     * @return
     */
    PageUtils<AnchorUrlDetailsEntity> queryPage(Integer page, Integer limit);

    /**
     * 根据主播ID获取附加信息
     *
     * @param secUid 主播ID
     * @return
     */
    AnchorUrlDetailsVo getBySecUid(String secUid);

    /**
     * 新增主播附加信息
     *
     * @param entity 主播附加信息对象
     * @return
     */
    AnchorUrlDetailsEntity save(AnchorUrlDetailsEntity entity);

    /**
     * 修改主播附加信息
     *
     * @param entity 主播附加信息对象
     * @return
     */
    AnchorUrlDetailsEntity update(AnchorUrlDetailsEntity entity);

    /**
     * 新增或修改主播附加信息
     *
     * @param entity 主播附加信息对象
     * @return
     */
    AnchorUrlDetailsEntity saveOrUpdate(AnchorUrlDetailsEntity entity);

    /**
     * 删除主播附加信息
     *
     * @param secUid 主播ID
     */
    void deleteBySecUid(String secUid);

    /**
     * 批量删除主播附加信息
     *
     * @param secUidList 主播ID列表
     */
    void deleteBySecUidList(List<String> secUidList);

    /**
     * 获取要获取关键词的主播id
     *
     * @param duration 最少视频时长
     * @param limit    一次获取数量
     * @return 主播id
     */
    List<String> secUidListByKeywordStatus(Integer duration, Integer limit);
}

