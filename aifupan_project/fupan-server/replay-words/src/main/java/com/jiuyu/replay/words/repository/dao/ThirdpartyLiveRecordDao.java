package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.ThirdpartyLiveRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 第三方直播录制记录Dao
 *
 * @author System
 * @date 2026-04-09
 */
@Mapper
public interface ThirdpartyLiveRecordDao extends BaseMapper<ThirdpartyLiveRecordEntity> {

    /**
     * 按行业ID集合随机查询指定数量的记录
     *
     * @param tradeIds 行业ID集合
     * @param limit    查询数量
     * @return 记录列表
     */
    @Select("<script>" +
            "SELECT id, cloud_url, viewers, monthly_sales FROM tb_thirdparty_live_record " +
            "WHERE is_deleted = 0 AND cloud_url IS NOT NULL AND cloud_url != '' " +
            "<if test='tradeIds != null and tradeIds.size() > 0'>" +
            "AND trade_id IN <foreach collection='tradeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "</if>" +
            "ORDER BY RAND() LIMIT #{limit}" +
            "</script>")
    List<ThirdpartyLiveRecordEntity> randomByTradeIds(
            @Param("tradeIds") List<Long> tradeIds,
            @Param("limit") int limit);

    /**
     * 全表随机查询（兜底）
     *
     * @param limit 查询数量
     * @return 记录列表
     */
    @Select("SELECT id, cloud_url, viewers, monthly_sales FROM tb_thirdparty_live_record " +
            "WHERE is_deleted = 0 AND cloud_url IS NOT NULL AND cloud_url != '' " +
            "ORDER BY RAND() LIMIT #{limit}")
    List<ThirdpartyLiveRecordEntity> randomAll(@Param("limit") int limit);
}
