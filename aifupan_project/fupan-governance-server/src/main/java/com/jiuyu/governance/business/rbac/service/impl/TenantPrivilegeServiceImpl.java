package com.jiuyu.governance.business.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.mapper.EmployeeMapper;
import com.jiuyu.governance.business.rbac.mapper.TenantPrivilegeMapper;
import com.jiuyu.governance.business.rbac.pojo.constants.AccountStatus;
import com.jiuyu.governance.business.rbac.pojo.entity.Employee;
import com.jiuyu.governance.business.rbac.pojo.entity.TenantPrivilege;
import com.jiuyu.governance.business.rbac.pojo.response.TenantStatusResponse;
import com.jiuyu.governance.business.rbac.service.TenantPrivilegeService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.openfeign.replay.UserAccountService;
import com.jiuyu.governance.openfeign.replay.response.ReplayAccountDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 租户权益服务 实现
 *
 * @author HeHui
 * @date 2026-03-27 15:16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TenantPrivilegeServiceImpl extends ServiceImpl<TenantPrivilegeMapper, TenantPrivilege> implements TenantPrivilegeService, InitializationTenant {

    private final UserAccountService accountService;

    private final EmployeeMapper employeeMapper;

    private final StringRedisTemplate redisTemplate;


    private final String cachePrefix = "tenant:privilege:available";


    /**
     * 判断租户是否可用
     * <p>
     * 该方法通过检查租户的账户状态和套餐等级来判断租户是否可用。
     * 如果租户状态异常或超过 10 分钟未验证，则会重新查询主账户信息进行校验。
     * </p>
     *
     * @param tenantId 租户 ID
     *
     * @return true 表示可用，false 表示不可用
     */
    @Override
    public boolean available(long tenantId) {
        String cacheKey = cachePrefix + tenantId;
        String value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            return Objects.equals(value, "true");
        }
        boolean status = queryStatus(tenantId, cacheKey, false);
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(status), Duration.ofMinutes(10));
        return status;
    }

    /**
     * 获取可用租户
     *
     * @param tenantIds    租户 ID 列表
     * @param forceRefresh 是否强制刷新
     *
     * @return 可用租户 ID 列表
     */
    @Override
    public List<Long> availableTenantIds(List<Long> tenantIds, boolean forceRefresh) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return List.of();
        }
        List<Long> notCacheIds = new ArrayList<>();
        Map<Long,  Boolean> statusMap = new HashMap<>();
        if (forceRefresh) {
            notCacheIds.addAll(tenantIds);
        } else {
            List<String> cacheKeys = tenantIds.stream().map(tenantId -> cachePrefix + tenantId).toList();
            List<String> bodyList = redisTemplate.opsForValue().multiGet(cacheKeys);
            if (EmptyUtil.isEmpty(bodyList)) {
                notCacheIds.addAll(tenantIds);
            } else {
                for (int i = 0; i < tenantIds.size(); i++) {
                    String body = bodyList.get(i);
                    if (body == null) {
                        notCacheIds.add(tenantIds.get(i));
                    } else {
                        statusMap.put(tenantIds.get(i), Boolean.parseBoolean(body));
                    }
                }
            }
        }
        if (EmptyUtil.isNotEmpty(notCacheIds)) {
            for (Long tenantId : notCacheIds) {
                boolean status = this.queryStatus(tenantId, cachePrefix + tenantId, forceRefresh);
                statusMap.put(tenantId, status);
            }
        }
        return statusMap.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
    }

    /**
     * 查询租户权益状态
     * <p>
     * 该方法会检查租户的权益信息，包括账户状态、套餐等级等。
     * 如果状态缓存过期（超过10分钟），会重新调用远程服务验证主账户信息。
     * </p>
     *
     * @param tenantId 租户ID
     * @param cacheKey Redis缓存键，用于存储租户状态缓存
     * @param forceRefresh 是否强制刷新缓存
     * @return true-租户权益正常可用，false-租户权益异常或不可用
     */
    private boolean queryStatus(long tenantId, String cacheKey, boolean forceRefresh) {
        TenantPrivilege tenantPrivilege = super.getById(tenantId);
        if (tenantPrivilege == null) {
            redisTemplate.opsForValue().set(cacheKey, "false", Duration.ofMinutes(10));
            return false;
        }

        if (Objects.equals(tenantPrivilege.getAccountStatus(), AccountStatus.FREEZE.getValue())) {
            redisTemplate.opsForValue().set(cacheKey, "false", Duration.ofMinutes(10));
            return false;
        }

        boolean status = Objects.equals(tenantPrivilege.getAccountStatus(), AccountStatus.NORMAL.getValue());
        LocalDateTime lastTime = LocalDateTime.now().minusMinutes(10);

        // 强制刷新缓存 > 判断是否需要重新验证账户状态：状态正常但开启时间超过10分钟，或状态异常但冻结时间超过10分钟
        if (forceRefresh || (status && tenantPrivilege.getRecentlyOpenTime().isBefore(lastTime)) || (!status && tenantPrivilege.getRecentlyFreezeTime().isBefore(lastTime))) {
            Optional<Long> accountOptional = ChainWrappers.lambdaQueryChain(employeeMapper)
                .eq(Employee::getTenantId, tenantId)
                .eq(Employee::getHoldTenant, true)
                .gt(Employee::getConnectorClientUserId, 0)
                .select(Employee::getConnectorClientUserId)
                .oneOpt().map(Employee::getConnectorClientUserId);

            // 未找到关联的主账户ID，标记租户过期
            if (accountOptional.isEmpty()) {
                this.expired(List.of(tenantId));
                return false;
            }

            ApiResponse<ReplayAccountDetailResponse> apiResponse = accountService.getMainAccountDetail(accountOptional.get());

            // 远程服务调用失败，返回当前缓存状态
            if (apiResponse.failed()) {
                log.warn("[租户权益] get main account detail failed, tenant: {}, accountId: {}, . msg: {}", tenantId, accountOptional.get(), apiResponse.getMsg());
                return status;
            }

            // 返回数据为空，标记租户过期
            if (apiResponse.getData() == null) {
                log.warn("[租户权益] get main account detail result empty, tenant: {}, accountId: {} ", tenantId, accountOptional.get());
                this.expired(List.of(tenantId));
                return false;
            }

            ReplayAccountDetailResponse account = apiResponse.getData();

            // 套餐等级为空，标记租户过期
            if (account.getPackageLevel() == null) {
                log.warn("[租户权益] get main account detail result packageLevel null, tenant: {}, accountId: {}", tenantId, accountOptional.get());
                this.expired(List.of(tenantId));
                return false;
            }

            // 套餐等级低于最小治理等级要求，标记租户过期
            if (account.getPackageLevel() < InitializationTenant.MIN_GOVERNANCE_LEVEL) {
                log.warn("[租户权益] get main account detail result {} packageLevel {} < {}, tenant: {}, accountId: {}", account.getPackageName(), account.getPackageLevel(), InitializationTenant.MIN_GOVERNANCE_LEVEL, tenantId, accountOptional.get());
                this.expired(List.of(tenantId));
                return false;
            }
            if (account.getTenantId() == null || account.getTenantId() != tenantId) {
                log.warn("[租户权益] get main account detail result tenantId {} != {}, tenant: {}, accountId: {}", account.getTenantId(), tenantId, tenantId, accountOptional.get());
                this.expired(List.of(tenantId));
                return false;
            }

            // 验证通过，更新租户状态为正常并刷新开启时间
            super.lambdaUpdate()
                .eq(TenantPrivilege::getId, tenantId)
                .set(TenantPrivilege::getRecentlyOpenTime, LocalDateTime.now())
                .set(TenantPrivilege::getAccountStatus, AccountStatus.NORMAL.getValue())
                .update();
            log.info("[租户权益] get main account detail result packageLevel {}, tenant: {}, accountId: {}", account.getPackageLevel(), tenantId, accountOptional.get());
            return true;
        }

        return status;
    }

    /**
     * 设置租户可用
     *
     * @param tenantId 租户 ID
     */
    @Override
    public void setAvailable(long tenantId) {
        boolean update = super.lambdaUpdate()
            .eq(TenantPrivilege::getId, tenantId)
            .eq(TenantPrivilege::getAccountStatus, AccountStatus.DISABLED.getValue())
            .set(TenantPrivilege::getRecentlyOpenTime, LocalDateTime.now())
            .set(TenantPrivilege::getAccountStatus, AccountStatus.NORMAL.getValue())
            .update();
        if (update) {
            log.info("[租户权益] set tenant available, tenantId: {}", tenantId);
            redisTemplate.opsForValue().set(cachePrefix + tenantId, "true", Duration.ofMinutes(10));
        }
    }


    /**
     * 冻结租户
     *
     * @param tenantId 租户 ID
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Override
    public ApiResponse<Void> freeze(long tenantId) {
        boolean update = super.lambdaUpdate()
            .eq(TenantPrivilege::getId, tenantId)
            .ne(TenantPrivilege::getAccountStatus, AccountStatus.FREEZE.getValue())
            .set(TenantPrivilege::getRecentlyFreezeTime, LocalDateTime.now())
            .set(TenantPrivilege::getAccountStatus, AccountStatus.FREEZE.getValue())
            .update();
        if (update) {
            log.info("[租户权益] freeze tenant, tenantId: {}", tenantId);
            redisTemplate.opsForValue().set(cachePrefix + tenantId, "false", Duration.ofMinutes(10));
        }
        return ApiResponse.success();
    }


    /**
     * 获取租户状态
     *
     * @param tenantIds 租户 ID 列表
     *
     * @return {@link List }<{@link TenantStatusResponse }>
     */
    @Override
    public List<TenantStatusResponse> getTenantStatus(List<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .in(TenantPrivilege::getId, tenantIds)
            .select(TenantPrivilege::getId, TenantPrivilege::getAccountStatus)
            .list()
            .stream()
            .map(tenantPrivilege -> {
                TenantStatusResponse response = new TenantStatusResponse();
                response.setId(tenantPrivilege.getId());
                response.setAccountStatus(tenantPrivilege.getAccountStatus());
                return response;
            })
            .toList();
    }

    /**
     * 判断租户是否存在
     *
     * @param tenantId 租户 ID
     *
     * @return true 存在，false 不存在
     */
    @Override
    public boolean hasTenant(long tenantId) {
        return super.lambdaQuery().eq(TenantPrivilege::getId, tenantId).exists();
    }

    /**
     * 租户版本过期
     *
     * @param tenantIds 租户 ID 列表
     */
    @Override
    public void expired(List<Long> tenantIds) {
        if (EmptyUtil.isEmpty(tenantIds)) {
            return;
        }
        super.lambdaUpdate()
            .in(TenantPrivilege::getId, tenantIds)
            .set(TenantPrivilege::getRecentlyFreezeTime, LocalDateTime.now())
            .set(TenantPrivilege::getAccountStatus, AccountStatus.DISABLED.getValue())
            .update();
        log.info("[租户权益] tenant version expired, tenantIds: {}", tenantIds);
        redisTemplate.delete(tenantIds.stream().map(id -> cachePrefix + id).toList());
    }


    /**
     * 加载可用租户
     *
     * @param idx   索引
     * @param limit 限制
     *
     * @return {@link List }<{@link Long }>
     */
    @Override
    public List<Long> loadAvailable(Long idx, int limit) {
        return super.lambdaQuery()
            .gt(idx != null, TenantPrivilege::getId, idx)
            .eq(TenantPrivilege::getAccountStatus, AccountStatus.NORMAL.getValue())
            .last("limit " + idx + ", " + limit)
            .select(TenantPrivilege::getId)
            .list()
            .stream()
            .map(TenantPrivilege::getId)
            .toList();
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
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.PARAM_INVALID, "缺少租户ID");
        }
        TenantPrivilege tenantPrivilege = super.getById(tenantId);
        if (tenantPrivilege == null) {
            tenantPrivilege = new TenantPrivilege();
            tenantPrivilege.setId(tenantId);
            tenantPrivilege.setAccountStatus(AccountStatus.NORMAL.getValue());
            tenantPrivilege.setFirstOpenTime(LocalDateTime.now());
            tenantPrivilege.setRecentlyOpenTime(LocalDateTime.now());
            tenantPrivilege.setFirstOpenUserId(accessUser.userId());
            tenantPrivilege.setRecentlyOpenUserId(accessUser.userId());
            super.getBaseMapper().insert(tenantPrivilege);
            log.info("[租户权益] opening an account for the first time, tenantId: {}", tenantId);
            //tenantStatusCache.put(tenantId, true);
        } else {
            tenantPrivilege.setRecentlyOpenTime(LocalDateTime.now());
            tenantPrivilege.setRecentlyOpenUserId(accessUser.userId());
            tenantPrivilege.setAccountStatus(AccountStatus.NORMAL.getValue());
            super.updateById(tenantPrivilege);
            log.info("[租户权益] reopen an account, tenantId: {}", tenantId);
            //tenantStatusCache.put(tenantId, true);
        }
    }
}
