package com.jiuyu.replay.power.repository.dao;

import com.jiuyu.replay.power.entity.BindingAccountEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 父子绑定记录
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Mapper
public interface BindingAccountDao extends BaseMapper<BindingAccountEntity> {
	
}
