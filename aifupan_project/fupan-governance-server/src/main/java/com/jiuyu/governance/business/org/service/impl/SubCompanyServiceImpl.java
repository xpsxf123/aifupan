package com.jiuyu.governance.business.org.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.mapper.EmployeeMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import com.jiuyu.governance.business.org.mapper.DeptMapper;
import com.jiuyu.governance.business.org.mapper.SubCompanyMapper;
import com.jiuyu.governance.business.org.pojo.constants.ManagerType;
import com.jiuyu.governance.business.org.pojo.entity.Dept;
import com.jiuyu.governance.business.org.pojo.entity.SubCompany;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyAddRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanySelectQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.SubCompanyUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.SubCompanyResponse;
import com.jiuyu.governance.business.org.service.SubCompanyService;
import com.jiuyu.governance.openfeign.replay.PropertyService;
import com.jiuyu.governance.openfeign.replay.consts.PropertyType;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 子公司服务实现类
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubCompanyServiceImpl extends ServiceImpl<SubCompanyMapper, SubCompany> implements SubCompanyService, InitializationTenant {

    private final DeptMapper deptMapper;

    private final ManagerConnectorProcessor connectorProcessor;

    private final EmployeeMapper employeeMapper;

    private final PropertyService propertyService;

    private final int maxCompany = 300;

    /**
     * 公司是否绑定了员工
     *
     * @param companyId 公司ID
     *
     * @return 是否绑定成功
     */
    private boolean employeeBind(long companyId) {
        return ChainWrappers.lambdaQueryChain(employeeMapper)
            .eq(Employee::getCompanyId, companyId)
            .eq(Employee::getAccountStatus, 1)
            .eq(Employee::getIsDeleted, false)
            .exists();
    }


    /**
     * 获取公司数量
     *
     * @param tenantId 租户ID
     *
     * @return 公司数量
     */
    private long countCompany(Long tenantId) {
        return super.lambdaQuery()
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getIsDeleted, false)
            .count();
    }


    /**
     * 默认顺序
     *
     * @return 顺序
     */
    @Override
    public int getOrder() {
        return 2;
    }

    /**
     * 初始化
     *
     * @param tenantContext 租户初始化上下文
     * @param accessUser    访问用户
     */
    @Override
    public void init(TenantInitContext tenantContext, AccessUser accessUser) {
        Long tenantId = tenantContext.getTenantId();
        SubCompany company = super.lambdaQuery().eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getIsDeleted, false)
            .last("limit 1").oneOpt().orElseGet(() -> {
                return this.initDefault(tenantId);
            });
        tenantContext.put("companyId", company.getId());
    }

    /**
     * 初始化默认公司
     *
     * @param tenantId 租户ID
     *
     * @return 默认公司
     */
    private SubCompany initDefault(Long tenantId) {
        SubCompany subCompany = new SubCompany();
        subCompany.setId(IdUtil.getSnowflakeNextId());
        subCompany.setTenantId(tenantId);
        subCompany.setName("默认公司");
        subCompany.setCreateBy(0L);
        subCompany.setUpdateBy(0L);
        subCompany.setCreateDate(LocalDateTime.now());
        subCompany.setUpdateDate(LocalDateTime.now());
        subCompany.setIsDeleted(false);
        subCompany.setSort(1);
        super.save(subCompany);
        return subCompany;
    }

    /**
     * 获取租户的第一个子公司
     *
     * @param tenantId 租户ID
     *
     * @return 子公司ID
     */
    @Override
    public Optional<Long> getTenantFirst(long tenantId) {
         return super.lambdaQuery()
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getIsDeleted, false)
             .orderByAsc(SubCompany::getId)
            .last("LIMIT 1")
            .select(SubCompany::getId)
            .oneOpt()
            .map(SubCompany::getId).or(() -> {
                return this.initDefault(tenantId).getId().describeConstable();
            });
    }

    /**
     * 新增子公司
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> addSubCompany(SubCompanyAddRequest request, Long tenantId, Long userId) {
        Long count = this.countCompany(tenantId);
        if (count >= maxCompany) {
            log.warn("[子公司管理] 新增子公司: {}, tenantId: {}, 超出最大数量 {} >= {}", request.getName(), tenantId, count, maxCompany);
            return ApiResponse.failed(BizErrorCode.QUOTA_EXCEEDED.getCode(), "公司数量已达上限");
        }
        boolean exists = super.lambdaQuery()
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getName, request.getName())
            .eq(SubCompany::getIsDeleted, false)
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "公司名称已存在");
        }

        SubCompany subCompany = BeanUtil.copyProperties(request, SubCompany.class);
        subCompany.setId(IdUtil.getSnowflakeNextId());
        subCompany.setTenantId(tenantId);
        subCompany.setCreateBy(userId);
        subCompany.setUpdateBy(userId);
        subCompany.setCreateDate(LocalDateTime.now());
        subCompany.setUpdateDate(LocalDateTime.now());
        subCompany.setIsDeleted(false);
        return propertyService.editTenantPropertyQuota(tenantId, count + 1, PropertyType.ENTERPRISE_SUBSIDIARIES_NUM,quota -> {
            if (!quota) {
                return ApiResponse.failed(BizErrorCode.QUOTA_EXCEEDED.getCode(), "您的子公司资产额度不足,请联系销售处理");
            }
            this.save(subCompany);
            log.info("[子公司管理] 新增子公司: {}, tenantId: {}", subCompany.getName(), tenantId);
            if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
                connectorProcessor.connector(ManagerType.COMPANY, subCompany.getId(), request.getManagerUserIds());
            }
            connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.COMPANY));
            return ApiResponse.success();
        });
    }

    /**
     * 修改子公司
     *
     * @param request  请求参数
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> updateSubCompany(SubCompanyUpdateRequest request, Long tenantId, Long userId) {
        SubCompany exist = this.getById(request.getId());
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "公司不存在或无权限修改");
        }

        boolean exists = super.lambdaQuery()
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getName, request.getName())
            .eq(SubCompany::getIsDeleted, false)
            .ne(SubCompany::getId, request.getId())
            .exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "公司名称已存在");
        }

        SubCompany updateEntity = BeanUtil.copyProperties(request, SubCompany.class);
        updateEntity.setUpdateBy(userId);
        updateEntity.setUpdateDate(LocalDateTime.now());
        this.updateById(updateEntity);
        log.info("[子公司管理] 修改子公司: {}, tenantId: {}", request.getId(), tenantId);
        if (EmptyUtil.isNotEmpty(request.getManagerUserIds())) {
            connectorProcessor.connector(ManagerType.COMPANY, request.getId(), request.getManagerUserIds());
        } else {
            connectorProcessor.disconnectAll(ManagerType.COMPANY, request.getId());
        }
        return ApiResponse.success();
    }

    /**
     * 删除子公司
     *
     * @param id       子公司ID
     * @param tenantId 租户ID
     * @param userId   当前用户ID
     *
     * @return 响应结果
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> deleteSubCompany(Long id, Long tenantId, Long userId) {
        SubCompany exist = this.getById(id);
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "公司不存在或无权限删除");
        }
        if (employeeBind(id)) {
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "该公司下存在员工，不允许删除");
        }
        // 检查是否有下属部门
        boolean hasDept = deptMapper.exists(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dept>()
            .eq(Dept::getCompanyId, id)
            .eq(Dept::getIsDeleted, false));
        if (hasDept) {
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "该公司下存在部门，不允许删除");
        }

        exist.setIsDeleted(true);
        exist.setUpdateBy(userId);
        exist.setUpdateDate(LocalDateTime.now());

        long count = this.countCompany(tenantId);
        if (count < 1) {
            return ApiResponse.failed(BizErrorCode.QUOTA_EXCEEDED.getCode(), "您至少需要一个子公司");
        }
        return propertyService.editTenantPropertyQuota(tenantId, count - 1, PropertyType.ENTERPRISE_SUBSIDIARIES_NUM, r -> {
            this.updateById(exist);
            log.info("[子公司管理] 删除子公司: {}, tenantId: {}", id, tenantId);
            connectorProcessor.disconnectAll(ManagerType.COMPANY, id);
            connectorProcessor.clearTenantCache(tenantId, List.of(ManagerType.COMPANY));
            return ApiResponse.success();
        });

    }

    /**
     * 分页查询子公司
     *
     * @param request    查询请求
     * @param accessUser 租户ID
     *
     * @return 分页数据
     */
    @Override
    public PageData<SubCompanyResponse> pageQuerySubCompany(SubCompanyPageQueryRequest request, AccessUser accessUser) {
        if (!OauthConstant.isTenantAdmin(accessUser) && EmptyUtil.isEmpty(request.getCompanyIds())) {
            return PageData.empty();
        }
        PageData<SubCompany> pageData = CustomPage.execute(request, (page, req) -> {
            return super.lambdaQuery()
                .in(EmptyUtil.isNotEmpty(req.getCompanyIds()), SubCompany::getId, req.getCompanyIds())
                .eq(SubCompany::getTenantId, accessUser.currentTenantId())
                .eq(SubCompany::getIsDeleted, false)
                .like(EmptyUtil.isNotEmpty(req.getName()), SubCompany::getName, req.getName())
                .orderByDesc(SubCompany::getId)
                .page(page);
        });

        PageData<SubCompanyResponse> data = pageData.conversion(entity -> BeanUtil.copyProperties(entity, SubCompanyResponse.class));
        Complete.start(data.getList())
            .build(SubCompanyResponse::getId, SubCompanyResponse::setManagerUserInfos, ids -> connectorProcessor.getConnectorManagerInfos(ManagerType.COMPANY, ids))
            .then()
            .over();
        return data;
    }


    /**
     * 判断是否存在ID
     *
     * @param companyId 公司ID
     * @param tenantId  租户ID
     *
     * @return 是否存在
     */
    @Override
    public boolean hasId(Long companyId, Long tenantId) {
        return super.lambdaQuery().eq(SubCompany::getId, companyId)
            .eq(SubCompany::getTenantId, tenantId)
            .eq(SubCompany::getIsDeleted, false)
            .exists();
    }

    /**
     * 获取名称Map
     *
     * @param ids ID列表
     *
     * @return 名称Map
     */
    @Override
    public Map<Long, String> getNameMap(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return Map.of();
        }
        return super.lambdaQuery().select(SubCompany::getId, SubCompany::getName)
            .eq(SubCompany::getIsDeleted, false)
            .in(SubCompany::getId, ids)
            .list()
            .stream()
            .collect(Collectors.toMap(SubCompany::getId, SubCompany::getName));
    }

    /**
     * 下拉搜索
     *
     * @param queryRequest 查询请求
     *
     * @return {@link List }<{@link LabelOption }>
     */
    @Override
    public List<LabelOption> options(SubCompanySelectQueryRequest queryRequest) {
        if (EmptyUtil.isEmpty(queryRequest.getCompanyIds())) {
            return List.of();
        }
        return super.lambdaQuery()
            .select(SubCompany::getId, SubCompany::getName)
            .notIn(EmptyUtil.isNotEmpty(queryRequest.getExcludeIds()), SubCompany::getId, queryRequest.getExcludeIds())
            .eq(SubCompany::getIsDeleted, false)
            .eq(SubCompany::getTenantId, queryRequest.getTenantId())
            .in(EmptyUtil.isNotEmpty(queryRequest.getCompanyIds()), SubCompany::getId, queryRequest.getCompanyIds())
            .like(EmptyUtil.isNotEmpty(queryRequest.getKeyword()), SubCompany::getName, queryRequest.getKeyword())
            .orderByAsc(SubCompany::getSort)
            .last("limit " + queryRequest.getLimit())
            .list()
            .stream()
            .map(entity -> new LabelOption(entity.getId(), entity.getName()))
            .toList();
    }

    /**
     * 获取公司列表
     *
     * @param companyIds 公司ID列表
     *
     * @return 公司列表
     */
    @Override
    public List<LabelOption> getCompanyList(Collection<Long> companyIds) {
        if (EmptyUtil.isEmpty(companyIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .select(SubCompany::getId, SubCompany::getName, SubCompany::getSort)
            .eq(SubCompany::getIsDeleted, false)
            .in(SubCompany::getId, companyIds)
            .list().stream().sorted(Comparator.comparingInt(SubCompany::getSort))
            .map(entity -> new LabelOption(entity.getId(), entity.getName())).toList();
    }
}
