package com.jiuyu.governance.plugins.oss.enums;

import lombok.Getter;

/**
 * OSS 桶枚举
 * <p>
 * 定义业务使用的桶别名，与配置文件中的 buckets 配置对应
 * </p>
 *
 * @author lj
 */
@Getter
public enum OssBucket {

    /**
     * AI数据桶（对应 replay-ai-data）
     */
    AI_DATA("ai-data", "AI数据存储"),

    /**
     * 分析数据桶（对应 replay-analysis-data）
     */
    ANALYSIS_DATA("analysis-data", "分析数据存储"),

    /**
     * 图片桶（对应 replay-images）
     */
    IMAGES("images", "图片存储");

    /**
     * 桶别名（对应配置文件中的 key）
     */
    private final String alias;

    /**
     * 桶描述
     */
    private final String description;

    OssBucket(String alias, String description) {
        this.alias = alias;
        this.description = description;
    }

    /**
     * 根据别名获取枚举
     *
     * @param alias 桶别名
     * @return 桶枚举，不存在则返回 null
     */
    public static OssBucket fromAlias(String alias) {
        for (OssBucket bucket : values()) {
            if (bucket.getAlias().equals(alias)) {
                return bucket;
            }
        }
        return null;
    }
}
