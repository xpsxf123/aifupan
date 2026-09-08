package com.jiuyu.governance.business.rbac.pojo.constants;

import com.jiuyu.framework.shandard.BaseEnum;
import lombok.Getter;

/**
 * 菜单类型
 *
 * @author HeHui
 * @date 2026-03-18 12:26
 */
@Getter
public enum MenuType implements BaseEnum<Integer> {

    /**
     * 0：菜单，左侧导航菜单项，参与权限控制
     */
    MENU(0, "菜单"),

    /**
     * 1：功能，按钮、API接口权限，参与权限控制，需要权限码
     */
    FUNCTION(1, "功能"),

    /**
     * 2：目录，菜单分组目录，不参与权限控制（仅展示）
     */
    DIRECTORY(2, "目录"),
    ;

    private final Integer value;

    private final String desc;

    MenuType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
