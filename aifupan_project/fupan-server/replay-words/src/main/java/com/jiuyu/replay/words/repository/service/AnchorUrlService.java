package com.jiuyu.replay.words.repository.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.words.bo.AnchorUrlPegBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
public interface AnchorUrlService extends IService<AnchorUrlEntity> {

    /**
     * 分页查询
     *
     * @param page           分页参数
     * @param anchorUrlPegBo 查询参数
     * @return 数据
     */
    Page<AnchorUrlEntity> pageList(IPage<AnchorUrlEntity> page, AnchorUrlPegBo anchorUrlPegBo);

    /**
     * 批量更新系统行业id
     *
     * @param anchorIds 主播id
     * @param tradeId    系统行业ID
     * @return 是否成功
     */
    boolean batchUpdateSystemTradeId(List<Long> anchorIds, Long tradeId);


    /**
     * 批量获取主播
     *
     * @param anchorIds 主播id
     * @return 主播
     */
    Map<Long, AnchorUrlEntity> getAnchorMap(List<Long> anchorIds);

    /**
     * 根据主播抖音号查询主播
     *
     * @param anchorNumber 主播抖音号
     * @param platform     平台 0：抖音 1：快手 2：视频号
     * @return 主播
     */
    Optional<AnchorUrlEntity> findByAnchorNumber(String anchorNumber, Integer platform);


    /**
     * 根据主播抖音号查询主播
     *
     * @param anchorNumbers 主播抖音号
     * @param platform     0：抖音 1：快手 2：视频号
     * @return 主播
     */
    Map<String, AnchorUrlEntity> getAnchorMapByAnchorNumber(List<String> anchorNumbers, Integer platform);
}

