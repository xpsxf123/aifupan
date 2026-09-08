package com.jiuyu.replay.words.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.dto.words.UserAnchorCountDto;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.AnchorUrlUserBll;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 主播用户关联API
 *
 * @author AI Assistant
 */
@Service
@RequiredArgsConstructor
public class AnchorUrlUserApi implements AnchorUrlUserFeign {

    private final AnchorUrlUserBll anchorUrlUserBll;
    private final AnchorUrlUserService anchorUrlUserService;
    private final AnchorUrlBll anchorUrlBll;

    /**
     * 根据用户ID列表获取每个用户的主播数量
     *
     * @param userIds  用户ID列表
     * @param tenantId 租户id
     * @return 用户主播数量列表
     */
    @Override
    public R<List<UserAnchorCountDto>> getAnchorCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        return anchorUrlUserBll.getAnchorCountByUserIdsAndTenantId(userIds, tenantId);
    }

    /**
     * 按 secUid + userId + tenantId 三元组查询直播间用户配置详情（B7 自动触发使用）
     *
     * @param secUid   主播唯一标识（tb_anchor_url_user.anchor_url_sec_uid）
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return AnchorUrlUserVo（含三开关字段）；不存在返 R.error(30000)
     */
    @Override
    public R<AnchorUrlUserVo> getBySecUidAndUser(String secUid, Long userId, Long tenantId) {
        AnchorUrlUserEntity entity = anchorUrlUserService.getOne(
                new LambdaQueryWrapper<AnchorUrlUserEntity>()
                        .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                        // tb_anchor_url_user 软删除主字段是 isRemoveRecord（业务移除状态），isDeleted 为 MP 逻辑删除字段（已自动注入条件）
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0));
        if (entity == null) {
            return R.error(30000, "记录不存在");
        }
        AnchorUrlUserVo vo = new AnchorUrlUserVo();
        BeanUtils.copyProperties(entity, vo);
        return R.ok(vo);
    }

    /**
     * 切换单个 AI 监控能力开关（B8）
     *
     * @param userId      用户 ID
     * @param tenantId    租户 ID
     * @param secUid      主播唯一标识
     * @param monitorType 监控类型 0=质检 / 1=还原度 / 2=巡检
     * @param enabled     目标状态 0=关 / 1=开
     * @return R&lt;Boolean&gt;；data=true 表示切换成功（含幂等情况）
     */
    @Override
    public R<Boolean> setMonitorEnabled(Long userId, Long tenantId, String secUid, Integer monitorType, Integer enabled) {
        return anchorUrlBll.updateMonitorSwitch(userId, tenantId, secUid, monitorType, enabled);
    }

    /**
     * 批量按 secUid 集合 + userId + tenantId 查询直播间用户配置（reportStatus 装配 monitorEnabled 使用）
     *
     * @param secUids  主播唯一标识列表
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return AnchorUrlUserVo 列表
     */
    @Override
    public R<List<AnchorUrlUserVo>> listBySecUidsAndUser(List<String> secUids, Long userId, Long tenantId) {
        if (secUids == null || secUids.isEmpty()) {
            return R.ok(List.of());
        }
        List<AnchorUrlUserEntity> entities = anchorUrlUserService.list(
                new LambdaQueryWrapper<AnchorUrlUserEntity>()
                        .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUids)
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                        // tb_anchor_url_user 软删除主字段 isRemoveRecord（业务移除），isDeleted 由 MP 注入
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0));
        List<AnchorUrlUserVo> vos = entities.stream().map(e -> {
            AnchorUrlUserVo vo = new AnchorUrlUserVo();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        return R.ok(vos);
    }
}
