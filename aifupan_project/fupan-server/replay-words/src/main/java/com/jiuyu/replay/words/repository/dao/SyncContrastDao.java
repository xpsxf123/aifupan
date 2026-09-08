package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.words.bo.SyncContrastListBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.vo.SyncContrastListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 客户端对比数据
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Mapper
public interface SyncContrastDao extends BaseMapper<SyncContrastEntity> {

    /**
     * 分析记录页面-对比分析列表分页接口
     *
     * @param syncContrastListBo 查询参数
     * @return 分页数据
     */
    Page<SyncContrastListVo> pageSyncContrastNew(IPage<AnchorUrlEntity> page, @Param("bo") SyncContrastListBo syncContrastListBo);
}
