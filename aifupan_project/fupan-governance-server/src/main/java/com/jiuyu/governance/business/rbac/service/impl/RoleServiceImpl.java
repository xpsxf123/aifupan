package com.jiuyu.governance.business.rbac.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.mybatispuls.util.CustomPage;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.framework.util.CollUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.mapper.MenuRoleMapper;
import com.jiuyu.governance.business.rbac.mapper.RoleMapper;
import com.jiuyu.governance.business.rbac.mapper.UserRoleMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.MenuRole;
import com.jiuyu.governance.business.rbac.pojo.entity.Role;
import com.jiuyu.governance.business.rbac.pojo.entity.UserRole;
import com.jiuyu.governance.business.rbac.pojo.request.RoleAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RolePageQueryRequest;
import com.jiuyu.governance.business.rbac.pojo.request.RoleUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;
import com.jiuyu.governance.business.rbac.pojo.response.RoleDetailResponse;
import com.jiuyu.governance.business.rbac.pojo.response.RoleResponse;
import com.jiuyu.governance.business.rbac.service.MenuService;
import com.jiuyu.governance.business.rbac.service.RoleService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.init.InitializationTenant;
import com.jiuyu.governance.common.init.TenantInitContext;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.bo.CountData;
import com.jiuyu.governance.common.pojo.bo.LabelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService, InitializationTenant {

    private final MenuRoleMapper menuRoleMapper;

    private final MenuService menuService;

    private final StringRedisTemplate redisTemplate;

    private final UserRoleMapper userRoleMapper;

    /**
     * 缓存前缀
     */
    private final String cachePrefix = "system:role:menu:";


    /**
     * 获取角色缓存key
     *
     * @param roleId 角色ID
     */
    private String getCacheKey(long roleId) {
        return cachePrefix + roleId;
    }


    /**
     * 初始化
     *
     * @param tenantContext 租户初始化上下文
     * @param accessUser    访问用户
     */
    @Override
    public void init(TenantInitContext tenantContext, AccessUser accessUser) {
        super.lambdaQuery()
            .eq(Role::getTenantId, tenantContext.getTenantId())
            .eq(Role::getIsDeleted, false)
            .last("limit 1").oneOpt().orElseGet(() -> {
                Role role = new Role();
                role.setTenantId(tenantContext.getTenantId());
                role.setName("超级管理员");
                role.setIsDefault(true);
                role.setCreateDate(LocalDateTime.now());
                role.setUpdateDate(role.getCreateDate());
                role.setIsDeleted(false);

                Role ordinary = new Role();
                ordinary.setTenantId(tenantContext.getTenantId());
                ordinary.setName("普通使用者");
                ordinary.setIsDefault(false);
                ordinary.setCreateDate(LocalDateTime.now());
                ordinary.setUpdateDate(role.getCreateDate());
                ordinary.setIsDeleted(false);
                // 初始化管理员和普通使用者
                super.saveBatch(List.of(role, ordinary));

                log.info("[角色] 初始化角色: {} , {}, tenantId: {}", role.getName(), ordinary.getName(), tenantContext.getTenantId());
                List<Long> menuIds = menuService.listAllIds();
                List<MenuRole> roleMenuList = new ArrayList<>();
                if (EmptyUtil.isNotEmpty(menuIds)) {
                    // 新的菜单ID
                    LocalDateTime now = LocalDateTime.now();
                    List<MenuRole> newJoinMenuList = buildMenuRoleList(menuIds, role.getId(), now);
                    log.info("[角色] 初始化角色: {} 分配菜单权限，菜单数量: {}", role.getName(), newJoinMenuList.size());
                    roleMenuList.addAll(newJoinMenuList);
                }
                List<Long> ordinaryMenuIds = menuService.getOrdinaryMenuIds();
                if (EmptyUtil.isNotEmpty(ordinaryMenuIds)) {
                    // 菜单ID
                    LocalDateTime now = LocalDateTime.now();
                    List<MenuRole> newJoinMenuList = buildMenuRoleList(ordinaryMenuIds, ordinary.getId(), now);
                    log.info("[角色] 添加角色: {} 菜单权限，菜单数量: {}", ordinary.getName(), newJoinMenuList.size());
                    roleMenuList.addAll(newJoinMenuList);
                }
                if (EmptyUtil.isNotEmpty(roleMenuList)) {
                    menuRoleMapper.insertBatch(roleMenuList);
                }
                return role;
            });
    }


    /**
     * 获取默认角色ID
     *
     * @param tenantId 租户ID
     *
     * @return 默认角色ID
     */
    @Override
    public long getDefaultRoleId(long tenantId) {
        return super.lambdaQuery()
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getIsDefault, true)
            .eq(Role::getIsDeleted, false)
            .select(Role::getId)
            .last("limit 1").oneOpt().map(Role::getId).orElse(0L);
    }

    /**
     * 构建菜单角色关联列表
     *
     * @param menuIds 菜单ID列表
     * @param roleId 角色ID
     * @param now 当前时间
     * @return 菜单角色关联列表
     */
    private List<MenuRole> buildMenuRoleList(List<Long> menuIds, Long roleId, LocalDateTime now) {
        if (EmptyUtil.isEmpty(menuIds) || roleId == null) {
            return Collections.emptyList();
        }

        return menuIds.stream()
            .filter(menuId -> menuId != null && menuId > 0)
            .map(menuId -> {
                MenuRole menuRole = new MenuRole();
                menuRole.setId(IdUtil.getSnowflakeNextId());
                menuRole.setRoleId(roleId);
                menuRole.setMenuId(menuId);
                menuRole.setCreateDate(now);
                menuRole.setUpdateDate(now);
                menuRole.setIsDeleted(false);
                return menuRole;
            })
            .toList();
    }

    /**
     * 添加角色
     *
     * @param request  添加请求
     * @param tenantId 租户ID
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> addRole(RoleAddRequest request, Long tenantId) {
        // 校验同名角色
        boolean exists = super.lambdaQuery()
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getName, request.getName())
            .eq(Role::getIsDeleted, false).exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "角色名已存在");
        }

        Role role = BeanUtil.copyProperties(request, Role.class);
        role.setTenantId(tenantId);
        role.setCreateDate(LocalDateTime.now());
        role.setUpdateDate(LocalDateTime.now());
        role.setIsDeleted(false);
        if (role.getIsDefault() == null) {
            role.setIsDefault(false);
        }

        this.save(role);
        log.info("[角色] 新增角色: {}, tenantId: {}", role.getName(), tenantId);
        if (EmptyUtil.isNotEmpty(request.getMenuIds())) {
            this.assignMenuToRole(role.getId(), request.getMenuIds(), tenantId);
        }
        return ApiResponse.success();
    }

    /**
     * 更新角色
     *
     * @param request  更新请求
     * @param tenantId 租户ID
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> updateRole(RoleUpdateRequest request, Long tenantId) {
        Role exist = this.getById(request.getId());
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "角色不存在或无权限修改");
        }

        // 校验同名角色
        boolean exists = this.lambdaQuery()
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getName, request.getName())
            .eq(Role::getIsDeleted, false)
            .ne(Role::getId, request.getId()).exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.DATA_DUPLICATED.getCode(), "角色名已存在");
        }

        Role updateRole = BeanUtil.copyProperties(request, Role.class);
        updateRole.setUpdateDate(LocalDateTime.now());
        this.updateById(updateRole);
        log.info("[角色管理] 修改角色: {}, tenantId: {}", request.getId(), tenantId);
        if (EmptyUtil.isNotEmpty(request.getMenuIds())) {
            this.assignMenuToRole(request.getId(), request.getMenuIds(), tenantId);
        }
        return ApiResponse.success();
    }

    /**
     * 删除角色
     *
     * @param id       角色ID
     * @param tenantId 租户ID
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> deleteRole(Long id, Long tenantId) {
        Role exist = this.getById(id);
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "角色不存在或无权限删除");
        }

        if (exist.getIsDefault() != null && exist.getIsDefault()) {
            return ApiResponse.failed(BizErrorCode.NO_POWER.getCode(), "默认角色不能删除");
        }
        boolean hasRelationUser = ChainWrappers.lambdaQueryChain(userRoleMapper)
            .eq(UserRole::getRoleId, id)
            .eq(UserRole::getIsDeleted, false)
            .exists();
        if (hasRelationUser) {
            return ApiResponse.failed(BizErrorCode.HAS_DEPENDENCY.getCode(), "角色下有关联人员，无法删除");
        }

        // 逻辑删除角色
        exist.setIsDeleted(true);
        exist.setUpdateDate(LocalDateTime.now());
        this.updateById(exist);

        // 删除角色关联的菜单关系
        ChainWrappers.lambdaUpdateChain(menuRoleMapper)
            .eq(MenuRole::getRoleId, id)
            .set(MenuRole::getIsDeleted, true)
            .set(MenuRole::getUpdateDate, LocalDateTime.now())
            .update();
        log.info("[角色管理] 删除角色及菜单关联: {}, tenantId: {}", id, tenantId);

        redisTemplate.delete(this.getCacheKey(id));
        return ApiResponse.success();
    }


    /**
     * 获取角色用户数量
     *
     * @param roleIds 角色ID列表
     *
     * @return 角色用户数量
     */
    private Map<Long, Integer> getRoleUserCountMap(List<Long> roleIds) {
        if (EmptyUtil.isEmpty(roleIds)) {
            return Map.of();
        }
        List<CountData> countDataList = userRoleMapper.selectRoleUserCount(roleIds);
        if (EmptyUtil.isEmpty(countDataList)) {
            return Map.of();
        }
        return countDataList.stream().collect(Collectors.toMap(CountData::getId, c -> c.getCount().intValue()));
    }


    /**
     * 分页查询角色
     *
     * @param request  查询请求
     * @param tenantId 租户ID
     *
     * @return 角色分页数据
     */
    @Override
    public PageData<RoleResponse> pageQueryRole(RolePageQueryRequest request, Long tenantId) {
        PageData<Role> pageData = CustomPage.execute(request, (page, req) -> {
            return super.lambdaQuery()
                .eq(Role::getTenantId, tenantId)
                .eq(Role::getIsDeleted, false)
                .like(EmptyUtil.isNotEmpty(req.getName()), Role::getName, req.getName())
                .orderByDesc(Role::getId)
                .page(page);
        });

        PageData<RoleResponse> conversion = pageData.conversion(role -> BeanUtil.copyProperties(role, RoleResponse.class));
        Complete.start(conversion.getList())
            .build(RoleResponse::getId, RoleResponse::setUserCount, this::getRoleUserCountMap)
            .then().over();
        return conversion;
    }


    /**
     * 判断角色是否存在
     *
     * @param roleId   角色ID
     * @param tenantId 租户ID
     *
     * @return 角色是否存在
     */
    @Override
    public boolean hasRole(long roleId, long tenantId) {
        return super.lambdaQuery()
            .eq(Role::getId, roleId)
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getIsDeleted, false)
            .exists();
    }

    /**
     * 给角色分配菜单
     *
     * @param roleId   角色ID
     * @param menuIds  菜单id
     * @param tenantId 租户ID
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public void assignMenuToRole(long roleId, List<Long> menuIds, Long tenantId) {
        if (EmptyUtil.isEmpty(menuIds)) {
            ChainWrappers.lambdaUpdateChain(menuRoleMapper)
                .eq(MenuRole::getRoleId, roleId)
                .set(MenuRole::getIsDeleted, true)
                .set(MenuRole::getUpdateDate, LocalDateTime.now())
                .update();
            log.info("[角色] 角色: {}, tenantId: {} 删除菜单关联", roleId, tenantId);
            redisTemplate.delete(this.getCacheKey(roleId));
            return;
        }
        // 原来的菜单ID
        List<Long> dbMenuIds = ChainWrappers.lambdaQueryChain(menuRoleMapper)
            .select(MenuRole::getMenuId)
            .eq(MenuRole::getRoleId, roleId)
            .eq(MenuRole::getIsDeleted, false)
            .list().stream().map(MenuRole::getMenuId).toList();

        // 新的菜单ID
        List<MenuRole> newJoinMenuList = menuIds.stream().filter(menuId -> !dbMenuIds.contains(menuId)).map(menuId -> {
            MenuRole menuRole = new MenuRole();
            menuRole.setId(IdUtil.getSnowflakeNextId());
            menuRole.setRoleId(roleId);
            menuRole.setMenuId(menuId);
            menuRole.setCreateDate(LocalDateTime.now());
            menuRole.setUpdateDate(LocalDateTime.now());
            menuRole.setIsDeleted(false);
            return menuRole;
        }).toList();

        // 需要删除的菜单ID
        List<Long> deleteMenuIds = dbMenuIds.stream().filter(menuId -> !menuIds.contains(menuId)).toList();
        if (EmptyUtil.isNotEmpty(deleteMenuIds)) {
            ChainWrappers.lambdaUpdateChain(menuRoleMapper)
                .eq(MenuRole::getRoleId, roleId)
                .in(MenuRole::getMenuId, deleteMenuIds)
                .set(MenuRole::getIsDeleted, true)
                .set(MenuRole::getUpdateDate, LocalDateTime.now())
                .update();
            log.info("[角色] 角色: {}, tenantId: {} 删除菜单关联: {}", roleId, tenantId, deleteMenuIds);
        }
        if (EmptyUtil.isNotEmpty(newJoinMenuList)) {
            menuRoleMapper.insertBatch(newJoinMenuList);
            log.info("[角色管理] 角色分配菜单完成: roleId={}, tenantId={}, menus={}", roleId, tenantId, newJoinMenuList.size());
        }
        redisTemplate.delete(this.getCacheKey(roleId));
    }


    /**
     * 获取角色树
     *
     * @param roleIds  角色ID列表
     * @param tenantId 租户ID
     *
     * @return 角色树
     */
    @Override
    public List<MenuTreeResponse> getRolesTree(List<Long> roleIds, long tenantId) {
        if (EmptyUtil.isEmpty(roleIds)) {
            return List.of();
        }
        List<List<MenuTreeResponse>> result = queruRoleMenuList(roleIds);
        return CollUtil.tree(result.stream().flatMap(List::stream).toList(), 0L);
    }

    /**
     * 查询角色菜单列表
     * <p>
     * 批量查询多个角色的菜单树列表，优先从 Redis 缓存中获取，缓存未命中则从数据库查询并回填缓存。
     * 采用批量查询方式避免 N+1 问题，提高查询效率。
     *
     * @param roleIds 角色 ID 列表，需要查询菜单的角色标识集合
     *
     * @return List<List < MenuTreeResponse>> 角色菜单树列表，外层 List 每个元素对应一个角色的菜单树列表
     */
    private List<List<MenuTreeResponse>> queruRoleMenuList(List<Long> roleIds) {
        Map<Long, String> roleCacheKeyMap = roleIds.stream().collect(Collectors.toMap(Function.identity(), this::getCacheKey));
        List<String> cacheValueList = redisTemplate.opsForValue().multiGet(roleCacheKeyMap.values());
        List<List<MenuTreeResponse>> result = new ArrayList<>();


        List<Long> notCacheRoleIds = new ArrayList<>();
        if (EmptyUtil.isNotEmpty(cacheValueList)) {
            for (int i = 0; i < roleIds.size(); i++) {
                String cacheValue = cacheValueList.get(i);
                if (cacheValue == null) {
                    notCacheRoleIds.add(roleIds.get(i));
                } else if (JsonTemplate.isJsonArray(cacheValue)) {
                    result.add(JsonTemplate.toList(cacheValue, MenuTreeResponse.class));
                }
            }
        } else {
            notCacheRoleIds.addAll(roleIds);
        }

        if (EmptyUtil.isNotEmpty(notCacheRoleIds)) {
            List<MenuRole> menuRoleList = new BatchQuery<>((limit, idx) -> {
                return ChainWrappers.lambdaQueryChain(menuRoleMapper)
                    .select(MenuRole::getId, MenuRole::getMenuId, MenuRole::getRoleId)
                    .gt(idx != null, MenuRole::getId, idx)
                    .in(MenuRole::getRoleId, notCacheRoleIds)
                    .eq(MenuRole::getIsDeleted, false)
                    .last("limit " + limit)
                    .list();
            }, MenuRole::getId).get();
            Map<Long, List<Long>> roleMoleMap = menuRoleList.stream().collect(Collectors.groupingBy(MenuRole::getRoleId, Collectors.mapping(MenuRole::getMenuId, Collectors.toList())));
            List<Long> menuIds = menuRoleList.stream().map(MenuRole::getMenuId).distinct().toList();
            List<MenuTreeResponse> menuList = menuService.listInIds(menuIds);
            Map<Long, MenuTreeResponse> menuMap = menuList.stream().collect(Collectors.toMap(MenuTreeResponse::getId, Function.identity()));
            notCacheRoleIds.forEach(roleId -> {
                String cacheValue = "";
                List<Long> roleMenuIds = roleMoleMap.getOrDefault(roleId, List.of());
                List<MenuTreeResponse> roleMenuList = roleMenuIds.stream().map(menuMap::get).filter(Objects::nonNull).toList();
                if (EmptyUtil.isNotEmpty(roleMenuList)) {
                    cacheValue = JsonTemplate.toJson(roleMenuList);
                }
                redisTemplate.opsForValue().set(roleCacheKeyMap.get(roleId), cacheValue, Duration.ofHours(3));
            });


            result.add(menuList);
        }
        return result;
    }


    /**
     * 获取角色权限码
     *
     * @param roleIds  角色ID列表
     * @param tenantId 租户ID
     *
     * @return 角色权限码列表
     */
    @Override
    public List<String> getRolesPermissionCode(List<Long> roleIds, long tenantId) {
        if (EmptyUtil.isEmpty(roleIds)) {
            return List.of();
        }
        return queruRoleMenuList(roleIds).stream().flatMap(List::stream).map(MenuTreeResponse::getPermissionCode).filter(EmptyUtil::isNotEmpty).distinct().toList();
    }

    /**
     * 获取角色详情
     *
     * @param roleId   角色ID
     * @param tenantId 租户ID
     *
     * @return 角色拥有的菜单ID列表
     */
    @Override
    public RoleDetailResponse getRoleMenus(Long roleId, Long tenantId) {
        Role exist = this.getById(roleId);
        if (exist == null || exist.getIsDeleted() || !exist.getTenantId().equals(tenantId)) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "角色不存在或无权限查看");
        }
        List<Long> menuIds = this.getRoleMenuIds(roleId);
        RoleDetailResponse response = new RoleDetailResponse();
        response.setRoleId(roleId);
        response.setName(exist.getName());
        response.setMenuTree(menuService.buildTree(menuIds));
        return response;
    }


    /**
     * 角色下拉选择
     *
     * @param keyword  关键字
     * @param tenantId 租户ID
     * @param limit    限制数量
     *
     * @return 角色下拉选项
     */
    @Override
    public List<LabelOption> options(String keyword, long tenantId, Integer limit) {
        limit = limit == null ? 50 : limit;

        return super.lambdaQuery()
            .select(Role::getId, Role::getName)
            .like(EmptyUtil.isNotEmpty(keyword), Role::getName, keyword)
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getIsDeleted, false)
            .last("limit " + limit)
            .list()
            .stream()
            .map(r -> new LabelOption(r.getId(), r.getName()))
            .collect(Collectors.toList());
    }

    /**
     * 获取角色的菜单ID列表
     *
     * @param roleId 角色ID
     *
     * @return 菜单ID列表
     */
    private List<Long> getRoleMenuIds(long roleId) {
        return ChainWrappers.lambdaQueryChain(menuRoleMapper)
            .select(MenuRole::getMenuId)
            .eq(MenuRole::getRoleId, roleId)
            .eq(MenuRole::getIsDeleted, false)
            .list().stream().map(MenuRole::getMenuId).toList();
    }

    @Override
    public Map<Long, String> getRoleNameMap(List<Long> ids) {
        if (EmptyUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return super.lambdaQuery()
            .select(Role::getId, Role::getName)
            .in(Role::getId, ids)
            .list()
            .stream()
            .collect(Collectors.toMap(Role::getId, Role::getName));
    }


    /**
     * 清空所有缓存
     */
    @Override
    public void clearAllCache() {
        log.info("[角色] role cache all clear...");
        BatchQuery<Long, Role> batchQuery = new BatchQuery<>((limit, idx) -> {
            return super.lambdaQuery()
                .select(Role::getId)
                .gt(idx != null, Role::getId, idx)
                .eq(Role::getIsDeleted, false)
                .last("limit " + limit)
                .list();
        }, Role::getId);
        batchQuery.consumer(roleList -> {
            List<String> cacheKeyList = roleList.stream().map(Role::getId).map(roleId -> {
                log.info("[角色] role cache clear... roleId: {}", roleId);
                return this.getCacheKey(roleId);
            }).toList();
            redisTemplate.delete(cacheKeyList);
        });
        batchQuery.run();
    }

    /**
     * 判断角色是否为默认角色
     *
     * @param roleIds  角色ID列表
     * @param tenantId 租户ID
     */
    @Override
    public boolean hasDefaultRole(List<Long> roleIds, long tenantId) {
        if (EmptyUtil.isEmpty(roleIds)) {
            return false;
        }
        return super.lambdaQuery()
            .eq(Role::getTenantId, tenantId)
            .eq(Role::getIsDefault, true)
            .in(Role::getId, roleIds)
            .eq(Role::getIsDeleted, false)
            .last("limit 1")
            .exists();
    }
}
