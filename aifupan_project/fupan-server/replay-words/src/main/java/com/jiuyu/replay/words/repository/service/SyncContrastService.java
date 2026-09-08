package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.bo.SyncContrastListBo;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.vo.SyncContrastListVo;

/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
public interface SyncContrastService extends IService<SyncContrastEntity> {


    /**
     * 分析记录页面-对比分析列表分页接口
     *
     * @param syncContrastListBo 查询参数
     * @return 分页数据
     */
    Page<SyncContrastListVo> pageSyncContrastNew(SyncContrastListBo syncContrastListBo);
}

