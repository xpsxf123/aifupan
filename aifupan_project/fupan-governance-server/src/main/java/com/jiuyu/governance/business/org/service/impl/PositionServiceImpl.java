package com.jiuyu.governance.business.org.service.impl;

import com.jiuyu.governance.business.org.pojo.response.DefaultPositionInfoResponse;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.org.mapper.PositionMapper;
import com.jiuyu.governance.business.org.pojo.entity.Position;
import com.jiuyu.governance.business.org.pojo.request.PositionAddRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.PositionResponse;
import com.jiuyu.governance.business.org.service.PositionService;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.business.rbac.service.EmployeeService;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 岗位业务 实现
 *
 * @author HeHui
 * @date 2026-03-23 11:07
 */
@Service
@Slf4j
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService, InitializationTenant {

    private final EmployeeService employeeService;

    public PositionServiceImpl(@Lazy EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 新增岗位
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> addPosition(PositionAddRequest request, AccessUser accessUser) {
        Long tenantId = accessUser.currentTenantId();

        // 校验岗位名称在租户内是否唯一
        if (checkNameUnique(tenantId, null, request.getName())) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "岗位名称已存在");
        }

        Position position = new Position();
        position.setTenantId(tenantId);
        position.setName(request.getName());
        position.setPositionCode("");
        position.setSort(request.getSort());
        position.setIsDefault(false);
        position.setIsDeleted(false);
        position.setCreateBy(accessUser.userId());
        position.setUpdateBy(accessUser.userId());
        position.setCreateDate(LocalDateTime.now());
        position.setUpdateDate(LocalDateTime.now());
        log.info("[岗位] tenant {} new position  {}", tenantId, position.getName());
        super.save(position);
        return ApiResponse.success();
    }

    /**
     * 修改岗位
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> updatePosition(PositionUpdateRequest request, AccessUser accessUser) {
        Long tenantId = accessUser.currentTenantId();
        Position position = super.lambdaQuery()
            .eq(Position::getId, request.getId())
            .eq(Position::getTenantId, tenantId)
            .eq(Position::getIsDeleted, false)
            .one();
        if (position == null) {
            log.warn("[岗位] tenant {} update position {} not exist", tenantId, request.getId());
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "岗位不存在或已删除");
        }

        // 校验岗位名称在租户内是否唯一
        if (checkNameUnique(tenantId, request.getId(), request.getName())) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "岗位名称已存在");
        }

        position.setName(request.getName());
        position.setSort(request.getSort());
        position.setUpdateBy(accessUser.userId());
        position.setUpdateDate(LocalDateTime.now());

        super.updateById(position);
        log.info("[岗位] tenant {} update position {}", tenantId, position.getName());
        return ApiResponse.success();
    }

    /**
     * 删除岗位
     *
     * @param id         ID
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> deletePosition(long id, AccessUser accessUser) {
        Long tenantId = accessUser.currentTenantId();

        Position position = super.lambdaQuery()
            .eq(Position::getId, id)
            .eq(Position::getTenantId, tenantId)
            .eq(Position::getIsDeleted, false)
            .one();

        if (position == null) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "岗位不存在或已删除");
        }

        if (Boolean.TRUE.equals(position.getIsDefault())) {
            log.warn("[岗位] tenant {} delete position {} is default", tenantId, position.getName());
            return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "默认岗位不可删除");
        }

        Long employeeCount = employeeService.countPositionEmployee(List.of(id)).getOrDefault(id, 0L);
        if (employeeCount > 0) {
            log.warn("[岗位] tenant {} delete position {} has employee", tenantId, position.getName());
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "该岗位下存在关联员工，不可删除");
        }

        position.setIsDeleted(true);
        position.setUpdateBy(accessUser.userId());
        position.setUpdateDate(LocalDateTime.now());

        super.updateById(position);
        log.info("[岗位] tenant {} delete position {}", tenantId, position.getName());
        return ApiResponse.success();
    }

    /**
     * 分页查询岗位
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link PageData }<{@link PositionResponse }>
     */
    @Override
    public PageData<PositionResponse> pageQueryPosition(PositionPageQueryRequest request, AccessUser accessUser) {
        Long tenantId = accessUser.currentTenantId();
        PageData<Position> pageData = CustomPage.execute(request, (page, req) -> {
            return super.lambdaQuery()
                .eq(Position::getTenantId, tenantId)
                .eq(Position::getIsDeleted, false)
                .like(EmptyUtil.isNotEmpty(req.getName()), Position::getName, req.getName())
                .orderByAsc(Position::getSort)
                .orderByDesc(Position::getId)
                .page(page);
        });

        PageData<PositionResponse> conversion = pageData.conversion(position -> {
            PositionResponse response = new PositionResponse();
            response.setId(position.getId());
            response.setName(position.getName());
            response.setSort(position.getSort());
            response.setIsDefault(position.getIsDefault());
            response.setUpdatedDate(position.getUpdateDate());
            return response;
        });
        Complete.start(conversion.getList())
            .build(PositionResponse::getId, PositionResponse::setEmployeeCount, employeeService::countPositionEmployee)
            .then().over();
        return conversion;
    }

    /**
     * 检查名称唯一
     *
     * @param tenantId 租户ID
     * @param id       ID
     * @param name     名称
     *
     * @return {@code true} 存在
     */
    private boolean checkNameUnique(Long tenantId, Long id, String name) {
        return super.lambdaQuery()
            .eq(Position::getTenantId, tenantId)
            .eq(Position::getName, name)
            .eq(Position::getIsDeleted, false)
            .ne(id != null, Position::getId, id)
            .exists();
    }

    /**
     * 初始化
     *
     * @param tenantContext 租户初始化上下文
     * @param accessUser    访问用户
     */
    @Override
    public void init(TenantInitContext tenantContext, AccessUser accessUser) {
        AtomicInteger count = new AtomicInteger(1);
        List<Position> positions = Arrays.stream(DefaultPosition.values()).map(p -> {
            Position position = new Position();
            position.setTenantId(tenantContext.getTenantId());
            position.setName(p.getDesc());
            position.setCreateDate(LocalDateTime.now());
            position.setUpdateDate(position.getCreateDate());
            position.setCreateBy(tenantContext.getTenantAdminId());
            position.setUpdateBy(tenantContext.getTenantAdminId());
            position.setSort(count.getAndIncrement());
            position.setIsDeleted(false);
            position.setIsDefault(true);
            position.setPositionCode(p.getValue());
            log.info("[租户初始化] init tenant {}, new default position {}", tenantContext.getTenantId(), p.getDesc());
            return position;
        }).toList();
        super.saveOrUpdateBatch(positions);
    }

    /**
     * 获取岗位名称映射
     *
     * @param ids ids
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    @Override
    public Map<Long, String> getPositionNameMap(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return Map.of();
        }
        return super.lambdaQuery()
            .select(Position::getId, Position::getName)
            .in(Position::getId, ids)
            .list()
            .stream()
            .collect(java.util.stream.Collectors.toMap(Position::getId, Position::getName));
    }

    /**
     * 获取岗位编码映射
     *
     * @param ids ids
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    @Override
    public Map<Long, String> getPositionCodeMap(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return Map.of();
        }
        return super.lambdaQuery()
            .select(Position::getId, Position::getPositionCode)
            .in(Position::getId, ids)
            .list()
            .stream()
            .collect(java.util.stream.Collectors.toMap(Position::getId, Position::getPositionCode));
    }

    /**
     * 获取岗位选项
     *
     * @param ids ids
     *
     * @return {@link List }<{@link LabelOption }>
     */
    @Override
    public List<LabelOption> getPositionOptions(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return List.of();
        }
        return super.lambdaQuery()
            .select(Position::getId, Position::getName, Position::getSort)
            .in(Position::getId, ids)
            .list().stream().sorted(Comparator.comparingInt(Position::getSort)).map(position -> new LabelOption(position.getId(), position.getName())).toList();
    }

    /**
     * 列出选项
     *
     * @param keyword  关键词
     * @param limit    限制
     * @param tenantId 租户ID
     *
     * @return {@link List }<{@link LabelOption }>
     */
    @Override
    public List<LabelOption> listOptions(String keyword, int limit, long tenantId) {
        return super.lambdaQuery()
            .select(Position::getId, Position::getName)
            .eq(Position::getTenantId, tenantId)
            .eq(Position::getIsDeleted, false)
            .like(EmptyUtil.isNotEmpty(keyword), Position::getName, keyword)
            .orderByAsc(Position::getSort)
            .last("limit " + limit)
            .list()
            .stream()
            .map(position -> {
                LabelOption option = new LabelOption();
                option.setKey(position.getId());
                option.setLabel(position.getName());
                return option;
            })
            .toList();
    }

    /**
     * 获取默认岗位映射
     *
     * @param defaultPositions 默认位置
     * @param tenantId         租户ID
     *
     * @return {@link Map }<{@link DefaultPosition }, {@link Long }>
     */
    @Override
    public Map<DefaultPosition, Long> getDefaultPositionMap(List<DefaultPosition> defaultPositions, long tenantId) {
        if (EmptyUtil.isEmpty(defaultPositions)) {
            return Map.of();
        }
        Map<String, DefaultPosition> map = Arrays.stream(DefaultPosition.values()).collect(Collectors.toMap(DefaultPosition::getValue, Function.identity()));
        List<String> codeList = defaultPositions.stream().map(DefaultPosition::getValue).toList();
        return super.lambdaQuery()
            .select(Position::getId, Position::getPositionCode)
            .eq(Position::getTenantId, tenantId)
            .eq(Position::getIsDeleted, false)
            .in(Position::getPositionCode, codeList)
            .list()
            .stream()
            .collect(java.util.stream.Collectors.toMap(position -> map.get(position.getPositionCode()), Position::getId));
    }

    /**
     * 获取默认岗位信息
     *
     * @param tenantId 租户ID
     *
     * @return {@link DefaultPositionInfoResponse }
     */
    @Override
    public DefaultPositionInfoResponse getDefaultPositionInfo(long tenantId) {
        List<Position> positionList = super.lambdaQuery()
            .select(Position::getId, Position::getName, Position::getPositionCode)
            .eq(Position::getTenantId, tenantId)
            .eq(Position::getIsDeleted, false)
            .eq(Position::getIsDefault, true)
            .list();
        DefaultPositionInfoResponse response = new DefaultPositionInfoResponse();
        Map<String, DefaultPosition> map = Arrays.stream(DefaultPosition.values()).collect(Collectors.toMap(DefaultPosition::getValue, Function.identity()));

        positionList.forEach(position -> {
            DefaultPosition defaultPosition = map.get(position.getPositionCode());
            switch (defaultPosition) {
                case ANCHOR -> {
                    response.setAnchorId(position.getId());
                    response.setAnchorName(position.getName());
                }
                case SUB_ANCHOR -> {
                    response.setSubAnchorId(position.getId());
                    response.setSubAnchorName(position.getName());
                }
                case OPERATION -> {
                    response.setOperationId(position.getId());
                    response.setOperationName(position.getName());
                }
                case CONTROL -> {
                    response.setControlId(position.getId());
                    response.setControlName(position.getName());
                }
                case TOU_CHER -> {
                    response.setTouCherId(position.getId());
                    response.setTouCherName(position.getName());
                }
                case EDITOR -> {
                    response.setEditorId(position.getId());
                    response.setEditorName(position.getName());
                }
                case GUEST -> {
                    response.setGuestId(position.getId());
                    response.setGuestName(position.getName());
                }
                case COMPLIANCE_SPECIALIST -> {
                    response.setComplianceSpecialistId(position.getId());
                    response.setComplianceSpecialistName(position.getName());
                }
                case COMPLIANCE_MANAGER -> {
                    response.setComplianceManagerId(position.getId());
                    response.setComplianceManagerName(position.getName());
                }
                case COMPLIANCE_LEADER -> {
                    response.setComplianceLeaderId(position.getId());
                    response.setComplianceLeaderName(position.getName());
                }
                default -> {
                }
            }
        });
        return response;
    }


    /**
     * 获取岗位映射
     *
     * @param ids ids
     *
     * @return {@link Map }<{@link Long }, {@link Position }>
     */
    @Override
    public Map<Long, Position> getPositionMap(List<Long> ids) {
        if (EmptyUtil.isNotEmpty(ids)) {
            return super.lambdaQuery()
                .select(Position::getId, Position::getName, Position::getPositionCode, Position::getSort, Position::getIsDefault)
                .in(Position::getId, ids)
                .list()
                .stream()
                .collect(Collectors.toMap(Position::getId, Function.identity()));
        }
        return Map.of();
    }
}
