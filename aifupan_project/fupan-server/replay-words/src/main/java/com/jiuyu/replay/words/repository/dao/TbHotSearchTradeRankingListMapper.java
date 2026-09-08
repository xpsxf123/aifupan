package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.framework.shandard.Entry;
import com.jiuyu.replay.words.entity.TbHotSearchTradeRankingList;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 热搜行业榜单-榜单主播核心表
 *
 * @date 2025-01-09 15:54:12
 */
@Mapper
public interface TbHotSearchTradeRankingListMapper extends BaseMapper<TbHotSearchTradeRankingList> {


    int updateBatchSelective(@Param("list") Collection<TbHotSearchTradeRankingList> list);

    int batchInsert(@Param("list") Collection<TbHotSearchTradeRankingList> list);

    /**
     * 统计行业指定渠道类型的主播数量
     *
     * @param isUp          是否为上架主播
     * @param lastUpdate    最后更新时间
     * @param channelTypes  渠道类型列表
     * @param collectStatus 采集状态
     * @param tradeIds      行业id列表
     *
     * @return 行业对应的主播数量
     */
    List<Entry<Long, Long>> countTradeAnchor(@Param("isUp") Boolean isUp, @Param("lastUpdate") LocalDateTime lastUpdate, @Param("channelTypes") List<Integer> channelTypes, @Param("collectStatus") Integer collectStatus, @Param("tradeIds") Collection<Long> tradeIds);


    /**
     * 批量删除
     *
     * @param ids             id列表
     * @param operationUserId 操作用户id
     *
     * @return 删除数量
     */
    Integer batchDelete(@Param("ids") List<Long> ids, @Param("operationUserId") long operationUserId);

    /**
     * 查询没有关联的主播
     *
     * @param anchorNumbers 主播编号列表
     *
     * @return 没有关联的主播列表
     */
    List<TbHotSearchTradeRankingList> queryAnchorNumberNotCorrelation(@Param("anchorNumbers") Collection<String> anchorNumbers);
}
