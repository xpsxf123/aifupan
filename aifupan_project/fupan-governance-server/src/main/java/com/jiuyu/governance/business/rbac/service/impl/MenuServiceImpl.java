package com.jiuyu.governance.business.rbac.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.CollUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.business.rbac.mapper.MenuMapper;
import com.jiuyu.governance.business.rbac.pojo.constants.MenuType;
import com.jiuyu.governance.business.rbac.pojo.entity.Menu;
import com.jiuyu.governance.business.rbac.pojo.request.MenuAddRequest;
import com.jiuyu.governance.business.rbac.pojo.request.MenuUpdateRequest;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;
import com.jiuyu.governance.business.rbac.service.MenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 菜单服务实现类
 *
 * @author HeHui
 * @date 2026-03-18
 */
@Service
@Slf4j
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    // 个人相关权限菜单
    private final List<String>  ordinaryMenuCodes = List.of("my:schedule:list", "my:performance:list", "my:profile:page");


    /**
     * 添加菜单
     *
     * @param request 添加请求
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public ApiResponse<Void> addMenu(MenuAddRequest request) {
        // 功能类型必须有权限码
        if (request.getType() == MenuType.FUNCTION) {
            if (EmptyUtil.isEmpty(request.getPermissionCode().trim())) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "功能类型必须填写权限码");
            }
        }
        List<Long> parentPathIds = null;
        // 校验父节点是否存在
        if (request.getParentId() != null && request.getParentId() != 0) {
            Optional<Menu> optionalMenu = super.lambdaQuery().eq(Menu::getId, request.getParentId())
                .select(Menu::getParentPathIds)
                .eq(Menu::getIsDeleted, false)
                .oneOpt();
            if (optionalMenu.isEmpty()) {
                return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "父菜单不存在");
            }
            parentPathIds = optionalMenu.get().getParentPathIds();
            if (EmptyUtil.isNotEmpty(parentPathIds) && parentPathIds.size() <= 10) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "菜单层级不能超过10级");
            }
        }
        if (EmptyUtil.isNotEmpty(parentPathIds)) {
            parentPathIds = new ArrayList<>(parentPathIds);
            parentPathIds.add(request.getParentId());
        }
        Menu menu = BeanUtil.copyProperties(request, Menu.class);
        menu.setCreateDate(LocalDateTime.now());
        menu.setUpdateDate(LocalDateTime.now());
        menu.setIsDeleted(false);
        menu.setParentPathIds(parentPathIds);
        if (menu.getPermissionCode() == null) {
            menu.setPermissionCode("");
        }
        this.save(menu);
        log.info("[菜单] add new menu, {} - {}", request.getType().getDesc(), request.getName());
        // todo 清除缓存
        return ApiResponse.success();
    }

    /**
     * 修改菜单
     *
     * @param request 修改请求
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> updateMenu(MenuUpdateRequest request) {
        Menu exist = this.getById(request.getId());
        if (exist == null || exist.getIsDeleted()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "菜单不存在");
        }

        // 防止父节点设置为自己
        if (request.getId().equals(request.getParentId())) {
            return ApiResponse.failed(BizErrorCode.INVALID_STATE.getCode(), "父菜单不能是自己");
        }
        List<Long> parentPathIds = null;
        // 校验父节点是否存在
        if (request.getParentId() != null && request.getParentId() != 0) {
            Optional<Menu> optionalMenu = super.lambdaQuery().eq(Menu::getId, request.getParentId())
                .eq(Menu::getIsDeleted, false)
                .oneOpt();
            if (optionalMenu.isEmpty()) {
                return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "父菜单不存在");
            }
            parentPathIds = optionalMenu.get().getParentPathIds();
            if (EmptyUtil.isNotEmpty(parentPathIds)) {
                if (parentPathIds.size() <= 10) {
                    return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "菜单层级不能超过10级");
                }
                if (parentPathIds.contains(request.getId())) {
                    return ApiResponse.failed(BizErrorCode.NO_POWER.getCode(), "不能把当前子级添加为父菜单");
                }
            }
        }

        // 功能类型必须有权限码
        if (request.getType() == MenuType.FUNCTION) {
            if (request.getPermissionCode() == null || request.getPermissionCode().trim().isEmpty()) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "功能类型必须填写权限码");
            }
        }

        Menu menu = BeanUtil.copyProperties(request, Menu.class);
        menu.setUpdateDate(LocalDateTime.now());
        menu.setParentPathIds(List.of());
        if (menu.getPermissionCode() == null) {
            menu.setPermissionCode("");
        }
        if (EmptyUtil.isNotEmpty(parentPathIds)) {
            parentPathIds = new ArrayList<>(parentPathIds);
            parentPathIds.add(request.getParentId());
            menu.setParentPathIds(parentPathIds);
        }
        this.updateById(menu);
        log.info("[菜单] update menu, {} - {}", request.getType().getDesc(), request.getName());
        // todo 清除缓存
        return ApiResponse.success();
    }

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public ApiResponse<Void> deleteMenu(Long id) {
        Menu exist = this.getById(id);
        if (exist == null || exist.getIsDeleted()) {
            return ApiResponse.failed(SystemErrorCode.NOT_FOUND.getCode(), "菜单不存在");
        }

        // 检查是否有子节点
        boolean exists = super.lambdaQuery().eq(Menu::getParentId, id)
            .eq(Menu::getIsDeleted, false).exists();
        if (exists) {
            return ApiResponse.failed(BizErrorCode.NO_POWER.getCode(), "存在子菜单，不允许删除");
        }
        // 逻辑删除
        exist.setIsDeleted(true);
        exist.setUpdateDate(LocalDateTime.now());
        this.updateById(exist);
        log.info("[菜单] delete menu, {} - {}", exist.getType().getDesc(), exist.getName());
        // todo 清除缓存
        return ApiResponse.success();
    }

    /**
     * 获取菜单树
     *
     * @return 菜单树列表
     */
    @Override
    public List<MenuTreeResponse> getMenuTree() {
        BatchQuery<Long, Menu> batchQuery = new BatchQuery<>((limit, idx) -> {
            return super.lambdaQuery()
                .gt(idx != null, Menu::getId, idx)
                .eq(Menu::getIsDeleted, false)
                .last("limit " + limit)
                .list();
        }, Menu::getId);
        List<MenuTreeResponse> menuTreeResponses = BeanUtil.copyToList(batchQuery.get(), MenuTreeResponse.class);
        return CollUtil.tree(menuTreeResponses, 0L);
    }

    /**
     * 构建树形结构
     *
     * @param menuIds 菜单ID列表
     *
     * @return 树形结构列表
     */
    @Override
    public List<MenuTreeResponse> buildTree(List<Long> menuIds) {
        if (EmptyUtil.isEmpty(menuIds)) {
            return List.of();
        }
        List<MenuTreeResponse> responses = super.lambdaQuery()
            .in(Menu::getId, menuIds)
            .eq(Menu::getIsDeleted, false)
            .list()
            .stream()
            .map(menu -> BeanUtil.copyProperties(menu, MenuTreeResponse.class))
            .toList();
        return CollUtil.tree(responses, 0L);
    }


    /**
     * 根据ID列表查询菜单
     *
     * @param menuIds 菜单ID列表
     *
     * @return 菜单列表
     */
    @Override
    public List<MenuTreeResponse> listInIds(List<Long> menuIds) {
        if (EmptyUtil.isEmpty(menuIds)) {
            return List.of();
        }
        return super.lambdaQuery()
            .in(Menu::getId, menuIds)
            .eq(Menu::getIsDeleted, false)
            .list()
            .stream()
            .map(menu -> BeanUtil.copyProperties(menu, MenuTreeResponse.class))
            .toList();
    }

    /**
     * 批量插入菜单
     *
     * @param menuList 菜单列表
     */
    @Override
    public void batchInsert(List<Menu> menuList) {
        super.saveBatch(menuList);
    }

    /**
     * 删除所有菜单
     */
    @Override
    public void deleteAll() {
        super.lambdaUpdate()
            .eq(Menu::getIsDeleted, false)
            .set(Menu::getIsDeleted, true)
            .set(Menu::getUpdateDate, LocalDateTime.now())
            .update();
    }

    /**
     * 列出所有菜单ID
     */
    @Override
    public List<Long> listAllIds() {
        return new BatchQuery<>((limit ,idx) -> {
            return super.lambdaQuery()
                .gt(idx != null, Menu::getId, idx)
                .eq(Menu::getIsDeleted, false)
                .select(Menu::getId)
                .last("limit " + limit)
                .list();
        }, Menu::getId).batch(3000).get().stream().map(Menu::getId).toList();
    }


    /**
     * 列出普通菜单ID
     */
    @Override
    public List<Long> getOrdinaryMenuIds() {
        List<Menu> menuList = super.lambdaQuery()
            .eq(Menu::getIsDeleted, false)
            .in(Menu::getPermissionCode, ordinaryMenuCodes)
            .select(Menu::getId, Menu::getParentId)
            .list();
        if (EmptyUtil.isEmpty(menuList)) {
            return List.of();
        }

        return menuList.stream()
            .flatMap(m -> {
                return Stream.of(m.getId(), m.getParentId());
            }).filter(id -> id > 0).distinct().toList();
    }
}
