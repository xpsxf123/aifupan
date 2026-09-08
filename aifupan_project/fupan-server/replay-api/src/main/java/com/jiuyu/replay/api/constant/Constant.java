package com.jiuyu.replay.api.constant;

public class Constant {

    public enum CodeMsgEnum {
        ALLOW_SKIP(1000, "允许跳过"),
        TIP_CUSTOM(3001, "自定义提示");

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
