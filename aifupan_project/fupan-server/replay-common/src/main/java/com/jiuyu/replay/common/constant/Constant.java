package com.jiuyu.replay.common.constant;

public class Constant {

    public enum CodeMsgEnum {
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

    /**
     * 通用枚举类
     */
    public enum GeneralEnum {
        GENERAL_YES(1, "是"), GENERAL_NO(0, "否");

        private int code;
        private String msg;

        GeneralEnum(int code, String msg) {
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
     * 通用枚举类
     */
    public enum UnKnownDataEnum {

        UN_KNOWN_DATA_SHOW(1, "未知");

        private int code;
        private String title;

        UnKnownDataEnum(int code, String title) {
            this.code = code;
            this.title = title;
        }

        public int getCode() {
            return code;
        }

        public String getTitle() {
            return title;
        }
    }


}
