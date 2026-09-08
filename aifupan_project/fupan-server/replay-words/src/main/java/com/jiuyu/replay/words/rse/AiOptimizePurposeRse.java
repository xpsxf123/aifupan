package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.vo.words.AiOptimizePurposeVo;
import com.jiuyu.replay.words.bo.AiOptimizePurposeSaveBo;
import com.jiuyu.replay.words.bo.AiOptimizePurposeUpdateBo;

import java.util.List;
import java.util.Set;

/**
 * AI优化目的RSE接口
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
public interface AiOptimizePurposeRse {

    /**
     * 新增AI优化目的
     *
     * @param saveBo 保存参数
     * @return 新增的ID
     */
    Long save(AiOptimizePurposeSaveBo saveBo);

    /**
     * 修改AI优化目的
     *
     * @param updateBo 更新参数
     * @return 是否成功
     */
    boolean update(AiOptimizePurposeUpdateBo updateBo);

    /**
     * 根据来源id查询AI优化目的信息
     *
     * @param sourceId   来源id
     * @return 优化目的信息
     */
    AiOptimizePurposeVo getBySourceId(String sourceId);

    /**
     * 批量查询存在AI优化目的记录的来源id集合
     *
     * @param sourceIds 来源id集合
     * @return 存在记录的来源id集合
     */
    Set<String> listExistSourceIds(List<String> sourceIds);
}
