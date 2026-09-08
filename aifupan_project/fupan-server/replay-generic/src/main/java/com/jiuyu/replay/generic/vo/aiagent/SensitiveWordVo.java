package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 行业敏感词条目（按行业父链聚合后的单条禁词，已按词名去重、保留最严重等级）。
 *
 * @author fupan-server
 */
@Data
public class SensitiveWordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 词本身（含主词与相似词，相似词已展开为独立条目）。
     */
    private String word;

    /**
     * 等级：0=1 级(封号) 1=2 级(严重警告) 2=3 级(警告)。数值越小越严重。
     */
    private Integer level;

    /**
     * 等级中文标签，便于模型直接理解严重度。
     */
    private String levelLabel;

    /**
     * 类型：0=广告 1=品牌 2=国家 3=限制词 4=其他。
     */
    private Integer type;

    /**
     * 类型中文标签。
     */
    private String typeLabel;

    /**
     * 相似词（原始库以 {@code _} 分隔的变体串；多数变体已展开为独立 {@link #word}，此处保留兜底）。
     */
    private String similarWords;
}
