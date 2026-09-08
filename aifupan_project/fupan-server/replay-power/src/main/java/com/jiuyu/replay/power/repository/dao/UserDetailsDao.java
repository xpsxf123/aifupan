package com.jiuyu.replay.power.repository.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.power.entity.UserDetailsEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户详情表
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Mapper
public interface UserDetailsDao extends BaseMapper<UserDetailsEntity> {

    Long countClientDetailBySaleId(@Param("saleId") Long saleId);
}
