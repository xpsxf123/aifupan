package com.jiuyu.replay.words.repository.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.AnchorUrlWhiteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 主播白名单表
 * 
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
@Mapper
public interface AnchorUrlWhiteDao extends BaseMapper<AnchorUrlWhiteEntity> {

    @Select("SELECT COUNT(*) AS count FROM tb_anchor_url_white WHERE sec_uid  = #{secUid} ")
    Integer getUserCounts(String secUid);
}
