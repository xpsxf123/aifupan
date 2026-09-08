package com.jiuyu.governance.business.rbac.pojo.constants;

import com.jiuyu.framework.shandard.BaseEnum;
import lombok.Getter;

/**
 * 默认岗位
 *
 * @author HeHui
 * @date 2026-03-17 19:42
 */
@Getter
public enum DefaultPosition implements BaseEnum<String> {
    ANCHOR("anchor", "主播"),
    SUB_ANCHOR("sub_anchor", "副播"),
    OPERATION("operation", "运营"),
    CONTROL("control", "中控"),
    TOU_CHER("tou_cher", "投手"),
    EDITOR("editor", "剪辑"),
    GUEST("guest", "嘉宾"),
    COMPLIANCE_SPECIALIST("compliance_specialist", "合规专员"),
    COMPLIANCE_MANAGER("compliance_manager", "合规经理"),
    COMPLIANCE_LEADER("compliance_leader", "合规负责人"),
    ;

    private final String value;

    private final String desc;

    DefaultPosition(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
