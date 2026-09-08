package com.jiuyu.replay.activity.constant;

public class Constant {

    public enum CodeMsgEnum {
        TIP_CUSTOM(9001, "自定义提示");

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
