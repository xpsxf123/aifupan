package com.jiuyu.replay.words.bo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SaveAnalysisCacheBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据json
     */
    private String dataJson;
    /**
     * 类型 0：视频 1：文件
     */
    private Integer type;
    /**
     * 文件或视频唯一标识
     */
    private String fileOrVideoId;
    /**
     * 识别状态  0：成功 1：失败
     */
    private Integer status;
    /**
     * 当前段落，从1开始
     */
    private Integer paragraph;
    /**
     * 行业id
     */
    private Long tradeId;
}
