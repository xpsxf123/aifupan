package com.jiuyu.replay.power.api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.power.AgentUserAddBo;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.SalesFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.RandomStringGenerator;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.UserPass;
import com.jiuyu.replay.power.bo.UserAddBo;
import com.jiuyu.replay.power.bo.UserDetailsBo;
import com.jiuyu.replay.power.bo.UserLoginLogBo;
import com.jiuyu.replay.power.bo.UserUpdateBo;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.entity.UserTokenEntity;
import com.jiuyu.replay.power.producer.*;
import com.jiuyu.replay.power.rse.UserRse;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午4:43
 */
@Component
@AllArgsConstructor
@Slf4j
public class UserApi implements UserFeign {

    private final TenantProducer tenantProducer;
    private final UserProducer userProducer;
    private final UserRse userRse;
    private final UserDetailsProducer userDetailsProducer;
    private final RoleProducer roleProducer;
    private final UserLoginLogProducer userLoginLogProducer;
    private final UserTokenProducer userTokenProducer;
    private final SalesFeign salesFeign;
    private final SystemKvProducer systemKvProducer;

    @Override
    public R<com.jiuyu.replay.generic.vo.power.UserCacheVo> getLocalUser() {

        UserCacheVo localUser = GlobalObject.getLocalUser();

        if (localUser != null) {
            com.jiuyu.replay.generic.vo.power.UserCacheVo userCacheVo = BeanUtil.copyProperties(localUser, com.jiuyu.replay.generic.vo.power.UserCacheVo.class);
            return R.ok(userCacheVo);
        }

        return R.error(3001, "缓存中不存在用户信息");
    }

    @Override
    public R<Long> getUserParentId(Long userId) {

        UserVo vo = userProducer.getById(userId);
        if(vo != null) {
            Long parentId = vo.getParentId();
            return R.ok(parentId != null && parentId == 0 ? null : parentId);
        }
        return R.ok();
    }

    @Override
    public Long getUserIdOrParentId(Long userId) {
        Long id = null;
        UserVo vo = userProducer.getById(userId);
        if (vo != null) {
            id = vo.getId();
            if (vo.getParentId() != null && vo.getParentId() != 0) {
                id = vo.getParentId();
            }
        } else {
            RRException.create("用户获取失败");
        }
        return id;
    }

    @Override
    public R<Long> getUserTenantId(Long userId) {
        UserVo vo = userProducer.getById(userId);
        if (vo != null) {
            return R.ok(vo.getActiveTenantId());
        }
        return R.ok(0L);
    }

    @Override
    public R<List<Long>> listByNameOrPhone(String keyword) {

        List<Long> userIds = userProducer.listIdsByLikePhoneOrName(keyword);
        return R.ok(userIds);
    }

    @Override
    public Long getCurrentUserParentId() {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (Objects.isNull(localUser)) {
            return null;
        }
        return localUser.getUserType() == 2 ? localUser.getParentId() : localUser.getId();
    }

    @Override
    public List<UserDto> listByIds(Collection<Long> ids) {

        List<UserListVo> userListVos = userProducer.listByIds(ids);
        return BeanConvertUtils.convertList(userListVos, UserDto.class);
    }

    @Override
    public UserDto userById(Long userId) {
        UserVo userVo = userProducer.getById(userId);
        if (userVo != null){
            return BeanUtil.copyProperties(userVo, UserDto.class);
        }
        return null;
    }

    @Override
    public com.jiuyu.replay.generic.vo.power.UserDetailsInfoVo getUserDetailByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDetailsInfoVo detail = userDetailsProducer.getDetailByUserId(userId);
        return detail != null ? BeanUtil.copyProperties(detail, com.jiuyu.replay.generic.vo.power.UserDetailsInfoVo.class) : null;
    }

    @Override
    public List<Long> listUserIdsAll() {
        return userProducer.listUserId();
    }

    @Override
    public List<Long> listUserByTenantId(Long tenantId) {
        if (tenantId == null) {
            return new ArrayList<>();
        }
        return tenantProducer.listUserByTenantId(tenantId);
    }

    @Override
    public UserDto getByPhoneAndType(String phone, Integer userType) {
        UserVo userVo = userProducer.getByPhoneAndType(phone, userType);
        if (userVo != null) {
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(userVo, userDto);
            return userDto;
        }
        return null;
    }

    @Override
    public UserPass saveOrUpdateAgentUser(AgentUserAddBo bo) {
        UserPass user = new UserPass();
        if (bo.getUserId() == null) {

            UserVo userCheck = userProducer.getByPhoneAndType(bo.getPhone(), UserEnums.userType.MANAGE_ADMIN_USER.getCode());
            if (ObjectUtil.isNotEmpty(userCheck)) {
                if (Objects.equals(bo.getAdminUserType(), userCheck.getAdminUserType())) {
                    SalesInfoVo sales = salesFeign.getBySalesUserId(userCheck.getId());
                    if (ObjectUtil.isNotEmpty(sales)) {
                        throw new BusinessException(StatusCode.PARAM_EX.getCode(), "该手机号的后台用户已存在，并且已关联销售");
                    } else {
                        bo.setUserId(userCheck.getId());
                    }
                } else {
                    throw new BusinessException(StatusCode.PARAM_EX.getCode(), "该手机号的后台用户已存在并且用户类型不一致");
                }
            }

            if (bo.getUserId() == null) {
                // 创建用户
                UserAddBo userAddBo = BeanUtil.copyProperties(bo, UserAddBo.class);
                userAddBo.setUsername(String.valueOf(SnowflakeManager.nextValue()));
                // 创建用户
                userAddBo.setPassword(RandomStringGenerator.generateRandomString());
                UserVo userDto = userProducer.save(userAddBo);
                user.setId(userDto.getId());
                user.setPass(userAddBo.getPassword());


                // 获取代理商角色ID并分配角色
                List<Long> adminUserRoleId = getAdminUserRoleId(bo.getAdminUserType());
                if (ObjectUtil.isNotEmpty(adminUserRoleId)) {
                    roleProducer.addUserRoles(userDto.getId(), adminUserRoleId);
                }

                // 创建用户详情
                UserDetailsBo userDetailsBo = new UserDetailsBo();
                userDetailsBo.setUserId(userDto.getId());
                userDetailsBo.setTradeId(bo.getTradeId());
                userDetailsBo.setChannelId(bo.getChannelId());
                userDetailsBo.setSaleId(bo.getSaleId());
                userDetailsBo.setAgentId(bo.getAgentId());
                // 创建用户详情
                userDetailsProducer.save(userDetailsBo);
            }
        }

        // 修改
        if (bo.getUserId() != null) {
            user.setId(bo.getUserId());
            // 校验
            long count = userProducer.countByAdminUser(bo.getPhone(), bo.getUserId());
            if (count > 0) {
                throw new BusinessException("登录用户已存在，修改用户失败");
            }
            UserUpdateBo userUpdateBo = BeanUtil.copyProperties(bo, UserUpdateBo.class);
            userUpdateBo.setId(bo.getUserId());
            userProducer.update(userUpdateBo);

            // 获取代理商角色ID并分配角色
            List<Long> adminUserRoleId = getAdminUserRoleId(bo.getAdminUserType());
            if (ObjectUtil.isNotEmpty(adminUserRoleId)) {
                roleProducer.addUserRoles(bo.getUserId(), adminUserRoleId);
            }

            // 更新用户详情
            UserDetailsInfoVo userDetails = userDetailsProducer.getByUserId(bo.getUserId());
            UserDetailsBo userDetailsBo = new UserDetailsBo();
            userDetailsBo.setUserId(bo.getUserId());
            userDetailsBo.setTradeId(bo.getTradeId());
            userDetailsBo.setChannelId(bo.getChannelId());
            userDetailsBo.setSaleId(bo.getSaleId());
            userDetailsBo.setAgentId(bo.getAgentId());
            if (userDetails != null) {
                userDetailsBo.setId(userDetails.getId());
                userDetailsProducer.update(userDetailsBo);
            } else {
                // 如果用户详情不存在，创建新的
                userDetailsProducer.save(userDetailsBo);
            }
        }
        return user;
    }

    /**
     * 获取代理商销售用户角色id
     *
     * @return 角色id
     */
    private List<Long> getAdminUserRoleId(Integer adminUserType) {
        SystemKvInfoVo kv = null;
        if (adminUserType == UserEnums.adminUserType.ADMIN.getCode()) {
            kv = systemKvProducer.getByKey("platform_sales_user_role_id");
        } else if (adminUserType == UserEnums.adminUserType.AGENT.getCode()) {
            kv = systemKvProducer.getByKey("agent_user_role_id");
        }
        if (adminUserType == UserEnums.adminUserType.AGENT_SALE.getCode()) {
            kv = systemKvProducer.getByKey("agent_sales_user_role_id");
        }
        if (kv == null || ObjectUtil.isEmpty(kv.getKvValue())) {
            return List.of();
        }
        // 切割-转为long-判空
        return Arrays.stream(kv.getKvValue().split(","))
                .map(item -> NumberUtil.parseLong(item, null))
                .filter(ObjectUtil::isNotEmpty)
                .toList();
    }

    @Override
    public void updateAdminUserStatusByPhone(String contactPhone, Integer agentStatus) {
        UserVo user = userProducer.getByPhoneAndType(contactPhone, UserEnums.userType.MANAGE_ADMIN_USER.getCode());

        if (user == null) {
            log.warn("[更新用户的状态] 没有查询到用户，跳过");
            return;
        }

        UserUpdateBo userUpdateBo = new UserUpdateBo();
        userUpdateBo.setId(user.getId());
        userUpdateBo.setStatus(agentStatus);
        boolean isSuccess = userProducer.update(userUpdateBo);
        List<UserEntity> removeUserTokenList = new ArrayList<>();
        if (isSuccess && agentStatus == 0) {
            // 删除登录token
            UserEntity e = new UserEntity();
            e.setId(user.getId());
            e.setNickName(user.getNickName());
            e.setUsername(user.getUsername());
            e.setUserType(user.getUserType());
            e.setPhone(user.getPhone());
            removeUserTokenList.add(e);
        }

        if (user.getUserType() == UserEnums.userType.MANAGE_ADMIN_USER.getCode() && user.getAdminUserType() == UserEnums.adminUserType.AGENT.getCode()) {
            // 如果是代理商，则一起更新代理商用户的子账号
            removeUserTokenList.addAll(userProducer.updateStatusByParentId(user.getParentId(), agentStatus));
        }

        // 删除token
        if (ObjectUtil.isNotEmpty(removeUserTokenList)) {

            Map<Long, UserEntity> userMap = removeUserTokenList.stream()
                    .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));

            List<UserTokenEntity> userTokenEntities = userProducer.tokenListByUserIds(new ArrayList<>(userMap.keySet()));
            if (ObjectUtil.isEmpty(userTokenEntities)) {
                return;
            }

            for (UserTokenEntity userTokenEntity : userTokenEntities) {
                UserEntity userEntity = userMap.get(userTokenEntity.getUserId());
                if (userEntity == null) {
                    continue;
                }
                // 添加登录日志
                UserCacheVo vo = new UserCacheVo();
                vo.setId(userEntity.getId());
                vo.setPhone(userEntity.getPhone());
                vo.setNickName(userEntity.getNickName());
                vo.setUsername(userEntity.getUsername());
                vo.setUserType(userEntity.getUserType());
                UserLoginLogBo bo = new UserLoginLogBo(vo, "127.0.0.1", "用户被冻结，强制退出", 3, 0);
                userLoginLogProducer.save(bo);
                log.info("退出登录，添加退出登录日志成功， token={}", userTokenEntity.getToken());
                // 删除缓存
                userTokenProducer.removeUserToken(userTokenEntity.getToken(), userEntity.getId());
            }
        }
    }

    @Override
    public Long getSubUserCountByUserId(Long userId) {
        return userProducer.getSubUserCountByUserId(userId);
    }

    @Override
    public List<UserDto> listByPhones(List<String> phoneList) {
        return userProducer.listByPhones(phoneList);
    }

    /**
     * 获取用户类型
     *
     * @param userIds 用户id列表
     *
     * @return 用户类型
     */
    @Override
    public Map<Long, Integer> getUserTypeMap(List<Long> userIds) {
        return userProducer.getUserTypeMap(userIds);
    }

    @Override
    public UserDto parentUserByUserId(Long userId) {

        UserVo vo = userProducer.getById(userId);

        if (vo == null) {
            return null;
        }

        if (vo.getParentId() != null && vo.getParentId() == 0) {
            return BeanUtil.copyProperties(vo, UserDto.class);
        }

        UserVo vo1 = userProducer.getById(vo.getParentId());

        if (vo1 == null) {
            throw new BusinessException("数据错误，获取获取失败，userId = " + vo.getParentId());
        }
        return BeanUtil.copyProperties(vo1, UserDto.class);
    }

    @Override
    public Long getUserIdByTenantId(Long tenantId) {
        TenantInfoVo info = tenantProducer.info(tenantId);
        if (ObjectUtil.isNotEmpty(info)) {
            return info.getUserId();
        }
        return null;
    }

    @Override
    public void saveClientVersion(Long userId, String clientVersion) {
        userRse.saveClientVersion(userId, clientVersion);
    }

    @Override
    public String getClientVersion(Long userId) {
        return userRse.getClientVersion(userId);
    }
}
