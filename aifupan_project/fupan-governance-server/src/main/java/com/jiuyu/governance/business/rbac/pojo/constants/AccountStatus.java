package com.jiuyu.governance.business.rbac.pojo.constants;

import com.jiuyu.framework.shandard.BaseEnum;
import lombok.Getter;

/**
 * 账户状态
 *
 * @author HeHui
 * @date 2026-03-18 16:16
 */
@Getter
public enum AccountStatus implements BaseEnum<Integer> {

    DISABLED(0, "停用"),
    NORMAL(1, "正常"),
    FREEZE(2, "冻结"),
    ;

    private final Integer value;

    private final String desc;

    AccountStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }


}
