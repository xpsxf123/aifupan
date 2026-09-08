package com.jiuyu.governance.business.rbac.pojo.constants;

import com.jiuyu.framework.shandard.BaseEnum;
import lombok.Getter;

/**
 * 就职类型
 *
 * @author HeHui
 * @date 2026-03-18 16:11
 */
@Getter
public enum JobType implements BaseEnum<Integer> {
    FULL_TIME(1, "全职"),
    PART_TIME(2, "兼职"),
    ;

    private final Integer value;

    private final String desc;

    JobType(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
