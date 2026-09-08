package com.jiuyu.governance.plugins.oauth.pojo;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.UserInfo;
import com.jiuyu.framework.util.EmptyUtil;

import java.util.Objects;

/**
 * 授权相关常量
 *
 * @author HeHui
 * @date 2026-03-17 15:52
 */
public class OauthConstant {

    private OauthConstant() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 客户端用户标识
     */
    public static final String CLIENT_USER = "client_user";


    /**
     * 爱复盘后台管理系统用户标识
     */
    public static final String SYSTEM_USER = "system_user";


    /**
     * 企业管理系统用户标识
     */
    public static final String GOVERNANCE_USER = "governance_user";




    /**
     * 数据权限 - 租户管理员标识
     */
    public static final String TENANT_ADMIN = "tenantAdmin";


    /**
     * 数据权限 - 部门标识
     */
    public static final String DEPT = "dept";

    /**
     * 数据权限 - 小组标识
     */
    public static final String TEAM = "team";

    /**
     * 数据权限 - 公司标识
     */
    public static final String COMPANY = "company";


    /**
     * 数据权限 - 直播间标识
     */
    public static final String LIVE_ROOM = "live_room";

    /**
     * 数据权限 - 员工标识
     */
    public static final String EMPLOYEE = "employee";



    /**
     * 获取数据权限级别
     *
     * @param type 数据权限类型
     * @return 数据权限级别
     */
    public static int getDataPermissionsLevel(String type) {
        return switch (type) {
            case COMPANY -> 1;
            case DEPT -> 2;
            case TEAM -> 3;
            case EMPLOYEE -> 4;
            case LIVE_ROOM -> 5;
            default -> Integer.MAX_VALUE;
        };
    }



    /**
     * 构建访问用户
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 访问用户
     */
    public static AccessUser buildAccessUser(long tenantId, long userId, String userType) {
        UserInfo userInfo = new UserInfo();
        userInfo.setAccessToken("");
        userInfo.setUserId(userId);
        userInfo.setUsername("");
        userInfo.setCurrentTenantId(tenantId);
        userInfo.setUserType(userType);
        userInfo.setLoginIp("");
        userInfo.setMetadata(null);
        return userInfo.toRecord();
    }

    /**
     * 是否是租户管理员
     *
     * @param accessUser 访问用户
     * @return 是否是租户管理员
     */
    public static boolean isTenantAdmin(AccessUser accessUser) {
        if (accessUser == null) {
            return false;
        }
        if (Objects.equals(accessUser.userType(), GOVERNANCE_USER)) {
            if (EmptyUtil.isNotEmpty(accessUser.metadata())) {
                return Objects.equals(accessUser.metadata().get(TENANT_ADMIN), "true");
            }
        }
        return false;
    }
}
