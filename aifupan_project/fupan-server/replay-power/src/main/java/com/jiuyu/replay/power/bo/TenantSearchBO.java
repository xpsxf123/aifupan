package com.jiuyu.replay.power.bo;

import lombok.Getter;
import lombok.Setter;

/**
 * 租户搜索查询参数
 *
 * @author HeHui
 * @date 2026-03-05 10:19
 */
@Getter
@Setter
public class TenantSearchBO {

    /**
     * 关键字 暂时只查询租户下关联用户名称
     */
    private String keyword;


    /**
     *  用户类型 0：主账户  2：子账号
     *  默认查询主账户的名称
     */
    private Integer userType = 0;


    /**
     * 搜索返回条数
     */
    public Integer limit = 10;



    public Integer getLimit() {
        if (limit == null || limit < 1) {
            return 10;
        }
        if (limit > 1000) {
            return 1000;
        }
        return limit;
    }
}
