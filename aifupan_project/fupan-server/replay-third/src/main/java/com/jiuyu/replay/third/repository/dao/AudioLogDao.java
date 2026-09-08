package com.jiuyu.replay.third.repository.dao;

import com.jiuyu.replay.third.entity.AudioLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 语音识别调用日志
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-25 16:38:48
 */
@Mapper
public interface AudioLogDao extends BaseMapper<AudioLogEntity> {
	
}
