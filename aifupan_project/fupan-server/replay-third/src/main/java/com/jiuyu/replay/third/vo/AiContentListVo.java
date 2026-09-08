package com.jiuyu.replay.third.vo;


import lombok.Data;

import java.util.List;

@Data
public class AiContentListVo {

    /**
     * AI返回的数据
     */
    List<String> aiContentList;
    /**
     * 当前对话的上下文缓存ID
     */
    String contextResultId;
}
