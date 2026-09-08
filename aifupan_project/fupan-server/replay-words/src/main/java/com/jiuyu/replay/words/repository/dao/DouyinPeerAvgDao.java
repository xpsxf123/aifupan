package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.DouyinPeerAvgEntity;
import com.jiuyu.replay.words.vo.peer.DouyinPeerAvgRawVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 抖音同行直播基础数据平均值 Dao
 *
 * @author jy
 * @date 2026-06-24
 */
@Mapper
public interface DouyinPeerAvgDao extends BaseMapper<DouyinPeerAvgEntity> {

    /**
     * 查询某一天所有行业的原始明细行（未聚合）。
     *
     * @param dayStart 当天 00:00:00
     * @param dayEnd   当天 23:59:59（或次日 00:00:00）
     * @return 原始明细行列表
     */
    List<DouyinPeerAvgRawVo> selectRawDataByDay(@Param("dayStart") Date dayStart, @Param("dayEnd") Date dayEnd);
}
