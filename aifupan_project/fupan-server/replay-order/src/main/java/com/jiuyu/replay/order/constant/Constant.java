package com.jiuyu.replay.order.constant;

public class Constant {

    public enum CodeMsgEnum {
        TIP_CUSTOM(6001, "自定义提示");

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
