package com.jiuyu.governance.business.org.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.mapper.DeptMapper;
import com.jiuyu.governance.business.org.mapper.TeamMapper;
import com.jiuyu.governance.business.org.pojo.bo.DeptInfo;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.org.pojo.request.DeptAddRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.DeptUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.DeptResponse;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.rbac.mapper.EmployeeMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.plugins.oauth.data.BeforePermission;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 部门服务实现类
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    private final SubCompanyService companyService;

    private final TeamMapper teamMapper;

    private final ManagerConnectorProcessor connectorProcessor;

    private final EmployeeMapper employeeMapper;

    /**
     * 部门最大数量
     */
    private final int maxDept = 500;

    /**
     * 部门是否绑定了员工
     *
     * @param deptId 部门ID
     *
     * @return 是否绑定成功
     */
    private boolean employeeBind(long deptId) {
        return ChainWrappers.lambdaQueryChain(employeeMapper)
            .eq(Employee::getDeptId, deptId)
            .eq(Employee::getAccountStatus, 1)
            .eq(Employee::getIsDeleted, false)
            .exists();
    }

    /**
     * 新增部门
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.COMPANY, dataId = "#request.companyId")
    public ApiResponse<Void> addDept(DeptAddRequest request, Long tenantId, Long userId) {
        // 校验公司是否存在
        boolean companyExists = companyService.hasId(request.getCompanyId(), tenantId);
        if (!companyExists) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "所属公司不存在");
        }
        Long count = super.lambdaQuery()
            .eq(Dept::getTenantId, tenantId)
            .eq(Dept::getIsDeleted, false).count();
        if (count >= maxDept) {
            log.warn("[部门管理] 新增子部门: {}, tenantId: {}, 超出最大数量 {} >= {}", request.getName(), tenantId, count, maxDept);
            return ApiResponse.failed(BizErrorCode.QUOTA_EXCEEDED.getCode(), "部门数量已达上限");
        }

        boolean exists = super.lambdaQuery()
            .eq(Dept::getTenantId, tenantId)
            .eq(Dept::getCompanyId, request.getCompanyId())
            .eq(Dept::getName, request.getName())
            .eq(Dept::getIsDeleted, false)
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "同公司下部门名称已存在");
        }

        Dept dept = BeanUtil.copyProperties(request, Dept.class);
        dept.setTenantId(tenantId);
        dept.setCreateBy(userId);
        dept.setUpdateBy(userId);
        dept.setCreateDate(LocalDateTime.now());
        dept.setUpdateDate(LocalDateTime.now());
        dept.setIsDeleted(false);
        this.save(dept);
        log.info("[部门管理] 新增部门: {}, tenantId: {}", dept.getName(), tenantId);
        if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
            connectorProcessor.connector(ManagerType.DEPT, dept.getId(), request.getManagerUserIds());
        }
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(dept.getCompanyId())));
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.DEPT));
        return ApiResponse.success();
    }

    /**
     * 修改部门
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.DEPT, dataId = "#request.id")
    public ApiResponse<Void> updateDept(DeptUpdateRequest request, Long tenantId, Long userId) {
        Dept exist = this.getById(request.getId());
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "部门不存在或无权限修改");
        }

        if (!exist.getCompanyId().equals(request.getCompanyId())) {
            // 校验新公司是否存在
            boolean companyExists = companyService.hasId(request.getCompanyId(), tenantId);
            if (!companyExists) {
                return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "所属公司不存在");
            }
            connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(exist.getCompanyId(), request.getCompanyId())));
        }

        boolean exists = super.lambdaQuery()
            .eq(Dept::getTenantId, tenantId)
            .eq(Dept::getCompanyId, request.getCompanyId())
            .eq(Dept::getName, request.getName())
            .eq(Dept::getIsDeleted, false)
            .ne(Dept::getId, request.getId())
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "同公司下部门名称已存在");
        }

        Dept updateEntity = BeanUtil.copyProperties(request, Dept.class);
        updateEntity.setUpdateBy(userId);
        updateEntity.setUpdateDate(LocalDateTime.now());
        this.updateById(updateEntity);
        log.info("[部门管理] 修改部门: {}, tenantId: {}", request.getId(), tenantId);
        if (EmptyUtil.isEmpty(request.getManagerUserIds())) {
            connectorProcessor.disconnectAll(ManagerType.DEPT, request.getId());
        } else {
            connectorProcessor.connector(ManagerType.DEPT, request.getId(), request.getManagerUserIds());
        }
        return ApiResponse.success();
    }

    /**
     * 删除部门
     *
     * @param id       部门ID
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.DEPT, dataId = "#id")
    public ApiResponse<Void> deleteDept(Long id, Long tenantId, Long userId) {
        Dept exist = this.getById(id);
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "部门不存在或无权限删除");
        }
        if (this.employeeBind(id)) {
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "该部门下存在员工，不允许删除");
        }

        // 检查是否有下属小组
        boolean hasTeam = teamMapper.exists(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Team>()
            .eq(Team::getDeptId, id)
            .eq(Team::getIsDeleted, false));
        if (hasTeam) {
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "该部门下存在小组，不允许删除");
        }

        exist.setIsDeleted(true);
        exist.setUpdateBy(userId);
        exist.setUpdateDate(LocalDateTime.now());
        this.updateById(exist);
        log.info("[部门管理] 删除部门: {}, tenantId: {}", id, tenantId);
        connectorProcessor.disconnectAll(ManagerType.DEPT, id);
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(exist.getCompanyId()), ManagerType.DEPT, List.of(id)));
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.DEPT));
        return ApiResponse.success();
    }

    /**
     * 分页查询部门
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     *
     * @return 分页数据
     */
    @Override
    public PageData<DeptResponse> pageQueryDept(DeptPageQueryRequest request, AccessUser accessUser) {
        if (EmptyUtil.isNotEmpty(request.getCompanyId()) && EmptyUtil.isNotEmpty(request.getCompanyIds())) {
            if (!request.getCompanyIds().contains(request.getCompanyId())) {
                return PageData.empty();
            }
        }

        PageData<Dept> pageData = CustomPage.execute(request, (page, req) -> {
            return super.getBaseMapper().pageQueryDept(page, req);
        });
        PageData<DeptResponse> data = pageData.conversion(entity -> BeanUtil.copyProperties(entity, DeptResponse.class));
        Complete.start(data.getList())
            .build(DeptResponse::getCompanyId, DeptResponse::setCompanyName, companyService::getNameMap)
            .then()
            .build(DeptResponse::getId, DeptResponse::setManagerUserInfos, ids -> connectorProcessor.getConnectorManagerInfos(ManagerType.DEPT, ids))
            .then()
            .over();
        return data;
    }


    /**
     * 根据ID查询部门是否存在
     *
     * @param deptId   部门ID
     * @param tenantId 租户ID
     *
     * @return 是否存在
     */
    @Override
    public boolean hasId(long deptId, Long tenantId) {
        return super.lambdaQuery()
            .eq(Dept::getId, deptId)
            .eq(Dept::getTenantId, tenantId)
            .eq(Dept::getIsDeleted, false)
            .exists();
    }

    /**
     * 根据ID查询部门
     *
     * @param deptId   部门ID
     * @param tenantId 租户ID
     *
     * @return 部门
     */
    @Override
    public Optional<Dept> findDept(long deptId, Long tenantId) {
        return super.lambdaQuery()
            .eq(Dept::getId, deptId)
            .eq(Dept::getTenantId, tenantId)
            .eq(Dept::getIsDeleted, false)
            .oneOpt();
    }

    /**
     * 根据ID查询部门名称
     *
     * @param ids 部门ID列表
     *
     * @return 部门名称列表
     */
    @Override
    public Map<Long, String> getDeptNameMap(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return Map.of();
        }
        return super.lambdaQuery()
            .select(Dept::getId, Dept::getName)
            .in(Dept::getId, ids)
            .eq(Dept::getIsDeleted, false)
            .list()
            .stream()
            .collect(Collectors.toMap(Dept::getId, Dept::getName));
    }


    /**
     * 部门下拉选择
     *
     * @param queryRequest 查询请求
     *
     * @return 部门下拉选择列表
     */
    @Override
    public List<LabelOption> options(DeptSelectQueryRequest queryRequest) {
        if (EmptyUtil.isNotEmpty(queryRequest.getCompanyId()) && EmptyUtil.isNotEmpty(queryRequest.getCompanyIds())) {
            if (!queryRequest.getCompanyIds().contains(queryRequest.getCompanyId())) {
                return List.of();
            }
        }
        return super.getBaseMapper().listDept(queryRequest)
            .stream()
            .map(entity -> new LabelOption(entity.getId(), entity.getName()))
            .toList();
    }


    /**
     * 部门下拉选择
     *
     * @param queryRequest 查询请求
     *
     * @return 部门下拉选择列表
     */
    @Override
    public List<DeptInfo> getDeptInfoList(DeptSelectQueryRequest queryRequest) {
        if (EmptyUtil.isNotEmpty(queryRequest.getCompanyId())) {
            if (EmptyUtil.isEmpty(queryRequest.getCompanyIds()) || !queryRequest.getCompanyIds().contains(queryRequest.getCompanyId())) {
                return List.of();
            }
        }
        return super.getBaseMapper().listDept(queryRequest)
            .stream()
            .map(entity -> {
                DeptInfo deptInfo = new DeptInfo();
                deptInfo.setId(entity.getId());
                deptInfo.setCompanyId(entity.getCompanyId());
                deptInfo.setName(entity.getName());
                deptInfo.setSort(entity.getSort());
                return deptInfo;
            })
            .toList();
    }

    /**
     * 根据部门ID列表查询部门信息
     *
     * @param deptIds 部门ID列表
     *
     * @return 部门信息列表
     */
    @Override
    public List<DeptInfo> getDeptInfoList(Collection<Long> deptIds) {
        if (EmptyUtil.isEmpty(deptIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .select(Dept::getId, Dept::getName, Dept::getCompanyId, Dept::getSort)
            .eq(Dept::getIsDeleted, false)
            .in(Dept::getId, deptIds)
            .list()
            .stream()
            .map(entity -> {
                DeptInfo deptInfo = new DeptInfo();
                deptInfo.setId(entity.getId());
                deptInfo.setCompanyId(entity.getCompanyId());
                deptInfo.setName(entity.getName());
                deptInfo.setSort(entity.getSort());
                return deptInfo;
            })
            .toList();
    }
}
