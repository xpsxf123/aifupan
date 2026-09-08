package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "词语tab")
public class WordsTabVo {

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long cruxTypeId;
    /**
     * 是否统计到总数 0：否 1：是
     */
    @Schema(description = "是否统计到总数 0：否 1：是")
    private Integer isCount;
    /**
     * 类型 0：全部 1：敏感词总数 2：关键词总数 3：关键词分类
     */
    @Schema(description = "类型 0：全部 1：敏感词总数 2：关键词总数 3：关键词分类")
    private Integer tabType;
    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer tabSort;
    /**
     * 分类占比比例值，0.21表示21%
     */
    @Schema(description = "分类占比比例值，0.21表示21%")
    private Double scale;
    /**
     * 词语数量
     */
    @Schema(description = "词语数量")
    private Integer num;
    /**
     * tab名称
     */
    @Schema(description = "tab名称")
    private String tabName;

    /**
     * 有参构造器
     * @param cruxTypeId 分类ID
     * @param tabType 类型 0：全部 1：敏感词总数 2：关键词总数 3：关键词分类
     * @param tabSort 排序
     * @param scale 分类占比比例值，0.21表示21%
     * @param num 词语数量
     * @param tabName tab名称
     * @param isCount 是否统计到总数 0：否 1：是
     */
    public WordsTabVo(long cruxTypeId, int tabType, int tabSort, double scale, int num, String tabName, int isCount) {
        this.cruxTypeId = cruxTypeId;
        this.tabType = tabType;
        this.tabSort = tabSort;
        this.scale = scale;
        this.num = num;
        this.tabName = tabName;
        this.isCount = isCount;
    }
}
