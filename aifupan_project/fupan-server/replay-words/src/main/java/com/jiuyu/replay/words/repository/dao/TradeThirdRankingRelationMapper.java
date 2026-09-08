package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.TradeThirdRankingRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 热搜行业榜单-行业与第三方榜单关联表
 *
 * @date 2025-01-09 15:54:12
 */
@Mapper
public interface TradeThirdRankingRelationMapper extends BaseMapper<TradeThirdRankingRelation> {
    int updateBatchSelective(@Param("list") List<TradeThirdRankingRelation> list);

    int batchInsert(@Param("list") List<TradeThirdRankingRelation> list);

    /**
     * 获取指定星期的行业id
     *
     * @param dayOfWeek 星期
     *
     * @return 第三方行业
     */
    List<TradeThirdRankingRelation> getDayOfWeekThirdTrades(@Param("dayOfWeek") int dayOfWeek);
}
