package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.entity.TotalOnlineNumEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 直播总观看人次
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Mapper
public interface TotalOnlineNumDao extends BaseMapper<TotalOnlineNumEntity> {
	
}
