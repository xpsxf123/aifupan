package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.vo.words.SourceStarVo;

import java.util.List;
import java.util.Set;

/**
 * 星标RSE接口
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
public interface SourceStarRse {

    /**
     * 添加星标
     *
     * @param sourceId   来源id
     * @param sourceType 来源类型
     * @param userId     用户id
     * @param tenantId   租户id
     * @return 新增的ID
     */
    Long save(String sourceId, Integer sourceType, Long userId, Long tenantId);

    /**
     * 删除星标记录
     *
     * @param sourceId   来源id
     * @param sourceType 来源类型
     * @return 是否成功
     */
    boolean remove(String sourceId, Integer sourceType);

    /**
     * 根据来源id和类型查询星标记录
     *
     * @param sourceId   来源id
     * @param sourceType 来源类型
     * @return 星标信息
     */
    SourceStarVo getBySourceIdAndType(String sourceId, Integer sourceType);

    /**
     * 检查是否存在星标记录
     *
     * @param sourceId   来源id
     * @param sourceType 来源类型
     * @return 是否存在
     */
    boolean existsStar(String sourceId, Integer sourceType);

    /**
     * 批量查询存在星标记录的来源id集合
     *
     * @param sourceIds  来源id集合
     * @param sourceType 来源类型
     * @return 存在星标的来源id集合
     */
    Set<String> listExistSourceIds(List<String> sourceIds, Integer sourceType);

    /**
     * 根据来源id集合查询星标列表
     *
     * @param sourceIds  来源id集合
     * @param sourceType 来源类型
     * @return 存在星标的来源id集合
     */
    List<SourceStarVo> listBySourceIds(List<String> sourceIds, Integer sourceType);
}
