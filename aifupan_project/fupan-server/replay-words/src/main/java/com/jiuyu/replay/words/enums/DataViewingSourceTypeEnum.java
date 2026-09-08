package com.jiuyu.replay.words.enums;

import lombok.Getter;

/**
 * 数据看板数据来源类型
 */
@Getter
public enum DataViewingSourceTypeEnum {

    CHANMAMA(0, "蝉妈妈"),
    JULIANGBAIYING(1, "巨量百应");

    private final Integer type;
    private final String msg;

    DataViewingSourceTypeEnum(Integer type, String msg) {
        this.type = type;
        this.msg = msg;
    }


}
