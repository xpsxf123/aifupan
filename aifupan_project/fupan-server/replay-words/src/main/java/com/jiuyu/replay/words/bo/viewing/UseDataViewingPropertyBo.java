package com.jiuyu.replay.words.bo.viewing;

import lombok.Data;

@Data
public class UseDataViewingPropertyBo {

    /**
     * 用户id
     */
    private Long userId;
    /**
     * 使用的数量 -1表示使用1个资源
     */
    private Long number;
}
