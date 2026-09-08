package com.jiuyu.governance.business.performance.pojo.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 组织来源类型枚举
 * 用于业绩汇总查询中标识数据的组织维度
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@AllArgsConstructor
public enum SourceType {

    /** 租户（全局） */
    TENANT("tenant"),

    /** 分公司 */
    SUB_COMPANY("subCompany"),

    /** 部门 */
    DEPT("dept"),

    /** 小组 */
    TEAM("team"),

    /** 直播间 */
    LIVE_ROOM("liveRoom");

    private final String value;

    /**
     * 根据字符串值获取枚举
     *
     * @param value 字符串值
     * @return 匹配的枚举，未找到返回null
     */
    public static SourceType fromValue(String value) {
        if (value == null) return null;
        for (SourceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
