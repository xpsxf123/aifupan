package com.jiuyu.replay.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.common.entity.ClientVersionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户端对应的版本
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-13 16:29:50
 */
@Mapper
public interface ClientVersionDao extends BaseMapper<ClientVersionEntity> {
	
}
