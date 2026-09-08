package com.jiuyu.replay.api.controller.power;

import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.logic.power.UserLogic;
import com.jiuyu.replay.api.utils.GetIPUtils;
import com.jiuyu.replay.common.annotation.AgentQueryUserCheck;
import com.jiuyu.replay.common.aspect.lock.repeatsubmit.NoRepeatSubmit;
import com.jiuyu.replay.common.repository.service.CacheFallbackDataService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.RegisterVo;
import com.jiuyu.replay.generic.vo.power.UserInfoExportVo;
import com.jiuyu.replay.order.bo.SubAccountListBo;
import com.jiuyu.replay.power.bo.*;
import com.jiuyu.replay.power.vo.*;
import com.jiuyu.replay.third.governance.GovernanceTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-02-26 10:00:04
 */
@RestController
@RequestMapping("replay/user")
@Tag(name = "用户")
@Slf4j
public class UserController {

    @Resource
    private UserLogic userLogic;
    @Autowired
    private CacheFallbackDataService cacheFallbackDataService;

    @Autowired
    private GovernanceTenantService governanceTenantService;

    /**
     * 获取平台运营用户列表
     *
     * @return
     */
    @Operation(summary = "获取平台运营用户列表")
    @GetMapping("/platformOperationList")
    public R<List<UserVo>> platformOperationList() {
        return this.userLogic.platformOperationList();
    }

    /**
     * 获取用户手机号
     *
     * @return
     */
    @Operation(summary = "获取用户手机号")
    @GetMapping("/getSelfPhone")
    public R<String> getSelfPhone() {

        return this.userLogic.getSelfPhone();
    }

    /**
     * 通过临时凭证登录
     *
     * @param tempToken 临时凭证
     * @return
     */
    @Operation(summary = "通过临时凭证登录")
    @GetMapping("/loginByTempToken")
    public R<UserLoginVo> loginByTempToken(@Parameter(description = "临时凭证", required = true) @RequestParam String tempToken, HttpServletRequest request) {

        String loginIp = GetIPUtils.getIpAddr(request);

        return this.userLogic.loginByTempToken(tempToken, loginIp);
    }

    /**
     * 获取登录的临时凭证
     *
     * @return
     */
    @Operation(summary = "获取登录的临时凭证")
    @GetMapping("/getLoginTempToken")
    public R<String> getLoginTempToken() {

        return this.userLogic.getLoginTempToken();
    }

    /**
     * 将Redis中的token信息全部加载到数据库
     *
     * @return
     */
    @Operation(summary = "将Redis中的token信息全部加载到数据库")
    @PostMapping("/loadRedisTokensToDatabase")
    public R<String> loadRedisTokensToDatabase() {
        try {
            String result = this.userLogic.loadRedisTokensToDatabase();
            return R.ok("同步操作完成", result);
        } catch (Exception e) {
            return R.error(500, "同步操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取验证码
     *
     * @param phone 手机号
     * @return
     */
    @Operation(summary = "获取验证码")
    @GetMapping("/getPhoneCode")
    public R<String> getPhoneCode(@RequestParam String phone) {

        return this.userLogic.getPhoneCode(phone);

    }

    /**
     * 校验验证码
     *
     * @param phone      手机号
     * @param code       验证码
     * @param isNewPhone 是否是新手机号 0：否 1：是
     * @return
     */
    @Operation(summary = "校验验证码")
    @GetMapping("/checkPhoneCode")
    public R<String> checkPhoneCode(@RequestParam String phone, @RequestParam String code, @RequestParam(required = false) Integer isNewPhone) {

        return this.userLogic.checkPhoneCode(phone, code, isNewPhone);

    }

    /**
     * 预约注册
     *
     * @param reqVo 数据
     * @return
     */
    @Operation(summary = "预约注册")
    @PostMapping("/registerAppointment")
    @NoRepeatSubmit(key = "#reqVo.phone")
    public R<RegisterVo> registerAppointment(@RequestBody @Valid RegisterReqVo reqVo) {
        return userLogic.registerAppointment(reqVo);
    }

    /**
     * 注册
     *
     * @param registerBo 数据
     * @return
     */
    @Operation(summary = "注册")
    @PostMapping("/register")
    @NoRepeatSubmit(key = "#registerBo.phone")
    public R<RegisterVo> register(@RequestBody @Validated(Insert.class) RegisterBo registerBo) {
        return this.userLogic.register(registerBo);
    }

    /**
     * 登录
     *
     * @param loginBo 数据
     * @return
     */
    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<UserLoginVo> login(@RequestBody LoginBo loginBo, HttpServletRequest request) {

        return this.userLogic.login(loginBo, request);

    }

    /**
     * 在线登录
     *
     * @param loginBo 数据
     * @return
     */
    @Operation(summary = "在线登录")
    @PostMapping("/loginOnline")
    public R<UserLoginVo> loginOnline(@RequestBody LoginBo loginBo, HttpServletRequest request) {

        return this.userLogic.loginOnline(loginBo, request);

    }


    /**
     * 登出
     *
     * @return
     */
    @Operation(summary = "登出")
    @GetMapping("/logout")
    public R<String> logout(HttpServletRequest request) {

        return this.userLogic.logout(request);

    }

    /**
     * 根据token获取用户信息
     *
     * @return
     */
    @Operation(summary = "根据token获取用户信息")
    @GetMapping("/getUserByToken")
    public R<UserVo> getUserByToken(HttpServletRequest request) {

        return this.userLogic.getUserByToken(request);

    }

    /**
     * 客户端修改密码
     *
     * @param updatePasswordBo 数据
     * @return
     */
    @PostMapping("/updatePasswordByClient")
    @Operation(summary = "客户端修改密码")
    public R<String> updatePasswordByClient(@RequestBody UpdatePasswordByClientBo updatePasswordBo) {

        return this.userLogic.updatePasswordByClient(updatePasswordBo);
    }

    /**
     * 修改密码
     *
     * @param updatePasswordBo 数据
     * @return
     */
    @PostMapping("/updatePassword")
    @Operation(summary = "修改密码")
    public R<String> updatePassword(@RequestBody UpdatePasswordBo updatePasswordBo) {

        return this.userLogic.updatePassword(updatePasswordBo);
    }

    /**
     * 用户列表
     *
     * @param userListBo 列表查询参数
     * @return
     */
    @PostMapping("/list")
    @Operation(summary = "列表")
    public R<PageUtils<UserListVo>> list(@Parameter(description = "列表查询参数", required = true) @RequestBody UserListBo userListBo) {

        return userLogic.selectClientList(userListBo);
    }

    @PostMapping("/listManage")
    @Operation(summary = "管理用户列表列表")
    public R<PageUtils<UserListManageVo>> listManage(@RequestBody UserListBo userListBo) {
        userListBo.setUserType(1);
        return userLogic.listManage(userListBo);
    }


    @PostMapping("/listAdmin")
    @Operation(summary = "后台管理员列表（默认支持分页，传limit -1 为不分页）")
    public R<PageUtils<UserVo>> listAdmin(@RequestBody UserListBo userListBo) {
        userListBo.setUserType(1);
        return userLogic.listAdmin(userListBo);
    }


    /**
     * 新增用户
     *
     * @param userAddBo 数据
     * @return
     */
    @Operation(summary = "新增用户")
    @PostMapping("/save")
    public R<String> save(@RequestBody UserAddBo userAddBo) {

        return this.userLogic.save(userAddBo);
    }

    /**
     * 客户端修改用户
     *
     * @param userUpdateClientBo 数据
     * @return
     */
    @Operation(summary = "修改用户")
    @PostMapping("/updateByClient")
    public R<UserInfoVo> updateByClient(@RequestBody UserUpdateClientBo userUpdateClientBo) {

        return this.userLogic.updateByClient(userUpdateClientBo);

    }

    /**
     * 后台修改用户
     *
     * @param userUpdateBo 数据
     * @return
     */
    @Operation(summary = "修改用户")
    @PostMapping("/update")
    public R<String> update(@RequestBody UserUpdateBo userUpdateBo) {

        return this.userLogic.update(userUpdateBo);
    }

    /**
     * 重置密码
     *
     * @param id 用户id
     * @return
     */
    @GetMapping("/resetPassword")
    @Operation(summary = "重置密码")
    public R<String> resetPassword(@Parameter(description = "用户id", required = true) @RequestParam("id") Long id) {

        return userLogic.resetPassword(id);
    }

    /**
     * 重置密码（随机六位数）
     *
     * @param id 用户id
     * @return
     */
    @GetMapping("/resetRandomPassword")
    @Operation(summary = "重置密码（随机六位数）")
    public R<String> resetRandomPassword(@Parameter(description = "用户id", required = true) @RequestParam("id") Long id) {

        return userLogic.resetRandomPassword(id);
    }

    /**
     * 客户端获取用户信息
     *
     * @return
     */
    @GetMapping("/infoByClient")
    @Operation(summary = "客户端获取用户信息")
    public R<UserInfoVo> infoByClient() {

        return userLogic.infoByClient();
    }

    @GetMapping("/userDetailByUserId")
    @Operation(summary = "获取用户详情")
    @AgentQueryUserCheck(checkUserId = "#args[0]")
    public R<UserDetailsInfoVo> userDetailByUserId(@Parameter(description = "用户id", required = true) @RequestParam("userId") Long userId) {
        return userLogic.userDetailByUserId(userId);
    }

    /**
     * 检查是否已经被冻结
     *
     * @return 0：未冻结 1：已冻结
     */
    @GetMapping("/checkFreeze")
    @Operation(summary = "检查是否已经被冻结")
    public R<Integer> checkFreeze() {

        return userLogic.checkFreeze();
    }

    /**
     * 信息
     *
     * @param id 用户id
     * @return
     */
    @GetMapping("/info")
    @Operation(summary = "信息")
    public R<UserInfoVo> info(@Parameter(description = "用户id", required = true) @RequestParam("id") Long id) {

        return userLogic.info(id, false);
    }

    /**
     * 删除
     *
     * @param id 用户id
     * @return
     */
    @GetMapping("/delete")
    @Operation(summary = "删除")
    public R<String> delete(@Parameter(description = "用户id", required = true) @RequestParam("id") Long id) {

        return userLogic.delete(id);
    }

    /**
     * 用户注册信息
     */
    @PostMapping("/userPropertyList")
    @Operation(summary = "用户资产信息）")
    public R<String> userPropertyList(@Parameter(description = "列表查询参数", required = true) @RequestBody UserListBo userListBo) {
        return userLogic.userPropertyList(userListBo);
    }


    /**
     * 条件分页查询用户列表
     *
     * @param userListBo
     * @return
     */
    @PostMapping("/pageList")
    @Operation(summary = "条件分页查询用户列表")
    public R<PageUtils<UserListVo>> pageList(@Parameter(description = "条件分页查询用户列表", required = true) @RequestBody UserListBo userListBo) {
        return userLogic.pageList(userListBo);
    }


    @PostMapping("/pageListNew")
    @Operation(summary = "条件分页查询用户列表-新方法(t+1)")
    public R<PageUtils<UserListVo>> pageListNew(@Parameter(description = "条件分页查询用户列表", required = true) @RequestBody UserListBo userListBo) {
        R<PageUtils<UserListVo>> response = userLogic.pageListNew(userListBo);
        if (EmptyUtil.isNotEmpty(response.getData()) && EmptyUtil.isNotEmpty(response.getData().getList())) {
            Complete.start(response.getData().getList())
                .build(UserListVo::getActiveTenantId, UserListVo::setGovernanceStatus, governanceTenantService::getTenantStatusForUserMap)
                .filter(user -> user.getPackageLevel() != null && user.getPackageLevel() >= 20 && user.getUserType() != null && user.getUserType() == 0)
                .then().over();
        }
        return response;
    }

    /**
     * 服务端冻结与解冻和用户
     */
    @PostMapping("/updateUser")
    @Operation(summary = "服务端冻结与解冻和用户")
    public R<String> updateUser(@RequestBody UserBo userBo) {
        return userLogic.updateUser(userBo);
    }

    /**
     * 服务端批量删除用户
     *
     * @param ids
     * @return
     */
    @PostMapping("/deleteByIds")
    @Operation(summary = "服务端批量删除用户")
    public R<String> deleteByIds(@RequestBody List<Long> ids) {
        return userLogic.deleteByIds(ids);
    }

    /**
     * 服务端修改用户账号类型
     */
    @Operation(summary = "服务端修改用户账号类型")
    @PostMapping("/updateByUserId")
    public R<String> updateByUserId(@RequestBody UserUpdateBo userUpdateBo) {

        return this.userLogic.updateByUserId(userUpdateBo);
    }

    /**
     * 服务端根据seu_uid获取用户信息
     */
    @PostMapping("/selectByuseId")
    public R<PageUtils<UserListVo>> selectByuseId(@RequestBody UserListBo userListBo) {
        return userLogic.selectByuseId(userListBo);
    }

    @GetMapping("/updateTrade")
    @Operation(summary = "更新用户行业")
    public R<String> updateTrade(Long userId, Long tradeId) {
        return userLogic.updateTrade(userId, tradeId);
    }

    /**
     * 用户列表
     *
     * @return
     */
    @PostMapping("/subAccountList")
    @Operation(summary = "子用户列表")
    public R<PageUtils<UserListVo>> subAccountList(@RequestBody SubAccountListBo bo) {
        return userLogic.subAccountList(bo);
    }

    /**
     * 导出新进用户信息
     *
     * @param userListBo
     * @return
     */
    @PostMapping("/exportUserInfoList")
    @Operation(summary = "导出新进用户信息")
    public R<List<UserInfoExportVo>> exportUserInfoList(@RequestBody UserListBo userListBo) {
        return userLogic.exportUserInfoList(userListBo);
    }

}
