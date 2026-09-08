package com.jiuyu.governance.business.rbac.service;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.rbac.pojo.bo.MobileLoginEmployee;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.request.*;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeInfoResponse;
import com.jiuyu.governance.business.rbac.pojo.response.EmployeeUnbindResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.openfeign.replay.response.ReplayUserDetailsInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 员工服务接口
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface EmployeeService {



    /**
     * 手机号码是否被占用(可用租户下)
     *
     * @param mobile     手机号码
     * @param excludeId  排除ID
     *
     * @return {@link Boolean}
     */
    boolean mobileOccupy(String mobile, Long excludeId);

    /**
     * 租户下手机号码是否被占用
     *
     * @param tenantId   租户ID
     * @param mobile     手机号码
     * @param excludeId  排除ID
     *
     * @return {@link Boolean}
     */
    boolean tenantMobileOccupy(long tenantId, String mobile, Long excludeId);


    /**
     * 客户用户是否被占用
     *
     * @param clientUserId 客户用户ID
     * @param excludeId    排除ID
     *
     * @return {@link Boolean}
     */
    boolean clientUserExist(long clientUserId, Long excludeId);

    /**
     * 新增人员
     *
     * @param request    人员新增请求
     * @param accessUser 访问用户
     */
    ApiResponse<Void> addEmployee(EmployeeAddRequest request, AccessUser accessUser);


    /**
     * 基于客户端账户创建人员
     *
     * @param account 客户端账户
     *
     * @return {@link ApiResponse<Void>}
     */
    ApiResponse<Void> addForReplayAccount(ReplayUserDetailsInfo account);

    /**
     * 修改人员
     *
     * @param request    人员修改请求
     * @param accessUser 访问用户
     */
    ApiResponse<Void> updateEmployee(EmployeeUpdateRequest request, AccessUser accessUser);

    /**
     * 人员详情
     *
     * @param id       人员ID
     * @param tenantId 租户ID
     *
     * @return {@link EmployeeInfoResponse}
     */
    EmployeeInfoResponse detail(long id, long tenantId);

    /**
     * 分页查询人员
     *  排除主账户
     * @param request    人员查询请求
     * @param accessUser 访问用户
     *
     * @return {@link PageData<EmployeeInfoResponse>}
     */
    PageData<EmployeeInfoResponse> pageQueryEmployee(EmployeeQueryRequest request, AccessUser accessUser);

    /**
     * 开启录制权限
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    ApiResponse<Void> onRec(Long id, AccessUser accessUser);

    /**
     * 关闭录制权限
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    ApiResponse<Void> offRec(Long id, AccessUser accessUser);

    /**
     * 同步子账户
     *
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse<Void>}
     */
    ApiResponse<Void> syncSubAccount(AccessUser accessUser);

    /**
     * 启用人员
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    ApiResponse<Void> enable(long id, AccessUser accessUser);

    /**
     * 禁用人员
     *
     * @param id         人员ID
     * @param accessUser 访问用户
     */
    ApiResponse<Void> disable(long id, AccessUser accessUser);


    /**
     * 搜索下拉
     *
     * @param searchRequest 搜索请求
     * @param accessUser   访问用户
     *
     * @return {@link List<LabelOption>}
     */
    List<LabelOption> search(EmployeeOptionSearchRequest searchRequest, AccessUser accessUser);


    /**
     * 获取租户的主账户ID
     *
     * @param tenantId 租户ID
     *
     * @return {@link Optional<Long>}
     */
    Optional<Long> getTenantMainAccountId(long tenantId);

    /**
     * 主账户是否存在
     *
     * @param mainAccountIds 主账户ID列表
     *
     * @return {@link List<Long>}
     */
    List<Long> mainAccountExist(List<Long> mainAccountIds);


    /**
     * 获取岗位员工数量
     *
     * @param positionIds 岗位ID列表
     *
     * @return {@link Map<Long, Long>}
     */
    Map<Long, Long> countPositionEmployee(List<Long> positionIds);

    /**
     *  登录场景下特殊根据手机号码查询逻辑（返回租户可用的的员工账户）
     *
     * @param mobile 手机号码
     *
     * @return {@link Optional }<{@link MobileLoginEmployee }>
     */
    Optional<MobileLoginEmployee> getMobileLoginAccount(String mobile);


    /**
     * 获取员工手机号码
     *
     * @param mobile 手机号码
     * @param tenantId   租户ID
     * @return {@link Optional<Employee>}
     */
    Optional<Employee> getMobileEmployee(String mobile, long tenantId);

    /**
     * 获取员工角色ID列表
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link List<Long>}
     */
    List<Long> getEmployeeRoleIds(long employeeId, long tenantId);

    /**
     * 删除员工
     *
     * @param employeeId 员工ID
     * @param accessUser 访问用户
     */
    ApiResponse<Void> delete(long employeeId, AccessUser accessUser);

    /**
     * 获取员工名称映射
     *
     * @param ids 员工ID列表
     *
     * @return {@link Map<Long, String>}
     */
    Map<Long, String> getNameMap(List<Long> ids);



    /**
     * 修改密码
     *
     * @param employeeId 员工ID
     * @param password   密码
     * @param tenantId   租户ID
     * @param sync       是否同步
     */
    ApiResponse<Void> updatePassword(long employeeId, String password, long tenantId, boolean sync);


    /**
     * 获取员工信息
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link Optional<Employee>}
     */
    Optional<Employee> getEmployeeInfo(long employeeId, long tenantId);


    /**
     * 获取员工手机号码
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link Optional<String>}
     */
    Optional<String> getEmployeeMobile(long employeeId, long tenantId);

    /**
     * 修改手机号码
     *
     * @param employeeId 员工ID
     * @param newMobile  新手机号码
     * @param tenantId   租户ID
     *
     * @return {@link ApiResponse<Void>}
     */
    ApiResponse<Void> updateMobile(long employeeId, String newMobile, long tenantId);


    /**
     * 获取租户的管理员用户
     *
     * @param idx   索引
     * @param limit 限制
     * @param tenantIds 租户ID列表
     *
     * @return {@link List<Employee>}
     */
    List<Employee> loadTenantHoldEmployee(Long idx, int limit, Collection<Long> tenantIds);


    /**
     * 获取租户的所有员工
     *
     * @param idx   索引
     * @param limit 限制
     * @param tenantIds 租户ID列表
     *
     * @return {@link List<Employee>}
     */
    List<Employee> loadTenantAllEmployee(Long idx, int limit, List<Long> tenantIds);


    /**
     * 查询客户端用户对应的员工
     *
     * @param clientUserId 账号ID
     * @param tenantId   租户ID
     * @param select     查询字段
     * @return {@link Optional<Employee>}
     */
    Optional<Employee> getClientEmployee(long clientUserId, long tenantId, List<SFunction<Employee, ?>> select);

    /**
     * 修改个人资料
     *
     * @param request    员工信息
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     *
     * @return {@link ApiResponse<Void>}
     */
    ApiResponse<Void> updateProfile(EmployeeProfileUpdateRequest request, long employeeId, long tenantId);

    /**
     * 解绑通知
     *
     * @param clientAccountId 客户账号ID
     *
     * @return {@link ApiResponse<EmployeeUnbindResponse>}
     */
    ApiResponse<EmployeeUnbindResponse> unbind(long clientAccountId);

    /**
     * 绑定回调添加人员
     *
     * @param request 绑定请求
     *
     * @return {@link ApiResponse<Void>}
     */
    ApiResponse<Void> bindCallback(BindSubAccountRequest request);
}
