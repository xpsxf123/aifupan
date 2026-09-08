package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.words.bo.AnchorUrlPegBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 主播url
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Mapper
public interface AnchorUrlDao extends BaseMapper<AnchorUrlEntity> {

    /**
     * 分页查询
     *
     * @param page           分页参数
     * @param anchorUrlPegBo 查询参数
     * @return 数据
     */
    Page<AnchorUrlEntity> pageList(IPage<AnchorUrlEntity> page, @Param("bo") AnchorUrlPegBo anchorUrlPegBo);


}
