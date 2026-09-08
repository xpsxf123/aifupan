package com.jiuyu.replay.common.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.common.entity.ClientLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ClientLogDao extends BaseMapper<ClientLogEntity> {
}
