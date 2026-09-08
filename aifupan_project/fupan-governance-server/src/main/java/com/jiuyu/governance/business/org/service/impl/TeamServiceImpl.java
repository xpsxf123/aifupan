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
import com.jiuyu.governance.business.org.pojo.bo.TeamInfo;
import com.jiuyu.governance.business.rbac.mapper.EmployeeMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.mapper.TeamMapper;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.org.pojo.request.TeamAddRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamSelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.TeamResponse;
import com.jiuyu.governance.business.org.service.DeptService;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.business.org.service.TeamService;
import com.jiuyu.governance.plugins.oauth.data.BeforePermission;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 小组服务实现类
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {

    private final DeptService deptService;

    private final SubCompanyService companyService;

    private final ManagerConnectorProcessor connectorProcessor;

    private final EmployeeMapper employeeMapper;

    private final int maxTeamSize = 1000;

    /**
     * 小组是否绑定了员工
     *
     * @param teamId 小组ID
     *
     * @return 是否绑定成功
     */
    private boolean employeeBind(long teamId) {
        return ChainWrappers.lambdaQueryChain(employeeMapper)
            .eq(Employee::getTeamId, teamId)
            .eq(Employee::getAccountStatus, 1)
            .eq(Employee::getIsDeleted, false)
            .exists();
    }

    /**
     * 新增小组
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.DEPT, dataId = "#request.deptId")
    public ApiResponse<Void> addTeam(TeamAddRequest request, Long tenantId, Long userId) {
        // 校验部门是否存在
        Optional<Dept> deptOptional = deptService.findDept(request.getDeptId(), tenantId);
        if (deptOptional.isEmpty()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "所属部门不存在");
        }
        Long count = super.lambdaQuery()
            .eq(Team::getTenantId, tenantId)
            .eq(Team::getIsDeleted, false).count();
        if (count >= maxTeamSize) {
            log.warn("[小组管理] 新增小组: {}, tenantId: {}, 超出最大数量 {} >= {}", request.getName(), tenantId, count, maxTeamSize);
            return ApiResponse.failed(BizErrorCode.QUOTA_EXCEEDED.getCode(), "小组数量已达上限");
        }

        boolean exists = super.lambdaQuery()
            .eq(Team::getTenantId, tenantId)
            .eq(Team::getDeptId, request.getDeptId())
            .eq(Team::getName, request.getName())
            .eq(Team::getIsDeleted, false)
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "同部门下小组名称已存在");
        }

        Team team = BeanUtil.copyProperties(request, Team.class);
        team.setTenantId(tenantId);
        team.setCompanyId(deptOptional.get().getCompanyId());
        team.setCreateBy(userId);
        team.setUpdateBy(userId);
        team.setCreateDate(LocalDateTime.now());
        team.setUpdateDate(LocalDateTime.now());
        team.setIsDeleted(false);

        this.save(team);
        log.info("[小组管理] 新增小组: {}, tenantId: {}", team.getName(), tenantId);
        if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
            connectorProcessor.connector(ManagerType.TEAM, team.getId(), request.getManagerUserIds());
        }
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(team.getCompanyId()), ManagerType.DEPT, List.of(team.getDeptId())));
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.TEAM));
        return ApiResponse.success();
    }

    /**
     * 修改小组
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.TEAM, dataId = "#request.id")
    public ApiResponse<Void> updateTeam(TeamUpdateRequest request, Long tenantId, Long userId) {
        Team exist = this.getById(request.getId());
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "小组不存在或无权限修改");
        }

        Long companyId = exist.getCompanyId();
        Long oldCompanyId = exist.getCompanyId();
        Long oldDeptId = exist.getDeptId();
        if (!exist.getDeptId().equals(request.getDeptId())) {
            Optional<Dept> deptOptional = deptService.findDept(request.getDeptId(), tenantId);
            if (deptOptional.isEmpty()) {
                return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "所属部门不存在");
            }
            companyId = deptOptional.get().getCompanyId();
        }
        Long newCompanyId = companyId;
        Long newDeptId = request.getDeptId();

        boolean exists = super.lambdaQuery()
            .eq(Team::getTenantId, tenantId)
            .eq(Team::getDeptId, request.getDeptId())
            .eq(Team::getName, request.getName())
            .eq(Team::getIsDeleted, false)
            .ne(Team::getId, request.getId())
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "同部门下小组名称已存在");
        }

        Team updateEntity = BeanUtil.copyProperties(request, Team.class);
        updateEntity.setUpdateBy(userId);
        updateEntity.setCompanyId(companyId);
        updateEntity.setUpdateDate(LocalDateTime.now());
        this.updateById(updateEntity);
        log.info("[小组管理] 修改小组: {}, tenantId: {}", request.getId(), tenantId);
        if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
            connectorProcessor.connector(ManagerType.TEAM, request.getId(), request.getManagerUserIds());
        } else {
            connectorProcessor.disconnectAll(ManagerType.TEAM, request.getId());
        }
        if (!Objects.equals(oldCompanyId, newCompanyId) || !Objects.equals(oldDeptId, newDeptId)) {
            Map<ManagerType, List<Long>> clearMap = new EnumMap<>(ManagerType.class);
            clearMap.put(ManagerType.COMPANY, Stream.of(oldCompanyId, newCompanyId).filter(Objects::nonNull).distinct().toList());
            clearMap.put(ManagerType.DEPT, Stream.of(oldDeptId, newDeptId).filter(Objects::nonNull).distinct().toList());
            connectorProcessor.clearOrgCache(clearMap);
        }
        return ApiResponse.success();
    }

    /**
     * 删除小组
     *
     * @param id       小组ID
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    @BeforePermission(type = OauthConstant.TEAM, dataId = "#id")
    public ApiResponse<Void> deleteTeam(Long id, Long tenantId, Long userId) {
        Team exist = this.getById(id);
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "小组不存在或无权限删除");
        }

        if (employeeBind(id)) {
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "小组下有员工，请先解除员工关系");
        }

        exist.setIsDeleted(true);
        exist.setUpdateBy(userId);
        exist.setUpdateDate(LocalDateTime.now());
        this.updateById(exist);
        log.info("[小组管理] 删除小组: {}, tenantId: {}", id, tenantId);
        connectorProcessor.disconnectAll(ManagerType.TEAM, id);
        connectorProcessor.clearOrgCache(Map.of(ManagerType.COMPANY, List.of(exist.getCompanyId()), ManagerType.DEPT, List.of(exist.getDeptId())));
        connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.TEAM));
        return ApiResponse.success();
    }

    /**
     * 分页查询小组
     *
     * @param request    查询请求
     * @param accessUser 访问用户
     *
     * @return 分页数据
     */
    @Override
    public PageData<TeamResponse> pageQueryTeam(TeamPageQueryRequest request, AccessUser accessUser) {
        PageData<Team> pageData = CustomPage.execute(request, (page, req) -> {
            return super.getBaseMapper().pageQueryTeam(page, req);
        });
        PageData<TeamResponse> data = pageData.conversion(entity -> BeanUtil.copyProperties(entity, TeamResponse.class));
        Complete.start(data.getList())
            .build(TeamResponse::getCompanyId, TeamResponse::setCompanyName, companyService::getNameMap)
            .then()
            .build(TeamResponse::getDeptId, TeamResponse::setDeptName, deptService::getDeptNameMap)
            .then()
            .build(TeamResponse::getId, TeamResponse::setManagerUserInfos, ids -> connectorProcessor.getConnectorManagerInfos(ManagerType.TEAM, ids))
            .then()
            .over();
        return data;
    }


    /**
     * 小组下拉选择
     *
     * @param queryRequest 查询请求
     *
     * @return {@link List }<{@link LabelOption }>
     */
    @Override
    public List<LabelOption> options(TeamSelectQueryRequest queryRequest) {
        if (queryRequest.getDeptId() != null) {
            if (EmptyUtil.isEmpty(queryRequest.getDeptIds()) || !queryRequest.getDeptIds().contains(queryRequest.getDeptId())) {
                return List.of();
            }
        }
        return super.getBaseMapper().listQueryTeam(queryRequest)
            .stream()
            .map(entity -> new LabelOption(entity.getId(), entity.getName()))
            .toList();
    }

    /**
     * 获取小组列表
     *
     * @param queryRequest 查询请求
     *
     * @return {@link List }<{@link TeamInfo }>
     */
    @Override
    public List<TeamInfo> getTeamList(TeamSelectQueryRequest queryRequest) {
        if (queryRequest.getDeptId() != null) {
            if (EmptyUtil.isEmpty(queryRequest.getDeptIds()) || !queryRequest.getDeptIds().contains(queryRequest.getDeptId())) {
                return List.of();
            }
        }
        return super.getBaseMapper().listQueryTeam(queryRequest)
            .stream()
            .map(entity -> {
                TeamInfo info = new TeamInfo();
                info.setId(entity.getId());
                info.setDeptId(entity.getDeptId());
                info.setCompanyId(entity.getCompanyId());
                info.setName(entity.getName());
                info.setSort(entity.getSort());
                return info;
            })
            .toList();
    }

    /**
     * 获取小组信息列表
     *
     * @param ids 小组ID列表
     *
     * @return {@link List }<{@link TeamInfo }>
     */
    @Override
    public List<TeamInfo> getTeamInfoList(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return List.of();
        }
        return super.lambdaQuery()
            .select(Team::getId, Team::getName, Team::getDeptId, Team::getCompanyId, Team::getSort)
            .eq(Team::getIsDeleted, false)
            .in(Team::getId, ids)
            .list()
            .stream()
            .map(entity -> {
                TeamInfo info = new TeamInfo();
                info.setId(entity.getId());
                info.setDeptId(entity.getDeptId());
                info.setCompanyId(entity.getCompanyId());
                info.setName(entity.getName());
                info.setSort(entity.getSort());
                return info;
            }).toList();
    }

    /**
     * 获取小组名称映射
     *
     * @param ids 小组ID列表
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    @Override
    public Map<Long, String> getTeamNameMap(List<Long> ids) {
         if (EmptyUtil.isEmpty(ids)) {
             return Map.of();
         }
         return super.lambdaQuery()
             .select(Team::getId, Team::getName)
             .eq(Team::getIsDeleted, false)
             .in(Team::getId, ids)
             .list()
             .stream()
             .collect(Collectors.toMap(Team::getId, Team::getName));
    }
}
