package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.UserAnalysisRollupEntity;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 视频的分析记录
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Mapper
public interface VideoAnalysisRecordDao extends BaseMapper<VideoAnalysisRecordEntity> {

}
