package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.dto.words.UserAnchorCountDto;

import java.util.List;
import java.util.Set;

/**
 * 主播用户关联RSE接口
 *
 * @author AI Assistant
 */
public interface AnchorUrlUserRse {

    /**
     * 根据用户ID列表获取每个用户的主播数量
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户主播数量列表
     */
    List<UserAnchorCountDto> getAnchorCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId);

    /**
     * 获取昨日新添加的主播secUid集合
     *
     * @return 主播secUid集合
     */
    Set<String> listYesterdayAnchorSecUids();

    /**
     * 分页获取最近添加的主播secUid列表（按创建时间倒序，去重）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 主播secUid列表
     */
    List<String> listRecentAnchorSecUids(int pageNum, int pageSize);

    /**
     * 统计指定授权类型的已授权记录数
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param authType 授权类型 1=巨量 / 2=千川 / 3=来客
     * @return 已授权记录数
     */
    long countAuthUsed(Long userId, Long tenantId, Integer authType);
}
