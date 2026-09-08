package com.jiuyu.replay.words.bo.video;

import com.jiuyu.replay.generic.dto.power.UserDto;

/**
 * 视频归属于用户
 *
 * @author HeHui
 * @date 2025-07-05 17:27
 */
public record VideoBelongUserBO(long userId, Long parentId, Integer userType, Long tenantId, boolean uploadStatus, String videoId) {


    /**
     * 创建一个包含用户ID和租户ID的VideoBelongUserBO实例
     * 此方法用于当需要同时指定用户ID和租户ID时使用
     *
     * @param userId   用户ID，用于标识用户
     * @param tenantId 租户ID，用于标识用户所属的租户
     * @param uploadStatus 上传状态，用于标识视频是否已经上传
     * @param videoId 视频ID，用于标识视频
     *
     * @return 返回一个包含指定用户ID和租户ID的VideoBelongUserBO实例，其他属性为null
     */
    public static VideoBelongUserBO of(long userId, long tenantId, boolean uploadStatus, String videoId) {
        return new VideoBelongUserBO(userId, null, null, tenantId, uploadStatus, videoId);
    }


    public VideoBelongUserBO full(int userType, Long parentId, long tenantId) {
        return new VideoBelongUserBO(userId, parentId, userType, this.tenantId == null ? tenantId : this.tenantId, uploadStatus, null);
    }

    public VideoBelongUserBO full(UserDto dto) {
        return this.full(dto.getUserType(), dto.getParentId(), tenantId == null ? dto.getActiveTenantId() : tenantId);
    }
}
