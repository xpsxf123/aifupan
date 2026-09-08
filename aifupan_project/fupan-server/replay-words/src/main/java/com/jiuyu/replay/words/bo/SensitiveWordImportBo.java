package com.jiuyu.replay.words.bo;

import lombok.Data;

@Data
public class SensitiveWordImportBo {

    /**
     * 示例词
     */
    private String name;
    /**
     * 分组
     */
    private String groupStr;
    /**
     * 行业，多级用/分开
     */
    private String tradeStr;
    /**
     * 违规类型
     */
    private String typeStr;
    /**
     * 违规等级
     */
    private String levelStr;
    /**
     * 概览
     */
    private String overView;
    /**
     * 描述
     */
    private String remarks;
    /**
     * 相似词
     */
    private String similarWords;
}
