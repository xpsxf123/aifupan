package com.jiuyu.governance.business.org.pojo.constants;

import com.jiuyu.framework.shandard.BaseEnum;
import com.jiuyu.governance.plugins.oauth.pojo.OauthConstant;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 管理类型
 *
 * @author HeHui
 * @date 2026-03-18 17:40
 */
@Getter
public enum ManagerType implements BaseEnum<Integer> {

    COMPANY(1, "公司", OauthConstant.COMPANY, null),
    DEPT(2, "部门", OauthConstant.DEPT, COMPANY),
    TEAM(3, "小组", OauthConstant.TEAM, DEPT),
    LIVE_ROOM(4, "直播间", OauthConstant.LIVE_ROOM, TEAM),
    EMPLOYEE(5, "员工", OauthConstant.EMPLOYEE, LIVE_ROOM);

    /**
     * 编号
     */
    private final Integer value;

    private final String desc;

    /**
     * 数据权限码
     */
    private final String code;

    /**
     * 上级组织
     */
    private final ManagerType superior;

    ManagerType(Integer value, String desc, String code, ManagerType superior) {
        this.value = value;
        this.desc = desc;
        this.code = code;
        this.superior = superior;
    }


    /**
     * 获取所有下级组织类型列表
     * 从当前类型开始，递归获取所有子级类型直到没有下级为止
     *
     * @return List<ManagerType> 下级组织类型列表，如果当前类型没有子级则返回 null
     */
    public List<ManagerType> getSubs() {
        List<ManagerType> types = new ArrayList<>(4);
        // 获取第一个子级类型
        ManagerType nextSon = getSub();
        if (nextSon == null) {
            return null;
        }
        types.add(nextSon);
        int length = values().length;
        // 递归获取后续所有子级类型
        for (int i = 0; i < length; i++) {
            nextSon = nextSon.getSub();
            if (nextSon == null) {
                break;
            }
            types.add(nextSon);
        }
        return types;
    }

    /**
     * 获取下一级的子类型
     */
    private ManagerType getSub() {
        return Arrays.stream(values()).filter(v -> Objects.equals(v.superior, this)).findFirst().orElse(null);
    }


}
