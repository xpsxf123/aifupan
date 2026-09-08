package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import com.jiuyu.replay.words.producer.AnchorUrlDetailsProducer;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.vo.anchor.AnchorUrlDetailsVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主播的附加表
 *
 * @author LJ
 * @date 2025-11-22
 */
@Component
@AllArgsConstructor
@Slf4j
public class AnchorUrlDetailsBll {

    private final AnchorUrlDetailsProducer anchorUrlDetailsProducer;
    private final RedisTemplate<String, String> redisTemplate;
    private final AnchorVideoProducer anchorVideoProducer;

    /**
     * 分页查询主播附加信息列表
     *
     * @param page  当前页
     * @param limit 每页记录数
     * @return
     */
    public PageUtils<AnchorUrlDetailsEntity> queryPage(Integer page, Integer limit) {
        return anchorUrlDetailsProducer.queryPage(page, limit);
    }

    /**
     * 根据主播ID获取附加信息
     *
     * @param secUid 主播ID
     * @return
     */
    public AnchorUrlDetailsVo getBySecUid(String secUid) {
        return anchorUrlDetailsProducer.getBySecUid(secUid);
    }

    /**
     * 新增主播附加信息
     *
     * @param entity 主播附加信息对象
     * @return
     */
    public AnchorUrlDetailsEntity save(AnchorUrlDetailsEntity entity) {
        return anchorUrlDetailsProducer.save(entity);
    }

    /**
     * 修改主播附加信息
     *
     * @param entity 主播附加信息对象
     * @return
     */
    public AnchorUrlDetailsEntity update(AnchorUrlDetailsEntity entity) {
        return anchorUrlDetailsProducer.update(entity);
    }

    /**
     * 新增或修改主播附加信息
     *
     * @param entity 主播附加信息对象
     * @return
     */
    public AnchorUrlDetailsEntity saveOrUpdate(AnchorUrlDetailsEntity entity) {
        return anchorUrlDetailsProducer.saveOrUpdate(entity);
    }

    /**
     * 删除主播附加信息
     *
     * @param secUid 主播ID
     */
    public void deleteBySecUid(String secUid) {
        anchorUrlDetailsProducer.deleteBySecUid(secUid);
    }

    /**
     * 批量删除主播附加信息
     *
     * @param secUidList 主播ID列表
     */
    public void deleteBySecUidList(List<String> secUidList) {
        anchorUrlDetailsProducer.deleteBySecUidList(secUidList);
    }

    /**
     * 获取要获取关键词的主播id
     *
     * @param duration 最少视频时长
     * @param limit    一次获取数量
     * @return 主播id
     */
    public List<String> secUidListByKeywordStatus(Integer duration, Integer limit) {
        return anchorUrlDetailsProducer.secUidListByKeywordStatus(duration, limit);
    }
}

