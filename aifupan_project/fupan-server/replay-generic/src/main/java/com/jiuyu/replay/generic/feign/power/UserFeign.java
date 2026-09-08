package com.jiuyu.replay.generic.feign.power;

import com.jiuyu.replay.generic.bo.power.AgentUserAddBo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.power.UserDetailsInfoVo;
import com.jiuyu.replay.generic.vo.power.UserPass;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午4:44
 */
public interface UserFeign {

    R<UserCacheVo> getLocalUser();

    /**
     * 获取用户上级id
     *
     * @param userId
     * @return
     */
    R<Long> getUserParentId(Long userId);

    /**
     * 获取用户id或者上级id-如果单前账号是子账号就是上级的userId
     *
     * @param userId
     * @return
     */
    Long getUserIdOrParentId(Long userId);

    /**
     * 获取用户租户id
     *
     * @param userId
     * @return
     */
    R<Long> getUserTenantId(Long userId);

    /**
     * 根据用户昵称或手机号获取用户列表
     * @param keyword 用户昵称或手机号
     * @return
     */
    R<List<Long>> listByNameOrPhone(String keyword);

    /**
     * 获取当前用户的父级id
     *
     * @return 1-如果当前账号是主账号直接返回本身用户id 2-如果是子账号获取父级主账号用户id 3-如果用户未登录或其它情况直接返回null
     */
    Long getCurrentUserParentId();

    /**
     * 获取用户信息通过ids
     *
     * @param ids
     * @return
     */
    List<UserDto> listByIds(Collection<Long> ids);

    /**
     * 获取用户信息
     * @param userId
     */
    UserDto userById(Long userId);

    /**
     * 获取用户详情
     *
     * @param userId 用户id
     * @return 用户详情
     */
    UserDetailsInfoVo getUserDetailByUserId(Long userId);

    /**
     * 获取用户ids根据租户id
     *
     * @param tenantId 租户id
     * @return
     */
    List<Long> listUserByTenantId(Long tenantId);

    /**
     * 获取所有用户id
     *
     * @return
     */
    List<Long> listUserIdsAll();

    /**
     * 根据手机号和用户类型查找用户
     *
     * @param phone    手机号
     * @param userType 用户类型
     * @return
     */
    UserDto getByPhoneAndType(String phone, Integer userType);

    /**
     * 创建或修改代理商用户
     *
     * @param bo 代理商用户信息
     */
    UserPass saveOrUpdateAgentUser(AgentUserAddBo bo);

    /**
     * 根据手机号修改管理员状态
     *
     * @param contactPhone 手机号
     * @param agentStatus  代理商状态 0：未启用 1：启用中
     */
    void updateAdminUserStatusByPhone(String contactPhone, Integer agentStatus);

    /**
     * 获取子账号数量
     *
     * @param userId 用户id
     * @return 子账号数量
     */
    Long getSubUserCountByUserId(Long userId);

    /**
     * 根据手机号查询用户
     *
     * @param phoneList 手机号列表
     * @return 用户列表
     */
    List<UserDto> listByPhones(List<String> phoneList);


    /**
     * 获取用户类型
     *
     * @param userIds 用户id列表
     * @return 用户类型
     */
    Map<Long, Integer> getUserTypeMap(List<Long> userIds);

    /**
     * 根据用户id获取父级用户
     *
     * @param userId 用户id
     * @return 父级用户
     */
    UserDto parentUserByUserId(Long userId);

    /**
     * 根据租户id获取对应的用户id
     *
     * @param tenantId 租户id
     * @return 用户id
     */
    Long getUserIdByTenantId(Long tenantId);

    /**
     * 设置用户的客户端版本
     *
     * @param userId
     * @param clientVersion 客户端版本
     */
    void saveClientVersion(Long userId, @NotNull(message = "版本不能为空") String clientVersion);

    /**
     * 获取客户端版本
     *
     * @param userId 用户
     * @return 客户端版本
     */
    String getClientVersion(Long userId);
}
