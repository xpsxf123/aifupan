package com.jiuyu.replay.words.vo;

import lombok.Data;

@Data
public class OnlineAnalysisItemVo {

    /**
     * ID
     */
    private Long id;
    /**
     * 文件/视频唯一标识uuid
     */
    private String fileUuid;
    /**
     * 当前段落 从1开始
     */
    private Integer paragraph;
    /**
     * 识别状态状态 0：成功 1：失败
     */
    private Integer status;
    /**
     * 词语json字符串内容
     */
    private String dataJson;
    /**
     * 行业id
     */
    private Long tradeId;
}
