package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.dto.words.UserAnchorCountDto;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;

import java.util.List;

/**
 * 主播用户关联 Feign 接口
 *
 * @author AI Assistant
 */
public interface AnchorUrlUserFeign {

    /**
     * 根据用户ID列表获取每个用户的主播数量
     *
     * @param userIds  用户ID列表
     * @param tenantId 租户id
     * @return 用户主播数量列表
     */
    R<List<UserAnchorCountDto>> getAnchorCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId);

    /**
     * 按 secUid + userId + tenantId 三元组查询直播间用户配置详情（B7 自动触发使用）。
     *
     * @param secUid   主播唯一标识（tb_anchor_url_user.anchor_url_sec_uid）
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return AnchorUrlUserVo（含三开关字段）；不存在返 R.error(30000)
     */
    R<AnchorUrlUserVo> getBySecUidAndUser(String secUid, Long userId, Long tenantId);

    /**
     * 切换单个 AI 监控能力开关（B8 — POST /replay/script-monitor/setMonitorEnabled）。
     *
     * <p>按「短路幂等 → 还原度暂禁 → 授权量校验 → 单字段写库 → 占用更新」4 步完成单能力切换。</p>
     *
     * @param userId      用户 ID（从 Controller GlobalObject 取）
     * @param tenantId    租户 ID（从 Controller GlobalObject 取）
     * @param secUid      主播唯一标识
     * @param monitorType 监控类型 0=质检 / 1=还原度 / 2=巡检
     * @param enabled     目标状态 0=关 / 1=开
     * @return R&lt;Boolean&gt;；data=true 表示切换成功（含幂等情况）；错误码：30000 / 70002 / 70014
     */
    R<Boolean> setMonitorEnabled(Long userId, Long tenantId, String secUid, Integer monitorType, Integer enabled);

    /**
     * 批量按 secUid 集合 + userId + tenantId 查询直播间用户配置（仅返回三开关相关字段）。
     *
     * <p>用于 reportStatus / batchReportStatus 装配 monitorEnabled 字段。结果仅包含
     * isRemoveRecord=0 的有效记录；同 secUid 跨租户不会污染。</p>
     *
     * @param secUids  主播唯一标识列表（去重，size 上限由调用方控制）
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return AnchorUrlUserVo 列表（含 anchorUrlSecUid + 三开关字段），空列表表示均无记录
     */
    R<List<AnchorUrlUserVo>> listBySecUidsAndUser(List<String> secUids, Long userId, Long tenantId);
}
