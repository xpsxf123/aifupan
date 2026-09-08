package com.jiuyu.replay.third.constant;

public class Constant {

    public enum CodeMsgEnum {
        TIP_CUSTOM(7001, "自定义提示"),
        NOT_QPS(7002, "Qps已满，请稍后再试"),
        NOT_BALANCE(7003, "没有余额");


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
