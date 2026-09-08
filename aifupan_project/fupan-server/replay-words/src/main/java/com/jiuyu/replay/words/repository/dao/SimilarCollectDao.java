package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.SimilarCollectEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 相似达人每日汇总
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-10-16 15:40:25
 */
@Deprecated
@Mapper
public interface SimilarCollectDao extends BaseMapper<SimilarCollectEntity> {

	/**
	 * 统计各行业30天内更新的相似主播数量（使用连表查询优化性能）
	 * 时间范围：最近30天（使用 SQL 函数 DATE_SUB(NOW(), INTERVAL 30 DAY)）
	 *
	 * @param tradeIds 行业ID列表
	 * @param checkRankEnabled 是否检查行业热榜生效状态（true: 只统计 rank_enabled = 1 的行业，false: 不检查）
	 * @return 返回Map列表，每个Map包含 tradeId 和 count
	 * @author RayChou
	 * @date 2025-10-30
	 * @update 2025-11-18
	 */
	List<Map<String, Object>> countByTradeIdsAndUpdateDate(
			@Param("tradeIds") List<Long> tradeIds,
			@Param("checkRankEnabled") Boolean checkRankEnabled
	);

}
