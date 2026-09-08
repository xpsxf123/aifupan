package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.api.controller.openapi.governance.response.OpenGovernanceUserDetailsInfo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.RegisterVo;
import com.jiuyu.replay.generic.vo.power.UserInfoExportVo;
import com.jiuyu.replay.order.bo.SubAccountListBo;
import com.jiuyu.replay.power.bo.*;
import com.jiuyu.replay.power.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

public interface UserLogic {

    /**
     * 登录
     * @param loginBo 数据
     * @return
     */
    R<UserLoginVo> login(LoginBo loginBo, HttpServletRequest request);

    /**
     * 修改密码
     * @param updatePasswordBo 数据
     * @return
     */
    R<String> updatePassword(UpdatePasswordBo updatePasswordBo);

    /**
     * 用户列表
     * @param userListBo 菜单列表查询参数
     * @return
     */
    R<PageUtils<UserListVo>> selectClientList(UserListBo userListBo);

    /**
     * 重置密码
     * @param id 用户id
     * @return
     */
    R<String> resetPassword(Long id);

    /**
     * 信息
     *
     * @param id           用户id
     * @param loadPassword 是否返回密码
     *
     * @return
     */
    R<UserInfoVo> info(Long id, boolean loadPassword);

    /**
     * 删除
     * @param id 用户id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 注册
     * @param registerBo 数据
     * @return
     */
    R<RegisterVo> register(RegisterBo registerBo);

    /**
     * 获取验证码
     * @param phone 手机号
     * @return
     */
    R<String> getPhoneCode(String phone);

    /**
     * 新增用户
     * @param userAddBo 数据
     * @return
     */
    R<String> save(UserAddBo userAddBo);

    /**
     * 修改用户
     * @param userUpdateBo 数据
     * @return
     */
    R<String> update(UserUpdateBo userUpdateBo);

    /**
     * 客户端修改用户
     * @param userUpdateClientBo 数据
     * @return
     */
    R<UserInfoVo> updateByClient(UserUpdateClientBo userUpdateClientBo);


    /**
     * 修改手机号
     *
     * @param userId    用户id
     * @param newMobile 新手机号
     * @param tenantId 租户id
     *
     * @return {@link R }<{@link Void }>
     */
    R<Void> updateMobile(long userId, String newMobile,  Long tenantId);

    /**
     * 客户端获取用户信息
     * @return
     */
    R<UserInfoVo> infoByClient();

    /**
     * 校验验证码
     * @param phone 手机号
     * @param code 验证码
     * @param isNewPhone 是否是新手机号 0：否 1：是
     * @return
     */
    R<String> checkPhoneCode(String phone, String code, Integer isNewPhone);

    /**
     * 登出
     * @return
     */
    R<String> logout(HttpServletRequest request);

    /**
     * 用户资产信息
     *
     * @param userListBo
     * @return
     */
    R<String> userPropertyList(UserListBo userListBo);

    /**
     * 条件分页查询用户列表
     * @param userListBo
     * @return
     */
    R<PageUtils<UserListVo>> pageList(UserListBo userListBo);

    /**
     * 条件分页查询用户列表
     *
     * @param userListBo
     * @return
     */
    R<PageUtils<UserListVo>> pageListNew(UserListBo userListBo);

    /**
     * 服务端冻结与解冻和用户
     * @param staust
     * @return
     */
    R<String> updateUser(UserBo staust);

    /**
     * 服务端批量删除用户
     * @param ids
     * @return
     */
    R<String> deleteByIds(List<Long> ids);

    /**
     * 服务端修改用户账号类型
     * @param userUpdateBo
     * @return
     */
    R<String> updateByUserId(UserUpdateBo userUpdateBo);

    /**
     * 服务端根据seu_uid获取用户信息
     * @param userListBo
     * @return
     */
    R<PageUtils<UserListVo>> selectByuseId(UserListBo userListBo);

    /**
     * 检查是否已经被冻结
     * @return 0：未冻结 1：已冻结
     */
    R<Integer> checkFreeze();

    /**
     * 根据token获取用户信息
     * @return
     */
    R<UserVo> getUserByToken(HttpServletRequest request);

    /**
     * 更新用户行业
     * @param userId
     * @param tradeId
     * @return
     */
    R<String> updateTrade(Long userId, Long tradeId);

    /**
     * 添加子账号
     *
     * @param aSubAccountBo@return
     */
    R<String> setUpASubAccount(bindingSubAccountBo aSubAccountBo, boolean verifyCode);

    /**
     * 添加子账号 并返回userId
     *
     */
    R<Long> setUpASubAccountResultUserId(bindingSubAccountBo aSubAccountBo, boolean verifyCode);

    /**
     * 解除子账号
     * @param aSubAccountBo
     * @return
     */
    R<String> unbindingSubAccount(bindingSubAccountBo aSubAccountBo);

    /**
     * 子账号列表
     * @param bo
     * @return
     */
    R<PageUtils<UserListVo>> subAccountList(SubAccountListBo bo);

    /**
     * 根据userId获取用户详情
     * @param userId
     * @return
     */
    R<UserDetailsInfoVo> userDetailByUserId(Long userId);

    /**
     * 在线登录
     * @param loginBo 数据
     * @return
     */
    R<UserLoginVo> loginOnline(LoginBo loginBo, HttpServletRequest request);

    /**
     * 管理员列表
     * @param userListBo
     * @return
     */
    R<PageUtils<UserListManageVo>> listManage(UserListBo userListBo);


    R<List<UserInfoExportVo>> exportUserInfoList(UserListBo userListBo);

    /**
     * 获取登录的临时凭证
     * @return
     */
    R<String> getLoginTempToken();

    /**
     * 通过临时凭证登录
     * @param tempToken 临时凭证
     * @param loginIp 登录ip
     * @return
     */
    R<UserLoginVo> loginByTempToken(String tempToken, String loginIp);

    /**
     * 客户端修改密码
     * @param updatePasswordBo 数据
     * @return
     */
    R<String> updatePasswordByClient(UpdatePasswordByClientBo updatePasswordBo);

    /**
     * 更新密码 从企业后台同步
     *
     * @param userId    用户ID
      * @param rwaPassword  密码
     * @param tenantId  租户ID
     *
     * @return {@link R }<{@link Void }>
     */
    R<Void> updatePasswordByGovernance(long userId,String rwaPassword, long tenantId);

    /**
     * 获取用户手机号
     * @return
     */
    R<String> getSelfPhone();

    /**
     * 获取平台运营用户列表
     * @return
     */
    R<List<UserVo>> platformOperationList();

    /**
     * 将Redis中的token信息全部加载到数据库
     *
     * @return 同步结果信息
     */
    String loadRedisTokensToDatabase();

    /**
     * 获取后台管理员列表
     * @param userListBo
     * @return
     */
    R<PageUtils<UserVo>> listAdmin(UserListBo userListBo);

    /**
     * 重置密码（随机六位数）
     * @param id
     * @return
     */
    R<String> resetRandomPassword(Long id);

    /**
     * 客户端登录后的接口
     */
    void clientLogoPost(String clientVersion);


    /**
     * 根据手机号获取用户id
     *
     * @param phone 手机号
     *
     * @return {@link Optional }<{@link Long }>
     */
    Optional<Long> getPhoneUserId(String phone);

    /**
     * 根据手机号获取用户信息
     *
     * @param phone 手机号
     *
     * @return {@link R }<{@link OpenGovernanceUserDetailsInfo }>
     */
    R<OpenGovernanceUserDetailsInfo> getMobileAccount(String phone);


    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     *
     * @return {@link R }<{@link OpenGovernanceUserDetailsInfo }>
     */
    R<OpenGovernanceUserDetailsInfo> getMobileAccount(long userId);

    /**
     * 获取租户下所有用户列表
     *
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link List<OpenGovernanceUserDetailsInfo> }>
     */
    R<List<OpenGovernanceUserDetailsInfo>> getAllUserList(long tenantId);

    /**
     * 预约注册
     * @param reqVo 数据
     * @return
     */
    R<RegisterVo> registerAppointment(RegisterReqVo reqVo);

}
