package com.jiuyu.governance.business.rbac.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.rbac.mapper.MenuMapper;
import com.jiuyu.governance.business.rbac.pojo.entity.Menu;
import com.jiuyu.governance.business.rbac.pojo.response.MenuTreeResponse;
import com.jiuyu.governance.business.rbac.service.MenuService;
import com.jiuyu.governance.business.rbac.service.RoleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * 测试 - 菜单API
 *
 * @author HeHui
 * @date 2026-03-24 19:46
 */
//@RestController
//@RequestMapping("/api/governance/menu-test")
@Slf4j
public class MenuTestController {

    private final MenuService menuService;

    private final RoleService roleService;

    public MenuTestController(MenuService menuService, RoleService roleService) {
        this.menuService = menuService;
        this.roleService = roleService;
    }


    /**
     * 导入
     *  最多只支持3层菜单, 注意每次都是删除然后再插入
     * @param menuTrees 菜单树
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @Transactional(rollbackFor = Throwable.class)
    @PostMapping("/import")
    public ApiResponse<Void> push(@RequestBody @Valid List<MenuTreeResponse> menuTrees) {
        Snowflake snowflake = IdUtil.getSnowflake();
        LocalDateTime now = LocalDateTime.now();
        List<Menu> menuList = menuTrees.stream().flatMap(tree -> {
            Menu level1 = getMenu(tree, snowflake, now);
            if (EmptyUtil.isNotEmpty(tree.getChildren())) {
                Stream<Menu> menuStream = tree.getChildren().stream().flatMap(child -> {
                    Menu level2 = getMenu(child, snowflake, now);
                    level2.setParentId(level1.getId());
                    level2.setParentPathIds(List.of(level1.getId()));
                    if (EmptyUtil.isNotEmpty(child.getChildren())) {
                        return Stream.concat(child.getChildren().stream().flatMap(child2 -> {
                            Menu level3 = getMenu(child2, snowflake, now);
                            level3.setParentId(level2.getId());
                            level3.setParentPathIds(List.of(level1.getId(), level2.getId()));
                            return Stream.of(level3);
                        }), Stream.of(level2));
                    }
                    return Stream.of(level2);
                });
                return Stream.concat(menuStream, Stream.of(level1));
            }
            return Stream.of(level1);
        }).toList();
        if (EmptyUtil.isNotEmpty(menuList)) {
            menuService.deleteAll();
            menuService.batchInsert(menuList);
            roleService.clearAllCache();
        }
        return ApiResponse.success();
    }

    private static Menu getMenu(MenuTreeResponse tree, Snowflake snowflake, LocalDateTime now) {
        Menu level1 = BeanUtil.copyProperties(tree, Menu.class);
        level1.setId(snowflake.nextId());
        level1.setParentPathIds(List.of());
        level1.setIsDeleted(false);
        level1.setCreateDate(now);
        level1.setUpdateDate(now);
        level1.setParentId(0L);
        log.info("[菜单] test menu import {} - {}", level1.getType().getDesc(), level1.getName());
        return level1;
    }
}
