package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.OnlineNumEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播实时在线人数
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Mapper
public interface OnlineNumDao extends BaseMapper<OnlineNumEntity> {
	
}
