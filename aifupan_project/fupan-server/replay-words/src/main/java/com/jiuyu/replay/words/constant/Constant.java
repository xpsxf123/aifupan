package com.jiuyu.replay.words.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class Constant {

    public enum CodeMsgEnum {
        TIP_CUSTOM(5003, "自定义提示"),
        TIP_CUSTOM_LOADING(5004, "数据加载中");

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
     * 提示词类型
     */
    public enum CueSourceTypeEnum {
        VIDEO(0, "视频资源"),
        FILE(1, "文件资源"),
        CONTRAST(2, "对比资源");

        private Integer code;
        private String msg;

        CueSourceTypeEnum(Integer code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public Integer getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }

    }

    @Getter
    @AllArgsConstructor
    public enum yesOrNoEnum {
        NO(0, "否"),
        YES(1, "是");
        private final Integer code;
        private final String msg;
    }
}
