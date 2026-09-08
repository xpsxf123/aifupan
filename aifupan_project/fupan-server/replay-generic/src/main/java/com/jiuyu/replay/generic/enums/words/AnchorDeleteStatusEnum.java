package com.jiuyu.replay.generic.enums.words;

import lombok.Getter;

@Getter
public enum AnchorDeleteStatusEnum {

    NOT_DELETE(0, "未删除"),
    RECORD_LIST_DELETE(1, "已从录制列表删除"),
    RECOVER_LIST_DELETE(2, "已从恢复列表删除");

    private final Integer status;
    private final String remarks;

    AnchorDeleteStatusEnum(Integer status, String remarks) {
        this.status = status;
        this.remarks = remarks;
    }
}
