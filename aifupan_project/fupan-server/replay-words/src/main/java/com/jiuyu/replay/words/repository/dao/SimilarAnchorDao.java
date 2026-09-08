package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.words.entity.SimilarAnchorEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.vo.TradeRankAdminVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 相似主播信息表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-10-16 15:40:25
 */
@Mapper
public interface SimilarAnchorDao extends BaseMapper<SimilarAnchorEntity> {

    /**
     * 行业热榜分页查询（使用连接查询优化性能，只查询最近30天）
     *
     * @param page        分页参数
     * @param tradeId     行业ID
     * @param anchorName
     * @param liveKeyword
     *
     * @return 分页结果
     *
     * @author RayChou
     * @date 2025-10-28
     */
    IPage<SimilarAnchorEntity> pageTradeRank(Page<SimilarAnchorEntity> page, @Param("tradeId") Long tradeId, @Param("anchorName") String anchorName, @Param("liveKeyword") String liveKeyword);

    /**
     * 后台管理系统-行业热榜分页查询（支持多条件筛选）
     *
     * @param page            分页参数
     * @param tradeId         行业ID（可选，不传则查询全部）
     * @param uniqueId        主播抖音账号（精确筛选）
     * @param anchorName      主播名称（模糊搜索）
     * @param heatStart       热度开始值
     * @param heatEnd         热度结束值
     * @param updateTimeStart 更新时间开始值
     * @param updateTimeEnd   更新时间结束值
     * @param orderBy         排序字段
     * @param orderDirection  排序方式
     * @param inStock         是否在直播间内（true：是，false：否）
     * @param upRanking       是否上榜（true：是，false：否）
     * @param sourceTypes     数据来源类型列表
     * @param liveKeyword
     * @param effective
     * @param collectStatus
     *
     * @return 分页结果
     *
     * @author RayChou, HeHui
     * @date 2025-10-28, 2026-1-27
     */
    IPage<TradeRankAdminVo> pageTradeRankAdmin(Page<SimilarAnchorEntity> page,
                                               @Param("tradeId") Long tradeId,
                                               @Param("uniqueId") String uniqueId,
                                               @Param("anchorName") String anchorName,
                                               @Param("heatStart") Integer heatStart,
                                               @Param("heatEnd") Integer heatEnd,
                                               @Param("updateTimeStart") LocalDateTime updateTimeStart,
                                               @Param("updateTimeEnd") LocalDateTime updateTimeEnd,
                                               @Param("orderBy") String orderBy,
                                               @Param("orderDirection") String orderDirection,
                                               @Param("inStock") Boolean inStock,
                                               @Param("upRanking") Boolean upRanking,
                                               @Param("sourceTypes") List<Integer> sourceTypes,
                                               @Param("liveKeyword") String liveKeyword,
                                               @Param("effective") Boolean effective,
                                               @Param("collectStatus") List<Integer> collectStatus);

    /**
     * 批量更新基础信息
     *
     * @param updateList 更新列表
     */
    void batchUpdateBaseInfo(List<SimilarAnchorEntity> updateList);


    /**
     * 批量保存（使用selective方式）
     *
     * @param saveList 保存列表
     */
    void batchUpdateSelective(List<SimilarAnchorEntity> saveList);

    /**
     * 批量保存基础信息
     *
     * @param saveList 保存列表
     */
    void batchInsertBaseInfo(List<SimilarAnchorEntity> saveList);

    /**
     * 批量保存
     *
     * @param saveList 批量保存列表
     */
    void batchInsert(List<SimilarAnchorEntity> saveList);
}
