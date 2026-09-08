package com.jiuyu.governance.business.performance.pojo.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据来源枚举
 *
 * @author lj
 * @date 2026-03-24
 */
@Getter
@AllArgsConstructor
public enum DataSource {

    SYSTEM(1, "系统录入"),
    MANUAL(2, "手动录入");

    private final int code;
    private final String desc;

    /**
     * 根据code获取枚举
     * @param code 枚举码
     * @return 枚举
     */
    public static DataSource fromCode(int code) {
        for (DataSource source : values()) {
            if (source.code == code) {
                return source;
            }
        }
        return null;
    }

    /**
     * 根据code获取枚举的desc
     * @param code 枚举码
     * @return 枚举的desc
     */
    public static String descByCode(int code) {
        DataSource dataSource = fromCode(code);

        if (dataSource == null){
            return null;
        }

        return dataSource.desc;
    }


}
