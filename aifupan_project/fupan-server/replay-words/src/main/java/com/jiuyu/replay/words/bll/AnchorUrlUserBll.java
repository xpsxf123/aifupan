package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.dto.words.UserAnchorCountDto;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.rse.AnchorUrlUserRse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主播用户关联业务逻辑层
 *
 * @author AI Assistant
 */
@Component
@RequiredArgsConstructor
public class AnchorUrlUserBll {

    private final AnchorUrlUserRse anchorUrlUserRse;

    /**
     * 根据用户ID列表获取每个用户的主播数量
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户主播数量列表
     */
    public R<List<UserAnchorCountDto>> getAnchorCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        List<UserAnchorCountDto> anchorCountList = anchorUrlUserRse.getAnchorCountByUserIdsAndTenantId(userIds, tenantId);
        return R.ok(anchorCountList);
    }

    /**
     * 统计指定授权类型的已授权记录数
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @param authType 授权类型 1=巨量 / 2=千川 / 3=来客
     * @return 已授权记录数
     */
    public long countAuthUsed(Long userId, Long tenantId, Integer authType) {
        return anchorUrlUserRse.countAuthUsed(userId, tenantId, authType);
    }
}
