package com.jiuyu.replay.common.bo;

import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.vo.power.UserVo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据归属用户
 *
 * @param parentId 上级用户id
 * @param userId   用户id
 * @param userType 用户类型 0：普通用户 1：后台管理员 2：子账号
 * @param tenantId 租户id
 *
 * @author HeHui
 * @date 2025-06-07 14:51
 */
public record BelongUserBO(long userId, Long parentId, Integer userType, Long tenantId) implements Serializable {
    @Serial
    private static final long serialVersionUID = -7544896364020281732L;


    /**
     * 创建一个仅包含用户ID的BelongUserBO实例
     * 此方法用于当只需要指定用户ID，而其他信息不重要或未提供时
     *
     * @param userId 用户ID，用于标识用户
     *
     * @return 返回一个包含指定用户ID的BelongUserBO实例，其他属性为null
     */
    public static BelongUserBO of(long userId) {
        return new BelongUserBO(userId, null, null, null);
    }

    /**
     * 创建一个包含用户ID和租户ID的BelongUserBO实例
     * 此方法用于当需要同时指定用户ID和租户ID时使用
     *
     * @param userId   用户ID，用于标识用户
     * @param tenantId 租户ID，用于标识用户所属的租户
     *
     * @return 返回一个包含指定用户ID和租户ID的BelongUserBO实例，其他属性为null
     */
    public static BelongUserBO of(long userId, long tenantId) {
        return new BelongUserBO(userId, null, null, tenantId);
    }

    /**
     * 根据用户视图创建用户归属业务对象
     * 该方法用于将用户视图中的相关信息转换为用户归属业务对象，以便在业务逻辑中使用
     *
     * @param user 用户包含用户的基本信息
     *
     * @return 返回一个包含用户归属信息的业务对象
     */
    public static BelongUserBO of(UserVo user) {
        return new BelongUserBO(user.getId(), user.getParentId(), user.getUserType(), user.getActiveTenantId());
    }

    /**
     * 创建一个包含所有信息的BelongUserBO实例
     * 此方法用于当需要完整指定用户类型、父ID和可能的租户ID时使用
     * 如果当前实例的租户ID为空，则使用提供的租户ID；否则保留当前实例的租户ID
     *
     * @param userType 用户类型，用于分类用户角色或权限
     * @param parentId 父ID，用于表示用户之间的层级关系，可能为null
     * @param tenantId 租户ID，用于在父ID为null时为新实例提供租户ID
     *
     * @return 返回一个完整信息的BelongUserBO实例
     */
    public BelongUserBO full(int userType, Long parentId, long tenantId) {
        return new BelongUserBO(userId, parentId, userType, this.tenantId == null ? tenantId : this.tenantId);
    }




    /**
     * 创建一个包含完整用户归属信息的业务对象
     * 该方法重载了full方法，允许通过单独的参数来创建用户归属业务对象
     *
     * @param user 用户包含用户的基本信息
     *
     * @return 返回一个包含用户归属信息的业务对象
     */
    public BelongUserBO full(UserVo user) {
        return this.full(user.getUserType(), user.getParentId(), user.getActiveTenantId());
    }

    public BelongUserBO full(UserDto dto) {
        return this.full(dto.getUserType(), dto.getParentId(), tenantId == null ? dto.getActiveTenantId() : tenantId);
    }
}
