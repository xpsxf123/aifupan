package com.jiuyu.replay.power.constant;

public class Constant {

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
}
