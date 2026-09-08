package com.jiuyu.replay.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/7/4 下午3:43
 */
public class UserEnums {

    public enum CodeMsgEnum {
        NO_LOGIN(4001, "没有登录"),
        NO_POWER(4002, "没有权限"),
        TIP_CUSTOM(4003, "自定义提示"),
        IS_REGISTER(4004, "自定义提示");

        private int code;
        private String msg;

        CodeMsgEnum(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    /**
     * 用户类型
     */
    @Getter
    @AllArgsConstructor
    public enum userType {
        CLIENT_USER(0, "爱复盘普通用户"),
        MANAGE_ADMIN_USER(1, "后台管理员"),
        CLIENT_CHILD_USER(2, "爱复盘子账号");
        private final int code;
        private final String msg;
    }

    /**
     * 后台管理员的用户类型
     */
    @Getter
    @AllArgsConstructor
    public enum adminUserType {
        ADMIN(0, "正常后台用户"),
        AGENT(1, "代理商"),
        AGENT_SALE(2, "代理商销售");
        private final int code;
        private final String msg;
    }

    /**
     * 销售类型
     */
    @Getter
    @AllArgsConstructor
    public enum salesType {
        ADMIN(0, "平台销售"),
        AGENT(1, "代理商销售"),
        ;
        private final int code;
        private final String msg;
    }



}
