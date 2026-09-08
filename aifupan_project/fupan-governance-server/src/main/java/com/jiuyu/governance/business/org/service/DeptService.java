package com.jiuyu.governance.business.org.service;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.pojo.bo.DeptInfo;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.request.DeptAddRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.DeptResponse;
import com.jiuyu.governance.common.pojo.bo.LabelOption;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 部门服务接口
 *
 * @author HeHui
 * @date 2026-03-18
 */
public interface DeptService {

    /**
     * 新增部门
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> addDept(DeptAddRequest request, Long tenantId, Long userId);

    /**
     * 修改部门
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> updateDept(DeptUpdateRequest request, Long tenantId, Long userId);

    /**
     * 删除部门
     *
     * @param id       部门ID
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    ApiResponse<Void> deleteDept(Long id, Long tenantId, Long userId);

    /**
     * 分页查询部门
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     *
     * @return 分页数据
     */
    PageData<DeptResponse> pageQueryDept(DeptPageQueryRequest request, AccessUser accessUser);

    /**
     * 根据ID查询部门是否存在
     *
     * @param deptId   部门ID
     * @param tenantId 租户ID
     *
     * @return 是否存在
     */
    boolean hasId(long deptId, Long tenantId);


    /**
     * 根据ID查询部门
     *
     * @param deptId   部门ID
     * @param tenantId 租户ID
     *
     * @return 部门
     */
    Optional<Dept> findDept(long deptId, Long tenantId);

    /**
     * 根据ID查询部门名称
     *
     * @param ids 部门ID列表
     *
     * @return 部门名称列表
     */
    Map<Long, String> getDeptNameMap(List<Long> ids);

    /**
     * 部门下拉选择
     *
     * @param queryRequest 查询请求
     *
     * @return 部门下拉选择列表
     */
    List<LabelOption> options(DeptSelectQueryRequest queryRequest);


    /**
     * 部门下拉选择
     *
     * @param queryRequest 查询请求
     *
     * @return 部门下拉选择列表
     */
    List<DeptInfo> getDeptInfoList(DeptSelectQueryRequest queryRequest);


    /**
     * 根据部门ID列表查询部门信息
     *
     * @param deptIds 部门ID列表
     *
     * @return 部门信息列表
     */
    List<DeptInfo> getDeptInfoList(Collection<Long> deptIds);
}
